package fri.gui.awt.keyboard;

import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Provides all available key codes and their symbolic names in a NON-local way.
 * That means that in both USA and Germany "Shift" is recognized as modifier.
 * This provides international keyboard shortcut symbols.
	<pre>
		String accelerator = "Ctrl-C";
		KeyNames table = KeyNames.getInstance();
		int c = table.getKeyCode(accelerator);
		int m = table.getKeyModifiers(accelerator);
		JMenuItem mi = new JMenuItem("Copy");
		mi.setAccelerator(KeyStroke.getKeyStroke(c, m));
		...
		String resourceName = table.getKeyName(c, m);
		// will be "Ctrl-C"
	</pre>
 * @author  Fritz Ritzberger
 * @author  $Author: fritzberger $
 * @version $Revision: 1.2 $  $Date: 2001/10/22 14:08:23 $
 */

public class KeyNames
{
	/** All default symbolic keys names */
	protected static String [] defaultNames = {
			"ENTER",
			"BACK_SPACE",
			"TAB",
			"CANCEL",
			"CLEAR",
			"SHIFT",
			"CONTROL",
			"ALT",
			"PAUSE",
			"CAPS_LOCK",
			"ESCAPE",
			"SPACE",
			"PAGE_UP",
			"PAGE_DOWN",
			"END",
			"HOME",
			"LEFT",
			"UP",
			"RIGHT",
			"DOWN",
			"COMMA",
			"MINUS",
			"PERIOD",
			"SLASH",
			"0",
			"1",
			"2",
			"3",
			"4",
			"5",
			"6",
			"7",
			"8",
			"9",
			"SEMICOLON",
			"EQUALS",
			"A",
			"B",
			"C",
			"D",
			"E",
			"F",
			"G",
			"H",
			"I",
			"J",
			"K",
			"L",
			"M",
			"N",
			"O",
			"P",
			"Q",
			"R",
			"S",
			"T",
			"U",
			"V",
			"W",
			"X",
			"Y",
			"Z",
			"OPEN_BRACKET",
			"BACK_SLASH",
			"CLOSE_BRACKET",
			"NUMPAD0",
			"NUMPAD1",
			"NUMPAD2",
			"NUMPAD3",
			"NUMPAD4",
			"NUMPAD5",
			"NUMPAD6",
			"NUMPAD7",
			"NUMPAD8",
			"NUMPAD9",
			"MULTIPLY",
			"ADD",
			"SEPARATER",
			"SUBTRACT",
			"DECIMAL",
			"DIVIDE",
			"DELETE",
			"NUM_LOCK",
			"SCROLL_LOCK",
			"F1",
			"F2",
			"F3",
			"F4",
			"F5",
			"F6",
			"F7",
			"F8",
			"F9",
			"F10",
			"F11",
			"F12",
			"F13",
			"F14",
			"F15",
			"F16",
			"F17",
			"F18",
			"F19",
			"F20",
			"F21",
			"F22",
			"F23",
			"F24",
			"PRINTSCREEN",
			"INSERT",
			"HELP",
			"META",
			"BACK_QUOTE",
			"QUOTE",
			"KP_UP",
			"KP_DOWN",
			"KP_LEFT",
			"KP_RIGHT",
			"DEAD_GRAVE",
			"DEAD_ACUTE",
			"DEAD_CIRCUMFLEX",
			"DEAD_TILDE",
			"DEAD_MACRON",
			"DEAD_BREVE",
			"DEAD_ABOVEDOT",
			"DEAD_DIAERESIS",
			"DEAD_ABOVERING",
			"DEAD_DOUBLEACUTE",
			"DEAD_CARON",
			"DEAD_CEDILLA",
			"DEAD_OGONEK",
			"DEAD_IOTA",
			"DEAD_VOICED_SOUND",
			"DEAD_SEMIVOICED_SOUND",
			"AMPERSAND",
			"ASTERISK",
			"QUOTEDBL",
			"LESS",
			"GREATER",
			"BRACELEFT",
			"BRACERIGHT",
			"AT",
			"COLON",
			"CIRCUMFLEX",
			"DOLLAR",
			"EURO_SIGN",
			"EXCLAMATION_MARK",
			"INVERTED_EXCLAMATION_MARK",
			"LEFT_PARENTHESIS",
			"NUMBER_SIGN",
			"PLUS",
			"RIGHT_PARENTHESIS",
			"UNDERSCORE",
			"FINAL",
			"CONVERT",
			"NONCONVERT",
			"ACCEPT",
			"MODECHANGE",
			"KANA",
			"KANJI",
			"ALPHANUMERIC",
			"KATAKANA",
			"HIRAGANA",
			"FULL_WIDTH",
			"HALF_WIDTH",
			"ROMAN_CHARACTERS",
			"ALL_CANDIDATES",
			"PREVIOUS_CANDIDATE",
			"CODE_INPUT",
			"JAPANESE_KATAKANA",
			"JAPANESE_HIRAGANA",
			"JAPANESE_ROMAN",
			"KANA_LOCK",
			"INPUT_METHOD_ON_OFF",
			"CUT",
			"COPY",
			"PASTE",
			"UNDO",
			"AGAIN",
			"FIND",
			"PROPS",
			"STOP",
			"COMPOSE",
			"ALT_GRAPH",
	};
		
