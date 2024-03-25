package fri.gui.swing.table.header;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;

public class EditableHeaderUI extends BasicTableHeaderUI
{    
	protected MouseInputListener createMouseInputListener() {
		return new MouseInputHandler((EditableHeader)header);
	}

	public class MouseInputHandler extends BasicTableHeaderUI.MouseInputHandler
	{
		private Component dispatchComponent;
		protected EditableHeader header;
		private int editingColumn = -1;
		private MouseEvent mousePress, mouseRelease;

		public MouseInputHandler(EditableHeader header) {
			this.header = header;
		}

		private void setDispatchComponent(MouseEvent e) { 
			Component editorComponent = header.getEditorComponent();
			Point p = e.getPoint();
			Point p2 = SwingUtilities.convertPoint(header, p, editorComponent);
			dispatchComponent = SwingUtilities.getDeepestComponentAt(editorComponent, p2.x, p2.y);
		}

		private boolean repostEvent(MouseEvent e) { 
			if (dispatchComponent == null) {
				return false; 
			}
			MouseEvent e2 = SwingUtilities.convertMouseEvent(header, e, dispatchComponent);
			dispatchComponent.dispatchEvent(e2); 
			return true; 
		}

		private int getColumnForMouseEvent(MouseEvent e)	{
			Point p = e.getPoint();
			TableColumnModel columnModel = header.getColumnModel();
			return columnModel.getColumnIndexAtX(p.x);
		}

		public void mousePressed(MouseEvent e) {
			super.mousePressed(mousePress = e);

			if (header.isEditing()) {
				int index = getColumnForMouseEvent(e);

				if (index != editingColumn) {
					header.getCellEditor().cancelCellEditing();
				}
			}
		}

		public void mouseClicked(MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e))	{
				return;
			}

			if (header.getResizingColumn() == null && header.getDraggedColumn() == null) {
				int index = getColumnForMouseEvent(e);
				if (index != -1) {
					if (header.editCellAt(index, e)) {
						editingColumn = index;
						setDispatchComponent(e); 
						repostEvent(mousePress);
						repostEvent(mouseRelease);
						repostEvent(e);
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(mouseRelease = e);
			
			if (!SwingUtilities.isLeftMouseButton(e)) {
				return;
			}
			
			repostEvent(e); 
			dispatchComponent = null;    
		}
		
	}

}