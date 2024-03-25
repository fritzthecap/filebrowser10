package fri.util;

import java.net.*;
import java.util.Enumeration;
import java.io.*;
import fri.util.os.OS;

/**
 * Methods to retrieve local host information.
 * Contains a utility to retrieve the MAC adress of network card, using ifconfig or ipconfig.
 * 
 * @author Ritzberger Fritz
 */
public abstract class NetUtil
{
	private static final int hostNameTimeoutMillis = 1000;
	private static InetAddress localHost;
	private static String hostName;
	private static String macAddress;
	private static Throwable error;

	
	// local host methods

	/** Returns the IP address object of the local host, or null if network is down. Error will be non-null then. */
	public static InetAddress getLocalHost()	{
		init();
		return localHost;
	}
	
	/** Returns the name of the local host. If network is down, this returns "localhost". Error will be non-null then. */
	public static String getLocalHostName()	{
		init();
		if (hostName != null)	{
			// get the name from Networkinterfaces when the name is an internet address
			String hn = localHost != null ? localHost.getHostName() : null;
			return hn == null || hn.equals(hostName)
				? hostName
				: isGoodHostName(hn)
					? hn	// trust the name from NetworkInterfaces more
					: hostName;
		}
		return localHost != null ? localHost.getHostName() : "localhost";
	}
	
	private static boolean isGoodHostName(String hostName)	{
		if (hostName == null)
			return false;
		// check the hostname if it is an internet address
		for (int i = 0; i < hostName.length(); i++)
			if (Character.isDigit(hostName.charAt(i)) == false && hostName.charAt(i) != '.')
				return true;
		return false;
	}
	
	/** Returns the dotted IP address of the local host. If network is down, this returns "127.0.0.1". Error will be non-null then. */
	public static String getLocalHostAddress()	{
		init();
		return localHost != null ? localHost.getHostAddress() : "127.0.0.1";
	}
	
	public static Throwable getLocalHostError()	{
		init();
		return error;
	}
	
	/** Returns the passed host's address, or null if network is down. */
	public static InetAddress getByName(String hostName)
		throws UnknownHostException
	{
		init();
		if (error != null)
			return null;	// would hang
		return InetAddress.getByName(hostName);
	}
	
	private static void init()	{
		if (localHost != null || error != null)	// already done
			return;
		
		// try to get local host information:
		// start a thread that retrieves the host information
		// (this call could hang quit long when network is down),
		// and waits for thread termination at most hostNameTimeoutMillis,
		// so this returns either null (timeout expired) or the valid host
		// address, in both cases this runs synchronized
		// (so getting null means there is no network available for a longer time).
		
		final Object lock = new Object();
		
		Runnable worker = new Runnable()	{
			public void run()	{
				try	{
					localHost = InetAddress.getLocalHost();
					if (localHost != null)	// prefer this as NetworkInterface could return another value!
						hostName = localHost.getHostName();
				}
				catch (Exception e)	{
					error = e;
					e.printStackTrace();
				}
				finally	{
					synchronized(lock)	{
						lock.notify();
					}
				}
			}
		};
		
		new Thread(worker).start();

		synchronized(lock)	{
			if (localHost == null)	{
				try	{
					lock.wait(hostNameTimeoutMillis);
				}
				catch (InterruptedException e)	{
					e.printStackTrace();	// will not happen
				}
			}
		}

		// if we got local host data, try to verify them from NetworkInterface (jdk 1.4)
		if (localHost != null && OS.isAboveJava13)	{
			try	{	// since jdk 1.4
				Enumeration netfaces = NetworkInterface.getNetworkInterfaces();

				while(netfaces.hasMoreElements()) {
					NetworkInterface netface = (NetworkInterface) netfaces.nextElement();
					Enumeration ipaddresses = netface.getInetAddresses();
					
					while (ipaddresses.hasMoreElements()){
						InetAddress ip = (InetAddress) ipaddresses.nextElement();
						
						if (ip.isLoopbackAddress() == false && ip.isLinkLocalAddress() == false)
							localHost = ip;
					}
				}	// end while
			}	// end since jdk 1.4
			catch (Throwable e)	{
				e.printStackTrace();
			}
		}
		
		if (localHost == null && error == null)	// make a dummy exception to avoid repeated calls
			error = new Exception("Reason for failed local host retrieval unknown");
	}

	

	// MAC address methods
	
