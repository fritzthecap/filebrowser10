package fri.gui.swing.xmleditor.view;

import java.util.List;
import javax.swing.table.*;

/**
	Render lists of XML attributes as 'name1="value1", name2="value2" ...'.
	If a value is empty, the name is not rendered.
*/

public class ListTableRenderer extends DefaultTableCellRenderer
{
	/**
		Renders a list in a label by converting it to a comma-separated list of items.
	*/
	protected void setValue(Object value) {
		List l = getList(value);

		if (l != null)	{
			String s = "";
			
			for (int i = 0; i < l.size(); i++)	{
				Object o = l.get(i);
				String s1 = o != null ? o.toString() : "";
				
				if (s1.endsWith("=\"\"") == false)
					if (s.length() > 0)
						s = s+", "+s1;
					else
						s = s1;
			}

			super.setValue(s);
		}
		else	{
			super.setValue("");
		}
	}

	/**
		Returns a List from an Object, if it is instanceof List and has elements.
		If it is not a List or has no elements, return null.
	*/
	private static List getList(Object value)	{
		try	{
			List l = (List)value;
			if (l != null && l.size() > 0)
				return l;
		}
		catch (ClassCastException e)	{
			e.printStackTrace();
		}
		return null;
	}

}
