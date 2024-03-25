package fri.gui.awt.resourcemanager.component;

import java.awt.Component;

/**
	Responsibilities of non-existent proxy components like tab-panel for JTabbedPane.
*/

public abstract class ArtificialComponent
{
	protected int index;
	protected Component parentComponent;
	
	/** Returns the parent component this artificial component is contained. Needed by BottomUpContainerVisitor to find this within Window. */
	public Component getComponent()	{
		return parentComponent;
	}

	/** Returns a resourcename ("listitem", "tab", "columnheader") that makes it identifyable within a component choice. */
	protected abstract String getName();

	/** Implements a generic equals method by comparing parent, index and name. */
	public boolean equals(Object o)	{
		if (o.getClass().equals(getClass()) == false)
			return false;
			
		ArtificialComponent ac = (ArtificialComponent) o;
		return ac.index == index && ac.getComponent() == getComponent() && ac.getName().equals(getName());
	}

	/** Implements a generic hash method by adding parent, index and name hash codes. */
	public int hashCode()	{
		return index + getComponent().hashCode() + getName().hashCode();
	}

}
