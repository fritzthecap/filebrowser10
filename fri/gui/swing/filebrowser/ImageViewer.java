package fri.gui.swing.filebrowser;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import java.lang.reflect.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.IconUtil;

/**
	Visualize 1-n Image Files with JIMI.
*/

public class ImageViewer extends Frame
{
	/** Show passed Image Files in the frame singleton. */
	public static Frame showImages(File [] files)	{
		ImageViewer frame = new ImageViewer();
		new FileImageViewerDndListener(frame);
		frame.showImageFiles(files);
		return frame;
	}

	
	public ImageViewer()	{
		super("Image Viewer");
		IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());

		addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				dispose();
			}
		});
	}
	
		
	void showImageFiles(File [] files)	{
		Panel panel = new Panel();
		panel.setBackground(Color.lightGray);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		ScrollPane sp = new ScrollPane();
		sp.add(panel);
		add(sp);
		
		for (int i = 0; i < files.length; i++)	{
			if (files[i].exists() == false)	{
				System.err.println("FEHLER: Datei existiert nicht: "+files[i]);
				continue;
			}
			
			String fileName;
			try	{
				fileName = files[i].getCanonicalPath();
			}
			catch (IOException e)	{
				fileName = files[i].getAbsolutePath();
			}
			panel.add(new Label(fileName));
			
			try	{
				//import com.sun.jimi.core.component.JimiCanvas;
				// Component canvas = new JimiCanvas(fileName);
				// panel.add(canvas);
				
				// loosely coupled call, enables jimi.jar to be present or not
				String s = "com.sun.jimi.core.component.JimiCanvas";
				Class cls = Class.forName(s);
				Class [] argClasses = new Class [] { String.class };
				Object [] argObjects = new Object [] { fileName };
				Constructor constr = cls.getConstructor(argClasses);
				Component canvas = (Component)constr.newInstance(argObjects);

				canvas.setBackground(Color.lightGray);
				canvas.setForeground(Color.lightGray);
				panel.add(canvas);
			}
			catch (Throwable e)	{
				JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		new GeometryManager(this).show();
	}


	public static void main(String [] args)	{
		File [] files = new File[args.length];
		for (int i = 0; i < args.length; i++)	{
			files[i] = new File(args[i]);
		}
		ImageViewer.showImages(files);
	}

}
