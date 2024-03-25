package fri.gui.swing.resourcemanager.dialog;

import java.io.File;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import fri.gui.swing.resourcemanager.resourceset.resource.JResourceFactory;
import fri.gui.swing.resourcemanager.resourceset.resource.convert.IconConverter;

public class JIconChooser extends JResourceChooser implements
	ActionListener
{
	private static JFileChooser imageChooser;
	private IconConverter.IconAndUrl icon;
	private JButton iconButton;
	private JPanel panel;
	private JTextField urlTextField;
	private JButton noIcon;
	
	public JIconChooser(IconConverter.IconAndUrl icon)	{
		this.icon = icon;
		build();
		init();
		listen();
	}

	private void build()	{
		JPanel pOptions = new JPanel();
		pOptions.setLayout(new BoxLayout(pOptions, BoxLayout.X_AXIS));
		pOptions.add(new JLabel(" URL:  "));
		pOptions.add(urlTextField = new JTextField(20));
		pOptions.add(noIcon = new JButton("Remove Icon"));
		panel = new JPanel(new BorderLayout());
		panel.add(iconButton = new JButton());
		panel.add(pOptions, BorderLayout.SOUTH);
	}
	
	private void init()	{
		if (icon != null && icon.icon != null)	{
			iconButton.setText(null);
			iconButton.setIcon(icon.icon);
			urlTextField.setText(icon.url);
			noIcon.setEnabled(true);
			System.err.println("Selected image is: "+icon.url);
		}
		else	{
			iconButton.setIcon(null);
			iconButton.setText("Choose Icon File");
			noIcon.setEnabled(false);
		}
	}
	
	private void listen()	{
		iconButton.addActionListener(this);
		urlTextField.addActionListener(this);
		noIcon.addActionListener(this);
	}


	public Object getValue()	{
		return icon;
	}
	
	protected Component getChooserPanel()	{
		return panel;
	}

	/** Implements ResourceChooser: Returns JResourceFactory.ICON. */
	public String getResourceTypeName()	{
		return JResourceFactory.ICON;
	}
	
	
	private void setNewIcon()	{
		ImageIcon i = null;

		if (icon != null && icon.url != null)	{
			try	{
				URL url = new URL(icon.url);
				i = new ImageIcon(url);
			}
			catch (MalformedURLException e)	{
				i = new ImageIcon(icon.url);
			}
		}

		if (i != null && i.getImageLoadStatus() == MediaTracker.COMPLETE)
			icon = new IconConverter.IconAndUrl(i, icon.url);
		else
			icon = null;

		init();
	}


	/** Implements ActionListener: reset the icon, or launch image file chooser to choose one. */
	public void actionPerformed(ActionEvent e)	{
		((Component)e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		if (e.getSource() == iconButton)	{
			if (imageChooser == null)	{
				imageChooser = new JFileChooser();
				imageChooser.addChoosableFileFilter(new ImageFilter());
				imageChooser.setAcceptAllFileFilterUsed(false);
				imageChooser.setAccessory(new ImagePreview(imageChooser));
			}
	
			if (imageChooser.showDialog(getPanel(), "Choose Image") == JFileChooser.APPROVE_OPTION)	{
				icon = new IconConverter.IconAndUrl(null, imageChooser.getSelectedFile().getPath());
				setNewIcon();
			}
			imageChooser.setSelectedFile(null);
		}
		else
		if (e.getSource() == noIcon)	{
			icon = null;
			setNewIcon();
		}
		else
		if (e.getSource() == urlTextField)	{
			icon = new IconConverter.IconAndUrl(null, urlTextField.getText());
			setNewIcon();
		}
		
		((Component)e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}



	// JFileChooser configuration helper classes
	
	private static class ImageFilter extends FileFilter
	{
		public boolean accept(File f) {
			return f.isDirectory() || isImage(f.getName());
		}
		
		private boolean isImage(String name)	{
			int i = name.lastIndexOf(".");
			if (i > 0)	{
				String e = name.substring(i).toLowerCase();
				return
						e.equals(".gif") ||
						e.equals(".png") ||
						e.equals(".jpg") ||
						e.equals(".jpeg") ||
						e.equals(".tif") ||
						e.equals(".tiff");
			}
			return false;
		}

		public String getDescription()	{
			return "Images";
		}
	}


	private static class ImagePreview extends JLabel implements
		PropertyChangeListener
	{
		private ImageIcon thumbnail = null;
		private File file = null;
		
		public ImagePreview(JFileChooser fc)	{
			setHorizontalAlignment(JLabel.CENTER);
			setVerticalAlignment(JLabel.CENTER);
			setPreferredSize(new Dimension(100, 50));
			fc.addPropertyChangeListener(this);
		}
		
		public void loadImage()	{
			if (file == null)	{
				thumbnail = null;
			}
			else	{
				ImageIcon tmpIcon = new ImageIcon(file.getPath());
				if (tmpIcon != null && tmpIcon.getImageLoadStatus() == MediaTracker.COMPLETE)
					if (tmpIcon.getIconWidth() > 90)
						thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(90, -1, Image.SCALE_DEFAULT));
					else
						thumbnail = tmpIcon;
			}
			setIcon(thumbnail);
		}
		
		public void propertyChange(PropertyChangeEvent e)	{
			boolean update = false;
			String prop = e.getPropertyName();
			
			if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop))	{
				file = null;
				update = true;
			}
			else
			if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop))	{
				file = (File) e.getNewValue();
				update = true;
			}
			
			if (update)	{
				thumbnail = null;
				if (isShowing())	{
					loadImage();
				}
			}
		}
	}



	// test main
	public static final void main(String [] args)	{
		JFrame f = new JFrame("IconChooser");
		JIconChooser ic = new JIconChooser(null);
		f.getContentPane().add(ic.getPanel());
		f.pack();
		f.setVisible(true);
	}

}
