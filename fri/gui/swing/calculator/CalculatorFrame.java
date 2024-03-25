package fri.gui.swing.calculator;

import java.awt.event.*;

import javax.swing.JFrame;
import fri.gui.awt.geometrymanager.GeometryManager;

/** The Calculator frame window. */

public class CalculatorFrame extends JFrame
{
	/** Baut den Rechner in diesem Frame auf. */
	public CalculatorFrame()	{
		super("Calculator");
		
		final CalculatorPanel calculatorPanel = new CalculatorPanel();
		getContentPane().add(calculatorPanel);
		new GeometryManager(this).show();
		
		// save splitpane geometry on close
		addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e) {
				calculatorPanel.close();
			}
		});
	}
	
	
	/** Bringt einen Rechner auf den Bildschirm. */
	public static void main(String [] args)	{
		new CalculatorFrame();
	}
}