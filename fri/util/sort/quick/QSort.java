package fri.util.sort.quick;

import java.util.*;

/**
 * C.A.R Hoare's Quick Sort algorithm, generic in Java
 */
public class QSort
{
	private Comparator comparer;
	private int len;
	private boolean reverse;

	public QSort()	{
	}

	public QSort(boolean reverse)	{
		this(null, reverse);
	}

	public QSort(Comparator comparer)	{
		this(comparer, false);
	}
	
	public QSort(Comparator comparer, boolean reverse)	{
		this.comparer = comparer;
		this.reverse = reverse;
	}
	

	public Vector sort(Vector list)	{
		if (list == null || list.size() <= 1)
			return list;

		Object [] array = new Object[list.size()];
		list.toArray(array);

		sort(array);

		Vector v = new Vector(array.length);
		for (int i = 0; i < array.length; i++)
			v.addElement(array[i]);

		return v;
	}
	
	public List sort(List list)	{
		if (list == null || list.size() <= 1)
			return list;

		Object [] array = new Object[list.size()];
		list.toArray(array);

		sort(array);

		ArrayList v = new ArrayList(array.length);
		for (int i = 0; i < array.length; i++)
			v.add(array[i]);

		return v;
	}
	
	public void sort(Object array[])	{
		for (len = 0; len < array.length && array[len] != null; len++)
			;

		if (len <= 1)
			return;
			
		try {
			sort(array, 0, len - 1);	// starte mit ganze Laenge des arrays
		}
		catch (Exception e)	{
			e.printStackTrace();
			System.err.println("FEHLER: QUICKSORT: "+e.toString());
		}
	}


	private void sort(Object array[], int lo0, int hi0)
		throws Exception
	{
		// zu Beginn ist lo == 0 und hi == array.length - 1
		int lo = lo0;	// kopiere untere Grenze als untere Marke
		int hi = hi0;	// und obere Grenze als obere Marke
		Object mid = array [(lo0 + hi0) / 2];	// bestimme Mitte

		while( lo <= hi )	{	// solange untere Grenze kleiner als obere
			// Suche aufwaerts den ersten der groesser ist als der mittlere, bis zur oberen Marke
			while (lo < hi0 && compare(array[lo], /* < */ mid) < 0)
				++lo;

			// Suche abwaerts den ersten der kleiner ist als der mittlere, bis zur unteren Grenze
			while (hi > lo0 && compare(array[hi], /* > */ mid) > 0)
				--hi;

			if (lo <= hi) {	// wenn zwei gefunden, die vertauscht werden muessen, tue dies
				Object T;
				T = array[lo]; 
				array[lo] = array[hi];
				array[hi] = T;

				lo++;	// verenge die Marken
				hi--;
			}
		}
		
		if (lo0 < hi)	// wenn untere Grenze kleiner als obere Marke
			sort (array, lo0, hi);	// sortiere diesen Bereich
			
		if (lo < hi0)	// wenn obere Grenze groesser als untere Marke
			sort (array, lo, hi0);	// sortiere diesen Bereich
	}


	private int compare(Object a, Object b)	{
		int i = compareImpl(a, b);
		if (reverse)
			return -i;
		return i;
	}
	
	
	protected int compareImpl(Object a, Object b)	{
		if (a == b)
			return 0;

		if (comparer != null)
			return comparer.compare(a, b);
			
		if (a instanceof Number) {
			double diff = ((Number)a).doubleValue() - ((Number)b).doubleValue();
			return diff < 0.0 ? -1 : diff > 0.0 ? 1 : 0;
		}
		else
		if (a instanceof Calendar)	{
			a = ((Calendar)a).getTime();
			b = ((Calendar)b).getTime();
		}

		if (a instanceof Date) {
			long diff = ((Date)a).getTime() - ((Date)b).getTime();
			return diff < 0L ? -1 : diff > 0L ? 1 : 0;
		}
		
		if (a instanceof Comparator) {
			return ((Comparator)a).compare(a, b);
		}
		
		return a.toString().toLowerCase().compareTo(b.toString().toLowerCase());
	}


	/* Test main */
	public static void main(String [] args)	{
		Vector v = new Vector();
		v.add(Long.valueOf(100));
		v.add(Long.valueOf(200));
		v.add(Long.valueOf(300));
		v = new QSort(new Comparator()	{
			public int compare(Object o1, Object o2) {
				return ((Long) o1).intValue() - ((Long) o2).intValue();
			}
		}).sort(v);
		System.err.println(v);
	}

}
