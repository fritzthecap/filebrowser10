package fri.gui.swing.combo.history;

import java.awt.event.*;

public interface TextLineHolder
{
	public String getText();
	public void selectAll();
	public boolean setText(String newText);
	public void setEnabled(boolean enable);
	public void addActionListener(ActionListener l);
	public void removeActionListener(ActionListener l);
}