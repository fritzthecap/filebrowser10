package fri.gui.swing.document.textfield.mask;

/**
	Adds setter method (writable) and cursor spin functionality
	to DefaultStringListMask.
*/

public class StringListMask extends DefaultStringListMask
{
	public StringListMask(String [] sarr)	{
		this(null, sarr);
	}

	public StringListMask(String initial, String [] sarr)	{
		super(initial, sarr);
		fixed = false;
	}



	public String cursorUp(int offset)	{
		return cursor(true);
	}

	public String cursorDown(int offset)	{
		return cursor(false);
	}

	protected String cursor(boolean up)	{
		if (first && up && selected <= 0)	{	// enable turnaround by cursor up when first
			selected = strings.size();
		}
		first = false;
			
		if (up && selected <= 0 || !up && selected >= strings.size() - 1)
			return null;
		
		selected = up ? selected - 1 : selected + 1;
		String newValue = strings.get(selected).toString();
		//System.err.println("cursor up "+up+" to new value "+newValue+", selected "+selected);
		
		textRemoval(0, length());
		textInsertion(0, newValue);
		
		return getText();
	}



	/* test main
	public static void main(String [] args)	{
		String [] sarr = new String []	{
			"abc",
			"abbc",
			"abcb",
			"aab",
			"baac",
		};
		javax.swing.JTextField tf = fri.gui.swing.document.textfield.MaskingDocument.createMaskingTextField(
				new StringListMask(sarr));

		tf.addKeyListener(new fri.gui.swing.document.textfield.SpinKeyListener());
		
		javax.swing.JFrame f = new javax.swing.JFrame("StringListMask");
		f.getContentPane().setLayout(new java.awt.FlowLayout());
		f.getContentPane().add(tf);
		f.pack();
		f.setVisible(true);
	}
	*/
	
}