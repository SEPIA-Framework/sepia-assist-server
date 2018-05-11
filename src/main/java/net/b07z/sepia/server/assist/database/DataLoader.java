package net.b07z.sepia.server.assist.database;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.data.Language;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * This class collects all the data loaders that are required by NLPs and answer-classes, 
 * e.g. methods that pre-load answers from a database or file.
 *  
 * @author Florian Quirin
 *
 */
public class DataLoader {
	
	//helper to track errors
	String last_line = "";
	int i_valid = 0;		//valid lines in data
	int i_stored = 0;		//stored lines (depending on normalizer it can be bigger than i_valid)
	
	//------getters------
	
	/**
	 * Get the last loaded line. In case of an error this can be used to find it.
	 */
	public String getLastLine(){
		return last_line;
	}
	/**
	 * Get the number of valid entries (lines in a file, objects in array, etc.) found in data object.
	 */
	public int getValidEntries(){
		return i_valid;
	}
	/**
	 * Get the number of stored entries. Compared to getValidEntries() this can be larger e.g. if a set of normalizers
	 * creates multiple different versions of the input text.
	 */
	public int getStoredEntries(){
		return i_stored;
	}
	/**
	 * If you want to reuse the same DataLoader consider a counter (valid entries, stored entries) reset.
	 */
	public void resetCounters(){
		i_valid = 0;
		i_stored = 0;
	}
	
	//------Loading scripts------
	
	//TODO: add loading from elasticSearch, add updatePool methods
	
	/**
	 * Load predefined commands from txt-files. File names are build for every language as follows:
	 * file_base_en.txt, file_base_de.txt, ... as supported.
	 * @param file_base - path + base name of txt-file used to load the commands 
	 */
	public Map<String, TreeMap<String, String>> loadCommandsFromFilebase(String file_base){
		
		Map<String, TreeMap<String, String>> map = new HashMap<>();
		for (String l : Config.supportedLanguages){
			TreeMap<String, String> chats_xyz = loadCommandsFromFileToMap_Reverse(file_base + "_" + l + ".txt", l);
			map.put(l, chats_xyz);
		}
		return map;
	}
	
	/**
	 * Load predefined answers for all languages from txt-files. File names are build for every language as follows:
	 * file_base_en.txt, file_base_de.txt, ... as supported. 
	 * @param file_base - path + base name of txt-file used to load the answers
	 */
	public Map<String, Map<String, List<Answer>>> loadAnswersFromFilebase(String file_base){
		
		Map<String, Map<String, List<Answer>>> answers = new HashMap<>();
		for (String l : Config.supportedLanguages){
			Map<String, List<Answer>> answers_xyz = loadAnswersFromFileToMap(file_base + "_" + l + ".txt", l);
			answers.put(l, answers_xyz);
		}
		return answers;
	}
	
	//------Loader methods------
	
	/**
	 * Make a pool of commands from a JSONArray, e.g. taken from teach-API: getPersonalCommands().
	 * Uses 2 normalizers (light and default) and adds both results.
	 * @param sentences - JSONArray of sentences (as seen in teach-API Sentence.class)
	 * @param language - language code 
	 * @return
	 */
	public Map<String, TreeMap<String, String>> makePoolFromSentencesArray(JSONArray sentences, String language){
		TreeMap<String, String> pool = new TreeMap<>(Collections.reverseOrder());
		Map<String, TreeMap<String, String>> fullPool = new HashMap<>();
		//use two normalizers
		Normalizer normalizer_light = Config.inputNormalizersLight.get(language);
		Normalizer normalizer_default = Config.inputNormalizers.get(language);
		
		try{
			JSONObject sentence;
			i_valid = 0;
			i_stored = 0;
			for (int i=0; i<sentences.size(); i++){
				//the result of "sentences.get("sentence")" is an array because it usually holds many languages 
				sentence = (JSONObject) ((JSONArray) JSON.getJObject(sentences, i).get("sentence")).get(0);
				String text = (String) sentence.get("tagged_text");
				if (text != null && !text.isEmpty()){
					//normalize tags ??? - think it is safe
				}else{
					text = (String) sentence.get("text");
				}
				String cmd = (String) sentence.get("cmd_summary");
				if (!cmd.isEmpty()){
					i_valid++;
					
					//create a default and a slightly stronger normalized version and add both
					String text_n1 = normalizer_default.normalize_text(text);
					if (!text_n1.isEmpty()){
						//add
						pool.put(text_n1, cmd);
						i_stored++;
					}
					String text_n2 = normalizer_light.normalize_text(text);
					if (!text_n1.equals(text_n2) && !text_n2.isEmpty()){
						//add
						pool.put(text_n2, cmd);
						i_stored++;
					}
				}
			}
			fullPool.put(language, pool);
			return fullPool;
			
		}catch (Exception e){
			Debugger.println("DataLoader.makePoolFromSentencesArray(..) failed: " + e.getMessage() + " at " + e.getStackTrace()[0], 1);
			//e.printStackTrace();
			fullPool.put(language, pool);
			return fullPool;
		}
	}
	
