package fri.gui.swing.concordance.filter;

import java.awt.*;
import javax.swing.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.actionmanager.MenuTree;
import fri.gui.swing.expressions.*;

/**
	Builds a FilterTree that contains only StringComparison and logical associations,
	not DateComparison or NumberComparsion or ObjectComparison.
*/

public class TextlineFilterMvcBuilder extends FilterMvcBuilder
{
	/** Deriving two classes to override the <i>FilterTreeView</i> method <i>getCreationTypeMenuItems()</i>. */
	protected FilterTreeMvcBuilder newFilterTreeMvcBuilder()	{
		return new FilterTreeMvcBuilder()	{
			protected FilterTreeView newFilterTreeView()	{
				return new FilterTreeView()	{
					public MenuTree [] getCreationTypeMenuItems()	{
						MenuTree [] menus = new MenuTree[1];
						menus[0] = new MenuTree(FilterTreeController.STRING_COMPARE);
						return menus;
					}
				};
			}
		};
	}


	/** Convenience method to show the MVC in a modal dialog. Used by ConcordanceFrame and TextareaConcordanceDialog. */
	public FilterModel showAsDialog(Component parent)	{
		JPanel p = new JPanel(new BorderLayout());
		build(p);

		// can not use JOptionPane as this catches ENTER key on CrudCombo
		JDialog dlg = parent instanceof Frame
				? new JDialog((Frame)parent, "Text Filter Settings", true)
				: new JDialog((Dialog)parent, "Text Filter Settings", true);
		dlg.getContentPane().add(p);
		new GeometryManager(dlg).show();

		getController().close();	// save model to persistence
		
		// get selected filter model as ValidityFilter
		return (FilterModel)getController().getModel();
	}

}
