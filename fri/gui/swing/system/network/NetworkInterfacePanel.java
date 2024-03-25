package fri.gui.swing.system.network;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.net.*;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import fri.gui.swing.tree.TreeExpander;
import fri.gui.swing.treetable.*;
import fri.util.error.Err;

/**
 * Panel that renders network information in a treetable (available since Java 1.4).
 * 
 * @author Fritz Ritzberger
 * Created on 15.01.2006
 */
public class NetworkInterfacePanel extends JPanel
{
	public NetworkInterfacePanel()	{
		super(new BorderLayout());

		TreeTableModel model = new NetworkInterfaceTreeTableModel(new NetworkInterfaceTreeNode());
		final JTreeTable treetable = new JTreeTable(model);
		initColumnWidth(treetable);
		TreeExpander.expandAllBranches(treetable.getTree());
		// get column label as tooltip text
		treetable.setDefaultRenderer(String.class, new DefaultTableCellRenderer()	{
			private int column;
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				this.column = column;
				setHorizontalAlignment(column > NetworkInterfaceTreeTableModel.IPADDRESS_COLUMN ? SwingConstants.CENTER : SwingConstants.LEFT);
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
			public String getToolTipText(MouseEvent e)	{
				return NetworkInterfaceTreeTableModel.cNames[column];
			}
			
		});
		
		add(new JScrollPane(treetable), BorderLayout.CENTER);
	}
	
	private void initColumnWidth(JTreeTable table)	{
		TableColumnModel m = table.getColumnModel();
		for (int i = 0; i < m.getColumnCount(); i++)	{
			if (i == NetworkInterfaceTreeTableModel.NAME_COLUMN)
				m.getColumn(i).setPreferredWidth(180);
			else
			if (i == NetworkInterfaceTreeTableModel.IPADDRESS_COLUMN)
				m.getColumn(i).setPreferredWidth(180);
			else
				m.getColumn(i).setPreferredWidth(40);
		}
	}
	
	private void showError(Throwable e)	{
		Err.error(e);
	}


	private class NetworkInterfaceTreeNode extends DefaultMutableTreeNode
	{
		private boolean listed = false;
		
		public NetworkInterfaceTreeNode()	{
			super(null, true);
			try	{
				setUserObject(InetAddress.getLocalHost().getHostName());
			}
			catch (UnknownHostException e)	{
				showError(e);
			}
		}
		
		protected NetworkInterfaceTreeNode(NetworkInterface intf)	{
			super(intf, true);
		}
		
		protected NetworkInterfaceTreeNode(InetAddress address)	{
			super(address, false);
		}
		
		public int getChildCount() {
			list();
			return super.getChildCount();
		}
		
		private void list()	{
			if (listed)
				return;
			
			listed = true;
			
			children = new Vector();
			Object o = getUserObject();
			
			if (o instanceof NetworkInterface)	{	// level 1
				Enumeration e = ((NetworkInterface) o).getInetAddresses();
				while (e.hasMoreElements())	{
					InetAddress address = (InetAddress) e.nextElement();
					NetworkInterfaceTreeNode newChild = new NetworkInterfaceTreeNode(address);
					newChild.setParent(this);
					children.add(newChild);
				}
			}
			else
			if (o instanceof String)	{	// root, level 0
				try	{
					Enumeration e = NetworkInterface.getNetworkInterfaces();
					while (e.hasMoreElements())	{
						NetworkInterface netface = (NetworkInterface) e.nextElement();
						NetworkInterfaceTreeNode newChild = new NetworkInterfaceTreeNode(netface);
						newChild.setParent(this);
						children.add(newChild);
					}
				}
				catch (Throwable e)	{
					showError(e);
				}
			}
		}
		
		public String toString()	{
			Object o = getColumnObject(0);
			return o == null ? "" : o.toString();
		}
		
		public Object getColumnObject(int col)	{
			Object o = getUserObject();
			
			if (o instanceof InetAddress)	{
				InetAddress addr = (InetAddress) o;
				switch (col)	{
					case NetworkInterfaceTreeTableModel.NAME_COLUMN:
						return addr.getHostName();
					case NetworkInterfaceTreeTableModel.IPADDRESS_COLUMN:
						return addr.getHostAddress();
					case NetworkInterfaceTreeTableModel.IS_LOOPBACK_COLUMN:
						try	{ return toBoolean(addr.isLoopbackAddress()); } catch (Error e)	{}	// Java 1.4 only
					case NetworkInterfaceTreeTableModel.IS_LINKLOCAL_COLUMN:
						try	{ return toBoolean(addr.isLinkLocalAddress()); } catch (Error e)	{}	// Java 1.4 only
					/*
					case NetworkInterfaceTreeTableModel.IS_MULTICAST_COLUMN:
						return toBoolean(addr.isMulticastAddress());
					case NetworkInterfaceTreeTableModel.IS_ANYLOCAL_COLUMN:
						return toBoolean(addr.isAnyLocalAddress());
					case NetworkInterfaceTreeTableModel.IS_SITELOCAL_COLUMN:
						return toBoolean(addr.isSiteLocalAddress());
					case NetworkInterfaceTreeTableModel.IS_MULTICAST_GLOBAL_COLUMN:
						return toBoolean(addr.isMCGlobal());
					case NetworkInterfaceTreeTableModel.IS_MULTICAST_ORGLOCAL_COLUMN:
						return toBoolean(addr.isMCOrgLocal());
					case NetworkInterfaceTreeTableModel.IS_MULTICAST_SITELOCAL_COLUMN:
						return toBoolean(addr.isMCSiteLocal());
					case NetworkInterfaceTreeTableModel.IS_MULTICAST_NODELOCAL_COLUMN:
						return toBoolean(addr.isMCNodeLocal());
					*/
					default:
						throw new IllegalStateException();
				}
			}
			else
			if (o instanceof NetworkInterface)	{
				NetworkInterface netface = (NetworkInterface) o;
				switch (col)	{
					case NetworkInterfaceTreeTableModel.NAME_COLUMN:
						return netface.getName();
				}
			}
			else
			if (o instanceof String)	{
				switch (col)	{
					case NetworkInterfaceTreeTableModel.NAME_COLUMN:
						return (String) o;
					case NetworkInterfaceTreeTableModel.IPADDRESS_COLUMN:
						try	{
							return InetAddress.getLocalHost().getHostAddress();
						}
						catch (UnknownHostException e)	{
							showError(e);
						}
				}
			}
			return null;
		}
		
