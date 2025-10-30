package fri.gui.swing.filebrowser;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.lang.reflect.*;

import fri.gui.CursorUtil;
import fri.gui.swing.commandmonitor.CommandMonitor;
import fri.util.text.CmdLineSubstitution;
import fri.util.os.OS;
import fri.util.FileUtil;
import fri.util.ArrayUtil;
import fri.util.props.PropertyUtil;
import fri.util.javastart.*;

/**
	Handle an open event for multiple files that can
	match to multiple commands.
	First launch groups are formed by equality of their pattern-match.
	For each group of files the actions are determined from
	the command-table, that (temporary) includes Java Activation Framework.
	If there are multiple actions for a group, a popup is opened,
	else the files are opened.
	Only one popup is opened, other groups that would
	need another popup decision are dropped.
	If there are undefined file types, JAF is consulted for
	them, if there is no action, some default actions are
	tried, especially for archives (ZIP, JAR).
	<p>
	Configuration:
	<ul>
		<li>Setting <b>"java -D filebrowser.actions.builtIn=false ..."</b> switches off
			all built-in actions for file types. This includes calls for Java editor,
			viewer, HTML-browser and archives (.zip, .jar, .tgz).
			</li>
		<li>Setting <b>"java -D filebrowser.actions.JAF=false ..."</b> switches off
			all JAF actions for file types. (This option is implemented in JAFView).
			</li>
		<li>Setting above actions both to false enables you to define your actions
			in the open-event-editor and nowhere else. This is quick if one file type
			has only one action, double click will open the file then without a popup
			choice. If you deny nothing, all actions are mixed and presented in a
			(maybe time consuming) popup choice.<br>
			Another reason for doing that are WINDOWS command verbs, that do not tell
			anything about the started application.
			</li>
	</ul>
	@see fri.gui.swing.filebrowser.JAFView
*/

public class OpenLauncher implements ActionListener
{
	private static boolean jafPresent = true;
	private LaunchGroup group = null;
	private Point point;
	private Component component;
	private final Component parent;
	private Vector undefined = null;
	private Vector undefinedBool = null;
	private Vector undefinedNode = null;
	private LaunchGroups launchGroups;
	private boolean folderEvent = false;
	private int exitcode = 0;
	private String currCmd = null;
	private String error = null;
	private TreeEditController tc;
	private Vector cmdChoice;
	private String tmpClassPath;
	
	/**
		Create an OpenLauncher that prepares commands and undefined items
		for the passed NetNodes. This constructor takes no action than
		<code>buildLaunchGroups()</code>.
		@param n nodes to look up for defined patterns
		@param cmdlist list of commands where to look
	*/
	public OpenLauncher(
		Component parent,
		TreeEditController tc,
		NetNode [] n,
		OpenCommandList cmdlist)
	{
		this.parent = parent;
		this.tc = tc;
		launchGroups = buildLaunchGroups(cmdlist, n);
	}
	
	/**
		Create an OpenLauncher that executes commands for the passed NetNodes
		or launches the OpenEventEditor when no patterns were defined.
		@param parent main window
		@param n nodes to look up for defined patterns
		@param cmdlist list of commands where to look
		@param p Point where to open a popup menu if more than one pattern matches
		@param c Component on which to open popup
	*/
	public OpenLauncher(
		Component parent,
		TreeEditController tc,
		NetNode [] n,
		OpenCommandList cmdlist,
		Point p,
		Component c)
	{
		this(parent, tc, n, cmdlist, p, c, false);	// leaf open event
	}
	
