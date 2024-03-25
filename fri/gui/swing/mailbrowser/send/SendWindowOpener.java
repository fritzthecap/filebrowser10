package fri.gui.swing.mailbrowser.send;

/**
	Implementer can open a new mail window.
*/

public interface SendWindowOpener
{
	/** Open an empty new message window. */
	public SendFrame openSendWindow();
}
