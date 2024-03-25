package fri.util.managers;

import java.util.Hashtable;

/**
	Manages Object instances that implement hashCode() and equals() as singletons,
	i.e. there will be no other Object instance that is equal to another obtained
	from this cache.
	An Object will be deleted from cache if all using callers have released it.
	Optionally a key can be used for the instance Object (if it does not implement
	hashCode() and equals()).
	<p />
	CAUTION: for every getInstance() call an instance counter gets incremented,
	so every retrieved singleton must be freed!
	
	@author Fritz Ritzberger
*/

public class InstanceManager
{
	private Hashtable objectCache = new Hashtable();
	private Hashtable inUseCounts = new Hashtable();


	/**
		Caches the passed instance and returns it or returns an already existing instance.
		This constructor makes sense when the passed object overrides <i>hashCode()</i> and <i>equals()</i>,
		so that a new constructed instance can be the key for an already existing object.
	*/
	public Object getInstance(Object instance)	{
		return getInstance(instance, instance);
	}
	
	/**
		Caches the passed instance with passed key and returns it or returns an already existing instance for that key.
		Increments the usage count if the instance already existed.
	*/
	public synchronized Object getInstance(Object key, Object instance)	{
		if (key == null)	// empty model for unconnected view
			throw new IllegalArgumentException("InstanceManager can not manage objects that are null!");
		
		Object o;
		if ((o = objectCache.get(key)) == null)	{
			 objectCache.put(key, o = instance);
			 inUseCounts.put(key, new Integer(1));
			 System.err.println("InstanceManager: New singleton inserted, key is: "+key+", value is: "+o);
		}
		else	{	// increment instance count of client
			Integer i = (Integer)inUseCounts.get(key);
			i = new Integer(i.intValue() + 1);
			inUseCounts.put(key, i);
			System.err.println("InstanceManager: Incremented instance count to "+i+", key is: "+key+", value is: "+o);
		}
		
		return o;
	}

	
	/** Retuns true if an instance for passed key already exists. */
	public synchronized boolean existsInstance(Object key)	{
		return objectCache.get(key) != null;
	}
	
	/** Returns an existing instance and increments the instance count for this, else returns null. */
	public synchronized Object getExistingInstance(Object key)	{
		if (existsInstance(key))
			return getInstance(key, null);
		return null;
	}
	

	/** Returns the count of getInstance() minus freeInstance() calls. */
	public int getInstanceCount(Object key)	{
		if (objectCache.get(key) == null)
			return 0;
		Integer i = (Integer)inUseCounts.get(key);
		return i.intValue();
	}
	
	
	/**
		Releases the passed instance that is interpreted as key, or decrement the usage count.
		@return null if there is another client for the instance,
			the Object itself if this was the last reference to it.
			The Object was removed from cache if the return is not null.
	*/
	public synchronized Object freeInstance(Object keyInstance)	{
		if (keyInstance == null)	// empty model for unconnected view
			throw new IllegalArgumentException("InstanceManager can not free objects that are null!");

		Object o;
		if ((o = objectCache.get(keyInstance)) != null)	{
			Integer i = (Integer)inUseCounts.get(keyInstance);

			if (i.intValue() == 1)	{	// remove completely
				objectCache.remove(keyInstance);
				inUseCounts.remove(keyInstance);
				System.err.println("InstanceManager: Released the singleton of key: "+keyInstance);
			}
			else	{	// decrement instance count
				i = new Integer(i.intValue() - 1);
				inUseCounts.put(keyInstance, i);
				System.err.println("InstanceManager: Decremented instance count to "+i+" for: "+keyInstance);
				
				o = null;	// was not last instance
			}
		}
		
		return o;
	}

}
