package fri.gui.swing.combo.history;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import fri.util.file.StringVectorFile;
import fri.gui.swing.combo.WideComboBox;
import fri.gui.awt.resourcemanager.ResourceIgnoringContainer;

/**
	Ein Textfeld mit History-Funktion. Es merkt sich alle
	eingegebenen Strings und holt den zuletzt gewaehlten immer
	an die oberste Stelle. Erhaelt es den Focus, wird der Text
	selektiert.
	Laden und Speichern von String-Listen aus serialisierten files
	ist in save() und load(File) implementiert.
	Die HistCombo kann wie ein Textfeld (ueber das Interface TextLineHolder)
	programmiert werden.
	<p>
	Benutzung:
	<pre>
	class CommandComboBox extends HistCombo implements
		TypedHistoryHolder
	{
		private static Vector globalHist;
		private static File globalFile = null;
	
		public CommandComboBox()	{
			super();
			manageTypedHistory(this, new File(HistConfig.dir()+"CommandComboBox.list"));
		}
	
		// interface TypedHistoryHolder
		public void setTypedHistoryData(Vector v, File f)	{
			globalHist = v;
			globalFile = f;
		}		
		public Vector getTypedHistory()	{
			return globalHist;
		}
		public File getHistoryFile()	{
			return globalFile;
		}
	}
	</pre>
	
	@author Fritz Ritzberger 1999
*/

