package fri.util.html;

import java.net.*;
import java.io.*;

public class HtmlScanner extends FilterInputStream
{
	private URL url;
	private URL contextUrl;
	protected boolean gettingUrl;
	private boolean stopped;
	private HtmlObserver observer;
	private Object clientData;


	public HtmlScanner(InputStream s, URL thisUrl, HtmlObserver observer) {
		this(s, thisUrl, observer, null);
	}

	public HtmlScanner(InputStream s, URL thisUrl, HtmlObserver observer, Object clientData) {
		super(s);
		
		if ((this.url = thisUrl) != null)	{
			try {
				this.contextUrl = Util.plainUrl(thisUrl.toString());
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		if (observer != null) {
			setObserver(observer, clientData);
		}
	}

	public void scan()	{
		try	{
			close();	// reads all data
		}
		catch (IOException e)	{
			System.err.println("FEHLER in "+url);
			e.printStackTrace();
		}
	}
	
	
	public void setObserver(HtmlObserver observer) {
		setObserver(observer, null);
	}

	public void setObserver(HtmlObserver observer, Object clientData) {
		this.observer = observer;
		this.clientData = clientData;
	}

	private boolean closed = false;

	public void close() throws IOException {
		if (!closed) {
			byte[] b = new byte[4096];
			while (read(b, 0, b.length) != -1)
				;
			in.close();
			closed = true;
		}
	}

	public void stopScan()	{
		stopped = true;
	}

	protected void finalize() throws java.lang.Throwable {
		try { close(); }
		catch (IOException e) {}
		super.finalize();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int r = in.read(b, off, len);

		if (r != -1 && stopped == false) {
			r += interpret(b, off, r);

			if (r < 0) {
				r = 0;
			}
		}

		return r;
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

	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public long skip(long n) throws IOException {
		byte[] b = new byte[(int) n];
		return read(b, 0, (int) n);
	}

	public boolean markSupported() {
		return false;
	}




	// finite state machine for HTML text

	private final static int ST_GROUND = 0;
	
	private final static int ST_LT = 1;
	private final static int ST_LTJUNK = 2;
	private final static int ST_LT_BANG = 3;
	private final static int ST_LT_BANG_DASH = 4;
	
	private final static int ST_COMMENT = 5;
	private final static int ST_COMMENT_DASH = 6;
	private final static int ST_COMMENT_DASH_DASH = 7;
	
	private final static int ST_LT_A = 8;
	private final static int ST_A = 9;
	private final static int ST_A_QUOTE = 10;
	private final static int ST_A_H = 11;
	private final static int ST_A_HR = 12;
	private final static int ST_A_HRE = 13;
	private final static int ST_A_HREF = 14;
	private final static int ST_A_HREF_EQUAL = 15;
	private final static int ST_AHREF_Q = 16;
	private final static int ST_AHREF_NQ = 17;
	
	private final static int ST_LT_I = 18;
	private final static int ST_LT_IM = 19;
	private final static int ST_LT_IMG = 20;
	private final static int ST_IMG = 21;
	private final static int ST_IMG_QUOTE = 22;
	private final static int ST_IMG_S = 23;
	private final static int ST_IMG_SR = 24;
	private final static int ST_IMG_SRC = 25;
	private final static int ST_IMG_SRC_EQUAL = 26;
	private final static int ST_IMGSRC_Q = 27;
	private final static int ST_IMGSRC_NQ = 28;
	
	private final static int ST_LT_F = 29;
	private final static int ST_LT_FR = 30;
	private final static int ST_LT_FRA = 31;
	private final static int ST_LT_FRAM = 32;
	private final static int ST_LT_FRAME = 33;
	private final static int ST_FRAME = 34;
	private final static int ST_FRAME_QUOTE = 35;
	private final static int ST_FRAME_S = 36;
	private final static int ST_FRAME_SR = 37;
	private final static int ST_FRAME_SRC = 38;
	private final static int ST_FRAME_SRC_EQUAL = 39;
	private final static int ST_FRAMESRC_Q = 40;
	private final static int ST_FRAMESRC_NQ = 41;
	
	private final static int ST_LT_B = 42;
	private final static int ST_LT_BA = 43;
	private final static int ST_LT_BAS = 44;
	private final static int ST_LT_BASE = 45;
	private final static int ST_BASE = 46;
	private final static int ST_BASE_QUOTE = 47;
	private final static int ST_BASE_H = 48;
	private final static int ST_BASE_HR = 49;
	private final static int ST_BASE_HRE = 50;
	private final static int ST_BASE_HREF = 51;
	private final static int ST_BASE_HREF_EQUAL = 52;
	private final static int ST_BASEHREF_Q = 53;
	private final static int ST_BASEHREF_NQ = 54;
	
	private final static int ST_LT_AR = 55;
	private final static int ST_LT_ARE = 56;
	private final static int ST_LT_AREA = 57;
	private final static int ST_AREA = 58;
	private final static int ST_AREA_QUOTE = 59;
	private final static int ST_AREA_H = 60;
	private final static int ST_AREA_HR = 61;
	private final static int ST_AREA_HRE = 62;
	private final static int ST_AREA_HREF = 63;
	private final static int ST_AREA_HREF_EQUAL = 64;
	private final static int ST_AREAHREF_Q = 65;
	private final static int ST_AREAHREF_NQ = 66;
	
	private final static int ST_LT_L = 67;
	private final static int ST_LT_LI = 68;
	private final static int ST_LT_LIN = 69;
	private final static int ST_LT_LINK = 70;
	private final static int ST_LINK = 71;
	private final static int ST_LINK_QUOTE = 72;
	private final static int ST_LINK_H = 73;
	private final static int ST_LINK_HR = 74;
	private final static int ST_LINK_HRE = 75;
	private final static int ST_LINK_HREF = 76;
	private final static int ST_LINK_HREF_EQUAL = 77;
	private final static int ST_LINKHREF_Q = 78;
	private final static int ST_LINKHREF_NQ = 79;
	
	private final static int ST_LT_BO = 80;
	private final static int ST_LT_BOD = 81;
	private final static int ST_LT_BODY = 82;
	private final static int ST_BODY = 83;
	private final static int ST_BODY_QUOTE = 84;
	private final static int ST_BODY_B = 85;
	private final static int ST_BODY_BA = 86;
	private final static int ST_BODY_BAC = 87;
	private final static int ST_BODY_BACK = 88;
	private final static int ST_BODY_BACKG = 89;
	private final static int ST_BODY_BACKGR = 90;
	private final static int ST_BODY_BACKGRO = 91;
	private final static int ST_BODY_BACKGROU = 92;
	private final static int ST_BODY_BACKGROUN = 93;
	private final static int ST_BODY_BACKGROUND = 94;
	private final static int ST_BODY_BACKGROUND_EQUAL = 95;
	private final static int ST_BODYBACKGROUND_Q = 96;
	private final static int ST_BODYBACKGROUND_NQ = 97;
	
	private final static int ST_LT_IN = 98;
	private final static int ST_LT_INP = 99;
	private final static int ST_LT_INPU = 100;
	private final static int ST_LT_INPUT = 101;
	private final static int ST_INPUT = 102;
	private final static int ST_INPUT_QUOTE = 103;
	private final static int ST_INPUT_S = 104;
	private final static int ST_INPUT_SR = 105;
	private final static int ST_INPUT_SRC = 106;
	private final static int ST_INPUT_SRC_EQUAL = 107;
	private final static int ST_INPUTSRC_Q = 108;
	private final static int ST_INPUTSRC_NQ = 109;

	private final static int ST_LT_T = 110;	// <TITLE>...</TITLE>
	private final static int ST_LT_TI = 111;
	private final static int ST_LT_TIT = 112;
	private final static int ST_LT_TITL = 113;
	private final static int ST_LT_TITLE = 114;
	private final static int ST_TITLE = 115;

	private final static int ST_LT_SLASH = 116;

	private final static int ST_LT_IF = 117;	// <IFRAME SRC="...">

	private final static int ST_LT_S = 118;	// <SCRIPT SRC="...">
	private final static int ST_LT_SC = 119;
	private final static int ST_LT_SCR = 120;
	private final static int ST_LT_SCRI = 121;
	private final static int ST_LT_SCRIP = 122;
	private final static int ST_LT_SCRIPT = 123;
	private final static int ST_SCRIPT = 124;
	private final static int ST_SCRIPT_QUOTE = 125;
	private final static int ST_SCRIPT_S = 126;
	private final static int ST_SCRIPT_SR = 127;
	private final static int ST_SCRIPT_SRC = 128;
	private final static int ST_SCRIPT_SRC_EQUAL = 129;
	private final static int ST_SCRIPTSRC_Q = 130;
	private final static int ST_SCRIPTSRC_NQ = 131;

	private final static int ST_LT_AP = 133;	// <APPLET CODE= CODEBASE= ARCHIVE= >
	private final static int ST_LT_APP = 134;	// <OBJECT CLASSID= DATA= >
	private final static int ST_LT_APPL = 135;
	private final static int ST_LT_APPLE = 136;
	private final static int ST_LT_APPLET = 137;
	private final static int ST_APPLET = 138;
	private final static int ST_APPLET_QUOTE = 139;
	private final static int ST_APPLET_C = 140;
	private final static int ST_APPLET_CO = 141;
	private final static int ST_APPLET_COD = 142;
	private final static int ST_APPLET_CODE = 143;
	private final static int ST_APPLET_CODE_EQUAL = 144;
	private final static int ST_APPLETCODE_Q = 145;
	private final static int ST_APPLETCODE_NQ = 146;
	private final static int ST_APPLET_A = 147;
	private final static int ST_APPLET_AR = 148;
	private final static int ST_APPLET_ARC = 149;
	private final static int ST_APPLET_ARCH = 150;
	private final static int ST_APPLET_ARCHI = 151;
	private final static int ST_APPLET_ARCHIV = 152;
	private final static int ST_APPLET_ARCHIVE = 153;
	private final static int ST_APPLET_ARCHIVE_EQUAL = 154;
	private final static int ST_APPLETARCHIVE_Q = 155;
	private final static int ST_APPLETARCHIVE_NQ = 156;
	private final static int ST_APPLET_CODEB = 157;
	private final static int ST_APPLET_CODEBA = 158;
	private final static int ST_APPLET_CODEBAS = 159;
	private final static int ST_APPLET_CODEBASE = 160;
	private final static int ST_APPLET_CODEBASE_EQUAL = 161;
	private final static int ST_APPLETCODEBASE_Q = 162;
	private final static int ST_APPLETCODEBASE_NQ = 163;
	
	private final static int ST_LT_TA = 164;	// <TABLE BACKGROUND= > <TR> <TD> <TH>
	private final static int ST_LT_TAB = 165;
	private final static int ST_LT_TABL = 166;
	private final static int ST_LT_TABLE = 167;
	private final static int ST_TABLE = 168;
	private final static int ST_TABLE_QUOTE = 169;
	private final static int ST_TABLE_B = 170;
	private final static int ST_TABLE_BA = 171;
	private final static int ST_TABLE_BAC = 172;
	private final static int ST_TABLE_BACK = 173;
	private final static int ST_TABLE_BACKG = 174;
	private final static int ST_TABLE_BACKGR = 175;
	private final static int ST_TABLE_BACKGRO = 176;
	private final static int ST_TABLE_BACKGROU = 177;
	private final static int ST_TABLE_BACKGROUN = 178;
	private final static int ST_TABLE_BACKGROUND = 179;
	private final static int ST_TABLE_BACKGROUND_EQUAL = 180;
	private final static int ST_TABLEBACKGROUND_Q = 181;
	private final static int ST_TABLEBACKGROUND_NQ = 182;
	private final static int ST_LT_TR = 183;
	private final static int ST_LT_TH = 184;
	private final static int ST_LT_TD = 185;

	private final static int ST_LT_O = 186;	// <OBJECT ...>
	private final static int ST_LT_OB = 187;
	private final static int ST_LT_OBJ = 188;
	private final static int ST_LT_OBJE = 189;
	private final static int ST_LT_OBJEC = 190;
	private final static int ST_LT_OBJECT = 191;	// goes to "applet"
	private final static int ST_OBJECT_D = 194;
	private final static int ST_OBJECT_DA = 195;
	private final static int ST_OBJECT_DAT = 196;
	private final static int ST_OBJECT_DATA = 197;
	private final static int ST_OBJECT_DATA_EQUAL = 198;
	private final static int ST_OBJECTDATA_Q = 199;
	private final static int ST_OBJECTDATA_NQ = 200;
	//private final static int ST_OBJECT_C = 201;
	private final static int ST_OBJECT_CL = 202;
	private final static int ST_OBJECT_CLA = 203;
	private final static int ST_OBJECT_CLAS = 204;
	private final static int ST_OBJECT_CLASS = 205;
	private final static int ST_OBJECT_CLASSI = 206;
	private final static int ST_OBJECT_CLASSID = 207;	// goes to "applet code"

	private final static int ST_LT_LA = 209;	// <LAYER BACKGROUND= SRC= >
	private final static int ST_LT_LAY = 210;
	private final static int ST_LT_LAYE = 211;
	private final static int ST_LT_LAYER = 212;
	private final static int ST_LAYER = 213;
	private final static int ST_LAYER_S = 214;
	private final static int ST_LAYER_SR = 215;
	private final static int ST_LAYER_SRC = 216;
	private final static int ST_LAYER_SRC_EQUAL = 217;
	private final static int ST_LAYERSRC_Q = 218;
	private final static int ST_LAYERSRC_NQ = 219;
	private final static int ST_LAYER_B = 220;
	private final static int ST_LAYER_BA = 221;
	private final static int ST_LAYER_BAC = 222;
	private final static int ST_LAYER_BACK = 223;
	private final static int ST_LAYER_BACKG = 224;
	private final static int ST_LAYER_BACKGR = 225;
	private final static int ST_LAYER_BACKGRO = 226;
	private final static int ST_LAYER_BACKGROU = 227;
	private final static int ST_LAYER_BACKGROUN = 228;
	private final static int ST_LAYER_BACKGROUND = 229;
	private final static int ST_LAYER_BACKGROUND_EQUAL = 230;
	private final static int ST_LAYERBACKGROUND_Q = 231;
	private final static int ST_LAYERBACKGROUND_NQ = 232;
	private final static int ST_LAYER_QUOTE = 233;

	private final static int ST_LT_H = 234;
	private final static int ST_LT_H1 = 235;
	private final static int ST_H1 = 236;

// was fehlt noch:	
// <object usemap="URL">
// <img lowsrc="URL">
// <img lowsrc="URL">
// <head profile="URL"> // rdf Angaben
// <blockquote cite="URL">
// <q cite="URL">
// ...

	private int state = ST_GROUND;

	private StringBuffer tagBuf = new StringBuffer(10);
	private StringBuffer titleBuf = new StringBuffer(100);
	private StringBuffer headBuf = new StringBuffer(100);
	boolean gettingTitle = false;
	boolean gettingHead = false;
	boolean gettingScript = false;
	boolean gotHead = false;

	private StringBuffer urlBuf = new StringBuffer(100);
	private byte[] interpBuf;
	private int interpIndex;
	private int interpEnd;
	private int interpDelta;


	protected void substitute(int oldLen, String newStr)	{
		//System.err.println("substitute "+oldLen+", "+newStr);
		int newLen = newStr.length();
		int d = newLen - oldLen;
		
		try	{

			System.arraycopy(
				interpBuf,
				interpIndex,
				interpBuf,
				interpIndex + d,
				interpEnd - interpIndex);

			byte[] newBytes = newStr.getBytes();

			System.arraycopy(
				newBytes,
				0,
				interpBuf,
				interpIndex - oldLen,
				newLen );

			interpIndex += d;
			interpEnd += d;
			interpDelta += d;
		}
		catch (ArrayIndexOutOfBoundsException e)	{
			e.printStackTrace();
			
			System.err.println(
				"interpretBuf len "+interpBuf.length+
				", interpIndex "+interpIndex+
				", d "+d+
				", interpEnd "+interpEnd+
				", oldLen "+oldLen+
				", newLen "+newLen+
				", gettingUrl "+gettingUrl);
		}
	}


	private int interpret(byte[] b, int off, int len) {
		interpBuf = b;
		interpDelta = 0;
		interpEnd = off + len;

		for (interpIndex = off; stopped == false && interpIndex < interpEnd; ++interpIndex) {
			char ch = (char) b[interpIndex];

			switch (state) {

				case ST_GROUND:
					switch (ch) {
						case '<': 
							state = ST_LT;
							break;
					}

					break;

				case ST_TITLE: 	// FRi
					switch (ch) {
						case '<': 
							state = ST_LT;
							break;
							
						case '\r':
							break;
							 
						default:
							titleBuf.append(ch == '\n' || ch == '\t' ? ' ' : ch);
							break;
					}

					break;

				case ST_H1: 	// FRi
					switch (ch) {
						case '<': 
							state = ST_LT;
							break;
							
						case '\r':
							break;
							 
						default:
							headBuf.append(ch == '\n' || ch == '\t' ? ' ' : ch);
							break;
					}

					break;

				case ST_LT: 
					switch (ch) {
						case '/': 	// FRi
							tagBuf.setLength(0);
							state = ST_LT_SLASH;
							break;

						case '!': 
							state = ST_LT_BANG;
							break;

						case 'A': 
						case 'a': 
							state = ST_LT_A;
							break;

						case 'B': 
						case 'b': 
							state = ST_LT_B;
							break;

						case 'F': 
						case 'f': 
							state = ST_LT_F;
							break;

						case 'H': 
						case 'h': 
							if (gotHead == false)
								state = ST_LT_H;
							else
								state = ST_LTJUNK;
							break;

						case 'I': 
						case 'i': 
							state = ST_LT_I;
							break;

						case 'L': 
						case 'l': 
							state = ST_LT_L;
							break;

						case 'T': 
						case 't': 
							state = ST_LT_T;
							break;

						case 'S': 
						case 's': 
							state = ST_LT_S;
							break;

						case 'O': 
						case 'o': 
							state = ST_LT_O;
							break;

						case '>': 
							if (gettingTitle)
								state = ST_TITLE;
							else
							if (gettingHead) 
								state = ST_H1;
							else
								state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;


				case ST_LT_SLASH: //FRi
					switch (ch) {
						case '>':
							if (gettingTitle)	{
								if (tagBuf.toString().trim().toUpperCase().equals("TITLE"))	{
									callTITLE(titleBuf.toString().trim());
									state = ST_GROUND;
									gettingTitle = false;
								}
								else	{
									state = ST_TITLE;
								}
							}
							else
							if (gettingHead)	{
								if (tagBuf.toString().trim().toUpperCase().equals("H1"))	{
									state = ST_GROUND;
									gettingHead = false;
									String head = headBuf.toString().trim();
									if (head.length() > 0)	{
										callHEAD(head);
										gotHead = true;
									}
								}
								else	{
									state = ST_H1;
								}
							}
							else	{
								state = ST_GROUND;
								if (gettingScript && tagBuf.toString().trim().toUpperCase().equals("SCRIPT"))
									gettingScript = false;
							}
							break;

						default:
							if (gettingTitle || gettingHead || gettingScript) 
								tagBuf.append(ch);
							break;					
					}
					break;
					

				case ST_LTJUNK: 
					switch (ch) {
						case '>': 
							if (gettingTitle)
								state = ST_TITLE;
							else
							if (gettingHead) 
								state = ST_H1;
							else
								state = ST_GROUND;
							break;
					}
					break;

				case ST_LT_BANG: 
					switch (ch) {
						case '-': 
							state = ST_LT_BANG_DASH;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_BANG_DASH: 
					switch (ch) {
						case '-': 
							state = ST_COMMENT;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_COMMENT: 
					switch (ch) {
						case '-': 
							state = ST_COMMENT_DASH;
							break;
					}

					break;

				case ST_COMMENT_DASH: 
					switch (ch) {
						case '-': 
							state = ST_COMMENT_DASH_DASH;
							break;
					}

					break;

				case ST_COMMENT_DASH_DASH: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;
					}

					break;

				case ST_LT_A: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_A;
							break;

						case 'R': 
						case 'r': 
							state = ST_LT_AR;
							break;

						case 'P': 
						case 'p': 
							state = ST_LT_AP;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_A: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_A_QUOTE;
							break;

						case 'H': 
						case 'h': 
							state = ST_A_H;
							break;

						default: 
							break;
					}

					break;

				case ST_A_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_A;
							break;
					}

					break;

				case ST_A_H: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_A_HR;
							break;

						case '"': 
							state = ST_A_QUOTE;
							break;

						default: 
							state = ST_A;
							break;
					}

					break;

				case ST_A_HR: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_A_HRE;
							break;

						case '"': 
							state = ST_A_QUOTE;
							break;

						default: 
							state = ST_A;
							break;
					}

					break;

				case ST_A_HRE: 
					switch (ch) {
						case 'F': 
						case 'f': 
							state = ST_A_HREF;
							break;

						case '"': 
							state = ST_A_QUOTE;
							break;

						default: 
							state = ST_A;
							break;
					}

					break;

				case ST_A_HREF: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_A_HREF_EQUAL;
							break;

						case '"': 
							state = ST_A_QUOTE;
							break;

						default: 
							state = ST_A;
							break;
					}

					break;

				case ST_A_HREF_EQUAL: 
					gettingUrl = true;
					urlBuf.setLength(0);

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_AHREF_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_AHREF_NQ;
							break;
					}

					break;

				case ST_AHREF_Q: 
					switch (ch) {
						case '"': 
							callAHREF(urlBuf.toString());
							gettingUrl = false;
							state = ST_A;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_AHREF_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callAHREF(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_A);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_H: 
					switch (ch) {
						case '1': 
							state = ST_LT_H1;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_H1: 
					switch (ch) {
						case ' ': 
						case '\n': 
						case '\r': 
						case '\t': 
							break;

						case '>': 
							headBuf.setLength(0);
							gettingHead = true;
							state = ST_H1;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_I: 
					switch (ch) {
						case 'M': 
						case 'm': 
							state = ST_LT_IM;
							break;

						case 'N': 
						case 'n': 
							state = ST_LT_IN;
							break;

						case 'F': 
						case 'f': 
							state = ST_LT_IF;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_IM: 
					switch (ch) {
						case 'G': 
						case 'g': 
							state = ST_LT_IMG;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_IMG: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_IMG;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_IMG: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_IMG_QUOTE;
							break;

						case 'S': 
						case 's': 
							state = ST_IMG_S;
							break;
					}

					break;

				case ST_IMG_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_IMG;
							break;
					}

					break;

				case ST_IMG_S: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_IMG_SR;
							break;

						case '"': 
							state = ST_IMG_QUOTE;
							break;

						default: 
							state = ST_IMG;
							break;
					}


					break;

				case ST_IMG_SR: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_IMG_SRC;
							break;

						case '"': 
							state = ST_IMG_QUOTE;
							break;

						default: 
							state = ST_IMG;
							break;
					}

					break;

				case ST_IMG_SRC: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_IMG_SRC_EQUAL;
							break;

						case '"': 
							state = ST_IMG_QUOTE;
							break;

						default: 
							state = ST_IMG;
							break;
					}

					break;

				case ST_IMG_SRC_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_IMGSRC_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_IMGSRC_NQ;
							break;
					}

					break;

				case ST_IMGSRC_Q: 
					switch (ch) {
						case '"': 
							callIMGSRC(urlBuf.toString());
							gettingUrl = false;
							state = ST_IMG;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_IMGSRC_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callIMGSRC(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_IMG);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_F: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_LT_FR;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_FR: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_LT_FRA;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_FRA: 
					switch (ch) {
						case 'M': 
						case 'm': 
							state = ST_LT_FRAM;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_FRAM: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LT_FRAME;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_FRAME: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_FRAME;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_FRAME: 
					switch (ch) {

						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_FRAME_QUOTE;
							break;

						case 'S': 
						case 's': 
							state = ST_FRAME_S;
							break;
					}

					break;

				case ST_FRAME_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_FRAME;
							break;
					}

					break;

				case ST_FRAME_S: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_FRAME_SR;
							break;

						case '"': 
							state = ST_FRAME_QUOTE;
							break;

						default: 
							state = ST_FRAME;
							break;
					}

					break;

				case ST_FRAME_SR: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_FRAME_SRC;
							break;

						case '"': 
							state = ST_FRAME_QUOTE;
							break;

						default: 
							state = ST_FRAME;
							break;
					}

