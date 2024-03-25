package fri.gui.swing.commandmonitor;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import fri.util.os.OS;
import fri.util.process.ProcessWriter;
import fri.util.text.CmdLineSubstitution;
import fri.gui.CursorUtil;
import fri.gui.text.*;
import fri.gui.swing.text.*;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.progresslabel.ProgressLabel;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.application.*;

/**
	Durchfuehren eines Kommandos ueber einen externen Prozess,
	dessen Ausgaben in einem Fenster angezeigt werden.
	Interaktive Eingabe fuer den laufenden Prozess ist moeglich.
	Einfache Kommandos wie cd, ls, cat sind eingebaut.
*/

public class DefaultCommandMonitor extends GuiApplication implements
	ActionListener,	// start commandline on Enter, Button callbacks
	KeyListener,	// send user input to started process when Enter is pressed
	ProcessWriter	// send user input to started process
{
	private static final String title = "Command Execution";
	private static JFileChooser fileChooser, inputChooser;
	protected HistCombo tf_cmd;
	protected ProgressLabel l_exitcode;
	protected TextRenderer outputText;
	protected JButton stop, start, workdir;
	protected transient ProcessHandler processHandler;
	protected transient String line;	// eingegebener Text
	protected String [] env;
	protected boolean ready;
	protected File workingDirectory, oldWorkingDirectory;
	protected JButton input;
	protected File inputFile;
	protected JCheckBox inputFileActive;


	/**
		Leeres Kommando-Fenster.
	*/
	public DefaultCommandMonitor()	{
		this(null);
	}

	/**
		Leeres Kommando-Fenster mit Arbeitsverzeichnis.
	*/
	public DefaultCommandMonitor(File workingDirectory)	{
		super(title);

		this.workingDirectory = workingDirectory;

		init(false);
	}

	/**
		Kommando-Fenster mit Kommando, das gestartet wird.
		@param cmd Kommandozeile, die $Variable enthalten kann,
				die aus dem environment substituiert werden
		@param env Environment, das zur Substitution der
				Kommandozeile dienen kann
	*/
	public DefaultCommandMonitor(String cmd, String [] env)	{
		this(cmd, env, null);
	}

	/**
		Kommando-Fenster mit Kommando, das gestartet wird.
		@param workingDirectory Arbeitsverzeichnis fuer das zu startende Programm,
	*/
	public DefaultCommandMonitor(String cmd, String [] env, File workingDirectory)	{
		super(title);

		this.env = env;
		this.workingDirectory = workingDirectory;

		init(true);
		
		tf_cmd.setText(cmd);
		start();
	}


	public HistCombo getCommandTextField()	{
		return tf_cmd;
	}
	
	
	public void setWorkingDirectory(File dir)	{
		oldWorkingDirectory = workingDirectory;

		if (dir != null && dir.isDirectory() == false)	{	// set work-dir and command file name
			String p = dir.getParent();
			if (p == null)
				return;
				
			tf_cmd.setText(dir.getName());
			dir = new File(p);
		}
		
		this.workingDirectory = dir;

		String t = (workingDirectory != null) ? title+"  -  "+workingDirectory.getAbsolutePath() : title;
		setTitle(t);
		
		String dirStr = (dir == null) ? System.getProperty("user.dir") : dir.getAbsolutePath();
		workdir.setToolTipText("Working Directory: "+dirStr);
	}
	

	protected void init(boolean haveCommand)	{
		Container c = getContentPane();
		c.setLayout(new BorderLayout());

		JComponent upperPanel = createUpperPanel();
		if (upperPanel != null)
			c.add(upperPanel, BorderLayout.NORTH);
		
		tf_cmd.setEnabled(haveCommand == false);

		c.add(createClientArea(), BorderLayout.CENTER);

		new FileDndListener((Component)outputText, this);
		new FileDndListener(workdir, this);
		new FileDndListener(input, this);
		new FileDndListener(tf_cmd.getTextEditor(), this);

		super.init();

		setWorkingDirectory(workingDirectory);
	}


	private void setInputFile(File file)	{
		this.inputFile = file;
		
		if (file != null)	{
			input.setToolTipText(file.getPath());
			input.setText("< "+file.getName());
			input.setBorderPainted(true);
		}
		else	{
			input.setToolTipText("Choose Input File For Command");
			input.setText("< Input");
			input.setEnabled(false);
			input.setBorderPainted(false);
		}
	}


	/**
		Called from DndListener when dropping a File object:
		file is interpreted to be new input file,
		directory is interpreted to be new working directory.
	*/ 
	public void setDndFile(File f, Component dropComponent)	{
		if (dropComponent == input || dropComponent == outputText && f.isFile())	{
			if (f.isFile())
				setInputFile(f);
		}
		else	{
			setWorkingDirectory(f);
		}
	}
	
	
	protected JComponent createUpperPanel()	{
		JToolBar tb = new JToolBar();
		if (OS.isAboveJava13) tb.setRollover(true);
		tb.add(createProgressLabel());
		tb.add(tf_cmd = createCommandComboBox());
		tf_cmd.addActionListener(this);
		tf_cmd.setToolTipText("Commandline To Launch");
		createInputButton(tb);
		//tb.addSeparator();
		fillToolBarWithActions(tb);
		return tb;
	}

	protected void createInputButton(JToolBar tb)	{
		input = new JButton("< Input");
		input.addActionListener(this);
		setInputFile(null);
		tb.add(input);

		inputFileActive = new JCheckBox();
		inputFileActive.setMargin(new Insets(0, -3/*-4*/, 0, 0));
		inputFileActive.setToolTipText("Activate Reading Input From File");
		inputFileActive.setBorderPaintedFlat(true);
		tb.add(inputFileActive);

		inputFileActive.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				input.setEnabled(inputFileActive.isSelected());
				inputFileActive.setToolTipText((inputFileActive.isSelected() ? "Deactivate" : "Activate")+" Reading Input From File");
				if (inputFile == null && inputFileActive.isSelected())	{
					chooseInputFile();
				}
			}
		});
	}
	
	protected JToolBar fillToolBarWithActions(JToolBar tb)	{
		tb.add(start = createButton(Icons.get(Icons.start), "Launch Command Process"));
		tb.add(stop = createButton(Icons.get(Icons.stop), "Terminate Launched Process"));
		stop.setEnabled(false);
		tb.add(new JSeparator(SwingConstants.VERTICAL));
		tb.add(workdir = createButton(Icons.get(Icons.chdir), null));
		return tb;
	}

	protected JToggleButton createToggleButton(Icon icon, String tooltip)	{
		JToggleButton btn = new JToggleButton(icon);
		configureButton(btn, icon, tooltip);
		return btn;
	}
	
	protected JButton createButton(Icon icon, String tooltip)	{
		JButton btn = new JButton(icon);
		configureButton(btn, icon, tooltip);
		return btn;
	}
	
	private void configureButton(AbstractButton btn, Icon icon, String tooltip)	{
		btn.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		btn.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		btn.setBorderPainted(false);
		if (tooltip != null)
			btn.setToolTipText(tooltip);
		btn.addActionListener(this);
	}

	protected ProgressLabel createProgressLabel()	{
		l_exitcode = new ProgressLabel("  ");
		l_exitcode.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		l_exitcode.setBorder(BorderFactory.createRaisedBevelBorder());
		l_exitcode.setToolTipText("Exitcode Of Launched Process");
		return l_exitcode;
	}

	protected HistCombo createCommandComboBox()	{
		tf_cmd = new CommandComboBox(new File(HistConfig.dir()+"DefaultCommandMonitor.list"));
		tf_cmd.setToolTipText("Enter A Commandline To Launch");
		return tf_cmd;
	}
	
	protected JComponent createClientArea()	{
		outputText = createOutputTextArea();
		((JTextComponent)outputText).addKeyListener(this);
		JScrollPane sp = new JScrollPane((Component)outputText);
		return sp;
	}

	protected TextRenderer createOutputTextArea()	{
		OutputTextArea ta = new OutputTextArea();
		ta.setCaretColor(Color.red);
		ta.setTabSize(4);
		return ta;
	}



	protected String getCmdFromTextField()	{
		tf_cmd.commit();
		String cmd = tf_cmd.getText();
		System.err.println("got command from textfield: >"+cmd+"<");

		if (cmd == null || cmd.trim().equals(""))	{
			Toolkit.getDefaultToolkit().beep();
			return null;
		}
		return cmd;
	}
	
	protected void start()	{
		String cmd = getCmdFromTextField();
		if (cmd == null)
			return;

		clearTextArea();
		startInternal(cmd);
	}
	
	protected void clearTextArea()	{
		outputText.setText("");
	}
		
	protected void startInternal(String cmd)	{
		if (isBuiltIn(cmd) == false)	{
			execute(cmd);
		}
	}

	protected void stop()	{
		if (processHandler != null)	{
			processHandler.stop();
			processHandler = null;
		}
	}

	/** Sets Buttons enabled and starts command. */
	protected void execute(String cmd)	{
		setButtonsEnabled(false);
		
		this.ready = true;
		this.line = null;
		processHandler = createProcessHandler(cmd);
		
		if (ready == false)	// could not be started
			processHandler = null;
	}
	
	private boolean feedingFileInput()	{
		return inputFile != null && inputFileActive.isSelected();
	}
	
	
	/** Executes and monitors the command. */
	protected ProcessHandler createProcessHandler(String cmd)	{
		return new ProcessHandler(
				parseCommandline(cmd),
				env,
				this,
				(TextRenderer)outputText,
				workingDirectory);
	}
	
	
	/** Substitutes $Variables and resolves quotes. */
	protected String [] parseCommandline(String cmd)	{
		return CmdLineSubstitution.parse(cmd, env);
	}


	protected void cleanUp(int ec)	{
		processHandler = null;
		ready = false;
		
		setButtonsEnabled(true);
		tf_cmd.getTextEditor().requestFocus();
	}


	protected void setButtonsEnabled(boolean enable)	{
		tf_cmd.setEnabled(enable);
		stop.setEnabled(!enable);
		start.setEnabled(enable);
		workdir.setEnabled(enable);
		input.setEnabled(enable ? inputFileActive.isSelected() : false);
		inputFileActive.setEnabled(enable);
	}
	

	public boolean close()	{
		if (processHandler != null)	{
			int ret = JOptionPane.showConfirmDialog(
							this,
							"Stop The Launched Application?",
							"Running Process",
							JOptionPane.YES_NO_CANCEL_OPTION);

			if (ret == JOptionPane.CANCEL_OPTION)	{
				return false;
			}
			else
			if (ret == JOptionPane.YES_OPTION)	{
				System.err.println("stopping process");
				if (processHandler != null)	// could be finished meanwhile
					processHandler.exitStop();	// Process Abbruch
			}
			
			if (processHandler != null)	{
				processHandler.closeOutput();
				processHandler = null;
			}
				
			ready = false;
		}

		tf_cmd.save();
		return super.close();
	}



	// interface ProcessWriter

	public void progress()	{
		if (ready())
			EventQueue.invokeLater(new Runnable()	{
				public void run()	{
					l_exitcode.progress();
				}
			});
	}
	
	public void exited(final int ec)	{
		System.err.println("exited with code "+ec);
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				l_exitcode.setText(String.valueOf(ec));
				cleanUp(ec);
			}
		});
	}
	
	public void notfound()	{
		System.err.println("command not found");
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				l_exitcode.setText(" ");
				cleanUp(-1);
			}
		});
	}
	
	public void userstopped()	{
		System.err.println("user stopped proces");
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				l_exitcode.setText(" ");
				cleanUp(-2);
			}
		});
	}
	
	public boolean ready()	{
		return ready;
	}
	
	/**
		Public due to implementation of ProcessWriter (not to be called from outside)!
		Gets called periodically from background thread.
		Use <code>setWriteLine(line)</code> to write a line of text to Process.
	*/
	public synchronized boolean write(OutputStream out)	{
		// User-Input an den wartenden Prozess schreiben.
		if (feedingFileInput())	{
			System.err.println("write input from file: "+inputFile);

			BufferedInputStream bin = null;
			int bufsize = (int) Math.min(inputFile.length(), (long)512);
			byte [] bytes = new byte[bufsize];
			
			try	{
				bin = new BufferedInputStream(new FileInputStream(inputFile));
				int cnt;
				while (bufsize > 0 && processHandler != null && (cnt = bin.read(bytes)) != -1)	{
					out.write(bytes, 0, cnt);
				}

				notify();	// eventuellen wait beenden
			}
			catch (IOException e)	{
				//e.printStackTrace();	// "Die Pipe wurde beendet", wenn weniger input angenommen wurde
			}
			finally	{
				try	{ bin.close(); } catch (Exception e)	{}
			}

			System.err.println("... finished writing input from file");
			return false;	// closes input on false, will not call write() again!
		}
		else
		if (line != null)	{
			try	{
				System.err.println("write >"+line+"<");

				out.write(line.getBytes());
				out.write(System.getProperty("line.separator").getBytes());
				out.flush();

				line = null;	// abgeholt

				notify();	// wait beenden
			}
			catch (IOException e)	{
				//e.printStackTrace();	// "Die Pipe wurde beendet", wenn Process bereits beendet
				return false;
			}
		}
		return true;
	}



	/** Write a line to Process, wait until the previous one was consumed. */
	public synchronized void setWriteLine(String command)	{
		while (processHandler != null && this.line != null && processHandler.isInputOpen()) {
			try {
				//System.err.println("waiting for Process to take away previous line >"+line+"<");
				wait(processHandler.getMillis() * 10);
			}
			catch (InterruptedException e) {
			}
		}
		//System.err.println("finished waiting for Process to take away previous line");
		this.line = command;
	}



	// interface KeyListener

	public void keyPressed(KeyEvent e)	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER && processHandler != null)	{
			setWriteLine(getCurrentLine(outputText));
		}
	}
	public void keyReleased(KeyEvent e)	{
	}
	public void keyTyped(KeyEvent e)	{
	}


	private String getCurrentLine(TextRenderer ta)	{
		int end = ta.getCaretPosition();
		int start = processHandler != null ? processHandler.getCaretPosition() : 0;
		String s = "";

		if (processHandler != null && end >= start)	{
			Document doc = ((JTextComponent)ta).getDocument();
			try	{
				s = doc.getText(start, end - start);
			}
			catch (BadLocationException ex)	{
				ex.printStackTrace();
			}

			// now set current input-read caret position to last text position
			processHandler.setCaretPosition(end + 1);	// plus newline, not appended yet
		}

		return s;
	}



	// interface WindowListener, from GuiApplication
	public void windowOpened(WindowEvent e) {
		if (processHandler == null)
			tf_cmd.getTextEditor().requestFocus();
		else
			outputText.requestFocus();
			
		super.windowOpened(e);
	}


	// interface ActionListener

	public void actionPerformed(ActionEvent e)	{
		CursorUtil.setWaitCursor(this);
		
		try	{
			if (e.getSource() == tf_cmd || e.getSource() == start)	{
				start();
			}
			else
			if (e.getSource() == stop)	{	// kill
				stop();
			}
			else
			if (e.getSource() == workdir)	{	// workdir chooser
				chooseWorkDir();
			}
			else
			if (e.getSource() == input)	{	// inputfile chooser
				chooseInputFile();
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}


	protected void chooseWorkDir()	{
		CursorUtil.setWaitCursor(this);

		try	{
			if (fileChooser == null)	{
				fileChooser = new JFileChooser(getWorkingDirectoryOrUserDir());
				
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			}
			
			if (workingDirectory != null)	{
				fileChooser.setSelectedFile(workingDirectory);
			}
			
			int ret = fileChooser.showOpenDialog(this);
			
			if (ret == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				if (file != null)	{
					setWorkingDirectory(file);
				}
			}
	
			fileChooser.cancelSelection();	// reuse dialog. JFileChooser stores APPROVE_OPTION !
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}
	

	protected void chooseInputFile()	{
		CursorUtil.setWaitCursor(this);
		try	{
			if (inputChooser == null)	{
				inputChooser = new JFileChooser(getWorkingDirectoryOrUserDir());
				
				inputChooser.setMultiSelectionEnabled(false);
				inputChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			}
			
			int ret = inputChooser.showOpenDialog(this);
			
			if (ret == JFileChooser.APPROVE_OPTION) {
				File file = inputChooser.getSelectedFile();
				if (file != null)	{
					setInputFile(file);
				}
			}
	
			inputChooser.cancelSelection();	// reuse dialog. JFileChooser stores APPROVE_OPTION !
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}


	protected boolean isBuiltIn(String cmd)	{
		//System.err.println("raw commandline >"+cmd+"<");
		return isBuiltIn(CmdLineSubstitution.parse(cmd, env));
	}
	

	protected boolean isBuiltIn(String [] carr)	{
		boolean ret = false;
		
		if (carr != null && carr.length > 0)	{
			if (changeWorkingDirectory(carr))
				ret = true;
			else
			if (printWorkingDirectory(carr))
				ret = true;
		}
			
		return ret;
	}
	
	
	private boolean changeWorkingDirectory(String [] cmd)	{
		if (cmd[0].equals("cd") == false || cmd.length > 2)
			return false;
		
		String s = cmd.length > 1 ? cmd[1] : "";
		
		if (s.equals("."))
			return true;	// we are there
		
		File newDir = null;
		
		if (s.length() <= 0)	{
			newDir = new File(System.getProperty("user.dir"));
		}
		else
		if (s.equals("$HOME") || s.equals("~"))	{
			newDir = new File(System.getProperty("user.home"));
		}
		else
		if (s.equals("-"))	{
			newDir = oldWorkingDirectory != null ? oldWorkingDirectory : new File(System.getProperty("user.dir"));
		}
		else
		if (s.equals(".."))	{
			File f1 = getWorkingDirectoryOrUserDir();
			newDir = f1.getParent() == null ? null : new File(f1.getParent());
		}
		else	{
			newDir = completeRelativePath(s);
		}
		
		if (newDir == null || newDir.exists() == false || newDir.isDirectory() == false)	{
			error("Can not change to directory \""+s+"\"");
			return true;
		}
		
		setWorkingDirectory(newDir);
		
		outputText.setText("Changed working directory to: "+newDir);
		tf_cmd.setText("cd ");

		return true;
	}


	protected boolean printWorkingDirectory(String [] cmd)	{
		if (cmd[0].equals("pwd") == false)
			return false;
			
		outputText.setText("Working directory is: "+getWorkingDirectoryOrUserDir());
		return true;
	}
	

	protected File getWorkingDirectoryOrUserDir()	{
		return workingDirectory != null ? workingDirectory : new File(System.getProperty("user.dir"));
	}


	protected File completeRelativePath(String s)	{
		if (s == null)
			return getWorkingDirectoryOrUserDir();
		
		File f = new File(s);
		
		if (f.isAbsolute() == false)	{	// was a relative path, make absolute
			f = new File(getWorkingDirectoryOrUserDir(), s);
		}

		return f;
	}


	protected void error(String msg)	{
		JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}

	



	/** application main procedure */

	public static void main (String [] args)	{
		if (args.length < 1)	{
			System.err.println("SYNTAX: DefaultCommandMonitor \"command\"");
			new DefaultCommandMonitor();
		}
		else	{
			new DefaultCommandMonitor(args[0], null);
		}
	}

}