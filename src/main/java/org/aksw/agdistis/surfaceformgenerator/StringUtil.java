package org.aksw.agdistis.surfaceformgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.ext.com.google.common.collect.Lists;

//FIXME remove this class

public class StringUtil {

	/**
	 * Returns the longest string contained in the list. The length is
	 * determined by String.length()
	 * 
	 * @param stringCollection
	 *            a collection to search in
	 * @return the longest string contained in the list, null if the list is
	 *         empty or null
	 */
	public static String getLongestSubstring(Collection<String> stringCollection) {

		// return null because we can't calculate longest match
		if (stringCollection.isEmpty() || stringCollection == null)
			return null;

		// go through this list and remember the longest match
		String longestMatch = "";
		Iterator<String> iter = stringCollection.iterator();
		while (iter.hasNext()) {

			String currentString = iter.next();
			longestMatch = currentString.length() >= longestMatch.length() ? currentString : longestMatch;
		}
		return longestMatch;
	}

	/**
	 * Non overlapping regex matches. This method returns all matches of
	 * regexNeedle in the heystack. There are no overlapping matches. Which
	 * means that regex: aba heystack: ababa would only return one match.
	 * 
	 * @param heystack
	 *            - a string to search in
	 * @param regexNeedle
	 *            - the regex which gets search
	 * @return all matches in a string list
	 */
	public static Collection<String> getRegexMatches(String heystack, String regexNeedle) {

		Collection<String> matches = new ArrayList<String>();
		Matcher m = Pattern.compile(regexNeedle)
		                   .matcher(heystack);
		while (m.find()) {
			matches.add(m.group());
		}
		return matches;
	}

	/**
	 * Returns true if s1 is a substring of s2
	 * 
	 * @param s1
	 *            - needle
	 * @param s2
	 *            - heystack
	 * @return true, if s1 is substring of s2
	 */
	public static boolean isSubstringOf(String s1, String s2) {

		return s2.indexOf(s1) != -1;
	}

	/**
	 * 
	 * @param string
	 * @param character
	 * @return
	 */
	public static String removeCharacterFromString(String string, Character character) {

		return string.replace(character.toString(), "");
	}

	/**
	 * 
	 * @param string
	 * @param charactersToRemove
	 * @return
	 */
	public static String removeCharactersFromString(String string, String charactersToRemove) {

		for (Character character : Lists.charactersOf(charactersToRemove)) {

			string = StringUtil.removeCharacterFromString(string, character);
		}
		return string;
	}
}