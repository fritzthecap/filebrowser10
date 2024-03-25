package fri.gui.swing.toolbar;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;
import fri.gui.swing.button.PressedEventGeneratingButton;

/**
 * A toolbar wrapper that optionally shows as a popup in MODAL_LAYER on mouse-knocking
 * at the edge it was added.
 * It shows two scroll buttons at start and end to scroll actions when toolbar is bigger
 * than viewport. It shows a radio button to switch its state between popup or always visible.
 * <p />
 * As a variant the visibility (flapping) can be toggled by an Action (ActionTrigger) from
 * some menu item or button. Then it does not show the radio toggle at first position.
 * Use the AppearanceTrigger constructor parameter for this variant.
 * <p />
 * For a coding example see ScrollablePopupToolbar.main().
 * 
 * @author Fritz Ritzberger
 */
public class ScrollablePopupToolbar extends JPanel implements
	HiddenToolbar
{
	private JComponent parentComponent;
	private JToolBar toolbar;
	private JScrollPane scrollpane;
	private AppearanceTrigger appearanceTrigger;
	private int alignment;
	private AbstractButton hiddenState;
	
	// we need a listener when the parent component gets resized while the toolbar is visible
	private ComponentListener componentListener = new ComponentAdapter()	{
		public void componentHidden(ComponentEvent e) {
			disappear();
		}
		public void componentMoved(ComponentEvent e) {
			disappear();
		}
		public void componentResized(ComponentEvent e) {
			disappear();
			appear();
		}
	};
	

	/**
	 * Creates and adds the toolbar for passed parent component at passed edge, with a mouse knock listener.
	 * If the parent is scrollable like JTree or JTable, the parent's scrollpane already must have been added
	 * to its container.
	 * @param parentComponent the JComponent on which the toolbar should appear.
	 * @param initiallyVisible true when this toolbar will be added to its parent by the caller.
	 * @param alignmentSwingConstant the edge where the popup should show,
	 * 		one of SwingConstants.TOP, RIGHT, BOTTOM, LEFT.
	 */
	public ScrollablePopupToolbar(JComponent parentComponent, boolean initiallyVisible, int alignmentSwingConstant)	{
		this(parentComponent, initiallyVisible, null, alignmentSwingConstant);
	}
	
	/**
	 * See above.
	 * @param name the name of the toolbar.
	 */
	public ScrollablePopupToolbar(JComponent parentComponent, boolean initiallyVisible, String name, int alignmentSwingConstant)	{
		this(parentComponent, initiallyVisible, name, alignmentSwingConstant, null);
	}
	
	/**
	 * Creates and adds the toolbar for passed parent component at passed edge.
	 * If the parent is scrollable like JTree or JTable, the parent's scrollpane
	 * already must have been added to its container.
	 * @param parentComponent the JComponent on which the toolbar should appear.
	 * @param initiallyVisible true when this toolbar will be added to its parent by the caller.
	 * @param name the name of the toolbar.
	 * @param alignmentSwingConstant the edge where the popup should show,
	 * 		one of SwingConstants.TOP, RIGHT, BOTTOM, LEFT.
	 * @param appearanceTrigger the AppearanceTrigger implementer that shows the toolbar when
	 * 		not mouse knocking, can be null for mouse knocking.
	 */
	public ScrollablePopupToolbar(JComponent parentComponent, boolean initiallyVisible, String name, int alignmentSwingConstant, AppearanceTrigger appearanceTrigger)	{
		super(new BorderLayout());
		
		if (alignmentSwingConstant != SwingConstants.TOP &&
				alignmentSwingConstant != SwingConstants.BOTTOM &&
				alignmentSwingConstant != SwingConstants.LEFT &&
				alignmentSwingConstant != SwingConstants.RIGHT)
			throw new IllegalArgumentException("Alignment constant must be one of SwingConstants.TOP, RIGHT, BOTTOM, LEFT");

		toolbar = new JToolBar(name);
		toolbar.setFloatable(false);	// the alignment is immutable by caller's parameter
		setToolbarAlignment(alignmentSwingConstant);
		
		scrollpane = new JScrollPane(toolbar, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollpane);
		
		JButton backwardButton = new ArrowButton(getBackwardButtonOrientation(alignmentSwingConstant), scrollpane.getViewport());
		add(backwardButton, getBackwardButtonBorderLayout(alignmentSwingConstant));
		JButton forwardButton = new ArrowButton(getForwardButtonOrientation(alignmentSwingConstant), scrollpane.getViewport());
		add(forwardButton, getForwardButtonBorderLayout(alignmentSwingConstant));
		
		setAppearanceTrigger(appearanceTrigger == null ? new MouseKnockAppearanceTrigger(this) : appearanceTrigger);
		
		if (parentComponent != null)
			setParentComponent(parentComponent);
		
		setVisible(initiallyVisible);
		
		if (getAppearanceTrigger() instanceof MouseKnockAppearanceTrigger)	{
			// add fixed/popup state switch on start of bar
			hiddenState = createHiddenStateToggleButton();
			hiddenState.setSelected(initiallyVisible);
			hiddenState.setHorizontalAlignment(SwingConstants.CENTER);
			hiddenState.setVerticalAlignment(SwingConstants.CENTER);
			hiddenState.setToolTipText(getHiddenStateToolTipText());
			hiddenState.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e) {
					hiddenState.setToolTipText(getHiddenStateToolTipText());
					switchToState(hiddenState.isSelected());
				}
			});
			backwardButton.getParent().remove(backwardButton);
			JPanel p = new JPanel(new BorderLayout());
			p.add(backwardButton, getBackwardButtonBorderLayout(alignmentSwingConstant));
			p.add(hiddenState);
			add(p, getBackwardButtonBorderLayout(alignmentSwingConstant));
		}
		
		if (initiallyVisible && getParentComponent() != null)	{
			JComponent parent = getParentComponent();
			if (parent.getParent() instanceof JViewport)
				parent = (JComponent) parent.getParent().getParent().getParent();	// the scrollpane parent
			parent.add(this, getBorderLayoutToolbarAlignment());
		}
	}
	
	/** Returns the wrapped toolbar. */
	public JToolBar getToolbar()	{
		return toolbar;
	}
	
	/** Sets a new wrapped toolbar. */
	public void setToolbar(JToolBar toolbar)	{
		this.toolbar = toolbar;
		setToolbarAlignment(getToolbarAlignment());
		scrollpane.setViewportView(toolbar);
	}
	
	/** Convenience method to add an action to the wrapped toolbar. */
	public void add(Action action)	{
		getToolbar().add(action);
	}
	
	/** Returns a JRadioButton. Override to create a customized JToggleButton for switching visibility. */
	protected JToggleButton createHiddenStateToggleButton()	{
		return new JRadioButton();
	}

	/** Override this to internationalize the pinned-state togglebutton tooltip. */
	protected String getFixedStateToolTipText()	{
		return "Click for Flapping Toolbar";
	}
	
	/** Override this to internationalize the popup-state togglebutton tooltip. */
	protected String getPopupStateToolTipText()	{
		return "Click to Pin Toolbar";
	}
	
	/** Override this to internationalize the scroll buttons tooltip. */
	protected String getScrollButtonToolTipText()	{
		return "Click to Scroll Actions";
	}
	
	
	/** The appearance trigger provides the hide timeout (when it is MouseKnockTrigger). */
	public AppearanceTrigger getAppearanceTrigger()	{
		return appearanceTrigger;
	}
	
	/** Sets the trigger on which the toolbar should appear, mouse-knocking or some action-button. */
	public void setAppearanceTrigger(AppearanceTrigger appearanceTrigger)	{
		JComponent p = getParentComponent();
		setParentComponent(null);
		this.appearanceTrigger = appearanceTrigger;
		setParentComponent(p);
	}

	/** Returns the parent where this toolbar is showing on. */
	public JComponent getParentComponent()	{
		return parentComponent;
	}
	
	/** Sets the parent where this toolbar is showing on. */
	public void setParentComponent(JComponent parentComponent)	{
		if (getParentComponent() != null && appearanceTrigger != null)
			appearanceTrigger.deinstall(getParentComponent());
		
		if (appearanceTrigger != null)
			appearanceTrigger.setHiddenToolbar(null);
			
		this.parentComponent = parentComponent;
		
		if (appearanceTrigger != null)
			appearanceTrigger.setHiddenToolbar(this);

		if (getParentComponent() != null && appearanceTrigger != null)
			appearanceTrigger.install(getParentComponent());
	}
	
	/** Returns the BorderLayout constant of this popup toolbar alignment, one of BorderLayout.WEST, NORTH, EAST, SOUTH. */
	public String getBorderLayoutToolbarAlignment()	{
		return getToolbarAlignment() == SwingConstants.TOP
			? BorderLayout.NORTH
			: getToolbarAlignment() == SwingConstants.BOTTOM
				? BorderLayout.SOUTH
				: getToolbarAlignment() == SwingConstants.LEFT
					? BorderLayout.WEST
					: BorderLayout.EAST;
	}
	
	/** Returns the alignment of this popup toolbar, one of SwingConstants.LEFT, TOP, RIGHT, BOTTOM. */
	public int getToolbarAlignment()	{
		return alignment;
	}
	

	/** Internally called by AppearanceTrigger. Sets the toolbar visible. */
	public void appear()	{
		if (hiddenState == null)	{	// is not mouse knocking
			switchToState(true);
			return;
		}

		if (hiddenState.isSelected())
			return;
			
		Rectangle r = getParentComponent().getVisibleRect();	// could be in a scrollpane
		JLayeredPane layeredPane = SwingUtilities.getRootPane(getParentComponent()).getLayeredPane();
		Point p = SwingUtilities.convertPoint(getParentComponent(), r.x, r.y, layeredPane);
		int w = toolbar.getOrientation() == JToolBar.HORIZONTAL ? r.width : getPreferredSize().width;
		int h = toolbar.getOrientation() == JToolBar.VERTICAL ? r.height : getPreferredSize().height;
		int x = p.x +
				(toolbar.getOrientation() == JToolBar.HORIZONTAL || alignment == SwingConstants.LEFT
					? 0
					: r.width - w);
		int y = p.y +
				(toolbar.getOrientation() == JToolBar.VERTICAL || alignment == SwingConstants.TOP
					? 0
					: r.height - h);
		setBounds(x, y, w, h);

		layeredPane.add(this, JLayeredPane.MODAL_LAYER);
		setVisible(true);
		installComponentListener(true);
	}
	
	/** Internally called by MouseKnockListener. Sets the toolbar invisible. */
	public void disappear()	{
		if (hiddenState == null)	{	// is not mouse knocking
			switchToState(false);
			return;
		}

		if (hiddenState.isSelected())
			return;
			
		if (checkOpenPopups(this) == false)
			return;
			
		setVisible(false);
		JLayeredPane layeredPane = SwingUtilities.getRootPane(getParentComponent()).getLayeredPane();
		layeredPane.remove(this);
		installComponentListener(false);
	}

	
	private void switchToState(boolean fixed)	{
		JComponent parent = getParentComponent();
		if (parent.getParent() instanceof JViewport)
			parent = (JComponent) parent.getParent().getParent().getParent();	// the scrollpane parent

		setVisible(fixed);
			
		if (fixed == false)
			parent.remove(this);
		else
			parent.add(this, getBorderLayoutToolbarAlignment());

		parent.revalidate();
		parent.repaint();
	}

	private boolean checkOpenPopups(Component c)	{
		if (c instanceof JComboBox)	{
			if (((JComboBox) c).isPopupVisible())
				return false;	// must not set hidden when combo is open, causes GUI deadlock
		}
		else
		if (c instanceof Container)	{
			Component [] children = ((Container) c).getComponents();
			for (int i = 0; i < children.length; i++)
				if (checkOpenPopups(children[i]) == false)
					return false;
		}
		return true;
	}

	private void installComponentListener(boolean add)	{
		Container parent = getParentComponent();
		if (parent.getParent() instanceof JViewport)
			parent = parent.getParent();
			
		if (add)
			parent.addComponentListener(componentListener);
		else
			parent.removeComponentListener(componentListener);
	}


	private String getHiddenStateToolTipText()	{
		return hiddenState == null ? "" : hiddenState.isSelected() ? getFixedStateToolTipText() : getPopupStateToolTipText();
	}
	
	/* Sets the alignment of this popup toolbar, one of SwingConstants.LEFT, TOP, RIGHT, BOTTOM. */
	private void setToolbarAlignment(int alignment)	{
		this.alignment = alignment;
		toolbar.setOrientation(alignment == SwingConstants.TOP || alignment == SwingConstants.BOTTOM
				? JToolBar.HORIZONTAL
				: JToolBar.VERTICAL);
	}
	
	private int getBackwardButtonOrientation(int alignmentSwingConstant)	{
		return alignmentSwingConstant == SwingConstants.TOP || alignmentSwingConstant == SwingConstants.BOTTOM
			? SwingConstants.WEST
			: SwingConstants.NORTH;
	}
	
	private int getForwardButtonOrientation(int alignmentSwingConstant)	{
		return alignmentSwingConstant == SwingConstants.TOP || alignmentSwingConstant == SwingConstants.BOTTOM
			? SwingConstants.EAST
			: SwingConstants.SOUTH;
	}
	
	private String getBackwardButtonBorderLayout(int alignmentSwingConstant)	{
		return alignmentSwingConstant == SwingConstants.TOP || alignmentSwingConstant == SwingConstants.BOTTOM
			? BorderLayout.WEST
			: BorderLayout.NORTH;
	}
	
	private String getForwardButtonBorderLayout(int alignmentSwingConstant)	{
		return alignmentSwingConstant == SwingConstants.TOP || alignmentSwingConstant == SwingConstants.BOTTOM
			? BorderLayout.EAST
			: BorderLayout.SOUTH;
	}
	
	
	
	private class ArrowButton extends BasicArrowButton
	{
		private static final int SCROLL_INCREMENT = 8;	// distance to scroll on one event
		private static final int AUTOSCROLL_INTERVAL = 25;	// millis
		
		/**
		 * Construct an arrow button for passed direction.
		 * @param direction one of SwingConstants.WEST, NORTH, ...
		 * @param viewport the scrollpane viewport to listen to for size change,
		 * 		this sets the button visible/invisible. 
		 */
		ArrowButton(int direction, JViewport viewport) {
			super(direction);
			
			setToolTipText(getScrollButtonToolTipText());
			setModel(new PressedEventGeneratingButton.AutoFiringButtonModel(AUTOSCROLL_INTERVAL));
			
			// set this button invisible when viewport is big enough to nest the whole toolbar
			viewport.addChangeListener(new ChangeListener()	{
				public void stateChanged(ChangeEvent e) {
					boolean isHorizontal = isHorizontal();
					Dimension viewSize = scrollpane.getViewport().getExtentSize();
					Dimension toolbarSize = toolbar.getSize();
					boolean visible =
							(isHorizontal == true && viewSize.width < toolbarSize.width ||
							isHorizontal == false && viewSize.height < toolbarSize.height);
					
					if (isVisible() != visible)	{
						setVisible(visible);
						getParent().doLayout();	// else invisible button is in layout, does repaint
					}
				}
			});
			
			// mouse-over scroll trigger
			addMouseListener(new MouseAdapter()	{
				public void mouseEntered(MouseEvent e) {
					getModel().setPressed(true);
				}
				public void mouseExited(MouseEvent e) {
					getModel().setPressed(false);
				}
			});
			
			// the button press listener, scrolling the viewport
			addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e) {
					boolean isHorizontal = isHorizontal();
					boolean isBackwardAction = (getDirection() == SwingConstants.WEST || getDirection() == SwingConstants.TOP);
					Dimension viewSize = scrollpane.getViewport().getExtentSize();
					Dimension toolbarSize = toolbar.getSize();
					Point viewPosition = scrollpane.getViewport().getViewPosition();
					if (isBackwardAction)	{
						if (isHorizontal)
							viewPosition.x -= Math.min(SCROLL_INCREMENT, viewPosition.x);
						else
							viewPosition.y -= Math.min(SCROLL_INCREMENT, viewPosition.y);
					}
					else	{
						if (isHorizontal)
							viewPosition.x += Math.min(SCROLL_INCREMENT, toolbarSize.width - viewPosition.x - viewSize.width);
						else
							viewPosition.y += Math.min(SCROLL_INCREMENT, toolbarSize.height - viewPosition.y - viewSize.height);
					}
					scrollpane.getViewport().setViewPosition(viewPosition);
				}
			});
		}
		
		private boolean isHorizontal()	{
			return (getToolbarAlignment() == SwingConstants.TOP || getToolbarAlignment() == SwingConstants.BOTTOM);
		}
	}
	


	/** Demo main.
	public static void main(String [] args)	{
		JFrame f = new JFrame("PopupToolbar Test");
		JTextArea textArea = new JTextArea();
		// need some text to test scrolled view
		StringBuffer sb = new StringBuffer(512);
		for (int i = 0; i < 2048; i++)	{
			sb.append((char) (i % 256 + 32));
			if (i % 120 == 0)
				sb.append('\n');
		}
		textArea.setText(sb.toString());
		JScrollPane scrollPane = new JScrollPane(textArea);
		f.getContentPane().add(scrollPane);

		ScrollablePopupToolbar left = new ScrollablePopupToolbar(textArea, false, "LEFT", SwingConstants.LEFT);
		left.getToolbar().add(new JButton("Ok Left"));
		left.getToolbar().add(new JButton("Cancel Left"));

		ScrollablePopupToolbar top = new ScrollablePopupToolbar(textArea, true, "TOP", SwingConstants.TOP);
		top.getToolbar().add(new JButton("Ok Top"));
		top.getToolbar().add(new JButton("Cancel Top"));
		ActionTrigger actionTrigger = new ActionTrigger("Show/Hide Bottom Toolbar");
		top.getToolbar().add(actionTrigger);
		JComboBox combo;
		top.getToolbar().add(combo = new JComboBox());
		combo.setToolTipText("Combo on popup toolbar");
		combo.addItem("One");
		combo.addItem("Two");
		combo.addItem("Three");

		ScrollablePopupToolbar right = new ScrollablePopupToolbar(textArea, false, "RIGHT", SwingConstants.RIGHT);
		right.getAppearanceTrigger().setHideToolbarTimeout(-1);
		right.getToolbar().add(new JButton("Ok Right"));
		right.getToolbar().add(new JButton("Cancel Right"));

		ScrollablePopupToolbar bottom = new ScrollablePopupToolbar(textArea, false, "BOTTOM", SwingConstants.BOTTOM, actionTrigger);
		bottom.getToolbar().add(new JButton("Ok Bottom"));
		bottom.getToolbar().add(new JButton("Cancel Bottom"));

		f.setSize(300, 600);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}
	*/
}
