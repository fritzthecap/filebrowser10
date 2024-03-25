package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JDialog;
import fri.util.i18n.*;
import fri.gui.awt.resourcemanager.ResourceManager;
import fri.gui.awt.resourcemanager.dialog.AwtCustomizerGUI;
import fri.gui.awt.resourcemanager.resourceset.ResourceSet;
import fri.gui.awt.resourcemanager.resourceset.resource.Resource;
import fri.gui.swing.resourcemanager.resourceset.resource.JResourceFactory;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.*;

public class JCustomizerGUI extends AwtCustomizerGUI
{
	private JButton ok, reset, test, cancel, further;
	private JTabbedPane cardContainer;
	private static String recentSelected;
	
	public JCustomizerGUI(Dialog d, ResourceSet resourceSet, ResourceManager resourceManager)	{
		super(resourceSet, resourceManager);
		parentDialog = d;
		init(new TypedModalDialog(d, "Customizer"));	// modal dialog
	}
	
	public JCustomizerGUI(Frame f, ResourceSet resourceSet, ResourceManager resourceManager)	{
		super(resourceSet, resourceManager);
		parentFrame = f;
		init(new TypedModalDialog(f, "Customizer"));	// modal dialog
	}

	protected void addToDialog(Dialog dialog, Component c, String layoutConstraint)	{
		((JDialog)dialog).getContentPane().add(c, layoutConstraint);
	}
	
	protected Component buildButtonPanel()	{
		JPanel p = new JPanel();
		p.add(ok = new JButton("Ok"));
		ok.addActionListener(this);
		p.add(reset = new JButton("Reset"));
		reset.addActionListener(this);
		reset.setEnabled(getResourceSet().hasNoValues() == false);
		p.add(test = new JButton("Test"));
		test.addActionListener(this);
		p.add(further = new JButton("Others"));
		further.addActionListener(this);
		p.add(cancel = new JButton("Cancel"));
		cancel.addActionListener(this);
		return p;
	}

	protected void dispose()	{
		String title = cardContainer.getTitleAt(cardContainer.getSelectedIndex());
		if (title.equals("Font") == false)	// "Font" is default first tab
			recentSelected = cardContainer.getTitleAt(cardContainer.getSelectedIndex());
		super.dispose();
	}

	protected boolean isRestrictedType(String type)	{
		if (super.isRestrictedType(type))
			return true;
		return type.equals(JResourceFactory.TOOLTIP) || type.equals(JResourceFactory.ACCELERATOR) || type.equals(JResourceFactory.MNEMONIC);
	}
		
