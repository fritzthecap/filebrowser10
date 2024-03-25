package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.awt.resourcemanager.ResourceIgnoringContainer;
import fri.gui.awt.resourcemanager.dialog.FontChooser;

public class JFontChooser extends FontChooser implements
	ListSelectionListener,
	NumberEditorListener
{
	private JCheckBox checkbox;
	private JCheckBox cbBold, cbItalic;
	private JList familyChooser;
	private SpinNumberField sizeChooser;
	private JTextField testView;
	private JPanel panel;

	public JFontChooser(Font font, boolean isComponentTypeBound, String componentTypeName)	{
		super(font, isComponentTypeBound, componentTypeName);
	}

	protected void build()	{
		testView = new ResourceIgnoringTextField(8);
		testView.setHorizontalAlignment(JTextField.CENTER);
		testView.setBorder(BorderFactory.createTitledBorder("View"));

		familyChooser = new JList(new DefaultListModel());
		familyChooser.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane familyPane = new JScrollPane(familyChooser);
		familyPane.setBorder(BorderFactory.createTitledBorder("Family"));

		JPanel pSize = new JPanel();
		sizeChooser = new SpinNumberField ((long)size, 4L, 80L);
		pSize.setBorder(BorderFactory.createTitledBorder("Size"));
		pSize.add(sizeChooser);

		JPanel pStyle = new JPanel();
		pStyle.setBorder(BorderFactory.createTitledBorder("Style"));
		pStyle.setLayout(new BoxLayout(pStyle, BoxLayout.Y_AXIS));
		pStyle.add(cbBold = new JCheckBox("Bold"));
		pStyle.add(cbItalic = new JCheckBox("Italic"));

		JPanel pSettings = new JPanel(new BorderLayout());
		pSettings.add(pSize, BorderLayout.NORTH);
		pSettings.add(pStyle, BorderLayout.CENTER);

		JPanel pCenter = new JPanel(new BorderLayout());
		pCenter.add(pSettings, BorderLayout.NORTH);
		pCenter.add(testView, BorderLayout.CENTER);

		panel = new JPanel(new BorderLayout());
		panel.add(pCenter, BorderLayout.CENTER);
		panel.add(familyPane, BorderLayout.WEST);
	}

	protected void renderFont(Font font)	{
		testView.setFont(font);
		testView.setText("Test");
		for (int i = 0; i < familyChooser.getModel().getSize(); i++)	{
			if (familyChooser.getModel().getElementAt(i).equals(font.getName()))	{
				familyChooser.setSelectedIndex(i);
				final int idx = i;
				SwingUtilities.invokeLater(new Runnable()	{
					public void run()	{
						familyChooser.ensureIndexIsVisible(idx);
					}
				});
				break;
			}
		}
		sizeChooser.setValue(font.getSize());
		cbBold.setSelected(font.isBold());
		cbItalic.setSelected(font.isItalic());
	}
	
	protected void listen()	{
		cbBold.addActionListener(this);
		cbItalic.addActionListener(this);
		familyChooser.addListSelectionListener(this);
		sizeChooser.addActionListener(this);
		sizeChooser.getNumberEditor().addNumberEditorListener(this);
	}

	protected void addFamilyItem(String familyName)	{
		((DefaultListModel)familyChooser.getModel()).addElement(familyName);
	}


	/** Returns true if this Resource should apply to all instances of Button, Label, ... */
	public boolean isComponentTypeBound()	{
		return checkbox.isSelected();
	}

	/** Returns the addable panel of this resource chooser. */
	public Container getPanel()	{
		if (checkbox == null)	{
			JPanel p = new JPanel(new BorderLayout());
			p.add(panel, BorderLayout.CENTER);
			checkbox = new JCheckBox(getCheckbox().getLabel(), getCheckbox().getState());
			JPanel p1 = new JPanel();
			p1.add(checkbox);
			p.add(p1, BorderLayout.SOUTH);
			panel = p;
		}
		return panel;
	}


	
	protected void showTestFont(Font font)	{
		testView.setFont(font);
	}


	/** Interface ListSelectionListener: GUI setting has changed, set new font. */
	public void valueChanged(ListSelectionEvent e)	{
		settingChanged();
	}

	/** Interface ActionListener: GUI setting has changed, set new font. */
	public void actionPerformed(ActionEvent e)	{
		settingChanged();
	}

	/** Interface NumberEditorListener: GUI setting has changed, set new font. */
	public void numericValueChanged(long newValue)	{
		settingChanged();
	}

	protected String getSelectedFamily()	{
		return (String) familyChooser.getSelectedValue();
	}
	
	protected int getSelectedSize()	{
		return (int) sizeChooser.getValue();
	}
	
	protected boolean isBoldActive()	{
		return cbBold.isSelected();
	}
	
	protected boolean isItalicActive()	{
		return cbItalic.isSelected();
	}


	private static class ResourceIgnoringTextField extends JTextField implements
		ResourceIgnoringContainer
	{
		ResourceIgnoringTextField(int width)	{
			super(width);
		}
	}


	// test main
	public static final void main(String [] args)	{
		JFrame f = new JFrame("FontChooser");
		JFontChooser fc = new JFontChooser(new Font("Dialog", Font.PLAIN, 12), false, "button");
		f.getContentPane().add(fc.getPanel());
		f.pack();
		f.show();
	}
	
}
