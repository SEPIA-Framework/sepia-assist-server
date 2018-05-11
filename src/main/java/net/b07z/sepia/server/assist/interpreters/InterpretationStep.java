package net.b07z.sepia.server.assist.interpreters;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.database.DataLoader;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;

public interface InterpretationStep {
	
	/**
	 * Call this step of the interpretation chain.
	 * @param input - NLU input
	 * @param cachedResults - temporary cached results to access from following step  
	 * @return
	 */
	public NluResult call(NluInput input, Map<String, NluResult> cachedResults);
	
	/**
	 * Check if its a direct command or return null.
	 */
	public static NluResult getDirectCommand(NluInput input){
		if (input.input_type.matches("direct_cmd")){
			Debugger.println("DIRECT COMMAND", 2); 					//debug
			//System.out.println("DIRECT CMD: " + input.text); 				//debug
			
			//define NL-Processor
			NluInterface NLP = new NluCmdReconstructor();
			//get result
			return NLP.interpret(input);
		}else{
			return null;
		}
	}
	
	/**
	 * Check if its a slash command, aka a command given as text input or return null.
	 */
	public static NluResult getSlashCommand(NluInput input){
		boolean is_slashCMD = RegexParameterSearch.contains_slashCMD(input.text_raw);
		//TODO: add better slash command handler - should the user be allowed to overwrite this with a personal command?
		if (is_slashCMD){
			//define NL-Processor for slash CMDs
			String NLP_keyword_ana_class = Config.keywordAnalyzers.get("default");
			NluInterface NLP = (NluInterface) ClassBuilder.construct(NLP_keyword_ana_class);
			//get result
			return NLP.interpret(input);
		}else{
			return null;
		}
	}
	
	/**
	 * Check if its a response to a previous question of the assistant or return null.
	 */
	public static NluResult getResponse(NluInput input){
		if (input.input_type.matches("response")){
			Debugger.println("RESPONSE", 2); 						//debug
			//System.out.println("RESPONSE: " + input.text); 					//debug
			
			//define NL-Processor
			NluInterface NLP = new ResponseHandler();
			//get result
			return NLP.interpret(input);
		}else{
			return null;
		}
	}
	
	/**
	 * Check if its a personal command aka a command defined by the user in the teach-UI.
	 * Use a tagged-sentence-matcher to match user sentences. Returns null if the similarity is less
	 * than the threshold previously defined.
	 * @param input - NLU input
	 * @param keepResultForLater - cache this result for one of the next NLU steps
	 * @param cacheName - if caching use this key
	 * @param cachedResults - all cached results passed down from step to step
	 */
	public static NluResult getPersonalCommand(NluInput input, boolean keepResultForLater, String cacheName, Map<String, NluResult> cachedResults){
		//Just for the log (this is supposed to be the first QUESTION handler in the chain):
		Debugger.println("QUESTION/COMMENT/CHAT", 2); 			//debug
		//System.out.println("QUESTION: " + input.text); 					//debug
		
		//check personal commands first
		Map<String, Object> filter = new HashMap<>();
		filter.put("language", input.language);
		filter.put("searchText", input.text_raw);
		UserDataInterface userData = input.user.getUserDataAccess();
		JSONArray sentences = userData.getPersonalCommands(input.user, filter);
		//to search personal AND system commands use: sentences = DB.getCommands(filter);
		//System.out.println(sentences.toJSONString()); 		//DEBUG
		DataLoader dl = new DataLoader();
		NluInterface personalCommands_NLP = new NluTaggedSentenceMatcher(dl.makePoolFromSentencesArray(sentences, input.language));
		NluResult pcResult = personalCommands_NLP.interpret(input);
		if (pcResult != null && pcResult.getCertaintyLevel() >= Config.threshold_personal_cmd){
			/*
			System.out.println("pc_res.: " + pc_result.cmd_summary); 				//debug
			System.out.println("pc_res.: " + pc_result.get_certainty_level()); 		//debug
			*/
			return pcResult;
		}else if(keepResultForLater){
			cachedResults.put(cacheName, pcResult);
		}
		return null;
	}
	/**
	 * If all previous steps failed you can try personal commands again with a lower threshold (or get null again).
	 * @param input - NLU input
	 * @param cachedResults - all cached results passed down from step to step
	 */
	public static NluResult tryPersonalCommandAsFallback(NluInput input, Map<String, NluResult> cachedResults){
		//try a lower threshold for personal commands
		NluResult pcResult = cachedResults.get("pcResult");
		if (pcResult != null && pcResult.getCertaintyLevel() >= Config.threshold_personal_cmd_2nd){
			return pcResult;
		}else{
			return null;
		}
	}
	
