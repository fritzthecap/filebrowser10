package fri.gui.swing.sound;

import java.io.*;
import java.util.*;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Control;

public class BasicPlayer implements Runnable
{
	public static final int PAUSED = 1;
	public static final int PLAYING = 0;
	public static final int STOPPED = 2;
	public static final int READY = 3;
	public static final String READY_STRING = "Ready";
	private static final int EXTERNAL_BUFFER_SIZE = 4000 * 4;
	private Thread playerThread;
	private Object dataSource;
	private AudioInputStream audioInputStream;
	private AudioFileFormat audioFileFormat;
	private SourceDataLine sourceDataLine;
	private FloatControl gainControl;
	private FloatControl panoramaControl;
	private int playerStatus;
	private	long doSeek = -1;
	private File file;
	private BasicPlayerListener playerListener;

	/**
	 * Constructs a Basic Player.
	 */
	public BasicPlayer()
	{
		setStatus(READY);
	}

	/**
	 * Constructs a Basic Player with a BasicPlayerListener.
	 */
	public BasicPlayer(BasicPlayerListener bpl)
	{
		this();
		playerListener = bpl;
	}

	/**
	 * Returns BasicPlayer status.
	 */
	public synchronized int getStatus()
	{
		return 	playerStatus;
	}

	/**
	 * Sets BasicPlayer status.
	 */
	private synchronized void setStatus(int status)
	{
		this.playerStatus = status;

		if (playerListener != null)
			playerListener.updateMediaState(status == READY ? READY_STRING : status == PLAYING ? "Playing" : status == PAUSED ? "Paused" : status == STOPPED ? "Stopped" : "???");
	}

	/**
	 * Sets the data source as a file.
	 */
	public void setDataSource(File file) throws UnsupportedAudioFileException, LineUnavailableException, IOException
	{
		if (file != null)
		{
			dataSource = file;
			initAudioInputStream();
		}
	}


	/**
	 * Sets the data source as an url.
	 */
	public void setDataSource(URL url) throws UnsupportedAudioFileException, LineUnavailableException, IOException
	{
		if (url != null)
		{
			dataSource = url;
			initAudioInputStream();
		}
	}


	/**
	 * Inits Audio ressources from the data source.<br>
	 * - AudioInputStream <br>
	 * - AudioFileFormat
	 */
	private void initAudioInputStream() throws UnsupportedAudioFileException, LineUnavailableException, IOException
	{
		if (dataSource instanceof URL)
		{
			initAudioInputStream((URL) dataSource);
		}
		else if (dataSource instanceof File)
		{
			initAudioInputStream((File) dataSource);
		}
	}

	/**
	 * Inits Audio ressources from file.
	 */
	private void initAudioInputStream(File file) throws	UnsupportedAudioFileException, IOException
	{
		this.file = file;
		audioInputStream = AudioSystem.getAudioInputStream(file);
		audioFileFormat = AudioSystem.getAudioFileFormat(file);
	}

	/**
	 * Inits Audio ressources from URL.
	 */
	private void initAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException
	{
		audioInputStream = AudioSystem.getAudioInputStream(url);
		audioFileFormat = AudioSystem.getAudioFileFormat(url);
	}

	/**
	 * Inits Audio ressources from AudioSystem.<br>
	 * DateSource must be present.
	 */
	protected void initLine() throws LineUnavailableException
	{
		if (sourceDataLine == null)
		{
			createLine();
			trace(1,getClass().getName(), "Create Line OK ");
			openLine();
		}
		else
		{
			AudioFormat	lineAudioFormat = sourceDataLine.getFormat();
			AudioFormat	audioInputStreamFormat = audioInputStream == null ? null : audioInputStream.getFormat();
			if (!lineAudioFormat.equals(audioInputStreamFormat))
			{
				sourceDataLine.close();
				openLine();
			}
		}
	}

