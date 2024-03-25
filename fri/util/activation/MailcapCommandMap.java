package fri.util.activation;

import java.io.*;
import java.util.*;
import javax.activation.CommandMap;
import javax.activation.CommandInfo;
import javax.activation.DataContentHandler;
import fri.util.activation.MailcapFile;

/**
 * Copied and adapted from javax.activation.MailcapCommandMap. <br>
 * Added support for commandlines without "x-java-xxx" options. <br>
 * Excludes the default map (no x-java-xxx options are provided). <br>
 * Added searching for ".mailcap" in "user.dir" (current directory) after "user.home". <br>
 * Added searching for "mailcap" in "/etc/" and "/usr/etc/". <br>
 */

public class MailcapCommandMap extends CommandMap
{
	private static final int PROG = 0;
	private static final int HOME = 1;
	private static final int DIR = 2;
	private static final int OS = 3;
	private static boolean debug;

	static {
		try {
			debug = Boolean.getBoolean("javax.activation.debug");
		}
		catch (Throwable _ex) {
		}
	}

	private MailcapFile DB[];

	public MailcapCommandMap() {
		// allocate DB's
		DB = new MailcapFile[OS + 1];

		if (debug)
			System.err.println("MailcapCommandMap: load HOME");

		try {
			String s = System.getProperty("user.home");
			if (s != null) {
				String s2 = s + File.separator + ".mailcap";
				DB[HOME] = loadFile(s2);
			}
		}
		catch (SecurityException _ex) {
		}

		try {
			// try current directory
			String s = System.getProperty("user.dir");
			if (s != null) {
				String s2 = s + File.separator + ".mailcap";
				DB[DIR] = loadFile(s2);
			}
		}
		catch (SecurityException _ex) {
		}

		// try UNIX directory
		try {
			DB[OS] = loadFile("/etc/mailcap");
			if (DB[OS] == null)
				DB[OS] = loadFile("/usr/etc/mailcap");
		}
		catch (SecurityException _ex) {
		}
	}

	public MailcapCommandMap(String s) throws IOException {
		this();
		if (debug)
			System.err.println("MailcapCommandMap: load PROG from " + s);
		if (DB[PROG] == null)
			DB[PROG] = new MailcapFile(s);
	}

	public MailcapCommandMap(InputStream inputstream) {
		this();
		if (debug)
			System.err.println("MailcapCommandMap: load PROG");
		if (DB[PROG] == null)
			try {
				DB[PROG] = new MailcapFile(inputstream);
				return;
			}
			catch (IOException _ex) {
				return;
			}
		else
			return;
	}


	private MailcapFile loadFile(String s) {
		MailcapFile mailcapfile = null;
		try {
			mailcapfile = new MailcapFile(s);
		}
		catch (IOException _ex) {
		}
		return mailcapfile;
	}

	/**
	 * Return ommandlines for the hardcoded "open" verb (when no Java class was defined by an "x-java-" option in mailcap
	 * file). This is used in UnixCommandLauncher.
	 * 
	 * @param mimeType
	 *          requested MIME type.
	 * @return command Vector where every element is a Vector containing 1. String command, 2. String test command, 3.
	 *         options Vector. These values should be retrieved with getMailcapCommandLine(), getMailcapCommandTest(),
	 *         getMailcapCommandOptions().
	 */
	public Vector getMailcapCommandList(String mimeType) {
		Vector all = null;
		for (int i = 0; i < DB.length; i++) {
			if (DB[i] != null) // every MailcapFile can do this as only sort order is needed
			{
				Vector v = DB[i].getMailcapCommandList(mimeType);
				if (v != null) {
					if (all == null)
						all = new Vector();
					all.addElement(v.elementAt(0));
					if (v.size() > 1)
						all.addElement(v.elementAt(1));
				}
			}
		}
		
		return all;
	}

	/**
	 * Return a commandline for the hardcoded "open" verb (when no Java class was defined by an "x-java-" option in
	 * mailcap file). This is used in UnixCommandLauncher.
	 * 
	 * @param v
	 *          Vector retrieved by getMailcapCommandList().
	 * @return command for passed command line.
	 */
	public String getMailcapCommandLine(Vector v) {
		for (int i = 0; i < DB.length; i++) {
			if (DB[i] != null) // every MailcapFile can do this as only sort order is needed
			{
				return DB[i].getMailcapCommandLine(v);
			}
		}
		return null;
	}

	/**
	 * Return the "test" condition command for the hardcoded "open" command. The main command must not be executed when
	 * test returns non-zero. This is used in UnixCommandLauncher.
	 * 
	 * @param v
	 *          Vector retrieved by getMailcapCommandList().
	 * @return test command for passed command line.
	 */
	public String getMailcapCommandTest(Vector v) {
		for (int i = 0; i < DB.length; i++) {
			if (DB[i] != null) // every MailcapFile can do this as only sort order is needed
			{
				return DB[i].getMailcapCommandTest(v);
			}
		}
		return null;
	}

