package fri.util.html;

import java.io.*;
import java.net.*;
import fri.util.observer.CancelProgressObserver;

public class Util
{
	private static final int CACHE_SIZE = 60;	// WINDOWS hangs when allocating more than 80 URLConnections
	private static LruHashtable connections = new LruHashtable(CACHE_SIZE);

	/** Get buffered URLConnections. Call clearConnections() after using this. */
	public static synchronized Object openURLConnection(URL url)	{
		Object uc = connections.get(url);
		
        if (connections.size() > CACHE_SIZE)  {
            clearConnections();
            System.gc();	// make URLConnection to release its network resources
            System.gc();
        }
        
		if (uc == null)	{
			try	{
				uc = getURLConnection(url);
			}
			catch (IOException e)	{
				connections.put(url, e.getMessage());
				return null;
			}

			if (uc == null)	{
				connections.put(url, "unknown error");
			}
			else	{
				connections.put(url, uc);
			}
		}
		return uc;
	}
	
	public static void clearConnections()	{
		connections.clear();
	}

	public static boolean clearConnection(String urlStr)	{
		return connections.remove(urlStr) != null;
	}

		
	/** Taken from JEditorPane to load URLs that are redirected. */
	public static URLConnection getURLConnection(URL page)
		throws IOException
	{
		URLConnection conn = page.openConnection();		
		
		if (conn instanceof HttpURLConnection) {
			HttpURLConnection hconn = (HttpURLConnection) conn;
			hconn.setInstanceFollowRedirects(false);
			int response = hconn.getResponseCode();
			boolean redirect = (response >= 300 && response <= 399);
			
			if (redirect) {
				String loc = conn.getHeaderField("Location");
				if (loc != null)	{
					if (loc.startsWith("http", 0)) {
						page = new URL(loc);
					}
					else {
						page = new URL(page, loc);
					}
					return getURLConnection(page);
				}
			}
		}
		return conn;
	}


	/** Puts URL contents to a File on harddisk. */
	public static void urlContentsToFile(
		String file,
		URL url,
		CancelProgressObserver observer)
		throws IOException
	{
		URLConnection conn = getURLConnection(url);
		InputStream in = conn.getInputStream();
		
		String dir = new File(file).getParent();
		if (dir != null && dir.equals(file) == false)
			new File(dir).mkdirs();
			
		FileOutputStream fos = new FileOutputStream(file);
		copyStream(in, fos, observer);
	}

	/** Copies a InputStream to a OutputStream, watched by an observer. */
	public static void copyStream(
		InputStream in,
		OutputStream out,
		CancelProgressObserver observer)
		throws IOException
	{
		byte[] buf = new byte[4096];
		int len;
		while (observer.canceled() == false && (len = in.read(buf)) != -1)	{
			observer.progress((long)len);
			//try{Thread.sleep(1000);}catch(Exception e){}
			out.write(buf, 0, len);
		}
		out.flush();
		out.close();
	}
	

	/** Puts URL contents to a String in memory. */
	public static String urlContentsToString(URL url)	{
		try	{
			URLConnection conn = getURLConnection(url);
			InputStream in = conn.getInputStream();
			int len = conn.getContentLength();
			StringWriter sw = new StringWriter(len);
			copyStream(in, sw);
			return sw.toString();
		}
		catch (IOException e)	{
			return e.getMessage();
		}
	}
	
	/** Copies a InputStream to a Writer. */
	public static void copyStream(InputStream in, Writer out)
		throws IOException
	{
		byte[] buf1 = new byte[4096];
		char[] buf2 = new char[4096];
		int len, i;
		while ((len = in.read(buf1)) != -1)	{
			for (i = 0; i < len; ++i)
				buf2[i] = (char) buf1[i];
			out.write(buf2, 0, len);
		}
		out.flush();
		out.close();
	}


	/**
		Calls new URL(context, urlStr).
		Removes CGI parameters from end of urlStr.
		Adds a slash to end if it is a directory.
		@return urlStr without CGI parameters.
	*/
	public static URL plainUrl(URL context, String urlStr)
		throws MalformedURLException
	{
		URL url = new URL(context, urlStr);
		String fileStr = url.getFile();

		// Are there URL-parameters?
		int i = fileStr.indexOf('?');
		if (i != -1) {
			fileStr = fileStr.substring(0, i);
		} 

		url = new URL(url.getProtocol(), url.getHost(), url.getPort(), fileStr);

		if (!fileStr.endsWith("/"))	{	// check if it is a site-URL, if true, add a "/"
			String s = url.toExternalForm();
			int j = s.lastIndexOf('/');
			if (j > 0)	{
				String t = s.substring(0, j + 1);
				if (t.equals("http://") || urlStrIsDir(url.toExternalForm()))	{
					fileStr = fileStr + "/";
					url = new URL(url.getProtocol(), url.getHost(), url.getPort(), fileStr);
				}
			}
		}

		return url;
	}


