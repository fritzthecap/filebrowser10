package fri.util.regexp;

/**
	RegExpUtil verwendet zum Matching von Patterns die toString()-Methode
	der Objekte im uebergebenen Vector. Um auch andere Methoden als
	toString() verwenden zu koennnen, gibt es dieses Interface.
	Die im uebergebenen Vector befindlichen Objekte werden mit
	instanceof darauf geprueft, ob sie dieses Interface implementieren. Wenn ja,
	wird statt toString() getMatchString() gerufen.
	<p>
	Responsibilities:
	Beinhaltet Methoden, die zum "matchen" von Objekten gegen Patterns dienen.
	
	@author Ritzberger Fritz
*/

public interface MatchStringProducer
{
	public String getMatchString();
}