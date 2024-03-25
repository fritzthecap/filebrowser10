package fri.gui.swing.toolbar;

import javax.swing.JComponent;

/**
 * The listener for toolbar appearance could be mouse knocking
 * or some action button that toggles (shows/hides) the toolbar.
 * 
 * @author Fritz Ritzberger
 */
public interface AppearanceTrigger
{
	/**
	 * Internal call.
	 * The appearance trigger implementation installs itself on
	 * passed component, according to its nature (action button does nothing,
	 * mouse knock trigger installs mouse listener recursively).
	 */
	public void install(JComponent sensor);

	/**
	 * Internal call.
	 * The appearance trigger implementation deinstalls itself from
	 * passed component.
	 */
	public void deinstall(JComponent sensor);
	
	/**
	 * Sets the toolbar for this appearance trigger.
	 */
	public void setHiddenToolbar(HiddenToolbar toolbar);

	
	/**
	 * Sets automatic hide timeout for the toolbar, in millis.
	 * Setting this to less than zero means no timeout, zero means 1500 millis (default),
	 * else after timeout millis the toolbar will disappear when the mouse is not over it.
	 * This applies to MouseKnockTrigger only, not to ActionTrigger.  
	 */
	public void setHideToolbarTimeout(int timeout);

}
