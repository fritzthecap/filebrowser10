package fri.gui.awt.dialog;

import java.awt.*;
import java.awt.event.*;
import fri.gui.awt.geometrymanager.GeometryManager;

public class OkCancelDialog extends Dialog implements ActionListener
{
	private int result = 0;
	private Button ok, cancel;

	public OkCancelDialog(Frame f, String msg) {
		super(f, "Message", true);
		init(msg);
	}
	
	public OkCancelDialog(Dialog d, String msg) {
		super(d, "Message", true);
		init(msg);
	}
	
	private void init(String msg)	{
		ok = new Button("Ok");
		cancel = new Button("Cancel");
		Panel p = new Panel (new GridLayout(1, 2));
		p.add(ok);
		p.add(cancel);
		Label l = new Label(msg, Label.CENTER) {
			// This adds some space around the text.
			public Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				return new Dimension(d.width+40, d.height+40);
			}
		};
		add(l, BorderLayout.NORTH);
		add(p, BorderLayout.SOUTH);

		ok.addActionListener(this);
		cancel.addActionListener(this);

		this.addWindowListener (new WindowAdapter () {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				try	{ dispose(); }	catch (Exception ex)	{ System.err.println("Dialog dispose threw "+ex.getMessage()); };
			}
			public void windowActivated(WindowEvent e) {
				ok.requestFocus();	// mind in JDK 1.4! But this is a modal dialog!
			}
		});

		new GeometryManager(this).show();
	}

	public void actionPerformed(ActionEvent e) {
		result = (e.getSource() == ok) ? 1 : 0;
		setVisible(false);
		try	{ dispose(); }	catch (Exception ex)	{ System.err.println("Dialog dispose threw "+ex.getMessage()); };
	}
	
	public boolean wasCanceled()	{
		return result == 0;
	}

}
