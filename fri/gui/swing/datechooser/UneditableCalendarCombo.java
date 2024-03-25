package fri.gui.swing.datechooser;

import java.util.*;
import java.text.DateFormat;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import fri.gui.swing.combo.*;

/**
 * ComboBox to show date (and optional time) in a textfield, and
 * a popup that shows a monthwise calendar.
 *
 * @author Fritz Ritzberger
*/
public class UneditableCalendarCombo extends WideComboBox
{
	protected int dateFormat, timeFormat;
	private DateFormat fmt = null;
	private boolean withTime, withSeconds;
	private Calendar initCalendar;
	private CalendarPanel calendarPanel;
	private boolean first = true;
	private ActionListener al = null;


	/**
		Construct a Combo that has no specific date/time chosen,
		but will render current month when popu shows (NULL constructor).
	*/
	public UneditableCalendarCombo()	{
		this(null);
	}
	
	/**
		Construct a Combo that shows passed calendar (which can be null),
		with time, without seconds.
	*/
	public UneditableCalendarCombo(Calendar c)	{
		this(c, true);
	}
	
	/**
		Construct a Combo that shows the passed calendar (which can be null),
		optional with time, if with time, without seconds.
	*/
	public UneditableCalendarCombo(Calendar c, boolean withTime)	{
		this(c, withTime, false);
	}

	/**
		Construct a Combo that shows the passed calendar (which can be null),
		@dateFormat one of DateFormat.SHORT, DateFormat.MEDIUM, ...
		@c Calendar date/time to show, nullable
	*/
	public UneditableCalendarCombo(Calendar c, int dateFormat)	{
		this(c, dateFormat, -1);
	}

	/**
		Construct a Combo that shows the passed calendar (which can be null),
		@dateFormat one of DateFormat.SHORT, DateFormat.MEDIUM, ...
		@timeFormat one of DateFormat.SHORT, DateFormat.MEDIUM, ...
		@c Calendar date/time to show, nullable
	*/
	public UneditableCalendarCombo(Calendar c, int dateFormat, int timeFormat)	{
		if (timeFormat != -1)	{
			this.withTime = true;
			this.withSeconds = (timeFormat != DateFormat.SHORT);
		}
		else	{
			this.withTime = this.withSeconds = false;
		}

		init(c, dateFormat, timeFormat);
	}

	/**
		Construct a Combo that shows the passed calendar (which can be null).
		optional with time and seconds.
		@c Calendar date/time to show, nullable
		@withTime if true, hour/minute is shown
		@withSeconds if true, seconds are shown
	*/
	public UneditableCalendarCombo(Calendar c, boolean withTime, boolean withSeconds)	{
		if (withTime == false && withSeconds)
			throw new IllegalArgumentException("Seconds can not be shown without time");
			
		this.withTime = withTime;
		this.withSeconds = withSeconds;

		init(c, DateFormat.MEDIUM, withSeconds ? DateFormat.MEDIUM : DateFormat.SHORT);
	}


	protected void init(Calendar c, int dateFormat, int timeFormat)	{
		this.initCalendar = c;
		this.dateFormat = dateFormat;
		this.timeFormat = timeFormat;
		
		this.calendarPanel = createCalendarPanel();	// create a calendar panel
		
		updateUI();	// now we do it explicitely

		createActionListener();	// overriding actionPerformed() is not recommended
	}


	private CalendarPanel createCalendarPanel()	{
		MonthPanel mp = createMonthPanel(initCalendar);
		
		// initCalendar will be set, so save it
		saveCalendar();
		
		TimePanel tp = null;
		if (withTime)	{
			tp = createTimePanel(mp.getCalendar(), withSeconds);
		}
		
		return createCalendarPanel(mp, tp);
	}
	
	/** Override this to create another month panel, can not return null. */
	protected MonthPanel createMonthPanel(Calendar c)	{
		return new MonthPanel(c);
	}

	/** Override this to create another time panel, can return null. */
	protected TimePanel createTimePanel(Calendar c, boolean withSeconds)	{
		return new TimePanel(c, withSeconds);
	}
	
	/** Override this to create another calendar panel, can not return null. */
	protected CalendarPanel createCalendarPanel(MonthPanel mp, TimePanel tp)	{
		return new CalendarPanel(mp, tp);
	}
	
	

	/** Returns the calendar panel. */
	protected CalendarPanel getCalendarPanel()	{
		return calendarPanel;
	}
	

	/** Sets a new calendar to panel and as ComboBox text. */
	public void setCalendar(Calendar c)	{
		//System.err.println("setting a new Calendar to CalendarCombo: "+(c != null ? ""+c.getTime() : "null"));
		setCalendarFromComboEditor(c);
		initComboModel();
	}


