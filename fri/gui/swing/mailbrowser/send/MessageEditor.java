package fri.gui.swing.mailbrowser.send;

import javax.swing.*;
import javax.swing.text.*;
import fri.gui.swing.mailbrowser.Language;
import fri.gui.swing.text.*;
import fri.gui.swing.actionmanager.*;

/**
	Class that holds a message editor delegate and provides all necessary accessor methods.
*/

public class MessageEditor
{
	private JTextComponent textArea;

	
	public MessageEditor(ActionManager controller)	{
		this.textArea = new MessageTextArea(controller);
	}
	
	public JTextComponent getSensorComponent()	{
		return textArea;
	}

	public String getText()	{
		return textArea.getText();
	}

	public void setText(String text)	{
		textArea.setText(text);
		textArea.setCaretPosition(0);
	}


	private class MessageTextArea extends ComfortTextArea
	{
		public MessageTextArea(ActionManager controller)	{
			controller.visualizeAction(SendController.ACTION_ATTACH, popup, false, 0);
			popup.insert(new JPopupMenu.Separator(), 1);
			controller.visualizeAction(SendController.ACTION_CUT, popup, false, 2);
			controller.visualizeAction(SendController.ACTION_COPY, popup, false, 3);
			controller.visualizeAction(SendController.ACTION_PASTE, popup, false, 4);
			popup.insert(new JPopupMenu.Separator(), 5);
			controller.visualizeAction(SendController.ACTION_UNDO, popup, false, 6);
			controller.visualizeAction(SendController.ACTION_REDO, popup, false, 7);
			popup.insert(new JPopupMenu.Separator(), 8);
			controller.visualizeAction(SendController.ACTION_CRYPT, popup, false, 9);
			popup.insert(new JPopupMenu.Separator(), 10);
		}

		protected String findLabel()	{
			return Language.get("Find_Text");
		}
		protected String goLineLabel()	{
			return Language.get("Go_To_Line");
		}
		protected String tabWidthLabel()	{
			return Language.get("Tab_Width");
		}
		protected String wrapLinesLabel()	{
			return Language.get("Wrap_Lines");
		}
	}

}
