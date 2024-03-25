package fri.gui.swing.filebrowser;

import java.io.File;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import fri.gui.CursorUtil;
import fri.gui.swing.application.*;
import fri.gui.swing.editor.*;
import fri.util.os.OS;

/**
	Frame that manages SearchResult-TextAreas showing search results from 0-n files.
	It provides a popup menu to view/edit one or all of the files.
*/

public class SearchResultFrame extends GuiApplication implements
	ActionListener,	// popup
	FocusListener,	// current textarea
	MouseListener	// show popup
{
	private static final String TITLE = "Text Search Results";
	private JPopupMenu popup;
	private JPanel p;
	private JMenuItem view, edit, clear, viewAll, editAll, report;
	private SearchResult current = null;

	
	public SearchResultFrame()	{
		super(TITLE);
		
		p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

		JScrollPane sp = new JScrollPane(p);
		JScrollBar sb = sp.getVerticalScrollBar();
		sb.setUnitIncrement(17);

		Container c = getContentPane();
		c.add(sp);
		
		popup = new JPopupMenu();
		view = new JMenuItem("View Selected");
		popup.add(view);
		view.addActionListener(this);
		edit = new JMenuItem("Edit Selected");
		popup.add(edit);
		edit.addActionListener(this);
		popup.addSeparator();
		viewAll = new JMenuItem("View All");
		popup.add(viewAll);
		viewAll.addActionListener(this);
		editAll = new JMenuItem("Edit All");
		popup.add(editAll);
		editAll.addActionListener(this);
		popup.addSeparator();
		clear = new JMenuItem("Remove Selected");
		popup.add(clear);
		clear.addActionListener(this);
		popup.addSeparator();
		report = new JMenuItem("Text Summary");
		popup.add(report);
		report.addActionListener(this);
		
		sp.setPreferredSize(new Dimension(700, 700));
		init(new Component [] { view, edit, viewAll, editAll, clear });
	}



	/** Add a new grep result to frame singleton. */
	public synchronized void addFoundLines(
		File file,
		int found,
		String lines,
		String pattern,
		String syntax,
		boolean ignoreCase,
		boolean wordMatch)
	{
		addFoundLines(new SearchResult(file, found, lines, pattern, syntax, ignoreCase, wordMatch));
		
		if (isVisible() == false)	{
			setVisible(true);
		}
	}
	
	/** Remove all grep results from frame singleton. */
	public synchronized void removeAllFoundLines()	{
		if (isShowing())
			removeAllResultsAndRepaint();
		else
			removeAllResults();
	}
	


	/** Returns true if frame has at least one grep result. */
	public boolean hasEntries()	{
		return p.getComponentCount() > 0;
	}

	/** Add a new grep result to frame. */
	void addFoundLines(SearchResult result)	{
		p.add(result);
		p.revalidate();
		result.addFocusListener(this);
		result.addMouseListener(this);
		setTitle(p.getComponentCount()+" "+TITLE);
	}


	// interface Closeable

	/** Implements Closeable to dispose all results and setVisible(false).  */
	public synchronized boolean close()	{
		setVisible(false);
		removeAllResults();
		return true;	//super.close();	// does dispose()
	}
	

	void removeAllResults()	{
		Component [] comps = p.getComponents();
		for (int i = 0; i < comps.length; i++)	{
			comps[i].removeMouseListener(this);
			comps[i].removeFocusListener(this);
		}
		p.removeAll();
		setTitle(TITLE);
	}

	void removeAllResultsAndRepaint()	{
		synchronized(SearchResult.class)	{
			removeAllResults();
			p.revalidate();
			p.repaint();
		}
	}
	

	// interface FocusListener
	
	public void focusGained(FocusEvent e)	{
		Object o = e.getSource();
		if (o instanceof SearchResult)	{
			setSelectedTextArea((SearchResult)o);
		}
	}
	public void focusLost(FocusEvent e)	{
	}


	// interface ActionListener

	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == view)	{
			current.view();
		}
		else
		if (e.getSource() == edit)	{
			current.edit();
		}
		else
		if (e.getSource() == viewAll)	{
			viewAll();
		}
		else
		if (e.getSource() == editAll)	{
			editAll();
		}
		else
		if (e.getSource() == clear)	{
			removeCurrent();
		}
		else
		if (e.getSource() == report)	{
			reportAsText();
		}
	}


	private void viewAll()	{
		CursorUtil.setWaitCursor(this);
		try	{
			Component [] comps = p.getComponents();
			for (int i = 0; i < comps.length; i++)	{
				SearchResult ta = (SearchResult)comps[i];
				ta.view();
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}

	private void editAll()	{
		CursorUtil.setWaitCursor(this);
		try	{
			EditorFrame ed = null;
			String syntax = "", pattern = "";
			boolean ignoreCase = false, wordMatch = false;
			boolean consistent = true;
			Component [] comps = p.getComponents();
	
			for (int i = 0; i < comps.length; i++)	{
				SearchResult ta = (SearchResult)comps[i];
				ed = ta.editNoFind();
	
				if (i == 0)	{
					ignoreCase = ta.getIgnoreCase();
					wordMatch = ta.getWordMatch();
					syntax = ta.getSyntax();
					pattern = ta.getPattern();
				}
				else	{
					if (ignoreCase != ta.getIgnoreCase() ||
							wordMatch != ta.getWordMatch() ||
							syntax.equals(ta.getSyntax()) == false ||
							pattern.equals(ta.getPattern()) == false)
					{
						consistent = false;
					}
				}
			}
			
			if (consistent)
				ed.find(pattern, syntax, ignoreCase, wordMatch);
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}

	private void reportAsText()	{
		CursorUtil.setWaitCursor(this);
		try	{
			StringBuffer sb = new StringBuffer(getTitle());
			sb.append(OS.newline);
			Component [] comps = p.getComponents();
			for (int i = 0; i < comps.length; i++)	{
				SearchResult ta = (SearchResult)comps[i];
				sb.append("Found "+ta.getFoundLocations()+" match(es) of \""+ta.getPattern()+"\" in "+ta.getFile().getAbsolutePath()+OS.newline);
				sb.append(ta.getText());
				sb.append(OS.newline);
			}
			
			new FileViewer(sb.toString());
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}

	private void removeCurrent()	{
		current.removeMouseListener(this);
		current.removeFocusListener(this);
		p.remove(current);
		p.revalidate();
		p.repaint();

		// look for next component to select
		boolean found = false;
		Component [] comps = p.getComponents();
		for (int i = 0; i < comps.length; i++)	{
			if (current == comps[i])	{	// found this textarea
				found = true;
				current = null;
			}
			else
			if (found)	{	// we are at next component
				setSelectedTextArea((SearchResult)comps[i]);
				return;
			}
		}
	}


	private void setSelectedTextArea(SearchResult ta)	{
		if (current != null)	{
			// restore original border to current selected
			TitledBorder orig = (TitledBorder)current.getBorder();
			Border origBorder = ((TitledBorder)ta.getBorder()).getBorder();
			orig.setTitleColor(((TitledBorder)ta.getBorder()).getTitleColor());
			orig.setBorder(origBorder);
			current.repaint();
		}
		
		current = ta;	// change focus
		
		TitledBorder newBorder = (TitledBorder)current.getBorder();
		newBorder.setTitleColor(Color.black);
		newBorder.setBorder(BorderFactory.createLineBorder(Color.black));
		current.repaint();
	}
	
	
	// interface MouseListener

	public void mousePressed(MouseEvent e)	{
		Object o = e.getSource();
		if (o instanceof SearchResult)	{
			setSelectedTextArea((SearchResult)o);
		}
		showPopup(e);
	}
	/** manage double click: open node in renderer */
	public void mouseClicked(MouseEvent e)	{
		if (e.getClickCount() >= 2)	{
			current.view();
		}
	}
	/** show popup-menu */
	public void mouseReleased(MouseEvent e)	{
		showPopup(e);
	}
	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}

	private void showPopup(MouseEvent e)	{
		if (e.isPopupTrigger() && hasEntries())	{
			popup.show((Component)e.getSource(), e.getX(), e.getY());
		}
	}

}