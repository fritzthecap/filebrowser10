package fri.gui.swing.sound;

public interface BasicPlayerListener
{
	public void updateCursor(int cursor,int total);
	public void updateMediaData(byte[] data);
	public void updateMediaState(String state);
}
