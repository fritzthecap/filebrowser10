package fri.gui.swing.datechooser;

import java.util.Calendar;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import fri.util.date.calendar.MonthLayout;
import fri.util.date.DateUtil;

/**
	This panel appears on the popup.
	Shows a calendar view on all days of a month of a year,
	including week numbers and holidays.
	Exactly one day can be selected by mouse or keyboard or programmatically.
	The days are written row by row horizontally, every week
	is a new line.
	<p>
	The Panel notifies registered DaySelectionListeners when day selection changes.
	<pre>
		MonthPanel p = new MonthPanel();
		frame.getContentPane().add(p);
	</pre>
	
	@author Fritz Ritzberger
*/
public class MonthPanel extends JPanel
{
	/** Flags for location of labels and togglebuttons */
	protected static final int UP = 1, DOWN = 2, LEFT = 4, RIGHT = 8;
	/** Indicates that this Calendar has a defined day. */
	protected boolean isNull;
	private Vector listenerList = new Vector(1);
	private MonthLayout monthLayout;
	private Calendar saveCalendar;
	private boolean mayChangeCalendar = true;

	/**
		Construct a panel that shows passed selected day within its year/month.
		If selected is null, current month is shown with no selection.
		@param selected the calendar to render.
		@param mayChangeCalendar true when the calendar may be changed by this panel.
	*/
	public MonthPanel(Calendar selected)	{
		saveCalendar(selected);
		init(-1, -1, selected);
	}

	/**
		Construct a panel for passed year/month that shows no selected day.
		The month is 1-12.
	*/
	public MonthPanel(int year, int month)	{
		init(year, month, null);
	}

	private void saveCalendar(Calendar c)	{
		if (c == null)
			saveCalendar = null;
		else
			saveCalendar = (Calendar)c.clone();
	}

	/**
		Set a new Calendar into this panel. The day will be selected.
		If c is null, no day will be selected and the current month will be shown.
	*/
	public void setCalendar(Calendar c)	{
		if (isNull() && c == null || c == getCalendar())	{
			return;	// no need for null if is already null
		}
		// else we need the pointer in monthLayout, even if the calendar is the same as the current!
		//System.err.println("setting new calendar "+(c != null ? ""+c.getTime() : "null")+", old calendar is: "+(getCalendar() != null ? ""+getCalendar().getTime() : "null"));
		saveCalendar(c);
		init(-1, -1, c);
	}

	/**
		Returns the currently selected Calendar day from this panel or null
		if no day was selected.
	*/
	public Calendar getCalendar()	{
		return monthLayout.getCalendar();
	}


	private boolean checkSavedCalendarMonth(int year, int month)	{
		if (saveCalendar != null &&
				saveCalendar.get(Calendar.YEAR) == year &&
				saveCalendar.get(Calendar.MONTH) == month)
		{
			init(-1, -1, saveCalendar);
			return true;
		}
		return false;
	}

	
	/** Made for the year-chooser to set a new year. */
	public void setYear(int year)	{
		if (checkSavedCalendarMonth(year, getMonth()) == false)
			init(year, getMonth() + 1, null);
	}

	public int getYear()	{
		return getCalendar().get(Calendar.YEAR);
	}

	/** Made for the month-chooser to set a new month. Passed month is 0-n. */
	public void setMonth(int month)	{
		if (checkSavedCalendarMonth(getYear(), month) == false)
			init(getYear(), month + 1, null);
	}

	public int getMonth()	{
		return getCalendar().get(Calendar.MONTH);
	}


	/** Returns true if no day was selected on calendar panel. */
	public boolean isNull()	{
		//System.err.println("MonthPanel "+hashCode()+" isNull: "+isNull);
		return isNull;
	}


	/** Default this method does nothing. */
	public void setEditable(boolean editable)	{
		this.mayChangeCalendar = editable;
	}


	/** Override this to return another MonthLayout with passed Calendar. */
	protected MonthLayout createMonthLayout(Calendar c)	{
		return new MonthLayout(c);
	}

	/** Override this to return another MonthLayout with passed year and month. */
	protected MonthLayout createMonthLayout(int y, int m)	{
		return new MonthLayout(y, m);
	}

