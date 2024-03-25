package fri.util.html;

import java.util.*;
import java.net.*;
import java.io.*;
import fri.util.observer.CancelProgressObserver;
import fri.util.file.ValidFilename;

/**
	Do recursive enumeration and download of HTML pages (main routine).
	You can allocate a Spider object and call
	<code>spider.addUrl("http://www.all.world")</code>.
	Enumerate the Spider and write URLs to files by calling 
	<code>spiderItem.toFile(directory)</code> or
	<code>spiderItem.scan()</code> just to parse the HTML document.
	The full path of base URL will be created in filesystem under the download
	target directory.
	<p>
	Following parameters can be set:
	<ul>
		<li>maximum downloaded files ("todoLimit")</li>
		<li>maximum depth (level) of document links followed</li>
		<li>maximum "done" cache size (this is for avoiding rescanning URLs)</li>
		<li>if URLs should be converted to relative URLs</li>
		<li>if hyperlinks should be followed recursively or just the passed page
			should be parsed/downloaded</li>
		<li>... and some others, see method documentation.
	</ul>
	Look at the main routine or call without arguments to see call syntax.
*/

public class Spider implements
	HtmlEditObserver,
	Enumeration
{
	public static boolean DEBUG = false;
	
	private static final String version = "1.0";

	protected PrintStream err;

	protected Queue todo = new Queue();
	protected int todoLimit = 0;

	protected Hashtable done;

	private Item item;
	private URL thisUrl;
	private String currContentType = null;

	private boolean gotOne = false;

	private boolean depthFirst = false;

	private String [] notMimeTypes = null;
	private String [] notExtensions = null;
	private boolean followLinks = true;
	private int depth = -1;
	private boolean convertToRelative = true;
	private boolean onlyWithinSite = true;
	private boolean belowDocument = true;
	
	

	/**
		Allocate a Spider with System.err as PrintStream for control output.
	*/
	public Spider()	{
		this(System.err);
	}

	/**
		Allocate a Spider with System.err as PrintStream for control output
		and a maximum of files to download.
	*/
	public Spider(int todoLimit, int doneLimit) {
		this(todoLimit, doneLimit, System.err);
	}

	/**
		Allocate a Spider with a PrintStream for control output.
	*/
	public Spider(PrintStream err)	{
		this(0, 10000, err);
	}

	/**
		Allocate a Spider with a PrintStream for control output
		and a maximum of files to download.
	*/
	public Spider(int todoLimit, int doneLimit, PrintStream err)	{
		this.err = err;
		this.todoLimit = todoLimit;
		if (doneLimit <= 0)
			done = new Hashtable();
		else
			done = new LruHashtable(10000);
	}

	/** Free all resources, especially clear URLConnection cache. */
	public void release()	{
		Util.clearConnections();
	}
	
	/** Overridden to call release(). */
	protected void finalize() throws Exception	{
		release();
	}

	/**
		Set search order. Default is breadth first.
	*/
	public void setDepthFirst(boolean depthFirst)	{
		this.depthFirst = depthFirst;
	}

	/**
		Set maximum downloaded files. Default is no limit.
	*/
	public void setTodoLimit(int todoLimit)	{
		this.todoLimit = todoLimit;
	}
	
	/**
		Convert all references to relative URLs when true is passed.
		Default is true.
	*/
	public void setConvertToRelative(boolean convertToRelative)	{
		this.convertToRelative = convertToRelative;
	}
	
	/**
		Follow HTML Links to other HTML documents and do download them.
		If false is passed, only Images, Scripts and others are downloaded.
		Default is true.
	*/
	public void setFollowLinks(boolean followLinks)	{
		this.followLinks = followLinks;
	}
	
	/**
		Set a array of Strings containing MIME types that should not be downloaded.
		Default all MIME types are included.
	*/
	public void setNotMimeTypes(String [] notMimeTypes)	{
		if (notMimeTypes != null)	{
			this.notMimeTypes = new String[notMimeTypes.length];
			for (int i = 0; i < notMimeTypes.length; i++)
				this.notMimeTypes[i] = notMimeTypes[i].toLowerCase();
		}
		else	{
			this.notMimeTypes = null;
		}
	}
	
	/**
		Set a array of Strings containing MIME types that should not be downloaded.
		Default all MIME types are included.
	*/
	public void setNotExtensions(String [] notExtensions)	{
		this.notExtensions = notExtensions;
	}
	
	/**
		Set maximum hyperlink depth of documents to follow.
		Default depth is undefined (no limit).
	*/
	public void setDepth(int depth)	{
		this.depth = depth;
	}
	
	/**
		If true is passed, only hyperlinks pointing to documents
		within the "site" of the root document are followed. "Site" means
		the first URL part, e.g. "http://www.example.com" from
		"http://www.example.com/doc/index.html"
		Default this is true.
	*/
	public void setOnlyWithinSite(boolean onlyWithinSite)	{
		this.onlyWithinSite = onlyWithinSite;
	}
	
	/**
		If true is passed, only hyperlinks pointing to documents
		"below" the root document are followed. "Below" means e.g. that
		the URL must start with "http://www.example.com/doc/" for
		a root document URL "http://www.example.com/doc/index.html".
		Default this is true.
	*/
	public void setBelowDocument(boolean belowDocument)	{
		this.belowDocument = belowDocument;
	}
	

	
	/**
		Add an URL to Spider. Without this call Spider makes no sense.
		This is NOT synchronized!
	*/
	public void addUrl(String urlStr) throws MalformedURLException {
		URL url = Util.plainUrl(urlStr);
		String thisUrlStr = url.toExternalForm();
		String baseUrlStr = Util.baseUrlStr(thisUrlStr);
		if (DEBUG) err.println("base URL is "+baseUrlStr);

		todo.addBack(new Item(thisUrlStr, null, 0, baseUrlStr));
	}


	// get a URL from todo list. This is NOT synchronized!
	private void getOne() {
		while (!todo.isEmpty()) {
			item = (Item) todo.getFront();

			if (!done.containsKey(item.thisUrlStr)) {
				done.put(item.thisUrlStr, item.thisUrlStr);

				try {
					thisUrl = new URL(item.thisUrlStr);
					gotOne = true;
					return;
				}
				catch (MalformedURLException e) {
					String msg = e.getMessage();
					if (checkMalformedURL(msg)) {
						brokenLink(myUrlToString(item.fromUrl), item.thisUrlStr, msg);
					}
				}
				catch (Exception e) {
					reportError(myUrlToString(item.fromUrl), item.thisUrlStr, e.toString());
				}
			}
		}
		gotOne = false;
		return;
	}



	/**
		Implements Enumeration. This is NOT synchronized!
		@return true if there are more URLs under the ones added with addUrl().
	*/
	public boolean hasMoreElements() {
		if (!gotOne) {
			getOne();
		}
		return gotOne;
	}


	/**
		Implements Enumeration. This is NOT synchronized!
		@return a Spider.Item with a InputStream and all necessary connection data.
			Return can be null if URL is not valid.
	*/
	public Object nextElement() {
		if (!gotOne) {
			getOne();
		}
		if (!gotOne) {
			return null;
		}

		gotOne = false;

		Item localItem = item;
		URL localThisUrl = thisUrl;

		try {
            Object o = Util.openURLConnection(localThisUrl);
            if (o instanceof URLConnection == false)    {
                reportError(myUrlToString(localItem.fromUrl), localThisUrl.toString(), ""+o);
                return null;
            }
			URLConnection uc = (URLConnection) o;
			
			//URLConnection uc = localThisUrl.openConnection();
			localItem.urlConnection = uc;
			uc.connect();

			InputStream s = uc.getInputStream();
			localItem.stream = s;
			String contentType = uc.getContentType();
			localItem.contentType = contentType;

			if (DEBUG) err.println("content type retrieved in nextElement is: "+contentType);
			
			if (contentType != null && contentType.startsWith("text/html")) {
				HtmlEditScanner scanner = new HtmlEditScanner(s, localThisUrl, this, localItem);
				localItem.stream = scanner;
			}
			
			return localItem;
		}
		catch (FileNotFoundException e) {
			String msg = e.getMessage();
			if (checkMalformedURL(msg))
				brokenLink(myUrlToString(localItem.fromUrl), localItem.thisUrlStr, msg);
		}
		catch (UnknownHostException e) {
			String msg = e.getMessage();
			if (checkMalformedURL(msg))
				brokenLink(myUrlToString(localItem.fromUrl), localItem.thisUrlStr, "unknown host -- " + msg);
		}
		catch (Exception e) {
			reportError(myUrlToString(localItem.fromUrl), localItem.thisUrlStr, e.toString());
		}

		return null;
	}


	private String myUrlToString(URL url) {
		if (url == null) {
			return "null";
		}
		else {
			return url.toExternalForm();
		}
	}

	/** implements HtmlEditObserver, sets title to Spider.Item */
	public String editTITLE(String urlStr, URL contextUrl, Object clientData) {
		((Item) clientData).setTitle(urlStr);
		return null;
	}
	/** implements HtmlEditObserver and calls add */
	public String editAHREF(String urlStr, URL contextUrl, Object clientData) {
		if (DEBUG) err.println("editAHREF got URL "+urlStr+" with context "+contextUrl);
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editIMGSRC(String urlStr, URL contextUrl, Object clientData) {
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editFRAMESRC(String urlStr, URL contextUrl, Object clientData) {
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editBASEHREF(String urlStr, URL contextUrl, Object clientData) {
		return "";	// delete www-bindings
	}
	/** implements HtmlEditObserver and calls add */
	public String editAREAHREF(String urlStr, URL contextUrl, Object clientData) {
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editLINKHREF(String urlStr, URL contextUrl, Object clientData) {
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editBODYBACKGROUND(String urlStr, URL contextUrl, Object clientData) {
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editSCRIPTSRC( String urlStr, URL contextUrl, Object clientData)	{
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editTABLEBACKGROUND( String urlStr, URL contextUrl, Object clientData )	{
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editAPPLETCODE( String urlStr, URL contextUrl, Object clientData )	{
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editAPPLETCODEBASE( String urlStr, URL contextUrl, Object clientData )	{
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editAPPLETARCHIVE( String urlStr, URL contextUrl, Object clientData )	{
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editOBJECTDATA( String urlStr, URL contextUrl, Object clientData )	{
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editLAYERSRC( String urlStr, URL contextUrl, Object clientData )	{
		return add(urlStr, contextUrl, (Item) clientData);
	}
	/** implements HtmlEditObserver and calls add */
	public String editLAYERBACKGROUND( String urlStr, URL contextUrl, Object clientData )	{
		return add(urlStr, contextUrl, (Item) clientData);
	}


	/** Decide if a found URL should be added to todo-list. */
	protected boolean doThisUrl(
		URL url,
		String thisUrlStr,
		URL context,
		int depth,
		String baseUrlStr)
	{
		if (DEBUG) err.println("Spider.doThisUrl: "+thisUrlStr);
		if (this.depth > 0 && depth > this.depth)	{
			if (DEBUG) err.println("gone too deep into document hierarchy: "+thisUrlStr+" depth "+this.depth);
			return false;
		}
			
		if (belowDocument && thisUrlStr.startsWith(baseUrlStr) == false)	{
			if (DEBUG) err.println("not below originator document: "+thisUrlStr+" baseUrlStr "+baseUrlStr);
			return false;
		}
			
		if (onlyWithinSite && Util.isWithinSameSite(thisUrlStr, baseUrlStr) == false)	{
			if (DEBUG) err.println("not within same site: "+thisUrlStr+" baseUrlStr "+baseUrlStr);
			return false;
		}

		boolean extensionAllowed = isExtensionAllowed(thisUrlStr);

		ensureContentType(url);
		if (DEBUG) err.println("current content type: "+currContentType);

		if (currContentType != null)	{
			boolean isHTML = currContentType.startsWith("text/html");

			if (isHTML && followLinks == false)	{
				if (DEBUG) err.println("do not follow links: "+thisUrlStr+" baseUrlStr "+baseUrlStr);
				return false;
			}

			if (false == isHTML && extensionAllowed == false)	{
				if (DEBUG) err.println("do not download extension: "+thisUrlStr+" baseUrlStr "+baseUrlStr);
				return false;
			}

			for (int i = 0; false == isHTML && notMimeTypes != null && i < notMimeTypes.length; i++)	{
				if (currContentType.startsWith(notMimeTypes[i]))	{
					if (DEBUG) err.println("excluded MIME-type: "+thisUrlStr+" MIME-type "+currContentType);
					return false;
				}
			}
		}
		else
		if (extensionAllowed == false)	{
			if (DEBUG) err.println("exclude extension: "+thisUrlStr+" baseUrlStr "+baseUrlStr);
			return false;
		}

		if (DEBUG) err.println("doThisUrl "+url+" original "+thisUrlStr+" at depth "+depth+" in "+context+", base "+baseUrlStr);

		return true;
	}


	private boolean isExtensionAllowed(String urlName)	{
		int dot = urlName.lastIndexOf(".");
		String ext = dot >= 0 ? urlName.substring(dot + 1) : null;
		for (int i = 0; ext != null && notExtensions != null && i < notExtensions.length; i++)	{
			if (notExtensions[i].equals(ext))	{
				if (DEBUG) err.println("excluded extension: "+ext+" extensions "+arrayToString(notExtensions));
				return false;
			}
		}
		return true;
	}


	private void ensureContentType(URL url)	{
		if (currContentType == null)	{
			Object o = Util.openURLConnection(url);
			if (o instanceof URLConnection)	{
				URLConnection uc = (URLConnection)o;
				currContentType = uc.getContentType();
				if (currContentType != null)
					currContentType = currContentType.toLowerCase();
			}
		}
	}
	

	/**
		Add a found URL to todo-list while scanning HTML document.
		@return substitution String when converting Links to relative URLs.
	*/
	protected String add(String urlStr, URL contextUrl, Item item) {
		if (DEBUG) err.println("scanned URL is "+urlStr);
		String origUrlStr = urlStr;
		try {
			URL url = Util.plainUrl(contextUrl, urlStr);
			urlStr = url.toExternalForm();
			
			currContentType = null;	// clear for doThisUrl()
			boolean added = false;

			if (doThisUrl(url, urlStr, contextUrl, item.itemDepth + 1, item.baseUrlStr) &&
					(todoLimit <= 0 || todo.size() < todoLimit))
			{
				if (false == done.containsKey(urlStr))	{
					if (depthFirst == false)	// is default
						todo.addBack(new Item(urlStr, contextUrl, item.itemDepth + 1, item.baseUrlStr));
					else
						todo.insertElementAt(new Item(urlStr, contextUrl, item.itemDepth + 1, item.baseUrlStr), 0);
					if (DEBUG) err.println("added URL: "+urlStr);
				}
				added = true;
			}

			// cut to relative URL
			if (added && convertToRelative)	{
				// is link HTML?
				ensureContentType(url);
				
				if (currContentType != null)	{
					if (currContentType.startsWith("text/html") && followLinks == false)
						return null;
				}
				
				if (DEBUG) err.println("MIME type is "+currContentType+" for "+urlStr);
				
				if (origUrlStr.startsWith(item.baseUrlStr))	// cut absolute URL below document
					return origUrlStr.substring(item.baseUrlStr.length());

				// href="page.html#SomeWhere", #-reference has been lost in Util.plainUrl()
				int i = origUrlStr.indexOf("#");
				String ref = null;
				if (i >= 0)
					ref = origUrlStr.substring(i);

				String s = Util.makeRelativeURLAboveURL(url, item.baseUrlStr);

				if (ref != null)
					s = s+ref;

				return s;				
			}
		}
		catch (MalformedURLException e) {
			err.println("MalFormed URL = "+urlStr);
			String msg = e.getMessage();
			if (checkMalformedURL(msg)) {
				brokenLink(myUrlToString(contextUrl), urlStr, msg);
			}
		}
		return null;	// leave URL unchanged
	}



	private static boolean checkMalformedURL(String msg) {
		if (msg.startsWith("unknown protocol: ")) {
			String protocol = msg.substring(msg.lastIndexOf(' ') + 1);
			return !otherProtocol(protocol);
		}
		return true;
	}

	private static boolean otherProtocol(String protocol) {
		if (protocol.equalsIgnoreCase("gopher")
				|| protocol.equalsIgnoreCase("ftp")
				|| protocol.equalsIgnoreCase("file")
				|| protocol.equalsIgnoreCase("telnet")
				|| protocol.equalsIgnoreCase("news")
				|| protocol.equalsIgnoreCase("mailto")
				|| protocol.equalsIgnoreCase("javascript"))
			return true;
		return false;
	}

	protected void brokenLink(String fromUrlStr, String toUrlStr, String errmsg) {
		//Thread.dumpStack();
		PrintStream out = err;
		if (out == null)
			out = System.err;
		out.println("Broken hyperlink in " + fromUrlStr);
		out.println("    pointing to " + toUrlStr);
		out.println("    " + errmsg);
	}

	protected void reportError(String fromUrlStr, String toUrlStr, String errmsg) {
		PrintStream out = err;
		if (out == null)
			out = System.err;
		out.println("Error in " + fromUrlStr);
		out.println("    pointing to " + toUrlStr);
		out.println("    " + errmsg);
	}


	private static String arrayToString(String [] array)	{
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; array != null && i < array.length; i++)	{
			if (i > 0)
				sb.append(", ");
			sb.append(array[i]);
		}
		sb.append("]");
		return sb.toString();
	}


	/**
	<pre>
			SYNTAX:
					fri.util.html.Spider
						[-f]
						[-c]
						[-w]
						[-m<digits>] [-d<digits>] 
						[-nDenyMimeType1,DenyMimeType2,DenyMimeType3,...]
						targetDir 
						urlString1 [urlString2 ...]
						
			-f: do not follow hyperlinks recursively
			-c: do not convert links in documents to relative URL's
			-m100: set maximum downloaded files e.g. to 100
			-d4: set maximum depth of followed links e.g. to 4
			-ncontent/unknown,application/zip: do not download links of MIME-types in list
			-w: even hyperlinks not within site are followed
	</pre>
	*/
	public static void main(String[] args) {
		System.err.println("HTML Spider Version "+version);
		
		//String url = "file://localhost/Projekte/Homepage/index.noframes.html";
		String downloadDir = null;
		
		if (args.length < 2) {
			System.err.println("SYNTAX: "+
					"fri.util.html.Spider "+
					"[-s] "+
					"[-f] "+
					"[-c] "+
					"[-w] "+
					"[-b] "+
					"[-n<digits>] "+
					"[-d<digits>] "+
					"[-m<DenyMimeType1,DenyMimeType2,DenyMimeType3,...>] "+
					"[-e<DenyExtension1,DenyExtension2,DenyExtension3,...>] "+
					"targetDir "+
					"urlString1 [urlString2 ...]");
			System.err.println("\t-s: just show URL's, do not download");
			System.err.println("\t-f: do NOT follow hyperlinks recursively");
			System.err.println("\t-c: do NOT convert links in documents to relative URL's");
			System.err.println("\t-n100: set maximum downloaded files e.g. to 100");
			System.err.println("\t-d4: set maximum depth of followed links e.g. to 4");
			System.err.println("\t-m\"content/unknown,application/octet-stream\": do not download links of MIME-types in list");
			System.err.println("\t-e\"exe,com,...\": do not download files with extensions in list");
			System.err.println("\t-w: even hyperlinks not within site are followed");
			System.err.println("\t-b: only hyperlinks below originator document\n");
			System.err.println("Do not forget to set proxy properties by command line");
			System.err.println("\tjava -Dhttp.proxyHost=myHost -Dhttp.proxyPort=3128 fri.util.html.Spider ...");
			System.exit(1);
		}

		int ret = 0;	// assume positive exit code
		
		boolean onlyWithinSite = true;	// follow links only within site
		boolean followLinks = true;	// follow hyperlinks
		boolean doConvertToRelative = true;
		boolean belowDocument = false;
		boolean justShow = false;
		String [] notMimeTypes = null;	// denied MIME types
		String [] notExtensions = null;	// denied extensions
		int todo = -1;	// no limit
		int depth = -1;	// unlimited depth
		
		for (int i = 0; i < args.length; i++)	{
			// interpret arguments
			if (args[i].startsWith("-"))	{	// is option
				if (args[i].equals("-f"))	{
					followLinks = false;
				}
				else
				if (args[i].equals("-c"))	{
					doConvertToRelative = false;
				}
				else
				if (args[i].equals("-w"))	{
					onlyWithinSite = false;
					belowDocument = false;
				}
				else
				if (args[i].equals("-b"))	{
					belowDocument = true;
				}
				else
				if (args[i].equals("-s"))	{
					justShow = true;
				}
				else
				if (args[i].startsWith("-n"))	{
					try	{
						todo = Integer.valueOf(args[i].substring(2)).intValue();
					}
					catch (Exception e)	{
						e.printStackTrace();
					}
				}
				else
				if (args[i].startsWith("-d"))	{
					try	{
						depth = Integer.valueOf(args[i].substring(2)).intValue();
					}
					catch (Exception e)	{
						e.printStackTrace();
					}
				}
				else
				if (args[i].startsWith("-m") || args[i].startsWith("-e"))	{
					boolean isMime = args[i].startsWith("-m");
					try	{
						StringTokenizer stok = new StringTokenizer(args[i].substring(2), ",");
						Vector v = new Vector();
						
						while (stok.hasMoreTokens())	{
							v.add(stok.nextToken());
						}
						if (isMime)	{
							notMimeTypes = new String [v.size()];
							v.copyInto(notMimeTypes);
						}
						else	{
							notExtensions = new String [v.size()];
							v.copyInto(notExtensions);
						}
					}
					catch (Exception e)	{
						e.printStackTrace();
					}
				}
				else
					System.err.println("ERROR: unknown option "+args[i]);
			}
			else
			if (downloadDir == null)	{	// first non-option must be download directory
				downloadDir = args[i];
				File newDir = new File(downloadDir);
				
				// check if directory exists
				if (justShow == false && newDir.exists() == false)	{
					System.err.println("WARNING: target directory not found: "+downloadDir+", creating it ...");
					newDir.mkdirs();
					if (newDir.exists() == false || newDir.isDirectory() == false)	{
						System.err.println("ERROR: could not create target directory: "+downloadDir);
						System.exit(2);
					}
				}
			}
			else	{	// argument is URL to scan
				Spider spider = new Spider(System.err);
				spider.setFollowLinks(followLinks);
				spider.setConvertToRelative(doConvertToRelative);
				spider.setNotMimeTypes(notMimeTypes);
				spider.setNotExtensions(notExtensions);
				spider.setDepth(depth);
				spider.setTodoLimit(todo);	// done in constructor
				spider.setOnlyWithinSite(onlyWithinSite);
				spider.setBelowDocument(belowDocument);
				System.err.println("==================================================");
				System.err.println("\tfollowLinks "+followLinks);
				System.err.println("\tconvertURLs "+doConvertToRelative);
				System.err.println("\tonlyWithinSite "+onlyWithinSite);
				System.err.println("\tbelowDocument "+belowDocument);
				System.err.println("\tnotMimeTypes "+arrayToString(notMimeTypes));
				System.err.println("\tnotExtensions "+arrayToString(notExtensions));
				System.err.println("\tmaximum "+todo);
				System.err.println("\tdepth "+depth);
				System.err.println("\tjustShow "+justShow);
				System.err.println("==================================================");
				System.err.println("\tdownloadDir "+downloadDir);
				System.err.println("\tURL "+args[i]);
				System.err.println("==================================================");
				
				try {
					String url = args[i];
					if (url.startsWith("www."))	{
						url = "http://"+url;
					}
					spider.addUrl(url);
				}
				catch (MalformedURLException e) {
					System.err.println(e);
					ret = 3;
					continue;
				}
		
				while (spider.hasMoreElements()) {
					Spider.Item item = (Spider.Item)spider.nextElement();
	
					if (item != null && item.urlConnection != null)	{	// not broken link
						//System.err.println(item);
						
						if (justShow)	{
							try {
								item.scan();	// parse the url
							}
							catch (IOException e) {
								e.printStackTrace();
							}
						}
						else	{	// scan and store to file		
							if (item.toFile(downloadDir) == false)
								ret = 4;
						}
					}
				}
				
				spider.release();
			}
		}
		
		System.exit(ret);
	}



	/**
		Contains all data of a HTML page or a Html-Reference necessary
		for downloading it by a valid InputStream in member variable "stream".
		This item can represent a HTML-pages, a image, a zip, a javascript, etc.
	*/
	public class Item
	{
		public String thisUrlStr;	// full URL string
		public URL fromUrl;
		public int itemDepth;
		public String baseUrlStr;
		public URLConnection urlConnection = null;
		public InputStream stream;
		public String contentType;
		public String title;
	
	
		Item(String thisUrlStr, URL fromUrl, int depth, String baseUrlStr) {
			this.thisUrlStr = thisUrlStr;
			this.fromUrl = fromUrl;
			this.itemDepth = depth;
			this.baseUrlStr = baseUrlStr;
		}


		/**
			Starting the HtmlEditScanner, closing the stream.
			Set the urlConnection to null after using, so it can be garbage collected.
		*/
		public void scan()
			throws IOException
		{
			stream.close();
		}		
		
		public String toString()	{
			return "URL="+thisUrlStr+", MIME="+contentType+", PARENT="+fromUrl+", BASE="+baseUrlStr;
		}


		/**
			Sets the title of this document.
		*/
		public void setTitle(String title)	{
			this.title = title;
		}
		
		/**
			Creates all necessary subdirs and stores HTML page and all references.
			Set the urlConnection to null after using, so it can be garbage collected.
			@param dirname name in filesystem where to store page.
		*/
		public boolean toFile(String dirname)	{
			return toFile(dirname, null);
		}
		
		/**
			Creates all necessary subdirs and stores HTML page and all references.
			Set the urlConnection to null after using, so it can be garbage collected.
			@param dirname name in filesystem where to store page.
			@param observer observer object that needs progress and can cancel download.
		*/
		public boolean toFile(String dirname, CancelProgressObserver observer)	{
			//String relativePath = Util.getRelativePath(thisUrlStr, baseUrlStr);
			String relativePath = Util.getURLWithoutProtocol(thisUrlStr);
			
			relativePath = ValidFilename.correctPath(relativePath);

			String filename = dirname.replace(File.separatorChar, '/');
			
			if (filename.endsWith("/") == false && relativePath.startsWith("/") == false)
				filename = filename + "/";
			
			filename = filename + relativePath;

			File thisFile = new File(filename);

			// If coming with a site, we have a file, but the directory may be already created.
			if (thisFile.isDirectory() == true)	{
				File newFile = new File(thisFile, thisFile.getName()+".html");
				if (DEBUG) err.println("Storing downloaded page "+thisFile+" to "+newFile);
				thisFile = newFile;
			}

			// Check if parent is a directory. If not, rename conflicting file
			// to file.html, create directory and move renamed file into it.
			// The renamed file could be a directory listing or a directory URL page!
			File parent = new File(thisFile.getParent());
			File pnt = parent;

			if (pnt != null && pnt.exists())	{
				if (pnt.isDirectory() == false)	{
					if (isExtensionAllowed(".html") == false)	{
						if (DEBUG) err.println("Deleting file "+pnt);
						pnt.delete();
						pnt.mkdirs();
					}
					else	{
						if (DEBUG) err.println("Renaming and moving "+pnt);
						File newFile = new File(pnt.getPath()+".html");
						if (newFile.exists())	// delete old file
							newFile.delete();

						boolean ok = pnt.renameTo(newFile);	// leave on same directory level
                        Thread.yield();
                        if (ok) // extra check this, rename is obscure on different platforms
                            ok = newFile.exists();

						if (false == ok)	{
							Thread.dumpStack();
							err.println("ERROR: Renaming "+pnt+" to "+newFile);
							return false;
						}
						else	{
							File tmpFile = newFile;
							pnt.mkdirs();
							
							// we MUST move the new HTML file to the created directory, else links will be invalid!
							newFile = new File(pnt, newFile.getName());
							ok = tmpFile.renameTo(newFile);	// put one level lower to new directory
							if (false == ok)
								err.println("ERROR: Moving "+tmpFile+" to "+newFile);
						}
					}
				}
				// parent exists and is (now) a directory
			}
			else
			if (pnt != null)	{	// not existing directory
				pnt.mkdirs();
			}

			if (isExtensionAllowed(thisFile.getName()) == false)
				return true;

			err.println(thisUrlStr+"\t->\t"+thisFile);
	
			FileOutputStream out = null;
			try	{
				out = new FileOutputStream(thisFile);
				byte[] buf = new byte[4096];
				int len;
		
				while ((observer == null || observer.canceled() == false) &&
						(len = stream.read(buf)) != -1)
				{
					//try{Thread.sleep(2000);}catch(Exception e){};
					out.write(buf, 0, len);
					if (observer != null)
						observer.progress(len);

					//try{Thread.sleep(1000);}catch(Exception e){}
				}
				stream.close();
				out.flush(); out.close();
			}
			catch (IOException e)	{
				e.printStackTrace();
				try	{ stream.close(); out.close(); new File(filename).delete(); } catch (Exception e1) {}
				//System.exit(3);
				return false;
			}
			return true;
		}
	
	}

}