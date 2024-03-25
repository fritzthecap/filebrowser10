package fri.gui.swing.htmlbrowser;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.datatransfer.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.beans.*;
import java.io.*;
import javax.swing.text.html.*;
import java.util.Vector;
import fri.util.NetUtil;
import fri.util.html.*;
import fri.util.application.Closeable;
import fri.util.os.OS;
import fri.gui.CursorUtil;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.textviewer.*;
import fri.gui.swing.iconbuilder.Icons;
import fri.gui.swing.actionmanager.ActionManager;

/**
	A Textviewer for RTF and HTML, loading hyperlinks.
	<p />
	TODO: A viewed file can not be removed from filesystem after viewing on WINDOWS,
		even when the HTMLViewer has been closed.
*/

public class HTMLViewer extends JPanel implements
	HyperlinkListener,
	ActionListener,
	HtmlStructureRenderer,
	Closeable,
	Runnable,
	PropertyChangeListener 
{
	protected JEditorPane ta;
	protected HyperlinkCombo tf;
	protected Component stop;
	private int historyPos = 0;
	private Vector history = new Vector();
	protected URL currURL = null, oldURL = null;
	private boolean historyComboSelection = false;
	private boolean addToHistory = false;
	private JTextField status;
	private boolean stopped = false;
	private String error = null;
	private String contentType = null;
	private String currentHyperlink = null;
	protected ActionManager am;
	private Point pos;
	private JViewport viewPort;
	private InputStream currentInputStream;
 
	
	/** Create a empty HTML viewer. */
	public HTMLViewer()	{
		this((File)null);
	}
	
	/** Create a HTML viewer and load an URL from a String. */
	public HTMLViewer(String urlStr)	{
		this((File)null);
		try	{
			URL url = new URL(urlStr);
			loadURL(url);
		}
		catch (MalformedURLException e)	{
			e.printStackTrace();
		}
	}
	
	/** Create a HTML viewer and load an URL. */
	public HTMLViewer(URL url)	{
		this((File)null);
		loadURL(url);
	}
	
	/** Create a HTML viewer and load a file. */
	public HTMLViewer(File f)	{
		super(new BorderLayout());
		
		HttpProxyDialog.load();
		
		URL url = null;
		if (f != null)	{
			url = urlFromFile(f);
		}
			
		Container c = this;
	
		status = new JTextField();
		status.setEditable(false);
		status.setBorder(BorderFactory.createLoweredBevelBorder());
		
		c.add(status, BorderLayout.SOUTH);
	
		ta = new InterruptableJEditorPane();
		ta.setEditable(false);
		ta.addHyperlinkListener(this);
		ta.addPropertyChangeListener(this);
		
		JScrollPane sp = new JScrollPane(ta);
		viewPort = sp.getViewport();
		sp.setPreferredSize(new Dimension(700, 800));
		c.add(sp, BorderLayout.CENTER);
		
		tf = new HyperlinkCombo();
		tf.setText("");	// no page is loaded at start
		tf.addActionListener(this);
		
		// add GUI Components for navigation
		JToolBar tb = new JToolBar(SwingConstants.HORIZONTAL);
		if (OS.isAboveJava13) tb.setRollover(true);
		
		// Actions
		am = new ActionManager(ta, this);

		am.registerAction("Back", Icons.get(Icons.back), "Go Back To Previous Page", KeyEvent.VK_LEFT, InputEvent.ALT_MASK);
		am.registerAction("Forward", Icons.get(Icons.forward), "Go Forward To Next Page", KeyEvent.VK_RIGHT, InputEvent.ALT_MASK);
		am.registerAction("View", Icons.get(Icons.eye), "View Page Source", KeyEvent.VK_U, InputEvent.CTRL_MASK);
		am.registerAction("Reload", Icons.get(Icons.refresh), "Reload Page", KeyEvent.VK_R, InputEvent.CTRL_MASK);
		am.registerAction("Stop", Icons.get(Icons.stop), "Interrupt Page Loading", KeyEvent.VK_ESCAPE, 0);
		if (needDownloadButtons())	{
			am.registerAction("Download", Icons.get(Icons.download), "Download Document Hierarchy (HTML Spider)");
			am.registerAction("Proxy", Icons.get(Icons.configure), "HTTP Proxy Settings");
		}
	
		am.visualizeAction("Back", tb);
		am.visualizeAction("Forward", tb);
		am.visualizeAction("View", tb);
		am.visualizeAction("Reload", tb);
		stop = am.visualizeAction("Stop", tb);

		if (needDownloadButtons())	{
			am.visualizeAction("Download", tb);
			am.visualizeAction("Proxy", tb);
		}

		am.setAllDisabled();
		am.setEnabled("Reload", true);

		if (needDownloadButtons())	{
			am.setEnabled("Download", true);
			am.setEnabled("Proxy", true);
		}

		stopThread(false, false);

		// receive drag&drop of files, send hyperlinks
		new TreeTableDndListener(ta, this)	{
			public Transferable sendTransferable()	{
				if (currentHyperlink == null)
					return null;	// do not send anything except hyperlinks
				return super.sendTransferable();
			}
		};
		
		// finish GUI
		
		tb.add(tf);

		c.add(tb, BorderLayout.NORTH);
		
		//System.err.println("Loading file URL: "+url);
		gotoURL(url);
	}
	
	
	/** Adds a button to toolbar. Override to change button behaviour. */
	protected JButton addButton(Action action, JToolBar tb, String tooltip)	{
		JButton b = tb.add(action);
		b.setAlignmentY(CENTER_ALIGNMENT);
		b.setAlignmentX(CENTER_ALIGNMENT);
		b.setToolTipText(tooltip);
		return b;
	}
	
	/** Returns true if "Download" and "Proxy" should be in toolbar. To be overridden. */
	protected boolean needDownloadButtons()	{
		return true;
	}
	

	/** Makes an "file:/" URL from a file. */
	public static URL urlFromFile(File f)	{
		try	{
			return NetUtil.makeURL(f);
		}
		catch (MalformedURLException mue) {
			System.err.println("FEHLER: ungueltige URL: "+f);
		}
		return null;
	}
	
	

	/** Implements PropertyChangeListener to disable stop button. */
	public void propertyChange(PropertyChangeEvent e)	{
		System.err.println("propertyChange "+e.getPropertyName());
		if (e.getPropertyName().equals("page"))	{
			stopThread(false, false);
							
			if (pos != null)	{
				viewPort.setViewPosition(pos);
				System.err.println("Setting view position to "+pos);
				pos = null;
			}

			if (currentInputStream != null)	{
				try	{ currentInputStream.close(); } catch (IOException ex)	{}
				currentInputStream = null;
				System.gc();	// stimulate garbage collector to close all open streams to some file (WINDOWS)
				System.gc();
			}
		}
	}
	
	
	
	/** Interrupt page loading. */
	protected synchronized void stopIt()	{
		System.err.println("STOP LOADING !!!");
		stopped = true;
	}
	
	
	/** Load page source text into a text viewer. */
	protected void viewSource()	{
		TextViewer.singleton(tf.getText(), ta.getText());
	}


	/** Re-Load current page. Trick out JEditorPane by setting Document.StreamDescriptionProperty to null. */
	protected void reload()	{
		// trick out setPage() call that checks for same URL
		ta.getDocument().putProperty(Document.StreamDescriptionProperty, null);
		
		// remember curent view position
		pos = viewPort.getViewPosition();
		System.err.println("View position is "+pos);
		
		// load current URL or URL from text field
		refreshFromTextField(tf.getText().equals(renderURL(currURL)) == false);
	}


	/** Go back in history and load the previous URL. */
	protected void back()	{
		if (historyPos > 1)	{
			historyPos--;
			URL url = (URL)history.elementAt(historyPos - 1);
			setNavigationEnabled();
			gotoURL(url, false);
		}
		else
			getToolkit().beep();
		System.err.println("back, history position "+historyPos+", size "+history.size());
	}
	
	/** Go forward in history and load the next URL. */
	protected void forward()	{
		if (history.size() > historyPos)	{
			URL url = (URL)history.elementAt(historyPos);
			historyPos++;
			setNavigationEnabled();
			gotoURL(url, false);
		}
		else
			getToolkit().beep();
		System.err.println("forward, history position "+historyPos+", size "+history.size());
	}

	private void setNavigationEnabled()	{
		am.setEnabled("Back", historyPos > 1);
		am.setEnabled("Forward", historyPos < history.size());
		am.setEnabled("Reload", true);
		am.setEnabled("View", true);
	}


	// begin interface HtmlStructureRenderer
	
	/** Implements HtmlStructureRenderer. */
	public void loadURL(String urlStr)	{
		try	{
			URL url = new URL(urlStr);
			loadURL(url);
		}
		catch (MalformedURLException ex)	{
			ex.printStackTrace();
		}
	}
	
	/** Implements HtmlStructureRenderer. */
	public String getSelectedURL()	{
		if (currentHyperlink != null)
			return currentHyperlink;

		if (currURL == null)
			return null;
		
		try	{
			return Util.plainUrl(currURL.toExternalForm()).toExternalForm();
		}
		catch (MalformedURLException e)	{
			return null;
		}
	}

	// end interface HtmlStructureRenderer


	/** Load the passed URL into HTML window. Do nothing if already loaded. */
	public void loadURL(URL url)	{
		if (currURL != null && url != null && currURL.equals(url))
			return;
		gotoURL(url);
	}


	/** Call gotoURL(url, true) if url is not null. */
	protected void gotoURL(URL url)	{
		if (url != null)
			gotoURL(url, true);
	}


	/** Load a file by converting it to an URL. Calls gotoURL(url). */
	public void gotoFile(File f)	{
		URL url = urlFromFile(f);
		if (url != null)
			gotoURL(url);
	}


	// Hack to adjust URLs
	private URL correctURL(URL url)
		throws MalformedURLException
	{
		String urlStr = url.toExternalForm();

		int offs = urlStr.indexOf("localhost//");
		while (offs >= 0)	{
			int offs2 = offs + "localhost/".length();
			urlStr = urlStr.substring(0, offs)+"localhost"+urlStr.substring(offs2);
			offs = urlStr.indexOf("localhost//");
		}
		
		if (Util.urlStrIsDir(urlStr) && urlStr.endsWith("/") == false)
			urlStr = urlStr+"/";
		
		URL newURL = new URL(urlStr);
		
		return newURL;
	}

		
	/** Go to the given URL and add it to history if requested. */
	protected void gotoURL(URL url, boolean addToHistory)	{
		CursorUtil.setWaitCursor(this);
		stop.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		status.setText("");
		
		oldURL = currURL;
		currURL = url;
		
		currentHyperlink = null;
		
		this.addToHistory = addToHistory;
		
		stopThread(false, true);

		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	

	private synchronized void stopThread(boolean stopped, boolean enabled)	{
		this.stopped = stopped;
		System.err.println("stopThread, stopped "+stopped+", enable "+enabled);
		am.setEnabled("Stop", enabled);
		CursorUtil.resetWaitCursor(this);
	}

	
	/**
		Implements Runnable to connect to an URL deferred in background.
		Loading is done in background by JEditorPane, but not connecting.
	*/
	public void run()	{
		error = null;
		contentType = null;
		
		try	{
			currURL = correctURL(currURL);
			
			URLConnection uc = currURL.openConnection();
			uc.connect();
			contentType = uc.getContentType().toLowerCase();	// NullPointerException!
			System.err.println("Got content type >"+contentType+"< for "+currURL);
		}
		catch(Exception e)	{
			error = "The URL \""+currURL+"\" could not be opened: "+e;
			System.err.println("Error loading page "+currURL+", message is "+e);
		}
		
		final boolean openFatal = (error != null);
		final boolean preloadFatal;
		if (error == null)	{		
			if (contentType == null)	{
				error = "No content type, no connection to URL "+currURL;
				preloadFatal = false;
			}
			else
			if (false == contentType.startsWith("text/html") &&
					false == contentType.startsWith("text/plain") &&
					false == contentType.startsWith("text/rtf") &&
					false == contentType.startsWith("application/rtf"))
			{
				error = "Can not load content type "+contentType;
				preloadFatal = false;
			}
			else
			if (checkDownloadableURL(currURL.toString()) == false)	{	// e.g. "mailto:" URL
				error = "The URL is is not viewable: "+currURL;
				preloadFatal = true;
			}
			else	{
				preloadFatal = false;
			}
		}
		else	{
			preloadFatal = false;
		}
		
		if (stopped)	{
			stopThread(false, false);
			pos = null;
			return;
		}
		
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				try	{
					boolean fatal = false;
					
					if (error == null)	{
						try	{
							ta.setContentType(contentType);
	
							System.err.println("goto URL "+currURL);
							ta.setPage(currURL);
							System.err.println(" ... gone");
							
							if (historyComboSelection == false)	{	// combo sets text by itself
								tf.setText(renderURL(currURL));
							}
							
							if (addToHistory && (oldURL == null || oldURL.equals(currURL) == false))	{
								// clean history for new path
								for (int i = history.size() - 1; i >= historyPos; i--)
									history.removeElementAt(i);
								
								history.addElement(currURL);
								historyPos++;
								//System.err.println("gotoURL "+url+" history position "+historyPos+", size "+history.size());
								setNavigationEnabled();
							}
						}
						catch(IOException io)	{
							error = "The URL \""+currURL+"\" could not be loaded.";
							fatal = true;
						}
						catch (Exception e)	{
							e.printStackTrace();
							error = e.getMessage();
							fatal = true;
						}
					}
					
					if (error != null && stopped == false)	{
						System.err.println("Error was: "+error);
						if (openFatal == false && preloadFatal == false && fatal == false)	// might be .jpg file or something other not viewable
							new DownloadDialog(ComponentUtil.getFrame(HTMLViewer.this), currURL);
						else
							error(error);
							
						currURL = oldURL;
						stopThread(false, false);
					}
				}
				finally	{
					CursorUtil.resetWaitCursor(HTMLViewer.this);
				}
			}
		});
	}


	private boolean checkDownloadableURL(String urlStr) {
		String protocol = urlStr.substring(0, urlStr.indexOf(':'));
		if (protocol.equalsIgnoreCase("gopher")
				|| protocol.equalsIgnoreCase("telnet")
				|| protocol.equalsIgnoreCase("news")
				|| protocol.equalsIgnoreCase("mailto")
				|| protocol.equalsIgnoreCase("javascript")) {
			return false;
		}
		return true;
	}

	/** Returns an URL string to render in the history combobox */
	protected String renderURL(URL url)	{
		if (url == null)
			return "";
		String urlStr = url.toExternalForm();
		//System.err.println("renderURL "+urlStr);
		return urlStr;
	}


	/** Common error routing popping up a modal dialog, running in event thread. */
	public static void error(final String msg)	{
		JOptionPane.showMessageDialog(
				null,
				msg,
				"Error",
				JOptionPane.ERROR_MESSAGE);
	}



	/** implements HyperlinkListener */
	public void hyperlinkUpdate(HyperlinkEvent evt)	{
		//System.err.println("HTMLViewer.hyperlinkUpdate: "+evt);
		if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)	{
			if (evt instanceof HTMLFrameHyperlinkEvent)	{
				HTMLDocument hd = (HTMLDocument)ta.getDocument();
				hd.processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent)evt);
			}
			else	{
				gotoURL(evt.getURL(), true);
			}
		}
		else
		if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED)	{
			//System.err.println("hyperlink entered");
			status.setText(currentHyperlink = evt.getURL().toExternalForm());			
		}
		else
		if (evt.getEventType() == HyperlinkEvent.EventType.EXITED)	{
			//System.err.println("hyperlink exited");
			status.setText("");
			currentHyperlink = null;
		}
	}


	/** implements ActionListener to get HistoryCombo event */
	public void actionPerformed(ActionEvent e)	{
		//System.err.println("HTMLViewer.actionPerformed: "+tf.getText());
		if (e.getSource() == tf)	{
			historyComboSelection = true;
			refreshFromTextField(true);
			historyComboSelection = false;
		}
		else
		if (e.getActionCommand().equals("Back"))	{
			back();
		}
		else
		if (e.getActionCommand().equals("Forward"))	{
			forward();
		}
		else
		if (e.getActionCommand().equals("Reload"))	{
			reload();
		}
		else
		if (e.getActionCommand().equals("Stop"))	{
			stopIt();
		}
		else
		if (e.getActionCommand().equals("View"))	{
			viewSource();
		}
		else
		if (e.getActionCommand().equals("Download"))	{
			downloadHierarchy();
		}
		else
		if (e.getActionCommand().equals("Proxy"))	{
			new HttpProxyDialog((JFrame)ComponentUtil.getFrame(this));
		}
	}

	private void downloadHierarchy()	{
		String url = getRenderedURL();
		if (url.length() <= 0)	{
			JOptionPane.showMessageDialog(this, "Please Enter HTTP-URL To Download.", "Error", JOptionPane.ERROR_MESSAGE);
		}

		// set parameters for download
		CursorUtil.setWaitCursor(this);
		DownloadParameters dlg = null;
		try	{
			dlg = new DownloadParameters((JFrame)ComponentUtil.getFrame(this));
			dlg.show();
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
		
		if (dlg == null || dlg.isCanceled())
			return;

		String [] notMimeTypes = dlg.getNotMimeTypes();
		boolean followLinks = dlg.getFollowLinks();
		String target = dlg.getDirectory();

		// do download in background
		Download download = new Download(
				(JFrame)ComponentUtil.getFrame(this),
				new String [] { url },
				target,
				0L,
				notMimeTypes,
				followLinks);
			
		download.setTodoLimit(dlg.getTodoLimit());
		download.setDepth(dlg.getDepth());
		download.setOnlyWithinSite(dlg.getOnlyWithinSite());

		download.setBelowDocument(dlg.getBelowDocument());
		download.setConvertToRelative(dlg.getConvertURLs());
		download.startDownload();
	}

	
	/** Returns a string from the URL listing Component. Override when modififying that Component. */
	protected String getRenderedURL()	{
		return tf.getText();
	}

	/** Requests the current URL from the URL listing Component and loads it. */
	protected void refreshFromTextField(boolean addToHistory)	{
		String s = getRenderedURL();
		try	{
			URL url = new URL(s);
			gotoURL(url, addToHistory);
		}
		catch (MalformedURLException e) {
			System.err.println("FEHLER: "+e.getMessage());
			error("The URL "+s+" is malformed.");
		}
	}
	
	/** implements Closeable and saves the history to filesystem. */
	public boolean close()	{
		stopIt();
		tf.save();
		return true;
	}

	/** Returns the URL history that is ordered by usage */
	public Vector getHistory()	{
		return tf.getTypedHistory();
	}
	


	/**
		An JEditorPane whose deferred loading process can be interrupted.
		This is done by overriding getStream() and returning an interruptable
		InputStream.
	*/
	private class InterruptableJEditorPane extends JEditorPane
	{
		protected InputStream getStream(URL page) throws IOException {
			currentInputStream = super.getStream(page);
			
			currentInputStream = new FilterInputStream(currentInputStream)	{
				public int read(byte b[], int off, int len) throws IOException {
					if (stopped)
						throw new IOException("user stopped");
					return super.read(b, off, len);	
				}
			};
			return currentInputStream;
		}
	}



	// test main
	public static void main(String [] args)	{
		JFrame f = new JFrame("HTML Viewer");
		f.getContentPane().add(new HTMLViewer());
		f.setSize(200, 200);
		f.setVisible(true);
	}
	
}




/**
	Implementation of the URL listing Component.
*/
class HyperlinkCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist;
	private static File globalFile = null;


	public HyperlinkCombo()	{
		super();
		manageTypedHistory(this, new File(HistConfig.dir()+"HtmlHistory.list"));
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