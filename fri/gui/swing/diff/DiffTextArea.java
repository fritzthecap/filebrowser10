package fri.gui.swing.diff;

import java.io.File;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.plaf.basic.*;
import fri.gui.swing.text.*;
import fri.util.diff.*;

/**
	TextArea that renders BalancedLines from a Diff comparison,
	attaching colors to their background.
*/

public class DiffTextArea extends ComfortTextArea
{
	private BalancedLines lines;
	private FileObjectOpenDialog fileLoader;
	private JMenuItem load;
	

	public DiffTextArea(FileObjectOpenDialog fileLoader)	{
		this.fileLoader = fileLoader;
		
		setEditable(false);	// no editing here
		setLineWrap(false);	// no line breaking
		setToolTipText("");	// Bug? No tooltip without that ...
		
		setUI(new DiffTextAreaUI());	// a colored background and visible lines
	}

	/** Shows line numbers in tooltip. */
	public String getToolTipText(MouseEvent e)	{
		if (lines == null || lines.size() <= 0)
			return null;
			
		int i = e.getY() / computeRowHeigth();
		if (i >= lines.size())
			return null;

		BalancedLines.Line line = (BalancedLines.Line)lines.get(i);
		i = line.getLineNumber();
		if (i < 0)
			return null;
			
		return "Line "+(i + 1);
	}


	/** Overridden to add Menuitem "Load File" and "Find". */
	protected void addFind()	{
		popup.add(load = new JMenuItem("Open"));
		load.addActionListener(this);

		popup.addSeparator();

		super.addFind();
	}

	/** Overridden to NOT add a wrap-line menu item. */
	protected void addWrapLines()	{
	}

	/** Overrides keyPressed() to catch Ctl-O to load file. */	
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_O && e.isControlDown())
			fileLoader.openFile(this);
		else
			super.keyPressed(e);
	}

	/** Overrides actionPerformed() to catch load menuitem. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == load)	{
			fileLoader.openFile(this);
		}
		else	{
			super.actionPerformed(e);
		}
	}


	/** Load passed file into textarea. */
	public void load(File file)	{
		fileLoader.load(file, this);
	}
	

	
	/** Get the diff result lines. */
	public BalancedLines getDiffLines()	{
		return lines;
	}
	
	/** Set a new Document from diff result lines. */
	public void setDiffLines(BalancedLines lines)	{
		this.lines = lines;
		System.err.println("DiffTextArea got lines, length: "+lines.size());

		PlainDocument doc = new PlainDocument();
		
		for (int i = 0; i < lines.size(); i++)	{
			BalancedLines.Line line = (BalancedLines.Line)lines.get(i);
			try	{
				doc.insertString(doc.getLength(), line.getLine().toString(), null);
			}
			catch (BadLocationException ex)	{
				ex.printStackTrace();
			}
		}

		setDocument(doc);
	}

	/** Associate colors with changed states. */
	protected Color getColorForChangeFlag(String changeFlag)	{
		return DiffTextArea.getColorForChangeFlag(changeFlag, this);
	}

	/** Associate colors with changed states. */
	public static Color getColorForChangeFlag(String changeFlag, Component c)	{
		return changeFlag == null ? c.getBackground() :
			changeFlag.equals(DiffChangeFlags.CHANGED) ? DiffColors.CHANGED :
			changeFlag.equals(DiffChangeFlags.INSERTED) ? DiffColors.INSERTED :
			changeFlag.equals(DiffChangeFlags.DELETED) ? DiffColors.DELETED :
			Color.white;	// will not happen!
	}

	
	
	
	// UI for colored background
	private class DiffTextAreaUI extends BasicTextAreaUI
	{
		DiffTextAreaUI()	{
			super();
		}
		
		/** Set colors according to diff result on textarea background. */
		protected void paintBackground(Graphics g)	{
			// in any case clear background
			Rectangle r = g.getClipBounds();
			g.clearRect(r.x, r.y, r.width, r.height);

			if (lines == null || lines.size() <= 0)	// no diff info present
				return;
			
			int rowHeight = computeRowHeigth();
			
			if (rowHeight > 0)	{
				Rectangle clipRect = g.getClipBounds();
				int startIndex = Math.max(0, (clipRect.y / rowHeight) - 1);
				int endIndex = (clipRect.y + clipRect.height) / rowHeight + 1;
				int width = getComponent().getWidth();	// clipRect.width is not enough for repaint after tooltips
				
				for (int i = startIndex; i >= 0 && i < endIndex && i < lines.size(); i++)	{	
					BalancedLines.Line line = (BalancedLines.Line)lines.get(i);
					int upper = rowHeight * i;
					int lower = upper + rowHeight;
					
					Color c = getColorForChangeFlag(line.getChangeFlag());
					g.setColor(c);
					g.fillRect(0, upper, width, lower);	// colored background
					g.setColor(Color.lightGray);
					g.drawLine(0, lower - 1, width, lower - 1);	// visible line
				}
			}
			else	{
				System.err.println("ERROR: DiffTextAreaUI, can't get row heigth: "+rowHeight);
			}
		}	
	}
	
}