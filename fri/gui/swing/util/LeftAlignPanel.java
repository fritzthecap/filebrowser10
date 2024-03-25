package fri.gui.swing.util;

import javax.swing.*;
import java.awt.*;

/**
	Panel that adds its members to a left aligning FlowLayout.
	Needed for property value rendering in GridLayouts.
*/

public class LeftAlignPanel extends JPanel
{
	public LeftAlignPanel(JComponent member)	{
		this(new JComponent [] { member });
	}
	
	public LeftAlignPanel(JComponent [] members)	{
		super(new FlowLayout(FlowLayout.LEFT));
		for (int i = 0; members != null && i < members.length; i++)
			add(members[i]);
	}

}
