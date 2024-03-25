package fri.gui.swing.diff;

import java.io.File;
import fri.gui.swing.application.*;

/**
 	Difference window for text files.
 @author Ritzberger Fritz<BR>
*/

public class FileDiffFrame extends GuiApplication
{
	/** Create an emtpy new diff window. */
	public FileDiffFrame() {
		this((File)null, (File)null);
	}

	/** Create new diff window with passed files. */
	public FileDiffFrame(File file1, File file2) {
		super("File Differences");
		
		if (file1 != null && file1.isFile() == false ||
				file2 != null && file2.isFile() == false)
		{
			throw new IllegalArgumentException("Arguments are not normal files!");
		}
		
		FileDiffPanel dp = new FileDiffPanel(file1, file2);
		getContentPane().add(dp);
		
		addWindowListener(dp);

		super.init(dp.getPopups());
	}
		


	// application main

	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].indexOf("-help") >= 0 || args[0].equals("-h"))	{
				System.err.println("SYNTAX: java "+FileDiffFrame.class.getName()+" [file1 file2]");
				System.exit(1);
			}
		}

		if (args.length == 2)
			new FileDiffFrame(new File(args[0]), new File(args[1]));
		else
			new FileDiffFrame();
	}

}