package net.b07z.sepia.server.assist.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.tools.StringCompare;

/**
 * This class holds the results of a sentence comparison.
 * 
 * @author Florian Quirin
 *
 */
public class SentenceMatch {
	
	/**
	 * Create matcher with two sentences. Consider normalizing the sentences before.
	 * @param inputSentence - sentence to be checked, e.g. input of the user
	 * @param testSentence - sentence to check against, e.g. tagged database sentence
	 */
	public SentenceMatch(String inputSentence, String testSentence){
		this.inputSentence = inputSentence;
		this.testSentence = testSentence;
	}
	
	//booleans
	public boolean isIdentical = false;			//are both sentences identical?
	
	//statistics
	public int numberOfCharacters = 0;				//how many characters has the input
	public int numberOfWords = 0;					//how many words has the input
	public int matchedWords_N = 0;					//how many words match? (note: how many words of input are in test)
	public int differentWords_N = 0;				//how many words are different? (note: sum of different words)
	public int taggedWords_N = 0;					//how many words are tags?
	public double matchedWords_P = 0.0d;			//how many percent of the words match (note: max number of words of both sentences is used)
	public int editDistance = Integer.MAX_VALUE;	//how many characters need to be changed to match the string
	public int wordDistance = Integer.MAX_VALUE;	//how many words need to be changed to match
	
	//data
	public String inputSentence = "";				//sentence to test and get statistics for
	//public String inputSentenceNorm = "";			//normalized sentence to test and get statistics for
	public String testSentence = "";				//sentence to test against
	public ArrayList<String> inputWords;			//list with words of input sentence
	public ArrayList<String> testWords;				//list with words of test sentence
	public HashMap<String, String> matchedTags;		//if the test sentence has tags they are stored here with the findings
	
	//checks
	private boolean checkedBoW = false;
	private boolean checkedWD = false;

	//------analyzers------
	
	public SentenceMatch getIdentity(){
		isIdentical = inputSentence.equals(testSentence);
		return this;
	}
	
	/**
	 * Count the number of characters in input.
	 * @return this
	 */
	public SentenceMatch getNumChars(){
		numberOfCharacters = inputSentence.length();
		return this;
	}
	
	/**
	 * Count number of words by splitting at whitespace.
	 * @return this
	 */
	public SentenceMatch getNumWords(){
		numberOfWords = inputSentence.split("\\s+").length;
		return this;
	}
	
	/**
	 * Do a bag-of-words analysis. Obtain numberOfWords, matchedWords_N, matchedWords_P, differentWords_N, taggedWords_N.
	 * Tagged words are treated properly by trying to match them.
	 * Order of words is NOT taken into account.
	 * @return this
	 */
	public SentenceMatch getBagOfWordsMatch(){
		
		//TODO: rate words by importance!
		
		//get input words
		String[] inputW = inputSentence.split("\\s+");
		
		//(re)set variables
		matchedWords_N = 0;
		taggedWords_N = 0;
		differentWords_N = 0;
		String test = testSentence;
		inputWords = new ArrayList<>();
		ArrayList<String> leftWords = new ArrayList<>();
		for (String w : inputW) {
			inputWords.add(w);
			leftWords.add(w);			//starts as a copy of inputWords
		}
		numberOfWords = inputW.length;
		
		//go word by word and look if it is included in text sentence. If so remove it from test.
		for (String w : inputWords){
			if (NluTools.stringContains(test, Pattern.quote(w))){
				matchedWords_N++;
				test = NluTools.stringRemoveFirst(test, Pattern.quote(w));	//remove found words from test
				leftWords.remove(w);										//remove word form list
			}
		}
		if (test.isEmpty()){
			//no rest
			matchedWords_P = ((double) matchedWords_N)/((double) numberOfWords);
			differentWords_N = numberOfWords - matchedWords_N;
		}else{
			String[] restW = test.split("\\s+");
			int numberOfRest = 0;
			//iterate the rest
			for (String rw : restW){
				//without tag we cannot matched better
				if (!Word.hasTag(rw)){
					numberOfRest++;
				}
				//with tag we can search if one of the remaining words fits to the tag 
				else if (leftWords.size()>0){
					taggedWords_N++;
					String found = "";
					for (String lw : leftWords){
						//TODO: optimize this to work together with getWordsForTags()
						if (Word.matches(lw, rw)){
							matchedWords_N++;
							found = lw;
							break;
						}
					}
					if (!found.isEmpty()){
						leftWords.remove(found);
					}else{
						numberOfRest++;
					}
				}
				//if there are no remaining word we need to add it to the rest
				else{
					numberOfRest++;
				}
			}
			matchedWords_P = ((double) matchedWords_N)/((double) (numberOfWords + numberOfRest));
			differentWords_N = numberOfWords + numberOfRest - matchedWords_N;
		}
		checkedBoW = true;
		return this;
	}
	
