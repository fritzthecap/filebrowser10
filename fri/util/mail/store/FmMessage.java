package fri.util.mail.store;

import java.io.InputStream;
import javax.mail.*;
import javax.mail.internet.MimeMessage;

public class FmMessage extends MimeMessage
{
	public FmMessage(FmFolder folder, InputStream in, int msgnum)
		throws MessagingException
	{
		super( folder, in, msgnum );
	}

	public Folder getFolder()	{
		Folder f = super.getFolder();
		if (f != null)	{
			try	{
				Folder pnt = f.getParent();
				if (pnt instanceof FmMaildirFolder)
					return pnt;
			}
			catch (MessagingException e)	{
			}
		}
		return f;
	}

}
