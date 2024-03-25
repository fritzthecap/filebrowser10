package fri.gui.swing.mailbrowser.viewers;

import java.io.*;
import javax.swing.*;
import javax.activation.*;

public class GifJpegViewer extends JScrollPane implements
	CommandObject,
	PartView
{
	private JLabel imageLabel;
	private DataHandler dh;
	
	/** Implementing CommandObject: show the image. */
	public void setCommandContext(String verb, DataHandler dh)
		throws IOException
	{
		this.dh = dh;
		
		// read the input stream into image
		InputStream is = dh.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		try	{
			byte [] b = new byte[1024];
			int cnt;
			while((cnt = bis.read(b)) != -1)	{
				os.write(b, 0, cnt);
			}
			os.close();
			byte [] bytes = os.toByteArray();
			
			ImageIcon icon = new ImageIcon(bytes);
			imageLabel = new JLabel(icon, SwingConstants.CENTER);
			setViewportView(imageLabel);

			PartSaveController.installSavePopup(this);
		}
		finally	{
			try	{ bis.close(); } catch (Exception e)	{}
		}
	}

	/** Implements PartView. */
	public DataHandler getDataHandler()	{
		return dh;
	}

	/** Implements PartView. */
	public JComponent getSensorComponent()	{
		return imageLabel;
	}

}