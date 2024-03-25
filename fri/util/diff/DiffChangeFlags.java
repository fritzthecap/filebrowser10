package fri.util.diff;

/**
	Change flags for diff wrapper objects.
*/

public abstract class DiffChangeFlags
{
	public static final String CHANGED = "c";
	public static final String INSERTED = "i";
	public static final String DELETED = "d";


	private DiffChangeFlags()	{}	// do not instantiate
}