	/**
	 * Return a list of options for the hardcoded "open" command. This could be "needsterminal" (needs in- and output) and
	 * "copiousoutput" (needs pager). This is used in UnixCommandLauncher.
	 * 
	 * @param v
	 *          Vector retrieved by getMailcapCommandList().
	 * @return Vector of String.
	 */
	public Vector getMailcapCommandOptions(Vector v) {
		for (int i = 0; i < DB.length; i++) {
			if (DB[i] != null) // every MailcapFile can do this as only sort order is needed
			{
				return DB[i].getMailcapCommandOptions(v);
			}
		}
		return null;
	}

	public synchronized CommandInfo[] getPreferredCommands(String s) {
		Vector vector = new Vector();
		for (int i = 0; i < DB.length; i++)
			if (DB[i] != null) {
				Hashtable hashtable = DB[i].getMailcapList(s);
				if (hashtable != null)
					appendPrefCmdsToVector(hashtable, vector);
			}

		CommandInfo acommandinfo[] = new CommandInfo[vector.size()];
		vector.copyInto(acommandinfo);
		return acommandinfo;
	}

	private void appendPrefCmdsToVector(Hashtable hashtable, Vector vector) {
		for (Enumeration enumeration = hashtable.keys(); enumeration.hasMoreElements();) {
			String s = (String) enumeration.nextElement();
			if (!checkForVerb(vector, s)) {
				Vector vector1 = (Vector) hashtable.get(s);
				String s1 = (String) vector1.firstElement();
				vector.addElement(new CommandInfo(s, s1));
			}
		}

	}

	private boolean checkForVerb(Vector vector, String s) {
		for (Enumeration enumeration = vector.elements(); enumeration.hasMoreElements();) {
			String s1 = ((CommandInfo) enumeration.nextElement()).getCommandName();
			if (s1.equals(s))
				return true;
		}

		return false;
	}

	public synchronized CommandInfo[] getAllCommands(String s) {
		Vector vector = new Vector();
		for (int i = 0; i < DB.length; i++)
			if (DB[i] != null) {
				Hashtable hashtable = DB[i].getMailcapList(s);
				if (hashtable != null)
					appendCmdsToVector(hashtable, vector);
			}

		CommandInfo acommandinfo[] = new CommandInfo[vector.size()];
		vector.copyInto(acommandinfo);
		return acommandinfo;
	}

	private void appendCmdsToVector(Hashtable hashtable, Vector vector) {
		for (Enumeration enumeration = hashtable.keys(); enumeration.hasMoreElements();) {
			String s = (String) enumeration.nextElement();
			Vector vector1 = (Vector) hashtable.get(s);
			String s1;
			for (Enumeration enumeration1 = vector1.elements(); enumeration1.hasMoreElements(); vector.insertElementAt(new CommandInfo(s, s1), 0))
				s1 = (String) enumeration1.nextElement();

		}

	}

	public synchronized CommandInfo getCommand(String s, String s1) {
		for (int i = 0; i < DB.length; i++)
			if (DB[i] != null) {
				Hashtable hashtable = DB[i].getMailcapList(s);
				if (hashtable != null) {
					Vector vector = (Vector) hashtable.get(s1);
					if (vector != null) {
						String s2 = (String) vector.firstElement();
						if (s2 != null)
							return new CommandInfo(s1, s2);
					}
				}
			}

		return null;
	}

	public synchronized void addMailcap(String s) {
		if (debug)
			System.err.println("MailcapCommandMap: add to PROG");
		if (DB[PROG] == null)
			DB[PROG] = new MailcapFile();
		DB[PROG].appendToMailcap(s);
	}

	private CommandMap defaultCommandMap;

	public synchronized DataContentHandler createDataContentHandler(String mimeType) {
		//throw new RuntimeException("Not implemented: MailcapCommandMap.createDataContentHandler");
		//return null;

		if (defaultCommandMap == null)
			defaultCommandMap = new javax.activation.MailcapCommandMap();

		return defaultCommandMap.createDataContentHandler(mimeType);
	}

	/** test main */
	public static void main(String [] args) {
		MailcapCommandMap map = new MailcapCommandMap();
		System.err.println("Command lines for image/* are: "+map.getMailcapCommandList("image/*"));
		System.err.println("Command lines for text/plain are: "+map.getMailcapCommandList("text/plain"));
		System.err.println("Command lines for application/postscript are: "+map.getMailcapCommandList("application/postscript"));
		System.err.println("Command lines for application/pdf are: "+map.getMailcapCommandList("application/pdf"));
	}

}
