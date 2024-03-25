package fri.util.file.archive;

import java.io.*;

/**
	Factory for getting archive objects from TAR, TGZ, JAR (EAR, WAR, SAR, RAR), ZIP, BZ2 files.
	The factory's behaviour depends on the filename extension.
*/

public abstract class ArchiveFactory
{
	public static final String TAR_EXTENSION = ".tar";
	public static final String TAR_GZ_EXTENSION = ".tar.gz";
	public static final String TARGZ_EXTENSION = ".tgz";
	public static final String TAR_BZ2_EXTENSION = ".tar.bz2";
	public static final String ZIP_EXTENSION = ".zip";
	public static final String JAR_EXTENSION = ".jar";	// Java archive
	public static final String EAR_EXTENSION = ".ear";	// enterprise archive
	public static final String WAR_EXTENSION = ".war";	// web archive
	public static final String SAR_EXTENSION = ".sar";	// system archive
	public static final String RAR_EXTENSION = ".rar";	// resource adapter archive
	
	public static final String [] EXTENSIONS = new String []	{
		TAR_EXTENSION,
		TAR_GZ_EXTENSION,
		TARGZ_EXTENSION,
		TAR_BZ2_EXTENSION,
		ZIP_EXTENSION,
		JAR_EXTENSION,
		EAR_EXTENSION,
		WAR_EXTENSION,
		SAR_EXTENSION,
		RAR_EXTENSION,
	};
	
	private ArchiveFactory()	{}

	/**
		Creates a (generic) archive object from passed file. The willNeedAllEntries setting is false.
	*/
	public static Archive newArchive(File archiveFile)
		throws Exception
	{
		return newArchive(archiveFile, false);
	}
	
	/**
		Creates a (generic) archive object from passed file.
		@param willNeedAllEntries true when all entries will be extracted, false when only some will be needed.
	*/
	public static Archive newArchive(File archiveFile, boolean willNeedAllEntries)
		throws Exception
	{
		if (isTar(archiveFile))	{
			return new TarFile(archiveFile, isGZipTar(archiveFile), isBZip2Tar(archiveFile), willNeedAllEntries);
		}
		else	{	// assume it is a ZIP as this view could have been forced. ZipException or IOException will be thrown!
			return new ZipFile(archiveFile);
		}
	}
	
	
	/**
		Returns true if passed file is not a directory and its name
		ends with ".zip", ".jar", ".ear", ".war", ".sar", ".rar", ".tgz", ".tar.gz", "tar.bz2".
	*/
	public static boolean isArchive(File f)	{
		return f.isDirectory() == false && isArchive(convertFilename(f.getName()));
	}
	
	public static boolean isTar(File f)	{
		return isTar(convertFilename(f.getName()));
	}

	public static boolean isGZipTar(File f)	{
		return isGZipTar(convertFilename(f.getName()));
	}
	
	public static boolean isBZip2Tar(File f)	{
		return isBZip2Tar(convertFilename(f.getName()));
	}
	
	public static boolean isZip(File f)	{
		return isZip(convertFilename(f.getName()));
	}

	public static boolean isJar(File f)	{
		return isJar(convertFilename(f.getName()));
	}


	/**
		Returns true if passed String
		ends with ".zip", ".jar", ".ear", ".war", ".sar", ".rar", ".tgz", ".tar.gz", "tar.bz2".
		The caller must ensure that the file is no directory!
	*/
	public static boolean isArchive(String name)	{
		return isZip(name) || isTar(name);
	}
	
	private static boolean isTar(String name)	{
		return name.endsWith(TAR_EXTENSION) || isGZipTar(name) || isBZip2Tar(name);
	}

	private static boolean isGZipTar(String name)	{
		return name.endsWith(TARGZ_EXTENSION) || name.endsWith(TAR_GZ_EXTENSION);
	}
	
	private static boolean isBZip2Tar(String name)	{
		return name.endsWith(TAR_BZ2_EXTENSION);
	}
	
	private static boolean isZip(String name)	{
		return name.endsWith(ZIP_EXTENSION) || isJar(name);
	}

	private static boolean isJar(String name)	{
		return
				name.endsWith(JAR_EXTENSION) ||
				name.endsWith(EAR_EXTENSION) ||
				name.endsWith(WAR_EXTENSION) ||
				name.endsWith(RAR_EXTENSION) ||
				name.endsWith(SAR_EXTENSION);
	}


	private static String convertFilename(String name)	{
		return name.toLowerCase();
	}

}
