package fri.gui.swing.resourcemanager.dialog;

import javax.swing.*;
import java.awt.event.*;
import fri.gui.swing.resourcemanager.LookAndFeel;

/**
	A JComboBox presenting installed look and feels for change by the user.
	All frames will be updated if an item is selectedu
*/

public class LAFComboBox extends JComboBox implements
	ActionListener
{
	public LAFComboBox()	{
		String [] lafNames = LookAndFeel.getInstalledLookAndFeels();
		int current = -1;
		for (int i = 0; i < lafNames.length; i++) {
			addItem(lafNames[i]);
			if (LookAndFeel.getLookAndFeel() != null && lafNames[i].equals(LookAndFeel.getLookAndFeel()))
				current = i;
		}
		
		if (current >= 0)
			setSelectedIndex(current);

		addActionListener(this);
		setToolTipText("Change The Look And Feel Of All Windows");
	}

	/** Implements ActionListener to change Look And Feel. */
	public void actionPerformed(ActionEvent e) {
		LookAndFeel.setLookAndFeel((String) getSelectedItem());
	}

}
