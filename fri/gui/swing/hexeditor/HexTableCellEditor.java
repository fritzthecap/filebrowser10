package fri.gui.swing.hexeditor;

import javax.swing.*;
import fri.gui.swing.document.textfield.mask.ByteMask;
import fri.gui.swing.document.textfield.MaskingDocument;

public class HexTableCellEditor extends DefaultCellEditor
{
	private ByteMask mask;
	private JTextField tf;
	
	public HexTableCellEditor(int base) {
		super(new JTextField());

		tf = (JTextField)editorComponent;
		tf.setHorizontalAlignment(HexTableCellRenderer.horizontalAlignment);
		mask = new ByteMask(base);
		tf.setDocument(new MaskingDocument(tf, mask));
	}

	public void setBase(int base)	{
		mask.setBase(base);
	}

	public JTextField getTextField()	{
		return tf;
	}
}
