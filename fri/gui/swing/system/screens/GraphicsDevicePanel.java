package fri.gui.swing.system.screens;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import fri.util.props.PropertiesList;
import fri.gui.awt.graphicsdevice.GraphicsDeviceInfo;
import fri.gui.swing.propertiestextfield.PropertiesTextField;

/**
 * Panel that renders the graphics (multi-)screen and printer environment,
 * available since Java 1.4.
 * 
 * @author Fritz Ritzberger 2006
 */
public class GraphicsDevicePanel extends JPanel
{
	public GraphicsDevicePanel()	{
		super(new BorderLayout());
		
		JTabbedPane tabbedPane = new JTabbedPane();	//JTabbedPane.LEFT);
		add(tabbedPane);
		
		GraphicsDeviceInfo devInfo = new GraphicsDeviceInfo();
		tabbedPane.addTab("Raster Screens", createScreensPanel(devInfo.screens));
		tabbedPane.addTab("Printers", createPrintersPanel(devInfo.printers));
		tabbedPane.addTab("Image Buffers", createImageBuffersPanel(devInfo.imageBuffers));
	}
	
	private JPanel createScreensPanel(GraphicsDeviceInfo.RasterScreen [] screensInfo)	{
		// render screen info
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		int resolutionDotsPerInch = toolkit.getScreenResolution();
		JLabel awtInfo = new JLabel("AWT Screen Width = "+screenSize.width+", Height = "+screenSize.height+", Resolution (dots/inch) = "+resolutionDotsPerInch, SwingConstants.LEFT);
		
		// render screens
		JPanel screensPanel = createDevicesPanel();
		
		for (int i = 0; i < screensInfo.length; i++)	{
			GraphicsDeviceInfo.RasterScreen screen = screensInfo[i];
			boolean isPreviousOnAwtScreen = i > 0 && screensInfo[i - 1].isWithinAWTScreen;
			boolean isNextOnAwtScreen = i < screensInfo.length - 1 && screensInfo[i + 1].isWithinAWTScreen;
			Screen s = new Screen(
					screen.getName(),
					screen.isDefault,
					screen.getBounds().x,
					screen.getBounds().y,
					screen.getBounds().width,
					screen.getBounds().height,
					i == 0,
					i == screensInfo.length - 1,
					screen.properties,
					isPreviousOnAwtScreen,
					screen.isWithinAWTScreen,
					isNextOnAwtScreen);
			screensPanel.add(s);
		}

		JPanel p = new JPanel(new BorderLayout());
		p.add(awtInfo, BorderLayout.NORTH);
		p.add(createDevicesPanelContainer(screensPanel), BorderLayout.CENTER);
		return p;
	}
	
	private JPanel createPrintersPanel(GraphicsDeviceInfo.Printer [] printersInfo)	{
		return createDevicePanel(printersInfo, "Printers");
	}
	
	private JPanel createImageBuffersPanel(GraphicsDeviceInfo.ImageBuffer [] imageBuffersInfo)	{
		return createDevicePanel(imageBuffersInfo, "Image Buffers");
	}
	
	private JPanel createDevicePanel(GraphicsDeviceInfo.Device [] devicesInfo, String noneLabel)	{
		JPanel devicesPanel = createDevicesPanel();
		
		if (devicesInfo.length <= 0)	{
			devicesPanel.add(new JLabel("(No "+noneLabel+")", SwingConstants.CENTER));
		}
		else	{
			for (int i = 0; i < devicesInfo.length; i++)	{
				GraphicsDeviceInfo.Device device = devicesInfo[i];
				Screen s = new Screen(
						device.getName(),
						device.getBounds().x,
						device.getBounds().y,
						device.getBounds().width,
						device.getBounds().height,
						i == 0,
						i == devicesInfo.length - 1,
						device.properties);
				devicesPanel.add(s);
			}
		}

		JPanel p = new JPanel(new BorderLayout());
		p.add(createDevicesPanelContainer(devicesPanel), BorderLayout.CENTER);
		return p;
	}
	
