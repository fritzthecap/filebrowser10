package fri.gui.swing.editor;

import javax.swing.Action;
import java.util.Vector;

import fri.gui.mvc.view.Selection;

/**
	Focus history that works differently from an UndoManager: when going back
	and then selecting a new frame, the skipped items are not removed from history. So the
	back button will go to the items that were skipped backwards before.
	
	@author Fritz Ritzberger
*/

public class FocusHistory
{
	private Action back, forward;
	private Selection focusSwitcher;
	private Object current;
	private boolean blind = false;
	private HistBuffer histBuf;

	
	public FocusHistory(Selection focusSwitcher)	{
		this.focusSwitcher = focusSwitcher;
		histBuf = new HistBuffer();
	}
	
	public boolean back()	{
		return move("back");
	}
	
	public boolean forward()	{
		return move("forward");
	}
	
	public void set(Object frame)	{
		if (blind)
			return;
			
		histBuf.set(frame);
		current = frame;
		
		if (histBuf.size() > 1)
			back.setEnabled(true);
		
		if (histBuf.canForward() == false)
			forward.setEnabled(false);
	}

	public void remove(Object frame)	{
		if (current == frame)	{
			if (back() == false)
				forward();
		}
		
		histBuf.del(frame);
		
		if (histBuf.size() <= 1)	{
			back.setEnabled(false);
			forward.setEnabled(false);
		}
		
		//System.err.println("focus history size is "+histBuf.size());
	}
	
	public void setFocusActions(Action back, Action forward)	{
		this.back = back;
		this.forward = forward;
		back.setEnabled(false);
		forward.setEnabled(false);
	}
	
	private boolean move(String direction)	{
		if (direction.equals("back"))	{
			if ((current = histBuf.back()) == null)	{
				back.setEnabled(false);
				return false;
			}
		}
		else	{
			if ((current = histBuf.forward()) == null)	{
				forward.setEnabled(false);
				return false;
			}
		}
		
		forward.setEnabled(histBuf.canForward());
		back.setEnabled(histBuf.canBack());
		
		blind = true;
		//System.err.println("FocusHistory calling setSelectedObject to "+focusSwitcher.getClass());
		focusSwitcher.setSelectedObject(current);
		blind = false;
		
		return true;
	}
	
}


class HistBuffer extends Vector
{
	private int index = -1;
	private static int MAX = 100;
	
	HistBuffer()	{
		super(MAX);
	}
	
	public void set(Object frame)	{
		if (size() > 0 && elementAt(index) == frame)
			return;	// nicht zweimal dasselbe hintereinander zulassen

		// wenn index nicht am Ende, history revidieren
		if (index < size() - 1)	{
			Object o = elementAt(index);
			index = size() - 1;
			set(o);	// haenge aktuelles hinten an
		}
		ensureMaximum();
		addElement(frame);
		incr();
		//System.err.println("histBuf is at index "+index);
	}

	private void ensureMaximum()	{
		// wenn buffer zu gross wird
		if (size() >= MAX)	{	// loesche das am weitesten entfernte Element
			del(0);
		}
	}
	
	public void del(Object frame)	{
		int i;
		do	{
			i = indexOf(frame);
			if (i >= 0)	{
				del(i);
			}
		}
		while (i >= 0);
	}
	
	private void del(int i)	{
		//System.err.println("Deleting history element at "+i);
		if (i <= index)
			decr();
		removeElementAt(i);
		// clean redundant entries
		if (i > 0 && i < size() && elementAt(i) == elementAt(i - 1))	{
			removeElementAt(i);
			if (i <= index)
				decr();
		}
	}
	
	public Object back()	{
		if (decr())
			return elementAt(index);
		return null;
	}
	
	public Object forward()	{
		if (incr())
			return elementAt(index);
		return null;
	}

	public boolean canBack()	{
		return index > 0;
	}

	private boolean decr()	{
		if (canBack())	{
			index--;
			return true;
		}
		return false;
	}

	public boolean canForward()	{
		return index < size() - 1;
	}
	
	private boolean incr()	{
		if (canForward())	{
			index++;
			return true;
		}
		return false;
	}
	
}