	/**
	 * Calculates how many words must be moved or changed to match the sentence. 
	 * Note that switching the position of 2 words counts as 2 required permutations. 
	 * @return this
	 */
	public SentenceMatch getWordDistance(){
		wordDistance = StringCompare.approxMatch(inputSentence, testSentence);
		checkedWD = true;
		return this;
	}
	
	/**
	 * Calculates how many characters must be changed to match the sentence.
	 * @return this
	 */
	public SentenceMatch getEditDistance(){
		editDistance = StringCompare.editDistance(inputSentence, testSentence);
		return this;
	}
	
	/**
	 * Try to get words from input that fit to the tags. Currently that works only when sentences are identical except the tags
	 * AND it cannot distinguish between position of tags until the Word.match(word,tag) works correctly!
	 * @return this
	 */
	public SentenceMatch getWordsToTags(){
		//TODO: this is a very easy condition and it only works when both sentences have same number of words
		matchedTags = new HashMap<>();
		if (taggedWords_N == wordDistance){
			if (inputWords == null){
				String[] inputW = inputSentence.split("\\s+");
				for (String w : inputW) {
					inputWords.add(w);
				}
			}
			String[] testW = testSentence.split("\\s+");
			testWords = new ArrayList<>();
			for (String w : testW) {
				testWords.add(w);
			}
			//go word by word if words are same
			if (inputWords.size() == testWords.size()){
				int foundTags = 0;
				for (int i=0; i<inputWords.size(); i++){
					String iw = inputWords.get(i);
					String tw = testWords.get(i);
					String tag = Word.getTag(tw);
					if (!tag.isEmpty()){
						if (matchedTags.containsKey(tag)){
							matchedTags.put(tag, matchedTags.get(tag) + " " + iw);
						}else{
							matchedTags.put(tag, iw);
						}
						foundTags++;
						if (foundTags == taggedWords_N){
							break;
						}
					}
				}
			}
		}
		return this;
	}
	
	/**
	 * Calculate certainty by using bag-of-words AND wordDistance. Defaults to 0.0d if none of the test have been made.
	 */
	public double getCertainty(){
		double scoreBoW = 0.0d;
		double scoreWD = 0.0d;
		if (checkedBoW){
			scoreBoW = matchedWords_P;
		}
		if (checkedWD){
			//scoreWD = ((double)(Math.max(numberOfWords, wordDistance) - Math.min(numberOfWords, wordDistance)))
			//  	  / ((double) Math.max(numberOfWords, wordDistance));
			double weightedNumberOfMatchedTaggedWords = (matchedTags == null)? 0 : (matchedTags.size() * 0.90d);
			scoreWD = ((double)(numberOfWords) - ((double)(wordDistance-weightedNumberOfMatchedTaggedWords)/2.0d)) / ((double) numberOfWords);
		}
		//TODO: find a better function?
		return (scoreBoW * scoreWD);
	}
}
