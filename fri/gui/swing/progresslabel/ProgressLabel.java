package fri.gui.swing.progresslabel;

import java.awt.*;
import javax.swing.*;

/**
	Ein rotierender Strich.
	Mit jedem Aufruf der Methode progress() wird der Strich gedreht.
*/

public class ProgressLabel extends JLabel 
{
	private int progress = 0;

	public ProgressLabel()	{
		this(" ");
	}

	public ProgressLabel(String init)	{
		super(init, SwingConstants.CENTER);
		//setFont(getFont().deriveFont(Font.BOLD));
	}

	public Dimension getPreferredSize()	{
		return new Dimension(28, 16);
	}
	public Dimension getMaximumSize()	{
		return getPreferredSize();
	}
	public Dimension getMinimumSize()	{
		return getPreferredSize();
	}

	/**
		Setzt das Label auf ein Space (leer).
	*/
	public /*synchronized*/ void clear()	{
		setText(" ");
	}

	/**
		Rotiert den Strich weiter.
	*/
	public /*synchronized*/ void progress()	{
		switch (progress)	{
			case 0: setText("|"); break;
			case 1: setText("/"); break;
			case 2: setText("-"); break;
			case 3: setText("\\"); break;
		}
		progress = (progress + 1) % 4;
	}
}