package fri.gui.swing.combo.history;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
	The editor for history combo boxes.
	@author Fritz Ritzberger
*/

class HistComboEditor implements
	ComboBoxEditor
{
	protected JTextField editor;
	
	public HistComboEditor() {
		editor = new BorderlessClipableTextField();
	}
	
	public Component getEditorComponent() {
		return editor;
	}
	
	/** Set a new item into textfield. */
	public void setItem(Object anObject) {
		if (anObject != null)
			editor.setText(anObject.toString());
		else
			editor.setText("");
	}
	
	/** Get the current item from textfield. */
	public Object getItem() {
		return editor.getText();
	}
	
	/** Select all text in textfield. */
	public void selectAll() {
		editor.selectAll();
		editor.requestFocus();
	}
	
	/** Adds an actionlistener to textfield. */
	public void addActionListener(ActionListener l) {
		editor.addActionListener(l);
	}
	
	/** Removes an actionlistener from textfield. */
	public void removeActionListener(ActionListener l) {
		editor.removeActionListener(l);
	}


	public void setEnabled(boolean enabled)	{
		editor.setEnabled(enabled);
	}


	static class BorderlessClipableTextField extends JTextField
	{
		public BorderlessClipableTextField() {
			super("", 6);
		}
		public void setBorder(Border b) {
			if (UIManager.getLookAndFeel().getName().equals("Metal"))
				super.setBorder(b);
		}
	}

}