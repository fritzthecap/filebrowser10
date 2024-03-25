package fri.util.html;

import java.net.*;
import java.io.*;

public class HtmlEditScanner extends HtmlScanner implements
	HtmlObserver
{
	private boolean gotEOF = false;
	private boolean stopped = false;
	private byte[] buf = new byte[4096];
	private int bufSize = buf.length;	// real size of read buffer, will change on demand
	private int bufOff = 0;	// current zero point of read operation
	private int bufLen = 0;	// current length of read operation
	private HtmlEditObserver observer;
	private Object clientData;


	public HtmlEditScanner(InputStream s, URL thisUrl, HtmlEditObserver observer, Object clientData) {
		super(s, thisUrl, null, clientData);
		setObserver(this, clientData);
		this.observer = observer;
		this.clientData = clientData;
	}

	
	public void stopScan()	{
		stopped = true;
	}
	

	public int read(byte[] b, int off, int len) throws IOException {
		int targetLen = len;

		while ((bufLen < len || gettingUrl) && !gotEOF) {
			boolean more = false;
			if (bufLen >= len) {	// getting URL
				//System.err.println("  (getting URL ...)");
				targetLen += 64;
				more = true;
			}
			checkBuf(targetLen + 512);
			//System.err.println("read bufOff="+bufOff+", bufLen="+bufLen+", targetLen="+targetLen+", buf.length="+buf.length);
			int r = super.read(buf, bufOff + bufLen, more ? 64 : targetLen - bufLen);

			if (r == -1 || stopped) {
				gotEOF = true;
			}
			else {
				bufLen += r;
			}
		}

		if (bufLen == 0) {
			return -1;
		}

		int i = Math.min(len, bufLen);
		System.arraycopy(buf, bufOff, b, off, i);

		bufOff += i;
		bufLen -= i;

		return i;
	}

	private void checkBuf(int need) {
		if (bufLen == 0) {
			bufOff = 0;
		}

		if (bufOff + need > bufSize) {
			if (need * 2 < bufSize) {
				//System.err.println("MOVE: checkBuf="+need+", bufOff="+bufOff+", bufLen="+bufLen);
				System.arraycopy(buf, bufOff, buf, 0, bufLen);
			}
			else {
				//System.err.println("GROW: checkBuf="+need+", bufOff="+bufOff+", bufLen="+bufLen);
				byte[] newBuf = new byte[need * 2];

				System.arraycopy(buf, bufOff, newBuf, 0, bufLen);

				buf = newBuf;
				bufSize = buf.length;
			}

			bufOff = 0;
		}
	}
	
	public int read() throws IOException {
		byte[] b = new byte[1];
		int r = read(b, 0, 1);

		if (r == -1) {
			return -1;
		}
		else {
			return b[0];
		}
	}

	

	// interface HtmlEditObserver
	
	public void gotAHREF(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editAHREF(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}

	public void gotIMGSRC(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editIMGSRC(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}

	public void gotFRAMESRC(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editFRAMESRC(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}

	public void gotBASEHREF(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editBASEHREF(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}

	public void gotAREAHREF(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editAREAHREF(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}

	public void gotLINKHREF(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editLINKHREF(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}

	public void gotBODYBACKGROUND(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editBODYBACKGROUND(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}

	public void gotSCRIPTSRC(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editSCRIPTSRC(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	public void gotTITLE(String urlStr, URL contextUrl, Object junk) {
		String changedUrlStr = observer.editTITLE(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}

	public void gotHEAD(String urlStr, URL contextUrl, Object junk) {
	}
	
	public void gotTABLEBACKGROUND(String urlStr, URL contextUrl, Object junk)	{
		String changedUrlStr = observer.editTABLEBACKGROUND(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	public void gotAPPLETCODE(String urlStr, URL contextUrl, Object junk)	{
		String changedUrlStr = observer.editAPPLETCODE(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	public void gotAPPLETCODEBASE(String urlStr, URL contextUrl, Object junk)	{
		String changedUrlStr = observer.editAPPLETCODEBASE(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	public void gotAPPLETARCHIVE(String urlStr, URL contextUrl, Object junk)	{
		String changedUrlStr = observer.editAPPLETARCHIVE(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	public void gotOBJECTDATA(String urlStr, URL contextUrl, Object junk)	{
		String changedUrlStr = observer.editOBJECTDATA(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	public void gotLAYERBACKGROUND(String urlStr, URL contextUrl, Object junk)	{
		String changedUrlStr = observer.editLAYERBACKGROUND(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	public void gotLAYERSRC(String urlStr, URL contextUrl, Object junk)	{
		String changedUrlStr = observer.editLAYERSRC(urlStr, contextUrl, clientData);
		if (changedUrlStr != null) {
			substitute(urlStr.length(), changedUrlStr);
		}
	}
	
}