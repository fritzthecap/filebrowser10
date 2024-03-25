package fri.gui.awt.resourcemanager;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import fri.gui.awt.component.WindowTypeName;
import fri.gui.awt.keyboard.KeyNames;
import fri.gui.awt.component.visitor.*;
import fri.gui.awt.resourcemanager.dialog.*;
import fri.gui.awt.resourcemanager.component.*;
import fri.gui.awt.resourcemanager.persistence.*;
import fri.gui.awt.resourcemanager.resourceset.*;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	Main package class, to be used by AWT customize clients, base class for Swing implementation.
	Listens for dynamical added components, installs key listeners, implements their callback to start customize dialogs,
	and loads and sets resources for Windows from persistence.
	<p>
	This class provides a System-Property named <i>fri.gui.awt.resourcemanager.customizeKey</i> which can be evaluated
	with Strings like "Ctrl+Alt+Shift-F7". This key brings up the customize dialog on focused components. Another System-Property named
	<i>fri.gui.awt.resourcemanager.chooseKey</i> brings up a dialog choice which lets choose a component for customizing,
	and lets edit all GUI texts in multiple languages (Swing only).
	<p>
	A ResourceManager exposes a method to bring up a component choice dialog for some parent Window:
	<pre>
		resourceManager.showDialogForAll (parentWindow);
	</pre>
	Technical specification:
	<ul>
		<li>Loops component tree and applies existing resources.</li>
		<li>Creates a AWT- or Swing-specific ResourceFactory, that will find resource classes dynamically.</li>
		<li>Distinguishes between customizable and non-customizable components (glasspane, layeredpane, ...).</li>
		<li>Installs KeyListeners on all focusable components, and opens dialogs on F7 key.</li>
		<li>Performs refreshes on all ResourceManager instances after dialog returned.</li>
		<li>Listens for added or removed components and makes them customizable or deregisters them.</li>
		<li>Tries to match lost resources by their label text (after progammer's window modifications).</li>
		<li>Deregisters all collected components from their resource sets on window close.</li>
	</ul>
	
	@author Fritz Ritzberger, 2004
	@see fri.gui.swing.resourcemanager.ResourceManagingEventQueue
*/

public class ResourceManager implements
	ContainerVisitor,
	KeyListener,
	WindowListener,
	ContainerListener
{
	private static int CUSTOMIZE_KEY = KeyEvent.VK_F7;
	private static int CHOOSE_KEY = CUSTOMIZE_KEY | InputEvent.ALT_MASK;
	static	{
		String customizeKey = System.getProperty("fri.gui.awt.resourcemanager.customizeKey");
		if (customizeKey != null)
			CUSTOMIZE_KEY = KeyNames.getInstance().getKeyCode(customizeKey) | KeyNames.getInstance().getKeyModifiers(customizeKey);

		String chooseKey = System.getProperty("fri.gui.awt.resourcemanager.chooseKey");
		if (chooseKey != null)
			CHOOSE_KEY = KeyNames.getInstance().getKeyCode(chooseKey) | KeyNames.getInstance().getKeyModifiers(chooseKey);
	}

	private static final List instances = new ArrayList();	// need to notify other instances of a new resource set
	
	// permanent member variables
	private Window window;
	private AbstractResourceFile resourceFile;
	private Map componentMap = new Hashtable();
	private Map containerEventSources = new Hashtable();
	
	// temporary member variables
	private Hashtable scanMap = new Hashtable();
	private String dynamicContainerName;	// temporary name of dynamical filled containers
	private List dynamicComponents;
	

	/**
		Perform customizing for a frame or dialog.
		@param window the parent window that contains all components that should be customizable.
				This will also be the dialogparent for any customize dialog.
	*/
	public ResourceManager(Window window)	{
		this(window, window);
	}

	/**
		Perform customizing for an explicitely given component.
		@param dialogParent the parent window that will be the identification for the
				resource-file, and the dialogparent for any customize dialog.
				This window will NOT be looped for customizable components!
		@param component a component that should be customizable.
	*/
	public ResourceManager(Window dialogParent, Object component)	{
		this(dialogParent, new Object [] { component });
	}

	/**
		Perform customizing for an explicitely given component.
		@param dialogParent the parent window that will be the identification for the
				resource-file, and the dialogparent for any customize dialog.
				This window will NOT be looped for customizable components!
		@param components array of components that should be customizable.
	*/
	public ResourceManager(Window dialogParent, Object [] components)	{
		this.window = dialogParent;
		this.resourceFile = createResourceFile(WindowTypeName.windowTypeName(getWindow()), createResourceFactory());

		dynamicComponents = new ArrayList();	// enable dynamic container detection
		addComponents(components);	// loop containers recursively

		Object [] oarr = dynamicComponents.toArray();	// add dynamic components
		dynamicComponents = null;	// disable dynamic container detection
		for (int i = 0; i < oarr.length; i++)
			dynamicComponentAdded(oarr[i]);

		getWindow().addWindowListener(this);	// listen for window close to remove contained components from their ResourceSets
		instances.add(this);
		System.err.println("Constructor, ResourceManager instances are now: "+instances.size()+", component count "+componentMap.size());
	}


	/** Add a Component for customizing. This is for popup menus that are not contained within the window's component tree. */
	public void addComponent(Object component)	{
		addComponents(new Object [] { component });
	}
	
	/** Add Components for customizing. This is for popup menus that are not contained within the window's component tree. */
	public void addComponents(Object [] components)	{
		for (int i = 0; i < components.length; i++)	// apply existing resources to all Components
			visitContainerTopDown(components[i], this);
			
		matchLostResources();	// try to match lost resources to existing components (workaround programmer's modifications)
	}


	/** Visits the passed container top down by a TopDownResourceContainerVisitee. To be overridden by Swing implementation. */
	protected void visitContainerTopDown(Object component, ContainerVisitor visitor)	{
		new TopDownResourceContainerVisitee(component, visitor);
	}
	
	/** Creates an AbstractResourceFile for a window type. To be overridden for providing another ResourceFile persistence. */
	protected AbstractResourceFile createResourceFile(String windowTypeName, ResourceFactory resourceFactory)	{
		return new ResourceFileFactory().getResourceFile(windowTypeName, resourceFactory);
	}

	/** Creates a factory that can allocate Resource subclasses. To be overridden by Swing ResourceManager (as this has other Resources). */
	protected ResourceFactory createResourceFactory()	{
		return new ResourceFactory();
	}

	/** Factory method to create a resource set. To be overridden by Swing implementation. */
	protected ResourceSet createResourceSet(Object toCustomize, Resource [] resources)	{
		return new ResourceSet(toCustomize, resources);
	}


	/** Implements ComponentVistee.Visitor to collect and customize resources within a Component tree. */
	public Object visit(Object c, Object userObject)	{
		ResourceComponentName componentName = new ResourceComponentName(c);	// build component type name
		String baseName = componentName.toString();	// brings "button" or "button_OK"
		String parent = userObject != null ? userObject.toString() : null;	// the parent containers name
		String hierarchicalName = HierarchicalName.hierarchicalNameFromParentAndChild(parent, baseName);	// make dotted name
		hierarchicalName = makeUnique(hierarchicalName);	// make unique name

		if (dynamicComponents != null)	{	// detection of dynamic components on construction
			if (withinDynamicContainer(parent, c))
				return hierarchicalName;	// do this later, without rootpane as parent
			
			detectDynamicContainer(baseName, hierarchicalName);	// try to detect a dynamic container
		}
		
		if (isDynamicContainer(baseName) || isCustomizable(baseName))	{
			if (c instanceof ResourceIgnoringComponent == false)	{
				componentMap.put(c, hierarchicalName);
				//System.err.println(hierarchicalName);
	
				ResourceSet resourceSet = getResourceSetForHierarchicalName(hierarchicalName);
				if (resourceSet != null)	// if there is a persistent resourceSet, add the visited Component to it
					resourceSet.addComponent(c);
				
				if (c instanceof Component && ((Component)c).isFocusTraversable() /*JDK14: isFocusable()*/)	{	// install customize dialog trigger F7
					((Component)c).removeKeyListener(this);
					((Component)c).addKeyListener(this);
				}
			}
			
			if (c instanceof Container && c instanceof ResourceIgnoringContainer == false)	{	// listen for dynamical added or removed components
				((Container)c).addContainerListener(this);
				containerEventSources.put(c, c);
			}
		}
		
		return c instanceof Window ? null : hierarchicalName;	// parent for next level of Components
	}

	private String makeUnique(String hierarchicalName)	{
		String s = hierarchicalName;
		for (int i = 0; scanMap.get(s) != null; i++)	// if already contained
			s = hierarchicalName + i;	// add a unique number
		scanMap.put(s, s);	// put to uniqueness map
		return s;
	}
	
	private boolean withinDynamicContainer(String parent, Object c)	{
		if (dynamicContainerName != null && parent != null && parent.startsWith(dynamicContainerName))	{
			if (parent.equals(dynamicContainerName))	// take only first level under dynamical container to list
				dynamicComponents.add(c);
			return true;
		}
		dynamicContainerName = null;	// not within dynamic container
		return false;
	}
	
	private boolean isDynamicContainer(String baseName)	{
		return baseName.equals("desktoppane") || baseName.equals("tabbedpane") /*|| baseName.equals("toolbar")*/;
	}
	
	private void detectDynamicContainer(String baseName, String hierarchicalName)	{
		if (isDynamicContainer(baseName))
			dynamicContainerName = hierarchicalName;
	}
	
	/** Return false if the passed name is a component type that is not customizable. For AWT this is all that starts with "popup". */
	protected boolean isCustomizable(String typeName)	{	// this covers AWT
		if (typeName.startsWith("popup"))
			return false;
		return true;
	}


	/**
		Returns the Map containing all customizable Components:
		key = Component, value = hierarchicalName (ResourceFile key).
		Needed by ComponentChoice to build buttons for all Components.
	*/
	public Map getComponentMap()	{
		return componentMap;
	}


	// interface KeyListener: start customize dialog
	
	/** Imlements KeyListener to catch F7 hotkey and bring up the Customizer GUI */
	public void keyPressed(KeyEvent e)	{
		int code = e.getKeyCode() | e.getModifiers();
		if (code == CUSTOMIZE_KEY)
			showDialogForOne((Component) e.getSource(), null);
		else
		if (code == CHOOSE_KEY)
			showDialogForAll(null);
	}
	public void keyTyped(KeyEvent e)	{}
	public void keyReleased(KeyEvent e)	{}

	
	/** Opens a Component chooser dialog exposing all customizeable Components within the obtained Container. */
	public void showDialogForAll(Window dialogParent)	{
		if (dialogParent == null)
			dialogParent = getWindow();
			
		dialogParent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		showChoiceDialog(dialogParent);
		dialogParent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	/** Opens a customize dialog for passed component, and stores any new resource on OK. */
	public void showDialogForOne(Object toCustomize, Window dialogParent)	{
		if (dialogParent == null)
			dialogParent = getWindow();
			
		// look for ResourceSet of toCustomize
		String hierarchicalName = (String) componentMap.get(toCustomize);
		ResourceSet resourceSet = getResourceSetForHierarchicalName(hierarchicalName);
		boolean found = (resourceSet != null);
		Resource [] resources = createResourceFactory().createResources(toCustomize);	// initializes resources from component
		if (found == false)
			resourceSet = createResourceSet(toCustomize, resources);	// no persistent Resource was found for this name
		else
			resourceSet.completeResources(resources);	// add all Resources for customization that are not persistent
		
		// show a customize dialog
		dialogParent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		ResourceSetEditor dialog = showCustomizeDialog(dialogParent, resourceSet);
		resourceSet = dialog.getResourceSet();
		dialogParent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
		// save results
		resourceFile.put(hierarchicalName, resourceSet);
		resourceFile.save();
		refreshAll();	// must set both component-bound and window-bound resources, on set and reset
	}
	
	/** Calls an AWT dialog to customize the GUI. To be overridden by Swing implementation. */
	protected ResourceSetEditor showCustomizeDialog(Window dialogParent, ResourceSet resourceSet)	{
		if (dialogParent instanceof Dialog)
			return new AwtCustomizerGUI((Dialog)dialogParent, resourceSet, this);
		else
			return new AwtCustomizerGUI((Frame)dialogParent, resourceSet, this);
	}
	
	/** Calls an AWT dialog to choose a GUI Component to customize. To be overridden by Swing implementation. */
	protected void showChoiceDialog(Window dialogParent)	{
		if (dialogParent instanceof Dialog)
			new AwtComponentChoiceGUI((Dialog)dialogParent, this, createResourceFactory());
		else
			new AwtComponentChoiceGUI((Frame)dialogParent, this, createResourceFactory());
	}
	

	/** Returns a ResourceSet for a hierarchical name. Needed in ComponentChoice/MultiTextLanguageEditor. */
	public ResourceSet getResourceSetForHierarchicalName(String hierarchicalName)	{
		return (ResourceSet) resourceFile.get(hierarchicalName);
	}

	/** Stores a collection of ResourceSets (value) for hierarchical names (key). Needed in ComponentChoice/MultiTextLanguageEditor. */
	public void storeResourceSets(Map resourceSets)	{
		for (Iterator it = resourceSets.entrySet().iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry) it.next();
			String hierarchicalName = (String) entry.getKey();
			ResourceSet resourceSet = (ResourceSet) entry.getValue();
			resourceFile.put(hierarchicalName, resourceSet);
		}
		
		if (resourceSets.size() > 0)	{
			resourceFile.save();
			refreshAll();	// sets both component-bound and window-bound resources
		}
	}
	
	private Window getWindow()	{
		return window;
	}
	
	private void refreshAll()	{	// loop all instances for refresh
		for (int i = 0; i < instances.size(); i++)	{
			ResourceManager rm = (ResourceManager) instances.get(i);
			rm.refresh();
		}
	}

	private void refresh()	{	// try to connect all components to the resourceFile's ResourceSets
		for (Iterator it = componentMap.entrySet().iterator(); it.hasNext(); )	{	// componentMap loop
			Map.Entry entry = (Map.Entry) it.next();
			ResourceSet resourceSet = getResourceSetForHierarchicalName((String) entry.getValue());
			
			if (resourceSet != null)
				resourceSet.addComponentForceResourceSet(entry.getKey());
		}
	}
	

	private void matchLostResources()	{
		// loop the resourceFile for lost resources
		Map toAdd = new Hashtable();

		for (Iterator it = resourceFile.entrySet().iterator(); it.hasNext(); )	{	// resourceFile loop
			Map.Entry entry = (Map.Entry) it.next();
			String hierarchicalName = (String) entry.getKey();
			ResourceSet resourceSet = (ResourceSet) entry.getValue();

			if (resourceSet.hasNoComponents())	{	// is a lost resource
				String componentName = HierarchicalName.componentNameFromHierarchicalName(hierarchicalName);	// make "button_Ok"
				if (componentName == null || componentName.indexOf(HierarchicalName.COMPONENT_TAG_SEPARATOR) < 0)
					continue;	// no chance to match "panel"
				
				//System.err.println("Checking unused resource: "+hierarchicalName);
				boolean found = false;	// search within componentMap
				
				for (Iterator it2 = componentMap.entrySet().iterator(); componentName != null && found == false && it2.hasNext(); )	{	// componentMap loop
					Map.Entry entry2 = (Map.Entry) it2.next();
					String hierarchicalName2 = (String) entry2.getValue();
					String componentName2 = HierarchicalName.componentNameFromHierarchicalName(hierarchicalName2);
					//System.err.println("   ... comparing "+hierarchicalName2+" and "+componentName2+" ...");
					
					if (componentName2 != null && componentName2.equals(componentName))	{
						System.err.println("... matched unused resource \""+hierarchicalName+"\" to \""+hierarchicalName2+"\"");
						toAdd.put(hierarchicalName2, resourceSet);
						Object c = entry2.getKey();
						resourceSet.addComponent(c);

						found = true;
						it.remove();	// remove resource-set from resourceFile
					}
				}	// componentMap loop end
			}	// end if has no components
		}	// resourceFile loop end
		
		if (toAdd.size() > 0)	{
			resourceFile.putAll(toAdd);
			resourceFile.save();
		}
	}
	

	/** Implements ContainerListener to customize dynamically added components. */
	public void componentAdded(ContainerEvent e)	{
		System.err.println("Dynamical adding component: "+e.getChild().getClass());
		dynamicComponentAdded(e.getChild());
	}
	
	private void dynamicComponentAdded(Object c)	{
		scanMap = new Hashtable();	// treat dynamically added components by their "type" and simulate same counts on every instance
		addComponent(c);
	}
	
	/** Implements ContainerListener to deregister from dynamical removed components. */
	public void componentRemoved(final ContainerEvent e)	{
		System.err.println("Dynamically removing component: "+e.getChild().getClass());
		visitContainerTopDown(e.getChild(), new ContainerVisitor()	{
			public Object visit(Object c, Object userObject)	{
				dynamicComponentRemoved(c, e.getContainer());
				return userObject;
			}
		});
	}
	
	/** Called when a component was removed (ContainerEvent). To be overridden by Swing implementation. */
	protected void dynamicComponentRemoved(Object c, Container parent)	{
		deregisterFromComponent(c);
		
		String hierarchicalName = (String) componentMap.get(c);
		if (hierarchicalName != null)	{
			ResourceSet resourceSet = (ResourceSet) resourceFile.get(hierarchicalName);
			if (resourceSet != null)
				resourceSet.removeComponent(c);
		}
		
		componentMap.remove(c);
		containerEventSources.remove(c);
	}



	/** Interface WindowListener: deregister from resourceFile, remove all listeners. */
	public void windowClosing(WindowEvent e)	{
		if (componentMap == null)
			return;	// already closed
		
		resourceFile.closeWindow(getWindow());
		getWindow().removeWindowListener(this);
		instances.remove(this);
		System.err.println("Window Close, ResourceManager instances are now: "+instances.size());
		
		for (Iterator it = componentMap.keySet().iterator(); it.hasNext(); )	{	// remove listeners from all Components
			deregisterFromComponent(it.next());
			it.remove();
		}
		for (Iterator it = containerEventSources.keySet().iterator(); it.hasNext(); )	{
			deregisterFromComponent(it.next());
			it.remove();
		}
		
		containerEventSources = componentMap = null;
		resourceFile = null;
		window = null;
	}

	private void deregisterFromComponent(Object c)	{
		if (c instanceof Component)
			((Component)c).removeKeyListener(this);
		if (c instanceof Container)
			((Container)c).removeContainerListener(this);
	}

	
	/** On some systems the closing callback does not arrive when dispose() was called, so deregister the ResourceManager her. */
	public void windowClosed(WindowEvent e)	{
		windowClosing(null);
	}
	
	public void windowOpened(WindowEvent e)	{}
	public void windowActivated(WindowEvent e)	{}
	public void windowDeactivated(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)	{}
	public void windowDeiconified(WindowEvent e)	{}


	/** ResourceManager Hello World Frame. */
	public static void main(String [] args)	{
		Frame f = new Frame();
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Hello World"));
		p.add(new Button("Answer Hello"), BorderLayout.SOUTH);
		f.add(p);
		new ResourceManager(f);
		f.pack();
		f.setVisible(true);
	}

}
