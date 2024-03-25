package fri.gui.swing.button;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Calls actionPerformed() as long as the button is pressed.
 * 
 * @author Fritz Ritzberger, 2007
 */
public class PressedEventGeneratingButton extends JButton
{
	public PressedEventGeneratingButton(Action action)	{
		super(action);
		setModel(new AutoFiringButtonModel());
		setAction(action);
	}

	public PressedEventGeneratingButton(String text)	{
		super(text);
		setModel(new AutoFiringButtonModel());
		setText(text);
	}


	/**
	 * A button model that starts a button-pressing thread when pressed and stops it when released.
	 */
	public static class AutoFiringButtonModel extends DefaultButtonModel
	{
		private static final int AUTO_FIRE_INTERVAL_MILLIS = 100;
		
		private Timer autoFireTimer;
		private final int interval;
		
		private ActionListener buttonPresser = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				// FRi 2007-06-12: following code was copied from JDK, this is in JToggleButton AND in AbstractButton
	            int modifiers = 0;
	            AWTEvent currentEvent = EventQueue.getCurrentEvent();
	            if (currentEvent instanceof InputEvent)
	                modifiers = ((InputEvent) currentEvent).getModifiers();
	            else
	            if (currentEvent instanceof ActionEvent)
	                modifiers = ((ActionEvent) currentEvent).getModifiers();

	            fireActionPerformed(	// this actionPerformed() is in dispatch thread
	                new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
	                                getActionCommand(),
	                                EventQueue.getMostRecentEventTime(),
	                                modifiers));
			}
		};

		
		public AutoFiringButtonModel()	{
			this(AUTO_FIRE_INTERVAL_MILLIS);
		}
		
		public AutoFiringButtonModel(int interval)	{
			this.interval = interval;
		}
		
		public void setPressed(boolean b)
		{
            if (isPressed() == b || false == isEnabled())	// copied from JDK AbstractButton impl
                return;
            
			super.setPressed(b);
			
			if (b == false)
				stopAutoFireThread();
			else
			if (autoFireTimer == null)
				startAutoFireThread();
		}
			
		private void startAutoFireThread()	{
			autoFireTimer = new Timer(interval, buttonPresser);
			autoFireTimer.setRepeats(true);
			autoFireTimer.start();
		}
		
		private void stopAutoFireThread()	{
			if (autoFireTimer != null)	{
				autoFireTimer.stop();
				autoFireTimer = null;
			}
		}
	}


	
	
	/*
	public static void main(String [] args)	{
		JFrame f = new JFrame("PressedEventGeneratingButton Test");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		AbstractButton b = new PressedEventGeneratingButton("Press Me");
		b.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				System.err.println("actionPerformed ... in event thread: "+EventQueue.isDispatchThread());
			}
		});
		f.getContentPane().add(b);
		f.pack();
		f.setVisible(true);
	}
	 */
}
