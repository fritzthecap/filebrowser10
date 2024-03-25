package fri.gui.swing.filechooser;

import java.io.File;
import java.awt.Component;
import javax.swing.JOptionPane;
import fri.gui.swing.ComponentUtil;

/**
	The save logic implementation for File GUIs.
	Derivations MUST implement the write() method
	(how a file is stored or what is written).
*/

public abstract class FileGUISaveLogicImpl implements SaveLogic.Impl
{
	private Component dialogParent;
	
	
	public FileGUISaveLogicImpl(Component dialogParent)	{
		this.dialogParent = dialogParent;
	}
	

	//public abstract void write(Object toWrite);


	/** Tests if the passed (File) object already exists (could be overwritten). */
	public boolean exists(Object toWrite)	{
		return ((File)toWrite).exists();
	}

	/** Tests if the passed (File) objects are equal. This is needed for "Save As" when the original file was chosen. */
	public boolean isEqual(Object toWrite1, Object toWrite2)	{
		File f1 = (File)toWrite1;
		File f2 = (File)toWrite2;
		if (f1.isAbsolute() == false)
			f1 = new File(f1.getAbsolutePath());
		if (f2.isAbsolute() == false)
			f2 = new File(f2.getAbsolutePath());
		return f1.equals(f2);
	}

	/** Dialog to get a (File) object name from user. Throws an Exception on Cancel. */
	public Object saveAsDialog(Object toWriteAs)
		throws CancelException
	{
		if (toWriteAs == null)
			DefaultFileChooser.setChooserFile(null);
		return DefaultFileChooser.saveDialog((File)toWriteAs, dialogParent, dialogParent.getClass());
	}

	/** Dialog to get overwrite confirmation from user. */
	public boolean overwriteDialog(Object toWriteAs)
		throws CancelException
	{
		File f = (File)toWriteAs;
		String title = f.getName()+(f.getParent() != null ? " in "+f.getParent() : "");
		
		int ret = JOptionPane.showConfirmDialog(
				ComponentUtil.getWindowForComponent(dialogParent),
				"Overwrite "+title+" ?",
				"Confirm Overwrite",
				JOptionPane.YES_NO_CANCEL_OPTION);
		
		if (ret == JOptionPane.YES_OPTION)
			return true;
		if (ret == JOptionPane.NO_OPTION)
			return false;
			
		throw new CancelException();
	}

	/** Dialog to show an IO error message. */
	public void errorDialog(Exception ex, Object toWrite)	{
		ex.printStackTrace();
		JOptionPane.showMessageDialog(
				dialogParent,
				"Error when saving "+toWrite+":\n"+ex.toString(),
				"Save Error",
				JOptionPane.OK_OPTION);
	}




	/* test main 
	public static void main(String [] args)	{
		FileGUISaveLogicImpl impl = new FileGUISaveLogicImpl(new javax.swing.JFrame())	{
			public void write(Object toWrite)
				throws Exception
			{
				System.out.println("Writing to File "+toWrite);
				new java.io.FileOutputStream((File)toWrite).close();
			}
		};
		
		try	{
			File f1 = new File("aaa");
			new java.io.FileOutputStream(f1).close();
			File f2 = new File("bbb");
			new java.io.FileOutputStream(f2).close();
			f2.setReadOnly();
			
			File f = (File)new SaveLogic().saveAs(impl, f1);
			System.out.print("Return from SaveLogic was: \""+f+"\"");
			
			f1.delete();
			f2.delete();
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
	}
	*/
}