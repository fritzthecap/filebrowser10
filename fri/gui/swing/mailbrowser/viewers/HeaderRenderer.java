package fri.gui.swing.mailbrowser.viewers;

import java.util.Date;
import java.io.UnsupportedEncodingException;
import java.awt.*;
import javax.swing.*;
import javax.mail.*;
import javax.mail.internet.*;
import fri.gui.swing.util.TextFieldUtil;
import fri.gui.swing.mailbrowser.MessageTableModel;
import fri.gui.swing.mailbrowser.Language;

public class HeaderRenderer extends JPanel
{
	public HeaderRenderer(Message msg)
		throws MessagingException
	{
		super(new BorderLayout());

		String subject = msg.getSubject();
		if (subject == null)
			subject = "";

		String from;
		try	{
			from = getAllAddresses(msg.getFrom());
		}
		catch (AddressException e)	{
			from = e.getMessage().replace('\n', ' ').replace('\r', ' ');
			e.printStackTrace();
		}
		
		String to;
		try	{
			to = getAllAddresses(msg.getAllRecipients());
		}
		catch (AddressException e)	{
			to = e.getMessage().replace('\n', ' ').replace('\r', ' ');
			e.printStackTrace();
		}
		
		String date = "";
		Date sentDate = msg.getSentDate();
		if (sentDate != null)
			date = MessageTableModel.dateFormat.format(sentDate);
		
		JPanel pLabels = new JPanel(new GridLayout(4, 1));
		JPanel pTexts = new JPanel(new GridLayout(4, 1));
		final JTextField tf1, tf2, tf3, tf4;

		pLabels.add(new JLabel(" "+Language.get("Subject")+" "));
		pTexts.add(tf1 = createTextField(subject));
		pLabels.add(new JLabel(" "+Language.get("From")+" "));
		pTexts.add(tf2 = createTextField(from));
		pLabels.add(new JLabel(" "+Language.get("To")+" "));
		pTexts.add(tf3 = createTextField(to));
		pLabels.add(new JLabel(" "+Language.get("Sent")+" "));
		pTexts.add(tf4 = createTextField(date));
		
		add(pLabels, BorderLayout.WEST);
		add(pTexts, BorderLayout.CENTER);

		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				TextFieldUtil.scrollToPosition(tf1, 0);
				TextFieldUtil.scrollToPosition(tf2, 0);
				TextFieldUtil.scrollToPosition(tf3, 0);
				TextFieldUtil.scrollToPosition(tf4, 0);
			}
		});
	}
	
	private String getAllAddresses(Address [] addr)	{
		String s = "";
		for (int i = 0; addr != null && i < addr.length; i++)
			s = (s.length() > 0 ? s+", " : "")+addr[i].toString();
		return s;
	}
	
	private JTextField createTextField(String text)	{
		JTextField tf = new JTextField();
		tf.setFont(tf.getFont().deriveFont(Font.BOLD));
		//tf.setBackground(Color.white);
		tf.setEditable(false);
		try	{
			tf.setText(MimeUtility.decodeText(text));
		}
		catch (UnsupportedEncodingException e)	{
			tf.setText(text);
		}
		return tf;
	}

}
