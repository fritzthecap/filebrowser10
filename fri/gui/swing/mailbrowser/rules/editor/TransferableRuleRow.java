package fri.gui.swing.mailbrowser.rules.editor;

import java.util.Arrays;
import java.util.List;
import java.io.*;
import java.awt.datatransfer.*;

/**
	Implementation of interface Transferable for Drag 'n Drop of rule rows.
*/

public class TransferableRuleRow implements
	Transferable,
	ClipboardOwner
{
	public static final DataFlavor ruleRowFlavor = new DataFlavor(RulesTableRow.class, "RuleRow");  		
	public static final DataFlavor[] flavors = {
		ruleRowFlavor,
	};
	private static final List flavorList = Arrays.asList(flavors);
	private RulesTableRow row;

	public TransferableRuleRow(RulesTableRow row) {
		this.row = row;
	}

	public synchronized DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor ) {
		return flavorList.contains(flavor);
	}
	
	public synchronized Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException, IOException
	{
		if (flavor.equals(ruleRowFlavor))	{
			return this.row;
		}
		else	{
			throw new UnsupportedFlavorException (flavor);
		}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}
	
	public String toString() {
		return "TransferableRuleRow";
	}

}
