package fri.gui.swing.filebrowser;

/**
	Matches a File time against a given time condition.
*/

public class SearchTimePattern implements SearchPattern
{
	private long time;
	private boolean younger;
	
	public SearchTimePattern(int time, String youngerOrOlder, String dim)	{
		this.time = System.currentTimeMillis() -
			(long)time *
				(dim.equals("Days") ?     86400000L :
				 dim.equals("Weeks") ?   604800000L :
				 dim.equals("Months") ? 2628000000L :
				 dim.equals("Years") ? 31536000000L :
				 dim.equals("Hours") ?     3600000L :
				 dim.equals("Minutes") ?     60000L : 1);
		this.younger = youngerOrOlder.equals(SearchFrame.YOUNGER);
	}
	
	public boolean match(SearchFile f)	{
		return younger ? f.getTime() >= time : f.getTime() <= time;
	}
}