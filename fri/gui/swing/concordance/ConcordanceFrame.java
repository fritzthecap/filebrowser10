package fri.gui.swing.concordance;

import java.io.File;
import java.awt.event.*;
import javax.swing.JFrame;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.IconUtil;
import fri.gui.swing.application.GuiApplication;

public class ConcordanceFrame extends JFrame implements WindowListener
{
	private ConcordanceController controller;
	
	public ConcordanceFrame() {
		this((File[])null);
	}

	public ConcordanceFrame(File [] files) {
		super("Concordances Search");
		
		IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());
		
		ConcordanceMvcBuilder builder = newConcordanceMvcBuilder();
		builder.build(getContentPane(), files);
		controller = builder.getController();
		
		new GeometryManager(this).show();
		
		addWindowListener(this);
	}

	protected ConcordanceMvcBuilder newConcordanceMvcBuilder()	{
		return new ConcordanceMvcBuilder();
	}
	
	public void windowClosing(WindowEvent e)	{
		controller.close();
	}
	public void windowClosed(WindowEvent e)	{}
	public void windowActivated(WindowEvent e)	{}	
	public void windowDeactivated(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)	{}
	public void windowDeiconified(WindowEvent e)	{}
	public void windowOpened(WindowEvent e)	{}


	public static void main(String[] args) {
		if (args.length > 0) {
			if (args[0].indexOf("-help") >= 0 || args[0].equals("-h"))	{
				System.err.println("SYNTAX: java "+ConcordanceFrame.class.getName()+" [file file ...]");
				System.exit(1);
			}
		}

		if (args.length > 0)	{
			File [] farr = new File[args.length];
			for (int i = 0; i < args.length; i++)
				farr[i] = new File(args[i]);
			new ConcordanceFrame(farr);
		}
		else	{
			new ConcordanceFrame();
		}
	}

}
