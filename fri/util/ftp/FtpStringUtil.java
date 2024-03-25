package fri.util.ftp;

import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Text work to be done with FTP.
 */
public abstract class FtpStringUtil
{
  /**
   * Workaround when NLIST does not work.
   * @param longListing the result of LIST command.
   * @return the String names of files and dirs in longListing.
   */
  public static String [] getFileAndDirectoryNames(String [] longListLines)  {
    Vector nameList = new Vector(longListLines.length);
    for (int i = 0; i < longListLines.length; i++)  {
      String name = parseNameFromLongListing((String) longListLines[i]);
      if (name != null && isValidFileName(name))  {
        nameList.add(name);
      }
    }
    return (String []) nameList.toArray(new String[nameList.size()]);
  }
  
  private static String parseNameFromLongListing(String line) {
    int i = line.length() - 1;  // set index on last char
    if (i <= 0)
      return null;
    
    while (i > 0 && Character.isWhitespace(line.charAt(i)))
      i--;  // go to left, reading away trailing spaces
    
    int lastNonSpace = i; // for later
    
    // assume UNIX "ls -l" format: the name is at end of line,
    // preceded by a date that ends with a digit
    while (i > 1 &&
        (Character.isWhitespace(line.charAt(i)) == false ||
           Character.isLetter(line.charAt(i - 1))))
      i--;
    
    if (i == 1) { // seems to be no UNIX listing, split by last space
      i = lastNonSpace;
      while (i > 0 && Character.isWhitespace(line.charAt(i)) == false)
        i--;
      
      if (i > 0)  // standing on a space
        i++;  // skip right to non-space
      
      return line.substring(i);
    }
    
    // UNIX listing, go right to first non-space
    while (Character.isWhitespace(line.charAt(i)))
      i++;
    
    return line.substring(i);
  }

  /**
   * @param longListing the result of LIST command, lines separated by "\n".
   * @return the String lines within longListing, tokenized using "\n" as separator.
   */
  public static Vector getLongListAsLines(String longListing)  {
    StringTokenizer stok = new StringTokenizer(longListing, "\n");  // FtpClient guarantees "\n" as separator
    Vector longList = new Vector(stok.countTokens());
    while (stok.hasMoreTokens())
      longList.add(stok.nextToken());
    
    return longList;
  }
  
	/** Returns false if passed name is "." or "..", else true. */
	public static boolean isValidFileName(String dirName)	{
		return dirName.equals(".") == false && dirName.equals("..") == false;
	}
	
	/**
	 * Parses response message and returns data socket IP address in returned string[0] and port in string[1].
	 * Incoming is e.g. "Entering passive mode (124,1,1,1,15,68)", outgoing IP address and port number as Strings.
	 */
	public static Object [] parseCommaSeparatedIPAddressAndPort(String response)	{
		// response example: "Entering passive mode (124,1,1,1,15,68)", where parenthesis are optional
		System.err.println("FTP response to parse is >"+response+"<");

		StringTokenizer st = new StringTokenizer(response, ",");
		int cnt = st.countTokens();
		String[] parts = new String[cnt];
		for (int i = 0; i < cnt; i++) {
			parts[i] = st.nextToken();
		}

		// get IP address
		StringBuffer ipAddress = new StringBuffer();
		
		// first part contains message: "Entering passive mode (124"
		for (int i = 0; i < 3; i++) {
			char c = parts[0].charAt(parts[0].length() - (3 - i));
			if (Character.isDigit(c)) {
				ipAddress.append(c);
			}
		}
		ipAddress.append('.');
		ipAddress.append(parts[1]);
		ipAddress.append('.');
		ipAddress.append(parts[2]);
		ipAddress.append('.');
		ipAddress.append(parts[3]);
		
		// get port
		
		int high = Integer.parseInt(parts[4]) << 8;

		// last part contains port number with brace: "15,68)"
		StringBuffer lowByte = new StringBuffer();
		for(int i = 0; i < parts[5].length() && i < 3; i++) {
			char c = parts[5].charAt(i);
			if (Character.isDigit(c)) {
				lowByte.append(c);
			}
		}
		int low = Integer.parseInt(lowByte.toString());
		
		return new Object [] { ipAddress.toString(), new Integer(high+low) };
	}

	
	/**
	 * Composes IP address and port to a comma separated string, e.g. "124,1,1,1,15,68".
	 */
	public static String buildCommaSeparatedIPAddressAndPort(String host, int port)	{
		System.err.println("FTP arguments to build >"+host+"< >"+port+"<");

		if (port < 0)
	    port = -port;
	  String h = host.replace('.', ',');
	  String p = ""+(port / 256)+","+(port % 256);
	  return h+","+p;
	}

	
	/** Returns the text within "quotes" when there two, else the unchanged text. */
	public static String getTextWithinQuotes(String text)	{
		int start = text.indexOf("\"");
		int end = text.lastIndexOf("\"");
		if (start >= 0 && end > 0 && start < end)
			return text.substring(start + 1, end);
		else
			return text;
	}
	

	/** FTP server respinses can have multiple lines, indicated by a '-' at 4th position (counted from 1). */
	public static boolean willResponseTextContinue(String response)	{
		return response.charAt(3) == '-';
	}
	
	
		
	private FtpStringUtil()	{}	// do not instantiate
	
	public static void main(String [] args)	{
	  String host = "127.0.0.2";
	  int port = -32651;
	  System.out.println(buildCommaSeparatedIPAddressAndPort(host, port));
	  String ftpResponse = "127,0,0,2,127,139";
	  Object [] hostPort = parseCommaSeparatedIPAddressAndPort(ftpResponse);
	  System.out.println("host "+hostPort[0]+", port "+hostPort[1]);
	  
	  String [] longListing = new String [] { "bla bla 17/02/2014  filename with spaces " };
	  System.out.println(Arrays.asList(getFileAndDirectoryNames(longListing)));
    longListing = new String [] { "bla bla filename with spaces " };
    System.out.println(Arrays.asList(getFileAndDirectoryNames(longListing)));
    longListing = new String [] { "bla bla filename <DIR> spa ces " };
    System.out.println(Arrays.asList(getFileAndDirectoryNames(longListing)));
	}
}
