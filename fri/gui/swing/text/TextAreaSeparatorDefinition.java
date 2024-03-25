package fri.gui.swing.text;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import fri.util.os.OS;

/**
	Utility class to define additional separator characters for a JTextArea.
	It implements "." and "-" as additional default word separators by
	substituting several actions from JTextArea ActionMap and installing a
	DefaultCaret subclass (double click subclassing).
	<p>
	Attaching this class to textarea shold be the last call after configuring
	the textarea, as this class substitutes the action map and the caret.
	
	@author Fritz Ritzberger
*/

public class TextAreaSeparatorDefinition
{
	private Action nextWordAction;
	private Action previousWordAction;
	private Action selectionNextWordAction;
	private Action selectionPreviousWordAction;
	private Action beginWordAction;
	private Action endWordAction;
	private Action selectionBeginWordAction;
	private Action selectionEndWordAction;
	private char [] separators = new char [] { '.', '-', '\n' };	// some of these are neccessary at line end


	/** Attach a default (additional) separator definition to passed JTextArea: '.', '-', '\n' */
	public TextAreaSeparatorDefinition(JTextArea textarea)	{
		this(textarea, null);
	}
	
	/** Attach a given (additional) separator definition to passed JTextArea. */
	public TextAreaSeparatorDefinition(JTextArea textarea, char [] separators)	{
		if (nextWordAction == null)	{
			nextWordAction = textarea.getActionMap().get(DefaultEditorKit.nextWordAction);
			previousWordAction = textarea.getActionMap().get(DefaultEditorKit.previousWordAction);
			selectionNextWordAction = textarea.getActionMap().get(DefaultEditorKit.selectionNextWordAction);
			selectionPreviousWordAction = textarea.getActionMap().get(DefaultEditorKit.selectionPreviousWordAction);
			beginWordAction = textarea.getActionMap().get(DefaultEditorKit.beginWordAction);
			endWordAction = textarea.getActionMap().get(DefaultEditorKit.endWordAction);
			selectionBeginWordAction = textarea.getActionMap().get(DefaultEditorKit.selectionBeginWordAction);
			selectionEndWordAction = textarea.getActionMap().get(DefaultEditorKit.selectionEndWordAction);
		}
		
		if (separators != null)
			this.separators = separators;

		initWordJumpAction(textarea);	// for Ctl-Left and -Right selecting word
		initCaret(textarea);	// for double clicks selecting word
	}


