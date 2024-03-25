package fri.gui.mvc.view.swing;

import javax.swing.JComponent;
import fri.util.application.Closeable;
import fri.gui.mvc.view.View;

/**
	A SwingView is composed of minimal an addable Component (panel)
	and a sensor Component (where hotkeys and drag&drop handler gets installed on).
	The sensor and the addable panel may be the same Component, but often
	the one is a scrollpane and the other a table or a tree.

	@author  Ritzberger Fritz
*/

public interface SwingView extends View, Closeable, Commitable
{
	/**
		Implement this method and return an JComponent that contains the
		sensor JComponent (that renders the Model) e.g. in a JScrollPane.
		@return the addable JComponent for e.g. <i>getContentPane().add()</i>.
	*/
	public JComponent getAddableComponent();

	/**
		Implement this method and return an JComponent that is the
		sensor for keypresses and popup menus.
		@return the JComponent where popups and keypresses get installed.
	*/
	public JComponent getSensorComponent();

}