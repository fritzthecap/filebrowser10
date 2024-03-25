package fri.gui.swing.iconbuilder;

import java.awt.Color;
import java.util.Hashtable;
import javax.swing.Icon;

/**
	Icon library. Most icons are 20x16 pixels.
	Usage:<pre>
		Icon icon = Icons.get(Icons.newDocument);
	</pre>
	Demo, showing all icons in this file:<pre>
		java fri.gui.swing.iconbuilder.Icons
	</pre>
	@author Fritz Ritzberger, 2003
*/

public abstract class Icons
{
	private static Hashtable cache = new Hashtable();
	private static IconBuilder builder = new IconBuilder();
	static	{
		builder.setColor(' ', null);
		builder.setColor('b', Color.black);
		builder.setColor('w', Color.white);
		builder.setColor('l', Color.lightGray);
		builder.setColor('g', Color.gray);
		builder.setColor('s', Color.yellow);	// flashlight bright
		builder.setColor('y', new Color(252, 248, 190));	// yellow
		builder.setColor('c', new Color(204, 204, 255));	// blue
		builder.setColor('r', new Color(255, 220, 153));	// red
	}

	
	private Icons()	{
	}
	
	public static Icon get(String [] design)	{
		return get(design, -1, -1);
	}

	public static Icon get(String [] design, int width, int height)	{
		Object icon = null;
		if ((icon = cache.get(design)) == null)	{
			icon = builder.getIcon(design, width, height);
			cache.put(design, icon);
		}
		return (Icon)icon;
	}


	public static final String eye[] = {	// 1
		"          bbbbbb      ",
		"       bbb      bb    ",
		"     bbb  bbbbbb  bb  ",
		"   bb  bbbbwwbbbbb bb ",
		"  bb bbwbbwwwbbbbbb bb",
		"bbb bbwbbbbwwbbbbbbb b",
		"   bbwwbbbbwwbbbbbbwbb",
		"bbbwwwwbbbbbwbbbbbbwwb",
		"    wwwbbbbbwbbbbbbww ",
		"     wwbbbbbbbbbbbbww ",
		"       wbbbbbbbbbbww  ",
		"        bbbbbbbbbbw   ",
		"         bbbbbbbb     ",
		"           bbbb       ",
		"                      ",
		"                      ",
	}; 

	public static final String world[] = {	// 2
		"      bbbbbbb      ",
		"    bbcccgrrrbb    ",
		"   bgccccgrrrrrb   ",
		"  brrgccggrggrrrb  ",
		"  brgcccgrrgcggrb  ",
		" brgcccccggcccrgcb ",
		" bgcccccggrrgggrrb ",
		" bcccccgrrrrrrrggb ",
		" bcccccgrrrrrrrgcb ",
		" bggccccggrrrrgccb ",
		" brrgcccccgrrgcccb ",
		"  brgcccccgrgcccb  ",
		"  brgcccccgrgcccb  ",
		"   bccccccggcccb   ",
		"    bbcccccgcbb    ",
		"      bbbbbbb      ",
	}; 

	public static final String openFolder[] = {	// 4
		"          bb        ",
		"         b  b       ", 
		"   bbbbb    b       ",
		"   byyyb     b      ",       
		" bbbbbbbbbbbbbbbbb  ",
		" blllllllllllblllb  ",
		" blllllllllllblllb  ",
		" bllllllllbbbbbbbl  ",
		" bllbbbbbbybbbbbybbb",
		" bllbyyyyyybbbbbyyyb",
		" blbyyyyyyyybbbyyyb ",
		" blbyyyyyyyybbbyyyb ",
		" bbyyyyyyyyyybyyyb  ",
		" bbyyyyyyyyyyyyyyb  ",
		" bbbbbbbbbbbbbbbb   ",
		"                    ",
	}; 

	public static final String newFolder[] = {	// 5
		"                  ",
		"             s    ", 
		"   bbbbbb    s    ",
		"   byyyyb  s s s  ",       
		" bbbbbbbbbblsssll ",
		" byyyyyyylsssssss ",
		" byyyyyyyyylsssll ",
		" byyyyyyyyysyslsb ",
		" byyyyyyyyyylslyb ",
		" byyyyyyyyyylslyb ",
		" byyyyyyyyyyylyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" bbbbbbbbbbbbbbbb ",
		"                  ",
	}; 

	public static final String folder[] = {	// 6
		"                  ",
		"                  ", 
		"   bbbbbb         ",
		"   byyyyb         ",       
		" bbbbbbbbbbbbbbbb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" bbbbbbbbbbbbbbbb ",
		"                   ",
	}; 

	public static final String document[] = {	// 7
		"                ",
		"  bbbbbbbb      ",
		"  bwwwwwwbb     ",
		"  bwwwwwwbwb    ",
		"  bwwwwwwbwwb   ",
		"  bwwwwwwbbbbb  ",
		"  bwwwwwwwwwwb  ",
		"  bwwwwwwwwwwb  ",
		"  bwwwwwwwwwwb  ",
		"  bwwwwwwwwwwb  ",
		"  bwwwwwwwwwwb  ",
		"  bwwwwwwwwwwb  ",
		"  bwwwwwwwwwwb  ", 
		"  bwwwwwwwwwwb  ",
		"  bwwwwwwwwwwb  ",
		"  bbbbbbbbbbbb  ",
	};

	public static final String diff[] = {	// 8
		"                  ",
		"                  ",
		"bbbbb     bbbbb   ",
		"bwwwbb    bwwwbb  ",
		"bwwwbwb   bwwwbwb ",
		"bwwwbwwb  bwwwbwwb",
		"bwwwbbbbb bwwwbbbb",
		"bwwwwwwwb bwwwwwwb",
		"bwwwwbwwb bwbwwwwb",
		"bwwwbbwwb bwbbwwwb",
		"bwwbbbbbbbbbbbbwwb",
		"bwwwbbwwb bwbbwwwb",
		"bwwwwbwwb bwbwwwwb",
		"bwwwwwwwb bwwwwwwb",
		"bbbbbbbbb bbbbbbbb",
		"                  ",
	};

