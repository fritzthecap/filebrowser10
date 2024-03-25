package fri.gui.swing.filebrowser;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import fri.util.dump.NumericDump;
import fri.util.text.encoding.Encodings;
import fri.gui.CursorUtil;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.text.*;
import fri.gui.swing.fileloader.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.IconUtil;
import fri.gui.swing.encoding.EncodingChooser;


public class FileViewer extends JFrame implements
	LoadObserver,
	FocusListener,
	ActionListener
{
	private ComfortTextArea ta;
	private JPanel panel;
	private boolean loading = false;
	private TextFileLoader loader;
	private File file;
	private long loadTime;
	private JMenuItem text, oct, dec, hex, bin;
	private JMenuItem encodingItem;
	private ButtonGroup btnGroup;
	private boolean interrupted;
	private Font oldFont = null, sysFont;
	private Document doc;
	private boolean isDump;
	private Point pos;
	private JViewport viewPort;
	private String encoding;

	
	/** Factory method to get a new FileViewer window. */
	public static FileViewer construct(File file)	{
		return construct(file, false);
	}

	/** Factory method to get a new FileViewer window, optional synchronous loading. */
	public static FileViewer construct(File file, boolean syncLoad)	{
		return new FileViewer(file, syncLoad);
	}
	

	/** New window from a text. */
	public FileViewer(String text)	{
		build();
		ta.setText(text);
		ta.setCaretPosition(0);
		setTitle("Text Viewer");
	}
	
	/**
		New window from a File object or null (empty).
		@param file File object to render or null.
	*/
	public FileViewer(File file)	{
		this(file, false);
	}

	/** New file window, optionally synchronous loading. */
	public FileViewer(File file, boolean syncLoad)	{
		init(file, syncLoad);
	}
	
	/** New window, start finder window as soon as text is loaded. */
	public FileViewer(File f,
		String pattern,
		String syntax,
		boolean ignoreCase,
		boolean wordMatch)
	{
		this(f);	// synchrone load because of search dialog
		find(pattern, syntax, ignoreCase, wordMatch);
	}



	/** Build GUI and set file contents into window. */
	protected void init(File file, boolean syncLoad)	{
		build();
		setFile(file, syncLoad);
	}
	
	
	/** Build the GUI. */
	protected void build()	{
		setTitle("File Viewer");
		IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());

		Container c = getContentPane();
		
		// we need a panel for optional progress bar
		panel = new JPanel(new BorderLayout());
		
		ta = new ComfortTextArea()	{
			public String getToolTipText(MouseEvent e)	{
				if (isDump == false)	{
					return super.getToolTipText(e);
				}
				return null;
			}
			public void paste()	{	// let no paste happen - not editable
			}
		};
		ta.setEditable(false);
		ta.setCaretColor(Color.red);
		
		JScrollPane sp = new JScrollPane(ta);
		viewPort = sp.getViewport();
		panel.add(sp);
		c.add(panel);
		
		//setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				if (loader != null)
					loader.interrupt();
				
				ta.removeFocusListener(FileViewer.this);
				
				interrupted = true;
			}
		});
		
		JPopupMenu popup = ta.getPopupMenu();

		btnGroup = new ButtonGroup();
		
		text = new JRadioButtonMenuItem("Text View");
		text.setSelected(true);
		btnGroup.add(text);
		popup.insert(text, 0);
		text.addActionListener(this);
		text.setActionCommand("0");
		
		dec = new JRadioButtonMenuItem("Decimal View");
		dec.setSelected(false);
		btnGroup.add(dec);
		popup.insert(dec, 1);
		dec.addActionListener(this);
		dec.setActionCommand("10");

		hex = new JRadioButtonMenuItem("Hexadecimal View");
		hex.setSelected(false);
		btnGroup.add(hex);
		popup.insert(hex, 2);
		hex.addActionListener(this);
		hex.setActionCommand("16");
		
		oct = new JRadioButtonMenuItem("Octal View");
		oct.setSelected(false);
		btnGroup.add(oct);
		popup.insert(oct, 3);
		oct.addActionListener(this);
		oct.setActionCommand("8");

		bin = new JRadioButtonMenuItem("Binary View");
		bin.setSelected(false);
		btnGroup.add(bin);
		popup.insert(bin, 4);
		bin.addActionListener(this);
		bin.setActionCommand("2");

		popup.insert(new JPopupMenu.Separator(), 5);
		
		encodingItem = new JMenuItem("Encoding ("+Encodings.defaultEncoding+")");
		popup.insert(encodingItem, 6);
		encodingItem.addActionListener(this);

		popup.insert(new JPopupMenu.Separator(), 7);

		ta.addFocusListener(this);
		
		new FileViewerDndListener(ta, this);

		new GeometryManager(this).show();
	}
	

	/** Implements ActionListener to change text view to hex, octal or decimal. */
	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == encodingItem)	{
			EncodingChooser chooser = new EncodingChooser(this, encoding);
			if (chooser.wasCanceled() == false)	{
				this.encoding = chooser.getEncoding();
				String displayEncoding = encoding == null ? Encodings.defaultEncoding : encoding;
				encodingItem.setText("Encoding ("+displayEncoding+")");
				setFile(file);	// reload view
			}
		}
		else
		if (file != null)	{
			reload(Integer.valueOf(e.getActionCommand()).intValue());
		}
	}
	
	private void reload(int base)	{
		if (base == 0)	{
			isDump = false;
			encodingItem.setEnabled(true);
			ta.setFont(oldFont);
			setFile(file);
		}
		else	{
			isDump = true;
			loadNumeric(base);
		}
		encodingItem.setEnabled(isDump == false);
	}
	
	
	/** Reload the textarea with converted numeric file contents. */
	protected void loadNumeric(int base)	{
		long len = file.length();
			
		if (len > 0)	{
			CursorUtil.setWaitCursor(this);
			
			try	{
				InputStream in = new BufferedInputStream(new FileInputStream(file));

				byte [] buff = new byte[len > 8192 ? 8192 : (int)len];
				int nch;
				
				ta.setText("");
				
				Document doc = ta.getDocument();
				
				while (!interrupted && (nch = in.read(buff, 0, buff.length)) != -1) {
					String dump = new NumericDump(buff, nch, base).toString();
					try	{
						doc.insertString(doc.getLength(), dump, null);
					}
					catch (BadLocationException ex)	{
					}
				}
				
				in.close();
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
			finally	{
				CursorUtil.resetWaitCursor(this);
			}
			ta.setCaretPosition(0);
		}
		
		if (oldFont == null)	{
			oldFont = ta.getFont();
			sysFont = new Font("Monospaced", Font.PLAIN, 12);
		}
			
		ta.setFont(sysFont);
	}
	
	
	/** Set a new File into this window. */
	public void setFile(File file)	{
		setFile(file, false);
	}
	
	/** Set a new File into this window, optional synchronous. */
	public void setFile(File file, boolean syncLoad)	{
		if (file == null || file.isFile() == false)	{
			//if (file != null)
			//	throw new IllegalArgumentException("File does not exist or is not normal file: "+file);
			return;
		}
		
		this.file = file;
		this.loadTime = file.lastModified();
		
		setTitle(file.getName()+" in "+file.getParent());
		
		if (isDump == false)	{
			loading = true;
			loader = new TextFileLoader(
					file,
					doc = new PlainDocument(),
					panel,
					this,
					this,
					encoding,
					encoding == null,
					encoding == null);
					
			//ta.setText("");
			ta.setDocument(doc);
			
			if (syncLoad)
				loader.run();
			else
				loader.start();
		}
		else	{	// numeric dump
			reload(Integer.valueOf(btnGroup.getSelection().getActionCommand()).intValue());
		}
	}
	
	
	/** implements LoadObserver */
	public void setLoading(boolean loading)	{
		this.loading = loading;
		
		if (loading == false)	{
			loader = null;
			
//			System.err.println("Finished loading, starting to render text ...");
//			long time = System.currentTimeMillis();
//			ta.setDocument(doc);
//			System.err.println("... Finished rendering text after "+(System.currentTimeMillis() - time)+" millis");

			if (pos != null)	{
				EventQueue.invokeLater(new Runnable()	{
					public void run()	{
						viewPort.setViewPosition(pos);
						pos = null;
					}
				});
			}
		}
	}

	
	/** Find a pattern in this window. */
	public void find(
		final String pattern,
		final String syntax,
		final boolean ignoreCase,
		final boolean wordMatch)
	{
		if (loading)	{
			final Object waitFor = this;

			Runnable r = new Runnable()	{
				public void run()	{
					synchronized(waitFor)	{
						try	{
							waitFor.wait();
						}
						catch (InterruptedException e)	{
						}
					}

					EventQueue.invokeLater(new Runnable()	{
						public void run()	{
							ta.find(pattern, syntax, ignoreCase, wordMatch);
						}
					});
				}
			};
			
			new Thread(r).start();
		}
		else	{
			ta.find(pattern, syntax, ignoreCase, wordMatch);
		}
	}

	
	// interface FocusListener
	
	/** Check if file has changed. */
	public void focusGained(FocusEvent e)	{
		if (file != null && file.lastModified() != loadTime)	{
	   	loadTime = file.lastModified();

			EventQueue.invokeLater(new Runnable()	{
				public void run()	{
					int ret = JOptionPane.showConfirmDialog(
						FileViewer.this,
						"\""+file.getName()+"\" in \""+file.getParent()+"\"\nhas changed. Reload?",
						"File Has Changed",
						JOptionPane.YES_NO_OPTION);
			
					if (ret == JOptionPane.YES_OPTION)	{
						pos = viewPort.getViewPosition();
						setFile(file);
					}
				}
			});
		}
	}
	
	public void focusLost(FocusEvent e)	{
	}
	
	
	
	// test main
	public static void main(String [] args)	{
		FileViewer fv = null;
		
		for (int i = 0; i < args.length; i++)	{
			File f = new File(args[i]);
			if (f.isFile())
				fv = new FileViewer(new File(args[i]));
			else
				System.err.println("ERROR: Not found: "+f);
		}
		
		if (fv == null)	{
			System.err.println("SYNTAX: java "+FileViewer.class.getName()+" file [file ...]");
			fv = new FileViewer((File)null);
		}
	}
	
}