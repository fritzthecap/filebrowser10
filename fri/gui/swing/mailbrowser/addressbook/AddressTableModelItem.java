package fri.gui.swing.mailbrowser.addressbook;

import java.util.Vector;
import fri.gui.mvc.model.swing.*;

public class AddressTableModelItem extends AbstractMutableTableModelItem
{
	public AddressTableModelItem(AddressTableRow row)	{
		super(row);
	}

	protected DefaultTableRow createTableRow(Vector v)	{
		return v == null ? new AddressTableRow() : new AddressTableRow(v);
	}
	
}
