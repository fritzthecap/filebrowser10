package fri.gui.swing.propertiestextfield;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import fri.gui.swing.combo.*;
import fri.util.props.*;

/**
 * Textfield that can edit a list of name-value pairs.
 * It shows a list where the name is left and the value is right.
 * This is made for String Properties that are single-line.
 * The combobox always works upon a clone of the passed list.
 *
 * @author  Ritzberger Fritz
 */

public class PropertiesTextField extends WideComboBox
{
	private PropertiesComboRenderer renderer;
	private PropertiesComboEditor editor;
	private PropertiesList propList;
	private ActionListener actionListener;


	/** Create an empty Properties textfield. */
	public PropertiesTextField()	{
		this(null);
	}
	
	/** Create an Properties textfield loaded with passed data. */
	public PropertiesTextField(PropertiesList propList)	{
		this(propList, true);
	}

	/** Create an Properties textfield loaded with passed data. */
	public PropertiesTextField(PropertiesList propList, boolean editable)	{
		super();	// default model, no data
		setEditable(editable);
		setText(propList);
	}


	/** Set a new Properties list to that editor. The list will be cloned. */
	public void setText(PropertiesList propList)	{
		// set renderer and editor
		this.propList = propList;

		if (propList == null)	{
			setModel(new DefaultComboBoxModel(new Vector(0)));
		}
		else	{
			this.propList = (PropertiesList)propList.clone();

			ensureRendererAndEditor(this.propList);

			// set data model
			setModel(new DefaultComboBoxModel(this.propList));
			takePopupSize();
		}
	}


	private void ensureRendererAndEditor(PropertiesList propList)	{
		if (renderer == null)	{
			renderer = createRenderer();
			setRenderer(renderer);
		}
		if (editor == null)	{
			editor = createEditor();
			setEditor(editor);
		}
	}

	/** Override this to set another renderer than PropertiesComboRenderer. */
	protected PropertiesComboRenderer createRenderer()	{
		return new PropertiesComboRenderer();
	}
	/** Override this to set another editor than PropertiesComboEditor. */
	protected PropertiesComboEditor createEditor()	{
		return new PropertiesComboEditor();
	}
	
	
	/** @return the Properties list that might have been edited */
	public PropertiesList getText()	{
		if (editor != null)
			editor.getItem();
		return propList;
	}


	/** Returns JTextField for ComboBox */
	public Component getTextEditor()	{
		if (editor != null)
			return editor.getTextField();
		return null;
	}


	/**
		Overridden to manage a private listener list.
		The underlying list is manipulated!
	*/
	public void addActionListener(ActionListener l)	{
		actionListener = AWTEventMulticaster.add(actionListener, l);
	}
	/** Overridden to manage a private listener list.*/
	public void removeActionListener(ActionListener l) {
		actionListener = AWTEventMulticaster.remove(actionListener, l);
	}

	/** Overridden to feed the private listener list.*/
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (isPopupVisible() == false)	// popup selection fires ActionEvents!
			actionListener.actionPerformed(e);
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
		if (renderer != null)
			renderer.setFont(f);
	}


	/* test main
	public static final void main(String [] args)	{
		Properties props = new Properties();
		props.setProperty("Hallo", "Welt");
		props.setProperty("Hello", "World");
		props.setProperty("Allo", "Monde");
		final PropertiesList propList = new PropertiesList(props);
		
		JFrame f = new JFrame("PropertiesTextField");
		f.addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				System.err.println(propList.toString());
				System.exit(1);
			}
		});
		f.getContentPane().setLayout(new FlowLayout());

		PropertiesTextField mltf = new PropertiesTextField(propList);
		mltf.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				System.err.println("actionPerformed "+e.getActionCommand());
				//Thread.dumpStack();
			}
		});
		
		f.getContentPane().add(mltf);
		f.setSize(new Dimension(200, 200));
		f.show();
	}
	*/

}