	// Ctl-Left and -Right must respect "." as separator
	private void initWordJumpAction(JTextArea textarea)	{
		textarea.getActionMap().put(DefaultEditorKit.nextWordAction, new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				gotoWord(true, ae, false, false);
			}
		});
		textarea.getActionMap().put(DefaultEditorKit.previousWordAction, new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				gotoWord(false, ae, false, false);
			}
		});
		textarea.getActionMap().put(DefaultEditorKit.selectionNextWordAction, new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				gotoWord(true, ae, true, false);
			}
		});
		textarea.getActionMap().put(DefaultEditorKit.selectionPreviousWordAction, new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				gotoWord(false, ae, true, false);
			}
		});
	}


	private void gotoWord(boolean next, ActionEvent ae, boolean select, boolean doWordStartAndEnd)	{
		JTextComponent c = (JTextComponent)ae.getSource();
		int offs = c.getCaretPosition();
		
		if (doWordStartAndEnd && next == false)
			offs++;
		
		// get the offset the EditorKit would take for the next/previous word
		int wordOffs = -1;
		try	{
			wordOffs = doWordStartAndEnd
					? (next ? Utilities.getWordEnd(c, offs) : Utilities.getWordStart(c, offs))
					: (next ? Utilities.getNextWord(c, offs) : Utilities.getPreviousWord(c, offs));
		}
		catch (BadLocationException ex)	{
		}
		
		// test pattern like "[A-Za-z_]" !!!
			
		// check if some of our separators lie within that range
		Element line = Utilities.getParagraphElement(c, offs);
		
		if (line != null && wordOffs >= 0)	{
			Document doc = line.getDocument();
			int lineStart = line.getStartOffset();
			int lineEnd = Math.min(line.getEndOffset(), doc.getLength());
			
			try	{
				int wordOffs2 = -1, idx = -1;
				String s = doc.getText(next ? offs : lineStart, next ? lineEnd - offs : offs - lineStart);
				
				if (next)	{
					idx = getSeparatorIndex(s, false);	// jump to left of separator when going to next
					wordOffs2 = offs + Math.max(idx, 1);	// minimum move is 1
				}
				else	{
					idx = getSeparatorIndex(s, true);
					if (idx >= 0)
						idx += 1;	// jump to right of separator when going to previous
					wordOffs2 = offs - Math.max(s.length() - idx, 1);	// minimum move is 1
				}
	
				//System.err.println("line fragment >"+s+"<, offs "+offs+", wordOffs "+wordOffs+", wordOffs2 "+wordOffs2+", idx "+idx+", go to right "+next);
				if (idx >= 0 && wordOffs2 >= 0 &&
						(wordOffs2 == -1 ||
						 next && (wordOffs2 < wordOffs || doWordStartAndEnd && wordOffs2 <= wordOffs + 1) ||
						!next && (wordOffs2 > wordOffs || doWordStartAndEnd && wordOffs2 >= wordOffs - 1)))
				{
					//System.err.println(" ... Going to wordOffs2 "+wordOffs2+", select is "+select);
					if (select)
						c.moveCaretPosition(wordOffs2);
					else
						c.setCaretPosition(wordOffs2);
	
					return;
				}
			}
			catch (BadLocationException ex)	{
				//System.err.println("BadLocationException on separator detection: "+ex);
			}
		}
		
		defaultAction(ae, next, select, doWordStartAndEnd);
	}


	private int getSeparatorIndex(String s, boolean lastIndex)	{
		int best = lastIndex ? -1 : Integer.MAX_VALUE;
		for (int i = 0; i < separators.length; i++)	{
			int idx = lastIndex ? s.lastIndexOf(separators[i]) : s.indexOf(separators[i]);
			if (idx >= 0 && (lastIndex && best < idx || !lastIndex && best > idx))
				best = idx;
		}
		return best == Integer.MAX_VALUE ? -1 : best;
	}
	
	private void defaultAction(ActionEvent ae, boolean next, boolean select, boolean doWordStartAndEnd)	{
		if (doWordStartAndEnd)
			if (next)
				if (select)
					selectionEndWordAction.actionPerformed(ae);
				else
					endWordAction.actionPerformed(ae);
			else
				if (select)
					selectionBeginWordAction.actionPerformed(ae);
				else
					beginWordAction.actionPerformed(ae);
		else
			if (next)
				if (select)
					selectionNextWordAction.actionPerformed(ae);
				else
					nextWordAction.actionPerformed(ae);
			else
				if (select)
					selectionPreviousWordAction.actionPerformed(ae);
				else
					previousWordAction.actionPerformed(ae);
	}


	// double click word selection must respect "." as separator
	private void initCaret(JTextArea textarea)	{
		textarea.setCaret(new DefaultCaret()	{
			public void mouseClicked(MouseEvent e) {
				if (OS.isAboveJava14 == false && e.isConsumed() == false && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
					ActionEvent ae = new ActionEvent(
						e.getComponent(),
						ActionEvent.ACTION_PERFORMED,
						null,
						e.getModifiers());
					gotoWord(false, ae, false, true);
					gotoWord(true, ae, true, true);
				}
				else	{
					try	{
						super.mouseClicked(e);
					}
					catch (Exception ex)	{
						System.err.println("WARNING: DefaultCaret threw exception: "+ex.toString());
					}
				}
			}
		});
	}

}
