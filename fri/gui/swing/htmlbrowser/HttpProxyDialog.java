package fri.gui.swing.htmlbrowser;

import javax.swing.JFrame;
import fri.util.props.*;
import fri.gui.swing.propdialog.*;

/**
	Ein Dialog zur Parametrisierung einer Proxy-Verbindung zum Internet.
*/

public class HttpProxyDialog extends PropEditDialog
{
	public static final String [] proxySettings = {
		"proxySet",	// true|false
		"proxyHost",
		"proxyPort",
		"http.proxyHost",
		"http.proxyPort",
		"http.maxConnections",
		"http.keepAlive",
		"http.nonProxyHosts",
		"ftpProxySet",	// true|false
		"ftpProxyHost",
		"ftpProxyPort",
		"socksProxyHost",
		"socksProxyPort",
	};


	/**
		Show a dialog to pass proxy and port to system properties "http.proxyHost"
		and "http.proxyPort" (and others).
	*/
	public HttpProxyDialog(JFrame frame)	{
		super(
				frame,
				PropertiesCopy.copyOverwrite(
						System.getProperties(),
						ClassProperties.getProperties(ProxySettings.class),
						proxySettings),
				"Proxy Settings");
		show();
	}

	/** Overridden to save class properties to file. */
	public void storeToProperties()	{
		super.storeToProperties();
		PropertiesCopy.copyWhenNotEmptyDeleteOptionalOnEmpty(
				ClassProperties.getProperties(ProxySettings.class),
				System.getProperties(),
				proxySettings,
				true);
		ClassProperties.store(ProxySettings.class);
	}

	/** Load stored HTTP properties and put them to System-Properties. Call this in main routine! */
	public static void load()	{
		PropertiesCopy.copyWhenNotEmptyDeleteOptionalOnEmpty(
				ClassProperties.getProperties(ProxySettings.class),
				System.getProperties(),
				proxySettings,
				false);
	}


	/* test main
	public static void main(String [] args)	{
		JFrame frame = new JFrame();
		HttpProxyDialog.load();
		new HttpProxyDialog(frame);
		System.getProperties().list(System.err);
	}*/

}


/** placeholder for properties */
class ProxySettings
{
}