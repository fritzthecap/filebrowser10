package fri.gui.awt.colorchoice;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

/** Klasse, um drei Scrollbars, mit denen Farbe eingestellt werden kann,
		verfügbar zu machen. Es kann ein Parent-Container (Panel) übergeben
		werden, in dem die Scrollbars dann angeordnet werden, oder sie
		können vom Aufrufer selbst eingefügt werden.
		<P>
		Je nach Einstellung können die Scrollbars die aktuelle Farbe
		selbst anzeigen oder einen farbigen Bezeichner
		("Sättigung", "Farbe", "Helligkeit") in ihren Scrollbereich zeichnen.
		<P>
		Weiters wird ein Popup-Menü hochgeklappt, wenn die rechte Maustaste
		über dem Grün- (Farbton-, links) Scrollbar gedrückt wird, das das
		Umschalten zwischen Beschriftung oder Farbanzeige zuläßt.
*/

public class ColorScrollbars
	implements AdjustmentListener, ActionListener, MouseListener, Serializable
{
	/** Ein möglicher Modus der Farbwahl.
			sb_red regelt Sättigung, sb_green Farbton und sb_blue die Helligkeit.
	*/
	public static final int HSB = 1;
	/** Ein möglicher Modus der Farbwahl.
			sb_red regelt Rotanteil, sb_green Grünanteil und sb_blue die Blauanteil.
	*/
	public static final int RGB = 0;

	/** Der rote Scrollbar (Sättigung im HSB-Modus) für Clients, die diesen selbst einfuegen wollen. */
	public Scrollbar sb_red;
	/** Der grüne Scrollbar (Farbton im HSB-Modus) für Clients, die diesen selbst einfuegen wollen. */
	public Scrollbar sb_green;
	/** Der blaue Scrollbar (Helligkeit im HSB-Modus) für Clients, die diesen selbst einfuegen wollen. */
	public Scrollbar sb_blue;

	/** Sollen die Scrollbars die eingestellte Farbe auch selbst anzeigen? */
	public boolean selfRend = true;

	private ColorRenderer darsteller;	// Renderer

	private Color color;	// eingestellte Farbe
	private int red, green, blue;

	private int mode = HSB;

	private float [] hsb = new float[3];

	// ä(ae)=\u00e4, ö(oe) = \u00f6, ü(ue)=\u00fc
	private static String satur = "Saturation";	//"S\u00e4ttigung";
	private static String hue = "Hue";
	private static String bright = "Brightness";
	private static String redstr = "Red";
	private static String greenstr = "Green";	//"Gr\u00fcn";
	private static String bluestr = "Blue";
	
	private static String labeledScrollbars = "Labeled Scrollbars";
	private static String colouredScrollbars = "Coloured Scrollbars";

	private PopupMenu popup;
	private MenuItem b_rgb, b_hsb, scrollfarbig;



	/** Funktionsloser Konstruktor. Grund: Serialisierung */
	public ColorScrollbars()	{
	}


	/** Konstruktor fuer Clients, die nicht selbst die Scrollbars einfuegen.
			@param fd Farb-Darsteller
			@param c anfangs einzustellende Farbe
			@param parent Container, in dem die Scrollbars eingefuegt werden sollen
			@param selfRend Kennung, ob Scrollbars selbst die Farbe darstellen sollen
	*/
	public ColorScrollbars(ColorRenderer fd, Color c, Container parent, boolean selfRend)	{
		init(fd, c, parent, HSB, selfRend);
	}


	/** Konstruktor fuer Clients, die nicht selbst die Scrollbars einfuegen.
			@param mode: Kennung, welches Farbmodell (RGB/HSB)
	*/
	public ColorScrollbars(ColorRenderer fd, Color c, Container parent, boolean selfRend, int mode)	{
		init(fd, c, parent, mode, selfRend);
	}


	/** Konstruktor fuer Clients, die nicht selbst die Scrollbars einfuegen.
	*/
	public ColorScrollbars(ColorRenderer fd, Color c, Container parent)	{
		this(fd, c, parent, HSB);
	}


	/** Konstruktor fuer Clients, die nicht selbst die Scrollbars einfuegen.
			Alle Parameter wie oben.
			@param mode Kennung, welches Farbmodell zu Beginn gesetzt werden soll (RGB/HSB)
	*/
	public ColorScrollbars(ColorRenderer fd, Color c, Container parent, int mode)	{
		init(fd, c, parent, mode, true);
	}


	/** Konstruktor fuer Clients, die selbst die Scrollbars einfuegen.
			@param redOrient Orientierung (Scrollbar.VERTICAL, .HORIZONTAL)
					des Rot-(Saturation-)Balkens
			@param greenOrient Scrollbar-Orientierung (Scrollbar.VERTICAL, .HORIZONTAL)
					des Gruen-(Hue-)Balkens
			@param blueOrient Scrollbar-Orientierung (Scrollbar.VERTICAL, .HORIZONTAL)
					des Blau-(Brightness-)Balkens
	*/
	public ColorScrollbars(ColorRenderer fd, Color initColor,
			int redOrient, int greenOrient, int bluOrient, int mode)	{
		init(fd, initColor, redOrient, greenOrient, bluOrient, mode);
	}


	/** Setzen von Einstellungen in den konstruierten Farbwähler. */
	public void init(ColorRenderer fd, Color c, Container parent,
			boolean selfRend)
	{
		init(fd, c, parent, HSB, selfRend);
	}


	private void init(ColorRenderer fd, Color c, Container parent,
		int mode, boolean selfRend)
	{
		this.selfRend = selfRend;

		if (parent.getLayout().toString().startsWith("java.awt.BorderLayout"))	{
			init(fd, c, Scrollbar.HORIZONTAL, Scrollbar.VERTICAL, Scrollbar.HORIZONTAL, mode);
			parent.add(sb_red, BorderLayout.NORTH);
			parent.add(sb_green, BorderLayout.WEST);
			parent.add(sb_blue, BorderLayout.SOUTH);
		}
		else
		if (parent.getLayout().toString().startsWith("java.awt.FlowLayout"))	{
			init(fd, c, Scrollbar.VERTICAL, Scrollbar.VERTICAL, Scrollbar.VERTICAL, mode);
			parent.add(sb_red);
			parent.add(sb_green);
			parent.add(sb_blue);
		}
		else
		if (parent.getLayout().toString().startsWith("java.awt.Grid"))	{
			init(fd, c, Scrollbar.VERTICAL, Scrollbar.VERTICAL, Scrollbar.VERTICAL, mode);
			Panel p = new Panel(new GridLayout(1, 3));
			p.add(sb_red);
			p.add(sb_green);
			p.add(sb_blue);
			parent.add(p);
		}
		else	{
			System.err.println("FEHLER: ColorScrollbars, unbekanntes Layout des parent-Containers: "+parent.getLayout());
		}
	}


	private void init(ColorRenderer fd, Color initColor,
			int redOrient, int greenOrient, int bluOrient, int mode)
	{
		this.darsteller = fd;
		this.mode = mode;
		this.color = initColor;

		// Scrollbars anlegen
		sb_red = new Scrollbar  (redOrient, 0, 20, 0, 255+20)	{
			public void paint(Graphics g)	{
				super.paint(g); paintRedBar(g);
			}
		};
		sb_green = new Scrollbar(greenOrient, 0, 20, 0, 255+20)	{
			public void paint(Graphics g)	{
				super.paint(g); paintGreenBar(g);
			}
		};
		sb_blue = new Scrollbar (bluOrient, 0, 20, 0, 255+20)	{
			public void paint(Graphics g)	{
				super.paint(g); paintBlueBar(g);
			}
		};

		createPopup();

		// Initialisierung der beiden Farbmodelle
		setColor(initColor);

		// Dem Darsteller die gesetzte Farbe bekanntgeben
		darsteller.changeColor(initColor);

		sb_red.addAdjustmentListener(this);
		sb_green.addAdjustmentListener(this);
		sb_blue.addAdjustmentListener(this);
	}


	private void createPopup()	{
		popup = new PopupMenu();
		// popup.setBackground(Color.white);	// gegen Farb-Vererbung wehren

		popup.add(b_rgb = new MenuItem (redstr+"/"+greenstr+"/"+bluestr));
		b_rgb.setActionCommand("RGB");
		b_rgb.addActionListener(this);
		popup.add(b_hsb = new MenuItem (satur+"/"+hue+"/"+bright));
		b_hsb.setActionCommand("HSB");
		b_hsb.addActionListener(this);
		popup.addSeparator();
		popup.add(scrollfarbig = new MenuItem (
				selfRend ? labeledScrollbars : colouredScrollbars));
		scrollfarbig.addActionListener(this);

		// Ein Scrollbar hat genau ein Popupmenu, sonst "XtGrabPointer failed"
		sb_green.add(popup);

		sb_green.addMouseListener(this);
	}


	/** Ändern der Einstellung, ob selbstdarstellend oder nicht. */
	public boolean toggleSelfRend()	{
		selfRend = !selfRend;
		scrollfarbig.setLabel(selfRend ? labeledScrollbars : colouredScrollbars);
		setScrollbarColors(color);
		return selfRend;
	}

	private void setMenuState()	{
		b_rgb.setEnabled(mode != RGB);
		b_hsb.setEnabled(mode != HSB);
	}


	/** Die Scrollbars beschriften. public wegen anonymous inner class */
	public void paintRedBar(Graphics g)	{
		String s = (mode == HSB) ? satur : redstr;
		if (sb_red.getOrientation() == Scrollbar.HORIZONTAL)
			paintHorizontalString(g, s, sb_red.getSize());
		else
			paintVerticalString(g, s, sb_red.getSize());
	}

	/** Die Scrollbars beschriften. public wegen anonymous inner class */
	public void paintGreenBar(Graphics g)	{
		String s = (mode == HSB) ? hue : greenstr;
		if (sb_green.getOrientation() == Scrollbar.HORIZONTAL)
			paintHorizontalString(g, s, sb_green.getSize());
		else
			paintVerticalString(g, s, sb_green.getSize());
	}

	/** Die Scrollbars beschriften. public wegen anonymous inner class */
	public void paintBlueBar(Graphics g)	{
		String s = (mode == HSB) ? bright : bluestr;
		if (sb_blue.getOrientation() == Scrollbar.HORIZONTAL)
			paintHorizontalString(g, s, sb_blue.getSize());
		else
			paintVerticalString(g, s, sb_blue.getSize());
	}

	/** Die Scrollbars beschriften. public wegen anonymous inner class */
	public void paintHorizontalString(Graphics g, String s, Dimension d)	{
		if (selfRend == false)	{
			FontMetrics fm = g.getFontMetrics();
			int x = (d.width - fm.stringWidth(s)) / 2;
			int y = (d.height + fm.getAscent()) / 2;
			g.drawString(s, x, y);
		}
	}

	/** Die Scrollbars beschriften. public wegen anonymous inner class */
	public void paintVerticalString(Graphics g, String s, Dimension d)	{
		if (selfRend == false)	{
			// System.err.println("paintVerticalString ...");
			FontMetrics fm = g.getFontMetrics();
			int fh = 2 * fm.getHeight() / 3;
			StringBuffer sb = new StringBuffer(s);
			char [] carr = new char [sb.length()];
			for (int i = 0; i < sb.length(); i++)	{
				sb.getChars(i, i+1, carr, 0);
				int x = (d.width - fm.charWidth(carr[0])) / 2;
				int y = d.height / 2 + (i - sb.length()/2) * fh;
				g.drawChars(carr, 0, 1, x, y);
			}
		}
	}


	/** Setzen einer anderen Farbe von aussen. Die Scrollbars bewegen sich zu neuen Positionen. */
	public void setColor(Color c)	{
		if (c == null)
			return;
		color = c;
		red = c.getRed();
		green = c.getGreen();
		blue = c.getBlue();
		hsb = Color.RGBtoHSB(red, green, blue, null);
		setMode(mode);	// Scrollbars bewegen zu neuen Werten
	}

	// Wechsle die Farbe, an alle Darsteller
	private void changeColor(Color c)	{
		color = c;
		darsteller.changeColor(c);
		// System.err.println("ColorScrollbars.changeColor("+c+")");
		setScrollbarColors(c);
	}

	private void setScrollbarColors(Color c)	{
		if (selfRend == false)	{
			// Um das Loeschen der Aufschriften zu vermeiden, setForeground() aufrufen
			sb_red.setForeground(c);
			sb_green.setForeground(c);
			sb_blue.setForeground(c);
			sb_red.setBackground(SystemColor.window);
			sb_green.setBackground(SystemColor.window);
			sb_blue.setBackground(SystemColor.window);
		}
		else	{	// selbst die Farbe darstellen
			if (mode == HSB)	{
				sb_red.setBackground(c);
				sb_green.setBackground(c);
				sb_blue.setBackground(c);
				Color fc = ColorUtil.getDrawColor(c);
				sb_red.setForeground(fc);
				sb_green.setForeground(fc);
				sb_blue.setForeground(fc);
			}
			else	{	// RGB
				sb_red.setBackground  (Color.getHSBColor((float)0/(float)3, hsb[1], hsb[2]));

				// Am gruenen Farbbalken ist das Popup-Menu befestigt, das die Farben erbt
				Color bc;
				sb_green.setBackground(bc = Color.getHSBColor((float)1/(float)3, hsb[1], hsb[2]));
				sb_green.setForeground(ColorUtil.getDrawColor(bc));
				// System.err.println("foreground: "+bc);

				sb_blue.setBackground (Color.getHSBColor((float)2/(float)3, hsb[1], hsb[2]));
			}
		}
	}


	/** Den Einstell-Modus (RGB, HSB) aendern.
			die Scrollbars werden zur eingestellten Farbe bewegt.
	*/
	public void setMode(int mode)	{
		this.mode = mode;
		setMenuState();

		if (mode == RGB)	{
			// Aktuelle Farbe umrechnen in neues Modell
			Color c = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
			sb_red.setValue(red = c.getRed());
			sb_green.setValue(green = c.getGreen());
			sb_blue.setValue(blue = c.getBlue());
		}
		else
		if (mode == HSB)	{
			hsb = Color.RGBtoHSB(red, green, blue, null);
			// Aktuelle Farbe umrechnen in neues Modell
			sb_red.setValue((int)(hsb[1] * 255.0 + 0.5));	// saturation
			sb_green.setValue((int)(hsb[0] * 255.0 + 0.5));	// hue
			sb_blue.setValue((int)(hsb[2] * 255.0 + 0.5));	// brightness
		}
		// changeColor(color);
		setScrollbarColors(color);
	}


	// interface AdjustmentListener
	public void adjustmentValueChanged(AdjustmentEvent e)	{
		// System.err.println("adjustmentValueChanged");
		if (mode == RGB)	{
			if (e.getSource() == sb_red)
				changeColor(new Color((red = e.getValue()), green, blue));
			else
			if (e.getSource() == sb_green)
				changeColor(new Color(red, (green = e.getValue()), blue));
			else
			if (e.getSource() == sb_blue)
				changeColor(new Color(red, green, (blue = e.getValue())));

			hsb = Color.RGBtoHSB(red, green, blue, null);
		}
		else	{	// HSB ist eingestellt
			Color c;
			if (e.getSource() == sb_red)	{	// saturation
				hsb[1] = (float)e.getValue() / (float)255;	// zwischen 0.0 und 1.0
			}
			else
			if (e.getSource() == sb_green)	{	// hue
				hsb[0] = (float)e.getValue() / (float)255;
			}
			else
			if (e.getSource() == sb_blue)	{	// brightness
				hsb[2] = (float)e.getValue() / (float)255;
			}
			changeColor(c = Color.getHSBColor(hsb[0], hsb[1], hsb[2]));

			red = c.getRed(); green = c.getGreen(); blue = c.getBlue();
		}
		// System.err.println("r="+red+", g="+green+", b="+blue);
		// System.err.println("h="+hsb[0]+", s="+hsb[1]+", b="+hsb[2]);
	}


	// interface ActionListener
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == scrollfarbig)	{
			toggleSelfRend();
		}
		else
		if (e.getActionCommand().equals("RGB"))	{
			setMode(RGB);
		}
		else
		if (e.getActionCommand().equals("HSB"))	{
			setMode(HSB);
		}
	}

	// interface MouseListener
	/** Wenn rechter Maus-Button gedrückt, das Popup-Menü aufklappen */
	public void mousePressed(MouseEvent e)	{
		// System.err.println("mousePressed");
		// if (e.isPopupTrigger())	// Funktioniert nicht in Windows
		int m = e.getModifiers();
		if ((m & InputEvent.BUTTON3_MASK) != 0 &&
				(m & InputEvent.META_MASK) != 0)	{
			popup.show((Component)e.getSource(), e.getX(), e.getY());
		}
	}
	public void mouseReleased(MouseEvent e)	{
		// System.err.println("mouseReleased");
	}
	public void mouseClicked(MouseEvent e)	{
		// System.err.println("mouseClicked");
	}
	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}
}