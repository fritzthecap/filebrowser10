package fri.gui.swing.editor;

import java.io.File;
import javax.swing.*;
import java.awt.*;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.swing.mdi.MdiFrame;

/**
	The MDI pane that makes menu and toolbar layout and
	allocates a TextEditFileManager for an opened File.

	@author  Ritzberger Fritz
*/

class TextEditorMdiPane extends EditorMdiPane
{
	private SpinNumberField tabSize;


	/**
		Create a desktop pane for internal frames.
		@param frame main window on which "close()" will be called on Menu-Exit.
	*/
	public TextEditorMdiPane(TextEditController controller)	{
		super(controller);
	}


	/**
		Wrap an TextEditFileManager around the passed file.
	*/
	public MdiFrame createMdiFrame(Object toRender)	{
		toRender = standardizeFile(toRender);

		return super.createMdiFrame(
				new TextEditFileManager((File)toRender, controller, popupMouseListener));
	}


	/** Overridden to add autoindent, wrap lines, tab size actions. */
	protected void fillToolBarFind(JToolBar toolbar)	{
		super.fillToolBarFind(toolbar);
		
		controller.visualizeAction(TextEditController.MENUITEM_GOTO, toolbar);
		controller.visualizeAction(TextEditController.MENUITEM_CONCORDANCE, toolbar);

		toolbar.addSeparator();
		controller.visualizeCheckableAction(TextEditController.MENUITEM_AUTOINDENT, Config.getAutoIndent(), toolbar);
		controller.visualizeCheckableAction(TextEditController.MENUITEM_WRAPLINES, Config.getWrapLines(), toolbar);

		toolbar.addSeparator();
		String tabTooltip = "Number Of Spaces For A Tabulator";
		JLabel tabLabel = new JLabel("Tab");
		tabLabel.setToolTipText(tabTooltip);
		toolbar.add(tabLabel);
		tabSize = new SpinNumberField(Config.getTabSize(), 1, 16);
		tabSize.setToolTipText(tabTooltip);
		tabSize.setMinimumSize(new Dimension(40, tabSize.getMinimumSize().height));
		tabSize.setMaximumSize(new Dimension(40, tabSize.getMaximumSize().height));
		tabSize.setPreferredSize(new Dimension(40, tabSize.getPreferredSize().height));
		tabSize.getNumberEditor().addNumberEditorListener((NumberEditorListener)controller);
		toolbar.add(tabSize);
		toolbar.add(Box.createHorizontalGlue());
	}


	protected void fillMenuOptions(JMenu options)	{
		JMenu saveoptions = new JMenu(TextEditController.MENU_SAVEWITH);
		String newline = Config.getNewline();
		AbstractButton ab;
		ButtonGroup group = new ButtonGroup();
		ab = controller.visualizeCheckableAction(TextEditController.MENUITEM_PLATFORMNEWLINE, newline == null, saveoptions);
		group.add(ab);
		ab = controller.visualizeCheckableAction(TextEditController.MENUITEM_UNIX_NEWLINE, newline != null && newline.equals(Config.UNIX_NEWLINE), saveoptions);
		group.add(ab);
		ab = controller.visualizeCheckableAction(TextEditController.MENUITEM_WINDOWS_NEWLINE, newline != null && newline.equals(Config.WINDOWS_NEWLINE), saveoptions);
		group.add(ab);
		ab = controller.visualizeCheckableAction(TextEditController.MENUITEM_MAC_NEWLINE, newline != null && newline.equals(Config.MAC_NEWLINE), saveoptions);
		group.add(ab);
		options.add(saveoptions);

		JMenu encoding = new JMenu(TextEditController.MENU_ENCODING);
		AbstractButton encodingItem = controller.visualizeAction(TextEditController.MENUITEM_CHOOSE_ENCODING, encoding, false);
		((TextEditController)controller).setEncodingItem(encodingItem);
		encoding.addSeparator();
		controller.visualizeCheckableAction(TextEditController.MENUITEM_DETECT_BYTEORDERMARK, Config.getDetectEncodingFromByteOrderMark(), encoding);
		controller.visualizeCheckableAction(TextEditController.MENUITEM_CREATE_BYTEORDERMARK, Config.getCreateByteOrderMark(), encoding);
		encoding.addSeparator();
		controller.visualizeCheckableAction(TextEditController.MENUITEM_DETECT_XMLHEADER, Config.getDetectXmlOrHtmlHeaderEncoding(), encoding);
		options.add(encoding);

		super.fillMenuOptions(options);
	}

	protected void fillMenuEditFind(JMenu edit)	{
		super.fillMenuEditFind(edit);
		controller.visualizeAction(TextEditController.MENUITEM_GOTO, edit, false);
		edit.addSeparator();
		controller.visualizeAction(TextEditController.MENUITEM_CONCORDANCE, edit, false);
	}



	public void fillPopupMenu(JPopupMenu popup)	{
		super.fillPopupMenu(popup);
		controller.visualizeAction(TextEditController.MENUITEM_GOTO, popup, false);
	}

}
