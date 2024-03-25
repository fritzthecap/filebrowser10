package fri.gui.awt.resourcemanager;

/**
	Implementing this marker interface protects a component, but NOT its children, from
	being customized. Every component that is instanceof ResourceIgnoringComponent
	will be ignored by the ResourceManager. Nevertheless its children will be looped.
*/

public interface ResourceIgnoringComponent
{
}
