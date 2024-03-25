package fri.gui.swing.yestoalldialog;

import java.awt.Component;

/**
	Show a modal overwrite dialog, optional from a background thread.
	Return the dialog result synchronously, by EventQueue.invokeAndWait().
*/

public class OverwriteLauncher extends YesToAllLauncher
{
	private String src, sinf, tgt, tinf;
	private String overwriteLabel, withLabel;
	private Component parent;

	
	/** Called from background thread to show dialog. */
	public int show(Component parent, String src, String sinf, String tgt, String tinf)
		throws UserCancelException
	{
		init(parent, src, sinf, tgt, tinf, null, null);
		return super.show();
	}
	
	/** Called from background thread to show dialog. */
	public int show(Component parent, String src, String sinf, String tgt, String tinf, String overwriteLabel, String withLabel)
		throws UserCancelException
	{
		init(parent, src, sinf, tgt, tinf, overwriteLabel, withLabel);
		return super.show();
	}
	

	private void init(Component parent, String src, String sinf, String tgt, String tinf, String overwriteLabel, String withLabel)	{
		this.parent = parent;
		this.src = src;
		this.sinf = sinf;
		this.tgt = tgt;
		this.tinf = tinf;
		this.overwriteLabel = overwriteLabel;
		this.withLabel = withLabel;
	}
		
	/** Called from event: create and init dialog. */
	protected void startDialog()	{
		if (dialog == null)	{
			dialog = new OverwriteDialog(parent, overwriteLabel, withLabel);
		}
		
		((OverwriteDialog)dialog).setInfo(src, sinf, tgt, tinf);

		super.startDialog();
	}
	
}