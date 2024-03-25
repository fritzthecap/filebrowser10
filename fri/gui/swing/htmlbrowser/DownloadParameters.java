package fri.gui.swing.htmlbrowser;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import fri.util.props.*;
import fri.gui.CursorUtil;
import fri.gui.swing.propdialog.*;
import fri.gui.swing.spinnumberfield.*;

/**
	Festlegen der Download-Parameter:
	Links verfolgen ja/nein, Mime-Typen (einzeln ein- und ausschaltbar), u.a. Optionen.
	Starten des Downloads.
*/

public class DownloadParameters extends BoolPropEditDialog
{
	private static Properties mimeTypes = null, tooltipProps = null;
	private static JFileChooser fc = null;
	private String [] notMimeTypes = null;
	private JCheckBox followLinks, takeAll, convertURLs, onlyWithinSite, belowDocument;
	private SpinNumberField todoLimit, depth;
	private JTextField directory;
	private JButton choose;
	
	
	public static Properties createProperties()	{
		if (mimeTypes == null)	{
			mimeTypes = ClassProperties.getProperties(mimetypes.class);
			tooltipProps = ClassProperties.getProperties(mimenames.class);
		}
		return mimeTypes;
	}
	
	
	public DownloadParameters(JFrame frame)	{
		super(frame, true, createProperties(), "HTML Download - Options");
	}
	
	protected JTable createTable(TableModel tm)	{
		return new JTable(tm)	{
			public String getToolTipText(MouseEvent e)	{
				int row = rowAtPoint(e.getPoint());
				if (row >= 0)	{
					int index = row;	//sorter.convertRowToModel(row);
					String mimeType = (String)getModel().getValueAt(index, 0);
					if (mimeType != null && mimeType.length() > 0)	{
						String tooltip = tooltipProps.getProperty(mimeType);
						return tooltip;
					}
				}
				return "";
			}
		};
	}
	
	
	protected Container buildGUI()	{
		Container c = super.buildGUI();
		c.add(buildDownloadParametersPanel(), BorderLayout.NORTH);
		return c;
	}
	
	private Component buildDownloadParametersPanel()	{
		JPanel p = new JPanel(new BorderLayout());

		String s = ClassProperties.get(getClass(), "followLinks");
		followLinks = new JCheckBox("Include Hyperlinked Documents", s == null || s.equals("true"));

		s = ClassProperties.get(getClass(), "takeAll");
		takeAll = new JCheckBox("All Mime Types", s != null && s.equals("true"));
		takeAll.addActionListener(this);
		switchTable(!takeAll.isSelected());

		s = ClassProperties.get(getClass(), "convertURLs");
		convertURLs = new JCheckBox("Convert Links To Relative", s == null || s.equals("true"));

		s = ClassProperties.get(getClass(), "onlyWithinSite");
		onlyWithinSite = new JCheckBox("Only Files On Same Site", s == null || s.equals("true"));

		s = ClassProperties.get(getClass(), "belowDocument");
		belowDocument = new JCheckBox("Only Files Below Document", s != null && s.equals("true"));
		actionPerformed(new ActionEvent(belowDocument, ActionEvent.ACTION_PERFORMED, ""));
		belowDocument.addActionListener(this);

		s = ClassProperties.get(getClass(), "todoLimit");
		todoLimit = new SpinNumberField("Maximum Files");
		todoLimit.setRange(0, 1000000);
		if (s != null)
			try	{ todoLimit.setValue(Integer.valueOf(s).intValue()); } catch (Exception e)	{}
		else
			todoLimit.clear();		

		s = ClassProperties.get(getClass(), "depth");
		depth = new SpinNumberField("Maximum Link Depth");
		depth.setRange(0, 1000);
		if (s != null)
			try	{ depth.setValue(Integer.valueOf(s).intValue()); } catch (Exception e)	{}
		else
			depth.clear();

		directory = new JTextField(new File(DownloadDialog.downloadDir).getAbsolutePath());
		directory.addActionListener(this);
		choose = new JButton("Target Directory");
		choose.addActionListener(this);

		JPanel p1 = new JPanel(new GridLayout(3, 2));
		p1.add(followLinks);
		p1.add(onlyWithinSite);
		p1.add(convertURLs);
		p1.add(belowDocument);
		p1.add(todoLimit);
		p1.add(depth);
		
		p.add(p1, BorderLayout.NORTH);
		p.add(choose, BorderLayout.WEST);
		p.add(directory, BorderLayout.CENTER);
		p.add(takeAll, BorderLayout.SOUTH);
		
		return p;
	}
	
