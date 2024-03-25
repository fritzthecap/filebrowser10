package fri.gui.swing.filebrowser;

import java.util.Calendar;
import java.awt.event.*;
import javax.swing.Timer;

import fri.gui.swing.datechooser.CalendarPanel;
import fri.gui.swing.datechooser.DaySelectionListener;
import fri.gui.swing.datechooser.MonthPanel;
import fri.gui.swing.datechooser.TimePanel;
import fri.gui.swing.datechooser.UneditableCalendarCombo;
import fri.util.date.DateUtil;

/**
	A Clock with a cuttable time-text, does not update time
	while user selects text.
*/

class Clock extends UneditableCalendarCombo
{
	private Timer timer, adjustTimer, minuteTimer;

	
	/**
		Create an updating CalendarComboBox, always with current time.
	*/
	public Clock()	{
		super(Calendar.getInstance());

		ActionListener al = createTimerListener();

		// create timers
		minuteTimer = new Timer(57000, al);
		adjustTimer = new Timer(500, al);
		
		// start timer
		timer = adjustTimer;
		timer.start();
	}
	
	/** Overridden to listen to day clicks. */
	protected CalendarPanel createCalendarPanel(MonthPanel mp, TimePanel tp)	{
		CalendarPanel cp = new CalendarPanel(mp, tp);
		cp.getMonthPanel().addDaySelectionListener(new DaySelectionListener()	{
			public void daySelectionChanged(Calendar selected)	{
				System.err.println("Day selection changed: "+DateUtil.toIsoString(selected));
			}
		});
		return cp; 
	}

	/** Stop the clock */
	public void stopClock()	{
		timer.stop();
	}
	
	private ActionListener createTimerListener()	{
		ActionListener al = new ActionListener()	{
			/** Implementing ActionListener to catch timer */
			public void actionPerformed(ActionEvent e)	{
				if (isPopupVisible())	{
					return;	// setting a new ComoBoxModel would close the popup
				}
				
				// get current time
				Calendar c = Calendar.getInstance();
				int minute = c.get(Calendar.MINUTE);
				int displayedMinute = getCalendar().get(Calendar.MINUTE);

				if (displayedMinute != minute)	{
					// if time has changed, order next event in 57 seconds
					setCalendar(c);

					toggleToTimer(minuteTimer);
				}
				else	{
					// if time has not changed, stop long period timer and start a short timer
					toggleToTimer(adjustTimer);
				}
			}
		};
		return al;
	}


	private void toggleToTimer(Timer t)	{
		if (timer != t)	{
			timer.stop();
			(timer = t).start();
		}
	}
	
	
	
	// test main
	public static void main(String [] args)	{
		javax.swing.JFrame f = new javax.swing.JFrame("Clock");
		f.getContentPane().add(new Clock());
		f.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.show();
	}
}