	/** All key codes */
	protected static int [] keys = {
			KeyEvent.VK_ENTER,
			KeyEvent.VK_BACK_SPACE,
			KeyEvent.VK_TAB,
			KeyEvent.VK_CANCEL,
			KeyEvent.VK_CLEAR,
			KeyEvent.VK_SHIFT,
			KeyEvent.VK_CONTROL,
			KeyEvent.VK_ALT,
			KeyEvent.VK_PAUSE,
			KeyEvent.VK_CAPS_LOCK,
			KeyEvent.VK_ESCAPE,
			KeyEvent.VK_SPACE,
			KeyEvent.VK_PAGE_UP,
			KeyEvent.VK_PAGE_DOWN,
			KeyEvent.VK_END,
			KeyEvent.VK_HOME,
			KeyEvent.VK_LEFT,
			KeyEvent.VK_UP,
			KeyEvent.VK_RIGHT,
			KeyEvent.VK_DOWN,
			KeyEvent.VK_COMMA,
			KeyEvent.VK_MINUS,
			KeyEvent.VK_PERIOD,
			KeyEvent.VK_SLASH,
			KeyEvent.VK_0,
			KeyEvent.VK_1,
			KeyEvent.VK_2,
			KeyEvent.VK_3,
			KeyEvent.VK_4,
			KeyEvent.VK_5,
			KeyEvent.VK_6,
			KeyEvent.VK_7,
			KeyEvent.VK_8,
			KeyEvent.VK_9,
			KeyEvent.VK_SEMICOLON,
			KeyEvent.VK_EQUALS,
			KeyEvent.VK_A,
			KeyEvent.VK_B,
			KeyEvent.VK_C,
			KeyEvent.VK_D,
			KeyEvent.VK_E,
			KeyEvent.VK_F,
			KeyEvent.VK_G,
			KeyEvent.VK_H,
			KeyEvent.VK_I,
			KeyEvent.VK_J,
			KeyEvent.VK_K,
			KeyEvent.VK_L,
			KeyEvent.VK_M,
			KeyEvent.VK_N,
			KeyEvent.VK_O,
			KeyEvent.VK_P,
			KeyEvent.VK_Q,
			KeyEvent.VK_R,
			KeyEvent.VK_S,
			KeyEvent.VK_T,
			KeyEvent.VK_U,
			KeyEvent.VK_V,
			KeyEvent.VK_W,
			KeyEvent.VK_X,
			KeyEvent.VK_Y,
			KeyEvent.VK_Z,
			KeyEvent.VK_OPEN_BRACKET,
			KeyEvent.VK_BACK_SLASH,
			KeyEvent.VK_CLOSE_BRACKET,
			KeyEvent.VK_NUMPAD0,
			KeyEvent.VK_NUMPAD1,
			KeyEvent.VK_NUMPAD2,
			KeyEvent.VK_NUMPAD3,
			KeyEvent.VK_NUMPAD4,
			KeyEvent.VK_NUMPAD5,
			KeyEvent.VK_NUMPAD6,
			KeyEvent.VK_NUMPAD7,
			KeyEvent.VK_NUMPAD8,
			KeyEvent.VK_NUMPAD9,
			KeyEvent.VK_MULTIPLY,
			KeyEvent.VK_ADD,
			KeyEvent.VK_SEPARATER,
			KeyEvent.VK_SUBTRACT,
			KeyEvent.VK_DECIMAL,
			KeyEvent.VK_DIVIDE,
			KeyEvent.VK_DELETE,
			KeyEvent.VK_NUM_LOCK,
			KeyEvent.VK_SCROLL_LOCK,
			KeyEvent.VK_F1,
			KeyEvent.VK_F2,
			KeyEvent.VK_F3,
			KeyEvent.VK_F4,
			KeyEvent.VK_F5,
			KeyEvent.VK_F6,
			KeyEvent.VK_F7,
			KeyEvent.VK_F8,
			KeyEvent.VK_F9,
			KeyEvent.VK_F10,
			KeyEvent.VK_F11,
			KeyEvent.VK_F12,
			KeyEvent.VK_F13,
			KeyEvent.VK_F14,
			KeyEvent.VK_F15,
			KeyEvent.VK_F16,
			KeyEvent.VK_F17,
			KeyEvent.VK_F18,
			KeyEvent.VK_F19,
			KeyEvent.VK_F20,
			KeyEvent.VK_F21,
			KeyEvent.VK_F22,
			KeyEvent.VK_F23,
			KeyEvent.VK_F24,
			KeyEvent.VK_PRINTSCREEN,
			KeyEvent.VK_INSERT,
			KeyEvent.VK_HELP,
			KeyEvent.VK_META,
			KeyEvent.VK_BACK_QUOTE,
			KeyEvent.VK_QUOTE,
			KeyEvent.VK_KP_UP,
			KeyEvent.VK_KP_DOWN,
			KeyEvent.VK_KP_LEFT,
			KeyEvent.VK_KP_RIGHT,
			KeyEvent.VK_DEAD_GRAVE,
			KeyEvent.VK_DEAD_ACUTE,
			KeyEvent.VK_DEAD_CIRCUMFLEX,
			KeyEvent.VK_DEAD_TILDE,
			KeyEvent.VK_DEAD_MACRON,
			KeyEvent.VK_DEAD_BREVE,
			KeyEvent.VK_DEAD_ABOVEDOT,
			KeyEvent.VK_DEAD_DIAERESIS,
			KeyEvent.VK_DEAD_ABOVERING,
			KeyEvent.VK_DEAD_DOUBLEACUTE,
			KeyEvent.VK_DEAD_CARON,
			KeyEvent.VK_DEAD_CEDILLA,
			KeyEvent.VK_DEAD_OGONEK,
			KeyEvent.VK_DEAD_IOTA,
			KeyEvent.VK_DEAD_VOICED_SOUND,
			KeyEvent.VK_DEAD_SEMIVOICED_SOUND,
			KeyEvent.VK_AMPERSAND,
			KeyEvent.VK_ASTERISK,
			KeyEvent.VK_QUOTEDBL,
			KeyEvent.VK_LESS,
			KeyEvent.VK_GREATER,
			KeyEvent.VK_BRACELEFT,
			KeyEvent.VK_BRACERIGHT,
			KeyEvent.VK_AT,
			KeyEvent.VK_COLON,
			KeyEvent.VK_CIRCUMFLEX,
			KeyEvent.VK_DOLLAR,
			KeyEvent.VK_EURO_SIGN,
			KeyEvent.VK_EXCLAMATION_MARK,
			KeyEvent.VK_INVERTED_EXCLAMATION_MARK,
			KeyEvent.VK_LEFT_PARENTHESIS,
			KeyEvent.VK_NUMBER_SIGN,
			KeyEvent.VK_PLUS,
			KeyEvent.VK_RIGHT_PARENTHESIS,
			KeyEvent.VK_UNDERSCORE,
			KeyEvent.VK_FINAL,
			KeyEvent.VK_CONVERT,
			KeyEvent.VK_NONCONVERT,
			KeyEvent.VK_ACCEPT,
			KeyEvent.VK_MODECHANGE,
			KeyEvent.VK_KANA,
			KeyEvent.VK_KANJI,
			KeyEvent.VK_ALPHANUMERIC,
			KeyEvent.VK_KATAKANA,
			KeyEvent.VK_HIRAGANA,
			KeyEvent.VK_FULL_WIDTH,
			KeyEvent.VK_HALF_WIDTH,
			KeyEvent.VK_ROMAN_CHARACTERS,
			KeyEvent.VK_ALL_CANDIDATES,
			KeyEvent.VK_PREVIOUS_CANDIDATE,
			KeyEvent.VK_CODE_INPUT,
			KeyEvent.VK_JAPANESE_KATAKANA,
			KeyEvent.VK_JAPANESE_HIRAGANA,
			KeyEvent.VK_JAPANESE_ROMAN,
			KeyEvent.VK_KANA_LOCK,
			KeyEvent.VK_INPUT_METHOD_ON_OFF,
			KeyEvent.VK_CUT,
			KeyEvent.VK_COPY,
			KeyEvent.VK_PASTE,
			KeyEvent.VK_UNDO,
			KeyEvent.VK_AGAIN,
			KeyEvent.VK_FIND,
			KeyEvent.VK_PROPS,
			KeyEvent.VK_STOP,
			KeyEvent.VK_COMPOSE,
			KeyEvent.VK_ALT_GRAPH,
	};

