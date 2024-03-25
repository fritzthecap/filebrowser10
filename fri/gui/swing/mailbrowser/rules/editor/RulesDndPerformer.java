package fri.gui.swing.mailbrowser.rules.editor;

import java.util.*;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.*;
import fri.gui.mvc.view.swing.TableSelectionDnd;
import fri.gui.mvc.controller.swing.dnd.AbstractAutoScrollingDndPerformer;
import fri.gui.swing.scroll.ScrollPaneUtil;

/**
*/

public class RulesDndPerformer extends AbstractAutoScrollingDndPerformer
{
	private RulesController controller;
	
	public RulesDndPerformer(Component sensor, RulesController controller)	{
		super(sensor, ScrollPaneUtil.getScrollPane(sensor));
		this.controller = controller;
	}
	
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		for (int i = 0; i < flavors.length; i++)
			if (flavors[i].equals(TransferableRuleRow.ruleRowFlavor))
				return flavors[i];
		return null;
	}
	
	public Transferable sendTransferable()	{
		List sel = (List)controller.getSelection().getSelectedObject();
		RulesTableRow row = (RulesTableRow)sel.get(0);
		return new TransferableRuleRow(row);
	}

	public boolean receiveTransferable(Object data, int action, Point p)	{
		RulesTableRow targetRow = (RulesTableRow) ((TableSelectionDnd)controller.getSelection()).getObjectFromPoint(p);
		int idx = controller.getModel().locate((RulesTableRow)data);
		RulesTableRow moving = controller.getModel().getRulesTableRow(idx);
		receiveRuleRow(moving, targetRow);
		return true;
	}


	private void receiveRuleRow(RulesTableRow droppedRow, RulesTableRow targetRow)	{
		Vector dropped = new Vector();
		dropped.add(droppedRow);
		Vector target = new Vector();
		target.add(targetRow);
		
		controller.cb_Cut(dropped);
		controller.paste(controller.getModel().locate(targetRow), target);
	}

}
