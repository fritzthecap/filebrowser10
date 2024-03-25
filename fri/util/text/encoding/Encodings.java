package fri.util.text.encoding;

import java.util.Hashtable;

/**
 * Contains a map with key = Java encoding and value = possible encoding names, in a String array.
 * @author Fritz Ritzberger
 */
public abstract class Encodings
{
	public final static Hashtable map = new Hashtable();
	public final static String defaultEncoding = System.getProperty("file.encoding");
	
	static	{
		String [] ascii = new String []	{
			"us-ascii",
			"ASCII",
			"ascii",
			"646",
			"iso_646.irv:1983",
			"ansi_x3.4-1968",
			"iso646-us",
			"default",
			"ascii7",
		};
		map.put("ASCII", ascii);
		
		String [] iso_8859_1 = new String []	{
			"8859_1",
			"iso_8859-1:1987",
			"iso-ir-100",
			"iso_8859-1",
			"iso-8859-1",
			"iso8859-1",
			"latin1",
			"l1",
			"ibm819",
			"ibm-819",
			"cp819",
			"819",
			"csisolatin1",
		};
		map.put("ISO8859_1", iso_8859_1);
		
		String [] iso_8859_2 = new String []	{
			"8859_2",
			"iso_8859-2:1987",
			"iso-ir-101",
			"iso_8859-2",
			"iso-8859-2",
			"iso8859-2",
			"latin2",
			"l2",
			"ibm912",
			"ibm-912",
			"cp912",
			"912",
			"csisolatin2",
		};
		map.put("ISO8859_2", iso_8859_2);
		
		String [] iso_8859_3 = new String []	{
			"8859_3",
			"iso_8859-3:1988",
			"iso-ir-109",
			"iso_8859-3",
			"iso-8859-3",
			"iso8859-3",
			"latin3",
			"l3",
			"ibm913",
			"ibm-913",
			"cp913",
			"913",
			"csisolatin3",
		};
		map.put("ISO8859_3", iso_8859_3);
		
		String [] iso_8859_4 = new String []	{
			"8859_4",
			"iso_8859-4:1988",
			"iso-ir-110",
			"iso_8859-4",
			"iso-8859-4",
			"iso8859-4",
			"latin4",
			"l4",
			"ibm914",
			"ibm-914",
			"cp914",
			"914",
			"csisolatin4",
		};
		map.put("ISO8859_4", iso_8859_4);
		
		String [] iso_8859_5 = new String []	{
			"8859_5",
			"iso_8859-5:1988",
			"iso-ir-144",
			"iso_8859-5",
			"iso-8859-5",
			"iso8859-5",
			"cyrillic",
			"csisolatincyrillic",
			"ibm915",
			"ibm-915",
			"cp915",
			"915",
		};
		map.put("ISO8859_5", iso_8859_5);
		
		String [] iso_8859_6 = new String []	{
			"8859_6",
			"iso_8859-6:1987",
			"iso-ir-127",
			"iso_8859-6",
			"iso-8859-6",
			"iso8859-6",
			"ecma-114",
			"asmo-708",
			"arabic",
			"csisolatinarabic",
			"ibm1089",
			"ibm-1089",
			"cp1089",
			"1089",
		};
		map.put("ISO8859_6", iso_8859_6);
		
		String [] iso_8859_7 = new String []	{
			"8859_7",
			"iso_8859-7:1987",
			"iso-ir-126",
			"iso_8859-7",
			"iso-8859-7",
			"iso8859-7",
			"elot_928",
			"ecma-118",
			"greek",
			"greek8",
			"csisolatingreek",
			"ibm813",
			"ibm-813",
			"cp813",
			"813",
		};
		map.put("ISO8859_7", iso_8859_7);
		
		String [] iso_8859_8 = new String []	{
			"8859_8",
			"iso_8859-8:1988",
			"iso-ir-138",
			"iso_8859-8",
			"iso-8859-8",
			"iso8859-8",
			"hebrew",
			"csisolatinhebrew",
			"ibm916",
			"ibm-916",
			"cp916",
			"916",
		};
		map.put("ISO8859_8", iso_8859_8);
		
		String [] iso_8859_9 = new String []	{
			"8859_9",
			"iso-ir-148",
			"iso_8859-9",
			"iso-8859-9",
			"iso8859-9",
			"latin5",
			"l5",
			"ibm920",
			"ibm-920",
			"cp920",
			"920",
			"csisolatin5",
		};
		map.put("ISO8859_9", iso_8859_9);
		
		String [] iso_8859_13 = new String []	{
			"8859_13",
			"iso_8859-13",
			"iso-8859-13",
			"iso8859-13",
		};
		map.put("ISO8859_13", iso_8859_13);
		
		String [] iso_8859_15 = new String []	{
			"8859_15",
			"iso-8859-15",
			"iso_8859-15",
			"iso8859-15",
			"ibm923",
			"ibm-923",
			"cp923",
			"923",
			"latin0",
			"latin9",
			"csisolatin0",
			"csisolatin9",
			"iso8859_15_fdis",
		};
		map.put("ISO8859_15", iso_8859_15);
		
		String [] utf8 = new String []	{
			"utf-8",
			"unicode-1-1-utf-8",
		};
		map.put("UTF8", utf8);
		
		String [] unicodeBigUnmarked = new String []	{
			"unicode-1-1",
			"iso-10646-ucs-2",
			"utf-16be",
			"x-utf-16be",
		};
		map.put("UnicodeBigUnmarked", unicodeBigUnmarked);
		
		String [] unicodeLittleUnmarked = new String []	{
			"utf-16le",
			"x-utf-16le",
		};
		map.put("UnicodeLittleUnmarked", unicodeLittleUnmarked);
		
		String [] utf16 = new String []	{
			"utf-16",
		};
		map.put("UTF16", utf16);
		
		String [] unicode = new String []	{
			"unicode",
		};
		map.put("Unicode", unicode);
		
		String [] ibmCp037 = new String []	{
			"ibm037",
			"ibm-037",
			"cp037",
			"037",
		};
		map.put("Cp037", ibmCp037);
		
		String [] ibmCp273 = new String []	{
			"ibm273",
			"ibm-273",
			"cp273",
			"273",
		};
		map.put("Cp273", ibmCp273);
		
		String [] ibmCp277 = new String []	{
			"ibm277",
			"ibm-277",
			"cp277",
			"277",
		};
		map.put("Cp277", ibmCp277);
		
		String [] ibmCp278 = new String []	{
			"ibm278",
			"Cp278",
			"ibm-278",
			"cp278",
			"278",
		};
		map.put("Cp278", ibmCp278);
		
		String [] ibmCp280 = new String []	{
			"ibm280",
			"Cp280",
			"ibm-280",
			"cp280",
			"280",
		};
		map.put("Cp280", ibmCp280);
		
		String [] ibmCp284 = new String []	{
			"ibm284",
			"ibm-284",
			"cp284",
			"284",
		};
		map.put("Cp284", ibmCp284);
		
		String [] ibmCp285 = new String []	{
			"ibm285",
			"ibm-285",
			"cp285",
			"285",
		};
		map.put("Cp285", ibmCp285);
		
		String [] ibmCp297 = new String []	{
			"ibm297",
			"ibm-297",
			"cp297",
			"297",
		};
		map.put("Cp297", ibmCp297);
		
		String [] ibmCp420 = new String []	{
			"ibm420",
			"ibm-420",
			"cp420",
			"420",
		};
		map.put("Cp420", ibmCp420);
		
		String [] ibmCp424 = new String []	{
			"ibm424",
			"ibm-424",
			"cp424",
			"424",
		};
		map.put("Cp424", ibmCp424);
		
		String [] ibmCp437 = new String []	{
			"ibm437",
			"ibm-437",
			"cp437",
			"437",
			"cspc8codepage437",
		};
		map.put("Cp437", ibmCp437);
		
		String [] ibmCp500 = new String []	{
			"ibm500",
			"ibm-500",
			"cp500",
			"500",
		};
		map.put("Cp500", ibmCp500);
		
		String [] ibmCp737 = new String []	{
			"ibm737",
			"ibm-737",
			"cp737",
			"737",
		};
		map.put("Cp737", ibmCp737);
		
		String [] ibmCp775 = new String []	{
			"ibm775",
			"ibm-775",
			"cp775",
			"775",
		};
		map.put("Cp775", ibmCp775);
		
		String [] ibmCp838 = new String []	{
			"ibm838",
			"ibm-838",
			"cp838",
			"838",
		};
		map.put("Cp838", ibmCp838);
		
		String [] ibmCp850 = new String []	{
			"ibm850",
			"ibm-850",
			"cp850",
			"850",
			"cspc850multilingual",
		};
		map.put("Cp850", ibmCp850);
		
		String [] ibmCp852 = new String []	{
			"ibm852",
			"ibm-852",
			"cp852",
			"852",
			"cspcp852",
		};
		map.put("Cp852", ibmCp852);
		
		String [] ibmCp855 = new String []	{
			"ibm855",
			"ibm-855",
			"cp855",
			"855",
			"cspcp855",
		};
		map.put("Cp855", ibmCp855);
		
		String [] ibmCp856 = new String []	{
			"ibm856",
			"ibm-856",
			"cp856",
			"856",
		};
		map.put("Cp856", ibmCp856);
		
		String [] ibmCp857 = new String []	{
			"ibm857",
			"ibm-857",
			"cp857",
			"857",
			"csibm857",
		};
		map.put("Cp857", ibmCp857);
		
		String [] ibmCp860 = new String []	{
			"ibm860",
			"ibm-860",
			"cp860",
			"860",
			"csibm860",
		};
		map.put("Cp860", ibmCp860);
		
		String [] ibmCp861 = new String []	{
			"ibm861",
			"ibm-861",
			"cp861",
			"cp-is",
			"861",
			"csibm861",
		};
		map.put("Cp861", ibmCp861);
		
		String [] ibmCp862 = new String []	{
			"ibm862",
			"ibm-862",
			"cp862",
			"862",
			"cspc862latinhebrew",
		};
		map.put("Cp862", ibmCp862);
		
		String [] ibmCp863 = new String []	{
			"ibm863",
			"ibm-863",
			"cp863",
			"863",
			"csibm863",
		};
		map.put("Cp863", ibmCp863);
		
		String [] ibmCp864 = new String []	{
			"ibm864",
			"ibm-864",
			"cp864",
			"csibm864",
		};
		map.put("Cp864", ibmCp864);
		
		String [] ibmCp865 = new String []	{
			"ibm865",
			"ibm-865",
			"cp865",
			"865",
			"csibm865",
		};
		map.put("Cp865", ibmCp865);
		
		String [] ibmCp866 = new String []	{
			"ibm866",

			"ibm-866",
			"cp866",
			"866",
			"csibm866",
		};
		map.put("Cp866", ibmCp866);
		
		String [] ibmCp868 = new String []	{
			"ibm868",
			"ibm-868",
			"cp868",
			"868",
		};
		map.put("Cp868", ibmCp868);
		
		String [] ibmCp869 = new String []	{
			"ibm869",
			"ibm-869",
			"cp869",
			"869",
			"cp-gr",
			"csibm869",
		};
		map.put("Cp869", ibmCp869);
		
		String [] ibmCp870 = new String []	{
			"ibm870",
			"ibm-870",
			"cp870",
			"870",
		};
		map.put("Cp870", ibmCp870);
		
		String [] ibmCp871 = new String []	{
			"ibm871",
			"ibm-871",
			"cp871",
			"871",
		};
		map.put("Cp871", ibmCp871);
		
		String [] ibmCp874 = new String []	{
			"ibm874",
			"ibm-874",
			"cp874",
			"874",
		};
		map.put("Cp874", ibmCp874);
		
		String [] ibmCp875 = new String []	{
			"ibm875",
			"ibm-875",
			"cp875",
			"875",
		};
		map.put("Cp875", ibmCp875);
		
		String [] ibmCp918 = new String []	{
			"ibm918",
			"ibm-918",
			"cp918",
			"918",
		};
		map.put("Cp918", ibmCp918);
		
		String [] ibmCp921 = new String []	{
			"ibm921",
			"ibm-921",
			"cp921",
			"921",
		};
		map.put("Cp921", ibmCp921);
		
		String [] ibmCp922 = new String []	{
			"ibm922",
			"ibm-922",
			"cp922",
			"922",
		};
		map.put("Cp922", ibmCp922);
		
		String [] ibmCp930 = new String []	{
			"ibm930",
			"ibm-930",
			"cp930",
			"930",
		};
		map.put("Cp930", ibmCp930);
		
		String [] ibmCp933 = new String []	{
			"ibm933",
			"ibm-933",
			"cp933",
			"933",
		};
		map.put("Cp933", ibmCp933);
		
		String [] ibmCp935 = new String []	{
			"ibm935",
			"ibm-935",
			"cp935",
			"935",
		};
		map.put("Cp935", ibmCp935);
		
		String [] ibmCp937 = new String []	{
			"ibm937",
			"ibm-937",
			"cp937",
			"937",
		};
		map.put("Cp937", ibmCp937);
		
		String [] ibmCp939 = new String []	{
			"ibm939",
			"ibm-939",
			"cp939",
			"939",
		};
		map.put("Cp939", ibmCp939);
		
		String [] ibmCp942 = new String []	{
			"ibm942",
			"ibm-942",
			"cp942",
			"942",
		};
		map.put("Cp942", ibmCp942);
		
		String [] ibmCp943 = new String []	{
			"ibm943",
			"ibm-943",
			"cp943",
			"943",
		};
		map.put("Cp943", ibmCp943);
		
		String [] ibmCp948 = new String []	{
			"ibm948",
			"ibm-948",
			"cp948",
			"948",
		};
		map.put("Cp948", ibmCp948);
		
		String [] ibmCp949 = new String []	{
			"ibm949",
			"ibm-949",
			"cp949",
			"949",
		};
		map.put("Cp949", ibmCp949);
		
		String [] ibmCp950 = new String []	{
			"ibm950",
			"ibm-950",
			"cp950",
			"950",
		};
		map.put("Cp950", ibmCp950);
		
		String [] ibmCp964 = new String []	{
			"ibm964",
			"ibm-964",
			"cp964",
			"964",
		};
		map.put("Cp964", ibmCp964);
		
		String [] ibmCp970 = new String []	{
			"ibm970",
			"ibm-970",
			"cp970",
			"970",
		};
		map.put("Cp970", ibmCp970);
		
		String [] ibmCp1006 = new String []	{
			"ibm1006",
			"ibm-1006",
			"cp1006",
			"1006",
		};
		map.put("Cp1006", ibmCp1006);
		
		String [] ibmCp1025 = new String []	{
			"ibm1025",
			"ibm-1025",
			"cp1025",
			"1025",
		};
		map.put("Cp1025", ibmCp1025);
		
		String [] ibmCp1026 = new String []	{
			"ibm1026",
			"ibm-1026",
			"cp1026",
			"1026",
		};
		map.put("Cp1026", ibmCp1026);
		
		String [] ibmCp1097 = new String []	{
			"ibm1097",
			"ibm-1097",
			"cp1097",
			"1097",
		};
		map.put("Cp1097", ibmCp1097);
		
		String [] ibmCp1098 = new String []	{
			"ibm1098",
			"ibm-1098",
			"cp1098",
			"1098",
		};
		map.put("Cp1098", ibmCp1098);
		
		String [] ibmCp1112 = new String []	{
			"ibm1112",
			"ibm-1112",
			"cp1112",
			"1112",
		};
		map.put("Cp1112", ibmCp1112);
		
		String [] ibmCp1122 = new String []	{
			"ibm1122",
			"ibm-1122",
			"cp1122",
			"1122",
		};
		map.put("Cp1122", ibmCp1122);
		
		String [] ibmCp1123 = new String []	{
			"ibm1123",
			"ibm-1123",
			"cp1123",
			"1123",
		};
		map.put("Cp1123", ibmCp1123);
		
		String [] ibmCp1124 = new String []	{
			"ibm1124",
			"ibm-1124",
			"cp1124",
			"1124",
		};
		map.put("Cp1124", ibmCp1124);
		
		String [] ibmCp1381 = new String []	{
			"ibm1381",
			"ibm-1381",
			"cp1381",
			"1381",
		};
		map.put("Cp1381", ibmCp1381);
		
		String [] ibmCp1383 = new String []	{
			"ibm1383",
			"ibm-1383",
			"cp1383",
			"1383",
		};
		map.put("Cp1383", ibmCp1383);
		
		String [] JISAutoDetect = new String []	{
			"jis auto detect",
		};
		map.put("JISAutoDetect", JISAutoDetect);
		
		String [] iso2022jp = new String []	{
			"jis",
			"iso-2022-jp",
			"csiso2022jp",
			"jis_encoding",
			"csjisencoding",
		};
		map.put("ISO2022JP", iso2022jp);
		
		String [] ms932 = new String []	{
			"windows-31j",
			"cswindows31j",
			"shift_jis",
			"csshiftjis",
			"x-sjis",
			"ms_kanji",
		};
		map.put("MS932", ms932);
		
		String [] sjis = new String []	{
			"pck",
		};
		map.put("SJIS", sjis);
		
		String [] euc_jp = new String []	{
			"eucjis",
			"euc-jp",
			"eucjp",
			"extended_unix_code_packed_format_for_japanese",
			"cseucpkdfmtjapanese",
			"x-euc-jp",
			"x-eucjp",
		};
		map.put("EUC_JP", euc_jp);
		
		String [] euc_jp_linux = new String []	{
			"euc-jp-linux",
		};
		map.put("EUC_JP_LINUX", euc_jp_linux);
		
		String [] ms874 = new String []	{
			"windows-874",
		};
		map.put("MS874", ms874);
		
		String [] cp1250 = new String []	{
			"windows-1250",
		};
		map.put("Cp1250", cp1250);
		
		String [] cp1251 = new String []	{
			"windows-1251",
			"ansi-1251",
		};
		map.put("Cp1251", cp1251);
		
		String [] cp1252 = new String []	{
			"windows-1252",
		};
		map.put("Cp1252", cp1252);
		
		String [] cp1253 = new String []	{
			"windows-1253",
		};
		map.put("Cp1253", cp1253);
		
		String [] cp1254 = new String []	{
			"windows-1254",
		};
		map.put("Cp1254", cp1254);
		
		String [] cp1255 = new String []	{
			"windows-1255",
		};
		map.put("Cp1255", cp1255);
		
		String [] cp1256 = new String []	{
			"windows-1256",
		};
		map.put("Cp1256", cp1256);
		
		String [] cp1257 = new String []	{
			"windows-1257",
		};
		map.put("Cp1257", cp1257);
		
		String [] cp1258 = new String []	{
			"windows-1258",
		};
		map.put("Cp1258", cp1258);
		
		String [] ibmCp33722 = new String []	{
			"ibm33722",
			"ibm-33722",
			"cp33722",
			"33722",
		};
		map.put("Cp33722", ibmCp33722);
		
		String [] koi8_r = new String []	{
			"koi8-r",
			"KOI8_R",
			"koi8",
			"cskoi8r",
		};
		map.put("KOI8_R", koi8_r);
		
		String [] euc_cn = new String []	{
			"gb2312",
			"gb2312-80",
			"gb2312-1980",
			"euc-cn",
			"euccn",
		};
		map.put("EUC_CN", euc_cn);
		
		String [] big5 = new String []	{
			"big5",
		};
		map.put("Big5", big5);
		
		String [] big5_hkscs = new String []	{
			"big5hk",
			"big5-hkscs",
			"big5-hkscs:unicode3.0",
		};
		map.put("Big5_HKSCS", big5_hkscs);
		
		String [] big5_solaris = new String []	{
			"big5_solaris",
		};
		map.put("Big5_Solaris", big5_solaris);
		
		String [] euc_tw = new String []	{
			"cns11643",
			"euc-tw",
			"euctw",
		};
		map.put("EUC_TW", euc_tw);
		
		String [] euc_kr = new String []	{
			"ksc5601",
			"euc-kr",
			"euckr",
			"ks_c_5601-1987",
			"ksc5601-1987",
			"ksc5601_1987",
			"ksc_5601",
			"5601",
		};
		map.put("EUC_KR", euc_kr);
		
		String [] johab = new String []	{
			"ksc5601-1992",
			"ksc5601_1992",
			"ms1361",
		};
		map.put("Johab", johab);
		
		String [] ms949 = new String []	{
			"windows-949",
		};
		map.put("MS949", ms949);
		
		String [] iso2022kr = new String []	{
			"iso-2022-kr",
			"csiso2022kr",
		};
		map.put("ISO2022KR", iso2022kr);
		
		String [] tis620 = new String []	{
			"tis620.2533",
			"tis-620",
		};
		map.put("TIS620", tis620);
		
		String [] compound_text = new String []	{
			"x-compound-text",
			"x11-compound_text",
		};
		map.put("COMPOUND_TEXT", compound_text);
		
		String [] cp942c = new String []	{
			"cp942c",
		};
		map.put("Cp942C", cp942c);
		
		String [] cp943c = new String []	{
			"cp943c",
		};
		map.put("Cp943C", cp943c);
		
		String [] cp949c = new String []	{
			"cp949c",
		};
		map.put("Cp949C", cp949c);
		
		String [] iscii = new String []	{	//ISCII91
			"iscii",
		};
		map.put("ISCII91", iscii);
	}

}
