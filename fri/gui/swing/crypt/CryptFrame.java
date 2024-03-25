package fri.gui.swing.crypt;

import java.io.File;
import fri.util.file.FileString;
import fri.gui.swing.application.GuiApplication;

public class CryptFrame extends GuiApplication
{
	private CryptPanel cryptPanel;
	
	public CryptFrame()	{
		this((String)null);
	}

	public CryptFrame(File file)	{
		this(FileString.get(file));
	}
	
	public CryptFrame(String text)	{
		getContentPane().add(cryptPanel = new CryptPanel(text));
		setTitle("Cryptography");
		init();
	}

	public boolean close()	{
		cryptPanel.close();
		return super.close();
	}
	

	public static void main(String [] args)	{
		if (args.length > 0)	{
			for (int i = 0 ; i < args.length; i++)	{
				File file = new File(args[i]);
				
				if (file.exists())
					new CryptFrame(file);
				else
					new CryptFrame(args[i]);
			}
		}
		else	{
			new CryptFrame();
		}
	}

}
