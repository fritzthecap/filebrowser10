package fri.gui.swing.filebrowser;

/**
	Matches a File size against a given size condition.
*/

public class SearchSizePattern implements SearchPattern
{
	private long size;
	private boolean bigger;
	
	public SearchSizePattern(int size, String biggerOrSmaller, String dim)	{
		this.size = (long)size *
				(long)
				(dim.equals("KB") ? 1024 :
				 dim.equals("MB") ? 1048576 :
				 dim.equals("GB") ? 1073741824 :
				 1);
		this.bigger = biggerOrSmaller.equals(SearchFrame.BIGGER);
	}
	
	public boolean match(SearchFile f)	{
		return bigger ? f.getSize() >= size : f.getSize() <= size;
	}
}