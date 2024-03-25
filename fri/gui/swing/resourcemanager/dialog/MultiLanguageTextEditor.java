package fri.gui.swing.resourcemanager.dialog;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import fri.util.i18n.*;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.TextResource;

public class MultiLanguageTextEditor implements
	MultiLanguage.ChangeListener
{
	private ResourceList textResources;
	private Component panel;
	private JTable table;
	private boolean changed;
	private JLanguageChooser languageChooser;
	
	/** Create a text editor that allows to edit all labels in all languages. @param textResources all TextResources. */
	public MultiLanguageTextEditor(ResourceList textResources)	{
		this.textResources = textResources;
		build();
		MultiLanguage.addChangeListener(this);
	}

	/** Returns the addable component of the editor. */
	public Component getPanel()	{
		return panel;
	}
	
	/** Returns true if any value has been changed, deleted or inserted. */
	public boolean isChanged()	{
		return changed;
	}
	
	/** Deregisters from language listening. */
	public void close()	{
		MultiLanguage.removeChangeListener(this);
		MultiLanguage.setLanguage((String) languageChooser.getValue());
	}
	
	private void build()	{
		Collections.sort(textResources);

		table = new JTable();	// create table
		updateTableModel();
		languageChooser = new JLanguageChooser(JCustomizerGUI.showRestricted);
		JComponent languagePanel = (JComponent) languageChooser.getPanel();
		JComponent textPanel = new JScrollPane(table);
		languagePanel.setBorder(BorderFactory.createTitledBorder("Languages"));
		textPanel.setBorder(BorderFactory.createTitledBorder("GUI Texts"));
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, languagePanel, textPanel);
		panel = split;
	}


	private void updateTableModel()	{
		table.setModel(new MultiLanguageTableModel());
	}
	
	
	/** Called when a new language was added. */
	public void languageAdded(String newLanguage)	{
		updateTableModel();
	}

	/** Called when a language was removed. */
	public void languageRemoved(String removedLanguage)	{
		int ret = JOptionPane.showConfirmDialog(getPanel(), "CAUTION: Translations get lost!", "Confirm Language Remove", JOptionPane.OK_CANCEL_OPTION);
		if (ret != JOptionPane.OK_OPTION)
			return;	// TODO: must add language again to LanguageChooser

		for (int i = 0; i < textResources.size(); i++)	{	// remove all translations of removed language
			TextResource textResource = textResources.getItem(i).textResource;
			MultiLanguageString mls = (MultiLanguageString) textResource.getUserValue();
			if (mls != null)
				mls.removeTranslation(removedLanguage);
		}

		updateTableModel();
	}

	public void languageChanged(String newLanguage)	{}



	/** Argument class for this editor. All text resources and their descriptions is contained. */
	static class ResourceList extends ArrayList
	{
		public void add(String componentType, String textResourceType, Resource textResource)	{
			add(new Item(componentType, textResourceType, textResource));
		}
		
		public Item getItem(int i)	{
			return (Item) get(i);
		}
		
		static class Item implements Comparable	// to be sortable by initial programmatic value
		{
			public final String componentType, textResourceType;
			public final TextResource textResource;
			
			Item(String componentType, String textResourceType, Resource textResource)	{
				this.componentType = componentType;
				this.textResourceType = textResourceType;
				this.textResource = (TextResource) textResource;
			}

			public int compareTo(Object o)	{
				Item other = (Item) o;
				if (componentType.equals(other.componentType) == false)
					return componentType.compareTo(other.componentType);
				String initial1 = textResource.getInitialValue() == null ? "" : (String) textResource.getInitialValue();
				String initial2 = other.textResource.getInitialValue() == null ? "" : (String) other.textResource.getInitialValue();
				return initial1.compareTo(initial2);
			}
		}
	}
	
	

	// TableModel with direct connection to textResources.
	private class MultiLanguageTableModel extends AbstractTableModel
	{
		private static final int LEADING_COLUMNS = 3;
		private String [] columns;
		
		MultiLanguageTableModel()	{
			String [] array = MultiLanguage.getLanguages();
			this.columns = new String[array.length + LEADING_COLUMNS];
			this.columns[0] = "Component";
			this.columns[1] = "Type";
			this.columns[2] = "Initial";
			System.arraycopy(array, 0, this.columns, LEADING_COLUMNS, array.length);
		}
		
		public int getRowCount()	{
			return textResources.size();
		}
		
		public int getColumnCount()	{
			return columns.length;
		}
		
		public String getColumnName(int column)	{
			return columns[column];
		}
		
		public Class getColumnClass(int column)	{
			return String.class;
		}
	
		public boolean isCellEditable(int row, int column)	{
			return column >= LEADING_COLUMNS;
		}

		public Object getValueAt(int row, int column)	{
			TextResource textResource = textResources.getItem(row).textResource;
			if (column < LEADING_COLUMNS)	{
				if (column == 0)
					return textResources.getItem(row).componentType;
				if (column == 1)
					return textResources.getItem(row).textResourceType.toUpperCase();
				if (column == 2)
					return textResource.getInitialValue();
			}
			MultiLanguageString mls = (MultiLanguageString) textResource.getUserValue();
			return (mls != null) ? mls.getTranslation(columns[column]) : null;
		}

		public void setValueAt(Object aValue, int row, int column)	{
			TextResource textResource = textResources.getItem(row).textResource;
			MultiLanguageString mls = (MultiLanguageString) textResource.getUserValue();
			
			if (mls == null && aValue != null && ((String) aValue).length() > 0)	{
				mls = new MultiLanguageString();
				textResource.setUserValue(mls);
			}
			
			if (mls != null)	{
				String s = (String) aValue;
				if (s.equals(mls.getTranslation(columns[column])) == false)	{
					mls.setTranslation(columns[column], s);
					changed = true;
				}
				fireTableCellUpdated(row, column);
			}
		}
	}
	
}
