package fri.gui.swing.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import fri.gui.awt.keyboard.KeyNames;
import fri.gui.awt.resourcemanager.dialog.ShortcutChooser;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.AcceleratorConverter;
import fri.gui.swing.resourcemanager.resourceset.resource.JResourceFactory;

public class JAcceleratorChooser extends ShortcutChooser implements
	ListSelectionListener
{
	private JList keynames;
	private JCheckBox cbShift, cbAlt, cbCtrl;
	private JButton reset;
	private JLabel keyLabel;
	private JPanel panel;
	
	public JAcceleratorChooser(AcceleratorConverter.KeyAndModifier accelerator)	{
		super(accelerator);
	}
	
	protected void build()	{
		keynames = new JList(new DefaultListModel());
		keynames.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		cbShift = new JCheckBox("Shift");
		cbAlt = new JCheckBox("Alt");
		cbCtrl = new JCheckBox("Ctrl");

		reset = new JButton("Reset");

		panel = new JPanel(new BorderLayout());
		panel.add(new JScrollPane(keynames), BorderLayout.CENTER);
		
		JPanel p = new JPanel(new BorderLayout());
		p.add(reset, BorderLayout.SOUTH);
		
		JPanel cbp = new JPanel(new GridLayout(3, 1));
		cbp.add(cbShift);
		cbp.add(cbAlt);
		cbp.add(cbCtrl);
		p.add(cbp, BorderLayout.NORTH);
		
		panel.add(p, BorderLayout.EAST);
		panel.add(keyLabel = new JLabel("", JLabel.CENTER), BorderLayout.SOUTH);
	}

	protected void addKeyName(String keyName)	{
		((DefaultListModel)keynames.getModel()).addElement(keyName);
	}

	protected void selectKeyName(final int pos)	{
		keynames.setSelectedIndex(pos);
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				keynames.ensureIndexIsVisible(pos);
			}
		});
		keyLabel.setText(KeyNames.getInstance().getKeyName(accelerator.keyCode, accelerator.modifiers));
		cbShift.setSelected((accelerator.modifiers & InputEvent.SHIFT_MASK) != 0);
		cbAlt.setSelected((accelerator.modifiers & InputEvent.ALT_MASK) != 0);
		cbCtrl.setSelected((accelerator.modifiers & InputEvent.CTRL_MASK) != 0);
	}

	protected void listen()	{
		keynames.addKeyListener(this);
		keynames.addListSelectionListener(this);
		cbShift.addKeyListener(this);
		cbShift.addActionListener(this);
		cbAlt.addKeyListener(this);
		cbAlt.addActionListener(this);
		cbCtrl.addKeyListener(this);
		cbCtrl.addActionListener(this);
		reset.addActionListener(this);

	}


	public Container getPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns ResourceFactory.ACCELERATOR. */
	public String getResourceTypeName()	{
		return JResourceFactory.ACCELERATOR;
	}
	
	
	protected void setNewShortcut()	{
		String newKey = (String) keynames.getSelectedValue();
		if (newKey != null)	{
			int modifier = 0;
			if (cbShift.isSelected())
				modifier |= InputEvent.SHIFT_MASK;
			if (cbAlt.isSelected())
				modifier |= InputEvent.ALT_MASK;
			if (cbCtrl.isSelected())
				modifier |= InputEvent.CTRL_MASK;
				
			accelerator = new AcceleratorConverter.KeyAndModifier(
					KeyNames.getInstance().getKeyCode(newKey),
					modifier);
		}
		else	{
			accelerator = null;
		}
		
		keyLabel.setText(accelerator == null ? "" : KeyNames.getInstance().getKeyName(accelerator.keyCode, accelerator.modifiers));
	}

	
	/** Interface ActionListener: Reset the accelerator, or render recently selected. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == reset)	{
			keynames.clearSelection();
			cbShift.setSelected(false);
			cbAlt.setSelected(false);
			cbCtrl.setSelected(false);
		}
		setNewShortcut();
	}
	
	/** Interface ListSelectionListener: Listen to selection change in key names list. */
	public void valueChanged(ListSelectionEvent e)	{
		if (e.getValueIsAdjusting() == false)
			setNewShortcut();
	}
	
	/** Interface KeyListener: render every keypress in list and checkboxes. */
	public void keyPressed(KeyEvent e)	{
		int key = e.getKeyCode();
		if (isAllowedKey(key))
			return;
		
		int i = KeyNames.getInstance().getKeyIndex(key);
		if (i >= 0)	{
			keynames.setSelectedIndex(i);
			keynames.ensureIndexIsVisible(i);
		}

		cbShift.setSelected(e.isShiftDown());
		cbAlt.setSelected(e.isAltDown());
		cbCtrl.setSelected(e.isControlDown());
		setNewShortcut();
	}
	public void keyTyped(KeyEvent e)	{}
	public void keyReleased(KeyEvent e)	{}
	
	
	// test main
	public static void main(String [] args)	{
		JFrame f = new JFrame("AcceleratorChooser");
		final JAcceleratorChooser sc = new JAcceleratorChooser(new AcceleratorConverter.KeyAndModifier(KeyEvent.VK_UP, InputEvent.CTRL_MASK));
		Container c = f.getContentPane();
		c.add(sc.getPanel());
		f.pack();
		f.setVisible(true);
	}
	
}