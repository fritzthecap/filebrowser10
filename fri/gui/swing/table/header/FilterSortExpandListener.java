package fri.gui.swing.table.header;

/**
 * Listen to changes in table header editor: a new filter was committed,
 * a new sort order was reqested, the column was collapsed or expanded.
 *
 * @author  Fritz Ritzberger 2001
 */

public interface FilterSortExpandListener
{
	/**
		The implmenter receives events in table header, when installed e.g.
		by <i>FilterSortExpandHeaderEditor.setTableHeader(jtable, listener);</i>
		call. The HeaderValueStruct contains all informations needed
		to detect the sort order, the expand state, or the filter string.
		@param newValue information about the event.
		@param column index of column where the event happened.
	*/
	public void headerChanged(HeaderValueStruct newValue, int column);
}
