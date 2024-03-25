package fri.gui.swing.spinner;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
	A Panel with a editor component and a spinner that scrolls infinitely.
	The attached AdjustmentListener gets value -1 for scroll up
	and +1 for scroll down.
	<pre>
		final JTextField tf = new JTextField(20);
		InfiniteSpinner p = new InfiniteSpinner(tf);
		p.addAdjustmentListener(new AdjustmentListener()	{
			public void adjustmentValueChanged(AdjustmentEvent e)	{
				System.err.println("adjustment value: "+e.getValue());
				tf.setText("Received value = "+e.getValue());
			}
		});
	</pre>
*/

public class InfiniteSpinner extends JPanel
{
	/** The spinner scrollbar. */
	protected JScrollBar sb;
	/** The editor component. */
	private JTextComponent editor = null;

	/**
		Create an InfiniteSpinner that packs passed component (e.g. JTextField)
		and a spinner (JScrollBar) together.
	*/
	public InfiniteSpinner(JTextComponent editor)	{
		super(new BorderLayout());
		init(editor);
	}

	protected void init(JTextComponent editor)	{
		setEditor(editor);
		this.sb = addSpinner();
	}
	
	/** Returns the passed editor component. */
	public JTextComponent getEditor()	{
		return editor;
	}

	/** Sets a new editor component. */
	public void setEditor(JTextComponent editor)	{
		boolean revalidate = false;

		if (getEditor() != null)	{
			remove(getEditor());
			revalidate = true;
		}

		this.editor = editor;

		if (getEditor() != null)	{
			add(getEditor(), BorderLayout.CENTER);

			if (revalidate)
				revalidate();
		}
	}

	/** If editable is false, the textfield and the spinner gets disabled. */
	public void setEditable(boolean editable)	{
		editor.setEnabled(editable);
		sb.setEnabled(editable);
	}

	/** Returns true if textfeld is editable. */
	public boolean isEditable()	{
		return editor.isEnabled();
	}
	

	/** Call super.setEnabled() and setEditable(). */
	public void setEnabled(boolean enable)	{
		super.setEnabled(enable);
		setEditable(enable);
	}


	/** Adds the spinner to BorderLayout.EAST. */
	protected InfiniteSpinScrollBar addSpinner()	{
		InfiniteSpinScrollBar spinner = new InfiniteSpinScrollBar(getEditor());
		add(spinner, BorderLayout.EAST);
		return spinner;
	}

	/**
		Adds an AdjustmentListener that receives -1 for scroll up
		and +1 for scroll down to scrollbar.
	*/
	public void addAdjustmentListener(AdjustmentListener lsnr)	{
		sb.addAdjustmentListener(lsnr);
	}
	
	/** Removes the passed AdjustmentListener from scrollbar. */
	public void removeAdjustmentListener(AdjustmentListener lsnr)	{
		sb.removeAdjustmentListener(lsnr);
	}
	


	// test main

	public static void main(String [] args)	{
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		final JTextField tf = new JTextField(20);
		InfiniteSpinner p = new InfiniteSpinner(tf);
		p.addAdjustmentListener(new AdjustmentListener()	{
			public void adjustmentValueChanged(AdjustmentEvent e)	{
				System.err.println("adjustment value: "+e.getValue());
				tf.setText("Received value = "+e.getValue());
				//Thread.dumpStack();
			}
		});
		frame.getContentPane().add(p);
		frame.pack();
		frame.show();
	}
	
}