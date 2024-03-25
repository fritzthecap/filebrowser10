package fri.util.activation;

/**
  Copied from com.sun.activation.registries.MailcapTokenizer.<br>
  Fixed bug with escaping autoquote character.
*/

public class MailcapTokenizer
{
    public static final int UNKNOWN_TOKEN = 0;
    public static final int START_TOKEN = 1;
    public static final int STRING_TOKEN = 2;
    public static final int EOI_TOKEN = 5;
    public static final int SLASH_TOKEN = 47;
    public static final int SEMICOLON_TOKEN = 59;
    public static final int EQUALS_TOKEN = 61;
    private String data;
    private int dataIndex;
    private int dataLength;
    private int currentToken;
    private String currentTokenValue;
    private boolean isAutoquoting;
    private char autoquoteChar;


    public MailcapTokenizer(String s)
    {
        data = s;
        dataIndex = 0;
        dataLength = s.length();
        currentToken = START_TOKEN;
        currentTokenValue = "";
        isAutoquoting = false;
        autoquoteChar = ';';
    }

    public void setIsAutoquoting(boolean flag)
    {
        isAutoquoting = flag;
    }

    public void setAutoquoteChar(char c)
    {
        autoquoteChar = c;
    }

    public int getCurrentToken()
    {
        return currentToken;
    }

    public static String nameForToken(int i)
    {
        String s = "really unknown";
        switch(i)
        {
        case UNKNOWN_TOKEN: // '\0'
            s = "unknown";
            break;

        case START_TOKEN: // '\001'
            s = "start";
            break;

        case STRING_TOKEN: // '\002'
            s = "string";
            break;

        case EOI_TOKEN: // '\005'
            s = "EOI";
            break;

        case SLASH_TOKEN: // '/'
            s = "'/'";
            break;

        case SEMICOLON_TOKEN: // ';'
            s = "';'";
            break;

        case EQUALS_TOKEN: // '='
            s = "'='";
            break;

        }
        return s;
    }

    public String getCurrentTokenValue()
    {
        return currentTokenValue;
    }

    public int nextToken()
    {
        if(dataIndex < dataLength)
        {
            for(; dataIndex < dataLength && isWhiteSpaceChar(data.charAt(dataIndex)); dataIndex++);
            if(dataIndex < dataLength)
            {
                char i = data.charAt(dataIndex);
                if(isAutoquoting)
                {
                    if(!isAutoquoteSpecialChar(i))
                        processAutoquoteToken();
                    else
                    if(i == SEMICOLON_TOKEN || i == EQUALS_TOKEN)
                    {
                        currentToken = i;
                        currentTokenValue = (new Character(i)).toString();
                        dataIndex++;
                    }
                    else
                    {
                        currentToken = UNKNOWN_TOKEN;
                        currentTokenValue = (new Character(i)).toString();
                        dataIndex++;
                    }
                }
                else
                if(isStringTokenChar(i))
                    processStringToken();
                else
                if(i == SLASH_TOKEN || i == SEMICOLON_TOKEN || i == EQUALS_TOKEN)
                {
                    currentToken = i;
                    currentTokenValue = (new Character(i)).toString();
                    dataIndex++;
                }
                else
                {
                    currentToken = UNKNOWN_TOKEN;
                    currentTokenValue = (new Character(i)).toString();
                    dataIndex++;
                }
            }
            else
            {
                currentToken = EOI_TOKEN;
                currentTokenValue = null;
            }
        }
        else
        {
            currentToken = EOI_TOKEN;
            currentTokenValue = null;
        }
        return currentToken;
    }

    private void processStringToken()
    {
        int i = dataIndex;
        for(; dataIndex < dataLength && isStringTokenChar(data.charAt(dataIndex)); dataIndex++);
        currentToken = STRING_TOKEN;
        currentTokenValue = data.substring(i, dataIndex);
    }

    private void processAutoquoteToken()
    {
        int i = dataIndex;
				// Fixed bug with escaping autoquote character.
        boolean masked = false;
        for(boolean flag = false; dataIndex < dataLength && !flag;)
        {
            char c = data.charAt(dataIndex);

            if(c == '\\')
            	masked = !masked;

            if(masked || c != autoquoteChar)
                dataIndex++;
            else
                flag = true;

            if(c != '\\')
            	masked = false;
        }

        currentToken = STRING_TOKEN;
        currentTokenValue = fixEscapeSequences(data.substring(i, dataIndex));
    }

    public static boolean isSpecialChar(char c)
    {
        boolean flag = false;
        switch(c)
        {
        case 34: // '"'
        case 40: // '('
        case 41: // ')'
        case 44: // ','
        case SLASH_TOKEN: // '/'
        case 58: // ':'
        case SEMICOLON_TOKEN: // ';'
        case 60: // '<'
        case EQUALS_TOKEN: // '='
        case 62: // '>'
        case 63: // '?'
        case 64: // '@'
        case 91: // '['
        case 92: // '\\'
        case 93: // ']'
            flag = true;
            break;

        }
        return flag;
    }

    public static boolean isAutoquoteSpecialChar(char c)
    {
        boolean flag = false;
        switch(c)
        {
        case SEMICOLON_TOKEN: // ';'
        case EQUALS_TOKEN: // '='
            flag = true;
            // fall through

        case 60: // '<'
        default:
            return flag;

        }
    }

    public static boolean isControlChar(char c)
    {
        return Character.isISOControl(c);
    }

    public static boolean isWhiteSpaceChar(char c)
    {
        return Character.isWhitespace(c);
    }

    public static boolean isStringTokenChar(char c)
    {
        return !isSpecialChar(c) && !isControlChar(c) && !isWhiteSpaceChar(c);
    }

    private static String fixEscapeSequences(String s)
    {
        int i = s.length();
        StringBuffer stringbuffer = new StringBuffer();
        stringbuffer.ensureCapacity(i);
        for(int j = 0; j < i; j++)
        {
            char c = s.charAt(j);
            if(c != '\\')
                stringbuffer.append(c);
            else
            if(j < i - 1)
            {
                char c1 = s.charAt(j + 1);
                stringbuffer.append(c1);
                j++;
            }
            else
            {
                stringbuffer.append(c);
            }
        }

        return stringbuffer.toString();
    }

}