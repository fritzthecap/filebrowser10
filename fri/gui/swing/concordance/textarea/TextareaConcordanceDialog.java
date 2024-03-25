package fri.gui.swing.concordance.textarea;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import fri.gui.CursorUtil;
import fri.gui.swing.dialog.FrameServiceDialog;
import fri.gui.swing.concordance.filter.*;

/**
	Dialog that can be used as concordance search within a textearea.
*/

public class TextareaConcordanceDialog extends FrameServiceDialog implements
	ActionListener
{
	private TextareaConcordancePanel concordancePanel;
	private JButton refresh, filter;
	
	public TextareaConcordanceDialog(Frame parentWindow, JTextComponent textarea)	{
		super(parentWindow);
		
		setTitle("Line Concordance Search");
		CursorUtil.setWaitCursor(parentWindow);
		try	{
			build(textarea);
			pack();	// packs the dialog according to last geometry state
			setFreeViewLocation();	// places dialog where is most place
			setVisible(true);
		}
		finally	{
			CursorUtil.resetWaitCursor(parentWindow);
		}
	}

	private void build(JTextComponent textarea)	{
		Container c = getContentPane();

		concordancePanel = new TextareaConcordancePanel(textarea);
		c.add(concordancePanel, BorderLayout.CENTER);
		
		JPanel p = new JPanel();
		refresh = new JButton("Refresh");
		refresh.setToolTipText("Search For Concordances In Textarea");
		refresh.addActionListener(this);
		p.add(refresh);
		filter = new JButton("Configure");
		filter.setToolTipText("Change Text Filter Settings");
		filter.addActionListener(this);
		p.add(filter);

		c.add(p, BorderLayout.SOUTH);

		refresh.requestFocus();	// else first textarea would not be selected on focusGained()
	}

	public void init(JTextComponent textarea)	{
		initParent(textarea);

		setWaitCursor(true);
		try	{
			concordancePanel.setTextArea(textarea);
		}
		finally	{
			setWaitCursor(false);
		}
	}

	public void actionPerformed(ActionEvent e)	{
		setWaitCursor(true);
		try	{
			if (e.getSource() == refresh)	{
				concordancePanel.refresh();
			}
			else
			if (e.getSource() == filter)	{
				new TextlineFilterMvcBuilder().showAsDialog(getDialog());
				// Stores the filter model. As the filter is constructed with new in TextareaConcordancePanel, it will be up-to-date
				concordancePanel.refresh();
			}
		}
		finally	{
			setWaitCursor(false);
		}
	}

}
