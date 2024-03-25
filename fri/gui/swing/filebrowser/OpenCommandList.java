package fri.gui.swing.filebrowser;

import java.util.*;
import java.io.*;
import fri.util.regexp.*;

/**
	Dies ist der Table-Vector fur ein TableModel.
	Es wird die Liste der Kommandos, die bei Doppelklick ausgefuehrt werden,
	verwaltet.
	Bei Konstruktion wird diese Liste vom Dateisystem geladen, oder eine
	Leerzeile erzeugt, falls noch kein Kommando existiert.
*/

public class OpenCommandList extends Vector
{
	public static final int SHORTNAME_COLUMN = 0;
	public static final int PATTERN_COLUMN = 1;
	public static final int COMMAND_COLUMN = 2;
	public static final int PATH_COLUMN = 3;
	public static final int MONITOR_COLUMN = 4;
	public static final int LOOP_COLUMN = 5;
	public static final int TYPE_COLUMN = 6;
	public static final int INVARIANT_COLUMN = 7;
	public static final int COLUMNS = 8;

	private static final Vector cols = new Vector(COLUMNS);
	// initialize data
	static	{	// initialize colums
		cols.addElement("Name");
		cols.addElement("Pattern");
		cols.addElement("Command And Arguments");
		cols.addElement("Working Directory");
		cols.addElement("Monitor");
		cols.addElement("Loop");
		cols.addElement("Type");
		cols.addElement("Precondition");
	}
	private String filename = FileBrowser.configDir+File.separator+"FileCommands.ser";

	private Hashtable environments = new Hashtable();
	
	
	
	/** Laden der Liste mit Default-Namen vom Dateisystem */
	public OpenCommandList()	{
		if (load() == false)	{
			//System.err.println("providing first empty line");
			Vector row = buildRow("", true);
			addElement(row);
		}
	}
	
	
	/**
		Laden einer Liste mit Inhalt einer anderen, clone-Ersatz.
		Verwendet zum temporaeren Erzeugen eines OpenEventTableModel
		im OpenEventLauncher.
	*/
	public OpenCommandList(Vector clone)	{
		setContent(clone);
	}
	
	
	/** Liefert eine Liste (von Listen) mit Kommandos zurueck */
	public Vector getColumns()	{
		return cols;
	}
	
	/** Eine leere Zeile in der Tabelle erzeugen */
	public Vector buildRow(String pattern, boolean isLeaf)	{
		return buildRow(pattern, isLeaf ? "Files" : "Folders");
	}
	
	/** Eine leere Zeile in der Tabelle erzeugen */
	public Vector buildRow(String pattern, String type)	{
		Vector row = new Vector();
		for (int i = 0; i < COLUMNS; i++)	{
			if (i == PATTERN_COLUMN)
				row.addElement(pattern);
			else
			if (i == MONITOR_COLUMN)	// "monitor window"
				row.addElement(new Boolean(true));
			else
			if (i == LOOP_COLUMN)	// "argument loop"
				row.addElement(new Boolean(false));
			else
			if (i == TYPE_COLUMN)	// folder or file
				row.addElement(type);
			else
			if (i == INVARIANT_COLUMN)	// "condition for opening"
				row.addElement(new Boolean(false));
			else
				row.addElement("");
		}
		return row;
	}
	
	/** @return environment for passed index of table-model vector */
	public String [] getEnvironment(int idx)	{
		Object o = environments.get(new Integer(idx));
		if (o == null)
			return null;
		return (String[])o;
	}
	
	/** Put a environment String for passed index of table-model vector */
	public void putEnvironment(int idx, String [] env)	{
		environments.put(new Integer(idx), env);
	}
	
	
	/** @return true if "requested" matches pattern containing "?*[^]" */
	public boolean match(String requested, String pattern)	{
		boolean ret = RegExpUtil.matchAlternation(requested, pattern);
		//System.err.println("matched extended "+requested+" to "+pattern+" "+ret);
		return ret;
	}

	/** Ist die Liste leer? Zeilen ohne Eintraege gelten nicht als gueltig! */
	public boolean isEmpty()	{
		for (int i = 0; i < size(); i++)	{
			Vector v = (Vector)elementAt(i);
			for (int j = 0; j < v.size(); j++)	{
				Object o = v.elementAt(j);
				if (j != TYPE_COLUMN && o != null && o instanceof String)	{
					String s = (String)o;
					if (s.trim().equals("") == false)
						return false;
				}
			}
		}
		//System.err.println("open command list is empty");
		return true;
	}


	/** clone this vector AND its sub.vectors to restore it at cancel */
	public synchronized Object clone() {
		Vector v = new Vector(size());
		for (Enumeration e = elements(); e.hasMoreElements(); )	{
			Vector v1 = (Vector)e.nextElement();
			v.addElement(v1.clone());
		}
		return v;
	}
	
	/** Set another (saved) content as consequence of canceling an edit process */
	public void setContent(Vector old)	{
		removeAllElements();
		for (int i = 0; i < old.size(); i++)	{
			Vector v = (Vector)old.elementAt(i);
			addElement(v);
		}
	}
	
	
	
	
	// Persistenz ueber Serialisierung

	/** Sichern der Kommandos auf das Dateisystem */
	public void save()  {
		if (isEmpty())	{
			File f = new File(filename);
			try	{ f.delete(); } catch (Exception e)	{ System.err.println("FEHLER: "+e); }
			System.err.println("Loesche "+filename);
			return;
		}
		
		System.err.println("save "+size()+" commands to "+filename);

		File pnt = new File(new File(filename).getParent());
		pnt.mkdirs();
		
		try	{
			FileOutputStream out = new FileOutputStream(filename);
			ObjectOutputStream oout = new ObjectOutputStream(out);
			oout.writeObject(this);
			oout.flush();
			oout.close();
		}
		catch (IOException e)	{
			System.err.println("FEHLER: "+e);
		}
	}

	
	// Laden der Kommandos vom Dateisystem
	private boolean load()  {
		try	{
			FileInputStream in = new FileInputStream(filename);
			ObjectInputStream oin = new ObjectInputStream(in);
			OpenCommandList l = (OpenCommandList)oin.readObject();
			oin.close();
			for (Enumeration e = l.elements(); e.hasMoreElements(); )	{
				Vector v = (Vector)e.nextElement();
				addElement(v);
			}
			this.environments = l.environments;
		}
		catch (FileNotFoundException e1)	{
			return false;	// muss nicht existieren, weil vielleicht nie gesichert wurde
		}
		catch (Exception e)	{
			System.err.println("FEHLER: "+e);
			return false;
		}
		return true;
	}

}
