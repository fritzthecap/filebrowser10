package fri.gui.swing.mailbrowser.addressbook;

import java.util.*;
import fri.util.props.*;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.swing.AbstractMutableTableModel;
import fri.gui.swing.mailbrowser.Language;

public class AddressTableModel extends AbstractMutableTableModel
{
	public static final int PERSON_COLUMN = 0;
	public static final int EMAIL_COLUMN = 1;
	public static final int PHONE_COLUMN = 2;
	public static final int ADDRESS_COLUMN = 3;
	public static final int COLUMN_COUNT = 4;
	private static final Vector columnNames = new Vector(COLUMN_COUNT);
	private static final String entityType = "person";	// persistent entity type
	private static final String [] attributeNames = new String [] { "name", "email", "phone", "address" };	// persistent attribute names

	static	{
		columnNames.add(Language.get("Personal_Name"));
		columnNames.add(Language.get("E_Mail"));
		columnNames.add(Language.get("Phone"));
		columnNames.add(Language.get("Address"));
	};

	private static Vector addresses = TableProperties.convert(
			ClassProperties.getProperties(AddressTableModel.class),
			entityType,
			attributeNames);

	private static Vector convertToAddressTableRow(Vector v)	{
		for (int i = 0; i < v.size(); i++)	{
			Vector row = (Vector)v.get(i);
			v.set(i, new AddressTableRow(row));
		}
		return v;
	}


	public AddressTableModel()	{
		super(convertToAddressTableRow(addresses), columnNames);
	}
	

	/** Always returns String class. */
	public Class getColumnClass(int column)	{
		return String.class;
	}
	
	/** Always returns true as this table is fully editable. */
	public boolean isCellEditable(int row, int column)	{
		return true;
	}


	/** Stores the addresses to a property file. */
	public void save()	{
		ClassProperties.setProperties(
				AddressTableModel.class,
				TableProperties.convert(getDataVector(), entityType, attributeNames));
		ClassProperties.store(AddressTableModel.class);
	}
	

	// MVC framework
	
	/** Implements AbstractMutableTableModel: returns a ModelItem wrapper for a table row. */
	public ModelItem createModelItem(Vector row)	{
		return new AddressTableModelItem((AddressTableRow)row);
	}


	/** Returns the row at passed index or null if index is not in space. */
	public AddressTableRow getAddressTableRow(int index)	{
		return (AddressTableRow)getRow(index);
	}

}