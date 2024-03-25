package fri.gui.swing.expressions;

import java.io.*;
import java.util.*;
import javax.swing.tree.*;
import fri.util.sort.quick.*;
import fri.util.props.*;
import fri.patterns.interpreter.expressions.*;
import fri.gui.GuiConfig;

/**
	Filter tree persistence by TreeProperties.
	Provided services are loading of named properties,
	storing of named properties, and listing of all loadable names.
*/

public abstract class FilterTreePersistence
{
	public static final String DEFAULT_FILTER_NAME = "Default-Filter";
	private static final String DIRNAME = GuiConfig.dir()+"expressions";
	
	private static final String LOGICAL_CONDITION = "logassoc";	// types of expressions
	private static final String COMPARISON_TAG = "cmp";
	private static final String STRING_COMPARISON = "str"+COMPARISON_TAG;
	private static final String NUMBER_COMPARISON = "num"+COMPARISON_TAG;
	private static final String DATE_COMPARISON = "date"+COMPARISON_TAG;
	private static final String OBJECT_COMPARISON = "obj"+COMPARISON_TAG;

	private static final String VARIABLE_TYPE = "var";	// leftType and rightType
	private static final String CONSTANT_TYPE = "const";

	private static final String ENTITY = "expr";	// property name start token
	private static final String [] ATTRIBUTES = new String []	{	// tree node record attributes
		"type",
		"op",
		"left",
		"right",
		"leftType",
		"rightType",
	};


	/**
		Loads a tree node root from persistence.
		@param stringIdentifier name of root for loading the model
		@return FilterTreeNode root for the Swing TreeModel
	*/
	static FilterTreeNode load(String stringIdentifier)	{
		FileInputStream in = null;
		try	{
			Properties props = new Properties();
			String fileName = makeFilename(stringIdentifier);
			in = new FileInputStream(fileName);
			props.load(in);
			FilterTreeNode root = load(props);
			return root;
		}
		catch (IOException e)	{
			if (stringIdentifier.equals(DEFAULT_FILTER_NAME))	{
				FilterTreeNode root = loadDefault();
				return root;
			}
		}
		finally	{
			try	{ in.close(); }	catch (Exception e)	{}
		}
		return null;
	}
	
	/* Loads the default properties that contain a condition for Java text filter. */
	private static FilterTreeNode loadDefault()	{
		Properties props = ClassProperties.getProperties(FilterTreePersistence.class);
		return load(props);
	}


