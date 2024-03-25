package fri.gui.swing.filebrowser;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.activation.*;
import fri.util.activation.*;
import fri.util.os.OS;
import fri.util.props.PropertyUtil;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.install.InstallLibraries;

/**
	Binding to javax.activation framework (JAF) for filebrowser.
	A WINDOWS Registry implementation and a UNIX mailcap
	implementation are provided in fri.util.activation.
	<p>
	If the framework returns Java Components, they are shown in a
	JFrame or a Frame, depending on what their class is derived.
	There will be ONE Frame for all Components and ONE JFrame for all JComponents.
	<p>
	Configuration of JAFView:
	<ul>
		<li>Setting <b>"java -D filebrowser.actions.forceMailcap=true ..."</b> forces
			this class to use fri.util.activation.MailcapCommandMap instead
			of platform specific CommandMap implementation. This provides executable
			Commands from the mailcap file. This is default for the UNIX platform.
			</li>
		<li>Setting <b>"java -D filebrowser.actions.JAF=false ..."</b> switches off
			all JAF actions for file types, no platform commands and no Java classes
			will be found.
			</li>
		<li>Setting <b>"java -D filebrowser.actions.JAF_Platform=false ..."</b> switches off
			all JAF actions that depend on the current platform (e.g. no Registry binding
			for WINDOWS is wanted).
			</li>
		<li>Setting <b>"java -D filebrowser.actions.JAF_Java=false ..."</b> switches off
			all JAF actions that are retrieved from the default JAF implementation.
			So you cannot use com.sun.activation.viewers classes: ImageViewer, AWT
			text editor and viewer, and anything that was put into a .mailcap file
			in HOME directory.
			</li>
	</ul>
*/

public class JAFView
{
	private Frame frame = null;
	private Panel panel;
	private JFrame jframe = null;
	private JPanel jpanel;
	//private String verb = OS.isWindows ? "open" : OS.isUnix ? "open" : "view";
	private static CommandMap defaultMap = null;
	private static FileTypeMap defaultTypes = null;


	// get verbs for JAF commands
		
	/**
		Returns all defined verbs for this file type.
		The verbs are tagged by hints that tell if they
		are retrieved from platform or from JAF classes:
		"Open (WINDOWS)" or "View (JAVA)". These tags are
		removed automatically by using <code>JAFView.doVerb(files, verb)</code>.
	*/
	public String [] getCommandVerbs(File file)	{
		if (PropertyUtil.checkSystemProperty("filebrowser.actions.JAF", "false"))
			return null;
		
		ensureInit();
		
		String [] sarr = null;
		if (PropertyUtil.checkSystemProperty("filebrowser.actions.JAF_Platform", "false") == false)
		{
			sarr = getCommandVerbsFromMaps(
					file,
					FileTypeMap.getDefaultFileTypeMap(),
					CommandMap.getDefaultCommandMap());
		}

		int l = addVerbHint(sarr, getPlatformVerbHint());

		String [] sarr2 = null;
		if (PropertyUtil.checkSystemProperty("filebrowser.actions.JAF_Java", "false") == false &&
				defaultMap != CommandMap.getDefaultCommandMap())
		{
			sarr2 = getCommandVerbsFromMaps(
					file,
					defaultTypes,
					defaultMap);
		}

		int l2 = addVerbHint(sarr2, getJavaVerbHint());
		
		String [] all = new String [l + l2];
		if (l > 0)
			System.arraycopy(sarr, 0, all, 0, l);
		if (l2 > 0)
			System.arraycopy(sarr2, 0, all, l, l2);
				
		return all;
	}


	private int addVerbHint(String [] sarr, String hint)	{
		if (sarr == null || sarr.length <= 0)
			return 0;
		
		for (int i = 0; i < sarr.length; i++)	{
			sarr[i] = sarr[i]+hint;
		}
		
		//System.err.println("JAFView, Command Verbs are: "+ArrayUtil.print(sarr));	
		return sarr.length;
	}
	

	// Returns all defined verbs for this file type from passed maps.
	private String [] getCommandVerbsFromMaps(
		File file,
		FileTypeMap types,
		CommandMap map)
	{
		try	{
			String mimeType = types.getContentType(file);
			CommandInfo [] ci = map.getPreferredCommands(mimeType);

			if (ci != null && ci.length > 0)	{
				String [] sarr = new String[ci.length];
				for (int i = 0; i < ci.length; i++)
					sarr[i] = ci[i].getCommandName();
					
				return sarr;
			}
		}
		catch (NullPointerException e)	{
			// at javax.activation.MimeType.parse(MimeType.java:86)
			// reason is: WINDOWS mime types may be Registry verbs like "java_file"
			e.printStackTrace();
			return null;	// defaultMap will not be able to process verb
		}
		
		return null;
	}

	private String getPlatformVerbHint()	{
		return " ("+OS.getName()+")";	// "Open (WINDOWS)"
	}
	
	private String getJavaVerbHint()	{
		return " (JAVA)";	// "Open (JAVA)"
	}


	// execute JAF verb

	/**
		Do the passed verb for all passed files. If the JAF bean is a Component,
		it will be shown in a Frame, one for all resulting Components.
		Else nothing happens, except if setCommandContext() of the bean
		does something by itself.
	*/
	public void doVerb(File [] files, String verb)	{
		ensureInit();
		
		for (int i = files.length - 1; i >= 0; i--)	{
			Object o = getBean(files[i], verb);
			
			if (o instanceof JComponent)	{
				ensureJFrame().add((JComponent)o);
			}
			else
			if (o instanceof Component)	{
				ensureFrame().add((Component)o);
			}
			else	{
				System.err.println("viewer was no Component, must show itself: "+o);
			}
		}
		ensureShowing();
	}


