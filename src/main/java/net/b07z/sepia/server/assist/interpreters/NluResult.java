package net.b07z.sepia.server.assist.interpreters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Every NLU interpreter is supposed to return an NLU_Result. NLU_Results contain all
 * necessary information to make an API call using the command type (e.g. "weather")
 * and related parameters (e.g. place="Berlin"). Usually one would use the best interpreter
 * result asking for get_command() and get_parameter(name), but lower scoring results can be
 * stored as well. Finally the http GET implementation will use the getBestResultJSON()
 * method to create and return an JSON string to the client.
 * NLU_Result is supposed to include NLU_Input to maintain access to user variables.
 * 
 * @author Florian Quirin
 *
 */
public class NluResult {
	
	//modified input
	public String normalizedText = "";			//input text after normalization
	//variables
	public String language = "en";				
	public String context = "default";			//context is what the user did/said before to answer queries like "do that again" or "and in Berlin?"
	public String environment = "default";		//environments can be stuff like home, car, phone etc. to restrict and tweak some results 
	public int mood = -1;						//mood level 0-10 (10: super happy) of ILA (or whatever the name of the assistant is) can be passed around too
	//command summary
	public String cmdSummary = "";				//this command as short summary, used e.g. as comparison to last_cmd of NLU_Input
	public String bestDirectMatch = "---";		//if this command was found by a direct match, save the best sentence-key here. It helps to understand what command was found. 
	//... more to come
	
	//NLU result includes NLU input
	public NluInput input;
	
	//result found?
	boolean foundResult = false;		//did the NL-Processor produce a result?
	
	//best result
	int bestScoreIndex = 0;				//index of the best result in result_array
	String commandType = "";			//best result command type (e.g. weather)
	Map<String, String> parameters;		//parameters of best result as HashMap
	double certaintyLvl = 0.0d;			//scoring of the best result (from 0-1), strongly depends on the NL-Processor how reliable that is
	
	//all NLP results are stored here:
	List<String> possibleCMDs = new ArrayList<String>();			//make a list of possible interpretations (commands) of the text
	List<Map<String, String>> possibleParameters = new ArrayList<>();		//possible parameters of these commands
	List<Integer> possibleScore = new ArrayList<Integer>();		//save scores to decide which one is correct/best command
	
	/**
	 * Use this constructor only if you know what you are doing! ^^.
	 * The default one is NLU_Result(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex); 
	 */
	public NluResult(){
	}
	/**
	 * Use this constructor only if you know what you are doing! ^^.
	 * The default one is NLU_Result(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex); 
	 * This one sets the input, but usually this should be done by the NL-Processor (interpreter)
	 */
	public NluResult(NluInput input){
		setInput(input);
	}
	/**
	 * The default constructor for NLU_Results. It will automatically set bestCommand and bestCommand_parameters so please use this 
	 * unless you know what you are doing ;-). It will also set the context to the best command.
	 * Note: don't forget to add things like mood, language, etc. and the input!
	 * 
	 * @param possibleCMDs - an ArrayList of Strings containing all possible commands identified by the NL-Processor
	 * @param possibleParameters - parameters parsed by NLP related to the identified commands
	 * @param possibleScore - scores of the identified commands (how certain is the NLP that this is the right result? values 0-1)
	 * @param bestScoreIndex - index of the best result (highest score) 
	 */
	public NluResult(List<String> possibleCMDs, List<Map<String, String>> possibleParameters, List<Integer> possibleScore, int bestScoreIndex){
		setBestResult(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex);
	}
	
