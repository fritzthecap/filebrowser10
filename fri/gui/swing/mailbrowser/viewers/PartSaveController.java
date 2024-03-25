package fri.gui.swing.mailbrowser.viewers;

import java.io.*;
import java.awt.event.*;
import fri.gui.swing.filechooser.*;

/**
	Controller that contains the "Save As" and "Open" action for attachments, with its callbacks.
*/

public class PartSaveController extends AbstractPartController
{
	public static final String ACTION_SAVE = "Save As";
	
	/** Create a "Save As" cntroller for passed PartView. */
	public PartSaveController(PartView partView)	{
		super(partView);
		registerAction(ACTION_SAVE, (String)null, (String)null, KeyEvent.VK_S, InputEvent.CTRL_MASK);
	}


	/** Callback for "Save As" action. */
	public void cb_Save_As(Object selection)	{
		File suggested = null;
		String name = partView.getDataHandler().getName();
		if (name != null)	{
			String dir = DefaultFileChooser.getChooserDirectory(partView.getSensorComponent().getClass());
			if (dir == null)
				dir = System.getProperty("user.dir");
			suggested = new File(dir, name);
		}
		System.err.println("Default file for saving part is: "+suggested);
		
		FileGUISaveLogicImpl saveImpl = new FileGUISaveLogicImpl(partView.getSensorComponent())	{
			public void write(Object toWrite)
				throws Exception
			{
				InputStream is = null;
				OutputStream os = null;
				try	{
					is = new BufferedInputStream(partView.getDataHandler().getInputStream());
					os = new BufferedOutputStream(new FileOutputStream((File)toWrite));
					byte [] b = new byte[1024];
					int cnt;
					while((cnt = is.read(b)) != -1)	{
						os.write(b, 0, cnt);
					}
				}
				finally	{
					try	{ is.close(); } catch (Exception e)	{}
					try	{ os.close(); } catch (Exception e)	{}
				}
				os.close();
			}
		};
		
		SaveLogic.saveAs(saveImpl, suggested);
	}


	/** Install a popup with "Save As" item on the passed PartView. */
	public static void installSavePopup(PartView partView)	{
		installPopup(new PartSaveController(partView), partView, new String [] { ACTION_SAVE });
	}

}
