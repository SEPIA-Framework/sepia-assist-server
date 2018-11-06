package net.b07z.sepia.server.assist.data;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.parameters.ParameterConfig;
import net.b07z.sepia.server.assist.parameters.ParameterHandler;
import net.b07z.sepia.server.core.tools.ClassBuilder;

/**
 * This class represents a parameter with some additional info than just the name.
 * 
 * @author Florian Quirin
 *
 */
public class Parameter {
	
	/**
	 * A number of default values. These values are usually put into brackets &lt...&gt when used.
	 */
	public enum Defaults {
		on, off, toggle,
		start, stop, pause, 
		set, increase, decrease,
		add, remove, show,
		user_home, user_work, user_location,
		user_time,
		user_account 		//this can be used to tell the build method of various parameters to get the value from the user account data (might get restricted) 
	}
	
	private String name = "";					//as seen in PARAMETERS
	private String handlerName = "";			//you can overwrite the parameter handler to make this parameter behave like any other 
	private boolean required = false;			//is definitely required?
	private String input = "";					//input given by the user before validation and transformation to default data format
	private JSONObject data = new JSONObject();	//data in default JSON structure
	private Object defaultValue = "";			//value that is used when the parameter is not given
	private String question = "";				//link to a pool of questions (recommended) or direct question (not recommended) to ask for this parameter
	private String questionFailAnswer = "";		//link to a pool of answers (recommended) or direct answer (not recommended) triggered when repeated answer fails
	//TODO: add "free", "individual" parameter and "select"
	
	/**
	 * Create parameter with name given in PARAMETERS.
	 */
	public Parameter(String name){
		setName(name);
	}
	/**
	 * Create parameter with name given in PARAMETERS and JSON data object.
	 */
	public Parameter(String name, JSONObject data){
		setName(name);
		setData(data);
	}
	/**
	 * Create parameter with name given in PARAMETERS and tell that it is required.
	 */
	public Parameter(String name, boolean isRequired){
		setName(name);
		setRequired(isRequired);
	}
	/**
	 * Create parameter with name given in PARAMETERS and assign a default value (as Object).<br>
	 * NOTE: the default value will be set BEFORE the build() method of the ParameterHandler is executed and is NOT put between brackets (see constructor with enum). 
	 */
	public Parameter(String name, Object defaultValue){
		setName(name);
		setDefaultValue(defaultValue);
	}
	/**
	 * Create parameter with name given in PARAMETERS and assign a default value (.name() of any enum).<br>
	 * Note: enumerator values are put between brackets &lt...&gt automatically and will be set BEFORE the build() method.
	 */
	public Parameter(String name, Enum<?> defaultValue){
		setName(name);
		setDefaultValue("<" + defaultValue.name() + ">");
	}
	
	/**
	 * Get name as seen in PARAMETERS.
	 */
	public String getName(){
		return name;
	}
	/**
	 * Set name as seen in PARAMETERS.
	 */
	public Parameter setName(String name){
		this.name = name;
		return this;
	}
	
	/**
	 * Overwrite the default handler by defining any other parameter's handler. This way a parameter can behave like any other parameter while keeping his name. 
	 * @param handlerName - name of another parameter.
	 */
	public Parameter setHandler(String handlerName){
		this.handlerName = handlerName;
		return this;
	}
	
	/**
	 * Get the handler for this parameter.
	 */
	public ParameterHandler getHandler(){
		if (handlerName.isEmpty()){
			return (ParameterHandler) ClassBuilder.construct(ParameterConfig.getHandler(name));
		}else{
			return (ParameterHandler) ClassBuilder.construct(ParameterConfig.getHandler(handlerName));
		}
	}
	
	/**
	 * Check if this parameter is generic. A generic parameter is a parameter that has no handler and thus only returns the exact input given.
	 */
	public boolean isGeneric(){
		return getHandler().isGeneric();
	}
	