	/**
	 * Check if its an exact match of one of the predefined fix commands (or get null).
	 */
	public static NluResult getFixCommandsExactMatch(NluInput input){
		return Config.fixCommands_NLP.interpret(input);
	}
	
	/**
	 * Check if its an approximate match of one of the predefined chat/small-talk commands (or get null).
	 */
	public static NluResult getChatSmallTalkMatch(NluInput input, boolean keepResultForLater, String cacheName, Map<String, NluResult> cachedResults){
		NluResult chatResult = Config.fixChats_NLP.interpret(input);
		//is it close enough?
		if (chatResult != null && chatResult.getCertaintyLevel() >= Config.threshold_chats_match){
			return chatResult;
		}else if(keepResultForLater){
			cachedResults.put(cacheName, chatResult);
		}
		return null;
	}
	/**
	 * If all previous steps failed you can try chat/small-talk commands again with a lower threshold (or get null again).
	 * @param input - NLU input
	 * @param cachedResults - all cached results passed down from step to step
	 */
	public static NluResult tryChatSmallTalkAsFallback(NluInput input, Map<String, NluResult> cachedResults){
		//try a lower threshold for chats
		NluResult chatResult = cachedResults.get("chatResult");
		if (chatResult != null && chatResult.getCertaintyLevel() >= Config.threshold_chats_last_chance){
			return chatResult;
		}else{
			return null;
		}
	}
	
	/**
	 * Check if its a match to one of the sentences in the database that belongs to the default assistant userId.
	 * Use a tagged-sentence-matcher to match sentences. Returns null if the similarity is less
	 * than the threshold previously defined.
	 * @param input - NLU input
	 * @param keepResultForLater - cache this result for one of the next NLU steps
	 * @param cacheName - if caching use this key
	 * @param cachedResults - all cached results passed down from step to step
	 */
	public static NluResult getPublicDbSentenceMatch(NluInput input, boolean keepResultForLater, String cacheName, Map<String, NluResult> cachedResults){
		if (Config.useSentencesDB || Config.enableSDK){
			HashMap<String, Object> filter = new HashMap<>();
			filter.put("language", input.language);
			filter.put("includePublic", Boolean.TRUE);
			filter.put("searchText", input.text_raw);
			//filter.put("userIds", "userA, userB, userC, ...");		//we could add more users here
			filter.put("userIds", Config.assistantId);
			JSONArray sentences = DB.getCommands(filter);
			//System.out.println("DB sentences result: " + sentences.toJSONString()); 		//DEBUG
			
			DataLoader dl = new DataLoader();
			NluInterface dbSentences_NLP = new NluTaggedSentenceMatcher(dl.makePoolFromSentencesArray(sentences, input.language));
			//NLU_Interface dbSentences_NLP = new NLU_approximate_matcher(dl.makePoolFromSentencesArray(sentences, input.language));
			NluResult dbSentencesResult = dbSentences_NLP.interpret(input);
			if (dbSentencesResult != null && dbSentencesResult.getCertaintyLevel() >= Config.threshold_chats_match){
				return dbSentencesResult;
			}else if(keepResultForLater){
				cachedResults.put(cacheName, dbSentencesResult);
			}
		}
		return null;
	}
	
	public static NluResult getKeywordAnalyzerResult(NluInput input, boolean keepResultForLater, String cacheName, Map<String, NluResult> cachedResults){
		//get analyzer for the given language
		String analyzerClass = Config.keywordAnalyzers.get(input.language);
		if (analyzerClass == null){
			analyzerClass = Config.keywordAnalyzers.get("default");	//has to be declared!
		}
		NluInterface NLP = (NluInterface) ClassBuilder.construct(analyzerClass);
		//get result
		NluResult kwaResult = NLP.interpret(input);
		//debug
		//System.out.println("result cmd: " + kwaResult.get_command());
		//System.out.println("result certainty: " + kwaResult.get_certainty_level());
		if (kwaResult != null && !kwaResult.getCommand().matches(CMD.NO_RESULT)){ 		//TODO: what about NO_RESULT? can it be an intended result? Should we filter it?
			return kwaResult;
		}else if(keepResultForLater){
			cachedResults.put(cacheName, kwaResult);
		}
		return kwaResult;
	}

}
