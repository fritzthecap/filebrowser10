package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import fri.util.i18n.MultiLanguageString;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.swing.resourcemanager.resourceset.resource.JResourceFactory;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.BorderConverter;

public class JBorderChooser extends JResourceChooser implements
	ActionListener,
	ListSelectionListener,
	NumberEditorListener
{
	public BorderConverter.BorderAndTitle border;
	private JList borderChooser;
	public JButton titleChooser;
	private JButton colorChooser;
	private SpinNumberField thickChooser;
	private JLabel borderViewer;
	private JLabel thickLabel;	// global for enable/disable
	private JPanel panel;
	private int thickness = 1;
	private Color color = Color.black;


	public JBorderChooser(BorderConverter.BorderAndTitle border, boolean isComponentTypeBound, String componentTypeName)	{
		super(isComponentTypeBound, componentTypeName);
		this.border = border;
		build();
		init();
		listen();
	}
	
	private void build()	{
		borderViewer = new JLabel(" ", JLabel.CENTER);
		borderChooser = new JList(new DefaultListModel());
		borderChooser.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		colorChooser = new JButton("Color");
		titleChooser = new JButton("Title Text");
		thickChooser = new SpinNumberField(1, 20);
		
		JPanel viewerPanel = new JPanel(new BorderLayout());
		viewerPanel.add(borderViewer, BorderLayout.CENTER);
		viewerPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

		JPanel optionPanel = new JPanel();
		optionPanel.add(colorChooser);
		optionPanel.add(thickLabel = new JLabel("Thickness"));
		optionPanel.add(thickChooser);
		optionPanel.add(titleChooser);

		panel = new JPanel(new BorderLayout());
		panel.add(viewerPanel, BorderLayout.CENTER);
		panel.add(new JScrollPane(borderChooser), BorderLayout.WEST);
		panel.add(optionPanel, BorderLayout.SOUTH);
	}

	private void init()	{
		for (int i = 0; i < BorderConverter.borderNames.length; i++)
			((DefaultListModel)borderChooser.getModel()).addElement(BorderConverter.borderNames[i]);

		Border b = border == null ? null : border.border;
		if (b instanceof LineBorder)	{
			LineBorder lb = (LineBorder)b;
			thickness = lb.getThickness();
			color = lb.getLineColor();
		}
		
		String bn = new BorderConverter().getChoosableBorderName(border);
		borderChooser.setSelectedValue(bn, true);
		thickChooser.setValue(thickness);

		setNewBorder();
	}
	
	private void listen()	{
		borderChooser.addListSelectionListener(this);
		colorChooser.addActionListener(this);
		titleChooser.addActionListener(this);
		thickChooser.getNumberEditor().addNumberEditorListener(this);
	}


	public Object getValue()	{
		return border;
	}
	
	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the type passed in constructor. */
	public String getResourceTypeName()	{
		return JResourceFactory.BORDER;
	}
	
	
	private void setNewBorder()	{
		String selectedBorder = (String) borderChooser.getSelectedValue();

		boolean isLineBorder = selectedBorder.equals("Line");
		boolean isNoBorder = selectedBorder.equals("(None)");
		thickChooser.setEnabled(isLineBorder);
		thickLabel.setEnabled(isLineBorder);
		colorChooser.setEnabled(isLineBorder);
		titleChooser.setEnabled(! isNoBorder);

		if (isNoBorder)	{
			borderViewer.setBorder(null);
			this.border = null;
		}
		else	{
			Object title = getTitle();
			Border b = new BorderConverter().stringToBorder(selectedBorder, thickness, color, title);
			borderViewer.setBorder(b);
			this.border = b == null ? null : new BorderConverter.BorderAndTitle(b, getTitle());
		}
	}
	
	private MultiLanguageString getTitle()	{
		return border == null ? null : border.title == null ? null : border.title.isEmpty() ? null : border.title;
	}


	/** interface ListSelectionListener: render the chosen border. */
	public void valueChanged(ListSelectionEvent e)	{
		if (e.getValueIsAdjusting())
			return;
		setNewBorder();
	}
	
	/** interface ActionListener: choose a color or a title text. */
	public void actionPerformed(ActionEvent e)	{
		((Component)e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		if (e.getSource() == colorChooser)	{
			Color c = javax.swing.JColorChooser.showDialog(getPanel(), "Border Color", color);
			if (c != null)
				color = c;
		}
		else
		if (e.getSource() == titleChooser)	{
			JTextChooser textChooser = new JTextChooser(getTitle(), "Title");
			JOptionPane pane = new JOptionPane(textChooser.getPanel(), JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
			JDialog dlg = pane.createDialog(getPanel(), "Border Title");
			dlg.setResizable(true);
			dlg.setVisible(true);
			
			Object o = pane.getValue();
			if (o != null)	{	// ok pressed
				MultiLanguageString mls = (MultiLanguageString) textChooser.getValue();
				border = new BorderConverter.BorderAndTitle(border.border, mls);
			}
		}

		setNewBorder();
		
		((Component)e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	/** Interface NumberChangedListener: thickness of line border changed. */
	public void numericValueChanged(long newValue)	{
		thickness = (int)newValue;
		setNewBorder();
	}


	// test main
	public static final void main(String [] args)	{
		JFrame f = new JFrame("BorderChooser");
		JBorderChooser fc = new JBorderChooser(null, false, "button");
		f.getContentPane().add(fc.getPanel());
		f.pack();
		f.setVisible(true);
	}
	
}