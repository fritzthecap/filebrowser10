package fri.gui.swing.mailbrowser.viewers;

import java.io.UnsupportedEncodingException;
import javax.mail.internet.MimeUtility;
import fri.util.activation.GenericCommandLauncher;

/**
	Subclass of GenericCommandLauncher to decode the MIME header text for the filename.
	This is not in the GenericCommandLauncher because it would require javax.mail library.
	This class is referenced in fmail.mailcap file, which is loaded and used by ViewerFactory.
*/

public class CommandLauncher extends GenericCommandLauncher
{
	/** To be overridden by applications that need to decode the name text as it comes from a Message header. */
	protected String decodeName(String name)	{
		try	{
			return MimeUtility.decodeText(name);
		}
		catch (UnsupportedEncodingException e)	{
			return name;
		}
	}
}
