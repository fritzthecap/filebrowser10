package fri.gui.swing.concordance.textarea;

import java.awt.EventQueue;
import java.util.*;
import javax.swing.text.*;
import fri.util.concordance.*;
import fri.util.concordance.text.*;
import fri.gui.swing.document.DocumentUtil;
import fri.gui.swing.concordance.ConcordancePanel;
import fri.gui.swing.concordance.filter.FilterModel;

/**
	Connects a ConcordancePanel with a textarea and selects the
	textarea lines of the selected block in ConcordancePanel.
*/

class TextareaConcordancePanel extends ConcordancePanel
{
	private JTextComponent textarea;
	
	TextareaConcordancePanel(JTextComponent textarea)	{
		super(null);
		setTextArea(textarea);
	}

	/** Sets the textarea and searches newly in its contents for concordances. */
	public void setTextArea(JTextComponent textarea)	{
		this.textarea = textarea;
		refresh();
	}
	
	/** Refreshes concordances from current textarea. */
	public void refresh()	{
		List lines = DocumentUtil.documentToLineList(textarea.getDocument(), true);
		FilterModel filterModel = new FilterModel();
		Concordance concordance = new TextConcordance(lines, filterModel, filterModel.getBreakAfterCount(), filterModel.getMinimumLinesPerBlock());
		List blockedList = concordance.getBlockedResult();
		init(blockedList);
	}
	
	/** Selectes the text in textarea that the selected concordance block points to. */
	protected void setSelectedBlockOccurence(JTextComponent blockRenderer, BlockOccurence blockOccurence)	{
		super.setSelectedBlockOccurence(blockRenderer, blockOccurence);
		
		LineWrapper firstLine = (LineWrapper) blockOccurence.block.getPartObject(blockOccurence.occurence, 0);
		LineWrapper lastLine  = (LineWrapper) blockOccurence.block.getPartObject(blockOccurence.occurence, blockOccurence.block.getPartCount() - 1);
		Document doc = textarea.getDocument();
		Element root = doc.getDefaultRootElement();
		Element firstElem = root.getElement(firstLine.lineNumber);
		Element lastElem  = root.getElement(lastLine.lineNumber);
		final int start = firstElem.getStartOffset();
		final int end = lastElem.getEndOffset();
		EventQueue.invokeLater(new Runnable()	{	// invoke later, as another textarea just got focus
			public void run()	{
				textarea.requestFocus();	// request focus, else selection would not show
				textarea.select(start, end - 1);
			}
		});
	}



	/* Test main.
	public static void main(String[] args) {
		javax.swing.JFrame f = new javax.swing.JFrame("Textarea Concordance");
		javax.swing.JTextArea ta = new javax.swing.JTextArea("aaa\nbbb\nccc\naaa\nbbb\nddd\nccc");
		ta.setTabSize(2);
		final TextareaConcordancePanel conc = new TextareaConcordancePanel(ta);
		javax.swing.JButton btn = new javax.swing.JButton("Search Concordances");
		btn.addActionListener(new java.awt.event.ActionListener()	{
			public void actionPerformed(java.awt.event.ActionEvent e)	{
				conc.refresh();
			}
		});
		javax.swing.JSplitPane sp = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT, conc, new javax.swing.JScrollPane(ta));
		sp.setDividerLocation(0.3f);
		f.getContentPane().add(sp);
		f.getContentPane().add(btn, java.awt.BorderLayout.SOUTH);
		f.setSize(600, 400);
		f.setVisible(true);
		ta.requestFocus();
	}
	*/
}
