package fri.gui.swing.mailbrowser;

import java.awt.Component;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.progressdialog.CancelProgressDialog;
import fri.gui.swing.yestoalldialog.OverwriteLauncher;
import fri.gui.swing.yestoalldialog.YesToAllDialog;
import fri.gui.swing.yestoalldialog.UserCancelException;

/**
	Confirm overwrite of copy target folders.
	@author  Ritzberger Fritz
*/

public class OverwriteDialog extends OverwriteLauncher
{
	private CancelProgressDialog progressDialog;

	public OverwriteDialog(CancelProgressDialog progressDialog)	{
		this.progressDialog = progressDialog;
	}

	public CancelProgressDialog getCancelProgressDialog()	{
		return progressDialog;
	}

	public void setCancelProgressDialog(CancelProgressDialog progressDialog)	{
		this.progressDialog = progressDialog;
	}

	public boolean show(String src, String srcInfo, String tgt, String tgtInfo)	{
		Component pnt = progressDialog == null ? null : progressDialog.getDialog();
		if (pnt == null)
			pnt = GuiApplication.globalFrame;
		
		try	{
			int ret = super.show(pnt, src, srcInfo, tgt, tgtInfo, Language.get("Overwrite"), Language.get("With"));
			return ret == YesToAllDialog.YES;
		}
		catch (UserCancelException e)	{
			throw new RuntimeException("User Cancel");
		}
	}

}
