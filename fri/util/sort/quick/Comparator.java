package fri.util.sort.quick;

/**
	Copied from JDK 1.2. Provided in this package to support
	JDK 1.1.<br>
	<b>CAUTION</b>: If a class implements <code>equals()</code> and
		always returns false to force <code>compare()</code> call,
		it WILL NOT BE FOUND in a Hashtable!
*/

public interface Comparator {
	/**
		Returns negative to be sorted to back,
		zero for equality and positive to be sorted to front.
	*/
	public int compare(Object o1, Object o2);
	/**
		Returns true if compare() would return zero, else false.
	*/
	public boolean equals(Object obj);
}