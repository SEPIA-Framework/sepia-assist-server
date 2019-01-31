package net.b07z.sepia.server.assist.interviews;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class that holds info about an interview module. Info can be defined inside interview module or taken from first service.
 * It is to some degree a copy of {@link ServiceInfo} which has historical and compatibility reasons, but should be fixed at some point. 
 * 
 * @author Florian Quirin
 *
 */
public class InterviewInfo {
	
	//meta info
	private String cmd = ""; 		//what is the command that triggers this interview
	
	//data
	public List<Parameter> requiredParameters; 		//parameters that are required to run the service
	public List<Parameter> optionalParameters;		//parameters that are optional and get replaced by default or ignored if not set
	public List<List<Parameter>> listOfRequiredChoices;	//list of list with "one of these" parameters
	public List<String> listOfChoiceQuestions;					//list with the questions to the required choices
	private List<ServiceInterface> services; 			//services used
	private Map<String, String> customAnswerMap;	//use this to add answers to your service
	private List<String> answerParameters;			//list of parameters that are used to build the answer. The order matters!
	
	/**
	 * InterviewInfo can be generated out of a command and an API_Info of a service module.
	 */
	public InterviewInfo(String cmd, ServiceInfo serviceInfo){
		this.cmd = cmd;
		this.requiredParameters = serviceInfo.requiredParameters;
		this.optionalParameters = serviceInfo.optionalParameters;
		this.listOfRequiredChoices = serviceInfo.listOfRequiredChoices;
		this.listOfChoiceQuestions = serviceInfo.listOfChoiceQuestions;
		this.customAnswerMap = serviceInfo.customAnswerMap;
		this.answerParameters = serviceInfo.answerParameters;
	}
	
	/**
	 * Get the command connected to this interview.
	 */
	public String getCommand(){
		return cmd;
	}
	
	/**
	 * Store the services.
	 */
	public InterviewInfo setServices(List<ServiceInterface> services){
		this.services = services;
		return this;
	}
	/**
	 * Get the services.
	 */
	public List<ServiceInterface> getServices(){
		return services;
	}
	
	/**
	 * Get default "success" answer.
	 */
	public String getSuccessAnswer(){
		return customAnswerMap.get(ServiceBuilder.SUCCESS);
	}
	/**
	 * Get default "fail" answer.
	 */
	public String getFailAnswer(){
		return customAnswerMap.get(ServiceBuilder.FAIL);
	}
	/**
	 * Get default "okay" answer. If not exists get "fail" answer.
	 */
	public String getOkayAnswer(){
		String ans = customAnswerMap.get(ServiceBuilder.OKAY);
		if (ans == null || ans.isEmpty()){
			ans = customAnswerMap.get(ServiceBuilder.FAIL);
		}
		return ans;
	}
	/**
	 * Return answer with this tag (name). 
	 * This method is probably never used as the interview usually runs automatically and takes API_Result.getCustomAnswerWorkpiece(). 
	 */
	public String getCustomAnswer(String tag){
		return customAnswerMap.get(tag);
	}
	/**
	 * Get the names of the parameters (keys) their values inside resultInfo should be used for the answer. Order is preserved.
	 */
	public List<String> getAnswerParameters(){
		return answerParameters;
	}
	
	/**
	 * Build JSON out of the info data.
	 */
	public JSONObject getJSON(){
		JSONObject info = new JSONObject();
		
		//command
		JSON.add(info, "command", cmd);
		
		//services
		JSONArray jas = new JSONArray();
		for (ServiceInterface ai : services){
			JSON.add(jas, ai.getClass().getName());
		}
		JSON.add(info, "services", jas);
		JSON.add(info, "numOfServices", services.size());
		
		//parameters
		JSONObject parameters = new JSONObject();
		if (requiredParameters != null && !requiredParameters.isEmpty()){
			JSONArray required = new JSONArray();
			for (Parameter p : requiredParameters){
				JSONObject jo = new JSONObject();
					JSON.add(jo, "name", p.getName());
					JSON.add(jo, "question", p.getQuestion());
					JSON.add(jo, "questionFailAnswer", p.getQuestionFailAnswer());
				JSON.add(required, jo);
			}
			JSON.add(parameters, "required", required);
		}
		if (optionalParameters != null && !optionalParameters.isEmpty()){
			JSONArray optional = new JSONArray();
			for (Parameter p : optionalParameters){
				JSONObject jo = new JSONObject();
					JSON.add(jo, "name", p.getName());
					JSON.add(jo, "question", p.getQuestion());
					JSON.add(jo, "questionFailAnswer", p.getQuestionFailAnswer());
					JSON.add(jo, "defaultValue", p.getDefaultValue());
				JSON.add(optional, jo);
			}
			JSON.add(parameters, "optional", optional);
		}
		if (listOfRequiredChoices != null && !listOfRequiredChoices.isEmpty()){
			JSONArray requiredChoices = new JSONArray();
			for (List<Parameter> l : listOfRequiredChoices){
				JSONArray ja = new JSONArray();
				for (Parameter p : l){
					JSONObject jo = new JSONObject();
						JSON.add(jo, "name", p.getName());
						JSON.add(jo, "question", p.getQuestion());
						JSON.add(jo, "questionFailAnswer", p.getQuestionFailAnswer());
					JSON.add(ja, jo);				
				}
				JSON.add(requiredChoices, ja);
			}
			JSON.add(parameters, "oneOfEach", requiredChoices);
		}
		JSON.add(info, "parameters", parameters);
		
		//answers and answerParameters
		if (customAnswerMap != null && !customAnswerMap.isEmpty()){
			JSONObject customAnswers = Converters.mapStrStr2Json(customAnswerMap);
			JSON.add(info, "answers", customAnswers);
			JSON.add(info, "answerParameters", answerParameters);
		}

		//return
		return info;
	}

}
