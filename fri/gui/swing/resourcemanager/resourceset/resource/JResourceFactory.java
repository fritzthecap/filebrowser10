package fri.gui.swing.resourcemanager.resourceset.resource;

import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	This factory provides AWT and Swing compatible Resource subclasses:
	Font, Foreground, Background, Accelerator, Text, Icon, Border, ToolTipText.
*/

public class JResourceFactory extends ResourceFactory
{
	public static final String TOOLTIP = "ToolTipText";
	/** Type identifier for accelerator Resource. */
	public static final String ACCELERATOR = "Accelerator";
	/** Type identifier for border Resource. */
	public static final String BORDER = "Border";
	/** Type identifier for icon Resource. */
	public static final String ICON = "Icon";
	/** Type identifier for row height Resource. */
	public static final String ROWHEIGHT = "RowHeight";
	/** Type identifier for textarea tab-size. */
	public static final String TABSIZE = "TabSize";
	/** Type identifier for textarea line-wrap. */
	public static final String LINEWRAP = "LineWrap";
	/** Type identifier for textarea line-wrap. */
	public static final String MNEMONIC = "Mnemonic";

	/** Returns all resource type names loadable by this factory. */
	protected String [] getResourceTypeNames()	{
		String [] awtTypes = super.getResourceTypeNames();
		String [] swingTypes = new String []	{
				TOOLTIP,
				ACCELERATOR,
				MNEMONIC,
				BORDER,
				ICON,
				ROWHEIGHT,
				TABSIZE,
				LINEWRAP,
		};
		String [] allTypes = new String[awtTypes.length + swingTypes.length];
		System.arraycopy(awtTypes, 0, allTypes, 0, awtTypes.length);
		System.arraycopy(swingTypes, 0, allTypes, awtTypes.length, swingTypes.length);
		return allTypes;
	}

}
