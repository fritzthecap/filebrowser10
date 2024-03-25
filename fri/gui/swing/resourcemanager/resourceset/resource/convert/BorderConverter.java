package fri.gui.swing.resourcemanager.resourceset.resource.convert;

import java.util.*;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.border.*;
import fri.util.Equals;
import fri.util.i18n.MultiLanguageString;
import fri.util.props.NestableProperties;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.*;

/**
	Encapsulates methods to convert (String - Object) a Border resource.
	The converted borders can be of all types except MatteBorder or
	CompoundBorders with more than two parts.
	<p>
	The border gets encoded using NestedProperties class, to pack a
	multilanguage title and a border name into one property value.
	It there is a title, the persistentce string starts with ">".
	If ther is no title and it is a LineBorder, the thickness
	and color is given by "-" separators after the border name.
*/

public class BorderConverter extends AbstractConverter
{
	public static final String [] borderNames = {
		"(None)",
		"Line",
		"EtchedLowered",
		"EtchedRaised",
		"Lowered",
		"Raised",
		"Lowered+Raised",
		"Raised+Lowered",
		"SoftLowered",
		"SoftRaised",
		"SoftLowered+SoftRaised",
		"SoftRaised+SoftLowered",
	};
	
	public static class BorderAndTitle
	{
		public final Border border;
		public final MultiLanguageString title;
		
		public BorderAndTitle(Border border, MultiLanguageString title)	{
			this.border = border;
			this.title = title;
		}
		
		public boolean equals(Object o)	{
			BorderAndTitle other = (BorderAndTitle)o;
			return Equals.equals(other.border, border) && Equals.equals(other.title, title);
		}
	}
	

	/** Returns the Swing Border or null. */
	public Object toGuiValue(Object value, Object component)	{
		if (value == null)
			return null;

		BorderAndTitle bt = (BorderAndTitle) value;
		if (bt.title == null)
			return bt.border;

		// when having title, always force language by building it newly
		bt = (BorderAndTitle) stringToObject(objectToString(value));
		return bt.border;
	}

	public Class getGuiValueClass(Object component)	{
		return Border.class;
	}


	
	/** Turn the border into a a persistence string. */
	public String objectToString(Object border)	{
		if (border == null)
			return null;
			
		if (border instanceof Border)	{
			Border b = (Border)border;
			return borderToString(b);
		}
		else	{
			BorderAndTitle bt = (BorderAndTitle)border;
			return borderToString(bt.border, bt.title);
		}
	}

	private String borderToString(Border b, Object title)	{
		if (b instanceof LineBorder)	{
			LineBorder lb = (LineBorder)b;
			int thickness = lb.getThickness();
			Color color = lb.getLineColor();
			return "Line-"+thickness+"-"+new ColorConverter().objectToString(color);
		}

		if (b instanceof TitledBorder)	{
			TitledBorder tb = (TitledBorder)b;
			Border brd = tb.getBorder();
			String spec = borderToString(brd);
			if (spec.startsWith(">"))
				throw new IllegalArgumentException("TitledBorder must not contain another titled border!");

			if (title == null)
				title = tb.getTitle() != null ? tb.getTitle() : "";
			String packedTitle = new TextConverter().objectToString(title);

			Properties np = new NestableProperties();
			np.setProperty("border", spec);
			np.setProperty("title", packedTitle);
			return ">"+np.toString();
		}

		if (b instanceof EtchedBorder)	{
			EtchedBorder eb = (EtchedBorder)b;
			if (eb.getEtchType() == EtchedBorder.RAISED)
				return "EtchedRaised";
			if (eb.getEtchType() == EtchedBorder.LOWERED)
				return "EtchedRaised";
		}

		if (b instanceof CompoundBorder)	{
			CompoundBorder cb = (CompoundBorder)b;
			String outer = borderToString(cb.getOutsideBorder());
			String inner = borderToString(cb.getInsideBorder());
			if (inner == null || outer == null)	{	// error on JComponentChoiceGUI
				//System.err.println("Could not convert border: "+b);
				return null;
			}
			if (inner.startsWith(">") || outer.startsWith(">"))
				throw new IllegalArgumentException("CompoundBorder must not contain a titled border!");
				
			return outer+"+"+inner;
		}

		if (b instanceof BevelBorder)	{
			BevelBorder bb = (BevelBorder)b;
			if (b instanceof SoftBevelBorder)	{
				if (bb.getBevelType() == BevelBorder.RAISED)
					return "SoftRaised";
				if (bb.getBevelType() == BevelBorder.LOWERED)
					return "SoftLowered";
			}
			if (bb.getBevelType() == BevelBorder.RAISED)
				return "Raised";
			if (bb.getBevelType() == BevelBorder.LOWERED)
				return "Lowered";
		}

		return null;
	}

