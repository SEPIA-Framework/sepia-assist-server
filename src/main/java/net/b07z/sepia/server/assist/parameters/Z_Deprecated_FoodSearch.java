package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.HashMap;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

/**
 * Class to generate the initial parameter set for food search 
 * 
 * @author Florian Quirin
 *
 */
public class Z_Deprecated_FoodSearch {
	
	//input
	NluInput nluInput;
	HashMap<String, String> preCheckedParameters;		//to avoid multiple runs of the same scripts you can pass this down to all the methods
	
	//output
	int score;
	ArrayList<String> guesses; 	//any parameter that could not be double-checked can be tried to "guess" and it's name should be tracked here then
	
	public Z_Deprecated_FoodSearch setup(NluInput nluInput, HashMap<String, String> preCheckedParameters){
		this.nluInput = nluInput;
		this.preCheckedParameters = preCheckedParameters;
		this.score = 0;
		this.guesses = new ArrayList<>();
		return this;
	}
	
	public int getScore(){
		return score;
	}
	
	public ArrayList<String> getGuesses(){
		return guesses;
	}
	
	public HashMap<String, String> getParameters(){
		HashMap<String, String> pv = preCheckedParameters;
		if (pv == null){
			pv = new HashMap<>();
		}
		String thisText = nluInput.text; 		//extra normalization required?
		Parameter_Handler aHandler = null;
		Parameter_Handler bHandler = null;
				
		//food item
		String p = PARAMETERS.FOOD_ITEM;
		if (!pv.containsKey(p)){
			aHandler = new FoodItem();
			aHandler.setup(nluInput);
			String item = aHandler.extract(thisText);
			if (!item.isEmpty()){
				score++;
			}
			pv.put(p, item);
		}
		
		//food class
		p = PARAMETERS.FOOD_CLASS;
		if (!pv.containsKey(p)){
			bHandler = new FoodClass();
			bHandler.setup(nluInput);
			String item = bHandler.extract(thisText);
			if (!item.isEmpty()){
				score++;
			}
			pv.put(p, item);
		}
		
		return pv;
	}

}
