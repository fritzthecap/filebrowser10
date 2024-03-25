package fri.gui.swing.filebrowser;

import java.awt.*;
import javax.swing.*;
import fri.gui.awt.geometrymanager.GeometryManager;

public class Monitor extends JDialog
{
	public Monitor(JFrame f, JTextArea ta)	{
		super(f, "Command Log", false);
		ta.setEditable(false);
		JScrollPane sp = new JScrollPane(ta);
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(sp, BorderLayout.CENTER);
		new GeometryManager(this).pack();
	}
}