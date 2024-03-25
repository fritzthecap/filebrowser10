package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import fri.gui.swing.spinnumberfield.*;

/**
	Screenshot workflow: Inform the user about the coming Frame
	containing a full-screen-shot and offer a delay time.
*/

public class ScreenshotDialog extends JPanel implements
	ActionListener,	// checkbox
	Runnable	// delayed screenshot
{
	private static int delaySeconds = 3;
	private NetNode root;
	private Component parent;
	private JCheckBox cbDelay;
	private SpinNumberField spDelay;
	private int delayMillis;
	private static final String message = 
			"Java Screenshot Utility.\n"+
			"\n"+
			"A window will be opened with a screenshot of the whole screen.\n"+
			"You can then mark any area within this window by dragging the mouse,\n"+
			"on mouse-release a little frame will be opened with the marked area.\n"+
			"Store the area by right mouseclick context menu on the child frame,\n"+
			"or close it and mark another rectangle.\n"+
			"Currently supported formats are JPG and GIF.\n"+
			"\n"+
			"Set a delay time if you need to prepare the target window before.\n"+
			"";
	
	
	public ScreenshotDialog(Component parent, NetNode root)	{
		super(new BorderLayout());
		
		this.root = root;
		this.parent = parent;
		
		JTextArea ta = new JTextArea(message);
		ta.setEditable(false);
		add(ta, BorderLayout.CENTER);

		JPanel p = new JPanel();
		cbDelay = new JCheckBox("Delay Time In Seconds", false);
		cbDelay.addActionListener(this);
		spDelay = new SpinNumberField(delaySeconds, 0, 300);
		spDelay.setEditable(cbDelay.isSelected());
		p.add(cbDelay);
		p.add(spDelay);
		
		add(p, BorderLayout.SOUTH);
		
		JOptionPane pane = new JOptionPane(
				this,
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.DEFAULT_OPTION);
		JDialog dlg = pane.createDialog(parent, "Screenshot Utility");
		dlg.show();
		
		delaySeconds = (int)spDelay.getValue();
			
		if (pane.getValue() != null)	{	// OK pressed
			int ds = cbDelay.isSelected() ? delaySeconds : 0;
			delayMillis = ds * 1000;
			
			Thread thread = new Thread(this);
			thread.start();
		}
	}
	
	

	/**
		Implement ActionListener to set enabled/disabled number textfield.
	*/
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == cbDelay)	{
			spDelay.setEditable(cbDelay.isSelected());
		}
	}
	
	/**
		Implement Runnable to start screenshot from background thread.
		This is needed not only fro delay time but also for giving the dialog
		time to disappear.
	*/
	public void run()	{
		try	{
			Thread.sleep(delayMillis);
		}
		catch (InterruptedException e)	{
			return;
		}
		
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				try	{
					new StoreableScreenshot(root);
				}
				catch (AWTException e)	{
					JOptionPane.showMessageDialog(parent, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}
	
}