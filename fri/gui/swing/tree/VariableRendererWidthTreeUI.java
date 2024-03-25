package fri.gui.swing.tree;

import java.awt.*;
import javax.swing.tree.TreePath;
import javax.swing.plaf.basic.BasicTreeUI;

/**
	Workaround the fact that updateUI() does not change
	tree renderer and editor properties.
*/

public class VariableRendererWidthTreeUI extends BasicTreeUI
{
	protected void paintRow(
		Graphics g, Rectangle clipBounds,
		Insets insets, Rectangle bounds, TreePath path,
		int row, boolean isExpanded,
		boolean hasBeenExpanded, boolean isLeaf)
	{
		// ask for preferred size every time to show the whole renderer,
		// else all renderers are displayed the same width and long labels are cutten!
		treeState.invalidateSizes();

		super.paintRow(
			g, clipBounds,
			insets, bounds, path,
			row, isExpanded,
			hasBeenExpanded, isLeaf);
	}

}
