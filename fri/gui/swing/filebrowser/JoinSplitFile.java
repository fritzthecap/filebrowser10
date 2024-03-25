package fri.gui.swing.filebrowser;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.progressdialog.*;
import fri.util.file.*;
import fri.util.observer.CancelProgressContinueObserver;

/**
	Provides arguments for FileSplit or FileJoin and a
	CancelProgressContinueObserver to control disk changes.
	<p>
	FileSplit: splitSize[M|K] sourceFile [targetDirectory]<br>
	FileJoin: sourceDirectory [targetDirectory]
	<p>
	Both need a "Yes - Yes To All - Cancel" dialog and a
	progress renderer.
*/

public class JoinSplitFile implements ActionListener
{
	private JFrame frame;
	private HistCombo splitSizeCombo, sourceCombo, targetCombo;
	private JButton chooseSource, chooseTarget;
	private JButton ok;
	private NetNode source, target;
	private boolean isSplit;
	private JoinSplitObserver dlg;


	/**
		Start a file split or join workflow.
		@param node source directory for join, source file for split
		@param isSplit false for join, true for split
	*/
	public JoinSplitFile(NetNode node, boolean isSplit)	{
		this(node, null, isSplit);
	}
	
	/**
		Start a file split or join workflow.
		@param node source directory for join, source file for split
		@param targetDir directory where the split files or the joined file should reside
		@param isSplit false for join, true for split
	*/
	public JoinSplitFile(NetNode node, NetNode targetDir, boolean isSplit)	{
		this.isSplit = isSplit;
		this.source = node;
		this.target = targetDir;
	}


	private JPanel createPanel()	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.add(Box.createVerticalGlue());

		sourceCombo = new SplitSourceCombo();
		sourceCombo.addActionListener(this);
		targetCombo = new SplitTargetCombo();
		targetCombo.addActionListener(this);
		
		if (isSplit == false)	{	// swap combos
			HistCombo h = sourceCombo;
			sourceCombo = targetCombo;
			targetCombo = h;
		}
		
		if (source != null)
			sourceCombo.setText(source.getFullText());
				
		if (target != null)
			targetCombo.setText(target.getFullText());
		else
			if (isSplit)	{
				if (source != null)
					targetCombo.setText(FileSplit.makeSplitDir((File)source.getObject()).toString());
				else
				if (targetCombo.getText().length() <= 0)
					targetCombo.setText(FileSplit.makeSplitDir(new File(FileJoin.getDefaultTargetDirectory(), "xxx")).toString());
			}
			else	{
				targetCombo.setText(FileJoin.getDefaultTargetDirectory().toString());
			}
		
