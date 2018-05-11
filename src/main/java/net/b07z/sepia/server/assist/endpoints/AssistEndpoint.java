package net.b07z.sepia.server.assist.endpoints;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.apis.Open_CustomLink;
import net.b07z.sepia.server.assist.database.CollectStuff;
import net.b07z.sepia.server.assist.events.EventsManager;
import net.b07z.sepia.server.assist.interpreters.InterpretationChain;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.Abort;
import net.b07z.sepia.server.assist.interviews.AbstractInterview;
import net.b07z.sepia.server.assist.interviews.InterviewResult;
import net.b07z.sepia.server.assist.interviews.InterviewInterface;
import net.b07z.sepia.server.assist.interviews.NoResult;
import net.b07z.sepia.server.assist.interviews.Repeat_Last;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
import spark.Request;
import spark.Response;

/**
 * API endpoint to handle interpretation and answering of user requests and manage user specific events.
 * 
 * @author Florian Quirin
 *
 */
public class AssistEndpoint {

	/**---INTERPRETER API---<br>
	 * End-point that interprets a user text input searching for commands and parameters and returns a JSON object describing the best result.
	 */
	public static String interpreterAPI(Request request, Response response) {
		
		Statistics.add_NLP_hit();					//hit counter
		long tic = System.currentTimeMillis();
		
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request);
		
