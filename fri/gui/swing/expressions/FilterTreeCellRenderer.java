package fri.gui.swing.expressions;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import fri.gui.mvc.view.swing.MovePendingTreeCellRenderer;
import fri.gui.mvc.model.swing.TreeNodeUtil;
import fri.patterns.interpreter.expressions.*;

/**
	Tree cell renderer that shows an AbstractCondition (LogicalCondition, Comparison).
*/

public class FilterTreeCellRenderer extends MovePendingTreeCellRenderer
{
	private JPanel panel;
	private JLabel
			logicLabel,
			contentLabel,
			fieldNameLabel,
			operatorLabel,
			compareValueLabel;
	
	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean selected,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus)
	{
		// build panel if not done
		if (panel == null)	{
			Font f = tree.getFont();
			FontMetrics fm = getFontMetrics(f);
			panel = new JPanel();
			panel.setBorder(null);
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

			logicLabel = getLeadingLogicOperatorLabel();
			panel.add(logicLabel);
			panel.add(new JLabel(" "));	// distance to first element
			contentLabel = new JLabel();
			contentLabel.setOpaque(true);
			panel.add(contentLabel);
			fieldNameLabel = new FixedLengthLabel(getMaximalFieldnameWidth(fm, tree));
			fieldNameLabel.setFont(f);
			fieldNameLabel.setOpaque(true);
			panel.add(fieldNameLabel);
			operatorLabel = new FixedLengthLabel(getMaximalCompareOperatorWidth(fm));
			operatorLabel.setFont(f);
			operatorLabel.setOpaque(true);
			panel.add(operatorLabel);
			compareValueLabel = new JLabel(" ");
			compareValueLabel.setFont(f);
			compareValueLabel.setOpaque(true);
			panel.add(compareValueLabel);
		}
		
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		
		panel.setForeground(tree.getForeground());
		panel.setBackground(tree.getBackground());

		configureJComponent(tree, fieldNameLabel, selected);
		configureJComponent(tree, operatorLabel, selected);
		configureJComponent(tree, compareValueLabel, selected);
		configureJComponent(tree, contentLabel, selected);

		FilterTreeNode node = (FilterTreeNode)value;
		AbstractCondition cond = (AbstractCondition)node.getUserObject();

		logicLabel.setText(getLeadingLogicOperatorText(node));
		
		if (cond instanceof Comparison)	{
			Comparison comparison = (Comparison)cond;
			contentLabel.setText("");
			fieldNameLabel.setText(comparison.getLeftValue().toString());
			if (fieldNameLabel.getText().length() > 0)
				operatorLabel.setHorizontalAlignment(SwingConstants.CENTER);
			operatorLabel.setText(cond.getOperator().toString());
			compareValueLabel.setText(comparison.getRightValue().toString());
			operatorLabel.setBorder(BorderFactory.createLineBorder(Color.gray));	//BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray));
		}
		else	{
			contentLabel.setText(toContentLogicOperatorText((LogicalCondition.LogicalOperator)cond.getOperator()));
			fieldNameLabel.setText(" ");
			operatorLabel.setText(" ");
			compareValueLabel.setText(" ");
			operatorLabel.setBorder(null);
		}
		
		panel.revalidate();
		
		return panel;
	}

	private void configureJComponent(JComponent src, JComponent tgt, boolean selected)	{
		tgt.setForeground(selected ? getTextSelectionColor() : src.getForeground());
		tgt.setBackground(selected ? getBackgroundSelectionColor() : src.getBackground());
		//tgt.setBorder(src.getBorder());
	}
	
	public void setEnabled(boolean enable)	{
		logicLabel.setEnabled(enable);
		contentLabel.setEnabled(enable);
		fieldNameLabel.setEnabled(enable);
		operatorLabel.setEnabled(enable);
		compareValueLabel.setEnabled(enable);
	}




	// geometry methods shared with cell editor
	
	public JLabel getLeadingLogicOperatorLabel()	{
		JLabel tmp = new JLabel();
		Font f = tmp.getFont();
		f = f.deriveFont(Font.BOLD);
		FontMetrics fm = tmp.getFontMetrics(f);
		JLabel l = new FixedLengthLabel(getMaximalLogicOperatorWidth(fm) + 6, 8);
		l.setBackground(UIManager.getColor("Tree.selectionBackground"));
		l.setBorder(new SoftBevelBorder(BevelBorder.RAISED));
		l.setHorizontalAlignment(SwingConstants.CENTER);
		l.setOpaque(true);
		return l;
	}

	public String getLeadingLogicOperatorText(FilterTreeNode node)	{
		FilterTreeNode pnt = (FilterTreeNode)node.getParent();
		if (pnt != null)	{
			String s = ((AbstractCondition)pnt.getUserObject()).getOperator().toString();
			if (TreeNodeUtil.getPosition(node).intValue() > 0)
				return s;
			if (s.equals(LogicalCondition.AND_NOT.toString()) || s.equals(LogicalCondition.OR_NOT.toString()))
				return AbstractCondition.NOT;
		}
		return " ";
	}

	public String toContentLogicOperatorText(LogicalCondition.LogicalOperator operator)	{	
		String tag = operator.toString().startsWith(LogicalCondition.AND.toString()) ? "Sequence" : "Alternatives";
		return "( "+operator.toString()+"-"+tag+" )";
	}

	public LogicalCondition.LogicalOperator fromContentLogicOperatorText(String operator)	{
		operator = operator.substring("( ".length(), operator.lastIndexOf("-"));
		for (int i = 0; i < LogicalCondition.operators.length; i++)
			if (LogicalCondition.operators[i].toString().equals(operator))
				return LogicalCondition.operators[i];
		throw new IllegalArgumentException("Unknown operator: "+operator);
	}

	public int getMaximalLogicOperatorWidth(FontMetrics fm)	{
		return fm.stringWidth(LogicalCondition.AND_NOT.toString());
	}

	public int getMaximalCompareOperatorWidth(FontMetrics fm)	{
		return fm.stringWidth(DateComparison.NOT_SAME_MONTH.toString());
	}

	public int getMaximalFieldnameWidth(FontMetrics fm, JTree tree)	{
		FilterTreeNode root = (FilterTreeNode)tree.getModel().getRoot();
		AbstractCondition cond = (AbstractCondition)root.getUserObject();
		String max = visitVariables((LogicalCondition)cond, "");
		return fm.stringWidth(max);
	}

	private String visitVariables(LogicalCondition cond, String max)	{
		Condition [] conditions = cond.getConditions();
		
		for (int i = 0; i < conditions.length; i++)	{
			AbstractCondition c = (AbstractCondition)conditions[i];
			
			if (c instanceof LogicalCondition)	{
				visitVariables((LogicalCondition)c, max);
			}
			else	{
				Comparison comparison = (Comparison)c;
				String s1 = getLongestFieldName(comparison.getLeftValue());
				String s2 = getLongestFieldName(comparison.getRightValue());
				if (s1.length() > max.length())
					max = s1;
				if (s2.length() > max.length())
					max = s2;
			}
		}
		return max;
	}


	private String getLongestFieldName(Value v)	{
		String max = "";
		String [] fields = v instanceof BeanVariable ? ((BeanVariable)v).getFieldNames()  : null;
		for (int i = 0; fields != null && i < fields.length; i++)
			if (fields[i].length() > max.length())
				max = fields[i];
		return max;
	}

	int getOperatorLabelAlignment()	{
		return operatorLabel.getHorizontalAlignment();
	}



	// JLabel subclass with fixed width
	
	private static class FixedLengthLabel extends JLabel
	{
		private int width, deltaHeight;
		
		FixedLengthLabel(int width)	{
			this(width, -1);
		}
		
		FixedLengthLabel(int width, int deltaHeight)	{
			//System.err.println("creating FixedLengthLabel with width "+width);
			this.width = width;
			this.deltaHeight = deltaHeight;
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
			d.width = width;
			if (deltaHeight > 0)
				d.height -= deltaHeight;
			return d;
		}
	}

}
