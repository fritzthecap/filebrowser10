package fri.gui.awt.resourcemanager.dialog;

import java.awt.*;

/** Any Panel that extends this class can be a GUI for customizing one Resurce like Font, Color, ... */

public abstract class AwtResourceChooser implements
	ResourceChooser
{
	private Panel panel;
	private Checkbox checkbox;

	protected AwtResourceChooser()	{
	}
	
	protected AwtResourceChooser(boolean isSelected, String componentTypeName)	{
		checkbox = new Checkbox("For All \""+componentTypeName+"\"", isSelected);
	}

	/** Returns true if this Resource should apply to all instances of Button, Label, ... */
	public boolean isComponentTypeBound()	{
		return checkbox != null ? checkbox.getState() : false;
	}

	/** Returns the addable panel of this resource chooser. */
	public Container getPanel()	{
		if (panel == null)	{
			panel = new Panel(new BorderLayout());
			panel.add(getChooserPanel(), BorderLayout.CENTER);
			if (checkbox != null)
				panel.add(checkbox, BorderLayout.SOUTH);
		}
		return panel;
	}

	/** Subclasses must place their content within the panel returned from here. */
	protected abstract Component getChooserPanel();

	protected Checkbox getCheckbox()	{
		return checkbox;
	}

}
