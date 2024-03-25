package fri.patterns.interpreter.expressions;

import java.util.StringTokenizer;
import java.lang.reflect.*;
import fri.util.Equals;

/**
	A Variable links to a value contained in some value holder object by
	<i>java.lang.reflect</i>. This represents a dynamic value, as it is
	retrieved at evaluation time.
	So the field "address" with a value holder of Person.class would call
	<i>valueHolder.getAddress()</i> to get the variable value.
	<p>
	A variable could be defined by another variable, then the reference
	variable gets evaluated first, after that the defined field is retrieved
	from the reference variable result object. Alternatively you could use 
	FIELDNAME_SEPARATOR in field names.
	So the field "address.street" with a value holder of Person.class would call
	<i>person.getAddress().getStreet()</i> to get the variable value.

	@todo Detect cycles on variable referencing!	
	@author Fritz Ritzberger, 2003
*/

public class BeanVariable implements Value
{
	/** The separator to be used for variable names containing reference variables. */
	public static final String FIELDNAME_SEPARATOR = ".";
	private Object valueHolder;	// the runtime object to evaluate data from, buffered for faster execution
	private Object value;
	private String fieldName;
	private BeanVariable referenceVariable;
	private String [] fieldNames;

	
	/**
		Create variable that returns the value holder (passed in evaluate) itself.
	*/
	public BeanVariable()	{
		this(null);
	}
	
	/**
		Create variable that returns the property retrieved by passed fieldName from value holder.
		@param fieldName name of field to retrieve from value holder. Can contain ".", than it is
			interpreted to be a reference.
	*/
	public BeanVariable(String fieldName)	{
		this(null, fieldName);
	}
	
	/**
		Create variable that returns the property retrieved by passed fieldName from value holder.
		@param fieldName name of field to retrieve from value holder. Can contain ".", than it is
			interpreted as a reference.
		@param fieldNames optional array of possible fieldnames for this Variable, held to service a GUI choice.
	*/
	public BeanVariable(String fieldName, String [] fieldNames)	{
		this(fieldName);
		this.fieldNames = fieldNames;
	}
	
	/**
		Create variable that returns the property retrieved by passed fieldName from value returned
		by reference variable.
		@param referenceVariable alternative to "." contained in fieldName.
		@param fieldName name of field, will be converted to a Java-Beans getter-method on evaluate().
	*/
	public BeanVariable(BeanVariable referenceVariable, String fieldName)	{
		setReferenceVariable(referenceVariable);	// keep order!
		setFieldName(fieldName);
	}
	
	
	/** Implements Expression: retrieves the referenced value from passed value holder. */
	public Object evaluate(Object valueHolder)	{
		if (this.valueHolder == valueHolder)	// would apply on null == null
			return value;
			
		Object src = (getReferenceVariable() != null) ? getReferenceVariable().evaluate(valueHolder) : valueHolder;

		if (src != null)	{
			if (fieldName == null)	{	// return the ValueHolder itself
				value = valueHolder;
			}
			else	{	// do reflection
				try	{
					String methodName = "get"+fieldName.substring(0, 1).toUpperCase()+fieldName.substring(1);
					Method m = src.getClass().getMethod(methodName, new Class[0]);
					value = m.invoke(src, new Object[0]);
				}
				catch (Exception e)	{
					//e.printStackTrace();
					throw new IllegalArgumentException("Method invocation failed with >"+fieldName+"< on "+src.getClass()+": "+e.getMessage());
				}
			}
		}
		
		//System.err.println("variable "+this+" got value >"+value+"<");
		this.valueHolder = valueHolder;
		return value;
	}


	/**
		Returns the name of the field this Variable is referencing within the value holder.
		If the fieldName contains dots ("."), then reference variables will be used.
	*/
	public String getFieldName()	{
		return fieldName;
	}

	/**
		If the fieldName contains FIELDNAME_SEPARATORs ("."), then reference variables will be used,
		this overrides <i>setReferenceVariable()</i> call. Else only the name of the
		referenced field within the value holder is stored.
	*/
	public void setFieldName(String fieldName)	{
		if (fieldName != null && fieldName.length() <= 0)
			fieldName = null;
			
		// check occurence if possible field names were defined
		if (fieldName != null && fieldNames != null)	{
			boolean found = false;
			for (int i = 0; found == false && i < fieldNames.length; i++)
				if (fieldNames[i].equals(fieldName))
					found = true;
			
			if (found == false)
				throw new IllegalArgumentException("field name was not found in possible field names!");
		}
		
		if (fieldName != null && fieldName.indexOf(FIELDNAME_SEPARATOR) > 0)	{
			BeanVariable ref = null;

			for (StringTokenizer stok = new StringTokenizer(fieldName, FIELDNAME_SEPARATOR, false); stok.hasMoreTokens(); )	{
				String field = stok.nextToken();

				if (stok.hasMoreTokens())	{	// more field names following
					ref = new BeanVariable(ref, field);
				}
				else	{	// we are done
					setReferenceVariable(ref);
					this.fieldName = field;	// the last field is ours
				}
			}
		}
		else	{
			this.fieldName = fieldName;
		}
		
		// set possible field names if not existing
		if (fieldName != null && fieldNames == null)	{
			fieldNames = new String [] { this.fieldName };
		}
	}


	/** Returns an explicitely set reference Variable. */
	public BeanVariable getReferenceVariable()	{
		return referenceVariable;
	}

	/** Explicitely set a reference Variable. This overrides <i>setFieldName()</i> when fieldName contains dots ("."). */
	public void setReferenceVariable(BeanVariable referenceVariable)	{
		this.referenceVariable = referenceVariable;
	}


	/**
		Retrieves all getters from passed class, including superclasses, including referenced classes.
		This is an optional method to provide a choice of possible references into the value holder.
		After this call the method <i>getFieldNames()</i> will return all possible fields.
	*/
	public void setValueHolderClass(Class valueHolderClass, boolean includeReferencedClasses)	{
		throw new RuntimeException("Not implemented!");
	}

	/**
		Returns the possible field names for this Variable. The names could contain dots (".").
		This method only makes sense after calling <i>setValueHolderClass()</i>.
	*/
	public String [] getFieldNames()	{
		return fieldNames;
	}


	public String toString()	{
		return (referenceVariable != null ? referenceVariable+"." : "")+(fieldName != null ? fieldName : "");
	}

	public Object clone()	{
		return new BeanVariable(getReferenceVariable() != null ? (BeanVariable)getReferenceVariable().clone() : null, fieldName);
	}

	/** Compares the fieldName and the referenceVariable, considering nulls to be equal. */
	public boolean equals(Object other)	{
		if (other instanceof BeanVariable == false)
			return false;
			
		BeanVariable v = (BeanVariable)other;
		return
				Equals.equals(v.fieldName, fieldName) &&
				Equals.equals(v.referenceVariable, referenceVariable);
	}

}
