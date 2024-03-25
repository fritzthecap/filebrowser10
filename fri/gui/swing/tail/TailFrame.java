package fri.gui.swing.tail;

import java.awt.event.*;
import java.io.File;
import fri.gui.swing.application.*;

/**
	File Change Monitor, that renders a text file and
	polls for changes. It implements "tail -f", but it
	redisplays the file even when it is shortened or changed.
	
	@author Ritzberger Fritz
*/

public class TailFrame extends GuiApplication
{
	private TailPanel p;
	
	
	/** Create an empty new tail window. */
	public TailFrame() {
		this(null);
	}

	/** Create new tail window with passed file. */
	public TailFrame(File file) {
		super("File Change Monitor");
		
		p = new TailPanel(file, this);
		getContentPane().add(p);
		
		super.init();
	}
	
	
	public void setTitle(String title)	{
		super.setTitle(title+" - File Change Monitor");
	}
	
	
	public void windowClosing(WindowEvent e)	{
		p.close();
		super.windowClosing(e);
	}


	// application main

	private static void syntax() {
		System.err.println("SYNTAX: java "+TailFrame.class.getName()+" [file1 file2 ...]");
	}
	
	public static void main(String[] args) {
		boolean done = false;
		
		for (int i = 0; i < args.length; i++)	{
			if (args[i].indexOf("-help") >= 0 || args[i].equals("-h"))	{
				syntax();
			}
			else	{
				new TailFrame(new File(args[i]));
				done = true;
			}
		}
		
		if (done == false)
			new TailFrame();
	}

}