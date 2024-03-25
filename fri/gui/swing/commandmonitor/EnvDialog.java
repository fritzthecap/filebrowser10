package fri.gui.swing.commandmonitor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import fri.util.text.CmdLineSubstitution;

/**
	Edit passed environment variables in a textarea.
*/

public class EnvDialog extends JDialog implements
	ActionListener
{
	private static final String title = "Command Variables";
	private boolean okPressed = false;
	private JButton ok, can;
	private JTextArea ta;


	public EnvDialog(JFrame f, String [] env)	{
		super(f, title, true);	// modal, weil env uebernommen wird
		init(env);
	}

	private void init(String [] env)	{
		ok = new JButton("Ok");
		ok.addActionListener(this);
		can = new JButton("Cancel");
		can.addActionListener(this);
		JPanel p1 = new JPanel();
		p1.add(ok);
		p1.add(can);
		JScrollPane sp = new JScrollPane(ta = new JTextArea());
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(new JLabel("Name = Value"), BorderLayout.NORTH);
		c.add(sp, BorderLayout.CENTER);
		c.add(p1, BorderLayout.SOUTH);

		ta.setText(CmdLineSubstitution.arrayToString(env));

		addWindowListener (new WindowAdapter () {
			public void windowClosing(WindowEvent ev) {
				dispose();
			}
		});
	}


	/** Return the edited environment. */
	public String [] getEnv()	{
		return CmdLineSubstitution.textToArray(ta.getText());
	}
	
	/** Return true if OK was pressed. */
	public boolean getOK()	{
		return okPressed;
	}
	
	
	// interface ActionListener
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == ok)	{
			okPressed = true;
			dispose();
		}
		else
		if (e.getSource() == can)	{
			dispose();
		}
		else	{
			System.err.println("actionPerformed "+e.getSource());
		}
	}

}