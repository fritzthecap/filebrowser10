package fri.gui.mvc.view.swing;

import java.awt.event.*;
import java.awt.Component;

/**
	MouseListener that calls <i>actionPerformed()</i> on passed ActionListener
	for any JComponent it gets installed on. The action command contained
	in generated ActionEvent will be "open".
	<p>
	Usage:
	<pre>
		DoubleClickListener.install(jTable, actionController);
	</pre>
	
	@author Fritz Ritzberger, 2003
*/

public class DoubleClickListener extends MouseAdapter
{
	private Component sensor;
	private ActionListener callback;
	
	/** Create a MouseListener that shows the passed popup on popup events. */
	public DoubleClickListener(Component sensor, ActionListener callback)	{
		if (callback == null || sensor == null)
			throw new IllegalArgumentException("DoubleClickMouseListener not installed correctly: sensor or callback is null!");
			
		this.callback = callback;
		this.sensor = sensor;
	}


	/** Implements MouseListener to catch popup event. */
	public void mouseClicked (MouseEvent e)	{
		if (e.getClickCount() >= 2)
			callback.actionPerformed(new ActionEvent(sensor, ActionEvent.ACTION_PERFORMED, "open"));
	}


	/** Creates a DoubleClickMouseListenr and installs it on passed sensor JComponent. */
	public static void install(Component sensor, ActionListener callback)	{
		sensor.addMouseListener(new DoubleClickListener(sensor, callback));
	}

}
