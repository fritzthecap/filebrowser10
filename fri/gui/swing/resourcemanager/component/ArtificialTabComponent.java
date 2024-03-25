package fri.gui.swing.resourcemanager.component;

import java.awt.Color;
import javax.swing.*;
import fri.gui.awt.resourcemanager.component.ArtificialComponent;

/**
	The proxy object for a tab within a JTabbedPane.
	Gets its tab index at construction and offers indexed resource methods.
*/

class ArtificialTabComponent extends ArtificialComponent
{
	/** Construct an item proxy with the container (Choice/JComboBox/List), the getter method and the index. */
	public ArtificialTabComponent(JTabbedPane tabbedPane, int index)	{
		this.parentComponent = tabbedPane;
		this.index = index;
	}

	/** Simply returns "tab". The label text will be retrieved and appended by ResourceComponentName. */
	public String getName()	{
		return "tab";
	}

	private JTabbedPane getTabbedPane()	{
		return (JTabbedPane) parentComponent;
	}


	public String getText()	{
		return getTabbedPane().getTitleAt(index);
	}

	public void setText(String newText)	{
		getTabbedPane().setTitleAt(index, newText);
	}

	public String getToolTipText()	{
		return getTabbedPane().getToolTipTextAt(index);
	}

	public void setToolTipText(String newText)	{
		getTabbedPane().setToolTipTextAt(index, newText);
	}

	public Icon getIcon()	{
		return getTabbedPane().getIconAt(index);
	}

	public void setIcon(Icon newIcon)	{
		getTabbedPane().setIconAt(index, newIcon);
	}

	public Color getBackground()	{
		return getTabbedPane().getBackgroundAt(index);
	}

	public void setBackground(Color c)	{
		getTabbedPane().setBackgroundAt(index, c);
	}

	public Color getForeground()	{
		return getTabbedPane().getForegroundAt(index);
	}

	public void setForeground(Color c)	{
		getTabbedPane().setForegroundAt(index, c);
	}

	public int getMnemonic()	{
		return getTabbedPane().getMnemonicAt(index);
	}
	
	public void setMnemonic(int mnemonic)	{
		getTabbedPane().setMnemonicAt(index, mnemonic);
	}

}
