package fri.gui.swing.combo.history;

import java.awt.event.*;
import java.util.Vector;
import javax.swing.JOptionPane;
import fri.gui.swing.ComponentUtil;

/**
	"Crud" stands for "Create/Read/Update/Delete". This combo manages
	its items and offers callbacks for insert/update/delete and select.
	
	@author Fritz Ritzberger 2003
*/

public class CrudCombo extends HistCombo
{
	private Vector lsnrs, pendingLsnrs;
	
	/** CrudListeners receive create/read/update/delete ("CRUD") events. */
	public interface CrudListener
	{
		/** A new item has been created. */
		public void itemCreated(Object item);

		/** An item has been renamed. */
		public void itemRenamed(Object oldName, Object newName);

		/** An item has been deleted. */
		public void itemDeleted(Object item);

		/** Another item has been selected. */
		public void itemSelected(Object item);
	}
	
	/** CrudPendingListeners receive create/read/update/delete ("CRUD") events before they happen and can veto them. */
	public interface CrudPendingListener
	{
		/** A new item is about to be created. @return false to veto creation. */
		public boolean canCreate(Object item);

		/** An item is about to be renamed. @return false to veto rename. */
		public boolean canRename(Object oldName, Object newName);

		/** An item is about to be deleted. @return false to veto deletion. */
		public boolean canDelete(Object item);
	}
	
	
	
	
	/** CRUD (create/read/update/delete) combo constructor. */
	public CrudCombo()	{
		super();
		init();
	}

	/** CRUD (create/read/update/delete) combo constructor with items. */
	public CrudCombo(String [] items)	{
		super(items);
		init();
	}

	/** CRUD (create/read/update/delete) combo constructor with items. */
	public CrudCombo(Vector items)	{
		super(items);
		init();
	}


	private void init()	{
		this.addItemListener(new ItemListener()	{	// do not wait for ENTER key to load selection
			public void itemStateChanged(ItemEvent e)	{
				if (e.getStateChange() == ItemEvent.SELECTED)
					selection(getSelectedItem());
			}
		});
	}


	public void addCrudListener(CrudListener lsnr)	{
		if (lsnrs == null)
			lsnrs = new Vector();
		lsnrs.add(lsnr);
	}

	public void removeCrudListener(CrudListener lsnr)	{
		lsnrs.remove(lsnr);
	}

	private void fireCrudEvent(Object oldItem, Object newItem, Object deletedItem)	{
		for (int i = 0; lsnrs != null && i < lsnrs.size(); i++)	{
			CrudListener lsnr = (CrudListener)lsnrs.get(i);

			if (oldItem != null && newItem != null)	{	// is rename
				lsnr.itemRenamed(oldItem, newItem);
			}
			else
			if (deletedItem != null)	{	// is delete
				lsnr.itemDeleted(deletedItem);
			}
			else
			if (newItem != null)	{	// is create
				lsnr.itemCreated(newItem);
			}
			else	{	// is select
				lsnr.itemSelected(oldItem);
			}
		}
	}



	public void addCrudPendingListener(CrudPendingListener lsnr)	{
		if (pendingLsnrs == null)
			pendingLsnrs = new Vector();
		pendingLsnrs.add(lsnr);
	}

	public void removeCrudPendingListener(CrudPendingListener lsnr)	{
		pendingLsnrs.remove(lsnr);
	}

	private boolean fireCrudPendingEvent(Object oldItem, Object newItem, Object deletedItem)	{
		for (int i = 0; pendingLsnrs != null && i < pendingLsnrs.size(); i++)	{
			CrudPendingListener lsnr = (CrudPendingListener)pendingLsnrs.get(i);
			boolean ok = true;

			if (oldItem != null && newItem != null)	{	// is rename
				ok = lsnr.canRename(oldItem, newItem);
			}
			else
			if (deletedItem != null)	{	// is delete
				ok = lsnr.canDelete(deletedItem);
			}
			else
			if (newItem != null)	{	// is create
				ok = lsnr.canCreate(newItem);
			}
			
			if (!ok)
				return false;
		}
		return true;
	}




