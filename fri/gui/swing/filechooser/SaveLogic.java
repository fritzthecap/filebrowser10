package fri.gui.swing.filechooser;

/**
	The workflow of saving to a persistence medium that requires an
	user-defined unique identifier for the new item (e.g. filesystem).
	This logic confirms overwries and lets perform "Save" and "Save As".
	It returns null everytime the Object was NOT saved.
*/

public class SaveLogic
{
	/**
		Any client using SaveLogic must implement these calls specific to the type of persistence object (e.g. File).
	*/
	public interface Impl
	{
		/**
			Tests if the passed (File) object already exists (could be overwritten).
		*/
		public boolean exists(Object toWrite);

		/**
			Tests if the passed (File) objects are equal. This is needed for "Save As" when the original file was chosen.
		*/
		public boolean isEqual(Object toWrite1, Object toWrite2);

		/**
			Writes data to the passed (File) object, and flushes/closes it.
			@exception throws any exception that occurs during write process
		*/
		public void write(Object toWrite) throws Exception;

		/**
			Dialog to get a (File) object name from user. Throws an Exception on Cancel.
			@param toWriteAs the original object or null, if no name ever existed (new File).
			@return the object where to save to
			@exception thrown when user cancels
		*/
		public Object saveAsDialog(Object toWriteAs) throws CancelException;

		/**
			Dialog to get overwrite confirmation from user.
			@return true when overwrite was chosen, false when not
			@exception thrown when user cancels
		*/
		public boolean overwriteDialog(Object toWriteAs) throws CancelException;

		/**
			Dialog to show an IO error message.
		*/
		public void errorDialog(Exception ex, Object toWrite);
	}
	

	/**
		Start a "Save" logic with passed implementation. The object to write is the
		representation of the target object in the persistence medium, can be null,
		then <i>saveAsDialog()</i> will be launched.
		This class does not deal with any data or write processing!
		@param impl the object that implements SaveLogicImpl
		@param toWrite the Object that is about to be written (e.g. class File for filesystems)
		@return the save target, or null if user canceled or an IO error occured (i.e. data were NOT saved).
			When the behaviour is to force writing (mustBeWritten is true)
	*/
	public static Object save(Impl impl, Object toWrite)	{
		return save(impl, toWrite, toWrite);
	}
	
	/**
		Start a "Save As" logic with passed implementation. The object to write is the
		representation of the target object in the persistence medium, can be null,
		in both cases a <i>saveAsDialog()</i> will be launched.
		This class does not deal with any data or write processing!
		@param impl the object that implements SaveLogicImpl
		@param toWrite the Object that is about to be written (e.g. class File for filesystems)
		@return the save target, or null if user canceled or an IO error occured (i.e. data were NOT saved).
			When the behaviour is to force writing (mustBeWritten is true)
	*/
	public static Object saveAs(Impl impl, Object toWrite)	{
		return save(impl, null, toWrite);
	}
	
	private static Object save(Impl impl, Object toWrite, final Object origToWrite)	{
		try	{
			Object lastToWrite = origToWrite;
			
			while (toWrite == null)	{					
				Object toWriteAs = lastToWrite = impl.saveAsDialog(toWrite != null ? toWrite : lastToWrite);	// cancel would throw exception
				if (toWriteAs == null)	// was window close
					return null;
				
				if (impl.exists(toWriteAs) && (origToWrite == null || impl.isEqual(origToWrite, toWriteAs) == false))	{
					// ask for overwrite if exists and is not the original
					if (impl.overwriteDialog(toWriteAs) == false)	{	// cancel would throw exception
						toWriteAs = null;
					}
				}
				
				toWrite = toWriteAs;
			}
		
			impl.write(toWrite);

			return toWrite;
		}
		catch (CancelException ec)	{
			System.err.println("User canceled saving "+toWrite);
		}
		catch (Exception eio)	{	// medium error, data were not saved
			impl.errorDialog(eio, toWrite);
		}

		return null;
	}





	/* test main
	public static void main(String [] args)	{
		SaveLogic.Impl impl = new SaveLogic.Impl()	{
			public boolean exists(Object toWrite)	{
				return ((java.io.File)toWrite).exists();
			}
			public void write(Object toWrite) throws Exception	{
				System.out.println("i am writing "+toWrite);
				new java.io.FileOutputStream((java.io.File)toWrite).close();
			}
			public Object saveAsDialog(Object toWriteAs) throws CancelException	{
				System.out.print("Save As: ("+toWriteAs+"): ");
				try	{
					String s = new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
					if (s != null && s.trim().equals("") == false)
						return new java.io.File(s);
				}
				catch (Exception e)	{
					e.printStackTrace();
				}
				throw new CancelException();
			}
			public boolean overwriteDialog(Object toWriteAs) throws CancelException	{
				System.out.print("Overwrite \""+toWriteAs+"\"? (y/n): ");
				try	{
					String s = new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
					if (s != null && s.trim().equals("y"))
						return true;
					if (s != null && s.trim().equals("n"))
						return false;
				}
				catch (Exception e)	{
					e.printStackTrace();
				}
				throw new CancelException();
			}
			public void errorDialog(Exception ex, Object toWrite)	{
				System.out.println("Error occured when writing "+toWrite+"\": "+ex);
			}
		};
		
		
		try	{
			java.io.File f1 = new java.io.File("aaa");
			new java.io.FileOutputStream(f1).close();
			java.io.File f2 = new java.io.File("bbb");
			new java.io.FileOutputStream(f2).close();
			f2.setReadOnly();
			
			java.io.File f = (java.io.File)SaveLogic.saveAs(impl, f1);
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