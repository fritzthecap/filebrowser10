package fri.gui.swing.text;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import fri.util.props.ClassProperties;
import fri.gui.CursorUtil;
import fri.gui.text.*;
import fri.gui.swing.searchdialog.*;
import fri.gui.swing.golinedialog.*;
import fri.gui.swing.ComponentUtil;

/**
	A JTextArea with a search dialog and a line wrap option per popup.
*/

public class ComfortTextArea extends OutputTextArea implements
	ActionListener,
	MouseListener,
	KeyListener
{
	protected SearchReplace searchDlg = null;
	protected JPopupMenu popup;
	protected JMenuItem find, goline, wrapLines, customize;
	protected JMenu tabWidth;
	protected JMenuItem w1, w2, w3, w4, w6, w8;
	private int rowHeight;


	public ComfortTextArea()	{
		this(30, 80);
	}

	public ComfortTextArea(int rows, int columns)	{
		super(rows,columns);
		initComfortTextArea(null);
	}

	public ComfortTextArea(String text)	{
		this(text, null);
	}
	
	public ComfortTextArea(String text, ActionListener customizer)	{
		super(30, 80);
		initComfortTextArea(customizer);
		setText(text);
	}
	
	protected void initComfortTextArea(ActionListener customizer)	{
		setToolTipText("");
		
		new TextAreaSeparatorDefinition(this);
		if (getCaret() instanceof DefaultCaret)
			setCaret(new VisibleCaret((DefaultCaret)getCaret()));	// respect caret of separator definition
		setCaretColor(Color.red);
		
		String tabSize = ClassProperties.get(getClass(), "tabSize");
		setTabSize(tabSize != null ? Integer.valueOf(tabSize).intValue() : 4);

		popup = new JPopupMenu();

		addFind();
		addGoLine();
		addTabWidth();
		addWrapLines();
		
		if (customizer != null)	{
			popup.addSeparator();
			JMenuItem mi = new JMenuItem("Customize GUI");
			popup.add(mi);
			mi.addActionListener(customizer);
		}

		addKeyListener(this);

		addMouseListener(this);
	}


	/** Add Menuitem "Find". */
	protected void addFind()	{
		popup.add(find = new JMenuItem(findLabel()));
		find.addActionListener(this);
	}

	protected String findLabel()	{
		return "Find";
	}

	/** Add Menuitem "Go To Line". */
	protected void addGoLine()	{
		popup.add(goline = new JMenuItem(goLineLabel()));
		goline.addActionListener(this);
	}

	protected String goLineLabel()	{
		return "Go To Line";
	}

	/** Add Menu "Tab Width". */
	protected void addTabWidth()	{
		popup.addSeparator();

		popup.add(tabWidth = new JMenu(tabWidthLabel()));
		
		tabWidth.add(w1 = new JMenuItem("1"));
		w1.addActionListener(this);
		tabWidth.add(w2 = new JMenuItem("2"));
		w2.addActionListener(this);
		tabWidth.add(w3 = new JMenuItem("3"));
		w3.addActionListener(this);
		tabWidth.add(w4 = new JMenuItem("4"));
		w4.addActionListener(this);
		tabWidth.add(w6 = new JMenuItem("6"));
		w6.addActionListener(this);
		tabWidth.add(w8 = new JMenuItem("8"));
		w8.addActionListener(this);
	}

	protected String tabWidthLabel()	{
		return "Tab Width";
	}

	/** Add Menuitem "Wrap Lines". */
	protected void addWrapLines()	{
		String s = ClassProperties.get(getClass(), "lineWrap");
		boolean lineWrap = (s != null && s.equals("true")) ? true : false;
		setLineWrap(lineWrap);

		popup.add(wrapLines = new JCheckBoxMenuItem(wrapLinesLabel(), lineWrap));
		wrapLines.addActionListener(this);
	}

	protected String wrapLinesLabel()	{
		return "Wrap Lines";
	}



	/** Shows line numbers in tooltip. */
	public String getToolTipText(MouseEvent e)	{
		int line = e.getY() / computeRowHeigth();
		if (line < 0)
			return getToolTipText();

		int max = getDocument().getDefaultRootElement().getElementCount();
		if (line >= max)
			return getToolTipText();

		return "Line "+(line + 1);
	}


	public void setDocument(Document doc)	{
		super.setDocument(doc);
		
		if (searchDlg != null)	{	// we must reset find dialog to avoid wrong positions
			if (searchDlg.isVisible())	{
				find();
			}
			else	{
				//searchDlg.init((TextHolder)this);
				searchDlg.setTextChanged();
			}
		}
	}
	
	
	protected int computeRowHeigth()	{
		if (rowHeight > 0)
			return rowHeight;
			
		try	{
			Rectangle r = modelToView(0);
			return rowHeight = r.height;
		}
		catch (BadLocationException e)	{
			e.printStackTrace();
		}
		return -1;
	}
	

	/**
		Returns the popup menu with "find" and "wrap lines".
	*/
	public JPopupMenu getPopupMenu()	{
		return popup;
	}



	/**
		Allocates a go-line dialog if not done. Returns true if it was
		allocated now, then setVisible() needs not to be called.
	*/
	public void goline()	{
		new GoLineDialog(ComponentUtil.getFrame(this), this);
	}

	
	/**
		Allocates a search dialog if not done. Returns true if it was
		allocated now, then setVisible() needs not to be called.
	*/
	protected boolean ensureSearchDlg()	{	
		if (searchDlg == null)	{
			searchDlg = new SearchReplace((JFrame)ComponentUtil.getFrame(this), (TextHolder)this);
			return true;
		}
		else	{
			return false;
		}
	}

	/** Set the search dialog visible and search for passed pattern. */
	public void find(String pattern, String syntax, boolean ignoreCase, boolean wordMatch)	{
		findPattern(pattern, syntax, ignoreCase, wordMatch);
	}

	/** Set the search dialog visible and load it with selected text. */
	public void find()	{
		CursorUtil.setWaitCursor(this);
		try	{
			String s = getSelectedText();
			if (s != null)	{
				ensureSearchDlg();
				findPattern(s, null, searchDlg.getIgnoreCase(), searchDlg.getWordMatch());
			}
			else
			if (ensureSearchDlg() == false)	{
				searchDlg.init((TextHolder)this, true);
				searchDlg.setVisible(true);
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}
	
	private void findPattern(String pattern, String syntax, boolean ignoreCase, boolean wordMatch)	{
		boolean b = ensureSearchDlg();
		searchDlg.init(this, pattern, syntax, ignoreCase, wordMatch); // search for pattern
		if (b == false)
			searchDlg.setVisible(true);
	}


	/** implements ActionListener: catch actions from popup. */	
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == find)	{
			find();
		}
		else
		if (e.getSource() == goline)	{
			goline();
		}
		else
		if (e.getSource() == wrapLines)	{
			setLineWrap(!getLineWrap());

			ClassProperties.put(getClass(), "lineWrap", getLineWrap() ? "true" : "false");
			ClassProperties.store(getClass());
		}
		else
		if (e.getSource() == w1 ||
				e.getSource() == w2 ||
				e.getSource() == w3 ||
				e.getSource() == w4 ||
				e.getSource() == w6 ||
				e.getSource() == w8)
		{
			String n = ((JMenuItem)e.getSource()).getText();
			System.err.println("setting tab size: "+n);
			int i = Integer.valueOf(n).intValue();
			setTabSize(i);
			
			ClassProperties.put(getClass(), "tabSize", n);
			ClassProperties.store(getClass());
		}
	}

	/** implements MouseListener: popup the menu. */	
	public void mousePressed(MouseEvent e)	{
		showPopup(e);
	}
	/** implements MouseListener: popup the menu. */	
	public void mouseReleased(MouseEvent e)	{
		showPopup(e);
	}
	public void mouseClicked(MouseEvent e)	{
	}
	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}

	private void showPopup(MouseEvent e)	{
		if (e.isPopupTrigger())	{
			if (wrapLines != null)	{
				wrapLines.removeActionListener(this);
				wrapLines.setSelected(getLineWrap());
				wrapLines.addActionListener(this);
			}
			popup.show(this, e.getX(), e.getY());
		}
	}

	/** implements KeyListener: Ctl-F, F3, Ctl-G. */	
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_F && e.isControlDown())
			find();
		else
		if (e.getKeyCode() == KeyEvent.VK_G && e.isControlDown())
			goline();
		else
		if (e.getKeyCode() == KeyEvent.VK_F3)
			if (searchDlg == null)
				find();
			else
				searchDlg.findNext();
	}
	public void keyReleased(KeyEvent e)	{
	}
	public void keyTyped(KeyEvent e)	{
	}
	
	

	// test main
	/*
	public static void main(String [] args)	{
		JFrame f = new JFrame();
		f.getContentPane().add(new ComfortTextArea("Hallo Welt!\nWie gehts?"));
		f.setSize(200, 200);
		f.setVisible(true);
	}
	*/
	
}

		

class VisibleCaret extends DefaultCaret
{
	private DefaultCaret original;
	
	VisibleCaret(DefaultCaret original)	{
		this.original = original;
	}
	
	public void focusGained(FocusEvent evt)	{
		if (getComponent().isEnabled())	{
			setVisible(true);
			setSelectionVisible(true);
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (original != null)
			original.mouseClicked(e);
		else
			super.mouseClicked(e);
	}
}