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