	/**
		Create an OpenLauncher that executes commands for the passed NetNodes
		or expands a JTree when no patterns were defined .
		@param folderEvent if true, the passed nodes must be all folders
		@param rest see above
	*/
	public OpenLauncher(
		Component parent,
		TreeEditController tc,
		NetNode [] n,
		OpenCommandList cmdlist,
		Point p,
		Component c,
		boolean folderEvent)
	{
		this.parent = parent;
		this.tc = tc;
		//System.err.println("got tree edit controller!");
		this.point = p;
		this.component = c;
		this.folderEvent = folderEvent;
		
		// group the indices together by equality of indices
		launchGroups = buildLaunchGroups(cmdlist, n);
		
		if (launchGroups == null)	// undefined folder open, be quick
			return;

		if (undefined != null)	{
			if (folderEvent == false)	{
				// open action editor with undefined file types
				Toolkit.getDefaultToolkit().beep();
				
				String [] sarr = new String [undefined.size()];
				undefined.copyInto(sarr);
				
				boolean [] barr = new boolean [undefined.size()];
				for (int i = 0; i < undefinedBool.size(); i++)
					barr[i] = ((Boolean)undefinedBool.elementAt(i)).booleanValue();
				
				OpenEventEditor.construct(cmdlist, sarr, barr);
				return;	// do no opens if defining new actions
			}
		}
				
		// for each group decide if popup choice or launch by count of indices
		for (int i = 0; i < launchGroups.size(); i++)	{
			LaunchGroup currGroup = (LaunchGroup)launchGroups.elementAt(i);
			
			if (currGroup.isUnique())	{	// no choice necessary
				startCommand(currGroup, currGroup.getCmdNames()[0]);
			}
			else	{	// user has to choose
				if (group != null)	{
					System.err.println("WARNING: sorry, only one group-popup is possible for open action.");
					continue;	// only one popup, leave the rest undone		
				}
				group = currGroup;	// remember global launch group for popup callback
				JPopupMenu popup = getOpenEventPopup(group, this);
				popup.show(component, point.x, point.y);
			}
		}		
	}


	// create launch groups by setting int indizes into the OpenEventTableModel.
	private LaunchGroups buildLaunchGroups(
		OpenCommandList cmdlist,
		NetNode [] n)
	{
		//System.err.println("buildLaunchGroups for "+ArrayUtil.print(n));
		OpenEventTableModel model = new OpenEventTableModel(new OpenCommandList((Vector)cmdlist.clone()));
		
		if (folderEvent == false)	{
			// add built-in command verbs (temporary) to model
			addBuiltInVerbs(model, n);
			// add JAF command verbs (temporary) to model
			addJAFVerbs(model, n);
		}
		
		Integer [][] indices = new Integer [n.length][];
		
		undefined = null;	// reset undefined indicator

		// search indices in table for each file
		for (int i = 0; i < n.length; i++)	{
			String name = folderEvent ? n[i].getFullText() : n[i].getLabel();
			
			// look for commands for the current file
			Vector v = getPatternIndex(name, model, cmdlist, n[i].isLeaf());
			
			if (v == null)	{
				if (undefined == null)	{
					undefined = new Vector(n.length);
					undefinedBool = new Vector(n.length);
					undefinedNode = new Vector(n.length);
				}
				//System.err.println("undefined filename "+name);
				undefined.addElement(name);
				undefinedBool.addElement(Boolean.valueOf(n[i].isLeaf()));
				undefinedNode.addElement(n[i]);
			}
			else	{
				Integer [] iarr = new Integer [v.size()];
				v.copyInto(iarr);
				indices[i] = iarr;
			}
		}

		//System.err.println("OpenLauncher, undefined files are: "+undefined);
		//System.err.println("OpenLauncher, model is: "+model.getDataVector());
		
		// group the indices together by equality of indices
		return new LaunchGroups(n, indices, model);
	}
	
	
	private void addJAFVerbs(OpenEventTableModel model, NetNode [] n)	{
		if (jafPresent == false)
			return;

		JAFView jv;
		try	{
			jv = new JAFView();
		}
		catch (NoClassDefFoundError e)	{
			jafPresent = false;
			System.err.println("FEHLER: JAF (javax.activation) ist nicht installiert: "+e);
			return;
		}

		// search verbs for each file
		Vector done = new Vector();
		String lastPatt = null;

		for (int i = 0; i < n.length; i++)	{
			if (n[i].isLeaf() == false)
				continue;
			
			File file = (File)n[i].getObject();
			String pattern = FileExtensions.makeFilePattern(file.getName());

			if (lastPatt != null && lastPatt.equals(pattern))
				continue;
			lastPatt = pattern;
			
			String [] verbs = jv.getCommandVerbs(file);
			//System.err.println("JAF Verbs for "+file.getName()+" are: "+ArrayUtil.print(verbs));
			
			for (int j = 0; verbs != null && j < verbs.length; j++)	{
				addModelRow(pattern, verbs[j], "JAF", model, done);
			}
		}
	}

	
	private void addBuiltInVerbs(OpenEventTableModel model, NetNode [] n)	{
		if (PropertyUtil.checkSystemProperty("filebrowser.actions.builtIn", "false"))
			return;
			
		Vector done = new Vector();
		String lastPatt = null;

		for (int i = 0; i < n.length; i++)	{
			if (n[i].isLeaf() == false)
				continue;
				
			String name = n[i].getLabel();
			String pattern = FileExtensions.makeFilePattern(name);

			if (lastPatt != null && lastPatt.equals(pattern))
				continue;
			lastPatt = pattern;
			
			String lowerPatt = pattern.toLowerCase();

			// internal opens

			String [] pattAndCmd;
			String menuName = "Java View";
			
			addPatternAndCommand(FileExtensions.isHTML(lowerPatt), menuName, model, done);
			
			addPatternAndCommand(FileExtensions.isArchive(lowerPatt), menuName, model, done);

			addPatternAndCommand(FileExtensions.isImage(lowerPatt), menuName, model, done);

			addPatternAndCommand(FileExtensions.isXML(lowerPatt), menuName, model, done);

			pattAndCmd = FileExtensions.isJavaClass(lowerPatt);
			if (pattAndCmd != null)	{
				addModelRow(pattAndCmd[0], "Internal Launch", pattAndCmd[1], model, done);

				if (lowerPatt.endsWith(".jar"))
					addModelRow(pattAndCmd[0], "External Launch", "java -jar $FILE", model, done);
				else
					addModelRow(pattAndCmd[0], "External Launch", "java -cp $CLASSPATH $BASE", model, done);
			}
			
			pattAndCmd = FileExtensions.isText(lowerPatt);
			if (pattAndCmd != null)	{
				addModelRow(pattAndCmd[0], menuName, pattAndCmd[1], model, done);
				addModelRow(pattAndCmd[0], "Java Edit", "EDIT $FILE", model, done);
			}
		}	// end for
	}
	

