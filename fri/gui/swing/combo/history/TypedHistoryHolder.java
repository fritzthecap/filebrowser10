package fri.gui.swing.combo.history;

import java.io.File;
import java.util.Vector;

/**
	Management einer globalen Liste mit Datei fuer alle Instanzen
	einer Spezialierung von HistCombo.
*/

public interface TypedHistoryHolder
{
	/**
		Setzen der globalen Liste fuer alle Instanzen der Spezialisierung.
		@param v Liste der String items.
		@param f Serialisierungs-Datei, aus der gelesen und in die gesichert wird.
	*/
	public void setTypedHistoryData(Vector v, File f);
	/**
		@return globale Liste, die die items enthaelt.
	*/
	public Vector getTypedHistory();
	/**
		@return globale Variable, die die Serialiserung der items als File enthaelt.
	*/
	public File getHistoryFile();
}