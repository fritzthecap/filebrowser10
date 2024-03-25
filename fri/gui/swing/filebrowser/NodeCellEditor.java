package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import javax.swing.Timer;
import javax.swing.text.*;
import javax.swing.plaf.FontUIResource;
import fri.gui.swing.text.ClipableJTextField;
import fri.util.file.ValidFilename;

/**
	Tree-Cell-Editor ueber die ganze Breite der Tree-View (mehr Platz zum Schreiben).
	Filtern der eingegebenen Zeichen, plattformspezifisch: z.B. "\/:*?"<>|" sind nicht erlaubt unter Windows.
*/

public class NodeCellEditor extends DefaultTreeCellEditor
{
	private NetNode nodeToEdit;
	private JTextField text;
	private DefaultMutableTreeNode currDMTN;
	private JTree tree;
	private DefaultTreeCellRenderer myRenderer;


	public NodeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
		this(tree, renderer, null);
	}

	public NodeCellEditor(JTree tree, DefaultTreeCellRenderer renderer, TreeCellEditor editor)	{
		super(tree, renderer, editor);
		
		this.tree = tree;
		this.myRenderer = renderer;
		
		editingContainer = new WideEditorContainer();
		text = (JTextField)((DefaultCellEditor)realEditor).getComponent();
		text.setDocument(new FileNameDocument());	// filter forbidden characters
	}

	/**
		Overridden to set a clipable JTextField as editor component
	*/
	protected TreeCellEditor createTreeCellEditor() {
		Border aBorder = UIManager.getBorder("Tree.editorBorder");
		DefaultCellEditor editor = new DefaultCellEditor(new DefaultClipableTextField(aBorder));
		editor.setClickCountToStart(1);
		return editor;
	}


	/**
		Store editing node temporarily and manage layout container.
	*/
	public Component getTreeCellEditorComponent(
		JTree tree,
		Object value,
		boolean isSelected,
		boolean expanded,
		boolean leaf,
		int row)
	{
		Component c = super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
		((WideEditorContainer)editingContainer).setLocalCopy(tree, lastPath, offset, editingComponent);    

		currDMTN = (DefaultMutableTreeNode)value;
		nodeToEdit = (NetNode)currDMTN.getUserObject();

		return c;                                   
	}   


	private Component getEditingComponent()	{
		return editingComponent;
	}

	/**
		Start Timer with 200 instead of 1200 millis delay.
	*/
	protected void startEditingTimer() {
		if (timer == null) {
			timer = new Timer(200, this);
			timer.setRepeats(false);
		}
		timer.start();
	}


	/**
		Call super and request focus on textfield.
	*/
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		if (tree != null && lastPath != null && getEditingComponent() != null) {
			getEditingComponent().requestFocus();
		}
	}



	// class for watching inserted characters
	
	protected class FileNameDocument extends PlainDocument
	{
		public void insertString(int offs, String str, AttributeSet a)
			throws BadLocationException
		{
			if (tree.isEditing() && nodeToEdit != null && nodeToEdit.isManipulable())	{
				char[] source = str.toCharArray();
				char[] result = new char[source.length];
				int j = 0;
				for (int i = 0; i < source.length; i++) {
					if (ValidFilename.checkFileCharacter(source[i]))	{
						result[j++] = source[i];
					}
					else {
						Toolkit.getDefaultToolkit().beep();
						System.err.println("FEHLER: >"+source[i]+"<"+" in "+nodeToEdit);
					}
				}
				super.insertString(offs, new String(result, 0, j), a);
			}
			else	{
				super.insertString(offs, str, a);
			}
		}
	}




	// class managing layout of tree cell editor
	
	private class WideEditorContainer extends DefaultTreeCellEditor.EditorContainer
	{
		JTree     tree;
		TreePath  lastPath;
		int       offset;
		Component editingComponent;
			
		public void doLayout() {
			if (editingComponent != null && lastPath != null) {
			 Dimension  cSize = getSize();
			 Dimension  eSize = editingComponent.getPreferredSize();
			 int n = lastPath.getPathCount(); 
			 Rectangle r = new Rectangle();
			 r = tree.getBounds(r);
			 eSize.width = r.width -(offset *n);
			 editingComponent.setSize(eSize);
			 editingComponent.setLocation(offset, 0);
			 editingComponent.setBounds(offset, 0, eSize.width, cSize.height);
			 setSize(new Dimension(eSize.width + offset, cSize.height));
			}
		}
			 
		void setLocalCopy(JTree tree, TreePath lastPath,
											 int offset,Component editingComponent) {
			this.tree     = tree;
			if (lastPath != null)
				this.lastPath = lastPath;
			this.offset   = offset;
			this.editingComponent = editingComponent;
		}
	}



	// editor component for tree

	class DefaultClipableTextField extends ClipableJTextField
	{
		// source code "reusage" by cut & paste (ahem)
		protected Border         border;
		
		public DefaultClipableTextField(Border border) {
			this.border = border;
		}
		
		public Border getBorder() {
			return border;
		}
		
		public Font getFont() {
			Font     font = super.getFont();
			if (font instanceof FontUIResource) {
				Container     parent = getParent();
				if(parent != null && parent.getFont() != null)
					font = parent.getFont();
			}
			return font;
		}
		
		public Dimension getPreferredSize() {
			Dimension      size = super.getPreferredSize();
			if (myRenderer != null && NodeCellEditor.this.getFont() == null) {
				Dimension     rSize = myRenderer.getPreferredSize();
				size.height = rSize.height;
			}
			return size;
		}
	}

}
