package fri.gui.swing.mailbrowser.viewers;

import java.io.*;
import javax.activation.*;
import fri.util.activation.StreamToTempFile;
import fri.gui.swing.sound.SoundPlayerPanel;

public class SoundViewer extends SoundPlayerPanel implements
	CommandObject
{
	/** Implementing CommandObject: play the sound. */
	public void setCommandContext(String verb, DataHandler dh)
		throws IOException
	{
		File tmpFile = StreamToTempFile.create(dh.getInputStream(), dh.getName(), dh.getContentType());
		super.start(tmpFile.getAbsolutePath());
	}

}