	public static final String dirDiff[] = {	// 9
		"                  ",
		"                  ", 
		"  bbbbb    bbbbb  ",
		"  byyyb    byyyb  ",       
		"bbbbbbbbbbbbbbbbbb",
		"byyyyyyybbyyyyyyyy",
		"byyyyyyybbyyyyyyyb",
		"byyyybyybbyybyyyyb",
		"byyybbyybbyybbyyyb",
		"byybbbbbbbbbbbbyyb",
		"byyybbyybbyybbyyyb",
		"byyyybyybbyybyyyyb",
		"byyyyyyybbyyyyyyyb",
		"byyyyyyybbyyyyyyyb",
		"bbbbbbbbbbbbbbbbbb",
		"                  ",
	}; 

	public static final String cut[] = {	// 10
		"   bb       bb    ",
		"   bb       bb    ",
		"   bwb     bwb    ",
		"   bwb     bwb    ",
		"   bwwb   bwwb    ",
		"    bwb   bwb     ",
		"    bwwb bwwb     ",
		"     bwb bwb      ",
		"      bbbbb       ",
		"      bbwbb       ",
		"     bbb bbb      ",
		"   bbbb   bbbb    ",
		"  bb bb   bb bb   ",
		"  b  bb   bb  b   ",
		" bb  bb   bb  bb  ",
		"  bbbb     bbbb   ",
	};

	public static final String copy[] = {	// 11
		"  bbbbbbb         ",
		"  bwwwwwbb        ",
		"  bwwwwwbwb       ",
		"  bwwwbbbbbbb     ",
		"  bwwwbwwwwwbb    ",
		"  bwwwbwwwwwbwb   ",
		"  bwwwbwwwwwbwwb  ",
		"  bwwwbwwwwwbbbbb ",
		"  bwwwbwwwwwwwwwb ",
		"  bwwwbwwwwwwwwwb ",
		"  bwwwbwwwwwwwwwb ",
		"  bwwwbwwwwwwwwwb ", 
		"  bwwwbwwwwwwwwwb ",
		"  bbbbbwwwwwwwwwb ",
		"      bwwwwwwwwwb ",
		"      bbbbbbbbbbb ",
	};

	public static final String paste[] = {	// 12
		"      bbbbbb      ",
		"  bbbbbwbbwbbbbb  ",
		"  bbbbbwbbwbbbbb  ",
		"  blllbbbbbbbllb  ",
		"  blllbwwwwwbblb  ",
		"  blllbwwwwwbwbb  ",
		"  blllbwwwwwbwwb  ",
		"  blllbwwwwwbbbbb ",
		"  blllbwwwwwwwwwb ",
		"  blllbwwwwwwwwwb ",
		"  blllbwwwwwwwwwb ",
		"  blllbwwwwwwwwwb ", 
		"  blllbwwwwwwwwwb ",
		"  bbbbbwwwwwwwwwb ",
		"      bwwwwwwwwwb ",
		"      bbbbbbbbbbb ",
	};

	public static final String delete[] = {	// 13
		"                ",
		"                ",
		"                ",
		" bbb          b ",
		"  bbb        b  ",
		"   bbb     b    ",
		"     bb   b     ",
		"      bb bb     ",
		"       bbb      ",
		"       bbb      ",
		"      bb bb     ",
		"     bb   bb    ",
		"    bbb    bb   ",
		"   bbb     bbb  ",
		"            bb  ",
		"                ",
	};

	public static final String remove[] = {	// 14
		"                ",
		"bb  bbb  bbb  bb",
		"b              b",
		" bbb          b ",
		"  bbb        b  ",
		"b  bbb     b   b",
		"b    bb   b    b",
		"b     bb bb    b",
		"       bbb      ",
		"       bbb      ",
		"b     bb bb    b",
		"b    bb   bb   b",
		"b   bbb    bb  b",
		"   bbb     bbb  ",
		"            bbb ",
		"bb  bbb  bbb  bb",
	};

	public static final String empty[] = {	// 15
		"                ",
		"  bbbbbbbb      ",
		"  bwwwwwwbb   b ",
		" bbbwwwwwbwb b  ",
		"  bbbwwwwbwbbb  ",
		"  bbbbwwwbbbbb  ",
		"  bwwbbwwwbwwb  ",
		"  bwwwbbwbbwwb  ",
		"  bwwwwbbbwwwb  ",
		"  bwwwwbbbwwwb  ",
		"  bwwwbbwbbwwb  ",
		"  bwwbbwwwbbwb  ",
		"  bwbbbwwwbbbb  ", 
		"  bbbbwwwwwbbb  ",
		"  bwwwwwwwwwwb  ",
		"  bbbbbbbbbbbb  ",
	};

	public static final String undo[] = {	// 16
		"                ",
		"                ",
		"     bb         ",
		"    brb         ",
		"   brrb         ",
		"  brrrbbbbbb    ",
		" brrrrrrrrrrb   ",
		" brrrrrrrrrrrb  ",
		"  brrrbbbbrrrb  ",
		"   brrb   brrb  ",
		"    brb   brrb  ",
		"     bb   brrb  ",
		"          brrb  ",
		"          brrb  ",
		"                ",
		"                ",
	};

	public static final String redo[] = {	// 17
		"                ",
		"                ",
		"         bb     ",
		"         bcb    ",
		"         bccb   ",
		"    bbbbbbcccb  ",
		"   bccccccccccb ",
		"  bcccccccccccb ",
		"  bcccbbbbcccb  ",
		"  bccb   bccb   ",
		"  bccb   bcb    ",
		"  bccb   bb     ",
		"  bccb          ",
		"  bccb          ",
		"                ",
		"                ",
	};

	public static final String refresh[] = {	// 18
		"    bbbb        ",
		"   bb  bb       ",
		"   bb   bbbbb   ",
		"   bb   bbb w   ",
		"   bb  bb w w   ",
		"   bb    w w w  ",
		"   bb    w w w  ",
		"   bb    w w w  ",
		"   bb b  w w w  ",
		"   bbbb  w w w  ",
		"   bb b  w w w  ",
		"   bb      w    ",
		"   bb    w   w  ", 
		"   bb      w    ",
		"   bb    w   w  ",
		"   bbbbbbbbbbb  ",
	};

	public static final String refresh2[] = {	// 19
		"bbbbbbbbbb       ",
		"yyyyyyyyyygb     ",
		"yyyyyyyyyyyyb    ",
		"bbbbbyyyyyyyyb   ",
		"   byyyyyyyyyyb  ",
		"  byyyyyyyyyyyyb ",
		"  bbbbbbbbbbbbbb ",
		"   wwwwwwwwwww   ",
		"   bbbbbbbbbbb   ",
		"     w w w w     ",
		"   w w w w w w   ",
		"   w w w w w w   ",
		"   w w w w w w   ",
		"   w w w w w w   ",
		"   w   w   w     ",
		"     w   w   w   ",
	};

