package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.assist.data.Parameter;

/**
 * Get some info about the service like the type (link, REST, text, regular expressions ...).
 * 
 * @author Florian Quirin
 *
 */
public class ServiceInfo {
	
	//service integration types - differences are not always clear, e.g. an interface to a local program can be a REST API etc. ...
	public enum Type {
		link,		//creates a link with parameters
		pageMeta,	//tries to read data from a website after calling with prepared link
		database,	//reads data from a database (might be identical to REST)
		account,	//reads data from account
		REST,		//gets data via a RESTful API
		otherAPI,	//gets data via an API that is not closer specified 
		RSS,		//reads data from an RSS feed (can be like "page_meta" or REST)
		plain,		//text only or simple local script
		program,	//gets info from another program that runs on the same machine
		tunnel,	 	//tunnels the input through to another service (can be like REST)
		other,		//types that are hard to specify
		unspecified		//this should be only used for compatibility with old services
	}
	
	//content and data types - defines the representation inside assistant
	public enum Content {
		redirect,		//redirect to a webpage
		data,			//contains the necessary data
		action,			//is basically just an action (but might contain answer data etc.)
		apiInterface,	//if the class is more of an interface to template than actually an API
		unspecified		//this should be only used for compatibility with old services
	}
	/*
	public static final String DATA_URL = "URL";					//URL for redirect
	public static final String DATA_JSON = "JSON";					//JSON data string
	public static final String DATA_HTML = "HTML";					//HTML data block
	public static final String DATA_TEXT = "text";					//plain text
	public static final String DATA_ACTION = "action";				//client action
	*/
	
	//settings
	public boolean worksStandalone = false;		//does this API handle the interview job as well?
	public String serviceType = "";				//type of integration (link, REST, ...)
	public String contentType = "";				//type of content 
	public boolean isRestricted = false;		//is the service restricted to certain locations, webpages, people ..?
	public boolean isPartner = false;			//is the service in a partner program with the assistant?
	
	//custom services
	public boolean makePublic = false;			//release custom service to public
	
	//result properties
	public boolean isUrlFrameCompatible = false;	//in case the service produces a URL, can it be shown inside an iframe?
	
	//data
	public String intendedCommand;				//this service is intended to be used for this command
	public List<Parameter> requiredParameters; 	//parameters that are required to run the service
	public List<Parameter> optionalParameters;		//parameters that are optional and get replaced by default or ignored if not set
	public List<List<Parameter>> listOfRequiredChoices;	//list with "one of these" parameters
	public List<String> listOfChoiceQuestions;					//list with the questions to the required choices
	
	//custom answers
	public Map<String, String> customAnswerMap;		//use this to add answers to your service
	public List<String> answerParameters;			//list of parameters that are used to build the answer. The order matters! - TODO: a Map would be cool! But impossible to change now :-(
	
	//custom trigger sentences and custom regEx - note: not in InterviewInfo (yet?)
	public Map<String, String> customTriggerRegEx = new HashMap<>();
	public int customTriggerRegExScoreBoost = 0;
	public Map<String, List<String>> customTriggerSentences = new HashMap<>();	//a list of sentences that can trigger the service sorted by language.
	
	/**
	 * Build common info object.
	 */
	public ServiceInfo(Type serviceType, Content contentType, boolean worksStandalone){
		this.serviceType = serviceType.name();
		this.contentType = contentType.name();
		this.worksStandalone = worksStandalone;
		//init
		requiredParameters = new ArrayList<>();
		optionalParameters = new ArrayList<>();
		listOfRequiredChoices = new ArrayList<>();		listOfChoiceQuestions = new ArrayList<>();
		customAnswerMap = new HashMap<>();
		answerParameters = new ArrayList<>();
	}
	
	/**
	 * If this is a custom service should it become publicly available?
	 */
	public ServiceInfo makePublic(){
		makePublic = true;
		return this;
	}
	
	/**
	 * Add required or optional parameter. Don't use this to add requiredChoice parameters as they have to be added to the choices list first.
	 */
	public ServiceInfo addParameter(Parameter parameter){
		if(parameter.isRequired()){
			requiredParameters.add(parameter);
		//}else if (parameter.isRequiredChoice()){
		//	throw new RuntimeException(DateTime.getLogDate() + " ERROR - API_Info.java / addParameter() - wrong requirements in: " + parameter.getName());
		}else{
			optionalParameters.add(parameter);
		}
		return this;
	}
	/**
	 * Add a couple of parameters to a "one-of-these-parameters-is-required"-list and add this list to the collection of other choices. Imagine you have 4 Parameters, 
	 * productName, productType, maxShipTime, maxPrice and you need at least (productName or productType) and (maxShipTime or maxPrice) then you can add two lists here (by calling the method 2 times), 
	 * one with (productName, productType) and one with (maxShipTime, maxPrice). The interview-module will automatically ask for the first parameter in each list if both parameters are missing.  
	 * 
	 * @param modifiedQuestion - in case you want to modify the default question. If empty it uses the question assigned to the first parameter or any generic question further down the hierarchy
	 * @param oneOfThese - array of parameters
	 */
	public ServiceInfo getAtLeastOneOf(String modifiedQuestion, Parameter... oneOfThese){
		List<Parameter> oneOfTheseList = new ArrayList<>();
		for (Parameter p : oneOfThese){
			oneOfTheseList.add(p);
		}
		if (!oneOfTheseList.isEmpty()){
			listOfRequiredChoices.add(oneOfTheseList);
			if (modifiedQuestion.isEmpty()) modifiedQuestion = oneOfThese[0].getQuestion();
			listOfChoiceQuestions.add(modifiedQuestion);
		}
		return this;
	}
	
