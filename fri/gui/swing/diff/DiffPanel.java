package fri.gui.swing.diff;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import fri.util.os.OS;
import fri.gui.CursorUtil;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.combo.WideComboBox;
import fri.gui.swing.scroll.SynchroneScrollingSplitPane;

/**
	A Panel that hosts two textareas that render the result of a Diff
	comparison.
	<p>
	This class is a WindowListener, as it must interrupt loading files
	when a parent frame is closing before loading has completed.
	Furthermore it compares file modification time with modification time
	when file was loaded, to recognize dirty files when frame is activated.<br>
	Please add as WindowListener to enclosing frame!
*/

public abstract class DiffPanel extends JPanel implements
	ActionListener,
	WindowListener,
	FileObjectOpenDialog
{
	protected static JFileChooser fileChooser1 = null, fileChooser2 = null;
	protected JTextField tf1, tf2;	// holding copyable names of files
	protected File file1, file2;	// left and right files
	protected long loadTime1, loadTime2;	// buffered load time for dirty file detection
	protected SynchroneScrollingSplitPane splitPane;
	protected JCheckBox ignoreSpaces, exceptLeadingSpaces;
	protected JButton toggleOrientation, recompare, toggleFiles;
	protected WideComboBox diffList;
	protected JLabel diffCount;
	private Point pos;	// remember view position when reloading files


	/** New empty window. */
	public DiffPanel()	{
		this(null, null);
	}

	/**
		New window from a two File objects.
		@param file1 left File object to render, can be null.
		@param file2 right File object to render, can be null.
	*/
	public DiffPanel(File file1, File file2)	{
		super(new BorderLayout());
		this.file1 = file1;
		this.file2 = file2;
	}
	

	/** Create both views. */
	protected abstract void buildViewers();	

	/** listening for drag&drop of files in textareas */
	protected abstract void addDndListeners();	

	/** Build the GUI. */
	protected void build()	{
		buildViewers();

		tf1 = new JTextField();	// showing file pathes
		tf1.setEditable(false);
		tf2 = new JTextField();
		tf2.setEditable(false);
		
		splitPane = new SynchroneScrollingSplitPane(getView1(), getView2());	// hosting textareas

		splitPane.getPanel1().add(tf1, BorderLayout.NORTH);
		splitPane.getPanel2().add(tf2, BorderLayout.NORTH);
				
		add(splitPane, BorderLayout.CENTER);
		
		// diff actions definition
		JToolBar tb = new JToolBar(SwingConstants.HORIZONTAL);
		tb.setFloatable(false);
		if (OS.isAboveJava13) tb.setRollover(true);
		
		buildToolbarRefresh(tb);
		buildToolbarOptions(tb);
		tb.add(Box.createHorizontalGlue());
		tb.add(new JSeparator(SwingConstants.VERTICAL));
		buildToolbarComboBox(tb);

		addDndListeners();
		
		add(tb, BorderLayout.NORTH);
	}
	


	protected void buildToolbarComboBox(JToolBar tb)	{
		diffCount = new JLabel("Diffs: ");
		tb.add(diffCount);

		diffList = new WideComboBox();
		diffList.setToolTipText("List Overview Of Differences");
		diffList.addActionListener(this);
		tb.add(diffList);
		diffList.setBorder(BorderFactory.createEtchedBorder());
		//diffList.setBackground(getView1().getBackground());
	}

	protected void buildToolbarRefresh(JToolBar tb)	{
		recompare = new JButton(Icons.get(Icons.refresh));
		recompare.setToolTipText("Re-Compare Files");
		recompare.addActionListener(this);
		recompare.setEnabled(false);
		tb.add(recompare);
	}
	
	protected void buildToolbarOptions(JToolBar tb)	{
		tb.addSeparator();
		
		ignoreSpaces = new JCheckBox("Ignore Spaces", true);
		ignoreSpaces.setToolTipText("Ignore All Spaces And Newlines When Comparing");
		ignoreSpaces.addActionListener(this);	// to set enabled exceptLeading
		tb.add(ignoreSpaces);
		
		exceptLeadingSpaces = new JCheckBox("Except Leading");
		exceptLeadingSpaces.setToolTipText("Do Not Ignore Leading Spaces");
		tb.add(exceptLeadingSpaces);

		buildToolbarToggleLeftRight(tb);
		
		toggleOrientation = new JButton(Icons.get(Icons.toggleSplit));
		toggleOrientation.setToolTipText("Toggle Between Horizontal And Vertical View Split");
		toggleOrientation.addActionListener(this);
		tb.add(toggleOrientation);
	}
	
	protected void buildToolbarToggleLeftRight(JToolBar tb)	{
		toggleFiles = new JButton(Icons.get(Icons.toggleSides));
		toggleFiles.setToolTipText("Toggle Left And Right Files");
		toggleFiles.addActionListener(this);
		tb.add(toggleFiles);
	}

	/** Returns the popupmenus from views for customization. */
	public abstract JPopupMenu [] getPopups();

	protected abstract boolean checkValidFile(File file);
	

	/** Set new pair of files into view. */
	public void setFiles(File file1, File file2)	{
		if (file1 != null && file2 != null && file1.equals(file2))	{
			JOptionPane.showMessageDialog(
				this,
				"Left file is the same as right file!",
				"Error",
				JOptionPane.ERROR_MESSAGE);
		}
		else	{
			storeViewPosition();
			
			if (checkValidFile(file1))
				setFile(file1, true);
			if (checkValidFile(file2))
				setFile(file2, false);
				
			recompare.setEnabled(file1 != null && file2 != null);
		}
	}

	/** Set a file into one view. @param isLeft true if first view. */
	protected abstract void setFile(File file, boolean isLeft);

	/** Compare the two contens when toggling their positions or loader ends. */
	protected abstract void compare();

	
	private void storeViewPosition()	{
		if (getView1() != null)	{
			pos = ((JViewport)getView1().getParent()).getViewPosition();
		}
	}
	
	protected void restoreViewPosition()	{
		if (getView1() != null && pos != null)	{
			// must invoke this later as document seems not to have been filled in!
			EventQueue.invokeLater(new Runnable()	{
				public void run()	{
					((JViewport) getView1().getParent()).setViewPosition(pos);
					pos = null;
				}
			});
		}
	}


	
	
	protected abstract class ComboDiffItem
	{
		protected String changeFlag;
		protected int visibleLine;
		
		ComboDiffItem(String changeFlag, int visibleLine)	{
			this.changeFlag = changeFlag;
			this.visibleLine = visibleLine;
		}
		
		public abstract void showItem();
		
		public abstract String toString();
	}
	


	
	/** Implements FileObjectOpenDialog: Called by one of the views when opening a file. */
	public void openFile(Component ta)	{
		CursorUtil.setWaitCursor(this);
		try	{
			boolean isLeft = (ta == getView1());
			JFileChooser fc = isLeft ? fileChooser1 : fileChooser2;
			
			if (fc == null)	{
				String dir = System.getProperty("user.home");
				if (isLeft && file1 != null)
					dir = file1.getParent();
				else
				if (!isLeft && file2 != null)
					dir = file2.getParent();
	
				fc = new JFileChooser(dir);
	
				if (isLeft)
					fileChooser1 = fc;
				else
					fileChooser2 = fc;
	
				fc.setMultiSelectionEnabled(false);
			}
			
			fc.setFileSelectionMode(getFileDialogMode());
	
			int ret = fc.showOpenDialog(this);
			
			if (ret == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				if (file != null)	{
					load(file, ta);
				}
			}
			fc.cancelSelection();	// reuse dialog. JFileChooser stores APPROVE_OPTION !
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}


	protected abstract int getFileDialogMode();
	

	/** Load one file, compare after loading. */
	public void load(File file, Component view)	{
		boolean isLeft = (view == getView1());
		setFiles(isLeft ? file : file1, isLeft ? file2 : file);
	}


	

	/** Get view component for ComboDiffItem to scroll to its item. */
	protected abstract JComponent getView1();

	/** Get view component for ComboDiffItem to scroll to its item. */
	protected abstract JComponent getView2();



	/** Implements ActionListener. */
	public void actionPerformed(ActionEvent e)	{
		System.err.println("DiffPanel.actionPerformed on "+e.getSource().getClass());
		if (e.getSource() == toggleOrientation)	{
			splitPane.toggleOrientation();
		}
		else
		if (e.getSource() == recompare)	{
			setFiles(file1, file2);
		}
		else
		if (e.getSource() == ignoreSpaces)	{
			exceptLeadingSpaces.setEnabled(ignoreSpaces.isSelected());
		}
		else
		if (e.getSource() == diffList)	{
			Object o = diffList.getSelectedItem();
			if (o instanceof ComboDiffItem)	{
				((ComboDiffItem)o).showItem();
			}
		}
		else
		if (e.getSource() == toggleFiles && file1 != null && file2 != null)	{
			String path = tf1.getText();
			tf1.setText(tf2.getText());
			tf2.setText(path);
			
			File file = file1; file1 = file2; file2 = file;
			long loadTime = loadTime1; loadTime1 = loadTime2; loadTime2 = loadTime;
			JFileChooser fileChooser = fileChooser1; fileChooser1 = fileChooser2; fileChooser2 = fileChooser;
			
			toggleContents();

			compare();
		}
	}


	/** The two views are exchanged, swap private variable contents. */
	protected void toggleContents()	{
	}

	
	
	public void windowOpened(WindowEvent e)	{}
	public void windowClosed(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)	{}
	public void windowDeiconified(WindowEvent e)	{}
	public void windowDeactivated(WindowEvent e)	{}
	public void windowClosing(WindowEvent e)	{}
	
	/** Implements WindowListener to check for dirty files. */
	public void windowActivated(WindowEvent e)	{
		EventQueue.invokeLater(new Runnable()	{	// deadlocks the GUI when not invoked later!
			public void run()	{
				if (checkDirty(true) == false)	// this call would reload both files
					checkDirty(false);	// do load second file only if first was not dirty
			}
		});
	}


	private boolean isDirty;
	
	private boolean checkDirty(boolean isLeft)	{
		File file = isLeft ? file1 : file2;
		long loadTime = isLeft ? loadTime1 : loadTime2;
		isDirty = false;
		
		if (file != null && file.lastModified() != loadTime)	{
			isDirty = true;
			
			int ret = JOptionPane.showConfirmDialog(
				DiffPanel.this,
				"\""+file.getName()+"\" in \""+file.getParent()+"\"\nhas changed. Do you want to reload it?",
				"File Has Changed",
				JOptionPane.YES_NO_OPTION);
	
			if (ret == JOptionPane.YES_OPTION)	{
				setFiles(file1, file2);
			}
			else	{
				isDirty = false;	// continue checking second file
			
				if (isLeft)
					loadTime1 = file1.lastModified();
				else
					loadTime2 = file2.lastModified();
			}
		}
		
		return isDirty;
	}

}