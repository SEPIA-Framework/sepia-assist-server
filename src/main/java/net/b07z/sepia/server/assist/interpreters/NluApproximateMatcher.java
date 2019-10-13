package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tools.StringCompare;

import java.util.TreeMap;

/**
 * This NL-processor simply tries to match the input to a known sentence from the database using text normalization, 
 * bag-of-words approach and eventually edit distance threshold matching (like in ILA).
 * 
 * @author Florian Quirin
 *
 */
public class NluApproximateMatcher implements NluInterface{
	
	//stores the references to all pre-defined commands in different languages
	Map<String, TreeMap<String, String>> commands = new HashMap<>();
	
	/**
	 * Create NLP with empty data. Use setCommandPool() to add data!
	 */
	public NluApproximateMatcher(){};
	
	/**
	 * Initialize the approximation matcher with a certain commands map.
	 * @param cmd_pool - map with commands in the given languages
	 */
	public NluApproximateMatcher(Map<String, TreeMap<String, String>> cmd_pool){
		this.commands = cmd_pool;
		//DEBUG:
		//DataLoader.list_data(cmd_pool);
	}
	
	/**
	 * Set the pool of possible commands.
	 * @param cmd_pool - pool of commands loaded from txt-files or database.
	 */
	public void setCommandPool(Map<String, TreeMap<String, String>> cmd_pool){
		this.commands = cmd_pool;
	}
	
	/**
	 * Try to find an approximate match for the input text (after text normalization).
	 * Returns null if no match is found.
	 * @param input - NluInput
	 * @return NluResult
	 */
	public NluResult interpret(NluInput input) {
		//supports different languages
		String language = input.language;
		String search = input.text;
		String cmd;
		String bestKey;
		double certainty_lvl;
		
		//normalize search
		//TODO: I'm a bit concerned that this is too strong here ... better use 2 rounds of searches with light norm. first?
		Normalizer normalizer = Config.inputNormalizers.get(language);
		if (normalizer != null){
			search = normalizer.normalizeText(search);
		}
		//get command and certainty
		TreeMap<String, String> chats_this = commands.get(language);
		if (chats_this != null){
			ArrayList<Object> res = find_best_match_bw(search, chats_this); 
			cmd = (String) res.get(0);
			certainty_lvl = (double) res.get(1);
			bestKey = (String) res.get(2);
		
		}else{
			return null;
		}
		
		//handle as if it was a direct command
		if (cmd!=null){
			String org_text = input.text;		//we need to save this to restore after reconstruction
			String org_input_type = input.inputType;		//same here
			input.text = cmd;
			input.inputType = "direct_cmd";
			NluInterface NLP;
			NLP = new NluCmdReconstructor();
			NluResult result = NLP.interpret(input);
			//overwrite certainty
			result.certaintyLvl = certainty_lvl;
			result.bestDirectMatch = bestKey;
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
	
	//----BEST MATCH FINDERS----
	
	/**
	 * Find best match in a TreeMap with bag-of-words approach.
	 * @param search - string to search
	 * @param map - TreeMap containing all sentences to match
	 * @return - value(index 0), certainty(index 1) and key (index 2) of best match key
	 */
	public static ArrayList<Object> find_best_match_bw(String search, TreeMap<String, String> map){
		ArrayList<Object> result = new ArrayList<Object>();
		String cmd = null;
		double first_match_score = 0.0;
		double best_match_score = 0.0;
		String best_match_key = "";
		String best_match_value = "";
		//run through all sentences
		for(Entry<String, String> entry : map.entrySet()) {
			String key = entry.getKey();
			double first_score = StringCompare.wordInclusion(search, key);
			if (first_score >= first_match_score){
				double this_score = first_score * StringCompare.wordInclusion(key, search);
				if (this_score > best_match_score){
					first_match_score = first_score;
					best_match_score = this_score;
					best_match_key = key;
					best_match_value = entry.getValue();
				}
				//same words but same sentence?
				else if (this_score == 1.0){
					if (key.matches(Pattern.quote(search))){
						best_match_score = 1.0;
						best_match_key = key;
						best_match_value = entry.getValue();
						break;
					}
				}
			}
		}
		//System.out.println(best_match_key + " => " + best_match_value + "; score: " + best_match_score);
		//decide later if you want to keep this result by checking the certainty
		cmd = best_match_value;
		result.add(cmd);
		result.add(best_match_score);
		result.add(best_match_key);

		return result;
	}

}