public class HistCombo extends WideComboBox implements
	ActionListener,
	KeyListener,
	FocusListener,
	TextLineHolder,
	ResourceIgnoringContainer	// do not customize anything here as these are runtime items
{
	/** Maximum items in persistent history. */
	protected static int MAXHIST = 50;
	
	/** Maximum height. */
	protected static int COMBO_HEIGHT = 24;
	
	/** Minimal item length to perform update instead of insert */
	protected static final int MIN_UPDATE_ITEM_LENGTH = 64;
	
	protected File file = null;
	protected Vector myHist = new Vector();	// the model as List
	protected DefaultComboBoxModel history;	// the model
	
	private ActionListener actionListener;
	private Object previousItem;
	private boolean clearingText;
	private boolean ignoreSelectionChange;
	private boolean existed;
	private boolean enterTyped;


	/** Anlegen einer leeren HistCombo. */
	public HistCombo()	{
		init(null);	// set list and model
		setEditable(true);
		setEditor(new HistComboEditor());
	}

	/**
		Anlegen einer HistCombo mit Inhalten aus einer Serialisierungs-Datei.
		@param f Datei aus der die history gelesen werden soll.
	*/
	public HistCombo(File f)	{
		this();
		init(f);
	}

	/** Anlegen einer HistCombo. @param first Inhalt des Textfeldes. */
	public HistCombo(String first)	{
		this();
		insertOnTopAndSelect(first);
	}

	/** Anlegen einer HistCombo. @param items History des Textfeldes. */
	public HistCombo(Vector items)	{
		this((String []) items.toArray(new String [items.size()]));
	}

	/** Anlegen einer HistCombo. @param items History des Textfeldes. */
	public HistCombo(String [] items)	{
		this();

		for (int i = items.length - 1; i >= 0; i--)
			if (items[i] != null)
				insertUniqueOnTop(items[i]);

		if (history.getSize() > 0)
			setSelectedIndex(0);
	}


	/** Overridden to remove/add listeners to editor. */
	public void setEditor(ComboBoxEditor anEditor)	{
		if (getEditor() != null)	{
			Component c = getEditor().getEditorComponent();
			c.removeKeyListener(this);
			c.removeFocusListener(this);
		}

		super.setEditor(anEditor);

		Component c = getEditor().getEditorComponent();
		c.addKeyListener(this);
		c.addFocusListener(this);
	}
	
	
	/**
		Implements Commitable to save the current item as the first.
		A client calls this method e.g. when pressing OK button.
	*/
	public void commit()	{
		String s = getText();
		if (s.length() > 0)
			setText(s);
	}
	

	/** 
		Returns true if Enter has been pressed,
		i.e. first list element is equal to text in textfield.
	*/
	public boolean isCommitted()	{
		if (history.getSize() > 0)	{
			String s1 = getText();
			String s2 = history.getElementAt(0).toString();
			return s1.equals(s2);
		}
		return false;
	}
	
	
	public Vector getDataVector()	{
		return myHist;
	}
	
	public void setDataVector(Vector v)	{
		history = new DefaultComboBoxModel(myHist = v);
		setModel(history);
		takePopupSize();
	}
	
	public void setDataItems(String [] items)	{
		Vector v = new Vector(items.length);
		for (int i = 0; i < items.length; i++)
			v.add(items[i]);
		setDataVector(v);
	}
	

	private void init(File f)	{
		if (f == null || load(f) == false)	{
			setDataVector(myHist);
		}
	}


	/** Returns the textfield from combobox editor. */
	public Component getTextEditor()	{
		return getEditor().getEditorComponent();
	}
	

	/**
		Um Gruppen von History-Listen zu ermoeglichen, die untereinander
		aktuell gehalten werden, aber nicht mit anderen vermischt werden
		duerfen, implementiert eine Spezialisierung von HistCombo
		das interface TypedHistoryHolder.
		Diese Methode dient zum Management der History-Liste beim
		Laden (Konstruktor), da nur die erste Instanz einer TypedHistory
		die Datei lesen soll, alle andern verbinden sich nur zu der
		globalen static Liste
		@param globalList gemeinsame Liste der Histoy-Items dieses Typs.
		@param f Dateinamen zum Laden der Liste
	*/
	public void manageTypedHistory(TypedHistoryHolder holder, File f)	{
		if (holder.getHistoryFile() == null)	{
			load(f);	// file und myHist wird belegt
			holder.setTypedHistoryData(myHist, file);
		}
		else	{
			myHist = holder.getTypedHistory();
			file = holder.getHistoryFile();
		}
		init(null);
	}



	// interface TextLineHolder: convenience-methods to textfields.

	/** Den uebergebenen Text in der ComboBox (an erste Stelle) einfuegen.  */
	public boolean setText(String item)	{
		//System.err.println("HistCombo.setText("+item+")");
		boolean isEmptyString = item != null && item.length() <= 0;
		if (isEmptyString)	{
			previousItem = null;	// do not delete a user item programmatically
			clearingText = true;
		}
		
		boolean b = insertOnTopAndSelect(item);
		
		clearingText = false;
		
		//System.err.println("HistCombo model is :"+myHist);
		return b;
	}


	/** Das Textfeld loeschen, Leerstring setzen */
	public void clear()	{
		setText("");
	}


	public void setEnabled(boolean enabled)	{
		super.setEnabled(enabled);
		((HistComboEditor)getEditor()).setEnabled(enabled);
	}


	/** Den sichtbaren Text aus der ComboBox zurueckliefern.  */
	public String getText()	{
		if (isEditable())	{
			String s = getEditor().getItem().toString();
			return (s == null) ? "" : s;
		}
		else	{
			return getSelectedItem().toString();
		}
	}


	/** Den Text in der ComboBox markieren.  */
	public void selectAll()	{
		getEditor().selectAll();
	}

	public void addActionListener(ActionListener l)	{
		actionListener = AWTEventMulticaster.add(actionListener, l);
	}
	public void removeActionListener(ActionListener l) {
		actionListener = AWTEventMulticaster.remove(actionListener, l);
	}

	// end interface TextLineHolder


	// interface ActionListener

	public void actionPerformed(ActionEvent e) {
		//System.err.println("HistCombo actionPerformed: selected item >"+getSelectedItem()+"<, item count "+getItemCount());
		ignoreSelectionChange = true;
		super.actionPerformed(e);
		ignoreSelectionChange = false;

		enterTyped = true;
		String s = getSelectedItem().toString();

		if (getItemCount() <= 0)	// Bug: Editor kommt nicht dran
			fireHistoryEvent();

		insertOnTopAndSelect(s);
		enterTyped = false;
	}


	/** Einfuegen eines items an erster (juengster) Position, den item im Editor sichtbar machen und selektieren. */
	protected boolean insertOnTopAndSelect(String item)	{
		//System.err.println("insertOnTopAndSelect >"+item+"<");
		if (item == null)
			return false;
			
		insertUniqueOnTop(item);
		
		// Set selected model-index, else choosing an item will bring another to textfield
		if (history.getSize() > 0 && (item.length() > 0 || clearingText))	{
			setSelectedIndex(0);
		}
			
		return true;
	}

	/** Einfuegen eines items an erster (juengster) Position, loeschen eines vorhandenen identen. */
	protected boolean insertUniqueOnTop(String item)	{
		//System.err.println("insertUniqueOnTop >"+item+"<");
		//Thread.dumpStack();
		if (item == null)
			return false;
		
		// remove the item from list to put it to top (remain unique)
		
		existed = false;
		ignoreSelectionChange = true;
		history.removeElement("");	// Leerstring immer loeschen
		ignoreSelectionChange = false;

		if (item.length() > 0)	{
			// remove-loop works from behind
			boolean removed = false;
			
			for (int i = history.getSize() - 1; removed == false && i >= 0; i--)	{
				Object o = history.getElementAt(i);
				String s = o == null ? null : o.toString();
				
				if (s != null && s.equals(item))	{
					if (i == 0)
						return true;	// item already at first position
						
					existed = history.getIndexOf(item) >= 0;
					history.removeElementAt(i);	// found, delete it
					//System.err.println("removing item "+item+" from history ...");
					removed = true;
				}
			}
			
			if (removed == false)	{	// no item found, check for similar item (compare first 64 characters)
				String newPart = null;
	
				for (int i = 0; i < history.getSize(); i++)	{
					Object o = history.getElementAt(i);
					String s = o == null ? null : o.toString();
					
					if (s != null &&
							MIN_UPDATE_ITEM_LENGTH > 0 &&
							item.length() > MIN_UPDATE_ITEM_LENGTH &&
							s.length() > MIN_UPDATE_ITEM_LENGTH)
					{
						// delete it if longer than 20 chars and first part is equal to new one
						String oldPart = s.substring(0, MIN_UPDATE_ITEM_LENGTH);
						if (newPart == null)
							newPart = item.substring(0, MIN_UPDATE_ITEM_LENGTH);
		
						if (newPart.equals(oldPart))	{
							history.removeElementAt(i);	// found, delete it
							break;	// break loop at first found
						}
					}
				}
			}
		}

		// now insert the item, or remove last selected when item is empty string
		insertAtPosition(item, 0);

		return true;
	}
	

	protected void insertAtPosition(Object item, int i)	{
		//System.err.println("insertAtPosition, item >"+item+"<, i="+i+", clearingText="+clearingText+", previousItem >"+previousItem+"<");

		int len = item.toString().length();
		boolean doRemove = clearingText == false && len <= 0;
		
		if (doRemove && history.getSize() > 0)	{
			int r = history.getIndexOf(previousItem);
			if (r >= 0)	{
				if (removePermitted(previousItem))	{
					//System.err.println("HistCombo removeElement: "+previousItem);
					previousItem = null;
					history.removeElementAt(r);
				}
				else	{
					setSelectedIndex(r);
				}
			}
		}
		else
		if (doRemove == false)	{	// must insert even empty string to clear text
			//System.err.println("HistCombo insertElement "+item+", i="+i+", previousItem "+previousItem+", history "+myHist+", existed "+existed+", enterTyped "+enterTyped);
			
			history.insertElementAt(item, i);
			//Thread.dumpStack();
			
			if (len > 0 && existed == false && enterTyped)	{
				if (previousItem != null)	{
					int ret = editIsCreate(previousItem, item);
					
					if (ret == 0)	{	// answer was rename
						int r = history.getIndexOf(previousItem);
						if (r >= 0)
							history.removeElementAt(r);
						previousItem = item;
					}
					else
					if (ret == -1)	{	// answer was cancel
						history.removeElementAt(i);
					}
					else	{	// answer was create
						previousItem = item;
					}
				}
				else	{
					if (createPermitted(item) == false)	// ask for creation
						history.removeElementAt(i);
					else
						previousItem = item;
				}
			}
			
			// check history for maximum size
			if (history.getSize() > MAXHIST)
				history.removeElementAt(history.getSize() - 1);
		}
		
		enterTyped = false;
	}

	/** Override if removal of items needs special behaviour. */
	protected boolean removePermitted(Object item)	{
		return true;
	}

	/** Override if creation of items needs special behaviour. */
	protected boolean createPermitted(Object item)	{
		return true;
	}

	/** Override if editing of items needs special behaviour. @return -1 for cancel, 0 for update, 1 for create. */
	protected int editIsCreate(Object oldItem, Object newItem)	{
		return 1;
	}


	// end interface ActionListener


	/** Overridden to store the last valid selected item if it gets removed. */
	protected void selectedItemChanged() {
		//System.err.println("begin selectedItemChanged, previous item was: "+previousItem);
		//Thread.dumpStack();
		super.selectedItemChanged();
		
		if (ignoreSelectionChange == false)	{
			Object o = getSelectedItem();
			if (o != null && o.toString().length() > 0)	{
				previousItem = o;
				//System.err.println("... selectedItemChanged, previous item is now: "+previousItem);
			}
		}
	}



	// interface KeyListener

	/** Enter key als ActionEvent umsetzen. FRi: unter JDK 1.2 noch noetig? */
	public void keyPressed(KeyEvent e)	{
		if (getItemCount() <= 0)
			return;
			
		if (e.getKeyCode() == KeyEvent.VK_ENTER)	{
			//System.err.println("HistCombo.keyPressed VK_ENTER");
			enterTyped = true;
			commit();
			fireHistoryEvent();
			enterTyped = false;
		}
	}
	
	public void keyReleased(KeyEvent e)	{}
	public void keyTyped(KeyEvent e)	{}


	private void fireHistoryEvent()	{
		if (actionListener != null && getEditor().getItem().toString().length() > 0)	{
			actionListener.actionPerformed(
					new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getEditor().getItem().toString()));
		}
	}


	// interface FocusListener

	public void focusGained(FocusEvent e)	{
	}

	/** Sichert den Text im Textfeld in die History */
	public void focusLost(FocusEvent e)	{
		String s = getEditor().getItem().toString();

		if (s.length() > 0)	{
			if (myHist.contains(s) == false)	{
				//System.err.println("focusLost "+s);
				enterTyped = true;
				setText(s);
				enterTyped = false;
			}
		}
	}
	

	private Dimension trimDimension(Dimension d)	{
		FontMetrics fm = getFontMetrics(getFont());
		int h = fm.getAscent() + fm.getDescent();
		d.height = Math.max(COMBO_HEIGHT, h);
		d.width = Math.max(d.width, 60);
		return d;
	}
	
	/** Overridden to avoid big combo box. */
	public Dimension getMaximumSize()	{
		return trimDimension(super.getMaximumSize());
	}	

	/** Overridden to avoid big combo box. */
	public Dimension getPreferredSize()	{
		return trimDimension(super.getPreferredSize());
	}

	
	// Persistenz ueber Properties

	/**
		Sichern der ComboBox-Inhalte auf das Dateisystem.
		@return true wenn erfolgreich 0-n items geschrieben, false
			wenn Datei nicht bekanntgemacht wurde.
	*/
	public boolean save()  {
		if (file != null)
			return save(file);
			
		Thread.dumpStack();
		return false;
	}
	
	/**
		Sichern der ComboBox-Inhalte auf das Dateisystem.
		@param file Datei, auf die der String-Vector serialisiert werden soll.
		@return true wenn erfolgreich 0-n items geschrieben, false
			wenn Datei nicht bekanntgemacht wurde.
	*/
	public boolean save(File file)  {
		if (myHist.size() > MAXHIST)
			myHist.setSize(MAXHIST);

		myHist.remove("");
		
		return doSave(file, myHist);
	}


	/**
		Laden und Einfuegen der ComboBox-Inhalte vom Dateisystem.
		@param f Datei, aus der ein serialisierter String-Vector
			gelesen werden soll.
		@return true wenn erfolgreich 0-n items gelesen, false wenn
			Fehler beim Oeffnen oder Schreiben der Datei.
	*/
	public boolean load(File f)	{
		if (file != null && f != null && f.equals(file))
			return true;
			
		file = f;
		
		boolean success = doLoad(file, myHist);
		if (myHist.size() > MAXHIST)
			myHist.setSize(MAXHIST);

		if (success)
			init(null);	// avoid recursion
		
		return success;
	}


	/** Save the passed list to file. */
	protected boolean doSave(File file, Vector myHist)  {
		System.err.println("saving history for "+getClass()+" to file "+file);
		return StringVectorFile.save(file, myHist);
	}


	/** Load the passed list from file. */
	protected boolean doLoad(File file, Vector myHist)	{
		System.err.println("loading history for "+getClass()+" from file "+file);
		return StringVectorFile.load(file, myHist);
	}



	// test main
	/*
	public static void main(String [] args)	{
		final JFrame f = new JFrame("HistCombo");
		f.getContentPane().setLayout(new FlowLayout());
		
		final HistCombo combo = new HistCombo();
		
		combo.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				System.err.println("------------> action performed "+((HistCombo)e.getSource()).getText());
			}
		});
		
		f.getContentPane().add(combo);
		JButton b = new JButton("Clear");
		f.getContentPane().add(b);
		JButton b2 = new JButton("Focus Change Test");
		f.getContentPane().add(b2);
		
		b.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)	{
				combo.clear();
			}
		});
		
		combo.clear();
		
		f.setSize(new Dimension(200, 200));
		f.show();
	}
	*/
}