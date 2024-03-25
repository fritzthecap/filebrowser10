package fri.util.props;

import java.util.*;

/**
 <UL>
 <LI><B>Background:</B><BR>
 	Brauchbare static Methoden im Zusammenhang mit java.util.Properties.
 	<p>
 	Die checkXXXProperties()-Methoden sollen das Pruefen und Vergleichen
 	von Laufzeit-Werten mit uebergebenen und Definition von Default-Returnwerten
 	standardisieren (case-insensitiver Vergleich).
 	<p>
 	Die newline()-Methode sucht nur beim erstenmal in den System-Properties
 	und ist daher als performantes "Makro" verwendbar.
 </LI><P>
 <UL>
 @version $Revision: 1.4 $ $Date: 2001/04/02 15:30:50 $ <BR>
 @author  $Author: fr $ - Ritzberger Fritz<BR>
 </UL>
*/

public abstract class PropertyUtil
{
	private static String newline = null;
	
	
	private PropertyUtil()	{}	// do not construct
	

	/**
		Liefert das Newline (Zeilenumbruch) fuer die aktuelle Plattform (Betriebsystem).
	*/
	public static String newline()	{
		if (newline == null)
			newline = System.getProperty("line.separator");
		return newline;
	}


	/**
		Liefert eine Double-Zahl aus den System-Properties oder -1, wenn nicht definiert.
		Eine eventuell auftretende Exception wird mit printStackTrace() ausgegeben.
	*/
	public static double getSystemDouble(String propName)	{
		return getSystemDouble(propName, -1.0);
	}
	
