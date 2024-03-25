package fri.gui.swing.filebrowser;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
//import fri.util.ArrayUtil;

/**
	Ziel: Verwalten aller geoeffneten Pfade der Treeview.
	Der Pfad mit dem aktuell selektiertem Item wird gesondert gehalten.
	Die Pfade koennen persistent gemacht werden, wobei sie dann
	als lesbares Property-file weggeschrieben werden.
	
	Konverterfunktionen TreePath zu String array
*/

public class PathPersistent implements
	TreeSelectionListener
{
	public final String PATH_SEPARATORS = "|\t/\\";	// all possible separators
	public final String PATH_SEPARATOR = "|";	// preferred one

	/** Variable for browser navigation helpers: show only folders */
	public boolean showfiles = true;
	/** Variable for browser navigation helpers: show hidden files */
	public boolean showhidden = false;

	/** Variable for browser navigation helpers: exclude filter */
	public boolean exclude = false;

	/** Variable for browser navigation helpers: sort-criterium */
	public int sortFlag = NetNode.SORT_DEFAULT;

	
	public int scrollspeed = 0;
	
	public boolean dropmenu = true;
	public boolean refresh = true;
	
	// listening to open, close and selection of pathes */
	private Vector currentSelection = new Vector();

	// variables loaded from persistent data
	private String filename;
	private String root = null;
	
	/* Array of currently selected pathes. */
	private String [][] selected = null;
	
	/* Array of currently open pathes. */
	private String [][] pathes = null;

	private String [] wastebasket = null;
	
	private JTree tree;



	/**
		Konstruktor ohne Lesen aus persistenten Daten, in FileChooser verwendet.
	*/
	public PathPersistent(String filename, String [] selectedPath, boolean showfiles)	{
		pathes = new String [1][];
		pathes[0] = selectedPath;
		selected = new String [1][];
		selected[0] = selectedPath;
		
		this.showfiles = showfiles;
		this.filename = filename;
	}


	/**
		Lesen von persistenten Tree-Pfaden aus einem file mit dem uebergebenen
		Namen. Danach kann getPathes() aufgerufen werden.
		@param filename Name des property-files mit den Pfaden.
	*/
	public PathPersistent(String filename)	{
		loadPropFile(load(this.filename = filename));
	}

	/**
		Nachtraegliches Setzen der Wurzel des Baumes, wenn er nicht
		aus persistenten Daten gelesen wurde.
	*/
	public void setRoot(String root)	{
		//System.err.println("PathPersistent, setRoot "+root);
		this.root = root;
	}

	/** @return Wurzel des Baumes als String */
	public String getRoot()	{
		return root;
	}
	
	/** @return Wastebasket als String-array */
	public String [] getWastebasket()	{
		return wastebasket;
	}

	/**
		Liefert die aus persistenten Daten gelesenen selektierten Pfade zurueck.
		@return array mit allen (persistent) selektierten Pfaden
	*/
	public String [][] getSelected()	{
		return selected;
	}

	/**
		Alle Pfade, die aus persistenten Daten gelesen wurden, zurueckliefern.
		@return array mit allen (persistent) geoeffneten Pfaden
	*/
	public String [][] getPathes()	{
		return pathes;
	}


	/**
		Bekanntgabe des JTree zum Installieren eines Listeners
	*/
	public void setTree(JTree tree)	{
		this.tree = tree;
		tree.addTreeSelectionListener(this);
	}



	private TreePath [] getOpenPathes()	{
		//System.err.println("getOpenPathes");
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
		TreePath tproot = new TreePath(root.getPath());
		Enumeration en = tree.getExpandedDescendants(tproot);
		Vector v = new Vector();
		
		for (; en != null && en.hasMoreElements(); )	{
			TreePath tp = (TreePath)en.nextElement();
			v.addElement(tp);
			//System.err.println("getExpandedDescendants: "+tp);
		}
		
		Vector v1 = new Vector(v.size());
		
		for (int i = 0; i < v.size(); i++)	{
			TreePath tp = (TreePath)v.elementAt(i);
			boolean takeit = true;
			for (int j = 0; j < v.size(); j++)	{
				if (i == j )
					continue;
				
				TreePath tp1 = (TreePath)v.elementAt(j);
				if (isDescendantOf(tp1, tp))	// there is a more specialized
					takeit = false;
			}
			if (takeit)	{
				//System.err.println("taking "+tp); 
				v1.addElement(tp);
			}
		}
		
		int len = v1.size();
		TreePath [] tps = new TreePath [len];		
		v1.copyInto(tps);

		return tps;
	}
	
	
	public String [][] selectedToStringArrays()	{
		//System.err.println("selectedToStringArrays");
		return pathesToStringArrays(currentSelection);
	}

	public String [][] treePathesToStringArrays()	{
		//System.err.println("treePathesToStringArrays");
		//return pathesToStringArrays(openPathes);
		return pathesToStringArrays(getOpenPathes());
	}


	public String [][] pathesToStringArrays(TreePath [] tps)	{
		if (tps == null)
			return null;
		String [][] sarr = new String [tps.length] [];
		for (int i = 0; i < tps.length; i++)	{
			String [] sa = pathToStringArray(tps[i]);
			sarr[i] = sa;
		}
		return sarr;
	}
	
	public String [][] pathesToStringArrays(Vector tp)	{
		TreePath [] tps = new TreePath [tp.size()];
		tp.copyInto(tps);
		return pathesToStringArrays(tps);
	}
	
	private String [] pathToStringArray(TreePath tp)	{
		Object [] oarr = tp.getPath();	// DefaultMutableTreeNodes werden geliefert
		String [] sarr = new String [oarr.length - 1];
		for (int i = 1; i < oarr.length; i++)	{	// leave out root
			//DefaultMutableTreeNode d = (DefaultMutableTreeNode)oarr[i];
			sarr[i - 1] = oarr[i].toString();
		}
		return sarr;
	}


	// Einen TreePath in einen Properties-String umwandeln
	private String treePathToPropertyValue(TreePath tp)	{
		return stringPathToPropertyValue(pathToStringArray(tp));
	}

	// Ein String-array in einen Properties-String umwandeln
	private String stringPathToPropertyValue(String [] sarr)	{
		if (sarr == null)
			return null;
			
		StringBuffer sb = new StringBuffer("");
		for (int i = 0; i < sarr.length; i++)	{	// leave out root
			sb.append(sarr[i]);
			if (i < sarr.length - 1)
				sb.append(PATH_SEPARATOR);
		}
		
		return sb.toString();
	}



	public String [][] getOpenTreeStringsUnderPath(TreePath tp)	{
		return pathesToStringArrays(getOpenTreePathesUnderPath(tp));
	}
	
	public TreePath [] getOpenTreePathesUnderPath(TreePath tpath)	{
		TreePath [] tps = getOpenPathes();
		Vector v = new Vector(tps.length);
		
		for (int t = 0; t < tps.length; t++)	{
			TreePath tp = tps[t];
			if (tp.toString().equals(tpath.toString()) == false && isDescendantOf(tp, tpath))	{
				v.addElement(tp);
			}
		}
		
		if (v.size() <= 0)
			return null;
			
		TreePath [] tp = new TreePath [v.size()];
		v.copyInto(tp);

		return tp;
	}

	
	public String [][] getOpenTreeStringsUnderSelection(boolean rescan)	{
		return pathesToStringArrays(getOpenTreePathesUnderSelection(rescan));
	}
	
	private TreePath [] getOpenTreePathesUnderSelection(boolean rescan)	{
		TreePath [] tps = getOpenPathes();
		Vector v = new Vector(tps.length);
		
		for (int t = 0; t < tps.length; t++)	{
			TreePath tp = tps[t];
			
			for (Enumeration e1 = currentSelection.elements(); e1.hasMoreElements(); )	{
				TreePath tp1 = (TreePath)e1.nextElement();
				
				if (tp1.isDescendant(tp))	{
					v.addElement(tp);
					
					// mark nodes between selected and open path
					int sel = tp1.getPathCount();
					int open = tp.getPathCount();
					
					for (int i = sel - 1; i < open; i++)	{
						BufferedTreeNode d = (BufferedTreeNode)tp.getPathComponent(i);
						
						if (rescan)
							d.setMarkedForReScan(true);
						else
							d.setMarkedForFilter(true);
						//System.err.println("        marked "+d+" for "+(rescan?"rescan":"filter")+" in "+tp);
					}
				}
			}
		}
		
		if (v.size() <= 0)
			return null;
			
		TreePath [] tp = new TreePath [v.size()];
		v.copyInto(tp);
		
		return tp;
	}
	

	public TreePath [] getSelectedTreePathes()	{
		int anz = currentSelection.size();
		if (anz <= 0)
			return null;
			
		TreePath [] tp = new TreePath [anz];
		currentSelection.copyInto(tp);

		return tp;
	}


	/** Speichern aller Pfade und Einstellungen */
	public boolean putPathes(
		String [] wastebasket,
		boolean showfiles,
		boolean showhidden,
		boolean exclude,
		int sortFlag,
		Integer scrollspeed,
		Boolean dropmenu,
		Boolean refresh)
	{
		if (root == null)	{
			System.err.println("FEHLER root muss definiert sein um pathes speichern zu koennen!");
			Thread.dumpStack();
			return false;
		}
		Properties props = new Properties();
		
		props.put("root", root);
		
		TreePath [] tps = getOpenPathes();
		for (int t = 0; t < tps.length; t++)	{
			TreePath tp = tps[t];
			//System.err.println("open path "+tp);
			String s = treePathToPropertyValue(tp);
			if (s != null && s.equals("") == false)	{
				//System.err.println("saving path "+s);
				props.put("path"+t, s);
			}
		}
		
		int i = 0;
		for (Enumeration e = currentSelection.elements(); e.hasMoreElements(); i++)	{
			TreePath tp = (TreePath)e.nextElement();
			String s = treePathToPropertyValue(tp);
			
			if (s != null && s.equals("") == false)	{
				//System.err.println("saving selection "+s);
				props.put("selected"+i, s);
			}
		}

		String w = stringPathToPropertyValue(wastebasket);
		if (w != null && w.equals("") == false)
			props.put("wastebasket", w);
		
		props.put("showfiles", showfiles ? "true" : "false");
		props.put("showhidden", showhidden ? "true" : "false");
		props.put("exclude", exclude ? "true" : "false");
		props.put("sortflag", Integer.toString(sortFlag));
		if (scrollspeed != null)
			props.put("scrollspeed", scrollspeed.toString());
		if (dropmenu != null)
			props.put("dropmenu", dropmenu.booleanValue() ? "true" : "false");
		if (refresh != null)
			props.put("refresh", refresh.booleanValue() ? "true" : "false");
		
		return save(filename, props);
	}


	// Scannen der Properties und konvertieren in String-Arrays
	private boolean loadPropFile(Properties props)	{
		if (props == null)
			return false;
			
		if (props.size() < 2)	{
			fileerror();
			props = null;
			return false;
		}
		
		Enumeration e = props.propertyNames();
		Vector patheS = new Vector(props.size());
		Vector selectS = new Vector(props.size());
		
		for (; e.hasMoreElements(); )	{
			String name = (String) e.nextElement();
			String value = props.getProperty(name);			
			if (value == null)	{
				continue;
			}
			//System.err.println(name+" = "+value);

			String [] sarr;
			
			// recognize single atributes
			if ((sarr = recognizeToken(name, value, "showfiles", " \t")) != null)	{
				showfiles = sarr[0].equals("false") ? false : true;
				continue;
			}
			if ((sarr = recognizeToken(name, value, "showhidden", " \t")) != null)	{
				showhidden = sarr[0].equals("false") ? false : true;
				continue;
			}
			if ((sarr = recognizeToken(name, value, "exclude", " \t")) != null)	{
				exclude = sarr[0].equals("false") ? false : true;
				continue;
			}
			if ((sarr = recognizeToken(name, value, "sortflag", " \t")) != null)	{
				sortFlag = Integer.valueOf(sarr[0]).intValue();
				continue;
			}
			if ((sarr = recognizeToken(name, value, "scrollspeed", " \t")) != null)	{
				scrollspeed = Integer.valueOf(sarr[0]).intValue();
				continue;
			}
			if ((sarr = recognizeToken(name, value, "dropmenu", " \t")) != null)	{
				dropmenu = sarr[0].equals("false") ? false : true;
				continue;
			}
			if ((sarr = recognizeToken(name, value, "refresh", " \t")) != null)	{
				refresh = sarr[0].equals("false") ? false : true;
				continue;
			}
			// recognize root
			if ((sarr = recognizeToken(name, value, "root", " \t")) != null)	{
				root = sarr[0];
				continue;
			}
			// recognize selection
			if ((sarr = recognizeToken(name, value, "selected", PATH_SEPARATORS)) != null)	{
				if (sarr.length <= 0)	{	// empty selection, means root was selected
					sarr = new String [1];
					sarr[0] = new String("");	// dummy entry
				}
				selectS.addElement(sarr);
				continue;
			}
			// read pathes
			if ((sarr = recognizeToken(name, value, "wastebasket", PATH_SEPARATORS)) != null)	{
				wastebasket = sarr;
				continue;
			}			
			// read pathes
			if ((sarr = recognizeToken(name, value, "path", PATH_SEPARATORS)) != null)	{
				patheS.addElement(sarr);
				continue;
			}			
			System.err.println("FEHLER: nicht erkannter Token: "+name);
		}

		if (root == null)	{
			fileerror();
			return false;
		}

		if (patheS.size() > 0)	{
			int i = 0;
			pathes = new String[patheS.size()] [];
			for (Enumeration en = patheS.elements(); en.hasMoreElements(); )	{
				String [] sarr = (String[])en.nextElement();
				pathes[i] = sarr;
				i++;
			}
		}
		if (selectS.size() > 0)	{
			int i = 0;
			selected = new String[selectS.size()] [];
			for (Enumeration en = selectS.elements(); en.hasMoreElements(); )	{
				String [] sarr = (String[])en.nextElement();
				selected[i] = sarr;
				i++;
			}
		}
		
		return true;
	}



	String [] recognizeToken(String name, String value, String token, String separators)	{
		if (name.toLowerCase().startsWith(token))	{
			StringTokenizer stok = new StringTokenizer(value, separators);
			String [] sarr = new String [stok.countTokens()];
			//System.err.print(name+" = ");
			for (int j = 0; stok.hasMoreTokens(); j++)	{
				String next = stok.nextToken();
				sarr[j] = next;
				//System.err.print(next+" ");
			}
			//System.err.println();	
			return sarr;
		}
		return null;
	}


	// Fehlermeldung ueber schlechtes property file
	private void fileerror()	{
		System.err.println("WARNUNG: Ungueltiges property-file: Es muss \"root\" definiert sein");
	}



	// Versuch, Properties aus file zu laden. Auf alle Faelle wird
	// die Membervariable props angelegt.
	private synchronized Properties load(String filename)	{
		Properties props = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream(filename);
		}
		catch (Exception e)	{
      System.err.println("WARNUNG: Nicht gefunden: "+e.getMessage());
			return null;	// Muss nicht sein
		}
		try {
			props.load (in);
		}
		catch (Exception e)	{
			return null;
		}
		try { in.close(); } catch (Exception e) {}
		return props;
	}



	// Speichern der gefuellten properties
	private synchronized boolean save(String filename, Properties props)	{
    FileOutputStream out;
    
    try	{
    	new File(new File(filename).getParent()).mkdirs();
    	
     out = new FileOutputStream(filename);
			props.store (out, "GUI properties for treeview, "+filename);
			// JDK 1.1 props.save (out, "opened pathes for treeview, "+filename);
			// JDK 1.2 props.store (out, "opened pathes for treeview, "+filename);
    }
    catch (Exception e) {
      System.err.println("FEHLER: Sichern "+filename+", "+e.getMessage());
      return false;
    }
    try { out.flush(); out.close(); } catch (Exception e) {}
    
    System.err.println("saved root, open pathes and selection to "+filename);
    return true;
	}


	
	// is first path more specialized than second?
	private boolean isDescendantOf(TreePath tp, TreePath p)	{
		if (tp.getPathCount() < p.getPathCount())
			return false;
		String tps = tp.toString();
		String ps  =  p.toString();
		ps = ps.substring(0, ps.length() - 1);	// cut last character
		//System.err.println("            cut last char off >"+ps+"<");
		if (tps.startsWith(ps))
			return true;
		return false;
	}
	
	/*private void dumpPathes(Vector pathes, String s)	{
		System.err.println(s+" pathes:");
		for (Enumeration e = pathes.elements(); e.hasMoreElements(); )	{
			TreePath tp = (TreePath)e.nextElement();
			System.err.println(" -> "+tp);
		}
	}*/


	// interface TreeSelectionListener

	public void valueChanged(TreeSelectionEvent e)	{
		TreePath [] tp = e.getPaths();
		for (int i = 0; i < tp.length; i++)	{
			if (e.isAddedPath(tp[i]))
				currentSelection.addElement(tp[i]);
			else
				currentSelection.removeElement(tp[i]);
		}
		//dumpPathes(currentSelection, "selected");
	}
}