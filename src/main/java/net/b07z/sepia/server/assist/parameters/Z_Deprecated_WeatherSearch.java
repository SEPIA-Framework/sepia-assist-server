package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.HashMap;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class to generate the initial parameter set for weather search 
 * 
 * @author Florian Quirin
 *
 */
public class Z_Deprecated_WeatherSearch {
	
	//input
	NluInput nluInput;
	HashMap<String, String> preCheckedParameters;		//to avoid multiple runs of the same scripts you can pass this down to all the methods
	
	//output
	int score;
	ArrayList<String> guesses; 	//any parameter that could not be double-checked can be tried to "guess" and it's name should be tracked here then
	
	public Z_Deprecated_WeatherSearch setup(NluInput nluInput, HashMap<String, String> preCheckedParameters){
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
		Parameter_Handler dateHandler = null;
		Parameter_Handler placeHandler = null;
				
		//Time
		String p = PARAMETERS.TIME;
		if (!pv.containsKey(p)){
			dateHandler = new DateAndTime();
			dateHandler.setup(nluInput);
			String date = dateHandler.extract(thisText);
			if (!date.isEmpty()){
				score++;
			}
			pv.put(p, date);
		}
		
		//Place
		p = PARAMETERS.PLACE;
		if (!pv.containsKey(p)){
			placeHandler = new Place();
			placeHandler.setup(nluInput);
			String place = placeHandler.extract(thisText);
			boolean guess = false;
			if (place.isEmpty()){
				place = placeHandler.guess(thisText);
				guess = true;
			}
			if (!place.isEmpty()){
				if (guess){
					//clean up guessed item
					//TODO: clean up
					Debugger.println("GUESS CLEAN: " + place + " = " + PARAMETERS.PLACE + " (Place)", 3);
					guesses.add(p);
				}
				score++;
			}
			if (place.isEmpty()){
				place = "<" + Parameter.Defaults.user_location + ">";
			}
			pv.put(p, place);
		}else{
			if (!pv.get(p).isEmpty()){
				score++;
			}
		}
		
		return pv;
	}

}
