package fri.gui.swing.spinner;

import java.awt.event.AdjustmentEvent;
import javax.swing.JComponent;
import javax.accessibility.AccessibleContext;

/**
	A JScrollBar that scrolls infinitely. The attached AdjustmentListener
	gets value -1 for scroll up and +1 for scroll down.
*/

public class InfiniteSpinScrollBar extends FiniteSpinScrollBar
{
	/** Creates a scrollbar with zero range. */
	public InfiniteSpinScrollBar(JComponent editor)	{
		super(editor, 0, 0, 0);
	}

	/** Call super and fireSetValue(). */
	public void setValue(int value)	{
		//Thread.dumpStack();
		super.setValue(value);
		fireSetValue(value);
	}


	/** Overridden enable continued timer scrolling. */
	public int getMaximum()	{
		// condition for stop timer is: scrollbar.getValue() + scrollbar.getVisibleAmount() >= scrollbar.getMaximum()
		return getValue() + getVisibleAmount() + 1;
	}

	/** Overridden enable continued timer scrolling. */
	public int getMinimum()	{
		// condition for stop timer is: scrollbar.getValue() <= scrollbar.getMinimum()
		return getValue() - 1;
	}

	
	/** As there will be no model change, fire AdjustmentValueChanged here. */
	protected void fireSetValue(int value)	{
		// value will be -1 for up, +1 for down
		fireAdjustmentValueChanged(
				AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
				AdjustmentEvent.TRACK,
				value);

		if (accessibleContext != null) {
			accessibleContext.firePropertyChange(
					AccessibleContext.ACCESSIBLE_VALUE_PROPERTY,
					Integer.valueOf(value),
					Integer.valueOf(value));
		}
	}
	
}