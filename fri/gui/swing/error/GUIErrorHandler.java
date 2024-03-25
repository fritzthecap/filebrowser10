package fri.gui.swing.error;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.*;
import fri.util.error.ErrorHandler;
import fri.util.text.LineSize;
import fri.gui.mvc.util.swing.EventUtil;

/**
	An ErrorHandler that shows GUI dialogs.
	
	@author  Ritzberger Fritz
*/

public class GUIErrorHandler implements ErrorHandler
{
	private static final int MAX_ROWS = 40, MAX_COLUMNS = 60;
	protected Component parent = null;
	protected StringBuffer errorBuffer = null;
	public static final String newline = System.getProperty("line.separator");
	

	/** Construct a handler showing dialogs for fatal, error and warning */	
	public GUIErrorHandler(Component parent)	{
		this.parent = parent;
	}

	
	private void show(final String title, String msg, final int type)	{
		int rows = LineSize.getLineCount(msg);
		int cols = LineSize.getMaximumLineLength(msg);
		rows = Math.min(MAX_ROWS, rows);
		if (rows < MAX_ROWS)
			rows += 2;
		cols = Math.min(MAX_COLUMNS, cols);
		
		JTextArea errorTextArea = new JTextArea(msg, rows, cols);
		errorTextArea.setEditable(false);
		errorTextArea.setCaretPosition(0);
		final JScrollPane scrollPane = new JScrollPane(errorTextArea);

		EventUtil.invokeSynchronous(new Runnable()	{
			public void run()	{
				JOptionPane.showMessageDialog(parent, scrollPane, title, type);
			}
		});

		if (errorBufferIsOk(msg))
			errorBuffer.append(msg+newline);
	}


	private boolean errorBufferIsOk(String msg)	{
		return (errorBuffer != null);	// && msg != errorBuffer.toString());
	}



	/** implements ErrorHandler */
	public boolean error(Throwable e)	{
		show("Error", e.toString(), JOptionPane.ERROR_MESSAGE);
		return false;	// prints stack trace
	}

	/** implements ErrorHandler */
	public boolean debug(String e)	{
		return false;	// pass to stderr
	}

	/** implements ErrorHandler */
	public boolean fatal(String e)	{
		show("Fatal Error", e, JOptionPane.ERROR_MESSAGE);
		return false;	// pass to stderr
	}

	/** implements ErrorHandler */
	public boolean warning(String e)	{
		show("Warning", e, JOptionPane.WARNING_MESSAGE);
		return false;	// pass to stderr
	}

	/** implements ErrorHandler */
	public boolean log(String e)	{
		if (errorBufferIsOk(e))
			errorBuffer.append(e+newline);
		return false;	// pass to stderr
	}

	/** implements ErrorHandler */
	public boolean logn(String e)	{
		if (errorBufferIsOk(e))
			errorBuffer.append(e);
		return false;	// pass to stderr
	}

	/** implements ErrorHandler */
	public boolean assertion(boolean condition)	{
		if (condition == false)	{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			new Exception("Assert - stack trace").printStackTrace(pw);
			String s = sw.toString();
			show("Assert", s, JOptionPane.ERROR_MESSAGE);
			throw new IllegalArgumentException("object is null");
		}
		return true;
	}

	/** implements ErrorHandler: caller wants to start a transaction */
	public void resetLog()	{
		errorBuffer = new StringBuffer();
	}

	/** implements ErrorHandler: return buffer contents and resolve error buffer */
	public String getLog()	{
		String s = (errorBuffer != null) ? errorBuffer.toString() : null;
		errorBuffer = null;
		return s;
	}

	/** implements ErrorHandler: set a new parent frame for dialogs */	
	public void setParentComponent(Object c)	{
		parent = (Component)c;
	}

	/** implements ErrorHandler: brings up a choice of items with a single selection. */	
	public Object choose(String title, List choice) {
		if (choice == null)
			return null;
		if (choice.size() <= 1)
			return choice.get(0);
		
		List clone = new ArrayList(choice);
		Collections.sort(clone);
		
		final JList list = new JList(clone.toArray());
		list.setSelectedIndex(0);
		JScrollPane scrollPane = new JScrollPane(list);
		JPanel message = new JPanel(new BorderLayout());
		JLabel label = new JLabel(title);
		label.setBorder(BorderFactory.createEmptyBorder(15, 4, 15, 4));
		message.add(label, BorderLayout.NORTH);
		message.add(scrollPane, BorderLayout.CENTER);
		
		JOptionPane dialogBuilder = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION);
		final JDialog dialog = dialogBuilder.createDialog(parent, "Please Choose ...");
		dialog.setResizable(true);
		dialog.pack();
		
		EventUtil.invokeSynchronous(new Runnable()	{
			public void run() {
				dialog.setVisible(true);
			}
		});
		
		return list.getSelectedValue();
	}
}