	/** Triggers ENTER key, to be used by a "Go!" button. This will fire actionPerformed to any ActionListener. */
	public void commitInput()	{
		keyPressed(new KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, '\n'));
	}
	
	/** Inserts the passed item on top and commits it (fires actionPerformed), to be used by a "Create" button. */
	public void createInput(String newItem)	{
		setText(newItem);
		commitInput();
		fireCrudEvent(null, newItem, null);
	}
	
	/** Deletes the currently selected item, to be used by a "Delete" button. */
	public void deleteInput()	{
		insertOnTopAndSelect("");
	}
	

	
	/** Override if selection of items needs special behaviour. This method does nothing. */
	protected void selection(Object selectedItem)	{
		System.err.println("CrudCombo selection is: "+selectedItem);
		fireCrudEvent(selectedItem, null, null);
	}

	/** Opens a confirm dialog. Override if removal of items needs special behaviour. */
	protected boolean removePermitted(Object item)	{
		if (fireCrudPendingEvent(null, null, item) == false)	{
			error("Can Not Delete \""+item+"\"!", "Delete Vetoed");
			return false;
		}
			
		int ret = JOptionPane.showConfirmDialog(
				ComponentUtil.getWindowForComponent(this),
				"Really Delete \""+item+"\" ?",
				"Confirm Delete",
				JOptionPane.YES_NO_OPTION);

		boolean ok = ret == JOptionPane.YES_OPTION;
		if (ok)	{
			fireCrudEvent(null, null, item);

			if (history.getSize() > 1)	{	// signalize selection of next item from top
				// the next item will be the one over the deleted one, if first is deleted, the one under
				int found = 1;	// defaults to the one under
				for (int i = history.getSize() - 1; found <= 1 && i >= 1; i--)	// compare from bottom up to second item
					if (history.getElementAt(i).equals(item))	// if item found
						found = i - 1;	// take the one over
				
				selection(history.getElementAt(found));
			}
		}

		return ok;
	}

	/** Opens a confirm dialog. Override if editing of items needs special behaviour. @return -1 for cancel, 0 for update, 1 for create. */
	protected int editIsCreate(Object oldItem, Object newItem)	{
		Object [] options = { "Rename", "Create", "Cancel" };
		int ret = JOptionPane.showOptionDialog(
				ComponentUtil.getWindowForComponent(this),
				"Rename \""+oldItem+"\" Or Create New \""+newItem+"\"?",
				"Rename Or Create?",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				options,
				options[0]);

		ret = (ret == 0) ? 0 : (ret == 1) ? 1 : -1;
		if (ret == 0)	{
			if (fireCrudPendingEvent(oldItem, newItem, null) == false)	{
				error("Can Not Rename \""+oldItem+"\"!", "Rename Vetoed");
				return -1;
			}
			fireCrudEvent(oldItem, newItem, null);
		}
		else
		if (ret == 1)	{
			if (fireCrudPendingEvent(null, newItem, null) == false)	{
				error("Can Not Create \""+newItem+"\"!", "Create Vetoed");
				return -1;
			}
			fireCrudEvent(null, newItem, null);
			selection(newItem);
		}

		return ret;
	}

	/** Opens a confirm dialog. Override if creation of items needs special behaviour. */
	protected boolean createPermitted(Object item)	{
		if (fireCrudPendingEvent(null, item, null) == false)	{
			error("Can Not Create \""+item+"\"!", "Create Vetoed");
			return false;
		}
			
		int ret = JOptionPane.showConfirmDialog(
				ComponentUtil.getWindowForComponent(this),
				"Create \""+item+"\"?",
				"Confirm Create",
				JOptionPane.YES_NO_OPTION);

		boolean ok = ret == JOptionPane.YES_OPTION;
		if (ok)
			fireCrudEvent(null, item, null);

		return ok;
	}


	private void error(String msg, String title)	{
		JOptionPane.showMessageDialog(
				ComponentUtil.getWindowForComponent(this),
				msg,
				title,
				JOptionPane.ERROR_MESSAGE);
	}
	



	public static void main(String [] args)	{
		final javax.swing.JFrame f = new javax.swing.JFrame("HistCombo");
		f.getContentPane().setLayout(new java.awt.FlowLayout());
		
		final CrudCombo combo = new CrudCombo();
		
		f.getContentPane().add(combo);
		javax.swing.JButton commit = new javax.swing.JButton("Commit");
		f.getContentPane().add(commit);
		javax.swing.JButton create = new javax.swing.JButton("Create");
		f.getContentPane().add(create);
		javax.swing.JButton delete = new javax.swing.JButton("Delete");
		f.getContentPane().add(delete);
		javax.swing.JButton gettext = new javax.swing.JButton("Get Text");
		f.getContentPane().add(gettext);
		final javax.swing.JTextField tf = new javax.swing.JTextField(10);
		f.getContentPane().add(tf);
		
		commit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)	{
				combo.commitInput();
			}
		});
		create.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)	{
				combo.createInput("Test Item");
			}
		});
		delete.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)	{
				combo.deleteInput();
			}
		});
		gettext.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)	{
				tf.setText(combo.getText());
			}
		});
				
		f.setSize(new java.awt.Dimension(400, 200));
		f.show();
	}

}
