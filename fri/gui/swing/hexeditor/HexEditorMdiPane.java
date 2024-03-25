package fri.gui.swing.hexeditor;

import java.io.File;
import javax.swing.*;
import java.awt.Dimension;
import fri.gui.swing.mdi.MdiFrame;
import fri.gui.swing.editor.EditorMdiPane;
import fri.gui.swing.util.MenuBarUtil;

/**
	The MDI pane that makes menu and toolbar layout and
	allocates a HexEditFileManager for an opened File.
	It visualizes additional action items in toolbar and menu.

	@author  Ritzberger Fritz
*/

public class HexEditorMdiPane extends EditorMdiPane
{
	/**
		Create a desktop pane for internal frames.
		@param frame main window on which "close()" will be called on Menu-Exit.
	*/
	public HexEditorMdiPane(HexEditController controller)	{
		super(controller);
	}


	/**
		Wrap an HexEditFileManager around the passed file.
	*/
	public MdiFrame createMdiFrame(Object toRender)	{
		toRender = standardizeFile(toRender);

		return super.createMdiFrame(
				new HexEditFileManager((File)toRender, controller, popupMouseListener));
	}



	/** Overridden to NOT create a line renderer. */
	protected void fillToolBarLineRenderer(JToolBar toolbar)	{
	}

	/** Overridden to make position renderer a lot wider. This label receives Point.x */
	protected void fillToolBarPositionRenderer(JToolBar toolbar)	{
		positionRenderer = new JLabel(" ");
		positionRenderer.setToolTipText("Byte Offset Of Selected Cell (Decimal)");
		positionRenderer.setHorizontalAlignment(JLabel.CENTER);
		positionRenderer.setBorder(BorderFactory.createLoweredBevelBorder());
		positionRenderer.setMinimumSize(new Dimension(48, positionRenderer.getMinimumSize().height));
		positionRenderer.setMaximumSize(new Dimension(48, positionRenderer.getMinimumSize().height));
		positionRenderer.setPreferredSize(new Dimension(48, positionRenderer.getPreferredSize().height));
		toolbar.add(positionRenderer);
	}


	protected void fillToolBarEdit(JToolBar toolbar)	{
		controller.visualizeAction(HexEditController.MENUITEM_INSERT, toolbar);
		controller.visualizeAction(HexEditController.MENUITEM_REMOVE, toolbar);
		controller.visualizeAction(HexEditController.MENUITEM_EDIT, toolbar);
		toolbar.addSeparator();
		
		super.fillToolBarEdit(toolbar);
	}

	public void fillMenuBar(JMenuBar mb)	{
		super.fillMenuBar(mb);
		
		ButtonGroup group;
		AbstractButton ab;
		JMenu view = new JMenu(HexEditController.MENU_VIEW);
		
		group  = new ButtonGroup();
		ab = controller.visualizeCheckableAction(HexEditController.MENUITEM_HEX, Config.getBase() == 16, view);
		group.add(ab);
		ab = controller.visualizeCheckableAction(HexEditController.MENUITEM_DEC, Config.getBase() == 10, view);
		group.add(ab);
		ab = controller.visualizeCheckableAction(HexEditController.MENUITEM_OCT, Config.getBase() == 8, view);
		group.add(ab);
		ab = controller.visualizeCheckableAction(HexEditController.MENUITEM_BIN, Config.getBase() == 2, view);
		group.add(ab);
		ab = controller.visualizeCheckableAction(HexEditController.MENUITEM_CHAR, Config.getBase() == 0, view);
		group.add(ab);
		
		view.addSeparator();
		
		group = new ButtonGroup();
		ab = controller.visualizeCheckableAction(HexEditController.MENUITEM_16_COLUMNS, Config.getColumnCount() == 16, view);
		group.add(ab);
		ab = controller.visualizeCheckableAction(HexEditController.MENUITEM_32_COLUMNS, Config.getColumnCount() == 32, view);
		group.add(ab);
		ab = controller.visualizeCheckableAction(HexEditController.MENUITEM_64_COLUMNS, Config.getColumnCount() == 64, view);
		group.add(ab);
		
		MenuBarUtil.insertMenu(mb, view, 2);	// insert at position after "File" and "Edit"
	}

	protected void fillMenuEdit(JMenu edit)	{
		controller.visualizeAction(HexEditController.MENUITEM_INSERT, edit, false);
		controller.visualizeAction(HexEditController.MENUITEM_REMOVE, edit, false);
		controller.visualizeAction(HexEditController.MENUITEM_EDIT, edit, false);
		edit.addSeparator();
		
		super.fillMenuEdit(edit);
	}

}
