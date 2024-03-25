package fri.gui.swing.concordance.filter;

import java.awt.*;
import javax.swing.*;
import fri.gui.mvc.view.swing.*;
import fri.gui.mvc.model.Model;
import fri.gui.swing.util.LeftAlignPanel;
import fri.gui.swing.spinnumberfield.SpinNumberField;
import fri.gui.swing.document.textfield.MaskingDocument;
import fri.gui.swing.document.textfield.mask.UniqueCharacterMask;
import fri.gui.swing.expressions.*;

/**
	Text filter panel, containing:
	<ul>
		<li>Break after maximum found concordances.</li>
		<li>Minimum lines per block.</li>
		<li>Textfield "Remove given Characters before evaluating".</li>
		<li>Checkbox "Remove Leading And Trailing Spaces".</li>
		<li>One filter "Exclude items shorter than ... characters".</li>
		<li>Filter expressions treeview.</li>
	</ul>
*/

public class FilterView extends DefaultSwingView
{
	private SpinNumberField breakAfterCount;
	private JTextField charsToRemove;
	private JCheckBox trimLines, normalizeLines;
	private SpinNumberField charMinimum;
	private SpinNumberField minimumLinesPerBlock;
	private FilterTreeView filterTreeView;
	
	
	public FilterView(FilterTreeView filterTreeView)	{
		this.filterTreeView = filterTreeView;
		
		JPanel panel = new JPanel();
		JPanel p = new JPanel(new BorderLayout());
		JPanel labels = new JPanel(new GridLayout(6, 1));
		p.add(labels, BorderLayout.WEST);
		JPanel fields = new JPanel(new GridLayout(6, 1));
		p.add(fields, BorderLayout.CENTER);
		panel.add(p);
		
		labels.add(new JLabel("Break After ... Concordances: "));
		breakAfterCount = new SpinNumberField();
		fields.add(new LeftAlignPanel(breakAfterCount));
		labels.add(new JLabel("Minimum Lines Per Block: "));
		minimumLinesPerBlock = new SpinNumberField();
		fields.add(new LeftAlignPanel(minimumLinesPerBlock));
		labels.add(new JLabel("Remove Leading And Trailing Spaces: "));
		trimLines = new JCheckBox("", true);
		fields.add(trimLines);
		labels.add(new JLabel("Normalize Lines (Remove Redundant Spaces): "));
		normalizeLines = new JCheckBox("", true);
		fields.add(normalizeLines);
		labels.add(new JLabel("Remove Characters Before Evaluation: "));
		charsToRemove = new JTextField(6);
		charsToRemove.setDocument(new MaskingDocument(charsToRemove, new UniqueCharacterMask()));
		fields.add(new LeftAlignPanel(charsToRemove));
		labels.add(new JLabel("Exclude Items Shorter Than ... Characters: "));
		charMinimum = new SpinNumberField();
		fields.add(new LeftAlignPanel(charMinimum));
		
		add(panel, BorderLayout.NORTH);
		add(filterTreeView, BorderLayout.CENTER);
	}

	public void setModel(Model model)	{
		super.setModel(model);
		
		FilterModel m = (FilterModel)model;
		
		filterTreeView.getModelManager().setSelectedItem(m.getFilterTreeModelName());
		breakAfterCount.setValue(m.getBreakAfterCount());
		minimumLinesPerBlock.setValue(m.getMinimumLinesPerBlock());
		trimLines.setSelected(m.getTrimLines());
		normalizeLines.setSelected(m.getNormalizeLines());
		charsToRemove.setText(m.getCharsToRemove());
		charMinimum.setValue(m.getCharMinimum());
	}

	public Model getModel()	{
		FilterModel m = (FilterModel)super.getModel();
		
		m.setFilterTreeModelName(filterTreeView.getModelManager().getSelectedItem().toString());
		m.setBreakAfterCount((int)breakAfterCount.getValue());
		m.setMinimumLinesPerBlock((int)minimumLinesPerBlock.getValue());
		m.setTrimLines(trimLines.isSelected());
		m.setNormalizeLines(normalizeLines.isSelected());
		m.setCharsToRemove(charsToRemove.getText());
		m.setCharMinimum((int)charMinimum.getValue());
		
		return m;
	}

}
