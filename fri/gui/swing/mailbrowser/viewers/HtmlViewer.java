package fri.gui.swing.mailbrowser.viewers;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import fri.util.browser.BrowserLaunch;
import fri.util.error.Err;

public class HtmlViewer extends RtfViewer
{
	protected void configureTextArea(final JEditorPane textArea)	{
		textArea.setContentType("text/html");
		
		textArea.addHyperlinkListener(new HyperlinkListener()	{
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)	{
					try	{
						BrowserLaunch.openUrl(e.getURL().toExternalForm());
					}
					catch (Exception ex)	{
						Err.error(ex);
					}
				}
				else
				if (e.getEventType() == HyperlinkEvent.EventType.ENTERED)	{
					textArea.setToolTipText(e.getURL().toExternalForm());			
				}
				else
				if (e.getEventType() == HyperlinkEvent.EventType.EXITED)	{
					textArea.setToolTipText(null);			
				}
			}
		});
	}

}