package fri.gui.swing.filebrowser;

import javax.swing.table.*;
import java.util.Vector;

/**
	Target: implement getColumnClass to render Boolean as CheckBox,
		obtain a invisible column "Environment", containing the
		environment variables for a command-line.
*/

public class OpenEventTableModel extends DefaultTableModel
{
	private OpenCommandList commands;
	
	
	public OpenEventTableModel(OpenCommandList commands)	{
		super(commands, commands.getColumns());
		this.commands = commands;
	}
	
	public Vector buildRow(String pattern, boolean isLeaf)	{
		return commands.buildRow(pattern, isLeaf);
	}
	public Vector buildRow(String pattern, String type)	{
		return commands.buildRow(pattern, type);
	}
	
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/** @return content of invisible row for process-environment as String */
	public String [] getEnvironment(int idx) {
		return commands.getEnvironment(idx);
	}

	/** put a environment for a specified index in table-model vector */
	public void putEnvironment(int idx, String [] env) {
		commands.putEnvironment(idx, env);
	}
		
}