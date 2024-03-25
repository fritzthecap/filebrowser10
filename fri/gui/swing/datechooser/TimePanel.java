package fri.gui.swing.datechooser;

import java.util.*;
import javax.swing.*;
import fri.gui.swing.spinnumberfield.*;

/**
	Panel that shows a time chooser as two or three text fields.
	<pre>
		TimePanel tp = new TimePanel(Calendar.getInstance());
		frame.getContentPane().add(tp);
	</pre>
	
	@author Fritz Ritzberger
	@version $Revision$
*/

public class TimePanel extends JPanel
{
	private Calendar calendar;
	private SpinNumberField hour, minute, second = null;
	private TimeFieldListener hourListener, minuteListener, secondListener;
	private boolean withSeconds;
	
	
	/** Constructor with a given Calendar. */
	public TimePanel(Calendar calendar)	{
		this(calendar, false);
	}

	/** Constructor for deferred build() - ComboBx builds the panel deferred. */
	public TimePanel(Calendar calendar, boolean withSeconds)	{
		this.calendar = calendar;
		this.withSeconds = withSeconds;
		build();
	}
	

	/** Set a new Calendar (time) into this panel. */
	public void setCalendar(Calendar c)	{
		setCalendar(c, false);
	}
	
	/** Set a new Calendar (time) into this panel, but keep time. */
	public void setCalendar(Calendar c, boolean keepTime)	{
		if (hour == null)	{
			build();
		}
		else	{
			//System.err.println("TimePanel.setCalendar keep time "+keepTime+", "+c.get(Calendar.MINUTE));
			this.calendar = c;

			if (keepTime)	{
				c.set(Calendar.HOUR_OF_DAY, (int)hour.getValue());
				c.set(Calendar.MINUTE, (int)minute.getValue());
				if (second != null)	{
					c.set(Calendar.SECOND, (int)second.getValue());
				}
			}
			else	{
				hour.getNumberEditor().removeNumberEditorListener(hourListener);
				hour.setValue((long)calendar.get(Calendar.HOUR_OF_DAY));
				hour.getNumberEditor().addNumberEditorListener(hourListener);
		
				minute.getNumberEditor().removeNumberEditorListener(minuteListener);
				minute.setValue((long)calendar.get(Calendar.MINUTE));
				minute.getNumberEditor().addNumberEditorListener(minuteListener);
				
				if (second != null)	{
					second.getNumberEditor().removeNumberEditorListener(secondListener);
					second.setValue((long)calendar.get(Calendar.SECOND));
					second.getNumberEditor().addNumberEditorListener(secondListener);
				}
			}
		}
	}

	/** Returns the Calendar object, i.e. the time. */
	public Calendar getCalendar()	{
		return calendar;
	}


	/** Sets all subcomponents to given state. Can be called after build(). */
	public void setEditable(boolean editable)	{
		hour.setEditable(editable);
		minute.setEditable(editable);
		if (second != null)
			second.setEditable(editable);
	}


	public boolean getWithSeconds()	{
		return second != null;
	}
	
	
	public void build()	{
		//Thread.dumpStack();
		removeAll();
		
		hour = new SpinNumberField((long)getCalendar().get(Calendar.HOUR_OF_DAY), 0, 23, (short)2);
		hourListener = new TimeFieldListener(hour);
		add(hour);
		
		minute = new SpinNumberField((long)getCalendar().get(Calendar.MINUTE), 0, 59, (short)2);
		minuteListener = new TimeFieldListener(minute);
		add(minute);
		
		if (withSeconds)	{
			second = new SpinNumberField((long)getCalendar().get(Calendar.SECOND), 0, 59, (short)2);
			secondListener = new TimeFieldListener(second);
			add(second);
		}
		
		setCalendar(getCalendar());
	}
	


	private class TimeFieldListener implements
		NumberEditorListener
	{
		private SpinNumberField src;
		
		TimeFieldListener(SpinNumberField src)	{
			this.src = src;
		}
		
		/** Receive a change event from a NumberEditor.*/
		public void numericValueChanged(long newValue)	{
			if (this.src == hour)	{
				getCalendar().set(Calendar.HOUR_OF_DAY, (int)newValue);
			}
			else
			if (this.src == minute)	{
				getCalendar().set(Calendar.MINUTE, (int)newValue);
			}
			else
			if (this.src == second)	{
				getCalendar().set(Calendar.SECOND, (int)newValue);
			}
		}
	}



	// test main
	/*
	public static void main(String [] args)	{
		//Locale.setDefault(Locale.US);
		System.err.println("SYNTAX: fri.gui.swing.datechooser.TimePanel");

		JFrame f = new JFrame("Time Chooser");
		TimePanel tp = new TimePanel(Calendar.getInstance());
		f.getContentPane().add(tp);
		
		f.pack();
		f.show();
	}
	*/
}