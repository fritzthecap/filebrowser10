package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
	A yes-no-cancel dialog that asks for applaying the current file filter.
	It offers the option to appear no more, returning last result in that case.
	It shows no more if yes or no was entered, cancel is no valid result.
	As this dialog contains static results, it *MUST* be modal!
*/

public class ApplyFilterDialog extends JDialog implements
	ActionListener
{
	private static boolean showNoMore = false;
	private static int result = JOptionPane.CANCEL_OPTION;
	private static Icon img = UIManager.getIcon("OptionPane.questionIcon");
	private JButton yes, no, can;
	private JCheckBox nomore;

	

	public ApplyFilterDialog(JDialog d, String msg)	{			
		super(d, "Apply Filter", true);
		init(msg, d);
	}
	
	public ApplyFilterDialog(JFrame f, String msg)	{			
		super(f, "Apply Filter", true);
		init(msg, f);
	}
	
	private void init(String msg, Component parent)	{
		//System.err.println("ApplyFilterDialog, getting parent "+parent.getClass());
		if (isValidShowNoMore())
			return;
		
		Container c = getContentPane();
		JLabel icon = new JLabel(img);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		c.add(icon, BorderLayout.WEST);
		JPanel p0 = new JPanel(new BorderLayout());
		JLabel l = new JLabel(msg);
		l.setVerticalAlignment(SwingConstants.CENTER);
		l.setHorizontalAlignment(SwingConstants.CENTER);
		l.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p0.add(l, BorderLayout.CENTER);
		p0.add(nomore = new JCheckBox("Do Not Ask Anymore"), BorderLayout.SOUTH);
		nomore.setHorizontalAlignment(SwingConstants.CENTER);
		c.add(p0, BorderLayout.CENTER);
		JPanel p = new JPanel();
		p.add(yes = new JButton("Yes"));
		yes.addActionListener(this);
		p.add(no = new JButton("No"));
		no.addActionListener(this);
		p.add(can = new JButton("Cancel"));
		can.addActionListener(this);
		c.add(p, BorderLayout.SOUTH);
		
		pack();
		setLocationRelativeTo(parent);

		addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				result = JOptionPane.CLOSED_OPTION;
			}
		});
	}
	
	
	public int showDialog()	{
		if (isValidShowNoMore())
			return result;
			
		setVisible(true);	// modal
			
		if (nomore.isSelected())
			showNoMore = true;
			
		return result;
	}
	
	
	/** implements ActionListener */
	public void actionPerformed(ActionEvent e)	{
		result =
				(e.getSource() == yes) ? JOptionPane.YES_OPTION :
				(e.getSource() == no) ? JOptionPane.NO_OPTION :
				JOptionPane.CANCEL_OPTION;
		dispose();
	}
	

	// Return true if yes or no was clicked before and the checkbox was selected.
	private static boolean isValidShowNoMore()	{
		return showNoMore && (result == JOptionPane.YES_OPTION || result == JOptionPane.NO_OPTION);
	}

}