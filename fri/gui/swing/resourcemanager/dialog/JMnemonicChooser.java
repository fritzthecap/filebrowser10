package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import javax.swing.*;
import fri.gui.swing.resourcemanager.resourceset.resource.JResourceFactory;

public class JMnemonicChooser extends JResourceChooser
{
	private JPanel panel;
	private JComboBox mnemonicCombo;
	
	public JMnemonicChooser(Integer character, Object menuLabel)	{
		mnemonicCombo = new JComboBox();
		mnemonicCombo.addItem("(No Mnemonic)");
		String chars = menuLabel.toString();	// can not be null, this is caught in JCustomizerGUI.build()
		int selected = -1;
		for (int i = 0; i < chars.length(); i++)	{
			char c = chars.charAt(i);
			if (character != null && c == character.intValue())
				selected = i;
			mnemonicCombo.addItem(""+c);
		}

		if (selected >= 0)
			mnemonicCombo.setSelectedIndex(selected);
			
		panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("Choose Mnemonic Character: "));
		panel.add(mnemonicCombo);
	}
	
	public Object getValue()	{
		String s = (String) mnemonicCombo.getSelectedItem();
		if (s.length() != 1)
			return new Integer(0);	// "(No Mnemonic)"
		return new Integer(s.charAt(0));
	}
	
	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns JResourceFactory.MNEMONIC. */
	public String getResourceTypeName()	{
		return JResourceFactory.MNEMONIC;
	}

}