	private JPanel createDevicesPanel()	{
		JPanel devicesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		// 0, 0: no gaps between screens because of AWT screen container border
		return devicesPanel;
	}
	
	private JComponent createDevicesPanelContainer(JComponent devicesPanel)	{
		return new JScrollPane(devicesPanel);
	}
	

	
	private static class Screen extends JPanel
	{
		/** GraphicsDevice printer constructor. */
		Screen(String name, int x, int y, int width, int height, boolean isFirst, boolean isLast, Properties properties)	{
			this(name, false, x, y, width, height, false, false, properties, false, false, false);
		}
		
		/** GraphicsDevice screen constructor. */
		Screen(String name, boolean isDefault, int x, int y, int width, int height, boolean isFirst, boolean isLast, Properties properties, boolean isPreviousOnAwtScreen, boolean isThisOnAwtScreen, boolean isNextOnAwtScreen)	{
			super(new BorderLayout());
			
			JPanel p = new JPanel(new BorderLayout());
			p.setBackground(Color.white);
			p.setForeground(Color.black);
			p.setPreferredSize(new Dimension(width / 6, height / 6));
			setBorder(p, isDefault, isFirst, isLast, isPreviousOnAwtScreen, isThisOnAwtScreen, isNextOnAwtScreen);
			
			JPanel north = new JPanel(new BorderLayout());
			north.setOpaque(false);
			north.add(new JLabel(""+x+"/"+y, SwingConstants.LEFT), BorderLayout.WEST);
			north.add(new JLabel(""+width, SwingConstants.CENTER), BorderLayout.CENTER);
			north.add(new JLabel(""+(x + width)+"/"+y, SwingConstants.RIGHT), BorderLayout.EAST);

			JPanel south = new JPanel(new BorderLayout());
			south.setOpaque(false);
			south.add(new JLabel(""+x+"/"+(y + height), SwingConstants.LEFT), BorderLayout.WEST);
			south.add(new JLabel(""+(x + width)+"/"+(y + height), SwingConstants.RIGHT), BorderLayout.EAST);

			p.add(north, BorderLayout.NORTH);
			p.add(new JLabel(""+height, SwingConstants.LEFT), BorderLayout.WEST);
			p.add(south, BorderLayout.SOUTH);

			JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
			nameLabel.setFont(nameLabel.getFont().deriveFont((float) (nameLabel.getFont().getSize() * 1.5)));
			p.add(nameLabel, BorderLayout.CENTER);
			
			add(p, BorderLayout.CENTER);

			JComponent propertiesRenderer = new PropertiesTextField(new PropertiesList(properties), false);
			propertiesRenderer.setBorder(BorderFactory.createEtchedBorder());
			add(propertiesRenderer, BorderLayout.SOUTH);
		}
		
		private void setBorder(JPanel p, boolean isDefault, boolean isFirst, boolean isLast, boolean isPreviousOnAwtScreen, boolean isThisOnAwtScreen, boolean isNextOnAwtScreen)	{
			Border inner = isDefault ? BorderFactory.createTitledBorder("Default") : BorderFactory.createTitledBorder("Other");
			int top = 4;
			int left = isFirst || isPreviousOnAwtScreen != isThisOnAwtScreen ? 4 : 0;
			int bottom = 4;
			int right = isLast || isNextOnAwtScreen != isThisOnAwtScreen ? 4 : 0;
			Border outer = isThisOnAwtScreen
				? BorderFactory.createMatteBorder(top, left, bottom, right, Color.darkGray)
				: BorderFactory.createMatteBorder(top, left, bottom, right, Color.lightGray);
			p.setBorder(BorderFactory.createCompoundBorder(outer, inner));
		}
	}
	
	
	
	public static void main(String [] args)	{
		JFrame f = new JFrame();
		f.getContentPane().add(new GraphicsDevicePanel());
		f.pack();
		f.setVisible(true);
	}

}
