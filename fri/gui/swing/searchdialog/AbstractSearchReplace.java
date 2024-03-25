package fri.gui.swing.searchdialog;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import gnu.regexp.*;
import fri.util.props.*;
import fri.util.regexp.*;
import fri.gui.CursorUtil;
import fri.gui.text.TextHolder;
import fri.gui.swing.dialog.FrameServiceDialog;

/**
	A Search and Replace Dialog.
	<p>
	Input: TextHolder providing text, caret position, selection.
		A parent Frame (which must not be the container of TextHolder).<br>
	Actions: Select found text within TextHolder. Replace text.<br>
	<p>
	Subclasses having another text renderer than a textarea must override
	<ul>
		<li>getMatchIndexByCaretImpl (called on F3)</li>
		<li>getMatchRangeImpl (called when result list selection changes)</li>
		<li>newSearch (actual text search)</li>
		<li>replace (actual text replacement)</li>
	</ul>

	@author Fritz Ritzberger
*/

public abstract class AbstractSearchReplace extends FrameServiceDialog implements
	ActionListener,
	ItemListener,
	ListSelectionListener
{
	private boolean ignoreCase = PropertyUtil.checkClassProperty("ignoreCase", getClass(), true);
	private boolean wordMatch = PropertyUtil.checkClassProperty("wordMatch", getClass());
	private String regExpSyntax = ClassProperties.get(getClass(), "regExpSyntax");

	/** Flag for constructor: no replace button is shown. */
	public static final int SEARCH_ONLY = 1;
	/** Flag for constructor: replace button is shown. */
	public static final int SEARCH_REPLACE = 2;

	private int mode;	// actual mode: search only or search and replace

	private JButton search, replace, replaceAll;
	private SearchHistoryCombo tf_search;
	private ReplaceHistoryCombo tf_replace;
	private JCheckBox cb_replace, cb_IgnoreCase, cb_WordMatch;
	private JList li_foundList;
	private Vector listData;
	private JLabel lb_foundMsg;
	private boolean doRegExp, doWildcards;
	private JComboBox cmb_RESyntax;

	protected TextHolder textarea;	// source text holder
	protected int currMatchNo;	// number of currently selected match
	protected int startSelection;	// aktueller Beginn der Selektion

	private String oldSearch = "";	// previous search pattern
	protected boolean searchChanged;	// were pattern or options changed?
	private boolean textChanged;	// was the source text changed?

	private RE expr;	// current search pattern
	private int defCompFlags = RE.REG_MULTILINE;	// 2nd parameter (cflags) for RE constructor
	private RESyntax defaultSyntax = RESyntax.RE_SYNTAX_PERL5;	// 3rd parameter (syntax) for RE constructor
	private RESyntax syntax = defaultSyntax;	// current pattern syntax
	protected REMatch[] matches = null;	// current matches



	/** Create a nonmodal search dialog that offers replace only when textarea is editable. */
	public AbstractSearchReplace(JFrame f, TextHolder textarea) {
		this(f, textarea, textarea.isEditable() ? SEARCH_REPLACE : SEARCH_ONLY);
	}

	/** Create a nonmodal search dialog that offers replace only when mode is SEARCH_REPLACE. */
	public AbstractSearchReplace(JFrame f, TextHolder textarea, int mode) {
		super(f);
		
		this.mode = mode;

		initParent(textarea.getTextComponent());	// install parent frame close listener

		Component c = parentWindow;
		CursorUtil.setWaitCursor(c);
		try	{
			build();	// build GUI
			init(textarea, true);	// start search
			addListeners();	// add listeners to all GUI components
			pack();	// packs the dialog according to last geometry state
	
			if (getSearchPattern().length() > 0)
				setFreeViewLocation();	// places dialog where is most place
			else
				centerOverParent();	// centers dialog when no pattern was selected in text
			
			setVisible(true);
		}
		finally	{
			CursorUtil.resetWaitCursor(c);
		}
	}


	/** Called when dialog closes, before close(). */
	protected void close()	{
		save();
		textChanged = false;
		searchChanged = true;
		tf_search.setPopupVisible(false);	// Fehler JDK ?
		if (textarea != null)
			textarea.requestFocus();
		
		super.close();
	}

	/** Called when parent window closes. Enables garbage collection of textarea. */
	protected void parentWindowClosing()	{
		textarea = null;
		matches = null;
	}
	
	/** Called when dialog opens. Requests focus on search pattern textfield. */
	protected void dialogWindowOpened()	{
		tf_search.getTextEditor().requestFocus();
	}


	private void build()	{
		setTitle(mode == SEARCH_ONLY ? "Search" : "Search And Replace");

		// options
		cb_IgnoreCase = new JCheckBox("Ignore Case", ignoreCase);
		cb_WordMatch = new JCheckBox("Match Whole Word", wordMatch);
		cmb_RESyntax = new JComboBox(Syntaxes.getSyntaxes());
		cmb_RESyntax.setEditable(false);
		if (regExpSyntax != null)
			cmb_RESyntax.setSelectedItem(regExpSyntax);
		JPanel p01 = new JPanel(new GridLayout(2, 1));
		p01.add(cb_IgnoreCase);
		p01.add(cb_WordMatch);
		JPanel p0 = new JPanel();
		p0.setLayout(new BoxLayout(p0, BoxLayout.X_AXIS));
		p0.add(p01);
		p0.add(Box.createHorizontalGlue());
		JPanel p02 = new JPanel();
		p02.add(cmb_RESyntax);
		p0.add(p02);

		// search/replace labels
		JPanel p1 = new JPanel (new GridLayout(mode == SEARCH_REPLACE ? 2 : 1, 1));
		p1.add(new JLabel("Search:"));
		if (mode == SEARCH_REPLACE)	{
			cb_replace = new JCheckBox("Replace:", false);
			cb_replace.addItemListener(this);
			p1.add(cb_replace);
		}

		// search/replace textfields
		JPanel p2 = new JPanel (new GridLayout(mode == SEARCH_REPLACE ? 2 : 1, 1));
		tf_search = new SearchHistoryCombo();
		// tf_search.setText("");	// FRi 2006-04-06: avoid clearing search pattern on new dialog
		p2.add(tf_search);

		if (mode == SEARCH_REPLACE)	{
			tf_replace = new ReplaceHistoryCombo();
			tf_replace.setEnabled(false);
			tf_replace.setText("");
			p2.add(tf_replace);
		}

		// optional other options
		JPanel pOthers = createOtherOptions();
		if (pOthers != null)	{
			JPanel pTmp = p0;
			p0 = new JPanel();
			p0.setLayout(new BoxLayout(p0, BoxLayout.Y_AXIS));
			p0.add(pTmp);
			p0.add(pOthers);
		}

		JPanel p3 = new JPanel (new BorderLayout());
		p3.add(p0, BorderLayout.NORTH);
		p3.add(p1, BorderLayout.WEST);
		p3.add(p2, BorderLayout.CENTER);

		// found label and location list
		JPanel p4 = new JPanel (new BorderLayout());
		lb_foundMsg = new JLabel(" ");
		p4.add(lb_foundMsg, BorderLayout.NORTH);
		li_foundList = new JList();
		li_foundList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane sp = new JScrollPane(li_foundList);
		p4.add(sp, BorderLayout.CENTER);

		// buttons
		JPanel bp = new JPanel ();
		search = new JButton("Search");
		search.addActionListener(this);	// not invoked by init()
		bp.add(search);
		if (mode == SEARCH_REPLACE)	{
			replace = new JButton("Replace");
			replace.setEnabled(false);
			replace.addActionListener(this);
			bp.add(replace);
			replaceAll = new JButton("All");
			replaceAll.setEnabled(false);
			replaceAll.addActionListener(this);	// not invoked by init()
			bp.add(replaceAll);
		}

		// all together now
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(p3, BorderLayout.NORTH);
		c.add(p4, BorderLayout.CENTER);
		c.add(bp, BorderLayout.SOUTH);
	}

	/** Returns null. Override this to input other options below search/replace textfields. */
	protected JPanel createOtherOptions()	{
		return null;
	}

	private void addListeners()	{	// add listeners after init()
		// listen for action callbacks after init() took place
		cb_IgnoreCase.addItemListener(this);
		cb_WordMatch.addItemListener(this);
		cmb_RESyntax.addActionListener(this);

		tf_search.addActionListener(this);
		if (tf_replace != null)
			tf_replace.addActionListener(this);

		tf_search.getTextEditor().addKeyListener(getCloseOnEscapeKeyListener());
		li_foundList.addKeyListener(getCloseOnEscapeKeyListener());
		search.addKeyListener(getCloseOnEscapeKeyListener());
	}


	protected void save()	{
		tf_search.save();
		if (tf_replace != null)
			tf_replace.save();

		ClassProperties.put(getClass(), "regExpSyntax", cmb_RESyntax.getSelectedItem().toString());
		ClassProperties.put(getClass(), "ignoreCase", cb_IgnoreCase.isSelected() ? "true" : "false");
		ClassProperties.put(getClass(), "wordMatch", cb_WordMatch.isSelected() ? "true" : "false");
		ClassProperties.store(getClass());
	}



	/** Notify search window that Document has changed. */
	public void setTextChanged()	{
		//System.err.println("AbstractSearchReplace - text was changed!");
		textChanged = true;
	}


	/** Set another TextHolder into that finder window. Do not search for selected text. */
	public void init(TextHolder textarea)	{
		init(textarea, false);
	}

	/** Set another TextHolder into that finder window. Optionally search for selected text. */
	public void init(TextHolder textarea, boolean pickSelectedText)	{
		init(textarea, null, null, false, false, pickSelectedText);	// args are ignored if pattern is null
	}

	/** Set another TextHolder into that finder window and start search. */
	public void init(
		TextHolder textarea,
		String searchPatt,
		String syntax,
		boolean ignoreCase,
		boolean wordMatch)
	{
		init(textarea, searchPatt, syntax, ignoreCase, wordMatch, false);
	}

	/** Set another TextHolder into that finder window and start search. */
	public void init(
		TextHolder textarea,
		String searchPatt,
		String syntax,
		boolean ignoreCase,
		boolean wordMatch,
		boolean pickSelectedText)
	{
		//System.err.println("SearchReplace.init(), pattern "+searchPatt);
		initParent(textarea.getTextComponent());
		
		if (this.textarea != null && searchPatt == null)	{	// change textarea context
			setListData(null);
			lb_foundMsg.setText(" ");
		}
		
		if (textChanged = (this.textarea != textarea))	{
			matches = null;	// clear search results
		}
		this.textarea = textarea;
		
		startSelection = textarea.getCaretPosition();
		
		String s = getSearchPattern();

		if (searchPatt == null)	{	// search for selected text
			if (pickSelectedText)	{
				s = "";	// window is opening, do not search for old pattern
				try	{	// try to get selected text from textarea
					String sel = textarea.getSelectedText();
					if (sel != null && sel.length() > 0)
						s = sel;
				}
				catch (StringIndexOutOfBoundsException e)	{
					e.printStackTrace();
				}
			}

			// set selection start instead of caret position if text was selected
			int startSel = textarea.getSelectionStart();
			if (startSel >= 0)
				startSelection = startSel;
		}
		else	{	// search for programmatic pattern
			s = searchPatt;
			
			if (syntax != null)
				cmb_RESyntax.setSelectedItem(syntax);
				
			cb_IgnoreCase.setSelected(ignoreCase);			
			cb_WordMatch.setSelected(wordMatch);			
		}
		
		if (s != null && s.length() > 0)	{
			tf_search.setText(s);

			checkForChangedSearch(s);

			search();
		}
		else	{
			// tf_search.clear();
		}
		
		tf_search.selectAll();
	}


	private void checkForChangedSearch(String s)	{
		if (searchChanged == false)	{
			searchChanged = oldSearch.equals(s) == false;
			System.err.println("changed search pattern from >"+oldSearch+"< to >"+s+"<, searchChanged is: "+searchChanged);
		}
		oldSearch = s;
	}




	/** Returns current inited textarea. */
	public TextHolder getCurrentTextArea()	{
		return textarea;
	}
	
	/** Returns true if ignore-case is switched on. */
	public boolean getIgnoreCase()	{
		return cb_IgnoreCase.isSelected();
	}

	/** Returns true if word-match is switched on. */
	public boolean getWordMatch()	{
		return cb_WordMatch.isSelected();
	}

	/** Returns current search pattern text. */
	public String getSearchPattern()	{
		return tf_search.getText();
	}

	/** Returns current replacement text. */
	protected String getReplacementText()	{
		return tf_replace.getText();
	}



	/**
		Find next item (seeking from caret position) without showing on screen.
		This method normally gets called on F3.
	*/
	public void findNext()	{
		if (matches == null || matches.length <= 0)
			return;
			
		searchChanged = false;
		
		int caret = textarea.getCaretPosition();
		currMatchNo = getMatchIndexByCaretImpl(caret);
		textarea.select(caret, caret);	// clear selection before selecting next location

		search();	// sets selection to currMatchNo
	}

	/** Returns the match that is the next (ascending) from passed caret position. Called when F3 was pressed in textarea. */
	protected abstract int getMatchIndexByCaretImpl(int caret);


	/** Implements ListSelectionListener: Selects a new location in textarea. */
	public void valueChanged(ListSelectionEvent e)	{
		//System.err.println(e);
		if (e.getValueIsAdjusting() == false && li_foundList.isSelectionEmpty() == false)	{
			showMatch(getSelectedMatch());
		}
	}


	protected int getSelectedMatch()	{
		int i = li_foundList.getSelectedIndex();
		return i;
	}


	private void showMatch(int index)	{
		if (textChanged)	{
			search();
			return;	// can not predict what was selected, as text might have changed completely
		}
				
		if (matches == null || matches.length <= 0)
			return;

		Point p = getMatchRangeImpl(currMatchNo = index);
		if (p == null)
			return;
		
		textarea.select(startSelection = p.x, p.y);
		
		if (matches != null && matches.length > 0)	{
			setFreeViewLocation();
		}
	}

	/** Returns the match range of passed search result index. Called when selection in result list changes. */
	protected abstract Point getMatchRangeImpl(int index);



	/** Implements ItemListener: "ignoreCase", "wordMatch" or "Replace" have changed their value. */
	public void itemStateChanged (ItemEvent e)	{
		if (e.getSource() == cb_IgnoreCase || e.getSource() == cb_WordMatch)	{
			searchChanged = true;
			//System.err.println("changed checkbox state of "+e.getSource());
		}
		else
		if (e.getSource() == cb_replace)	{
			boolean enable = cb_replace.isSelected();

			if (enable && 
					(getSearchPattern().length() <= 0 || matches == null || matches.length <= 0))
			{
				cb_replace.setSelected(false);
				tf_search.getTextEditor().requestFocus();
				return;	// Zuerst muss ein Suchkriterium eingetragen sein
			}

			replace.setEnabled(enable);	// Buttons

			if (tf_replace != null)	{
				replaceAll.setEnabled(enable);			
				tf_replace.setEnabled(enable);	// Textfeld
			}
			if (enable && tf_replace != null)	{
				tf_replace.getTextEditor().requestFocus();
			}
			else	{
				tf_search.getTextEditor().requestFocus();
			}
		}
	}



	/** Implements ActionListener: regExp syntax selected, search or replace button pressed. */
	public void actionPerformed (ActionEvent e)	{
		tf_search.setPopupVisible(false);	// Fehler JDK ?
		
		checkForChangedSearch(getSearchPattern());

		if (e.getSource() == cmb_RESyntax)	{
			syntax = null;	// freigeben
			doRegExp = true;
			
			String sel = (String)cmb_RESyntax.getSelectedItem();
			syntax = Syntaxes.getSyntax(sel);
			
			if (syntax == null)	{	// do wildcards or no regular expressions
				doRegExp = false;
				syntax = defaultSyntax;
				doWildcards = Syntaxes.doWildcards(sel);
			}
			searchChanged = true;
			//System.err.println("changed regular expression syntax combobox state");
		}
		else	// search command, check if valid pattern
		if (getSearchPattern().equals("") == false)	{
			tf_search.commit();
			
			if (e.getSource() == tf_search || e.getSource() == search)	{
				searchChanged = true;	// always refresh everything when search button pressed
				search();
			}
			else
			if (e.getSource() == tf_replace || e.getSource() == replace)	{
				checkConditionsAndReplace(false);
			}
			else
			if (e.getSource() == replaceAll)	{
				checkConditionsAndReplace(true);
			}
		}
	}


	private RE getExpression()	{
		// Suchbegriff holen
		String such = getSearchPattern();

		if (such.equals(""))	{	// kein Suchbegriff eingegeben
			Toolkit.getDefaultToolkit().beep();
			expressionError("Please Enter A Search Pattern!");
			return null;
		}

		// Falls nicht regexp, interpretierte Zeichen ausfluchten
		if (doRegExp == false)
			if (doWildcards)
				such = RegExpUtil.setDefaultWildcards(such);
			else
				such = RegExpUtil.setNoWildcards(such);

		// Umwandeln, falls word match
		if (cb_WordMatch.isSelected())
			such = RegExpUtil.getWordBoundsPattern(such);
				
		System.err.println("Suchbegriff >"+such+"<");
		
		// Expression compilieren
		int compFlags;
		if (cb_IgnoreCase.isSelected())
			compFlags = defCompFlags | RE.REG_ICASE;
		else
			compFlags = defCompFlags;

		RE expression = null;
		try	{
			expression = new RE(such, compFlags, syntax);
		}
		catch (Exception e)	{	// Meldung bei syntaktischen Fehler
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(
					getDialog(),
					e.getMessage(),
					"Error",
					JOptionPane.ERROR_MESSAGE);
			expressionError(e.getMessage());
			return null;
		}

		return expression;
	}

	private void expressionError(String msg)	{
		lb_foundMsg.setText(msg);
		tf_search.getTextEditor().requestFocus();
		matches = null;
		setListData(null);
	}


	/**
		Search for given pattern in TextHolder, generate result
		list, and insert results into JList.
		Following variables are evaluated: matches, currMatchNo.
		@param expr the compiled regular expression to search for.
		@returns the new search result that is to be put into result list.
	*/
	protected abstract Vector newSearch(RE expr);


	// Set data into the list of locations.
	private void setListData(Vector listData)	{
		if (listData == null)
			listData = new Vector();
		li_foundList.setListData(this.listData = listData);
	}

	/**
		Get data from the list of locations.
		Used by subclasses to retrieve their list object returned from newSearch().
	*/
	protected Vector getListData()	{
		return listData;
	}
	

	/**
		Service method for newSearch(): returns a int[2] array, the start index at
		first and the end index on second position. This considers the Word-Match setting.
	*/
	protected int [] getRealStartEnd(REMatch match, String textbuffer)	{
		int start = match.getStartIndex();
		int end = Math.min(match.getEndIndex(), textbuffer.length());
		
		// do corrections if word match is active
		if (getWordMatch())	{
			if (start > 0)
				start++;	// as start character is within the found match
			if (end < textbuffer.length())
				end--;	// as end character is within the found match
		}

		return new int [] { start, end };
	}


	/** Returns true if a checkbox or combobox or searchpattern was changed. */
	protected boolean searchConditionsChanged()	{
		System.err.println("conditions changed: "+(expr == null)+", "+searchChanged+", "+textChanged);
		return expr == null || searchChanged || textChanged;
	}


	/**
		Callable from subclasses that implement replace to get new search results.
		Puts the search result returned by newSearch() into JList. 
	*/
	protected void search()	{
		setWaitCursor(true);
		try	{
			// do new search only if changed conditions
			if (searchConditionsChanged())	{
				li_foundList.removeListSelectionListener(this);
	
				if ((expr = getExpression()) != null)	{
					currMatchNo = 0;
					Vector result = newSearch(expr);	// perform actual search
					setListData(result);
	
					searchChanged = textChanged = false;
				}
	
				li_foundList.addListSelectionListener(this);
			}
	
			// when there are found locations
			if (matches != null && matches.length > 0)	{	// select the list line according to selection in textarea
				lb_foundMsg.setText(matches.length + "  Location(s) Found");
				li_foundList.clearSelection();
				li_foundList.setSelectedIndex(currMatchNo);	// currMatchNo was set by newSearch()
				currMatchNo = (currMatchNo + 1) % matches.length;	// skippen
			}
			else	{	// when not found
				lb_foundMsg.setText("Not Found");
	
				if (cb_replace != null)	// disable replace textarea
					cb_replace.setSelected(false);
	
				// clear selection in textarea
				int pos = textarea.getSelectionStart();
				if (pos >= 0)
					textarea.select(pos, pos);
			}
		}
		finally	{
			setWaitCursor(false);
		}
	}


	private void checkConditionsAndReplace(boolean all)	{
		if (searchConditionsChanged())	{
			System.err.println("WARNING: search conditions changed, searching again!");
			search();
		}

		setWaitCursor(true);
		try	{
			if (replace(all))	{
				setTextChanged();
				search();
			}
		}
		finally	{
			setWaitCursor(false);
		}
	}

	/** Replace or replace all was clicked by user. Returns true if a replacement has been done. */
	protected abstract boolean replace(boolean all);

}