package fri.gui.swing.mailbrowser;

/**
	Message showing frame that exits with application (not registered as application frame).
	It keeps in touch with mail command map.
*/

public class ViewerFrame extends CommandMapAwareFrame
{
	public ViewerFrame(String title)	{
		super(title);
	}

	public void start()	{
		init();
	}

	/** Overridden to not add to window management: exit with others. */
	protected void registerWindow()	{
	}

}
