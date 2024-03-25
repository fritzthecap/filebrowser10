package fri.util.activation;

import java.io.File;
import fri.util.file.FileString;

/**
 * Adds the UNIX file /etc/mime.types to the underlying MimetypesFileTypeMap (Sun does not do such).
 * 
 * This is where Sun searches for mime.types:
 * <ul>
 * 	<li>user.home/.mime.types</li>
 * 	<li>java.home/lib/mime.types</li>
 * 	<li>activation.jar/META-INF/mimetypes.default</li>
 * 	<li>in all CLASSPATH files named META-INF/mime.types</li>
 * </ul>
 * 
 * @author Fritz Ritzberger, 2008
 */
public class MimetypesFileTypeMap extends javax.activation.MimetypesFileTypeMap
{
	public MimetypesFileTypeMap()	{
		super();
		
		File mimeFile = new File("/etc/mime.types");
		if (mimeFile.exists())	{
			String mimeTypeSpec = FileString.get(mimeFile);
			if (mimeTypeSpec != null && mimeTypeSpec.length() > 0)
				addMimeTypes(mimeTypeSpec);
		}

	}
}
