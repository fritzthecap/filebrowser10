package fri.util.activation;

import java.io.*;
import javax.activation.*;

public class Main
{
	public static void main(String [] args)	{
		System.err.println("DLL's must be in: "+System.getProperty("java.library.path"));

		if (args.length <= 0)	{
			System.err.println("SYNTAX: java Main wordFile.doc imageFile.jpg ...");
			System.err.println("	... will open files native on WINDOWS platform.");
			//System.exit(1);
			args = new String [1];
			//args[0] = "C:\\Dokumente\\Führerscheinantrag.doc";
			args[0] = "C:\\Dokumente\\Lebenslauf.doc";
		}

		FileTypeMap.setDefaultFileTypeMap(new Win32RegistryFileTypeMap());
		CommandMap.setDefaultCommandMap(new Win32RegistryCommandMap());

		for (int i = 0; i < args.length; i++)	{
			File f = new File(args[i]);
			if (f.exists())	{

				DataSource ds = new FileDataSource(f);
				DataHandler dh = new DataHandler(ds);
				System.err.println("Content Type: "+dh.getContentType().toLowerCase());
				CommandInfo ci = dh.getCommand("open");
				System.err.println("CommandInfo is: "+ci);

				Object bean = dh.getBean(ci);
				System.err.println("bean is: "+bean);
			}
			else	{
				System.err.println("File not found: "+f);
			}
		}
	}

	/*
	public static void main(String [] args)	{
		System.err.println("DLL's must be in: "+System.getProperty("java.library.path"));
		File f = new File(args[0]);
		if (f.exists())	{
			try	{
				String capfile = "mailcap.txt";
				CommandMap.setDefaultCommandMap(
					new MailcapCommandMap(
						Main.class.getResourceAsStream(capfile)));

				String mimefile = "mimetypes.txt";
				FileTypeMap.setDefaultFileTypeMap(
					new MimetypesFileTypeMap(
						Main.class.getResourceAsStream(mimefile)));
			}
			catch (Exception e)	{
				e.printStackTrace();
			}

			DataSource ds = new FileDataSource(f);
			DataHandler dh = new DataHandler(ds);
			System.err.println("Content Type: "+dh.getContentType().toLowerCase());
			CommandInfo ci = dh.getCommand("open");
			System.err.println("CommandInfo is: "+ci);

			Object bean = dh.getBean(ci);
			System.err.println("bean is: "+bean);
		}
		else	{
			System.err.println("File not found: "+f);
		}
	}
	*/

}