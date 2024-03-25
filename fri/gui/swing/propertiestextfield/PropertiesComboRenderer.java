package fri.gui.swing.propertiestextfield;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import fri.util.props.*;

/**
	The renderer for Properties combo box, that shows
	a label and a text.
	This class is responsible for calculating the size of the
	label shown to the left of the text.

	@author  Ritzberger Fritz
 */

public class PropertiesComboRenderer extends JPanel implements
    ListCellRenderer
{
	private JLabel propName = new JLabel(" ");
	private JLabel propValue = new JLabel(" ");
	static final Object labelSide = BorderLayout.WEST;
	static final Border valueBorder = BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.gray, 1),
				BorderFactory.createEmptyBorder(0, 2, 0, 2));

	
	public PropertiesComboRenderer() {
		setLayout(new BorderLayout());
		
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		
		add(propName, labelSide);
		add(propValue, BorderLayout.CENTER);
		
		propName.setAlignmentX(Component.LEFT_ALIGNMENT);
		propValue.setAlignmentX(Component.RIGHT_ALIGNMENT);
	}
	
	public Component getListCellRendererComponent(
		JList list,
		Object value,
		int index, 
		boolean isSelected, 
		boolean cellHasFocus)
	{
		// prepare GUI
		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}

		setFont(list.getFont());
		propName.setFont(list.getFont());
		propValue.setFont(list.getFont());

		// calculate and set size for name-label

		PropertiesList.Tuple t = (PropertiesList.Tuple)value;
		propName.setText(t.name);
		propName.setToolTipText(t.name);

		ListModel model = list.getModel();
		int nameSize = getMaxSize(propName.getFont(), model);
		propName.setPreferredSize(new Dimension(nameSize, propName.getPreferredSize().height));
		
		propValue.setText(t.value);
		propValue.setBorder(t.value == null || t.value.length() <= 0 ? null : valueBorder);
		propValue.setToolTipText(t.value);

		return this;
	}
	
	
	private ListModel prevModel;
	private int prevSize;

	private int getMaxSize(Font f, ListModel model)	{
		if (model == prevModel)
			return prevSize;

		int size = 0;
		FontMetrics fm = getFontMetrics(f);
		for (int i = 0; i < model.getSize(); i++)	{
			PropertiesList.Tuple t = (PropertiesList.Tuple)model.getElementAt(i);
			int len = fm.stringWidth(t.name);
			size = Math.max(size, len);
		}
		
		prevSize = size;
		prevModel = model;

		return size + 4;
	}

}