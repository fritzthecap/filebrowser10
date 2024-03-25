package fri.util.date.calendar;

/**
	Computes the easter sunday for an input year.
*/

public abstract class EasterSunday
{
	private EasterSunday()	{}

	/**
		Returns an int[3] array that contains:
		<ul>
			<li>0=year (1 - 2299, restriction by Gauss)</li>
			<li>1=month (1-12)</li>
			<li>2=day (1-31)</li>
		</ul>
		which represent the easter sunday for given year.
		@param year year for which to compute easter sunday
		@return array of int[3] containing year, month, day, or null if
			year not between 1583 and 2299
	*/
	public static int [] date(int annum)	{
		if (annum < 1 || annum > 2299)
			throw new IllegalArgumentException("Can not calculate easter sunday for year "+annum);
		
		int K  = annum / 100;
		int M, S;
		if (annum > 1582)	{
			M  = 15 + (3 * K + 3) / 4 - (8 * K + 13) / 25;
			S  =  2 - (3 * K + 3) / 4;
		}
		else	{
			M  = 15;
			S  =  0;
		}

		int A  =  annum % 19;
		int D  =  (19 * A + M) % 30;
		int R  =  D / 29 + (D / 28 - D / 29) * (A / 11);
		int OG = 21 + D - R;
		int SZ = 7 - (annum + annum / 4 + S) % 7;
		int OE = 7 - (OG - SZ) % 7;

		int dies = OG + OE;
		int mon = 3;
		if (dies > 31)	{
			dies = dies - 31;
			mon = 4;
		}
		
		return new int [] { annum, mon, dies };
	}


	// test main
	public static void main(String [] args)	{
		if (args.length <= 0)	{
			int y = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
			int [] arr = date(y);
			System.err.println(arr[0]+"-"+arr[1]+"-"+arr[2]);
		}
		else	{
			for (int i = 0; i < args.length; i++)	{
				int y = Integer.valueOf(args[i]).intValue();
				int [] arr = date(y);
				System.err.println(arr[0]+"-"+arr[1]+"-"+arr[2]);
			}
		}
	}

}

