package fri.util.diff;

import java.io.*;
import java.util.Vector;
import gnu.regexp.*;
import fri.util.regexp.*;
import fri.util.observer.CancelProgressObserver;
import fri.util.os.OS;

/**
	Wrapper for File objects when comparing directories recursive.
	Overrides <i>equals()</i> to return relative paths,
	overrides <i>toString()</i> to render filename without path.
	Implements additional comparison methods like <i>equalsContent()</i>
	and <i>equalsCompound()</i> to compare files with same relative path.
	<p>
	This is *NOT* made to be passed to Diff class, as
	files in analogous directories may have same pathes but maybe
	different contents!
	<p>
	Usage:
	<pre>
		File f1 = new File("a/b/c/");	// directory 1
		File f2 = new File("x/y/");	// directory 2
		DiffFileTree tdw1 = new DiffFileTree(f1, true, false, "*.java", true);
		DiffFileTree tdw2 = new DiffFileTree(f2, true, false, "*.java", true);
		// perform recursive comparison
		boolean isEqual = tdw1.equalsCompound(tdw2);
	</pre>
*/

public class DiffFileTree
{
	private File file;
	private boolean insertedAndDeletedMustMatchPattern = true;	// if true: show only *.java, not a deleted XXX.gif file
	private boolean ignoreSpaces, exceptLeading;
	private String name;
	private RE [] expr;
	private boolean include;
	private String changeFlag = null;
	private Vector children = null;
	private CancelProgressObserver observer;

	
	/** Root constructor, considers case sensitivity of platform. */
	public DiffFileTree(
		File file,
		boolean ignoreSpaces,
		boolean exceptLeading,
		String pattern,
		boolean include)
	throws
		REException
	{
		this(file, ignoreSpaces, exceptLeading, (RE[])null, include);
		
		if (pattern != null && !pattern.equals("") && !pattern.equals("*"))	{
			initPatternExpressions(pattern);
		}
	}
	
	/** Empty useless construcor for filler element, internal formatting. */
	public DiffFileTree(boolean isDirectory)	{
		if (isDirectory)
			children = new Vector();
	}
	
	/** Internal child constructor. */
	private DiffFileTree(
		File file,
		boolean ignoreSpaces,
		boolean exceptLeading,
		RE [] expr,
		boolean include)
	{
		this.file = file;
		this.ignoreSpaces = ignoreSpaces;
		this.exceptLeading = exceptLeading;
		this.expr = expr;
		this.include = include;
		
		name = file.getName();
		if (OS.supportsCaseSensitiveFiles() == false)
			name = name.toLowerCase();
	}
	
	
	// compile and buffer regular expression for filenames
	private void initPatternExpressions(String pattern)
		throws REException
	{
		int compFlags = OS.supportsCaseSensitiveFiles() ? 0 : RE.REG_ICASE;
		pattern = RegExpUtil.setDefaultWildcards(pattern);
		RESyntax syntax = Syntaxes.getSyntax("PERL5");
		
		if (RegExpUtil.isMoreThanOnePattern(pattern))	{
			String [] stok = RegExpUtil.parseAlternation(pattern);
			expr = new RE [stok.length];
			
			for (int i = 0; i < stok.length; i++)	{
				expr[i] = new RE(stok[i], compFlags, syntax);
			}
		}
		else	{
			expr = new RE [1];
			expr[0] = new RE(pattern, compFlags, syntax);
		}
	}

	private boolean match(File f)	{
		if (f.isDirectory())
			return true;	// take all directories
			
		if (expr == null)
			return include;

		for (int i = 0; i < expr.length; i++)	{
			if (expr[i].isMatch(f.getName()))
				return include;
		}
		return !include;
	}



	public File getFile()	{
		return file;
	}
	
	public String getChangeFlag()	{
		return changeFlag;
	}

	public void setChangeFlag(String changeFlag)	{
		this.changeFlag = changeFlag;
	}
	
	
	public void setChildren(Vector formattedChildren)	{
		this.children = formattedChildren;
	}
	
	public Vector getChildren()	{
		if (children == null && getFile() != null && getFile().isDirectory())	{
			children = new Vector();
			String [] list = getFile().list();

			for (int i = 0; i < list.length; i++)	{
				if (observer != null && observer.canceled())
					return new Vector();
				
				File f = new File(getFile(), list[i]);
				
				if (insertedAndDeletedMustMatchPattern == false || match(f))	{
					DiffFileTree dft = new DiffFileTree(f, ignoreSpaces, exceptLeading, expr, include);
					dft.observer = observer;
					children.add(dft);
				}
			}
		}
		
		return children;
	}
	

	
	/** Return the filename to render. */
	public String toString()	{
		return name != null ? name : "";
	}
	


	/**
		Recursive comparison of files and directories.
	*/
	public boolean equalsCompound(DiffFileTree dft)	{
		return equalsCompound(dft, null);
	}

	/**
		Recursive comparison of files and directories.
	*/
	public boolean equalsCompound(DiffFileTree dft, CancelProgressObserver observer)	{
		//System.err.println("equalsCompound	"+this+" to "+dft+", observer "+observer);
		this.observer = observer;
		dft.observer = observer;
		
		boolean equal = equals(dft);	// simple comparison
		boolean equalContent = equalsContent(dft);	// perform recursion
		
		if (observer != null)	{
			observer.endDialog();
		}
		
		return equal && equalContent;
	}

	
	/**
		Compare the relative path.
		Compare directories with directories and files with files.
	*/
	public boolean equals(Object o)	{
		DiffFileTree dft = (DiffFileTree)o;
		return
				(getFile().isDirectory() && dft.getFile().isDirectory() ||
					getFile().isFile() && dft.getFile().isFile()) &&
				dft.toString().equals(toString());
	}

