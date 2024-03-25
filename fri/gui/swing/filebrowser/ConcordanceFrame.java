package fri.gui.swing.filebrowser;

import java.io.File;
import java.awt.event.WindowEvent;
import javax.swing.text.JTextComponent;
import fri.util.concordance.textfiles.FileLineWrapper;
import fri.gui.swing.concordance.ConcordanceMvcBuilder;
import fri.gui.swing.concordance.ConcordanceView;
import fri.gui.swing.concordance.ConcordancePanel;

/**
	Adds a popup to the LineCount table for viewing and editing files.
*/

public class ConcordanceFrame extends fri.gui.swing.concordance.ConcordanceFrame implements
	ViewEditPopup.FileSelection
{
	private File file;
	private ViewEditPopup popup;
	private JTextComponent prevTextarea;
	
	public ConcordanceFrame(File [] files)	{
		super(files);
		popup = new ViewEditPopup(this);
	}
	
	protected ConcordanceMvcBuilder newConcordanceMvcBuilder()	{
		return new ConcordanceMvcBuilder()	{
			protected ConcordanceView newConcordanceView()	{
				return new ConcordanceView()	{
					protected ConcordancePanel createConcordancePanel()	{
						return new ConcordancePanel()	{
							protected void setSelectedBlockOccurence(JTextComponent textarea, BlockOccurence blockOccurence)	{
								cleanListener();
								
								super.setSelectedBlockOccurence(textarea, blockOccurence);
								
								Object o = blockOccurence.block.getPartObject(blockOccurence.occurence, 0);
								if (o instanceof FileLineWrapper)	{
									FileLineWrapper flw = (FileLineWrapper)o;
									file = flw.file;
									textarea.addMouseListener(popup);
									prevTextarea = textarea;
								}
							}
							
							protected void cleanListeners()	{
								cleanListener();
								super.cleanListeners();
							}
						};
					}
				};
			}
		};
	}


	private void cleanListener()	{
		if (prevTextarea != null)
			prevTextarea.removeMouseListener(popup);
	}
	

	/** Implements ViewEditPopup.FileSelection. */
	public File [] getFiles()	{
		return new File [] { file };
	}


	public void windowClosing(WindowEvent e)	{
		if (prevTextarea != null)
			prevTextarea.removeMouseListener(popup);

		super.windowClosing(e);
	}

}