	public static final String documentEdit[] = {	// 20
		"                  ",
		"  bbbbbbbb   bbb  ",       
		"  byyyyyybb  bccb ",
		"  byyyyyybybbccb  ",
		"  bylllllbyybccb  ",
		"  byyyyyybbbccb   ",
		"  bylllllllbccb   ",
		"  byyyyyyybccb    ",
		"  byllllllbccb    ",
		"  byyyyyybccbb    ",
		"  bylllllbccbb    ",
		"  byyyyybbbyyb    ",
		"  byyyyybbyyyb    ", 
		"  byyyyybyyyyb    ",
		"  byyyyyyyyyyb    ",
		"  bbbbbbbbbbbb    ",
	};

	public static final String fieldEdit[] = {	// 21
		"                  ",
		"             bbb  ",
		"             byyb ",
		"            byyb  ",
		"            byyb  ",
		"           byyb   ",
		"           byyb   ",
		"          byyb    ",
		" bbbbbbbbbbyybb   ",
		" bwwwwwwwbyybwb   ",
		" bwwwwwwbbyybwb   ",
		" bwlllllbbbwwwb   ",
		" bwwwwwwbbwwwwb   ", 
		" bwwwwwwbwwwwwb   ",
		" bwwwwwwwwwwwwb   ",
		" bbbbbbbbbbbbbb   ",
	};

	public static final String newDocument[] = {	// 22
		"    s           ",
		"   lslbbbbb     ",
		"  slslslwwbb    ",
		"  lssslwwwbwb   ",
		" sssssssgwbwwb  ",
		"  lssslwwwbbbbb ",
		"  slslslwwwwwwb ",
		"   lslwwwwwwwwb ",
		"   lswwwwwwwwwb ",
		"   bwwwwwwwwwwb ",
		"   bwwwwwwwwwwb ",
		"   bwwwwwwwwwwb ",
		"   bwwwwwwwwwwb ", 
		"   bwwwwwwwwwwb ",
		"   bwwwwwwwwwwb ",
		"   bbbbbbbbbbbb ",
	};

	public static final String save[] = {	// 23
		"  bbbbbbbbbbbbbbb  ",
		"  bgglllllllllggb  ",
		"  bbblllllllllbbb  ",
		"  bgglllllllllggb  ",
		"  bgglllllllllggb  ",
		"  bgglllllllllggb  ",
		"  bgglllllllllggb  ",
		"  bgglllllllllggb  ",
		"  bgggggggggggggb  ",
		"  bggbbbbbbbbbggb  ",
		"  bggbwwwwwwwbggb  ",
		"  bggbwbwwwwwbggb  ",
		"  bggbwbwwwwwbggb  ", 
		"  bggbwbwwwwwbggb  ",
		"  bggbwwwwwwwbggb  ",
		"   bbbbbbbbbbbbbb  ",
	};

	public static final String find[] = {	// 24
		"     bbbb   bbbb     ",
		"     bggb   bggb     ",
		"     bggb   bggb     ",
		"     bggb   bggb     ",
		"  bbbbbbbbbbbbbbbbb  ",
		"  bbggbbb b bbggbbb  ",
		"  bbggbbb b bbggbbb  ",
		"  bbggbbb b bbggbbb  ",
		"  bbggbbb b bbggbbb  ",
		" bbbggbbb b bbggbbbb ",
		" bgggbbbb b bbbggbbb ",
		" bgggbbbbbbbbbgggbbb ",
		" bgwgbbb     bgwgbbb ",
		" bgwgbbb     bgwgbbb ",
		" bgwgbbb     bgwgbbb ",
		" bbbbbbb     bbbbbbb ",
	};

	public static final String info[] = {	// 25
		"      bbbbbbb      ",
		"    bbwwwwwwwbb    ",
		"   bwwwwwbwwwwwb   ",
		"  bwwwwwbbbwwwwwb  ",
		"  bwwwwwbbbwwwwwb  ",
		" bwwwwwwwbwwwwwwwb ",
		" bwwwwwwwwwwwwwwwb ",
		" bwwwwwwbbbwwwwwwb ",
		" bwwwwwwbbbwwwwwwb ",
		" bwwwwwwbbbwwwwwwb ",
		" bwwwwwwbbbwwwwwwb ",
		"  bwwwwwbbbwwwwwb  ",
		"  bwwwwwbbbwwwwwb  ",
		"   bwwwwwwwwwwwb   ",
		"    bbwwwwwwwbb    ",
		"      bbbbbbb      ",
	}; 

	public static final String key[] = {	// 26
		"                    ",
		"                    ",
		"   bbbb             ",
		"  bbbbbb            ",
		" bb    bb           ",
		" bb    bb           ",
		" bb    bbbbbbbbbbbbb",
		" bb    bbllllllllllb",
		" bb    bbbbbbbbbbbbb",
		" bb    bb     bbbbb ",
		" bb    bb     bbbbb ",
		"  bbbbbb     bblllb ",
		"   bbbb       bbbbb ",
		"              bbbbb ",
		"                    ",
		"                    ",
	};

	public static final String compress[] = {	// 27
		"  bb   bbbbbbbbbb   ",
		"bbbbbbbbbbbbbbbbbbb ",
		"bbbbbbb     bbbbbb  ",
		"  bb        byyyyb  ",
		" bbbb  bbbbbbbbbbbbb",
		"  bb   byyyyyyyyyyyb",
		" bbbb  byyyyyyyyyyyb",
		"  bb   bylllllllllyb",
		" bbbb  byyyyyyyyyyyb",
		"  bb   byyyyyyyyyyyb",
		" bbbb  bbbbbbbbbbbbb",
		"  bb         bbbb   ",
		"bbbbbbb      bbbb   ",
		"bbbbbbbbbbbbbbbbbbb ",
		"  bb   bbbbbbbbbb   ",
		"bbbbbb              ",
	};

