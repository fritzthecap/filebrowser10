package fri.gui.swing.datechooser;

import java.awt.Color;
import java.util.Date;
import java.util.Calendar;
import java.text.ParseException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
	ComboBoxEditor for Date/Time that parses a Date from a String
	and manages an error border for JTextField.
	
	@author Fritz Ritzberger
	@version $Revision: 2.3 $
*/

public class SimpleCalendarComboEditor extends BasicComboBoxEditor.UIResource
{
	protected DateFormat fmt;
	private Border orig, err;
	private boolean withTime, withSeconds;
	

	public SimpleCalendarComboEditor(DateFormat fmt, boolean withTime, boolean withSeconds)	{
		this.fmt = fmt;
		this.withTime = withTime;
		this.withSeconds = withSeconds;

		this.editor = createMyEditorComponent();

		this.orig = editor.getBorder();
	 	this.err = createErrorBorder(orig);
		
		try	{
			SimpleDateFormat sdf = (SimpleDateFormat)fmt;
			String pattern = sdf.toLocalizedPattern();
			editor.setToolTipText(pattern.toUpperCase());
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}


	/** Called from constructor to create the error border. Override to change Color. */
	protected Border createErrorBorder(Border orig)	{
		return BorderFactory.createLineBorder(Color.red);
	}
	
	
	/** Called from constructor to create the textfield. */
	private JTextField createMyEditorComponent()	{
		JTextField editor = new JTextField(11);

		if (UIManager.getLookAndFeel().getName().equals("Metal") == false)
			editor.setBorder(null);	// else border too wide
		
		return editor;
	}
	

	/** Overridden: set original border and delegate to super. */
	public void setItem(Object item) {
		editor.setBorder(orig);	// assume error will be corrected
		super.setItem(item);	// calls toString()
	}


	/** Overridden: set original border, try to parse date input with given DateFormat. */
	public Object getItem() {
		editor.setBorder(orig);
		Calendar c = Calendar.getInstance();
		String s = editor.getText();
		
		if (s.length() > 0)	{	// empty string means: set it to null!
			try	{
				Date d = fmt.parse(s);
				c.setTime(d);
				
				if (withTime == false)	{
					c.set(Calendar.HOUR_OF_DAY, 0);
					c.set(Calendar.MINUTE, 0);
					c.set(Calendar.SECOND, 0);
					c.set(Calendar.MILLISECOND, 0);
				}
				else
				if (withSeconds == false)	{
					c.set(Calendar.SECOND, 0);
					c.set(Calendar.MILLISECOND, 0);
				}
				
				return new UneditableCalendarCombo.CalendarWrapper(c, false, fmt);
			}
			catch (ParseException e)	{
				e.printStackTrace();
				editor.setBorder(err);
			}
		}

		return new UneditableCalendarCombo.CalendarWrapper(c, true, fmt);
	}


	// test main
	/*
	public static void main(String [] args)	{
		//Locale.setDefault(Locale.US);

		DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		Calendar c = Calendar.getInstance();
		
		javax.swing.JComboBox comboBox = new javax.swing.JComboBox();
		comboBox.setEditable(true);
		
		comboBox.addItem(new CalendarCombo.CalendarWrapper(c, false, fmt));

		comboBox.setEditor(new SimpleCalendarComboEditor(fmt, true, false));

		javax.swing.JFrame f = new javax.swing.JFrame("Calendar ComboBox Editor");
		f.getContentPane().setLayout(new java.awt.FlowLayout());
		f.getContentPane().add(comboBox);
		f.getContentPane().add(new javax.swing.JButton("..."));
		
		f.pack();
		f.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		f.show();
	}
	*/
}
