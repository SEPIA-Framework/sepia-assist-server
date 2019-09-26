package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.core.assistant.CMD;

/**
 * Class that organizes the chain of interpretation attempts to find the user intent.
 * 
 * @author Florian Quirin
 *
 */
public class InterpretationChain {
	
	private List<InterpretationStep> steps;
	private Map<String, NluResult> cachedResults;
	private NluResult lastNoResult;
	
	//available steps
	public static Map<String, InterpretationStep> coreSteps = new HashMap<>();
	public static Map<String, InterpretationStep> availableSteps = new HashMap<>();		//special steps "WEB:..." and "CLASS:..." see 'InterpretationStep' and 'Config'
	static {
		//check for input modifiers
		coreSteps.put("applyInputModifiers", 	(input, cachedResults) -> InterpretationStep.applyInputModifiers(input));
		//direct command
		coreSteps.put("getDirectCommand", 		(input, cachedResults) -> InterpretationStep.getDirectCommand(input));
		//response to previous input
		coreSteps.put("getResponse", 			(input, cachedResults) -> InterpretationStep.getResponse(input));
		//slash command
		coreSteps.put("getSlashCommand",		(input, cachedResults) -> InterpretationStep.getSlashCommand(input));
		
		//spoken question/comment/chat
		//TODO: Optimize database access! We are loading data from the DB multiple times here, each time with a slightly different configuration
		//TODO: ... it would be better if we could just do one call and then distribute the data, also we can:
		//TODO: Normalize first, store normalized input and then go into the interpreters
		
		//check if its a personal command aka a command defined by the user (in SDK or teach-UI)
		availableSteps.put("getPersonalCommand",		(input, cachedResults) -> InterpretationStep.getPersonalCommand(input, true, "pcResult", cachedResults));
		//check for exact sentence match of normalized text in commands ("teachIt") - high priority fixed commands
		availableSteps.put("getFixCommandsExactMatch",	(input, cachedResults) -> InterpretationStep.getFixCommandsExactMatch(input));
		//check for approximate matches in chats ("chat" aka small-talk)
		availableSteps.put("getChatSmallTalkMatch",		(input, cachedResults) -> InterpretationStep.getChatSmallTalkMatch(input, true, "chatResult", cachedResults));
		//check for DB sentences - NOTE: make sure to avoid duplicated sentences with previous step (smallTalk)
		availableSteps.put("getPublicDbSentenceMatch",	(input, cachedResults) -> InterpretationStep.getPublicDbSentenceMatch(input, false, "dbSentencesResult", cachedResults));
		//check with complex keyword matcher
		availableSteps.put("getKeywordAnalyzerResult",	(input, cachedResults) -> InterpretationStep.getKeywordAnalyzerResult(input, false, "kwaResult", cachedResults));
		//check personal commands again with lower threshold (loads from "pcResult" cache)
		availableSteps.put("tryPersonalCommandAsFallback",	(input, cachedResults) -> InterpretationStep.tryPersonalCommandAsFallback(input, cachedResults));
		//check chats again with lower threshold (loads from "chatResult" cache)
		availableSteps.put("tryChatSmallTalkAsFallback",	(input, cachedResults) -> InterpretationStep.tryChatSmallTalkAsFallback(input, cachedResults));
		
		//TODO: make a new NLP to analyze context commands - search for simple context keywords, check last context, return to NLU
	}
	
	/**
	 * Build new chain.
	 */
	public InterpretationChain(){
		steps = new ArrayList<>();
		cachedResults = new HashMap<>();
	}
	
	/**
	 * Add a step to the interpretation chain.
	 * @param step - a step can be any method that accepts the NLU input and returns an NLU result or null (fail).
	 */
	public InterpretationChain addStep(InterpretationStep step){
		steps.add(step);
		return this;
	}
	/**
	 * Set a list of steps as the interpretation chain (overwrites previous settings/steps).
	 */
	public InterpretationChain setSteps(List<InterpretationStep> steps){
		this.steps = steps;
		return this;
	}

	/**
	 * Interpret user input and get best result/command. Main interpretation chain.
	 * 
	 * @return NLU_Result with best command
	 */
	public NluResult getResult(NluInput input){
		
		//decide how to proceed - choose NL-Processor
		NluResult result = null;
		
		//iterate over the individual NLU steps
		for (InterpretationStep nluStep : steps){
			result = nluStep.call(input, cachedResults);
			if (result != null && !result.getCommand().equals(CMD.NO_RESULT)){	//TODO: what about NO_RESULT? can it be an intended result? Should we filter it?
				return result;
			}else if (result != null){
				lastNoResult = result;
			}
		}
		
		//restore at least a valid NO_RESULT?
		if (result == null && lastNoResult != null){
			result = lastNoResult;
		}
		
		/*
		if (result != null){
			System.out.println("res.: " + result.get_certainty_level());		//debug
			System.out.println("res. cmd: " + result.get_command());			//debug
			System.out.println("res. params: " + result.get_best_result_JSON().toJSONString());			//debug
		}
		*/

		return result;
	}

}