	/**
	 * Is this parameter definitely required?
	 */
	public boolean isRequired(){
		return required;
	}
	/**
	 * Make this a required parameter. 
	 */
	public Parameter setRequired(boolean isRequired){
		this.required = isRequired;
		/*
		if (isRequired){
			this.requiredChoice = false;
		}
		*/
		return this;
	}
	/**
	 * Is this parameter part of a list of choices were one must be given?
	 */
	/*
	public boolean isRequiredChoice(){
		return requiredChoice;
	}
	*/
	/**
	 * Make this a parameter one that is part of a list of choices were one parameter needs to be given.
	 * Note: add the parameter to API_Info by using "...".  
	 */
	/*
	public Parameter setRequiredChoice(boolean isChoice){
		this.requiredChoice = isChoice;
		if (isChoice){
			this.required = false;
		}
		return this;
	}
	*/
	
	/**
	 * Set input of parameter.
	 */
	public Parameter setInput(String input){
		this.input = input;
		return this;
	}
	/**
	 * Get input given by the user before validation and transformation to default data format. Can be empty as the field "input" is not guaranteed.
	 */
	public String getInput(){
		return input;
	}
	/**
	 * Does the parameter contain an input string?
	 */
	public boolean isInputEmpty(){
		return input.isEmpty();
	}
	
	/**
	 * Set data of parameter.
	 */
	public Parameter setData(JSONObject data){
		this.data = data;
		if (data.containsKey(InterviewData.INPUT)){
			this.input = data.get(InterviewData.INPUT).toString();
		}
		return this;
	}
	/**
	 * Get data of parameter.
	 */
	public JSONObject getData(){
		return data;
	}
	/**
	 * Since this is used so often ... get data.value and make type conversion to String.<br>
	 * Same as "(String) getData().get(InterviewData.VALUE)" except it checks for null and returns empty string in that case.
	 */
	public String getValueAsString(){
		String val = (String) data.get(InterviewData.VALUE);
		return (val == null)? "" : val;
	}
	/**
	 * Does the parameter contain data (in final format, not input string)?
	 */
	public boolean isDataEmpty(){
		return (data == null)? true : data.isEmpty();
	}
	/**
	 * If there is a default value given you get it with this method. Always returns a string!
	 */
	public Object getDefaultValue(){
		return defaultValue;
	}
	/**
	 * Set a default value (from enumerator) that is used when no other value could be extracted from input.
	 * Enumerator values are put between brackets &lt...&gt automatically.
	 */
	public Parameter setDefaultValue(Defaults value){
		this.defaultValue = "<" + value.name() + ">";
		return this;
	}
	/**
	 * Set a default value (unknown) that is used when no other value could be extracted from input. <br>
	 * Note: compared to the version using an enumerator this value is NOT put between brackets &lt...&gt automatically. 
	 */
	public Parameter setDefaultValue(Object value){
		this.defaultValue = value; 
		return this;
	}
	/**
	 * Get data of field (key) in JSON data or defaultValue. Particularly (only?) useful for optional parameters.<br>
	 * Note: The same default value will be applied to ALL keys.
	 * @param key - field inside the JSON data 
	 */
	public Object getDataFieldOrDefault(String key){
		if (data != null && data.containsKey(key)){
			return data.get(key);
		}else{
			return defaultValue;
		}
	}
	
	/**
	 * Define a question for this parameter (pool or direct question) like "test_0a" or "&ltdirect&gtwhat is this?". 
	 */
	public Parameter setQuestion(String question){
		this.question = question;
		return this;
	}
	/**
	 * Get question to this parameter.
	 */
	public String getQuestion(){
		return question;
	}
	/**
	 * Define an answer for this parameter that is triggered on repeated question fail. 
	 */
	public Parameter setQuestionFailAnswer(String answer){
		this.questionFailAnswer = answer;
		return this;
	}
	/**
	 * Get question fail answer to this parameter.
	 */
	public String getQuestionFailAnswer(){
		return questionFailAnswer;
	}
}
