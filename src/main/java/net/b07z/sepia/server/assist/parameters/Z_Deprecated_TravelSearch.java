package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.HashMap;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

/**
 * Class to generate the initial parameter set for any travel search 
 * 
 * @author Florian Quirin
 *
 */
public class Z_Deprecated_TravelSearch {
	
	//input
	NluInput nluInput;
	HashMap<String, String> preCheckedParameters;		//to avoid multiple runs of the same scripts you can pass this down to all the methods
	
	//output
	int score;
	ArrayList<String> guesses; 	//any parameter that could not be double-checked can be tried to "guess" and it's name should be tracked here then
	
	public Z_Deprecated_TravelSearch setup(NluInput nluInput, HashMap<String, String> preCheckedParameters){
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
		
		Parameter_Handler pHandler1 = null;
		Parameter_Handler pHandler2 = null;
		Parameter_Handler pHandler3 = null;
		Parameter_Handler pHandler4 = null;
		Parameter_Handler pHandler5 = null;
		Parameter_Handler pHandler6 = null;
				
		//Time
		String p = PARAMETERS.TIME;
		if (!pv.containsKey(p)){
			pHandler1 = new DateAndTime();
			pHandler1.setup(nluInput);
			String result = pHandler1.extract(thisText);
			if (!result.isEmpty()){
				score++;
			}
			pv.put(p, result);
		}
		
		//Travel type
		p = PARAMETERS.TRAVEL_TYPE;
		if (!pv.containsKey(p)){
			pHandler2 = new TravelType();
			pHandler2.setup(nluInput);
			String result = pHandler2.extract(thisText);
			if (!result.isEmpty()){
				score++;
			}
			pv.put(p, result);
		}
		
		//Travel request info
		p = PARAMETERS.TRAVEL_REQUEST_INFO;
		if (!pv.containsKey(p)){
			pHandler6 = new TravelRequestInfo();
			pHandler6.setup(nluInput);
			String result = pHandler6.extract(thisText);
			if (!result.isEmpty()){
				score++;
			}
			pv.put(p, result);
		}
		
		//Destination
		p = PARAMETERS.LOCATION_END;
		if (!pv.containsKey(p)){
			pHandler3 = new LocationEnd();
			pHandler3.setup(nluInput);
			String result = pHandler3.extract(thisText);
			if (!result.isEmpty()){
				score++;
			}
			pv.put(p, result);
		}
		
		//Waypoint
		p = PARAMETERS.LOCATION_WAYPOINT;
		if (!pv.containsKey(p)){
			pHandler4 = new LocationWaypoint();
			pHandler4.setup(nluInput);
			String result = pHandler4.extract(thisText);
			if (!result.isEmpty()){
				score++;
			}
			pv.put(p, result);
		}
		
		//Start
		p = PARAMETERS.LOCATION_START;
		if (!pv.containsKey(p)){
			pHandler5 = new LocationStart();
			pHandler5.setup(nluInput);
			String result = pHandler5.extract(thisText);
			if (!result.isEmpty()){
				score++;
			}
			pv.put(p, result);
		}
		
		return pv;
	}

}
