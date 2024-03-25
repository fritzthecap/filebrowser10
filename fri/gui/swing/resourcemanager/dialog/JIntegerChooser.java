package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import javax.swing.*;
import fri.gui.swing.spinnumberfield.SpinNumberField;

public class JIntegerChooser extends JResourceChooser
{
	private SpinNumberField integerChooser;
	private String typeName;
	private Integer integer;
	private JPanel panel;
	private int min;
	
	public JIntegerChooser(Integer integer, String label, int min, int max, String typeName, boolean isComponentTypeBound, String componentTypeName)	{
		super(isComponentTypeBound, componentTypeName);
		this.integer = integer;
		this.min = min;
		this.typeName = typeName;

		integerChooser = new SpinNumberField(min, max);
		if (integer != null && integer.intValue() >= min && integer.intValue() <= max)
			integerChooser.setValue(integer.intValue());

		panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel(label));
		panel.add(integerChooser);
	}
	
	public Object getValue()	{
		int rh = (int) integerChooser.getValue();
		if (rh >= min)
			return new Integer(rh);
		return integer;
	}
	
	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the type passed in constructor. */
	public String getResourceTypeName()	{
		return typeName;
	}

}
