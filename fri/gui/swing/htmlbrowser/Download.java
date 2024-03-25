package fri.gui.swing.htmlbrowser;

import javax.swing.*;
import java.net.*;
import fri.util.html.*;
import fri.gui.swing.progressdialog.*;

/**
	Wrapper fuer den HTML-Spider, der ganze Baeume herunterlaedt.
	Ein Cancel-Dialog laesst den Vorgang abbrechen, der als Thread laeuft.
*/

public class Download
{
	private String [] urls;
	private String target;
	private long size = 0L;
	private Spider spider;
	private CancelProgressDialog observer;
	private JFrame parent;


	/**
		Download einer oder mehrer URLs, die HTML sind.
		Die Links werden nicht verfolgt, aber Links aufeinander
		zu relativen konvertiert.
		Enthaltene Links auf Nicht-HTML Files und Images werden auch downgeloadet.
		Optional koennen bestimmte Mime-Typen ausgeschalten werden.
		Die Gesamtlaenge aus allen Dateien muss angegeben werden.
	*/
	public Download(JFrame parent, String [] urls, String target, long size, String [] notMimeTypes)	{
		this(parent, urls, target, size, notMimeTypes, false);
	}

	/**
		Download einer oder mehrer URLs, die HTML sind.
		Alle Links, die auf Dateien unterhalb der Wurzel weisen
		oder auf Dateien innerhalb der anderen Wurzeln,
		werden verfolgt und zu relativen konvertiert.
		Es wird die volle Verzeichnis-Struktur der Site angelegt,
		die Wurzel ist "www.x.y", wenn die Site "http://www.x.y/..." ist.
		Enthaltene Links auf Nicht-HTML Files und Images werden auch downgeloadet.
		Optional koennen bestimmte Mime-Typen ausgeschalten werden.
	*/
	public Download(JFrame parent, String [] urls, String target, String [] notMimeTypes)	{
		this(parent, urls, target, 0L, notMimeTypes, true);
	}
	
		
	// this is private as followLinks == true and size != 0L is not legal
	public Download(
		JFrame parent,
		String [] urls,
		String target,
		long size,
		String [] notMimeTypes,
		boolean followLinks)
	{
		this.parent = parent;
		this.urls = urls;
		this.target = target;
		this.size = size;

		spider = new Spider();
		spider.setFollowLinks(followLinks);
		spider.setNotMimeTypes(notMimeTypes);
	}
	


	/**
		Set maximum of downloaded files.
	*/
	public void setTodoLimit(int todoLimit)	{
		spider.setTodoLimit(todoLimit);
	}

	/**
		Set maximum Hyperlink depth of documents to follow.
	*/
	public void setDepth(int depth)	{
		spider.setDepth(depth);
	}
	
	/**
		Convert all references to relative URLs when true is passed (which is default).
	*/
	public void setConvertToRelative(boolean convertToRelative)	{
		spider.setConvertToRelative(convertToRelative);
	}
	
	/**
		Convert all references to relative URLs when true is passed (which is default).
	*/
	public void setOnlyWithinSite(boolean onlyWithinSite)	{
		spider.setOnlyWithinSite(onlyWithinSite);
	}

	/**
		Convert all references to relative URLs when true is passed (which is default).
	*/
	public void setBelowDocument(boolean belowDocument)	{
		spider.setBelowDocument(belowDocument);
	}



	/**
		Start the download with a modeless progress/cancel dialog,
		showing progress only if size was set bigger than zero.
	*/	
	public void startDownload()	{		
		try	{
			for (int i = 0; i < urls.length; i++)	{
				spider.addUrl(urls[i]);
			}
		}
		catch (MalformedURLException e)	{
			e.printStackTrace();
		}

		observer = new CancelProgressDialog(
				parent,
				"Download "+urls[0]+(urls.length > 1 ? " ..." : ""),
				size);
		
		Runnable download = new Runnable()	{
			public void run()	{
				while (observer.canceled() == false && spider.hasMoreElements())	{
					Spider.Item item = (Spider.Item)spider.nextElement();
					if (item != null)	{
						observer.setNote(item.thisUrlStr);
						item.toFile(target, size > 0L ? observer : null);
					}
				}
				observer.endDialog();
				spider.release();
			}
		};
		observer.start(download);
	}


	/**
		Start the download with a modeless progress/cancel dialog,
		showing progress only if size was set bigger than zero.
	*/	
	public void stopDownload()	{		
		observer.setCanceled();
	}

}