package fri.gui.swing.golinedialog;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import fri.gui.swing.spinnumberfield.*;

/**
	Go to a specified line in a JTextComponent, by a dialog.
	This dialog is not reusable.
*/

public class GoLineDialog extends JDialog implements
	ActionListener
{
	private JTextComponent textarea;
	private JButton ok;
	private SpinNumberField tf;
	private int lines;
	private JCheckBox doSelect;
	private static int prevLine = 0;	// undefined at begin, then buffer
	private static boolean selectLine = true;
	
	
	private GoLineDialog(Frame parent) {
		super(parent, "Go To Line", true);
		
		Container c = getContentPane();
		JPanel p = new JPanel();
		p.add(ok = new JButton("Ok"));
		c.add(tf = new SpinNumberField(0, Integer.MAX_VALUE - 1));
		c.add(p, BorderLayout.SOUTH);
		doSelect = new JCheckBox("  Select Line", selectLine);
		doSelect.setHorizontalTextPosition(SwingConstants.LEFT);
		c.add(doSelect, BorderLayout.EAST);
	}

	
	public GoLineDialog(Frame parent, JTextComponent textarea) {
		this(parent);
		init(textarea);
		listen();

		pack();
		setLocationRelativeTo(parent);

		setVisible(true);
	}

	
	private void init(JTextComponent ta)	{
		this.textarea = ta;
		
		int pos = lineFromCaretPosition(ta.getCaretPosition());	// set linecount
		if (prevLine > 0 && prevLine > lines)	// prevLine could be bigger than linecount
			prevLine = pos;
			
		tf.setValueAndRange(prevLine > 0 ? prevLine : pos, 1, lines);
		
		((JTextComponent)tf.getNumberEditor()).selectAll();
	}

	private void listen()	{
		ok.addActionListener(this);
		tf.addActionListener(this);

		KeyAdapter ka = new KeyAdapter()	{
			public void keyPressed(KeyEvent e)	{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)	{
					close();
				}
			}
		};
		addKeyListener(ka);
		ok.addKeyListener(ka);
		tf.addKeyListener(ka);
	}
	
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == ok || e.getSource() == tf.getNumberEditor())	{
			prevLine = (int)tf.getValue();
			gotoLine(prevLine);
			
			close();
			((Component)textarea).requestFocus();
		}
	}

	
	private int lineFromCaretPosition(int dot)	{
		// set number of lines as side effect
		Document doc = textarea.getDocument();
		Element map = doc.getDefaultRootElement();

		this.lines = map.getElementCount();
		return map.getElementIndex(dot) + 1;
	}
	
	
	private void gotoLine(int line)	{
		line--;
		if (line < 0)
			return;
			
		Document doc = textarea.getDocument();
		Element map = doc.getDefaultRootElement();
		Element lineElement = map.getElement(line);

		if (lineElement == null)	{
			//Thread.dumpStack();
			//Toolkit.getDefaultToolkit().beep();
			return;
		}

		int start = lineElement.getStartOffset();

		if (doSelect.isSelected() == false)	{
			textarea.setCaretPosition(start);
		}
		else	{
			int end = lineElement.getEndOffset() - 1;
			textarea.setCaretPosition(end);
			textarea.moveCaretPosition(start);
			//textarea.select(start, end);
		}
	}

	private void close()	{
		selectLine = doSelect.isSelected();

		// in JDK 1.3 this was possible: setVisible(false);
		dispose();	// JDK 1.4 needs dispose, no reusage of model dialogs?
	}


	// test main
	/*
	public static final void main(String [] args)	{
		final JFrame frame = new JFrame("GoLine Test");
		final JTextArea ta = new JTextArea();
		StringBuffer b = new StringBuffer();
		for (int i = 1; i <= 3000; i++)	{
			b.append(i);
			b.append('\n');
		}
		ta.setText(b.toString());
		frame.getContentPane().add(new JScrollPane(ta));
		JButton go = new JButton("go to line ...");
		frame.getContentPane().add(go, BorderLayout.SOUTH);
		final GoLineDialog dlg = new GoLineDialog(frame, ta);
		go.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				dlg.init(ta);
				dlg.setVisible(true);
			}
		});
		frame.setSize(200, 200);
		frame.show();
	}
	*/
	
}