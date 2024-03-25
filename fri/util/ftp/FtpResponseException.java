package fri.util.ftp;

import java.io.IOException;

public class FtpResponseException extends IOException	// for convenience
{
	public final FtpServerResponse response;
	
	public FtpResponseException(String sentCommand, FtpServerResponse response)	{
		super(sentCommand);
		this.response = response;
	}
	
	public String getMessage()	{
		String s = super.getMessage();
		s = (response != null) ? "Sent command >"+s+"<, received reply >"+response+"<" : s;
		return s;
	}

}