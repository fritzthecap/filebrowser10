package fri.gui.swing.filebrowser;

import java.util.*;
import javax.swing.*;
import fri.gui.swing.text.ClipableJTextField;

/**
	Let edit the filetime.
*/

public class InfoTableTimeCellEditor extends DefaultCellEditor
{
	private ClipableJTextField tf;
	private InfoTableDndListener lsnr = null;
	private boolean prevActive;


	public InfoTableTimeCellEditor(JTextField dummy) {	// dummy constructor
		super(new ClipableJTextField());		
		tf = (ClipableJTextField)editorComponent;
		this.clickCountToStart = 2;
		
		delegate = new EditorDelegate() {
			public void setValue(Object value) {
				System.err.println("InfoTableTimeCellEditor.EditorDelegate.setValue "+value);
				setDndListenerActive(false);
				
				if (value == null)	{
					tf.setText("");
				}
				else	{
					Long modified = (Long)value;
					String time = FileNode.dateFormater.format(new Date(modified.longValue()));
					tf.setText(time);
				}
			}
			
			public Object getCellEditorValue() {
				System.err.println("InfoTableTimeCellEditor.EditorDelegate.getCellEditorValue");
				setDndListenerActive(true);
				return tf.getText();
			}
		};
	}
	
	public void setDndListener(InfoTableDndListener lsnr)	{
		this.lsnr = lsnr;
	}
	
	private void setDndListenerActive(boolean active)	{
		if (lsnr != null)	{
			if (active == false)
				prevActive = lsnr.getActive();
			else
				active = prevActive;
			lsnr.setActive(active);
		}
	}

}