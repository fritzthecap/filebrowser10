package fri.gui.swing.hexeditor;

import java.io.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import fri.util.dump.NumericDump;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.util.RefreshTable;
import fri.gui.swing.util.CommitTable;
import fri.gui.swing.fileloader.*;
import fri.gui.swing.editor.*;

/**
	A table that loads and saves a file.
	Implementation of EditorTextHolder as a byte view.

	@author Ritzberger Fritz
*/

public class HexTable extends JTable implements
	EditorTextHolder,
	LoadObserver,
	EditorTextHolderSupport.Saver,
	EditorTextHolderSupport.Loader
{
	private JComponent panel;
	private EditorTextHolderSupport support;
	private byte [] bytes;
	private CaretListener caretListener;
	private TableModelListener modelListener;
	private int base = Config.getBase();
	private HexTableCellEditor byteCellEditor;
	private PropertyChangeSupport changeNotifier;
	private int startMovePending = -1, endMovePending = -1;



	/**
		Create the textarea and load the passed file if not null.
		@param file to load.
		@param tabSize number of spaces for one tab.
	*/
	public HexTable(File file)	{
		setModel(new HexTableModel());

		support = new EditorTextHolderSupport(file, this, this, this);

		changeNotifier = new PropertyChangeSupport(this);

		modelListener = new TableModelListener()	{
			public void tableChanged(TableModelEvent e)	{
				if (support != null)	// after super constructor
					support.setChanged(true);
			}
		};
		
		init();
	}
	
	private void init()	{
		setShowGrid(false);
		setIntercellSpacing(new Dimension(0, 0));
		getTableHeader().setReorderingAllowed(false);
		
		// set a toggling selection model to achieve caret position (non-selected focus)
		setSelectionModel(new HexTableSelectionModel(this));
		getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		setCellSelectionEnabled(true);
		
		setDefaultRenderer(String.class, new HexTableCellRenderer());
		setDefaultEditor(String.class, byteCellEditor = new HexTableCellEditor(base));

		TableColumnModel m = getColumnModel();
		m.getColumn(HexTableModel.ADDRESS_COLUMN).setPreferredWidth(24);
		for (int i = 0; i < m.getColumnCount(); i++)	{
			if (getHexTableModel().isHexByteColumn(i))
				m.getColumn(i).setPreferredWidth(16);
		}
		
		getSelectionModel().addListSelectionListener(new TableCaretListener());	// generate caret events
		getColumnModel().getSelectionModel().addListSelectionListener(new TableCaretListener());	// generate caret events
 	}



	// Listener that generates a textarea-like CaretEvent when selection changes.
	private class TableCaretListener implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent e)	{
			if (e.getValueIsAdjusting())
				return;

			if (caretListener != null)	{
				caretListener.caretUpdate(new CaretEvent(this)	{
					private int dot = -2, mark = -2;
					
					public int getDot()	{
						if (dot > -2)	// calculate only once
							return dot;
						Point p = getHexTableSelectionModel().getCaretCell();
						int pos = p != null ? getHexTableModel().pointToPosition(p.x, p.y) : -1;
						return dot = pos;
					}
					
					public int getMark()	{
						if (mark > -2)	// calculate only once
							return mark;
						Point start = getHexTableSelectionModel().getSelectionStart();
						Point end = getHexTableSelectionModel().getSelectionEnd();
						if (start == null || end == null)	{	// no selection exists
							mark = getDot();
						}
						else	{	// the mark must be the cell one behind the last selected cell
							boolean biggerThanDot = start.equals(getHexTableSelectionModel().getCaretCell());
							Point p = biggerThanDot ? end : start;
							int pos = getHexTableModel().pointToPosition(p.x, biggerThanDot ? p.y + 1 : p.y - 1);
							if (pos < 0 && biggerThanDot)	// valid selection at end of bytes
								pos = getDot() + 1;
							mark = pos;
						}
						return mark;
					}
				});
			}
		}

	}


	/** Overridden (JTable method) to show a textarea-like selection. */
	public boolean isCellSelected(int row, int col)	{
		if (getHexTableModel().isHexByteColumn(col) == false)
			return false;
			
		int minRow = getSelectionModel().getMinSelectionIndex();
		int maxRow = getSelectionModel().getMaxSelectionIndex();
		
		if (row > minRow && row < maxRow)
			return true;	// all cells between start and end but not start and end
			
		if ((row == minRow || row == maxRow) && minRow != maxRow)	{
			int anchorCol = getColumnModel().getSelectionModel().getAnchorSelectionIndex();
			int leadCol = getColumnModel().getSelectionModel().getLeadSelectionIndex();
			int anchorRow = getSelectionModel().getAnchorSelectionIndex();
			int leadRow = getSelectionModel().getLeadSelectionIndex();
			//boolean toRight = leadCol > anchorCol;
			boolean toBottom = leadRow > anchorRow;
			
			if (toBottom)
				if (row == maxRow)
					return col <= leadCol;
				else	// minRow
					return col >= anchorCol;
			else	// to top
				if (row == maxRow)
					return col <= anchorCol;
				else	// minRow
					return col >= leadCol;
		}
		
		return super.isCellSelected(row, col);
	}


	/** Overridden to set the input focus (caret) to the textfield when editing programmatically. */
	public boolean editCellAt(int row, int col)	{
		boolean b = super.editCellAt(row, col);
		ComponentUtil.requestFocus(byteCellEditor.getTextField());
		return b;
	}
	
	
	/** Overridden to show the byte offset of the cell the mouse is over. */
	public String getToolTipText(MouseEvent e)	{
		if (e != null)	{
			int row = rowAtPoint(e.getPoint());
			int col = columnAtPoint(e.getPoint());
			if (row >= 0)	{
				int offset = getHexTableModel().pointToPosition(row, col);
				if (offset >= 0)
					return "Offset "+offset+" (Decimal)";
			}
		}
		return null;
	}



	// view to model

	/** Overridden to display a Byte according to the current radix (base). */
	public Object getValueAt(int row, int column)	{
		Object o = getModel().getValueAt(row, column);
		if (o instanceof Byte)	{
			o = byteToView(((Byte)o).byteValue());
		}
		return o;
	}

	/** Converts a byte to a String. */
	private String byteToView(byte b)	{
		if (getBase() < Character.MIN_RADIX)
			return ""+NumericDump.byteToChar(b);
		else
			return NumericDump.byteToString(b, getBase());
	}

	/**
		Does nothing if the value was NOT changed, else calls the <i>PropertyChangeListener</i>,
		which in turn must call <i>getModel().setValueAt()</i> by creating an <i>UpdateCommand</i>
		and calling its <i>doit()</i> method.
	*/
	public void setValueAt(Object aValue, int row, int column)	{
		//System.err.println("setValueAt() in table with model "+getModel().hashCode());
		Byte oldValue = (Byte)getModel().getValueAt(row, column);
		
		byte newValue = viewToByte((String)aValue, oldValue.byteValue());
		if (newValue == oldValue.byteValue())
			return;

		PropertyChangeEvent e = new PropertyChangeEvent(
				this,	// event source
				"UPDATED",	// type, not really used as this is the only event
				new ByteAndPosition(oldValue, row, column),	// old value
				new ByteAndPosition(Byte.valueOf(newValue), row, column));	// new value
		
		// make controller create an UpdateCommand and execute it
		getChangeNotifier().firePropertyChange(e);
	}

	/** Converts a String to a byte, if possible, else returns original byte. */
	private byte viewToByte(String s, byte originalByte)	{
		try	{
			if (getBase() < Character.MIN_RADIX)	{	// charatcer representation
				if (s.length() > 1 || s.length() == 1 && s.charAt(0) > 255)	{
					throw new IllegalArgumentException("Input is not exactly one character, or is not a valid character: '"+s+"'");
				}
				return s.length() == 0 ? originalByte : NumericDump.charToByte(s.charAt(0));
			}
			else	{
				return s.length() == 0 ? originalByte : NumericDump.stringToByte(s, base);
			}
		}
		catch (Exception e)	{
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(
					ComponentUtil.getWindowForComponent(this),
					"Not a valid byte: '"+s+"'\n"+e.getMessage(),
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return originalByte;
		}
	}

	/** Returns the ProperyChangeSupport notifier object for cell updates. */
	public PropertyChangeSupport getChangeNotifier()	{
		return changeNotifier;
	}



	/** Sets the new base (radix), one of 0 (character), 2, 8, 10, 16. */
	public void setBase(int base)	{
		CommitTable.commit(this);
		
		this.base = base;
		byteCellEditor.setBase(base);
		
		RefreshTable.refresh(this);
	}

	/** Returns the current base (radix), one of 0 (character), 2, 8, 10, 16. */
	public int getBase()	{
		return base;
	}
	


	// controller helpers
	
	/** Called by the ControllerModelItem when "Cut" was executed. */
	public void setMovePending(int start, int end, boolean movePending)	{
		if (movePending)	{
			startMovePending = start;
			endMovePending = end;
		}
		else	{
			startMovePending = -1;
			endMovePending = -1;
		}
		RefreshTable.refresh(this);
	}
	
	/** Queried by the renderer when painting cells. */
	public boolean isMovePending(int row, int column)	{
		if (startMovePending < 0)
			return false;
			
		int pos = getHexTableModel().pointToPosition(row, column);
		return (pos >= startMovePending && pos < endMovePending);
	}


	/** Setting new cloumn count to tablemodel, refreshing table. */
	public void setColumnCount(int columnCount)	{
		HexTableModel m = getHexTableModel();
		setModel(new HexTableModel());
		m.setColumnCount(columnCount);
		setModel(m);
		init();
		//RefreshTable.refresh(this);
	}




	// interface EditorTextHolder, Saver, Loader

	/** Implements EditorTextHolder. @return the file that is loaded in this textarea */
	public File getFile()	{
		return support.getFile();
	}

	/** Implements EditorTextHolder */	
	public boolean getChanged()	{
		return support.getChanged();
	}

	/** Implements EditorTextHolder: stop a file loading thread. */
	public void interrupt()	{
		support.interrupt();
	}


	/** Implements EditorTextHolder. Saves the file synchronously. Delegates to EditorTextHolderSupport.  */	
	public boolean saveAs()	{
		return support.saveAs();
	}

	/** Implements EditorTextHolder. Saves the file synchronously. Delegates to EditorTextHolderSupport. */	
	public boolean save()	{
		return support.save();
	}
	
	/** Implements EditorTextHolderSupport.Saver. Saves the file synchronously. */	
	public void store(File file)
		throws Exception
	{
		BufferedOutputStream bout = null;

		try {
			HexTableModel model = (HexTableModel)getModel();
			byte [] bytes = model.getBytes();

			FileOutputStream out = new FileOutputStream(file);
			bout = new BufferedOutputStream(out);

			bout.write(bytes, 0, bytes.length);
		}
		finally	{
			try	{ bout.close(); }	catch (IOException ex)	{}
		}
	}


	/** Implements EditorTextHolder. Gets called only at start. */
	public void load()	{
		if (getFile() != null)	{
			support.load();
		}
		else	{	// add DocumentListener to empty new file to catch changes
			getModel().addTableModelListener(modelListener);
			setLoading(false);
		}
	}

	/** Implements EditorTextHolder, FileChangeSupport.Reloader */
	public void reload()	{
		support.reload();
	}

	/** Implements EditorTextHolderSupport.Loader: loads the file into textarea. */
	public FileLoader createFileLoader(File file)	{
		bytes = new byte[(int)file.length()];
		
		return new ByteFileLoader(
				file,	// File
				bytes,	// data
				panel,	// progress bar panel
				this,	// TextLoadObserver
				this);	// to be notified if some thread is waiting for loading finished
	}
	
	/** Implements TextLoadObserver: sets the document and restores the view position when load finishes. */
	public synchronized void setLoading(boolean loading)	{
		support.setLoading(loading);
	}
	
	/** Implements EditorTextHolder. @return true if file is loading. Wait object is TextEditArea.this. */
	public synchronized boolean isLoading()	{
		return support.isLoading();
	}

	/** Implements EditorTextHolderSupport.Loader: loads the file bytes into table. */
	public void afterLoading()	{
		getModel().removeTableModelListener(modelListener);
		getHexTableModel().setBytes(bytes);
		getModel().addTableModelListener(modelListener);
		bytes = null;
	}


	/** Implements EditorTextHolder. Set the fileChangeSupport active or not. */	
	public void setWarnDirty(boolean warnDirty)	{
		support.setWarnDirty(warnDirty);
	}
	
	/** Implements EditorTextHolder. Stores the passed panel for file loading progress. */	
	public void setProgressContainer(JComponent panel)	{
		this.panel = panel;
	}
	
	/** Implements EditorTextHolder */	
	public void setUndoListener(UndoableEditListener undoListener)	{
	}
	
	/** Implements EditorTextHolder. Called when closing internal frame. */	
	public void unsetUndoListener(UndoableEditListener undoListener)	{
	}
	
	/** Implements EditorTextHolder */	
	public void setCaretListener(CaretListener cl)	{
		this.caretListener = cl;
	}
	/** Implements EditorTextHolder */	
	public void unsetCaretListener(CaretListener cl)	{
		this.caretListener = null;
	}
	
	/** Implements EditorTextHolder */	
	public void setChangeListener(ChangeListener cl)	{
		support.setChangeListener(cl);
	}
	
	/** Implements EditorTextHolder */	
	public void unsetChangeListener(ChangeListener cl)	{
		support.unsetChangeListener(cl);
		getModel().removeTableModelListener(modelListener);
	}

	/** Implements EditorTextHolder. This must match the search criterion for <i>cleanEdits()</i>. */	
	public Object getUndoableEditIdentifier()	{
		return this;
	}

	/** Implements EditorTextHolder: byte position is stored to Point.x */	
	public Point caretToPoint(int dot)	{
		return new Point(dot, 0);
	}




	// interface TextHolder

	/** Sets the current cursor position in the textarea. */
	public void setCaretPosition(int pos)	{
		Point p = getHexTableModel().positionToPoint(pos);
		if (p == null)	{
			clearSelection();
		}
		else	{
			setRowSelectionInterval(p.x, p.x);
			setColumnSelectionInterval(p.y, p.y);
		}
	}

	/** Returns the current cursor position of the textarea. */
	public int getCaretPosition()	{
		Point p = getHexTableSelectionModel().getCaretCell();
		if (p == null)
			return -1;
		return getHexTableModel().pointToPosition(p.x, p.y);
	}

	/** Returns the start offset of selected text in textarea. */
	public int getSelectionStart()	{
		Point p = getHexTableSelectionModel().getSelectionStart();
		int pos = (p == null) ? -1 : getHexTableModel().pointToPosition(p.x, p.y);
		return pos;
	}

	/** Returns the end offset of selected text in textarea. */
	public int getSelectionEnd()	{
		Point p = getHexTableSelectionModel().getSelectionEnd();
		int pos = (p == null) ? -1 : getHexTableModel().pointToPosition(p.x, p.y) + 1;
		return pos;
	}

	/**
		Selects a location within textarea. If start is equal to end, clears selection.
		Implicitely sets the caret position.
	*/
	public void select(int start, int end)	{
		if (start >= end || start < 0 || end <= 0)	{
			clearSelection();
		}
		else	{
			Point startPoint = getHexTableModel().positionToPoint(start);
			Point endPoint = getHexTableModel().positionToPoint(end - 1);
			setRowSelectionInterval(startPoint.x, endPoint.x);
			setColumnSelectionInterval(startPoint.y, endPoint.y);
			
			Rectangle cellRect = getCellRect(startPoint.x, endPoint.y, false);
			if (cellRect != null)
				scrollRectToVisible(cellRect);
		}
	}

	/**
		Returns the currently selected text. This is for setting initial search pattern.
	*/
	public String getSelectedText()	{
		int start = getSelectionStart();
		int end = getSelectionEnd();
		if (start >= end || start < 0 || end <= 0)
			return "";
			
		byte [] bytes = getHexTableModel().getBytes();
		byte [] pattern = new byte[end - start];
		System.arraycopy(bytes, start, pattern, 0, end - start);
		return bytesToString(pattern);
	}

	/** Returns true if the TextHolder is NOT readonly. */
	public boolean isEditable()	{
		return true;
	}

	/** Returns the Component the text lies within. */
	public Component getTextComponent()	{
		return this;
	}



	// interface TextGetSet
	
	public String getText()	{
		return bytesToString(getHexTableModel().getBytes());
	}
	
	public void setText(String text)	{
		throw new IllegalStateException("Can not set text without undoable edits: use controller for that purpose!");
	}
	
	
	private String bytesToString(byte [] bytes)	{
		char [] chars = new char[bytes.length];
		for (int i = 0; i < bytes.length; i++)	{
			char c = NumericDump.byteToChar(bytes[i]);
			chars[i] = c;
		}
		return new String(chars);
	}
	
	public byte [] stringToBytes(String text)	{
		byte [] bytes = new byte[text.length()];
		for (int i = 0; i < text.length(); i++)	{
			char c = text.charAt(i);
			byte b = NumericDump.charToByte(c);
			bytes[i] = b;
		}
		return bytes;
	}



	// general utils

	private HexTableModel getHexTableModel()	{
		return (HexTableModel)getModel();
	}

	private HexTableSelectionModel getHexTableSelectionModel()	{
		return (HexTableSelectionModel)getSelectionModel();
	}

}