		//authenticate
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}
		
		Statistics.add_NLP_hit_authenticated();		//authorized hit counter
		Statistics.save_Auth_total_time(tic);		//time it took
				
		tic = System.currentTimeMillis();			//track interpreter time
		
		//get input
		NluInput input = AssistEndpoint.getInput(params);
		
		//create user
		input.user = new User(input, token);
		
		//decide how to proceed - interpret input
		InterpretationChain nluChain = new InterpretationChain()
				.setSteps(Config.nluInterpretationSteps);
		NluResult result = nluChain.getResult(input);
		
		Statistics.save_NLP_total_time(tic);		//store NLP time
		
		//write basic statistics for user
		input.user.saveStatistics();
		
		//return answer in requested format
		String msg = result.getBestResultJSON().toJSONString();
		return SparkJavaFw.returnResult(request, response, msg, 200);
	}

	/**---ANSWER API---<br>
	 * End-point that interprets the input text and tries to find a service to answer the request.
	 */
	public static String answerAPI(Request request, Response response) {
		
		Statistics.add_API_hit();					//hit counter
		long tic = System.currentTimeMillis();
		
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request);
		
		//authenticate
		Authenticator token = Start.authenticate(params, request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}
		
		Statistics.add_API_hit_authenticated();		//authorized hit counter
		Statistics.save_Auth_total_time(tic);		//time it took
		
		tic = System.currentTimeMillis();			//track interpreter time
		
		//get input
		NluInput input = AssistEndpoint.getInput(params);
		
		//create user
		input.user = new User(input, token);
		
		//decide how to proceed - interpret input
		InterpretationChain nluChain = new InterpretationChain()
				.setSteps(Config.nluInterpretationSteps);
		NluResult result = nluChain.getResult(input);
		
		Statistics.save_NLP_total_time(tic);		//store NLP time and prepare API time
		tic = System.currentTimeMillis();			//track answer time
		
		//choose API and get the result of the API as JSON String
		ApiResult answer;
		String cmd = result.getCommand();
		if (!cmd.equals(CMD.NO_RESULT)){
			Debugger.println("USER " + input.user.getUserID() + " used SERVICE " + cmd + " - TS: " + System.currentTimeMillis() + " - LANGUAGE: " + input.language + " - CLIENT: " + input.client_info + " - API: " + Config.apiVersion, 3);
		}
		
		//check if user wants repetition of old command
		String no_res_fallback_key = "no_answer_0a";		//answer key for custom_no_result answer
		if (cmd.matches(CMD.REPEAT)){
			//we handle that with a "quasi"-API
			NluResult new_result = Repeat_Last.reload(result);
			if (new_result == null){
				cmd = "custom_no_result";
				no_res_fallback_key = "repeat_0a";
			}else{
				cmd = new_result.getCommand();
				result = new_result;
			}
		}
		
		//TODO: add a check if the user is allowed to use API xy
		
		//interview module with services
		ArrayList<ApiInterface> services = ConfigServices.getCustomOrSystemServices(input, input.user, cmd);
		if (!services.isEmpty()){
			InterviewInterface interview = new AbstractInterview();
			interview.setCommand(cmd);
			interview.setServices(services);
			InterviewResult iResult = interview.getMissingParameters(result);
			if (iResult.isComplete()){
				answer = interview.getServiceResults(iResult);
			}else{
				answer = iResult.getApiComment();
			}
		
			/* ???
			ConfigServices.load_command_map(); 			//plug-ins to commands
			API_Interface plugin = ConfigServices.command_map.get(result.get_command());
			if (plugin != null){
				answer = plugin.getResult(result);
			*/
		
		//abort
		} else if (cmd.matches(CMD.ABORT)){
			answer = Abort.get(result);
			
		//Open Link
		} else if (cmd.matches(CMD.OPEN_LINK)){
			answer = Open_CustomLink.get(result);
			
		//Custom no result
		} else if (cmd.matches("custom_no_result")){
			answer = NoResult.get(result, no_res_fallback_key);
			
		//Demo Commands
		/*
		} else if (cmd.matches("demo_cmds")){
			answer = Demo.get(result);
		*/
			
		//no API available - or other reason for No_Result
		}else{
			answer = NoResult.get(result);
		}
		
		Statistics.save_API_total_time(tic);			//store API call time
		
		CollectStuff.saveAsync(result); 				//store useful stuff to build up database and corpi 
		
		//different formats ... only JSON right now
		String answer_JSON = answer.getResultJSON();
		
		//write basic statistics for user
		input.user.saveStatistics();
				
		//return answer in requested format
		//System.out.println(answer_JSON); 		//DEBUG
		return SparkJavaFw.returnResult(request, response, answer_JSON, 200);
	}

	/**-- EVENTS --<br>
	 * End-point that returns data of the events manager.
	 */
	public static String events(Request request, Response response){
		long tic = System.currentTimeMillis();
		
		//authenticate
		Authenticator token = Start.authenticate(request);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(request, response, token.getErrorCode());
		}
		
		//prepare parameters
		RequestParameters params = new RequestGetOrFormParameters(request);
		
		//get input
		NluInput input = AssistEndpoint.getInput(params);
		
		//create user
		input.user = new User(input, token);
		
		//build common events that are valid for all users (maybe depending on location and time)
		JSONObject ans = EventsManager.buildCommonEvents(input);
		
		//can add user account specific events here
		
		//stats
		Statistics.addOtherApiHit("Events endpoint");
		Statistics.addOtherApiTime("Events endpoint", tic);
		
		return SparkJavaFw.returnResult(request, response, ans.toJSONString(), 200);
	}

	/**
	 * Creates the NLU_input for the NL-Processor out of the parameters passed to the API.<br>
	 * The more parameters are send by the client the more personal the AI can react. See NLU_Input for parameter descriptions.<br>
	 * Note: an option for the future is to store parts of these parameters in the user account!
	 * @param params
	 * @return
	 */
	public static NluInput getInput(RequestParameters params){
		//get parameters
		//-defaults:
		String text = params.getString("text");
		//String text = request.params(":text");
		String language = params.getString("lang");
		String context = params.getString("context");
		String mood_str = params.getString("mood");
		int mood = -1;
		String env = params.getString("env");
		String time_str = params.getString("time"); 				//system time - time stamp
		String time_local = params.getString("time_local");		//local time date
		String client_info = params.getString("client");
		long time = -1;
		//-answer params:
		String last_cmd = params.getString("last_cmd");
		int last_cmd_N = 0;			String last_cmd_N_str = params.getString("last_cmd_N");
		String input_type = params.getString("input_type");
		String input_miss = params.getString("input_miss");
		int dialog_stage = 0;		String dialog_stage_str = params.getString("dialog_stage");
		//personal info
		String user_location = params.getString("user_location");
		String demomode = params.getString("demomode");
		//-tokens:
		//tokens are isolated inside authenticators!
		
		//build
		NluInput input = new NluInput(text);
		if (language!=null)			input.language = language;
		if (context!=null)			input.context = context;
		if (client_info!=null)		input.client_info = client_info;
		if (env!=null)				input.environment = env; 		else 	input.environment = "all"; //input.client_info.replaceFirst("_v\\d.*", "").trim();
		if (user_location!=null)	input.user_location = user_location;
		if (time_local!=null)		input.user_time_local = time_local;
		//
		if (mood_str!=null){
			try {
				mood = Integer.parseInt(mood_str);				input.mood = mood;
			}catch (Exception e){
				input.mood = -1;								e.printStackTrace();
			}
		}
		//System.out.println("l:"+ language +", c:"+ context +", m:"+ mood +", e:"+ env);		//debug
		if (time_str!=null){
			try {
				time = Long.parseLong(time_str);				input.user_time = time;
			}catch (Exception e){
				input.user_time = -1;							e.printStackTrace();
			}
		}
		if (last_cmd!=null)	input.last_cmd = last_cmd;
		if (last_cmd_N_str!=null){
			try {
				last_cmd_N = Integer.parseInt(last_cmd_N_str);	input.last_cmd_N = last_cmd_N;
			}catch (Exception e){
				input.last_cmd_N = 0;							e.printStackTrace();
			}
		}
		if (input_type!=null)	input.input_type = input_type;
		if (input_miss!=null)	input.input_miss = input_miss;
		if (dialog_stage_str!=null){
			try {
				dialog_stage = Integer.parseInt(dialog_stage_str);		input.dialog_stage = dialog_stage;
			}catch (Exception e){
				input.dialog_stage = 0;									e.printStackTrace();
			}
		}
		
		if (demomode!=null && demomode.matches("x16y42")){
			input.demo_mode = true;
		}
		
		//System.out.println("in user_location: " + input.user_location);
		//System.out.println("in user_time_local: " + input.user_time_local);
		//System.out.println("client: " + input.client_info);
		//System.out.println("env. submit: " + env + ", saved: " + input.environment);
		return input;
	}

}
