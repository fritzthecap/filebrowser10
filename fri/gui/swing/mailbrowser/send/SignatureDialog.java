package fri.gui.swing.mailbrowser.send;

import java.awt.*;
import javax.swing.*;
import fri.util.props.ClassProperties;
import fri.gui.swing.mailbrowser.Language;

/**
	Edit mail signature, to be appended to every new mail.
*/

public class SignatureDialog
{
	private static String signature = ClassProperties.get(SignatureDialog.class, "signature");

	/** Returns the signature singleton. */
	public static String getSignature()	{
		return signature;
	}
		
	/** Opens a dialog that lets edit the mail signature text. */
	public SignatureDialog(Component parent)	{
		JPanel panel = new JPanel(new BorderLayout());
		JLabel l = new JLabel(Language.get("Enter_Your_Mail_Signature"));
		l.setToolTipText(Language.get("This_Text_Will_Be_Appended_To_Every_Message"));
		panel.add(l, BorderLayout.NORTH);
		JTextArea ta = new JTextArea(20, 40);
		panel.add(new JScrollPane(ta), BorderLayout.CENTER);
		if (getSignature() != null)
			ta.setText(getSignature());
		
		// show modal dialog
		int result = JOptionPane.showConfirmDialog(
				parent,
				panel,
				Language.get("Mail_Signature"),
				JOptionPane.OK_CANCEL_OPTION,
				-1);
		
		if (result == JOptionPane.OK_OPTION)	{	// retrieve result and put to properties
			String text = ta.getText();
			
			if (text.trim().length() <= 0)	{
				signature = null;
				ClassProperties.remove(SignatureDialog.class, "signature");
			}
			else	{
				signature = text;
				ClassProperties.put(SignatureDialog.class, "signature", signature);
			}
			ClassProperties.store(SignatureDialog.class);
		}
	}

}