	public static final String photo[] = {	// 28
		"     s   s   s      ",
		"      s  s  s       ",
		"      bsssssb       ",
		"     bbbsssbbb      ",
		"  bb bbsssssbb bb   ",
		" bbbbbsbbsbbsbbbbb  ",
		" blllbbbwsbbbblllb  ",
		" bllbbbwwwbwwbbllb  ",
		" blbbwbbwwbbbwbblb  ",
		" blbbwbbbwbbbwbblb  ",
		" blbbwbbbbbbbwbblb  ",
		" blbbwbbbbbbbwbblb  ",
		" bllbbwwbbbwwbbllb  ",
		" bggggbbwwwbbggggb  ",
		" bbbbbbbbbbbbbbbbb  ",
		"                    ",
	};

	public static final String computer[] = {	// 29
		"  bbbbbbbbbbbbbbbb  ",
		"  bllbbbbbbbbbbllb  ",
		"  blbwwwwwlllllblb  ",
		"  blbwwggggggggblb  ",
		"  blbwygygygygyblb  ",
		"  blbwgggggggggblb  ",
		"  blbwygygygygyblb  ",
		"  blbwgggggggggblb  ",
		"  blbwygygygygyblb  ",
		"  bllbbbgggbbbbllb  ",
		"  bbbbbbbbbbbbbbbb  ",
		"      bbbbbbb       ",
		"bbbbbbbbbbbbbbbbbbbb",
		"blwwwblllllggggggglb",
		"blwwwblllllggggggglb",
		"bbbbbbbbbbbbbbbbbbbb",
	};

	public static final String xmlEdit[] = {	// 30
		"                  ",
		"    bbbbbbbbbbbb  ",       
		"    bwwwwwwwwwwb  ",
		"    bwwwwwwwwwwb  ",
		"    bwwwwwwwwwwb  ", 
		"    wwwwwwwwwwwl  ",
		"  bbwbbwbwwwbwbw  ",
		"   blbwwbbwbbwbw  ",
		"    blwwbwbwbwbw  ",
		"   blbwwbwwwbwbl  ",
		"  bbwbbbbwwwbbbbb ",
		"    wwwwwwwwwwww  ",
		"    bwwwwwwwwwwb  ",
		"    bwwwwwwwwwwb  ",
		"    bwwwwwwwwwwb  ",
		"    bbbbbbbbbbbb  ",
	};

	public static final String dtd[] = {	// 31
		"                  ",
		"    bbbbbbbbbbbb  ",       
		"    bwwwwwwwwwwb  ",
		"    bwwwwwwwwwwb  ",
		"    bwwwwwwwwwwb  ", 
		"    wwwwwwwwwwww  ",
		"   bbbwbbbbbwbbb  ",
		"   bwwbwwbwwwbwwb ",
		"   bwwbwwbwwwbwwb ",
		"   bwwbwwbwwwbwwb ",
		"   bbbwwwbwwwbbb  ",
		"    wwwwwwwwwwww  ",
		"    bwwwwwwwwwwb  ",
		"    bwwwwwwwwwwb  ",
		"    bwwwwwwwwwwb  ",
		"    bbbbbbbbbbbb  ",
	};

	public static final String hexEdit[] = {	// 32
		"                  ",
		"  bbbbbbbb        ",
		"  bwwwwwwbb       ",
		"  bwwwwwwbwb      ",
		"  bwwwwwwbwwb     ",
		"  bwwwwwwbbbbb    ",
		"  wwwwwwwwwwww    ",
		" bbwbbwbbbwbbwbb  ",
		" bbwbbwbbbwbbwbb  ",
		" bbwbbwbbwwwbbb   ",
		" bbbbbwbbbwwbbb   ",
		" bbwbbwbwwwwbbb   ",
		" bbwbbwbbbwbbwbb  ", 
		" bbwbbwbbbwbbwbb  ",
		"  lwwwwwwwwwwl    ",
		"  bbbbbbbbbbbb    ",
	};

	public static final String ftp[] = {	// 33
		"       bbbb        ",
		"   bb bbbbbb bb    ",
		" bbbb b bb b bbbb  ",
		"bbbbb   bb   bbbbb ",
		"bb      bb   bb bbb",
		"bb      bb   bb  bb",
		"bbbbb   bb   bb  bb",
		"bbbbb   bb   bbbbb ",
		"bbbbb   bb   bbbb  ",
		"bb      bb   bb    ",
		"bb      bb   bb    ",
		"bb      bb   bb    ",
		"bb      bb   bb    ",
		" b      bb   b     ",
		"        bb         ",
		"                   ",
	};

	public static final String trash[] = {	// 34
		"       bbbb       ",
		"      b    b      ",
		" bbbbbbbbbbbbbbbb ",
		" bllllllllllllllb ",
		" bbbbbbbbbbbbbbbb ",
		"   bwwwwwwwwwwb   ",
		"   bwwbllbllbgb   ",
		" b bwwbllbllbgg b ",
		" bbbwwbllbllbgbbb ",
		"   bwwbllbllbgb   ",
		"   bwwbllbllbgb   ",
		"   bwwbllbllbgb   ",
		"   bwwbllbllbgb   ",
		"   bwwwlllllllb   ",
		"   bbbbbbbbbbbb   ",
		"                  ",
	};

	public static final String tail[] = {	// 35
		" b                ",
		" b                ",
		"  bbbbbbbbbbbbbb  ",
		"                b ",
		"                b ",
		"  b b b b b b b   ",
		" b                ",
		" b                ",
		"  bbbbbbbb        ",
		"         b        ",
		"        bbb       ",
		"        bbb       ",
		"       bbbbb      ",
		"         b        ",
		"         b        ",
		"         b        ",
	};

	public static final String lineCount[] = {	// 36
		"bbbbbbbbbbbbbbbbbb",
		"b                 ",
		"b                 ",
		"b  b    b    b    ",
		"b bcc  bcc  bcc   ",
		"bbbccbbbccbbbccbbb",
		"b bcc  bcc  bcc   ",
		"b  b    b    b    ",
		"b                 ",
		"b                 ",
		"b  b    b      b  ",
		"b brr  brr    brr ",
		"bbbrrbbbrrbbbbbrrb",
		"b brr  brr    brr ",
		"b  b    b      b  ",
		"b                 ",
	};

	public static final String start[] = {	// 37
		"                ",
		"                ",
		"  b     b       ",
		"  bb    bb      ",
		"  bcb   bcb     ",
		"  bccb  bccb    ",
		"  bcccb bcccb   ",
		"  bccccbbccccb  ",
		"  bccccbbccccb  ",
		"  bcccb bcccb   ",
		"  bccb  bccb    ",
		"  bcb   bcb     ",
		"  bb    bb      ",
		"  b     b       ",
		"                ",
		"                ",
	};

