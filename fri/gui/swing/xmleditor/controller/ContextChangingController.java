package fri.gui.swing.xmleditor.controller;

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import fri.gui.swing.actionmanager.connector.ActionConnector;
import fri.gui.swing.treetable.JTreeTable;
import fri.gui.swing.treetable.TreeTableSelection;
import fri.gui.swing.xmleditor.model.MutableXmlTreeTableModel;

/**
	The pluggable controller for the XML editor window is able to change
	from one editor window to another. So it is possible to use one controller
	for multiple editor windows.
	>p>
	The controller listens to selection changes in treetable and calls
	<i>setEnabledActions()</i> when the selection changes or another view is set.
	It listens to mouse clicks and opens the action popup on popup trigger.

	@author  Ritzberger Fritz
*/
 
abstract class ContextChangingController extends ActionConnector implements
	ListSelectionListener,
	MouseListener
{
	private JPopupMenu popup;
	protected int recentX = 4, recentY = 4;	// open poup point when key pressed
	private MutableXmlTreeTableModel model;


	/** Create a pluggable controller with its initial view. */
	public ContextChangingController(JTreeTable sensor, XmlTreeTableSelection selection)	{
		super(sensor, selection, null);
		installListeners(sensor);
	}


	protected MutableXmlTreeTableModel getXmlTreeTableModel()	{
		return model;
	}

	/** Drag&Drop must know if the popup is showing. */
	public JPopupMenu getPopup()	{
		return popup;
	}


	/** Called when selection changes. Override to set enabled actions. */
	public abstract void setEnabledActions();


	/**
		Plug another view into controller.
		Call this method when another window is switched to foreground.
	*/
	public void setView(JTreeTable sensor)	{
		uninstallListeners();
		installListeners(sensor);

		setEnabledActions();
	}

	private void uninstallListeners()	{
		if (defaultKeySensor != null)
			((JTreeTable)defaultKeySensor).getSelectionModel().removeListSelectionListener(this);

		this.model = null;

		if (popup != null && defaultKeySensor != null)	{
			defaultKeySensor.removeMouseListener(this);

			if (defaultKeySensor.getParent() != null)
				defaultKeySensor.getParent().removeMouseListener(this);
		}

		changeAllKeyboardSensors(null);
	}

	private void installListeners(JTreeTable sensor)	{
		changeAllKeyboardSensors(sensor);

		((TreeTableSelection)getSelection()).setTreeTable(sensor);

		if (sensor != null)
			this.model = (MutableXmlTreeTableModel)sensor.getTreeTableModel();

		if (defaultKeySensor != null)
			((JTreeTable)defaultKeySensor).getSelectionModel().addListSelectionListener(this);

		if (popup != null && defaultKeySensor != null)	{
			defaultKeySensor.addMouseListener(this);

			if (defaultKeySensor.getParent() != null)
				defaultKeySensor.getParent().addMouseListener(this);
		}
	}


	/** Set a popup menu that contains all actions. */
	public void installPopup(JPopupMenu popup)	{
		uninstallListeners();
		this.popup = popup;
		installListeners((JTreeTable)defaultKeySensor);
	}


	/**
		Call this method when the foreground window closes.
		All listener lists will be cleaned and garbage collection will be enabled.
	*/
	public void closeView(JTreeTable sensor)	{
		if (sensor == defaultKeySensor)	{
			changeAllKeyboardSensors(null);
			uninstallListeners();

			this.defaultKeySensor = null;
			this.model = null;

			setEnabledActions();
		}
	}


	/**
		Implements ListSelectionListener to call <i>setEnabledActions()</i> when
		selection changes in treetable.
	*/
	public void valueChanged(ListSelectionEvent e)	{
		if (e.getValueIsAdjusting())	{
			return;
		}

		setEnabledActions();
	}


	/** implements MouseListener to show popup */
	public void mousePressed (MouseEvent e)	{
		recentX = e.getX();
		recentY = e.getY();

		if (e.isPopupTrigger())	{
			showActionPopup(e);
		}
	}
	/** implements MouseListener to show popup */
	public void mouseReleased (MouseEvent e)	{
		if (e.isPopupTrigger())	{
			showActionPopup(e);
		}
	}
	public void mouseClicked (MouseEvent e)	{ }
	public void mouseEntered (MouseEvent e)	{ }
	public void mouseExited (MouseEvent e)	{ }

	private void showActionPopup(MouseEvent e)	{
		popup.show((Component)e.getSource(), e.getX(), e.getY());
	}

}