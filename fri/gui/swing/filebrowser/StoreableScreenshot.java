package fri.gui.swing.filebrowser;

import java.io.File;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.IconUtil;
import fri.gui.swing.screenshot.Screenshot;

/**
	Screenshot object with a FileChooser for storing contained image.
*/

public class StoreableScreenshot extends Screenshot
{
	private NetNode root;


	protected StoreableScreenshot(BufferedImage img, NetNode root) throws
		AWTException
	{
		super(img);	
		this.root = root;
	}
	
	protected Screenshot newInstance(BufferedImage img)
		throws AWTException
	{
		return new StoreableScreenshot(img, root);
	}


	protected JFrame createJFrame(String title)	{
		JFrame f = super.createJFrame(title);
		IconUtil.setFrameIcon(f, GuiApplication.getApplicationIconURL());
		return f;
	}

	
	/** Call super() for full screen shot and store root for FileChooser. */
	public StoreableScreenshot(NetNode root) throws
		AWTException
	{
		super();	// shoot the screen
		
		this.root = root;
		
		JFrame f = createJFrame("Full Screen Shot");

		f.getContentPane().add(new JScrollPane(this));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();   // put on primary screen
		f.setSize(screenSize.width, screenSize.height);
		f.setVisible(true);
	}
	
	/** Override this to create a FileChooser */
	protected File chooseFile(String ext)	{
		File [] files = FileChooser.showFileDialog(
				"Store Image As "+ext.toUpperCase(),
				this,
				root,
				"Screenshot."+ext,	// suggested file name
				new File(System.getProperty("user.home")),
				"*."+ext,	// filter
				true);	// single select
	
		if (files != null && files.length > 0) {
			File targetFile = files[0];
			if (targetFile.exists())	{
				int ret = JOptionPane.showConfirmDialog(
					this,
					"Overwrite "+targetFile.getName()+"?",
					"Existing Image File",
					JOptionPane.YES_NO_OPTION);
	
				if (ret != JOptionPane.YES_OPTION)
				    return null;
			}
			
			System.err.println("saving image to "+targetFile);
			return targetFile;
		}

		return null;
	}
	
}