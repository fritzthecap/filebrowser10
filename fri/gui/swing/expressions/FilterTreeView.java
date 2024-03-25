package fri.gui.swing.expressions;

import javax.swing.*;
import fri.gui.mvc.view.swing.TreeSelectionDnd;
import fri.gui.mvc.model.Model;
import fri.gui.mvc.view.swing.DefaultActionRenderingView;
import fri.gui.mvc.view.Selection;
import fri.gui.swing.util.*;
import fri.gui.swing.combo.history.CrudCombo;
import fri.gui.swing.tree.*;
import fri.gui.swing.actionmanager.MenuTree;
import fri.gui.swing.document.textfield.MaskingDocument;
import fri.gui.swing.document.textfield.mask.FilenameMask;

/**
	Filter panel containing an tree editor for expressions.
	
	@todo Add an editable combo with all loadable filter models.
			Deletion of a name means deletion of filter (ensure-dialog),
			editing of a name means copy, rename or new (decision-dialog),
			selecting a name means storing current filter (when changed) and loading new one.
*/

public class FilterTreeView extends DefaultActionRenderingView
{
	private CrudCombo modelManager;
	
	public FilterTreeView()	{
		JTree tree = new CustomJTree();
		tree.setEditable(true);
		tree.setUI(new VariableRendererWidthTreeUI());
		setSensorComponent(tree);
	}
	

	/** Overrides DefaultSwingView: Creates a new TreeSelectionDnd object for the treeview, on every call. */
	protected Selection createSelection()	{
		return new TreeSelectionDnd(getTree());
	}
	
	/** Ends all open tree view cell editors. */
	public void commit()	{
		CommitTree.commit(getTree());
	}

	/** Refreshes the tree view. */
	public void refresh()	{
		RefreshTree.refresh(getTree());
	}

	/** Sets the model into view and installs renderers. Does NOT set the model into controller! */
	public void setModel(Model model)	{
		super.setModel(model);
		
		getTree().setModel((FilterTreeModel)model);
		
		FilterTreeCellRenderer renderer = new FilterTreeCellRenderer();
		getTree().setCellRenderer(renderer);
		getTree().setCellEditor(new FilterTreeCellEditor(getTree(), renderer));
		TreeExpander.expandAllBranches(getTree());
	}

	/** Returns the model CRUD manager as a combobox (create/rename/delete filter models). */
	public CrudCombo getModelManager()	{
		if (modelManager == null)	{
			modelManager = new CrudCombo();
			// avoid input of characters that can not be in a filename
			JTextField tf = (JTextField)modelManager.getTextEditor();
			String text = tf.getText();
			tf.setDocument(new MaskingDocument(tf, new FilenameMask()));
			tf.setText(text);
		}
		return modelManager;
	}

	/** To be called once on init by the MVC builder, to render actions within a toolbar or popup menu. */
	public void renderActions(FilterTreeController controller)	{
		JToolBar toolbar = installToolBar(null);
		controller.visualizeAction(FilterTreeController.ACTION_NEW_MODEL, toolbar);
		controller.visualizeAction(FilterTreeController.ACTION_DELETE_MODEL, toolbar);
		toolbar.add(getModelManager());
		controller.visualizeAction(FilterTreeController.ACTION_LOAD_MODEL, toolbar);
		toolbar.addSeparator();
		//toolbar.add(new JSeparator(SwingConstants.VERTICAL));
		toolbar.add(Box.createHorizontalGlue());
		controller.visualizeAction(FilterTreeController.ACTION_NEW, toolbar);
		controller.visualizeAction(FilterTreeController.ACTION_DELETE, toolbar);
		toolbar.addSeparator();
		controller.visualizeAction(FilterTreeController.ACTION_CUT, toolbar);
		controller.visualizeAction(FilterTreeController.ACTION_COPY, toolbar);
		controller.visualizeAction(FilterTreeController.ACTION_PASTE, toolbar);
		
		JPopupMenu popup = installPopup(null);
		controller.visualizeAction(FilterTreeController.ACTION_NEW, popup, false);
		controller.visualizeAction(FilterTreeController.ACTION_DELETE, popup, false);
		toolbar.addSeparator();
		controller.visualizeAction(FilterTreeController.ACTION_CUT, popup, false);
		controller.visualizeAction(FilterTreeController.ACTION_COPY, popup, false);
		controller.visualizeAction(FilterTreeController.ACTION_PASTE, popup, false);
	}

	/**
		Returns the menu items for creation of new expression items.
		AND / OR is not contained, as this is mandatory. This method
		returns all possible items (String/Number/Date/Object). When
		overridden this method should return at least
		<i>FilterTreeController.STRING_COMPARE</i>.
	*/
	public MenuTree [] getCreationTypeMenuItems()	{
		MenuTree [] menus = new MenuTree[4];
		menus[0] = new MenuTree(FilterTreeController.STRING_COMPARE);
		menus[1] = new MenuTree(FilterTreeController.NUMBER_COMPARE);
		menus[2] = new MenuTree(FilterTreeController.DATE_COMPARE);
		menus[3] = new MenuTree(FilterTreeController.OBJECT_COMPARE);
		return menus;
	}


	private JTree getTree()	{
		return (JTree)getSensorComponent();
	}
	
}
