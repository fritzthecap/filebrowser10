package fri.gui.awt.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import fri.gui.awt.resourcemanager.ResourceIgnoringContainer;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

public class FontChooser extends AwtResourceChooser implements
	ItemListener,
	ActionListener
{
	private Font font;
	private TextField testText;
	private Checkbox cbBold, cbItalic;
	private List familyChooser;
	private NumTextField sizeChooser;
	private FontView testView;
	private int style;
	protected int size = 12;
	private String family = "Dialog";
	private Panel panel;

	public FontChooser(Font font, boolean isComponentTypeBound, String componentTypeName)	{
		super(isComponentTypeBound, componentTypeName);
		this.font = font;
		build();
		init();
		listen();
	}

	protected void build()	{
		familyChooser = new List();
		testView = new FontView();
		testText = new TextField("Test");
		cbBold = new Checkbox("Bold");
		cbItalic = new Checkbox("Italic");
		sizeChooser = new NumTextField(size);

		Panel pStyle = new Panel(new GridLayout(2, 1));
		pStyle.add(cbBold);
		pStyle.add(cbItalic);

		Panel pEast = new Panel(new BorderLayout());
		pEast.add(sizeChooser, BorderLayout.NORTH);
		pEast.add(pStyle, BorderLayout.SOUTH);

		Panel pTest = new Panel(new BorderLayout());
		pTest.add(testText, BorderLayout.NORTH);
		pTest.add(testView, BorderLayout.CENTER);

		panel = new Panel(new BorderLayout());
		panel.add(familyChooser, BorderLayout.CENTER);
		panel.add(pEast, BorderLayout.EAST);
		panel.add(pTest, BorderLayout.SOUTH);
	}
	
	private void init()	{
		loadFonts();
		if (font != null)
			renderFont(font);
	}

	private void loadFonts()	{
		String [] families = getFontList();
		for (int i = 0; i < families.length; i++)
			addFamilyItem(families[i]);
	}
	
	protected void addFamilyItem(String familyName)	{
		familyChooser.add(familyName);
	}

	private String [] getFontList()	{
		String [] names = null;
		try	{
			GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
			names = genv.getAvailableFontFamilyNames();
		}
		catch (Throwable e)	{
			System.err.println("WARNUNG: "+e.getMessage());
		}

		if (names == null)
			names = Toolkit.getDefaultToolkit().getFontList();	// this is here for compatibility to Java 1.1

		return names;
	}
	
	protected void renderFont(Font font)	{
		testView.init(font, testText.getText());
		for (int i = 0; i < familyChooser.getItemCount(); i++)	{
			if (familyChooser.getItem(i).equals(font.getName()))	{
				familyChooser.select(i);
				familyChooser.makeVisible(i);
				break;
			}
		}
		sizeChooser.setValue(font.getSize());
		cbBold.setState(font.isBold());
		cbItalic.setState(font.isItalic());
	}
	
	protected void listen()	{
		familyChooser.addItemListener(this);
		testText.addActionListener(this);
		cbBold.addItemListener(this);
		cbItalic.addItemListener(this);
	}


	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the currently chosen font. */
	public Object getValue()	{
		return font;
	}
	
	/** Implements ResourceChooser: Returns ResourceFactory.FONT. */
	public String getResourceTypeName()	{
		return ResourceFactory.FONT;
	}



	private void setNewFont()	{
		font = new Font(family, style, size);
		showTestFont(font);
	}

	protected void showTestFont(Font font)	{
		testView.setFont(font);
	}
	
	
	/** Interface ItemListener: set new font according to user action. */
	public void itemStateChanged(ItemEvent e)	{
		settingChanged();
	}
	
	protected void settingChanged()	{
		size = getSelectedSize();
		family = getSelectedFamily();

		style = 0;
		if (isBoldActive())
			style |= Font.BOLD;
		if (isItalicActive())
			style |= Font.ITALIC;

		setNewFont();
	}

	protected String getSelectedFamily()	{
		return familyChooser.getSelectedItem();
	}
	
	protected int getSelectedSize()	{
		return sizeChooser.getValue();
	}
	
	protected boolean isBoldActive()	{
		return cbBold.getState();
	}
	
	protected boolean isItalicActive()	{
		return cbItalic.getState();
	}
	
	
	private void numValueChanged(int newValue)	{
		size = newValue;
		setNewFont();
	}


	/** Interface ActionListener: entered new font test text. */
	public void actionPerformed(ActionEvent e)	{
		testView.setString(testText.getText());
	}



	protected static class FontView extends Component implements ResourceIgnoringContainer
	{
		private Font font;
		private String exampleText = "Test";

		public void init(Font f, String exampleText)	{
			this.font = f;
			this.exampleText = exampleText;
		}
	
		public void setFont(Font f)	{
			this.font = f;
			repaint();
		}
	
		public void setString(String s)	{
			this.exampleText = s;
			repaint();
		}
	
		public void paint(Graphics g)	{
			if (font == null)	{
				super.paint(g);
			}
			else	{
				FontMetrics fm = g.getFontMetrics(font);
				g.setColor(Color.black);
				g.drawRect(1, 1, getSize().width-2, getSize().height-2);
				int x = (getSize().width - fm.stringWidth(exampleText)) / 2;
				int y = (getSize().height + fm.getAscent()) / 2;
				g.setColor(Color.black);
				g.setFont(font);
				g.drawString(exampleText, x, y);
			}
		}
	
		public Dimension getPreferredSize()	{
			return new Dimension (50, 80);
		}
		public Dimension getMinimumSize()	{
			return getPreferredSize();
		}
	}



	private class NumTextField extends Panel implements
		AdjustmentListener,
		ActionListener,
		KeyListener
	{
		private TextField tf;
		private Scrollbar sb;
		public int value;
	
		public NumTextField (int initValue)	{
			setLayout(new FlowLayout(FlowLayout.RIGHT));
			Panel p = new Panel(new BorderLayout());
			tf = new TextField(3);
			tf.addActionListener(this);
			tf.addKeyListener(this);
			sb = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, 80 + 1);
			sb.addAdjustmentListener(this);
			p.add(tf, BorderLayout.CENTER);
			p.add(sb, BorderLayout.SOUTH);
			add(new Label("Size"));
			add(p);
			setValue(initValue);
		}
	
		private void setValue(int newValue)	{
			value = newValue;
			tf.setText(String.valueOf(value));
			sb.setValue(value);
		}
	
		public int getValue()	{
			return value;
		}

		public void adjustmentValueChanged (AdjustmentEvent e)	{
			value = e.getValue();
			tf.setText(String.valueOf(value));
			FontChooser.this.numValueChanged(value);
		}
	
		// interface ActionListener
		public void actionPerformed (ActionEvent e)	{
			try	{
				value = Integer.valueOf(tf.getText()).intValue();
				sb.setValue(value);
				FontChooser.this.numValueChanged(value);
			}
			catch(Exception e1)	{
			}
		}
	
		/** Interface KeyListener: Read away all non-numeric keys. */
		public void keyPressed(KeyEvent e)	{
			if (e.isActionKey() == false &&
					e.getKeyCode() != KeyEvent.VK_ENTER && e.getKeyCode() != KeyEvent.VK_BACK_SPACE &&
					(e.getKeyCode() < KeyEvent.VK_0 || e.getKeyCode() > KeyEvent.VK_9))
				e.consume();
		}
		public void keyTyped(KeyEvent e)	{}
		public void keyReleased(KeyEvent e)	{}
	}



	// test main
	public static void main(String [] args)	{
		Frame f = new Frame("FontChooser");
		final FontChooser fc = new FontChooser(null, false, "button");
		f.add(fc.getPanel());
		f.setSize(300, 300);
		f.setVisible(true);
	}

}
