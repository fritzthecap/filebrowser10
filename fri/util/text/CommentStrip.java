package fri.util.text;

import java.io.*;

/**
	Utility using StreamTokenizer to strip C-style comments. Produces "pretty" source!
*/

public abstract class CommentStrip
{
	/**
		Strip the Java comments from a File.
		Remove trailing spaces and more than one empty line.
		@return a String that does not contain comment like "//" or "/*" any more.
	*/
	public static String stripComments(File file)
		throws IOException
	{
		FileReader r = new FileReader(file);
		BufferedReader br = new BufferedReader(r);
		String s = stripComments(br);
		br.close(); r.close();
		return s;
	}
	
	/**
		Strip the Java comments from a String.
		Remove trailing spaces and more than one empty line.
		@return a String that does not contain comment like "//" or "/*" any more.
	*/
	public static String stripComments(String s)
		throws IOException
	{
		StringReader r = new StringReader(s);
		StringWriter w = new StringWriter();
		stripComments(r, w);
		w.flush(); w.close(); r.close();
		return w.toString();
	}
	
	/**
		Strip the Java comments from the stream.
		Remove trailing spaces and more than one empty line.
		@return a String that does not contain comment like "//" or "/*" any more.
	*/
	public static String stripComments(Reader r)
		throws IOException
	{
		StringWriter w = new StringWriter();
		stripComments(r, w);
		w.flush(); w.close();
		return w.toString();
	}


	
	public static void stripCommentsQuick(Reader r, Writer w)
		throws IOException
	{
		StreamTokenizer tok = new StreamTokenizer(r);

		tok.resetSyntax();
		tok.eolIsSignificant(true);
		tok.slashSlashComments(true);
		tok.slashStarComments(true);

		int type;
		while ((type = tok.nextToken()) != StreamTokenizer.TT_EOF)	{
			w.write(type);
		}
	}
	
	
	/**
		Strip the Java comments from the reader and write it to writer.
		Remove trailing spaces and more than one empty line.
		This method produces a "pretty" output.
	*/
	public static void stripComments(Reader r, Writer w)
		throws IOException
	{
		StreamTokenizer tok = new StreamTokenizer(r);
		tok.resetSyntax();
		//tok.eolIsSignificant(true);
		tok.slashSlashComments(true);
		tok.slashStarComments(true);

		int type;
		StringBuffer sb = null;
		
		while ((type = tok.nextToken()) != StreamTokenizer.TT_EOF)	{
			//w.write(type);
			
			// do some space processing: eliminate spaces before newlines and
			// do not print more than one empty line
			
			switch (type)	{				
				case ' ':
				case '\n':
				case '\t':
				case '\r':	// buffer spaces
					if (sb == null)
						sb = new StringBuffer();
					sb.append((char)type);
					break;
					
				default:
				if (sb != null)	{	// flush buffer
					// print only newlines and not more than two newlines
					int newlines = 0;
					int len = sb.length();
					
					for (int i = 0; i < len; i++)	{
						char c = sb.charAt(i);
						
						boolean moreNewlines = false;
						for (int j = i; j < len; j++)
							if (sb.charAt(j) == '\n')
								moreNewlines = true;
						
						if ((c == '\n' || c == '\r') && newlines < 2 || !moreNewlines)	{
							w.write(c);
							if (c == '\n' && moreNewlines)	// if not a trailing space
								newlines++;
						}
					}
					sb = null;
				}
				w.write(type);
				break;
			}			
		}
	}


	private CommentStrip()	{}


	// test main
	
	public static final void main(String [] args)	{
		if (args.length <= 0)	{
			System.err.println("SYNTAX: java CommentStrip file1.java file2.java ...");
			System.err.println("	Strips comments from the source file and prints to stdout");
			System.exit(1);
		}
		
		fri.util.TimeStopper ts = new fri.util.TimeStopper();
		
		PrintWriter pw = new PrintWriter(System.out);
		
		for (int i = 0; i < args.length; i++)	{
			try	{
				stripCommentsQuick(new FileReader(args[i]), pw);
				pw.flush();
			}
			catch (Exception e)	{
				e.printStackTrace();
			}
		}
		
		System.err.println("time was: "+ts.stopMillis());
	}

}
