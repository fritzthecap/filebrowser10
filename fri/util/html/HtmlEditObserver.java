package fri.util.html;

import java.net.*;

public interface HtmlEditObserver
{
	public String editAHREF(String urlStr, URL contextUrl, Object clientData);
	public String editIMGSRC(String urlStr, URL contextUrl, Object clientData);
	public String editFRAMESRC(String urlStr, URL contextUrl, Object clientData);
	public String editBASEHREF(String urlStr, URL contextUrl, Object clientData);
	public String editAREAHREF(String urlStr, URL contextUrl, Object clientData);
	public String editLINKHREF(String urlStr, URL contextUrl, Object clientData);
	public String editBODYBACKGROUND(String urlStr, URL contextUrl, Object clientData);
	public String editTABLEBACKGROUND( String urlStr, URL contextUrl, Object clientData );
	public String editAPPLETCODE( String urlStr, URL contextUrl, Object clientData );
	public String editAPPLETCODEBASE( String urlStr, URL contextUrl, Object clientData );
	public String editAPPLETARCHIVE( String urlStr, URL contextUrl, Object clientData );
	public String editOBJECTDATA( String urlStr, URL contextUrl, Object clientData );
	public String editLAYERSRC( String urlStr, URL contextUrl, Object clientData );
	public String editLAYERBACKGROUND( String urlStr, URL contextUrl, Object clientData );
	public String editSCRIPTSRC(String urlStr, URL contextUrl, Object clientData);
	public String editTITLE(String urlStr, URL contextUrl, Object clientData);
}