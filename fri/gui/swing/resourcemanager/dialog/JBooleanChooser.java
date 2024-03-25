package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import javax.swing.*;

public class JBooleanChooser extends JResourceChooser
{
	private JCheckBox theBooleanChooser;
	private String typeName;
	private JPanel panel;
	
	public JBooleanChooser(Boolean theBoolean, String label, String typeName, boolean isComponentTypeBound, String componentTypeName)	{
		super(isComponentTypeBound, componentTypeName);
		this.typeName = typeName;

		theBooleanChooser = new JCheckBox(label, theBoolean == null ? false : theBoolean.booleanValue());

		panel = new JPanel(new GridBagLayout());
		panel.add(theBooleanChooser);
	}
	
	public Object getValue()	{
		return theBooleanChooser.isSelected() ? Boolean.TRUE : Boolean.FALSE;
	}
	
	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the type passed in constructor. */
	public String getResourceTypeName()	{
		return typeName;
	}

}
