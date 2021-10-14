package net.b07z.sepia.server.assist.answers;

import java.util.HashMap;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

/**
 * This class connects parameters to default questions and commands to default answers. The maps can either be hard-coded or loaded
 * from DB on startup (or loaded live?).
 * 
 * @author Florian Quirin
 *
 */
public class DefaultReplies {
	
	//private static HashMap<String, String> answers; 		//TODO: how to handle all the specific answers that ends a service?
	private static HashMap<String, String> questions;
	
	/**
	 * Prepare the default replies by loading the links. Should be used on server start.
	 */
	public static void setupDefaults(){
		questions = new HashMap<>();
		questions.put(PARAMETERS.LOCATION_START, "directions_ask_start_0a");
		questions.put(PARAMETERS.LOCATION_END, "directions_ask_end_0a");
		//questions.put(PARAMETERS.TIME, "flights_ask_time_0a");
		
		//TODO: add more or replace by list or DB --- UPDATE PLZ
	}
	
	/**
	 * Get the link to the question that can be used to ask for this parameter. The link is a reference to the database entry
	 * that can be called via one of the ANS_Loader_Interface methods (e.g. getAnswer).  
	 * @param parameter - the "Parameter" defined in service or interview module
	 */
	public static String getQuestionLink(Parameter parameter){
		String custom = parameter.getQuestion();
		if (!custom.isEmpty()){
			return custom;
		}else{
			return questions.get(parameter.getName());
		}
	}

}
