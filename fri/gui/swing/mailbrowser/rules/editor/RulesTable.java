package fri.gui.swing.mailbrowser.rules.editor;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.*;
import fri.util.ruleengine.PropertiesRuleExecutionSet;
import fri.gui.mvc.view.Selection;
import fri.gui.mvc.view.swing.TableSelectionDnd;
import fri.gui.swing.table.*;

public class RulesTable extends JPanel
{
	private JTable table;
	private Selection selection;
	
	public RulesTable()	{
		super(new BorderLayout());
		
		TableModel model = new RulesTableModel();
		table = new JTable(model);
		table.getTableHeader().setReorderingAllowed(false);
		
		table.setDefaultRenderer(String.class, new RulesTableCellRenderer());
		table.setDefaultRenderer(Vector.class, new RulesTableCellRenderer());
		// do not as drag&drop would not work: ((DefaultCellEditor)table.getDefaultEditor(String.class)).setClickCountToStart(1);
		
		// FRi: das funktioniert nicht:
		// table.setDefaultEditor(Vector.class, new ListTableCellEditor());

		TableColumnModel cm = table.getColumnModel();
		TableColumn c;
		c = cm.getColumn(PropertiesRuleExecutionSet.CONDITION_LOGIC);
		c.setCellEditor(new ListTableCellEditor());
		c = cm.getColumn(PropertiesRuleExecutionSet.CONDITION_FIELDNAME);
		c.setCellEditor(new ListTableCellEditor());
		c = cm.getColumn(PropertiesRuleExecutionSet.COMPARISON_METHOD);
		c.setCellEditor(new ListTableCellEditor());
		c = cm.getColumn(PropertiesRuleExecutionSet.ACTION_NAME);
		c.setCellEditor(new ListTableCellEditor());

		if (PersistentColumnsTable.load(getSensorComponent(), RulesTable.class) == false)	{	// nothing persistent
			c = cm.getColumn(PropertiesRuleExecutionSet.CONDITION_LOGIC);
			c.setPreferredWidth(50);
			c = cm.getColumn(PropertiesRuleExecutionSet.CONDITION_FIELDNAME);
			c.setPreferredWidth(70);
			c = cm.getColumn(PropertiesRuleExecutionSet.COMPARISON_METHOD);
			c.setPreferredWidth(70);
			c = cm.getColumn(PropertiesRuleExecutionSet.CONDITION_VALUE);
			c.setPreferredWidth(120);
			c = cm.getColumn(PropertiesRuleExecutionSet.ACTION_NAME);
			c.setPreferredWidth(70);
			c = cm.getColumn(PropertiesRuleExecutionSet.ACTION_ARGUMENT);
			c.setPreferredWidth(120);
		}
		
		add(new JScrollPane(table), BorderLayout.CENTER);
	}


	public JTable getSensorComponent()	{
		return table;
	}
	
	public Selection getSelection()	{
		if (selection == null)
			selection = new TableSelectionDnd(table);
		return selection;
	}


	/** Returns the model for message requests. */
	public RulesTableModel getModel()	{
		return (RulesTableModel)table.getModel();
	}
	
	public void close()	{
		PersistentColumnsTable.store(getSensorComponent(), RulesTable.class);
	}



	private class RulesTableCellRenderer extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean selected,
			boolean focus,
			int row,
			int col)
		{
			super.getTableCellRendererComponent(table, value, selected, focus, row, col);

			RulesTableRow ruleRow = (RulesTableRow) ((TableSelectionDnd)getSelection()).getRow(row);
			setEnabled(ruleRow.isMovePending() == false);
			
			int modelColumn = table.convertColumnIndexToModel(col);
			Object o = ruleRow.get(modelColumn);
			if (o instanceof Vector)
				setText((String) ((Vector)o).get(0));

			return this;
		}
	}

}
