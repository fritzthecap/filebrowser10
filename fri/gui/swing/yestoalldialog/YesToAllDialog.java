package fri.gui.swing.yestoalldialog;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import fri.gui.swing.ComponentUtil;

/**
	Ziel: Wiederverwendbarer Dialog, der nach einmaligem Ausloesen des
		Buttons "yes to all" ueber die show() Methode nicht mehr
		am Schirm angezeigt wird, sondern immer nur "YES" liefert.
		Dadurch ist es moeglich, in Kopier-Schleifen die Ueberschreiben-
		Abfrage zu uebergehen, ohne ein Extra Flag benutzen zu muessen.
*/

public class YesToAllDialog implements
	KeyListener,
	ActionListener
{
	/** value to request result after dialog finished */
	public static final int YES = 1;
	public static final int NO = 0;
	public static final int CANCEL = -1;
	private static final int YES_TO_ALL = 2;
	private static final int NO_TO_ALL = 3;
	private static Icon img = UIManager.getIcon("OptionPane.questionIcon");
	/** after dialog finished, this field is set to user action */
	protected int result = -2;
	protected JDialog delegate;
	private JButton yes, no, yestoall, notoall, cancel;
	protected Component parent;


	/**
		Ask to overwrite an object with another. Once pressed "Yes To All",
		the dialog shows no more and always returns true.
	*/
	public YesToAllDialog(Component parent)	{
		//System.err.println("YesToAll constructor "+currentParent.getName());
		parent = ComponentUtil.getWindowForComponent(parent);
		if (parent instanceof JFrame)
			delegate = new JDialog((JFrame)parent, "Transaction Conflict", true);
		else
			delegate = new JDialog((JDialog)parent, "Transaction Conflict", true);

		this.parent = parent;

		Container c = delegate.getContentPane();
		c.setLayout(new BorderLayout());

		JLabel icon = new JLabel(img);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		c.add(icon, BorderLayout.WEST);
		
		JPanel p0 = new JPanel();
		yes = new JButton("Yes");
		yes.addActionListener(this);
		p0.add(yes);
		yestoall = new JButton("Yes To All");
		yestoall.addActionListener(this);
		p0.add(yestoall);
		no = new JButton("No");
		no.addActionListener(this);
		p0.add(no);
		notoall = new JButton("No To All");
		notoall.addActionListener(this);
		p0.add(notoall);
		cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		p0.add(cancel);

		c.add(p0, BorderLayout.SOUTH);

		delegate.getRootPane().setDefaultButton(yes);

		yes.addKeyListener(this);

		delegate.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	}


	public int show()
		throws UserCancelException
	{
		if (result == YES_TO_ALL)
			return YES;
		if (result == NO_TO_ALL)
			return NO;
		if (result == CANCEL)
			throw new UserCancelException("user canceled");
			
		delegate.pack();
		delegate.setLocationRelativeTo(parent);
		delegate.show();
		
		if (result == CANCEL)
			throw new UserCancelException("user canceled");
		if (result == YES_TO_ALL)
			return YES;
		if (result == NO_TO_ALL)
			return NO;
			
		return result;
	}


	public boolean isOverwriteAll()	{
		return result == YES_TO_ALL;
	}


	/** implementing KeyListener */
	public void actionPerformed(ActionEvent e)	{
		action(e);
	}
	
	private void action(EventObject e)	{
		if (e.getSource() == yes)
			result = YES;
		else
		if (e.getSource() == yestoall)
			result = YES_TO_ALL;
		else
		if (e.getSource() == no)
			result = NO;
		else
		if (e.getSource() == notoall)
			result = NO_TO_ALL;
		else
		if (e.getSource() == cancel)
			result = CANCEL;
		else
			result = 0;
			
		//System.err.println("YesToAllDialog action = "+result);
		delegate.dispose();
	}

	/** implementing KeyListener */
	public void keyPressed(KeyEvent e)	{
		switch(e.getKeyCode())
		{
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_ENTER:
				//action(e);
				action(new KeyEvent(yes, e.getID(), e.getWhen(), e.getModifiers(), e.getKeyCode(), e.getKeyChar()));
				break;
			case KeyEvent.VK_ESCAPE:
				result = CANCEL;
				delegate.dispose();
				break;
		}
	}
	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

}