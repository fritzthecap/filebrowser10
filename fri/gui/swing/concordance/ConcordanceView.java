package fri.gui.swing.concordance;

import java.io.File;
import java.util.Vector;
import java.awt.BorderLayout;
import javax.swing.*;
import fri.util.os.OS;
import fri.gui.mvc.view.Selection;
import fri.gui.mvc.view.swing.DefaultSwingView;
import fri.gui.swing.combo.ConstantHeightComboBox;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.splitpane.*;

public class ConcordanceView extends DefaultSwingView
{
	private ConcordancePanel concordancePanel;
	private ConstantHeightComboBox modeCombo;
	private JTextArea fileDisplay;
	private JComboBox patternInclude;
	private HistCombo filenamePattern;
	private SplitPane split;
	private ConcordanceDndPerformer [] dndListeners;
	
	public ConcordanceView() {
		fileDisplay = new JTextArea();
		fileDisplay.setEditable(false);
		JScrollPane sp = new JScrollPane(fileDisplay);
		sp.setBorder(BorderFactory.createTitledBorder("Search Folders And Files:"));

		concordancePanel = createConcordancePanel();
		concordancePanel.setBorder(BorderFactory.createTitledBorder("Found Concordance Blocks:"));

		split = new SplitPane(ConcordanceView.class, JSplitPane.VERTICAL_SPLIT, sp, concordancePanel);
		split.setDividerLocation(0.08d);

		add(split, BorderLayout.CENTER);

		modeCombo = new ConstantHeightComboBox();
		modeCombo.addItem(ConcordanceController.FILE_CONTENTS);
		modeCombo.addItem(ConcordanceController.FILE_NAMES);

		filenamePattern = new ConcordanceFilenameCombo();

		patternInclude = new ConstantHeightComboBox();
		patternInclude.addItem(ConcordanceController.INCLUDING);
		patternInclude.addItem(ConcordanceController.EXCLUDING);
	}

	protected ConcordancePanel createConcordancePanel()	{
		return new ConcordancePanel();
	}
	
	protected Selection createSelection()	{
		return new ConcordanceSelection(concordancePanel);
	}
	
	public void renderActions(ConcordanceController controller) {
		JToolBar tb = new JToolBar();
		if (OS.isAboveJava13) tb.setRollover(true);
		add(tb, BorderLayout.NORTH);
		
		tb.add(modeCombo);
		controller.visualizeAction(ConcordanceController.ACTION_OPEN, tb);
		controller.visualizeAction(ConcordanceController.ACTION_CONFIGURE, tb);
		controller.visualizeAction(ConcordanceController.ACTION_START, tb);
		tb.add(filenamePattern);
		tb.add(patternInclude);
		tb.add(Box.createHorizontalGlue());
		
		dndListeners = new ConcordanceDndPerformer[3];
		dndListeners[0] = new ConcordanceDndPerformer(tb, controller);
		dndListeners[1] = new ConcordanceDndPerformer(fileDisplay, controller);
		dndListeners[2] = new ConcordanceDndPerformer(concordancePanel.getViewport().getView(), controller);
	}

	public ConcordancePanel getConcordancePanel()	{
		return concordancePanel;
	}
	
	public void setSelectedFiles(File [] files)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; files != null && i < files.length; i++)
			sb.append((i > 0 ? "\n" : "")+files[i].getPath());
		fileDisplay.setText(sb.toString());
	}
	
	public JComboBox getModeCombo()	{
		return modeCombo;
	}

	public HistCombo getFilenamePattern()	{
		return filenamePattern;
	}

	public JComboBox getPatternIncluding()	{
		return patternInclude;
	}

	public void setWorking(boolean working)	{
		getModeCombo().setEnabled(working == false);
		getFilenamePattern().setEnabled(working == false);
		getPatternIncluding().setEnabled(working == false);
		for (int i = 0; i < dndListeners.length; i++)
			dndListeners[i].setActivated(working == false);
	}

	public boolean close()	{
		split.close();
		filenamePattern.save();
		return super.close();
	}
	
}




class ConcordanceFilenameCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();
	private static File globalFile;

	public ConcordanceFilenameCombo()	{
		this(new File(HistConfig.dir()+"ConcordanceFilenames.list"));
	}
	public ConcordanceFilenameCombo(File f)	{
		super();
		manageTypedHistory(this, f);
	}

	// interface TypedHistoryHolder
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}
	public Vector getTypedHistory()	{
		return globalHist;
	}
	public File getHistoryFile()	{
		return globalFile;
	}
}
