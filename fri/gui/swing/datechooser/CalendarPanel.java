package fri.gui.swing.datechooser;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import fri.util.date.DateUtil;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.swing.spintextfield.*;

/**
	This is the panel that appears on the popup.
	It adds passed year and month selection boxes to the MonthPanel.
	Optionally a TimePanel can be added.
	<p>
	This object holds and manages time and date panels, but not the Calendar object.

	@author Fritz Ritzberger
*/

public class CalendarPanel extends JPanel implements
	ItemListener,
	NumberEditorListener
{
	private boolean shortMonthNames = false;
	private MonthPanel monthPanel;
	private TimePanel timePanel;
	private SpinNumberField year;
	private SpinTextField month;
	

	/**
		Construct a panel that controls the passed MonthPanel.
	*/
	public CalendarPanel(MonthPanel monthPanel)	{
		this(monthPanel, null);
	}

	/**
		Construct a panel that controls the passed MonthPanel and adds
		the passed TimePanel.
	*/
	public CalendarPanel(MonthPanel monthPanel, TimePanel timePanel)	{
		this(monthPanel, timePanel, true);
	}

	/**
		Construct a panel that controls the passed MonthPanel and adds
		the passed TimePanel.
		If build is false, updateUI() must be called (deferred build, ComboBox).
	*/
	public CalendarPanel(MonthPanel monthPanel, TimePanel timePanel, boolean build)	{
		super(new BorderLayout());
		
		this.monthPanel = monthPanel;
		this.timePanel = timePanel;
		
		if (build)
			build();
	}
	

	public boolean isNull()	{
		return getMonthPanel().isNull();
	}
	
	
	public void setShortMonthNames(boolean shortMonthNames)	{
		this.shortMonthNames = shortMonthNames;
	}
	

	public void setEditable(boolean editable)	{
		getMonthPanel().setEditable(editable);
		if (getTimePanel() != null)
			getTimePanel().setEditable(editable);
			
		year.setEditable(editable);
		year.getSpinner().setEnabled(true);	// special for this, be able to use calendar
		month.setEditable(editable);
		month.getSpinner().setEnabled(true);	// special for this, be able to use calendar
	}


	/** Set a new Calendar into this panel. */
	public void setCalendar(Calendar c)	{
		getMonthPanel().setCalendar(c);
		synchronizeTimePanelCalendar();
		
		if (c != null)	{
			year.getNumberEditor().removeNumberEditorListener(this);
			year.setValue(c.get(Calendar.YEAR));
			year.getNumberEditor().addNumberEditorListener(this);
			
			month.removeItemListener(this);
			month.setSelectedIndex(c.get(Calendar.MONTH));
			month.addItemListener(this);
		}
	}
	
	
	public Calendar getCalendar()	{
		return getMonthPanel().getCalendar();
	}
	

	public MonthPanel getMonthPanel()	{
		return monthPanel;
	}

	public TimePanel getTimePanel()	{
		return timePanel;
	}


	public void updateUI()	{
		super.updateUI();
		
		if (getMonthPanel() != null)	{
			getMonthPanel().updateUI();
			getMonthPanel().build();
			
			if (getTimePanel() != null)	{
				getTimePanel().updateUI();
				getTimePanel().build();
			}

			build();
		}
	}
	

	private void build()	{
		//Thread.dumpStack();
		removeAll();
		
		add(getMonthPanel(), BorderLayout.CENTER);
		
		if (getTimePanel() != null)	{
			add(getTimePanel(), BorderLayout.SOUTH);
		}
		
		year = new SpinNumberField(monthPanel.getYear());
		month = new SpinTextField(DateUtil.getMonthDisplayNames(shortMonthNames));
		month.setSelectedIndex(getMonthPanel().getMonth());

		// figure out if "month year" or "year month".
		boolean yearBeforeMonth = DateUtil.isYearLeftOfMonth();
		
		JPanel p = new JPanel();
		p.add(yearBeforeMonth ? (Component)year : (Component)month);
		p.add(yearBeforeMonth ? (Component)month : (Component)year);
		
		add(p, BorderLayout.NORTH);
		
		year.getNumberEditor().addNumberEditorListener(this);
		month.addItemListener(this);
	}


	// ensure timePanel uses the same calendar as MonthPanel
	private void synchronizeTimePanelCalendar()	{
		synchronizeTimePanelCalendar(false);
	}

	// ensure timePanel uses the same calendar as MonthPanel
	private void synchronizeTimePanelCalendar(boolean keepTime)	{
		if (getTimePanel() != null)	{
			getTimePanel().setCalendar(getMonthPanel().getCalendar(), keepTime);
		}
	}
		
	
	/** Implements ItemListener to catch changes of month. */
	public void itemStateChanged(ItemEvent e)	{
		getMonthPanel().setMonth(month.getSelectedIndex());
		synchronizeTimePanelCalendar(true);
	}
	
	
	/** Implements NumberEditorListener to catch changes of year. */
	public void numericValueChanged(long value)	{
		getMonthPanel().setYear((int)value);
		synchronizeTimePanelCalendar(true);
	}
	
	
	

	// test main
	/*
	public static void main(String [] args)	{
		//Locale.setDefault(Locale.US);

		JFrame f = new JFrame("Calendar");
		MonthPanel mp = new MonthPanel(null);
		TimePanel tp = new TimePanel(mp.getCalendar(), false);
		CalendarPanel cp = new CalendarPanel(mp, tp);
		f.getContentPane().add(cp);
		
		f.pack();
		f.show();
	}
	*/
}