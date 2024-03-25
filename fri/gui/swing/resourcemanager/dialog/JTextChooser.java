package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import fri.util.i18n.*;
import fri.gui.awt.resourcemanager.dialog.TextChooser;

public class JTextChooser extends TextChooser implements
	ActionListener
{
	private JTextArea textArea;
	private JComboBox languageChoice;
	private JPanel panel;
	private String type;
	
	/** Create a label text editor that allows multiple languages. @param type "Tooltip" or "Text". */
	public JTextChooser(MultiLanguageString text, String type)	{
		super(text, type);
		this.type = type;
	}

	protected void build(String type)	{
		panel = new JPanel(new BorderLayout());
		textArea = new JTextArea();
		panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		languageChoice = new JComboBox();
		JPanel upper = new JPanel(new BorderLayout());
		upper.add(new JLabel("Language: ", JLabel.RIGHT), BorderLayout.CENTER);
		upper.add(languageChoice, BorderLayout.EAST);
		panel.add(upper, BorderLayout.NORTH);
	}
	
	protected void init()	{
		textArea.setText(text != null ? text.toString() : "");
		fillLanguages();
	}
	
	protected void addLanguage(String language)	{
		languageChoice.addItem(language);
	}

	protected void selectLanguage(int i)	{
		languageChoice.setSelectedIndex(i);
	}

	protected void listen()	{
		MultiLanguage.addChangeListener(this);
		languageChoice.addActionListener(this);
	}


	public Container getPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the type passed in constructor. */
	public String getResourceTypeName()	{
		return type;
	}


	protected String getLabelText()	{
		return textArea.getText();
	}
	
	protected void setLabelText(String s)	{
		textArea.setText(s == null ? "" : s);
	}
	
	protected String getSelectedLanguage()	{
		return (String) languageChoice.getSelectedItem();
	}

	
	/** Interface ActionListener: language changed, show another translation. */
	public void actionPerformed(ActionEvent e)	{
		selectedLanguageChanged();
	}

	protected int getLanguageChoiceCount()	{
		return languageChoice.getItemCount();
	}

	protected String getLanguageChoice(int i)	{
		return (String) languageChoice.getItemAt(i);
	}

	protected void addLanguageChoice(String language)	{
		languageChoice.addItem(language);
	}

	protected void removeLanguageChoice(String language)	{
		languageChoice.removeItem(language);
	}


	// test main
	public final static void main(String [] args)	{
		JFrame f = new JFrame("Text Chooser");
		MultiLanguageString mls = new MultiLanguageString("Langer langer Text");
		mls.setTranslation("Englisch", "Long long Text");
		f.getContentPane().add(new JTextChooser(mls, "Text").getPanel());
		f.pack();
		f.setVisible(true);
	}

}
