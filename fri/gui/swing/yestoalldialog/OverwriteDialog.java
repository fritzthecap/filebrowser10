package fri.gui.swing.yestoalldialog;

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
		Dadurch ist es moeglich, in Kopier-Schleifen die Ueberschreiben-
		Abfrage zu uebergehen, ohne ein Extra Flag benutzen zu muessen.
*/

public class OverwriteDialog extends YesToAllDialog
{
	private JTextField tf1, tf2;
	private JLabel info1, info2;
	private String overwriteLabel;


	/**
		Ask to overwrite an object with another. 
	*/
	public OverwriteDialog(Component parent)	{
		this(parent, null, null);
	}
	
	public OverwriteDialog(Component parent, String overwriteLabel, String withLabel)	{
		super(parent);
		
		this.overwriteLabel= overwriteLabel;
		
		Container c = delegate.getContentPane();

		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
		
		p1.add(Box.createRigidArea(new Dimension(0, 10)));

		p1.add(new JLabel(overwriteLabel == null ? "Overwrite" : overwriteLabel));
		
		p1.add(tf1 = new ClipableJTextField("_______________________________________________"));
		tf1.setEditable(false);
		p1.add(info1 = new JLabel(  "________________________________________"));

		p1.add(Box.createRigidArea(new Dimension(0, 10)));
		p1.add(new JLabel(withLabel == null ? "With" : withLabel));
		
		p1.add(tf2 = new ClipableJTextField("_______________________________________________"));
		tf2.setEditable(false);
		p1.add(info2 = new JLabel(  "________________________________________"));

		p1.add(Box.createRigidArea(new Dimension(0, 10)));

		c.add(p1, BorderLayout.CENTER);

		tf1.addKeyListener(this);
		tf2.addKeyListener(this);
	}


	public void setInfo(String src, String srcInfo, String tgt, String tgtInfo)	{
		delegate.setTitle((overwriteLabel == null ? "Overwrite: " : overwriteLabel+": ")+new File(tgt).getName());

		tf1.setText(tgt);
		TextFieldUtil.scrollToPosition(tf1, tgt.length());
		info1.setText(tgtInfo);
		
		tf2.setText(src);
		TextFieldUtil.scrollToPosition(tf2, src.length());
		info2.setText(srcInfo);
	}

}