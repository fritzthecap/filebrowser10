package fri.gui.swing.commandmonitor;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import fri.util.FileUtil;
import fri.util.ArrayUtil;
import fri.util.sort.quick.*;
import fri.util.os.OS;
import fri.util.regexp.RegExpUtil;
import fri.util.text.CmdLineSubstitution;
import fri.util.process.ProcessManager;
import fri.util.props.*;
import fri.gui.LocationUtil;
import fri.gui.text.*;
import fri.gui.swing.text.*;
import fri.gui.swing.toolbar.ScrollablePopupToolbar;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.splitpane.*;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.propdialog.PropViewDialog;

/**
	Durchfuehren eines Kommandos ueber einen externen Prozess,
	dessen Ausgaben in einem Fenster angezeigt werden.
	Auch Eingaben an den Prozess werden ueber diese Fenster
	abgewickelt (Enter-Taste).<BR>
	Das Fenster kann geschlossen werden, ohne den Prozess zu 
	unterbrechen, der Prozess kann gestoppt und neu gestartet
	werden.
	<p>
	Folgende Kommandos sind eingebaut:
	<ul>
		<li>ls, dir</li>
		<li>cat, type</li>
	</ul>
*/

public class CommandMonitor extends DefaultCommandMonitor implements
	ChangeListener	// slider
{
	private JButton envir, sysprops, newwin, clear, find, stop2, start2;
	private JToggleButton scrollLock;
	private JSlider sl_speed;
	private TextRenderer errorText;
	private SplitPane split;
	private JCheckBox useBuiltIns, separateOutput;
	private JComponent outputScrollPane;
	private boolean storeEnv = false;
	private JCheckBox substituteWildcards;
	private Vector commandlines;
	private EnvDialog envDlg;
	private JScrollPane errorScrollPane;

	/**
		Leeres Kommando-Fenster.
	*/
	public CommandMonitor()	{
		super();
	}

	/**
		Leeres Kommando-Fenster mit gesetztem Arbeitsverzeichnis.
	*/
	public CommandMonitor(File workingDirectory)	{
		super(workingDirectory);
	}

	/**
		Kommando-Fenster mit Kommando, das gestartet wird.
		@param cmd Kommandozeile, in der sich $Variablen, die aus
			dem Environment ersetzt werden, und "Hochkommas" zur
			Gruppierung der Argumente befinden koennen.
		@param env Environment-Variablen als String-array, die als
			$Variable im Kommando substituiert werden.
	*/
	public CommandMonitor(String cmd, String [] env)	{
		super(cmd, env);
	}

	/**
		Kommando-Fenster mit Kommando, das gestartet wird.
		@param workingDirectory Arbeitsverzeichnis fuer das zu startende Programm
	*/
	public CommandMonitor(String cmd, String [] env, File workingDirectory)	{
		super(cmd, env, workingDirectory);
	}


	protected void initEnv()	{
		if (this.env == null)	{
			Properties props = ClassProperties.getProperties(EnvDialog.class);
			
			if (props != null && props.size() > 0)	{
				this.env = CmdLineSubstitution.propsToArray(props);
			}
		}
	}
	
	
	protected JComponent createUpperPanel()	{
		initEnv();
		
		JToolBar tb1 = new JToolBar();
		tb1.setFloatable(false);
		if (OS.isAboveJava13) tb1.setRollover(true);
		
		tf_cmd = createCommandComboBox();
		tf_cmd.addActionListener(this);
		tb1.add(tf_cmd);
		createInputButton(tb1);

		JPanel p = new JPanel(new BorderLayout());

		ScrollablePopupToolbar popupToolbar = new ScrollablePopupToolbar(p, true, SwingConstants.TOP);
		fillToolBarWithActions(popupToolbar.getToolbar());
		p.add(tb1);

		return p;
	}

	protected JToolBar fillToolBarWithActions(JToolBar tb)	{
		tb.setFloatable(false);
		if (OS.isAboveJava13) tb.setRollover(true);
		
		tb.add(createProgressLabel());
		tb.addSeparator();

		super.fillToolBarWithActions(tb);	// start, stop, workdir

		tb.add(envir = createButton(Icons.get(Icons.configure), "Environment Variables For Commandline"));
		tb.addSeparator();

		boolean usebuiltin = PropertyUtil.checkClassProperty("useBuiltIns", getClass(), "true", true);
		tb.add(useBuiltIns = new JCheckBox("Use Built-Ins", usebuiltin));
		useBuiltIns.setToolTipText("cd, pwd, echo, ls / dir, cat / type");
		
		boolean substitute = PropertyUtil.checkClassProperty("substituteWildcards", getClass(), "true");	// default programs do this
		tb.add(substituteWildcards = new JCheckBox("Expand Wildcards", substitute));
		substituteWildcards.setToolTipText("Substitute File Wildcards Containing \"*+?[^]\"");
		
		boolean separate = PropertyUtil.checkClassProperty("separateOutput", getClass(), "true", true);
		tb.add(separateOutput = new JCheckBox("Separate Outputs", separate));
		separateOutput.setToolTipText("Redirect System.out And System.err To Different Textareas");
		separateOutput.addActionListener(this);

		sl_speed = new JSlider(JSlider.HORIZONTAL, 0, ProcessManager.SLEEPMAX, 0);
		sl_speed.setValue(ProcessManager.MILLIS);
		sl_speed.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		sl_speed.setInverted(true);	// grosser Wert = langsam
		sl_speed.addChangeListener(this);
		sl_speed.setToolTipText("min <- Output Speed -> max");
		tb.add(sl_speed);

		tb.addSeparator();

		tb.add(scrollLock = createToggleButton(new ImageIcon(CommandMonitor.class.getResource("images/scroll_lock.gif")), "Lock or Unlock Autoscrolling"));
		
		tb.add(find = createButton(Icons.get(Icons.find), "Find Text In Output"));
		tb.add(clear = createButton(Icons.get(Icons.clear), "Clear Output And Error Text Areas"));
		tb.add(newwin = createButton(Icons.get(Icons.computer), "Open New Command Window"));
		tb.add(sysprops = createButton(Icons.get(Icons.question), "Show Java System Properties"));
		
		tb.add(new JSeparator(SwingConstants.VERTICAL));
		tb.add(start2 = createButton(Icons.get(Icons.start), "Launch Command Process"));
		tb.add(stop2 = createButton(Icons.get(Icons.stop), "Terminate Launched Process"));
		stop2.setEnabled(false);

		return tb;
	}

	protected JComponent createClientArea()	{
		outputScrollPane = super.createClientArea();
		
		errorText = new ComfortTextArea(10, 40);
		((JTextComponent)errorText).setEditable(false);
		split = new SplitPane(getClass(), JSplitPane.VERTICAL_SPLIT);
		if (separateOutput.isSelected())
			split.setTopComponent(outputScrollPane);
		split.setBottomComponent(errorScrollPane = new JScrollPane((Component)errorText));
		split.setOneTouchExpandable(true);
		split.setDividerLocation(0.5);	// will be overridden when persistent
		
		((JTextComponent)outputText).setToolTipText("Output Text / Input");
		((JTextComponent)errorText).setToolTipText("Error Output");
		
		MouseListener scrollLockListener = new MouseAdapter()	{
			public void mousePressed(MouseEvent e) {
				handleStopMouseEventOnScrollLock();
			}
		};
		((JTextComponent) outputText).addMouseListener(scrollLockListener);
		((JTextComponent) errorText).addMouseListener(scrollLockListener);
		((JScrollPane) outputScrollPane).getVerticalScrollBar().addMouseListener(scrollLockListener);
		((JScrollPane) errorScrollPane).getVerticalScrollBar().addMouseListener(scrollLockListener);
		
		return separateOutput.isSelected() ? split : outputScrollPane;
	}

	private void handleStopMouseEventOnScrollLock()	{
		// ph is not null when process output is running
		if (processHandler != null)	{
			if (scrollLock.isSelected() == false)
				scrollLock.doClick();
		}
		else	{
			if (scrollLock.isSelected())
				scrollLock.doClick();
		}
	}
	
	protected TextRenderer createOutputTextArea()	{
		return new ComfortTextArea(10, 40);
	}

	/**
		Setting MultilineHistCombo: It is not possible to launch multiline commands even on UNIX,
		but good for launching more than one command.
	*/
	protected HistCombo createCommandComboBox()	{
		tf_cmd = new MultilineHistCombo(new File(HistConfig.dir()+"CommandMonitor.list"));
		tf_cmd.setToolTipText("Enter A Commandline To Launch");
		return tf_cmd;
	}
	


	protected void clearTextArea()	{
		super.clearTextArea();
		errorText.setText("");
	}
		
	protected void startInternal(String cmd)	{
		if (cmd.indexOf("\n") >= 0 || cmd.indexOf("\r") >= 0)	{	// batch job, split to lines
			StringTokenizer stok = new StringTokenizer(cmd, "\r\n");
			
			while (stok.hasMoreTokens())	{
				if (commandlines == null)
					commandlines = new Vector(stok.countTokens());
					
				String s = stok.nextToken().trim();
				if (s.length() > 0)
					commandlines.add(s);
			}
			cmd = (String)commandlines.remove(0);
		}
		
		if (isBuiltIn(cmd) == false)
			execute(cmd);
		else
			cleanUp(0);
	}

	protected void cleanUp(int ec)	{
		super.cleanUp(ec);
		
		if (commandlines != null && commandlines.size() > 0)	{
			String cmd = (String)commandlines.remove(0);
			startInternal(cmd);
		}
		else	{
			commandlines = null;
		}
	}

	protected void stop()	{
		commandlines = null;	// break any batch job
		super.stop();	// terminte process
	}



	/** Executes and monitors the command. Optionally passes eror textarea to handler. */
	protected ProcessHandler createProcessHandler(String cmd)	{
		ProcessHandler ph = new ProcessHandler(
				parseCommandline(cmd),
				env,
				this,
				(TextRenderer)outputText,
				separateOutput.isSelected() ? (TextRenderer)errorText : null,
				workingDirectory);
		ph.setMillis(sl_speed.getValue());
		return ph;
	}

	
	/** Substitutes $Variables and resolves quotes, optionally substitutes wildcards. */
	protected String [] parseCommandline(String cmd)	{
		String [] carr = super.parseCommandline(cmd);
		
		if (substituteWildcards.isSelected())
			carr = substituteWildcards(carr);	// interpret "*.java"

		return carr;
	}


	protected void setButtonsEnabled(boolean enable)	{
		super.setButtonsEnabled(enable);
		
		stop2.setEnabled(stop.isEnabled());
		start2.setEnabled(start.isEnabled());

		envir.setEnabled(enable);
		useBuiltIns.setEnabled(enable);
		separateOutput.setEnabled(enable);
		substituteWildcards.setEnabled(enable);
	}


	public boolean close()	{
		boolean ret = super.close();
		
		if (ret)	{
			// commandlines = null;	// would break a batch job ...

			ClassProperties.put(getClass(), "separateOutput", separateOutput.isSelected() ? "true" : "false");
			ClassProperties.put(getClass(), "useBuiltIns", useBuiltIns.isSelected() ? "true" : "false");
			ClassProperties.put(getClass(), "substituteWildcards", substituteWildcards.isSelected() ? "true" : "false");
			split.close();	// stores ClassProperties
	
			if (storeEnv)	{
				ClassProperties.setProperties(EnvDialog.class, CmdLineSubstitution.arrayToProps(env));
				ClassProperties.store(EnvDialog.class);
			}
		}
		
		return ret;
	}
	

	// interface ActionListener

	public void actionPerformed(ActionEvent e)	{
		if (e.getSource() == start2)	{
			start();
		}
		else
		if (e.getSource() == stop2)	{
			stop();
		}
		else
		if (e.getSource() == scrollLock)	{
			setScrollLockState((JTextComponent) outputText);
			setScrollLockState((JTextComponent) errorText);
		}
		else
		if (e.getSource() == separateOutput)	{
			Container c = getContentPane();
			
			if (separateOutput.isSelected())	{
				c.remove(outputScrollPane);
				split.setTopComponent(outputScrollPane);
				c.add(split, BorderLayout.CENTER);
				split.setDividerLocation(0.8);	// default
			}
			else	{
				c.remove(split);
				split.remove(outputScrollPane);
				c.add(outputScrollPane, BorderLayout.CENTER);
			}
			
			((JComponent)c).revalidate();
			c.repaint();
		}
		else
		if (e.getSource() == find)	{
			((ComfortTextArea)outputText).find();
		}
		else
		if (e.getSource() == clear)	{
			outputText.setText("");
			errorText.setText("");
		}
		else
		if (e.getSource() == sysprops)	{	// system-properties anzeigen
			setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
			PropViewDialog dlg = new PropViewDialog(this);
			dlg.show();
			setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));
		}
		else
		if (e.getSource() == envir)	{	// environment
			setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
			if (envDlg == null)	{
				envDlg = new EnvDialog(this, this.env);
				envDlg.setSize(200, 200);
				LocationUtil.centerOverParent(envDlg, this);
			}
			envDlg.setVisible(true);
			
			if (envDlg.getOK())	{
				this.env = envDlg.getEnv();
				storeEnv = true;
			}
			setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));
		}
		else
		if (e.getSource() == newwin)	{	// open new window
			setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
			new CommandMonitor(workingDirectory);
			setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));
		}
		else	{	// delegate to super
			super.actionPerformed(e);
		}
	}

	private void setScrollLockState(JTextComponent textArea)	{
		// when there is a running process
		if (processHandler != null)	{
			if (scrollLock.isSelected() == false)	{
				// trigger autoscrolling by setting caret dot to end of text
				textArea.getCaret().setDot(textArea.getDocument().getLength());
			}
			else	{
				// stop autoscrolling by NOT setting caret dot to end of text
				int len = textArea.getDocument().getLength();
				if (len > 0 && textArea.getCaret().getDot() == len)
					textArea.getCaret().setDot(len - 1);
			}
		}
	}


	// interface ChangeListener

	public void stateChanged(ChangeEvent e)	{
		if (processHandler != null && sl_speed.getValueIsAdjusting() == false)	{
			processHandler.setMillis(sl_speed.getValue());
		}
	}



	protected boolean isBuiltIn(String [] carr)	{
		//System.err.println("parsed commandline >"+ArrayUtil.print(carr)+"<");
		boolean ret = super.isBuiltIn(carr);	// cd and pwd
		if (ret)	// was interpreted
			return ret;
		
		if (useBuiltIns.isSelected() == false)
			return false;

		int origLen = carr.length;
		
		if (carr != null && carr.length > 0)	{
			carr = substituteWildcards(carr);	// interpret "*.java"
			//System.err.println("substituted commandline >"+ArrayUtil.print(carr)+"<");

			if (echo(carr))
				ret = true;
			else
			if (listFiles(carr, origLen > 1))
				ret = true;
			else
			if (showFileContents(carr))
				ret = true;
		}
			
		return ret;
	}
	
	private String [] substituteWildcards(String [] args)	{
		Vector v = new Vector(args.length);
		v.add(args[0]);	// this is the command
		
		for (int i = 1; i < args.length; i++)	{	// loop arguments
			String path = FileUtil.separatePath(args[i], false);
			String pattern = FileUtil.separateFile(args[i]);
			
			if (pattern == null ||
					pattern.length() <= 0 ||
					!RegExpUtil.containsDefaultWildcards(pattern))
			{
				v.add(args[i]);
			}
			else	{	// has a pattern, substitute it
				if (path.equals("."+File.separator))
					path = null;
				
				File f = completeRelativePath(path);
				
				if (f.isDirectory())	{
					String [] names = f.list();
					
					if (names == null || names.length > 0)	{
						Vector filtered = RegExpUtil.getFilteredAlternation(
								pattern,
								ArrayUtil.toVector(names),
								true,	// include
								OS.supportsCaseSensitiveFiles());
						
						if (filtered != null && filtered.size() > 0)	{
							filtered = new QSort().sort(filtered);
							
							for (int j = 0; j < filtered.size(); j++)	{
								String s = new File(path, filtered.get(j).toString()).getPath();
								v.add(s);
							}
						}
						else	{
							v.add(args[i]);
						}
					}
				}
			}
		}
		
		args = new String [v.size()];
		v.copyInto(args);
		return args;
	}



	protected boolean printWorkingDirectory(String [] cmd)	{
		if (useBuiltIns.isSelected() == false && OS.isUnix)
			return false;
			
		return super.printWorkingDirectory(cmd);
	}


	private boolean echo(String [] cmd)	{
		if (cmd[0].equals("echo") == false)
			return false;
			
		Document doc = ((JTextComponent)outputText).getDocument();
		for (int i = 1; i < cmd.length; i++)	{
			try	{ doc.insertString(doc.getLength(), cmd[i]+" ", null); }
			catch (BadLocationException e)	{}
		}
		return true;
	}
	

	private boolean showFileContents(String [] cmd)	{
		if (cmd[0].equals("cat") == false && cmd[0].equals("type") == false || cmd.length < 2)
			return false;
			
		for (int i = 1; i < cmd.length; i++)	{
			String s = cmd[i];
			File f = completeRelativePath(s);

			if (f.exists() == false || f.isFile() == false)	{
				error("Can not show file contents of \""+s+"\"");
				break;
			}
			else	{
				Document doc = ((JTextComponent)outputText).getDocument();
				BufferedReader r = null;
				try	{
					if (cmd.length > 2)	{
						doc.insertString(doc.getLength(), s+" ========================================\n", null);
					}

					r = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
					
					while ((s = r.readLine()) != null)
						doc.insertString(doc.getLength(), s+"\n", null);
				}
				catch (BadLocationException e)	{
				}
				catch (IOException e)	{
					error(e.getMessage());
				}
				finally	{
					if (r != null)
						try { r.close(); } catch (IOException e) {}
				}
			}
		}
		
		return true;
	}
	

	private boolean listFiles(String [] cmd, boolean thereWasArg)	{
		if (cmd[0].equals("ls") == false && cmd[0].equals("dir") == false)
			return false;
		
		if (cmd[0].equals("ls") && cmd.length > 1 && cmd[1].startsWith("-"))
			return false;

		if (cmd.length < 2 && thereWasArg == false)	{
			listFiles(new File("."));
		}
		else	{
			for (int i = 1; i < cmd.length; i++)	{
				listFiles(new File(cmd[i]));
			}
		}
		return true;
	}


	private void listFiles(File f)	{
		String currdir = getWorkingDirectoryOrUserDir().getPath();
		
		if (f.getPath().equals(File.separator))	{
			String s;
			f = getWorkingDirectoryOrUserDir();
			while ((s = f.getParent()) != null)	{
				if (s.endsWith(File.separator) == false)
					s = s+File.separator;
				f = new File(s);
			}
		}
		else
		if (f.getPath().equals("."))	{
			f = new File(currdir);
		}
		else
		if (f.getPath().equals(".."))	{
			String pnt = getWorkingDirectoryOrUserDir().getParent();
			if (pnt == null)
				return;
			f = new File(pnt);
		}

		//System.err.println("file before relativation is: "+f);
		f = completeRelativePath(f.getPath());
		//System.err.println("file after relativation is: "+f);
		
		String [] list = null;
		
		if (f.isDirectory())	{	// list directory
			list = f.list();
			
			for (int i = 0; i < list.length; i++)	{
				String s = f.getPath();
				if (s.endsWith(File.separator) == false)
					s = s+File.separator;
				s = s+list[i];
				
				list[i] = FileUtil.makeRelativePath(currdir, s);
			}
		}
		else
		if (f.isFile())	{	// list file
			list = new String [] { FileUtil.makeRelativePath(currdir, f.getPath()) };
		}
		
		if (list != null)	{
			new QSort().sort(list);
			
			Document doc = ((JTextComponent)outputText).getDocument();
			for (int i = 0; i < list.length; i++)	{
				String s = list[i];
				if (s.startsWith("."+File.separator))
					s = s.substring(2);
					
				try	{ doc.insertString(doc.getLength(), s+"\n", null); }
				catch (BadLocationException e)	{}
			}
		}
	}




	/** application main procedure */

	public static void main (String [] args)	{
		if (args.length < 1)	{
			System.err.println("SYNTAX: java "+CommandMonitor.class.getName()+" \"command\" [name=value name=value ...]");
			new CommandMonitor();
		}
		else	{
			String [] env = null;
			if (args.length > 1)	{
				env = new String [args.length - 1];
				System.arraycopy(args, 1, env, 0, env.length);
			}
			new CommandMonitor(args[0], env);
		}
	}

}