	/**
	 * Get all required and optional parameters, e.g. for the NLU module (so that it knows what to look for and calculate NLU result score).
	 */
	public List<Parameter> getAllParameters(){
		List<Parameter> allPs = new ArrayList<>();
		allPs.addAll(requiredParameters);
		allPs.addAll(optionalParameters);
		return allPs;
	}
	
	/**
	 * Add the default answer for service success.
	 * @param answer - tag or direct
	 */
	public ServiceInfo addSuccessAnswer(String answer){
		customAnswerMap.put(ServiceBuilder.SUCCESS, answer);
		return this;
	}
	/**
	 * Add the default answer for service fail.
	 * @param answer - tag or direct
	 */
	public ServiceInfo addFailAnswer(String answer){
		customAnswerMap.put(ServiceBuilder.FAIL, answer);
		return this;
	}
	/**
	 * Add the default answer for service still ok (everything worked but the result was not as hoped).
	 * @param answer - tag or direct
	 */
	public ServiceInfo addOkayAnswer(String answer){
		customAnswerMap.put(ServiceBuilder.OKAY, answer);
		return this;
	}
	/**
	 * Add a custom answer with a "meaningful" name. This is used to identify answers when requesting the service-info.<br>
	 * This method is only for info purposes, to activate one of these answers anywhere in the service use API.setCustomAnswer(..). 
	 * @param name - name this answer to find it later (don't use "success" and "fail"!)
	 * @param answer - tag or direct
	 */
	public ServiceInfo addCustomAnswer(String name, String answer){
		customAnswerMap.put(name, answer);
		return this;
	}
	/**
	 * Add the answer parameters in the same order as they are used inside the "custom" or "default" answer.
	 * You can add more parameters than actually required if you have answers with different requirements just choose the right
	 * ones in your answer by using the proper references, e.g. "this is &lt1&gt for &lt3&gt" (skipping 2 knowing that it is empty).<br>
	 * Use {@Link API#resultInfoPut(String, Object)} to set the values as soon as you got them. Use empty strings to fill up the ones not needed. 
	 * @param parameters - names of the parameters (keys) their values inside resultInfo should be used for the answer
	 */
	public ServiceInfo addAnswerParameters(String... parameters){
		for (String p : parameters){
			answerParameters.add(p);
		}
		return this;
	}
	/**
	 * Set the command this service is intended for. If the service registers itself as a custom services this will be used. 
	 * @param command - custom or official command
	 */
	public ServiceInfo setIntendedCommand(String command){
		intendedCommand = command;
		return this;
	}
	/**
	 * Set a language dependent custom trigger regular expression for this service. 
	 * @param regX - regular expression that is used to trigger scoring for this service. Score will decide the probability that this service is selected later and depends on parameters too.
	 * @param language - language code
	 */
	public ServiceInfo setCustomTriggerRegX(String regX, String language){
		customTriggerRegEx.put(language, regX);
		return this;
	}
	/**
	 * Custom score boost that can be applied when the regEx is so strong that it should be weighted higher in scoring just for being found in 'text' already (typically 0 or 1)
	 */
	public ServiceInfo setCustomTriggerRegXscoreBoost(int scoreBoost){
		customTriggerRegExScoreBoost = scoreBoost;
		return this;
	}
	/**
	 * Get score boost for regEx custom trigger. Defaults to 0.
	 */
	public int getCustomTriggerRegXscoreBoost() {
		return customTriggerRegExScoreBoost;
	}
	/**
	 * Get the previously stored custom trigger regular expression for a given language.
	 * @param language - ISO code as usual
	 */
	public String getCustomTriggerRegX(String language){
		return customTriggerRegEx.get(language);
	}
	/**
	 * Add a custom trigger sentence to the collection. If the service registers itself as a custom services this will be used.
	 * @param sentence - a sample sentence
	 */
	public ServiceInfo addCustomTriggerSentence(String sentence, String language){
		List<String> list = customTriggerSentences.get(language);
		if (list == null){
			list = new ArrayList<>();
			customTriggerSentences.put(language, list);
		}
		list.add(sentence);
		return this;
	}
}
