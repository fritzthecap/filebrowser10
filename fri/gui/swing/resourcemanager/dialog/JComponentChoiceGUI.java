package fri.gui.swing.resourcemanager.dialog;

import java.util.*;
import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import fri.gui.awt.resourcemanager.ResourceManager;
import fri.gui.awt.resourcemanager.dialog.CustomizerGUI;
import fri.gui.awt.resourcemanager.dialog.AwtComponentChoiceGUI;
import fri.gui.awt.resourcemanager.resourceset.ResourceSet;
import fri.gui.swing.resourcemanager.resourceset.JResourceSet;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;
import fri.gui.swing.resourcemanager.resourceset.resource.JResourceFactory;
import fri.gui.swing.resourcemanager.dialog.tree.JComponentChoiceTreeview;

/**
	Every Component/MenuComponent of the ResourceManager's Window is represented by a Button.
	Pressing that Button opens a customize dialog for the associated Component/MenuComponent.
*/

public class JComponentChoiceGUI extends AwtComponentChoiceGUI
{
	private ResourceManager resourceManager;
	private MultiLanguageTextEditor multiLanguageTextEditor;
	private Map textResourceSetMap;
	
	public JComponentChoiceGUI(Dialog d, ResourceManager resourceManager, ResourceFactory resourceFactory)	{
		parentDialog = d;
		init(resourceManager, resourceFactory, new JDialog(d, "Component Choice", true));
	}
	
	public JComponentChoiceGUI(Frame f, ResourceManager resourceManager, ResourceFactory resourceFactory)	{
		parentFrame = f;
		init(resourceManager, resourceFactory, new JDialog(f, "Component Choice", true));
	}

	protected void closeDialog()	{
		if (multiLanguageTextEditor != null)	{
			if (multiLanguageTextEditor.isChanged())
				resourceManager.storeResourceSets(textResourceSetMap);
			multiLanguageTextEditor.close();
		}
		super.closeDialog();
	}

	protected Component buildGUI(Dialog dialog, ResourceManager resourceManager, ResourceFactory resourceFactory)	{
		this.resourceManager = resourceManager;
		
		JScrollPane sp = new JScrollPane(addComponentButtons(resourceManager, resourceFactory));
		JPanel p = new JPanel(new BorderLayout());
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p1.add(new JLabel("Look And Feel: "));
		p1.add(new LAFComboBox());
		
		p.add(p1, BorderLayout.NORTH);
		p.add(sp, BorderLayout.CENTER);
		
		if (CustomizerGUI.showRestricted)
			return p;
			
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Components", p);
		tabbedPane.addTab("Text", buildMultiLanguageTextEditor(resourceManager, resourceFactory));
		return tabbedPane;
	}
		
	protected void addToDialog(Dialog dialog, Component c)	{
		((JDialog)dialog).getContentPane().add(c);
	}
	
	protected Container createScrollPaneContainer(int size)	{
		// return new JPanel(new GridLayout(size, 1));
		return new JComponentChoiceTreeview();
	}
	
	/** Builds the tree view within scrollpane. Passed map contains hierarchical name as key and button as value. */
	protected Component addToScrollPaneContainer(Map map)	{
		// Component list = createScrollPaneContainer(map.size());
		JComponentChoiceTreeview tree = (JComponentChoiceTreeview) createScrollPaneContainer(map.size());
		
		for (Iterator it = map.entrySet().iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry) it.next();
			// list.add((Component) entry.getValue());
			
			tree.addComponentTreeNode((String) entry.getKey(), (AbstractButton) entry.getValue());
		}
		
		tree.expandAllBranches();
		
		// return list;
		return tree;
	}

	protected Component createComponentButton(String componentName, ActionListener al, String hierarchicalName)	{
		JButton b = new JButton(componentName);
		b.addActionListener(al);
		b.setToolTipText(hierarchicalName);
		return b;
	}
	
	protected void addToButtonText(Component button, String s)	{
		JButton b = (JButton) button;
		b.setText(editButtonLabel(b.getText(), s));
	}

	/** Returns false when TOOLTIP is about to be rendered, as button representation has its own tooltip. */
	protected boolean canRenderResourceValue(String typeName)	{
		return typeName.equals(JResourceFactory.TOOLTIP) == false;
	}

	/** Returns false when BORDER is about to be rendered, as L&F borders can not be converted and would be null. */
	protected boolean canRenderGuiValue(String typeName)	{
		return typeName.equals(JResourceFactory.BORDER) == false;
	}


	private Component buildMultiLanguageTextEditor(ResourceManager resourceManager, ResourceFactory resourceFactory)	{
		this.textResourceSetMap = new Hashtable();
		MultiLanguageTextEditor.ResourceList textResourceList = new MultiLanguageTextEditor.ResourceList();
		
		// retrieve all Resources either from resourceFile or from Component
		for (Iterator it = resourceManager.getComponentMap().entrySet().iterator(); it.hasNext(); )	{
			Map.Entry entry = (Map.Entry) it.next();
			Object component = entry.getKey();
			String hierarchicalName = (String) entry.getValue();
			
			ResourceSet resourceSet = resourceManager.getResourceSetForHierarchicalName(hierarchicalName);
			Resource [] resources;
			if (resourceSet == null)	{	// no persistent Resource was found for this name
				resources = resourceFactory.createResources(component);
				resourceSet = new JResourceSet(component, resources);
			}
			else	{
				resources = new Resource[resourceSet.size()];
				resourceSet.toArray(resources);
			}
			
			for (int i = 0; i < resources.length; i++)	{
				Object value = resources[i].getVisibleValue();
				if (value != null)	{
					if (resources[i].getTypeName().equals(ResourceFactory.TEXT) || resources[i].getTypeName().equals(JResourceFactory.TOOLTIP))	{
						resourceSet.completeResources(new Resource [] { resources[i] });	// add only text resource (if not already contained)
						textResourceSetMap.put(hierarchicalName, resourceSet);
						textResourceList.add(
								createComponentTypeName(component),
								resources[i].getTypeName(),
								resources[i]);
					}
				}
			}
		}

		this.multiLanguageTextEditor = new MultiLanguageTextEditor(textResourceList);	// needed for change control
		return multiLanguageTextEditor.getPanel();
	}

}
