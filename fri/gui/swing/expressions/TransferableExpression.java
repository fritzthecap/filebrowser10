package fri.gui.swing.expressions;

import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.awt.datatransfer.*;
import fri.patterns.interpreter.expressions.Expression;

/**
	Implementation of interface Transferable for Drag 'n Drop of filter tree nodes.
*/

public class TransferableExpression implements
	Transferable,
	ClipboardOwner
{
	public static final DataFlavor expressionFlavor = new DataFlavor(Expression.class, "Expression");  		
	public static final DataFlavor[] flavors = {
		expressionFlavor,
	};
	private static final List flavorList = Arrays.asList(flavors);
	private List expressions;

	public TransferableExpression(List expressions) {
		this.expressions = expressions;
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
		if (flavor.equals(expressionFlavor))	{
			return this.expressions;
		}
		else	{
			throw new UnsupportedFlavorException (flavor);
		}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}
	
	public String toString() {
		return "TransferableExpression";
	}

}
