package fri.gui.swing.hexeditor;

/**
	Holding a byte value and its row/column position.
	Needed for UpdateCommand when a cell was edited.
*/

public class ByteAndPosition
{
	public final Byte theByte;
	public final int row;
	public final int column;

	public ByteAndPosition(Byte theByte, int row, int column)	{
		this.theByte = theByte;
		this.row = row;
		this.column = column;
	}

}
