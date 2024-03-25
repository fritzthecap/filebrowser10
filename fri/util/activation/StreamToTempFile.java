package fri.util.activation;

import java.io.*;
import fri.util.file.ValidFilename;

/**
	Write a byte stream to a temporary file that gets deleted when VM exits.
*/
public abstract class StreamToTempFile
{
	/**
		Create a temporary file that contains the bytes of passed stream.
		@param is stream to store to file
		@param suggestedFilename name of file, can be null, then it gets constructed from MIME type
		@param mimeType MIME type of stream, can be null, then no extension will be appended to file
		@return the created file
	*/
	public static File create(InputStream is, String suggestedFilename, String mimeType)
		throws IOException
	{
		File createdFile;
		
		if (suggestedFilename != null)	{
			File f = new File(suggestedFilename);	// cut off path
			suggestedFilename = ValidFilename.correctFilename(f.getName());	// substitute all platform-incompatible chars
			suggestedFilename = suggestedFilename.replace(' ', '_');	// on WINDOWS any space makes shell fail finding application
			
			// substitute all "." except last one
			int idx1 = suggestedFilename.indexOf(".");
			int idx2 = suggestedFilename.lastIndexOf(".");
			if (idx2 > 0 && idx1 != idx2)	{	// if more than one dot
				String s = suggestedFilename.substring(0, idx2).replace('.', '_');
				suggestedFilename = s + suggestedFilename.substring(idx2);
			}
			
			createdFile = new File(System.getProperty("java.io.tmpdir"), suggestedFilename);
		}
		else	{	// try to extract extension from MIME type
			int i = mimeType != null ? mimeType.indexOf("/") : -1;
			
			if (i >= 0)	{
				int j = mimeType.indexOf(";");
				String ext = mimeType.substring(i + 1, j > 0 ? j : mimeType.length());
				int k = ext.lastIndexOf("-");	// "audio/x-wav"
				if (k >= 0)
					ext = ext.substring(k + 1);
				createdFile = File.createTempFile("fmail", "."+ext);
			}
			else	{
				createdFile = File.createTempFile("fmail", null);	// sorry for filetype map ...
			}
		}
		
		createdFile = createUniqueFile(createdFile);
		
		OutputStream os = null;
		System.err.println("StreamToTempFile, created temporary file is "+createdFile+", will be deleted on exit.");
		
		try	{
			os = new BufferedOutputStream(new FileOutputStream(createdFile));
			byte [] data = new byte[1024];
			int count;
			while((count = is.read(data)) > 0)	{
				os.write(data, 0, count);
			}
		}
		finally	{
			try { is.close(); } catch (Exception e) {}
			try { os.close(); } catch (Exception e) {}
			createdFile.deleteOnExit();	// delete when VM exits
		}
		
		return createdFile;
	}

	static synchronized File createUniqueFile(File file)	{
		int index = 0;
		while (file.exists())	{	// append increasing number before extension
			index++;

			String path = file.getPath();
			int i = path.lastIndexOf(".");
			if (i > 0)
				path = path.substring(0, i)+"_"+index+path.substring(i);
			else
				path = path+"_"+index;

			file = new File(path);
		}
		return file;
	}
	
}
