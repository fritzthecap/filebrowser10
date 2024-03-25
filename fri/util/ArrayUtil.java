package fri.util;

import java.util.*;

/**
	Utility Methods around Arrays and Vectors.
*/

public abstract class ArrayUtil
{
	/** Returns a String containing as much lines as Vector has elements. */
	public static String print(List list)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; list != null && i < list.size(); i++)	{
			Object o = list.get(i);
			String s = o != null ? o.toString() : "null";
			sb.append(i == 0 ? s : "\n"+s);
		}
		return sb.toString();
	}

	/** Returns all array member separated by ", " in a String. */
	public static String print(int [] array)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; array != null && i < array.length; i++)	{
			sb.append(i == 0 ? ""+array[i] : ", "+array[i]);
		}
		return sb.toString();
	}

	/** Returns all array member separated by ", " in a String. */
	public static String print(Object [] array)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; array != null && i < array.length; i++)	{
			String s = array[i] != null ? array[i].toString() : "null";
			sb.append(i == 0 ? s : ", "+s);
		}
		return sb.toString();
	}

	/**
		Convert the List to an array of Objects.
		@param list the List to convert to an array
	*/
	public static Object [] toArray(List list)	{
		return list.toArray();
	}

	/**
		Convert an array of Objects to a Vector.
		@param array the array to convert to a Vector
	*/
	public static Vector toVector(Object [] array)	{
		if (array == null)
			return null;
			
		Vector list = new Vector(array.length);
		for (int i = 0; i  < array.length; i++)
			list.add(array[i]);
			
		return list;
	}

	/**
		Reduce an array to unique elements by comparing via toString() method.
		@param array the array to made unique.
	*/
	public static String [] makeUnique(String [] array)	{
		if (array == null)
			return null;
		int k = 0;
		String [] newarr = new String [array.length];
		for (int j = 0; j < array.length; j++)	{
			boolean found = false;
			for (int i = 0; i < k; i++)
				if (newarr[i].equals(array[j]))
					found = true;
			if (found == false)	{
				newarr[k] = array[j];
				k++;
			}
		}
		String [] uniqarr = new String [k];
		System.arraycopy(newarr, 0, uniqarr, 0, k);
		return uniqarr;
	}

}
