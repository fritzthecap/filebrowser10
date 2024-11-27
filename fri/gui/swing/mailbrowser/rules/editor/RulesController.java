package fri.gui.swing.mailbrowser.rules.editor;

import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.controller.clipboard.*;
import fri.gui.swing.util.RefreshTable;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.mailbrowser.Language;

public class RulesController extends ActionConnector implements
	ListSelectionListener,
	TableModelListener
{
	public static final String ACTION_SAVE = "Save";
	public static final String ACTION_NEW = "New";
	public static final String ACTION_DELETE = "Delete";
	public static final String ACTION_CUT = "Cut";
	public static final String ACTION_COPY = "Copy";
	public static final String ACTION_PASTE = "Paste";
	private static DefaultClipboard clipboard = new DefaultClipboard();
	private RulesTable rulesTable;
	
	public RulesController(RulesTable rulesTable)	{
		super(rulesTable.getSensorComponent(), rulesTable.getSelection(), null);

		this.rulesTable = rulesTable;

		registerAction(ACTION_SAVE, Icons.get(Icons.save), "Save Rules", KeyEvent.VK_S, InputEvent.CTRL_MASK);
		registerAction(ACTION_NEW, Icons.get(Icons.newLine), "Create New Rule", KeyEvent.VK_INSERT, 0);
		registerAction(ACTION_DELETE, Icons.get(Icons.delete), "Delete Selected Rules", KeyEvent.VK_DELETE, 0);
		registerAction(ACTION_CUT, Icons.get(Icons.cut), "Cut Selected Rules", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		registerAction(ACTION_COPY, Icons.get(Icons.copy), "Copy Selected Rules", KeyEvent.VK_C, InputEvent.CTRL_MASK);
		registerAction(ACTION_PASTE, Icons.get(Icons.paste), "Paste Rules", KeyEvent.VK_V, InputEvent.CTRL_MASK);
		
		setEnabled(ACTION_SAVE, false);
		setEnabled(ACTION_DELETE, false);
		setEnabled(ACTION_CUT, false);
		setEnabled(ACTION_COPY, false);
		setEnabled(ACTION_PASTE, false);
		
		if (getModel().getRowCount() <= 0)	// create a default row if table is empty, before listeners are installed
			cb_New(null);
		
		// add listeners
		new RulesDndPerformer(rulesTable.getSensorComponent(), this);
		getModel().addTableModelListener(this);
		rulesTable.getSensorComponent().getSelectionModel().addListSelectionListener(this);
		// release clipboard when escape is pressed
		rulesTable.getSensorComponent().addKeyListener(new KeyAdapter()	{
			public void keyPressed(KeyEvent e)	{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
					RulesController.clipboard.clear();
					RefreshTable.refresh(RulesController.this.rulesTable.getSensorComponent());
					setEnabledActions();
				}
			}
		});
	}

	/** Do internationalization for action labels. */
	protected String language(String label)	{
		return Language.get(label);
	}


	/** Implements TableModelListener to set SAVE enabled. */
	public void tableChanged(TableModelEvent e)	{
		System.err.println("Rules table changed");
		//Thread.dumpStack();
		setEnabled(ACTION_SAVE, true);
	}

	/* Implements ListSelectionListener to set enabled actions. */
	public void valueChanged(ListSelectionEvent e)	{
		setEnabledActions();
	}

	private void setEnabledActions()	{
		List sel = (List)getSelection().getSelectedObject();
		boolean selected = (sel != null && sel.size() > 0);
		setEnabled(ACTION_DELETE, selected);
		setEnabled(ACTION_CUT, selected);
		setEnabled(ACTION_COPY, selected);
		setEnabled(ACTION_PASTE, clipboard.isEmpty() == false);
	}

	public RulesTableModel getModel()	{
		return rulesTable.getModel();
	}


	// callbacks
	
	public void cb_Save(Object selection)	{
		getModel().save();
		setEnabled(ACTION_SAVE, false);
	}

	public void cb_New(Object selection)	{
		int selectedRow = rulesTable.getSensorComponent().getSelectedRow();
		if (selectedRow < 0)
			selectedRow = getModel().getRowCount();	// if nothing selected, append at end
			
		// launch a create command
		DefaultCreateCommand cmd = new DefaultCreateCommand(
				getModel().createModelItem(null),	// dummy ModelItem
				getModel(),
				Integer.valueOf(selectedRow));
		cmd.doit();
		
		setEnabledActions();

		// need to start editing at position
		rulesTable.getSensorComponent().editCellAt(selectedRow, 0);
	}

	public void cb_Delete(Object selection)	{
		List sel = (List)selection;
		for (int i = sel.size() - 1; i >= 0; i--)	{
			Vector row = (Vector)sel.get(i);
			DefaultRemoveCommand cmd = new DefaultRemoveCommand(
					getModel().createModelItem(row),
					getModel());
			cmd.doit();
		}
		setEnabledActions();
	}



	private List toModelItems(List sel)	{
		Vector v = new Vector();
		for (int i = 0; i < sel.size(); i++)	{
			Vector row = (Vector)sel.get(i);
			v.add(getModel().createModelItem(row));
		}
		return v;
	}
	
	public void cb_Cut(Object selection)	{
		clipboard.cut(toModelItems((List)selection));
		setActionState();
	}

	public void cb_Copy(Object selection)	{
		clipboard.copy(toModelItems((List)selection));
		setActionState();
	}

	public void cb_Paste(Object selection)	{
		int selectedRow = rulesTable.getSensorComponent().getSelectedRow();
		if (selectedRow < 0)
			selectedRow = getModel().getRowCount();	// append at end
		paste(selectedRow, (List)selection);
	}
	
	public void paste(int selectedRow, List target)	{
		CommandArguments args = new CommandArguments.Paste(getModel(), Integer.valueOf(selectedRow));
		clipboard.paste(toModelItems(target), args);
		setActionState();
	}

	/** Set GUI state after drag&drop or cut&paste actions. */
	private void setActionState()	{
		setEnabledActions();
		RefreshTable.refresh(rulesTable.getSensorComponent());
	}



	private int confirmSave()	{
		if (getEnabled(ACTION_SAVE) == false)
			return JOptionPane.NO_OPTION;	// no changes

		return JOptionPane.showConfirmDialog(
				rulesTable.getSensorComponent(),
				Language.get("Save_Changes"),
				Language.get("Warning"),
				JOptionPane.YES_NO_CANCEL_OPTION);
	}


	public boolean close()	{
		// check for unsaved changes
		int ret = confirmSave();
		if (ret == JOptionPane.YES_OPTION)	{
			getModel().save();
		}
		else
		if (ret != JOptionPane.NO_OPTION)	{
			return false;	// canceled
		}
		return true;
	}


}