	public static final String stop[] = {	// 38
		"                ",
		"                ",
		"       b     b  ",
		"      bb    bb  ",
		"     brb   brb  ",
		"    brrb  brrb  ",
		"   brrrb brrrb  ",
		"  brrrrbbrrrrb  ",
		"  brrrrbbrrrrb  ",
		"   brrrb brrrb  ",
		"    brrb  brrb  ",
		"     brb   brb  ",
		"      bb    bb  ",
		"       b     b  ",
		"                ",
		"                ",
	};

	public static final String mail[] = {	// 39
		"                   ",
		"                   ",
		" bbbbbbbbbbbbbbbbb ",
		" bbyyyyyyyyyyyyybb ",
		" bybyyyyyyyyyyybyb ",
		" byybyyyyyyyyybyyb ",
		" byyybyyyyyyybyyyb ",
		" byyyybyyyyybyyyyb ",
		" byyyybbyyybbyyyyb ",
		" byyybyybybyybyyyb ",
		" byybyyyybyyyybyyb ",
		" bybyyyyyyyyyyybyb ",
		" bbyyyyyyyyyyyyybb ",
		" bbbbbbbbbbbbbbbbb ",
		"                   ",
		"                   ",
	};

	public static final String sendMail[] = {	// 40
		"                    ",
		"                    ",
		" bbbbbbbbbbbbbbbbb  ",
		" bbyyyyyyyyyyyyy b  ",
		" bybyyyyyyyyyyby    ",
		" byybyyyyyyyyybby   ",
		" byyybyyyyyyyybbby  ",
		" byyyybyyybbbbbbbb  ",
		" byyyybbyybbbbbbbbb ",
		" byyybyybybbbbbbbb  ",
		" byybyyyybyyyybbby  ",
		" bybyyyyyyyyyybby   ",
		" bbyyyyyyyyyyyby b  ",
		" bbbbbbbbbbbbblbbb  ",
		"                    ",
		"                    ",
	};

	public static final String configure[] = {	// 41
		"         bbb    ",
		"    bbb bwwwb   ",
		"   bwwwbbwwrb   ",
		"   bwwcbblrrb   ",
		"   bwccb brb    ",
		"    bcb  brb    ",
		"    bcb  brb    ",
		"    bcb brrrb   ",
		"   bwwcbggrrrb  ",
		"  bwwwccbggrrb  ",
		"  bcwcccbggrrb  ",
		"  bcwcccbggrrb  ",
		"  bcccccbggrb   ",
		"   bcccbbggrb   ",
		"   bcccb bbb    ",
		"   bbbbb        ",
	};

	public static final String doubleClick[] = {	// 42
		"     s            ",
		"     s            ",
		"  s  s  s         ",
		"   s s s          ",
		"    sss           ",
		"ssssssbbbbbbbbb   ",
		"    ssbwwwwwwb    ",
		"   s sbwwwwwb     ",
		"  s  sbwwwwb      ",
		"     sbwwwwwb     ",
		"     sbwwbwwwb    ",
		"      bwb bwwwb   ",
		"      bb   bwwwb  ",
		"      b     bwwwb ",
		"             bwb  ",
		"              b   ",
	};

	public static final String box[] = {	// 43
		"                  ",
		"                  ",
		"       bbbbbbbbbb ",
		"     bbblllllllbb ",
		"   bbbbblllllbblb ",
		" bbbbbbbbbbbblllb ",
		" bwwwwwwwwwbllllb ",
		" bwwwwwwwwwbllllb ",
		" bwwwwwwwwwbllllb ",
		" bwwwwwwwwwbllllb ",
		" bwwwwwwwwwbllllb ",
		" bwwwwwwwwwbllllb ",
		" bwwwwwwwwwbllbb  ",
		" bwwwwwwwwwbbb    ",
		" bbbbbbbbbbb      ",
		"                  ",
	};

	public static final String tree[] = {	// 44
		" b                ",
		" b    bbb         ",
		" b   bbybbb       ",
		" bbbbbyyyyb       ",
		" b   bbbbbb       ",
		" b     b          ",
		" b     b    bbb   ",
		" b     b   bbybbb ",
		" b     bbbbbyyyyb ",
		" b     b   bbbbbb ",
		" b     b          ",
		" b     b    bbb   ",
		" b     b   bbybbb ",
		" b     bbbbbbyyyb ",
		" b     b   bbbbbb ",
		" b     b          ",
	};

	public static final String question[] = {	// 45
		"                ",
		"      bbbbb     ",
		"     bbbbbbb    ",
		"    bbb   bbb   ",
		"    bbb   bbb   ",
		"    bbb   bbb   ",
		"          bbb   ",
		"         bbb    ",
		"        bbb     ",
		"       bbb      ",
		"       bbb      ",
		"       bbb      ",
		"                ",
		"                ",
		"       bbb      ",
		"       bbb      ",
	};

	public static final String calculator[] = {	// 46
		"   bbbbbbbbbb   ",
		"  bbggggggggbb  ",
		"  bbbbbbbbbbbb  ",
		"  bbrrbrrbrrbb  ",
		"  bbrrbrrbrrbb  ",
		"  bbbbbbbbbbbb  ",
		"  bbbbbbbbbbbb  ",
		"  bgyygyyggygb  ",
		"  bggggggggggb  ",
		"  bgyygyyggygb  ",
		"  bggggggggggb  ",
		"  bgyygyyggygb  ",
		"  bggggggggggb  ",
		"  bgyygyyggygb  ",
		"  bbbbbbbbbbbb  ",
		"   bbbbbbbbbb   ",
	};

	public static final String history[] = {	// 47
		"  bbbbbbbbbbbb   ",
		" blllbwwwwwwwwb  ",
		" blblbwwwwwwwwwb ",
		" blblbwwwwwwwwwb ",
		" blllbwgggggggwb ",
		"  bbbbwwwwwwwwwb ",
		"     bwwwwwwwwwb ",
		"     bwgggggggwb ",
		"     bwwwwwwwwwb ",
		"     bwwwwwwwwwb ",
		"  bbbbwgggggggwb ",
		" blllbwwwwwwwwwb ",
		" blblbwwwwwwwwwb ",
		" blblbwwwwwwwwwb ",
		" blllbwwwwwwwwb  ",
		"  bbbbbbbbbbbb   ",
	};

