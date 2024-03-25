package fri.gui.swing.mailbrowser.viewers;

import java.awt.Component;
import javax.mail.*;
import javax.activation.*;

public abstract class ViewerFactory
{
	private static CommandMap fmCommandMap = new MailcapCommandMap(ViewerFactory.class.getResourceAsStream("fmail.mailcap"));
	private static boolean loadInlineViewers = false;
	
	public static Component getViewer(Part part)
		throws Exception
	{
		DataHandler dh = part.getDataHandler();
		
		// first try the application defined command map
		CommandInfo ci = getCommandInfo(dh, CommandMap.getDefaultCommandMap());

		if (ci == null)	{
			dh.setCommandMap(CommandMap.getDefaultCommandMap());
			throw new Exception("No \"open\" or \"view\" command verb found for content type "+part.getContentType());
		}
		
		System.err.println("looking for command object for content type "+dh.getContentType());
		Object bean = ci.getCommandObject(dh, dh.getClass().getClassLoader());
		//Object bean = dh.getBean(ci);	// does not report exceptions!
		System.err.println(" ... found bean "+(bean != null ? ""+bean.getClass() : ""));

		dh.setCommandMap(CommandMap.getDefaultCommandMap());
		
		if (bean == null)
			throw new Exception("No CommandObject bean found for content type "+part.getContentType());
		
		if (bean instanceof Component)
			return (Component)bean;
		
		return null;	// was external launch of platform viewer
	}


	/**
		Returns true if this Part could have an addable Java Component as renderer
		(not a command launcher). Called when an attachment is attributed as "INLINE".
	*/
	public static boolean canViewInline(Part part)
		throws MessagingException
	{
		if (loadInlineViewers == false)
			return false;
		CommandInfo ci = getCommandInfo(part.getDataHandler(), null);
		return ci != null;
	}
	
	
	private static CommandInfo getCommandInfo(DataHandler dh, CommandMap defaultMap)	{
		// first try the application defined command map
		CommandMap cm = fmCommandMap;
		dh.setCommandMap(cm);
		CommandInfo ci = getCommandInfo(dh);
		
		// if this fails for "open" and "view", try default command map
		if (ci == null && defaultMap != null)	{
			dh.setCommandMap(cm = defaultMap);
			ci = getCommandInfo(dh);
			if (ci != null)
				System.err.println(" .... trying default command map, got command info "+ci.getCommandName()+", class "+ci.getCommandClass());
		}
		
		System.err.println("Got CommandInfo = "+ci+" from CommandMap "+cm);
		return ci;
	}
	

	private static CommandInfo getCommandInfo(DataHandler dh)	{
		CommandInfo ci = dh.getCommand("open");
		if (ci == null)	{
			System.err.println("Got no CommandInfo for \"open\", trying \"view\" ...");
			ci = dh.getCommand("view");
		}
		return ci;
	}

}
