package fri.gui.awt.graphicsdevice;

import java.awt.*;
import java.awt.image.ColorModel;
import java.util.*;
import java.util.List;

/**
 * Splits GraphicsDevices by type into screens, printers and image buffers.
 * Sorts screen devices by start coordinates. Marks the default screen by
 * a wrapper, and sets a flag for every screen if it is within the AWT
 * screen ranges.
 * 
 * @author Fritz Ritzberger
 * Created on 18.01.2006
 */
public class GraphicsDeviceInfo
{
	public abstract static class Device
	{
		public final GraphicsDevice device;
		public final Properties properties;
		
		Device(GraphicsDevice screen, Properties properties)	{
			this.device = screen;
			this.properties = properties;
		}
		
		public String getName()	{
			return device.getIDstring();
		}
		public Rectangle getBounds()	{
			return device.getDefaultConfiguration().getBounds();
		}
	}

	public static class RasterScreen extends Device
	{
		public final boolean isDefault;
		public final boolean isWithinAWTScreen;
		
		public RasterScreen(GraphicsDevice screen, Properties properties, boolean isDefault, boolean isWithinAWTScreen)	{
			super(screen, properties);
			this.isDefault = isDefault;
			this.isWithinAWTScreen = isWithinAWTScreen;
		}
	}
	
	public static class Printer extends Device
	{
		Printer(GraphicsDevice screen, Properties properties)	{
			super(screen, properties);
		}
	}

	public static class ImageBuffer extends Device
	{
		ImageBuffer(GraphicsDevice screen, Properties properties)	{
			super(screen, properties);
		}
	}


	public final RasterScreen [] screens;
	public final Printer [] printers;
	public final ImageBuffer [] imageBuffers;
	
	public GraphicsDeviceInfo() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		//int resolutionDotsPerInch = toolkit.getScreenResolution();
		Dimension screenSize = toolkit.getScreenSize();

		// get graphic devices, split and sort them
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice defaultGraphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
		GraphicsDevice [] graphicsDevices = graphicsEnvironment.getScreenDevices();
		int graphicDevicesCount = graphicsDevices != null ? graphicsDevices.length : -1;
		List screenDevices = new ArrayList(4), printerDevices = new ArrayList(2), imageBufferDevices = new ArrayList();

		// split
		for (int i = 0; i < graphicDevicesCount; i++)	{
			if (graphicsDevices[i].getType() == GraphicsDevice.TYPE_RASTER_SCREEN)
				screenDevices.add(graphicsDevices[i]);
			else
			if (graphicsDevices[i].getType() == GraphicsDevice.TYPE_PRINTER)
				printerDevices.add(graphicsDevices[i]);
			else
			if (graphicsDevices[i].getType() == GraphicsDevice.TYPE_IMAGE_BUFFER)
				imageBufferDevices.add(graphicsDevices[i]);
		}
		
		// provide a sorter for screens
		Comparator sorter = new Comparator()	{
			public int compare(Object o1, Object o2) {
				return ((GraphicsDevice) o1).getDefaultConfiguration().getBounds().x - ((GraphicsDevice) o2).getDefaultConfiguration().getBounds().x;
			}
		};
		
		// sort screens by coordinates and wrap them with flags
		GraphicsDevice [] screenArray = (GraphicsDevice []) screenDevices.toArray(new GraphicsDevice[screenDevices.size()]);
		Arrays.sort(screenArray, sorter);
		this.screens = new RasterScreen[screenArray.length];
		for (int i = 0; i < screenArray.length; i++)	{
			Device dev = createDeviceInfo(screenArray[i], screenArray[i] == defaultGraphicsDevice, screenSize, toolkit);
			screens[i] = (RasterScreen) dev;
		}

		GraphicsDevice [] printerArray = (GraphicsDevice []) printerDevices.toArray(new GraphicsDevice[printerDevices.size()]);
		Arrays.sort(printerArray, sorter);
		this.printers = new Printer[printerArray.length];
		for (int i = 0; i < printerArray.length; i++)	{
			Device dev = createDeviceInfo(printerArray[i], false, null, toolkit);
			printers[i] = (Printer) dev;
		}

		GraphicsDevice [] imageBufferArray = (GraphicsDevice []) imageBufferDevices.toArray(new GraphicsDevice[imageBufferDevices.size()]);
		Arrays.sort(imageBufferArray, sorter);
		this.imageBuffers = new ImageBuffer[imageBufferArray.length];
		for (int i = 0; i < imageBufferArray.length; i++)	{
			Device dev = createDeviceInfo(imageBufferArray[i], false, null, toolkit);
			imageBuffers[i] = (ImageBuffer) dev;
		}
	}
	
	private Device createDeviceInfo(GraphicsDevice screen, boolean isDefault, Dimension screenSize, Toolkit toolkit)	{
		Properties props = new Properties();

		GraphicsConfiguration graphicsConfiguration = screen.getDefaultConfiguration();
		Rectangle bounds = graphicsConfiguration.getBounds();
		boolean isOnAwtScreen = screenSize != null && bounds.x >= 0 && bounds.x + bounds.width <= screenSize.width;
		try	{
			props.put("accelerated", ""+graphicsConfiguration.getImageCapabilities().isAccelerated());
			props.put("trueVolatile", ""+graphicsConfiguration.getImageCapabilities().isTrueVolatile());
			Insets insets = toolkit.getScreenInsets(graphicsConfiguration);
			props.put("insets left", ""+insets.left);
			props.put("insets right", ""+insets.right);
			props.put("insets top", ""+insets.top);
			props.put("insets bottom", ""+insets.bottom);
			props.put("displayChangeSupported", ""+screen.isDisplayChangeSupported());
			props.put("fullScreenSupported", ""+screen.isFullScreenSupported());
		}
		catch (Error e)	{
			e.printStackTrace();
		}
		
		try	{
			DisplayMode displayMode = screen.getDisplayMode();
			props.put("displayModeHeight", ""+displayMode.getHeight());
			props.put("displayModeWidth", ""+displayMode.getWidth());
			props.put("bitDepth", ""+displayMode.getBitDepth());
			props.put("refreshRate", ""+displayMode.getRefreshRate());
		}
		catch (Error e)	{
			e.printStackTrace();
		}
		
		try	{
			ColorModel colorModel = graphicsConfiguration.getColorModel();
			props.put("bitsPerPixel", ""+colorModel.getPixelSize());
			props.put("transparency", colorModel.getTransparency() == ColorModel.TRANSLUCENT
				? "TRANSLUCENT"
				: colorModel.getTransparency() == ColorModel.OPAQUE
					? "OPAQUE"
					: colorModel.getTransparency() == ColorModel.BITMASK
						? "BITMASK"
						: "(unknown)");
			props.put("alpha", ""+colorModel.hasAlpha());
			props.put("alphaPremultiplied", ""+colorModel.isAlphaPremultiplied());
		}
		catch (Error e)	{
			e.printStackTrace();
		}

		return screen.getType() == GraphicsDevice.TYPE_RASTER_SCREEN
			? (Device) new RasterScreen(screen, props, isDefault, isOnAwtScreen)
			: screen.getType() == GraphicsDevice.TYPE_PRINTER
				? (Device) new Printer(screen, props)
				: screen.getType() == GraphicsDevice.TYPE_IMAGE_BUFFER
					? (Device) new ImageBuffer(screen, props)
					: null;
	}

}