	// construct the Java Bean and setCommandContext()
	private Object getBean(File file, String verb)	{
		if (verb.endsWith(getPlatformVerbHint()))	{
			// remove "WINDOWS" tag from verb
			verb = verb.substring(0, verb.length() - getPlatformVerbHint().length());
			System.err.println("JAFView get Platform bean: "+verb+", "+file);
			
			try	{
				return getBean(file, verb, null, null);
			}
			catch (NullPointerException e)	{
				// at javax.activation.MimeType.parse(MimeType.java:86)
				// reason is: WINDOWS mime types may be Registry verbs like "java_file"
				e.printStackTrace();
				return null;	// defaultMap will not be able to process verb
			}
		}
		else
		if (verb.endsWith(getJavaVerbHint()))	{
			// remove "JAVA" tag from verb
			verb = verb.substring(0, verb.length() - getJavaVerbHint().length());
			System.err.println("JAFView get Java bean: "+verb+", "+file);
			
			try	{
				return getBean(file, verb, defaultTypes, defaultMap);
			}
			catch (NullPointerException e)	{	// at javax.activation.MimeType.parse(MimeType.java:86)
				e.printStackTrace();
				return null;
			}
		}
		else	{
			throw new IllegalArgumentException("JAFView, wrong verb was passed: "+verb);
		}
	}


	private Object getBean(File file, String verb, FileTypeMap types, CommandMap map)	{
		try	{
			FileDataSource fds = new FileDataSource(file);
			if (types != null)
				fds.setFileTypeMap(types);
				
			DataHandler dh = new DataHandler(fds); 			
			if (map != null)
				dh.setCommandMap(map);
			
			CommandInfo ci = dh.getCommand(verb);
			if (ci != null)	{
				//return dh.getBean(ci);	// catches Exceptions and does not report them
				return ci.getCommandObject(dh, dh.getClass().getClassLoader());
			}
			System.err.println("JAFView no bean found for: "+verb+", "+file+", CommandInfo is null!");
		}
		catch (ClassNotFoundException e)	{
			e.printStackTrace();
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
		return null;
	}
	

	/**
	 * Loads good activation implementations into Factories.
	 * <p />
	 * FRi 2008-01-14: this is code duplication of GenericCommandLauncher.
	 * The difference is that here implementations are loaded explicitely, whereby
	 * GenericCommandLauncher loads by Class.forName().
	 */
	private void ensureInit()	{
		if (defaultMap == null)	{
			defaultMap = CommandMap.getDefaultCommandMap();
			defaultTypes = FileTypeMap.getDefaultFileTypeMap();
		}
		
		// set platform fitting CommandMap and FileTypeMap to JAF
		// ... don't know what will happen on Mac ...
		if (OS.isWindows == false || Boolean.getBoolean("filebrowser.actions.forceMailcap"))
		{
			if (CommandMap.getDefaultCommandMap() instanceof fri.util.activation.MailcapCommandMap == false)	{
				CommandMap.setDefaultCommandMap(new fri.util.activation.MailcapCommandMap());
				FileTypeMap.setDefaultFileTypeMap(new fri.util.activation.MimetypesFileTypeMap());
			}
		}
		else
		if (OS.isWindows)	{
			try	{
				// ensure that DLL's are unpacked in current directory
				InstallLibraries.ensure(new String [] {
						Win32Shell.getDdeDLLBaseName(),
						Win32Shell.getRegistryDLLBaseName()
					});
				
				if (Win32Shell.testWin32ActivationDLLs())	{
					if (CommandMap.getDefaultCommandMap() instanceof Win32RegistryCommandMap == false)	{
						CommandMap.setDefaultCommandMap(new Win32RegistryCommandMap());
						FileTypeMap.setDefaultFileTypeMap(new Win32RegistryFileTypeMap());
					}
				}
			}
			catch (Error err)	{
				err.printStackTrace();
				System.err.println("FEHLER: icejni.jar nicht installiert: "+err);
			}
		}
	}


	private JPanel ensureJFrame()	{
		// allocate one frame for all Components from JAF if necessary
		if (jframe == null)	{
			jframe = new JFrame("JAF File Viewer (Swing)");
			jpanel = new JPanel();
			JScrollPane sp = new JScrollPane(jpanel);
			jframe.getContentPane().add(sp);
		}
		return jpanel;
	}

	private Panel ensureFrame()	{
		// allocate one frame for all Components from JAF if necessary
		if (frame == null)	{
			frame = new Frame("JAF File Viewer (AWT)");
			panel = new Panel();
			ScrollPane sp = new ScrollPane();
			sp.add(panel);
			frame.add(sp);
		}
		return panel;
	}

	private void ensureShowing()	{
		// if a frame was allocated, show it on screen, else do nothing
		if (jframe != null)	{
			new GeometryManager(jframe, false).show();

			jframe.addWindowListener(new WindowAdapter()	{
				public void windowClosing(WindowEvent e)	{
					jframe.dispose();
				}
			});
		}
		
		if (frame != null)	{
			new GeometryManager(frame, false).show();

			frame.addWindowListener(new WindowAdapter()	{
				public void windowClosing(WindowEvent e)	{
					frame.dispose();
				}
			});
		}
	}
	
}
