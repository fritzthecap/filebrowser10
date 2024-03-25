package fri.gui.swing.filebrowser;

import java.awt.Component;
import java.io.*;
import java.util.zip.*;
import java.util.jar.*;
import fri.util.FileUtil;

public class ObservedJarWrite extends ObservedZipWrite
{	
	private Manifest manifest;
	
	
	public ObservedJarWrite(
		Component parent,
		NetNode [] n,
		String filter,
		boolean include,
		boolean showfiles,
		boolean showhidden)
		throws Exception
	{
		super(parent, n, filter, include, showfiles, showhidden);
	}

	
	public ObservedJarWrite(Component parent, NetNode [] n)
		throws Exception
	{
		super(parent, n);
	}


	// overriding ObservedZipWrite methods
	
	/** Overridden to start ManifestDialog and to set title "jar" */
	protected void startProgressDialog(Runnable runnable, Runnable finish, long size, File target)	{
		// let edit manifest
		ManifestDialog md = new ManifestDialog(parent, target.getName());	// modal
		if (md.isCanceled())
			return;	// canceled, do not compress
				
		//this.manifest = null;	
		this.manifest = md.getManifest();
		System.err.println("ObservedJarWrite.startProgressDialog manifest = "+this.manifest);
		
		// start observed background thread
		super.startProgressDialog(runnable, finish, size, target);
	}


	protected String getProgressDialogTitle()	{
		return "Jar";
	}
	
	
	// overriding ZipWrite methods
	
	public File getDefaultArchive()	{
		File tgt = super.getDefaultArchive();
		return new File(FileUtil.cutExtension(tgt.getPath())+".jar");
	}


	protected ZipOutputStream openZipOutputStream(OutputStream os)
		throws Exception
	{
		System.err.println("ObservedJarWrite.openZip, manifest is "+this.manifest);
		if (this.manifest == null)	{
			return new JarOutputStream(os);
		}
		System.err.println("jar output with manifest");
		return new JarOutputStream(os, this.manifest);
	}


	protected ZipEntry getArchiveEntry(String name)	{
		return new JarEntry(name);
	}

}