	/** Returns the calendar from the panel. */
	public Calendar getCalendar()	{
		return getCalendarPanel().getCalendar();
	}


	/** Returns true if this calendar is not defined. */
	public boolean isNull()	{
		return getCalendarPanel().isNull();
	}
	
	
	/**
		Overridden to set a SimpleCalendarComboEditor and do updateUI() for CalendarPanel.
		This method executes only when super.constructor is done (i.e. calendarPanel not null).
	*/
	public void updateUI()	{
		if (getCalendarPanel() != null)	{	// do updateUI() only when super.constructor is done.
			super.updateUI();
			
			if (first == false)	{
				getCalendarPanel().updateUI();
			}
			first = false;

			ComboBoxEditor edi = createCalendarComboEditor();
			setEditor(edi);
				
			initComboModel();
		}
	}


	/** Override this to set another ComboBoxEditor for CalendarWrapper objects. */
	protected ComboBoxEditor createCalendarComboEditor()	{
		ComboBoxEditor edi = new SimpleCalendarComboEditor(getDateFormat(), withTime, withSeconds);
		initComboModel();
		return edi;
	}
	

	/** Returns default false for UneditableCalendarCombo. */
	public boolean isMutable()	{
		return false;
	}

	
	/**
		Set the CelandarPanel editable, too. This overriding does not
		disallow editable setting, as this is needed in CalendarCombo superclass.
	*/
	public void setEditable(boolean editable)	{
		super.setEditable(editable);
		if (getCalendarPanel() != null)
			getCalendarPanel().setEditable(editable);
	}

	

