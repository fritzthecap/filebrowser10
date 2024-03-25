package fri.gui.swing.commandmonitor;

import java.awt.*;
import java.io.*;
import fri.gui.text.*;
import fri.util.process.*;

/**
	ProcessReader zu ProcessManager, der den Output auf Text-Felder
	umleitet und eine Cursor-Position fuer allfaellige Eingaben haelt.
	Mit closeOutput() laesst sich das Befuellen der Textfelder abstellen.
*/

public class ProcessHandler implements ProcessReader
{
	private int caretPos = 0;
	private TextRenderer ta_out, ta_err = null;
	private ProcessManager processManager;
	private boolean doOutput = true;


	/**
		Starten eines neuen Process, der von einem Hintergrund-Thread gesteuert wird.
		@param cmd durchzufuehrendes Kommando
		@param pw ProcessWriter, der an den Process schreibt
		@param ta_out TextRenderer fuer stdout und stderr vom Process
	*/
	public ProcessHandler(
		String [] cmd,
		ProcessWriter pw,
		TextRenderer ta_out)
	{
		this(cmd, null, pw, ta_out, (TextRenderer)null, (File)null);
	}

	/**
		Starten eines neuen Process, der von einem Hintergrund-Thread gesteuert wird.
		@param cmd durchzufuehrendes Kommando
		@param env environment des Process, -Dxxx=...
		@param pw ProcessWriter, der an den Process schreibt
		@param ta_out TextRenderer fuer stdout und stderr vom Process
	*/
	public ProcessHandler(
		String [] cmd,
		String [] env,
		ProcessWriter pw,
		TextRenderer ta_out,
		File workingDirectory)
	{
		this(cmd, env, pw, ta_out, null, workingDirectory);
	}
	
	/**
		Starten eines neuen Process, der von einem Hintergrund-Thread gesteuert wird.
		@param cmd durchzufuehrendes Kommando
		@param env kann null sein, environment des Process, -Dxxx=...
		@param pw ProcessWriter, der an den Process schreibt
		@param ta_out TextRenderer fuer stdout vom Process
		@param ta_err TextRenderer fuer stderr vom Process
	*/
	public ProcessHandler(
		String [] cmd,
		String [] env,
		ProcessWriter pw,
		TextRenderer ta_out,
		TextRenderer ta_err,
		File workingDirectory)
	{
		this.ta_out = ta_out;
		this.ta_err = ta_err;

		processManager = new ProcessManager(pw, this, cmd, env, workingDirectory);
	}

	/** Process (sanft) beenden */
	public void stop()	{
		if (processManager != null)	{
			System.err.println("stopping process ...");
			processManager.stop();
			processManager = null;
		}
	}

	/** Process (hart) abbrechen */
	public void exitStop()	{
		processManager.exitStop();
	}

	/** Setzt Output-Flag auf false,
		es werden keine empfangene Zeilen mehr in die TextAreas gesetzt. */
	public void closeOutput()	{
		doOutput = false;
	}

	/** Setzt die aktuelle Zeitverzoegerung bei der Ausgabe (Output-Bremse). */
	public void setMillis(int value)	{
		if (processManager != null)
			processManager.setDelayMillis(value);
	}

	/** Liefert die aktuelle Zeitverzoegerung bei der Ausgabe (Output-Bremse). */
	public int getMillis()	{
		if (processManager != null)
			return processManager.getDelayMillis();
		return -1;
	}


	/** Liefert true wenn an den Process geschrieben werden kann. */
	public boolean isInputOpen()	{
		return processManager.isInputOpen();
	}


	/** Setzen einer neuen Output-TextArea. */
	public void setOutTextRenderer(TextRenderer ta_out)	{
		this.ta_out = ta_out;
	}

	/** Liefert die aktuelle Output-TextArea. */
	public TextRenderer getOutTextRenderer()	{
		return ta_out;
	}

	/** Setzen einer neuen Error-TextArea. */
	public void setErrTextRenderer(TextRenderer ta_err)	{
		this.ta_err = ta_err;
	}

	/** Liefert die aktuelle Error-TextArea. */
	public TextRenderer getErrTextRenderer()	{
		return ta_err;
	}


	/**
		Implementiert ProcessReader, schreibt Empfangenes in Output-TextArea.
		Setzt die Lese-Position auf das Text-Ende.
	*/
	public void printlnOut(String line)	{
		println(ta_out, line);
		setCaretPosition(ta_out.getCaretPosition());
	}

	/**
		Implementiert ProcessReader, schreibt Empfangenes in Error-TextArea,
		falls diese definiert wurde, sonst in Output-TextArea.
	*/
	public void printlnErr(String line)	{
		if (ta_err == null)
			printlnOut(line);
		else
			println(ta_err, line);
	}

	private void println(final TextRenderer ta, final String line)	{
		if (doOutput && ta != null)	{
			EventQueue.invokeLater(new Runnable()	{
				public void run()	{
					ta.append(line);
					if (ta == ta_out)
						((Component)ta).requestFocus();
				}
			});
		}
	}

	/** Implementiert ProcessReader, tut nichts. */
	public void exitcode(int ec)	{
		processManager = null;
	}

	/** Implementiert ProcessReader, tut nichts. */
	public void progress()	{
	}
	
	
	/** Liefert die Lese-Position in der TextArea. */
	public synchronized int getCaretPosition()	{
		return caretPos;
	}
	
	/** Setzt die Lese-Position in der TextArea. */
	public synchronized void setCaretPosition(int caretPos)	{
		this.caretPos = caretPos;
	}

}