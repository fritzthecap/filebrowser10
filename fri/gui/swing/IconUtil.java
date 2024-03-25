package fri.gui.swing;

import java.net.URL;
import java.awt.Frame;
import java.awt.Window;
import java.awt.Image;
import javax.swing.ImageIcon;

/**
	Utilities for setting Icons to application Frames.
	@auhor Ritzberger Fritz
*/

public abstract class IconUtil
{
	public static Image lastIcon = null;
	
	/**
		Set the passed ImageIcon to the upper left corner of the Frame.
		The ImageIcon can be constructed by
		<code>frame.getToolkit().getImage("images/MyIcon.gif")</code>.
		<p>
		The variable <i>lastIcon</i> contains the created Icon after ths call
		and can be used by other frames when opening.
	*/
	public static void setFrameIcon(Frame frame, URL iconURL)	{
		ImageIcon imgIcon = new ImageIcon(iconURL);
		lastIcon = imgIcon.getImage();
		frame.prepareImage(lastIcon, frame);
		frame.setIconImage(lastIcon);
	}

	/**
		Set the Icon of the parent to the Frame.
	*/
	public static void setFrameIcon(Window parent, Frame frame)	{
		if (parent instanceof Frame)	{
			Image icon = ((Frame)parent).getIconImage();
			frame.setIconImage(icon);
		}
	}
}