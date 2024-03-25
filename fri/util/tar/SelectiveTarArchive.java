package fri.util.tar;

import java.util.*;
import java.io.*;
import com.ice.tar.*;
import fri.util.os.OS;
import fri.util.file.ValidFilename;

/**
	TarArchive derivation.
	Add capability to list entry structure (size, time, name)
	and to extract specified entries.
*/

public class SelectiveTarArchive extends TarArchive
{
	private StringBuffer errors = null;
	
	
	public SelectiveTarArchive(InputStream in)	{
		super(in);
	}

	public SelectiveTarArchive(OutputStream out)	{
		super(out);
	}
	
	
	public String getError()	{
		return errors == null ? null : errors.toString();
	}
	
	
	/**
	 * Enumerate the contents of this archive.
	 */
	public Enumeration entries()
		throws Exception
	{
		Vector v = new Vector();
		while (true)	{
			TarEntry entry = this.tarIn.getNextEntry();
			
			if (entry == null)	{
				break;
			}
			else	{
				v.add(new SelectiveTarEntry(entry));
			}
		}
		return v.elements();
	}


	/**
	 * Extract the contents of the passed SelectiveTarEntry to dir.
	 * <br>MIND: This method sets the filetime to that of the archive entry!
	 *
	 * @param dir The destination directory to which to extract.
	 * @param entries The entries to extract, all entries when null.
	 * @return list of File that have been extracted.
	 */
	public Hashtable extractEntries(File dir, SelectiveTarEntry [] entries) throws
		IOException,
		InvalidHeaderException
	{
		int cap = entries != null ? entries.length : 100;
		float load = entries != null ? 1.0f : 0.75f;
		Hashtable h = new Hashtable(cap, load);
		
		while (true)	{
			TarEntry e = this.tarIn.getNextEntry();

			if (e == null)	{	// we are at EOF
				break;
			}
			else
			if (!canceled())	{	// always read all entries, do not return when canceled
				boolean found = (entries == null);	// true if all entries
				for (int i = 0; found == false && i < entries.length; i++)
					if (entries[i].equals(e))
						found = true;
						
				if (found)	{  // no links in Java
	                if (e.getHeader().linkName == null || e.getHeader().linkName.length() <= 0)  {  // no links in Java
    					try	{
    						File created = super.extractEntry(dir, e);
    						created.setLastModified(e.getModTime().getTime());
    						//System.err.println("Extracted "+e.getName()+" from TAR to: "+created);
    						
    						h.put(e.getName(), created);
    					}
    					catch (FileNotFoundException ex)	{
    						if (errors == null)
    							errors = new StringBuffer();
    							
    						errors.append(ex.toString()+OS.newline());
    						
    						errorProgress(e.getSize());
    					}
                    }
	                else {
	                    System.err.println("Did not extract link: "+e.getName()+" -> "+e.getHeader().linkName);
	                }
				}	// end if found
			}	// end if e != null
		}	// end while

		return h;
	}

	protected String checkFilename(String name)	{
		return ValidFilename.correctFilename(name);
	}
	
	protected String checkDirname(String name)	{
		return name;
	}
			
	/**
		Returns always false. Made to be overridden for an cancelable extraction.
	*/
	protected boolean canceled()	{
		return false;
	}

	/**
		Does nothing. Made to be overridden for progress observer.
	*/
	protected void errorProgress(long size)	{
	}
}