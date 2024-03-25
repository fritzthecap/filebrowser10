package fri.util.file;

import java.io.*;

/**
	Create the String representation of a File's contents.
*/

public abstract class FileString
{
	/**
		Returns the String content of the passed file identified by name.
	*/
	public static String get(String file)	{
		return get(new File(file));
	}
	
	/**
		Returns the String content of the passed file.
	*/
	public static String get(File file)	{
		long len = file.length();
		
		BufferedReader in = null;
		try	{
			char [] buf = new char[(int)len];
			in = new BufferedReader(new FileReader(file));
			in.read(buf, 0, buf.length);
			return new String(buf);
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
		finally	{
			try	{ in.close(); } catch (Exception e)	{}
		}

		return null;
	}
	
	/**
		Saves the String content to the passed file.
	*/
	public static boolean put(String s, File file)	{
		BufferedWriter out = null;
		try	{
			out = new BufferedWriter(new FileWriter(file));
			out.write(s, 0, s.length());
			return true;
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
		finally	{
			try	{ out.close(); } catch (Exception e)	{}
		}
		return false;
	}
	
	
	public static void main(String [] args)	{
		System.err.println(args.length > 0 ? get(args[0]) : get("fri/util/file/FileString.java"));
	}

}
