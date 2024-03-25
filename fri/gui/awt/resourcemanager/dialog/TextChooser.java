package fri.gui.awt.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import fri.util.i18n.*;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

public class TextChooser extends AwtResourceChooser implements
	ItemListener,
	MultiLanguage.ChangeListener
{
	protected MultiLanguageString text;
	protected String selectedLanguage;
	private TextArea textArea;
	private Choice languageChoice;
	private Container panel;
	
	/** Create a label text editor that allows multiple languages. */
	public TextChooser(MultiLanguageString text)	{
		this(text, "Text");
	}
	
	public TextChooser(MultiLanguageString text, String type)	{
		this.text = text != null ? (MultiLanguageString) text.clone() : null;
		build(type);
		init();
		listen();
	}
	
	protected void build(String type)	{
		panel = new Panel(new BorderLayout());
		textArea = new TextArea();
		panel.add(textArea, BorderLayout.CENTER);
		languageChoice = new Choice();
		Panel upper = new Panel(new BorderLayout());
		upper.add(new Label("Language: ", Label.RIGHT), BorderLayout.CENTER);
		upper.add(languageChoice, BorderLayout.EAST);
		panel.add(upper, BorderLayout.NORTH);
	}
	
	protected void init()	{
		textArea.setText(text != null ? text.toString() : "");
		fillLanguages();
	}
	
	protected void fillLanguages()	{
		String [] languages = MultiLanguage.getLanguages();
		int toSelect = -1;

		for (int i = 0; i < languages.length; i++)	{
			addLanguage(languages[i]);
			if (languages[i].equals(MultiLanguage.getLanguage()))
				toSelect = i;
		}

		if (toSelect >= 0)	{
			selectLanguage(toSelect);
			selectedLanguage = languages[toSelect];
		}
	}

	protected void addLanguage(String language)	{
		languageChoice.add(language);
	}

	protected void selectLanguage(int i)	{
		languageChoice.select(i);
	}

	protected void listen()	{
		MultiLanguage.addChangeListener(this);
		languageChoice.addItemListener(this);
	}


	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Commits all inputs and returns the currently chosen multilanguage text. */
	public Object getValue()	{
		if (getLabelText().length() > 0)	{
			if (text == null)
				text = new MultiLanguageString(selectedLanguage, getLabelText());
			else
				text.setTranslation(selectedLanguage, getLabelText());
		}
		else	// no text in textarea
		if (text != null)	{	// value is not null
			text.setTranslation(selectedLanguage, "");
			// ignore empty MultiLanguageString and return empty instance to be able to replace button text by icon
		}
		return text;
	}

	/** Implements ResourceChooser: Returns the ResourceFactory.TEXT. */
	public String getResourceTypeName()	{
		return ResourceFactory.TEXT;
	}


	protected String getLabelText()	{
		return textArea.getText();
	}
	
	protected void setLabelText(String s)	{
		textArea.setText(s == null ? "" : s);
	}
	
	protected String getSelectedLanguage()	{
		return languageChoice.getSelectedItem();
	}


	/** Interface ItemListeber: language changed, show another translation. */
	public void itemStateChanged(ItemEvent e)	{
		if (e.getStateChange() == ItemEvent.SELECTED)
			selectedLanguageChanged();
	}
	
	protected void selectedLanguageChanged()	{
		getValue();	// commit
		selectedLanguage = getSelectedLanguage();
		if (text != null)
			setLabelText(text.getTranslation(selectedLanguage));
	}

	protected int getLanguageChoiceCount()	{
		return languageChoice.getItemCount();
	}

	protected String getLanguageChoice(int i)	{
		return languageChoice.getItem(i);
	}

	protected void addLanguageChoice(String language)	{
		languageChoice.add(language);
	}

	protected void removeLanguageChoice(String language)	{
		languageChoice.remove(language);
	}
	
	
	/** Interface MultiLanguage.ChangeListener: Called when a language was added. */
	public void languageAdded(String newLanguage)	{
		addLanguageChoice(newLanguage);
	}
	
	/** Interface MultiLanguage.ChangeListener: Called when a language was removed. */
	public void languageRemoved(String language)	{
		removeLanguageChoice(language);
	}
	
	public void languageChanged(String newLanguage)	{}
	

	// test main
	public static void main(String [] args)	{
		Frame f = new Frame("Text Chooser");
		f.setLayout(new BorderLayout());
		MultiLanguageString mls = new MultiLanguageString("Langer langer Text");
		mls.setTranslation("Englisch", "Long long Text");
		f.add(new TextChooser(mls).getPanel());
		f.pack();
		f.setVisible(true);
	}

}