	/**
	 * load commands to a REVERSE order sorted map - SORTED! is important to avoid miss-matching from simple to complex.
	 * Uses 2 normalizers (light and default) and adds both results.
	 * @param filename - file with full path
	 * @param language - language, mainly used for normalizers
	 * @return
	 */
	public TreeMap<String, String> loadCommandsFromFileToMap_Reverse(String filename, String language) {
		
		TreeMap<String, String> commands = new TreeMap<>(Collections.reverseOrder());	
		Path path = Paths.get(filename);
		
		//load from file
		try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
			//i_valid = 0;
			//i_stored = 0;
			last_line = "";
			lines.forEachOrdered(line -> {
				
				//System.out.println("line: " + line); 	//debug
				last_line = line;
				if (!line.trim().isEmpty() && !line.trim().startsWith("#")){
					
					//check for command
					if (!line.contains("command=")){
						System.err.println("ERROR --- line: " + last_line + " in FILE: " + filename); 	//debug
						
					//all clear (I think)
					}else{
						i_valid++;
					
						//split line  
						String[] tmp_line = line.split(";;", 2);
						String sent = tmp_line[0].trim();
						String cmd = tmp_line[1].trim().replaceFirst("^command=", "").trim();
						
						//remove help tags
						cmd = cmd.replaceFirst("\\s#.*", "").trim();
						//cmd = cmd.replaceFirst("#\\w*$", "").trim();
						
						//handle context and environment?
						//
						
						//format command
						cmd = cmd.replaceAll(";;\\s+", ";;");
						if (!cmd.isEmpty()){
							String sent_n1 = "";
							String sent_n2 = "";
							
							//normalize sentence 2 times with light and default - use language specific method
							Normalizer normalizer_default = Config.inputNormalizers.get(language);
							if (normalizer_default != null){
								sent_n1 = normalizer_default.normalize_text(sent);
								if (!sent_n1.isEmpty()){
									//add
									commands.put(sent_n1, cmd);
									i_stored++;
								}
							}
							Normalizer normalizer_light = Config.inputNormalizersLight.get(language);
							if (normalizer_light != null){
								sent_n2 = normalizer_light.normalize_text(sent);
								if (!sent_n1.equals(sent_n2) && !sent_n2.isEmpty()){
									//add
									commands.put(sent_n2, cmd);
									i_stored++;
								}
							}
						}
					}
				}
				  
			});
			Debugger.println("DataLoader.loadCommandsFromFileToMap_Reverse(), number: " + i_stored + "(" + i_valid + ") , file: " + filename, 2);
			return commands;
			
		//ERROR?
		}catch (Exception e){
			System.err.println("ERROR --- line: " + last_line + " in FILE: " + filename); 	//debug
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * load answers to HashMap, mostly used for answers not commands (means e.g. no normalizer used)
	 * @param filename - file with full path
	 * @param language - language code used to create "Answer"
	 */
	public Map<String, List<Answer>> loadAnswersFromFileToMap(String filename, String language) {
		
		Language lang = Language.from(language);
		
		Map<String, List<Answer>> answersThis = new HashMap<>();		
		Path path = Paths.get(filename);
		
		//load from file
		try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
			//i_valid = 0;
			//i_stored = 0;
			last_line = "";
			lines.forEachOrdered(line -> {
				
				last_line = line;
				if (!line.trim().isEmpty() && !line.trim().startsWith("#")){
					i_valid++;
					//System.out.println(line); 		//debug
					//split line  
					String[] tmp_line = line.split(";;");
					String tmp_key = tmp_line[0].trim();
					List<Answer> tmp_list;
					if (answersThis.containsKey(tmp_key)){
						tmp_list = answersThis.get(tmp_key);
						tmp_list.add(Answer.importAnswerString(line, lang, false));
						answersThis.put(tmp_key, tmp_list);
						i_stored++;
					}else{
						tmp_list = new ArrayList<Answer>();
						tmp_list.add(Answer.importAnswerString(line, lang, false));
						answersThis.put(tmp_key, tmp_list);
						i_stored++;
					}
				} 
			});
			Debugger.println("DataLoader.loadAnswersFromFileToMap(), number: " + i_stored + "(" + i_valid + ") , file: " + filename, 2);
			return answersThis;
			
		//ERROR?
		}catch (Exception e){
			e.printStackTrace();
			System.err.println("ERROR --- line: " + last_line + " in FILE: " + filename); 	//debug
			return null;
		}
	}
	
	//list commands - used to debug the lists
	public static void list_data(HashMap<String, TreeMap<String, String>> map){
		long tic = Debugger.tic(); 	//take time
		for(Entry<String, TreeMap<String, String>> entry : map.entrySet()) {
			System.out.println("----------" + entry.getKey() + "----------");
			plot_list(entry.getValue());
		}
		System.out.println("-------------------------");
		System.out.println("time to run through all predefined commands: " + Debugger.toc(tic) + " ms.");
	}
	
	//print out all commands of a TreeMap<String, String>
	private static void plot_list(TreeMap<String, String> map){
		for(Entry<String, String> entry : map.entrySet()) {
			
			String key = entry.getKey();
			String value = entry.getValue();
			
			System.out.println(key + " => " + value);
		}
	}

}
