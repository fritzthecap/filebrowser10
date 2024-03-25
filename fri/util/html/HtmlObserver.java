package fri.util.html;

import java.net.*;

public interface HtmlObserver
{
	public void gotAHREF( String urlStr, URL contextUrl, Object clientData );
	public void gotIMGSRC( String urlStr, URL contextUrl, Object clientData );
	public void gotFRAMESRC( String urlStr, URL contextUrl, Object clientData );
	public void gotBASEHREF( String urlStr, URL contextUrl, Object clientData );
	public void gotAREAHREF( String urlStr, URL contextUrl, Object clientData );
	public void gotLINKHREF( String urlStr, URL contextUrl, Object clientData );
	public void gotBODYBACKGROUND( String urlStr, URL contextUrl, Object clientData );
	public void gotTABLEBACKGROUND( String urlStr, URL contextUrl, Object clientData );
	public void gotAPPLETCODE( String urlStr, URL contextUrl, Object clientData );
	public void gotAPPLETCODEBASE( String titleStr, URL contextUrl, Object clientData );
	public void gotAPPLETARCHIVE( String urlStr, URL contextUrl, Object clientData );
	public void gotOBJECTDATA( String urlStr, URL contextUrl, Object clientData );
	public void gotLAYERSRC( String urlStr, URL contextUrl, Object clientData );
	public void gotLAYERBACKGROUND( String urlStr, URL contextUrl, Object clientData );
	public void gotSCRIPTSRC( String urlStr, URL contextUrl, Object clientData );
	public void gotTITLE( String titleStr, URL contextUrl, Object clientData );
	public void gotHEAD( String headStr, URL contextUrl, Object clientData );

}