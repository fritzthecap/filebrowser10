package fri.util.io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

/**
	Check the underlying InputStreamReader for newlines and
	provide a method to retrieve the detected newline sequence.
	<p />
	The detected newlines are one of "\r\n", "\r", "\n".
	As soon as one of them has been identified uniquely,
	input is checked no more.
	<p />
	This works safe only with BufferedReader, as this calls for 8192 bytes
	at first time, and "lines" are expected to be shorter than this.
	
	@author Ritzberger Fritz, 2002
*/

public class NewlineDetectingInputStreamReader extends InputStreamReader
{
	private boolean was13 = false, was10 = false;
	private String newline = null;
	private boolean endedWithNewline;
	private char [] fileEnd;
	
	
	/** This Reader must work with an InputStream as only this reads bytes. */
	public NewlineDetectingInputStreamReader(InputStream in)	{
		super(in);
	}
	
	/**
		Overridden to check the newly read buffer for newlines
		as long as the newline has not been identified. This is
		safe as the read() method calls this method!
	*/
	public int read(char [] cbuf, int off, int len)
		throws IOException
	{
		int ret = super.read(cbuf, off, len);
		
		if (ret > 0)	{
			// find out newline sequence
			if (newline == null)	{
				boolean is13 = false;
				boolean is10 = false;
				
				for (int i = off, max = off + ret; newline == null && i < max; i++)	{
					is13 = cbuf[i] == '\r';
					is10 = cbuf[i] == '\n';
					
					if (was13 && is10)	{
						newline = "\r\n";
					}
					else
					if (was10)	{
						newline = "\n";
					}
					else
					if (was13)	{
						newline = "\r";
					}
					
					was13 = is13;
					was10 = is10;
				}
			
				if (ret == 1)	// when there was only one character
					if (is13)
						newline = "\r";
					else
					if (is10)
						newline = "\n";
			}
			
			// find out if file ends with newline
			if (newline != null)	{
				if (fileEnd == null)
					fileEnd = new char[newline.length()];
					
				if (ret == 1 && fileEnd.length > 1)	{
					fileEnd[0] = fileEnd[1];
					fileEnd[1] = cbuf[off];
				}
				else
				if (fileEnd.length == 1)	{
					fileEnd[0] = cbuf[off + ret - 1];
				}
				else	{
					fileEnd[0] = cbuf[off + ret - 2];
					fileEnd[1] = cbuf[off + ret - 1];
				}
			}
		}
		else
		if (ret == -1)	{
			endedWithNewline = fileEnd != null && new String(fileEnd).equals(newline);
		}
		
		return ret;
	}
	
	
	/**
		Returns the first detected newline, one of "\r\n", "\r", "\n", or "" if none was found.
	*/
	public String getNewline()	{
		return newline != null ? newline : "";
	}
	
	/**
		Returns true if the file ended with a newline.
	*/
	public boolean endedWithNewline()	{
		return endedWithNewline;
	}
	
	
	/*
	public static void main(String [] args)	{
		try	{
			//InputStream in = new java.io.FileInputStream("fri/util/io/NewlineAwareInputStreamReader.java");
			InputStream in = new java.io.FileInputStream("aaa");
			NewlineAwareInputStreamReader r = new NewlineAwareInputStreamReader(in);
			java.io.BufferedReader br = new java.io.BufferedReader(r);
			
			String line;
			for (; (line = br.readLine()) != null; ) {
				System.err.print(">"+line+"<");
				System.err.println(new fri.util.dump.ByteString(r.getNewline()));
			}
			System.err.println("ended with newline: "+r.endedWithNewline());
			
			br.close();
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
	}
	*/
	
}