	private boolean addPatternAndCommand(String [] pattAndCmd, String menuName, OpenEventTableModel model, Vector done)	{
		if (pattAndCmd != null)	{
			addModelRow(pattAndCmd[0], menuName, pattAndCmd[1], model, done);
			return true;
		}
		return false;
	}
	

	// add a row to temporary command table
	private void addModelRow(
		String pattern,
		String verb,
		String command,
		OpenEventTableModel model,
		Vector done)
	{
		String remember = verb+" "+pattern;
		if (done.contains(remember))
			return;
		
		done.add(remember);
		
		Vector jafRow = model.buildRow(pattern, true);	// file pattern, isLeaf
		int cnt = model.getRowCount();
		model.addRow(jafRow);
		model.setValueAt(verb, cnt, OpenCommandList.SHORTNAME_COLUMN);
		model.setValueAt(command, cnt, OpenCommandList.COMMAND_COLUMN);
	}
	
	
	

	// Returns list of int indizes for the passed name in OpenEventTableModel.
	private Vector getPatternIndex(
		String name,
		OpenEventTableModel model,
		OpenCommandList cmdlist,
		boolean isLeaf)
	{
		int anz = model.getRowCount();
		Vector v = null;
		
		for (int i = 0; i < anz; i++)	{
			String pattern = (String)model.getValueAt(i, OpenCommandList.PATTERN_COLUMN);
			String cmd = (String)model.getValueAt(i, OpenCommandList.COMMAND_COLUMN);
			String type = (String)model.getValueAt(i, OpenCommandList.TYPE_COLUMN);
			Boolean invariant = (Boolean)model.getValueAt(i, OpenCommandList.INVARIANT_COLUMN);
			boolean invar = invariant.booleanValue();
			//System.err.println("open event model entry is "+pattern+" - "+cmd);
			
			if (pattern == null || pattern.equals("") ||
					cmd == null || cmd.equals("") ||
					type.toLowerCase().equals("folders") && (isLeaf || invar == false && folderEvent) ||
					type.toLowerCase().equals("files") && !isLeaf)
				continue;
				
			if (OS.supportsCaseSensitiveFiles() == false)	{
				name = name.toLowerCase();
				pattern = pattern.toLowerCase();
			}
			
			//System.err.println("matching "+name+" against "+pattern);
			if (cmdlist.match(name, pattern))	{
				//System.err.println(" ... success at "+i);
				if (v == null)
					v = new Vector();
				v.addElement(Integer.valueOf(i));
			}
		}
		return v;
	}


	
	/**
		Return list of String that contains all filenames without defined open-event
	*/
	public Vector getUndefined()	{
		return undefined;
	}
	
