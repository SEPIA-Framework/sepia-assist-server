package net.b07z.sepia.server.assist.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.answers.AnswerTools;
import net.b07z.sepia.server.assist.answers.Answers;
import net.b07z.sepia.server.assist.assistant.ActionBuilder;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.data.Card.ElementType;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * The Open_CustomLink class handles commands that are supposed to open browser links with parameters.
 * If these parameters are given as a parameter_set than this class just constructs the proper link and sends it as action to the client.
 * If they are not given it uses the question_set to ask for the parameters. Questions that are not available will be replaced by a default
 * question. Answers can be given in an answer_set. In case there is more than one answer (separated by "||") a random answer will be chosen.<br>
 * NOTE: this class does not implement the services interface.
 * 
 * @author Florian Quirin
 *
 */
public class OpenCustomLink {
	
	/**
	 * The default method to create a service result handling all the question/answer/action construction. 
	 * @param NluResult - typically this is given by a direct command
	 * @return service result
	 */
	public static ServiceResult get(NluResult nluResult){
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult);
		
		//get parameters
		String url = nluResult.getParameter("url");							//the url to call (can include wildcards in the form ***)
		
		String parameter_set = nluResult.getParameter("parameter_set");		//the parameters filling the wildcards connected by "&&"
		String question_set = nluResult.getParameter("question_set");			//a set of questions to the wildcards connected by "&&"
		String answer_set = nluResult.getParameter("answer_set");				//a set of answers to complete the command, if there is more than one (separated by "||") a random one will be chosen
		
		String title = nluResult.getParameter("title");					//title of link-card
		if (title.isEmpty()) title = "Link";
		
		String description = nluResult.getParameter("description");		//description of link-card
		if (description.isEmpty()) description = ActionBuilder.getDefaultButtonText(api.language);
		
		String iconUrl = nluResult.getParameter("icon_url");				//icon URL to be used for link-card
		if (iconUrl.isEmpty())	iconUrl = Config.urlWebImages + "/cards/link.png";
		
		//handle parameters/answers/questions
		//p
		String[] params;
		int nbr_of_openparams = 0;
		if (!parameter_set.matches("")){
			params = parameter_set.split("&&");
			nbr_of_openparams = NluTools.countOccurrenceOf(parameter_set, "***");
		}else{
			//check url for open parameters to see if we need to create a set of open parameters (in case the user did not set them)
			nbr_of_openparams = NluTools.countOccurrenceOf(url, "***");
			if (nbr_of_openparams>0){
				params = new String[nbr_of_openparams];
				for (int i=0; i<nbr_of_openparams; i++){
					params[i] = "***";
					parameter_set = parameter_set.trim() + "&&***";
				}
				//feed back parameter_set to NLU_result
				parameter_set = parameter_set.replaceFirst("^&&", "").trim();
				nluResult.setParameter("parameter_set", parameter_set);
			}else{
				params = new String[]{""};
			}
		}
		//a
		if (answer_set.matches("")){
			answer_set = "<default_open_link_1a>";		//if there is no parameter for answers set the default open_link answer
		}
		//q
		String[] questions;
		if (!question_set.matches("")){
			questions = question_set.split("&&");		//this might still fail if the user does not specify enough questions! Then it falls back to the set of default questions. 
		}else{
			questions = new String[nbr_of_openparams];				//if there is no parameter for questions set the default parameter question for all open parameters
			for (int i=0; i<nbr_of_openparams; i++){
				questions[i] = "<default_ask_parameter_0a>";
				question_set = question_set.trim() + "&&<default_ask_parameter_0a>";
			}		
			//feed back question_set to NLU_result
			question_set = question_set.replaceFirst("^&&", "").trim();
			nluResult.setParameter("question_set", question_set);
		}
		//check if number of questions is equal to number of missing parameters
		if ((nbr_of_openparams > 0) && (nbr_of_openparams <= questions.length)){
			String question_key = "";
			//ASK
			for (int i=0; i<params.length; i++){
				if (params[i].contains("***")){
					//check for direct answer or database link
					question_key = AnswerTools.handleUserAnswerSets(questions[i]);
					return AskClient.question(question_key, "parameter_set", nluResult);
				}
			}
			
		//not enough questions for open parameters
		}else if (nbr_of_openparams > 0){
			//ASK with default question (default_ask_parameter_0a)
			for (int i=0; i<params.length; i++){
				if (params[i].matches("\\*\\*\\*")){
					return AskClient.question("default_ask_parameter_0a", "parameter_set", nluResult);
				}
			}
		}
		
		//make an object array for answers vararg
		Object[] params_obj = new Object[params.length];
		for (int i=0; i<params.length; i++){
			params_obj[i] = params[i];
		}
		
		//make answer - if more than one direct answer choose randomly
		String answer_key = AnswerTools.handleUserAnswerSets(answer_set);		//check for direct answer or database link and handle random selection
		api.answer = Answers.getAnswerString(nluResult, answer_key, params_obj);
		api.answerClean = Converters.removeHTML(api.answer);
		
		//make action: browser url call
		String callURL = url;
		try {
			for (String p : params){
				callURL = callURL.replaceFirst("\\*\\*\\*", URLEncoder.encode(p, "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			callURL = "";
			//e.printStackTrace();
		}
		api.addAction(ACTIONS.OPEN_URL);
		api.putActionInfo("url", callURL);
		/*
		api.actionInfo_add_action(ACTIONS.BUTTON_URL);
		api.actionInfo_put_info("url", callURL);
		api.actionInfo_put_info("title", ACTIONS.getDefaultButtonText(api.language));
		*/
		
		//card
		Card card = new Card(Card.TYPE_SINGLE);
		JSONObject linkCard = card.addElement(ElementType.link, 
				JSON.make("title", title, "desc", description),
				null, null, "", 
				callURL, 
				iconUrl, 
				null, null);
		JSON.put(linkCard, "imageBackground", "transparent");	//use any CSS background option you wish
		api.addCard(card.getJSON());
		
		api.hasAction = true;
		
		//build card
		/*
		JSONObject element = new JSONObject();
		if (!link_info.matches("")){
			for (String p : params){
				link_info = link_info.replaceFirst("<\\d+>", p);
			}							element.put("text", "<b>info:</b><br><br>" + link_info);				}
		else{							element.put("text", "<b>result:</b><br><br>" + api.answer);				}
		element.put("url", call_url);
		if (!link_ico.matches("")){		element.put("image", link_ico);											}
		else{							element.put("image", Config.url_web_images + "icons/mixed/" + "link-logo.png");	}
		//add it
		api.cardInfo.add(element);
		api.hasCard = false;
		*/

		api.status = "success";
		
		//anything else?
		//...api.more
		//...api.context ?
		
		//finally build the API_Result
		ServiceResult result = api.buildResult();
		
		//return result_JSON.toJSONString();
		return result;
	}
}
