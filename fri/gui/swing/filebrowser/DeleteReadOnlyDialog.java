package fri.gui.swing.filebrowser;

import java.io.File;
import java.awt.*;
import javax.swing.*;
import fri.gui.swing.util.TextFieldUtil;
import fri.gui.swing.text.ClipableJTextField;
import fri.gui.swing.yestoalldialog.YesToAllDialog;

/**
	Ziel: Wiederverwendbarer Dialog, der nach einmaligem Ausloesen des
		Buttons "yes to all" ueber die show() Methode nicht mehr
		am Schirm angezeigt wird, sondern immer nur "YES" liefert.
		Dadurch ist es moeglich, in Delete-Schleifen die Ueberschreiben-
		Abfrage zu uebergehen, ohne ein Extra Flag benutzen zu muessen.
*/

public class DeleteReadOnlyDialog extends YesToAllDialog
{
	private JTextField tf1;
	private JLabel info1;


	/** Ask to delete a readonly object. */
	public DeleteReadOnlyDialog(Component parent)	{
		super(parent);
		
		Container c = delegate.getContentPane();

		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));

		p1.add(Box.createRigidArea(new Dimension(0, 10)));
		
		p1.add(new JLabel("Delete Read-Only File:"));
		
		p1.add(tf1 = new ClipableJTextField("_______________________________________________"));
		tf1.setEditable(false);
		p1.add(info1 = new JLabel(  "________________________________________"));

		p1.add(Box.createRigidArea(new Dimension(0, 10)));

		c.add(p1, BorderLayout.CENTER);

		tf1.addKeyListener(this);
	}


	public void setInfo(String tgt, String tgtInfo)	{
		String s = new File(tgt).getName();
		
		delegate.setTitle("Delete Read-Only File: "+s);

		tf1.setText(tgt);
		TextFieldUtil.scrollToPosition(tf1, tgt.length());
		
		info1.setText(tgtInfo);
	}

}