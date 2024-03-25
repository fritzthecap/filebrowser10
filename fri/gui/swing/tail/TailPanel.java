package fri.gui.swing.tail;

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import gnu.regexp.*;
import fri.util.os.OS;
import fri.util.regexp.*;
import fri.util.text.lines.TextlineList;
import fri.gui.CursorUtil;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.text.ComfortTextArea;
import fri.gui.swing.fileloader.*;
import fri.gui.swing.combo.history.*;

/**
	Panel holding action buttons and textarea of TailFrame.
	
	@author Ritzberger Fritz
*/

public class TailPanel extends JPanel implements
	LoadObserver,
	Runnable,
	ActionListener
{
	protected static JFileChooser fileChooser;
	private static final String APPEND_ONLY = "Append And Truncate Only";
	private static final String FULL_RELOAD = "Full Reload On Every Change";
	private ComfortTextArea textarea;
	private File file;
	private long size, date;
	private boolean interrupted = false, suspended = false;
	
	private JButton suspend, resume, open, clear, reload, filter, find;
	private HistCombo filterText;
	private JComboBox include, mode;
	private Frame frame;

	private String origText;
	private String pattern;
	
	
	
	/** Create new tail panel with passed file. */
	public TailPanel(File file, Frame frame) {
		super(new BorderLayout());
		
		this.frame = frame;	// setting title when new file gets set

		textarea = new ComfortTextArea()	{
			public String getToolTipText(MouseEvent e)	{
				if (pattern == null)
					return super.getToolTipText(e);
				return null;
			}
		};
		
		textarea.setEditable(false);
		add(new JScrollPane(textarea), BorderLayout.CENTER);

		JToolBar tb = new JToolBar(JToolBar.HORIZONTAL);
		if (OS.isAboveJava13) tb.setRollover(true);

		tb.add(open = new JButton("Open"));
		open.setToolTipText("Open A File To Observe For Changes");
		open.addActionListener(this);
		tb.add(mode = new JComboBox());
		mode.setToolTipText("Monitor All File Changes, Or Appending And Truncating Only");
		mode.addItem(APPEND_ONLY);
		mode.addItem(FULL_RELOAD);
		tb.addSeparator();
		tb.add(suspend = new JButton("Suspend"));
		suspend.setToolTipText("Pause Polling For Updates");
		suspend.addActionListener(this);
		suspend.setEnabled(false);
		tb.add(resume = new JButton("Resume"));
		resume.setToolTipText("Poll For Updates");
		resume.addActionListener(this);
		resume.setEnabled(false);
		tb.addSeparator();
		tb.add(clear = new JButton("Clear"));
		clear.addActionListener(this);
		clear.setToolTipText("Clear Text");
		tb.add(reload = new JButton("Reload"));
		reload.setToolTipText("Reload Text From File");
		reload.addActionListener(this);
		reload.setEnabled(false);
		tb.addSeparator();
		tb.add(find = new JButton("Find"));
		find.setToolTipText("Find Text In Textarea");
		find.addActionListener(this);
		
		tb.addSeparator();
		tb.add(Box.createHorizontalGlue());
		tb.add(new JSeparator(SwingConstants.VERTICAL));
		tb.addSeparator();

		tb.add(filter = new JButton("Filter"));
		filter.setToolTipText("Filter File Text");
		filter.addActionListener(this);
		tb.add(filterText = new TailHistoryComboBox());
		filterText.setToolTipText("Text Filter Pattern");
		filterText.setText("*");
		filterText.addActionListener(this);
		tb.add(include = new JComboBox());
		include.setToolTipText("Filter Works In- Or Excluding");
		include.addItem("Include");
		include.addItem("Exclude");

		add(tb, BorderLayout.NORTH);

		new TailFileDndListener(textarea, this);

		if (file != null)
			setFile(file);
	}


	private void interrupt()	{
		interrupted = true;
	}

	public void close()	{
		interrupt();
		filterText.save();
	}


	
	/** Set a new file into this watcher window. */
	public synchronized void setFile(File f)	{
		if (f.isFile() == false || f.canRead() == false)	{
			JOptionPane.showMessageDialog(
					ComponentUtil.getFrame(this),
					"Can not open "+f,
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		CursorUtil.setWaitCursor(this);
		
		this.file = f;
		frame.setTitle(f.getName());

		textarea.setText("");
		origText = null;

		new TextFileLoader(f, textarea.getDocument(), this, this, null, false).start();

		suspended = false;
		resume.setEnabled(suspended);
		suspend.setEnabled(!suspended);
		reload.setEnabled(true);
	}

	
	/** Implements LoadObserver to filter, set default cursor and start watcher thread. */
	public void setLoading(boolean loading)	{
		if (loading == false)	{
			if (isFiltered())	{
				filter();
			}
			
			textarea.setCaretPosition(textarea.getDocument().getLength());

			CursorUtil.resetWaitCursor(this);

			Thread t = new Thread(this);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		}
	}
	
	

	/** Implements Runnable to compare in background. */
	public void run()	{
		System.err.println("TailPanel, starting thread, waiting for GUI ...");
		while (ComponentUtil.getFrame(TailPanel.this).isShowing() == false)	{
			try	{ Thread.sleep(500); }	catch (InterruptedException e)	{}
		}
		System.err.println("    TailPanel, starting to watch "+this.file);
		
		insertString("");	// set caret to text end

		File myFile = this.file;
		this.date = myFile.lastModified();
		this.size = myFile.length();
		
		boolean restarting = false;
		
		while (!interrupted && !restarting)	{
			if (myFile != this.file)	{
				System.err.println("TailPanel, ending to watch as file has changed.");
				return;
			}

			try	{
				if (!suspended)	{
					synchronized(this)	{
						long filelen = file.length();
		
						if (filelen > size && isReloadAlways() == false)	{
							long filedate = file.lastModified();
							
							RandomAccessFile raf = null;
							try	{
								raf = new RandomAccessFile(file, "r");
								raf.seek(size);
								byte[] b = new byte[(int)(filelen - size)];
								raf.readFully(b);
								insertString(new String(b));
							}
							finally	{
								try	{ raf.close(); }	catch (Exception e)	{}
							}
							
							this.size = filelen;
							this.date = filedate;
						}
						else
						if (filelen != size || date != file.lastModified())	{
							restarting = true;
							restart();
						}
					}
				}
				
				if (!restarting)
					Thread.sleep(500);
			}
			catch (InterruptedException e)	{
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
		}
		
		System.err.println("TailPanel, ending to watch "+myFile);
	}
	

	private void insertString(String s)	{
		final String t = isFiltered() ? filter(s, getLineCount(origText)) : s;
		if (origText != null)
			origText = origText + s;
		
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				Document doc = textarea.getDocument();
				try	{
					doc.insertString(doc.getLength(), t, null);
					textarea.setCaretPosition(doc.getLength());
				}
				catch (BadLocationException e)	{
					e.printStackTrace();
				}
			}
		});
	}
	
	private void restart()	{
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				setFile(new File(TailPanel.this.file.getPath()));
			}
		});
	}



	/** Implements ActionListener to suspend, resume and open a new file. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == open)	{
			File f;
			if ((f = chooseFile()) != null)
				setFile(f);
		}
		else
		if (e.getSource() == clear)	{
			synchronized(this)	{
				textarea.setText("");
			}
		}
		else
		if (e.getSource() == reload)	{
			setFile(new File(this.file.getPath()));
		}
		else
		if (e.getSource() == filter || e.getSource() == filterText)	{
			filter();
		}
		else
		if (e.getSource() == find)	{
			textarea.find();
		}
		else	{
			if (e.getSource() == suspend)	{
				suspended = true;
			}
			else
			if (e.getSource() == resume)	{
				suspended = false;
			}
			resume.setEnabled(suspended);
			suspend.setEnabled(!suspended);
		}
	}


	private File chooseFile()	{
		File file = null;
		CursorUtil.setWaitCursor(this);
		try	{
			if (fileChooser == null)	{
				String dir = file != null ? file.getParent() : System.getProperty("user.home");
				fileChooser = new JFileChooser(dir);
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			}
	
			int ret = fileChooser.showOpenDialog(this);
			
			if (ret == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			}
			fileChooser.cancelSelection();	// reuse dialog. JFileChooser stores APPROVE_OPTION !
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
		return file;
	}


	private void filter()	{
		String s = filterText.getText();
		
		if (RegExpUtil.alwaysMatches(s, null))	{
			if (origText != null)
				textarea.setText(origText);
			
			origText = pattern = null;
		}
		else	{
			if (origText == null)
				origText = textarea.getText();
			
			pattern = s;
			textarea.setText(filter(origText, 0));
		}
	}
	

	private String filter(String text, int baseLineNr)	{
		RE expr;
		REMatch [] matches;

		try	{
			expr = new RE(pattern, RE.REG_MULTILINE, Syntaxes.getSyntax("PERL5"));
			matches = expr.getAllMatches(text);
		}
		catch (REException e)	{
			return text;
		}
		
		boolean isInclude = isInclude();
		
		if (matches == null || matches.length <= 0)	{
			return isInclude ? "" : text; 
		}

		// get matched text lines
		TextlineList lines = new TextlineList(text, baseLineNr);
		lines.setUseLineNumbers(false);
		
		for (int i = 0; i < matches.length; i++)	{
			int start = matches[i].getStartIndex();
			int end   = matches[i].getEndIndex();
			
			if (start < end)	{
				if (isInclude)
					lines.insertMatch(start, end);
				else
					lines.deleteMatch(start, end);
			}
		}
		
		return lines.toString();
	}
	

	private boolean isInclude()	{
		return include.getSelectedItem().equals("Include");
	}

	private boolean isReloadAlways()	{
		return include.getSelectedItem().equals(FULL_RELOAD);
	}

	private int getLineCount(String text)	{
		int cnt = 0;
		for (int i = 0; i < text.length(); i++)	{
			char c = text.charAt(i);
			if (c == '\n')
				cnt++;
		}
		return cnt;
	}


	private boolean isFiltered()	{
		return pattern != null;
	}
	
}




class TailHistoryComboBox extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile = null;

	public TailHistoryComboBox()	{
		super();
		manageTypedHistory(this, new File(HistConfig.dir()+"TailHistoryComboBox.list"));
	}

	// interface TypedHistoryHolder
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}
	public Vector getTypedHistory()	{
		return globalHist;
	}
	public File getHistoryFile()	{
		return globalFile;
	}

}