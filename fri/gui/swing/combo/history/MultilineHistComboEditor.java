package fri.gui.swing.combo.history;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.text.MultilineEditDialog;

/**
	The editor for multiline history combo boxes.
	Insists on a JTextField that does NOT filter newlines!
	@author Fritz Ritzberger
*/

class MultilineHistComboEditor extends HistComboEditor implements
	ActionListener
{
	private JButton openEditor;
	private JPanel panel;
	private MultilineHistCombo combo;


	public MultilineHistComboEditor(MultilineHistCombo combo) {
		super();
		this.combo = combo;
		init();
	}

	public void setEnabled(boolean enabled)	{
		super.setEnabled(enabled);
		openEditor.setEnabled(enabled);
	}

	private void init()	{
		openEditor = new JButton("...");
		openEditor.setMargin(new Insets(0, 0, 0, 0));
		openEditor.addActionListener(this);
		openEditor.setBackground(editor.getBackground());
		openEditor.setFocusPainted(false);
		openEditor.setToolTipText("Click To Edit Multiline Text");
		panel = new JPanel(new BorderLayout());
		panel.add(editor, BorderLayout.CENTER);
		panel.add(openEditor, BorderLayout.EAST);
		
		// needed in JDK 1.4 to switch off newline filtering
		editor.getDocument().putProperty("filterNewlines", Boolean.FALSE);
	}
	
	public Component getEditorComponent() {
		return panel;
	}
	
	Component getTextField() {
		return editor;
	}
	

	/** Implements ActionListener to start editor window */
	public void actionPerformed(ActionEvent e)	{
		// no doubt about event source
		Window w = ComponentUtil.getWindowForComponent(editor);
		MultilineEditDialog dlg;
		
		if (w instanceof Frame)
			dlg = new MultilineEditDialog((Frame)w, editor, editor.getText());
		else
			dlg = new MultilineEditDialog((Dialog)w, editor, editor.getText());
		
		String s = dlg.getText();
		//System.err.println("MultilineHistoryCombo setting text: >"+s+"<");
		combo.setText(s);
		//System.err.println("MultilineHistoryCombo received text: >"+combo.getText()+"<");
	}

}