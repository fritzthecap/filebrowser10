package fri.gui.awt.component.visitor;

/**
	Interface to visit every Component of a Container Component tree.
	As this could be AWT Component or AWT MenuComponent, the parameter
	is of type Object and must be casted by visitor when necessary.
*/

public interface ContainerVisitor
{
	/**
		The implementer receives all Components of the hierarchy in this call.
		This can be a MenuComponent or a Component. Normally it returns the passed
		userObject, but it can return any client data needed on next level of recursion.
		@param c received MenuComponent or Component
		@param userObject arbitrary callback argument, e.g. the parent Component
		@return userObject for next level of traversal, does not influence the traversal.
	*/
	public Object visit(Object component, Object userObject);

}
