package net.b07z.sepia.server.assist.tools;

/**
 * This is a wrapper for fuzzy search implementations.
 */
public class FuzzySearch {

	/**
	 * This is just a wrapper, use {@link StringCompare} instead.
	 */
	public static int ratio(String a, String b){
		return com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch.ratio(a, b);
	}
	/**
	 * This is just a wrapper, use {@link StringCompare} instead.
	 */
	public static int partialRatio(String partial, String full){
		return com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch.partialRatio(partial, full);
	}
}
