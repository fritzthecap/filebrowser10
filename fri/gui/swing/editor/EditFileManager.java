package fri.gui.swing.editor;

import java.io.File;
import java.awt.Component;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import fri.gui.mvc.view.swing.PopupMouseListener;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.mdi.*;

/**
	The lifecycle manager for exactly one open file. This manager creates a contained Component
	for a created new editor panel (internal frame or tabpanel), provides a title and
	tooltip, and receives all events from MDI: activated, closing, closed. Further
	it holds the rendered Object, a File in this case.
	<p>
	The method <i>setRenderedObject()</i> does NOT set a new file into the editor,
	it just changes the title and tooltip.
	<p>
	Implement <i>getRenderingComponent()</i> to manage a special kind of (text-)editor.
*/

public abstract class EditFileManager implements MdiObjectResponsibilities
{
	protected File file;
	protected EditController controller;
	protected PopupMouseListener popupListener;
	protected EditorTextHolder editor;
	private static final String UNNAMED = "Unnamed";
	private static final String UNNAMED_TOOLTIP = "New File";
	protected JPanel panel;


	public EditFileManager(File file, EditController controller, PopupMouseListener popupListener)	{
		this.file = file;
		this.controller = controller;
		this.popupListener = popupListener;
	}


	/** Override this to create just the editor area without scrollpane or load() call. */
	protected abstract EditorTextHolder createEditArea(File file);


	/**
		Returns a Component that renders the passed Object.
		This Component will then be added to container on CENTER.
		<p>
		This method builds the GUI: calls createEditArea(),
		allocates a panel and a scrollpane that holds the editor.
		<p>
		Override this and call opened(editor) after building a
		customized GUI (do not forget to allocate panel).
	*/
	public Component getRenderingComponent(MdiFrame ic)	{
		if (panel != null)
			return panel;
		
		this.editor = createEditArea(file);
		
		panel = new JPanel(new BorderLayout());
		panel.add(new JScrollPane(editor.getTextComponent()), BorderLayout.CENTER);

		opened(editor);

		return panel;
	}
	
	/** Performs open logic: setProgressContainer, load calls. */
	protected void opened(EditorTextHolder editor)	{
		// keep order!
		
		controller.editorOpened(editor);	// add undo listener before load(), as this happens in background

		editor.setProgressContainer(panel);
		editor.load();	// load (background) after controller added all listeners

		// end keep order

		editor.getTextComponent().addMouseListener(popupListener);
	}



	/**
		Implements MdiObjectResponsibilities:
		Returns the title of the rendered Object that will be displayed in titlebar or on tab header.
	*/
	public String getTitle(MdiFrame containerType)	{
		if (containerType instanceof MdiInternalTabPanel)	{
			return file != null ? file.getName() : UNNAMED;
		}
		else	{
			return file != null ? file.getName()+(file.getParent() != null ? " in "+file.getParent() : "") : UNNAMED;
		}
	}

	public String getToolTip(MdiFrame containerType)	{
		return file != null ? file.getAbsolutePath() : UNNAMED_TOOLTIP;
	}



	/** Sets the rendered object (e.g. when it was renamed). */
	public void setRenderedObject(Object renderedObject)	{
		this.file = (File)renderedObject;
	}

	/** Returns the rendered object. */
	public Object getRenderedObject()	{
		return file;
	}


	/**
		Called every time the internal container gets activated (becomes visible).
	*/
	public void activated(MdiFrame ic)	{
		controller.editorActivated(editor);
	}

	/**
		The built Component gets closed.
		Used for save confirmation when text was changed.
	*/
	public void closing(MdiFrame ic, PropertyChangeEvent e)
		throws PropertyVetoException
	{
		editor.interrupt();
		
		if (editor.getChanged() == false)
			return;

		// must warn, bring frame to front
		controller.setSelectedEditor(editor);
		
		int ret = JOptionPane.showConfirmDialog(
				ComponentUtil.getWindowForComponent(editor.getTextComponent()),
				"Save changes?",
				getTitle(null),
				JOptionPane.YES_NO_CANCEL_OPTION);

		if (ret == JOptionPane.CANCEL_OPTION)
			throw new PropertyVetoException("Benutzerabbruch", e);

		if (ret == JOptionPane.YES_OPTION)	{
			if (controller.save(editor) == false)	// must return saved file
				throw new PropertyVetoException("Editor did NOT save file", e);
		}
	}

	/** The built Component was closed. Uninstall popup listener and  pass event to controller. */
	public void closed(MdiFrame ic)	{
		editor.getTextComponent().removeMouseListener(popupListener);
		controller.editorClosed(editor);
	}
	
	
	/** Returns the editor that was created by this manager. */
	public EditorTextHolder getEditorTextHolder()	{
		return editor;
	}
	
	
	/** Returns true if both EditFileManagers have the same File. */
	public boolean equals(Object o)	{
		EditFileManager other = (EditFileManager)o;
		return other.file != null && file != null && other.file.equals(file);
	}
	
}