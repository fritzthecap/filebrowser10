package fri.gui.swing.resourcemanager;

import java.util.ArrayList;
import java.awt.Frame;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

/**
	The Look And Feel singleton holding setter and getter for the current value
	and a list of all available Look And Feels for the current operating system.
	Provides a ChangeListener for clients that want to persist the LAF when it changes.
*/

public abstract class LookAndFeel
{
	private static String [] lafNames;
	private static ArrayList lsnrs;
	
	
	/** Clients interested in Look And Feel changes implement this interface. */
	public interface ChangeListener
	{
		/** Called when the current Look And Feel changes. @param newLookAndFeel the name of the new LAF. */
		public void lookAndFeelChanged(String newLookAndFeel);
	}
	
	
	public static String [] getInstalledLookAndFeels()	{
		if (lafNames == null)	{
			UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
			lafNames = new String [lafs.length];
			for (int i = 0; i < lafs.length; i++)
				lafNames[i] = lafs[i].getName();
		}
		return lafNames;
	}

	public static String getLookAndFeel()	{
		return UIManager.getLookAndFeel() != null ? UIManager.getLookAndFeel().getName() : null;
	}
	
	public static void setLookAndFeel(String lookAndFeelName)	{
		if (getLookAndFeel() == null || getLookAndFeel().equals(lookAndFeelName) == false)	{
			UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
			
			for (int i = 0; i < lafs.length; i++)	{
				if (lafs[i].getName().equals(lookAndFeelName))	{
					try	{
						UIManager.setLookAndFeel(lafs[i].getClassName());
						
						Frame[] frames = Frame.getFrames();
						for (int j = 0; j < frames.length; j++)
							SwingUtilities.updateComponentTreeUI(frames[j]);
	
						for (int j = 0; lsnrs != null && j < lsnrs.size(); j++)
							((ChangeListener) lsnrs.get(j)).lookAndFeelChanged(lookAndFeelName);

						return;
					}
					catch (Throwable e)	{
						e.printStackTrace();
					}
				}
			}
		}
	}


	public static void addChangeListener(ChangeListener lsnr)	{
		if (lsnrs == null)
			lsnrs = new ArrayList();
		lsnrs.add(lsnr);
	}
	
	public static void removeChangeListener(ChangeListener lsnr)	{
		if (lsnrs != null)
			lsnrs.remove(lsnr);
	}
	
	
	private LookAndFeel()	{}	// do not instantiate
}
