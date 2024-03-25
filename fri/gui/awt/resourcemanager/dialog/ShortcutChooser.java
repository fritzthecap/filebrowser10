package fri.gui.awt.resourcemanager.dialog;

import java.awt.*;
import java.awt.event.*;
import fri.gui.awt.keyboard.KeyNames;
import fri.gui.awt.resourcemanager.resourceset.resource.convert.ShortcutConverter;
import fri.gui.awt.resourcemanager.resourceset.resource.ResourceFactory;

public class ShortcutChooser extends AwtResourceChooser implements
	KeyListener,
	ActionListener,
	ItemListener
{
	protected ShortcutConverter.KeyAndModifier accelerator;
	private List keynames;
	private Checkbox cbShift;
	private Button reset;
	private Label keyLabel;
	private Panel panel;
	
	public ShortcutChooser(ShortcutConverter.KeyAndModifier accelerator)	{
		this.accelerator = accelerator;
		build();
		init();
		listen();
	}
	
	protected void build()	{
		keynames = new List();
		cbShift = new Checkbox("Shift");
		reset = new Button("Reset");
		
		panel = new Panel(new BorderLayout());
		panel.add(keynames, BorderLayout.CENTER);
		
		Panel pOptions = new Panel(new BorderLayout());
		pOptions.add(reset, BorderLayout.SOUTH);
		pOptions.add(cbShift, BorderLayout.CENTER);
		
		panel.add(pOptions, BorderLayout.EAST);
		panel.add(keyLabel = new Label("", Label.CENTER), BorderLayout.SOUTH);
	}

	private void init()	{
		loadKeyNames();
		if (accelerator != null)
			renderShortcut();
	}
	
	private void loadKeyNames()	{
		for (int i = 0; i < KeyNames.getInstance().getKeyNames().length; i++)
			addKeyName(KeyNames.getInstance().getKeyNames()[i]);
	}
	
	protected void addKeyName(String keyName)	{
		keynames.add(keyName);
	}
	
	private void renderShortcut()	{
		for (int i = 0; i < KeyNames.getInstance().getKeys().length; i++)	{
			if (KeyNames.getInstance().getKeys()[i] == accelerator.keyCode)	{
				selectKeyName(i);
				break;
			}
		}
	}
		
	protected void selectKeyName(int pos)	{
		keynames.select(pos);
		keynames.makeVisible(pos);
		keyLabel.setText("Ctrl+"+KeyNames.getInstance().getKeyName(accelerator.keyCode, accelerator.modifiers));
		cbShift.setState((accelerator.modifiers & InputEvent.SHIFT_MASK) != 0);
	}
	
	protected void listen()	{
		keynames.addKeyListener(this);
		keynames.addItemListener(this);
		cbShift.addKeyListener(this);
		cbShift.addItemListener(this);
		reset.addActionListener(this);
	}


	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns the currently chosen accelerator. */
	public Object getValue()	{
		return accelerator;
	}

	/** Implements ResourceChooser: Returns ResourceFactory.SHORTCUT. */
	public String getResourceTypeName()	{
		return ResourceFactory.SHORTCUT;
	}


	protected void setNewShortcut()	{
		String newKey = keynames.getSelectedItem();
		if (newKey != null)
			accelerator = new ShortcutConverter.KeyAndModifier(
					KeyNames.getInstance().getKeyCode(newKey),
					cbShift.getState() ? KeyEvent.SHIFT_MASK : 0);
		else
			accelerator = null;

		keyLabel.setText(accelerator == null ? "" : "Ctrl+"+KeyNames.getInstance().getKeyName(accelerator.keyCode, accelerator.modifiers));
	}

	
	/** Interface ActionListener: reset the accelerator. */
	public void actionPerformed(ActionEvent e)	{
		keynames.deselect(keynames.getSelectedIndex());
		cbShift.setState(false);
		setNewShortcut();
	}
	
	/** Interface ItemListener: key selection or shift selection changed. */
	public void itemStateChanged(ItemEvent e)	{
		setNewShortcut();
	}
	
	protected boolean isAllowedKey(int key)	{
		return key == KeyEvent.VK_SHIFT || key == KeyEvent.VK_ALT || key == KeyEvent.VK_CONTROL || key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN;
	}
	
	/** Interface KeyListener: render the pressed key like it was selected. */
	public void keyPressed(KeyEvent e)	{
		int key = e.getKeyCode();
		if (isAllowedKey(key))
			return;

		int i = KeyNames.getInstance().getKeyIndex(key);
		if (i >= 0)	{
			keynames.select(i);
			keynames.makeVisible(i);
		}
		cbShift.setState(e.isShiftDown());
		setNewShortcut();
	}
	public void keyTyped(KeyEvent e)	{}
	public void keyReleased(KeyEvent e)	{}
	
	
	// test main
	public static void main(String [] args)	{
		Frame f = new Frame("MenuShortcut");
		final ShortcutChooser sc = new ShortcutChooser(new ShortcutConverter.KeyAndModifier(KeyEvent.VK_DOWN, InputEvent.SHIFT_MASK));
		f.add(sc.getPanel());
		f.pack();
		f.setVisible(true);
	}
	
}
