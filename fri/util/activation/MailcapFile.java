package fri.util.activation;

import java.io.*;
import java.util.*;
import com.sun.activation.registries.MailcapParseException;
import fri.util.activation.MailcapTokenizer;

/**
	Copied from com.sun.activation.registries.MailcapFile.<br>
	Added support for command lines.<br>
*/

public class MailcapFile
{
    private Hashtable type_hash;
    private static boolean debug;

    // hashtable for commandlines: mimeType = [ [ command, test, [ option, option, ... ] ], [ command ], ... ]
    private Hashtable commandLines = new Hashtable();

    static 
    {
        try
        {
            debug = Boolean.getBoolean("javax.activation.debug");
        }
        catch(Throwable _ex)
        {
        }
    }
    
    
    public MailcapFile(String s)
        throws IOException
    {
        if(debug)
            System.err.println("new MailcapFile: file " + s);
        FileReader filereader = null;
        filereader = new FileReader(s);
        type_hash = createMailcapHash(filereader);
        if (debug)
            System.err.println("Got MailcapFile: " + s);
    }

    public MailcapFile(InputStream inputstream)
        throws IOException
    {
        if(debug)
            System.err.println("new MailcapFile: InputStream");
        type_hash = createMailcapHash(new InputStreamReader(inputstream, "iso-8859-1"));
    }

    public MailcapFile()
    {
        if(debug)
            System.err.println("new MailcapFile: default");
        type_hash = new Hashtable();
    }

    public Hashtable getMailcapList(String s)
    {
        Hashtable hashtable = null;
        Hashtable hashtable1 = null;
        hashtable = (Hashtable)type_hash.get(s);
        int i = s.indexOf(MailcapTokenizer.SLASH_TOKEN);
        String s1 = s.substring(0, i + 1) + "*";
        hashtable1 = (Hashtable)type_hash.get(s1);
        if(hashtable1 != null)
            if(hashtable != null)
                hashtable = mergeResults(hashtable, hashtable1);
            else
                hashtable = hashtable1;
        return hashtable;
    }


    /**
    	Returns the possible command lines for passed MIME type, including test and options.
    	@return a Vector where each element is a Vector that represents a possible command line
    		for passed MIME type. First element matches e.g "text/plain", second "text/*".
    */
    public Vector getMailcapCommandList(String mimeType)
    {
        // try exact match
        Vector v1 = (Vector)commandLines.get(mimeType);
        	    
        // try wildcard
        int i = mimeType.indexOf(MailcapTokenizer.SLASH_TOKEN);
        mimeType = mimeType.substring(0, i + 1) + "*";
        Vector v2 = (Vector)commandLines.get(mimeType);

        Vector v = new Vector();
        if (v1 != null)
        	v.addElement(v1);
        if (v2 != null)
        	v.addElement(v2);

        return v.size() > 0 ? v : null;
    }

    /**
    	Returns the command line from the COmamdndList Vector retrieved by
    	getMailcapCommandList().
    	@return String Vector with "standalone" options.
    */
    public String getMailcapCommandLine(Vector v)
    {
      if (v.size() > 0)
			  return (String)v.elementAt(0);
			return null;
    }

    /**
    	Returns the "test" condition command ("test=test -n $DISPLAY")
    	from passed Vector. The main command must not be started if this
    	test command returns other than zero.
    */
    public String getMailcapCommandTest(Vector commandLine)
    {
      String s = (String)commandLine.elementAt(1);
      if (s != null && s.length() > 0)
      		return s;
			return null;
    }

    /**
    	Returns the standalone command options ("needsterminal", "copiousoutput")
    	from passed Vector. "needsterminal" means that in- and output must be
    	managed for the command, "copiousoutput" means that a pager should be used
    	for command output.
    	@return String Vector with "standalone" options.
    */
    public Vector getMailcapCommandOptions(Vector commandLine)
    {
      if (commandLine.size() > 2)
			  return (Vector)commandLine.elementAt(2);
			return null;
    }


    private Hashtable mergeResults(Hashtable hashtable, Hashtable hashtable1)
    {
        Enumeration enumeration = hashtable1.keys();
        Hashtable hashtable2 = (Hashtable)hashtable.clone();
        while(enumeration.hasMoreElements()) 
        {
            String s = (String)enumeration.nextElement();
            Vector vector = (Vector)hashtable2.get(s);
            if(vector == null)
            {
                hashtable2.put(s, hashtable1.get(s));
            }
            else
            {
                Vector vector1 = (Vector)hashtable1.get(s);
                Enumeration enumeration1 = vector1.elements();
                vector = (Vector)vector.clone();
                hashtable2.put(s, vector);
                for(; enumeration1.hasMoreElements(); vector.addElement(enumeration1.nextElement()));
            }
        }

        return hashtable2;
    }

