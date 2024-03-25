package fri.gui.swing.actionmanager;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import fri.util.Equals;

/**
	Ein Basisknoten fuer einen dynamisch erzeugten Menu-Tree. 
	Ein Knoten besteht aus dem Text des Menuitems und der der
	Eigenschaft enabled/disabled.
	Unter jedem Knoten koennen beliebig viele andere haengen, da der
	Knoten selbst eine Liste ist.<br>
	Ein Menue-Separator kann durch Einfuegen des Wertes "null" erzeugt
	werden: <code>menuTree.add(null);</code>

	@see #main(String[]) - Eine minimale ActionManager Anwendung.
	@author  Ritzberger Fritz
*/

public class MenuTree extends Vector
{
	private boolean enabled;
	private String label = null;
	private String menuItemSeparator;


	/** Anlegen eines Menu-Items mit enabled = true. */
	public MenuTree(String label)	{
		this(label, true);
	}

	/** Anlegen eines Menu-Items mit uebergebenem enabled-Zustand. */
	public MenuTree(String label, boolean enabled)	{
		this.label = label;
		this.enabled = enabled;
		this.menuItemSeparator = ActionManager.menuItemSeparator;
	}


	/**
		Liefert die erste Ebene des Baumes als array von JMenuItem.
		Unter diesen JMenuItem sind jene von Klasse JMenu, die auch
		noch in andere Items verzweigen.
		Diese Methode wird von FillableManagedAction benutzt.
	*/
	protected JMenuItem [] getAsJMenuItems(FillableManagedAction fa, ActionListener al)	{
		return getAsJMenuItems(fa, al, label);
	}
	
	private JMenuItem [] getAsJMenuItems(
		FillableManagedAction fa,
		ActionListener al,
		String actionCommand)
	{
		JMenuItem [] items = new JMenuItem[size()];
		
		int i = 0;
		for (Enumeration e = elements(); e.hasMoreElements(); i++)	{
			Object o = e.nextElement();
			
			if (o != null && o instanceof MenuTree && ((MenuTree)o).size() > 0)	{
				MenuTree node = (MenuTree)o;
				items[i] = new JMenu(node.toString());
				
				String newActionCommand = FillableManagedAction.catenizeActionCommand(
								actionCommand,
								menuItemSeparator,
								node.toString());

				JMenuItem [] children = node.getAsJMenuItems(fa, al, newActionCommand);
				for (int j = 0; j < children.length; j++)	{
					items[i].add(children[j]);
				}
			}
			else	{
				if (o == null)	{
					items[i] = null;
				}
				else	{
					items[i] = fa.createMenuItem(o.toString(), actionCommand, al);
					if (o instanceof MenuTree)
						items[i].setEnabled(((MenuTree)o).enabled);
				}
			}
		}
		
		return items;
	}

	
	/**
		Liefert das Label des Menuitems. Wird benoetigt fuer generische
		Verwendung beim Erzeugen von MenuItems.
	*/
	public String toString()	{
		return label;
	}
	
	/**
		Liefert true wenn der Knoten und sein ganzer Sub-Tree gleich
		dem uebergebenen ist. Es wird die Struktur und Reihenfolge inklusive
		Separatoren mit equals() verglichen.
	*/
	public boolean equals(Object o)	{
		if (o instanceof MenuTree == false)
			return false;
		
		MenuTree other = (MenuTree)o;
		if (label.equals(other.label) == false ||
				other.enabled != enabled ||
				other.size() != size())
			return false;
		
		for (int i = 0; i < size(); i++)	{
			Object o1 = get(i);
			Object o2 = other.get(i);
			if (Equals.equals(o1, o2) == false)
				return false;
		}
		return true;
	}



	// test main

	/* Enthaelt ActionManager Minimal-Sample (siehe Source-Code).
	public static void main(String [] args)	{
		// The action
		final String actionName = "OpenActionPopup";
		
		JFrame f = new JFrame();
		Container c = f.getContentPane();
		
		// The action's listener
		ActionListener al = new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				System.err.println("actionPerformed "+e.getActionCommand());
			}
		};
		
		// The action's manager
		final ActionManager am = new ActionManager(al);
		am.insertFillableAction(actionName);

		// The action's dynamic changing menu trees
		final MenuTree
				menu1 = new MenuTree(actionName),
				menu2 = new MenuTree(actionName);

		// Building the two different trees
		MenuTree help;

		menu1.add("A");
		menu1.add(help = new MenuTree("a"));
		help.add("aa");
		help.add("aaa");
		
		menu2.add(help = new MenuTree("B"));
		help.add("BB");
		menu2.add(help = new MenuTree("b"));
		help.add(new MenuTree("bb", false));	// disabled

		// A button to switch the action's menue tree dynamically
		JButton b = new JButton("Press to fill/change Action");
		b.setBackground(Color.white);
		b.addActionListener(new ActionListener()	{
			private boolean toggle = false;
			public void actionPerformed(ActionEvent e)	{
				am.fillAction(actionName, toggle ? menu1 : menu2);
				toggle = !toggle;
			}
		});
		c.add(b, BorderLayout.CENTER);
		
		// The action's Container Component
		JToolBar tb = new JToolBar(JToolBar.HORIZONTAL);
		am.visualizeAction(actionName, tb, false);

		// Another action's Container Component
		JMenuBar mb = new JMenuBar();
		JMenu menu = new JMenu("File");
		mb.add(menu);
		am.visualizeAction(actionName, menu, false);
		
		// Add tools around center Component (button).
		f.setJMenuBar(mb);
		c.add(tb, BorderLayout.NORTH);

		f.pack();
		f.setVisible(true);
	}
	*/

}