	/**
	 * Returns the MAC address of the network card of this machine, using ifconfig or ipconfig (platform specific).
	 */
	public static String getMacAddress() throws IOException, FileNotFoundException {
		if (macAddress != null)
			return macAddress;
		
		String cmd = null;
		if (OS.isWindows) {
			cmd = "ipconfig /all";
		}
		else { // assume UNIX or Mac OS-X
			cmd = "ifconfig"; // we additionally need its path, as user normally can not call this
			
			String [] possiblePathes = { "/bin/", "/sbin/", "/usr/bin/", "/usr/sbin/", };
			String executable = null;
			for (int i = 0; executable == null && i < possiblePathes.length; i++)
				if (new File(possiblePathes[i], cmd).exists())
					executable = possiblePathes[i] + cmd;
			
			cmd = executable;
		}

		if (cmd == null)
			throw new FileNotFoundException(cmd);

		Process p = Runtime.getRuntime().exec(cmd);
		InputStream is = p.getInputStream();
		try {
			return findMacAddress(new BufferedReader(new InputStreamReader(is)));
		}
		finally {
			try { is.close(); } catch (IOException e) { }
		}
	}

	private static String findMacAddress(Reader r) throws IOException {
		StreamTokenizer stok = new StreamTokenizer(r);
		stok.resetSyntax(); // clear all default tokenizing rules
		stok.whitespaceChars(0, ' '); // 0-space are whitespace
		stok.wordChars(' ' + 1, 255); // non-spaces start above space (32)

		String mac = null;

		// find MAC with minimal word offset from preceding token containing "eth"
		int currentWordDistance = Integer.MAX_VALUE - 2;
		int minimalWordDistance = Integer.MAX_VALUE;

		int type;
		while ((type = stok.nextToken()) != StreamTokenizer.TT_EOF) {
			if (type != StreamTokenizer.TT_WORD)
				continue; // ignore numbers

			if (stok.sval.toUpperCase().indexOf("ETH") != -1) { // test for "eth" token
				currentWordDistance = 0; // now start to count word distance
				continue;
			}

			// we come to here when "eth" was found
			currentWordDistance++;

			String token = testForMac(stok.sval); // try to read MAC address
			if (token != null && currentWordDistance < minimalWordDistance) {
				mac = token; // evaluate return object
				minimalWordDistance = currentWordDistance; // set minimal distance to current
			}
		}
		return mac;
	}

	private static String testForMac(String s) {
		if (s.length() != 17)	// 00-11-2F-52-CF-48
			return null;

		char c = s.charAt(2);
		if (c != ':' && c != '-' ||	// separators can be ':' (LINUX) or '-' (WINDOWS)
				s.charAt(5) != c ||
				s.charAt(8) != c ||
				s.charAt(11) != c ||
				s.charAt(14) != c)
			return null;

		return parseMac(s) != null ? s : null;
	}

	private static byte[] parseMac(String s) {
		try {
			return new byte [] {
				(byte) Integer.parseInt(s.substring(0, 2),   16),
				(byte) Integer.parseInt(s.substring(3, 5),   16),
				(byte) Integer.parseInt(s.substring(6, 8),   16),
				(byte) Integer.parseInt(s.substring(9, 11),  16),
				(byte) Integer.parseInt(s.substring(12, 14), 16),
				(byte) Integer.parseInt(s.substring(15),     16) };
		}
		catch (NumberFormatException e) {
			return null;
		}
	}


	// URL methods

	/**
		Returns an URL String made from the passed file path.
	*/
	public static URL makeURL(File file) throws
		MalformedURLException
	{
		String path = file.getAbsolutePath();
		return makeURL(path);
	}

	/**
		Returns an URL String made from the passed file path.
	*/
	public static URL makeURL(String file) throws
		MalformedURLException
	{
		String urlSep = file.startsWith(File.separator) ? "" : "/";
		URL url = new URL("file://" + urlSep + file.replace(File.separatorChar, '/'));
		return url;
	}

	/**
		Returns an URL from passed URL that references into an JAR File.
		<pre>
		return new URL("jar", "", url + "!/");
		</pre>
	*/
	public static URL makeJarUrl(URL url) throws
		MalformedURLException
	{
		return new URL("jar", "", url + "!/");
	}



	/** Test main that outputs the local hostname and address. */
	public static void main(String [] args)
		throws Exception
	{
		System.err.println("Network local hostname: "+getLocalHostName()+", IP address: "+getLocalHostAddress()+", MAC address: "+getMacAddress()+", error: "+error);

		try	{
			System.err.println("Java local host: "+InetAddress.getLocalHost()+", loopback: "+InetAddress.getLocalHost().isLoopbackAddress());
		}
		catch (Exception e)	{
			e.printStackTrace();
			// workaround for SuSE LINUX 8.1 with DHCP: has correct hostname in error message, after last space
			String error = e.getMessage();
			int i = error != null ? error.lastIndexOf(" ") : -1;
			String host = i > 0 ? error.substring(i).trim() : null;
			System.err.println("Workaround (LINUX 8.1 with DHCP) hostname: "+host);
		}
	}

	private NetUtil()	{}

}
