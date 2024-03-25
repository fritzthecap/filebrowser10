package fri.gui.swing.xmleditor.model;

import fri.gui.swing.treetable.TreeTableModel;
import fri.gui.swing.treetable.AbstractTreeTableModel;
import fri.util.props.PropertiesList;

/**
	XML specific implementation of tree table model.
	This class defines the columns of treetable.
	All insert/remove/edit actions must be done by
	this model.

	@author Ritzberger Fritz
*/

public class XmlTreeTableModel extends AbstractTreeTableModel
{
	public static final int TAG_COLUMN = 0;
	public static final int LONGTEXT_COLUMN = 1;
	public static final int ATTRIBUTES_COLUMN = 2;
	public static final String TAG_COLUMN_STRING = "Tag";
	public static final String LONGTEXT_COLUMN_STRING = "Text";
	public static final String ATTRIBUTES_COLUMN_STRING = "Attributes";
	
	private static String[] cNames = {
		TAG_COLUMN_STRING,
		LONGTEXT_COLUMN_STRING,
		ATTRIBUTES_COLUMN_STRING,
	};
	private static Class[]	cTypes = {
		TreeTableModel.class,
		String.class,
		PropertiesList.class,
	};
	private String [] localNames = cNames;	// for reduced column count



	/**
		Create a model with given root and these columns: tag - attributes - texts.
	*/
	public XmlTreeTableModel(XmlNode root) {
		super(root, true);
	}


	/** If root not shown (configured) and no attributes, reduce column count. */
	protected void checkForReducedColumns()	{
		XmlNode root = (XmlNode)getRoot();

		if (root.hasAnyAttributes() == false)	{
			localNames = new String [] {
				TAG_COLUMN_STRING,
				LONGTEXT_COLUMN_STRING
			};
			// localNames is for getColumnCount()
		}
	}



	// The TreeTableModel interface overriding

	/**
	 * Returns the number of columns.
	 */
	public int getColumnCount() {
		return localNames.length;
	}

	/**
	 * Returns the name for a particular column.
	 */
	public String getColumnName(int column) {
		return cNames[column];
	}

	/**
	 * Returns the class for the particular column.
	 */
	public Class getColumnClass(int column) {
		return cTypes[column];
	}

	/**
	 * Returns the value of the particular column.
	 */
	public Object getValueAt(Object node, int column) {
		XmlNode dn = (XmlNode)node;
		return dn.getColumnObject(column);
	}

	/**
	 * Returns true for attributes column, as there is a combobox that would not work.
	 */
	public boolean isCellEditable(Object node, int column) {
		return column == ATTRIBUTES_COLUMN;
	}

}
