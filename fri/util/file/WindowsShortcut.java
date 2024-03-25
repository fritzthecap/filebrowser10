package fri.util.file;

import java.io.*;

/**
	Provides the real target file of a Windows shortcut file (".LNK").
	<p />
	After many hours of trying to understand and apply Jesse Hager's .LNK specification
	of Windows shortcut files and testing around with LNK files of different Windows versions
	this class was written in half an hour and can retrieve the link target of .LNK files of
	all versions of Windows that will ever exist. Long live simplicity!
	
	@author Fritz Ritzberger, 2004
*/
public class WindowsShortcut
{
	private String target;
	private String networkShare;
	
	public WindowsShortcut(File lnkFile)
		throws IOException, FileNotFoundException
	{
		init(lnkFile);
	}
	
	/** Returns the WINDOWS network share where the file this .LNK file points to resides (e.g. "\\JESSE\WG"). */
	public String getNetworkShare()	{
		return networkShare;
	}

	/** Returns the absolute WINDOWS path this .LNK file points to (e.g. "C:\WINDOWS\system32\config\waitforever.exe"). */
	public String getTarget()	{
		return target;
	}
	
	private void init(File lnkFile)
		throws IOException, FileNotFoundException
	{
		InputStream in = null;
		try	{
			in = new FileInputStream(lnkFile);
			int b = in.read(), prev = 0;
			if (b != 'L')	// magic number
				return;
				
			while ((b = in.read()) != -1 && this.target == null)	{
				if (b == ':' && prev >= 'A' && prev <= 'Z' && (b = in.read()) >= 32)	{
					if ((b == 0 || b == '\\') && (b = in.read()) >= 32)	{
						String s = scanUntilZero((char)prev+":\\"+(char)b, in);
						if (s != null)
							if (this.target == null)
								this.target = s;
					}
				}
				else
				if (this.networkShare == null && b == '\\' && prev == '\\')	{	// seems to be network share path
						String s = scanUntilZero("\\\\", in);
						if (s != null)
							this.networkShare = s;
				}
				prev = b;
			}
		}
		finally	{
			try	{ in.close(); }	catch (Exception e)	{}
		}
	}

	private String scanUntilZero(String startString, InputStream in)
		throws IOException
	{
		int b;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		while ((b = in.read()) >= 32)
			bout.write(b);
		bout.close();
		byte [] bytes = bout.toByteArray();
		if (bytes.length > 0)
			return startString + new String(bytes);
		return null;
	}


	/** Outputs the targets and network shares of all passed .LNK file arguments. */
	public static void main(String [] args)
		throws IOException
	{
		if (args.length <= 0)	{
			System.err.println("SYNTAX: java "+WindowsShortcut.class.getName()+" file ...");
			System.err.println("	Prints the Windows shortcut target file.");
			args = new String [] { "/windows/C/WINDOWS/system32/config/systemprofile/Startmenü/Programme/Zubehör/Windows-Explorer.lnk" };
		}
		for (int i = 0; i < args.length; i++)	{
			WindowsShortcut ws = new WindowsShortcut(new File(args[i]));
			System.err.println("file="+args[i]+"\ntarget="+ws.getTarget()+"\nnetworkshare="+ws.getNetworkShare());
		}
	}

}