	public static final String chdir[] = {	// 48
		"                    ",
		"                    ", 
		"     bbbbbb         ",
		"     byyyyb         ",       
		"   bbbbbbbbbbbbbbbb ",
		"   byyyyyyyyyyyyyyb ",
		"   byyyybyyyyyyyyyb ",
		"   lyyyybbyyyyyyyyb ",
		" bbbbbbbbbbyyyyyyyb ",
		" bbbbbbbbbbbyyyyyyb ",
		" bbbbbbbbbbyyyyyyyb ",
		"   lyyyybbyyyyyyyyb ",
		"   byyyybyyyyyyyyyb ",
		"   byyyyyyyyyyyyyyb ",
		"   bbbbbbbbbbbbbbbb ",
		"                    ",
	}; 

	public static final String validate[] = {	// 49
		"             b  ",
		"            b   ",
		"           b    ",
		"          bb    ",
		"         bb     ",
		"         bb     ",
		"        bb      ",
		"        bb      ",
		"        bb      ",
		"   b   bb       ",
		"  bbb  bb       ",
		"   bbb bb       ",
		"    bbbbb       ",
		"     bbb        ",
		"      bb        ",
		"                ",
	}; 

	public static final String music[] = {	// 50
		"              bbb ",
		"bbbbbbbbbbbbbbbbbb",
		"wwwwwwwwwwbbbbwwbw",
		"wwwwwwwwbbbbwwwwbw",
		"wwwwwwbbbbwwwwwwbw",
		"bbbbbbbbbbbbbbbbbb",
		"wwwwwwbwwwwwwwwwbw",
		"wwwwwwbwwwwwwbb bw",
		"wwwwwwbwwwwwbbbbbw",
		"bbbbbbbbbbbbbbbbbb",
		"wwwwwwbwwwwwbbbbww",
		"wwwbb bwwwwwwbbwww",
		"wwbbbbbwwwwwwwwwww",
		"bbbbbbbbbbbbbbbbbb",
		"  bbbb            ",
		"   bb             ",
	};

	public static final String picture[] = {	// 51
		"wwwwwwwwwwwwwwwwww",
		"wllllllllllllllllw",
		"wllllllbbblllllllw",
		"wlllllbbbbbblllllw",
		"wllllbrrrbbbbllllw",
		"wgllgrrrrrbbbggllw",
		"wgggbggrggrbbbgggw",
		"wgggbrgrgrrbbbgggw",
		"wgggbrrrrrrbbbgggw",
		"wgggbbrggrrbbbgggw",
		"wgggbbrrrrbbbbbggw",
		"wgggbbbbbbbbbbbggw",
		"wgggbbgggggbbbbbgw",
		"wgggbbrrrrrbbbbbbw",
		"wgbbbrrrrrrbbbbbbw",
		"wwwwwwwwwwwwwwwwww",
	};

	public static final String deleteLine[] = {	// 52
		"                ",
		"  bbbbbbbb      ",       
		"  bwwwwwwbb     ",
		"  bwbbbbwbwb    ",
		"  bwwwwwwbwwb   ",
		"  bwbbbbwbbbbb  ",
		"  bwwwwwwwwwwb  ",
		"  wwbbblbbbbwb  ",
		" bbwwwbbwwwwwb  ",       
		" bbbwbblbbbbwb  ",
		"  lbbbwwwwwwwb  ",
		"  lbbbblbbbbwb  ",
		"  bbwwbbwwwwwb  ",
		" bbwbblbblbbwb  ",
		" bbwwwwwbwwwwb  ",
		"  lbbbbbbbbbbb  ",
	};

	public static final String newLine[] = {	// 53
		"                ",
		"   bbbbbbbb     ",       
		"   bwwwwwwbb    ",
		"   bwbbbbwbwb   ",
		"   bwwwwwwbwwb  ",
		"   bwbbbbwbbbbb ",
		"   bwwwwwwwwwwb ",
		"   bwbbbbbbbbwb ",
		"   lswwwwwwwwwb ",       
		"  slslslbbbbbwb ",
		"  lssslwwwwwwwb ",
		" ssssssslbbbbwb ",
		"  lssslwwwwwwwb ",
		"  slslslbbbbbwb ",
		"   lslwwwwwwwwb ",
		"   bbbbbbbbbbbb ",
	};

	public static final String gotoLine[] = {	// 54
		"                ",
		"   bbbbbbbb     ",
		"   bwwwwwwbb    ",
		"   bwbbbbwbwb   ",
		"   bwwwwwwbwwb  ",
		"   bwblbbwbbbbb ",
		"   bwwbwwwwwwwb ",
		"   bwwbblbbbbwb ",
		" bbbbbbbbwwwwwb ",
		" bbbbbbbbblbbwb ",
		" bbbbbbbbwwwwwb ",
		"   bwlbblbbbbwb ",
		"   bwwbwwwwwwwb ", 
		"   bwblbbbbbbwb ",
		"   bwwwwwwwwwwb ",
		"   bbbbbbbbbbbb ",
	};

	public static final String home[] = {	// 55
		"                ",
		"       b        ",
		"      brb bb    ",
		"     brrrbbb    ",
		"    brrrrrbb    ",
		"   brrrrrrrb    ",
		"  brrrrrrrrrb   ",
		" bbbbbbbbbbbbb  ",
		"  bwwwwwwwwwb   ",
		"  bwwwwwgggwb   ",
		"  bwgggwgcgwb   ",
		"  bwgrgwgcgwb   ",
		"  bwgrgwgggwb   ",
		"  bwgrgwwwwwb   ",
		"  bwgrgwwwwwb   ",
		"  bbbbbbbbbbb   ",
	};

	public static final String palette[] = {	// 56
		"     bbbbbb       ",
		"   bbrb    bb     ",
		"  b rrrb     b    ",
		" bbrrrrrb b   b   ",
		" b brrrb byb  b   ",
		" b  brb byyyb  b  ",
		" b   b  byyyyb b  ",
		" b      byyyb  b  ",
		"  b      byb   b  ",
		"   bbb    b    b  ",
		"      b  bcb   b  ",
		"      b bcccb b   ",
		"       bcccccbb   ",
		"       bbcccbb    ",
		"       b bcbb     ",
		"        bbb       ",
	};

