package fri.util.props;

import java.io.*;
import java.util.Properties;

/**
	Packing properties to a String that can be unpacked to Properties again.
	This String can be used as "nested" property value. So Properties can
	be stored to a parent Properties object as value.
	<pre>
		// pack to string
		Properties parent = new Properties();
		Properties nestedProperties = new NestableProperties();
		nestedProperties.setProperty(..., ...);
		...
		parent.setProperty("nested", nestedProperties.toString());

		// unpack from string
		String nested = parent.getProperty("nested");
		Properties nestedProperties = new NestableProperties(nested);
	</pre>
*/

public class NestableProperties extends Properties
{
	public NestableProperties()	{
	}
	
	public NestableProperties(String nestedProperties)	{
		InputStream in = new ByteArrayInputStream(nestedProperties.getBytes());
		try	{
			load(in);
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
		finally	{
			try	{ in.close(); }	catch (Exception e)	{}
		}
	}
	
	public String toString()	{
		OutputStream out = new ByteArrayOutputStream();
		try	{
			store(out, null);
			out.close();
			String s = out.toString();
			//System.err.println("Nestable properties >"+s+"<");
			int i = s.indexOf(System.getProperty("line.separator"));
			return s.substring(i + 1);
		}
		catch (IOException e)	{
			try	{ out.close(); }	catch (Exception e2)	{}
			e.printStackTrace();
			return null;
		}
	}

}
