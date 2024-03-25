package fri.gui.swing.calculator;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import fri.gui.swing.button.NoInsetsButton;
import fri.gui.swing.spinnumberfield.*;
import fri.gui.swing.splitpane.*;

public class CalculatorPanel extends SplitPane implements
	ActionListener,
	KeyListener,
	NumberEditorListener
{
	private static final String DEC_FMT_ITEM = "Dec Fmt";
	private static final String DEC_ITEM = "Dec";
	private static final String OCT_ITEM = "Oct";
	private static final String HEX_ITEM = "Hex";
	private static final String BIN_ITEM = "Bin";
	private static final String CHOOSE_ITEM = "Other";
	private Calculator calculator = new Calculator();
	private NumberFormat decimalFormat;
	private int fixedFraction = -1;
	private JTextArea input;
	private JTextField output;
	private JComboBox radix;
	private SpinNumberField fix;
	private JButton clear;
	private JButton sqrt;
	private JButton plusMinus, equals;
	private JButton pi;
	private JButton e;
	private JButton not;
	private JButton sin, cos, asin, acos, tan, atan;
	private JButton abs, round, rad, deg, lgn, exp;
	private JButton equals2, toLeft;
	private Object lastResult;
	private SplitPane upperSplitPane;
	
	public CalculatorPanel()	{
		super(CalculatorFrame.class, JSplitPane.VERTICAL_SPLIT);
		build();
	}
	
	private void build()	{
		setTopComponent(buildInputOutputTextFields());
		setBottomComponent(buildInputPanels());
	}
	
	private Component buildInputOutputTextFields()	{
		input = new JTextArea(2, 16);
		input.addKeyListener(this);
		input.setToolTipText("Arithmetic Input, \"xFF\" (hex), \"o77\" (oct), \"b11\" (bin)");

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
		
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
		p1.add(equals2 = new NoInsetsButton(" = "));
		equals2.setBackground(Color.green);
		equals2.addActionListener(this);
		equals2.setToolTipText("Calculate!");
		equals2.setMaximumSize(new Dimension(32, 16));
		p1.add(toLeft = new NoInsetsButton(" <= "));
		toLeft.setBackground(Color.white);
		toLeft.addActionListener(this);
		toLeft.setToolTipText("Put Result To Input Field");
		toLeft.setMaximumSize(new Dimension(32, 16));
		rightPanel.add(p1);
		
		output = new JTextField(22);
		output.setMaximumSize(new Dimension(800, 24));
		output.setEditable(false);
		output.setBackground(input.getBackground());
		output.setHorizontalAlignment(JTextField.RIGHT);
		output.setToolTipText("Result Output");
		rightPanel.add(output);

		radix = new JComboBox();
		radix.setToolTipText("Result Display Radix");
		radix.addItem(DEC_FMT_ITEM);
		radix.addItem(DEC_ITEM);
		radix.addItem(HEX_ITEM);
		radix.addItem(OCT_ITEM);
		radix.addItem(BIN_ITEM);
		radix.addItem(CHOOSE_ITEM);
		radix.addActionListener(this);
		radix.setMaximumSize(new Dimension(100, 24));
		rightPanel.add(radix);
		
		fix = new SpinNumberField(" Fix");
		fix.setToolTipText("Number Of Fraction Digits");
		fix.setMaximumSize(new Dimension(60, 24));
		fix.getNumberEditor().addNumberEditorListener(this);
		fix.setRange(0, 15);
		rightPanel.add(fix);

		upperSplitPane = new SplitPane(CalculatorPanel.class, SplitPane.HORIZONTAL_SPLIT, new JScrollPane(input), rightPanel);
		upperSplitPane.setDividerLocation(0.5d);
		return upperSplitPane;
	}
	
	public boolean close()	{
		upperSplitPane.close();
		return super.close();
	}
	
	private Component buildInputPanels()	{
		JPanel p = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		p.setLayout(gridbag);

		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.BOTH;
		gc.gridheight = 1;
		gc.weighty = 1.0;
		
		Component c;

		c = buildBasicInput();
		gc.gridwidth = 3;
		gc.weightx = 3.0;
		gridbag.setConstraints(c, gc);
		p.add(c);
		
		c = buildBasicOperators();
		gc.gridwidth = 2;
		gc.weightx = 2.0;
		gridbag.setConstraints(c, gc);
		p.add(c);

		c = buildExtendedOperators();
		gc.gridwidth = 3;
		gc.weightx = 3.0;
		gridbag.setConstraints(c, gc);
		p.add(c);

		c = buildTrigonometricOperators();
		gc.gridwidth = 3;
		gc.weightx = 3.0;
		gridbag.setConstraints(c, gc);
		p.add(c);

		return p;
	}

	private JButton createButton(String label, String tooltip, JPanel box)	{
		JButton b = new JButton(label);
		b.setMinimumSize(new Dimension(24, 24));
    b.setPreferredSize(new Dimension(40, 24));
    // b.setMaximumSize(new Dimension(40, 24)); // does not help to avoid button stretch
		b.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		if (tooltip != null)
			b.setToolTipText(tooltip);
		box.add(b);
		b.addActionListener(this);
		return b;
	}
	
	private Component buildBasicInput()	{
		JPanel p = new JPanel(new GridLayout(4, 3));
		p.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		createButton("7", null, p);
		createButton("8", null, p);
		createButton("9", null, p);

		createButton("4", null, p);
		createButton("5", null, p);
		createButton("6", null, p);

		createButton("1", null, p);
		createButton("2", null, p);
		createButton("3", null, p);

		createButton("0", null, p);
		plusMinus = createButton("+/"+Calculator.minus, "Unary Sign", p);
		createButton(""+Calculator.decimalSeparator, "Decimal Separator", p);
		
		return p;
	}
	
	private Component buildBasicOperators()	{
		JPanel p = new JPanel(new GridLayout(4, 2));
		p.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		createButton(" "+Calculator.div+" ", "Division", p);
		createButton(" "+Calculator.mod+" ", "Remainder Of Division", p);

		createButton(" "+Calculator.mult+" ", "Multiplication", p);
		createButton(" "+Calculator.pow+" ", "Power", p);

		createButton(" "+Calculator.minus+" ", "Subtraction", p);
		sqrt  = createButton(" "+Calculator.sqrt+" ", "Square Root", p);

		createButton(" "+Calculator.plus+" ", "Addition", p);
		equals = createButton("=", "Calculate!", p);
		equals.setBackground(Color.green);
		
		return p;
	}
	
	private Component buildExtendedOperators()	{
		JPanel p = new JPanel(new GridLayout(4, 3));
		p.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		createButton(" "+Calculator.and+" ", "Bitwise AND", p);
		createButton("(", null, p);
		createButton(")", null, p);

		createButton(" "+Calculator.or+" ", "Bitwise OR", p);
		createButton(" "+Calculator.lshift+" ", "Shift Left", p);
		pi = createButton("PI", "The ratio of a circle's circumference to its diameter", p);
		
		createButton(" "+Calculator.xor+" ", "Bitwise XOR", p);
		createButton(" "+Calculator.rshift+" ", "Shift Right", p);
		e = createButton("E", "The base of the natural logarithms", p);

		not = createButton(""+Calculator.not+"", "Bitwise NOT", p);
		createButton(" "+Calculator.urshift+" ", "Unsigned Shift Right", p);
		clear = createButton("Clear", "Clear Selection Or Textfields", p);
		clear.setBackground(input.getBackground());
		
		return p;
	}

	private Component buildTrigonometricOperators()	{
		JPanel p = new JPanel(new GridLayout(4, 3));
		p.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		sin = createButton(" "+Calculator.sin+" ", "Trigonometric Sine", p);
		tan  = createButton(" "+Calculator.tan+" ", "Trigonometric Tangent", p);
		exp = createButton(" "+Calculator.exp+" ", "Exponential Number Raised To Power", p);

		cos = createButton(" "+Calculator.cos+" ", "Trigonometric Cosine", p);
		atan = createButton(" "+Calculator.atan+" ", "Arc Tangent", p);
		lgn = createButton(" "+Calculator.lgn+" ", "Natural Logarithm", p);

		asin = createButton(" "+Calculator.asin+" ", "Arc Sine", p);
		rad = createButton(" "+Calculator.rad+" ", "To Radians", p);
		abs = createButton(" "+Calculator.abs+" ", "Absolute Value", p);

		acos = createButton(" "+Calculator.acos+" ", "Arc Cosine", p);
		deg = createButton(" "+Calculator.deg+" ", "To Degrees", p);
		round = createButton(" "+Calculator.round+" ", "Round To Nearest Integer", p);
		
		return p;
	}


	private void refreshOutput()	{
		String s = format((Double)calculator.getResult());
		output.setText(s);
	}
	
	// interface ActionListener

	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == equals || e.getSource() == equals2)	{
			calculateAction();
		}
		else
		if (e.getSource() == toLeft)	{
			String s = output.getText();
			if (s.length() > 0 && calculator.getResult() != null)	{
				input.setText(calculator.getResult().toString());
				output.setText("");
				calculator.clearResult();
			}
		}
		else
		if (e.getSource() == clear)	{
			if (input.getSelectionStart() < input.getSelectionEnd())
				input.replaceSelection("");
			else
				input.setText("");

			output.setText("");
			equals.setBackground(Color.green);
			calculator.clearResult();
		}
		else
		if (e.getSource() == radix)	{
			if (radix.getSelectedItem().equals(CHOOSE_ITEM))	{
				chooseRadix();
			}
			if (output.getText().length() > 0 && calculator.getResult() instanceof Double)	{
				refreshOutput();
			}
		}
		else
		if (e.getSource() == plusMinus)	{
			unaryAction(Calculator.minus, true);
		}
		else
		if (isUnaryAction(e.getSource()))	{
			unaryAction(((JButton)e.getSource()).getText(), false);
		}
		else
		if (e.getSource() == pi)	{
			insertText(format(new Double(Math.PI)));
		}
		else
		if (e.getSource() == this.e)	{
			insertText(format(new Double(Math.E)));
		}
		else	{
			JButton src = (JButton)e.getSource();
			String s = src.getText();
			insertText(s);
		}
	}
	
	private boolean isUnaryAction(Object evtSrc)	{
		return
				evtSrc == not ||
				evtSrc == sqrt ||
				evtSrc == lgn ||
				evtSrc == exp ||
				evtSrc == abs ||
				evtSrc == round ||
				evtSrc == sin ||
				evtSrc == cos ||
				evtSrc == asin ||
				evtSrc == acos ||
				evtSrc == tan ||
				evtSrc == atan ||
				evtSrc == rad ||
				evtSrc == deg;
	}
	
	private void unaryAction(String operator, boolean toggleWhenFound)	{
		if (input.getSelectionStart() < input.getSelectionEnd())	{
			String s = input.getSelectedText();
			if (s.equals(operator) && toggleWhenFound)
				input.replaceSelection("");
			else
				input.replaceSelection(operator);
		}

		// seek from cursorposition for next number and invert its sign
		// to left if within or at end of a number
		// to right if on space or operator

		// get current line
		int dot = input.getCaretPosition();
		Element map = input.getDocument().getDefaultRootElement();
		int currLine = map.getElementIndex(dot);
		Element lineElement = map.getElement(currLine);
		int start = lineElement.getStartOffset();
		int end = lineElement.getEndOffset();
		int relativeDotOffset = dot - start;
		String line = null;
		try	{ line = input.getText(start, end - start - 1); }
		catch (Exception ex)	{}
		
		if (line == null || line.length() <= 0)	{
			input.insert(operator, dot);
			return;
		}
			
		boolean searchLeft;
		//System.err.println("relative dot offset is "+relativeDotOffset+", line length "+line.length()+" >"+line+"<");
		
		if (relativeDotOffset <= 0)	{	// at line start, search to right
			searchLeft = false;
		}
		else
		if (relativeDotOffset >= line.length())	{	// at line end, search to left
			searchLeft = partOfDigit(line.charAt(relativeDotOffset - 1));
		}
		else	{
			char c1 = line.charAt(relativeDotOffset - 1);
			char c2 = line.charAt(relativeDotOffset);
			
			if (!partOfDigit(c1) && !partOfDigit(c2))
				return;	// cursor is not on or at end of a number

			searchLeft = partOfDigit(c1) && partOfDigit(c2) || partOfDigit(c2) == false;
		}
		
		int pos = relativeDotOffset;	// estimate insert position
		//System.err.println("searching left is "+searchLeft);
		
		if (searchLeft)	{	// search left
			if (pos > 0)
				pos--;
			
			for (; pos > 0 && partOfDigit(line.charAt(pos)) == false; pos--)
				;
			boolean foundDigit = false;
			for (; pos > 0 && partOfDigit(line.charAt(pos)); pos--)
				foundDigit = true;
				
			if (foundDigit && partOfDigit(line.charAt(pos)) == false)
				pos++;
		}
		else	{	// search right
			for (; pos < line.length() && partOfDigit(line.charAt(pos)) == false; pos++)
				;
		}

		// now we are before a number or at end of line
		if (pos == line.length() || partOfDigit(line.charAt(pos)))	{
			if (pos - operator.length() >= 0)	{	// check if operator min is already there
				String s = line.substring(pos - operator.length(), pos);

				if (s.equals(operator) && toggleWhenFound)	// erase operator
					input.replaceRange("", start + pos - operator.length(), start + pos);
				else	// insert operator
					input.insert(operator, start + pos);
			}
			else	{
				input.insert(operator, start + pos);
			}
		}
	}
	
	
	private boolean partOfDigit(char c)	{
		return Character.isDigit(c) || c == Calculator.decimalSeparator || c == '.';
	}

	
	
	private void calculateAction()	{
		String s = input.getText().trim();
		
		if (s.length() <= 0)	{
			output.setText("");
			equals.setBackground(Color.green);
			return;
		}
			
		// prepare error stream
		ByteArrayOutputStream errs = new ByteArrayOutputStream();
		PrintStream errStream = new PrintStream(errs);
		
		// calculate
		Object result = calculator.calculate(s, errStream);
		errStream.close();
		
		// render result
		if (result instanceof Double)	{
			s = format((Double)result);
			output.setText(s);
			equals.setBackground(Color.green);
		}
		else	{	// error happened
			output.setText("");
			equals.setBackground(Color.red);
			
			String error = errs.toString();
			JTextArea err = new JTextArea(error);
			err.setFont(new Font("Monospaced", Font.PLAIN, 12));
			err.setEditable(false);
			
			JOptionPane.showMessageDialog(
					this,
					new JScrollPane(err),
					"Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	

	private boolean isDecimalOutputOption(String base)	{
		return base.equals(DEC_ITEM) || base.equals(DEC_FMT_ITEM);
	}
		
	private String format(Double result)	{
		if (result == null)
			return "";

		String text = "";
		String base = (String)radix.getSelectedItem();

		// if it is a integer, consider base number radix
		if (result.isNaN() == false &&
				isDecimalOutputOption(base) == false &&
				result.doubleValue() == Math.rint(result.doubleValue()))	// no floating point
		{
			long n = result.longValue();
			
			if (base.equals(HEX_ITEM))
				text = Long.toHexString(n);
			else
			if (base.equals(OCT_ITEM))
				text = Long.toOctalString(n);
			else
			if (base.equals(BIN_ITEM))
				text = Long.toBinaryString(n);
			else	{	// arbitrary radix
				int b = Integer.parseInt(base);
				text = Long.toString(n, b);
			}
			text = text.toUpperCase();
		}
		else	{
			if (result.isNaN() == false && isDecimalOutputOption(base) == false)	{
				radix.removeActionListener(this);
				radix.setSelectedItem(DEC_FMT_ITEM);
				radix.addActionListener(this);
			}
			
			if (result.isNaN())	{
				text = "Not a Number";
			}
			else	{
				text = ensureDecimalFormat().format(result.doubleValue());
				// not precise when longer than 16 positions
				text = checkDecimalNumberLength(result, text, base.equals(DEC_ITEM));
			}
		}
		
		return text;
	}
	
	
	private void chooseRadix()	{
		JPanel p = new JPanel();
		SpinNumberField nf = new SpinNumberField("Radix");
		nf.setToolTipText("Choose Radix For Number Display");
		nf.setValueAndRange(32, Character.MIN_RADIX, Character.MAX_RADIX);
		p.add(nf);

		JOptionPane pane = new JOptionPane(
				p,
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.DEFAULT_OPTION);
		JDialog dlg = pane.createDialog(this, "Choose Radix");

		dlg.setVisible(true);
		
		if (pane.getValue() == null)	{	// canceled
			radix.setSelectedItem(DEC_FMT_ITEM);
		}
		else	{
			int r = (int)nf.getValue();
			
			switch (r)	{
				case 2:
					radix.setSelectedItem(BIN_ITEM);
					break;
				case 8:
					radix.setSelectedItem(OCT_ITEM);
					break;
				case 10:
					radix.setSelectedItem(DEC_ITEM);
					break;
				case 16:
					radix.setSelectedItem(HEX_ITEM);
					break;
				default:
					String s = Integer.toString(r);
					boolean found = false;
					for (int i = 0; found == false && i < radix.getItemCount(); i++)	{
						Object o = radix.getItemAt(i);
						if (o.equals(s))
							found = true;
					}
					
					if (found == false)
						radix.addItem(s);
						
					radix.setSelectedItem(s);
					break;
			}
		}
	}


	private String checkDecimalNumberLength(Object result, String text, boolean removeFormatChars)	{
		StringBuffer sb = removeFormatChars ? new StringBuffer() : null;	// re-formated number
		
		// count significant positions of number
		int len = 0;
		boolean started = false, ended = false;
		for (int i = 0; i < text.length(); i++)	{	// respect decimal separator
			char c = text.charAt(i);
			boolean digit = Character.isDigit(c);
			boolean exponent = (c == 'E' || c == 'e');

			if (digit && started == false)
				started = (c != '0');	// ignore leading zeros
			else
			if (!ended && exponent)	// exponent starting
				ended = true;
			
			if (digit && started && !ended)
				len++;

			if (removeFormatChars && (digit || exponent || c == Calculator.decimalSeparator || c == Calculator.minus.charAt(0)))
				sb.append(c);
		}

		// Warn that this was rounded
		if (len > 16 && lastResult != result)	{
			lastResult = result;	// avoid repeated work
			
			JOptionPane.showMessageDialog(
					this,
					"CAUTION: Results with more than 16 digits are not precise!",
					"Warning",
					JOptionPane.WARNING_MESSAGE);
		}
		
		return removeFormatChars ? sb.toString() : text;
	}
	

	private NumberFormat ensureDecimalFormat()	{
		if (decimalFormat == null)
			decimalFormat = NumberFormat.getInstance();
		
		if (fixedFraction >= 0)
			decimalFormat.setMaximumFractionDigits(fixedFraction);
		else
			decimalFormat.setMaximumFractionDigits(Integer.MAX_VALUE);

		return decimalFormat;
	}


	private void insertText(String s)	{
		//input.insert(s, input.getCaretPosition());
		input.replaceSelection(s);
	}


	// interface KeyListener

	/** imlements KeyListener to catch "=" in TextArea. */
	public void keyTyped(KeyEvent e)	{
		checkChar(e);
	}
	/** imlements KeyListener to catch "=" in TextArea. */
	public void keyReleased(KeyEvent e)	{
		checkChar(e);
	}
	/** imlements KeyListener to catch "=" in TextArea. */
	public void keyPressed(KeyEvent e)	{
		if (checkChar(e))
			calculateAction();
	}

	private boolean checkChar(KeyEvent e)	{
		if (e.getKeyChar() == '=')	{
			e.consume();
			return true;
		}
		return false;
	}
	
	
	// interface NumberEditorListener

	/** imlements NumberEditorListener to set fraction width. */
	public void numericValueChanged(long newValue)	{
		fixedFraction = (int)newValue;
		
		if (calculator.getResult() instanceof Double)	{
			refreshOutput();
		}
	}
	
}