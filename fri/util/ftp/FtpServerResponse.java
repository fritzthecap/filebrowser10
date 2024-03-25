package fri.util.ftp;

import java.io.*;

/**
	Parses a FTP server response into return code and the rest (message).
	
	@author Fritz Ritzberger, 2003
*/

public class FtpServerResponse
{
	private static final int ERROR_CODE = Integer.MIN_VALUE;
	
	public final int code;	// 100 - 600
	private String message;	// rest behind code, substring from character index 4


	/**
		Gets the code when response has a three-digit-number at start and a blank behind.
		Gets the message when code not null and response longer than 4 character.
		Message will be empty string if code not null but shorter than 4, else when code is null the
		message will be identical with the response.
		The code is null when its value is Integer.MIN_VALUE.
	*/
	public FtpServerResponse(String response) {
		if (response.length() >= 3 &&	// either only 3-digit-code, or space or '-' after code, everything else is error
				(response.length() == 3 || response.charAt(3) == ' ' || FtpStringUtil.willResponseTextContinue(response)))
			code = parseReplyCode(response);
		else
			code = ERROR_CODE;
			
		if (code != ERROR_CODE)
			if (response.length() > 4)
				message = response.substring(4);
			else
				message = "";
		else
			message = response;
	}


	private int parseReplyCode(String response)	{
		try	{
			return Integer.parseInt(response.substring(0, 3));
		}
		catch (NumberFormatException e)	{
			return ERROR_CODE;
		}
	}
	

	/** Returns the message, without the reply code at start, if there was any. */
	public String getMessage()	{
		return message;
	}
	
	/** Returns code-blank-message. */
	public String toString()	{
		return (code == ERROR_CODE ? "" : ""+code)+" "+getMessage();
	}
	
	

	/** Indicates whether the response code corresponds to a positive preliminary response. */
	protected boolean isPositivePreliminary() {
		return (code >= 100 && code < 200);
	}

	/** Indicates whether the response code corresponds to a positive preliminary response. */
	protected boolean isPositiveIntermediate() {
		return (code >= 300 && code < 400);
	}

	/** Indicates whether the response code corresponds to a positive complete response. */
	protected boolean isPositiveComplete() {
		return (code >= 200 && code < 300);
	}

	/** Indicates whether the response code corresponds to a directory exists error. */
	public boolean isDirectoryExistsError() {
		return code == 521;
	}

	/** Indicates whether the response code corresponds to an action not taken error. */
	public boolean isActionNotTaken() {
		return code == 550;
	}



	
	/**
	 * Reads a complete response from server. A hyphen ('-') at index 3 indicates multiline responses.
	 */
	public static FtpServerResponse getServerResponse(BufferedReader serverIn, PrintStream log)
		throws IOException
	{
		String response;
		boolean done = true;	// assume only one response line
		FtpServerResponse serverResponse = null;	// will hold all response lines as return value
		
		do {
			if (log != null) { log.print("FTP RECEIVE >"); log.flush(); }
			
			response = serverIn.readLine();

			if (response != null)	{
				if (log != null) log.println(response+"<");

				if (serverResponse == null)	{	// at start
					serverResponse = new FtpServerResponse(response);	// get the numeric code
					
					if (serverResponse.code != ERROR_CODE && FtpStringUtil.willResponseTextContinue(response))
						done = false;	// other lines will follow
				}
				else	{	// continued reading of lines until code appears at line start
					serverResponse.message = serverResponse.message+"\n"+response;
					
					FtpServerResponse sr = new FtpServerResponse(response);
					if (sr.code != ERROR_CODE && sr.code == serverResponse.code && FtpStringUtil.willResponseTextContinue(response) == false)
						done = true;
				}
			}
		}
		while (response != null && done == false);
		
		return serverResponse;
	}

}