/*
	public static int [] date5(int year)	{
		int nMonth, nDay, nMoon, nEpact, nSunday, nGold, nCent, nCorx, nCorz;
		
		nGold = ((year % 19) + 1);
		nCent = (int) ((Math.floor(year / 100)) + 1);
		nCorx = (int) ((Math.floor((3 * nCent) / 4)) - 12);
		nCorz = (int) ((Math.floor((8 * nCent + 5) / 25)) - 5);
		nSunday = (int) ((Math.floor((5 * year) / 4)) - nCorx - 10);
		nEpact = (((11 * nGold) + 20 + nCorz - nCorx) % 30);
		
		if (nEpact < 0)
			nEpact = nEpact + 30;
		    
		if (((nEpact == 25) && (nGold > 11)) || (nEpact == 24))
			nEpact = (nEpact + 1);
		
		nMoon = 44 - nEpact;
		
		if (nMoon < 21)
			nMoon = nMoon + 30;
		
		nMoon = (nMoon + 7 - ((nSunday + nMoon) % 7));
		
		if (nMoon > 31)	{
			nMonth = 4;
			nDay = (nMoon - 31);
		}
		else	{
			nMonth = 3;
			nDay = nMoon;
		}
		
		return new int [] { year, nMonth, nDay };
	}


	public static int [] date2(int year)	{
		int x = year;
		
		//1. K = INT (X / 100);
		int k = x / 100;
		
		//2. M = 15 + INT ((3 * K + 3) / 4) - INT ((8 * K + 13) / 25);
		int m = 15 + (3 * k + 3) / 4 - (8 * k + 13) / 25;
		
		//3. S = 2 - INT ((3 * K + 3) / 4);
		int s = 2 - (3 * k + 3) / 4;
		
		//4. A = MOD (X,19);
		int a = x % 19;
		
		//5. D = MOD (19 * A + M, 30);
		int d = (19 * a + m) % 30;
		
		//6. R = INT (D / 29) + (INT (D / 28) - INT (D / 29)) * INT (A / 11);
		int r = d / 29 + (d / 28 - d / 29) * (a / 11);
		
		//7. OG = 21 + D - R;
		int og = 21 + d - r;
		
		//8. SZ = 7 - MOD (X + INT (X / 4) + S, 7);
		int sz = 7 - (x + x / 4 + s) % 7;
		
		//9. OE = 7 - MOD (OG - SZ, 7);
		int oe = 7 - (og - sz) % 7;
		
		// Dann ist OS = OG + OE das Datum des Ostersonntags, als Datum im Monat März dargestellt.
		// (Der 32. März entspricht dem 1. April).
		int os = og + oe;
		int month = 3;
		if (os > 31)	{
			os = os - 31;
			month = 4;
		}
		
		return new int [] { year, month, os };
	}


	public static int [] date3(int year)	{
		//var a, b, c, d, e, f, g, h, i, k, l, m, n, p: Integer;
		int a, b, c, d, e, f, g, h, i, k, l, m, n, p;
		
		//  a := Year mod 19;
		a = year % 19;
		
		//  b := Year div 100;
		b = year / 100;
		
		//  c := Year mod 100;
		c = year % 100;

		//  d := b div 4;
		d = b / 4;

		//  e := b mod 4;
		e = b % 4;
		
		//  f := (b + 8) div 25;
		f = (b + 8) / 25;

		//  g := (b - f + 1) div 3;
		g = (b - f + 1) / 3;
		
		//  h := (19 * a + b - d - g + 15) mod 30;
		h = (19 * a + b - d - g + 15) % 30;
		
		//  i := c div 4;
		i = c / 4;
		
		//  k := c mod 4;
		k = c % 4;
		
		//  l := (32 + 2 * e + 2 * i - h - k) mod 7;
		l = (32 + 2 * e + 2 * i - h - k) % 7;
		
		//  m := (a + 11 * h + 22 * l) div 451;
		m = (a + 11 * h + 22 * l) / 451;
		
		//  n := (h + l - 7 * m + 114) div 31;
		n = (h + l - 7 * m + 114) / 31;
		
		//  p := (h + l - 7 * m + 114) mod 31;
		p = (h + l - 7 * m + 114) % 31;
		
		//  Jetzt enthält n die Monatsnummer (also 3 = März bzw. 4 = April),
		//  und (p + 1) gibt den Tag an, auf den in diesem Monat das Osterdatum fällt.
		//  Result := EncodeDate(Year, n, p + 1);
		
		return new int [] { year, n, p + 1 };
	}
	
	
	public static int [] date4(int year)	{
		// ****************************************************************
		// *  Gauss'sche Regel (Gaussian Rule)                            *
		// *  ================================                            *
		// *  Quelle / Source:                                            *
		// *  H. H. Voigt, "Abriss der Astronomie", Wissenschaftsverlag,  *
		// *  Bibliographisches Institut, Seite 9.                        *
		// ****************************************************************
		
		int a, b, c, d, e, m, n, day, month;
		
		if (year < 1583 || year > 2299)
			return null;
		
		if (year < 1700)	{
			m = 22;
			n = 2;
		}
		else
		if (year < 1800)	{
			m = 23;
			n = 3;
		}
		else
		if (year < 1900)	{
			m = 23;
			n = 4;
		}
		else
		if (year < 2100)	{
			m = 24;
			n = 5;
		}
		else
		if (year < 2200)	{
			m = 24;
			n = 6;
		}
		else	{
			m = 25;
			n = 0;
		}
		
		a = year % 19;
		b = year % 4;
		c = year % 7;
		d = (19 * a + m) % 30;
		e = (2 * b + 4 * c + 6 * d + n) % 7;
		day = 22 + d + e;
		month = 3;

		if (day > 31)	{
			day -= 31; // same as *day = d + e - 9;
			month++;
		}

		if (day == 26 && month == 4)
			day = 19;

		if (day == 25 && month == 4 && d == 28 && e == 6 && a > 10)
			day = 18;

		return new int [] { year, month, day };
	}



	// test main
	public static void main(String [] args)	{
		// algorithm test
		fri.util.TimeStopper timer1 = new fri.util.TimeStopper(false);
		fri.util.TimeStopper timer2 = new fri.util.TimeStopper(false);
		fri.util.TimeStopper timer3 = new fri.util.TimeStopper(false);
		fri.util.TimeStopper timer4 = new fri.util.TimeStopper(false);
		fri.util.TimeStopper timer5 = new fri.util.TimeStopper(false);
		
		for (int cnt = 0; cnt < 100; cnt++)	{
			boolean error = false;

			//for (int y = 0; y < 2299; y++)	{
			for (int y = 1583; y < 2299; y++)	{
			//for (int y = 1999; y < 2000; y++)	{
				timer1.resume();
				int [] arr1 = date(y);
				timer1.suspend();
				//System.err.println("timer 1 = "+timer1.stopMillis());

				timer2.resume();
				int [] arr2 = date2(y);
				timer2.suspend();
				//System.err.println("timer 2 = "+timer2.stopMillis());

				timer3.resume();
				int [] arr3 = date3(y);
				timer3.suspend();
				//System.err.println("timer 3 = "+timer3.stopMillis());

				timer4.resume();
				int [] arr4 = date5(y);
				timer4.suspend();
				//System.err.println("timer 4 = "+timer4.stopMillis());

				for (int i = 1; i < 3; i++)	{
					if (arr1[i] != arr2[i] ||
							arr1[i] != arr3[i] ||
							arr1[i] != arr4[i] ||
							arr2[i] != arr3[i] ||
							arr2[i] != arr4[i] ||
							arr3[i] != arr4[i])
					{
						System.err.println("detected algorithm diffs: "+y);
						System.err.println(fri.util.ArrayUtil.print(arr1));
						System.err.println(fri.util.ArrayUtil.print(arr2));
						System.err.println(fri.util.ArrayUtil.print(arr3));
						System.err.println(fri.util.ArrayUtil.print(arr4));
						error = true;
					}
				}
			}

			if (error)
				System.exit(1);
		}
		System.err.println("timer 1 = "+timer1.stopMillis());
		System.err.println("timer 2 = "+timer2.stopMillis());
		System.err.println("timer 3 = "+timer3.stopMillis());
		System.err.println("timer 4 = "+timer4.stopMillis());
	}
}
*/