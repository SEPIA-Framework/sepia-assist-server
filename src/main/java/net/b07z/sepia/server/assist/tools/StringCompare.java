package net.b07z.sepia.server.assist.tools;

import java.util.Collection;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.server.Config;

//Edit Distance implementation as seen in Jaivox library http://www.jaivox.com/ and Wikipedia

/**
 * Compare strings with different methods like "Levenshtein Distance" etc..
 */
public class StringCompare {
	
	/**
	 * Multi-purpose compare result object. 
	 */
	public static class StringCompareResult {
		private String inputStringNorm;
		private String resultString;
		private String resultStringNorm;
		private int resultPercent;
		private double resultDecimal;
		
		/**
		 * Create multi-purpose compare result object. 
		 */
		public StringCompareResult(){};
		
		public String getInputStringNormalized(){
			return inputStringNorm;
		}
		public StringCompareResult setInputStringNormalized(String inputString){
			this.inputStringNorm = inputString;
			return this;
		}
		
		public String getResultString(){
			return resultString;
		}
		public String getResultStringNormalized(){
			return resultStringNorm;
		}
		public int getResultPercent(){
			return resultPercent;
		}
		public double getResultDecimal(){
			return resultDecimal;
		}

		public StringCompareResult setResultString(String resultString){
			this.resultString = resultString;
			return this;
		}
		public StringCompareResult setResultStringNormalized(String resultStringNorm){
			this.resultStringNorm = resultStringNorm;
			return this;
		}
		public StringCompareResult setResultPercent(int resultPercent){
			this.resultPercent = resultPercent;
			return this;
		}
		public StringCompareResult setResultDecimal(double resultDecimal){
			this.resultDecimal = resultDecimal;
			return this;
		}
	}
	
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
	public static int editDistance(String one, String two){
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
	public static int approxMatch(String a, String b){
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
	 * @param words - input words as one string
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
	
	/**
	 * Scan a sentence for a phrase of words.<br>
	 * Score examples:<br>
	 * phrase="with", sentence="with it" -> score=100 (contains full word)<br>
	 * phrase="wit", sentence="with it" -> score=99 (contains full string, but not as word)<br>
	 * phrase="with", sentence="wit" -> score=86
	 * @param phrase - input words in given order as one string
	 * @param sentence - sentence to scan
	 * @return percent match as integer (full phrase in sentence -> 100)
	 */
	public static int scanSentenceForPhrase(String phrase, String sentence){
		int score;
		//if (sentence.contains(phrase)){
		if (NluTools.stringContains(sentence, Pattern.quote(phrase))){
			score = 100;
		}else if (sentence.length() < phrase.length()){
			score = FuzzySearch.ratio(phrase, sentence);
			if (score == 100) score = 99;
		}else{
			score = FuzzySearch.partialRatio(phrase, sentence);
			if (score == 100) score = 99;
		}
		return score;
	}
	/**
	 * Scan a sentence for a list of phrases and return the best match. If the sentence contains the whole phrase the result is 100.<br>
	 * Sentence and phrases will be normalized. Longer phrases have higher priority.<br>
	 * NOTE: If no normalizer is available result will be empty. 
	 * @param sentence - raw sentence to scan
	 * @param phrases - collection of raw phrases to scan for
	 * @param language - ISO language code used for norm. {@link LANGUAGES}
	 * @return best phrase or empty result (string = empty, score = 0)
	 */
	public static StringCompareResult scanSentenceForBestPhraseMatch(String sentence, Collection<String> phrases, String language){
		Normalizer normalizer = Config.inputNormalizersLight.get(language);
		String bestPhrase = "";
		String bestPhraseNorm = "";
		int bestScore = 0;
		String sentenceNorm = normalizer.normalizeText(sentence);
		if (normalizer != null){
			for (String phrase : phrases){
				String phraseNorm = normalizer.normalizeText(phrase);
				int score = scanSentenceForPhrase(phraseNorm, sentenceNorm);
				//System.out.println("phraseNorm: " + phraseNorm + " - score: " + score); 		//DEBUG
				if (score > bestScore || (score == bestScore && phraseNorm.length() > bestPhraseNorm.length())){
					bestScore = score;
					bestPhrase = phrase;
					bestPhraseNorm = phraseNorm;
				}
			}
		}
		return new StringCompareResult()
			.setInputStringNormalized(sentenceNorm)
			.setResultString(bestPhrase)
			.setResultStringNormalized(bestPhraseNorm)
			.setResultPercent(bestScore);
	}
	
	/**
	 * Compare a base-string (sentence, words, single token) to a collection of other strings and find best match.
	 * Base-string and collection strings will be normalized. Longer strings have higher priority when matching score is same.<br>
	 * NOTE: If no normalizer is available result will be empty. 
	 * @param baseString - raw string reference (can be a sentence, a few words or a single token)
	 * @param stringsToCompare - collection of raw strings to compare to base string
	 * @param language - ISO language code used for norm. {@link LANGUAGES}
	 * @return best match or empty result (string = empty, score = 0)
	 */
	public static StringCompareResult findMostSimilarMatch(String baseString, Collection<String> stringsToCompare, String language){
		Normalizer normalizer = Config.inputNormalizersLight.get(language);
		String bestMatch = "";
		String bestMatchNorm = "";
		int bestScore = 0;
		String baseStringNorm = normalizer.normalizeText(baseString);
		if (normalizer != null){
			for (String stringToCompare : stringsToCompare){
				String stringToCompareNorm = normalizer.normalizeText(stringToCompare);
				//System.out.println("stringToCompareNorm: " + stringToCompareNorm + " - baseStringNorm: " + baseStringNorm); 		//DEBUG
				int score = FuzzySearch.ratio(stringToCompareNorm, baseStringNorm);
				//System.out.println("bestMatchNorm: " + bestMatchNorm + " - score: " + score); 		//DEBUG
				if (score > bestScore || (score == bestScore && stringToCompareNorm.length() > bestMatchNorm.length())){
					bestScore = score;
					bestMatch = stringToCompare;
					bestMatchNorm = stringToCompareNorm;
				}
			}
		}
		return new StringCompareResult()
			.setInputStringNormalized(baseStringNorm)
			.setResultString(bestMatch)
			.setResultStringNormalized(bestMatchNorm)
			.setResultPercent(bestScore);
	}
}
