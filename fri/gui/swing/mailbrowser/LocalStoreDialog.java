package fri.gui.swing.mailbrowser;

import java.io.File;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import fri.gui.swing.filechooser.*;

/**
	If no local store is existing, this dialog asks for a path and returns it.
	It must not set it to LocalStore class, as this is done in caller.
*/

public class LocalStoreDialog
{
	private String chosenPath;
	
	public LocalStoreDialog(Component parent, String suggestedPath)	{
		final JTextField storePath = new JTextField(20);
		storePath.setToolTipText(Language.get("Choose_The_Root_Directory_Of_Your_Local_Store"));
		if (suggestedPath != null)
			storePath.setText(suggestedPath);

		JButton choose = new JButton(Language.get("Local_Store_Path"));
		choose.setToolTipText(Language.get("Choose_The_Root_Directory_Of_Your_Local_Store"));
		
		choose.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				DefaultFileChooser.setOpenMultipleFiles(false);
				DefaultFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				String s = storePath.getText();
				File suggested = s.length() > 0 ? new File(s) : null;
				
				try	{
					File [] files = DefaultFileChooser.openDialog(suggested, storePath, getClass());
					
					if (files != null && files.length > 0)
						storePath.setText(files[0].getAbsolutePath());
				}
				catch (CancelException ex)	{
				}
			}
		});
		
		JPanel p = new JPanel();
		p.add(choose);
		p.add(storePath);

		int result = JOptionPane.showConfirmDialog(
				parent,
				p,
				Language.get("Configure_Local_Store"),
				JOptionPane.OK_CANCEL_OPTION);
		
		if (result == JOptionPane.OK_OPTION)	{
			chosenPath = storePath.getText();

			if (new File(chosenPath).isDirectory() == false)	{
				JOptionPane.showMessageDialog(parent, Language.get("Please_choose_an_existing_directory_or_cancel"));
				LocalStoreDialog dlg = new LocalStoreDialog(parent, suggestedPath);
				chosenPath = dlg.getChosenPath();
			}
		}
	}

	public String getChosenPath()	{
		return chosenPath;
	}

}