	private static KeyNames singleton = null;
	
	/** Table with key = Integer(KeyCode) and value = KeyName. */
	private Hashtable keyTable = new Hashtable();
	private Hashtable nameTable = new Hashtable();
	
	/** modifiers symbolic names */
	protected String shift;
	protected String alt;
	protected String ctrl;
	protected String meta;
	protected String altgr;
	
	/** keys symbolic names*/
	protected String [] keyNames;
	
	
	
	/**
	* Factory that returns a KeyNames instance singleton.
	*/
	public static KeyNames getInstance()	{
		return singleton == null ? (singleton = new KeyNames()) : singleton;
	}
	
	
	/** Constructor that only calls init(). */
	protected KeyNames() { // do not instantiate
		init();
	}
	
	protected void init()	{
		initTables();
		
		shift = "Shift";
		alt   = "Alt";
		ctrl  = "Ctrl";
		meta  = "Meta";
		altgr = "AltGr";
	}

	protected void initTables()	{
		String [] names = getKeyNames();
		for (int i = 0; i < names.length; i++) {
			Integer keyCode = Integer.valueOf(keys[i]);
			keyTable.put(names[i], keyCode);
			nameTable.put(keyCode, names[i]);
		}
	}
	

	/** Returns all available key names. */
	public String [] getKeyNames()	{
		return (keyNames == null) ? defaultNames : keyNames;
	}
	
