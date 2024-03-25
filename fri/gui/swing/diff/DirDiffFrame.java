package fri.gui.swing.diff;

import java.io.File;
import fri.gui.swing.application.*;

/**
 	Difference window for directories.
 @author Ritzberger Fritz<BR>
*/

public class DirDiffFrame extends GuiApplication
{
	/** Create an emtpy new diff window. */
	public DirDiffFrame() {
		this((File)null, (File)null, (String)null);
	}

	/** Create new diff window with passed files. */
	public DirDiffFrame(File file1, File file2) {
		this(file1, file2, (String)null, true);
	}
	
	/** Create new diff window with passed files. */
	public DirDiffFrame(String pattern, boolean include) {
		this((File)null, (File)null, pattern, include);
	}
	
	/** Create new diff window with passed files. */
	public DirDiffFrame(File file1, File file2, String pattern) {
		this(file1, file2, pattern, true);
	}
	
	/** Create new diff window with passed files. */
	public DirDiffFrame(File file1, File file2, String pattern, boolean include) {
		super("Directory Differences");
		
		if (file1 != null && file1.isDirectory() == false ||
				file2 != null && file2.isDirectory() == false)
		{
			throw new IllegalArgumentException("Arguments are not directories!");
		}
		
		DirDiffPanel dp = new DirDiffPanel(file1, file2, pattern, include);
		getContentPane().add(dp);
		
		addWindowListener(dp);

		super.init(dp.getPopups());
	}
		


	// application main

	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].indexOf("-help") >= 0 || args[0].equals("-h"))	{
				System.err.println("SYNTAX: java "+DirDiffFrame.class.getName()+" [directory1 directory2 [filePattern [true|false]]]");
				System.exit(1);
			}
		}

		if (args.length >= 2)	{
			if (args.length <= 2)
				new DirDiffFrame(new File(args[0]), new File(args[1]));
			else
			if (args.length <= 3)
				new DirDiffFrame(new File(args[0]), new File(args[1]), args[2]);
			else
				new DirDiffFrame(new File(args[0]), new File(args[1]), args[2], args[3].toLowerCase().equals("true") ? true : false);
		}
		else	{
			new DirDiffFrame();
		}
	}

}