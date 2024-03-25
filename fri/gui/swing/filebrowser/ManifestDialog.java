package fri.gui.swing.filebrowser;

import java.awt.*;
import javax.swing.*;
import java.util.jar.*;
import java.util.Properties;
import java.util.Enumeration;
import java.io.*;
import fri.gui.swing.ComponentUtil;
import fri.gui.swing.propdialog.*;

/**
	Let write name/value pairs for JarFile Manifest.
	Names can be "[A-Za-z0-9_-]+" and be 70 characters long.
	Values can be any String.
*/

public class ManifestDialog
{
	public static String manifestFile = "Manifest.template";
	private PropEditDialog dlg;	// delegate object
	private Manifest manifest = null;
	private Attributes.Name [] defaultNames = new Attributes.Name [] {
		Attributes.Name.MANIFEST_VERSION,
		Attributes.Name.SIGNATURE_VERSION,
		Attributes.Name.CONTENT_TYPE,
		Attributes.Name.CLASS_PATH,
		Attributes.Name.MAIN_CLASS,
		Attributes.Name.SEALED,
		Attributes.Name.IMPLEMENTATION_TITLE,
		Attributes.Name.IMPLEMENTATION_VERSION,
		Attributes.Name.IMPLEMENTATION_VENDOR,
		Attributes.Name.SPECIFICATION_TITLE,
		Attributes.Name.SPECIFICATION_VERSION,
		Attributes.Name.SPECIFICATION_VENDOR,
		new Attributes.Name("Java-Bean"),
	};
	
	/*
		Manifest-Version: 1.0
		Main-Class: SmileyFrame
		Sealed: True
		Specification-Title: "FRi specified a Smiley" 
		Specification-Version: "4.2"
		Specification-Vendor: "FRi specifications, Inc.".
		Implementation-Title: "FRi implemented in Java" 
		Implementation-Version: "Mai 1999"
		Implementation-Vendor: "FRi implementations, Inc."
		//Java-Bean: True
	*/
	
	public ManifestDialog(Component parent, String title)	{
		Properties props = getManifestProps();
		
		parent = ComponentUtil.getWindowForComponent(parent);
		if (parent instanceof JFrame)
			dlg = new PropEditDialog((JFrame)parent, props, "Manifest for "+title);	// modal
		else
			dlg = new PropEditDialog((JDialog)parent, props, "Manifest for "+title);	// modal

		dlg.show();

		System.err.println("ManifestDialog finished");
		if (dlg.isCanceled() == false)	{
			props = dlg.getProperties();
			manifest = propsToManifest(props);
		}
	}
	

	public Manifest getManifest()	{
		//System.err.println("ManifestDialog getManifest "+manifest);
		return manifest;
	}
	
	public boolean isCanceled()	{
		return dlg.isCanceled();
	}
	
	
	private Properties getManifestProps()	{
		Properties props = new Properties();
		for (int i = 0; i < defaultNames.length; i++)	{
			if (defaultNames[i].equals(Attributes.Name.MANIFEST_VERSION))
				props.put(defaultNames[i].toString(), "1.0");
			else
				props.put(defaultNames[i].toString(), "");
		}
		File m = new File(manifestFile);
		FileInputStream in = null;
		if (m.exists() && m.length() > 0)	{
			try	{
				props.load(in = new FileInputStream(m));
				in.close();
			}
			catch (Exception e)	{
				if (in != null)
					try	{ in.close(); } catch (Exception ex)	{}
				System.err.println("FEHLER: Laden "+manifestFile+", "+e);
			}
		}
		return props;
	}
	
	
	private Manifest propsToManifest(Properties props)	{
		Attributes attr = null;
		for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )	{
			String name = (String)e.nextElement();
			String value = (String)props.getProperty(name);
			if (value != null && value.trim().equals("") == false)	{
				//System.err.println("  name = >"+name+"<  value = >"+value+"<");
				if (manifest == null)	{
					manifest = new Manifest();
					attr = manifest.getMainAttributes();
				}
				attr.put(new Attributes.Name(name), value);
			}
		}
		System.err.println("ManifestDialog.propsToManifest return "+manifest);
		FileOutputStream out;
		try	{
			manifest.write(System.err);
			manifest.write(out = new FileOutputStream(manifestFile));
			out.flush();
			out.close();
		}
		catch (Exception e)	{
		}
		return manifest;
	}

}