	/* Allocate and arrange all needed choosers. */
	protected Component buildAndArrangeResourceChoosers(String componentTypeName)	{
		JColorChooser backgroundChooser = null, foregroundChooser = null;
		JFontChooser fontChooser = null;
		JTextChooser textChooser = null;
		JLanguageChooser languageChooser = null;
		JAcceleratorChooser acceleratorChooser = null;
		JIconChooser iconChooser = null;
		JBorderChooser borderChooser = null;
		JTextChooser tooltipChooser = null;
		JIntegerChooser rowHeightChooser = null, tabSizeChooser = null;
		JBooleanChooser lineWrapChooser = null;
		JMnemonicChooser mnemonicChooser = null;

		String [] types = getResourceTypes();

		for (int i = 0; i < types.length; i++)	{
			Resource resource = getResourceSet().getResourceByType(types[i]);
			Object value = resource.getVisibleValue();

			if (types[i].equals(JResourceFactory.FONT))	{
				addResourceChooser(fontChooser = new JFontChooser((Font) value, resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(JResourceFactory.BACKGROUND))	{
				addResourceChooser(backgroundChooser = new JColorChooser((Color) value, JResourceFactory.BACKGROUND, resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(JResourceFactory.FOREGROUND))	{
				addResourceChooser(foregroundChooser = new JColorChooser((Color) value, JResourceFactory.FOREGROUND, resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(JResourceFactory.ACCELERATOR))	{
				addResourceChooser(acceleratorChooser = new JAcceleratorChooser((AcceleratorConverter.KeyAndModifier) value));
			}
			else
			if (types[i].equals(JResourceFactory.MNEMONIC))	{
				Object mls = getResourceSet().getResourceByType(JResourceFactory.TEXT).getVisibleValue();
				String menuLabel = mls != null ? mls.toString() : null;
				if (menuLabel != null && menuLabel.length() > 0)	// icon buttons have no text, so do not provide mnemonic
					addResourceChooser(mnemonicChooser = new JMnemonicChooser((Integer) value, menuLabel));
			}
			else
			if (types[i].equals(JResourceFactory.ICON))	{
				addResourceChooser(iconChooser = new JIconChooser((IconConverter.IconAndUrl) value));
			}
			else
			if (types[i].equals(JResourceFactory.BORDER))	{
				addResourceChooser(borderChooser = new JBorderChooser((BorderConverter.BorderAndTitle) value, resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(JResourceFactory.ROWHEIGHT))	{
				addResourceChooser(rowHeightChooser = new JIntegerChooser((Integer) value, "Row Height: ", 4, 100, JResourceFactory.ROWHEIGHT, resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(JResourceFactory.TABSIZE))	{
				addResourceChooser(tabSizeChooser = new JIntegerChooser((Integer) value, "Tab Size: ", 1, 16, JResourceFactory.TABSIZE, resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(JResourceFactory.LINEWRAP))	{
				addResourceChooser(lineWrapChooser = new JBooleanChooser((Boolean) value, "Line Wrap", JResourceFactory.LINEWRAP, resource.isComponentTypeBound(), componentTypeName));
			}
			else
			if (types[i].equals(JResourceFactory.TEXT))	{
				addResourceChooser(textChooser = new JTextChooser((MultiLanguageString) value, JResourceFactory.TEXT));
			}
			else
			if (types[i].equals(JResourceFactory.TOOLTIP))	{
				addResourceChooser(tooltipChooser = new JTextChooser((MultiLanguageString) value, JResourceFactory.TOOLTIP));
			}
		}
		
		addResourceChooser(languageChooser = new JLanguageChooser(showRestricted));

		// build a tabbed pane
		cardContainer = new JTabbedPane();
		
		if (fontChooser != null)
			cardContainer.addTab("Font", fontChooser.getPanel());

		if (backgroundChooser != null)
			cardContainer.addTab("Background", backgroundChooser.getPanel());

		if (foregroundChooser != null)
			cardContainer.addTab("Foreground", foregroundChooser.getPanel());

		if (textChooser != null)
			cardContainer.addTab("Text", textChooser.getPanel());
   
		if (tooltipChooser != null)
			cardContainer.addTab("Tooltip", tooltipChooser.getPanel());

		if (languageChooser != null)
			cardContainer.addTab("Language", languageChooser.getPanel());
   
		if (acceleratorChooser != null)
			cardContainer.addTab("Accelerator", acceleratorChooser.getPanel());

		if (mnemonicChooser != null)
			cardContainer.addTab("Mnemonic", mnemonicChooser.getPanel());

		if (iconChooser != null)
			cardContainer.addTab("Icon", iconChooser.getPanel());

		if (borderChooser != null)
			cardContainer.addTab("Border", borderChooser.getPanel());

		if (rowHeightChooser != null)
			cardContainer.addTab("Row Height", rowHeightChooser.getPanel());

		if (tabSizeChooser != null)
			cardContainer.addTab("Tab Size", tabSizeChooser.getPanel());

		if (lineWrapChooser != null)
			cardContainer.addTab("Line Wrap", lineWrapChooser.getPanel());
		
		if (recentSelected != null)	{
			for (int i = 0; i < cardContainer.getTabCount(); i++)	{
				String t = cardContainer.getTitleAt(i);
				if (t.equals(recentSelected))
					cardContainer.setSelectedIndex(i);
			}
		}
			
		return cardContainer;
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


	protected boolean ensureReset()	{
		int ret = JOptionPane.showConfirmDialog(dialog, "CAUTION: Settings get lost!", "Confirm Reset", JOptionPane.OK_CANCEL_OPTION);
		return ret == JOptionPane.OK_OPTION;
	}



	private static class TypedModalDialog extends JDialog
	{
		TypedModalDialog(Frame parent, String title)	{
			super(parent, title, true);
		}
	
		TypedModalDialog(Dialog parent, String title)	{
			super(parent, title, true);
		}
	}

}
