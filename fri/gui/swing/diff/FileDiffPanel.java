package fri.gui.swing.diff;

import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.text.*;
import fri.util.diff.*;
import fri.gui.CursorUtil;
import fri.gui.swing.fileloader.*;
import fri.gui.swing.document.DocumentUtil;

/**
	A Panel that hosts two textareas that render the result of a Diff comparison.
*/

public class FileDiffPanel extends DiffPanel
{
	private DiffTextArea ta1, ta2;	// holding file texts
	private PlainDocument doc1, doc2;	// temporary Documents for FileLoader
	private FileLoader loader1, loader2;	// temporary file loaders


	/** New empty window. */
	public FileDiffPanel()	{
		this(null, null);
	}

	/**
		New window from a two File objects.
		@param file1 left File object to render, can be null.
		@param file2 right File object to render, can be null.
	*/
	public FileDiffPanel(File file1, File file2)	{
		super(file1, file2);
		build();
		setFiles(file1, file2);
	}


	/** Create a color-enabled JEditorPane for the diff texts. */
	protected DiffTextArea createTextArea()	{
		return new DiffTextArea(this);
	}

	/** Create both textareas */
	protected void buildViewers()	{	
		ta1 = createTextArea();	// showing file contents
		ta2 = createTextArea();
	}

	/** listening for drag&drop of files in textareas */
	protected void addDndListeners()	{		
		new FileDndListener(this, getView1());
		new FileDndListener(this, getView2());
	}


	/** Argument checking: accept only non-null normal files. */
	protected boolean checkValidFile(File file)	{
		if (file == null || file.isFile() == false)	{
			if (file != null)	{
				JOptionPane.showMessageDialog(
						this,
						"\""+file+"\" does not exist or is not normal file!",
						"File Error",
						JOptionPane.ERROR_MESSAGE);
			}
			return false;
		}
		return true;
	}


	protected void setFile(File file, boolean isLeft)	{
		JTextArea ta;
		JTextField tf;
		
		if (isLeft)	{
			file1 = file;
			loadTime1 = file.lastModified();
			ta = ta1;
			tf = tf1;
		}
		else	{
			file2 = file;
			loadTime2 = file.lastModified();
			ta = ta2;
			tf = tf2;
		}

		ta.setText("");
		tf.setText(file.getAbsolutePath());

		CursorUtil.setWaitCursor(this);

		FileLoader loader = new TextFileLoader(
				isLeft ? file1 : file2,
				isLeft ? (doc1 = new PlainDocument()) : (doc2 = new PlainDocument()),
				isLeft ? splitPane.getPanel1() : splitPane.getPanel2(),
				new TextLoadObserver(isLeft),
				null,
				null,
				true,
				true);	// encoding
				
		if (isLeft)
			loader1 = loader;
		else
			loader2 = loader;

		loader.start();
	}




	/** Observer to compare files when both are loaded. */
	protected class TextLoadObserver implements LoadObserver
	{
		private boolean isLeft;
		
		TextLoadObserver(boolean isLeft)	{
			this.isLeft = isLeft;
		}
		
		public void setLoading(boolean loading)	{
			if (loading == false)	{	// finished loading this one file
				compareIfBothLoaded(isLeft);
			}
		}
	}