	/** Returns all available key codes. */
	public int [] getKeys()	{
		return keys;
	}
	
	
	private String getPartialString(String keyName, boolean takeFirst)	{
		String keyText = null;
		for (StringTokenizer stok = new StringTokenizer(keyName, "-"); stok.hasMoreTokens(); )	{
			keyText = stok.nextToken().trim();
			if (takeFirst)
				return keyText;
		}
		return keyText;
	}
	
	/**
		Returns the keycode for passed symbolic name. The input name must be
		<b>"BACK_SPACE"</b> * for <i>KeyEvent.VK_BACK_SPACE</i>, <b>"ENTER"</b> for
		<i>KeyEvent.VK_ENTER</i>, .... * as listed in KeyEvent.java (mind that these
		can be overridden by Toolkit.getProperty()). * Possible modifiers are:
		"Shift", "Ctrl", "Alt", "AltGr", "Shift+Alt", ... e.g. "Shift+Ctrl-ENTER" *
	 	@param keyName symbolic key name, e.g. "Shift+Ctrl-F3" or "Alt+Ctrl-DEL"
	 	@return key code of passed name
	*/
	public int getKeyCode(String keyName)	{
		String keyText = getPartialString(keyName, false);
		Integer i = (Integer)keyTable.get(keyText);
		return i != null ? i.intValue() : -1;
	}
	
