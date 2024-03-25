package fri.gui.swing.application;

import java.awt.*;
import javax.swing.*;
import java.net.URL;
import java.awt.event.*;
import fri.util.application.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.IconUtil;
import fri.gui.swing.resourcemanager.*;

/**
	Instance management and geometry management for JFrames, exit at last close.
	<p>
	The method <i>init()</i> must be used by subclasses to show the frame on screen.
	Without this call the functionality of GuiApplication is not installed!
	<p>
	This class provides an global JFrame that is always the one in forground.
	This can be used for dialogs to define a parent.
*/

public class GuiApplication extends JFrame implements
	Closeable,
	WindowListener
{
	/** The variable globalFrame can be used for dialogs to define a parent. */
	public static JFrame globalFrame = null;
	private static Icon logo;
	
	private int style = GeometryManager.CASCADING;
	private boolean registered;

	{
		JResourceManagingEventQueue.install();	// push customizer event queue if not done
	}

	/** Constructs a new frame that is initially invisible. */
	public GuiApplication()	{
		super();
	}

	/** Creates a Frame in the specified GraphicsConfiguration of a screen device and a blank title. */
	public GuiApplication(GraphicsConfiguration gc)	{
		super(gc);
	}
	
	/** Creates a new, initially invisible Frame with the specified title. */
	public GuiApplication(String title)	{
		super(title);
	}
	
	/** Creates a JFrame with the specified title and the specified GraphicsConfiguration of a screen device. */
	public GuiApplication(String title, GraphicsConfiguration gc)	{
		super(title, gc);
	}


	/**
		Subclasses can set the style (GeometryManager.CASCADING, GeometryManager.TILING)
		before calling init().
	*/
	protected void setStyle(int style)	{
		this.style = style;
	}
	
	/**
		Set the passed icon to upper left corner of Frame.
	*/
	public void setFrameIcon(URL iconURL)	{
		IconUtil.setFrameIcon(this, iconURL);
	}
	

	/**
		Show the Frame on screen and add it to window management.
		This calls <i>init(null, null)</i>.
	*/
	protected void init()	{
		init(null, null);
	}

	/**
		Show the Frame on screen and add it to window management.
		This calls <i>init(customize, null)</i>.
		@param customize a MenuItem to start window customizer. Can be null.
	*/
	protected void init(AbstractButton customize)	{
		init(customize, null);
	}
	
	/**
		Show the Frame on screen and add it to window management.
		This calls <i>init(null, customize)</i>.
		@param components additional Components like popup menues (not in Component tree)
			for customizer. Can be null.
	*/
	protected void init(Object [] components)	{
		init(null, components);
	}
	
	/**
		Show the Frame on screen and add it to window management.
		@param customize a MenuItem or Button to start window customizer. Can be null.
		@param components additional Components like popup menues (not in Component tree)
			for customizer. Can be null.
	*/
	protected void init(AbstractButton customize, Object [] components)	{
		registerWindow();
		
		setFrameIcon(getApplicationIconURL());
		
		if (components != null)
			JResourceManagingEventQueue.addComponents(this, components);

		if (customize != null)
			customize.addActionListener(new JResourceDialogActionListener(this));

		new GeometryManager(this, style).show();

		addWindowListener(this);

		globalFrame = this;
	}


	/**
		Override to do nothing if frame should not be added to window management (closes with parent application).
	*/
	protected void registerWindow()	{
		Application.register(this);
		registered = true;
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}
	
	
	/**
		Implements Closeable.
		Does the dispose as DO_NOTHING_ON_CLOSE was set.
		Default returns true to signalize that this Frame is closeable.
		Override this to save data or do finalizations.
	*/
	public boolean close()	{
		if (registered == false)
			return true;

		try	{
			dispose();
		}
		catch (IllegalStateException e)	{
			System.err.println("WARNING: exception when closing window: "+e.toString());
		}
		return true;
	}


	/**
		Overridden to deiconiy the Frame when it is a buffered Frame
		and was iconfied by the user.
	*/
	public void setVisible(boolean visible)	{
		if (visible && getState() == Frame.ICONIFIED)
			setState(Frame.NORMAL);
		super.setVisible(visible);
	}



	// interface WindowListener
	/** Calls the global window management to signalize close. */
	public void windowClosing(WindowEvent e)	{
		Application.closeExit(this);
	}
	public void windowClosed(WindowEvent e)	{
	}
	/** Evaluates the global current foreground frame variable. */
	public void windowActivated(WindowEvent e)	{
		globalFrame = this;
		System.err.println("GuiApplication.globalFrame is now: "+globalFrame.hashCode()+", "+globalFrame.getTitle());
	}	
	public void windowDeactivated(WindowEvent e)	{
	}
	public void windowIconified(WindowEvent e)	{
	}
	public void windowDeiconified(WindowEvent e)	{
	}
	/** Evaluates the global current foreground frame variable. */
	public void windowOpened(WindowEvent e)	{
		globalFrame = this;
	}
	
	
	public static URL getApplicationIconURL()	{
		return GuiApplication.class.getResource("images/moewe.gif");
	}

	public static Icon getLogoIcon()	{
		if (logo == null)
			logo = new ImageIcon(GuiApplication.class.getResource("images/friware-logo.gif"));
		return logo;
	}

}