	private synchronized void compareIfBothLoaded(boolean isLeft)	{
		System.err.println("FileDiffPanel, comparing files "+file1+", "+file2);
		try	{
			if (isLeft)
				loader1 = null;
			else
				loader2 = null;
	
			if (loader1 == null && loader2 == null)	{	// both are finished
				compare();	// compare and render diffs
				
				restoreViewPosition();
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}

	protected void compare()	{
		// when loading one after another, create empty defaults
		if (doc1 == null)
			doc1 = new PlainDocument();
		if (doc2 == null)
			doc2 = new PlainDocument();
		
		// create diff-able arrays from Document read by FileLoader
		Object [] left = DocumentUtil.documentToStringArray(doc1);
		Object [] right = DocumentUtil.documentToStringArray(doc2);
		
		if (ignoreSpaces.isSelected())	{
			left = createSpaceFilters(left);
			right = createSpaceFilters(right);
		}
		
		BalancedLines [] leftRight = BalancedLines.createBalancedLines(left, right);
		
		if (ignoreSpaces.isSelected())	{
			filterNewlines(leftRight);
		}

		// set results into overview combobox
		fillDiffListComboBox(leftRight);

		// set results into textareas
		ta1.setDiffLines(leftRight[0]);
		ta2.setDiffLines(leftRight[1]);
	}


	/** When ignoring spaces, a special SpaceIgnoringString is used for diff comparison. */
	private Object [] createSpaceFilters(Object [] lines)	{
		Object [] oarr = new Object[lines.length];
		for (int i = 0; i < lines.length; i++)	{
			oarr[i] = new SpaceIgnoringString(lines[i].toString(), exceptLeadingSpaces.isSelected());
		}
		return oarr;
	}
	
	// return true if DELETED or INSERTED newline that is not preceeded
	// or followed by a change other than INSERTED/DELETED newline
	private boolean isInsertedOrDeletedNewline(BalancedLines.Line bl, String flag)	{
		return flag != null && bl.getLine().toString().equals("\n") &&
				(flag.equals(DiffChangeFlags.DELETED) || flag.equals(DiffChangeFlags.INSERTED));
	}
	
	/** When ignoring spaces, diffs that contain only newlines are eliminated. */
	private void filterNewlines(BalancedLines [] leftRight)	{
		for (int i = 0; i < leftRight.length; i++)	{
			BalancedLines b = leftRight[i];
			boolean isolated = true;	// start condition
			
			// eliminate DELETED or INSERTED newlines that are not preceeded
			// or followed by a change other than INSERTED/DELETED newline
			for (int j = 0; j < b.size(); j++)	{
				BalancedLines.Line bl = (BalancedLines.Line)b.get(j);
				String chng = bl.getChangeFlag();
				
				if (isInsertedOrDeletedNewline(bl, chng))	{
					if (isolated)
						bl.setChangeFlag(null);
				}
				else	{
					isolated = (chng == null);
				}
			}
		}
	}



	private void fillDiffListComboBox(BalancedLines [] leftRight)	{
		diffList.removeActionListener(this);
		diffList.removeAllItems();
		
		int size = Math.min(leftRight[0].size(), leftRight[1].size());
		FileComboDiffItem currItem = null;
		int lastValidLine1 = -1, lastValidLine2 = -1;
		
		for (int i = 0; i < size; i++)	{
			BalancedLines.Line line1 = (BalancedLines.Line)leftRight[0].get(i);
			BalancedLines.Line line2 = (BalancedLines.Line)leftRight[1].get(i);
			//System.err.println("line1="+line1+", line2="+line2);
			
			String chng1 = line1.getChangeFlag();
			String chng2 = line2.getChangeFlag();
			
			if (chng1 != null || chng2 != null)	{
				String flag = chng1 != null ? chng1 : chng2;	// if CHANGED, bot are equal
				
				// report continous changes only once
				if (currItem == null)	{
					currItem = new FileComboDiffItem(i, flag,
							line1.getLineNumber() >= 0 ? line1.getLineNumber() : lastValidLine1,
							line2.getLineNumber() >= 0 ? line2.getLineNumber() : lastValidLine2);
					diffList.addItem(currItem);
				}
				else	{	// continue item
					currItem.addLine();
				}
			}
			else	{
				currItem = null;
			}

			lastValidLine1 = line1.getLineNumber() >= 0 ? line1.getLineNumber() : lastValidLine1;
			lastValidLine2 = line2.getLineNumber() >= 0 ? line2.getLineNumber() : lastValidLine2;
		}
		
		diffCount.setText(""+diffList.getItemCount()+" Diffs: ");
		diffList.addActionListener(this);
		diffList.takePopupSize();
	}


	
	protected class FileComboDiffItem extends ComboDiffItem
	{
		private int realLine1, realLine2;
		private int range;
		
		FileComboDiffItem(int visibleLine, String changeFlag, int realLine1, int realLine2)	{
			super(changeFlag, visibleLine);
			this.realLine1 = realLine1 + 1;
			this.realLine2 = realLine2 + 1;
			this.changeFlag = changeFlag;
		}
		
		public void addLine()	{
			range++;
		}
		
		public String toString()	{
			String lineRange1 = range <= 0 || changeFlag.equals(DiffChangeFlags.INSERTED) ? ""+realLine1 : ""+realLine1+"-"+(realLine1 + range);
			String lineRange2 = range <= 0 || changeFlag.equals(DiffChangeFlags.DELETED) ? ""+realLine2 : ""+realLine2+"-"+(realLine2 + range);
			String label = 
				(changeFlag.equals(DiffChangeFlags.DELETED) ? "Deleted " :
					changeFlag.equals(DiffChangeFlags.INSERTED) ? "Inserted After " :
					"Changed ")+
				"Left "+lineRange1+", "+
				(changeFlag.equals(DiffChangeFlags.DELETED) ? "After " : "")+
				"Right "+lineRange2;
			return label;
		} 
		
		public void showItem()	{
			JTextComponent ta = changeFlag.equals(DiffChangeFlags.DELETED) ? ta2 : ta1;
			Document doc = ta.getDocument();
			Element root = doc.getDefaultRootElement();
			Element line1 = root.getElement(visibleLine);
			Element line2 = root.getElement(visibleLine + range);
			int start = line1.getStartOffset();
			int end = line2.getEndOffset() - 1;
			ta.setCaretPosition(end);
			ta.moveCaretPosition(start);
			System.err.println("selecting text from "+start+" to "+end);
		}
		
	}
	
	
	protected JComponent getView1()	{
		return ta1;
	}
	
	protected JComponent getView2()	{
		return ta2;
	}


	public JPopupMenu [] getPopups()	{
		return new JPopupMenu[] { ta1.getPopupMenu(), ta2.getPopupMenu() };
	}

	protected int getFileDialogMode()	{
		return JFileChooser.FILES_ONLY;
	}


	
	/** The two views are exchanged, swap private variable contents. */
	protected void toggleContents()	{
		PlainDocument doc = doc1;
		doc1 = doc2;
		doc2 = doc;
	}

	
	/** Implements WindowListener to interrupt loading files. */
	public synchronized void windowClosing(WindowEvent e)	{
		if (loader1 != null)
			loader1.interrupt();

		if (loader2 != null)
			loader2.interrupt();
	}
	


	// test main
	public static void main(String [] args)	{
		File f1 = new File("fri/util/diff/aaa");
		File f2 = new File("fri/util/diff/bbb");
		if (args.length == 2)	{
			f1 = new File(args[0]);
			f2 = new File(args[1]);
		}
		DiffPanel p = new FileDiffPanel(f1, f2);
		JFrame f = new JFrame("Differences");
		f.addWindowListener(p);
		f.getContentPane().add(p);
		f.setSize(600, 400);
		f.setVisible(true);
	}
	
}