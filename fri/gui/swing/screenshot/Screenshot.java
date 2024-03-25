package fri.gui.swing.screenshot;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import javax.swing.*;

import fri.gui.CursorUtil;
import fri.gui.awt.graphicsdevice.GraphicsDeviceInfo;
import fri.gui.awt.image.*;
import fri.util.os.OS;

/**
	Create a screenshot of full or part of screen and let the user
	draw a rectangle around a area of interest. This area
	gets opened in a new Frame. Every Frame can be saved
	separately by Ctl-S or right mouse button and a popup
	choice: Store as GIF or JPEG.
*/

public class Screenshot extends JLabel implements
	ActionListener,
	KeyListener,
	MouseListener,
	MouseMotionListener
{
	private static final String USE_POPUP_TO_STORE_IMAGE = "Use Popup Menu to Store Image";
	
    private static /*final*/ GraphicsDeviceInfo gdi;
	static	{
		try	{
			gdi = new GraphicsDeviceInfo();
		}
		catch (Throwable e)	{
			System.err.println("WARNING: no graphics device information available - old Java version?");
			gdi = null;
		}
	}

	private BufferedImage img;
	private JPopupMenu popup;
	private JMenuItem jpg, gif, ppm;
	private Point currPoint, anchor;


	/**
		Create a Full-Screen-Shot object that can store the image to a File.
	*/
	public Screenshot()
		throws AWTException
	{
		this((Rectangle)null);
	}
	
	/**
		Create a Screenshot object from Rectangle that can store the image to a File.
		@param rect the base Rectangle on screen, whole screen when null.
	*/
	public Screenshot(Rectangle rect)
		throws AWTException
	{
		if (rect == null)	{
			try	{
				Rectangle leftmost = gdi.screens[0].getBounds();
				Rectangle rightmost = gdi.screens[gdi.screens.length - 1].getBounds();
				Point topLeft = new Point(
				        Math.min(leftmost.x, rightmost.x), 
				        Math.min(leftmost.y, rightmost.y));
                Point bottomRight = new Point(
                        Math.max(leftmost.x + leftmost.width, rightmost.x + rightmost.width), 
                        Math.max(leftmost.y + leftmost.height, rightmost.y + rightmost.height));
				rect = new Rectangle(
				        topLeft.x,
				        topLeft.y,
				        bottomRight.x - topLeft.x,
						bottomRight.y - topLeft.y);
			}
			catch (Throwable e)	{
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				rect = new Rectangle(0, 0, screenSize.width, screenSize.height);
			}
		}
		Robot robot = new Robot();
		init(robot.createScreenCapture(rect));
	}

	/**
		Create a Screenshot object with a predefinde image. This gets called
		when user draws a Rectangle around an area of interest.
		@param img the Image to show
	*/
	protected Screenshot(BufferedImage img)
		throws AWTException
	{
		init(img);
	}


	/** Overrideable factory method for construction with image. */
	protected Screenshot newInstance(BufferedImage img)
		throws AWTException
	{
		return new Screenshot(img);
	}


	/** Show the ImageIcon made from Image. Create a popup menu. Add listeners. */
	protected void init(BufferedImage img)	{
		this.img = img;
		
		setHorizontalAlignment(SwingConstants.CENTER);
		setVerticalAlignment(SwingConstants.CENTER);
		
		ImageIcon icon = new ImageIcon(img);
		setIcon(icon);
		createPopup();
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
	}

	/** Create a popup menu for storing the shown image to disk. */
	protected void createPopup()	{	
		popup = new JPopupMenu();
		popup.add(jpg = new JMenuItem("Store As JPG"));
		popup.add(gif = new JMenuItem("Store As GIF"));
		//popup.add(ppm = new JMenuItem("Store As PPM"));	// neither ACDSee nor Jimi can do this
		jpg.addActionListener(this);
		gif.addActionListener(this);
		//ppm.addActionListener(this);
	}



	/** Overridden to draw a red rectangle when dragging mouse. */
	public void paintComponent(Graphics g)	{
		super.paintComponent(g);
		
		Rectangle rect = getMouseRect();
		if (rect != null)	{
			Color c = g.getColor();
			g.setColor(Color.red);
			g.drawRect(rect.x, rect.y, rect.width, rect.height);
			g.setColor(c);
		}
	}
	

	// interface MouseListener

	/** Implemented show store-popup. */
	public void mousePressed(MouseEvent e)	{
		if (showPopup(e) == false)	{
			anchor = e.getPoint();
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		}
	}
	
	/** Implemented show store-popup or open a new image frame. */
	public void mouseReleased(MouseEvent e)	{
		if (showPopup(e) == false)	{
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			Rectangle rect = getMouseRect();
			
			if (rect != null)	{
				BufferedImage subImg = img.getSubimage(rect.x, rect.y, rect.width, rect.height);

				try	{
					JFrame f = createJFrame(USE_POPUP_TO_STORE_IMAGE);
					f.getContentPane().add(newInstance(subImg));
					f.setSize(rect.width + 8, rect.height + 28);
					f.setLocation(getLocationOnScreen().x + rect.x, getLocationOnScreen().y + rect.y);
					f.setVisible(true);
				}
				catch (Exception ex)	{
					ex.printStackTrace();
				}
				
				repaint();
				//System.err.println("mouseReleased: repaint was called");
			}
		}
		anchor = currPoint = null;
	}
	
	public void mouseClicked(MouseEvent e)	{
	}
	public void mouseEntered(MouseEvent e)	{
	}
	public void mouseExited(MouseEvent e)	{
	}
	
	/** Overridden to enable KeyListener. */
	public boolean isFocusTraversable()	{
		return true;
	}
	
	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
			if (anchor != null)	{
				anchor = null;
				repaint();
			}
		}
	}
	public void keyReleased(KeyEvent e)	{
	}
	public void keyTyped(KeyEvent e)	{
	}
	
	private boolean showPopup(MouseEvent e)	{
		if (e.isPopupTrigger())	{
			showPopup(e.getX(), e.getY());
			return true;
		}
		return false;
	}
	
	private void showPopup(int x, int y)	{
		popup.show(this, x, y);
	}


	protected JFrame createJFrame(String title)	{
		return new JFrame(title != null ? title : USE_POPUP_TO_STORE_IMAGE);
	}


	private Rectangle getMouseRect()	{
		if (anchor == null || currPoint == null)
			return null;
		int minX = (anchor.x < currPoint.x) ? anchor.x : currPoint.x;
		int minY = (anchor.y < currPoint.y) ? anchor.y : currPoint.y;
		int maxX = (anchor.x > currPoint.x) ? anchor.x : currPoint.x;
		int maxY = (anchor.y > currPoint.y) ? anchor.y : currPoint.y;
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}


	// interface MouseMotionListener

	/** Implemented to keep track of mouse drag. */
	public void mouseDragged(MouseEvent e)	{
		if (anchor != null)	{
			currPoint = e.getPoint();
			repaint();
		}
	}

	public void mouseMoved(MouseEvent e)	{
	}



	// interface ActionListener

	/** Implemented to store the shown image. */
	public void actionPerformed(ActionEvent e)	{
		String type =
				e.getSource() == gif ? "gif" :
				e.getSource() == jpg ? "jpg" :
				e.getSource() == ppm ? "ppm" : null;

		writeImageFile(type);
	}

	protected void writeImageFile(String type)	{
		CursorUtil.setWaitCursor(this);
		try	 {

			File file;
			if ((file = chooseFile(type)) != null)	{
				writeImageFile(file, type);
				JOptionPane.showMessageDialog(this, "Wrote Image To "+file);
			}
		}
		catch (Exception ex)	{
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}
	
	
	/**
		Return a File where the Image is to be stored.
		Ensure that it ends with passed type which is an extension without dot).
	*/
	protected File chooseFile(String ext)	{
		if (ext == null)
			throw new IllegalArgumentException("Screenshot image file type is null");
		return new File("Screenshot."+ext);
	}
	


	/** Store the image to the passed file by calling the appropriate method for passed type. */
	protected void writeImageFile(File file, String type)
		throws IOException
	{
		if (type.equals("jpg"))
			writeJpegFile(file);
		else
		if (type.equals("gif"))
			writeGifFile(file);
		else
		if (type.equals("ppm"))
			writePpmFile(file);
	}
	

	/** Write a Screenshot as JPG to a File. */
	public File writeJpegFile(File file)
		throws IOException
	{
		return writeJpegFileAboveJava14(file);
	}

	private File writeJpegFileAboveJava14(File file) throws IOException	{
		if (javax.imageio.ImageIO.write(img, "jpg", file) == true)
			return file;
		return null;
	}

	/* Old JDK:
	private File writeJpegFileBelowJava9(File file) throws IOException	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		com.sun.image.codec.jpeg.JPEGImageEncoder encoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(stream);
		encoder.encode(img);
		return writeFile(file, stream.toByteArray());
	} */

	/** Write a Screenshot as GIF to a File. */
	public File writeGifFile(File file)
		throws IOException
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		GifEncoder encoder = new GifEncoder(img, stream);
		encoder.encode();
		return writeFile(file, stream.toByteArray());
	}

	/** Write a Screenshot as PPM to a File. */
	public File writePpmFile(File file)
		throws IOException
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		PpmEncoder encoder = new PpmEncoder(img, stream);
		encoder.encode();
		return writeFile(file, stream.toByteArray());
	}

	private File writeFile(File file, byte [] bytes)
		throws IOException
	{
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(bytes);
		fos.flush();
		fos.close();
		return file;
	}


		
	
	// test main
	
	public static void main(String [] args)	{
		JFrame f = new JFrame();
		try	{
			f.getContentPane().add(new JScrollPane(new Screenshot()));
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			f.setSize(screenSize.width, screenSize.height);
			f.setVisible(true);
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}

}