package fri.gui.swing.scroll;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import fri.gui.swing.splitpane.SymmetryListener;

/**
	Two view components can be added to this panel, that provides
	scrollbars for both and a split divider. Both views always will show
	the same viewport, which means that their scrollers are synchronized:
	when the left bar moves, right moves for the same amount.
	<p>
	This class uses a SymmetryListener, as it must relocate the divider
	when a parent frame is sizing: keep proportion of the divider.<br>
	<p>
	This SplitPane provides two Panels where the scrollable Components
	ly within, where one can add a toolbar or progressbar using <i>BorderLayout</i>.
*/

public class SynchroneScrollingSplitPane extends JSplitPane implements
	ChangeListener	// scroll synchronously
{
	private JPanel p1, p2;
	protected JScrollPane sp1, sp2;
	private boolean neverDone = true;


	/** Create an empty SynchroneScrollingSplitPane. */
	public SynchroneScrollingSplitPane()	{
		build();
		
		new SymmetryListener(this);
	}

	/** Create a SynchroneScrollingSplitPane with two Components like JEditorPane. */
	public SynchroneScrollingSplitPane(Component view1, Component view2)	{
		this();

		setComponent1(view1);
		setComponent2(view2);
	}
	
	
	/** Create two Panels and two ScrollPanes inside. */
	protected void build()	{
		p1 = createPanel(true, sp1 = createScrollPane(true));
		p2 = createPanel(false, sp2 = createScrollPane(false));
	}
	
	private JPanel createPanel(boolean isNumberOne, JScrollPane sp)	{
		JPanel p = new JPanel(new BorderLayout());
		p.add(sp);
		addPanel(p, isNumberOne);
		return p;
	}

	private void addPanel(JPanel p, boolean isNumberOne)	{
		if (getOrientation() == HORIZONTAL_SPLIT)
			if (isNumberOne)
				setLeftComponent(p);
			else
				setRightComponent(p);
		else
			if (isNumberOne)
				setTopComponent(p);
			else
				setBottomComponent(p);
	}

	private JScrollPane createScrollPane(boolean isNumberOne)	{
		JScrollPane sp = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		// synchronize scollpanes
		sp.getViewport().addChangeListener(this);

		// to avoid unpainted areas when scrolling to left with divider != 0.5
		//sp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);//SIMPLE_SCROLL_MODE);

		return sp;
	}

	/** Change the view from horizontal split to vertical or vice versa, depending on current state. */
	public void toggleOrientation()	{
		if (getOrientation() == HORIZONTAL_SPLIT)
			setOrientation(VERTICAL_SPLIT);
		else
			setOrientation(HORIZONTAL_SPLIT);

		setDividerLocation(0.5);
	}
	
	/** Take left panel to right and right panel to left or vice versa, depending on current state. */
	public void toggleViews()	{
		JPanel p = p1;
		p1 = p2;
		p2 = p;
		remove(p1);
		remove(p2);
		addPanel(p1, true);
		addPanel(p2, false);
		setDividerLocation(0.5);
		revalidate();
		repaint();
	}
	
	public void switchSynchroneScrolling(boolean on)	{
		sp1.getViewport().removeChangeListener(this);
		sp2.getViewport().removeChangeListener(this);

		if (on)	{
			sp1.getViewport().addChangeListener(this);
			sp2.getViewport().addChangeListener(this);
		}
	}
	

	/** Sets the first (left or upper) component for this view. */
	public void setComponent1(Component view1)	{
		sp1.setViewportView(view1);
	}

	/** Sets the second (right or lower) component for this view. */
	public void setComponent2(Component view2)	{
		sp2.setViewportView(view2);
	}


	/** Returns the panel where the left/upper textarea lies within. */
	public JPanel getPanel1()	{
		return p1;
	}

	/** Returns the panel where the right/lower textarea lies within. */
	public JPanel getPanel2()	{
		return p2;
	}

	
	/** Implements ChangeListener to synchronize the two scrollpanes. */
	public void stateChanged(ChangeEvent e)	{
		JViewport src = null, tgt = null;
		if (e.getSource() == sp1.getViewport())	{
			src = sp1.getViewport();
			tgt = sp2.getViewport();
		}
		else
		if (e.getSource() == sp2.getViewport())	{
			src = sp2.getViewport();
			tgt = sp1.getViewport();
		}
			
		if (tgt != null && src != null)	{
			// set position of peer view
			tgt.removeChangeListener(this);
			
			tgt.setViewPosition(src.getViewPosition());
			
			tgt.addChangeListener(this);
		}
	}

	/** Workaround for setDividerLocation() bug. */
	public void paintComponent(Graphics g)	{
		if (neverDone)	{
			setDividerLocation(0.5);
			neverDone = false;
		}
		super.paintComponent(g);
	}



	/** Test main  */
	public static void main(String [] args)	{
		String s = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890\nb\nc\nd\ne\ne\nf\ng\nh\ni\nj\nk\nl\nm\nn\no\np\nq\nr\ns\nt\nu\nv\nw\nx\ny\nz";
		JFrame f = new JFrame("SynchroneScrollingSplitPane");
		JTextArea ta1 = new JTextArea(s);
		JTextArea ta2 = new JTextArea(s);
		SynchroneScrollingSplitPane dsp = new SynchroneScrollingSplitPane(ta1, ta2);
		f.getContentPane().add(dsp);
		f.setSize(400, 200);
		f.setVisible(true);
	}
	
}