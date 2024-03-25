package fri.util.xml;

import java.io.*;
import java.net.*;

/**
	Read a document from an URI. Checks if the URI is a file
	or a URL and returns appropriate streams.
*/
public abstract class DocumentBroker
{
	/** For now: returns a FileBroker. */
	public static DocumentBroker getInstance(String uri)	{
		try	{
			URL u = new URL(uri);
			if (u.getProtocol().equals("file"))	{
				uri = u.getPath();
			}
			else	{
				return new UrlBroker(u);
			}
		}
		catch (MalformedURLException e)	{
			System.err.println("Not a valid URL: "+uri);
		}

		return new FileBroker(uri);
	}

	public abstract InputStream getInputStream() throws Exception;
	public abstract OutputStream getOutputStream() throws Exception;
	public abstract boolean canSaveWithoutOverwriteCheck() throws Exception;

	private static class FileBroker extends DocumentBroker
	{
		private String path;

		FileBroker(String path)	{
			this.path = path;
		}

		public InputStream getInputStream()
			throws IOException
		{
			return new BufferedInputStream(new FileInputStream(path));
		}

		public OutputStream getOutputStream()
			throws IOException
		{
			return new BufferedOutputStream(new FileOutputStream(path));
		}

		public boolean canSaveWithoutOverwriteCheck()
			throws Exception
		{
			File f = new File(path);
			return f.exists() == false;
		}
	}

	private static class UrlBroker extends DocumentBroker
	{
		private URL url;
		
		protected UrlBroker(URL url)	{
			this.url = url;
		}

		public InputStream getInputStream()
			throws IOException
		{
			URLConnection connection = url.openConnection();
			return connection.getInputStream();
		}

		public OutputStream getOutputStream()
			throws IOException
		{
			throw new IOException("Can not store to URL: "+url);
		}

		public boolean canSaveWithoutOverwriteCheck()
			throws Exception
		{
			return false;
		}
	}

}
