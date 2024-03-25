package fri.gui.swing.xmleditor.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import org.w3c.dom.Node;
import fri.util.xml.DOMUtil;
import fri.gui.swing.treetable.JTreeTable;
import fri.gui.swing.xmleditor.model.XmlNode;
import fri.gui.swing.xmleditor.model.XmlTreeTableModel;

/**
	Table cell editor for the long text within an element with PCDATA.
	It opens an editor when the "..." button is clicked or the text field
	is doubleclicked.

	@author  Ritzberger Fritz 
*/

public class LongTextTableCellEditor extends DefaultCellEditor implements
	ActionListener
{
	private XmlMultilineTextField textfield;
	private JButton openEditor;
	private JPanel textPanel;
	
	
	/**
		Create a TableCellEditor that can open a text editor dialog.
	*/
	public LongTextTableCellEditor()	{
		super(new XmlMultilineTextField());

		this.textfield = (XmlMultilineTextField)editorComponent;
		textfield.setCellEditorForStop(this);

		setClickCountToStart(1);

		openEditor = new JButton("...");
		openEditor.setMargin(new Insets(0, 0, 0, 0));
		openEditor.addActionListener(this);
		
		textPanel = new JPanel(new BorderLayout());
		textPanel.add(this.textfield, BorderLayout.CENTER);
		textPanel.add(openEditor, BorderLayout.EAST);
	}
	
	
	/**
		Liefert den richtigen Editor, indem der Datentyp des Knotens
		ueber die Tabellenzeile festgestellt wird.
	*/
	public Component getTableCellEditorComponent(
		JTable table,
		Object value,
		boolean selected,
		int row,
		int column)
	{
		super.getTableCellEditorComponent(table, value, selected, row, column);

		// get the selected tree node
		TreePath tp = ((JTreeTable)table).getTree().getPathForRow(row);
		if (tp != null)	{
			XmlNode n = (XmlNode)tp.getLastPathComponent();
			int type = n.getNodeType();
			boolean canBeCDATA =
					type == Node.ELEMENT_NODE &&
					((XmlTreeTable)table).getConfiguration().complexMode == false;

			if (canBeCDATA)	{
				String text = (String)n.getColumnObject(XmlTreeTableModel.LONGTEXT_COLUMN);
				if (text != null)
					text = text.trim();

				// if no text or CDATA not present or CDATA at start and end
				canBeCDATA = text == null || 
						text.indexOf(DOMUtil.CDATA) < 0 ||
						text.startsWith(DOMUtil.CDATA) && text.endsWith(DOMUtil.CDEND);
			}

			textfield.setCanBeCDATA(canBeCDATA);
			textfield.setDialogTitle(n.toString());
		}
		else	{
			textfield.setCanBeCDATA(false);
			textfield.setDialogTitle("");
		}

		// do some GUI handling
		textfield.setBackground(table.getBackground());
		openEditor.setBackground(textfield.getBackground());
		textfield.setForeground(table.getForeground());
		openEditor.setForeground(textfield.getForeground());
		textfield.setFont(table.getFont());
		openEditor.setFont(textfield.getFont());
		
		return textPanel;
	}

	
	/** Implements ActionListener: "..." Button was clicked to open editor. */
	public void actionPerformed(ActionEvent e)	{
		textfield.openEditor();
	}

}
