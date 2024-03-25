package fri.gui.swing.resourcemanager.dialog;

import java.awt.Container;
import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import fri.gui.awt.resourcemanager.dialog.AwtResourceChooser;

/** Any Panel that extends this class can be a GUI for customizing one Resurce like Font, Color, ... */

public abstract class JResourceChooser extends AwtResourceChooser
{
	private JCheckBox checkbox;
	private JPanel panel;

	protected JResourceChooser()	{
	}
	
	protected JResourceChooser(boolean isSelected, String componentTypeName)	{
		checkbox = new JCheckBox("For All \""+componentTypeName+"\"", isSelected);
	}

	/** Returns true if this Resource should apply to all instances of JButton, JLabel, ... */
	public boolean isComponentTypeBound()	{
		return checkbox != null ? checkbox.isSelected() : false;
	}

	/** Returns the addable panel of this resource chooser. */
	public Container getPanel()	{
		if (panel == null)	{
			panel = new JPanel(new BorderLayout());
			panel.add(getChooserPanel(), BorderLayout.CENTER);
			if (checkbox != null)	{
				JPanel p = new JPanel();
				p.add(checkbox);
				panel.add(p, BorderLayout.SOUTH);
			}
		}
		return panel;
	}

}
