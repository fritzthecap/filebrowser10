package fri.gui.swing.combo.history;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
	Associate two combo boxes by a property list of associated items.
	This is a one-way association. ItemStateChange on first combo triggers
	change in second combo, but not reverse. The action depends on contents
	of passed Map, which must be managed by caller (fill and store). If there
	is no value within Map, no change is triggered in second combo.
	<pre>
		Properties map = ClassProperties.getProperties(Host2UserAssoc.class);
		...
		// install associator: change user on host selection
		hostCombo.addItemListener(new ItemSelectAssociation(userCombo, map, "host."));
		...
		// on action performed of host-combo associate as name/value pair in the Map
		map.putProperty("host."+hostCombo.getText(), userCombo.getText());
	</pre>
*/

public class ItemSelectAssociation implements
	ItemListener
{
	private HistCombo selector;
	private Map itemMap;
	private String mapPrefix;
	

	/**
		Associate combo box to passed Map. Empty string is used as mapPrefix (assuming no other combos are associated).
		@param selector second ComboBox that must change its item when selection in first ComboBox changes
		@param itemMap Map containing items from first combo as name and items of second combo as value
	*/
	public ItemSelectAssociation(HistCombo selector, Map itemMap)	{
		this(selector, itemMap, "");
	}

	/**
		Associate combo box to passed Map.
		@param selector second ComboBox that must change its item when selection in first ComboBox changes
		@param itemMap Map containing items (mapPrefix + item) from first combo as name and items of second combo as value
		@param mapPrefix prefix to be used for retrieving a value when selection changes, can be "".
			Any separator (like ".") must be included in the prefix.
			This is needed for property lists that could contain pairs not related to combo box mapping
	*/
	public ItemSelectAssociation(HistCombo selector, Map itemMap, String mapPrefix)	{
		this.selector = selector;
		this.itemMap = itemMap;
		this.mapPrefix = mapPrefix;
	}


	/** Implements ItemListener to set an associated value to the second ComboBox. */
	public void itemStateChanged(ItemEvent e)	{
		if (e.getStateChange() == ItemEvent.SELECTED)	{
			String selected = (String)e.getItem();
			String associated = (String)itemMap.get(mapPrefix+selected);

			if (associated != null && associated.length() > 0)
				selector.setText(associated);
		}
	}

}