	private String borderToString(Border b)	{
		return borderToString(b, null);
	}
	


	/** Turn a persistence string into a border. */
	public Object stringToObject(String spec)	{
		if (spec == null || spec.length() <= 0 || spec.equals("(None)") || spec.equals("null"))
			return null;
		
		int thickness = 1;
		Color color = Color.black;
		MultiLanguageString title = null;
		if (spec.startsWith(">"))	{	// contains title text resource
			Properties p = new NestableProperties(spec.substring(">".length()));
			spec = p.getProperty("border");
			title = (MultiLanguageString) new TextConverter().stringToObject(p.getProperty("title"));
		}
		
		if (spec.startsWith("Line-"))	{
			StringTokenizer stok = new StringTokenizer(spec, "-");
			for (int j = 0; stok.hasMoreTokens(); j++)	{
				String s = stok.nextToken();
				switch (j)	{
					case 1:
						try	{ thickness = Integer.parseInt(s); }
						catch (Exception e)	{ e.printStackTrace(); }
						break;
					case 2:
						color = (Color) new ColorConverter().stringToObject(s);
						break;
				}
			}
			spec = "Line";
		}
		
		Border b = stringToBorder(spec, thickness, color, title);
		return new BorderAndTitle(b, title);
	}

	/** Returns a Border built from passed values, needed in dialog. @param b name of border as in exposed String array. */
	public Border stringToBorder(
		String b,
		int thickness,
		Color color,
		Object title)
	{
		if (title != null)	{
			Border brd = stringToBorder(b, thickness, color);
			return BorderFactory.createTitledBorder(brd, title.toString());
		}
		return stringToBorder(b, thickness, color);
	}
	
	private Border stringToBorder(String b, int thickness, Color color)	{
		if (b.equals("Line"))
			return BorderFactory.createLineBorder(color, thickness);
		return stringToBorder(b);
	}
	
	private static Border stringToBorder(String b)	{
		if (b.indexOf("+") > 0)	{
			String outer = b.substring(0, b.indexOf("+"));
			String inner = b.substring(b.indexOf("+") + 1);
			return BorderFactory.createCompoundBorder(
					stringToBorder(outer),
					stringToBorder(inner));
		}
		if (b.equals("(None)"))
			return null;
		if (b.equals("Line"))
			return BorderFactory.createLineBorder(Color.black);
		if (b.equals("EtchedLowered"))
			return BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		if (b.equals("EtchedRaised"))
			return BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		if (b.equals("SoftRaised"))
			return new SoftBevelBorder(BevelBorder.RAISED);
		if (b.equals("SoftLowered"))
			return new SoftBevelBorder(BevelBorder.LOWERED);
		if (b.equals("Raised"))
			return BorderFactory.createRaisedBevelBorder();
		if (b.equals("Lowered"))
			return BorderFactory.createLoweredBevelBorder();
		return null;
	}


	/** Returns the symbolic name of the border, e.g. "Line" from "Line-4-#12345678". */
	public String getChoosableBorderName(BorderAndTitle b)	{
		if (b == null)
			return "(None)";
			
		String spec = objectToString(b);
		if (spec.startsWith(">"))	{	// contains title text resource
			Properties p = new NestableProperties(spec.substring(">".length()));
			spec = p.getProperty("border");
		}
		if (spec.startsWith("Line-"))
			return spec.substring(0, spec.indexOf("-"));
		return spec;
	}

}
