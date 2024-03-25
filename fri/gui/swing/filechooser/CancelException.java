package fri.gui.swing.filechooser;

public class CancelException extends Exception
{
	public CancelException()	{
		super("User canceled");
	}
}
