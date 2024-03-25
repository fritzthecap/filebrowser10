package fri.gui.swing.resourcemanager.dialog;

import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.*;
import fri.gui.awt.resourcemanager.dialog.LanguageChooser;

public class JLanguageChooser extends LanguageChooser implements
	ListSelectionListener
{
	private JTextField languageTextField;
	private JList languageList;
	private JButton addButton, removeButton, chooseButton;
	private JPanel panel;
	private static String [] availableLanguageDisplayNames;
	
	/** If restricted is true, only the selection list (without any listener) is provided. */
	public JLanguageChooser(boolean restricted)	{
		super(restricted);
	}
	
	protected void buildRestricted()	{
		panel = new JPanel(new BorderLayout());
		panel.add(new JScrollPane(languageList = new JList(new DefaultListModel())), BorderLayout.CENTER);
		languageList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	
	protected void build()	{
		JPanel p0 = new JPanel(new BorderLayout());
		languageTextField = new JTextField(8);
		p0.add(languageTextField, BorderLayout.CENTER);
		JPanel p1 = new JPanel();
		p1.add(addButton = new JButton("Add"));
		addButton.setEnabled(false);
		p1.add(removeButton = new JButton("Remove"));
		p1.add(chooseButton = new JButton("Choose"));
		p0.add(p1, BorderLayout.EAST);
		panel.add(p0, BorderLayout.SOUTH);
	}
	
	protected void addLanguageItem(String language)	{
		((DefaultListModel)languageList.getModel()).addElement(language);
	}
	
	protected void setSelectedLanguageIndex(int i)	{
		languageList.setSelectedIndex(i);
	}

	protected void listen()	{
		languageList.addListSelectionListener(this);
		languageTextField.addActionListener(this);
		languageTextField.addKeyListener(this);
		addButton.addActionListener(this);
		chooseButton.addActionListener(this);
		removeButton.addActionListener(this);
	}
	
	
	public Container getPanel()	{
		return panel;
	}


	protected boolean isAddAction(ActionEvent e)	{
		return e.getSource() == addButton || e.getSource() == languageTextField;
	}

	protected boolean isDeleteAction(ActionEvent e)	{
		return e.getSource() == removeButton;
	}

	protected String getSelectedLanguageItem()	{
		return (String) languageList.getSelectedValue();
	}

	protected int getSelectedLanguageIndex()	{
		return languageList.getSelectedIndex();
	}
	
	protected void removeLanguageItem(int i)	{
		((DefaultListModel)languageList.getModel()).removeElementAt(i);
	}

	protected String getItem(int i)	{
		return (String) languageList.getModel().getElementAt(i);
	}
		
	protected int getLanguageCount()	{
		return languageList.getModel().getSize();
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


	/** Interface ActionListener: catch additional "choose" button to open an i18n list. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == chooseButton)	{	// choose from available Locales
			chooseButton.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			JPanel p = new JPanel(new BorderLayout());
			JList i18nList = new JList(new DefaultListModel());
			fillInternationalList((DefaultListModel) i18nList.getModel());
			p.add(new JScrollPane(i18nList));
			
			JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
			JDialog dlg = pane.createDialog(getPanel(), "Available Languages");
			dlg.setResizable(true);
			dlg.setVisible(true);
			Object ret = pane.getValue();
			
			if (ret != null)	{
				Object [] selected = i18nList.getSelectedValues();
				if (selected != null && selected.length > 0)	{
					for (int i = 0; i < selected.length; i++)
						addLanguage((String) selected[i]);
					setSelectedLanguageIndex(getLanguageCount() - 1);
				}
			}
			chooseButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		else	{
			super.actionPerformed(e);
		}
	}
	
	private void fillInternationalList(DefaultListModel model)	{
		if (availableLanguageDisplayNames == null)	{	// get all language names into static array
			Locale [] locales = Locale.getAvailableLocales();
			Map map = new TreeMap();
			for (int i = 0; i < locales.length; i++)
				map.put(locales[i].getDisplayLanguage(), "");
			availableLanguageDisplayNames = new String [map.size()];
			int i = 0;
			for (Iterator it = map.keySet().iterator(); it.hasNext(); i++)
				availableLanguageDisplayNames[i] = (String) it.next();
		}
		for (int i = 0; i < availableLanguageDisplayNames.length; i++)
			model.addElement(availableLanguageDisplayNames[i]);
	}
	
	
	/** Interface ListSelectionListener: a language item gets selected, render new item. */
	public void valueChanged(ListSelectionEvent e)	{
		if (e.getValueIsAdjusting())
			return;
		languageTextField.setText(getSelectedLanguageItem());
	}



	// test main
	public static void main(String [] args)	{
		Frame f = new Frame("Language Chooser");
		f.setLayout(new BorderLayout());
		f.add(new JLanguageChooser(false).getPanel());
		f.pack();
		f.setVisible(true);
	}

}