	/**
		Return list of NetNodes that contains all nodes without defined open-event
	*/
	public Vector getUndefinedNodes()	{
		return undefinedNode;
	}
	
	/**
		Return list of launch groups that were formed by the constructor
	*/
	public LaunchGroups getLaunchGroups()	{
		return launchGroups;
	}
	

	// build a popup from a launch group
		
	private JPopupMenu getOpenEventPopup(LaunchGroup group, ActionListener l)	{
		JPopupMenu popup = new JPopupMenu();
		JMenuItem [] mi = buildMenuItems(group, l);
		for (int i = 0; i < mi.length; i++)	{
			popup.add(mi[i]);
		}
		return popup;
	}
	
	/**
		Returns an array of menu items from a launch group that is not unique.
		The global variable cmdChoice is evaluated.
	*/
	public JMenuItem [] buildMenuItems(LaunchGroup group, ActionListener l)	{
		String [] cmdnames = group.getCmdNames();
		JMenuItem [] mi = new JMenuItem[cmdnames.length];
		
		cmdChoice = new Vector(cmdnames.length);
		
		for (int i = 0; i < cmdnames.length; i++)	{
			String cmdname = cmdnames[i].equals("") ?
					"please add a command-name for pattern" :
					cmdnames[i];
			String patt = group.getPattern(i);
			Vector v = new Vector(2);
			v.addElement(cmdname);
			v.addElement(patt);
			cmdChoice.addElement(v);
			cmdname = cmdname+" "+patt;
			mi[i] = new JMenuItem(cmdname);
			mi[i].setActionCommand(cmdname);
			mi[i].addActionListener(l);
		}
		
		return mi;
	}
	
	
	
	// interface ActionListener: popup callback
	
	public void actionPerformed(ActionEvent e)	{
		String s = e.getActionCommand();
		startCommandWithPattern(group, s);
	}


	/** Search for the menu command in internal list of commands with patterns 
	and start it. A call of buildMenuItems() must precede this call! */
	public void startCommandWithPattern(LaunchGroup group, String s)	{
		// locate the command
		for (int i = 0; i < cmdChoice.size(); i++)	{
			Vector v = (Vector)cmdChoice.elementAt(i);
			String cmd = (String)v.elementAt(0);
			String pat = (String)v.elementAt(1);
			if (s.equals(cmd+" "+pat))	{
				//System.err.println("launch action performed: "+s);
				startCommand(group, cmd, pat);
				break;
			}
		}
	}
	
	
	// helpers
	

	/**
		Start the command of the LaunchGroup that is not a unique one after popup choice
	*/
	private void startCommand(LaunchGroup group, String cmd, String pat)	{
		startCommand(
				cmd,
				group.getCommand(cmd, pat),
				group.getPath(cmd, pat),
				group.getEnvironment(cmd, pat),
				group.getMonitor(cmd, pat),
				group.getLoop(cmd, pat),
				group.getInvariant(cmd, pat),
				group.getNodes());
	}
	
	/**
		Start the command of the LaunchGroup that is a unique one
	*/
	private void startCommand(LaunchGroup group, String verb)	{
		startCommand(
				verb,
				group.getCommand(),
				group.getPath(),
				group.getEnvironment(),
				group.getMonitor(),
				group.getLoop(),
				group.getInvariant(),
				group.getNodes());
	}


