package fri.gui.swing.filebrowser;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;

import fri.gui.GuiConfig;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.application.GuiApplication;
import fri.util.NumberUtil;
import fri.util.application.Application;

/**
	Hierarchical File-System Explorer. Main-Class for this package.

	@author Fritz Ritzberger
*/
public class FileBrowser extends GuiApplication
{
	private static final long serialVersionUID = 1L;

	public final static String configDir = GuiConfig.dir()+"filebrowser";
	private final static String propfile = configDir+File.separator+"FileBrowser.properties";
	private static NetWatcher netWatcher = null;

	public static Properties manifest;
	private static boolean closingAll = false;
	
	private TreePanel treepanel;
	int myinstance;

	/** Construct an hierarchical filesystem browser. */
	public FileBrowser()	{
		NetNode root;
		boolean persistentFound;

		// try to load persistent treepanel configuration
		PathPersistent pp = new PathPersistent(propfile);
		if (pp.getRoot() != null)	{	// persistent state was found
			root = FileNode.constructRoot(pp.getRoot(), pp.getWastebasket());
			persistentFound = true;
		}
		else	{	// no persistent state
			root = FileNode.constructRoot((String)null);
			persistentFound = false;
		}
		
		init1(root, pp);	// now treepanel exists
		
		if (persistentFound)	// open and select given pathes
			treepanel.expandiere(pp.getSelected(), pp.getPathes());
		else	// default expand only root
			treepanel.expandiere();
		
		String free = NumberUtil.getFileSizeString(Runtime.getRuntime().freeMemory());
		String total = NumberUtil.getFileSizeString(Runtime.getRuntime().totalMemory());
		String max = NumberUtil.getFileSizeString(Runtime.getRuntime().maxMemory());
		System.err.println("Memory: max="+max+", total="+total+", free="+free);
	}

	/** Internal constructor: Create a new window with a copy of current tree model */
	FileBrowser(DefaultTreeModel model)	{
		PathPersistent pp = new PathPersistent(propfile);	// was saved before
		treepanel = new TreePanel(this, model, pp);
		init2(treepanel, pp);
		treepanel.expandiere(pp.getSelected(), pp.getPathes());	// open given path
	}
	
	
	private void init1(NetNode root, PathPersistent pp)	{
		treepanel = new TreePanel(this, root, pp);
		init2(treepanel, pp);
	}
	
	private void init2(TreePanel treepanel, PathPersistent pp)	{
		setTitle(implementation());
		if (pp != null)	{
			pp.setTree(treepanel.getTree());
		}
		Container c = getContentPane();
		c.add(treepanel, BorderLayout.CENTER);
		
		myinstance = Application.instances(getClass());
		
		setStyle(GeometryManager.TILING);
		super.init(treepanel.getCustomizeButton(), new Object [] { treepanel.getPopupMenu() });

		// start a background thread for this view that watches the opened pathes
		setAutoRefresh(treepanel.isAutoRefresh());
	}


	/** Schaltet den NetWatcher ein/aus */
	public void setAutoRefresh(boolean refresh)	{
		//System.err.println("FileBrowser setAutoRefresh "+refresh);
		if (refresh && netWatcher == null)	{
			netWatcher = new NetWatcher(treepanel.getRootNode(), treepanel.getEditController());
			//System.err.println("Started auto refresh thread ..."+netWatcher);
		}
		else
		if (!refresh && netWatcher != null)	{
			netWatcher.stopRefresh();
			netWatcher = null;
		}
	}


	/** Implements Closeable: Schliessen des Fensters */
	public boolean close()	{
		if (closingAll == false && isLastToSave())	{
			/*
			int ret = JOptionPane.showConfirmDialog(
							this,
							"Really Exit File Browser?",
							"Finish Application",
							JOptionPane.YES_NO_OPTION);			
			if (ret != JOptionPane.YES_OPTION)	{
				return false;
			}
			*/
		}
		else	{
			treepanel.removeNodeListeners();
		}
		
		if (isLastToSave())	{
			treepanel.save();	// save data to filesystem
		}
		
		return super.close();
	}

	private boolean isLastToSave()	{
		return Application.instances(getClass()) <= 0;
	}
	

	/** Callback fuer Menu-Item "Close All" */
	public void closeAll()	{
		/*
		int ret = JOptionPane.showConfirmDialog(
						this,
						"Close All Windows and Exit?",
						"Finish Application",
						JOptionPane.YES_NO_OPTION);			
		if (ret != JOptionPane.YES_OPTION)
			return;
		*/
		closingAll = true;
		Application.closeAllExit();
		closingAll = false;
	}


	// interface WindowListener

	public void windowActivated(WindowEvent e) {
		super.windowActivated(e);

		System.err.println("windowActivated instance "+myinstance+" tree hashcode "+treepanel.getTree().hashCode());
		treepanel.setEnabledActions();	// inherit global settings of another instance
		
		//treepanel.getTree().requestFocus();	// leads to infinite loop in JDK 1.4!
		
		treepanel.getEditController().checkClipboard();
		treepanel.commitFilterOnWindowActivated();
	}
	
	public void windowOpened(WindowEvent e) {
		treepanel.getTree().requestFocus();
	}

	public static Properties manifest() {
		if(manifest==null) {
			manifest = new Properties();
			try {
				manifest.load(FileBrowser.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return manifest;
	}
	public static String implementationName() {
		return manifest().getProperty("Implementation-Title");
	}

	public static String implementationVersion() {
		return manifest().getProperty("Implementation-Version");
	}

	public static String implementation() {
		return implementationName()+" "+implementationVersion();
	}
	
	/** FileBrowser appliction main. */
	public static final void main (String [] args)	{
		System.out.println(implementation()+", platform independent file manager. Author Fritz Ritzberger, Vienna 1999-2024");
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				new FileBrowser();
			}
		});
	}
}
