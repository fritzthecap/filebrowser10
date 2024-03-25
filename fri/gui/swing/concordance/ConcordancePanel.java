package fri.gui.swing.concordance;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.JTextComponent;
import fri.util.concordance.text.*;
import fri.util.concordance.textfiles.FileLineWrapper;
import fri.util.concordance.Concordance;
import fri.util.observer.CancelProgressObserver;
import fri.gui.mvc.util.swing.EventUtil;

/**
	Scrollpane for concordant text blocks and its contained parts.
	@author Fritz Ritzberger
*/

public class ConcordancePanel extends JScrollPane implements
	FocusListener
{
	private static final Color selectionColor = Color.black, normalColor = Color.gray;
	private Map textAreaMap = new Hashtable();
	private JPanel panel;
	private Concordance.Block selectedBlock;
	private JTextComponent prevTextarea;
	
	
	public ConcordancePanel() {
		this(null);
	}
	
	public ConcordancePanel(Concordance concordance) {
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		getVerticalScrollBar().setUnitIncrement(17);
		setViewportView(panel);
		
		if (concordance != null)
			init(concordance.getBlockedResult());
	}

	
	/** Fills all blocks from concordance into panel if not null, else clears panel. */
	public void init(List blockedList)	{
		init(blockedList, null);
	}
	
	/** Fills all blocks from concordance into panel if not null, else clears panel. */
	public void init(final List blockedList, CancelProgressObserver observer)	{
		cleanListeners();

		// remove all current components
		EventUtil.invokeLaterOrNow(new Runnable()	{
			public void run()	{
				panel.removeAll();
				
				if (blockedList == null || blockedList.size() <= 0)	{
					setBorder(BorderFactory.createTitledBorder("Found No Concordances."));
					panel.revalidate();
					panel.repaint();
				}
			}
		});
		
		if (blockedList != null && blockedList.size() > 0)	{
			// build new components
			for (int i = 0; i < blockedList.size(); i++)	{
				if (observer != null)
					if (observer.canceled())
						break;
					else
						observer.setNote("Building Block "+i);
	
				addBlockToPanel((Concordance.Block)blockedList.get(i), i);
			}
			
			EventUtil.invokeLaterOrNow(new Runnable()	{
				public void run()	{
					panel.revalidate();
					setBorder(BorderFactory.createTitledBorder("Found "+blockedList.size()+" Concordance Blocks:"));
				}
			});
		}
	}


	private void addBlockToPanel(Concordance.Block block, int index)	{
		final JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(BorderFactory.createTitledBorder(""+(index + 1)));
		
		for (int i = 0; i < block.getOccurencesCount(); i++)	{	// loop over all occurences of block
			addOccurenceToBlock(block, i, p);
		}
		
		EventUtil.invokeLaterOrNow(new Runnable()	{
			public void run()	{
				panel.add(p);
			}
		});
	}
	
	
	private void addOccurenceToBlock(Concordance.Block block, int i, JPanel p)	{
		JTextComponent ta = createTextRenderer(new BlockOccurence(block, i));
		
		Object o = block.getPartObject(i, 0);
		if (o instanceof FileLineWrapper)	{
			FileLineWrapper flw = (FileLineWrapper)o;
			String name = flw.file.getName();
			String path = flw.file.getParent();
			ta.setBorder(BorderFactory.createTitledBorder(name+" in "+path));
			((TitledBorder)ta.getBorder()).setTitleColor(normalColor);
			((TitledBorder)ta.getBorder()).setBorder(BorderFactory.createLineBorder(normalColor));
		}
		else	{
			ta.setBorder(BorderFactory.createLineBorder(normalColor));
		}

		StringBuffer sb = new StringBuffer();
		
		for (int j = 0; j < block.getPartCount(); j++)	{
			addPartStringToOccurence(block, i, j, sb);
		}
		
		ta.setText(sb.toString());
		p.add(ta);
	}


	private void addPartStringToOccurence(Concordance.Block block, int i, int j, StringBuffer sb)	{
		if (j > 0)
			sb.append("\n");
		
		Object o = block.getPartObject(i, j);

		if (o instanceof LineWrapper)	{
			LineWrapper line = (LineWrapper)o;
			sb.append(line.lineNumber + 1);
			sb.append(":	");
			sb.append(line.line);
		}
		else	{
			sb.append(o.toString());
		}
	}
	

	protected JTextComponent createTextRenderer(BlockOccurence blockOccurence)	{
		JTextArea ta = new JTextArea();
		ta.setEditable(false);
		ta.setTabSize(2);

		ta.addFocusListener(this);

		textAreaMap.put(ta, blockOccurence);

		return ta;
	}

	protected void cleanListeners()	{
		Component [] comps = panel.getComponents();
		for (int i = 0; i < comps.length; i++)
			comps[i].removeFocusListener(this);

		textAreaMap.clear();
	}




	protected static class BlockOccurence
	{
		public final int occurence;
		public final Concordance.Block block;
		
		BlockOccurence(Concordance.Block block, int occurence)	{
			this.occurence = occurence;
			this.block = block;
		}
	}
	


	
	/** Implements FocusListener to set selected border. */
	public void focusGained(FocusEvent e)	{
		Object o = e.getSource();
		if (o instanceof JTextComponent && e.getOppositeComponent() != null && prevTextarea != o)	{
			setSelectedBlockOccurence((JTextComponent)o, (BlockOccurence)textAreaMap.get(o));
		}
	}
	public void focusLost(FocusEvent e)	{}


	protected void setSelectedBlockOccurence(JTextComponent textarea, BlockOccurence blockOccurence)	{
		this.selectedBlock = blockOccurence.block;
		setSelectedBorder(textarea);
	}

	public Concordance.Block getSelectedBlock()	{
		return selectedBlock;
	}

	private void setSelectedBorder(JTextComponent textarea)	{
		if (prevTextarea != null)	{
			Border border = prevTextarea.getBorder();
			if (border instanceof TitledBorder)	{
				((TitledBorder)border).setTitleColor(normalColor);
				((TitledBorder)border).setBorder(BorderFactory.createLineBorder(normalColor));
			}
			else	{
				prevTextarea.setBorder(BorderFactory.createLineBorder(normalColor));
			}
				
			prevTextarea.repaint();
		}

		prevTextarea = textarea;

		Border border = textarea.getBorder();
		if (border instanceof TitledBorder)	{
			((TitledBorder)border).setTitleColor(selectionColor);
			((TitledBorder)border).setBorder(BorderFactory.createLineBorder(selectionColor));
		}
		else	{
			textarea.setBorder(BorderFactory.createLineBorder(selectionColor));
		}

		textarea.repaint();
	}

}
