package fri.gui.awt.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.awt.component.ComponentName;
import fri.gui.awt.resourcemanager.ResourceManager;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

/**
	Every Component/MenuComponent of the ResourceManager's Window is represented by a Button.
	Pressing that Button opens a customize dialog for the associated Component/MenuComponent.
*/

public class AwtComponentChoiceGUI
{
	protected Frame parentFrame;
	protected Dialog parentDialog;
	protected Dialog dialog;

	protected AwtComponentChoiceGUI()	{
	}
	
	public AwtComponentChoiceGUI(Dialog d, ResourceManager resourceManager, ResourceFactory resourceFactory)	{
		parentDialog = d;
		init(resourceManager, resourceFactory, new Dialog(d, "Component Choice", true));	// modal dialog
	}
	
	public AwtComponentChoiceGUI(Frame f, ResourceManager resourceManager, ResourceFactory resourceFactory)	{
		parentFrame = f;
		init(resourceManager, resourceFactory, new Dialog(f, "Component Choice", true));	// modal dialog
	}

	protected void init(ResourceManager resourceManager, ResourceFactory resourceFactory, Dialog dialog)	{
		this.dialog = dialog;
		
		addToDialog(dialog, buildGUI(dialog, resourceManager, resourceFactory));
		
		dialog.addWindowListener (new WindowAdapter () {
			public void windowClosing(WindowEvent e) {
				closeDialog();
			}
		});
		
		new GeometryManager(dialog).show();
	}

	protected void closeDialog()	{
		AwtComponentChoiceGUI.this.dialog.setVisible(false);
		try	{ AwtComponentChoiceGUI.this.dialog.dispose(); }	catch (Exception ex)	{ System.err.println("AwtComponentChoiceGUI caught exception on dispose: "+ex.getMessage()); }
		// JDC bug 4289940 ???
	}
	
	protected void addToDialog(Dialog dialog, Component c)	{
		dialog.add(c);
	}
	
	protected Component buildGUI(Dialog dialog, ResourceManager resourceManager, ResourceFactory resourceFactory)	{
		ScrollPane sp = new ScrollPane();
		sp.add(addComponentButtons(resourceManager, resourceFactory));
		return sp;
	}

	protected Component addComponentButtons(ResourceManager resourceManager, ResourceFactory resourceFactory)	{
		Map map = new TreeMap();	// sort component buttons by hierarchical name
		for (Iterator it = resourceManager.getComponentMap().entrySet().iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry) it.next();
			addComponentRepresentation(resourceManager, resourceFactory, entry.getKey(), map, (String) entry.getValue());	// key is Component
		}
		return addToScrollPaneContainer(map);	// add all button representations sorted
	}
	
	/** Builds the button panel within scrollpane. Passed map contains hierarchical name as key and button as value. */
	protected Component addToScrollPaneContainer(Map map)	{
		Container list = createScrollPaneContainer(map.size());
		for (Iterator it = map.entrySet().iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry) it.next();
			list.add((Component) entry.getValue());
		}
		return list;
	}

	protected Container createScrollPaneContainer(int size)	{
		return new Panel(new GridLayout(size, 1));
	}
	
	protected Component createComponentButton(String componentName, ActionListener al, String hierarchicalName)	{
		Button b = new Button(componentName);
		b.addActionListener(al);
		return b;
	}
	
	protected void addToButtonText(Component button, String s)	{
		Button b = (Button) button;
		b.setLabel(editButtonLabel(b.getLabel(), s));
	}
	
	protected String editButtonLabel(String componentName, String labelText)	{
		int i = componentName.lastIndexOf(" (\"");
		if (i > 0)	// remove old component text
			componentName = componentName.substring(0, i);
		return componentName+labelText;
	}
	
	private void addComponentRepresentation(final ResourceManager resourceManager, final ResourceFactory resourceFactory, final Object component, Map map, String hierarchicalName)	{
		ActionListener al = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				dialog.setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
				resourceManager.showDialogForOne(component, dialog);
				applyResourceSet(resourceFactory, component, (Component) e.getSource());
				dialog.setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));
			}
		};
		Component button = createComponentButton(createComponentTypeName(component), al, hierarchicalName);
		map.put(hierarchicalName, button);

		applyResourceSet(resourceFactory, component, button);
	}

	/** Returns a component-type name for passed component. */
	protected String createComponentTypeName(Object component)	{
		return new ComponentName(component).toString();
	}
		
	private void applyResourceSet(ResourceFactory resourceFactory, Object component, Component button)	{
		// put all properties of component to the button
		Resource [] resources = resourceFactory.createResources(component);
		
		for (int i = 0; i < resources.length; i++)	{
			Object value = resources[i].getVisibleValue();
			Object initial = resources[i].getInitialValue();
			
			if (value != null || initial != null && canRenderGuiValue(resources[i].getTypeName()))	{
				if (resources[i].getTypeName().equals(ResourceFactory.TEXT))	{
					String s = value != null ? value.toString() : initial.toString();
					if (s.length() > 30)
						s = s.substring(0, 30)+"...";

					if (s.length() > 0)
						addToButtonText(button, " (\""+s+"\")");
				}
				else
				if (canRenderResourceValue(resources[i].getTypeName()))	{
					resources[i].setToComponent(button, false);
				}
			}
		}
	}

	protected boolean canRenderResourceValue(String typeName)	{
		return true;
	}
	
	protected boolean canRenderGuiValue(String typeName)	{
		return false;
	}
	
}