	public static final String newWindow[] = {	// 57
		"   s              ",
		"  lslbbbbbbbbbbbb ",
		" slslsccccccbbcbb ",
		"  ssslccccccbcbcb ",
		"ssssssslccccbbcbb ",
		"  ssslbbbbbbbbbbb ",
		" slslswwwwwwwwwwb ",
		"  lslwwwwwwwwwwwb ",
		"  lswwwwwwwwwwwwb ",
		"  lwwwwwwwwwwwwwb ",
		"  bwwwwwwwwwwwwwb ",
		"  bwwwwwwwwwwwwwb ",
		"  bwwwwwwwwwwwwwb ",
		"  bwwwwwwwwwwwwwb ",
		"  bbbbbbbbbbbbbbb ",
		"                  ",
	};

	public static final String pin[] = {	// 58
		"                    ",
		"                    ",
		"                    ",
		"          bbb       ",
		"  bbb    brrb       ",
		"  brb   brrrb       ",
		"  brbbbbbrrrb       ",
		"  brbrrrbrrrbwwwwb  ",
		"  brbrrrbrrrbwwwwwbb",
		"  brbrrrbrrrbbbbbb  ",
		"  brbbbbbrrrb       ",
		"  brb   brrrb       ",
		"  bbb    brrb       ",
		"          bbb       ",
		"                    ",
		"                    ",
	};

	public static final String signature[] = {	// 59
		"                  ",
		"             bbb  ",       
		"             bccb ",
		"            bccb  ",
		"            bccb  ",
		"           bccb   ",
		"           bccb   ",
		"          bccb    ",
		"  bbbbbb bbccb b  ",
		"  b      bccb  b  ",
		"  b b  bbbccb  b  ",
		"  b  bb bbb    b  ",
		"  b  bb bb     b  ", 
		"  b b  bb      b  ",
		"  b            b  ",
		"  bbbbbbbbbbbbbb  ",
	};

	public static final String export[] = {	// 60
		"                  ",
		"                  ",
		"       bbbb       ",
		"    b b    b b    ",       
		"  bbbbbbbbbbbbbb  ",
		" bwwbwwwwccccbccb ",
		" bwccccccbccccccb ",
		" bwccccccbbcccccb ",
		" bwccbbbbbbbccccb ",
		" bwccbbbbbbbbcccb ",
		" bwccbbbbbbbccccb ",
		" bwccccccbbcccccb ",
		" bwccccccbccccccb ",
		"  bbbbbbbbbbbbbb  ",
		"   bb        bb   ",
		"                  ", 
	}; 

	public static final String mailForward[] = {	// 61
		"            bb     ",
		"           bbbb    ",
		"           bllb    ",
		"           bllb    ",
		"    b       bb     ",
		"    bb   bbbbbbbb  ",
		" bbbbbb bccccccccb ",
		" bbbbbbbbccccccccb ",
		" bbbbbb bcbccccbcb ",
		"    bb  bcbccccbcb ",
		"    b   bcbccccbcb ",
		"         bbcbbcbb  ",
		"          bcbbcb   ",
		"          bcbbcb   ",
		"          bcbbcb   ",
		"         bbb  bbb  ",
	}; 

	public static final String mailReply[] = {	// 62
		"     bb            ",
		"    bbbb           ",
		"    bllb           ",
		"    bllb           ",
		"     bb       b    ",
		"  bbbbbbbb   bb    ",
		" brrrrrrrrb bbbbbb ",
		" brrrrrrrrbbbbbbbb ",
		" brbrrrrbrb bbbbbb ",
		" brbrrrrbrb  bb    ",
		" brbrrrrbrb   b    ",
		"  bbrbbrbb         ",
		"   brbbrb          ",
		"   brbbrb          ",
		"   brbbrb          ",
		"  bbb  bbb         ",
	}; 

	public static final String rules[] = {	// 63
		"        b        ",
		"        b        ",
		"        b        ",
		"        b        ",
		"        b        ",
		"       bwb       ",
		"      bwwwb      ",
		"     bwwwwwb     ",
		"  bbbwwwwwwwbbb  ",
		"  b  bwwwwwb  b  ",
		"  b   bwwwb   b  ",
		"  b    bwb    b  ",
		"  b     b     b  ",
		"  b           b  ",
		"  b           b  ",
		"  b           b  ",
	}; 

	public static final String bulb[] = {	// 64
		"       bbbbb       ",
		" y   blyyyyylb   y ",
		"  y byyyyyyyyyb y  ",
		"    byyyyyyyyyb    ",
		"    byyyyyyyyyb    ",
		" yy byyyyyyyyyb yy ",
		"    byyyyyyyyyb    ",
		"    byyyyyyyyyb    ",
		"   y byyyyyyyb y   ",
		"  y   byyyyyb   y  ",
		"       bbbbb       ",
		"                   ",
		"       bbbbb       ",
		"                   ",
		"       bbbbb       ",
		"        bbb        ",
	}; 

	public static final String forward[] = {	// 65
		"                 ",
		"       bb        ",
		"       bcb       ",
		"       bccb      ",
		"       bcccb     ",
		" bbbbbbbccccb    ",
		" bcccccccccccb   ",
		" bccccccccccccb  ",
		" bcccccccccccccb ",
		" bccccccccccccb  ",
		" bcccccccccccb   ",
		" bbbbbbbccccb    ",
		"       bcccb     ",
		"       bccb      ",
		"       bcb       ",
		"       bb        ",
	}; 

	public static final String back[] = {	// 66
		"                 ",
		"        bb       ",
		"       brb       ",
		"      brrb       ",
		"     brrrb       ",
		"    brrrrbbbbbbb ",
		"   brrrrrrrrrrrb ",
		"  brrrrrrrrrrrrb ",
		" brrrrrrrrrrrrrb ",
		"  brrrrrrrrrrrrb ",
		"   brrrrrrrrrrrb ",
		"    brrrrbbbbbbb ",
		"     brrrb       ",
		"      brrb       ",
		"       brb       ",
		"        bb       ",
	}; 

