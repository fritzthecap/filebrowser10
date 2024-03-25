package fri.gui.swing.ftpbrowser;

import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import fri.gui.mvc.view.swing.PopupMouseListener;

/**
	A TextArea that renders every new read from a stream. 
	
	@author Fritz Ritzberger
*/

public class LogTextArea extends JTextArea implements
	ActionListener
{
	private PrintStream stream;
	private JPopupMenu popup;
	
	
	public LogTextArea()	{
		setEditable(false);
		
		popup = new JPopupMenu();
		JMenuItem clear = new JMenuItem("Clear");
		popup.add(clear);
		clear.addActionListener(this);
		addMouseListener(new PopupMouseListener(popup));
	}

	public void actionPerformed(ActionEvent e)	{
		setText("");
	}

	public PrintStream getPrintStream()	{
		if (stream == null)	{
			OutputStream out = new OutputStream()	{
				public void write(byte[] b, int off, int len)	{
					LogTextArea.this.append(new String(b, off, len));
					LogTextArea.this.setCaretPosition(LogTextArea.this.getDocument().getLength());
				}
				public void write(byte[] b)	{
					write(b, 0, b.length);
				}
				public void write(int i)	{
					write(new byte [] { (byte)i });
				}
			};
			stream = new PrintStream(out, true);
		}
		
		return stream;
	}
	
	
	public void close()	{
		try	{
			stream.close();
		}
		catch (Exception e)	{
		}
	}
	
}
