package fri.gui.swing.mailbrowser.rules.editor;

import java.io.*;
import java.net.URL;
import java.util.*;
import fri.util.error.Err;
import fri.util.props.*;
import fri.util.ruleengine.PropertiesRuleExecutionSet;
import fri.gui.mvc.model.ModelItem;
import fri.gui.mvc.model.swing.AbstractMutableTableModel;
import fri.gui.swing.mailbrowser.Language;
import fri.gui.swing.mailbrowser.RulesUrl;

public class RulesTableModel extends AbstractMutableTableModel
{
	private static Vector columnNames = new Vector(PropertiesRuleExecutionSet.attributes.length);
	private static Vector rules;

	static	{	// load the rules
		PropertiesRuleExecutionSet prs = null;
		URL url = RulesUrl.getMailRulesUrl();
		if (url != null)
			prs = PropertiesRuleExecutionSet.getRuleSet(url.toString());
		
		if (prs != null)	{
			rules = (Vector)((Vector)prs.getRules()).clone();	// do not modify original rules list

			for (int i = 0; i < rules.size(); i++)	{
				Vector row = (Vector)rules.get(i);
				rules.set(i, new RulesTableRow(row));	// clone rule Vector to not modify original rule
			}
		}
		else	{
			rules = new Vector();
		}
		
		for (int i = 0; i < PropertiesRuleExecutionSet.attributes.length; i++)	{
			columnNames.add(Language.get(PropertiesRuleExecutionSet.attributes[i]));
		}
	}

	private RulesTableRow templateRow = new RulesTableRow();
	

	public RulesTableModel()	{
		super(rules, columnNames);
	}
	

	/** Delegates to private template row. */
	public Class getColumnClass(int column)	{
		return templateRow.get(column).getClass();
	}
	
	/** Always returns true as this table is fully editable. */
	public boolean isCellEditable(int row, int column)	{
		return true;
	}

	/** Overridden to fire table changed only if the value really has changed. */
	public void setValueAt(Object aValue, int row, int column)	{
		Object o1 = getValueAt(row, column);
		if (o1 instanceof Vector)
			o1 = ((Vector)o1).get(0);
		Object o2 = aValue;
		if (o2 instanceof Vector)
			o2 = ((Vector)o2).get(0);

		if (o2.equals(o1))
			return;
		
		System.err.println("aValue is "+aValue+" getValueAt is "+o1);
		super.setValueAt(aValue, row, column);
	}




	/** Stores the addresses to a property file. */
	public void save()	{
		// convert Vectors in rows to the string at first (chosen) position
		Vector persistentRules = new Vector(getRowCount());
		
		for (int i = 0; i < getRowCount(); i++)	{
			RulesTableRow ruleRow = getRulesTableRow(i);
			String compareValue = (String)ruleRow.get(PropertiesRuleExecutionSet.CONDITION_VALUE);
			
			if (compareValue.length() > 0)	{	// ignore incomplete rules
				Vector row = ruleRow.convertToPersistence();
				persistentRules.add(row);
			}
		}

		URL url = RulesUrl.getMailRulesUrl();
		File file = new File(url.getFile());
		
		if (persistentRules.size() > 0)	{
			FileOutputStream fos = null;
			Properties props = TableProperties.convert(persistentRules, PropertiesRuleExecutionSet.entityType, PropertiesRuleExecutionSet.attributes);
			try	{
				fos = new FileOutputStream(file);
				props.store(fos, "Fri-Mail rules "+new Date());
				
				PropertiesRuleExecutionSet.clearCache();	// force refresh from disk next time the factory is called
			}
			catch (IOException e)	{
				Err.error(e);
			}
			finally	{
				try	{ fos.close(); }	catch (Exception e)	{}
			}
		}
		else	{	// no rules defined, delete file
			file.delete();
		}
	}
	

	// MVC framework
	
	/** Implements AbstractMutableTableModel: returns a ModelItem wrapper for a table row. */
	public ModelItem createModelItem(Vector row)	{
		return new RulesTableModelItem((RulesTableRow)row);
	}


	/** Returns the typed row at passed index or null if index is not in space. */
	public RulesTableRow getRulesTableRow(int index)	{
		return (RulesTableRow)getRow(index);
	}

}