	private MonthLayout createMonthLayout(int y, int m, Calendar c)	{
		this.isNull = false;
		
		if (c != null)
			return createMonthLayout(c);

		// calendar was null
		this.isNull = true;

		if (y >= 0 && m >= 0)
			return createMonthLayout(y, m);

		c = Calendar.getInstance();
		y = c.get(Calendar.YEAR);
		m = c.get(Calendar.MONTH) + 1;
		return createMonthLayout(y, m);
	}


	private void init(int y, int m, Calendar c)	{
		this.monthLayout = createMonthLayout(y, m, c);
		
		build();

		if (isVisible())	{
			revalidate();
			repaint();
		}
	}



	/** Override this to create other ToggleButtons. */
	protected AbstractButton createDaySelectionToggleButton(String s, Calendar calendarToChange, int location)	{
		AbstractButton b = new DaySelectionToggleButton(s);
		setLocationBorder(b, location);
		return b;
	}

	/** Override this to create other Labels. */
	protected Component createLabel(String s, int location)	{
		JLabel l = new JLabel(s, JLabel.CENTER);
		setLocationBorder(l, location);
		return l;
	}

	/** Different button borders (depending on location) form the inner border of calendar panel. */
	protected void setLocationBorder(JComponent l, int location)	{
		if ((location & UP) != 0 && (location & LEFT) != 0)
			;
		else
		if ((location & UP) != 0 && (location & RIGHT) != 0)
			l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, l.getForeground()));
		else
		if ((location & DOWN) != 0 && (location & RIGHT) != 0)
			l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, l.getForeground()));
		else
		if ((location & RIGHT) != 0 || (location & LEFT) != 0)
			l.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, l.getForeground()));
		else
		if ((location & DOWN) != 0 || (location & UP) != 0)
			l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, l.getForeground()));
		else
		if (l instanceof AbstractButton)
			((AbstractButton)l).setBorderPainted(false);
	}

	/** Fills this panel with buttons according to the current monthLayout. */
	public void build()	{
		//Thread.dumpStack();
		removeAll();	// remove all current sub-components
			
		//setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 21));
		setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 8));
		
		int ROWS = 6 + 1;	// always 6 week rows to avoid popup size errors, 1 for header
		int COLS = 7 + 1;	// 7 day columns, 1 header column
		setLayout(new GridLayout(ROWS, COLS));

		add(createLabel(" ", UP | LEFT));	// matrix cross at 0/0

		// add day names row
		String [] days = monthLayout.getDayHeaders();
		for (int i = 0; i < days.length; i++)	{
			add(createLabel(days[i], i == days.length - 1 ? UP|RIGHT : UP));
		}
		
		String [] weeks = monthLayout.getWeekHeaders();

		ButtonGroup group = null;
		
		// add day numbers and preceeding week column
		int dayIndex = 0;
		int max = (ROWS - 1) * COLS;	// one row already added: header
		
		for (int i = 0; i < max; i++)	{
			int location = 0;

			if (i % COLS == COLS - 1)
				location |= RIGHT;
			if (i % COLS == 0)
				location |= LEFT;
			if (i / COLS == ROWS - 2)
				location |= DOWN;
			
			if (dayIndex >= monthLayout.size())	{	// placeholders
				add(createLabel(" ", location));
			}
			else	{
				if (i % COLS == 0)	{	// add week header
					add(createLabel(weeks[i / 7], location));
				}
				else	{
					String s = monthLayout.elementAt(dayIndex).toString().trim();
					boolean valid = s.length() > 0;	// else filler element before 1. and after 31.
					
					Component comp = valid ?
							(Component)createDaySelectionToggleButton(s, getCalendar(), location) :
							(Component)createLabel(s, location);
		
					if (comp instanceof DaySelectionToggleButton)	{
						DaySelectionToggleButton b = (DaySelectionToggleButton)comp;
						
						if (group == null)
							group = new ButtonGroup();
						
						group.add(b);
		
						//System.err.println("defined day "+s+" = "+ml.isDefinedDay(dayIndex)+", isNull: "+isNull);
						if (monthLayout.isDefinedDay(dayIndex))	{
							b.setSelected(true);
							b.adjustSelectionBorder(true);
						}
						
						String t;
						if ((t = monthLayout.isHoliday(dayIndex)) != null)	{
							b.setForeground(Color.green);
							b.setToolTipText(t);
						}
						else
						if (monthLayout.isSunday(dayIndex))	{
							b.setForeground(Color.red);
						}

						b.addListeners();	// listen for selection to call DaySelectionListeners and to setBorderPainted()
						
					}	// end if instanceof DaySelectionToggleButton
					
					add(comp);

					dayIndex++;	// after all checks for this day, increment it

				}	// end if not week number column
			}	// end if not beyond last day of month
		}
	}



	public void addDaySelectionListener(DaySelectionListener l)	{
	 	listenerList.add(l);
	}
	
	public void removeDaySelectionListener(DaySelectionListener l)	{
	 	listenerList.remove(l);
	}
	

	/**
		Made for the day selection button to set a new day.
		Notifies all registered DaySelectionListeners.
	*/
	protected void fireDaySelectionChanged(int day)	{
		Calendar c = getCalendar();
		if (mayChangeCalendar)	{
			c.set(Calendar.DAY_OF_MONTH, day);
			saveCalendar(c);
			this.isNull = false;
		}
		else{
			c = (Calendar) c.clone();
			c.set(Calendar.DAY_OF_MONTH, day);
		}
		
		for (int i = 0; i < listenerList.size(); i++)	{
			((DaySelectionListener) listenerList.elementAt(i)).daySelectionChanged(c);
		}
	}



	/**
		Returns a localized String representaion for currently set month like
		"October" (shortName = false) or "Oct" (shortName = true).
	*/
	public String getMonthDisplayName(boolean shortName)	{
		String [] months = DateUtil.getMonthDisplayNames(shortName);
		return months[getMonth()];
	}

	/**
		Returns a localized String representaion for currently set year.
	*/
	public String getYearDisplayName()	{
		return ""+getYear();
	}



	/**
		A toggle button that contains the day number in its label text.
		To avoid fireDaySelectionChanged() when setting default selection,
		the addItemListener() method must be called from outside.
	*/
	protected class DaySelectionToggleButton extends JToggleButton implements
		ItemListener,	// listen to myself because then button can be garbage collected
		ActionListener
	{
		private Border matte, original;
		
		public DaySelectionToggleButton(String s)	{
			super(s);
		}
		
		public Insets getInsets()	{
			return new Insets(3, 5, 3, 5);
		}
		
		public void setBorder(Border b)	{
			super.setBorder(b);
			
			if (b != null)	{	// store borders for itemStateChanged() calls
				if (matte == null && b instanceof MatteBorder)	{
					matte = b;
				}
				else
				if (original == null)	{
					original = b;
				}
			}
		}

		/** Set the line border or the button border according to selection state. */
		protected void adjustSelectionBorder(boolean selected)	{
			if (selected)
				if (matte != null)
					DaySelectionToggleButton.this.setBorder(original);
				else
					DaySelectionToggleButton.this.setBorderPainted(true);
			else
				if (matte != null)
					DaySelectionToggleButton.this.setBorder(matte);
				else
					DaySelectionToggleButton.this.setBorderPainted(false);
		}

		void addListeners()	{
			addItemListener(this);
			addActionListener(this);
		}
		
		/** Implements ItemListener to fire day change. */
		public void actionPerformed(ActionEvent e) {
			String s = getText().trim();
			int day = Integer.valueOf(s).intValue();
			fireDaySelectionChanged(day);
		}
		
		/** Implements ItemListener to shift selection border. */
		public void itemStateChanged(ItemEvent e)	{
			if (e.getID() == ItemEvent.ITEM_STATE_CHANGED)
				adjustSelectionBorder(e.getStateChange() == ItemEvent.SELECTED);
		}
		
	}




	// test main
	/*
	public static void main(String [] args)	{
		//Locale.setDefault(Locale.US);
		System.err.println("SYNTAX: fri.gui.swing.datechooser.MonthPanel [month [year]]");

		MonthPanel p;
		if (args.length > 0)	{
			int m = -1, y = -1;
			m = Integer.valueOf(args[0]).intValue();
			if (args.length > 1)
				y = Integer.valueOf(args[1]).intValue();
			else
				y = Calendar.getInstance().get(Calendar.YEAR);
			p = new MonthPanel(y, m);
		}
		else	{
			p = new MonthPanel(Calendar.getInstance());
		}
		
		JFrame f = new JFrame(p.getMonthDisplayName(false)+" "+p.getYearDisplayName());
		f.getContentPane().add(p);
		
		f.pack();
		f.show();
	}
	 */
}
