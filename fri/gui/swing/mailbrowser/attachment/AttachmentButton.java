package fri.gui.swing.mailbrowser.attachment;

import java.awt.Component;
import javax.swing.*;
import javax.swing.border.*;
import javax.mail.Part;
import javax.mail.MessagingException;
import javax.activation.DataHandler;
import fri.util.error.Err;
import fri.gui.CursorUtil;
import fri.gui.awt.geometrymanager.GeometryManager;
import fri.gui.swing.*;
import fri.gui.swing.application.GuiApplication;
import fri.gui.swing.mailbrowser.viewers.PartView;
import fri.gui.swing.mailbrowser.viewers.ViewerFactory;

/**
	Button that reponds to double click or Enter by opening its attachment.

	@author Fritz Ritzberger, 2003
*/

public class AttachmentButton extends JButton implements
	PartView
{
	private Part part;
	
	public AttachmentButton(Part part, String label, String tooltip)	{
		super(label);
		
		this.part = part;

		setToolTipText(tooltip);
		setBorder(
			BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 4, 0, 0),
				new SoftBevelBorder(BevelBorder.RAISED)
			)
		);
		
		new AttachmentDndSender(this);	// sending attachment by drag&drop as file
		
		setContentAreaFilled(false);
		//setFocusPainted(false);
	}


	// interface PartView
	
	/** Implements PartView: returns "this". */
	public JComponent getSensorComponent()	{
		return this;
	}
	
	/** Implements PartView: returns dataHandler from "part" membervariable. */
	public DataHandler getDataHandler()	{
		try	{
			return part.getDataHandler();
		}
		catch (MessagingException e)	{
			Err.error(e);
		}
		return null;
	}



	/** Open the obtained Part in a viewer frame. */
	public void open()	{
		CursorUtil.setWaitCursor(this);
		try	{
			Component c = ViewerFactory.getViewer(part);
			if (c != null)	{
				new AttachmentFrame(AttachmentPanel.makeButtonLabel(part), c);
			}
			// else: was a command launcher
		}
		catch (Exception ex)	{
			Err.error(ex);
		}
		finally	{
			CursorUtil.resetWaitCursor(this);
		}
	}



	private static class AttachmentFrame extends JFrame
	{
		AttachmentFrame(String title, Component c)	{
			super(title);
			
			IconUtil.setFrameIcon(this, GuiApplication.getApplicationIconURL());
			
			getContentPane().add(c);
			new GeometryManager(AttachmentFrame.this).show();
		}
	}
	
}