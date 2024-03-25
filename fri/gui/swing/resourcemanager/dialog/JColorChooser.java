package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class JColorChooser extends JResourceChooser implements
	ChangeListener
{
	private Color color;
	private javax.swing.JColorChooser colorChooser;
	private JPanel panel;
	private String type;
	
	public JColorChooser(Color color, String type, boolean isComponentTypeBound, String componentTypeName)	{
		super(isComponentTypeBound, componentTypeName);
		this.type = type;
		this.color = color;
		this.colorChooser = color != null ? new javax.swing.JColorChooser(color) : new javax.swing.JColorChooser();
		colorChooser.getSelectionModel().addChangeListener(this);
		panel = new JPanel(new BorderLayout());
		panel.add(colorChooser, BorderLayout.CENTER);
	}
	

	public Object getValue()	{
		return color;
	}
	
	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the type passed in constructor. */
	public String getResourceTypeName()	{
		return type;
	}


	/** Implements ChangeListener: set membervariable color to chosen color. */
	public void stateChanged(ChangeEvent e) {
		color = colorChooser.getColor();
	}



	// test main
	public static final void main(String [] args)	{
		javax.swing.JFrame f = new javax.swing.JFrame("ColorChooser");
		JColorChooser fc = new JColorChooser(null, "Foreground", false, "button");
		f.getContentPane().add(fc.getPanel());
		f.pack();
		f.setVisible(true);
	}
	
}