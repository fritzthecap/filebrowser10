package fri.gui.swing.mailbrowser.rules.editor;

import java.util.Vector;
import fri.util.ruleengine.PropertiesRuleExecutionSet;
import fri.gui.mvc.model.swing.DefaultTableRow;
import fri.gui.swing.mailbrowser.Language;
import fri.gui.swing.mailbrowser.rules.MessageRuleWrapper;

/**
	One table row in rules table, which is one rule.
*/

public class RulesTableRow extends DefaultTableRow
{
	private Vector languageNeutral = new Vector();	// internationalization helper
	
	public RulesTableRow()	{
		super(PropertiesRuleExecutionSet.attributes.length);
		fill(null);
	}

	public RulesTableRow(Vector v)	{
		super(v.size());
		fill(v);
	}

	public RulesTableRow(RulesTableRow v)	{
		super(v.size());
		for (int i = 0; i < v.size(); i++)	{
			Object o = v.get(i);
			if (o instanceof Vector)
				o = ((Vector)o).clone();
			add(o);
		}
	}

	private void fill(Vector v)	{
		if (PropertiesRuleExecutionSet.attributes.length != PropertiesRuleExecutionSet.ACTION_ARGUMENT + 1)
			throw new IllegalArgumentException("ERROR: column count is not equal last column + 1 !");
		
		for (int i = 0; i < PropertiesRuleExecutionSet.attributes.length; i++)	{
			String current = v == null ? "" : v.get(i) instanceof Vector ? (String) ((Vector)v.get(i)).get(0) : (String) v.get(i);
			
			if (i == PropertiesRuleExecutionSet.CONDITION_LOGIC)
				add(getChoiceVector(current, PropertiesRuleExecutionSet.possibleConditionLogics));
			else
			if (i == PropertiesRuleExecutionSet.CONDITION_FIELDNAME)
				add(getChoiceVector(current, MessageRuleWrapper.fieldNames));
			else
			if (i == PropertiesRuleExecutionSet.COMPARISON_METHOD)
				add(getChoiceVector(current, PropertiesRuleExecutionSet.possibleComparisonMethods));
			else
			if (i == PropertiesRuleExecutionSet.CONDITION_VALUE)
				add(current);
			else
			if (i == PropertiesRuleExecutionSet.ACTION_NAME)
				add(getChoiceVector(current, MessageRuleWrapper.actionNames));
			else
			if (i == PropertiesRuleExecutionSet.ACTION_ARGUMENT)
				add(current);
		}
	}

	
	/** Un-internationalizes all contained values to language-neutral strings returns new Vector. */
	public Vector convertToPersistence()	{
		Vector v = new Vector(size());
		
		for (int i = 0; i < size(); i++)	{
			Object o = get(i);
			
			if (o instanceof Vector)	{	// put first element instead of Vector
				v.add(languageNeutral((String) ((Vector)o).get(0)));
			}
			else	{	// else: must be runtime String
				v.add(o);
			}
		}
		
		return v;
	}
	
	private String languageNeutral(String s)	{
		for (int i = 0; i < languageNeutral.size(); i++)	{
			if (Language.get((String)languageNeutral.get(i)).equals(s))
				return (String)languageNeutral.get(i);
		}
		throw new IllegalArgumentException("ERROR: language neutral string not found for: "+s);
	}

	private Vector getChoiceVector(String currentValue, String [] possibleValues)	{
		Vector v = new Vector(possibleValues.length);
		
		if (currentValue.length() > 0)
			v.add(Language.get(currentValue));	// put given value to first position
		
		for (int i = 0; i < possibleValues.length; i++)	{
			String value = possibleValues[i];
			
			if (languageNeutral.indexOf(value) < 0)
				languageNeutral.add(value);
			
			if (currentValue.length() <= 0 || value.equals(currentValue) == false)
				v.add(Language.get(value));
		}
		
		return v;
	}

}
