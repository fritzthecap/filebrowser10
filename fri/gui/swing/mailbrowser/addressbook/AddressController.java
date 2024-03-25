package fri.gui.swing.mailbrowser.addressbook;

import java.io.File;
import java.util.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.mail.internet.*;
import fri.util.error.Err;
import fri.util.sort.quick.*;
import fri.util.file.FileString;
import fri.util.text.TextUtil;
import fri.gui.mvc.controller.*;
import fri.gui.mvc.controller.clipboard.*;
import fri.gui.swing.util.RefreshTable;
import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.filechooser.*;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.mailbrowser.Language;
import fri.gui.swing.mailbrowser.send.*;

public class AddressController extends ActionConnector implements
	ListSelectionListener
{
	public static final String ACTION_MAIL_TO = "Mail To";
	public static final String ACTION_NEW = "New Address";
	public static final String ACTION_DELETE = "Delete Address";
	public static final String ACTION_CUT = "Cut Address";
	public static final String ACTION_PASTE = "Paste Address";
	public static final String ACTION_EXPORT = "Export";
	private static DefaultClipboard clipboard = new DefaultClipboard();
	private AddressTable addressTable;
	private SendWindowOpener sendWindowOpener;
	
	public AddressController(AddressTable addressTable, SendWindowOpener sendWindowOpener)	{
		super(addressTable.getSensorComponent(), addressTable.getSelection(), null);

		this.addressTable = addressTable;
		this.sendWindowOpener = sendWindowOpener;

		registerAction(ACTION_MAIL_TO, Icons.get(Icons.newDocument), "New Mail To Selected Addresses", KeyEvent.VK_N, InputEvent.CTRL_MASK);
		registerAction(ACTION_NEW, Icons.get(Icons.newLine), "Create New Address", KeyEvent.VK_INSERT, 0);
		registerAction(ACTION_DELETE, Icons.get(Icons.delete), "Delete Selected Addresses", KeyEvent.VK_DELETE, 0);
		registerAction(ACTION_CUT, Icons.get(Icons.cut), "Cut Selected Addresses", KeyEvent.VK_X, InputEvent.CTRL_MASK);
		registerAction(ACTION_PASTE, Icons.get(Icons.paste), "Paste Addresses", KeyEvent.VK_V, InputEvent.CTRL_MASK);
		registerAction(ACTION_EXPORT, Icons.get(Icons.export), "Export Addresses To File");
		
		setEnabled(ACTION_DELETE, false);
		setEnabled(ACTION_CUT, false);
		setEnabled(ACTION_PASTE, false);
		setEnabled(ACTION_MAIL_TO, false);
		setEnabled(ACTION_EXPORT, addressTable.getModel().getRowCount() > 0);
		
		addressTable.getSensorComponent().getSelectionModel().addListSelectionListener(this);
		addressTable.getSensorComponent().addKeyListener(new KeyAdapter()	{
			public void keyPressed(KeyEvent e)	{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
					AddressController.clipboard.clear();
					RefreshTable.refresh(AddressController.this.addressTable.getSensorComponent());
					setEnabledActions();
				}
			}
		});
	}

	/** Do internationalization for action labels. */
	protected String language(String label)	{
		return Language.get(label);
	}


	/* Implements ListSelectionListener to set the selected message to message viewer and mark it as SEEN. */
	public void valueChanged(ListSelectionEvent e)	{
		setEnabledActions();
	}

	private void setEnabledActions()	{
		List sel = (List)getSelection().getSelectedObject();
		boolean selected = (sel != null && sel.size() > 0);
		setEnabled(ACTION_MAIL_TO, selected);
		setEnabled(ACTION_DELETE, selected);
		setEnabled(ACTION_CUT, selected);
		setEnabled(ACTION_PASTE, selected && clipboard.isEmpty() == false);
		setEnabled(ACTION_EXPORT, addressTable.getModel().getRowCount() > 0);
	}

	private AddressTableModel getModel()	{
		return addressTable.getModel();
	}


	// callbacks
	
	public void cb_New_Address(Object selection)	{
		int selectedRow = addressTable.getSensorComponent().getSelectedRow();
		if (selectedRow < 0)
			selectedRow = getModel().getRowCount();	// if nothing selected, append at end
			
		// launch a create command
		DefaultCreateCommand cmd = new DefaultCreateCommand(
				getModel().createModelItem(null),	// dummy ModelItem
				getModel(),
				new Integer(selectedRow));
		cmd.doit();
		
		setEnabledActions();

		// need to start editing at position
		addressTable.getSensorComponent().editCellAt(selectedRow, 0);
	}

	public void cb_Delete_Address(Object selection)	{
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



	public void cb_Mail_To(Object selection)	{
		List sel = (List)selection;
		Vector v = new Vector(sel.size());
		
		for (int i = sel.size() - 1; i >= 0; i--)	{
			AddressTableRow row = (AddressTableRow)sel.get(i);
			String address = (String)row.get(AddressTableModel.EMAIL_COLUMN);
			String person = (String)row.get(AddressTableModel.PERSON_COLUMN);
			
			if (address.length() > 0)	{
				try	{
					if (person.length() > 0)
						v.add(new InternetAddress(address, person));
					else
						v.add(new InternetAddress(address));
				}
				catch (Exception e)	{
					Err.error(e);
				}
			}
		}
		
		if (v.size() <= 0)
			return;
		
		SendFrame sendWindow = sendWindowOpener.openSendWindow();
		InternetAddress [] addr = new InternetAddress[v.size()];
		v.copyInto(addr);
		sendWindow.setTo(addr);
	}



	private List toModelItems(List sel)	{
		Vector v = new Vector();
		for (int i = 0; i < sel.size(); i++)	{
			Vector row = (Vector)sel.get(i);
			v.add(getModel().createModelItem(row));
		}
		return v;
	}
	
	public void cb_Cut_Address(Object selection)	{
		cut((List)selection);
		setActionState();
	}

	public void cb_Paste_Address(Object selection)	{
		int selectedRow = addressTable.getSensorComponent().getSelectedRow();
		paste((List)selection, selectedRow);
		setActionState();
	}


	private void cut(List source)	{
		clipboard.cut(toModelItems(source));
	}

	private void paste(List target, int selectedRow)	{
		CommandArguments args = new CommandArguments.Paste(getModel(), new Integer(selectedRow));
		clipboard.paste(toModelItems(target), args);
	}

	/** Set GUI state after drag&drop or cut&paste actions. Public for DndPerformer. */
	public void setActionState()	{
		setEnabledActions();
		RefreshTable.refresh(addressTable.getSensorComponent());
	}


	/** Add the transferredRow to end if not found in list, else insert it at dropRow when dropRow is not null. */
	public void mergeAddress(Vector transferredRow, AddressTableRow dropRow)	{
		int i = getModel().locate(transferredRow);	// find the dropped row in model

		if (i >= 0)	{	// transferred row was found in model, drop if target index is different
			if (dropRow != null)	{	// item moving
				int j = getModel().locate(dropRow);
				if (i == j)
					return;	// dropped on itself
				
				List source = new Vector();
				source.add(getModel().getAddressTableRow(i));
	
				List target = new Vector();
				target.add(dropRow);
	
				cut(source);
				paste(target, j);
			}
			// else: already contained, do nothing if there was no target row
		}
		else	{	// new to model, append behind, happens on file import by drag&drop
			// launch a create command
			DefaultCreateCommand cmd = new DefaultCreateCommand(
					getModel().createModelItem(null),	// dummy ModelItem
					getModel(),
					transferredRow);
			cmd.doit();
		}
	}


	public String packRow(String [] row)	{
		String s = "";
		for (int i = 0; i < row.length; i++)
			s = s + (i == 0 ? "" : ";") + row[i];
		return s;
	}
	
	public String packRow(Vector row)	{
		String [] sarr = new String [row.size()];
		row.copyInto(sarr);
		return packRow(sarr);
	}

	public Vector parseRow(String line)	{
		Vector v = TextUtil.tokenizeBySeparatorRespectQuotes(line, ";,", '"');
		
		if (v.size() > AddressTableModel.COLUMN_COUNT)
			v.setSize(AddressTableModel.COLUMN_COUNT);
		else
			for (int i = v.size(); i < AddressTableModel.COLUMN_COUNT; i++)
				v.add("");
		

		return v;
	}


	public String toRow(InternetAddress inetAddr)	{
		String [] row = new String [AddressTableModel.COLUMN_COUNT];
		
		for (int i = 0; i < row.length; i++)	{
			if (i == AddressTableModel.EMAIL_COLUMN)
				row[i] = inetAddr.getAddress();
			else
			if (i == AddressTableModel.PERSON_COLUMN && inetAddr.getPersonal() != null)
				row[i] = inetAddr.getPersonal();
			else
				row[i] = "";
		}
		
		return packRow(row);
	}

	public void mergePackedAddresses(Vector addresses)	{
		addresses = new QSort().sort(addresses);
		for (int i = 0; i < addresses.size(); i++)	{
			String address = (String)addresses.get(i);
			mergeAddress(parseRow(address), null);
		}
		setActionState();
	}



	public void cb_Export(Object selection)	{
		AddressTableModel model = addressTable.getModel();
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < model.getRowCount(); i++)	{
			if (i > 0)
				sb.append("\n");
			sb.append(packRow(model.getAddressTableRow(i)));
		}
		
		try	{
			File file = DefaultFileChooser.saveDialog(null, addressTable, AddressController.class);
			if (file != null)	{
				if (FileString.put(sb.toString(), file) == false)	{
					Err.warning("Could not save to file "+file);
				}
			}
		}
		catch (CancelException e)	{
		}
	}

}