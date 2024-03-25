package fri.util.xml.xml4j;

import java.io.Writer;
import java.util.Vector;
import com.ibm.xml.parser.*;

/**
	XML4J dependent class.<br>
	Bug fix for FormatPrintVisitor, that escapes nested
	general entities in entity-declarations.
*/

class FormatPrintVisitorFix extends FormatPrintVisitor
{
    public FormatPrintVisitorFix(Writer writer, String encoding, int indent)	{
    	super(writer, encoding, indent);
	}


    /** Bug fix for leaving nested entites unchanged. */
    public void visitEntityDeclPre(EntityDecl entitydecl)
        throws Exception
    {
        Util.printSpace(writer, currentIndent);	// from FormatPrintVisitor

        writer.write("<!ENTITY ");	// from ToXMLStringVisitor
        if(entitydecl.isParameter())
            writer.write("% ");
        writer.write(entitydecl.getNodeName() + " ");
        if(entitydecl.getValue() != null)
        {
            writer.write("\"" + backReferenceForEntity(entitydecl.getValue(), encoding) + "\">");
        }
        else
        {
            writer.write(entitydecl.getExternalID().toString());
            if(entitydecl.getNotationName() != null)
                writer.write(" NDATA " + entitydecl.getNotationName());
            writer.write(">");
        }

        writer.write(10);
        throw new ToNextSiblingTraversalException();
    }


	/**
		Bypass entities in entity declarations.
		if &amp; is not part of an entitity-reference or character-reference.
	*/
	private String backReferenceForEntity(String value, String encoding)	{
		Vector v = splitByEntities(value);
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < v.size(); i++)	{
			String s = (String)v.get(i);

			if (s.charAt(0) == '&')	{	// entity
				sb.append(s);
			}
			else	{	// other text
				sb.append(Util.backReferenceForEntity(s, encoding));
			}
		}

		return sb.toString();
	}


	// split the text into entities and text between. */
	private Vector splitByEntities(String str)	{
		StringBuffer buf = new StringBuffer();
		StringBuffer ent = new StringBuffer();
		int len = str.length();
		Vector v = new Vector();

		for (int pos = 0; pos < len; pos++)	{
			char c = str.charAt(pos);

			switch (c) {
			case '&':
				char c1 = (char)0;
				int i = pos + 1;
				
				for (; i < len && (c1 = str.charAt(i)) != ';' && Character.isWhitespace(c1) == false; i++)	{
					ent.append(c1);
				}
			
				if (ent.length() > 0 && c1 == ';')	{	// entity identified
					if (buf.length() > 0)	{
						v.add(buf.toString());	// pack text until now
						buf.setLength(0);
					}
					v.add("&"+ent+";");
					pos += ent.length() + 1;
					ent.setLength(0);
				}
				else	{
					buf.append(c);
				}
				break;

			default:
				buf.append(c);
				break;
			}
		}

		if (buf.length() > 0)
			v.add(buf.toString());

		return v;
	}

}