					break;

				case ST_FRAME_SRC: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_FRAME_SRC_EQUAL;
							break;

						case '"': 
							state = ST_FRAME_QUOTE;
							break;

						default: 
							state = ST_FRAME;
							break;
					}

					break;

				case ST_FRAME_SRC_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_FRAMESRC_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_FRAMESRC_NQ;
							break;
					}

					break;

				case ST_FRAMESRC_Q: 
					switch (ch) {
						case '"': 
							callFRAMESRC(urlBuf.toString());
							gettingUrl = false;
							state = ST_FRAME;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_FRAMESRC_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callFRAMESRC(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_FRAME);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_B: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_LT_BA;
							break;

						case 'O': 
						case 'o': 
							state = ST_LT_BO;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_T: 
					switch (ch) {
						case 'I': 
						case 'i': 
							state = ST_LT_TI;
							break;

						case 'A': 
						case 'a': 
							state = ST_LT_TA;
							break;

						case 'R': 
						case 'r': 
							state = ST_LT_TR;
							break;

						case 'H': 
						case 'h': 
							state = ST_LT_TH;
							break;

						case 'D': 
						case 'd': 
							state = ST_LT_TD;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TI: 
					switch (ch) {
						case 'T': 
						case 't': 
							state = ST_LT_TIT;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TIT: 
					switch (ch) {
						case 'L': 
						case 'l': 
							state = ST_LT_TITL;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TITL: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LT_TITLE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TITLE: 
					switch (ch) {
						case ' ': 
						case '\n': 
						case '\r': 
						case '\t': 
							break;

						case '>': 
							titleBuf.setLength(0);
							gettingTitle = true;
							state = ST_TITLE;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_BA: 
					switch (ch) {
						case 'S': 
						case 's': 
							state = ST_LT_BAS;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_BAS: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LT_BASE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_BASE: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_BASE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;

							break;
					}

					break;

				case ST_BASE: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_BASE_QUOTE;
							break;

						case 'H': 
						case 'h': 
							state = ST_BASE_H;
							break;
					}

					break;

				case ST_BASE_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_BASE;
							break;
					}

					break;

				case ST_BASE_H: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_BASE_HR;
							break;

						case '"': 
							state = ST_BASE_QUOTE;
							break;

						default: 
							state = ST_BASE;
							break;
					}

					break;

				case ST_BASE_HR: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_BASE_HRE;
							break;

						case '"': 
							state = ST_BASE_QUOTE;
							break;

						default: 
							state = ST_BASE;
							break;
					}

					break;

				case ST_BASE_HRE: 
					switch (ch) {
						case 'F': 
						case 'f': 
							state = ST_BASE_HREF;
							break;

						case '"': 
							state = ST_BASE_QUOTE;
							break;

						default: 
							state = ST_BASE;
							break;
					}

					break;

				case ST_BASE_HREF: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_BASE_HREF_EQUAL;
							break;

						case '"': 
							state = ST_BASE_QUOTE;
							break;

						default: 
							state = ST_BASE;
							break;
					}

					break;

				case ST_BASE_HREF_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_BASEHREF_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_BASEHREF_NQ;
							break;
					}

					break;

				case ST_BASEHREF_Q: 
					switch (ch) {

						case '"': 
							callBASEHREF(urlBuf.toString());
							gettingUrl = false;

							try {
								contextUrl = Util.plainUrl(contextUrl, urlBuf.toString());
							} catch (MalformedURLException e) {}

							state = ST_BASE;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_BASEHREF_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callBASEHREF(urlBuf.toString());
							gettingUrl = false;

							try {
								contextUrl = Util.plainUrl(contextUrl, urlBuf.toString());
							} catch (MalformedURLException e) {}

							state = (ch == '>' ? ST_GROUND : ST_BASE);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_AR: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LT_ARE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_ARE: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_LT_AREA;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_AREA: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_AREA;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_AREA: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_AREA_QUOTE;
							break;

						case 'H': 
						case 'h': 
							state = ST_AREA_H;
							break;
					}

					break;

				case ST_AREA_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_AREA;
							break;
					}

					break;

				case ST_AREA_H: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_AREA_HR;
							break;

						case '"': 
							state = ST_AREA_QUOTE;
							break;

						default: 
							state = ST_AREA;
							break;
					}

					break;

				case ST_AREA_HR: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_AREA_HRE;
							break;

						case '"': 
							state = ST_AREA_QUOTE;
							break;

						default: 
							state = ST_AREA;
							break;
					}

					break;

				case ST_AREA_HRE: 
					switch (ch) {
						case 'F': 
						case 'f': 
							state = ST_AREA_HREF;
							break;

						case '"': 
							state = ST_AREA_QUOTE;
							break;

						default: 
							state = ST_AREA;
							break;
					}

					break;

				case ST_AREA_HREF: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_AREA_HREF_EQUAL;
							break;

						case '"': 
							state = ST_AREA_QUOTE;
							break;

						default: 
							state = ST_AREA;
							break;
					}

					break;

				case ST_AREA_HREF_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_AREAHREF_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_AREAHREF_NQ;
							break;
					}

					break;

				case ST_AREAHREF_Q: 
					switch (ch) {
						case '"': 
							callAREAHREF(urlBuf.toString());
							gettingUrl = false;
							state = ST_AREA;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_AREAHREF_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callAREAHREF(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_AREA);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_L: 
					switch (ch) {
						case 'I': 
						case 'i': 
							state = ST_LT_LI;
							break;

						case 'A':	// LAYER 
						case 'a': 
							state = ST_LT_LA;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_LI: 
					switch (ch) {
						case 'N': 
						case 'n': 
							state = ST_LT_LIN;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_LIN: 
					switch (ch) {
						case 'K': 
						case 'k': 
							state = ST_LT_LINK;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_LINK: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_LINK;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LINK: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_LINK_QUOTE;
							break;

						case 'H': 
						case 'h': 
							state = ST_LINK_H;
							break;
					}

					break;

				case ST_LINK_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_LINK;
							break;
					}

					break;

				case ST_LINK_H: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_LINK_HR;
							break;

						case '"': 
							state = ST_LINK_QUOTE;
							break;

						default: 
							state = ST_LINK;
							break;
					}

					break;

				case ST_LINK_HR: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LINK_HRE;
							break;

						case '"': 
							state = ST_LINK_QUOTE;
							break;

						default: 
							state = ST_LINK;
							break;
					}

					break;

				case ST_LINK_HRE: 
					switch (ch) {
						case 'F': 
						case 'f': 
							state = ST_LINK_HREF;
							break;

						case '"': 
							state = ST_LINK_QUOTE;
							break;

						default: 
							state = ST_LINK;
							break;
					}

					break;

				case ST_LINK_HREF: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_LINK_HREF_EQUAL;
							break;

						case '"': 
							state = ST_LINK_QUOTE;
							break;

						default: 
							state = ST_LINK;
							break;
					}

					break;

				case ST_LINK_HREF_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_LINKHREF_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_LINKHREF_NQ;
							break;
					}

					break;

				case ST_LINKHREF_Q: 
					switch (ch) {
						case '"': 
							callLINKHREF(urlBuf.toString());
							gettingUrl = false;
							state = ST_LINK;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LINKHREF_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callLINKHREF(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_LINK);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_BO: 
					switch (ch) {
						case 'D': 
						case 'd': 
							state = ST_LT_BOD;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_BOD: 
					switch (ch) {
						case 'Y': 
						case 'y': 
							state = ST_LT_BODY;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_BODY: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_BODY;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_BODY: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						case 'B': 
						case 'b': 
							state = ST_BODY_B;
							break;
					}

					break;

				case ST_BODY_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_B: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_BODY_BA;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BA: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_BODY_BAC;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BAC: 
					switch (ch) {
						case 'K': 
						case 'k': 
							state = ST_BODY_BACK;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BACK: 
					switch (ch) {
						case 'G': 
						case 'g': 
							state = ST_BODY_BACKG;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BACKG: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_BODY_BACKGR;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BACKGR: 
					switch (ch) {
						case 'O': 
						case 'o': 
							state = ST_BODY_BACKGRO;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BACKGRO: 
					switch (ch) {
						case 'U': 
						case 'u': 
							state = ST_BODY_BACKGROU;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BACKGROU: 
					switch (ch) {
						case 'N': 
						case 'n': 
							state = ST_BODY_BACKGROUN;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BACKGROUN: 
					switch (ch) {
						case 'D': 
						case 'd': 
							state = ST_BODY_BACKGROUND;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BACKGROUND: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_BODY_BACKGROUND_EQUAL;
							break;

						case '"': 
							state = ST_BODY_QUOTE;
							break;

						default: 
							state = ST_BODY;
							break;
					}

					break;

				case ST_BODY_BACKGROUND_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_BODYBACKGROUND_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_BODYBACKGROUND_NQ;
							break;
					}

					break;

				case ST_BODYBACKGROUND_Q: 
					switch (ch) {
						case '"': 
							callBODYBACKGROUND(urlBuf.toString());
							gettingUrl = false;
							state = ST_BODY;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_BODYBACKGROUND_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callBODYBACKGROUND(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_BODY);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_IN: 
					switch (ch) {
						case 'P': 
						case 'p': 
							state = ST_LT_INP;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_INP: 
					switch (ch) {
						case 'U': 
						case 'u': 
							state = ST_LT_INPU;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_INPU: 
					switch (ch) {
						case 'T': 
						case 't': 
							state = ST_LT_INPUT;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_INPUT: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_INPUT;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_INPUT: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_INPUT_QUOTE;
							break;

						case 'S': 
						case 's': 
							state = ST_INPUT_S;
							break;
					}

					break;

				case ST_INPUT_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_INPUT;
							break;
					}

					break;

				case ST_INPUT_S: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_INPUT_SR;
							break;

						case '"': 
							state = ST_INPUT_QUOTE;
							break;

						default: 
							state = ST_INPUT;
							break;
					}

					break;

				case ST_INPUT_SR: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_INPUT_SRC;
							break;

						case '"': 
							state = ST_INPUT_QUOTE;
							break;

						default: 
							state = ST_INPUT;
							break;
					}

					break;

				case ST_INPUT_SRC: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_INPUT_SRC_EQUAL;
							break;

						case '"': 
							state = ST_INPUT_QUOTE;
							break;

						default: 
							state = ST_INPUT;
							break;
					}

					break;

				case ST_INPUT_SRC_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_INPUTSRC_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_INPUTSRC_NQ;
							break;
					}

					break;

				case ST_INPUTSRC_Q: 
					switch (ch) {
						case '"': 
							callIMGSRC(urlBuf.toString());
							gettingUrl = false;
							state = ST_INPUT;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_INPUTSRC_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callIMGSRC(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_INPUT);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_IF: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_LT_FR;	// goto state ST_LT_FRAME_SRC
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_S:	// goto <SCRIPT SRC=... >
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_LT_SC;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					
				case ST_LT_SC: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_LT_SCR;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_SCR: 
					switch (ch) {
						case 'I': 
						case 'i': 
							state = ST_LT_SCRI;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_SCRI: 
					switch (ch) {
						case 'P': 
						case 'p': 
							state = ST_LT_SCRIP;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_SCRIP: 
					switch (ch) {
						case 'T': 
						case 't': 
							state = ST_LT_SCRIPT;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_SCRIPT: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_SCRIPT;
							break;

						case '>': 
							gettingScript = true;
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_SCRIPT: 
					switch (ch) {
						case '>': 
							gettingScript = true;
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_SCRIPT_QUOTE;
							break;

						case 'S': 
						case 's': 
							state = ST_SCRIPT_S;
							break;
					}

					break;

				case ST_SCRIPT_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_SCRIPT;
							break;
					}

					break;

				case ST_SCRIPT_S: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_SCRIPT_SR;
							break;

						case '"': 
							state = ST_SCRIPT_QUOTE;
							break;

						default: 
							state = ST_SCRIPT;
							break;
					}

					break;

				case ST_SCRIPT_SR: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_SCRIPT_SRC;
							break;

						case '"': 
							state = ST_SCRIPT_QUOTE;
							break;

						default: 
							state = ST_SCRIPT;
							break;
					}

					break;

				case ST_SCRIPT_SRC: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_SCRIPT_SRC_EQUAL;
							break;

						case '"': 
							state = ST_SCRIPT_QUOTE;
							break;

						default: 
							state = ST_SCRIPT;
							break;
					}

					break;

				case ST_SCRIPT_SRC_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_SCRIPTSRC_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_SCRIPTSRC_NQ;
							break;
					}

					break;

				case ST_SCRIPTSRC_Q: 
					switch (ch) {
						case '"': 
							callSCRIPTSRC(urlBuf.toString());
							gettingUrl = false;
							state = ST_SCRIPT;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_SCRIPTSRC_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callSCRIPTSRC(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_SCRIPT);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;


				// <APPLET CODE=... >

				case ST_LT_AP:
					switch (ch) {
						case 'P': 
						case 'p': 
							state = ST_LT_APP;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					
				case ST_LT_APP:
					switch (ch) {
						case 'L': 
						case 'l': 
							state = ST_LT_APPL;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					
				case ST_LT_APPL:
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LT_APPLE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					
				case ST_LT_APPLE:
					switch (ch) {
						case 'T': 
						case 't': 
							state = ST_LT_APPLET;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					

				case ST_LT_APPLET: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_APPLET;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_APPLET: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						case 'C': 
						case 'c': 
							state = ST_APPLET_C;
							break;

						case 'A': 
						case 'a': 
							state = ST_APPLET_A;
							break;
							
						case 'D':	// <OBJECT DATA= >
						case 'd': 
							state = ST_OBJECT_D;
							break;
					}

					break;

				case ST_APPLET_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_C: 
					switch (ch) {
						case 'O': 
						case 'o': 
							state = ST_APPLET_CO;
							break;

						case 'L':	// <OBJECT CLASSID= >
						case 'l': 
							state = ST_OBJECT_CL;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_CO: 
					switch (ch) {
						case 'D': 
						case 'd': 
							state = ST_APPLET_COD;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_COD: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_APPLET_CODE;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_CODE: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case 'B': 
						case 'b': 
							state = ST_APPLET_CODEB;
							break;

						case '=': 
							state = ST_APPLET_CODE_EQUAL;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_CODE_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_APPLETCODE_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_APPLETCODE_NQ;
							break;
					}

					break;

				case ST_APPLETCODE_Q: 
					switch (ch) {
						case '"': 
							callAPPLETCODE(urlBuf.toString());
							gettingUrl = false;
							state = ST_APPLET;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_APPLETCODE_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callAPPLETCODE(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_APPLET);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_APPLET_CODEB: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_APPLET_CODEBA;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_CODEBA: 
					switch (ch) {
						case 'S': 
						case 's': 
							state = ST_APPLET_CODEBAS;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_CODEBAS: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_APPLET_CODEBASE;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_CODEBASE: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_APPLET_CODEBASE_EQUAL;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_CODEBASE_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_APPLETCODEBASE_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_APPLETCODEBASE_NQ;
							break;
					}

					break;

				case ST_APPLETCODEBASE_Q: 
					switch (ch) {
						case '"': 
							callAPPLETCODEBASE(urlBuf.toString());
							gettingUrl = false;
							state = ST_APPLET;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_APPLETCODEBASE_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callAPPLETCODEBASE(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_APPLET);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_APPLET_A: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_APPLET_AR;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_AR: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_APPLET_ARC;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_ARC: 
					switch (ch) {
						case 'H': 
						case 'h': 
							state = ST_APPLET_ARCH;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_ARCH: 
					switch (ch) {
						case 'I': 
						case 'i': 
							state = ST_APPLET_ARCHI;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_ARCHI: 
					switch (ch) {
						case 'V': 
						case 'v': 
							state = ST_APPLET_ARCHIV;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_ARCHIV: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_APPLET_ARCHIVE;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_ARCHIVE: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_APPLET_ARCHIVE_EQUAL;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_APPLET_ARCHIVE_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_APPLETARCHIVE_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_APPLETARCHIVE_NQ;
							break;
					}

					break;

				case ST_APPLETARCHIVE_Q: 
					switch (ch) {
						case '"': 
							callAPPLETARCHIVE(urlBuf.toString());
							gettingUrl = false;
							state = ST_APPLET;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_APPLETARCHIVE_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callAPPLETARCHIVE(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_APPLET);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;


				// <OBJECT ...>
				
				case ST_LT_O:
					switch (ch) {
						case 'B': 
						case 'b': 
							state = ST_LT_OB;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					
				case ST_LT_OB:
					switch (ch) {
						case 'J': 
						case 'j': 
							state = ST_LT_OBJ;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					
				case ST_LT_OBJ:
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LT_OBJE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					
				case ST_LT_OBJE:
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_LT_OBJEC;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					
				case ST_LT_OBJEC:
					switch (ch) {
						case 'T': 
						case 't': 
							state = ST_LT_OBJECT;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;
					

				case ST_LT_OBJECT: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_APPLET;	// go to APPLET
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_OBJECT_CL: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_OBJECT_CLA;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_CLA: 
					switch (ch) {
						case 'S': 
						case 's': 
							state = ST_OBJECT_CLAS;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_CLAS: 
					switch (ch) {
						case 'S': 
						case 's': 
							state = ST_OBJECT_CLASS;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_CLASS: 
					switch (ch) {
						case 'I': 
						case 'i': 
							state = ST_OBJECT_CLASSI;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_CLASSI: 
					switch (ch) {
						case 'D': 
						case 'd': 
							state = ST_OBJECT_CLASSID;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_CLASSID: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_APPLET_CODE_EQUAL;	// go to <APPLET CODE= >
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_D: 	// <OBJECT DATA= >
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_OBJECT_DA;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_DA: 
					switch (ch) {
						case 'T': 
						case 't': 
							state = ST_OBJECT_DAT;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_DAT: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_OBJECT_DATA;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_DATA: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_OBJECT_DATA_EQUAL;
							break;

						case '"': 
							state = ST_APPLET_QUOTE;
							break;

						default: 
							state = ST_APPLET;
							break;
					}

					break;

				case ST_OBJECT_DATA_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_OBJECTDATA_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_OBJECTDATA_NQ;
							break;
					}

					break;

				case ST_OBJECTDATA_Q: 
					switch (ch) {
						case '"': 
							callOBJECTDATA(urlBuf.toString());
							gettingUrl = false;
							state = ST_APPLET;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_OBJECTDATA_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callOBJECTDATA(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_APPLET);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;


				// TABLE BACKGROUND
				
				case ST_LT_TA: 
					switch (ch) {
						case 'B': 
						case 'b': 
							state = ST_LT_TAB;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TAB: 
					switch (ch) {
						case 'L': 
						case 'l': 
							state = ST_LT_TABL;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TABL: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LT_TABLE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TABLE: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_TABLE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_TABLE: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						case 'B': 
						case 'b': 
							state = ST_TABLE_B;
							break;
					}

					break;

				case ST_TABLE_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_B: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_TABLE_BA;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BA: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_TABLE_BAC;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BAC: 
					switch (ch) {
						case 'K': 
						case 'k': 
							state = ST_TABLE_BACK;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BACK: 
					switch (ch) {
						case 'G': 
						case 'g': 
							state = ST_TABLE_BACKG;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BACKG: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_TABLE_BACKGR;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BACKGR: 
					switch (ch) {
						case 'O': 
						case 'o': 
							state = ST_TABLE_BACKGRO;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BACKGRO: 
					switch (ch) {
						case 'U': 
						case 'u': 
							state = ST_TABLE_BACKGROU;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BACKGROU: 
					switch (ch) {
						case 'N': 
						case 'n': 
							state = ST_TABLE_BACKGROUN;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BACKGROUN: 
					switch (ch) {
						case 'D': 
						case 'd': 
							state = ST_TABLE_BACKGROUND;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BACKGROUND: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_TABLE_BACKGROUND_EQUAL;
							break;

						case '"': 
							state = ST_TABLE_QUOTE;
							break;

						default: 
							state = ST_TABLE;
							break;
					}

					break;

				case ST_TABLE_BACKGROUND_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_TABLEBACKGROUND_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_TABLEBACKGROUND_NQ;
							break;
					}

					break;

				case ST_TABLEBACKGROUND_Q: 
					switch (ch) {
						case '"': 
							callTABLEBACKGROUND(urlBuf.toString());
							gettingUrl = false;
							state = ST_TABLE;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_TABLEBACKGROUND_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callTABLEBACKGROUND(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_TABLE);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LT_TH: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_TABLE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TR: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_TABLE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_TD: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_TABLE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;


				// LAYER BACKGROUND
				
				case ST_LT_LA: 
					switch (ch) {
						case 'Y': 
						case 'y': 
							state = ST_LT_LAY;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_LAY: 
					switch (ch) {
						case 'E': 
						case 'e': 
							state = ST_LT_LAYE;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_LAYE: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_LT_LAYER;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LT_LAYER: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							state = ST_LAYER;
							break;

						case '>': 
							state = ST_GROUND;
							break;

						default: 
							state = ST_LTJUNK;
							break;
					}

					break;

				case ST_LAYER: 
					switch (ch) {
						case '>': 
							state = ST_GROUND;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						case 'B': 
						case 'b': 
							state = ST_LAYER_B;
							break;

						case 'S': 
						case 's': 
							state = ST_LAYER_S;
							break;
					}

					break;

				case ST_LAYER_QUOTE: 
					switch (ch) {
						case '"': 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_B: 
					switch (ch) {
						case 'A': 
						case 'a': 
							state = ST_LAYER_BA;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BA: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_LAYER_BAC;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BAC: 
					switch (ch) {
						case 'K': 
						case 'k': 
							state = ST_LAYER_BACK;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BACK: 
					switch (ch) {
						case 'G': 
						case 'g': 
							state = ST_LAYER_BACKG;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BACKG: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_LAYER_BACKGR;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BACKGR: 
					switch (ch) {
						case 'O': 
						case 'o': 
							state = ST_LAYER_BACKGRO;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BACKGRO: 
					switch (ch) {
						case 'U': 
						case 'u': 
							state = ST_LAYER_BACKGROU;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BACKGROU: 
					switch (ch) {
						case 'N': 
						case 'n': 
							state = ST_LAYER_BACKGROUN;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BACKGROUN: 
					switch (ch) {
						case 'D': 
						case 'd': 
							state = ST_LAYER_BACKGROUND;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BACKGROUND: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_LAYER_BACKGROUND_EQUAL;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_BACKGROUND_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_LAYERBACKGROUND_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_LAYERBACKGROUND_NQ;
							break;
					}

					break;

				case ST_LAYERBACKGROUND_Q: 
					switch (ch) {
						case '"': 
							callLAYERBACKGROUND(urlBuf.toString());
							gettingUrl = false;
							state = ST_LAYER;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LAYERBACKGROUND_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callLAYERBACKGROUND(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_LAYER);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LAYER_S: 
					switch (ch) {
						case 'R': 
						case 'r': 
							state = ST_LAYER_SR;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_SR: 
					switch (ch) {
						case 'C': 
						case 'c': 
							state = ST_LAYER_SRC;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_SRC: 
					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '=': 
							state = ST_LAYER_SRC_EQUAL;
							break;

						case '"': 
							state = ST_LAYER_QUOTE;
							break;

						default: 
							state = ST_LAYER;
							break;
					}

					break;

				case ST_LAYER_SRC_EQUAL: 
					urlBuf.setLength(0);
					gettingUrl = true;

					switch (ch) {
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							break;

						case '"': 
							state = ST_LAYERSRC_Q;
							break;

						default: 
							urlBuf.append(ch);
							state = ST_LAYERSRC_NQ;
							break;
					}

					break;

				case ST_LAYERSRC_Q: 
					switch (ch) {
						case '"': 
							callLAYERSRC(urlBuf.toString());
							gettingUrl = false;
							state = ST_LAYER;
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

				case ST_LAYERSRC_NQ: 
					switch (ch) {
						case '>': 
						case ' ': 
						case '\t': 
						case '\n': 
						case '\r': 
							callLAYERSRC(urlBuf.toString());
							gettingUrl = false;
							state = (ch == '>' ? ST_GROUND : ST_LAYER);
							break;

						default: 
							urlBuf.append(ch);
							break;
					}

					break;

			}	// end switch

		}	// end for

		return interpDelta;
	}





	private void callAHREF(String urlStr) {
		if (gettingScript == false)
			observer.gotAHREF(urlStr, contextUrl, clientData);
	}

	private void callIMGSRC(String urlStr) {
		if (gettingScript == false)
			observer.gotIMGSRC(urlStr, contextUrl, clientData);
	}

	private void callFRAMESRC(String urlStr) {
		if (gettingScript == false)
			observer.gotFRAMESRC(urlStr, contextUrl, clientData);
	}

	private void callBASEHREF(String urlStr) {
		if (gettingScript == false)
			observer.gotBASEHREF(urlStr, contextUrl, clientData);
	}

	private void callAREAHREF(String urlStr) {
		if (gettingScript == false)
			observer.gotAREAHREF(urlStr, contextUrl, clientData);
	}

	private void callLINKHREF(String urlStr) {
		if (gettingScript == false)
			observer.gotLINKHREF(urlStr, contextUrl, clientData);
	}

	private void callBODYBACKGROUND(String urlStr) {
		if (gettingScript == false)
			observer.gotBODYBACKGROUND(urlStr, contextUrl, clientData);
	}

	private void callTITLE(String titleText) {
		if (gettingScript == false)
			observer.gotTITLE(titleText, contextUrl, clientData);
	}

	private void callHEAD(String headText) {
		if (gettingScript == false)
			observer.gotHEAD(headText, contextUrl, clientData);
	}

	private void callSCRIPTSRC(String urlStr) {
		if (gettingScript == false)
			observer.gotSCRIPTSRC(urlStr, contextUrl, clientData);
	}

	private void callTABLEBACKGROUND(String urlStr) {
		if (gettingScript == false)
			observer.gotTABLEBACKGROUND(urlStr, contextUrl, clientData);
	}
	
	private void callAPPLETCODE(String urlStr)	{
		if (gettingScript == false)
			observer.gotAPPLETCODE(urlStr, contextUrl, clientData);
	}
	
	private void callAPPLETCODEBASE(String urlStr)	{
		if (gettingScript == false)
			observer.gotAPPLETCODEBASE(urlStr, contextUrl, clientData);
	}
	
	private void callAPPLETARCHIVE(String urlStr)	{
		if (gettingScript == false)
			observer.gotAPPLETARCHIVE(urlStr, contextUrl, clientData);
	}
	
	private void callOBJECTDATA(String urlStr)	{
		if (gettingScript == false)
			observer.gotOBJECTDATA(urlStr, contextUrl, clientData);
	}
	
	private void callLAYERBACKGROUND(String urlStr)	{
		if (gettingScript == false)
			observer.gotLAYERBACKGROUND(urlStr, contextUrl, clientData);
	}
	
	private void callLAYERSRC(String urlStr)	{
		if (gettingScript == false)
			observer.gotLAYERSRC(urlStr, contextUrl, clientData);
	}

}
