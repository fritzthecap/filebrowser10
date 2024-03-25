package fri.util.concordance;

/**
	Implementers can influence the concordance search by returning
	null or not null. When null, the object is ignored on hashing by
	the concordance search. When not null, the returned object is used
	as unique hashing key.
*/
public interface ValidityFilter
{
	/**
		Returns null when the object should be ignored on hashing (e.g. a space-only line),
		else a key object to be used for hashing the object.
		When the object itself is returned, make sure it implements hashCode() and equals()
		to get expected results!
	*/
	public Object isValid(Object object);
}