	public static final String toggleSplit[] = {	// 67
		"                  ",
		"       brrb b     ",
		"   b   brrb   b   ",
		"       brrb    b  ",
		"  b    brrb       ",
		"       brrb       ",
		" bbbbbbbbbbbbbbbb ",
		" ccccccbbbbcccccc ",
		" ccccccbbbbcccccc ",
		" bbbbbbbbbbbbbbbb ",
		"       brrb       ",
		"       brrb    b  ",
		"  b    brrb       ",
		"   b   brrb   b   ",
		"     b brrb       ",
		"                  ",
	}; 

	public static final String toggleSides[] = {	// 68
		"       b b     ",
		"       b bb    ",
		"       b bcb   ",
		"  bbbbbbbbccb  ",
		"  bccccccccccb ",
		"  bbbbbbbbccb  ",
		"       b bcb   ",
		"     b b bb    ",
		"    bb b b     ",
		"   brb b       ",
		"  brrbbbbbbbb  ",
		" brrrrrrrrrrb  ",
		"  brrbbbbbbbb  ",
		"   brb b       ",
		"    bb b       ",
		"     b b       ",
	}; 

	public static final String download[] = {	// 69
		"      bbbbbbb     ",
		"    bblllllllbb   ",
		"   blllllllllllb  ",
		"   bbblllllllbbb  ",
		"   blwbbbbbbbllb  ",
		"   blwbbwwwwlllb  ",
		"   blwbbbwwwlllb  ",
		" bbbbbbbbbwwlllb  ",
		" bbbbbbbbbbwlllb  ",
		" bbbbbbbbbwwlllb  ",
		" bbbbbbbbwwwlllb  ",
		"   blwbbwwwwlllb  ",
		"   blwbwwwwwlllb  ",
		"   blwwwwwwwlllb  ",
		"    bbwwwwwwlbb   ",
		"      bbbbbbb     ",
	}; 

	public static final String clear[] = {	// 70
		"                ",
		"  bbbbbbbb      ",
		"  bwwwwwwbb     ",
		"  bwwwwwwbwb    ",
		"  bwwwwwwbwwb   ",
		"  bwwwwwwbbbbb  ",
		"  bwwwwwwwwwwb  ",
		"  bwwwwwwwwwbb  ",
		"  bwwwwwwwwbbl  ",
		"  bwwwwwwwbbwb  ",
		"  bwwwwwwbbwwb  ",
		"  bwwwwwbblbwb  ", 
		"  bwwwwbbwwwwb  ",
		"  bwwwbblbbbwb  ",
		"  bwwbbwwwwwwb  ",
		"  blbblbbbbbbb  ",
	}; 

	public static final String folderWatch[] = {	// 71
		"                  ",
		"        bbb       ", 
		"   bbblbb bb      ",
		"   byybbl  bb     ",       
		" bbbblbblblbblbbb ",
		" byyyybbyyybbyyyb ",
		" byyyyyyyybbyyyyb ",
		" byyyyyyybbyyyyyb ",
		" byyyyyybbyyyyyyb ",
		" byyyyyybbyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" byyyyyybbyyyyyyb ",
		" byyyyyybbyyyyyyb ",
		" byyyyyyyyyyyyyyb ",
		" bbbbbbbbbbbbbbbb ",
		"                  ",
	}; 

	public static final String concordance[] = {	// 72
		"                 ",
		"    bbb   bbb    ",
		"   brrrb bcccb   ",
		"  brrrrrbcccccb  ",
		"  brrrrrwcccccb  ",
		"  brrrrrwcccccb  ",
		"  brrrbbbbbcccb  ",
		"  brrrrrwcccccb  ",
		"  brrrbbbbbcccb  ",
		"   brrrrwccccb   ",
		"   brrrrwccccb   ",
		"    brrrwcccb    ",
		"     brrwccb     ",
		"      brwcb      ",
		"       bwb       ",
		"        b        ",
	}; 

	public static final String spiral[] = {	// 73
		"     bb         ",
		"    b           ",
		"   b    bbb     ",
		"  b   bb   bb   ",
		"  b  b       b  ",
		" b   b  bbb  b  ",
		" b  b  bb  b  b ",
		" b  b  bbb b  b ",
		" b  b  bbb b  b ",
		" b  b  bbb b  b ",
		" b  b   b  b  b ",
		" b   b    b  b  ",
		"  b   bbbb   b  ",
		"  b         b   ",
		"   bb     bb    ",
		"     bbbbb      ",
	}; 

	public static final String network[] = {	// 74
		"             b     ", 
		"            bb     ",
		"            bb     ",       
		"           byb     ",
		"           byb     ",
		"      bb   byb     ",
		"      byb byyb     ",
		"     byyb byyb     ",
		"     byyybyyyb     ",
		"    byyyyyyyyb     ",
		"    byyybyyyyb     ",
		"   byyyybbyyyb     ",
		"   byyyyb byyb     ",
		"  byyyyb    bb     ",
		"  byyyyb     b     ",
		" byyyyyb           ",
	}; 


	public static void main(String [] args)	{
		String [] [] icons = {
			eye,
			world,
			openFolder,
			newFolder,
			folder,
			document,
			diff,
			dirDiff,
			cut,
			copy,
			paste,
			delete,
			remove,
			empty,
			undo,
			redo,
			refresh,
			refresh2,
			documentEdit,
			fieldEdit,
			newDocument,
			save,
			find,
			info,
			key,
			compress,
			photo,
			computer,
			xmlEdit,
			dtd,
			hexEdit,
			ftp,
			trash,
			tail,
			lineCount,
			start,
			stop,
			mail,
			sendMail,
			configure,
			doubleClick,
			tree,
			box,
			question,
			calculator,
			history,
			chdir,
			validate,
			music,
			picture,
			deleteLine,
			newLine,
			gotoLine,
			home,
			palette,
			newWindow,
			pin,
			signature,
			export,
			mailForward,
			mailReply,
			rules,
			bulb,
			forward,
			back,
			toggleSplit,
			toggleSides,
			download,
			clear,
			folderWatch,
			concordance,
			spiral,
			network,
		};
		javax.swing.JFrame f = new javax.swing.JFrame("IconBuilder");
		f.getContentPane().setLayout(new java.awt.FlowLayout());
		for (int i = 0; i < icons.length; i++)	{
			System.err.println("icon "+i+" ...");
			javax.swing.JLabel l = new javax.swing.JLabel(get(icons[i]));
			f.getContentPane().add(l);
		}
		f.setSize(200, 400);
		f.setLocation(0, 200);
		f.setVisible(true);
	}

}
