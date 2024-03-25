package fri.gui.swing.xmleditor.view;

import java.util.Properties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.htmlbrowser.HttpProxyDialog;
import fri.gui.swing.propdialog.PropEditDialog;
import fri.gui.swing.xmleditor.model.Configuration;

/**
	Let edit Configuration of an editor window or of loading defaults for all.
*/

public class ConfigureDialog extends JPanel implements
	ActionListener
{
	private static boolean setAsDefault = false;
	private Configuration configuration, orig;
	private Frame parent;
	private String rootTag;
	private Properties tagMap;
	private boolean enableOnlyInsertable;
	private boolean canceled;
	private boolean tagMapChanged;
	private JCheckBox cbValidate;
	private JCheckBox cbExpandEntities;
	private JCheckBox cbMapTags;
	private JButton editTagMap;
	private JCheckBox cbComplexMode;
	private JCheckBox cbShowComments;
	private JCheckBox cbShowPIs;
	private JCheckBox cbShowProlog;
	private JCheckBox cbExpandAllOnOpen;
	private JCheckBox cbCreateAllTagsEmpty;
	private JCheckBox cbEnableOnlyInsertable;
	private JCheckBox cbSetAsDefault;
	private JButton httpProxySettings;


	public ConfigureDialog(
		Frame parent,
		Configuration configuration,
		boolean isDefault,
		String rootTag,
		boolean enableOnlyInsertable)
	{
		this.enableOnlyInsertable = enableOnlyInsertable;
		this.orig = configuration;
		this.configuration = (Configuration)configuration.clone();	// work on a copy
		this.rootTag = rootTag;
		this.parent = parent;

		build(isDefault);
		display();
	}


	private void build(boolean isDefault)	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		Dimension max = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);

		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder("Parser"));
		p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
		p1.setMaximumSize(max);
		p1.setAlignmentX(Component.LEFT_ALIGNMENT);

		cbValidate = new JCheckBox("Validate", configuration.validate);
		cbValidate.setToolTipText("Check Document For Validity When Loading");
		p1.add(cbValidate);
		cbValidate.addActionListener(this);

		cbExpandEntities = new JCheckBox("Expand Entities", configuration.expandEntities);
		cbExpandEntities.setToolTipText("Expand XML Entities In Text When Loading");
		p1.add(cbExpandEntities);
		cbExpandEntities.addActionListener(this);

		JPanel p2 = new JPanel();
		p2.setBorder(BorderFactory.createTitledBorder("View"));
		p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
		p2.setMaximumSize(max);
		p2.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel p21 = new JPanel(new BorderLayout());
		p21.setAlignmentX(Component.LEFT_ALIGNMENT);
		cbMapTags = new JCheckBox("Show Attribute Instead Of Tag",
				configuration.mapTags && (rootTag == null || ensureTagMap() != null));
		cbMapTags.setToolTipText("The Values Of Assigned Attributes Will Be Displayed Instead Of Tags");
		p21.add(cbMapTags, BorderLayout.CENTER);
		cbMapTags.addActionListener(this);
		if (rootTag != null)	{
			editTagMap = new JButton("Edit Map");
			editTagMap.setToolTipText("Assign Attributes Shown Instead Of Tags");
			editTagMap.addActionListener(this);
			p21.add(editTagMap, BorderLayout.EAST);
		}
		p2.add(p21);

		cbComplexMode = new JCheckBox("Complex Mode - Show Text In Sub-Nodes", configuration.complexMode);
		cbComplexMode.setToolTipText("Simple Mode: Show Element Text Within Element Node If Possible");
		p2.add(cbComplexMode);
		cbComplexMode.addActionListener(this);

		cbShowComments = new JCheckBox("Show Comments", configuration.showComments);
		cbShowComments.setToolTipText("Comment Nodes Will Be Invisible When False");
		p2.add(cbShowComments);
		cbShowComments.addActionListener(this);

		cbShowPIs = new JCheckBox("Show Processing Instructions", configuration.showPIs);
		cbShowPIs.setToolTipText("Processing Instruction Nodes Will Be Invisible When False");
		p2.add(cbShowPIs);
		cbShowPIs.addActionListener(this);

		cbShowProlog = new JCheckBox("Show XML Declaration", configuration.showProlog);
		cbShowProlog.setToolTipText("Root Node Holding The XML Prolog Will Be Invisible When False");
		p2.add(cbShowProlog);
		cbShowProlog.addActionListener(this);

		JPanel p3 = new JPanel();
		p3.setBorder(BorderFactory.createTitledBorder("Editor"));
		p3.setLayout(new BoxLayout(p3, BoxLayout.Y_AXIS));
		p3.setMaximumSize(max);
		p3.setAlignmentX(Component.LEFT_ALIGNMENT);

		cbExpandAllOnOpen = new JCheckBox("Expand All Nodes On Open", configuration.expandAllOnOpen);
		cbExpandAllOnOpen.setToolTipText("Expand All Tree Branches When Loading A Document");
		p3.add(cbExpandAllOnOpen);
		cbExpandAllOnOpen.addActionListener(this);

		cbCreateAllTagsEmpty = new JCheckBox("Create Empty Sub-Elements", configuration.createAllTagsEmpty);
		cbCreateAllTagsEmpty.setToolTipText("Create All Sub-Elements For An Empty Document Or A New Element");
		p3.add(cbCreateAllTagsEmpty);
		cbCreateAllTagsEmpty.addActionListener(this);

		cbEnableOnlyInsertable = new JCheckBox("Disable Not Insertable Elements", enableOnlyInsertable);
		cbEnableOnlyInsertable.setToolTipText("Only Insertable Elements Will Be Enabled In Menu");
		p3.add(cbEnableOnlyInsertable);
		cbEnableOnlyInsertable.addActionListener(this);

		add(p1);
		add(p2);
		add(p3);
		add(httpProxySettings = new JButton("HTTP Proxy Settings"));
		httpProxySettings.addActionListener(this);

		if (isDefault == false)	{
			JPanel p4 = new JPanel();
			p4.setMaximumSize(max);
			p4.setAlignmentX(Component.LEFT_ALIGNMENT);
			cbSetAsDefault = new JCheckBox("Set As Loading Defaults", setAsDefault);
			cbSetAsDefault.setToolTipText("Settings Should Be Default When Loading A Document");
			p4.add(cbSetAsDefault);

			add(p4);
		}
	}

	private JDialog getDialogParent()	{
		return (JDialog)ComponentUtil.getWindowForComponent(this);
	}


	private void display()	{
		int ret = JOptionPane.showConfirmDialog(
				parent,
				this,
				rootTag == null ? "Default Configuration" : "Configuration",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (ret != JOptionPane.OK_OPTION)
			canceled = true;
		else
		if (cbSetAsDefault == null || cbSetAsDefault.isSelected())
			Configuration.setDefault(configuration);
		
		if (cbSetAsDefault != null)
			setAsDefault = cbSetAsDefault.isSelected();
	}


	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == cbValidate)	{
			configuration.validate = cbValidate.isSelected();
		}
		else
		if (e.getSource() == cbExpandEntities)	{
			if (configuration.expandEntities = cbExpandEntities.isSelected())	{	// warn about lost reference
				JOptionPane.showMessageDialog(
					getDialogParent(),
					"CAUTION: If an entity reference gets expanded by parser,\n"+
						"it will be normal text. A subsequent change of the\n"+
						"referenced entity will then have no effect.\n\n"+
						"Do not save documents with expanded entity references!",
					"Warning",
					JOptionPane.WARNING_MESSAGE);
			}
		}
		else
		if (e.getSource() == cbComplexMode)	{
			configuration.complexMode = cbComplexMode.isSelected();
		}
		else
		if (e.getSource() == cbShowComments)	{
			configuration.showComments = cbShowComments.isSelected();
		}
		else
		if (e.getSource() == cbEnableOnlyInsertable)	{
			enableOnlyInsertable = cbEnableOnlyInsertable.isSelected();
		}
		else
		if (e.getSource() == cbShowPIs)	{
			configuration.showPIs = cbShowPIs.isSelected();
		}
		else
		if (e.getSource() == cbShowProlog)	{
			configuration.showProlog = cbShowProlog.isSelected();
		}
		else
		if (e.getSource() == cbExpandAllOnOpen)	{
			configuration.expandAllOnOpen = cbExpandAllOnOpen.isSelected();
		}
		else
		if (e.getSource() == cbCreateAllTagsEmpty)	{
			configuration.createAllTagsEmpty = cbCreateAllTagsEmpty.isSelected();
		}
		else
		if (e.getSource() == cbMapTags)	{
			configuration.mapTags = cbMapTags.isSelected();
			if (rootTag != null && configuration.mapTags && ensureTagMap() == null)
				editTagMap();
		}
		else
		if (editTagMap != null && e.getSource() == editTagMap)	{
			editTagMap();
		}
		else
		if (e.getSource() == httpProxySettings)	{
			new HttpProxyDialog((JFrame) parent);
		}
	}

	private boolean isChanged()	 {
		return
			tagMapChanged ||
			orig.validate != configuration.validate ||
			orig.expandEntities != configuration.expandEntities ||
			orig.complexMode != configuration.complexMode ||
			orig.showComments != configuration.showComments ||
			orig.showPIs != configuration.showPIs ||
			orig.showProlog != configuration.showProlog ||
			orig.expandAllOnOpen != configuration.expandAllOnOpen ||
			orig.createAllTagsEmpty != configuration.createAllTagsEmpty ||
			orig.mapTags != configuration.mapTags;
	}


	/** Returns a new Configuration instance with edited values. */
	public Configuration getConfiguration()	{
		if (canceled || isChanged() == false)
			return null;

		return configuration;
	}

	/** Returns true if only insertable elements should be enabled in menu. */
	public boolean getEnableOnlyInsertable()	{
		return enableOnlyInsertable;
	}


	private Properties ensureTagMap()	{
		if (tagMap == null)	{
			boolean isOn = configuration.mapTags;
			configuration.mapTags = true;
			tagMap = configuration.getTagMapForRootTag(rootTag);
			configuration.mapTags = isOn;
		}
		return tagMap;
	}

	private void editTagMap()	{
		ensureTagMap();

		// make a clone to see changes
		Properties clone = (tagMap == null) ? new Properties() : (Properties)tagMap.clone();

		// edit properties
		PropEditDialog dlg = new PropEditDialog(
				getDialogParent(),
				tagMap == null ? new Properties() : tagMap,
				"Mapping Of Tag-Substituting Attribute Names For \""+rootTag+"\"")
		{
			protected String getColumn1Name()	{
				return "Tag (Or * For All Tags)";
			}
			protected String getColumn2Name()	{
				return "Attribute";
			}
		};

		dlg.setVisible(true);

		// store edited properties
		Properties props = dlg.getProperties();
		tagMap = configuration.putTagMapForRootTag(rootTag, props);

		if (tagMapChanged == false)	// indicate changes
			tagMapChanged = (props.equals(clone) == false);
	}

}