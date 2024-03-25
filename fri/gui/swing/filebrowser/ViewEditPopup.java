package fri.gui.swing.filebrowser;

import java.io.File;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import fri.gui.CursorUtil;

/**
	A general purpose popup that provides "View" and "Edit" actions to any Component.
	Doubleclick will trigger "View".
*/

public class ViewEditPopup extends JPopupMenu implements
	MouseListener,
	ActionListener
{
	private FileSelection selectionHolder;
	private JMenuItem edit, view;
	private Component eventComponent;

	/** Clients implement this interface to return all selected files upon a popup action event. */
	public interface FileSelection
	{
		public File [] getFiles();
	}
	
	
	public ViewEditPopup(FileSelection selectionHolder)	{
		this.selectionHolder = selectionHolder;
		
		add(view = new JMenuItem("View"));
		view.addActionListener(this);
		add(edit = new JMenuItem("Edit"));
		edit.addActionListener(this);
	}

	public void mousePressed(MouseEvent e)	{
		eventComponent = e.getComponent();
		if (e.isPopupTrigger())
			show(eventComponent, e.getX(), e.getY());
	}
	public void mouseReleased(MouseEvent e)	{
		eventComponent = e.getComponent();
		if (e.isPopupTrigger())
			show(eventComponent, e.getX(), e.getY());
	}
	public void mouseClicked(MouseEvent e)	{
		eventComponent = e.getComponent();
		if (e.getClickCount() >= 2)
			viewOrEditAction(false);	// view node
	}
	public void mouseEntered(MouseEvent e)	{}
	public void mouseExited(MouseEvent e)	{}

	/** Implements ActionListener to respond to contained menu actions. */
	public void actionPerformed(ActionEvent e)	{
		viewOrEditAction(e.getSource() == edit);
	}

	private void viewOrEditAction(boolean isEdit)	{
		File [] files = selectionHolder.getFiles();

		if (files != null)	{
			Component c = eventComponent;
			CursorUtil.setWaitCursor(c);
			try	{
				if (isEdit)
					TreeEditController.getEditor(files);
				else
					for (int i = 0; i < files.length; i++)
						new FileViewer(files[i]);
			}
			finally	{
				CursorUtil.resetWaitCursor(c);
			}
		}
		
		eventComponent = null;
	}

}