	/**
	 * Set input in case you are loading CMD_summaries or NLPs that don't set it.<br>
	 * Be careful! It will overwrite some parameters, so in case the NLP modifies them use this before you apply the modified ones.
	 * As context might be set already in the default constructor it is checked before (for "" or "default").
	 * @param input - NluInput sent to server by client-interface
	 */
	public void setInput(NluInput input){
		language = input.language;
		if (context.matches("") || context.matches("default"))	context = input.context;	//sets the old context
		environment = input.environment;
		mood = input.mood;
		this.input = input;
	}
	/**
	 * Sets the best result and "activates" the NluResult. Typically this is only called by the constructor, but in case
	 * you use an alternative constructor like NluResult(NluInput input) you have to set this afterwards.<br>
	 * Use this in combination with set_input to load a "cmd_summary" for example.
	 * 
	 * @param possibleCMDs - an ArrayList of Strings containing all possible commands identified by the NL-Processor
	 * @param possibleParameters - parameters parsed by NLP related to the identified commands
	 * @param possibleScore - scores of the identified commands (how certain is the NLP that this is the right result? values 0-1)
	 * @param bestScoreIndex - index of the best result (highest score) 
	 */
	public void setBestResult(List<String> possibleCMDs, List<Map<String, String>> possibleParameters, List<Integer> possibleScore, int bestScoreIndex){
		this.possibleCMDs = possibleCMDs;
		this.possibleParameters = possibleParameters;
		this.possibleScore = possibleScore;
		this.bestScoreIndex = bestScoreIndex;
		if (possibleCMDs.size()>0){
			foundResult = true;
			commandType = possibleCMDs.get(bestScoreIndex);
			context = commandType;		//can be edited later
			parameters = possibleParameters.get(bestScoreIndex);
			cmdSummary = commandType + ";;" + Converters.mapStrStr2Str(parameters);
			Debugger.println("CMD Summary: " + cmdSummary, 2); 		//debug
		}
	}
	
