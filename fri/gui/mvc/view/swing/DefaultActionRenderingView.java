package fri.gui.mvc.view.swing;

import java.awt.BorderLayout;
import javax.swing.JToolBar;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import fri.util.os.OS;

/**
	Manages toolbars, button panels (dialogs) and popu menus as action visualization.
	This is a container view that could host several other views.

	@author  Ritzberger Fritz
*/

public class DefaultActionRenderingView extends DefaultSwingView
{
	private JPopupMenu actionPopup;
	private PopupMouseListener actionPopupMouseListener;
	private JPanel actionPanel;
	private JToolBar actionToolbar;

	/** Calls installToolBar(null). */
	public JToolBar installToolBar()	{
		return installToolBar(null);
	}
	
	/**
		Install the passed toolbar in <i>BorderLayout.NORTH</i>.
		If the passed toolbar is null, a horizontal JToolBar is generated.
		Any previously installed toolbar will be removed.
		@param toolbar the toolbar to install or null
		@return the passed or generated (empty) toolbar.
	*/
	public JToolBar installToolBar(JToolBar toolbar)	{
		if (this.actionToolbar != null)
			remove(this.actionToolbar);
	
		if (toolbar == null)	{
			toolbar = new JToolBar(getToolbarOrientation());
			if (OS.isAboveJava13) toolbar.setRollover(true);
		}
		else	{
			toolbar.setOrientation(getToolbarOrientation());
		}

		add(toolbar, getToolbarBorderLayoutAlignment());
			
		return this.actionToolbar = toolbar;
	}

	/** Returns BorderLayout.NORTH. Override for other toolbar alignment. */
	protected String getToolbarBorderLayoutAlignment()	{
		return BorderLayout.NORTH;
	}
	
	private int getToolbarOrientation()	{
		if (getToolbarBorderLayoutAlignment() == BorderLayout.WEST ||
				getToolbarBorderLayoutAlignment() == BorderLayout.EAST ||
				getToolbarBorderLayoutAlignment() == BorderLayout.LINE_START ||
				getToolbarBorderLayoutAlignment() == BorderLayout.LINE_END ||
				getToolbarBorderLayoutAlignment() == BorderLayout.BEFORE_LINE_BEGINS ||
				getToolbarBorderLayoutAlignment() == BorderLayout.AFTER_LINE_ENDS)
			return JToolBar.VERTICAL;
		return JToolBar.HORIZONTAL;
	}
	
	/** Calls installButtonPanel(null). */
	public JPanel installButtonPanel()	{
		return installButtonPanel(null);
	}
	
	/**
		Install the passed button panel (used in dialogs) in <i>BorderLayout.SOUTH</i>.
		If the passed panel is null, a JPanel with <i>FlowLayout</i> is generated.
		Any previously installed panel will be removed.
		@param buttonPanel the panel to install or null
		@return the passed or generated (empty) panel.
	*/
	public JPanel installButtonPanel(JPanel buttonPanel)	{
		if (this.actionPanel != null)
			remove(this.actionPanel);
	
		if (buttonPanel == null)
			buttonPanel = new JPanel();
		
		add(buttonPanel, BorderLayout.SOUTH);
			
		return this.actionPanel = buttonPanel;
	}

	/** Calls installPopup(null). */
	public JPopupMenu installPopup()	{
		return installPopup(null);
	}
	
	/**
		Install the passed popup menu on the sensor view by adding a PopupMouseListener to it.
		If the passed popup is null, a popup is generated.
		Any previously installed popup will be removed.
		@param popup the popup menu to install or null
		@return the passed or generated (empty) popup menu.
	*/
	public JPopupMenu installPopup(JPopupMenu popup)	{
		if (this.actionPopup != null)
			getSensorComponent().removeMouseListener(this.actionPopupMouseListener);
		
		if (popup == null)
			popup = new JPopupMenu();
			
		getSensorComponent().addMouseListener(this.actionPopupMouseListener = new PopupMouseListener(popup));
		
		return this.actionPopup = popup;
	}

}