	public void actionPerformed(ActionEvent e)	{
		super.actionPerformed(e);
		
		if (e.getSource() == choose)	{
			CursorUtil.setWaitCursor(frame);
			int ret = JFileChooser.CANCEL_OPTION;
			try	{
				if (fc == null)	{
					fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fc.setCurrentDirectory(new File(new File(DownloadDialog.downloadDir).getParent()));
					fc.setSelectedFile(new File(DownloadDialog.downloadDir));
				}
				ret = fc.showOpenDialog(frame);
			}
			finally	{
				CursorUtil.resetWaitCursor(frame);
			}
			
			if (ret == JFileChooser.APPROVE_OPTION) {
				File dir = fc.getSelectedFile();
				directory.setText(dir.getAbsolutePath());
			}
		}
		else
		if (e.getSource() == takeAll)	{
			commitTable();
			switchTable(false == takeAll.isSelected());
		}
		else
		if (e.getSource() == belowDocument)	{
			boolean b = belowDocument.isSelected();
			if (b)
				onlyWithinSite.setSelected(b);
			onlyWithinSite.setEnabled(!b);
		}
	}
	

	private void switchTable(final boolean on)	{
		SwingUtilities.invokeLater(new Runnable()	{
			public void run()	{
				table.setEnabled(on);
				((Component)table.getDefaultRenderer(String.class)).setEnabled(on);
				((Component)table.getDefaultRenderer(Boolean.class)).setEnabled(on);
			}
		});
	}

	protected JScrollPane buildPanel()	{
		JScrollPane sp = super.buildPanel();
		sp.setBorder(BorderFactory.createTitledBorder("Select Mime Types"));
		return sp;
	}

	protected String getColumn1Name()	{
		return "Mime Type";
	}
	protected String getColumn2Name()	{
		return "Download";
	}

	protected void close()	{
		System.err.println("closing DownloadParameters Dialog");
		ClassProperties.put(getClass(), "followLinks", followLinks.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "takeAll", takeAll.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "convertURLs", convertURLs.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "onlyWithinSite", onlyWithinSite.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "belowDocument", belowDocument.isSelected() ? "true" : "false");

		if (depth.getValue() > 0)
			ClassProperties.put(getClass(), "depth", Integer.toString((int)depth.getValue()));
		else
			ClassProperties.remove(getClass(), "depth");

		if (todoLimit.getValue() > 0)
			ClassProperties.put(getClass(), "todoLimit", Integer.toString((int)todoLimit.getValue()));
		else
			ClassProperties.remove(getClass(), "todoLimit");

		super.close();
	}



	public String getDirectory()	{
		return directory.getText();
	}		

	public void storeToProperties()	{
		super.storeToProperties();
		ClassProperties.store(mimetypes.class);
		
		if (directory.getText().equals("") == false)
			DownloadDialog.storeDownloadDir(directory.getText());

		if (takeAll.isSelected() == false)	{
			Vector m = new Vector();		
			for (int i = 0; i < values.size(); i++)	{
				Vector v = (Vector)values.elementAt(i);
				String name = ((String)v.elementAt(0)).trim();
				Boolean value = (Boolean)v.elementAt(1);
	
				if (name.length() > 0)	{
					if (value.booleanValue() == false)	{
						m.add(name);
					}
				}
			}		
			if (m.size() > 0)	{
				notMimeTypes = new String [m.size()];
				m.copyInto(notMimeTypes);
			}
		}
		close();		
	}

	
	public String [] getNotMimeTypes()	{
		return notMimeTypes;	// was made by OK-Button
	}
	
	public boolean getFollowLinks()	{
		return followLinks.isSelected();
	}

	public boolean getBelowDocument()	{
		return belowDocument.isSelected();
	}

	public boolean getOnlyWithinSite()	{
		return onlyWithinSite.isSelected();
	}

	public boolean getConvertURLs()	{
		return convertURLs.isSelected();
	}

	public int getTodoLimit()	{
		int t = (int)todoLimit.getValue();
		System.err.println("todo limit "+t);
		return t;
	}

	public int getDepth()	{
		int d = (int)depth.getValue();
		System.err.println("link depth "+d);
		return d;
	}



	/** test main
	public static void main(String [] args)	{
		DownloadParameters dlg = new DownloadParameters(new JFrame());
		dlg.show();
		System.err.println("-----------------------------------");
		dlg.props.list(System.err);
		System.err.println("-----------------------------------");
		mimeTypes.list(System.err);
		System.err.println("-----------------------------------");
	}
	*/
}


/** Marker class for property file */
class mimetypes
{
}

/** Marker class for property file */
class mimenames
{
}