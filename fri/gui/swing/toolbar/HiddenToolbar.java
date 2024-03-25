package fri.gui.swing.toolbar;

import javax.swing.JComponent;

/**
 * Clients of AppearanceTrigger implement this interface.
 */
public interface HiddenToolbar
{
	JComponent getParentComponent();
	int getToolbarAlignment();
	AppearanceTrigger getAppearanceTrigger();
	void setAppearanceTrigger(AppearanceTrigger appearanceTrigger);
	boolean isVisible();
	void appear();
	void disappear();
}