package fri.gui.awt.resourcemanager;

/**
	Implementing this marker interface opens a AWT TextArea, TextField and
	Swing JTextComponent with its subclasses for text customizing.
	By default the text-getters/setters of these Components will be ignored,
	as the content of these is mostly runtime-specific programmatic, even if
	they are set to being not editable.
	<p>
	This interface is useful for text Components that hold static information
	which should be renderable in multiple languages.
*/

public interface ResourceProvidingTextComponent
{
}