	/**
	 * Inits a DateLine.<br>
	 *
	 * We check if the line supports Volume and Pan controls.
	 *
	 * From the AudioInputStream, i.e. from the sound file, we
	 * fetch information about the format of the audio data. These
	 * information include the sampling frequency, the number of
	 * channels and the size of the samples. There information
	 * are needed to ask JavaSound for a suitable output line
	 * for this audio file.
	 * Furthermore, we have to give JavaSound a hint about how
	 * big the internal buffer for the line should be. Here,
	 * we say AudioSystem.NOT_SPECIFIED, signaling that we don't
	 * care about the exact size. JavaSound will use some default
	 * value for the buffer size.
	 */
	private void createLine() throws LineUnavailableException
	{
		if (sourceDataLine == null)
		{
			AudioFormat	sourceFormat = audioInputStream.getFormat();
			trace(1,getClass().getName(), "Source format : ", sourceFormat.toString());
			AudioFormat	targetFormat = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
														sourceFormat.getSampleRate(),
														16,
														sourceFormat.getChannels(),
														sourceFormat.getChannels() * 2,
														sourceFormat.getSampleRate(),
														false);

			trace(1,getClass().getName(), "Target format: " + targetFormat);
			audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);	// TODO: IllegalArgumentException (WINDOWS)
			AudioFormat audioFormat = audioInputStream.getFormat();
			trace(1,getClass().getName(), "Create Line : ", audioFormat.toString());
			DataLine.Info	info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);

			/*-- Display supported controls --*/
			Control[] c = sourceDataLine.getControls();
			for (int p=0;p<c.length;p++)
			{
				trace(2,getClass().getName(), "Controls : "+c[p].toString());
			}
			/*-- Is Gain Control supported ? --*/
			if (sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN))
			{
				gainControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
				trace(1,getClass().getName(), "Master Gain Control : ["+gainControl.getMinimum()+","+gainControl.getMaximum()+"]",""+gainControl.getPrecision());
			}

			/*-- Is Pan control supported ? --*/
			if (sourceDataLine.isControlSupported(FloatControl.Type.PAN))
			{
				panoramaControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.PAN);
				trace(1,getClass().getName(), "Pan Control : ["+ panoramaControl.getMinimum()+","+panoramaControl.getMaximum()+"]",""+panoramaControl.getPrecision());
			}
		}
	}


	/**
	 * Opens the line.
	 */
	private void openLine() throws LineUnavailableException
	{
		if (sourceDataLine != null)
		{
			AudioFormat	audioFormat = audioInputStream.getFormat();
			trace(1,getClass().getName(), "AudioFormat : "+audioFormat);
			sourceDataLine.open(audioFormat, sourceDataLine.getBufferSize());
		}
	}

	/**
	 * Stops the playback.<br>
	 *
	 * Player Status = STOPPED.<br>
	 * Thread should free Audio ressources.
	 */
	public synchronized void stopPlayback()
	{
		if (playerStatus == PLAYING || playerStatus == PAUSED)
		{
			if (sourceDataLine != null)
			{
				sourceDataLine.flush();
				sourceDataLine.stop();
			}
			setStatus(STOPPED);
			Thread.yield();	// let other finalize
			trace(1,getClass().getName(), "Stop called");
		}
	}

	/**
	 * Pauses the playback.<br>
	 *
	 * Player Status = PAUSED.
	 */
	public synchronized void pausePlayback()
	{
		if (sourceDataLine != null)
		{
			if (playerStatus == PLAYING)
			{
				sourceDataLine.flush();
				sourceDataLine.stop();
				setStatus(PAUSED);
				trace(1,getClass().getName(), "Pause called");
			}
		}
	}

	/**
	 * Resumes the playback.<br>
	 *
	 * Player Status = PLAYING.
	 */
	public synchronized void resumePlayback()
	{
		if (sourceDataLine != null)
		{
			if (playerStatus == PAUSED)
			{
				sourceDataLine.start();
				setStatus(PLAYING);
				trace(1,getClass().getName(), "Resume called");
			}
		}
	}

	/**
	 * Starts playback. Returns null (OK) or "ERROR".
	 */
	public synchronized void startPlayback() throws Exception
	{
		if (playerStatus == READY)
		{
			initLine();

			trace(1,getClass().getName(), "Creating new thread");
			playerThread = new Thread(this);
			playerThread.start();

			if (sourceDataLine != null)
			{
				sourceDataLine.start();
			}
		}
		else	{
			trace(0,getClass().getName(), "Player status is: ",""+playerStatus);
		}
	}

	/**
	 * Main loop.
	 *
	 * Player Status == STOPPED => End of Thread + Freeing Audio Ressources.<br>
	 * Player Status == PLAYING => Audio stream data sent to Audio line.<br>
	 * Player Status == PAUSED => Waiting for another status.
	 */
	public void run()
	{
		trace(1,getClass().getName(), "Thread Running");
		//if (audioInputStream.markSupported()) audioInputStream.mark(audioFileFormat.getByteLength());
		//else trace(1,getClass().getName(), "Mark not supported");
		int	nBytesRead = 1;
		setStatus(PLAYING);
		int nBytesCursor = 0;
		byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];
		float nFrameSize = (float) sourceDataLine.getFormat().getFrameSize();
		float nFrameRate =  sourceDataLine.getFormat().getFrameRate();
		float bytesPerSecond = nFrameSize * nFrameRate;
		//int secondsTotal  =  Math.round((float)audioFileFormat.getByteLength()/bytesPerSecond);
		int secondsTotal = (int) Math.round(getTotalLengthInSeconds());
		
		while (nBytesRead != -1 && getStatus() != STOPPED)
		{
			if (getStatus() == PLAYING)
			{
				try
				{
					//============ Seek implementation start. WAV format only !
					if (doSeek > -1 )
					{
						if (( getAudioFileFormat() != null) && (getAudioFileFormat().getType().toString().startsWith("WAV")) )
						{
							if ( (secondsTotal != AudioSystem.NOT_SPECIFIED) && (secondsTotal > 0) )
							{
								sourceDataLine.flush();
								sourceDataLine.stop();
								//audioInputStream.reset();
								audioInputStream.close();
								audioInputStream = AudioSystem.getAudioInputStream(file);
								nBytesCursor = 0;
								if (audioFileFormat.getByteLength()-doSeek < abData.length)
									doSeek = audioFileFormat.getByteLength() - abData.length;
								doSeek = doSeek - doSeek % 4;
								int toSkip = (int) doSeek;
								// skip(...) instead of read(...) runs out of memory ?!
								while ( (toSkip > 0) && (nBytesRead > 0) )
								{
									if (toSkip > abData.length)
										nBytesRead = audioInputStream.read(abData, 0, abData.length);
									else
										nBytesRead = audioInputStream.read(abData, 0, toSkip);
									
									toSkip = toSkip - nBytesRead;
									nBytesCursor = nBytesCursor + nBytesRead;
								}
								sourceDataLine.start();
							}
							else trace(1,getClass().getName(), "Seek not supported for this InputStream : "+secondsTotal);
						}
						else
						{
							trace(1,getClass().getName(), "Seek not supported for this InputStream : "+secondsTotal);
						}
						doSeek = -1;
					}
					//============ Seek implementation end.


					// read from audioInputStream
					nBytesRead = audioInputStream.read(abData, 0, abData.length);
					
				}
				catch (Exception e)
				{
					trace(1,getClass().getName(), "InputStream error : ("+nBytesRead+")",e.getMessage());
					e.printStackTrace();
					exitThread();
					return;
				}
				
				// write to sourceDataLine
				if (nBytesRead >= 0)
				{
					if (playerListener != null)
						playerListener.updateMediaData(abData);
						
					int	nBytesWritten = sourceDataLine.write(abData, 0, nBytesRead);
					nBytesCursor = nBytesCursor + nBytesWritten;
					
					if (playerListener != null)
						playerListener.updateCursor((int)Math.round((float)nBytesCursor/bytesPerSecond), secondsTotal);
				}
			}
			else
			{
				try	{ Thread.sleep(200); }	catch (Exception e)	{}
			}
		}	// end while

		exitThread();
	}


	private synchronized void exitThread()	{
		if (sourceDataLine != null)
		{
			try
			{
				sourceDataLine.drain();
				sourceDataLine.stop();
				sourceDataLine.close();
			}
			catch (Exception e)
			{
				trace(1,getClass().getName(), "Cannot Free Audio ressources",e.getMessage());
			}
			finally
			{
				sourceDataLine = null;
			}
		}

		trace(1,getClass().getName(), "Thread Stopped");
		setStatus(READY);
			
		playerThread = null;
	}



	/*----------------------------------------------*/
	/*--               Gain Control               --*/
	/*----------------------------------------------*/

	/**
	 * Returns true if Gain control is supported.
	 */
	public boolean hasGainControl()
	{
		return gainControl != null;
	}

	/**
	 * Sets Gain value.
	 * Linear scale 0.0  <-->  1.0
	 * Threshold Coef. : 1/2 to avoid saturation.
	 */
	public void setGain(double fGain)
	{
		if (hasGainControl())
		{
			double minGainDB = getMinimum();
            double ampGainDB = ((10.0f/20.0f)*getMaximum()) - getMinimum();
            double cste = Math.log(10.0)/20;
            double valueDB = minGainDB + (1/cste)*Math.log(1+(Math.exp(cste*ampGainDB)-1)*fGain);
		    //trace(1,getClass().getName(), "Gain : "+valueDB);
			gainControl.setValue((float)valueDB);
		}
	}

	/**
	 * Returns Gain value.
	 */
	public float getGain()
	{
		if (hasGainControl())
		{
			return gainControl.getValue();
		}
		else
		{
			return 0.0F;
		}
	}

	/**
	 * Gets max Gain value.
	 */
	public float getMaximum()
	{
		if (hasGainControl())
		{
			return gainControl.getMaximum();
		}
		else
		{
			return 0.0F;
		}
	}


	/**
	 * Gets min Gain value.
	 */
	public float getMinimum()
	{
		if (hasGainControl())
		{
			return gainControl.getMinimum();
		}
		else
		{
			return 0.0F;
		}
	}


	/*----------------------------------------------*/
	/*--               Pan Control                --*/
	/*----------------------------------------------*/

	/**
	 * Returns true if Pan control is supported.
	 */
	public boolean hasPanControl()
	{
		return panoramaControl != null;
	}

	/**
	 * Returns Pan precision.
	 */
	public float getPrecision()
	{
		if (hasPanControl())
		{
			return panoramaControl.getPrecision();
		}
		else
		{
			return 0.0F;
		}
	}


	/**
	 * Returns Pan value.
	 */
	public float getPan()
	{
		if (hasPanControl())
		{
			return panoramaControl.getValue();
		}
		else
		{
			return 0.0F;
		}
	}

	/**
	 * Sets Pan value.
	 * Linear scale : -1.0 <--> +1.0
	 */
	public void setPan(float fPan)
	{
		if (hasPanControl())
		{
		   //trace(1,getClass().getName(), "Pan : "+fPan);
			panoramaControl.setValue(fPan);
		}
	}


	/*----------------------------------------------*/
	/*--                   Seek                   --*/
	/*----------------------------------------------*/

	/**
	 * Sets Seek value.
	 * Linear scale : 0.0 <--> +1.0
	 */
	public void setSeek(double seek) throws IOException
	{
		double length = -1;
		if ( (audioFileFormat != null) && (audioFileFormat.getByteLength() != AudioSystem.NOT_SPECIFIED) ) length = (double) audioFileFormat.getByteLength();
		long newPos = (long) Math.round(seek*length);
		doSeek = newPos;
	}

	/*----------------------------------------------*/
	/*--               Audio Format               --*/
	/*----------------------------------------------*/

	/**
	 * Returns source AudioFormat.
	 */
	public AudioFormat getAudioFormat()
	{
		if (audioFileFormat != null)
		{
			return audioFileFormat.getFormat();
		}
		else return null;
	}

	/**
	 * Returns source AudioFileFormat.
	 */
	public AudioFileFormat getAudioFileFormat()
	{
		if (audioFileFormat != null)
		{
			return audioFileFormat;
		}
		else return null;
	}

	/**
	 * Returns total length in seconds.
	 */
	public double getTotalLengthInSeconds()
	{
		double lenghtInSecond = 0.0;
		if ( getAudioFileFormat() != null)
		{
			int FL = (getAudioFileFormat()).getFrameLength();
			int FS = (getAudioFormat()).getFrameSize();
			float SR = (getAudioFormat()).getSampleRate();
			float FR = (getAudioFormat()).getFrameRate();
			int TL = (getAudioFileFormat()).getByteLength();
			String type = (getAudioFileFormat()).getType().toString();
			String encoding = (getAudioFormat()).getEncoding().toString();
			if ((FL != -1) && ( (type.startsWith("MP3")) || (type.startsWith("VORBIS")) ) )
			{
				// No accurate formula :-(
				// Alternative dirty solution with SPI
				StringTokenizer st = new StringTokenizer(type,"x");
				st.nextToken();st.nextToken();
				String totalMSStr = st.nextToken();
				lenghtInSecond=Math.round((Integer.parseInt(totalMSStr))/1000);
			}
			else
			{
				int br = getBitRate();
				if (br > 0) lenghtInSecond = TL*8/br;
				else lenghtInSecond = TL/(FS*SR);

			}
			trace(2,getClass().getName(),"Type="+type+" Encoding="+encoding+" FL="+FL+" FS="+FS+" SR="+SR+" FR="+FR+" TL="+TL," lenghtInSecond="+lenghtInSecond);
		}
		if (lenghtInSecond < 0.0) lenghtInSecond = 0.0;
		return lenghtInSecond;
	}

	/**
	 * Returns bit rate.
	 */
	public int getBitRate()
	{
		int bitRate = 0;
		if ( getAudioFileFormat() != null)
		{
			int FL = (getAudioFileFormat()).getFrameLength();
			int FS = (getAudioFormat()).getFrameSize();
			float SR = (getAudioFormat()).getSampleRate();
			float FR = (getAudioFormat()).getFrameRate();
			int TL = (getAudioFileFormat()).getByteLength();
			String type = (getAudioFileFormat()).getType().toString();
			String encoding = (getAudioFormat()).getEncoding().toString();
			// Assumes that type includes xBitRate string.
			if ( (type != null) && ((type.startsWith("MP3")) || (type.startsWith("VORBIS"))) )
			{
				// BitRate string appended to type.
				// Another solution ?
				StringTokenizer st = new StringTokenizer(type,"x");
				if (st.hasMoreTokens())
				{
					st.nextToken();
					String bitRateStr = st.nextToken();
					bitRate=Math.round((Integer.parseInt(bitRateStr)));
				}
			}
			else
			{
				bitRate = Math.round(FS*FR*8);
			}
			trace(2,getClass().getName(),"Type="+type+" Encoding="+encoding+" FL="+FL+" FS="+FS+" SR="+SR+" FR="+FR+" TL="+TL," bitRate="+bitRate);
		}
		// N/A so computes bitRate for output.
		if ((bitRate <= 0) && (sourceDataLine != null))
		{
			bitRate = Math.round(((sourceDataLine.getFormat()).getFrameSize())*((sourceDataLine.getFormat()).getFrameRate())*8);
		}
		return bitRate;
	}



	/*----------------------------------------------*/
	/*--                   Misc                   --*/
	/*----------------------------------------------*/

	/**
	 * Sends traces to Debug.
	 */
	private void trace(int level, String msg1, String msg2)
	{
		//System.err.println("Trace level "+level+": "+msg1+":"+msg2);
	}

	private void trace(int level, String msg1, String msg2, String msg3)
	{
		//System.err.println("Trace level "+level+": "+msg1+":"+msg2+","+msg3);
	}

}
