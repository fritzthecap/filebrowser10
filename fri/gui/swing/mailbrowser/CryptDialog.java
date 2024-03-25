package fri.gui.swing.mailbrowser;

import java.awt.*;
import javax.swing.*;
import fri.util.error.Err;
import fri.util.crypt.Crypt;
import fri.util.dump.NumericDump;
import fri.gui.swing.ComponentUtil;

/**
	Dialog that takes a text and starts a dialog to decrypt or encrypt that text.
*/

public class CryptDialog
{
	private static String key;
	private String result;
	private boolean encrypted;
	
	public CryptDialog(Component parent, String text)	{
		this(parent, text, true);
	}
	
	public CryptDialog(Component parent, String text, boolean defaultEncrypt)	{
		//System.err.println("CryptDialog got parent "+parent);
		
		JPanel p = new JPanel();
		p.add(new JLabel(Language.get("Keyword")+": "));
		JPasswordField keyword = new JPasswordField(20);
		if (key != null)
			keyword.setText(key);
		p.add(keyword);
		
		Object[] options = { Language.get("Encrypt"), Language.get("Decrypt") };
		
		int ret = JOptionPane.showOptionDialog(
				ComponentUtil.getWindowForComponent(parent),
				p,
				Language.get("Text_Encryption"),
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				defaultEncrypt ? options[0] : options[1]);
		System.err.println("option dialog returned int return: "+ret);
		
		if (ret != JOptionPane.CANCEL_OPTION)	{
			key = new String(keyword.getPassword());
			try	{
				if (ret == 0)	{	// encrypt
					this.encrypted = true;

					byte [] bytes = text.getBytes();
					Crypt enc = new Crypt(key, "IDEA");
					bytes = enc.getBytes(bytes, true);
					result = NumericDump.toNumberString(bytes, 31, 23);	// base 31, newline after 23 bytes
				}
				else
				if (ret == 1)	{	// decrypt
					this.encrypted = false;

					Crypt dec = new Crypt(key, "IDEA");
					byte [] bytes = dec.getBytes(NumericDump.fromNumberString(text, 31), false);
					result = new String(bytes);
				}
			}
			catch (Exception e)	{
				Err.error(e);
			}
		}
	}

	public String getResult()	{
		return result;
	}

	public boolean wasEncrypt()	{
		return encrypted;
	}
}