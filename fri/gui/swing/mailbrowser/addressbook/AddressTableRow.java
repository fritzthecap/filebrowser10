package fri.gui.swing.mailbrowser.addressbook;

import java.util.Vector;
import fri.gui.mvc.model.swing.DefaultTableRow;

public class AddressTableRow extends DefaultTableRow
{
	public AddressTableRow()	{
		super(AddressTableModel.COLUMN_COUNT);
		for (int i = 0; i < AddressTableModel.COLUMN_COUNT; i++)
			add("");
	}

	public AddressTableRow(Vector v)	{
		super(v);
	}

}
