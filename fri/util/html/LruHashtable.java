package fri.util.html;

import java.util.*;

public class LruHashtable extends Hashtable {
	private static final int nBuckets = 2;
	private float loadFactor;
	private int threshold;
	private int eachCapacity;
	private Hashtable oldTable;
	private Hashtable newTable;

	public LruHashtable(int initialCapacity, float loadFactor) {
		super(1);

		if (initialCapacity <= 0 || loadFactor <= 0.0) {
			throw new IllegalArgumentException();
		} 

		this.loadFactor = loadFactor;
		threshold = (int) (initialCapacity * loadFactor) - 1;
		eachCapacity = initialCapacity / nBuckets + 1;
		oldTable = new Hashtable(eachCapacity, loadFactor);
		newTable = new Hashtable(eachCapacity, loadFactor);
	}

	public LruHashtable(int initialCapacity) {
		this(initialCapacity, 0.75F);
	}

	public int size() {
		return newTable.size() + oldTable.size();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public synchronized Enumeration keys() {
		return new LruHashtableEnumerator(oldTable, newTable, true);
	}

	public synchronized Enumeration elements() {
		return new LruHashtableEnumerator(oldTable, newTable, false);
	}

	public synchronized boolean contains(Object value) {
		if (newTable.contains(value)) {
			return true;
		} 
		if (oldTable.contains(value)) {
			return true;
		}
		return false;
	}

	public synchronized boolean containsKey(Object key) {
		if (newTable.containsKey(key)) {
			return true;
		} 
		if (oldTable.containsKey(key)) {
			Object value = oldTable.get(key);

			newTable.put(key, value);
			oldTable.remove(key);

			return true;
		}
		return false;
	}

	public synchronized Object get(Object key) {
		Object value;

		value = newTable.get(key);

		if (value != null) {
			return value;
		} 

		value = oldTable.get(key);

		if (value != null) {
			newTable.put(key, value);
			oldTable.remove(key);

			return value;
		}

		return null;
	}

	public synchronized Object put(Object key, Object value) {
		Object oldValue = newTable.put(key, value);

		if (oldValue != null) {
			return oldValue;
		} 

		oldValue = oldTable.get(key);

		if (oldValue != null) {
			oldTable.remove(key);
		} else {
			if (size() >= threshold) {
				oldTable = newTable;
				newTable = new Hashtable(eachCapacity, loadFactor);
			}
		}

		return oldValue;
	}

	public synchronized Object remove(Object key) {
		Object oldValue = newTable.remove(key);

		if (oldValue == null) {
			oldValue = oldTable.remove(key);
		} 

		return oldValue;
	}

	public synchronized void clear() {
		newTable.clear();
		oldTable.clear();
	}

	public synchronized Object clone() {
		LruHashtable n = (LruHashtable) super.clone();

		n.newTable = (Hashtable) n.newTable.clone();
		n.oldTable = (Hashtable) n.oldTable.clone();

		return n;
	}

}

class LruHashtableEnumerator implements Enumeration {
	Enumeration oldEnum;
	Enumeration newEnum;
	boolean old;

	LruHashtableEnumerator(Hashtable oldTable, Hashtable newTable, 
												 boolean keys) {
		if (keys) {
			oldEnum = oldTable.keys();
			newEnum = newTable.keys();
		} else {
			oldEnum = oldTable.elements();
			newEnum = newTable.elements();
		}

		old = true;
	}

	public boolean hasMoreElements() {
		boolean r;

		if (old) {
			r = oldEnum.hasMoreElements();

			if (!r) {
				old = false;
				r = newEnum.hasMoreElements();
			}
		} else {
			r = newEnum.hasMoreElements();
		}

		return r;
	}

	public Object nextElement() {
		if (old) {
			return oldEnum.nextElement();
		} 

		return newEnum.nextElement();
	}

}

