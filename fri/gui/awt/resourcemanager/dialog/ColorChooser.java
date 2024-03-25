package fri.gui.awt.resourcemanager.dialog;

import java.io.Serializable;
import java.awt.*;
import java.awt.event.*;
import fri.gui.awt.colorchoice.*;
import fri.gui.awt.resourcemanager.ResourceIgnoringContainer;

public class ColorChooser extends AwtResourceChooser implements
	ActionListener,
	ColorRenderer,
	ResourceIgnoringContainer
{
	private Color color;
	private ColorCell [] colors = new ColorCell[256];
	private ColorMixer colorMixer;
	private TextField colorText;
	private Label capt;
	private String type;
	private Panel panel;

	public ColorChooser(Color c, String type, boolean isComponentTypeBound, String componentTypeName)	{
		super(isComponentTypeBound, componentTypeName);
		
		this.color = c;
		this.type = type;

		loadColors();
		Panel p = new Panel(new BorderLayout());
		Panel p1 = new Panel(new BorderLayout());
		capt = new Label(type);
		p1.add(capt, BorderLayout.WEST);
		colorText = new TextField(7);
		colorText.setText(ColorCell.toString(c));
		p1.add(colorText, BorderLayout.CENTER);
		colorText.addActionListener(this);
		p.add(p1, BorderLayout.NORTH);
		colorMixer = new ColorMixer();
		p.add(colorMixer, BorderLayout.CENTER);
		colorMixer.init(this, c);
		Panel pColor = buildColorBar();
		p.add(pColor, BorderLayout.SOUTH);
		
		panel = p;
	}


	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the currently chosen value (Font, Color, ...). */
	public Object getValue()	{
		return color;
	}
	
	/** Implements ResourceChooser: Returns ResourceFactory.FOREGROUND or ResourceFactory.BACKGROUND. */
	public String getResourceTypeName()	{
		return type;
	}


	private void loadColors()	{
		colors[0] = new ColorCell("red");
		colors[1] = new ColorCell("orange");
		colors[2] = new ColorCell("yellow");
		colors[3] = new ColorCell("green");
		colors[4] = new ColorCell("cyan");
		colors[5] = new ColorCell("blue");
		colors[6] = new ColorCell("magenta");
		colors[7] = new ColorCell("pink");
		colors[8] = new ColorCell("white");
		colors[9] = new ColorCell("lightgray");
		colors[10] = new ColorCell("gray");
		colors[11] = new ColorCell("darkgray");
		colors[12] = new ColorCell("black");
	}

	private Panel buildColorBar()	{
		Panel p = new Panel();
		int i;	// Farbfeld abzaehlen
		for (i = 0; i < colors.length && colors[i] != null; i++)
			;
		p.setLayout(new GridLayout(1, i));
		for (i = 0; i < colors.length && colors[i] != null; i++)	{
			Button b;
			p.add(b = new Button());
			b.setBackground(colors[i].color);
			b.addActionListener(this);
		}
		return p;
	}

	/** interface ColorRenderer, Feinabstimmung ueber Farbfeld. */
	public void changeColor(Color c)	{
		this.color = c;
		colorText.setText(ColorCell.toString(c));
	}

	// interface ActionListener, Grobauswahl ueber Buttons
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == colorText)	{
			String s = colorText.getText();
			Color c = null;
			if ((c = ColorCell.toColor(s)) != null)	{
				changeColor(c);
				colorMixer.setColor(c);
			}
		}
		else	{
			Color c = ((Button)e.getSource()).getBackground();
			changeColor(c);
			colorMixer.setColor(c);
		}
	}



	protected static class ColorMixer extends Panel implements
		ColorRenderer
	{
		private ColorRenderer farbDarsteller;
		private ColorScrollbars farbWaehler;	// Drei Scrollbars und ihr Listener
		private ColorPot farbFeld;

		public ColorMixer()	{
			setLayout(new BorderLayout());
			farbFeld = new ColorPot();
			add(farbFeld, BorderLayout.CENTER);
			farbWaehler = new ColorScrollbars();
		}
	
		public ColorMixer(ColorRenderer farbDarsteller, Color farbe)	{
			this();
			init(farbDarsteller, farbe);
		}
	
		public void init(ColorRenderer farbDarsteller, Color farbe)	{
			this.farbDarsteller = farbDarsteller;
			farbWaehler.init(this, farbe, this, true);
		}
	
		/** Event comes from ColorChooser. */
		public void setColor(Color c)	{
			farbWaehler.setColor(c);
			farbFeld.setColor(c);
		}
	
		/** interface ColorRenderer, Event kommt von Scrollbars */
		public void changeColor(Color c)	{
			farbDarsteller.changeColor(c);
			farbFeld.setColor(c);
		}
	
		public Dimension getPreferredSize()	{
			return new Dimension(50, 80);
		}
		public Dimension getMinimumSize()	{
			return getPreferredSize();
		}
	}


	protected static class ColorPot extends Canvas
	{
		public void paint(Graphics g)	{
			g.fillOval(0, 0, getSize().width - 2, getSize().height - 2);
			g.setColor(Color.black);
			g.drawOval(0, 0, getSize().width - 2, getSize().height - 2);
		}
	
		public void setColor(Color c)	{
			setForeground(c);
			repaint();
		}
	}


	/**
		Zuordnung von Strings zu Farben und umgekehrt,
		um die vorgegebenen Class Color Farben verwenden zu koennen.
	*/
	protected static class ColorCell implements Serializable
	{
		public String string = null;
		public Color color;
	
		public ColorCell(String s)	{
			string = s;
			color = toColor(s);
		}
	
		public static final String toString(Color c)	{
			if (c == null)
				return "";
			String s = Integer.toHexString(c.getRGB() & 0x00FFFFFF);
			while (s.length() < 6)	{
				s = "0"+s;
			}
			return "#"+s;
		}
	
		public static final Color toColor(String s)	{
			Color c = null;
			if      (s.equals("yellow"))
				c = Color.yellow;
			else if (s.equals("blue"))
				c = Color.blue;
			else if (s.equals("green"))
				c = Color.green;
			else if (s.equals("red"))
				c = Color.red;
			else if (s.equals("orange"))
				c = Color.orange;
			else if (s.equals("magenta"))
				c = Color.magenta;
			else if (s.equals("cyan"))
				c = Color.cyan;
			else if (s.equals("pink"))
				c = Color.pink;
			else if (s.equals("black"))
				c = Color.black;
			else if (s.equals("white"))
				c = Color.white;
			else if (s.equals("gray"))
				c = Color.gray;
			else if (s.equals("darkgray"))
				c = Color.darkGray;
			else if (s.equals("lightgray"))
				c = Color.lightGray;
			else	{
				try { // Farbe in numerischer Schreibweise angegeben
					c = Color.decode(s);
				}
				catch (NumberFormatException e) {
					System.err.println("FEHLER: nicht erkannte Farbe: "+s);
				}
			}
			return c;
		}
	}


	// test main
	public static void main(String [] args)	{
		Frame f = new Frame("ColorChooser");
		final ColorChooser cc = new ColorChooser(null, "Foreground", false, "button");
		f.add(cc.getPanel());
		f.addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				System.exit(0);
			}
		});
		f.setSize(300, 300);
		f.setVisible(true);
	}

}