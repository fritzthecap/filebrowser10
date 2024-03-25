package fri.gui.swing.editor;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import fri.util.io.BomAwareReader;
import fri.util.text.encoding.Encodings;
import fri.gui.swing.text.OutputTextArea;
import fri.gui.swing.document.DocumentUtil;
import fri.gui.swing.fileloader.*;
		
/**
	A textarea that loads and saves a file.
	Implementation of EditorTextHolder as a simple text view.

	@author Ritzberger Fritz
*/
public class BasicTextEditArea extends OutputTextArea implements
	EditorTextHolder,
	DocumentListener,
	LoadObserver,
	EditorTextHolderSupport.Saver,
	EditorTextHolderSupport.Loader
{
	protected JComponent panel;
	private Document doc;
	protected UndoableEditListener undoListener;
	private EditorTextHolderSupport support;
	private String encoding;
	private boolean havingPrivateEncoding;	// true only if BOM or XML-endoding enforced a specific encoding
	private byte [] byteOrderMark;
	
	/**
		Create the textarea and load the passed file if not null.
		@param file to load.
		@param tabSize number of spaces for one tab.
	*/
	public BasicTextEditArea(File file)	{
		super(30, 80);
		this.support = new EditorTextHolderSupport(file, this, this, this);
		setCaretColor(Color.red);	// the best we can do ...
	}
	
	
	// interface DocumentListener
		
	/** implements DocumentListener */	
	public void changedUpdate(DocumentEvent e)	{
		setChanged();
	}
	/** implements DocumentListener */	
	public void insertUpdate(DocumentEvent e)	{
		setChanged();
	}
	/** implements DocumentListener */	
	public void removeUpdate(DocumentEvent e)	{
		setChanged();
	}

	protected void setChanged()	{
		support.setChanged(true);
	}


	// interface EditorTextHolder, Saver, Loader
	
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


	/** To be overridden when some text conversion should take place before writing text to file. */
	protected String convertWhenStoring(String text)	{
		return text;
	}
	
	/** Implements EditorTextHolderSupport.Saver. Saves the file synchronously. */	
	public void store(File file)
		throws Exception
	{
		BufferedWriter bw = null;

		try {
			String text = getText();
			text = convertWhenStoring(text);

			OutputStream out = new FileOutputStream(file);
			
			byte [] bom = byteOrderMark;
			if (bom != null || (bom = mustCreateByteOrderMark()) != null)
				out.write(bom);	// write byte order mark
			
			bw = (encoding == null)
				? new BufferedWriter(new OutputStreamWriter(out))
				: new BufferedWriter(new OutputStreamWriter(out, encoding));

			bw.write(text);
		}
		finally	{
			if (bw != null) 
				try	{ bw.close(); } catch (Exception e) {}	// ignore close error
			/*
			java.lang.NullPointerException at fri.gui.swing.editor.BasicTextEditArea.store(BasicTextEditArea.java:123)
			        at fri.gui.swing.editor.EditorTextHolderSupport.write(EditorTextHolderSupport.java:136)
			        at fri.gui.swing.filechooser.SaveLogic.save(SaveLogic.java:102)
			        at fri.gui.swing.filechooser.SaveLogic.save(SaveLogic.java:66)
			        at fri.gui.swing.editor.EditorTextHolderSupport.save(EditorTextHolderSupport.java:123)
			        at fri.gui.swing.editor.EditorTextHolderSupport.save(EditorTextHolderSupport.java:117)
			        at fri.gui.swing.editor.BasicTextEditArea.save(BasicTextEditArea.java:91)
			        at fri.gui.swing.editor.EditController.save(EditController.java:365)
			        at fri.gui.swing.editor.EditController.save(EditController.java:360)
			        at fri.gui.swing.editor.EditController.cb_Save_All(EditController.java:350)
        	*/
		}
	}

	private byte [] mustCreateByteOrderMark()	{
		if (Config.getCreateByteOrderMark() == false)
			return null;
		
		String enc = encoding != null ? encoding : Config.getEncoding() != null ? Config.getEncoding() : Encodings.defaultEncoding;
		if (enc.equalsIgnoreCase("UTF-8"))
			return BomAwareReader.getByteOrderMark("UTF-8");
		
		String [] utf16be = (String []) Encodings.map.get("UnicodeBigUnmarked");
		for (int i = 0; i < utf16be.length; i++)
			if (utf16be[i].equalsIgnoreCase(enc))
				return BomAwareReader.getByteOrderMark("UTF-16BE");
		
		String [] utf16le = (String []) Encodings.map.get("UnicodeLittleUnmarked");
		for (int i = 0; i < utf16le.length; i++)
			if (utf16le[i].equalsIgnoreCase(enc))
				return BomAwareReader.getByteOrderMark("UTF-16LE");
		
		return null;
	}


	/** Implements EditorTextHolder. Gets called only at start. */
	public void load()	{
		if (getFile() != null)	{
			support.load();
		}
		else	{	// add DocumentListener to empty new file to catch changes
			getDocument().addDocumentListener(this);
			support.setLoading(false);
		}
	}

	/** Implements EditorTextHolder, FileChangeSupport.Reloader */
	public void reload()	{
		support.reload();
	}

	/** Implements EditorTextHolderSupport.Loader: Creates a FileLoader that fills an empty Document. */
	public FileLoader createFileLoader(File file)	{
		// editor is about to load a file
		setText("");	// clear text in document, put old text to undo listener
		doc = getDocument();	// we need the same document, because undo listener is cleaning edits belonging to it
		setDocument(new PlainDocument());	// set a temporary empty document, else loader will display any inserted line
		if (havingPrivateEncoding)	{
			havingPrivateEncoding = false;
			encoding = Config.getEncoding();
			byteOrderMark = null;
		}

		TextFileLoader loader = new TextFileLoader(
				file,	// File
				doc,	// Document
				this,	// DocumentListener
				undoListener,	// UndoableEditListener
				panel,	// progress bar panel
				this,	// LoadObserver
				this,	// to be notified if some thread is waiting for loading finished
				encoding,
				Config.getDetectEncodingFromByteOrderMark(),
				Config.getDetectXmlOrHtmlHeaderEncoding());	// optional encoding of file, requires storing to disk in same encoding
		
		String detectedEncoding = loader.detectedEncoding();
		if (detectedEncoding != null)	{
			havingPrivateEncoding = true;
			encoding = detectedEncoding;
			byteOrderMark = loader.detectedByteOrderMark();
		}
		
		return loader;
	}
	
	/** Implements LoadObserver: sets the document and restores the view position when finishing reload(). */
	public synchronized void setLoading(boolean loading)	{
		support.setLoading(loading);
	}

	/** Implements EditorTextHolder. @return true if file is loading. Wait object is TextEditArea.this. */
	public synchronized boolean isLoading()	{
		return support.isLoading();
	}

	/** Implements EditorTextHolderSupport.Loader: sets the document. */
	public void afterLoading()	{
		setDocument(doc);
		doc = null;
		setEncodingTooltip();
	}


	/** Implements EditorTextHolder. Set the fileChangeSupport active or not. */	
	public void setWarnDirty(boolean warnDirty)	{
		support.setWarnDirty(warnDirty);
	}
	
	/** Implements EditorTextHolder. Stores the passed panel for file loading progress. */	
	public void setProgressContainer(JComponent panel)	{
		this.panel = panel;
	}
	
	/** Implements EditorTextHolder. @return the file that is loaded in this textarea */
	public File getFile()	{
		return support.getFile();
	}

	/** Implements EditorTextHolder */	
	public void setUndoListener(UndoableEditListener undoListener)	{
		this.undoListener = undoListener;
		getDocument().addUndoableEditListener(undoListener);
	}
	
	/** Implements EditorTextHolder. Called when closing internal frame. */	
	public void unsetUndoListener(UndoableEditListener undoListener)	{
		getDocument().removeUndoableEditListener(undoListener);
		this.undoListener = null;
	}
	
	/** Implements EditorTextHolder */	
	public void setCaretListener(CaretListener cl)	{
		super.addCaretListener(cl);
	}
	/** Implements EditorTextHolder */	
	public void unsetCaretListener(CaretListener cl)	{
		super.removeCaretListener(cl);
	}
	
	/** Implements EditorTextHolder */	
	public void setChangeListener(ChangeListener cl)	{
		support.setChangeListener(cl);
	}
	/** Implements EditorTextHolder */	
	public void unsetChangeListener(ChangeListener cl)	{
		support.unsetChangeListener(cl);
	}

	/** Implements EditorTextHolder. This must match the search criterion for <i>cleanEdits()</i>. */	
	public Object getUndoableEditIdentifier()	{
		return getDocument();
	}

	/** Implements EditorTextHolder */	

	public Point caretToPoint(int dot)	{
		return DocumentUtil.caretToPoint(dot, getDocument());
	}



	/** Sets the file encoding used by load and save. */
	public void setEncoding(String encoding)	{
		if (havingPrivateEncoding)	{
			System.err.println("NOT changing encoding because having private encoding "+this.encoding);
			return;
		}
		
		System.err.println("Changing encoding from "+this.encoding+" to "+encoding);
		this.encoding = encoding;
		
		String oldText = getText();

		String newText;

		try	{
			byte [] oldBytes = encoding == null ? oldText.getBytes() : oldText.getBytes(encoding);
			newText = encoding == null ? new String(oldBytes) : new String(oldBytes, encoding);
		}
		catch (UnsupportedEncodingException e)	{
			JOptionPane.showMessageDialog(this, "Could not set encoding "+encoding+": "+e.toString());
			return;
		}

		setText(newText);
		
		setEncodingTooltip();
	}

	private void setEncodingTooltip()	{
		if (havingPrivateEncoding)
			setToolTipText("Encoding: "+encoding);
		else
			setToolTipText(null);
	}

}