    public void appendToMailcap(String s)
    {
        if(debug)
            System.err.println("appendToMailcap: " + s);
        try
        {
            parse(new StringReader(s), type_hash);
            return;
        }
        catch(IOException _ex)
        {
            return;
        }
    }

    private Hashtable createMailcapHash(Reader reader)
        throws IOException
    {
        Hashtable hashtable = new Hashtable();
        parse(reader, hashtable);
        return hashtable;
    }

    private void parse(Reader reader, Hashtable hashtable)
        throws IOException
    {
        BufferedReader bufferedreader = new BufferedReader(reader);
        String s = null;
        String s1 = null;
        while((s = bufferedreader.readLine()) != null) 
        {
            s = s.trim();
            try
            {
                if(s.charAt(0) != '#')
                    if(s.charAt(s.length() - 1) == '\\')
                    {
                        if(s1 != null)
                            s1 = s1 + s.substring(0, s.length() - 1);
                        else
                            s1 = s.substring(0, s.length() - 1);
                    }
                    else
                    if(s1 != null)
                    {
                        s1 = s1 + s;
                        try
                        {
                            parseLine(s1, hashtable);
                        }
                        catch(MailcapParseException _ex) { }
                        s1 = null;
                    }
                    else
                    {
                        try
                        {
                            parseLine(s, hashtable);
                        }
                        catch(MailcapParseException _ex) { }
                    }
            }
            catch(StringIndexOutOfBoundsException _ex) { }
        }

    }

/*
    public static final int UNKNOWN_TOKEN = 0;
    public static final int START_TOKEN = 1;
    public static final int STRING_TOKEN = 2;
    public static final int EOI_TOKEN = 5;
    public static final int SLASH_TOKEN = 47;
    public static final int SEMICOLON_TOKEN = 59;
    public static final int EQUALS_TOKEN = 61;
*/

