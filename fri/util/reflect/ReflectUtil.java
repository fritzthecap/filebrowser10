package fri.util.reflect;

import java.lang.reflect.*;

/**
	Utilities for quick reflection, catching most exceptions and returning null then.
	The public <i>success</i> flag is valid for <i>invoke()</i> and is NOT thread-safe.
*/

public abstract class ReflectUtil
{
	/** Contains true when the last "invoke" call succeeded, esle false. This is NOT thread-safe! */
	public static boolean success;
	
	/** Returns a new instance of passed class, having a constructor without arguments. */
	public static Object newInstance(String className)	{
		return newInstance(className, new Object[0]);
	}
	
	/** Returns a new instance of passed class, having a constructor with classes of passed argument objects. */
	public static Object newInstance(String className, Object [] arguments)	{
		Class [] argumentClasses = paramTypes(arguments);
		return newInstance(className, argumentClasses, arguments);
	}
	
	/** Returns a new instance of passed class, having a constructor with passed argument classes and objects. */
	public static Object newInstance(String className, Class [] argumentClasses, Object [] arguments)	{
		try	{
			Class clazz = Class.forName(className);
			Constructor constructor = clazz.getConstructor(argumentClasses);
			return constructor.newInstance(arguments);
		}
		catch (Exception e)	{
		}
		return null;
	}
	
	/** Assume that the method has no arguments. */
	public static Method getMethod(Object o, String name)	{
		return getMethod(o, name, new Class[0]);
	}
	
	/** Looks for a method. No stack trace will be printed when not found. */
	public static Method getMethod(Object o, String name, Object [] paramValues)	{
		return getMethod(o, name, paramTypes(paramValues));
	}
	
	/** Looks for a method. No stack trace will be printed when not found. */
	public static Method getMethod(Object o, String name, Class [] paramTypes)	{
		Class c = o.getClass();
		Method m = null;
		try	{
			m = c.getMethod(name, paramTypes);
			m.setAccessible(true);
		}
		catch (Exception e)	{
		}
		return m;
	}

	/** Invoke a method, assume that the method has no arguments. */
	public static Object invoke(Object o, String name)	{
		return invoke(o, name, new Object[0]);
	}
	
	/** Invoke a method, assume that the parameter classes can be derived from parameters. */
	public static Object invoke(Object o, String name, Object [] paramValues)	{
		return invoke(o, name, paramTypes(paramValues), paramValues);
	}

	/** Invoke a method. Stack trace will be printed to stderr when error. */
	public static Object invoke(Object o, String name, Class [] paramTypes, Object [] paramValues)	{
		success = false;
		Method m = getMethod(o, name, paramTypes);
		if (m != null)
			try	{
				Object ret = m.invoke(o, paramValues);
				success = true;
				return ret;
			}
			catch (Exception e)	{
				//e.printStackTrace();
			}
		return null;
	}

	private static Class [] paramTypes(Object [] paramValues)	{
		Class [] paramTypes = new Class[paramValues != null ? paramValues.length : 0];
		for (int i = 0; paramValues != null && i < paramValues.length; i++)
			paramTypes[i] = paramValues[i].getClass();
		return paramTypes;
	}


	protected ReflectUtil()	{}

}
