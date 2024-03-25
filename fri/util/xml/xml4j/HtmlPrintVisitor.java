package fri.util.xml.xml4j;

import java.io.*;
import org.w3c.dom.*;
import com.ibm.xml.parser.*;
import fri.util.text.Replace;

/**
	XML4J dependent class.
	Outputs HTML representation of XML, without DTD and PI's.
*/
class HtmlPrintVisitor extends FormatPrintVisitor
{
	private int isTextattributationHtmlTag;
	
	public HtmlPrintVisitor(Writer writer, String encoding)	{
		super(writer, encoding);
	}

	public void visitDocumentPre(TXDocument document)	{
		try	{
			writer.write("<HTML>\n");
			writer.write("<HEAD></HEAD>\n");
			writer.write("<BODY>\n");
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
	}

	public void visitDocumentPost(TXDocument document)	{
		try	{
			writer.write("</BODY>\n");
			writer.write("</HTML>\n");
			writer.flush();
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
	}

	public void visitCommentPost(TXComment comment)	{
		String c = comment.getNodeValue();
		if (c != null && c.trim().length() > 0)	{
			c = Util.backReference(c, encoding);
			c = Replace.replace(c, "\n", "<br>");
			try	{
				writer.write("<font size=-1 color=\"rgb(150,150,150)\">"+c+"</font>");
			}
			catch (IOException e)	{
				e.printStackTrace();
			}
		}
	}
	
	public void visitElementPre(TXElement element)	{
		try	{
			if (isTextattributationHtmlTag(element.getNodeName()))
				isTextattributationHtmlTag++;
				
			if (isTextattributationHtmlTag <= 0)	{	// do not show primitive HTML tags as elements but let them take part in HTML representation
				writer.write("<BLOCKQUOTE>\n");
				NamedNodeMap attributes = element.getAttributes();
				boolean hasAttributes = attributes != null && attributes.getLength() > 0;
				writer.write("<B>"+element.getNodeName()+"</B>"+(hasAttributes ? "<TABLE BORDER=1 CELLPADDING=2>\n" : "<BR>\n"));
				for (int i = 0; attributes != null && i < attributes.getLength(); i++)	{
					Attr attr = (Attr)attributes.item(i);
					writer.write("<TR><TD>"+attr.getNodeName()+"</TD><TD>"+attr.getNodeValue()+"</TD></TR>\n");
				}
				if (hasAttributes)
					writer.write("</TABLE>\n");
			}
			else	{	// is XHTML text attributation
				writer.write("<"+element.getNodeName());
				
				if (element.getNodeName().equalsIgnoreCase("a"))	{
					String link = element.getAttribute("href");
					if (link != null)
						writer.write(" href=\""+link+"\"");
				}
				
				writer.write(">");
			}
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
	}

	public void visitElementPost(TXElement element)	{
		try	{
			if (isTextattributationHtmlTag <= 0)	{
				writer.write("</BLOCKQUOTE>\n");
			}
			else	{
				writer.write("</"+element.getNodeName()+">");
				isTextattributationHtmlTag--;
			}
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
	}

	public void visitTextPre(TXText text)	{
		try	{
			if (text instanceof CDATASection)	{
				writer.write("<PRE>"+text.getNodeValue()+"</PRE>\n");
			}
			else	{
				String t = text.getNodeValue().trim();
				t = Replace.replace(t, "\n", "<br>");
				writer.write(t+"\n");
			}
		}
		catch (IOException e)	{
			e.printStackTrace();
		}
	}

	public void visitGeneralReferencePre(GeneralReference generalReference)	{
	}
	public void visitAttlistPre(Attlist attlist)	{
	}
	public void visitAttDefPre(AttDef attDef)	{
	}
	public void visitDTDPost(DTD dtd)	{
	}
	public void visitDTDPre(DTD dtd)	{
	}
	public void visitElementDeclPre(ElementDecl elementDecl)	{
	}
	public void visitNotationPre(TXNotation notation)	{
	}
	public void visitPIPre(TXPI pi)	{
	}

	private boolean isTextattributationHtmlTag(String tag)	{
		return
			tag.equalsIgnoreCase("a") ||
			tag.equalsIgnoreCase("p") ||
			tag.equalsIgnoreCase("b") ||
			tag.equalsIgnoreCase("i") ||
			tag.equalsIgnoreCase("u") ||
			tag.equalsIgnoreCase("font") ||
			tag.equalsIgnoreCase("hr") ||
			tag.equalsIgnoreCase("pre") ||
			//tag.equalsIgnoreCase("center") ||
			tag.equalsIgnoreCase("ul") ||
			tag.equalsIgnoreCase("ol") ||
			tag.equalsIgnoreCase("d1") ||
			tag.equalsIgnoreCase("li") ||
			tag.equalsIgnoreCase("h1") ||
			tag.equalsIgnoreCase("h2") ||
			tag.equalsIgnoreCase("h3") ||
			tag.equalsIgnoreCase("h4") ||
			tag.equalsIgnoreCase("h5") ||
			tag.equalsIgnoreCase("h6");
	}

}
