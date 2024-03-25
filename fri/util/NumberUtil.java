package fri.util;

import java.text.NumberFormat;

public abstract class NumberUtil
{
	private static NumberFormat numberFormater = NumberFormat.getInstance();


	private NumberUtil()	{
	}	


	/**
		Convert the passed number to a String and add leading zeros until the String is finalWidth wide.
		If the number is wider than finalWidth, the String is NOT reduced to that size!
		This can be used to create sortable numbers ("09", "19" versus "19", "9").
	*/
	public static String convertWithLeadingZeros(int number, int width)	{
		String n = Integer.toString(number);
		int len = width - n.length();
		for (int i = 0; i < len; i++)
			n = "0"+n;
		return n;
	}
	
	
	
	/** Formatiere eine ganze Zahl in einen String mit Tausender-Punkten */
	public static String printNumber(int i)	{
		return numberFormater.format(i);
	}
	
	/** Formatiere eine Kommazahl in einen String mit vernuenftig vielen Kommastellen */
	public static String printNumber(double f)	{
		/*if (f >= 1000.0)
			numberFormater.setMaximumFractionDigits(0);
		else
		if (f >= 100.0)
			numberFormater.setMaximumFractionDigits(1);
		else
		if (f >= 10.0)
			numberFormater.setMaximumFractionDigits(2);
		else
			numberFormater.setMaximumFractionDigits(3);*/			
		if (f >= 100.0)
			numberFormater.setMaximumFractionDigits(0);
		else
		if (f >= 10.0)
			numberFormater.setMaximumFractionDigits(1);
		else
			numberFormater.setMaximumFractionDigits(2);
			
		return numberFormater.format(f);
	}


	/** 
		Verwandle von Byte-Anzahl in besser lesbare Byte/KB/MB/GB Angabe.
		Ist die Zahl <= 1 wird sie angezeigt.
 */
	public static String getFileSizeString(long size)	{
		return getFileSizeString(size, true);
	}
	
	/** 
		Verwandle von Byte-Anzahl in besser lesbare Byte/KB/MB/GB Angabe.
		@param showZero wenn true und die Zahl <= 1 wird sie angezeigt, wenn false
			und die Zahl <= 1 wird Leerstring geliefert.
 */
	public static String getFileSizeString(long size, boolean showZero)	{
		String len;
		//int l = Math.round((float)size / (float)1024);
		float l = (float)size / (float)1024;
		if (l <= 1.0)	{
			if (size > 0 || showZero)
				len = String.valueOf(size)+" Bytes";
			else
				len = "";
		}
		else	{
			//l = Math.round((float)size / (float)1048576);
			float l1 = (float)size / (float)1048576;
			if (l1 <= 1.0)
				len = printNumber((double)l)+" KB";
			else	{
				//l = Math.round((float)size / (float)1073741824);
				float l2 = (float)size / (float)1073741824;
				if (l2 <= 1.0)
					len = printNumber((double)l1)+" MB";
				else
					len = printNumber((double)l2)+" GB";
			}
		}
		return len;
	}

}
