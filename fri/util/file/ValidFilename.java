package fri.util.file;

import java.io.File;
import fri.util.os.OS;

public abstract class ValidFilename
{
	/**
		Return true if the passed name is platform dependent a correct filename
	*/
	public static boolean checkFilename(String name)	{
		char[] chars = name.toCharArray();
		for (int i = 0; i < chars.length; i++)
			if (checkFileCharacter(chars[i]) == false)
				return false;
		return true;
	}

	/**
		Return a platform correct filename made from passed String.
		Does not check if the name is empty.
		@exception NullPointerException when the passed name is null.
	*/
	public static String correctFilename(String name)	{
		char[] chars = name.toCharArray();
		for (int i = 0; i < chars.length; i++)
			if (checkFileCharacter(chars[i]) == false)
				chars[i] = '_';
		return new String(chars);
	}

	/**
		Return a platform correct path made from passed String.
		Does not check if the path is empty.
		@exception NullPointerException when the passed path is null.
	*/
	public static String correctPath(String path)	{
		char[] chars = path.toCharArray();
		for (int i = 0; i < chars.length; i++)
			if (checkPathCharacter(chars[i]) == false)
				chars[i] = '_';
		return new String(chars);
	}

	/**
		Returns true if the passed character is valid for filenames of current platform.
	*/
	public static boolean checkFileCharacter(char chr)	{
		if (chr == File.separatorChar)
			return false;
			
		if (OS.isWindows)	{
			if (chr == '\\' ||
					chr == '/' ||
					chr == ':' ||
					chr == '*' ||
					chr == '?' ||
					chr == '"' ||
					chr == '<' ||
					chr == '>' ||
					chr == '|')
				return false;
		}
		return true;

		/*
		else
		if (OS.isUnix)	{	// UNIX discourages a lot of shell specific characters
			if (chr == '*' ||
					chr == '?' ||
				//	chr == '[' ||
				//	chr == ']' ||
					chr == '"' ||
					chr == '\'' ||
					chr == '`' ||
					chr == '\\' ||
					chr == '/' ||
				//	chr == '(' ||
				//	chr == ')' ||
					chr == '|' ||
				//	chr == '{' ||
				//	chr == '}' ||
				//	chr == '&' ||
				//	chr == ';' ||
					chr == '>' ||
					chr == '<' )
				return false;
		}
		*/
	}
	
	/**
		Returns true if the passed character is valid for a path of current platform.
	*/
	public static boolean checkPathCharacter(char chr)	{
		if (chr == File.separatorChar || chr == '/')
			return true;
		return checkFileCharacter(chr);
	}

	
	private ValidFilename()	{}

}
