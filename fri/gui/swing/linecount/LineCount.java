package fri.gui.swing.linecount;

import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.text.NumberFormat;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import fri.util.os.OS;
import fri.util.regexp.RegExpUtil;
import fri.util.io.NewlineDetectingInputStreamReader;
import fri.gui.CursorUtil;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.dnd.*;
import fri.gui.swing.table.sorter.*;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.progressdialog.*;
import fri.gui.swing.IconUtil;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.table.*;
import fri.util.text.CommentStrip;

/**
	Line counting panel (and window), showing results in a table.
	Counting happens interruptable in background.
*/

public class LineCount extends JPanel implements
	DndPerformer,
	Runnable,
	ActionListener,
	ListSelectionListener
{
	private static JFileChooser fileChooser;
	protected JTable table;
	private JTextField sumLabel;
	private JFrame frame;
	private long fileCount;
	private long lineCount;
	private long charCount;
	private File [] files;
	private CancelProgressDialog progress;
	private DefaultTableModel model;
	private LineCountFilterComboBox filterText;
	private JComboBox filterInclude;
	private Object expr;
	private boolean isInclude, alwaysMatches;
	private JButton open, filterBtn;
	private NumberFormat numberFormat;
	private JCheckBox cbStripComments;
	private JCheckBox cbIgnoreEmptyLines;
	private boolean ignoreEmptyLines = false;
	private boolean stripComments = false;
	
	
	private static final Vector columns = new Vector(4);
	static	{
		columns.add("File");
		columns.add("Line Count");
		columns.add("Character Count");
		columns.add("Directory");
	}
	
	
	/**
		Create a LineCount frame.
	*/
	public LineCount(File [] files)	{
		super(new BorderLayout());

		build();
		showInFrame();
		
		if (files != null)
			init(files);
	}
	

	
	private JFrame showInFrame()	{
		frame = new JFrame("Line Count");
		IconUtil.setFrameIcon(frame, GuiApplication.getApplicationIconURL());

		frame.getContentPane().add(this, BorderLayout.CENTER);
		
		new GeometryManager(frame).show();
		
		frame.addWindowListener(new WindowAdapter()	{
			public void windowClosing(WindowEvent e)	{
				synchronized(LineCount.this)	{
					if (progress != null)	{
						progress.setCanceled();
						progress.endDialog();
					}
				}
				filterText.save();
				PersistentColumnsTable.store(table, LineCount.class);
			}
		});
		
		return frame;
	}


	protected void build()	{
		JToolBar tb = new JToolBar(SwingConstants.HORIZONTAL);
		tb.setFloatable(false);
		if (OS.isAboveJava13) tb.setRollover(true);

		open = new JButton(Icons.get(Icons.openFolder));
		open.setToolTipText("Choose Files And Directories For Line Counting");
		open.addActionListener(this);
		tb.add(open);
		tb.addSeparator();

		filterBtn = new JButton(Icons.get(Icons.start));
		tb.add(filterBtn);
		filterBtn.addActionListener(this);
		filterBtn.setToolTipText("Start Line Counting");

		filterText = new LineCountFilterComboBox();
		if (filterText.getDataVector().size() <= 0)	{
			filterText.addItem("*");
			filterText.addItem("*.java");
			filterText.addItem("*.java|*.properties");
			filterText.addItem("*.java|*.properties|*.xml");
		}
		filterText.setToolTipText("Filename Filter For Line Count");
		filterText.addActionListener(this);
		tb.add(filterText);

		filterInclude = new JComboBox();
		tb.add(filterInclude);
		filterInclude.addItem("Include");
		filterInclude.addItem("Exclude");
		filterInclude.setToolTipText("Filter Works In- Or Excluding");

		tb.addSeparator();
		tb.add(Box.createHorizontalGlue());

		cbIgnoreEmptyLines = new JCheckBox("Ignore Empty Lines");
		cbIgnoreEmptyLines.setToolTipText("Count Only Non-Empty Lines");
		tb.add(cbIgnoreEmptyLines);
		cbStripComments = new JCheckBox("Strip C-Style Comments");
		cbStripComments.setToolTipText("Strip \"/* */\" And \"//\" Comments");
		tb.add(cbStripComments);

		add(tb, BorderLayout.NORTH);

		table = new JTable(new Vector(), columns);
		JScrollPane sp = new JScrollPane(table);
		add(sp, BorderLayout.CENTER);

		add(sumLabel = new JTextField(" "), BorderLayout.SOUTH);
		sumLabel.setEditable(false);

		new DndListener(this, table);
		new DndListener(this, sp.getViewport());	// when table is empty
	}
	

	private void initColumnWidth()	{
		if (PersistentColumnsTable.load(table, LineCount.class) == false)	{
			TableColumn column;
			column = table.getColumnModel().getColumn(0);
			column.setPreferredWidth(70);
			column = table.getColumnModel().getColumn(1);
			column.setPreferredWidth(20);
			column = table.getColumnModel().getColumn(2);
			column.setPreferredWidth(20);
			column = table.getColumnModel().getColumn(3);
			column.setPreferredWidth(120);
		}
	}
	

	/**
		Set a new set of files for line counting.
		Counting happens interruptable in background.
	*/
	public void init(File [] files)	{
		this.files = files;

		fileCount = lineCount = charCount = 0;

		model = new UneditableTableModel(new Vector(), columns);

		table.getSelectionModel().removeListSelectionListener(this);

		TableSorter sorter = new TableSorter(model, this);
		table.setModel(sorter);
		sorter.addMouseListenerToHeaderInTable(table);

		table.getSelectionModel().addListSelectionListener(this);

		initColumnWidth();

		expr = RegExpUtil.getDefaultExpressionIgnoreCase(filterText.getText());
		isInclude = filterInclude.getSelectedItem().equals("Include");
		alwaysMatches = RegExpUtil.alwaysMatches(filterText.getText(), null);
		
		ignoreEmptyLines = cbIgnoreEmptyLines.isSelected();
		stripComments = cbStripComments.isSelected();

		progress = new CancelProgressDialog(frame, "Counting Lines ...");
		progress.start(this);
	}


	/** Implements Runnable to count in background. */
	public void run()	{
		// wait for frame to show
		while (frame.isShowing() == false)	{
			try	{ Thread.sleep(500); }	catch (InterruptedException e)	{}
		}
		CursorUtil.setWaitCursor(frame);
		try	{
			setEnabledStates(false);
			progress.setNote("Starting ...");
	
			// start with long lasting work
			loopFiles(files);
			
			setSumText();
			
			synchronized(this)	{
				progress.endDialog();
				progress = null;
			}
			
			setEnabledStates(true);
		}
		finally	{
			CursorUtil.resetWaitCursor(frame);
		}
	}


	
	private void loopFiles(File [] files)	{
		if (progress.canceled())
			return;
			
		for (int i = 0; progress.canceled() == false && files != null && i < files.length; i++)	{
			if (files[i].isDirectory())	{
				File [] dirContent = files[i].listFiles();
				loopFiles(dirContent);
			}
			else	{
				boolean match = alwaysMatches || RegExpUtil.matchExpression(expr, files[i].getName());
				boolean doit = match && isInclude || !match && !isInclude;
				
				if (doit)	{
					TableRowFile row = new TableRowFile(files[i]);
					if (progress.canceled() == false)
						addLineCountRow(row);
				}
			}
		}
	}


	private void setEnabledStates(final boolean enable)	{
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				open.setEnabled(enable);
				filterBtn.setEnabled(enable);
				filterText.setEnabled(enable);
				filterInclude.setEnabled(enable);
			}
		});
	}
	
	private void setSumText()	{
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				setSumTextInEventThread();
			}
		});
	}

	private void setSumTextInEventThread()	{
		setSumTextInEventThread(getSumText());
	}

	private void setSumTextInEventThread(String text)	{
		sumLabel.setText(text);
	}

	private String getSumText()	{
		return
				getFormattedText(lineCount)+" Line(s), "+
				getFormattedText(charCount)+" Character(s) in "+
				getFormattedText(fileCount)+" File(s)";
	}

	private String getFormattedText(long l)	{
		if (numberFormat == null)
			numberFormat = NumberFormat.getInstance();
		return numberFormat.format(l);
	}

	
	private void addLineCountRow(final Vector row)	{
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				model.addRow(row);
				setSumTextInEventThread();
			}
		});
	}





	// Insertable row data for TableModel
	private class TableRowFile extends Vector
	{
		File file;
		long lines;
		long chars;
		
		TableRowFile(File f)	{
			super(columns.size());
			
			this.file = f;
			
			add(f.getName());
			add(lineCount(f));	// call linecount
			add(new Long(chars));	// chars are now valid
			
			String s = f.getParent();
			add(s != null ? s : "");
		}
		
		private Long lineCount(File f)	{
			//System.err.println("Counting lines of file "+file);
			long len = f.length();

			BufferedReader r = null;
			
			try	{
				NewlineDetectingInputStreamReader in = new NewlineDetectingInputStreamReader(new FileInputStream(f));
				r = new BufferedReader(in);

				if (stripComments)	{
					StringWriter sw = new StringWriter((int)len);
					//new CStyleCommentStrip(r, sw);	// both get closed
					CommentStrip.stripComments(r, sw);
					r.close();
					sw.close();
					r = new BufferedReader(new StringReader(sw.toString()));
				}

				String line;

				while ((line = r.readLine()) != null)	{
					if (progress.canceled())
						return new Long(-1);
					
					if (ignoreEmptyLines == false || line.trim().length() > 0)	{
						lines++;
						chars += line.length() + in.getNewline().length();
					}
				}
				
				if (chars > 0 && in.endedWithNewline() == false)
					chars -= in.getNewline().length();
				
				fileCount++;
				lineCount += lines;
				charCount += chars;

				progress.setNote(f.getPath());	// show immediately
				//System.err.println("Line count is "+i+" for file "+f);
				return new Long(lines);
			}
			catch (Exception e)	{
				//JOptionPane.showMessageDialog(frame, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
			finally	{
				try	{ r.close(); } catch (Exception e) {}
			}
			
			return new Long(-1);
		}
		
	}
	
	

	// Interface ListSelectionListener
	
	public void valueChanged(ListSelectionEvent e)	{
		if (e.getValueIsAdjusting())
			return;
		
		String baseText = getSumText();	// all files sum
		
		long sum = 0L, sumChars = 0L;
		
		TableRowFile [] rows = getSelectedTableRowFiles();
		for (int i = 0; rows != null && i < rows.length; i++)	{
			sum += rows[i].lines;
			sumChars += rows[i].chars;
		}
		
		String selText = (rows == null) ?
				"" :
				"  -  "+
					getFormattedText(sum)+" Line(s), "+
					getFormattedText(sumChars)+" Character(s) in "+
					getFormattedText(rows.length)+" Selected File(s)";
		
		setSumTextInEventThread(baseText+selText);
	}


	protected File [] getSelectedFiles()	{
		Vector files = getSelected(true);
		if (files == null)
			return null;
		
		File [] farr = new File[files.size()];
		files.copyInto(farr);
		
		return farr;
	}

	private TableRowFile [] getSelectedTableRowFiles()	{
		Vector files = getSelected(false);
		if (files == null)
			return null;
		
		TableRowFile [] farr = new TableRowFile[files.size()];
		files.copyInto(farr);
		
		return farr;
	}

	private Vector getSelected(boolean returnTypeFile)	{
		int [] selectedRows = table.getSelectedRows();
		if (selectedRows == null || selectedRows.length <= 0)
			return null;
			
		Vector data = model.getDataVector();
		TableSorter sorter = (TableSorter)table.getModel();
		Vector files = new Vector();
		
		for (int i = 0; i < selectedRows.length; i++)	{
			int modelIndex = sorter.convertRowToModel(selectedRows[i]);
			TableRowFile row =(TableRowFile)data.get(modelIndex);
			//System.err.println("selected row file = "+row.file);

			if (returnTypeFile)
				files.add(row.file);
			else
				files.add(row);
		}
		
		return files;
	}

	

	/** Implements ActionListener to respond to toolbar actions. */
	public void actionPerformed(ActionEvent e)	{
		if (progress != null)
			return;
		
		if (e.getSource() == open)	{
			File [] files = chooseFiles();
			if (files != null && files.length > 0)
				this.files = files;
				//init(files);
		}
		else
		if (e.getSource() == filterText || e.getSource() == filterBtn)	{
			filterText.commit();
			init(files);
		}
	}


	private File [] chooseFiles()	{
		CursorUtil.setWaitCursor(this);
		File [] files = null;
		try	{
			if (fileChooser == null)	{
				File dir = files != null && files.length > 0 ? files[0] : new File(System.getProperty("user.dir"));
				fileChooser = new JFileChooser(dir);
				fileChooser.setMultiSelectionEnabled(true);
				fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			}
	
			int ret = fileChooser.showOpenDialog(this);
			
			if (ret == JFileChooser.APPROVE_OPTION) {
				files = fileChooser.getSelectedFiles();
			}
			fileChooser.cancelSelection();	// reuse dialog. JFileChooser stores APPROVE_OPTION !
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
		
		return files;
	}
	
	
	
	// Interface DndPerformer

	/** Implements DndPerformer, sends nothing. */
	public Transferable sendTransferable()	{
		return null;
	}

	/** Implements DndPerformer, receives File(s). */
	public boolean receiveTransferable(Object data, int action, Point p)	{
		try	{
			List fileList = (List)data;
			
			if (fileList.size() > 0)	{
				Iterator iterator = fileList.iterator();
				File [] files = new File [fileList.size()];
				for (int i = 0; iterator.hasNext(); i++) {
					files[i] = (File)iterator.next();
				}
	
				init(files);
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		return false;
	}
	
	/** Implements DndPerformer to show drag cursor. */
	public boolean dragOver(Point p)	{
		return true;
	}

	/** Implements DndPerformer to accept File(s). */
	public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
		return DataFlavor.javaFileListFlavor;
	}

	public void actionCanceled()	{}
	public void dataCopied()	{}
	public void dataMoved()	{}
	public void startAutoscrolling()	{}
	public void stopAutoscrolling()	{}



	//** Test main, counting lines of all files in argument list. */
	public static void main(String [] args)	{
		File[] files = null;
		
		if (args.length > 0)	{
			files = new File [args.length];
			
			for (int i = 0; i < args.length; i++)
				files[i] = new File(args[i]);
		}
		else	{
			System.err.println("SYNTAX: java "+LineCount.class.getName()+" file [file ...]");
			System.err.println("	Counting lines of text files.");
		}
		
		new LineCount(files);
	}

}




class LineCountFilterComboBox extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist = new Vector();	// alle haengen zusammen
	private static File globalFile = null;


	public LineCountFilterComboBox()	{
		this(new File(HistConfig.dir()+"LineCountFilter.list"));
	}

	/** Anlegen einer SearchComboBox. @param f file aus dem die Strings zu lesen sind. */
	public LineCountFilterComboBox(File f)	{
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