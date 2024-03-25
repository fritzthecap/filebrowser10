package fri.gui.awt.clipboard;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import fri.gui.text.TextHolder;

/**
	Klasse, um Funktionalitaet des System-Clipboard in AWT- und Swing-
	Textfeldern plattformuebergreifend als Tasten-Kombinationen einzubinden.
	(Linux Implementierungs-Maengel, unter Windows ist die Verwendung
	dieser Klasse nicht notwendig!)
	<P>
	Die Standard-Keys Ctrl-C, Ctrl-X, Ctrl-V implementieren
	ihre entsprechenden Funktion: Copy, Cut, Paste.
	Der KeyListener "konsumiert" diese drei Tastendruecke!
*/

public class ClipKeyListener implements KeyListener, ClipboardOwner
{
	/* Clipboard Funktionen */
	private transient TextHolder textcomp;
	private ClipTextRenderer renderer;

	/** Anlegen eines Clip-Key-Listeners fuer die uebergebene Text-Komponente.
			@param textcomp TextField oder TextArea, die zu bearbeitenden Text haelt.
			@param renderer Objekt, das die entsprechenden Funktionen zum
				Einfuegen/Loeschen von Text implementiert. In der Regel derselbe
				Wert wie der erste Parameter.
	*/
	public ClipKeyListener(TextHolder textcomp, ClipTextRenderer renderer)	{
		this.textcomp = textcomp;
		this.renderer = renderer;
	}

	/**
		Kopieren des selektierten (markierten) Textes in der TextComponent.
	*/
	public void copy()	{
		System.err.println("ClipKeyListener.copy");
		String selected = textcomp.getSelectedText();
		if (selected != null && selected.equals("") == false)	{
			StringSelection str = new StringSelection(textcomp.getSelectedText());
			((Component)textcomp).getToolkit().getSystemClipboard().setContents(str, this);
		}
		//System.err.println("ClipKeyListener.copy end");
		//System.err.println(textcomp.getSelectedText());
	}

	/**
		Ausschneiden des selektierten (markierten) Textes in der TextComponent
		mittels Funktions-Aufruf des ClipTextRenderer
	*/
	public void cut()	{
		if (textcomp.isEditable() == false)
			return;
		System.err.println("ClipKeyListener.cut");
		copy();
		renderer.replaceRange("", textcomp.getSelectionStart(), textcomp.getSelectionEnd());
	}

	/**
		Einfuegen des aktuellen Clipboard-Textes aus der TextComponent.
		mittels Funktions-Aufruf des ClipTextRenderer
	*/
	public void paste()	{
		if (textcomp.isEditable() == false)
			return;			
		System.err.println("ClipKeyListener.paste");
		Transferable t = ((Component)textcomp).getToolkit().getSystemClipboard().getContents(this);
		if (t != null)	{
			try	{
				if (t.isDataFlavorSupported(DataFlavor.stringFlavor))	{
					String pasteText = (String)t.getTransferData(DataFlavor.stringFlavor);
					//System.err.println(pasteText);
					boolean selected = false;
					try	{
						String s = textcomp.getSelectedText();
						selected = s != null && s.equals("") == false;
					}
					catch (StringIndexOutOfBoundsException se)	{ }
					if (selected)	// Text ist selektiert
						renderer.replaceRange(pasteText, textcomp.getSelectionStart(), textcomp.getSelectionEnd());
					else
						renderer.insert(pasteText, textcomp.getCaretPosition());
				}
			}
			catch (Exception e)	{
				System.err.println("Paste Exception: "+e);
				((Component)textcomp).getToolkit().beep();
			}
		}
	}


	// interface KeyListener

	public void keyPressed(KeyEvent e)	{
		/** Clipboard Cut/Copy/Paste Abhandlung fuer das Textfeld */
		if (e.isControlDown())	{
			if (e.getKeyCode() == KeyEvent.VK_C)	{	// Copy
				// System.err.println("Ctrl-C");
				e.consume();
				copy();
			}
			if (e.getKeyCode() == KeyEvent.VK_V)	{	// Paste
				// System.err.println("Ctrl-V");
				e.consume();
				paste();
			}
			if (e.getKeyCode() == KeyEvent.VK_X)	{	// Cut
				// System.err.println("Ctrl-X");
				e.consume();
				cut();
			}
		}
	}
	public void keyReleased(KeyEvent e)	{
	}
	public void keyTyped(KeyEvent e)	{
	}


	// interface ClipboardOwner

	public void lostOwnership(Clipboard c, Transferable t)	{
		//System.err.println("Clipboard lost ownership");
	}
}