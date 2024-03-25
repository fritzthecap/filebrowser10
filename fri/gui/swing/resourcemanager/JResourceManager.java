package fri.gui.swing.resourcemanager;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import fri.gui.awt.component.visitor.ContainerVisitor;
import fri.gui.awt.resourcemanager.ResourceManager;
import fri.gui.awt.resourcemanager.component.ArtificialComponent;
import fri.gui.awt.resourcemanager.persistence.AbstractResourceFile;
import fri.gui.swing.resourcemanager.persistence.JResourceFileFactory;
import fri.gui.awt.resourcemanager.dialog.ResourceSetEditor;
import fri.gui.swing.resourcemanager.dialog.*;
import fri.gui.awt.resourcemanager.resourceset.ResourceSet;
import fri.gui.swing.resourcemanager.resourceset.JResourceSet;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;
import fri.gui.swing.resourcemanager.resourceset.resource.JResourceFactory;
import fri.gui.swing.resourcemanager.component.JTopDownResourceContainerVisitee;

/**
	The Swing specific ResourceManager. It provides access to Swing-specific
	resources and dialogs.
*/

public class JResourceManager extends ResourceManager
{
	/**
		Perform customizing for a frame or dialog.
		@param window the parent window that contains all components that should be customizable.
				This will also be the dialogparent for any customize dialog.
	*/
	public JResourceManager(Window w)	{
		super(w);
	}

	/**
		Perform customizing for an explicitely given component.
		@param dialogParent the parent window that will be the identification for the
				resource-file, and the dialogparent for any customize dialog.
				This window will NOT be looped for customizable components!
		@param component a component that should be customizable.
	*/
	public JResourceManager(Window dialogParent, Object component)	{
		super(dialogParent, component);
	}

	/**
		Perform customizing for an explicitely given component.
		@param dialogParent the parent window that will be the identification for the
				resource-file, and the dialogparent for any customize dialog.
				This window will NOT be looped for customizable components!
		@param components array of components that should be customizable.
	*/
	public JResourceManager(Window dialogParent, Object [] component)	{
		super(dialogParent, component);
	}


	/** Creates an AbstractResourceFile for a window type. To be overridden for providing another ResourceFile persistence. */
	protected AbstractResourceFile createResourceFile(String windowTypeName, ResourceFactory resourceFactory)	{
		return new JResourceFileFactory().getResourceFile(windowTypeName, resourceFactory);
	}

	/** Factory method to create a resource set. Overridden to return JResourceSet. */
	protected ResourceSet createResourceSet(Object toCustomize, Resource [] resources)	{
		return new JResourceSet(toCustomize, resources);
	}


	/** Overridden to visit the passed container by a Swing visitee. */
	protected void visitContainerTopDown(Object component, ContainerVisitor visitor)	{
		new JTopDownResourceContainerVisitee(component, visitor);
	}
	
	/** Overridden to allocate JResourceFactory. */
	protected ResourceFactory createResourceFactory()	{
		return new JResourceFactory();
	}

	/** Return false if the passed name is a component type that is not customizable. */
	protected boolean isCustomizable(String typeName)	{	// this covers Swing
		if (super.isCustomizable(typeName) == false)
			return false;
		if (	// do not let customize invisible manager panes
				typeName.startsWith("plaf_") && typeName.indexOf("comboboxeditor") < 0 ||
				typeName.equals("glasspane") ||
				typeName.equals("contentpane") ||
				typeName.equals("layeredpane") ||
				typeName.equals("scrollpane_scrollbar") ||
				typeName.indexOf("cellrenderer") >= 0 ||
				typeName.endsWith("scrollbutton") ||
				typeName.equals("toolbar_separator") ||
				typeName.equals("box_filler") ||
				typeName.equals("rootpane"))
			return false;
		return true;
	}


	/** Overridden to call a Swing customize dialog implementation. */
	protected ResourceSetEditor showCustomizeDialog(Window dialogParent, ResourceSet resourceSet)	{
		if (dialogParent instanceof Dialog)
			return new JCustomizerGUI((Dialog)dialogParent, resourceSet, this);
		else
			return new JCustomizerGUI((Frame)dialogParent, resourceSet, this);
	}
	
	/** Overridden to call a Swing choice dialog implementation. */
	protected void showChoiceDialog(Window dialogParent)	{
		if (dialogParent instanceof Dialog)
			new JComponentChoiceGUI((Dialog)dialogParent, this, createResourceFactory());
		else
			new JComponentChoiceGUI((Frame)dialogParent, this, createResourceFactory());
	}


	/** Called when a component was removed (ContainerEvent). Overridden for handling JTabbedPane. */
	protected void dynamicComponentRemoved(Object c, Container parent)	{
		if (parent instanceof JTabbedPane)	{	// look for the artificial tab of the removed panel
			Map map = getComponentMap();
			Object toRemove = null;
			for (Iterator it = map.keySet().iterator(); toRemove == null && it.hasNext(); )	{
				Object component = it.next();
				if (component instanceof ArtificialComponent && ((ArtificialComponent)component).getComponent() == parent)
					toRemove = component;
			}
			
			if (toRemove != null)	{
				super.dynamicComponentRemoved(toRemove, null);
			}
		}
		super.dynamicComponentRemoved(c, parent);
	}

}
