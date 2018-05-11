package net.b07z.sepia.server.assist.interpreters;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.b07z.sepia.server.assist.server.Config;

/**
 * This NL-processor simply tries to match the input to a known sentence from the database using text normalization
 * and eventually edit distance threshold matching (like in ILA).
 * 
 * @author Florian Quirin
 *
 */
public class NluSentenceMatcher implements NluInterface{

	//helper
	String last_line = "";
		
	//stores the references to all pre-defined commands in different languages
	Map<String, TreeMap<String, String>> commands = new HashMap<>();
	
	/**
	 * Create NLP with empty data. Use setCommandPool() to add data!
	 */
	public NluSentenceMatcher(){};
	
	/**
	 * Initialize the approximation matcher with a certain commands map.
	 * @param cmd_pool - map with commands in the given languages
	 */
	public NluSentenceMatcher(Map<String, TreeMap<String, String>> cmd_pool){
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
	 * Try to find an exact match for the input text (after text normalization).
	 * Returns null if no match is found.
	 * @param input - NLU_input
	 * @return NLU_Result
	 */
	public NluResult interpret(NluInput input) {
		//supports different languages
		String language = input.language;
		String search = input.text;
		String cmd;
		
		//normalize search 2 times
		Normalizer normalizer_default = Config.inputNormalizers.get(language);
		Normalizer normalizer_light = Config.inputNormalizersLight.get(language);
		
		//get command
		TreeMap<String, String> commands_this = commands.get(language);
		String matched_search = "";
		if (commands_this != null){
			String search_n1 = "";
			String search_n2 = "";
			
			//check first with light normalizer
			if (normalizer_light != null){
				search_n1 = normalizer_light.normalize_text(search);
			}
			cmd = commands_this.get(search_n1);
			matched_search = search_n1;
			
			//then check with stronger normalizer if nothing was found
			if (cmd == null){
				if (normalizer_default != null){
					search_n2 = normalizer_default.normalize_text(search);
				}
				if (!search_n2.isEmpty() && !search_n1.equals(search_n2)){
					cmd = commands_this.get(search_n2);
					matched_search = search_n1;
				}
			}
			
		//no commands list available
		}else{
			return null;
		}
		
		//handle as if it was a direct command
		if (cmd!=null){
			input.text = cmd;
			input.input_type = "direct_cmd";
			NluInterface NLP;
			NLP = new NluCmdReconstructor();
			NluResult result = NLP.interpret(input);
			
			result.bestDirectMatch = matched_search;
			result.certaintyLvl = 1.0d;	//as this is the identity matcher he must be 100% sure.
			
			return result;
			
		}else{
			return null;
		}
	}

	//certainty might result from edit distance ...
	public double getCertaintyLevel(NluResult result) {
		return result.certaintyLvl;
	}

}
