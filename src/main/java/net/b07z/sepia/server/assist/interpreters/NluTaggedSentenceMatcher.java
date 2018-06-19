package net.b07z.sepia.server.assist.interpreters;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.b07z.sepia.server.assist.data.SentenceMatch;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.CMD;

import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * This NL-processor tries to match the input to a known sentence from the database that (optionally) has tagged parameters.
 * Possibilities are endless here, e.g. we could combine it with normalization, b-o-w, editDistance, nGram, etc. ... 
 * 
 * @author Florian Quirin
 *
 */
public class NluTaggedSentenceMatcher implements NluInterface{

	//stores the references to all pre-defined commands in different languages
	Map<String, TreeMap<String, String>> commands = new HashMap<>();
	
	/**
	 * Create NLP with empty data. Use setCommandPool() to add data!
	 */
	public NluTaggedSentenceMatcher(){};
	
	/**
	 * Initialize the approximation matcher with a certain commands map.
	 * @param cmd_pool - map with commands in the given languages
	 */
	public NluTaggedSentenceMatcher(Map<String, TreeMap<String, String>> cmd_pool){
		this.commands = cmd_pool;
	}
	
	/**
	 * Set the pool of possible commands.
	 * @param cmd_pool - pool of commands loaded from txt-files or database.
	 */
	public void setCommandPool(Map<String, TreeMap<String, String>> cmd_pool){
		this.commands = cmd_pool;
	}
	
	/**
	 * Try to find a match for the input text (after text normalization).
	 * Returns null if no match is found.
	 * @param input - NLU_input
	 * @return NLU_Result
	 */
	public NluResult interpret(NluInput input) {
		//supports different languages
		String language = input.language;
		String search = input.text;
		String cmd = "";
		
		SentenceMatch best_sm = null;
		double best_score = 0.0d;				//matched words
		int best_score2 = Integer.MAX_VALUE;	//word distance
		String best_cmd = "";
		
		//normalize search
		Normalizer normalizer = Config.inputNormalizersLight.get(language);
		search = normalizer.normalize_text(search);

		//get command
		TreeMap<String, String> commands_this = commands.get(language);
		if (commands_this != null){
			
			//run through all sentences
			for(Entry<String, String> entry : commands_this.entrySet()) {
				String sentence = entry.getKey();
				SentenceMatch sm = new SentenceMatch(search, sentence);
				//System.out.println("COMPARE: '" + search + "' and '" + sentence + "'"); 		//debug
				sm.getIdentity()
					.getBagOfWordsMatch()
					.getWordDistance();
				if (sm.isIdentical){
					best_score = 1.0d;
					best_score2 = 0;
					best_sm = sm;
					best_cmd = entry.getValue();
					break;
				}else{
					double this_score = (sm.matchedWords_P);
					int this_score2 = (sm.wordDistance);
					//best score?
					if (this_score == 1.0d && this_score2 == 0){
						best_score = 1.0d;
						best_score2 = 0;
						best_sm = sm;
						best_cmd = entry.getValue();
						break;
					}
					//better score?
					if (this_score > best_score){
						best_score = this_score;
						best_score2 = this_score2;
						best_sm = sm;
						best_cmd = entry.getValue();
					//same score?
					}else if (this_score == best_score){
						//better 2nd score?
						if (this_score2 < best_score2){
							best_score = this_score;
							best_score2 = this_score2;
							best_sm = sm;
							best_cmd = entry.getValue();
						}
					}
				}
			}
			
			
		//no commands list available
		}else{
			return null;
		}
		
		//make new result (use direct command reconstruction as helper)
		if (best_sm != null && !best_cmd.isEmpty()){
			/* DEBUG
			System.out.println("SENTENCE MATCH: " + best_score);				//debug
			System.out.println("SENTENCE MATCH: " + best_sm.inputSentence);		//debug
			System.out.println("SENTENCE MATCH: " + best_sm.testSentence);		//debug
			System.out.println("SENTENCE MATCH: " + best_sm.matchedWords_N);		//debug
			System.out.println("SENTENCE MATCH: " + best_sm.taggedWords_N);		//debug
			System.out.println("SENTENCE MATCH: " + best_sm.differentWords_N);		//debug */
			
			//replace command summary with new parameters
			if (best_sm.isIdentical){
				//identity
				cmd = best_cmd;
			}else if (best_score == 1.0d && best_score2 == best_sm.wordDistance){
				//same number of different words and tags
				cmd = best_cmd;
				best_sm.getWordsToTags();
				//Debugger.printMap_SS(best_sm.matchedTags);							//debug
				for (Map.Entry<String, String> entry : best_sm.matchedTags.entrySet()) {
					String p = entry.getKey();
					String v = entry.getValue();
					if (cmd.contains(p+"=")){
						cmd = cmd.replaceFirst(Pattern.quote(p) + "=.*?(;;|$)", p + "=" + v + ";;");
					}else{
						cmd = cmd.replaceFirst(";;$", "") + ";;" + p + "=" + v + ";;";
					}
				}
			}else{
				//similar command with uncertainty in tag to word association - remove parameters
				//TODO: here is a lot of potential for improvements!
				//--make an exception for chats and open_link
				if (best_cmd.startsWith(CMD.CHAT) || best_cmd.startsWith(CMD.OPEN_LINK)){
					cmd = best_cmd;
				}else{
					cmd = best_cmd.replaceFirst(";;.*", ";;").trim();
				}
			}
			//System.out.println("scores: " + best_score + " - " + best_score2); 		//debug
			//System.out.println("old cmd: " + best_cmd);								//debug
			//System.out.println("new cmd: " + cmd); 									//debug
			
			//construct
			String org_text = input.text;				//we need to save this to restore after reconstruction
			String org_input_type = input.inputType;	//same here
			input.text = cmd;
			input.inputType = "direct_cmd";
			NluInterface NLP;
			NLP = new NluCmdReconstructor();			//reconstruct to make a proper result from best_cmd
			NluResult result = NLP.interpret(input);
			
			//overwrite certainty
			result.certaintyLvl = best_sm.getCertainty();
			
			result.bestDirectMatch = (best_sm == null)? "---" : best_sm.testSentence;
			//restore original input
			input.text = org_text;
			input.inputType = org_input_type;
			
			return result;
			
		}else{
			return null;
		}
	}

	//certainty might result from edit distance ...
	public double getCertaintyLevel(NluResult result) {
		return result.certaintyLvl;
	}
	
	//--------------------------------------
	
	/* 			WHAT WAS THIS FOR???
	public static HashMap<String, String> matchTaggedSentence_free(String input, String sentence){
		HashMap<String, String> result = new HashMap<>();
		if (NLU_Tools.stringContains(input, "<\\w+>")){
			return null;
		}else{
			return null;
		}
	}
	*/

}