	// method to execute one row of command-table with 1-n files
	private void startCommand(
		String verb,
		String cmd,
		String path,
		String [] env,
		boolean monitor,
		boolean loop,
		boolean invariant,
		NetNode [] n)
	{
		if (cmd == null || cmd.trim().equals(""))	{
			System.err.println("FEHLER: command is empty");
			return;
		}
		System.err.println("startCommand, verb = "+verb+", command = "+cmd);

		cmd = cmd.trim();

		// check for internal launch "macros"

		StringTokenizer stok = new StringTokenizer(cmd);
		String c1 = null;
		if (stok.hasMoreTokens())
			c1 = stok.nextToken();

		// check for JAF command verbs
		if (c1 != null && c1.equals("JAF"))	{
			CursorUtil.setWaitCursor(parent);
			try	{
				File [] files = new File[n.length];
				
				for (int i = 0; i < n.length; i++)
					files[i] = (File)n[i].getObject();
				
				JAFView jv = new JAFView();
				jv.doVerb(files, verb);
			}
			finally	{
				CursorUtil.resetWaitCursor(parent);
			}
			return;
		}
		
		if (tc != null && c1 != null)	{
			if (c1.equals("IMAGE"))	{
				CursorUtil.setWaitCursor(parent);
				try	{
					File [] files = new File[n.length];
					for (int i = 0; i < n.length; i++)
						files[i] = (File)n[i].getObject();
						
					try	{
						ImageViewer.showImages(files);
					}
					catch (NoClassDefFoundError e)	{
						System.err.println("FEHLER: JIMI (com.sun.jimi) ist nicht installiert: "+e);
					}
				}
				finally	{
					CursorUtil.resetWaitCursor(parent);
				}
				return;
			}
			else
			if (c1.equals("ARCHIVE"))	{
				CursorUtil.setWaitCursor(parent);
				try	{
					for (int i = 0; i < n.length; i++)
						tc.infoDialog(new NetNode [] { n[i] }, true);	// force archive view
				}
				finally	{
					CursorUtil.resetWaitCursor(parent);
				}
				return;
			}
			else
			if (c1.equals("HTML"))	{
				CursorUtil.setWaitCursor(parent);
				try	{
					tc.viewNodesRichText(n);
				}
				finally	{
					CursorUtil.resetWaitCursor(parent);
				}
				return;
			}
			else
			if (c1.equals("XML"))	{
				CursorUtil.setWaitCursor(parent);
				try	{
					tc.xmlEditNodeObjects(n);
				}
				finally	{
					CursorUtil.resetWaitCursor(parent);
				}
				return;
			}
			else
			if (c1.equals("VIEW"))	{
				CursorUtil.setWaitCursor(parent);
				try	{
				tc.viewNodes(n);
				}
				finally	{
					CursorUtil.resetWaitCursor(parent);
				}
				return;
			}
			else
			if (c1.equals("EDIT"))	{
				CursorUtil.setWaitCursor(parent);
				try	{
					tc.editNodeObjects(n);
				}
				finally	{
					CursorUtil.resetWaitCursor(parent);
				}
				return;
			}
		}


		// set working directory if first argument is command itself
		File workingDirectory = null;
		if (path != null && path.length() > 0)	{
	        if (path.equals("$DIR"))
	            workingDirectory = (File)n[0].getObject();
	        else
	            workingDirectory = new File(path);
		}
		else
		if (c1 != null && c1.equals("$FILE"))	{
			workingDirectory = (File)n[0].getParent().getObject();
		}

		// exec command with arguments
		if (loop)	{
			for (int i = 0; i < n.length; i++)	{
				executeCommand(cmd, c1, env, new NetNode [] { n[i] }, monitor, invariant, workingDirectory);
			}
		}
		else	{	// launch all files in one command	
			executeCommand(cmd, c1, env, n, monitor, invariant, workingDirectory);
		}
	}


