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
import net.b07z.sepia.server.core.tools.Is;
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
	
	//custom interpreter data
	public JSONObject customData = null;		//variable to carry any specific 'InterpretationStep' data that does not fit to the predefined stuff.
	
	//all NLP results are stored here:
	List<String> possibleCMDs = new ArrayList<String>();			//make a list of possible interpretations (commands) of the text
	List<Map<String, String>> possibleParameters = new ArrayList<>();		//possible parameters of these commands
	List<Integer> possibleScore = new ArrayList<Integer>();		//save scores to decide which one is correct/best command
	
	/**
	 * Use this constructor only if you know what you are doing! ^^.
	 * The default one is NluResult(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex); 
	 */
	public NluResult(){
	}
	/**
	 * Use this constructor only if you know what you are doing! ^^.
	 * The default one is NluResult(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex); 
	 * This one sets the input, but usually this should be done by the NL-Processor (interpreter)
	 */
	public NluResult(NluInput input){
		setInput(input);
	}
	/**
	 * The default constructor for NluResult. It will automatically set bestCommand and bestCommand_parameters so please use this 
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
	 * Returns the best (highest scoring) result of the NLU interpreter as a JSON string object.
	 * @return best result as JSON object in camel-case format
	 * @see JSONObject
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getBestResultJSON(){
		JSONObject obj = new JSONObject();
		if (foundResult && !commandType.matches(CMD.NO_RESULT)) {
			obj.put("result", "success");
			obj.put("command", commandType);
			obj.put("certainty", new Double(certaintyLvl));
			obj.put("bestDirectMatch", bestDirectMatch);
			JSONObject params = Converters.mapStrStr2Json(parameters);
			/*for (String e : parameters){
				params.put(e.replaceFirst("=.*", "").trim(), e.replaceFirst(".*?=", "").trim()); 
			}*/
			obj.put("parameters", params);
		}else{
			obj.put("result", "fail");
			obj.put("error", "no match found");
			obj.put("answer", "sorry, but I totally didn't understand that! :-(");
		}
		obj.put("language", language);
		obj.put("context", context);
		obj.put("environment", environment);
		obj.put("mood", new Integer(mood));
		if (Is.notNullOrEmpty(normalizedText)){
			obj.put("normalizedText", normalizedText);
		}
		if (Is.notNullOrEmpty(customData)){
			obj.put("customData", customData);
		}
		return obj;
	}
	/**
	 * Returns basic data of the instance as JSON object in snake-case format so it can be used with e.g. the Python-bridge API.<br>
	 * NOTE: Parameter names are not guaranteed to be snake-case :-/
	 * @return basic data as JSON object in snake-case format
	 * @see JSONObject
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getJsonForWebApi(){
		JSONObject obj = new JSONObject();
		if (foundResult && !commandType.matches(CMD.NO_RESULT)){
			obj.put("result", "success");
			obj.put("command", commandType);
			obj.put("certainty", new Double(certaintyLvl));
			obj.put("best_direct_match", bestDirectMatch);
			obj.put("parameters", Converters.mapStrStr2Json(parameters));	//note: parameter names are not guaranteed to be snake-case :-/
		}else{
			obj.put("result", "fail");
			obj.put("error", "no match found");
		}
		obj.put("language", language);
		obj.put("context", context);
		obj.put("environment", environment);
		obj.put("mood", new Integer(mood));
		if (Is.notNullOrEmpty(normalizedText)){
			obj.put("normalized_text", normalizedText);
		}
		if (Is.notNullOrEmpty(customData)){
			obj.put("custom_data", customData);
		}
		return obj;
	}
	/**
	 * Import a result received as JSON object in camel-case format, e.g. from internal API.<br>
	 * Works best with 'input' constructor: NluResult(NluInput input)
	 * @param jsonData in camel-case format
	 */
	public void importJson(JSONObject jsonData){
		String res = JSON.getStringOrDefault(jsonData, "result", "fail");
		JSONObject paramsJson = null;
		this.foundResult = (res.equals("success")? true : false);
		if (this.foundResult){
			this.commandType = JSON.getString(jsonData, "command");
			this.certaintyLvl = Converters.obj2DoubleOrDefault(jsonData.get("certainty"), 0.0d);
			this.bestDirectMatch = JSON.getStringOrDefault(jsonData, "bestDirectMatch", "---");
		}
		if (jsonData.containsKey("normalizedText")) this.normalizedText = JSON.getString(jsonData, "normalizedText");
		if (jsonData.containsKey("customData")) this.customData = JSON.getJObject(jsonData, "customData");
		if (jsonData.containsKey("parameters")){
			paramsJson = JSON.getJObject(jsonData, "parameters");
			this.parameters = Converters.json2HashMapStrStr(paramsJson);
		}
		if (Is.notNullOrEmpty(this.commandType) && Is.notNullOrEmptyMap(this.parameters)){
			this.cmdSummary = Converters.makeCommandSummary(this.commandType, paramsJson);
		}
		//TODO: add this? modify input?
		if (jsonData.containsKey("language")) this.language = JSON.getString(jsonData, "language");
		if (jsonData.containsKey("context")) this.context = JSON.getString(jsonData, "context");
		if (jsonData.containsKey("environment")) this.environment = JSON.getString(jsonData, "environment");
		if (jsonData.containsKey("mood")) this.mood = JSON.getIntegerOrDefault(jsonData, "mood", 5);
	}
	/**
	 * Import a result received as JSON object in snake-case format, e.g. from web-API.
	 * This version fits better when using e.g. the Python-bridge API.<br>
	 * Works best with 'input' constructor: NluResult(NluInput input)
	 * @param jsonData in snake-case format
	 */
	public void importJsonFromWebApi(JSONObject jsonData){
		String res = JSON.getStringOrDefault(jsonData, "result", "fail");
		JSONObject paramsJson = null;
		this.foundResult = (res.equals("success")? true : false);
		if (this.foundResult){
			this.commandType = JSON.getString(jsonData, "command");
			this.certaintyLvl = Converters.obj2DoubleOrDefault(jsonData.get("certainty"), 0.0d);
			this.bestDirectMatch = JSON.getStringOrDefault(jsonData, "best_direct_match", "---");
		}
		if (jsonData.containsKey("normalized_text")) this.normalizedText = JSON.getString(jsonData, "normalized_text");
		if (jsonData.containsKey("custom_data")) this.customData = JSON.getJObject(jsonData, "custom_data");
		if (jsonData.containsKey("parameters")){
			paramsJson = JSON.getJObject(jsonData, "parameters");
			this.parameters = Converters.json2HashMapStrStr(paramsJson);
		}
		if (Is.notNullOrEmpty(this.commandType) && Is.notNullOrEmptyMap(this.parameters)){
			this.cmdSummary = Converters.makeCommandSummary(this.commandType, paramsJson);
		}
		//TODO: add this? modify input?
		if (jsonData.containsKey("language")) this.language = JSON.getString(jsonData, "language");
		if (jsonData.containsKey("context")) this.context = JSON.getString(jsonData, "context");
		if (jsonData.containsKey("environment")) this.environment = JSON.getString(jsonData, "environment");
		if (jsonData.containsKey("mood")) this.mood = JSON.getIntegerOrDefault(jsonData, "mood", 5);
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
	 * A dynamic parameter is created on-the-fly inside a service but will be included in interview build scripts.
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
	
	/**
	 * Get custom NLU data create by the {@link InterpretationStep}.
	 * @return JSONObject or null
	 */
	public JSONObject getCustomNluData(){
		return this.customData;
	}
	
	//------ export / import ------
	
	/**
	 * Transforms a cmd_summary back to a {@link NluResult} adding the {@link NluInput} and using it to restore the default
	 * values for context, mood, environment etc.
	 * @param input - {@link NluInput} with all the settings (language, environment, mood, etc...)
	 * @param cmd_sum - cmd_summary to be transformed back
	 * @return {@link NluResult}
	 */
	public static NluResult cmdSummaryToResult(NluInput input, String cmd_sum){
		NluResult result = NluResult.cmdSummaryToResult(cmd_sum);
		result.setInput(input);
		return result;
	}
	/**
	 * Transforms a cmd_summary back to a {@link NluResult}.<br>
	 * Note: If you don't supply the {@link NluInput} you have to add it to the result later with result.setInput(NluInput input) or by adding all important stuff manually!
	 * @param cmd_sum - cmd_summary to be transformed back
	 * @return {@link NluResult}
	 */
	public static NluResult cmdSummaryToResult(String cmd_sum){
		//initialize
		List<String> possibleCMDs = new ArrayList<>();
		List<Map<String, String>> possibleParameters = new ArrayList<>();
		List<Integer> possibleScore = new ArrayList<>();
		int bestScoreIndex = 0;
		
		//split string - Compare to: Converters.getParametersFromCommandSummary (TODO: use?)
		String cmd;
		String params;
		if (cmd_sum.trim().matches(".*?;;.+")){
			String[] parts = cmd_sum.split(";;", 2);		//TODO: change this whole ";;" structure to JSON?
			cmd = parts[0].trim();
			params = parts[1].trim();
		}else{
			cmd = cmd_sum.replaceFirst(";;$", "").trim();
			params = "";
		}
		
		//construct result
		possibleCMDs.add(cmd);
		possibleScore.add(1);
		Map<String, String> kv = new HashMap<>();
		for (String p : params.split(";;")){				//TODO: change this whole ";;" structure to JSON?
			String[] e = p.split("=", 2);
			if (e.length == 2){
				String key = e[0].trim();
				String value = e[1].trim();
				kv.put(key, value);
			}
		}
		possibleParameters.add(kv);
		NluResult result = new NluResult(possibleCMDs, possibleParameters, possibleScore, bestScoreIndex);
		result.certaintyLvl = 1.0d;
		
		//TODO: missing:	input parameters parsed to result (language, context, environment, etc. ...)
		
		return result;
	}
}
