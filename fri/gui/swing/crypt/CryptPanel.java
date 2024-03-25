package fri.gui.swing.crypt;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import fri.util.crypt.BASE64Decoder;
import fri.util.crypt.BASE64Encoder;
import fri.util.os.OS;
import fri.util.crypt.*;
import fri.util.dump.NumericDump;
import fri.gui.CursorUtil;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.splitpane.*;
import fri.gui.swing.text.ComfortTextArea;
import fri.gui.swing.spinnumberfield.SpinNumberField;
import fri.gui.swing.filechooser.*;

/**
	The panel that holds two textareas and actions/callbacks to drive the GUI.
	
	@author Fritz Ritzberger
*/

public class CryptPanel extends JPanel implements
	ActionListener,
	ItemListener
{
	private static final String NO_CRYPT_ALGORITHM = "(No Algorithm)";
	private static final String BASE_64_ALGORITHM = "Base 64 Encoding";
	
	private JButton newwin, encrypt, decrypt;
	private JLabel keyLabel, algorithmLabel, baseLabel;
	private JComboBox algorithm;
	private JPasswordField keyText;
	private JCheckBox bytesAsNumbers;
	private SpinNumberField numberBase;
	private ByteHoldingTextArea decrypted;
	private ByteHoldingTextArea encrypted;
	private SplitPane splitPane;
	
	
	public CryptPanel(String text)	{
		super(new BorderLayout());
		
		build();	// build GUI

		setActionsEnabled();

		if (text != null)
			decrypted.setBytes(text.getBytes());	// after installing DocumentListeners
			
		ComponentUtil.requestFocus(decrypted);
	}
	
	private void build()	{
		splitPane = new SplitPane(CryptPanel.class, JSplitPane.VERTICAL_SPLIT);

		JScrollPane spDec = new JScrollPane();
		TitledBorder decryptedBorder = BorderFactory.createTitledBorder("Decrypted Text");
		spDec.setBorder(decryptedBorder);
		spDec.setViewportView(decrypted = new ByteHoldingTextArea("Decrypted Text", decryptedBorder));
		splitPane.setTopComponent(spDec);
		
		JScrollPane spEnc = new JScrollPane();
		TitledBorder encryptedBorder = BorderFactory.createTitledBorder("Encrypted Text");
		spEnc.setBorder(encryptedBorder);
		spEnc.setViewportView(encrypted = new ByteHoldingTextArea("Encrypted Text", encryptedBorder));
		splitPane.setBottomComponent(spEnc);
		
		splitPane.setDividerLocation(0.5d);
		new SymmetryListener(splitPane);
		
		keyText = new JPasswordField();
		
		algorithm = new JComboBox();
		algorithm.addItem("IDEA");
		algorithm.addItem("AES");
		algorithm.addItem("DES3");
		algorithm.addItem("RC4");
		algorithm.addItem(BASE_64_ALGORITHM);
		algorithm.addItem(NO_CRYPT_ALGORITHM);
		
		bytesAsNumbers = new JCheckBox("As Numbers", true);
		
		numberBase = new SpinNumberField(31, 2, 32, (short)2)	{	// JDK can convert maximum base 32
			public Dimension getMaximumSize()	{
				Dimension d = super.getMaximumSize();
				d.width = 20;
				return d;
			}
		};

		JToolBar toolbar = new JToolBar();
		if (OS.isAboveJava13) toolbar.setRollover(true);
		toolbar.add(encrypt = new JButton("Encrypt"));
		encrypt.setToolTipText("Encrypt Top Text To Bottom Textarea");
		encrypt.addActionListener(this);
		toolbar.add(decrypt = new JButton("Decrypt"));
		decrypt.setToolTipText("Decrypt Bottom Text To Top Textarea");
		decrypt.addActionListener(this);
		toolbar.add(keyLabel = new JLabel(" Key: "));
		toolbar.add(keyText);
		keyText.setToolTipText("Key Phrase For Cryptographic Algorithm");
		keyText.addActionListener(this);
		keyText.getDocument().addDocumentListener(new DocumentListener()	{
			public void changedUpdate(DocumentEvent e)	{
				setActionsEnabled();
			}
			public void insertUpdate(DocumentEvent e)	{
				setActionsEnabled();
			}
			public void removeUpdate(DocumentEvent e)	{
				setActionsEnabled();
			}
		});
		toolbar.add(algorithmLabel = new JLabel(" Algorithm: "));
		toolbar.add(algorithm);
		algorithm.addItemListener(this);
		algorithm.setToolTipText("Choose An Cryptographic Algorithm");
		// Cipher Block Chaining - checkbox for block ciphers (Idea, Des3)
		toolbar.add(new JSeparator(SwingConstants.VERTICAL));
		toolbar.add(bytesAsNumbers);
		bytesAsNumbers.addActionListener(this);
		bytesAsNumbers.setToolTipText("Interpret Bytes As Numbers in Bottom Textarea");
		toolbar.add(baseLabel = new JLabel("With Base "));
		toolbar.add(numberBase);
		numberBase.setToolTipText("Radix For Numbers in Bottom Textarea");
		toolbar.add(new JSeparator(SwingConstants.VERTICAL));
		toolbar.addSeparator();
		toolbar.add(newwin = new JButton("New Window"));
		newwin.addActionListener(this);
		newwin.setToolTipText("Open New Window");

		add(toolbar, BorderLayout.NORTH);
		add(splitPane, BorderLayout.CENTER);
	}

		
	// interface ActionListener

	public void actionPerformed(ActionEvent e)	{
		CursorUtil.setWaitCursor(this);

		try	{
			if (e.getSource() == newwin)	{
				new CryptFrame();
			}
			else
			if (e.getSource() == bytesAsNumbers)	{
				numberBase.setEnabled(bytesAsNumbers.isSelected());
				baseLabel.setEnabled(numberBase.isEnabled());
			}
			else
			if (e.getSource() == keyText)	{
				if (encrypted.isEnabled())
					encrypt();
				else
				if (decrypted.isEnabled())
					decrypt();
			}
			else
			if (e.getSource() == decrypt)	{
				decrypt();
			}
			else
			if (e.getSource() == encrypt)	{
				encrypt();
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}


	/** Implements ItemListener to set key combo disabled when "None" algorithm gets chosen. */
	public void itemStateChanged(ItemEvent e)	{
		setActionsEnabled();
	}
	

	private void clearOtherTextArea(ByteHoldingTextArea thisTextArea)	{
		ByteHoldingTextArea other = (thisTextArea == decrypted) ? encrypted : decrypted;
		other.setBytes(null);
	}
	
	// enable actions when some event changes GUI state
	private void setActionsEnabled()	{
		boolean hasEncryptableText = decrypted.getDocument().getLength() > 0;
		boolean hasDecryptableText = encrypted.getDocument().getLength() > 0;

		boolean hasCryptableText = hasEncryptableText || hasDecryptableText;
		
		Crypt crypt = new Crypt("test", getSelectedAlgorithm());
		boolean needCryptKey = hasCryptableText && crypt.hasValidAlgorithm();

		keyText.setEnabled(needCryptKey);
		keyLabel.setEnabled(needCryptKey);

		algorithm.setEnabled(hasCryptableText);
		algorithmLabel.setEnabled(algorithm.isEnabled());

		boolean canConvert = getKeyText().trim().length() > 0 || needCryptKey == false;

		encrypt.setEnabled(hasEncryptableText && canConvert);
		decrypt.setEnabled(hasDecryptableText && canConvert);

		bytesAsNumbers.setEnabled(hasCryptableText && canConvert && getSelectedAlgorithm().equals(BASE_64_ALGORITHM) == false);
		numberBase.setEnabled(bytesAsNumbers.isSelected() && bytesAsNumbers.isEnabled());
		baseLabel.setEnabled(numberBase.isEnabled());
	}


	private String getSelectedAlgorithm()	{
		return algorithm.getSelectedItem().toString();
	}
	
	private String getKeyText()	{
		return new String(keyText.getPassword());
	}
	
	private void encrypt()	{
		try	{
			byte [] bytes = decrypted.getBytes();
			Crypt enc = new Crypt(getKeyText(), getSelectedAlgorithm());
			if (enc.hasValidAlgorithm())
				bytes = enc.getBytes(bytes, true);
			setEncryptedBytes(bytes);
		}
		catch (IOException e)	{
			error(e);
		}
	}

	private void decrypt()	{
		try	{
			byte [] bytes = getEncryptedBytes();
			Crypt dec = new Crypt(getKeyText(), getSelectedAlgorithm());
			if (dec.hasValidAlgorithm())
				bytes = dec.getBytes(bytes, false);
			decrypted.setBytes(bytes);
		}
		catch (IOException e)	{
			error(e);
		}
	}
	
	private byte [] getEncryptedBytes()	{
		if (getSelectedAlgorithm().equals(BASE_64_ALGORITHM))	{
			BASE64Decoder decoder = new BASE64Decoder();
			try	{
				return decoder.decodeBuffer(encrypted.getText());
			}
			catch (IOException e)	{
				error(e);
			}
		}
		else
		if (bytesAsNumbers.isSelected())	{
			try	{
				return NumericDump.fromNumberString(encrypted.getText(), (int)numberBase.getValue());
			}
			catch (NumberFormatException e)	{
				error(e);
			}
		}
		else	{
			return encrypted.getBytes();
		}
		return null;	// was exception
	}

	private void setEncryptedBytes(byte [] bytes)	{
		if (getSelectedAlgorithm().equals(BASE_64_ALGORITHM))	{
			BASE64Encoder decoder = new BASE64Encoder();
			encrypted.setText(decoder.encodeBuffer(bytes));
		}
		else
		if (bytesAsNumbers.isSelected())	{
			encrypted.setText(NumericDump.toNumberString(bytes, (int)numberBase.getValue(), 23));	// linelength 23
			encrypted.renderByteLength(new Integer(bytes.length));
		}
		else	{
			encrypted.setBytes(bytes);
		}
	}

	
	private void error(Exception e)	{
		e.printStackTrace();
		JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	/** Save combo strings and store splitpane divider location. */
	public void close()	{
		splitPane.close();
	}





	/**
		Let set and get bytes, buffer them, as String conversion might change them.
	*/
	class ByteHoldingTextArea extends ComfortTextArea implements
		DocumentListener
	{
		private byte [] bytes;
		private JMenuItem clear, open, save, selectAll;
		private String tooltip, title;
		private TitledBorder border;
		private File file;
		
		
		ByteHoldingTextArea(String title, TitledBorder border)	{
			this.tooltip = title;
			this.title = title;
			this.border = border;
			getDocument().addDocumentListener(this);
			setFont(new Font("Monospaced", Font.PLAIN, 12));
			
			new CryptDndPerformer(this);
		}
		
		public String getToolTipText(MouseEvent e)	{
			return tooltip;
		}
		
		public void setBytes(byte [] bytes)	{
			// keep order: DocumentListener is installed!
			setText(bytes != null ? new String(bytes) : "");
			setCaretPosition(0);
			this.bytes = bytes;
			renderByteLength(bytes != null ? new Integer(bytes.length) : null);
		}
	
		public void renderByteLength(Integer len)	{
			border.setTitle(title+(len != null && len.intValue() > 0 ? ": "+len+" Bytes" : ""));
		}
		
		public byte [] getBytes()	{
			return bytes != null ? bytes : getText().getBytes();
		}

		/** Add Menuitem "Clear". */
		protected void addFind()	{
			popup.add(open = new JMenuItem("Open File"));
			open.addActionListener(this);
			popup.add(save = new JMenuItem("Save To File"));
			save.setEnabled(false);
			save.addActionListener(this);
			popup.addSeparator();
			popup.add(selectAll = new JMenuItem("Select All"));
			selectAll.addActionListener(this);
			popup.add(clear = new JMenuItem("Clear"));
			clear.addActionListener(this);
			popup.addSeparator();

			super.addFind();
		}

		protected void addGoLine()	{	// no goto line dialog
		}

		protected void addTabWidth()	{	// no tab width choice
		}
		
	
		/** Implements KeyListener: Ctl-O open file, Ctl-S save file. */	
		public void keyPressed(KeyEvent e)	{
			if (e.getKeyCode() == KeyEvent.VK_O && e.isControlDown())
				open();
			else
			if (e.getKeyCode() == KeyEvent.VK_S && e.isControlDown())
				save();
			else
				super.keyPressed(e);
		}

		
		/** Implements DocumentListener to set buttons enabled/disabled. */	
		public void changedUpdate(DocumentEvent e)	{
			changed();
		}
		/** Implements DocumentListener to set buttons enabled/disabled. */	
		public void insertUpdate(DocumentEvent e)	{
			changed();
		}
		/** Implements DocumentListener to set buttons enabled/disabled. */	
		public void removeUpdate(DocumentEvent e)	{
			changed();
		}
		
		private void changed()	{
			setActionsEnabled();	// this call is the reason why this class is an inner class
			this.bytes = null;
			save.setEnabled(true);
		}


		/** Respond to menuitems. */
		public void actionPerformed(ActionEvent e)	{
			if (e.getSource() == open)	{
				open();
				clearOtherTextArea(this);
			}
			else
			if (e.getSource() == save)	{
				save();
			}
			else
			if (e.getSource() == selectAll)	{
				select(0, getDocument().getLength());
			}
			else
			if (e.getSource() == clear)	{
				setBytes(null);
			}
			else	{
				super.actionPerformed(e);
			}
		}

		private void open()	{
			try	{
				DefaultFileChooser.setOpenMultipleFiles(false);
				File [] files = DefaultFileChooser.openDialog(ByteHoldingTextArea.this, CryptPanel.class);
				
				if (files != null && files.length > 0)	{
					open(files[0]);
				}
			}
			catch (CancelException ex)	{
			}
		}
		
		/** Loads the passed file. For use in Drag&Drop. */
		public void open(File file)	{
			InputStream bin = null;
			try {
				CursorUtil.setWaitCursor(this);

				byte [] buf = new byte[(int)file.length()];
				bin = new BufferedInputStream(new FileInputStream(file));
				bin.read(buf);

				setBytes(buf);
				save.setEnabled(true);
				this.file = file;
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
			finally	{
				CursorUtil.resetWaitCursor(this);
				try	{ bin.close(); }	catch (IOException ex)	{}
			}
		}
	
		private void save()	{
			if (file != null)	{	// set suggested chooser file if exists
				DefaultFileChooser.setChooserFile(file);
			}
	
			FileGUISaveLogicImpl impl = new FileGUISaveLogicImpl(ByteHoldingTextArea.this)	{
				public void write(Object toWrite)
					throws Exception
				{
					OutputStream bout = null;
					CursorUtil.setWaitCursor(CryptPanel.this);
					try {
						byte [] text = getBytes();
						bout = new BufferedOutputStream(new FileOutputStream((File)toWrite));
						bout.write(text);
					}
					finally	{
						CursorUtil.resetWaitCursor(CryptPanel.this);
						try	{ bout.close(); }	catch (IOException ex)	{}
					}
				}
			};
			
			SaveLogic.save(impl, null);	// null: always confirm writing to a file
		}

	}

}