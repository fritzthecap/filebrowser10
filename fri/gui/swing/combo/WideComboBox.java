package fri.gui.swing.combo;

import java.util.Vector;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import fri.gui.swing.BugFixes;

/**
	Eine ComboBox, die ihre Popup-Liste so breit macht, wie der
	laengste enthaltene String ist. Dadurch sieht man z.B.
	in einer JTable den ganzen Inhalt, sonst ist er durch
	die Spaltenbreite eingeschraenkt.
	Weiters <b>erzwingt</b> diese ComboBox nicht seine Breite (naemlich die
	des laengstene Strings), indem sie getMinimumSize() ueberschreibt.
	<p>
	Voraussetzung fuer diese Klasse ist allerdings der Aufruf der
	Methode takePopupSize() unmittelbar nach dem Befuellen der Combo mit Items,
	damit die Maximalbreite festgestellt wird, sobald alle Items bekannt sind.

	@author  Ritzberger Fritz
*/

public class WideComboBox extends JComboBox
{
	/* Set this variable after the ComboBox has been filled to combo.set. */
	private int width = 0;

	/** Constructor calling super(). */
	public WideComboBox()	{
		super();
	}

	/** Constructor calling super(items) and takePopupSize() */
	public WideComboBox(Vector items)	{
		super(items);
		takePopupSize();
	}

	/** Constructor calling super(items) and takePopupSize(). */
	public WideComboBox(String [] items)	{
		super(items);
		takePopupSize();
	}

	/**
		Overridden to return 40/0. (When not implemented, the ComboBox
		takes as much space as the largest contained String needs!)
	*/
	public Dimension getMinimumSize()	{
		return new Dimension(40, 0);
	}	

	/** Overridden to set the preferred width that was set by takePopupSize(). */
	public Dimension getSize()	{
		Dimension d = super.getSize();
		if (width <= 0 || d.height <= 0)	// takePopupSize() was not called, or invalid
			return d;

		int myWidth = Math.max(width, d.width);	// do not make smaller than ComboButton
		d.width = myWidth + 4;
		return d;
		//return new Dimension(myWidth + 4, d.height);
	}
	
	/** Overridden to avoid "xxx..." appearance of JComboBox. */
	public Dimension getPreferredSize()	{
		Dimension d = super.getPreferredSize();
		d.width += 4;
		return d;
	}
	
	/** Call this each time after the Combo has been filled with items! */
	public void takePopupSize()	{
		this.width = getPreferredSize().width;
	}

	/** Overridden to set a private size/location aware UI. */
	public void updateUI()	{
		//setUI(createComboBoxUI());
		setUI(new WideComboBoxUI());
	}
	
	
	protected ComboPopup createComboPopupUI()	{
		ComboPopup popup = new WideComboPopup(this);
		return popup;
	}
	
	
	// BasicComboBoxUI computes rubbish for WideComboBox
	protected class WideComboBoxUI extends BasicComboBoxUI
	{
		/** Create a popup that computes a good location. */
		protected ComboPopup createPopup()	{
			return createComboPopupUI();
		}
	}

	public class WideComboPopup extends BasicComboPopup
	{
		public WideComboPopup(JComboBox comboBox)	{
			super(comboBox);
		}
		
		/** Show at properly computed location. */
		public void show(Component invoker, int x, int y)	{
			Point p = BugFixes.computePopupLocation(x, y, invoker, this, WideComboBox.this.getSize().height);
			super.show(invoker, p.x, p.y);
		}
		
		/** Create list scroller with both scrollbars. */
		protected JScrollPane createScroller()	{
			return new JScrollPane(
					list,
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		}
	}


}