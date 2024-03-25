package fri.gui.swing.sound;

import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.event.*;

import fri.gui.CursorUtil;
import fri.gui.swing.combo.history.*;
import fri.gui.swing.dnd.*;
import fri.gui.swing.filechooser.*;

/**
	A minimal Sound player GUI with start/stop button and status label.
	<p>
	Needs following libraries:<br>
	jars/jl020.jar jars/jogg-0.0.5.jar jars/jorbis-0.0.12.jar jars/mp3sp.1.4.jar jars/vorbisspi0.6.jar
	<p>
	Supports following extensions: .m3u .wsz .snd .aifc .aif .wav .au .mp1 .mp2 .mp3 .ogg
*/

public class SoundPlayerPanel extends JPanel implements
	ActionListener,
	ChangeListener,
	BasicPlayerListener
{
	private static String extensions = ".m3u .wsz .snd .aifc .aif .wav .au .mp1 .mp2 .mp3 .ogg";
	
	private BasicPlayer player;
	private SoundURLCombo urlInput;
	private JLabel status;
	private JButton startStop, pauseResume, chooseFile;
	private JSlider gain, panorama;
	private JProgressBar progress;
	private String waitingToPlay;


	/** Open a player. */
	public SoundPlayerPanel()	{
		super(new BorderLayout());
		
		urlInput = new SoundURLCombo();
		urlInput.setToolTipText("Enter Filename Or URL Of Sound File");
		chooseFile = new JButton("Choose File");
		chooseFile.setToolTipText("Choose A Sound File");
		status = new JLabel(" ");
		status.setToolTipText("Drag To Here To Start Playing A Sound File");
		startStop = new JButton("Start");
		pauseResume = new JButton("Pause");
		pauseResume.setEnabled(false);
		progress = new JProgressBar();
		gain = new JSlider(JSlider.HORIZONTAL, 1, 100, 80);
		gain.setToolTipText("Gain Control");
		gain.setBorder(BorderFactory.createTitledBorder("Volume"));
		panorama = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
		panorama.setBorder(BorderFactory.createTitledBorder("Balance"));
		panorama.setToolTipText("Panorama Control");

		Container c = this;
		JPanel p0 = new JPanel();
		p0.setLayout(new BoxLayout(p0, BoxLayout.X_AXIS));
		p0.add(urlInput);
		p0.add(chooseFile);
		c.add(p0, BorderLayout.NORTH);
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
		p1.add(gain);
		p1.add(panorama);
		p1.add(status);
		p1.add(progress);
		c.add(p1, BorderLayout.CENTER);
		JPanel p2 = new JPanel();
		p2.add(startStop);
		p2.add(pauseResume);
		c.add(p2, BorderLayout.SOUTH);
		
		urlInput.addActionListener(this);
		chooseFile.addActionListener(this);
		startStop.addActionListener(this);
		pauseResume.addActionListener(this);
		gain.addChangeListener(this);
		panorama.addChangeListener(this);

		new SoundDndPerformer(this);
		new SoundDndPerformer(urlInput.getTextEditor());
		new SoundDndPerformer(status);
		new SoundDndPerformer(startStop);
		new SoundDndPerformer(p1);
	}

	/** Start the passed audiofile in this player. Breaks the currently playing. */
	public void start(String audioFile)	{
		if (player != null)	{
			synchronized(player)	{
				if (stop() == false)	{	// close current player
					waitingToPlay = audioFile;
					return;	// thread will start after finishing
				}
			}
		}
		
		waitingToPlay = null;
		
		urlInput.setText(audioFile);

		URL url = null;
		String urlString = audioFile;
		
		CursorUtil.setWaitCursor(this);
		
		if (audioFile.startsWith("file:/") == false && audioFile.startsWith("http://") == false && audioFile.startsWith("ftp://") == false)	{
			String filePart = new File(audioFile).getAbsolutePath().replace(File.separatorChar, '/');
			if (filePart.startsWith("/") == false)
				filePart = "/"+filePart;
				
			urlString = "file://localhost"+filePart;
			System.err.println("Made URL from file: "+urlString);
		}

		try	{
			url = new URL(urlString);
		}
		catch (MalformedURLException e2)	{
			status.setText(e2.getMessage());
		}

		if (url == null)	{
			CursorUtil.resetWaitCursor(this);
			return;
		}
			
		try	{
			status.setForeground(Color.red);
			status.setText("Loading "+audioFile+" ...");

			if (player == null)	{
				player = new BasicPlayer(this);
			}
			
			player.setDataSource(url);
			player.startPlayback();
			
			progress.setMaximum((int)player.getTotalLengthInSeconds());
			if (progress.getMaximum() <= 0)	{
				progress.setEnabled(false);
			}
			else	{
				progress.setEnabled(true);
				progress.setMinimum(0);
				progress.setValue(0);
			}
			
			if (player.hasGainControl())	{
				player.setGain((double)gain.getValue() / (double)100);
			}
			else	{
				gain.setEnabled(false);
			}
			
			if (player.hasPanControl())	{
				player.setPan((float)panorama.getValue() / (float)100);
			}
			else	{
				panorama.setEnabled(false);
			}

			startStop.setText("Stop");
			pauseResume.setEnabled(true);
		}
		catch (Exception e)	{
			status.setText(e.toString());
			e.printStackTrace();
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}


	private boolean stop()	{	
		if (player != null)	{
			if (player.getStatus() != BasicPlayer.READY)	{
				player.stopPlayback();
				return false;	// thread still running
			}
		}
		return true;
	}
	

	/** Responding to start/stop button and textfield ENTER. */
	public void actionPerformed(ActionEvent e)	{
		boolean running = (player != null && player.getStatus() != BasicPlayer.READY);
		
		if ((e.getSource() == startStop || e.getSource() == urlInput) && urlInput.getText().length() > 0)	{
			stop();	// maybe currently running

			if (e.getSource() == urlInput || running == false)	{	// new text input or stop button
				start(urlInput.getText());
			}
		}
		else
		if (e.getSource() == pauseResume && running)	{
			if (player.getStatus() == BasicPlayer.PAUSED)	{
				player.resumePlayback();
				pauseResume.setText("Pause");
			}
			else	{
				player.pausePlayback();
				pauseResume.setText("Resume");
			}
		}
		else
		if (e.getSource() == chooseFile)	{
			try	{
				File [] f = DefaultFileChooser.openDialog(this, getClass(), new String [] { extensions });
				urlInput.removeActionListener(this);
				for (int i = 0; f != null && i < f.length; i++)	{
					urlInput.setText(f[i].getPath());
				}
				urlInput.addActionListener(this);
			}
			catch (CancelException ex)	{
			}
		}
	}


	/** Implements ChangeLister to respond to user setting volume and panorama control. */
	public void stateChanged(ChangeEvent e)	{
		if (player == null)
			return;
			
		JSlider src = (JSlider)e.getSource();
		double value = (double)src.getValue();
		if (src == gain)	{
			player.setGain((double)(value / (double)100));
		}
		else
		if (src == panorama)	{
			player.setPan((float)(value / (double)100));
		}
	}


	/** Interface BasicPlayerListener: render progress. */
	public void updateCursor(final int cursor, int total)	{
		if (progress.getMaximum() <= 0)
			return;
			
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				progress.setValue(cursor);
			}
		});
	}

	/** Interface BasicPlayerListener: render player state. */
	public void updateMediaState(final String state)	{
		EventQueue.invokeLater(new Runnable()	{
			public void run()	{
				status.setText(state);
				
				if (state.equals(BasicPlayer.READY_STRING))	{
					status.setForeground(Color.green);
					startStop.setText("Start");
					pauseResume.setText("Pause");
					pauseResume.setEnabled(false);
					progress.setValue(0);
					
					if (waitingToPlay != null)	{
						start(waitingToPlay);
					}
				}
			}
		});
	}

	public void updateMediaData(byte[] data)	{
	}



	/** Stop the player when closing. Call super. */
	public void close()	{
		stop();
		urlInput.save();
	}



	private class SoundDndPerformer implements
		DndPerformer
	{
		public SoundDndPerformer(Component component)	{
			new DndListener(SoundDndPerformer.this, component);
		}
	
		// interface DndPerformer
	
		public Transferable sendTransferable()	{
			return null;
		}
	
		public boolean receiveTransferable(Object data, int action, Point p)	{
			List fileList = (List)data;
			Iterator iterator = fileList.iterator();
			
			while (iterator != null && iterator.hasNext()) {
				File file = (File)iterator.next();
				
				if (file.isFile())	{
					start(file.getAbsolutePath());
					return false;
				}
			}
	
			return false;
		}
	
		public DataFlavor supportsDataFlavor(int action, Point p, DataFlavor[] flavors)	{
			return DataFlavor.javaFileListFlavor;
		}
	
		public void dataMoved()	{}	
		public void dataCopied()	{}
		public void actionCanceled()	{}
	
		public boolean dragOver(Point p)	{
			return true;
		}
		public void startAutoscrolling()	{}
		public void stopAutoscrolling()	{}
	}

}



class SoundURLCombo extends HistCombo implements
	TypedHistoryHolder
{
	private static Vector globalHist;
	private static File globalFile = null;

	public SoundURLCombo()	{
		super();
		manageTypedHistory(this, new File(HistConfig.dir()+"SoundURLCombo.list"));
	}

	// interface TypedHistoryHolder
	public void setTypedHistoryData(Vector v, File f)	{
		globalHist = v;
		globalFile = f;
	}		
	public Vector getTypedHistory()	{
		return globalHist;
	}
	public File getHistoryFile()	{
		return globalFile;
	}

}