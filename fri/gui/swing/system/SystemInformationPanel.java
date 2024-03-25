package fri.gui.swing.system;

import java.awt.BorderLayout;
import javax.swing.*;
import fri.util.error.Err;
import fri.gui.swing.propdialog.PropViewDialog;
import fri.gui.swing.system.network.NetworkInterfacePanel;
import fri.gui.swing.system.screens.GraphicsDevicePanel;

/**
 * Render hardware information about the local machine and system properties.
 * 
 * @author Fritz Ritzberger
 * Created on 18.01.2006
 */
public class SystemInformationPanel extends JPanel
{
	public SystemInformationPanel()	{
		super(new BorderLayout());
		
		JTabbedPane tabbedPane = new JTabbedPane();
		add(tabbedPane);
		
		tabbedPane.addTab("System Properties", createSystemPropertiesPanel());
		
		// hardware was not supported on Java prior to 1.4
		try	{
			tabbedPane.addTab("Graphic Devices", new GraphicsDevicePanel());
		}
		catch (Error e)	{
			Err.error(e);
		}
		
		try	{
			tabbedPane.addTab("Network", new NetworkInterfacePanel());
		}
		catch (Error e)	{
			Err.error(e);
		}
	}
	
	private JComponent createSystemPropertiesPanel()	{
		JDialog dlg = new PropViewDialog((JFrame) null);
		return (JComponent) dlg.getContentPane().getComponent(0);
	}

	
	public static void main(String [] args)	{
		JFrame f = new JFrame();
		f.getContentPane().add(new SystemInformationPanel());
		f.pack();
		f.setVisible(true);
	}
}
