package fri.gui.swing.encoding;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import fri.util.collections.Tuple;
import fri.util.sort.quick.*;
import fri.util.text.encoding.Encodings;
import fri.gui.swing.tree.CustomJTree;

/**
	Lets choose an character encoding.
*/

public class EncodingChooser implements TreeSelectionListener
{
	private static final String DEFAULT_ENCODING = "System-Default";
	private JTree tree;
	private JTextField encoding;
	private boolean canceled;


	public EncodingChooser(Component parent)	{
		this(parent, null);
	}
	
	public EncodingChooser(Component parent, String currentEncoding)	{
		tree = new CustomJTree()	{
			public String getToolTipText(MouseEvent e)	{
				if (getRowForLocation(e.getX(), e.getY()) < 0)
					return null;
				TreePath curPath = getPathForLocation(e.getX(), e.getY());
				DefaultMutableTreeNode d = (DefaultMutableTreeNode)curPath.getLastPathComponent();
				if (isAlias(d))
					return "Alias For "+d.getParent();
				if (d.toString().equals(DEFAULT_ENCODING))
					return Encodings.defaultEncoding;
				return d.toString();
			}
		};
		tree.setModel(createEncodingsModel());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.getSelectionModel().addTreeSelectionListener(this);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		
		JScrollPane sp = new JScrollPane(tree);
		JPanel p = new JPanel(new BorderLayout());
		encoding = new JTextField();
		encoding.setEditable(false);
		encoding.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(new JLabel("Selected Encoding: "), BorderLayout.WEST);
		p1.add(encoding, BorderLayout.CENTER);
		
		p.add(sp, BorderLayout.CENTER);
		p.add(p1, BorderLayout.NORTH);

		// select current encoding
		EncodingTreeNode root = (EncodingTreeNode)tree.getModel().getRoot();
		searchAndSelectEncoding(currentEncoding, root);

		JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dlg = pane.createDialog(parent, "Choose Encoding");
		dlg.setResizable(true);
		dlg.setVisible(true);
		Object retObj = pane.getValue();
		int ret = retObj != null ? ((Integer)retObj).intValue() : JOptionPane.OK_OPTION + 1;
		
		canceled = (ret != JOptionPane.OK_OPTION);
	}
	
	/** Returns the selected encoding after dialog has been finished. */
	public String getEncoding()	{
		String s = encoding.getText();
		return s.length() <= 0 || s.equals(DEFAULT_ENCODING) ? null : s;
	}
	
	/** Returns true if no valid encoding was selected or the dialog was canceled. */
	public boolean wasCanceled()	{
		return canceled || getEncoding() == null && encoding.getText().equals(DEFAULT_ENCODING) == false;
	}
	
	
	/** Implements TreeSelectionListener to catch selected encoding. */
	public void valueChanged(TreeSelectionEvent e)	{
		TreePath tp = tree.getSelectionPath();
		EncodingTreeNode n = tp != null ? (EncodingTreeNode)tp.getLastPathComponent() : null;
		if (n != null && (n.getLevel() > 1 || n.toString().equals(DEFAULT_ENCODING)))	{
			if (isAlias(n))	// is an alias
				n = (EncodingTreeNode)n.getParent();
			encoding.setText(n.toString());
		}
		else	{
			encoding.setText("");
		}
	}

	private boolean isAlias(DefaultMutableTreeNode d)	{
		return d.getLevel() > 2 && d.isLeaf();
	}
	
	private TreeModel createEncodingsModel()	{
		Map encodings = Encodings.map;
		Set set = encodings.keySet();
		
		Vector ibm = new Vector();
		Vector windows = new Vector();
		Vector iso = new Vector();
		Vector others = new Vector();
		
		for (Iterator it = set.iterator(); it.hasNext(); )	{
			String encoding = (String)it.next();
			String [] aliases = (String []) encodings.get(encoding);
			Tuple tupel = new SortableTuple(encoding, aliases);
			
			if (encoding.startsWith("ISO"))	{
				iso.add(tupel);
			}
			else
			if (encoding.startsWith("MS"))	{
				windows.add(tupel);
			}
			else
			if (encoding.startsWith("Cp"))	{
				if (encoding.startsWith("Cp125"))	{	// CP 1250 - 1258 is windows
					windows.add(tupel);
				}
				else	{	// is IBM
					ibm.add(tupel);
				}
			}
			else	{	// others
				others.add(tupel);
			}
		}

		iso = new QSort().sort(iso);
		ibm = new QSort().sort(ibm);
		windows = new QSort().sort(windows);
		others = new QSort().sort(others);
		
		Vector allEncodings = new Vector();
		allEncodings.add(DEFAULT_ENCODING);
		allEncodings.add(new SortableTuple("ISO", iso));
		allEncodings.add(new SortableTuple("IBM", ibm));
		allEncodings.add(new SortableTuple("Microsoft", windows));
		allEncodings.add(new SortableTuple("Others", others));
		
		DefaultMutableTreeNode root = new EncodingTreeNode(new SortableTuple("Encodings", allEncodings));
		DefaultTreeModel model = new DefaultTreeModel(root);
		
		return model;
	}


	private void searchAndSelectEncoding(String enc, DefaultMutableTreeNode node)	{
		for (int i = 0; i < node.getChildCount(); i++)	{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode)node.getChildAt(i);
			
			if (enc == null && n.toString().equals(DEFAULT_ENCODING) || enc != null && n.toString().equals(enc))	{
				TreePath tp = new TreePath(n.getPath());
				tree.setSelectionPath(tp);
				tree.scrollPathToVisible(tp);
				return;
			}
			else	{
				searchAndSelectEncoding(enc, n);
			}
		}
	}




	private class SortableTuple extends Tuple
	{
		SortableTuple(String o1, Object o2)	{
			super(o1, o2);
		}
		
		public String toString()	{
			return obj1.toString();
		}
	}
	

	private class EncodingTreeNode extends DefaultMutableTreeNode
	{
		private boolean listed;
		
		EncodingTreeNode(Tuple tupel)	{
			super(tupel);
		}

		EncodingTreeNode(String s)	{
			super(s);
		}
		
		public int getChildCount()	{
			if (listed == false)	{
				listed = true;
				Object o = getUserObject();
				
				if (o instanceof Tuple)	{
					Object chldr = ((Tuple)o).obj2;
					
					if (chldr instanceof Vector)	{
						Vector v = (Vector)chldr;
						for (int i = 0; i < v.size(); i++)	{
							Object elem = v.get(i);
							if (elem instanceof Tuple)
								insert(new EncodingTreeNode((Tuple)elem), i);
							else	// instanceof String: DEFAULT_ENCODING
								insert(new EncodingTreeNode((String)elem), i);
						}
					}
					else
					if (chldr instanceof String[])	{
						String [] array = (String[]) chldr;
						for (int i = 0; i < array.length; i++)	{
							String s = array[i];
							insert(new EncodingTreeNode(s), i);
						}
					}
				}
			}
			return super.getChildCount();
		}
	}



	public static void main(String [] args)	{
		EncodingChooser enc = new EncodingChooser(new JFrame(), "Cp850");
		System.err.println("Selected encoding was: "+enc.getEncoding()+", canceled "+enc.wasCanceled());
	}

}