		private String toBoolean(boolean b)	{
			return b ? " X" : "";
		}

	}


	// the data model for treetable

	static class NetworkInterfaceTreeTableModel extends AbstractTreeTableModel
	{
		public static final int NAME_COLUMN = 0;
		public static final String NAME_COLUMN_STRING = "Name";
		public static final int IPADDRESS_COLUMN = 1;
		public static final String IPADDRESS_COLUMN_STRING = "IP Address";
		public static final int IS_LOOPBACK_COLUMN = 2;
		public static final String IS_LOOPBACK_COLUMN_STRING = "Loopback";
		public static final int IS_LINKLOCAL_COLUMN = 3;
		public static final String IS_LINKLOCAL_COLUMN_STRING = "LinkLocal";
		/*
		public static final int IS_MULTICAST_COLUMN = 4;
		public static final String IS_MULITCAST_COLUMN_STRING = "Multicast (MC)";
		public static final int IS_ANYLOCAL_COLUMN = 5;
		public static final String IS_ANYLOCAL_COLUMN_STRING = "AnyLocal (Wildcard)";
		public static final int IS_SITELOCAL_COLUMN = 6;
		public static final String IS_SITELOCAL_COLUMN_STRING = "SiteLocal";
		public static final int IS_MULTICAST_GLOBAL_COLUMN = 7;
		public static final String IS_MULTICAST_GLOBAL_COLUMN_STRING = "MCGlobal";
		public static final int IS_MULTICAST_ORGLOCAL_COLUMN = 8;
		public static final String IS_MULTICAST_ORGLOCAL_COLUMN_STRING = "MCOrgLocal";
		public static final int IS_MULTICAST_SITELOCAL_COLUMN = 9;
		public static final String IS_MULTICAST_SITELOCAL_COLUMN_STRING = "MCSiteLocal";
		public static final int IS_MULTICAST_NODELOCAL_COLUMN = 10;
		public static final String IS_MULTICAST_NODELOCAL_COLUMN_STRING = "MCNodeLocal";
		*/
		static String [] cNames = {
			NAME_COLUMN_STRING,
			IPADDRESS_COLUMN_STRING,
			IS_LOOPBACK_COLUMN_STRING,
			IS_LINKLOCAL_COLUMN_STRING,
			/*
			IS_MULITCAST_COLUMN_STRING,
			IS_ANYLOCAL_COLUMN_STRING,
			IS_SITELOCAL_COLUMN_STRING,
			IS_MULTICAST_GLOBAL_COLUMN_STRING,
			IS_MULTICAST_ORGLOCAL_COLUMN_STRING,
			IS_MULTICAST_SITELOCAL_COLUMN_STRING,
			IS_MULTICAST_NODELOCAL_COLUMN_STRING,
			*/
		};
		private static Class[]	cTypes = {
			TreeTableModel.class,
			String.class,
			String.class,
			String.class,
			/*
			String.class,
			String.class,
			String.class,
			String.class,
			String.class,
			String.class,
			String.class,
			*/
		};

		public NetworkInterfaceTreeTableModel(Object o) {
			super((TreeNode)o);
		}

		public int getColumnCount() {
			return cNames.length;
		}
		public String getColumnName(int column) {
			return cNames[column];
		}
		public Class getColumnClass(int column) {
			return cTypes[column];
		}
		public Object getValueAt(Object node, int column) {
			NetworkInterfaceTreeNode dn = (NetworkInterfaceTreeNode) node;
			return dn.getColumnObject(column);
		}
		public void setValueAt(Object aValue, Object node, int column) {
		}
		public boolean isCellEditable(Object node, int column) {
			return false;
		}

	}

	
	

	public static void main(String [] args)	{
		JFrame f = new JFrame();
		f.getContentPane().add(new NetworkInterfacePanel());
		f.setSize(600, 400);
		f.setVisible(true);
	}

}