	/**
		Creates and adds the ActionListener that updates the calendar panel
		when ComboBoxEditor changed date.
	*/
	protected void createActionListener()	{
		if (al == null)	{
			al = new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					CalendarWrapper newItem = (CalendarWrapper)getEditor().getItem();
					Calendar c = newItem.isNull() ? null : newItem.getCalendar();
					setCalendarFromComboEditor(c);
				}
			};
			addActionListener(al);
		}
	}


	private void saveCalendar()	{
		if (initCalendar != null)	{	// save the calendar, if defined
			initCalendar = (Calendar)initCalendar.clone();
		}
	}
	
	
	/** Called by ComboEditor Textfield when ENTER pressed or focus lost. */
	private void setCalendarFromComboEditor(Calendar c)	{
		//System.err.println("setCalendarFromComboEditor: "+c.getTime()+", old was "+initCalendar.getTime());
		getCalendarPanel().setCalendar(initCalendar = c);
		saveCalendar();
		calendarChanged();
	}
	
	
	/** Called by ComboPopup when closing. */
	private void setComboModelFromPopup()	{
		//System.err.println("setComboModelFromPopup: "+getCalendar().getTime()+", isNull "+isNull());
		if (isMutable())	{
			if (isNull() == false)	{	// avoid setting when no selection took place
				initCalendar = getCalendar();
				saveCalendar();
				initComboModel();
				calendarChanged();
			}
		}
		else	{
			setCalendarFromComboEditor(initCalendar);
		}
	}


	/** Does nothing. Override this to fire value changed to some listener. */
	protected void calendarChanged()	{
	}
	
	/** Overridden (JDK 1.4) to do nothing when focus event forces to close popup. It will be closed by DaySelectionListener. */
    protected void processFocusEvent(FocusEvent e) {
    }

	/** Called by ComboPopup when closing, and updateUI(), and setCalendar(). */
	private void initComboModel()	{
		Object obj = new CalendarWrapper(getCalendar(), isNull(), getDateFormat());
		setModel(createComboBoxModel(obj));
	}


	/**
		Create a ComboBoxModel for a new date value.
		This allocates a CalendarComboModel.
	*/
	protected ComboBoxModel createComboBoxModel(Object o)	{
		return new CalendarComboModel(o);
	}
		

	/**
		Overridden to do nothing, as this call would always set the old editor
		value into calendar panel when a popup choice was made.
	*/
	public void setSelectedIndex(int i)	{
		//System.err.println("setSelectedIndex("+i+")");
	}
	
	
	/** Override this to set a special date format. */
	protected DateFormat getDateFormat()	{
		if (fmt == null)	{
			if (withTime)	{
				fmt = DateFormat.getDateTimeInstance(dateFormat, timeFormat);
			}
			else	{
				fmt = DateFormat.getDateInstance(dateFormat);
			}
		}
		return fmt;
	}
	
	
	/** Overridden from WideComboBox, creates a CalendarPopup. */
	protected ComboPopup createComboPopupUI()	{
		ComboPopup popup = new CalendarPopup(this);
		return popup;
	}

	
	/**
	 * Due to a bad event behaviour in JDK 1.3 this was overridden.
	 * ComboBoxEditor FocusListener calls actionPerformed() when
	 * loosing focus (because of mousePressed) just before popup is closed (because of mouseReleased).
	 * The ItemEvent and the FocusEvent seem to arrive coincidentally, sometimes FocusEvent comes before ItemEvent.
	 * (Why? Swing events are not OS-specific!?)
	 * Then the new value from (empty) editor is set and popup itemStateChange callback does not take
	 * place anymore. The effect is, that the date value can not be changed.
	 */
	public void actionPerformed(ActionEvent e)	{
		if (e.getID() != ActionEvent.ACTION_PERFORMED)	{
			//System.err.println("UneditableCalendarCombo.actionPerformed, event ID is: "+e.getID());
			return;	// seems to come from BasicComboBoxUI.editor FocusListener, do not close before itemStateChanged was called.
		}
		super.actionPerformed(e);
	}
	

	
	/** Extends WideComboPopup to add calendar panel to popup and close on day selection. */
	public class CalendarPopup extends WideComboPopup implements
		DaySelectionListener
	{
		private boolean inHide;	// workaround to avoid recursion from setModel() when editable
		
		public CalendarPopup(JComboBox comboBox)	{
			super(comboBox);
			CalendarPopup.this.removeAll();
			CalendarPopup.this.add(getCalendarPanel());
			
			getCalendarPanel().getMonthPanel().addDaySelectionListener(CalendarPopup.this);
		}
		
		/** Implements DaySelectionListener: closes the popup. */
		public void daySelectionChanged(Calendar c)	{
			CalendarPopup.this.hide();
		}
		
		public void show()	{
			getCalendarPanel().setEditable(isMutable());
			if (isMutable())
				al.actionPerformed(null);	// sets current calendar from editor
			super.show();
		}

		public void hide()	{
			if (inHide == false)	{
				inHide = true;	// avoid Swing recursion
				if (CalendarPopup.this.isVisible())	{
					setComboModelFromPopup();
				}
				super.hide();
				inHide = false;
			}
		}

	}



	/** Model Item to render a Calendar in ComboBoxEditor as String. */
	public static class CalendarWrapper
	{
		private Calendar c;
		private DateFormat fmt;
		private boolean isNull;
		
		public CalendarWrapper(Calendar c, boolean isNull, DateFormat fmt)	{
			this.c = c;
			this.isNull = isNull;
			this.fmt = fmt;
		}
		
		public Calendar getCalendar()	{
			return c;
		}

		public boolean isNull()	{
			return isNull;
		}

		public String toString()	{
			if (isNull)
				return "";
			String s = fmt.format(c.getTime());
			return s;
		}
	}



	/** Simplifying Model for CalendarCombo. */
	public static class CalendarComboModel extends AbstractListModel implements
		ComboBoxModel
	{
		private Object item;
		
		CalendarComboModel(Object item)	{
			this.item = item;
		}
		
		public void setSelectedItem(Object anObject)	{
			this.item = anObject;
		}
		
		public Object getSelectedItem()	{
			return item;
		}
		
		public int getSize()	{
			return 1;
		}

		public Object getElementAt(int i)	{
			return item;
		}
	}
	
	


	// test main
	/**/
	public static void main(String [] args)	{
		//Locale.setDefault(Locale.US);
		//Locale.setDefault(Locale.GERMAN);
		//Locale.setDefault(new Locale("hr", "HR"));
		//Locale.setDefault(new Locale("sv", "SE"));

		JFrame f = new JFrame("UneditableCalendarCombo");
		final UneditableCalendarCombo cc = new UneditableCalendarCombo(Calendar.getInstance());//, false);
		cc.getCalendarPanel().getMonthPanel().addDaySelectionListener(new DaySelectionListener()	{
			public void daySelectionChanged(Calendar c) {
				System.err.println("selected calendar: "+fri.util.date.DateUtil.toIsoString(c));
			}
		});
		
		JButton btnTestCal = new JButton("setCalendar(1.1.2000)");
		btnTestCal.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				Calendar testCal = Calendar.getInstance();
				testCal.set(Calendar.DAY_OF_MONTH, 1);	// 01
				testCal.set(Calendar.MONTH, 0);	// Jan
				testCal.set(Calendar.YEAR, 2000);	// 2000
				
				cc.setCalendar(testCal);
			}
		});
		JButton btnFocus = new JButton("Test Focus Change");
		
		f.getContentPane().setLayout(new java.awt.FlowLayout());
		f.getContentPane().add(cc);
		f.getContentPane().add(new UneditableCalendarCombo());
		f.getContentPane().add(btnFocus);
		f.getContentPane().add(btnTestCal);
		
		f.setSize(400, 400);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}
	/**/
}
