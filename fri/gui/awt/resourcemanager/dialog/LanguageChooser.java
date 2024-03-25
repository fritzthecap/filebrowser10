package fri.gui.awt.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import fri.util.text.Replace;
import fri.util.i18n.*;

public class LanguageChooser extends AwtResourceChooser implements
	ActionListener,
	ItemListener,
	KeyListener
{
	private TextField languageTextField;
	private List languageList;
	private Button addButton, removeButton;
	private Panel panel;
	
	/** If restricted is true, only the selection list (without any listener) is provided. */
	public LanguageChooser(boolean restricted)	{
		buildRestricted();
		if (restricted == false)	{
			build();
			listen();
		}
		init();
	}
	
	protected void buildRestricted()	{
		panel = new Panel(new BorderLayout());
		panel.add(languageList = new List(), BorderLayout.CENTER);
	}

	protected void build()	{
		Panel p0 = new Panel(new BorderLayout());
		languageTextField = new TextField(8);
		p0.add(languageTextField, BorderLayout.CENTER);
		Panel p1 = new Panel();
		p1.add(addButton = new Button("Add"));
		addButton.setEnabled(false);
		p1.add(removeButton = new Button("Remove"));
		p0.add(p1, BorderLayout.EAST);
		panel.add(p0, BorderLayout.SOUTH);
	}
	
	private void init()	{
		String [] languages = MultiLanguage.getLanguages();
		int toSelect = -1;
		for (int i = 0; i < languages.length; i++)	{
			addLanguageItem(languages[i]);
			if (languages[i].equals(MultiLanguage.getLanguage()))
				toSelect = i;
		}
		if (toSelect >= 0)
			setSelectedLanguageIndex(toSelect);
	}
	
	protected void listen()	{
		languageList.addItemListener(this);
		languageTextField.addKeyListener(this);
		languageTextField.addActionListener(this);
		removeButton.addActionListener(this);
		addButton.addActionListener(this);
	}
	
	protected void addLanguageItem(String language)	{
		languageList.add(language);
	}
	
	protected void setSelectedLanguageIndex(int i)	{
		languageList.select(i);
	}
	
	
	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the currently chosen language. */
	public Object getValue()	{
		String language = getSelectedLanguageItem();
		return language != null && language.equals("") == false	? language : null;
	}

	/** Implements ResourceChooser: Returns ResourceChooser.LANGUAGE. */
	public String getResourceTypeName()	{
		return ResourceChooser.LANGUAGE;
	}



	/** Interface ActionListener: answer to add and delete buttons, and to textfield ENTER keypress. */
	public void actionPerformed(ActionEvent e)	{
		if (isDeleteAction(e))	{	// delete a language from list
			String item = getSelectedLanguageItem();
			int i = getSelectedLanguageIndex();
			if (i >= 0)	{
				removeLanguageItem(i);
				MultiLanguage.removeLanguage(item);
				setLanguageTextFieldText("");
				if (getLanguageCount() <= i)
					i = getLanguageCount() - 1;
				if (i >= 0)
					setSelectedLanguageIndex(i);
			}
		}
		else
		if (isAddAction(e))	{
			String language = getModifiedLanguageTextFieldText();
			if (language.length() > 0)	{
				int i = addLanguage(language);
				if (i >= 0)	// already contained
					setSelectedLanguageIndex(i);
				else	// select last item that was added
					setSelectedLanguageIndex(getLanguageCount() - 1);
			}
		}
	}
	
	protected boolean isAddAction(ActionEvent e)	{
		return e.getSource() == addButton || e.getSource() == languageTextField;
	}

	protected boolean isDeleteAction(ActionEvent e)	{
		return e.getSource() == removeButton;
	}

	protected int addLanguage(String language)	{
		int i = getListIndex(language);
		if (i < 0)	{	// not contained
			MultiLanguage.addLanguage(language);
			addLanguageItem(language);
		}
		return i;
	}
	
	protected String getSelectedLanguageItem()	{
		return languageList.getSelectedItem();
	}

	protected int getSelectedLanguageIndex()	{
		return languageList.getSelectedIndex();
	}
	
	protected void removeLanguageItem(int i)	{
		languageList.remove(i);
	}

	protected String getItem(int i)	{
		return languageList.getItem(i);
	}
		
	protected int getLanguageCount()	{
		return languageList.getItemCount();
	}
	
	protected void setLanguageTextFieldText(String language)	{
		languageTextField.setText(language);
	}
	
	protected String getLanguageTextFieldText()	{
		return languageTextField.getText();
	}
	
	protected void enableAddButton(boolean enable)	{
		addButton.setEnabled(enable);
	}
	
	private String getModifiedLanguageTextFieldText()	{
		String s = getLanguageTextFieldText();
		s = Replace.replace(s, "#", "_");	// Properties special chars
		s = Replace.replace(s, ":", "_");
		s = Replace.replace(s, "=", "_");
		s = Replace.replace(s, " ", "_");
		s = Replace.replace(s, "!", "_");
		return s;
	}
	
	private int getListIndex(String item)	{
		for (int i = 0; i < getLanguageCount(); i++)
			if (getItem(i).equals(item))
				return i;
		return -1;
	}
	
	/** Interface ItemListener: a language item gets selected, save old contents from textarea, render new item. */
	public void itemStateChanged(ItemEvent e)	{
		if (e.getStateChange() == ItemEvent.SELECTED)	{
			int i = ((Integer)e.getItem()).intValue();
			String language = languageList.getItem(i);
			setLanguageTextFieldText(language);
		}
	}
	
	/** Interface KeyListener: enable cursor up and down to choose language item. */
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_UP)	{
			int i = getSelectedLanguageIndex();
			if (i > 0)
				setSelectedLanguageIndex(i - 1);
		}
		else
		if (e.getKeyCode() == KeyEvent.VK_DOWN)	{
			int i = getSelectedLanguageIndex();
			if (i < getLanguageCount() - 1)
				setSelectedLanguageIndex(i + 1);
		}
	}
	public void keyReleased(KeyEvent e)	{
		String s = getLanguageTextFieldText();
		enableAddButton(s != null && s.length() > 0 && getListIndex(s) < 0);
	}
	public void keyTyped(KeyEvent e)	{}


	// test main
	public static void main(String [] args)	{
		Frame f = new Frame("Language Chooser");
		f.setLayout(new BorderLayout());
		f.add(new LanguageChooser(false).getPanel());
		f.pack();
		f.setVisible(true);
	}

}
