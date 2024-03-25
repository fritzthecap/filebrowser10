package fri.gui.swing.expressions;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import fri.gui.swing.ComponentUtil;
import fri.patterns.interpreter.expressions.*;

/**
	Tree cell editor that lets edit an AbstractCondition (LogicalCondition, Comparison).
*/

public class FilterTreeCellEditor extends DefaultTreeCellEditor
{
	private AbstractCondition condition;
	private FilterTreeNode node;
	private JTree tree;	// for ListTableComboRenderer
	private JPanel logicPanel, comparisonPanel;
	private JLabel logicLabel1, logicLabel2;
	private JComboBox fieldNameCombo;
	private JComboBox operatorCombo;
	private JComboBox contentCombo;
	private JTextField compareValueTextField;
	private JComboBox compareValueCombo;
	
	private ActionListener commitActionListener = new ActionListener()	{
		public void actionPerformed(ActionEvent e)	{
			stopCellEditing();
		}
	};


	public FilterTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
		super(tree, renderer);
	}


	public boolean stopCellEditing()	{
		getCondition();	// save data to expression
		return super.stopCellEditing();
	}

	public Object getCellEditorValue()	{
		return condition;
	}

	public Component getTreeCellEditorComponent(
		JTree tree,
		Object value,
		boolean isSelected,
		boolean expanded,
		boolean leaf,
		int row)
	{
		this.tree = tree;	// for ListTableComboRenderer
		this.node = (FilterTreeNode)value;
		this.condition = (AbstractCondition)node.getUserObject();

		if (logicPanel == null)
			buildEditorComponent(tree);

		// call to set "lastRow"
		super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
		
		return initEditorComponent();
	} 


	private void buildEditorComponent(JTree tree)	{
		Font f = tree.getFont();
		FontMetrics fm = tree.getFontMetrics(f);

		comparisonPanel = new JPanel();
		comparisonPanel.setLayout(new BoxLayout(comparisonPanel, BoxLayout.X_AXIS));

		logicLabel1 = getFilterTreeCellRenderer().getLeadingLogicOperatorLabel();
		comparisonPanel.add(logicLabel1);
		comparisonPanel.add(new JLabel(" "));
		int w = getFilterTreeCellRenderer().getMaximalFieldnameWidth(fm, tree);
		if (w > 0)	{
			fieldNameCombo = new FixedWidthCombo(w);
			comparisonPanel.add(fieldNameCombo);
		}
		operatorCombo = new FixedWidthCombo(getFilterTreeCellRenderer().getMaximalCompareOperatorWidth(fm));
		comparisonPanel.add(operatorCombo);
		compareValueTextField = new JTextField();
		comparisonPanel.add(compareValueTextField);
		compareValueCombo = new FixedWidthCombo(w);	// do not add yet, will replace compareValueTextField when needed
		
		logicPanel = new JPanel();
		logicPanel.setLayout(new BoxLayout(logicPanel, BoxLayout.X_AXIS));
		
		logicLabel2 = getFilterTreeCellRenderer().getLeadingLogicOperatorLabel();
		logicPanel.add(logicLabel2);
		logicPanel.add(new JLabel(" "));
		contentCombo = new FixedWidthCombo(-1);
		contentCombo.setRenderer(new ListTableComboRenderer());
		logicPanel.add(contentCombo);
	}


	private Component initEditorComponent()	{
		Component ret;
		
		contentCombo.removeActionListener(commitActionListener);
		operatorCombo.removeActionListener(commitActionListener);
		compareValueTextField.removeActionListener(commitActionListener);
		if (fieldNameCombo != null)
			fieldNameCombo.removeActionListener(commitActionListener);

		if (isLogicCondition())	{
			configureComponent(tree, logicPanel);
			
			logicLabel2.setText(getFilterTreeCellRenderer().getLeadingLogicOperatorText(node));
			
			configureComponent(tree, contentCombo);
			if (contentCombo.getItemCount() <= 0)
				for (int i = 0; i < condition.getOperators().length; i++)
					contentCombo.addItem(getFilterTreeCellRenderer().toContentLogicOperatorText((LogicalCondition.LogicalOperator)condition.getOperators()[i]));
			contentCombo.setSelectedItem(getFilterTreeCellRenderer().toContentLogicOperatorText((LogicalCondition.LogicalOperator)condition.getOperator()));
			contentCombo.setBackground(tree.getBackground());

			ret = logicPanel;
		}
		else	{
			configureComponent(tree, comparisonPanel);

			Comparison comparison = (Comparison)condition;
			
			logicLabel1.setText(getFilterTreeCellRenderer().getLeadingLogicOperatorText(node));
			
			if (fieldNameCombo != null)	{
				BeanVariable leftValue = (BeanVariable)comparison.getLeftValue();
				configureComponent(tree, fieldNameCombo);
				fieldNameCombo.removeAllItems();
				String [] fieldNames = leftValue.getFieldNames();
				for (int i = 0; i < fieldNames.length; i++)
					fieldNameCombo.addItem(fieldNames[i]);
				fieldNameCombo.setSelectedItem(leftValue.getFieldName());
			}
			
			configureComponent(tree, operatorCombo);
			operatorCombo.removeAllItems();
			for (int i = 0; i < condition.getOperators().length; i++)
				operatorCombo.addItem(condition.getOperators()[i].toString());
			operatorCombo.setSelectedItem(condition.getOperator().toString());
			((ListTableComboRenderer)operatorCombo.getRenderer()).setHorizontalAlignment(getFilterTreeCellRenderer().getOperatorLabelAlignment());
				
			if (comparison.getRightValue() instanceof BeanVariable)	{
				BeanVariable rightValue = (BeanVariable)comparison.getRightValue();
				if (compareValueCombo.getParent() == null)
					ComponentUtil.replaceComponent(comparisonPanel, compareValueTextField, compareValueCombo);
				configureComponent(tree, compareValueCombo);
				compareValueCombo.removeAllItems();
				String [] fieldNames = rightValue.getFieldNames();
				for (int i = 0; i < fieldNames.length; i++)
					compareValueCombo.addItem(fieldNames[i]);
				compareValueCombo.setSelectedItem(rightValue.getFieldName());
			}
			else	{
				if (compareValueTextField.getParent() == null)
					ComponentUtil.replaceComponent(comparisonPanel, compareValueCombo, compareValueTextField);
				configureComponent(tree, compareValueTextField);
				String s = comparison.getRightValue().toString();
				compareValueTextField.setColumns(Math.max(s.length(), 10));
				compareValueTextField.setText(s);
			}

			ret = comparisonPanel;
		}

		contentCombo.addActionListener(commitActionListener);
		operatorCombo.addActionListener(commitActionListener);
		compareValueTextField.addActionListener(commitActionListener);
		if (fieldNameCombo != null)
			fieldNameCombo.addActionListener(commitActionListener);
		
		return ret;
	}
	
	private void configureComponent(JComponent src, JComponent tgt)	{
		tgt.setBackground(src.getBackground());
		tgt.setForeground(src.getForeground());
		tgt.setFont(src.getFont());
	}

	
	private AbstractCondition getCondition()	{
		// get values from editors
		if (isLogicCondition())	{
			LogicalCondition logCond = (LogicalCondition)condition;
			
			String contentOperator = (String)contentCombo.getSelectedItem();
			logCond.setOperator(getFilterTreeCellRenderer().fromContentLogicOperatorText(contentOperator));
		}
		else	{
			AbstractComparison comparison = (AbstractComparison)condition;
			
			if (fieldNameCombo != null)	{
				String fieldName = (String)fieldNameCombo.getSelectedItem();
				comparison.setLeftValue(new BeanVariable(fieldName));
			}

			String operator = (String)operatorCombo.getSelectedItem();
			AbstractCondition.Operator [] operators = condition.getOperators();
			for (int i = 0; i < operators.length; i++)
				if (operators[i].toString().equals(operator))
					comparison.uncheckedSetOperator(operators[i]);

			String compareValue = compareValueTextField.getText();
			comparison.setRightValue(new Constant(compareValue));
		}

		return condition;
	}

	
	private boolean isLogicCondition()	{
		return condition instanceof LogicalCondition;
	}
	
	private FilterTreeCellRenderer getFilterTreeCellRenderer()	{
		return (FilterTreeCellRenderer)renderer;
	}


	protected boolean inHitRegion(int x, int y) {
		if (lastRow != -1 && tree != null)	{
			Rectangle r = tree.getRowBounds(lastRow);
			if (r != null && x <= r.x + logicLabel1.getSize().width)	{
				//System.err.println("not in hit region: "+r+", logicLabel size "+logicLabel1.getSize()+", x "+x);
				return false;
			}
			//System.err.println("was in hit region: "+r+", logicLabel size "+logicLabel1.getSize()+", x "+x);
		}
		return true;
	}

	protected void startEditingTimer() {
		if (timer == null) {
			timer = new Timer(200, this);
			timer.setRepeats(false);
		}
		timer.start();
	}





	private class FixedWidthCombo extends JComboBox
	{
		private int width;
		
		FixedWidthCombo(int width)	{
			//System.err.println("creating FixedWidthCombo with width "+width);
			this.width = width;
			setRenderer(new ListTableComboRenderer());
		}
		
		public Dimension getPreferredSize()	{
			return changeDimension(super.getPreferredSize());
		}
		public Dimension getMaximumSize()	{
			return changeDimension(super.getMaximumSize());
		}
		public Dimension getMinimumSize()	{
			return changeDimension(super.getMinimumSize());
		}
		
		private Dimension changeDimension(Dimension d)	{
			if (width > 0)
				d.width = width;
			d.height = Math.min(d.height, Math.max(18, tree.getRowHeight()));
			return d;
		}
	}




	private class ListTableComboRenderer extends BasicComboBoxRenderer
	{
		public Component getListCellRendererComponent(
			JList list, 
			Object value,
			int index, 
			boolean isSelected, 
			boolean cellHasFocus)
		{
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (isSelected) {
				c.setBackground(UIManager.getColor("Tree.selectionBackground"));
				c.setForeground(UIManager.getColor("Tree.selectionForeground"));
			}
			else {
				c.setBackground(tree != null ? tree.getBackground() : UIManager.getColor("Tree.background"));
				c.setForeground(tree != null ? tree.getForeground() : UIManager.getColor("Tree.foreground"));
			}
			return c;
		}
	}

}
