package fri.util.ftp;

import java.io.*;

/**
	Copy and move from a FTP server to another FTP server.
	Usage:
	<pre>
		FtpClient c1 = new FtpClient("downloads.viaarena.com");
		// ObservableFtpClient c1 = new ObservableFtpClient(observer, "downloads.viaarena.com");
		FtpClient c2 = new FtpClient("ftp.friware.at", 21, "fritz.ritzberger", "password".getBytes());
		try	{
			new FtpClientToClient(c1, c2).copyFile("fun/VIA800.jpg");
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		finally	{
			try	{ c1.disconnect(); } catch (Exception e)	{}
			try	{ c2.disconnect(); } catch (Exception e)	{}
		}
	</pre>
*/

public class FtpClientToClient
{
	private FtpClient sourceClient, targetClient;


	/**
		Bind two FTP clients for a following copy or move action.
		To achieve an observable and interruptable copy or move process pass an
		<i>ObservableFtpClient</i> as <b>sourceClient</b>.
	*/
	public FtpClientToClient(FtpClient sourceClient, FtpClient targetClient)	{
		this.sourceClient = sourceClient;
		this.targetClient = targetClient;
	}
	
	
	/**
		Copy a file from one FTP server to another, keeping the same file name.
		The source file must be a file (not a directory).
	*/
	public void copyFile(String sourceFile)
		throws IOException, FtpResponseException
	{
		copyFile(sourceFile, sourceFile);
	}
	
	/**
		Copy a file to another FTP server, optionally to another name.
		The source file must be a file (not a directory).
	*/
	public void copyFile(String sourceFile, String targetFile)
		throws IOException, FtpResponseException
	{
		// ensure that an optional directory exists on the target server
		String dir = new File(targetFile).getParent();
		if (dir != null)	{
			try	{
				targetClient.mkdir(dir);
			}
			catch (FtpResponseException e)	{
				if (e.response.isDirectoryExistsError() == false)
					throw e;
			}
		}
		
		// copy file from sourceClient to targetClient
		InputStream inStream = null;
		OutputStream outStream = null;
		try	{
			inStream = sourceClient.getInputStream(sourceFile);
			outStream = targetClient.getOutputStream(targetFile);
			sourceClient.getConnection().copy(inStream, outStream);
		}
		finally	{
			try	{
				inStream.close();
			}
			finally	{
				outStream.close();
			}
		}		
	}
	
	
	/**
		Move from one FTP server to another:
		copy the file to the other server and delete it after successful transfer.
		This is NOT for moving files to another folder on the same server,
		use <i>client.renameTo(...)</i> for that purpose!
	*/
	public void moveFile(String sourceFile, String targetFile)
		throws IOException, FtpResponseException
	{
		copyFile(sourceFile, targetFile);	// any exception terminates processing
		
		sourceClient.deleteFile(sourceFile);
	}
	
	/**
		Move a file to another FTP server, keeping the same name.
	*/
	public void moveFile(String sourceFile)
		throws IOException, FtpResponseException
	{
		moveFile(sourceFile, sourceFile);
	}



	// directory methods

	/**
	 * Downloads the specified directory from the FTP server to the specified local path.
	 */
	public void copyDirectory(String sourceDir, String targetDir, boolean recursive)
		throws IOException, FtpResponseException
	{
		String old = sourceClient.pwd();
		sourceClient.chdir(sourceDir);
		copyDirectory(targetDir, recursive);
		sourceClient.chdir(old);
	}

	/**
	 * Downloads the current working directory from the FTP server to the specified local path.
	 */
	private void copyDirectory(String targetDir, boolean recursive)
		throws IOException, FtpResponseException
	{
		try {	// create remote path, ignore error if exists
			targetClient.mkdir(targetDir);
		}
		catch (FtpResponseException e) {
			if (e.response.isDirectoryExistsError() == false && e.response.isActionNotTaken() == false) {
				throw e;
			}
		}
		
		String[] names = sourceClient.listNames();
		String old = sourceClient.pwd();
		
		for (int i = 0; i < names.length; i++) {
			if (FtpStringUtil.isValidFileName(names[i])) {
				String subPath = targetDir + "/" + names[i];
				
				try {
					sourceClient.chdir(names[i]);	// will fail if file
					
					if (recursive) {
						copyDirectory(subPath, recursive);
					}
					
					sourceClient.chdir(old);
				}
				catch (FtpResponseException e) {
					if (e.response.isActionNotTaken()) {
						copyFile(names[i], subPath);
					}
					else {
						throw e;
					}
				}
			}
		}
	}


}
