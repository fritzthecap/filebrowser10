package fri.gui.swing.concordance.filter;

import java.util.*;
import fri.util.props.ClassProperties;
import fri.util.text.Trim;
import fri.util.concordance.ValidityFilter;
import fri.patterns.interpreter.expressions.*;
import fri.gui.mvc.model.DefaultModel;
import fri.gui.swing.expressions.*;

/**
	Text filter model that provides ValidityFilter functionality for concordance search.
	It evaluates text lines by some settings and a filter expression, configurable at runtime.
	<p>
	The default settings in member variables are made for Java source-code.
*/

public class FilterModel extends DefaultModel implements
	ValidityFilter
{
	private String filterTreeModelName = FilterTreePersistence.DEFAULT_FILTER_NAME;
	private int breakAfterCount;
	private int minimumLinesPerBlock;
	private boolean trimLines = true, normalizeLines = true;
	private String charsToRemove = "{};";
	private int charMinimum = 5;
	private StringBuffer sb = new StringBuffer(120);
	private Properties props;
	private FilterTreeModel filterTreeModel;
	

	public FilterModel()	{
		this(null);
	}
	
	public FilterModel(Properties props)	{
		if (props == null)
			props = ClassProperties.getProperties(FilterModel.class);

		this.props = props;
		
		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			String key = (String)e.nextElement();
			String value = props.getProperty(key);
			
			if (key.equals("filterTreeModelName"))
				setFilterTreeModelName(value);
			else
			if (key.equals("breakAfterCount"))
				setBreakAfterCount(Integer.parseInt(value));
			else
			if (key.equals("minimumLinesPerBlock"))
				setMinimumLinesPerBlock(Integer.parseInt(value));
			else
			if (key.equals("trimLines"))
				setTrimLines(value.equalsIgnoreCase("true"));
			else
			if (key.equals("normalizeLines"))
				setNormalizeLines(value.equalsIgnoreCase("true"));
			else
			if (key.equals("charsToRemove"))
				setCharsToRemove(value);
			else
			if (key.equals("charMinimum"))
				setCharMinimum(Integer.parseInt(value));
		}
	}

	/** Returns the loadable Properties after editing. */
	public Properties getProperties()	{
		props.setProperty("filterTreeModelName", getFilterTreeModelName());
		props.setProperty("breakAfterCount", ""+getBreakAfterCount());
		props.setProperty("minimumLinesPerBlock", ""+getMinimumLinesPerBlock());
		props.setProperty("trimLines", getTrimLines() ? "true" : "false");
		props.setProperty("normalizeLines", getNormalizeLines() ? "true" : "false");
		props.setProperty("charsToRemove", getCharsToRemove());
		props.setProperty("charMinimum", ""+getCharMinimum());

		return props;
	}
	
	/** Saves the Properties to persistence. */
	public void save()	{
		ClassProperties.setProperties(FilterModel.class, getProperties());
		ClassProperties.store(FilterModel.class);
		
		if (filterTreeModel != null)	{
			FilterTreeModelFactory.free();
			filterTreeModel = null;
		}
	}
	
	
	/** Implements ValidityFilter and checks with current parameters. */
	public Object isValid(Object o)	{
		String line = o.toString();
		
		if ((line = checkQuick(line)) == null)
			return null;
			
		if (charsToRemove != null && charsToRemove.length() > 0)	{
			boolean found = false;

			for (int i = 0; i < line.length(); i++)	{
				char c = line.charAt(i);
				
				if (charsToRemove.indexOf(c) < 0)
					sb.append(c);
				else
					found = true;
			}
			
			if (found)
				line = sb.toString();
			
			sb.setLength(0);
		}

		if ((line = checkQuick(line)) == null)
			return null;
		
		if (filterTreeModel == null)
			filterTreeModel = FilterTreeModelFactory.singleton().get(filterTreeModelName);

		Condition condition = (Condition) ((FilterTreeNode)filterTreeModel.getRoot()).getUserObject();

		Boolean b = (Boolean)condition.evaluate(line);
		//System.err.println("Condition returned "+b+" for >"+line+"< on expression: "+condition);
		if (b.booleanValue() == false)
			return null;
			
		return line;
	}

	private String checkQuick(String line)	{
		if (isEmptyLine(line))
			return null;
		
		if (trimLines)	{
			line = line.trim();
			if (isEmptyLine(line))
				return null;
		}
			
		if (normalizeLines)	{
			line = Trim.removeSpaceAmounts(line);
			if (isEmptyLine(line))
				return null;
		}
			
		return line;
	}

	private boolean isEmptyLine(String line)	{
		int len = line.length();
		if (len < charMinimum || charMinimum < 0 && len <= 0)
			return true;
		return false;
	}

	
	public String getFilterTreeModelName()	{
		return filterTreeModelName;
	}

	public void setFilterTreeModelName(String filterTreeModelName)	{
		this.filterTreeModelName = filterTreeModelName;
	}

	public int getBreakAfterCount()	{
		return breakAfterCount;
	}

	public void setBreakAfterCount(int breakAfterCount)	{
		this.breakAfterCount = breakAfterCount;
	}

	public int getMinimumLinesPerBlock()	{
		return minimumLinesPerBlock;
	}

	public void setMinimumLinesPerBlock(int minimumLinesPerBlock)	{
		this.minimumLinesPerBlock = minimumLinesPerBlock;
	}

	public boolean getTrimLines()	{
		return trimLines;
	}

	public void setTrimLines(boolean trimLines)	{
		this.trimLines = trimLines;
	}

	public boolean getNormalizeLines()	{
		return normalizeLines;
	}

	public void setNormalizeLines(boolean normalizeLines)	{
		this.normalizeLines = normalizeLines;
	}

	public String getCharsToRemove()	{
		return charsToRemove;
	}

	/** Checks for escaped characters within passed text and substitutes them: \t, \r, \f. */
	public void setCharsToRemove(String charsToRemove)	{
		// check for escaped tabulator or carriage-return
		StringBuffer sb = new StringBuffer(charsToRemove.length());
		for (int i = 0; i < charsToRemove.length(); i++)	{
			char c = charsToRemove.charAt(i);
			
			if (c == '\\' && i < charsToRemove.length() - 1)	{
				if (charsToRemove.charAt(i + 1) == 't')	{
					c = '\t'; i++;
				}
				else
				if (charsToRemove.charAt(i + 1) == 'r')	{
					c = '\r'; i++;
				}
				else
				if (charsToRemove.charAt(i + 1) == 'f')	{
					c = '\f'; i++;
				}
			}
			sb.append(c);
		}

		this.charsToRemove = sb.toString();
	}

	public int getCharMinimum()	{
		return charMinimum;
	}

	public void setCharMinimum(int charMinimum)	{
		this.charMinimum = charMinimum;
	}

}