	/**
		Stores a tree node root to persistence.
		@param stringIdentifier name of root for storing the model
		@return true if successfully stored, else false.
	*/
	static boolean store(String stringIdentifier, FilterTreeNode root)	{
		Properties props = save(root);
		FileOutputStream out = null;
		try	{
			String fileName = makeFilename(stringIdentifier);
			ClassProperties.ensureDirectory(fileName);
			out = new FileOutputStream(fileName);
			props.store(out, System.getProperty("user.name")+" at "+new Date());
			return true;
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		finally	{
			try	{ out.close(); }	catch (Exception e)	{}
		}
		return false;
	}
	
	/**
		Returns a String array with names of available filters.
	*/
	public static String [] list()	{
		File dir = new File(DIRNAME);
		String [] list = dir.list();
		Vector v = new Vector();
		
		for (int i = 0; list != null && i < list.length; i++)	{
			String name = list[i];
			if (name.endsWith(".properties"))
				v.add(name.substring(0, name.length() - ".properties".length()));
		}

		v = new QSort().sort(v);
		
		if (v.indexOf(DEFAULT_FILTER_NAME) < 0)
			v.add(0, DEFAULT_FILTER_NAME);
			
		list = new String [v.size()];
		v.toArray(list);

		return list;
	}
	
	/**
		Deletes the named filter.
	*/
	static boolean delete(String stringIdentifier)	{
		String fileName = makeFilename(stringIdentifier);
		File f = new File(fileName);
		boolean ret = f.delete();
		return ret;
	}
	
	/**
		Renames the named filter.
	*/
	static void rename(String oldIdentifier, String newIdentifier)	{
		if (newIdentifier.equals(oldIdentifier))
			return;
			
		File oldFile = new File(makeFilename(oldIdentifier));
		File newFile = new File(makeFilename(newIdentifier));
		oldFile.renameTo(newFile);
	}
	

	private static String makeFilename(String stringIdentifier)	{
		return DIRNAME+File.separator+stringIdentifier+".properties";
	}
		
	
	/* Returns a tree node root from passed persistable Properties object. */
	private static FilterTreeNode load(Properties treeData)	{
		DefaultMutableTreeNode root = TreeProperties.convert(treeData, ENTITY, ATTRIBUTES);
		Condition cond = toCondition(root);
		return new FilterTreeNode(cond);
	}
	
	/* Returns a persistable Properties object from passed tree node root. */
	private static Properties save(FilterTreeNode root)	{
		AbstractCondition cond = (AbstractCondition)root.getUserObject();
		DefaultMutableTreeNode propRoot = fromCondition(cond);
		Properties props = TreeProperties.convert(propRoot, ENTITY, ATTRIBUTES);
		return props;
	}


	private static Condition toCondition(DefaultMutableTreeNode node)	{
		List l = (List)node.getUserObject();
		//System.err.println("persistent condition list is "+l);

		String type = (String)l.get(0);	// type at 0

		String leftType = (String)l.get(4);	// optional left type at 4
		if (leftType.length() <= 0)
			 leftType = VARIABLE_TYPE;

		String rightType = (String)l.get(5);	// optional right type at 5
		if (rightType.length() <= 0)
			 rightType = CONSTANT_TYPE;
		
		LogicalCondition.LogicalOperator logOp = type.equals(LOGICAL_CONDITION) ? toLogicalOperator((String)l.get(1)) : null;
		AbstractCondition.Operator compOp = type.endsWith(COMPARISON_TAG) ? toCompareOperator((String)l.get(1), type) : null;
		
		Value left = l.size() <= 2 ? null : leftType.equals(VARIABLE_TYPE) ? (Value)new BeanVariable((String)l.get(2)) : (Value)new Constant(l.get(2));
		Value right = l.size() <= 3 ? null : rightType.equals(VARIABLE_TYPE) ? (Value)new BeanVariable((String)l.get(3)) : (Value)new Constant(l.get(3));

		Condition [] childConditions = null;
		
		if (type.equals(LOGICAL_CONDITION))	{
			childConditions = new Condition[node.getChildCount()];
			
			for (int i = 0; i < node.getChildCount(); i++)	{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
				childConditions[i] = toCondition(child);
			}
		}

		AbstractCondition cond =
			type.equals(LOGICAL_CONDITION)
				? (AbstractCondition) new LogicalCondition(logOp, childConditions)
				: type.equals(STRING_COMPARISON)
					? (AbstractCondition) new StringComparison(left, (StringComparison.StringOperator)compOp, right)
					: type.equals(NUMBER_COMPARISON)
						? (AbstractCondition) new NumberComparison(left, (NumberComparison.NumberOperator)compOp, right)
						: type.equals(DATE_COMPARISON)
							? (AbstractCondition) new DateComparison(left, (DateComparison.DateOperator)compOp, right)
							: type.equals(OBJECT_COMPARISON)
								? (AbstractCondition) new ObjectComparison(left, (ObjectComparison.ObjectOperator)compOp, right)
								: null;
		
		if (cond == null)
			throw new IllegalArgumentException("Unknown type of expression: >"+type+"<");

		//System.err.println("Built condition >"+cond+"<, left variable "+(cond instanceof Comparison ? ""+((Comparison)cond).getLeftValue().getClass() : ""));
		return cond;
	}

	private static LogicalCondition.LogicalOperator toLogicalOperator(String op)	{
		for (int i = 0; i < LogicalCondition.operators.length; i++)
			if (op.equals(LogicalCondition.operators[i].toString()))
				return LogicalCondition.operators[i];
		
		throw new IllegalArgumentException("Can not translate logical operator: "+op);
	}
	
	private static AbstractCondition.Operator toCompareOperator(String op, String type)	{
		AbstractCondition.Operator [] operators =
			type.equals(STRING_COMPARISON)
				? operators = StringComparison.operators
				: type.equals(NUMBER_COMPARISON)
					? operators = NumberComparison.operators
					: type.equals(DATE_COMPARISON)
						? operators = DateComparison.operators
						: type.equals(OBJECT_COMPARISON)
							? operators = ObjectComparison.operators
							: null;
		
		for (int i = 0; operators != null && i < operators.length; i++)
			if (op.equals(operators[i].toString()))
				return operators[i];
		
		throw new IllegalArgumentException("Can not translate compare operator: "+op+" with type "+type);
	}



	private static DefaultMutableTreeNode fromCondition(AbstractCondition cond)	{
		Vector row = new Vector(ATTRIBUTES.length);
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(row);

		if (cond instanceof LogicalCondition)	{
			row.add(LOGICAL_CONDITION);	// type
			row.add(cond.getOperator().toString());	// operator

			Condition [] conditions = ((LogicalCondition)cond).getConditions();
			for (int i = 0; i < conditions.length; i++)	{
				DefaultMutableTreeNode child = fromCondition((AbstractCondition)conditions[i]);
				node.add(child);
			}
		}
		else
		if (cond instanceof AbstractComparison)	{
			AbstractComparison comparison = (AbstractComparison)cond;
			
			String type =
				cond instanceof StringComparison
					? STRING_COMPARISON
					: cond instanceof NumberComparison
						? NUMBER_COMPARISON
						: cond instanceof DateComparison
							? DATE_COMPARISON
							: cond instanceof ObjectComparison
								? OBJECT_COMPARISON
								: null;
			
			if (type == null)
				throw new IllegalArgumentException("Unknown type of Comparison: "+cond.getClass());
				
			row.add(type);	// type at 0
			row.add(comparison.getOperator().toString());	// operator at 1
			row.add(comparison.getLeftValue().toString());	// left value at 2
			row.add(comparison.getRightValue().toString());	// right value at 3
			if (comparison.getLeftValue() instanceof Constant)
				row.add(CONSTANT_TYPE);
			if (comparison.getRightValue() instanceof BeanVariable)
				row.add(VARIABLE_TYPE);
		}
		else	{
			throw new IllegalArgumentException("Can not translate "+cond.getClass());
		}
		
		return node;
	}


	private FilterTreePersistence()	{}	// do not instantiate

}