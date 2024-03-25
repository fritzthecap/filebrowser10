package fri.gui.swing.filebrowser;

import java.awt.BorderLayout;
import javax.swing.*;
import java.io.File;
import fri.gui.swing.text.ClipableJTextField;
import fri.util.FileUtil;

/**
	Let edit the filename, but protect the path. Needed in SearchFrame,
	where path and file is rendered in one table column.
*/

public class InfoTableCellEditor extends DefaultCellEditor
{
	private JPanel panel = new JPanel();
	private ClipableJTextField tf;
	private ClipableJTextField lb = new ClipableJTextField();
	private InfoTableDndListener lsnr = null;
	private boolean added = false;
	private boolean prevActive;


	public InfoTableCellEditor(JTextField dummy) {	// dummy constructor
		super(new ClipableJTextField());
		
		tf = (ClipableJTextField)editorComponent;
		lb.setEditable(false);

		panel.setLayout(new BorderLayout());
		panel.add(tf, BorderLayout.CENTER);

		editorComponent = panel;
		this.clickCountToStart = 2;
		
		delegate = new EditorDelegate() {
			public void setValue(Object value) {
				System.err.println("InfoTableCellEditor.EditorDelegate.setValue "+value);
				setDndListenerActive(false);
				if (value == null)	{
					tf.setText("");
					lb.setText("");
				}
				else	{
					String pathFile = (String)value;
					if (pathFile.indexOf(File.separator) >= 0)	{
						if (added == false)	{
							panel.add(lb, BorderLayout.WEST);
							added = true;
						}
						String path = FileUtil.separatePath(pathFile, false);
						lb.setText(path);
					}
					else	{
						if (added == true)	{
							panel.remove(lb);
							added = false;
						}
					}
					String file = FileUtil.separateFile(pathFile);
					tf.setText(file);
				}
			}
			
			public Object getCellEditorValue() {
				//System.err.println("InfoTableCellEditor.EditorDelegate.getCellEditorValue");
				setDndListenerActive(true);
				return added ? lb.getText()+tf.getText() : tf.getText();
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