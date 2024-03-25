package fri.gui.swing.dnd;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * Interface for Components that do drag and drop within themselves and from/to other sensor components.
 * <p />
 * Coding sample:
 * <pre>
 *  class Xxx implements DndPerformer
 * 	{
 * 	    Xxx()	{
 * 	        Component sensorComponent = ...;
 * 	        JPanel mainPanel = ...;
 * 	        JScrollPane scrollPane = new JScrollPane(sensorComponent);
 * 	        mainPanel.add(scrollPane);
 * 	        
 * 			new DndListener(this, sensorComponent);
 * 			...
 * 		}
 * 		
 * 		// implementation of interface DndPerformer
 * 		
 *  	public DataFlavor supportsDataFlavor(int action, Point point, DataFlavor [] flavors)	{
 *  		for (int i = 0; i < flavors.length; i++)
 *  			if (flavors[i].equals(DataFlavor.javaFileListFlavor))
 *  				return DataFlavor.javaFileListFlavor;
 *  		return null;
 *  	}
 *  
 * 		public Transferable sendTransferable()	{
 * 			File [] filesToTransfer = ...;	// File is serializable by default
 * 			return new JavaFileList(Arrays.asList(filesToTransfer));
 * 		}
 * 	
 * 		public boolean receiveTransferable(Object data, int action, Point point)	{
 * 			List fileList = (List)data;
 * 			Iterator iterator = fileList.iterator();
 * 			while (iterator.hasNext()) {
 * 				File file = (File) iterator.next();
 * 				...
 * 			}
 * 			return false;
 * 		}
 * </pre>
 * 
 * @author Fritz Ritzberger, 1999
 */
public interface DndPerformer
{
	/** Constant for DndListener constructor: move action. */
	public static final int MOVE = 300;
	/** Constant for DndListener constructor: copy action. */
	public static final int COPY = 301;
	/** Constant for DndListener constructor: copy or move action. */
	public static final int COPY_OR_MOVE = 302;
	/** Constant for DndListener constructor: link action. */
	public static final int LINK = 303;
	/** Constant for DndListener constructor. */
	public static final int REFERENCE = 304;
	/** Constant for DndListener constructor. */
	public static final int NONE = 305;
	/** Constant for DndListener constructor. */
	public static final int UNKNOWN = 306;

	/**
	 * Called in dragGestureRecognized() routine by the Listener.
	 * @return a Transferable object representing sent data.
	 * 		Mind that all data referenced in the Transferable must be serializable!
	 */
	public Transferable sendTransferable();

	/**
	 * Implementers receive dragged data here.
	 * The received data have been serialized and might need to be mapped to local data
	 * (contained pointers might be not valid anymore as they have been reconstructed
	 * on deserialization).
	 * @param data the data received.
	 * @param action one of the constants in DndPerformer.
	 * @param point location where the drop event happened.
	 * @return true if data were processed.
	 * 		This return gets passed to dropEvent.dropComplete().
	 */
	public boolean receiveTransferable(Object data, int action, Point point);

	/**
	 * Called in drop() routine by the Listener. Return the desired Flavor.
	 * Java provides several standard flavors, or they can be application-defined.
	 * @param action one of the constants in DndPerformer.
	 * @param point location where the drop event happened.
	 * @param flavors all flavors the dragged data actually provides.
	 */
	public DataFlavor supportsDataFlavor(int action, Point point, DataFlavor[] flavors);

	/**
	 * Notification from drop target to cancel action.
	 */
	public void actionCanceled();

	/**
	 * Notification from drop target to remove data, because they were copied
	 * successfully and the specified action was MOVE.
	 */
	public void dataMoved();

	/**
	 * Notification from drop target to remove data, because they were copied
	 * successfully and the specified action was COPY.
	 */
	public void dataCopied();

	/**
	 * The data are dragged over this Component-relative point.
	 * @return true if the drop would be accepted at the passed point.
	 */
	public boolean dragOver(Point point);

	/**
	 * The drag leaves the component. dragOver() passed the coordinates just before.
	 */
	public void startAutoscrolling();

	/**
	 * The drag enters the component.
	 */
	public void stopAutoscrolling();

}
