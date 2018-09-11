package net.b07z.sepia.server.assist.tools;

import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.server.Config;

//Edit Distance implementation as seen in Jaivox library http://www.jaivox.com/ and Wikipedia

public class StringCompare {
	
	/**
	 * Used for edit distance
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	// http://en.wikibooks.org/wiki/Algorithm_implementation/Strings/Levenshtein_distance#Java
	static int minimum(int a, int b, int c) {
		return Math.min (Math.min(a, b), c);
	}
	
	/**
	 * Determine the Levenshtein edit distance between two strings of characters
	 * @param one
	 * @param two
	 * @return
	 */	
	public static int editDistance (String one, String two) {
		int n = one.length ();
		int m = two.length ();
		int [][] distance = new int [n + 1][m + 1];

		for (int i = 0; i <= n; i++)
			distance [i][0] = i;
		for (int j = 0; j <= m; j++)
			distance [0][j] = j;

		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= m; j++) {
				distance [i][j] = minimum (
				distance [i - 1][j] + 1,
				distance [i][j - 1] + 1,
				distance [i - 1][j - 1]
					+ ((one.charAt (i - 1) == two.charAt (j - 1)) ? 0 : 1));
			}
		}

		return distance [n][m];
	}

	/**
	 * Get the edit distance between two strings of words
	 * @param a
	 * @param b
	 * @return
	 */	
	public static int approxMatch (String a, String b) {
		String one [] = a.split (" ");
		String two [] = b.split (" ");
		int n = one.length;
		int m = two.length;
        int [][] distance = new int [n + 1][m + 1];

        for (int i = 0; i <= n; i++) {
            distance [i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            distance [0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                distance[i][j] = minimum (
                    distance[i-1][j] + 1,
                    distance[i][j-1] + 1,
                    distance[i-1][j-1] + (one[i-1].equals (two[j-1]) ? 0 : 1));
            }
        }
        return distance [n][m];
    }
	
	/**
	 * Calculates a similarity by comparing how many words of "words" are included in "text".
	 * The result is (number_of_same_words/total_number_of_words).
	 * @param words - input words in one string
	 * @param text - text to search for words
	 * @return (number_of_same_words/total_number_of_words)
	 */
	public static double wordInclusion(String words, String text){
		double result = 0;
		words = words.replaceAll("\\.|,|;|:|!|\\?|'|\\(|\\)|\\[|\\]", "").toLowerCase().trim();
		words = words.replaceAll("-|_", " ").toLowerCase().trim();
		text = text.replaceAll("\\.|,|;|:|!|\\?|'|\\(|\\)|\\[|\\]", "").toLowerCase().trim();
		text = text.replaceAll("-|_", " ").toLowerCase().trim();
		String words_array[] = words.split("\\s+");
		if (words_array.length>0 && !text.isEmpty()){
			int count = 0;
			for (String w : words_array){
				if (text.matches(".*\\b" + Pattern.quote(w) + "\\b.*")){
					count++;
				}
			}
			result = ((double)count)/((double) words_array.length);
		}
		return result;
	}
	/**
	 * Calculates a similarity by comparing how many words of "words" are included in "text".
	 * Both texts are normalized before evaluation.
	 * The result is (number_of_same_words/total_number_of_words).
	 * @param words - input words in one string
	 * @param text - text to search for words
	 * @return (number_of_same_words/total_number_of_words)
	 */
	public static double wordInclusionWithNorm(String words, String text, String lang){
		Normalizer normalizer = Config.inputNormalizersLight.get(lang);
		if (normalizer != null){
			words = normalizer.normalizeText(words);
			text = normalizer.normalizeText(text);
		}
		return wordInclusion(words, text);
	}
}
