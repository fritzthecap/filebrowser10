package fri.gui.swing.concordance;

import java.util.*;
import java.io.*;
import java.awt.Window;
import java.awt.event.*;
import javax.swing.*;
import gnu.regexp.RE;
import fri.util.file.SortedFileCollectVisitor;
import fri.util.concordance.*;
import fri.util.observer.CancelProgressObserver;
import fri.util.os.OS;
import fri.util.regexp.RegExpUtil;
import fri.util.concordance.textfile.TextfileConcordance;
import fri.util.concordance.textfiles.TextfilesConcordance;
import fri.util.concordance.filenames.*;
import fri.gui.CursorUtil;
import fri.gui.swing.filechooser.*;
import fri.gui.swing.progressdialog.CancelProgressDialog;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.actionmanager.connector.AbstractSwingController;
import fri.gui.swing.concordance.filter.*;

public class ConcordanceController extends AbstractSwingController
{
	public static final String ACTION_OPEN = "Open";
	public static final String ACTION_CONFIGURE = "Configure";
	public static final String ACTION_START = "Start";
	public static final String FILE_CONTENTS = "Check Text File Contents";
	public static final String FILE_NAMES = "Check File Names In Directories";
	public static final String INCLUDING = "Include";
	public static final String EXCLUDING = "Exclude";
	
	private FilterModel filterModel = new FilterModel();
	private File [] files;
	private CancelProgressDialog observer;
	