    protected void parseLine(String s, Hashtable hashtable)
        throws MailcapParseException, IOException
    {
        MailcapTokenizer mailcaptokenizer = new MailcapTokenizer(s);
        mailcaptokenizer.setIsAutoquoting(false);
        String s1 = "";
        String s2 = "*";

        if(debug)
            System.err.println("parse: " + s);

        int i = mailcaptokenizer.nextToken();
        if(i != MailcapTokenizer.STRING_TOKEN)
            reportParseError(MailcapTokenizer.STRING_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());

        s1 = mailcaptokenizer.getCurrentTokenValue().toLowerCase();
        i = mailcaptokenizer.nextToken();
        if(i != MailcapTokenizer.SLASH_TOKEN && i != MailcapTokenizer.SEMICOLON_TOKEN)
            reportParseError(MailcapTokenizer.SLASH_TOKEN, MailcapTokenizer.SEMICOLON_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());

        if(i == MailcapTokenizer.SLASH_TOKEN)
        {
            i = mailcaptokenizer.nextToken();
            if(i != MailcapTokenizer.STRING_TOKEN)
                reportParseError(MailcapTokenizer.STRING_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());
            s2 = mailcaptokenizer.getCurrentTokenValue().toLowerCase();
            i = mailcaptokenizer.nextToken();
        }
        String mimeType = s1 + "/" + s2;
        if(debug)
            System.err.println("  Type: " + mimeType);
        
        // try to get existng MIME-type hashtable
        Hashtable hashtable1 = (Hashtable)hashtable.get(mimeType);
        if(hashtable1 == null)
        {
        	// if not exists, create and put in
            hashtable1 = new Hashtable();
            hashtable.put(mimeType, hashtable1);
        }
        if(i != MailcapTokenizer.SEMICOLON_TOKEN)
            reportParseError(MailcapTokenizer.SEMICOLON_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());
            
        mailcaptokenizer.setIsAutoquoting(true);


        // name of executable start
        
        i = mailcaptokenizer.nextToken();
        String commandname = "";
        if (i == MailcapTokenizer.STRING_TOKEN)
        	    commandname = mailcaptokenizer.getCurrentTokenValue();
        
        mailcaptokenizer.setIsAutoquoting(false);
        
        if(i != MailcapTokenizer.STRING_TOKEN && i != MailcapTokenizer.SEMICOLON_TOKEN)
            reportParseError(MailcapTokenizer.STRING_TOKEN, MailcapTokenizer.SEMICOLON_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());

        if(i != MailcapTokenizer.SEMICOLON_TOKEN)	{
            i = mailcaptokenizer.nextToken();
        }

        Vector cmdLine = null;

        if (commandname.length() > 0)	{
          // store command for JAF launcher
	        Vector v = new Vector();
	        v.addElement(MailcapCommandLauncher.class.getName());	// one class for all command lines
	        hashtable1.put("open", v);
	
          // store command for UnixShell launcher
	        cmdLine = new Vector();
	        cmdLine.addElement(commandname);
	        cmdLine.addElement("");	// dummy test command, to keep order
	        commandLines.put(mimeType, cmdLine);
	        if (debug)
	            System.err.println("  Executable Command for "+mimeType+" is: " + commandname);
        }
        

        // name of executable end

        
        if(i == MailcapTokenizer.SEMICOLON_TOKEN)
        {
            do
            {
                i = mailcaptokenizer.nextToken();
                if(i != MailcapTokenizer.STRING_TOKEN)
                    reportParseError(MailcapTokenizer.STRING_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());
                    
                String s4 = mailcaptokenizer.getCurrentTokenValue().toLowerCase();
                i = mailcaptokenizer.nextToken();
                if(i != MailcapTokenizer.EQUALS_TOKEN && i != MailcapTokenizer.SEMICOLON_TOKEN && i != MailcapTokenizer.EOI_TOKEN)
                    reportParseError(MailcapTokenizer.EQUALS_TOKEN, MailcapTokenizer.SEMICOLON_TOKEN, MailcapTokenizer.EOI_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());
                    
                if(i == MailcapTokenizer.EQUALS_TOKEN)
                {
                    mailcaptokenizer.setIsAutoquoting(true);
                    i = mailcaptokenizer.nextToken();
                    mailcaptokenizer.setIsAutoquoting(false);
                    if(i != MailcapTokenizer.STRING_TOKEN)
                        reportParseError(MailcapTokenizer.STRING_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());
                        
                    String s5 = mailcaptokenizer.getCurrentTokenValue();

                    if(s4.startsWith("x-java-"))
                    {
                        String s6 = s4.substring(7);
                        if(debug)
                            System.err.println("    Command: " + s6 + ", Class: " + s5);
                            
                        Vector vector = (Vector)hashtable1.get(s6);
                        if(vector == null)
                        {
                            vector = new Vector();
                            hashtable1.put(s6, vector);
                        }
                        vector.insertElementAt(s5, 0);	// insert java class
                    }
                    else
                    if(s4.equals("test") && cmdLine != null)
                    {
                        cmdLine.setElementAt(s5, 1);
                    }

                    i = mailcaptokenizer.nextToken();
                }
                else
                if (cmdLine != null)
                {   // standalone option like "needsterminal"
                    Vector v;
                    if (cmdLine.size() > 2)	{
                        v = (Vector)cmdLine.elementAt(2);
                    }
                    else	{
                        v = new Vector();
                        cmdLine.addElement(v);
                    }
                    v.addElement(s4);
                }
            }
            while(i == MailcapTokenizer.SEMICOLON_TOKEN);
            return;
        }
        
        if(i != MailcapTokenizer.EOI_TOKEN)
            reportParseError(MailcapTokenizer.EOI_TOKEN, MailcapTokenizer.SEMICOLON_TOKEN, i, mailcaptokenizer.getCurrentTokenValue());
        
    }

    protected static void reportParseError(int i, int j, String s)
        throws MailcapParseException
    {
        throw new MailcapParseException("Encountered a " + MailcapTokenizer.nameForToken(j) + " token (" + s + ") while expecting a " + MailcapTokenizer.nameForToken(i) + " token.");
    }

    protected static void reportParseError(int i, int j, int k, String s)
        throws MailcapParseException
    {
        throw new MailcapParseException("Encountered a " + MailcapTokenizer.nameForToken(k) + " token (" + s + ") while expecting a " + MailcapTokenizer.nameForToken(i) + " or a " + MailcapTokenizer.nameForToken(j) + " token.");
    }

    protected static void reportParseError(int i, int j, int k, int l, String s)
        throws MailcapParseException
    {
        if(debug)
            System.err.println("PARSE ERROR: Encountered a " + MailcapTokenizer.nameForToken(l) + " token (" + s + ") while expecting a " + MailcapTokenizer.nameForToken(i) + ", a " + MailcapTokenizer.nameForToken(j) + ", or a " + MailcapTokenizer.nameForToken(k) + " token.");
        throw new MailcapParseException("Encountered a " + MailcapTokenizer.nameForToken(l) + " token (" + s + ") while expecting a " + MailcapTokenizer.nameForToken(i) + ", a " + MailcapTokenizer.nameForToken(j) + ", or a " + MailcapTokenizer.nameForToken(k) + " token.");
    }



    // test main
    public static void main(String [] args)	{
        debug = true;
        MailcapFile mcf = new MailcapFile();
        mcf.appendToMailcap("text/plain;;x-java-view=fri.util.activation.SomeCommandBean");
    }
    
}