package fri.gui.swing.combo;

import java.util.Vector;
import java.awt.FontMetrics;
import java.awt.*;
import javax.swing.*;

/**
	Eine ComboBox, die ihre Hoehe nicht dem Container anpasst.
	Die ComboBox kann allerdings diese Eigenschaft in einem
	BorderLayout nicht durchsetzen.
	
	@author  Ritzberger Fritz
*/

public class ConstantHeightComboBox extends JComboBox
{
	public ConstantHeightComboBox()	{
		super();
	}

	public ConstantHeightComboBox(Vector items)	{
		super(items);
	}

	public ConstantHeightComboBox(String [] items)	{
		super(items);
	}


	public Dimension getMaximumSize()	{
		FontMetrics fm = getFontMetrics(getFont());
		int h = fm.getAscent() + fm.getDescent();
		Dimension d = super.getMaximumSize();
		d.height = Math.max(24, h);
		return d;
	}	

}
