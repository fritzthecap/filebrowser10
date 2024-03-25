package fri.util.ruleengine;

import java.lang.reflect.*;

/**
	Provides field values of a (generic) object. The values must be exposed
	by public getter methods. The passed fieldName is case sensitive and will
	be capitalized if not starting with upper case letter.
	<p>
	This wrapper is needed to evaluate conditions (rule patterns).
*/

class ObjectIntrospector
{
	private Object object;
	
	public ObjectIntrospector(Object object)	{
		this.object = object;
	}

	public Object getFieldValue(String fieldName)	{
		String methodName = "get"+ensureFirstLetterUpperCase(fieldName);
		try	{
			Method m = object.getClass().getMethod(methodName, new Class[0]);
			return m.invoke(object, new Object[0]);
		}
		catch (Exception e)	{
			e.printStackTrace();
			return null;
		}
	}

	public void invokeMethod(String methodName, String argument)	{
		try	{
			Method m = object.getClass().getMethod(methodName, argument == null ? new Class[0] : new Class[] { String.class });
			m.invoke(object, argument == null ? new Object[0] : new Object [] { argument });
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}
	
	private String ensureFirstLetterUpperCase(String name)	{
		char c = name.charAt(0);
		if (Character.isLowerCase(c))
			name = Character.toUpperCase(c)+name.substring(1);
		return name;
	}

}
