package fri.gui.swing.tabbedpane;

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.TabbedPaneUI;

/**
	A TabbedPane that shows a close-button whenever the mouse is over the right corner.
	Due to a Swing bug up to 1.4 it is not possible to use this with scroll-layout-policy.
	
	@author Fritz Ritzberger 2003-01-23
*/

public class CloseableTabbedPane extends JTabbedPane implements
	MouseListener,
	MouseMotionListener,
	ActionListener
{
	private static Icon closeIcon = UIManager.getIcon("InternalFrame.closeIcon");
	private static boolean showCloseButtonOnlyOnSelectedTab = false;	// configure this
	private JButton close;
	
	/** TabbedPane with wrap layout policy and tabs at bottom. */
	public CloseableTabbedPane() {
		this(JTabbedPane.TOP);
	}

	/** TabbedPane with wrap layout policy. */
	public CloseableTabbedPane(int tabPlacement) {
		super(tabPlacement);
		
		close = new JButton(closeIcon)	{
			public Insets getInsets()	{
				return new Insets(0, 0, 0, 0);	// top, left, bottom, right
			}
		};
		close.setBorder(null);
		close.setToolTipText(UIManager.getString("InternalFrame.closeButtonToolTip"));
		close.addActionListener(this);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		close.addMouseListener(this);
		close.addMouseMotionListener(this);
	}

	/** Implements mouse listener to show the close button when needed. */
	public void mouseEntered(MouseEvent e)	{
		if (e.getSource() == this)
			if (close.isVisible())
				hideCloseButton();
			else
				showCloseButton(e);
	}
	/** Implements mouse listener to hide the close button when no more needed. */
	public void mouseExited(MouseEvent e)	{
		if (e.getSource() == close)
			hideCloseButton();
	}
	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e)	{}
	public void mouseReleased(MouseEvent e)	{}

	/** Implements mouse listener to show the close button when needed. */
	public void mouseMoved(MouseEvent e)	{
		if (e.getSource() == close)
			e.consume();
		else
			mouseEntered(e);
	}
	/** Implements mouse listener to show the close button when needed. */
	public void mouseDragged(MouseEvent e)	{
		mouseMoved(e);
	}
	
	private void showCloseButton(MouseEvent e)	{
		if (getTabCount() <= 0 || getSelectedIndex() < 0)
			return;
			
		int index = getCloseRenderingTabIndex(e);
		if (index < 0)
			return;
			
		Rectangle r = getUI().getTabBounds(this, index);	// the bounds of the visible part of the tab rectangle
		// calculate offsets of button
		int deltaX = r.width - closeIcon.getIconWidth() - 2;	// align right
		int deltaY = (r.height - closeIcon.getIconHeight()) / 2;	// align center
		r.x += deltaX;	// add offset
		r.width -= deltaX;	// decrement width
		r.y += deltaY;	// add offset
		r.height -= deltaY;	// decrement height
		
		if (r.contains(e.getPoint()))	{
			if (close.isVisible() == false)	{
				JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();
				Point p = SwingUtilities.convertPoint(this, r.x, r.y, layeredPane);
				close.setBounds(p.x, p.y, closeIcon.getIconWidth(), closeIcon.getIconHeight());
				layeredPane.add(close, JLayeredPane.PALETTE_LAYER);
				close.setVisible(true);
			}
		}
		else	{
			hideCloseButton();
		}
	}
	
	private void hideCloseButton()	{
		if (close.isVisible())	{
			close.setVisible(false);
			JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();
			layeredPane.remove(close);
		}
	}

	/** Implements action listener to close a tab on click. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == close)	{
			closeTab();
			hideCloseButton();
		}
	}

	/** Override this to catch the close of a tab. Call getCloseCandidateIndex() to retrieve the index about to be closed. */
	protected void closeTab()	{
		int index = getCloseCandidateIndex();
		if (index >= 0)
			remove(index);
	}

	private int getCloseRenderingTabIndex(MouseEvent e)	{
		if (showCloseButtonOnlyOnSelectedTab)
			return getSelectedIndex();
		return indexAtLocationJDK13(e.getX(), e.getY());
	}

	/** Returns the index of the tab about to be closed. */
	protected int getCloseCandidateIndex()	{
		if (showCloseButtonOnlyOnSelectedTab)
			return getSelectedIndex();
		JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();
		Point p = SwingUtilities.convertPoint(layeredPane, close.getX(), close.getY(), this);
		return indexAtLocationJDK13(p.x, p.y);
	}

	private int indexAtLocationJDK13(int x, int y) {
		return ((TabbedPaneUI)getUI()).tabForCoordinate(this, x, y);
	} 




	/** Test main. */
	public static void main(String [] args)	{
		CloseableTabbedPane tp = new CloseableTabbedPane();
		for (int i = 1; i <= 10; i++)	{
			tp.addTab("Tab "+i, new JButton("Button "+i));
		}
		JFrame f = new JFrame();
		f.getContentPane().add(new JLabel(" offset "), java.awt.BorderLayout.WEST);
		f.getContentPane().add(new JLabel(" offset "), java.awt.BorderLayout.NORTH);
		f.getContentPane().add(tp);
		f.setSize(200, 200);
		f.setVisible(true);
	}

}
