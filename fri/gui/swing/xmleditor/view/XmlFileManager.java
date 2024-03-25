package fri.gui.swing.xmleditor.view;

import java.io.File;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.mdi.*;
import fri.gui.swing.xmleditor.model.Configuration;
import fri.gui.swing.xmleditor.controller.*;
import fri.gui.swing.util.CommitTreeTable;

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

public class XmlFileManager implements MdiObjectResponsibilities
{
	private String uri;
	private XmlEditController controller;
	protected XmlTreeTable treetable;
	protected JPanel panel;


	public XmlFileManager(String uri, XmlEditController controller)	{
		this.uri = uri;
		this.controller = controller;
	}


	/**
		Returns a Component that renders the passed Object.
		This Component will then be added to container on CENTER.
	*/
	public Component getRenderingComponent(MdiFrame ic)	{
		if (panel != null)
			return panel;
		
		try	{
			treetable = new XmlTreeTable(uri, Configuration.getDefault());
			this.uri = treetable.getURI();
			
			JScrollPane sp = new JScrollPane(treetable);
			panel = new JPanel(new BorderLayout());
			panel.add(sp, BorderLayout.CENTER);
	
			// manage drag and drop
			new XmlDndPerformer(
					treetable,
					sp,
					controller,
					new FileDndPerformer(sp.getViewport(), controller));
			treetable.setNoDragAndDrop(false);
			
			final JTextField statusBar = new JTextField();
			Font font = statusBar.getFont();
			statusBar.setFont(font.deriveFont(Font.BOLD, font.getSize()));
			statusBar.setEditable(false);
			treetable.getSelectionModel().addListSelectionListener(new ListSelectionListener()	{
				public void valueChanged(ListSelectionEvent e) {
					showXPath(statusBar);
				}
			});
			panel.add(statusBar, BorderLayout.SOUTH);
		}
		catch (Exception e)	{
			e.printStackTrace();
			JOptionPane.showMessageDialog(
					ComponentUtil.getWindowForComponent((Component)ic),
					"Error when loading document: "+e.getMessage(),
					"Error",
					JOptionPane.ERROR_MESSAGE);
		}
		
		return panel;
	}
	

	private void showXPath(JTextField statusBar)	{
		TreePath tp = treetable.getSelectionPath();
		Object [] nodes = tp != null ? tp.getPath() : null;
		StringBuffer sb = new StringBuffer();
		
		for (int i = 1; nodes != null && i < nodes.length; i++)	{	// 1: leave out #document
			sb.append("/");
			sb.append(nodes[i].toString());	// element name

			// optionally make XPath-like index
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes[i];
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
			int siblings = 0;	// number of children with same element name
			int position = 1;	// XPath position of this node, 1-n
			boolean found = false;

			for (int j = 0; parent != null && j < parent.getChildCount(); j++)	{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(j);
				
				if (child == nodes[i])
					found = true;
				
				if (child.toString().equals(nodes[i].toString()))	{
					siblings++;
					if (found == false)
						position++;
				}
			}
			
			if (siblings > 1)
				sb.append("["+position+"]");
		}
		
		statusBar.setText(sb.toString());
	}

	
	/**
		Implements MdiObjectResponsibilities:
		Returns the title of the rendered Object that will be displayed in titlebar or on tab header.
	*/
	public String getTitle(MdiFrame containerType)	{
		File f = new File(uri);
		String s = f.getName();
		if (containerType instanceof MdiInternalTabPanel)	{
			return s != null && s.length() > 0 ? s : uri;
		}
		else	{
			String path = f.getParent();
			return s != null && s.length() > 0 ? s+" in "+path : uri;
		}
	}

	public String getToolTip(MdiFrame containerType)	{
		return uri;
	}



	/** Sets the rendered object (e.g. when it was renamed). */
	public void setRenderedObject(Object renderedObject)	{
		this.uri = (String)renderedObject;
	}

	/** Returns the rendered object. */
	public Object getRenderedObject()	{
		return uri;
	}


	/**
		Called every time the internal container gets activated (becomes visible).
	*/
	public void activated(MdiFrame ic)	{
		controller.setView(treetable);
	}

	/**
		The built Component gets closed.
		Used for save confirmation when text was changed.
	*/
	public void closing(MdiFrame ic, PropertyChangeEvent e)
		throws PropertyVetoException
	{
		CommitTreeTable.commit(treetable);	// end cell editor
		if (treetable.isChanged() == false)
			return;

		controller.setSelectedEditor(treetable);	// must warn, bring frame to front
		
		int ret = JOptionPane.showConfirmDialog(
				ComponentUtil.getWindowForComponent(treetable),
				"Save changes?",
				getTitle(null),
				JOptionPane.YES_NO_CANCEL_OPTION);

		if (ret == JOptionPane.CANCEL_OPTION)
			throw new PropertyVetoException("Benutzerabbruch", e);

		if (ret == JOptionPane.YES_OPTION)	{
			if (controller.save() == false)	// must return saved file
				throw new PropertyVetoException("Editor did NOT save file", e);
		}
	}

	/** The built Component was closed. Uninstall popup listener and  pass event to controller. */
	public void closed(MdiFrame ic)	{
		controller.closeView(treetable);
	}
	
	
	/** Returns the editor that was created by this manager. */
	public Component getEditor()	{
		return treetable;
	}
	
	
	/** Returns true if both EditFileManagers have the same File. */
	public boolean equals(Object o)	{
		XmlFileManager other = (XmlFileManager)o;
		return other.uri.equals(uri);
	}
	
}