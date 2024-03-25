package fri.util.collections;

public class SimpleTuple
{
	protected static final int prim = 12345623;
	public final Object obj1, obj2;
	
	/** Merken von einem Objekt, das zweite bleibt undefiniert (ist optional). */
	public SimpleTuple(Object obj1)	{
		this(obj1, null);
	}

	/** Merken von zwei Objekten, die dann als EIN Parameter verwendet werden koennen. */
	public SimpleTuple(Object obj1, Object obj2)	{
		this.obj1 = obj1;
		this.obj2 = obj2;
	}
	
}
