package fri.util.activation;

import java.io.File;
import java.util.*;
import javax.activation.*;
import com.ice.jni.registry.*;

/**
	Provides MIME mapping to file extensions by WINDOWS registry.
	Uses a native library for that purpose.
*/

public class Win32RegistryFileTypeMap extends FileTypeMap
{
	private static Hashtable cache = new Hashtable();


	/**
		Return the type of the file object.
	*/
	public String getContentType(File file)	{
		if (file == null)
			return null;
		String name = file.getName();
		return getContentType(name);
	}

	/**
		Return the type of the file passed in.
	*/
	public String getContentType(String filename)	{
		int last = filename.lastIndexOf(".");
		if (last < 0)
			return null;

		String ext = filename.substring(last);
		String defaultType = null;

		if ((defaultType = (String)cache.get(ext)) != null)
			return defaultType;

		RegistryKey key = null, rkey = Registry.HKEY_CLASSES_ROOT;

		try	{
			// try to get type from Registry HKEY_CLASSES_ROOT
			boolean tryMimeDatabase = false;
			try	{
				key = rkey.openSubKey(ext);
				try	{	defaultType = key.getDefaultValue(); } catch (Exception e)	{}
				String contentType = key.getStringValue("Content Type");
				if (contentType != null)
					defaultType = contentType;
			}
			catch (Exception e)	{
				tryMimeDatabase = true;
				System.err.println("No Content Type in HKEY_CLASSES_ROOT for: "+ext+" - defaultType is >"+defaultType+"<, exception: "+e);
			}
			finally	{
				try	{ key.closeKey(); } catch (Exception e)	{}
			}

			if (tryMimeDatabase || defaultType == null)	{
				// try to get type from Registry MIME Database
				ext = ext.toLowerCase();
				try	{
					key = rkey.openSubKey("MIME\\Database\\Content Type");
					Enumeration e = key.keyElements();
					String type = null;
					for (; type == null && e.hasMoreElements(); )	{
						String mimeType = (String)e.nextElement();

						RegistryKey mimeKey = null;
						String mimeExt = null;
						try	{
							mimeKey = key.openSubKey(mimeType);
							mimeExt = mimeKey.getStringValue("Extension");
						}
						finally	{
							try	{ mimeKey.closeKey(); } catch (Exception ex)	{}
						}

						if (mimeExt != null && mimeExt.toLowerCase().equals(ext))
							type = mimeType;
					}

					if (type != null)	{
						System.err.println("Found Content Type in \"MIME\\Database\\Content Type\" for "+ext+": "+type);
						defaultType = type;
					}
				}
				catch (Throwable e) {
					System.err.println("No Content Type in MIME Database: for: "+ext+" - "+e);
				}
				finally	{
					try	{ key.closeKey(); } catch (Exception e)	{}
				}
			}
		}
		finally	{
			try	{ rkey.closeKey(); } catch (Exception e)	{ }
		}

		if (defaultType == null)	{
			defaultType = "application/octet-stream";
			System.err.println("Could not find Content Type for "+ext+" - taking: "+defaultType);
		}
		
		cache.put(ext, defaultType);

		return defaultType;	// is no MIME type but may work with Win32RegistryCommandMap
	}



	// test main
	public static void main(String [] args)	{
		Win32RegistryFileTypeMap map = new Win32RegistryFileTypeMap();
		String filename = "somefile.java";
		System.err.println("mime type for "+filename+" is "+map.getContentType(new File(filename)));
	}

}