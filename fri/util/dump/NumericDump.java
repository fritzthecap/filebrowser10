package fri.util.dump;

import java.util.*;

/**
	NumericDump produces old-fashioned "hex-dumps", that have
	16 characters encoded as numbers, and to the right of every line
	the ASCII representation, if printable, else a dot.
	The result is formatted with (Java) newlines, not with platform newlines!
*/

public class NumericDump
{
	/** Convert negative bytes -1 .. -128 to numbers 128 .. 256 */
	public boolean do2Complement = true;
	private String s = null;
	private byte [] bytes;
	private int lineLength;
	private int base;
	private int count;
	
	
	/**
		Prepare a numeric dump to be produced by toString().
		It will have 16 chars per line and will be hexadecimal.
	*/
	public NumericDump(byte [] bytes)	{
		this(bytes, 16);
	}

	/**
		Prepare a numeric dump to be produced by toString().
		It will have 16 chars per line and will be of the passed base.
	*/
	public NumericDump(byte [] bytes, int base)	{
		this(bytes, bytes.length, base);
	}
	public NumericDump(byte [] bytes, int count, int base)	{
		this(bytes, count, base, 16);
	}
	
	/**
		Prepare a numeric dump to be produced by toString().
		It will have the passed number of chars per line and will be of the passed base.
	*/
	public NumericDump(byte [] bytes, int count, int base, int lineLength)	{
		if (base != 2 && base != 8 && base != 10 && base != 16)
			throw new IllegalArgumentException("NumericDump, base must be 2, 8, 10 or 16");

		if (lineLength <= 0)
			throw new IllegalArgumentException("NumericDump, line length must be > 0");
			
		this.bytes = bytes;
		this.count = count;
		this.base = base;
		this.lineLength = lineLength;
	}
	

	public String toString()	{
		if (s != null)
			return s;
			
		String text;
		String addrSep = ": ";
		StringBuffer sb = new StringBuffer(count > 0 ? "       0"+addrSep : "");
		StringBuffer chars = new StringBuffer(16);
		
		int i = 0;	// we need i after loop
			
		byte b;
		int ib;
		String prefix = " ";
		
		for (; i < count; i++)	{
			b = bytes[i];
			
			if (b < 0)	{
				if (do2Complement == false)	{
					ib = (int)b;
					prefix = "-";
				}
				else	{
					ib = (int)(256 + b);
				}
			}
			else	{
				ib = (int)b;
				if (do2Complement == false)	// reset prefix
					prefix = " ";
			}
			
			text = convert(ib, base);
			sb.append(ensureLength(prefix, text));

			chars.append(b >= 32 && b < 127 ? (char)b : '.');
			
			if ((i + 1) % lineLength == 0)	{
				sb.append("    ");
				sb.append(chars.toString());
				chars.setLength(0);

				sb.append('\n');
				
				if (i < count - 1)	{
					String number = convert(i + 1, base);
	
					if (number.length() < 2)       sb.append("       ");
					else if (number.length() < 3)  sb.append("      ");
					else if (number.length() < 4)  sb.append("     ");
					else if (number.length() < 5)  sb.append("    ");
					else if (number.length() < 6)  sb.append("   ");
					else if (number.length() < 7)  sb.append("  ");
					else if (number.length() < 8)  sb.append(" ");
	
					sb.append(number);
					sb.append(addrSep);
				}
			}
		}
		
		if (chars.length() > 0)	{	// do the rest
			int blanks = lineLength - (i % lineLength);
			for (int j = 0; j < blanks; j++)	{
				sb.append(ensureLength(" ", " "));
			}

			sb.append("    ");
			sb.append(chars.toString());	// append StringBuffer only since JDK 1.4
		}
		
		return s = sb.toString();
	}


	private String ensureLength(String pre, String s)	{
		return pre+ensureLength(s, base);
	}


	/** Fills with leading spaces, with zeros in the case of binary base (2). */
	public static String ensureLength(String s, int base)	{	// long but fast
		if (base == 16)	{
			if (s.length() == 1)	s = " "+s;
		}
		else
		if (base == 8 || base == 10)	{
			if (s.length() == 1)	s = "  "+s;
			else
			if (s.length() == 2)	s = " "+s;
		}
		else
		if (base == 2)	{
			boolean space = s.equals(" ");
			if (s.length() == 1)
				if (space) s = "       "+s; else	s = "0000000"+s;
			else
			if (s.length() == 2)
				if (space) s = "      "+s; else s = "000000"+s;
			else
			if (s.length() == 3)
				if (space) s = "     "+s; else s = "00000"+s;
			else
			if (s.length() == 4)
				if (space) s = "    "+s; else s = "0000"+s;
			else
			if (s.length() == 5)
				if (space) s = "   "+s; else s = "000"+s;
			else
			if (s.length() == 6)
				if (space) s = "  "+s; else s = "00"+s;
			else
			if (s.length() == 7)
				if (space) s = " "+s; else s = "0"+s;
		}
		return s;
	}

	

