package fri.gui.awt.resourcemanager.dialog;

import java.util.ArrayList;
import java.awt.*;
import java.awt.event.*;
import fri.util.i18n.*;
import fri.gui.awt.dialog.OkCancelDialog;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.awt.resourcemanager.ResourceManager;
import fri.gui.awt.resourcemanager.resourceset.ResourceSet;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.ShortcutConverter;

public class AwtCustomizerGUI extends CustomizerGUI implements
	ActionListener
{
	protected Frame parentFrame;
	protected Dialog parentDialog;
	protected Dialog dialog; 
	private Button ok, reset, test, cancel, further;
	
	protected AwtCustomizerGUI(ResourceSet resourceSet, ResourceManager resourceManager)	{
		super(resourceSet, resourceManager);
	}
	
	public AwtCustomizerGUI(Dialog d, ResourceSet resourceSet, ResourceManager resourceManager)	{
		super(resourceSet, resourceManager);
		parentDialog = d;
		init(new TypedModalDialog(d, "Customizer"));	// modal dialog
	}
	
	public AwtCustomizerGUI(Frame f, ResourceSet resourceSet, ResourceManager resourceManager)	{
		super(resourceSet, resourceManager);
		parentFrame = f;
		init(new TypedModalDialog(f, "Customizer"));	// modal dialog
	}

	protected void init(Dialog dialog)	{
		this.dialog = dialog;

		String componentTypeName = getResourceSet().toString();
		dialog.setTitle("Component type \""+componentTypeName+"\"");
		addToDialog(dialog, buildButtonPanel(), BorderLayout.SOUTH);
		addToDialog(dialog, buildAndArrangeResourceChoosers(componentTypeName), BorderLayout.CENTER);

		dialog.addWindowListener (new WindowAdapter () {
			public void windowClosing(WindowEvent e) {
				cancel();
				dispose();
			}
		});

		new GeometryManager(dialog).show();
	}

	protected void addToDialog(Dialog dialog, Component c, String layoutConstraint)	{
		dialog.add(c, layoutConstraint);
	}
	
	protected void dispose()	{
		for (int i = 0; i < chooserList.size(); i++)	{	// deregister all TextChoosers from language listening
			Object o = chooserList.get(i);
			if (o instanceof MultiLanguage.ChangeListener)
				MultiLanguage.removeChangeListener((MultiLanguage.ChangeListener) o);
		}
		
		// JDC bug 4289940 when disposing dialogs?
		dialog.setVisible(false);
		try	{ dialog.dispose(); }	catch (Exception ex)	{ System.err.println("AwtCustomizerGUI caught exception on dispose: "+ex.getMessage()); }
	}


	protected Component buildButtonPanel()	{
		Panel p = new Panel();
		p.add(ok = new Button("Ok"));
		ok.addActionListener(this);
		p.add(reset = new Button("Reset"));
		reset.addActionListener(this);
		reset.setEnabled(getResourceSet().hasNoValues() == false);
		p.add(test = new Button("Test"));
		test.addActionListener(this);
		p.add(further = new Button("Others"));
		further.addActionListener(this);
		p.add(cancel = new Button("Cancel"));
		cancel.addActionListener(this);
		return p;
	}
	
	/* Allocate and arrange all needed choosers. */
	protected Component buildAndArrangeResourceChoosers(String componentTypeName)	{
		ColorChooser backgroundChooser = null, foregroundChooser = null;
		FontChooser fontChooser = null;
		TextChooser textChooser = null;
		LanguageChooser languageChooser = null;
		ShortcutChooser shortcutChooser = null;

		String [] types = getResourceTypes();

		for (int i = 0; i < types.length; i++)	{
			Resource resource = getResourceSet().getResourceByType(types[i]);
			Object value = resource.getVisibleValue();

			if (types[i].equals(ResourceFactory.FONT))	{
				addResourceChooser(fontChooser = new FontChooser((Font) value, resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(ResourceFactory.BACKGROUND))	{
				addResourceChooser(backgroundChooser = new ColorChooser((Color) value, "Background", resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(ResourceFactory.FOREGROUND))	{
				addResourceChooser(foregroundChooser = new ColorChooser((Color) value, "Foreground", resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(ResourceFactory.TEXT))	{
				addResourceChooser(textChooser = new TextChooser((MultiLanguageString) value));
			}
			else
			if (types[i].equals(ResourceFactory.SHORTCUT))	{
				addResourceChooser(shortcutChooser = new ShortcutChooser((ShortcutConverter.KeyAndModifier) value));
			}
		}
		
		addResourceChooser(languageChooser = new LanguageChooser(showRestricted));

		// build a tabbed pane
		final CardLayout cardLayout = new CardLayout();
		final Panel cardContainer = new Panel(cardLayout);
		Panel btnPanel = new Panel(new GridLayout(1, 0));
		final ArrayList btnList = new ArrayList();

		final ActionListener al = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				cardLayout.show(cardContainer, e.getActionCommand());
				for (int i = 0; i < btnList.size(); i++)	{
					Button b = (Button)btnList.get(i);
					setSelected(b, b == e.getSource());
				}
			}
		};
		
		Panel colorPanel = null;
		if (backgroundChooser != null || foregroundChooser != null)	{
			int anz = (backgroundChooser != null && foregroundChooser != null) ? 2 : 1;
			colorPanel = new Panel(new GridLayout(1, anz, anz > 1 ? 15 : 0, anz > 1 ? 15 : 0));
			if (backgroundChooser != null)
				colorPanel.add(backgroundChooser.getPanel());
			if (foregroundChooser != null)
				colorPanel.add(foregroundChooser.getPanel());
		}		
		
		if (fontChooser != null)
			addToCardContainer(cardContainer, fontChooser.getPanel(), btnPanel, "Font", btnList, al);

		if (colorPanel != null)
			addToCardContainer(cardContainer, colorPanel, btnPanel, "Color", btnList, al);

		if (textChooser != null)
			addToCardContainer(cardContainer, textChooser.getPanel(), btnPanel, "Text", btnList, al);
   
		if (languageChooser != null)
			addToCardContainer(cardContainer, languageChooser.getPanel(), btnPanel, "Language", btnList, al);
   
		if (shortcutChooser != null)
			addToCardContainer(cardContainer, shortcutChooser.getPanel(), btnPanel, "Shortcut", btnList, al);

		Panel p = new Panel(new BorderLayout());
		p.add(cardContainer, BorderLayout.CENTER);
		if (btnList.size() > 1)
			p.add(btnPanel, BorderLayout.NORTH);
		
		return p;
	}

	private void addToCardContainer(Panel cardContainer, Component panel, Panel btnPanel, String text, ArrayList btnList, ActionListener al)	{
		cardContainer.add(panel, text);
		Button b = new Button(text);
		b.setActionCommand(text);
		b.addActionListener(al);
		btnPanel.add(b);
		btnList.add(b);
		setSelected(b, btnPanel.getComponentCount() <= 1);
	}

	private void setSelected(Button b, boolean selected)	{
		b.setBackground(selected ? Color.lightGray : Color.gray);
		b.setForeground(selected ? Color.darkGray : Color.lightGray);
	}
	

	protected boolean isOkButton(ActionEvent e)	{
		return e.getSource() == ok;
	}
	protected boolean isResetButton(ActionEvent e)	{
		return e.getSource() == reset;
	}
	protected boolean isTestButton(ActionEvent e)	{
		return e.getSource() == test;
	}
	protected boolean isFurtherButton(ActionEvent e)	{
		return e.getSource() == further;
	}
	protected boolean isCancelButton(ActionEvent e)	{
		return e.getSource() == cancel;
	}

	// interface ActionListener

	public void actionPerformed(ActionEvent e) {
		if (isTestButton(e))	{
			test();
		}
		else
		if (isCancelButton(e))	{
			cancel();
			dispose();
		}
		else
		if (isFurtherButton(e))	{	// call ComponentChoice
			cancel();
			dispose();
			resourceManager.showDialogForAll(parentFrame != null ? (Window)parentFrame : (Window)parentDialog);
		}
		else
		if (isOkButton(e))	{
			set();
			dispose();
		}
		else
		if (isResetButton(e) && ensureReset())	{
			reset();
			dispose();
		}
	}

	protected boolean ensureReset()	{
		OkCancelDialog okCancel = new OkCancelDialog(dialog, "CAUTION: Settings get lost!");
		return okCancel.wasCanceled();
	}



	private static class TypedModalDialog extends Dialog
	{
		TypedModalDialog(Frame parent, String title)	{
			super(parent, title, true);
		}
	
		TypedModalDialog(Dialog parent, String title)	{
			super(parent, title, true);
		}
	}

}