	/**
	 * Returns the modifiers for a symbolic key name like "Shift+Alt-DOWN".
	 * @param keyName symbolic key name, e.g. "Shift+Ctrl-F3" or "Alt+Ctrl-DEL"
	 * @return modifier like KeyEvent.CTRL_MASK, KeyEvent.SHIFT_MASK, ...
	 */
	public int getKeyModifiers(String keyName)	{
		String modText = getPartialString(keyName, true);
		int m = 0;
		
		if (modText != null)	{
			for (StringTokenizer stok = new StringTokenizer(modText, "+"); stok.hasMoreTokens(); ) {
				String s = stok.nextToken().trim();
				
				if (s.equals(shift))
					m |= KeyEvent.SHIFT_MASK;
				else
				if (s.equals(ctrl))
					m |= KeyEvent.CTRL_MASK;
				else
				if (s.equals(alt))
					m |= KeyEvent.ALT_MASK;
				else
				if (s.equals(altgr))
					m |= KeyEvent.ALT_GRAPH_MASK;
				else
				if (s.equals(meta))
					m |= KeyEvent.META_MASK;
			}
		}
	
		return m;
	}


	/**
		Returns a symbolic name for passed keycode and modifiers, e.g. "Ctl+Alt-DELETE".
	*/
	public String getKeyName(int keyCode, int modifiers)	{
		String keyName = (String) nameTable.get(Integer.valueOf(keyCode));
		String modifier = "";
		
		if ((modifiers & KeyEvent.SHIFT_MASK) != 0)
			modifier += (modifier.length() <= 0) ? shift : "+"+shift;
			
		if ((modifiers & KeyEvent.ALT_MASK) != 0)
			modifier += (modifier.length() <= 0) ? alt : "+"+alt;
			
		if ((modifiers & KeyEvent.CTRL_MASK) != 0)
			modifier += (modifier.length() <= 0) ? ctrl : "+"+ctrl;
			
		if ((modifiers & KeyEvent.ALT_GRAPH_MASK) != 0)
			modifier += (modifier.length() <= 0) ? altgr : "+"+altgr;
			
		if ((modifiers & KeyEvent.META_MASK) != 0)
			modifier += (modifier.length() <= 0) ? meta : "+"+meta;
			
		return (modifier.length() > 0 ? modifier+"-" : "")+keyName;
	}

	/**
		Returns the index for passed keycode within global key array from getKeys() call.
	*/
	public int getKeyIndex(int keyCode)	{
		for (int i = 0; i < keys.length; i++)
			if (keys[i] == keyCode)
				return i;
		return -1;
	}


	/* Test Main
	public static void main(String [] args)	{
		System.err.println("International key code for Shift+Ctrl-F3 is: "+getInstance().getKeyCode("Shift+Ctrl-F3 - F3"));
		System.err.println("International modifiers for Shift+Ctrl-F3 is: "+getInstance().getKeyModifiers("Shift+Ctrl-F3 - F3")); }
	}
	*/

}