	/**
	 * Returns the best (highest scoring) result of the NLU interpreter 
	 * as a JSON string object.
	 * 
	 * @return      the best result as JSON object
	 * @see         JSONObject
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getBestResultJSON(){
		JSONObject obj = new JSONObject();
		if (foundResult && !commandType.matches(CMD.NO_RESULT)) {
			obj.put("result", "success");
			obj.put("language", language);
			obj.put("command", commandType);
			obj.put("certainty", new Double(certaintyLvl));
			obj.put("bestDirectMatch", bestDirectMatch);
			obj.put("context", context);
			obj.put("environment", environment);
			obj.put("mood", new Integer(mood));
			JSONObject params = Converters.mapStrStr2Json(parameters);
			/*for (String e : parameters){
				params.put(e.replaceFirst("=.*", "").trim(), e.replaceFirst(".*?=", "").trim()); 
			}*/
			obj.put("parameters", params);
			return obj;
		}else{
			obj.put("result", "fail");
			obj.put("language", language);
			obj.put("error", "no match found");
			obj.put("answer", "sorry, but I totally didn't understand that! :-(");
			obj.put("context", context);
			obj.put("environment", environment);
			obj.put("mood", new Integer(mood));
			return obj;
		}
	}
	
	/**
	 * Get raw text input as sent by user.
	 */
	public String getRawInput(){
		return input.textRaw;
	}
	/**
	 * Get text input of user after normalization.
	 */
	public String getNormalizedInput(){
		return input.text;
	}
	
	/**
	 * INFO: use this inside interviews, inside services use "getRequiredParameter", "getOptionalParameter" or "getChoiceParameter" when possible (not too old)!<br> 
	 * Get a certain parameter of the best NLU result command 
	 * as a String, e.g. command="weather", parameter: place="Berlin".<br>
	 * If there is no such parameter returns empty string "".
	 * 
	 * @param  p - 	the parameter you are looking for (search, type, destination, etc. ..., see command list).
	 * @return      parameter as a string
	 */
	public String getParameter(String p){
		String res = "";
		if (parameters != null){
			res = parameters.get(p);
			if (res == null)
				return "";
			else
				return res;
		}else{
			return "";
		}
	}
	
	/**
	 * Get a parameter that is required. "data" can be empty so use this to check it.
	 */
	public Parameter getRequiredParameter(String p){
		String dataString = getParameter(p);
		JSONObject data = new JSONObject();
		if (!dataString.isEmpty()){
			data = JSON.parseStringOrFail(dataString);
		}
		return new Parameter(p, data).setRequired(true);
	}
	/**
	 * Get a parameter that is optional. Check for "isEmpty()" and use "getData" or "getDefaultValue" accordingly.
	 * NOTE: compared to the "defaultValue" set in the ServiceInfo Parameter constructor this value is (probably) never influencing the
	 * build() process of the parameter.  
	 */
	public Parameter getOptionalParameter(String p, Object defaultValue){
		String dataString = getParameter(p);
		Parameter param = new Parameter(p, defaultValue);
		//System.out.println("getOptionalParameter - dataString: " + dataString + ", for p: " + p ); 		//debug
		if (!dataString.isEmpty()){
			param.setData(JSON.parseStringOrFail(dataString));
		}
		return param;
	}
	/**
	 * Basically the same as "getRequiredParameter" just that for this one "isEmpty()" can be true.
	 */
	/*
	public Parameter getChoiceParameter(String p){
		return getRequiredParameter(p).setRequiredChoice(true);
	}
	*/
	
	/**
	 * Set or overwrite a certain parameter and reconstruct cmd_summary if command_type is set
	 * @param param - parameter to set
	 * @param value - new value to parameter
	 */
	public void setParameter(String param, String value){
		if (parameters == null){
			parameters = new HashMap<String, String>();
			parameters.put(param, value);
		}else{
			parameters.put(param, value);
		}
		if (commandType != null && !commandType.matches("")){
			cmdSummary = commandType + ";;" + Converters.mapStrStr2Str(parameters);
		}
	}
	/**
	 * Remove a certain parameter and reconstruct cmd_summary if command_type is set
	 * @param param - parameter to remove
	 */
	public void removeParameter(String param){
		if (parameters != null){
			parameters.remove(param);
			if (commandType != null && !commandType.matches("")){
				cmdSummary = commandType + ";;" + Converters.mapStrStr2Str(parameters);
			}
		}
	}
	/**
	 * Build final set and check it for this parameter. Usually this is in the interview but the response handler might need it too.
	 */
	public boolean isParameterFinal(String parameter){
		Set<String> finals = new HashSet<>();
		String finals_str = getParameter(PARAMETERS.FINAL).replaceAll("^\\[|\\]$", "");
		if (!finals_str.isEmpty()){
			for (String p : finals_str.split(",")){
				finals.add(p);
			}
		}
		return finals.contains(parameter);
	}
	/**
	 * Remove a parameters from the "finals".
	 */
	public void removeFinal(String param){
		if (parameters != null){
			String fin = parameters.get(PARAMETERS.FINAL);
			//get final parameters
			Set<String> finals = new HashSet<>();
			String finals_str = (fin == null)? "" : fin.replaceAll("^\\[|\\]$", "");
			if (!finals_str.isEmpty()){
				for (String p : finals_str.split(",")){
					if (!p.equals(param)){
						finals.add(p);
					}
				}
			}
			fin = finals.toString().replaceAll("\\s+", "");
			parameters.put(PARAMETERS.FINAL, fin);
			if (commandType != null && !commandType.isEmpty()){
				cmdSummary = commandType + ";;" + Converters.mapStrStr2Str(parameters);
			}
		}
	}
	/**
	 * Add a dynamic parameter.
	 */
	public void addDynamicParameter(String parameter){
		Set<String> dynamics = new HashSet<>();
		String dynamics_str = getParameter(PARAMETERS.DYNAMIC).replaceAll("^\\[|\\]$", "");
		if (!dynamics_str.isEmpty()){
			for (String p : dynamics_str.split(",")){
				dynamics.add(p);
			}
		}
		dynamics.add(parameter);
		setParameter(PARAMETERS.DYNAMIC, dynamics.toString().replaceAll("\\s+", ""));
	}
	
	/**
	 * Get the command type of the best NLU result 
	 * as a String, e.g. command="weather".
	 * 
	 * @return      command as string
	 */
	public String getCommand(){
		return commandType;
	}
	/**
	 * Set a the command type of the best NLU result, if you need to overwrite it. Use it before set_parameter! 
	 * Refreshes cmd_summary too.
	 */
	public void setCommand(String new_command){
		commandType = new_command;
		if (commandType != null && !commandType.matches("") && parameters != null){
			cmdSummary = commandType + ";;" + Converters.mapStrStr2Str(parameters);
		}
	}
	
	/**
	 * @return		String with all commands identified by NL-Processor
	 */
	public String showAllPossibleCMDs(){
		String all = "";
		for (String c : possibleCMDs){
			all += c + ";";
		}
		return all;
	}
	
	/**
	 * Return certainty level of best command. How good and useful this is depends on the NLP implementation.
	 * 
	 * @return a number between 0 and 1 somehow calculated by the NLP during command interpretation.
	 */
	public double getCertaintyLevel() {
		return certaintyLvl;
	}
}
