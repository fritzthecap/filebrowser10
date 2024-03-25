package fri.util.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implements a natural sort order where numbers within names
 * are detected and evaluated as numbers, not strings.
 * Thus "a1", "a10", "a2" would be sorted as "a1", "a2", "a10".
 * See unit test for more examples.
 * 
 * @author fritzthecat Oct 2015
 */
public class NaturalSortComparator4 implements Comparator
{
    /**
     * String part that implements a comparison according to its nature.
     */
    private class Part implements Comparable
    {
        private final String content;
        private final int leadingZeros;
        private final Long number;
        
        Part(boolean isNumber, String content) {
            int i = 0;
            if (isNumber)   // remove and count leading zeros when number
                while (i < content.length() && content.charAt(i) == '0')
                    i++;

            this.content = (i == 0) ? content : content.substring(i);
            this.leadingZeros = i;
            this.number = isNumber ? Long.valueOf(content) : null;
        }
        
        /**
         * Compares this part with given one, according to their nature
         * either as numbers or others. This has to be fast.
         */
        public int compareTo(Object o) {
            Part other = (Part) o;
            if (number != null && other.number != null)  {
                int result = number.compareTo(other.number);
                if (result != 0)
                    return result;
                
                return compare(leadingZeros, other.leadingZeros);
            }
            
            return caseSensitive
                    ? content.compareTo(other.content)
                    : content.compareToIgnoreCase(other.content);
        }
        
        private int compare(int x, int y) {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }
    
    
    private final boolean caseSensitive;
    private final boolean ignoreSpaces;
    
    private Map cache = new Hashtable();
    
    private final StringBuffer sb = new StringBuffer();

    
    /** Comparator that compares case-insensitive. */
    public NaturalSortComparator4() {
        this(false);
    }
    
    /** Comparator that treats case-sensitivity according to given parameter. */
    public NaturalSortComparator4(boolean caseSensitive) {
        this(caseSensitive, true);
    }
    
    /** Comparator that treats case-sensitivity according to given parameter, and optionally does not ignore spaces. */
    public NaturalSortComparator4(boolean caseSensitive, boolean ignoreSpaces) {
        this.caseSensitive = caseSensitive;
        this.ignoreSpaces = ignoreSpaces;
    }
    
    
    /**
     * Splits the given strings and compares them
     * by delegating to the Comparable parts.
     */
    public int compare(Object o1, Object o2) {
        String string1 = (String) o1;
        String string2 = (String) o2;
        final Iterator iterator1 = split(string1).iterator();
        final Iterator iterator2 = split(string2).iterator();
        
        while (iterator1.hasNext() || iterator2.hasNext()) {
            // first has no more parts -> comes first
            if ( ! iterator1.hasNext() && iterator2.hasNext())
                return -1;
            
            // second has no more parts -> comes first
            if (iterator1.hasNext() && ! iterator2.hasNext())
                return 1;

            // compare next parts
            Comparable next1 = (Comparable) iterator1.next();
            Comparable next2 = (Comparable) iterator2.next();
            int result = next1.compareTo(next2);
            if (result != 0)    // found difference
                return result;
        }
        
        return 0;   // are equal
    }

    
    /** Splits given string into a list of numbers and other parts. */
    private List split(String string) {
        final List cachedList = (List) cache.get(string);
        if (cachedList != null) // cache results to be fast
            return cachedList;
        
        final List list = new ArrayList();
        boolean digits = false;
        
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            final boolean ignore = (ignoreSpaces && Character.isWhitespace(c));
            final boolean isDigit = (ignore == false && Character.isDigit(c));
            
            if (isDigit != digits)    { // state change
                closeCurrentPart(list, digits);
                digits = isDigit;
            }
            
            if (ignore == false)
                sb.append(c);
        }
        closeCurrentPart(list, digits);
        
        cache.put(string, list);
        return list;
    }

    private void closeCurrentPart(List list, boolean digits) {
        if (sb.length() > 0)    { // close current string part
            list.add(new Part(digits, sb.toString()));
            sb.setLength(0);
        }
    }

}
