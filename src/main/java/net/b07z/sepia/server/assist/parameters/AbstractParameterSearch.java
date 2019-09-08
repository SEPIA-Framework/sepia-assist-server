package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Can be used for an individual set of parameters as first search script.
 * 
 * @author Florian Quirin
 *
 */
public class AbstractParameterSearch {
	
	ArrayList<Parameter> parameters = new ArrayList<>();
	boolean doGuess = true;
	
	/**
	 * Set the parameters this search has to analyze.
	 */
	public AbstractParameterSearch setParameters(String... parameterNames){
		for (String p : parameterNames){
			parameters.add(new Parameter(p));
		}
		return this;
	}
	/**
	 * Set the parameters this search has to analyze.
	 */
	public AbstractParameterSearch setParameters(Parameter... parameters){
		for (Parameter p : parameters){
			this.parameters.add(p);
		}
		return this;
	}
	
	//input
	NluInput nluInput;
	Map<String, String> preCheckedParameters;		//to avoid multiple runs of the same scripts you can pass this down to all the methods
	
	//output
	int score;
	ArrayList<String> guesses; 	//any parameter that could not be double-checked can be tried to "guess" and it's name should be tracked here then
	
	/**
	 * Setup the search.
	 * @param nluInput - input from NLU
	 * @param preCheckedParameters - a map of parameters that have been checked before to avoid redundant checks. This is the simple map, parameters can also use the NLU_Input parameter store.
	 */
	public AbstractParameterSearch setup(NluInput nluInput, Map<String, String> preCheckedParameters){
		this.nluInput = nluInput;
		this.preCheckedParameters = preCheckedParameters;
		this.score = 0;
		this.guesses = new ArrayList<>();
		return this;
	}
	
	/**
	 * Prevent guessing of parameters when extraction fails (some parameters have a 'guess' implementation.
	 */
	public void preventGuessing(){
		doGuess = false;
	}
	
	/**
	 * Each successful parameter extraction scores +1. Get the total score with this.
	 */
	public int getScore(){
		return score;
	}
	
	/**
	 * In case some parameters had to be 'guessed' their names are collected here.
	 */
	public ArrayList<String> getGuesses(){
		return guesses;
	}
	
	/**
	 * Extract the parameters and return map. Any found parameters will also be added to the preCheckedParameters map.
	 */
	public Map<String, String> getParameters(){
		Map<String, String> pv = new HashMap<>();
		String thisText = nluInput.text; 		//extra normalization required?
		ParameterHandler paramHandler;
				
		for (Parameter pa : parameters){
			//any normal parameter
			String name = pa.getName();
			if (!preCheckedParameters.containsKey(name)){
				paramHandler = pa.getHandler();
				//System.out.println("Handler: " + paramHandler.getClass().getCanonicalName()); 	//DEBUG
				paramHandler.setup(nluInput);
				String result = paramHandler.extract(thisText);
				boolean guess = false;
				if (result.isEmpty() && doGuess){
					result = paramHandler.guess(thisText);
					guess = true;
				}
				if (!result.isEmpty() && guess){
					//clean up guessed item by removing all other items
					//TODO: remove all other parameters (this has been removed from abstract handler)
					/* example:
					if (colorHandler != null) item = colorHandler.remove(item, colorHandler.getFound());
					if (genHandler != null) item = genHandler.remove(item, genHandler.getFound());
					if (sizeHandler != null) item = sizeHandler.remove(item, sizeHandler.getFound());
					item = item.trim();
					if (brandHandler != null && !item.equals(brandHandler.getFound())){
						item = brandHandler.remove(item, brandHandler.getFound());
					}
					*/
				}
				if (!result.isEmpty()){
					if (guess){
						Debugger.println("GUESS (CLEANED): " + result + " = " + name + "", 3);
						guesses.add(name);
					}
					//Increase score (under certain conditions)
					if (!paramHandler.isGeneric()){
						score++;
					}
				}
				preCheckedParameters.put(name, result);
				pv.put(name, result);
			}else{
				pv.put(name, preCheckedParameters.get(name));
			}
		}
		
		return pv;
	}

}
