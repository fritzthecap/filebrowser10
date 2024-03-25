package fri.gui.swing.xmleditor.view;

import java.awt.Frame;
import java.awt.event.*;
import javax.swing.*;
import fri.util.xml.DOMUtil;
import fri.gui.swing.text.MultilineEditDialog;

/**
	A modal dialog that lets edit the text of an element.
*/

public class ElementEditDialog extends MultilineEditDialog
{
	private JMenuItem convert;
	private boolean inited;


	/** Create a modal dialog for editing an element text, add convert menuitem if canBeCDATA. */
	public ElementEditDialog(Frame parent, JComponent launcher, String text, String title, boolean canBeCDATA)	{
		super(parent, launcher, text, title, true);	// must be modal because cell editor relies on that

		if (canBeCDATA)
			createConvertMenuItem(text);

		if (isCDATA(text))
			textarea.setText(text.trim());	// to get CDATA marks at start and end

		inited = true;	// now setVisible() will work
		setVisible(true);
	}


	/** Overridden to do nothing if not inited. */
	public void setVisible(boolean visible)	{
		if (visible == false || inited)	{
			super.setVisible(visible);
		}
	}


	/* Create an additional menu item that lets convert to and from CDATA. */
	private void createConvertMenuItem(String text)	{
		JPopupMenu popup = getPopup();
		convert = new JMenuItem();
		convert.addActionListener(this);
		popup.insert(convert, 0);
		popup.insert(new JPopupMenu.Separator(), 1);

		setConvertMenuLabel(text);
	}


	/** Overridden to catch convert menu item. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == convert)	{
			String text = getText();

			if (isCDATA(text))	{
				int len = text.length();
				int start = len - DOMUtil.CDEND.length();
				textarea.replaceRange("", start, len);	// remove end tag
				textarea.replaceRange("", 0, DOMUtil.CDATA.length());	// remove start tag
			}
			else	{
				textarea.insert(DOMUtil.CDATA, 0);
				textarea.append(DOMUtil.CDEND);
			}

			setConvertMenuLabel(getText());
		}
		else	{
			super.actionPerformed(e);
		}
	}

	private boolean isCDATA(String text)	{
		return text.trim().startsWith(DOMUtil.CDATA) && text.endsWith(DOMUtil.CDEND);
	}

	private void setConvertMenuLabel(String text)	{
		convert.setText(isCDATA(text) ? "Unmark To TEXT" : "Mark As CDATA");
	}

}
