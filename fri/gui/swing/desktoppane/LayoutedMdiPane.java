package fri.gui.swing.desktoppane;

import javax.swing.*;
import java.util.Vector;
import java.awt.Rectangle;
import java.awt.Dimension;

/**
	Obtains a list of internal frames and do their layout: tiled/cascaded

	@author Ritzberger Fritz
*/

public class LayoutedMdiPane extends JDesktopPane
{
	public static final int CASCADED = 1;
	public static final int TILED_HORIZONTAL = 2;
	public static final int TILED_VERTICAL = 3;
	protected static int layoutmode = CASCADED;
	private static final int DELTAX = 30;
	private static final int DELTAY = 25;
	private JInternalFrame selectedFrame = null;	// backward to JDK1.2
	protected Vector frames = new Vector();


	/** Create a MDI pane that can layout its internal frames. */
	public LayoutedMdiPane()	{
		setDesktopManager(new DefaultDesktopManager()	{
			public void activateFrame(JInternalFrame f)	{
				super.activateFrame(f);
				selectedFrame = f;	// catch frame selection
			}
			public void minimizeFrame(JInternalFrame f)	{
				setNewLayout(layoutmode, false);	// what to do when one is minimized
			}
		});
	}


	/** Call this from subclasses to add a frame to a sorted list and to the parent desktoppane. */
	protected void addFrame(JInternalFrame frame)	{
		//System.err.println("adding new internal frame: "+frame);
		super.add(frame);
		frames.add(frame);
	}

	/** Call this from subclasses to remove a frame just from the sorted list (NOT from parent desktop, as it was closed). */
	protected void removeFrame(JInternalFrame frame)	{
		frames.remove(frame);
	}
	
	
	/** Returns an array of all contained internal frames, in the order they were added. */
	public JInternalFrame [] getAllFramesSorted()	{
		return (JInternalFrame[])frames.toArray(new JInternalFrame[frames.size()]);
	}
	

	protected boolean isOneMaximum()	{
		JInternalFrame [] frames = getAllFrames();

		for (int i = 0; i < frames.length; i++)	{
			JInternalFrame frame = frames[i];

			if (frame.isMaximum())
				return true;
		}

		return false;
	}


	/** Tile or cascade the internal frames */
	public boolean setNewLayout(int layout)	{
		return setNewLayout(layout, true);
	}
	
	/** Tile or cascade the internal frames */
	public boolean setNewLayout(int layout, boolean notIfMaximum)	{
		if (notIfMaximum && isOneMaximum())	{
			layoutmode = layout;
			return true;
		}

		Dimension d = getSize();
		Rectangle r = new Rectangle();
		
		for (int i = 0; i < frames.size(); i++)	{
			JInternalFrame frame = (JInternalFrame)frames.get(i);

			if (layout == TILED_HORIZONTAL)	{
				r.width = d.width;
				r.height = d.height / frames.size();
				r.x = 0;
				r.y = r.height * i;
			}
			else
			if (layout == TILED_VERTICAL)	{
				r.width = d.width / frames.size();
				r.height = d.height;
				r.x = r.width * i;
				r.y = 0;
			}
			else
			if (layout == CASCADED)	{
				r.width = d.width - (frames.size() - 1) * DELTAX;
				r.height = d.height - (frames.size() - 1) * DELTAY;
				r.x = DELTAX * i;
				r.y = DELTAY * i;
			}

			if (frame.isIcon())
				continue;
				
			if (notIfMaximum == false && frame.isMaximum())
				try	{ frame.setMaximum(false); } catch (Exception e)	{}

			// detect errors
			if (r.width <= 1)
				r.width = 640;
			if (r.height <= 1)
				r.height = 510;
			
			frame.setBounds(r);
			//System.err.println("frame "+i+" layout, rectangle "+r+", title "+frame.getTitle());
		}

		layoutmode = layout;
		return false;
	}


	public void switchToFrame(int menuIndex)	{
		if (menuIndex < frames.size())	{
			//System.err.println("Setting selected index "+menuIndex);
			JInternalFrame frame = (JInternalFrame)frames.get(menuIndex);
			switchToFrame(frame);
		}
	}
	
	public void switchToFrame(JInternalFrame frame)	{
		JInternalFrame curr = selectedFrame;
		boolean maximum = (curr == null ) ? false : curr.isMaximum();

		if (frame.isIcon())
			try	{ frame.setIcon(false); }	catch (Exception ex)	{}

		frame.toFront();

		// JDK 1.2
		try	{ frame.setSelected(true); }	catch (Exception ex)	{}

		// JDK 1.3
		//setSelectedFrame(frame);

		if (maximum)
			try	{ frame.setMaximum(true); }	catch (Exception ex)	{}
	}

}