	/**
		Converts a byte to a String, regarding binary complement. Does not fill with zero or spaces.
		@param b byte to convert to String
		@param base radix between Character.MIN_RADIX and Character.MAX_RADIX.
		@return String representation of byte, regarding binary complement.
	*/
	public static String byteToString(byte b, int base)	{
		//int ib = (b < 0) ? (int)(256 + b) : (int)b;
		return convert(byteToChar(b), base);
	}
	
	private static String convert(int ib, int base)	{
		String s;
		if (base == 16)
			s = Integer.toHexString(ib);
		else
		if (base == 10)
			s = Integer.toString(ib);
		else
		if (base == 8)
			s = Integer.toOctalString(ib);
		else
		if (base == 2)
			s = Integer.toBinaryString(ib);
		else
			s = Integer.toString(ib, base);
			
		return s;
	}
	
	/**
		Converts a String to a byte, regarding binary complement.
		@param s String to convert to byte
		@param base radix between Character.MIN_RADIX and Character.MAX_RADIX.
		@return byte representation of String, regarding binary complement.
	*/
	public static byte stringToByte(String s, int base)	{
		int ib = Integer.parseInt(s, base);
		if (ib > Byte.MAX_VALUE)
			ib -= 256;
			
		return (byte)ib;
	}

	public static byte charToByte(char c)	{
		return (c > Byte.MAX_VALUE) ? (byte)(c - 256) : (byte)c;
	}
	
	public static char byteToChar(byte b)	{
		return (b < 0) ? (char)(256 + b) : (char)b;
	}
	


	/**
		Converts the passed byte array to a number string, bytes separated by one space,
		newline after 23 bytes, numbers based on given radix (base).
	*/
	public static String toNumberString(byte [] bytes, int base, int newlineAfterBytes)	{
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < bytes.length; i++)	{
			if (i > 0)
				if (newlineAfterBytes > 0 && i % newlineAfterBytes == 0)	// 3 chars per byte, mail line count is 70
					sb.append('\n');
				else
					sb.append(' ');

			sb.append(NumericDump.ensureLength(NumericDump.byteToString(bytes[i], base), base));
		}
		
		return sb.toString();
	}

	/**
		Converts the passed String to a byte array. Given text bytes separated by space,
		numbers based on given radix (base).
	*/
	public static byte [] fromNumberString(String text, int base)	{
		StringTokenizer stok = new StringTokenizer(text);
		Vector v = new Vector();
		
		while (stok.hasMoreTokens())	{
			String s = stok.nextToken();
			byte b = NumericDump.stringToByte(s, base);
			v.add(new Byte(b));
		}
		
		byte [] result = new byte[v.size()];
		for (int i = 0; i < v.size(); i++)
			result[i] = ((Byte)v.get(i)).byteValue();
			
		return result;
	}



	/* test main 
	public static void main(String [] args)	{
		if (args.length <= 0)	{
			byte [] bytes = new byte[256];
			for (int i = Byte.MIN_VALUE, j = 0; i <= Byte.MAX_VALUE; i++, j++)
				bytes[j] = (byte)i;
			System.out.println(new NumericDump(bytes));
			System.err.println("SYNTAX: "+NumericDump.class+" file|string [file|string ...]");
		}
		else	{
			for (int i = 0; i < args.length; i++)	{
				File f = new File(args[i]);
	
				if (f.isFile())	{
					try	{
						byte [] bytes = new byte[(int)f.length()];
						InputStream in = new BufferedInputStream(new FileInputStream(f));
						in.read(bytes);
						if (args.length > 1)
							System.out.println("================== "+f+" ==================");
						System.out.println(new NumericDump(bytes));
					}
					catch (Exception e)	{
						e.printStackTrace();
					}
				}
				else	{
					System.out.println(new NumericDump(args[i].getBytes()));
				}
			}
		}
	}
	*/

}