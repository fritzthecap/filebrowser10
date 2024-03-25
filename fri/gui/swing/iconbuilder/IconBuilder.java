package fri.gui.swing.iconbuilder;

import java.awt.*;
import java.awt.image.MemoryImageSource;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
	Builder for Icon library. Constructs an Image from a String array.
	Lets set dimension and colors (for chars) before build.
	
	@author Fritz Ritzberger, 2003
*/

class IconBuilder
{
	private int [] colorMap = new int [256];
		
	public IconBuilder()	{
	}
	
	public void setColor(char symbol, Color meaning)	{
		colorMap[symbol] = meaning == null ? 0 : meaning.getRGB();
	}
	
	public Icon getIcon(String [] design)	{
		return getIcon(design, design[0].length(), design.length);
	}
	
	public Icon getIcon(String [] design, int width, int height)	{
		int w = design[0].length();
		
		if (w > width)
			width = w;
			
		int h = design.length;
		if (h > height)
			height = h;

		int insetLeft = (width - w) / 2;
		int insetTop = (height - h) / 2;

		int [] pixels = new int [width * height];
		
		for (int row = 0; row < height; row++)	{
			for (int col = 0; col < width; col++)	{
				int idx = row * width + col;
				
				if (row >= insetTop && row < height - insetTop && col >= insetLeft && col < width - insetLeft)	{
					String s = design[row - insetTop];
					int ci = col - insetLeft;
					char c = s.charAt(ci);
					pixels[idx] = colorMap[c];
				}
				else	{
					pixels[idx] = 0;
				}
			}
		}
		
		MemoryImageSource imgSrc = new MemoryImageSource(width, height, pixels, 0, width);
		Image image = Toolkit.getDefaultToolkit().createImage(imgSrc);
		return new ImageIcon(image);
	}
	
}
