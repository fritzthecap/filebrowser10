package fri.gui.swing.filebrowser;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import fri.gui.CursorUtil;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.IconUtil;
import fri.gui.swing.application.GuiApplication;

/**
	Visualize 1-n image files.
*/
public class ImageViewer extends JFrame
{
    private static ImageViewer singleton;
    
	/** Show passed Image Files in the frame singleton. */
	public static void showImages(File [] files)	{
	    if (singleton == null)
	        singleton = new ImageViewer();
	    singleton.showImageFiles(files);
	}

	
	private final JTabbedPane tabbedPane;
	private List<BufferedImage> images = new ArrayList<>();
	
	private ImageViewer()	{
		super("Image Viewer"); // title bar text
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // never dispose singleton
        addWindowListener(new WindowAdapter() { // instead set invisible
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
        IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());
        new FileImageViewerDndListener(this); // receive drag&drop images
        
        this.tabbedPane = new JTabbedPane();
        getContentPane().add(tabbedPane);
        
        new GeometryManager(this).show();
        
        addComponentListener(new ComponentAdapter() { // resize focused image to fit frame
            // we need a deferred resize, because component-events come very frequently
            private final ActionListener imageResizer = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final int selectedIndex = tabbedPane.getSelectedIndex();
                    if (images.size() > selectedIndex && images.get(selectedIndex) != null) {
                        final BufferedImage image = images.get(selectedIndex);
                        final JComponent selectedTab = (JComponent) tabbedPane.getSelectedComponent();
                        setImageOnTab(selectedIndex, selectedTab, image);
                    }
                }
            };
            private final Timer timer = new Timer(500, imageResizer);
            {
                timer.setRepeats(false); // default is true
            }
            
            @Override
            public void componentResized(ComponentEvent event) {
                final int selectedIndex = tabbedPane.getSelectedIndex();
                if (images.size() > selectedIndex && images.get(selectedIndex) != null)
                    if (timer.isRunning())
                        timer.restart();
                    else
                        timer.start();
            }
        });
	}
		
	private void showImageFiles(File [] files)	{
	    tabbedPane.removeAll(); // drag & drop, remove all former images
	    images.clear();
	    
		for (int i = 0; i < files.length; i++)	{
		    final int index = i;
            tabbedPane.addTab(files[index].getName(), new JPanel());
            
            SwingUtilities.invokeLater(() -> {
                final JComponent tab = (JComponent) tabbedPane.getComponentAt(index);
                try {
                    CursorUtil.setWaitCursor(tab);
                    final BufferedImage image = ImageIO.read(files[index]);
                    if (image == null)
                        throw new IllegalArgumentException("Can not read "+files[index]);
                    
                    images.add(index, image);
                    CursorUtil.resetWaitCursor(tab);
                    
                    if (tab.getWidth() > 0 && tab.getHeight() > 0)
                        setImageOnTab(index, tab, image);
                    // else: resize event will render the image
                }
                catch (Exception e) {
                    CursorUtil.resetWaitCursor(tab);
                    if (images.size() <= index)
                        images.add(index, null); // occupy index
                    
                    final JLabel error = new JLabel("<html>"+e.getMessage()+"</html>");
                    error.setFont(error.getFont().deriveFont(Font.BOLD, 16));
                    error.setForeground(Color.RED);
                    tabbedPane.setComponentAt(index, error);
                }
            });
		}
		
        setVisible(true);
	}

    private void setImageOnTab(int index, JComponent tab, BufferedImage image) {
        CursorUtil.setWaitCursor(tab);
        try {
            final Dimension imageDimension = new Dimension(image.getWidth(), image.getHeight());
            final Dimension canvasDimension = new Dimension(tab.getWidth() - 6, tab.getHeight() - 6);
            final Dimension zoomed = calculateZoomDimension(imageDimension, canvasDimension);
            final ImageIcon icon = new ImageIcon(image.getScaledInstance(zoomed.width, zoomed.height, Image.SCALE_DEFAULT));
            tabbedPane.setComponentAt(index, new JLabel(icon));
        }
        finally {
            CursorUtil.resetWaitCursor(tab);
        }
    }

	private Dimension calculateZoomDimension(Dimension image, Dimension canvas) {
	    if (canvas.width >= image.width && canvas.height >= image.height)
	        return image;
	    
	    final double widthRatio  = (double) canvas.width  / (double) image.width;
	    final double heightRatio = (double) canvas.height / (double) image.height;
	    final double smallerRatio = Math.min(widthRatio, heightRatio);
	    
        int width = (int) Math.round(smallerRatio * (double) image.width);
        int height = (int) Math.round(smallerRatio * (double) image.height);
        
        return new Dimension(width, height);
    }
}