	/**
		Liefert eine Double-Zahl aus den System-Properties oder den whenNullValue,
		wenn nicht definiert.
		Eine eventuell auftretende Exception wird mit printStackTrace() ausgegeben.
	*/
	public static double getSystemDouble(String propName, double whenNullValue)	{
		String s;
		if ((s = System.getProperty(propName)) != null)	{
			try	{
				return Double.valueOf(s).doubleValue();
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
		return whenNullValue;
	}

	/**
		Liefert eine Integer-Zahl aus den System-Properties oder -1, wenn nicht definiert.
		Eine eventuell auftretende Exception wird mit printStackTrace() ausgegeben.
	*/
	public static int getSystemInteger(String propName)	{
		return (int)getSystemDouble(propName, -1.0);
	}
	
	/**
		Liefert eine Integer-Zahl aus den System-Properties oder -1, wenn nicht definiert.
		Eine eventuell auftretende Exception wird mit printStackTrace() ausgegeben.
	*/
	public static int getSystemInteger(String propName, int whenNullValue)	{
		return (int)getSystemDouble(propName, (double)whenNullValue);
	}
	
	/**
		Diese Methode sucht in den uebergebenen Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist, ansonst false.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static int getClassInteger(String propName, Class cls, int whenNullValue)	{
		String s = getStringProperty(propName, ClassProperties.getProperties(cls));
		if (s != null)	{
			try	{
				return Integer.valueOf(s).intValue();
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
		return whenNullValue;
	}

	/**
		Diese Methode sucht in System-Properties nach propName und liefert true,
		wenn irgendein Wert dafuer gesetzt ist, ansonst false.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkSystemProperty(String propName)	{
		return checkProperty(propName, System.getProperties());
	}
	
	/**
		Diese Methode sucht in den uebergebenen Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist, ansonst false.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkClassProperty(String propName, Class cls)	{
		return checkProperty(propName, ClassProperties.getProperties(cls));
	}

	/**
		Diese Methode sucht in den uebergebenen Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist, ansonst false.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkProperty(String propName, Map props)	{
		return checkProperty(propName, props, false);	// default whenNullValue
	}


	/**
		Diese Methode sucht in den System-Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist, ansonst den uebergebenen whenNullValue.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkSystemProperty(String propName, boolean whenNullValue)	{
		return checkProperty(propName, System.getProperties(), whenNullValue);
	}
	
	/**
		Diese Methode sucht in ClassProperties der uebergebenen Klasse nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist, ansonst den uebergebenen whenNullValue.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkClassProperty(String propName, Class cls, boolean whenNullValue)	{
		return checkProperty(propName, ClassProperties.getProperties(cls), whenNullValue);
	}

	/**
		Diese Methode sucht in den uebergebenen Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist, ansonst den uebergebenen whenNullValue.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkProperty(String propName, Map props, boolean whenNullValue)	{
		return checkProperty(propName, props, null, whenNullValue);	// default no expected value
	}


	/**
		Diese Methode sucht in den System-Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist und er mit dem expectedValue uebereinstimmt, ansonst
		false. Wenn der expectedValue null ist und propName gefunden wird, wird allerdings true geliefert.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkSystemProperty(String propName, String expectedValue)	{
		return checkProperty(propName, System.getProperties(), expectedValue);
	}
	
	/**
		Diese Methode sucht in ClassProperties der uebergebenen Klasse nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist und er mit dem expectedValue uebereinstimmt, ansonst
		false. Wenn der expectedValue null ist und propName gefunden wird, wird allerdings true geliefert.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkClassProperty(String propName, Class cls, String expectedValue)	{
		return checkProperty(propName, ClassProperties.getProperties(cls), expectedValue);
	}

	/**
		Diese Methode sucht in den uebergebenen Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist und er mit dem expectedValue uebereinstimmt, ansonst
		false. Wenn der expectedValue null ist und propName gefunden wird, wird allerdings true geliefert.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkProperty(String propName, Map props, String expectedValue)	{
		return checkProperty(propName, props, expectedValue, false);	// default whenNullValue
	}


	/**
		Diese Methode sucht in den System-Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist und er mit dem expectedValue uebereinstimmt, ansonst
		false, wenn der expectedValue nicht null ist, sonst den uebergebenen whenNullValue.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkSystemProperty(String propName, String expectedValue, boolean whenNullValue)	{
		return checkProperty(propName, System.getProperties(), expectedValue, whenNullValue);
	}
	
	/**
		Diese Methode sucht in den ClassProperties der uebergebenen Klasse nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist und er mit dem expectedValue uebereinstimmt, ansonst
		false, wenn der expectedValue nicht null ist, sonst den uebergebenen whenNullValue.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkClassProperty(String propName, Class cls, String expectedValue, boolean whenNullValue)	{
		return checkProperty(propName, ClassProperties.getProperties(cls), expectedValue, whenNullValue);
	}

	/**
		Diese Methode sucht in den uebergebenen Properties nach propName und liefert true,
		wenn ein Wert dafuer gesetzt ist und er mit dem expectedValue uebereinstimmt, ansonst
		false, wenn der expectedValue nicht null ist, sonst den uebergebenen whenNullValue.
		Beide Werte werden vor dem Vergleich in Kleinbuchstaben verwandelt (case-insensitiv!).
	*/
	public static boolean checkProperty(String propName, Map props, String expectedValue, boolean whenNullValue)	{
		String s = getStringProperty(propName, props);
		if (s != null)	{
			s = s.toLowerCase();
			return expectedValue != null
				?	expectedValue.toLowerCase().equals(s)
				:	s.equals("false") == false &&	// all these strings are evaluated to false
					s.equals("no") == false &&
					s.equals("null") == false &&
					s.equals("0") == false;
		}
		return whenNullValue;
	}
	
	public static String getStringProperty(String propName, Map props)	{
		if (props != null && propName != null && propName.length() > 0)	{
			Object o = props.get(propName);
			return o == null ? (String) o : o.toString();
		}
		return null;
	}





	// test main
	
	public static void main(String [] args)	{
		System.getProperties().setProperty("int", "123");
		System.err.println("property 'int' is "+getSystemInteger("int"));
		System.err.println("property 'boolean' is "+checkSystemProperty("boolean", true));
		System.getProperties().setProperty("boolean", "no");
		System.err.println("property 'boolean' is "+checkSystemProperty("boolean", true));
	}
	
}