	/**
		Removes CGI parameters from end of urlStr.
		@return urlStr without CGI parameters.
	*/
	public static URL plainUrl(String urlStr) throws MalformedURLException {
		return plainUrl(null, urlStr);
	}

	/**
		If urlStr ends with "/", return urlStr. Test if it is a directory.
		@return urlStr + "/" if it is a directory, else cuts filename from end.
	*/
	public static String baseUrlStr(String urlStr) {
		if (urlStr.endsWith("/")) {
			return urlStr;
		} 
		if (urlStrIsDir(urlStr)) {
			return urlStr + "/";
		}
		// Test if its a site-URL
		String s = urlStr.substring(0, urlStr.lastIndexOf('/') + 1);
		if (s.equals("http://"))
			return urlStr + "/";	// add a slash to a site-URL, as referenced files use this as root.
		return s;
	}



	/**
		@return true if URL is a directory.
		This is tested in worst case by connecting to the URL and
		checking getContentLength() for -1.
		Results are buffered to speed repeated requests.
	*/
	private static LruHashtable directoryTests = new LruHashtable(200);

	public static boolean urlStrIsDir(String urlStr) {
		if (urlStr.endsWith("/"))	// hope that this is significant
			return true;

		Object b = directoryTests.get(urlStr);	// look for cached URLs
		if (b != null)
			return ((Boolean) b).booleanValue();
		
		// test link quickly for wellknown extensions
		int lastSlash = urlStr.lastIndexOf('/');
		int lastPeriod = urlStr.lastIndexOf('.');
		if (lastPeriod != -1 && (lastSlash == -1 || lastPeriod > lastSlash)) {
			String s = urlStr.substring(lastPeriod).toLowerCase();
			if (s.equals(".html") ||
					s.equals(".htm") ||
					s.equals(".shtml") ||
					s.equals(".jhtml") ||
					s.equals(".pdf") ||
					s.equals(".doc") ||
					s.equals(".xls") ||
					s.equals(".txt") ||
					s.equals(".php") ||
					s.equals(".css") ||
					s.equals(".js") ||
					s.equals(".java") ||
					s.equals(".class") ||
					s.equals(".zip") ||
					s.equals(".jar") ||
					s.equals(".tar") ||
					s.equals(".tgz") ||
					s.equals(".gz") ||
					s.equals(".gzip") ||
					s.equals(".gif") ||
					s.equals(".jpg") ||
					s.equals(".jpeg") ||
					s.equals(".png"))
			{
				directoryTests.put(urlStr, new Boolean(false));
				return false;
			}
		} 

		if (urlStr.toLowerCase().startsWith("mailto:"))	{
			directoryTests.put(urlStr, new Boolean(false));
			return false;
		}

		boolean isDir = false;

		try {
			URL url = new URL(urlStr);
			System.err.print("<");
			Object o = openURLConnection(url);
			if (o instanceof URLConnection == false)	{
				isDir = false;
			}
			else	{
				URLConnection uc = (URLConnection)o;
				int bytes = uc.getContentLength();
				isDir = bytes < 0;	// directory has length -1
			}
			System.err.print(">");
		}
		catch (Exception e) {
			//e.printStackTrace();
			isDir = false;
		}
		
		//System.err.println("... is directory "+urlStr+": "+ret);
		directoryTests.put(urlStr, new Boolean(isDir));
		return isDir;
	}
	

	/** remove triple slashes from URL */
	public static String getStartURL(String urlStr) {
		String s = baseUrlStr(urlStr);
		if (s.equals("http://") == false)
			return s;
		s = urlStr.substring("http://".length());
		int i = s.indexOf("/");
		if (i > 0)
			return "http://"+s.substring(0, i);
		return urlStr;
	}

	/** @return urlStr minus base */
	public static String getRelativePath(String urlStr, String base) {
		if (urlStr.startsWith(base))
			urlStr = urlStr.substring(base.length());
		int i;
		if (base.equals("http://") &&
				(i = urlStr.indexOf("/")) >= 0 &&
				i < urlStr.length())
		{
			urlStr = urlStr.substring(i + 1);
		}
		return urlStr;
	}