	private void executeCommand(
		String cmd,
		String firstArg,
		String [] e,
		NetNode [] n,
		boolean monitor,
		boolean invariant,
		File workingDirectory)
	{
		System.err.println("executeCommand "+cmd);

		// put the selected files into command by substitution
		cmd = substitutePrivateVariables(cmd, n);	// tmpClassPath might be evaluated

		CursorUtil.setWaitCursor(parent);
		try	{
			// if this is a launch of a Java class within this VM
			if (firstArg != null && firstArg.equals("JAVA"))	{
				doInternalJavaLaunch(cmd, e);
			}
			else	{
				// look for working directory if java call
				if (workingDirectory == null &&
						firstArg != null && firstArg.equals("java") &&
						tmpClassPath != null)
				{
					if (cmd.indexOf(" -jar ") > 0)	{
						String s = new File(tmpClassPath).getParent();
						if (s != null)
							workingDirectory = new File(s);
					}
					else
					if (cmd.indexOf(" -cp ") > 0)	{
						workingDirectory = new File(tmpClassPath);
					}
				}
				
				if (monitor)	{
					new CommandMonitor(cmd, e, workingDirectory);
					
					// no wait for folders when monitored
					// as CommandMonitor runs in same event loop
					if (invariant)	{
						JOptionPane.showMessageDialog(
								parent,
								"Cannot wait for termination of command when Java-Monitor is used!\n"+
								"Please uncheck the monitor option and watch output in shell window.",
								"Warning",
								JOptionPane.WARNING_MESSAGE);
					}
				}
				else	{
					currCmd = cmd;	// for succeeded()
		
					exitcode = 0;	// assume success, if invariant is false
					
					executeUnmonitoredCommand(cmd, e, invariant, workingDirectory);
		
					if (succeeded() == false)	{
						JOptionPane.showMessageDialog(
								parent,
								"Command failed.\nExitcode "+
									getExitcode()+
									"\nError: "+getError(),
								"Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
		finally	{
			CursorUtil.resetWaitCursor(parent);
		}
	}

	
	private void executeUnmonitoredCommand(String cmd, String [] env, boolean invariant, File workingDirectory)	{
		System.err.println("executeUnmonitoredCommand "+cmd);
		String [] carr = CmdLineSubstitution.parse(cmd, env);

		try	{
			System.err.println("executing command: "+ArrayUtil.print(carr));
			final Process p = workingDirectory != null ?
					Runtime.getRuntime().exec(carr, env, workingDirectory) :
					Runtime.getRuntime().exec(carr, env);
			
			// fri_2025-10-30: fixing issue #2: need to consume stdout and stderr, 
			// else process may freeze when it writes lots of logging
			readAwayProcessOutput(p.getInputStream());
            readAwayProcessOutput(p.getErrorStream());
					
			if (invariant)	{
				System.err.println("waiting for termination of process ...");
				// wait for termination as this could be a invariant for opening a folder
				exitcode = p.waitFor();
				System.err.println(" ... terminated with "+exitcode);
			}
		}
		catch (Exception ex)	{
			System.err.println("FEHLER: execution of "+cmd);
			System.err.println(ex);
			exitcode = -1;
			error = ex.toString();
		}
	}

    private void readAwayProcessOutput(InputStream in) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (in.read() != -1) // -1 is end of stream
                        ;
                    in.close();
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }


	private void doInternalJavaLaunch(String cmd, String [] e)	{
		String [] carr = CmdLineSubstitution.parse(cmd, e);
		String clsString = carr[1];	// carr[0] is "JAVA"
		
		boolean isJar = clsString.toLowerCase().endsWith(".jar");
		boolean isClass = clsString.toLowerCase().endsWith(".class");
		
		String [] args = new String [carr.length - 2];	// "JAVA class arg1 arg2 ..."
		System.arraycopy(carr, 2, args, 0, args.length);

		// identify a possible method name
		String methodName = null;
		
		if (!isClass && !isJar)	{	// if it is a dotted class name
			String [] clsAndMethod = splitClassAndMethodName(clsString);
			// tries to get a mtehod name, according to Java Coding Conventions "my.pkg.MyClass"
			if (clsAndMethod != null)	{
				clsString = clsAndMethod[0];
				methodName = clsAndMethod[1];
			}
		}
		
		try	{
			Class cls = isJar ? Jarfile.getClass(clsString) :
					isClass ? Classfile.getClass(clsString) :
						Class.forName(clsString);
			
			if (isClass && cls == null && Classfile.error != null)
				throw new Exception(Classfile.error);	// Classfile does not throw Error
			// Jarfile throws error by itself
			
			System.err.println("Internal launch JAVA class: "+cls+(methodName != null ? " method: "+methodName : "")+" args: "+ArrayUtil.print(args));
			
			// try a lot of combinations for class and method and arguments
			callJavaInternal(cls, methodName, args);
		}
		catch (Throwable ex)	{
			if (ex instanceof InvocationTargetException)
				ex = ((InvocationTargetException)ex).getTargetException();
				
			ex.printStackTrace();
			JOptionPane.showMessageDialog(
					parent,
					ex.toString(),
					"Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	

	private String [] splitClassAndMethodName(String clsName)	{
		int last2 = clsName.lastIndexOf(".");
		if (last2 > 0 && last2 < clsName.length() - 1)	{
			char firstMethodChar = clsName.charAt(last2 + 1);
			int last1 = clsName.lastIndexOf(".", last2 - 1);
			char firstClassChar = clsName.charAt(last1 + 1);

			if (Character.isUpperCase(firstClassChar) && Character.isLowerCase(firstMethodChar))	{
				String methodName = clsName.substring(last2 + 1);
				clsName = clsName.substring(0, last2);
				return new String [] { clsName, methodName };
			}
		}
		return null;
	}
	

	private void callJavaInternal(Class cls, String methodName, String [] args)
		throws Exception
	{
		Exception toThrow = null;
		
		if (methodName != null)	{	// we have to call a method
			if (args.length <= 0)	{
				try	{
					callClassAndMethod(cls, methodName, new Class [0], new Object[0]);
					return;
				}
				catch (Exception ex)	{
					toThrow = ex;
				}
			}
			else	{
				if (args.length == 1)	{
					try	{
						callClassAndMethod(cls, methodName, new Class [] { String.class }, new Object[] { args[0] });
						return;

					}
					catch (Exception ex)	{
					}
					try	{
						callConstructorAndMethod(cls, methodName, new Class [] { String.class }, new Object[] { args[0] });
						return;

					}
					catch (Exception ex)	{
					}
				}
				
				try	{
					callClassAndMethod(cls, methodName, new Class [] { String[].class }, new Object[] { args });
					return;
				}
				catch (Exception ex)	{
					if (toThrow == null)
						toThrow = ex;
				}
			}
	
			try	{
				callConstructorAndMethod(cls, methodName, new Class [] { String[].class }, new Object[] { args });
				return;
			}
			catch (Exception ex)	{
				if (toThrow == null)
					toThrow = ex;
			}
		}
		else	{
			try	{
				Method m = cls.getMethod("main", new Class [] { String[].class });
				m.setAccessible(true);
				m.invoke(null, new Object[] { args });
				return;
			}
			catch (Exception ex)	{
				toThrow = ex;
			}

			if (args.length <= 0)	{
				try	{
					cls.newInstance();
					return;
				}
				catch (Exception ex)	{
				}
			}
			else
			if (args.length == 1)	{
				try	{
					Constructor constr = cls.getConstructor(new Class [] { String.class });
					constr.newInstance(new Object [] { args[0] });
					return;
				}
				catch (Exception ex)	{
				}
			}

			try	{
				Constructor constr = cls.getConstructor(new Class [] { String[].class });
				constr.newInstance(new Object [] { args });
				return;
			}
			catch (Exception ex)	{
			}
		}
		
		throw toThrow;
	}


	private void callClassAndMethod(Class cls, String methodName, Class [] argClasses, Object [] args)
		throws Exception
	{
		Method m = cls.getMethod(methodName, argClasses);
		Object o = null;
		if (Modifier.isStatic(m.getModifiers()) == false)
			o = cls.newInstance();
		m.invoke(o, args);
	}

	private void callConstructorAndMethod(Class cls, String methodName, Class [] argClasses, Object [] args)
		throws Exception
	{
		Constructor constr = cls.getConstructor(argClasses);
		Object o = constr.newInstance(args);
		Method m = cls.getMethod(methodName, new Class[0]);
		m.invoke(o, new Object[0]);
	}





	public boolean succeeded()	{
		//System.err.println("exit code of "+currCmd+" is "+exitcode);
		return exitcode == 0;
	}
	
	public String getCommand()	{
		return currCmd;
	}
	
	public int getExitcode()	{
		return exitcode;
	}
	
	public String getError()	{
		return error;
	}
	
	
	
	private String substitutePrivateVariables(String cmd, NetNode [] n)	{
		Hashtable hash = new Hashtable();
		
		if (cmd.indexOf("FILE") >= 0)
			hash.put("FILE", buildFILE(n));	// whole path with file with extension
		if (cmd.indexOf("DIR") >= 0)
			hash.put("DIR", buildDIR(n));
		if (cmd.indexOf("BASE") >= 0)
			hash.put("BASE", buildBASE(n));
		if (cmd.indexOf("BASEEXT") >= 0)
			hash.put("BASEEXT", buildBASEEXT(n));
		if (cmd.indexOf("EXT") >= 0)
			hash.put("EXT", buildEXT(n));
		if (cmd.indexOf("FILEBASE") >= 0)
			hash.put("FILEBASE", buildFILEBASE(n));
		if (cmd.indexOf("CLASSPATH") >= 0)
			hash.put("CLASSPATH", buildCLASSPATH(n));
			
		CmdLineSubstitution.setCaseSensitive(false);
		String s = CmdLineSubstitution.stringSubstitution(cmd, hash);
		CmdLineSubstitution.setCaseSensitive(true);
		return s;
	}

	private String buildCLASSPATH(NetNode [] n)	{
		StringBuffer sb = new StringBuffer();
		sb.append('"');

		String nextClassPath = "";
		for (int i = 0; i < n.length; i++)	{
			nextClassPath = Classfile.getClassPath(n[i].getFullText(), nextClassPath);
			if (i == 0 && nextClassPath.length() > 0)
				tmpClassPath = nextClassPath;
		}
		
		if (nextClassPath.length() <= 0)
			sb.append(tmpClassPath = System.getProperty("user.dir"));
		else
			sb.append(nextClassPath);
		
		//String classPath = System.getProperty("java.class.path");
		//sb.append(File.pathSeparator+classPath);

		sb.append('"');
		return sb.toString();
	}

	private String buildBASE(NetNode [] n)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n.length; i++)	{
			beginString(i, sb);
			sb.append(Classfile.getClassName(n[i].getFullText()));
			endString(i, n.length, sb);
		}
		return sb.toString();
	}

	private String buildFILE(NetNode [] n)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n.length; i++)	{
			beginString(i, sb);
			sb.append(n[i].getFullText());
			endString(i, n.length, sb);
		}
		return sb.toString();
	}

	private String buildDIR(NetNode [] n)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n.length; i++)	{
			beginString(i, sb);
			sb.append(n[i].getParent().getFullText());
			endString(i, n.length, sb);
		}
		return sb.toString();
	}

	private String buildBASEEXT(NetNode [] n)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n.length; i++)	{
			beginString(i, sb);
			sb.append(n[i].getLabel());
			endString(i, n.length, sb);
		}
		return sb.toString();
	}

	private String buildEXT(NetNode [] n)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n.length; i++)	{
			sb.append(FileUtil.getExtension(n[i].getLabel()));
		}
		return sb.toString();
	}

	private String buildFILEBASE(NetNode [] n)	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n.length; i++)	{
			beginString(i, sb);
			sb.append(FileUtil.cutExtension(n[i].getFullText()));
			endString(i, n.length, sb);
		}
		return sb.toString();
	}


	private void beginString(int i, StringBuffer sb)	{	
		sb.append('"');
	}

	private void endString(int i, int len, StringBuffer sb)	{	
		if (i < len - 1)
			sb.append("\" ");
		else
			sb.append("\"");
	}
	
}