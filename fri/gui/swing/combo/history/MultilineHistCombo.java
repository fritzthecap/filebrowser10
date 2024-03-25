package fri.gui.swing.combo.history;

import java.io.*;
import java.util.*;
import java.awt.*;

/**
	Ein Textfeld mit History-Funktion. Auch mehrzeilige Strings koennen
	mithilfe eines Buttons in einem kleinen Editor-Fenster erstellt werden.
	<p>
	Benutzung siehe HistCombo.
*/

public class MultilineHistCombo extends HistCombo
{
	public MultilineHistCombo()	{
		super();
		initEditor();
	}

	public MultilineHistCombo(File f)	{
		super(f);
		initEditor();
	}

	public MultilineHistCombo(Vector items)	{
		super(items);
		initEditor();
	}

	public MultilineHistCombo(String [] items)	{
		super(items);
		initEditor();
	}



	public Component getTextEditor()	{
		return ((MultilineHistComboEditor)getEditor()).getTextField();
	}


	private void initEditor()	{
		setEditor(new MultilineHistComboEditor(this));

		Component c = getEditor().getEditorComponent();
		c.removeKeyListener(this);
		c.removeFocusListener(this);
		
		c = getTextEditor();
		c.addKeyListener(this);
		c.addFocusListener(this);
	}


	/** Overridden to store multiline text */
	protected boolean doSave(File file, Vector myHist)  {
		System.err.println("saving multiline history for "+getClass()+" to file "+file);

		// use Properties to mask newline
		Properties props = new Properties();
		
		for (int i = 0; i < myHist.size(); i++)	{
			String s = myHist.elementAt(i).toString();
			String n = getKey(i);
			props.put(n, s);
		}
		
		try	{
			props.store(new FileOutputStream(file), "Multiline text history");
		}
		catch (IOException e)	{
			e.printStackTrace();
			return false;
		}
		return true;
	}


	/** Overridden to store multiline text */
	protected boolean doLoad(File file, Vector myHist)	{
		System.err.println("loading multiline history for "+getClass()+" from file "+file);

		Properties props = new Properties();
		
		try	{
			props.load(new FileInputStream(file));
		}
		catch (IOException e)	{
			//e.printStackTrace();	// may not be there
			return false;
		}
		
		for (int i = 0; i < props.size(); i++)	{
			String n = getKey(i);
			String s = props.getProperty(n);
			if (s != null)
				myHist.addElement(s);
		}
		return true;
	}
	
	private String getKey(int i)	{
		return "item"+(i < 10 ? "00"+i : i < 100 ? "0"+i : ""+i);
	}


	// test main
	/*
	public static void main(String [] args)	{
		JFrame f = new JFrame("MultilineHistCombo");
		f.getContentPane().setLayout(new FlowLayout());
		MultilineHistCombo combo = new MultilineHistCombo();
		combo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)	{
				System.err.println("------------> action performed "+((HistCombo)e.getSource()).getText());
			}
		});
		f.getContentPane().add(combo);
		f.getContentPane().add(new JButton("OK"));	// just for focus test
		f.setSize(new Dimension(200, 200));
		f.show();
	}
	*/
}