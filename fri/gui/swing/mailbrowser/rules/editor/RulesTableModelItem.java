package fri.gui.swing.mailbrowser.rules.editor;

import java.util.Vector;
import fri.gui.mvc.model.swing.*;

public class RulesTableModelItem extends AbstractMutableTableModelItem
{
	public RulesTableModelItem(RulesTableRow row)	{
		super(row);
	}

	protected DefaultTableRow createTableRow(Vector v)	{
		return v == null
				? new RulesTableRow()
				: new RulesTableRow((RulesTableRow)v);	// force RulesTableRow constructor for cloning without Language translation
	}

}