	/** @return filename of URL */
	public static String getLastName(String urlStr) {
		int i = urlStr.indexOf('?');

		if (i != -1) {
			urlStr = urlStr.substring(0, i);
		}
		
		i = urlStr.lastIndexOf('/');
		if (i != -1 && urlStr.length() > i + 1)	{
			urlStr = urlStr.substring(i + 1);
		}
		return urlStr;
	}

	/**
		@return the URL without leading protocol and slashes:
		"http://ahost/afile.html" -> "ahost/afile.html".
	*/
	public static String getURLWithoutProtocol(String urlStr) {
		try	{
			URL url = new URL(urlStr);
			urlStr = url.getHost()+url.getFile();
		}
		catch (MalformedURLException e)	{
			e.printStackTrace();
		}
		return urlStr;
	}


	/**
		@return true if both URLs have the same host.
	*/
	public static boolean isWithinSameSite(String urlStr, String base)	{
		try	{
			URL url1 = new URL(urlStr);
			URL url2 = new URL(base);
			String host1 = url1.getHost();
			String host2 = url2.getHost();
			//System.err.println("host 1 "+host1+" host 2 "+host2);
			if (host1.equals(host2))
				return true;
			//return host2.length() <= 0 || host1.equals(host2);
		}
		catch (MalformedURLException e)	{
			e.printStackTrace();
		}
		//System.err.println("Not within same site: "+urlStr+" base is: "+base);
		return false;
	}


	/** Trim URL path upwards by adding "../../" @param base must end with "/"! */
	public static String makeRelativeURLAboveURL(URL url, String base)	{
		//System.err.println("makeRelativeURLAboveURL url="+url+", base="+base);
		try	{
			URL baseUrl = new URL(base);
			String s = baseUrl.getFile();
			String baseFile = s.length() > 0 ? baseUrl.getFile().substring(1) : "";	// cut leading "/"				

			s = url.getFile();
			String urlFile = s.length() > 0 ? s.substring(1) : "";	// cut leading "/"
			//System.err.println("  urlfile "+urlFile);
			//System.err.println("  basefile "+baseFile);

			if (urlStrIsDir(baseUrl.toExternalForm()) == false)	{
				int last = baseFile.lastIndexOf("/");
				if (last > 0)	{
					baseFile = baseFile.substring(0, last);
				}
				else	{
					baseFile = "";
				}
			}
			
			String upwards = "";
			while (baseFile.length() > 0 && urlFile.startsWith(baseFile) == false)	{
				int last = baseFile.lastIndexOf("/", baseFile.length() - 2);
				int len = baseFile.length();	// check length of old url
				if (last > 0)	{
					baseFile = baseFile.substring(0, last + 1);
					if (len - baseFile.length() == 1)	// was double slash in url
						continue;
				}
				else	{
					baseFile = "";
				}
					
				upwards = "../"+upwards;
				//System.err.println("    basefile >"+baseFile+"<");
			}

			if (url.getHost().equals(baseUrl.getHost()) || url.getHost().length() <= 0)	{
				String result = upwards + urlFile.substring(baseFile.length());
				return result;
			}
			
			String result = upwards + "../" + url.getHost() + "/" + urlFile.substring(baseFile.length());
			return result;
		}
		catch (MalformedURLException e)	{
			e.printStackTrace();
		}
		return null;
	}
	
	

	/*
	public static void main(String [] args)	{
		try	{
//			URL url = new URL("http://www.ich.und.du/aa/bbb/c.html");
//			System.err.println("host "+url.getHost()+", port "+url.getPort()+", file "+url.getFile());
			
//			System.err.println(getURLWithoutProtocol(url.toExternalForm()));
//			
//			System.err.println(isWithinSameSite(
//					url.toExternalForm(),
//					"http://www.wo.anders/aa/bbb/c.html"));
//			System.err.println(isWithinSameSite(
//					"file:///directory/a/b/c",
//					"file:///dir/a/b/c"));
//					
//			System.err.println(makeRelativeURLAboveURL(
//					url,
//					"http://www.ich.und.du/aa/bbb/ffff/"));
//
//			System.err.println(makeRelativeURLAboveURL(
//					new URL("file:///directory/aa/bbb/c.html"),
//					"file:///dir/aa/bbb/ffff/"));

			//URL url = new URL("http://www.webreview.com/webtools/index.html");
			URL url = new URL("http://www.webreview.com/index.html");
			URL url1 = Util.plainUrl(url, "/community");

			System.err.println("urlstring is: "+url1.toExternalForm()+" -> "+
				makeRelativeURLAboveURL(
					url1,	//new URL(),
					url.toExternalForm()));
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}
	*/
}