	/**
		Compare the file or directory contents.
		Directory contents are all contained files.
	*/
	private boolean equalsContent(DiffFileTree dft)	{
		boolean equal = true;
		
		if (getFile().isDirectory() && dft.getFile().isDirectory())	{
			equal = equalsDirContents(dft);
		}
		else
		if (getFile().isFile() && dft.getFile().isFile())	{
			equal = equalsFileContent(dft);
		}
		
		if (equal == false)	{
			setChangeFlag(DiffChangeFlags.CHANGED);
			dft.setChangeFlag(DiffChangeFlags.CHANGED);
		}

		return equal;
	}
	

	private boolean equalsDirContents(DiffFileTree dft)	{
		// compare every file of this object with equal file of passed object
		// do CHANGED and INSERTED, seen from this object
		boolean equal1 = equalsDirContents(this, dft);
		// do DELETED, seen from peer object
		boolean equal2 = equalsDirContents(dft, this);
		return equal1 && equal2;
	}

	
	// compare the contents of two directories, do *NOT* break at first difference
	private boolean equalsDirContents(DiffFileTree tdw1, DiffFileTree tdw2)	{
		boolean changed = false;
		boolean isLeft = tdw1 == this;
		
		for (int i = 0; i < tdw1.getChildren().size(); i++)	{
			if (observer != null && observer.canceled())
				return false;

			DiffFileTree thisOne = (DiffFileTree)tdw1.getChildren().get(i);
			int j = tdw2.getChildren().indexOf(thisOne);	// search in other list via equals
			
			if (j >= 0)	{	// element is in common set
				if (isLeft)	{	// is first comparison
					DiffFileTree thatOne = (DiffFileTree)tdw2.getChildren().get(j);
	
					if (thisOne.equalsContent(thatOne) == false)	{
						changed = true;
					}
				}
			}
			else	{	// element is not in common set
				thisOne.setChangeFlag(isLeft ? DiffChangeFlags.DELETED : DiffChangeFlags.INSERTED);
				changed = true;
			}
		}

		return !changed;
	}


	// Compare the contents of two files, break at first difference
	// This does not use the Diff algorithm for performance reasons!
	private boolean equalsFileContent(DiffFileTree dft)	{
		File file1 = getFile();
		File file2 = dft.getFile();
		
		if (match(file1) == false)	{	// file2 is same name in other directory
			return true;
		}

		if (ignoreSpaces == false && file1.length() != file2.length())	{
			return false;
		}
			
		BufferedReader in1 = null, in2 = null;
		
		if (observer != null && observer.canceled() == false)	{
			observer.setNote(file1.getName());
		}
		else	{
			System.err.println("comparing	"+file1+"	"+file2);
		}
		
		try	{
			in1 = new BufferedReader(new FileReader(file1));
			in2 = new BufferedReader(new FileReader(file2));
			boolean holded1 = false, holded2 = false;
			String line1 = null, line2 = null;
			SpaceIgnoringString sis1 = null, sis2 = null;

			while (true)	{
				if (!holded1)
					line1 = in1.readLine();
				if (!holded2)
					line2 = in2.readLine();

				//System.err.println("line1 >"+line1+"< line2 >"+line2+"<");
				if (line1 == null && line2 == null)	// both have ended
					return true;
				
				// not EOF of one of the files
				if (ignoreSpaces == false)	{	// do not ignore spaces
					if (line1 == null || line2 == null || line1.equals(line2) == false)
						return false;
				}
				else	{	// ignore spaces
					if (line1 == null || line2 == null)	{	// one has ended
						if (line1 == null)	{	// EOF file 1
							holded1 = true;	// do not read anymore
							sis1 = new SpaceIgnoringString("", exceptLeading);
							sis2 = new SpaceIgnoringString(line2, exceptLeading);
						}
						else
						if (line2 == null)	{	// EOF file 2
							holded2 = true;
							sis2 = new SpaceIgnoringString("", exceptLeading);
							sis1 = new SpaceIgnoringString(line1, exceptLeading);
						}
					}
					else	{
						sis1 = new SpaceIgnoringString(line1, exceptLeading);
						sis2 = new SpaceIgnoringString(line2, exceptLeading);
					}

					if (sis1.equals(sis2) == false)	{	// trimmed lines are different
						// ignore amount of newlines
						if (sis1.getTrimmed().equals("") && !holded1)	{	// line 1 is newline
							holded2 = true;	// hold peer line 2 which is NOT newline
						}
						else
						if (sis2.getTrimmed().equals("") && !holded2)	{	// line 2 is newline
							holded1 = true;	// hold peer line 1 which is NOT newline
						}
						else	{
							return false;
						}
					}
					else	{	// release newline state
						if (line1 != null)
							holded1 = false;
						if (line2 != null)
							holded2 = false;
					}
				}
				
				if (observer != null && observer.canceled())
					return false;
			}
		}
		catch (IOException e)	{
			e.printStackTrace();
			return false;
		}
		finally	{
			try { in1.close(); } catch (Exception e)	{} 
			try { in2.close(); } catch (Exception e)	{}
		}
	}



	// test main
	public static void main(String [] args)
		throws Exception
	{
		if (args.length != 2)	{
			System.err.println("SYNTAX: java "+DiffFileTree.class.getName()+" directory1 directory2");
		}
		else	{
			DiffFileTree tdw1 = new DiffFileTree(new File(args[0]), true, false, (String)null, true);
			DiffFileTree tdw2 = new DiffFileTree(new File(args[1]), true, false, (String)null, true);
			System.err.println(tdw1.equalsCompound(tdw2));
		}
	}
	
}