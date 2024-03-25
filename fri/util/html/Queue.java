package fri.util.html;

import java.util.*;

public class Queue extends Vector {
	public Object addBack(Object item) {
		addElement(item);
		return item;
	}

	public Object getFront() {
		Object item = firstElement();
		removeElementAt(0);
		return item;
	}

}