	public ConcordanceController(ConcordanceView view) {
		super(view);

		getConcordanceView().getModeCombo().addItemListener(new ItemListener()	{
			public void itemStateChanged(ItemEvent e)	{
				setEnabled(ACTION_CONFIGURE, isFileContentSearch());
			}
		});
		
		getConcordanceView().getFilenamePattern().addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				Window w = getDialogParent();
				CursorUtil.setWaitCursor(w);
				cb_Start(null);
				CursorUtil.resetWaitCursor(w);
			}
		});
	}

	protected void insertActions()	{
		registerAction(ACTION_OPEN, Icons.get(Icons.openFolder), "Open Files Or Directories To Search For Concordances");
		registerAction(ACTION_CONFIGURE, Icons.get(Icons.configure), "Configure Text Filter");
		registerAction(ACTION_START, Icons.get(Icons.start), "Start Concordance Search");
	}
	
	
	/** Called by the Frame when closing. */
	public synchronized void interrupt()	{
		if (observer != null)	{
			observer.setCanceled();
		}
	}
	

	// callbacks

	/** Configure the text filter for concordance search. */
	public void cb_Configure(Object selection)	{
		// show dialog and get customized filter model as ValidityFilter
		filterModel = new TextlineFilterMvcBuilder().showAsDialog(getDialogParent());
	}

	
	/** Start the concordance search. */
	public void cb_Start(Object selection)	{
		if (files == null || files.length <= 0)
			cb_Open(null);

		if (files != null && files.length > 0)
			open(files);
	}
	
	
	/** Open button was triggered: choose files/directories according to modeCombo setting. */
	public void cb_Open(Object selection)	{
		DefaultFileChooser.setOpenMultipleFiles(true);
		DefaultFileChooser.setFileSelectionMode(
				isFileContentSearch()
					? JFileChooser.FILES_AND_DIRECTORIES
					: JFileChooser.DIRECTORIES_ONLY);
				
		try	{
			setSelectedFiles(DefaultFileChooser.openDialog(getDialogParent(), getClass()));
		}
		catch (CancelException e)	{
		}
	}

	private void setSelectedFiles(File [] files)	{
		getConcordanceView().setSelectedFiles(this.files = files);
	}
	

	public void open(final File [] files)	{
		setSelectedFiles(files);

		setWorking(true);
		
		observer = new CancelProgressDialog(getDialogParent(), "Concordance Search ...");

		Runnable work = new Runnable()	{
			public void run()	{
				try	{
					Concordance concordance = null;
					ValidityFilter filter = getFilenameValidityFilter();
		
					if (isFileContentSearch())	{	// search text concordances
						// resolve directories to all contained files
						ArrayList v = new ArrayList();
							
						for (int i = 0; files != null && i < files.length; i++)	{
							if (observer.canceled())
								return;
								
							File f = files[i];
							if (f.isDirectory())	{
								new TextFileCollectVisitor(filter, observer, f, v);
							}
							else
							if (f.isFile() && filter.isValid(f) != null)	{
								v.add(f);
							}
						}
				
						if (v.size() > 0)	{	// build concordance and render it
							File [] sortedFiles = new File [v.size()];	// no directories are contained any more
							v.toArray(sortedFiles);
							v = null;	// set free the big list of textfiles
		
							// long lasting model work: search text concordances
							concordance = (sortedFiles.length == 1)
									? new TextfileConcordance(
											sortedFiles[0],
											filterModel,
											filterModel.getBreakAfterCount(),
											filterModel.getMinimumLinesPerBlock())
									: new TextfilesConcordance(
											sortedFiles,
											filterModel,
											observer,
											filterModel.getBreakAfterCount(),
											filterModel.getMinimumLinesPerBlock());
						}
					}
					else	{	// search file name concordances in directories
						if (files != null && files.length > 0)	{
							// long lasting model work: search filename concordances
							concordance = new DirectoriesConcordance(
									files,
									filter,
									observer);
						}
					}
					
					if (observer.canceled())
						return;
						
					// long lasting model work: build concordance blocks
					System.err.println("Starting to build blocked list ...");
					List blockedList = concordance != null ? concordance.getBlockedResult(observer) : null;
		
					// long lasting GUI work: fill the result panel
					getConcordanceView().getConcordancePanel().init(blockedList, observer);
		
				}
				catch (Throwable e)	{	// memory error!
					error(e);
				}
				finally	{
					observer.endDialog();
					synchronized(ConcordanceController.this)	{
						observer = null;
					}
				}
			}
		};

		Runnable finish = new Runnable()	{
			public void run()	{
				System.err.println("running finish thread ...");
				setWorking(false);
			}
		};

		observer.start(work, finish);
		observer.getDialog();
	}



	private static class TextFileCollectVisitor extends SortedFileCollectVisitor
	{
		private ValidityFilter filter;
		private CancelProgressObserver observer;
		
		TextFileCollectVisitor(ValidityFilter filter, CancelProgressObserver observer, File dir, List list)	{
			super(list);

			this.filter = filter;
			this.observer = observer;
			
			try	{
				loop(dir);
			}
			catch (RuntimeException e)	{
				if (e.getMessage() == null || e.getMessage().equals("mine") == false)
					throw e;
			}
		}
							
		protected void visit(File f)	{
			if (observer.canceled())
				throw new RuntimeException("mine");
				
			if (filter.isValid(f) != null)
				super.visit(f);
		}
	}



	private ValidityFilter getFilenameValidityFilter()	{
		return new DefaultFilenameValidityFilter()	{
			private RE expr = OS.supportsCaseSensitiveFiles()
								? RegExpUtil.getDefaultExpression(getFilenamePattern())
								: RegExpUtil.getDefaultExpressionIgnoreCase(getFilenamePattern());
			private boolean including = isPatternIncluding();
			
			public Object isValid(Object o)	{
				String name = (String)super.isValid(o);
				if (name != null)	{
					boolean match = RegExpUtil.matchExpression(expr, name);
					if (match == false && including || match && including == false)
						return null;
				}
				return name;
			}
		};
	}
	
	
	private ConcordanceView getConcordanceView()	{
		return (ConcordanceView)getView();
	}
	
	private boolean isFileContentSearch()	{
		return getConcordanceView().getModeCombo().getSelectedItem().equals(FILE_CONTENTS);
	}

	private String getFilenamePattern()	{
		String s = getConcordanceView().getFilenamePattern().getText();
		if (s.length() <= 0)
			s = "*";
		return s;
	}

	private boolean isPatternIncluding()	{
		return getConcordanceView().getPatternIncluding().getSelectedItem().equals(INCLUDING);
	}

	private void setWorking(boolean working)	{
		setEnabled(ACTION_OPEN, working == false);
		setEnabled(ACTION_CONFIGURE, working == false && isFileContentSearch());
		setEnabled(ACTION_START, working == false);
		getConcordanceView().setWorking(working);
	}


	public boolean close()	{
		return super.close();
	}

}
