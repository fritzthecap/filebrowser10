package fri.gui.swing.sound;

import java.awt.*;
import fri.gui.swing.application.GuiApplication;

/**
	A minimal Sound player GUI with start/stop button and status label.
	<p>
	Needs following libraries:<br>
	jars/jl020.jar jars/jogg-0.0.5.jar jars/jorbis-0.0.12.jar jars/mp3sp.1.4.jar jars/vorbisspi0.6.jar
	<p>
	Supports following extensions: .m3u .wsz .snd .aifc .aif .wav .au .mp1 .mp2 .mp3 .ogg
*/

public class SoundPlayerFrame extends GuiApplication
{
	private static SoundPlayerFrame singleton;
	private SoundPlayerPanel panel;


	/** Returns the one and only sound player. */
	public static SoundPlayerFrame singleton()	{
		if (singleton == null)
			singleton = new SoundPlayerFrame();
		singleton.setVisible(true);
		return singleton;
	}

	/** Open a player. */
	private SoundPlayerFrame()	{
		super("Sound Player");
		
		panel = new SoundPlayerPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		
		init();
	}

	public void start(String audioFile)	{
		panel.start(audioFile);
	}


	/** Stop the player when closing. Call super. */
	public boolean close()	{
		panel.close();
		return super.close();
	}




	/** Opens a Sound player GUI for first argument. */
	public static void main(String [] args)	{
		SoundPlayerFrame f = new SoundPlayerFrame();

		if (args.length <= 0)
			System.err.println("SYNTAX: java "+SoundPlayerFrame.class.getName()+" file|url [file|url ...]");
		else
			f.start(args[0]);
	}

}