		chooseSource = new JButton("Source "+(isSplit ? "File" : "Directory"));
		chooseSource.addActionListener(this);
		chooseTarget = new JButton("Target Directory");
		chooseTarget.addActionListener(this);
		
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
		p1.add(chooseSource);
		p1.add(sourceCombo);

		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS));
		p2.add(chooseTarget);
		p2.add(targetCombo);

		JPanel p3 = new JPanel(new BorderLayout());
		p3.setLayout(new BoxLayout(p3, BoxLayout.Y_AXIS));
		p3.add(p1);
		p3.add(p2);
		
		p.add(p3);

		if (isSplit)	{
			splitSizeCombo = new SplitSizeCombo();
			splitSizeCombo.addActionListener(this);
			((JTextField)splitSizeCombo.getTextEditor()).setDocument(new SplitSizeDocument());
			
			if (splitSizeCombo.getText().length() <= 0)	// set default floppy size
				splitSizeCombo.setText(FileSplit.FLOPPY_SIZE);
				
			JPanel p4 = new JPanel();
			p4.add(new JLabel("Split Size: "));
			p4.add(splitSizeCombo);

			p.add(p4);
		}
		
		p.add(Box.createVerticalGlue());
		return p;
	}


	private JPanel createButtons()	{
		ok = new JButton("Start");
		ok.addActionListener(this);
		JPanel p = new JPanel();
		p.add(ok);
		return p;
	}


	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == ok ||
				e.getSource() == sourceCombo ||
				e.getSource() == targetCombo ||
				splitSizeCombo != null && e.getSource() == splitSizeCombo)
		{
			doit();
		}
		else
		if (e.getSource() == chooseSource)	{
			String src = sourceCombo.getText();

			File [] files;
			if (isSplit)	{
				files = FileChooser.showFileDialog(
						"Split - Source File",
						frame,
						source.getRoot(),
						src.length() > 0 ? new File(src) : null,
						true);	// single select
			}
			else	{
				files = FileChooser.showDirectoryDialog(
						"Join - Source Directory",
						frame,
						source.getRoot(),
						src.length() > 0 ? new File(src) : null,
						true);
			}

			if (files != null) {
				sourceCombo.setText(files[0].getPath());
			}
		}
		else
		if (e.getSource() == chooseTarget)	{
			String tgt = targetCombo.getText();
			
			File [] files = FileChooser.showDirectoryDialog(
					(isSplit ? "Split" : "Join")+" - Target Directory",
					frame,
					source.getRoot(),
					tgt.length() > 0 ? new File(tgt) : null,
					true);
					
			if (files != null) {
				targetCombo.setText(files[0].getPath());
			}
		}
	}
	

	private void close()	{
		if (splitSizeCombo != null)
			splitSizeCombo.save();
		sourceCombo.save();
		targetCombo.save();
		//frame.dispose();
	}
	
	
	private void doit()	{
		String src = sourceCombo.getText();
		String tgt = targetCombo.getText();

		try	{
			File srcFile = new File(src);
			if (isSplit && srcFile.isFile() == false)
				throw new IOException("File not found: "+srcFile);
					
			Runnable r;

			dlg = new JoinSplitObserver(
					frame,
					(isSplit ? "Split " : "Join ")+srcFile,
					isSplit ? srcFile.length() : 0L);
					
			if (isSplit)	{
				int size = FileSplit.splitSizeFromString(splitSizeCombo.getText());
				final FileSplit fs = new FileSplit(srcFile, new File(tgt), size, dlg);
				r = new Runnable()	{
					public void run()	{
						try	{
							fs.split();
						}
						catch (Exception e)	{
							error(e.getMessage());
						}
					}
				};
			}
			else	{
				final FileJoin fj = new FileJoin(new File(src), new File(tgt), dlg);
				r = new Runnable()	{
					public void run()	{
						try	{
							fj.join();
						}
						catch (Exception e)	{
							error(e.getMessage());
						}
					}
				};
			}
				
			dlg.start(r);
		}
		catch (Exception e)	{
			error(e.getMessage());
			e.printStackTrace();
		}
	}
	

	private void error(final String s)	{
		// must be able to be called from a background-thread
		if (SwingUtilities.isEventDispatchThread())	{
			renderError(s);
		}
		else	{
			try	{
				SwingUtilities.invokeAndWait(new Runnable()	{
					public void run()	{
						renderError(s);
					}
				});
			}
			catch (Exception e)	{
			}
		}
	}

	private Component getDialogParent()	{
		return dlg != null && dlg.getDialog() != null ? (Component)dlg.getDialog() : (Component)frame;
	}
	
	private void renderError(String s)	{
		JOptionPane.showMessageDialog(
				getDialogParent(),
				s,
				"Error",
				JOptionPane.ERROR_MESSAGE);
	}
	

	public JFrame showInFrame()	{
		frame = new JFrame(isSplit ? "Split File" : "Join To File");
		frame.getContentPane().add(createPanel(), BorderLayout.CENTER);
		frame.getContentPane().add(createButtons(), BorderLayout.SOUTH);
		frame.addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				close();
			}
		});
		new GeometryManager(frame, false).show();
		return frame;
	}


	
	private class JoinSplitObserver extends CancelProgressDialog implements
		CancelProgressContinueObserver
	{
		private int dialogAnswer;
		
		public JoinSplitObserver(Component pnt, String label, long size)	{
			super(pnt, label, size);
		}
		
		/**
			Prompt the user for continue confirmation. 
		*/
		public boolean askContinue(final String msg)	{
			dialogAnswer = JOptionPane.YES_OPTION - 1;
			
			try	{
				SwingUtilities.invokeAndWait(new Runnable()	{
					public void run()	{
						dialogAnswer = JOptionPane.showConfirmDialog(
								getDialogParent(),
								msg+(isSplit ? "" : "\n\n"+"Press \"No\" if finished.\nStatus is: "+getNote()),
								"Question",
								JOptionPane.YES_NO_OPTION);
					}
				});
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
			
			System.err.println("answered with yes: "+(dialogAnswer == JOptionPane.YES_OPTION));
			return dialogAnswer == JOptionPane.YES_OPTION;
		}
	}
	


	private class SplitSizeDocument extends PlainDocument
	{
		public void insertString(int offs, String str, AttributeSet a) 
			throws BadLocationException
		{
			char[] source = str.toCharArray();
			char[] result = new char[source.length];
			int j = 0;
			
			// check each character to be digit
			for (int i = 0; i < result.length; i++) {
				if (source[i] == '.' || source[i] == 'M' || source[i] == 'K' ||
						Character.isDigit(source[i]))
				{
					result[j++] = source[i];
				}
				else {
					Toolkit.getDefaultToolkit().beep();
					return;
				}
			}
			
			super.insertString(offs, new String(result, 0, j), a);
		}
	}
	

	
	
	/**
		Application Main
	*/
	public static void main(String [] args)	{
		boolean isSplit = false;
		if (args.length >= 1 &&
				(args[0].toLowerCase().equals("split") || args[0].toLowerCase().equals("join")))
		{
			isSplit = args[0].toLowerCase().equals("split");
		}
		else	{
			System.err.println("SYNTAX: java fri.gui.swing.filebrowser.JoinSplitFile join|split [sourceDir [targetDir]]");
		}
		new JoinSplitFile(FileNode.constructRoot((String)null), isSplit).showInFrame();
	}
	
}


class SplitSourceCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile = null;


	public SplitSourceCombo()	{
		this(new File(HistConfig.dir()+"SplitSourceCombo.list"));
	}
	
	/** Anlegen einer SearchComboBox.
		@param f file aus dem die Datei-Patterns zu lesen sind. */
	public SplitSourceCombo(File f)	{
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


class SplitTargetCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile = null;


	public SplitTargetCombo()	{
		this(new File(HistConfig.dir()+"SplitTargetCombo.list"));
	}
	
	/** Anlegen einer SearchComboBox.
		@param f file aus dem die Datei-Patterns zu lesen sind. */
	public SplitTargetCombo(File f)	{
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


class SplitSizeCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile = null;


	public SplitSizeCombo()	{
		this(new File(HistConfig.dir()+"SplitSizeCombo.list"));
	}
	
	/** Anlegen einer SearchComboBox.
		@param f file aus dem die Datei-Patterns zu lesen sind. */
	public SplitSizeCombo(File f)	{
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