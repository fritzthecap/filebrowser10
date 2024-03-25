package fri.gui.swing.propertiestextfield;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import fri.util.props.*;

/**
	The editor for Properties combo box, that shows
	a label and an editable text.

	@author  Ritzberger Fritz
 */

public class PropertiesComboEditor extends JPanel implements
	ComboBoxEditor,
	FocusListener
{
	private JTextField editor;
	private JLabel propName;
	private PropertiesList.Tuple nameValue;
	private boolean inited = false, saved = true;


	/**
		Create the editor. The label and the textfield are allocated.
		@param rend the renderer, needed for calculating the width of label.
	*/
	public PropertiesComboEditor() {
		super(new BorderLayout());

		//setBorder(null);

		editor = createTextField();
		editor.addFocusListener(this);	// save data if loosing focus

		propName = new JLabel(" ");
		propName.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		propName.addMouseListener(new MouseAdapter(){});	// else Mouseclick is not read away

		add(propName, PropertiesComboRenderer.labelSide);
		add(editor, BorderLayout.CENTER);
	}

	/** Override this to set another textfield. */
	protected JTextField createTextField()	{	
		return new BorderlessTextField();
	}


	public Insets getInsets()	{
		return new Insets(0, 0, 0, 0);
	}
	
	/** Called by paint() method. Returns this PropertiesComboEditor. */
	public Component getEditorComponent() {
		setBackground(editor.getBackground());
		return this;
	}
	
	/** Returns the TextField */
	public Component getTextField() {
		return editor;
	}
	
	/** Called when editor is activated or committed. */
	public void setItem(Object o) {
		//Thread.dumpStack();
		//System.err.println("setItem "+o);		
		if (saved == false && inited == true)
			getItem();
		
		saved = false;
		
		if (o != null)	{
			inited = true;
			
			nameValue = (PropertiesList.Tuple)o;
			propName.setText(nameValue.name);
			editor.setText(nameValue.value);
		}
		else	{
			inited = false;
			
			editor.setText("");
			propName.setText(" ");
		}
	}
	
	/** called when editor is finished */
	public Object getItem() {
		if (nameValue != null)
			nameValue.value = editor.getText();
		saved = true;
		//System.err.println("getItem "+t.text);
	  return nameValue;
	}
	
	/** delegate method to editor */
	public void selectAll() {
		editor.selectAll();
		editor.requestFocus();
	}
	
	/** delegate method to editor */
	public void addActionListener(ActionListener l) {
		editor.addActionListener(l);
	}
	
	/** delegate method to editor */
	public void removeActionListener(ActionListener l) {
		editor.removeActionListener(l);
	}



	/** implements FocusListener */
	public void focusGained(FocusEvent e)	{
	}	
	/** implements FocusListener to save edited data */
	public void focusLost(FocusEvent e)	{
		//System.err.println("focusLost "+t.text);
		getItem();
	}


	public void setBackground(Color c)	{
		super.setBackground(c);
		if (editor != null)
			editor.setBackground(c);
	}
	public void setForeground(Color c)	{
		super.setForeground(c);
		if (editor != null)
			editor.setForeground(c);
	}
	public void setFont(Font f)	{
		super.setFont(f);
		if (editor != null)
			editor.setFont(f);
		if (propName != null)
			propName.setFont(f);
	}

	
	static class BorderlessTextField extends JTextField {
	    public void setBorder(Border b) {
			if (UIManager.getLookAndFeel().getName().equals("Metal"))
				super.setBorder(b);
		}
	}
	
}