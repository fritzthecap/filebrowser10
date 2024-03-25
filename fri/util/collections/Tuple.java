package fri.util.collections;

import fri.util.Equals;

/**
	Diese Klasse repraesentiert ein Paar von Objekten.
	Implementiert ist toString(), equals() und hashCode().
	
	@author Ritzberger Fritz
*/

public class Tuple extends SimpleTuple
{
	/** Merken von einem Objekt, das zweite bleibt undefiniert (ist optional). */
	public Tuple(Object obj1)	{
		super(obj1);
	}

	/** Merken von zwei Objekten, die dann als EIN Parameter verwendet werden koennen. */
	public Tuple(Object obj1, Object obj2)	{
		super(obj1, obj2);
	}
	
	/**
		Liefert true wenn alle enthaltenen Objekte denen des uebergebenen Tuples
		entsprechen. D.h. dass sie entweder beide null sind oder ihre equals()
		Methode true liefert.
	*/
	public boolean equals(Object o)	{
		if (o instanceof Tuple == false)
			return false;
		Tuple t = (Tuple)o;
		return
				Equals.equals(t.obj1, obj1) &&
				Equals.equals(t.obj2, obj2);
	}

	/**
		Fuer den Gebrauch in Hashtable muss hashCode() implementiert sein.
		Diese Methode delegiert an hashCode() aller beteiligten Objekte und
		multipliziert jeweils mit einer Primzahl.
	*/
	public int hashCode()	{
		int h1 = obj1 != null ? obj1.hashCode() : 0;
		int h2 = obj2 != null ? obj2.hashCode() : 0;
		int hash = h1;
		hash = hash * prim + h2;
    return hash;
	}

	/**
		Verkettet die Ergebnisse der toString() Aufrufe aller Objekte mit " | ".
	*/
	public String toString(Object o)	{
		String s1 = obj1 != null ? obj1.toString() : "null";
		String s2 = obj2 != null ? obj2.toString() : "null";
		return s1+" | "+s2;
	}

}
