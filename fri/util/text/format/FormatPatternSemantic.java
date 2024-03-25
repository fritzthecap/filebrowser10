package fri.util.text.format;

/**
	Implementer receives the pattern interpreter callbacks with
	meaningful masks and separator sections of a pattern.
	
  @author  Ritzberger Fritz
*/

public interface FormatPatternSemantic
{
	/** Return true if this character opens a mask part of the pattern. */
	public boolean isMaskCharacter(char c);
	/** Dispatch a finished separator section (without quotes). */
	public void finishSeparator(StringBuffer sep);
	/** Dispatch a finished mask token. */
	public void finishMask(StringBuffer mask);
}