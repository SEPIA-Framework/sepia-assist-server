package net.b07z.sepia.server.assist.apis;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.assistant.CmdBuilder;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.AskClient;
import net.b07z.sepia.server.assist.interviews.ConvertResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tools.StringCompare;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.assistant.CLIENTS;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Query Wikipedia Knowledgebase.
 * 
 * @author Florian Quirin
 *
 */
public class Knowledgebase_Wiki implements ApiInterface{
	
	//--- data ---
	public static String getButtonText(String language){
		if (language.equals(LANGUAGES.DE)){
			return "Wikipedia öffnen";
		}else{
			return "Open Wikipedia";
		}
	}

	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.REST, Content.data, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//looking at these parameters: 	search
		//requires at least:			search
		
		//make the external API call and build API_Result
		try {
			String search = NLU_result.getParameter(PARAMETERS.SEARCH);
			//String org_search = search;
			Debugger.println("cmd: Knowledgebase, search: " + search, 2);		//debug
			
			if (search == null || search.isEmpty()){
				return AskClient.question("knowledgeB_3a", PARAMETERS.SEARCH, NLU_result);
			}
			//search = NLU_Tools.capitalizeAll(search);			//this is kind of stupid but Wiki seems to be case sensitive somehow oO
			//search = URLEncoder.encode(search, "UTF-8");
		
			//make the HTTP GET calls to Wikipedia API
			
			//first call is title search
			String url = "https://" + api.language + ".wikipedia.org/w/api.php" + "?action=query&list=search" + "&format=json" + "&srlimit=4" + "&srsearch=" + URLEncoder.encode(search, "UTF-8");
			//System.out.println("wiki url: " + url);			//debug
			JSONObject response = Connectors.httpGET(url);
			//TODO: check Connectors.httpSuccess(response)
			JSONObject query = (JSONObject)response.get("query");
			JSONArray hits = (JSONArray) query.get("search");
			//check for NO SEARCH RESULT
			if (hits.isEmpty()){
				api.answer = Config.answers.getAnswer(NLU_result, "knowledgeB_0a", NLU_result.getParameter("search"));
				api.answerClean = Converters.removeHTML(api.answer);
				api.htmlInfo = "";
				api.hasInfo = false;	api.hasCard = false;	
				
				api.addAction(ACTIONS.BUTTON_CMD);
				api.putActionInfo("title", "Websearch");
				api.putActionInfo("info", "direct_cmd");
				api.putActionInfo("cmd", CmdBuilder.getWebSearch(search));
				api.putActionInfo("options", JSON.make(ACTIONS.SKIP_TTS, true));
				
				api.hasAction = true;	
			
			}else{
				//second is content extraction
				
				//check all results
				ArrayList<String> titles = new ArrayList<String>();
				ArrayList<String> snippets = new ArrayList<String>();
				int best_title_index = 0;
				int best_snippet_index = 0;
				double best_title_sim = 0;
				double best_snippet_sim = 0;
				//get all titles, snippets and best result
				for (int i=0; i<hits.size(); i++){
					String title_i = JSON.getString(hits, i, "title");
					titles.add(title_i);				
					String snippet_i = JSON.getString(hits, i, "snippet");
					snippets.add(snippet_i);
					double title_sim = 0;
					if (NluTools.isAbbreviation(search, title_i)){
						title_sim = 1.0;
					}else{
						title_sim = Math.pow(StringCompare.wordInclusionWithNorm(search, title_i, api.language), 2);
						//System.out.println("WIKI - T1.: " + search);
						//System.out.println("WIKI - T2.: " + title_i);
						//System.out.println("WIKI - Propab.: " + title_sim);
					}
					if (title_sim > best_title_sim){
						best_title_sim = title_sim;
						best_title_index = i;
					}
					double snippet_sim = StringCompare.wordInclusionWithNorm(search, snippet_i, api.language);
					if (snippet_sim > best_snippet_sim){
						best_snippet_sim = snippet_sim;
						best_snippet_index = i;
					}
					//System.out.println("title: " + title_i + ", title_sim: " + title_sim + ", snippet_sim: " + snippet_sim);		//debug
				}
				//System.out.println("best title: " + titles.get(best_title_index) + ", similarity: " + best_title_sim);			//debug
				//System.out.println("best snippet: " + titles.get(best_snippet_index) + ", similarity: " + best_snippet_sim);	//debug
				
				//assign best result and construct related info
				String base_url = "https://" + api.language + ".wikipedia.org/wiki/";
				//String wikiThumb = "https://upload.wikimedia.org/wikipedia/en/2/28/WikipediaMobileAppLogo.png";
				String relatedInfo = "<b>related:</b><br>";
				String best_title = "";
				//String best_snippet = "";
				
				double hit_threshold = 0.49;
				boolean articleFits;
				if (best_title_sim > hit_threshold){
					//good result - load the article later
					articleFits = true;
					best_title = titles.get(best_title_index);
					search = URLEncoder.encode(best_title.replaceAll("\\s+", "_"), "UTF-8");		//new search title
					//best_snippet = snippets.get(best_title_index);
					titles.remove(best_title_index);
					snippets.remove(best_title_index);
					if (!titles.isEmpty()){
						for (int j=0; j<titles.size(); j++){
							relatedInfo += " - <a href='" + base_url + URLEncoder.encode(titles.get(j).replaceAll("\\s+", "_"), "UTF-8") + "'>" + titles.get(j) + "</a><br>";
						}
					}else{
						relatedInfo = "";
					}
				}else{
					//bad result - just use the best snippet
					articleFits = false;
					best_title = titles.get(best_snippet_index);
					search = URLEncoder.encode(best_title.replaceAll("\\s+", "_"), "UTF-8");		//new search title
					//best_snippet = snippets.get(best_snippet_index);		//new answer
					titles.remove(best_snippet_index);
					snippets.remove(best_snippet_index);
					if (!titles.isEmpty()){
						relatedInfo += "<br>";
						for (int j=0; j<titles.size(); j++){
							relatedInfo += " - <a href='" + base_url + URLEncoder.encode(titles.get(j).replaceAll("\\s+", "_"), "UTF-8") + "'>" + titles.get(j) + "</a><br><br>";
							relatedInfo += "   ..." + snippets.get(j) + "...<br><br>";
						}
					}else{
						relatedInfo = "";
					}
				}
				
				//---make the result for a non-fitting article---
				if (!articleFits){
					/*
					//get relevant sentence(s) from snippet
					String that_sentence = "";
					for (String s : NLU_Tools.splitSentence(Converters.removeHTML(best_snippet))){
						if (StringCompare.wordInclusion(org_search, s) > 0.50){
							that_sentence += s + ". ";
						}
					}
					that_sentence = that_sentence.replaceAll("\\s+", " ").trim();
					if (that_sentence.replaceAll("\\.", "").trim().length() > 3 && NLU_Tools.countOccurrenceOf(that_sentence, ":") < 2){
						//answer - in this API the text IS! the answer so we don't need an get_answer() request.
						api.answer = Config.answers.getAnswer(NLU_result, "knowledgeB_2a", best_title.trim(), that_sentence);
						api.answer_clean = api.answer;
					}else{
						//answer - in this API the text IS! the answer so we don't need an get_answer() request.
						api.answer = Config.answers.getAnswer(NLU_result, "knowledgeB_2b", best_title.trim());
						api.answer_clean = Converters.removeHTML(api.answer);
					}
					
					//html
					String wikiURL = "https://" + api.language + ".wikipedia.org/wiki/" + search;
					if (relatedInfo.isEmpty()){
						api.htmlInfo = "<div><b>Wikipedia Info: <a href='" + wikiURL + "'>" + org_search.toUpperCase() + " / " + best_title + "</a></b><br><br>..." + best_snippet.trim() + "...</div>";
					}else{
						api.htmlInfo = "<div><b>Wikipedia Info: <a href='" + wikiURL + "'>" + org_search.toUpperCase() + " / " + best_title + "</a></b><br><br>..." + best_snippet.trim() + "...<br><br>" + relatedInfo + "</div>";
					}
					api.hasInfo = true;
					
					//card
					Card card = new Card();
					String card_text = "<b>Wikipedia Info: " + org_search.toUpperCase() + " / " + best_title + "</b><br><br>..." + best_snippet.trim() + "...";
					card.addElement(card_text, wikiURL, wikiThumb);
					//add it
					api.cardInfo = card.cardInfo;
					if (api.cardInfo.isEmpty()){
						api.hasCard = false;
					}else{
						api.hasCard = true;
					}
					
					//action
					api.actionInfo_add_action(ACTIONS.OPEN_INFO);
					api.hasAction = true;
					
					*/
					
					/* version B
					api.answer = Config.answers.getAnswer(NLU_result, "knowledgeB_0a", NLU_result.get_parameter("search"));
					api.answer_clean = Converters.removeHTML(api.answer);
					api.htmlInfo = "";
					api.hasInfo = false;	api.hasCard = false;	api.hasAction = false;
					
					api.status = "success"; //kind of
					
					//finally build the API_Result
					API_Result result = api.build_API_result();
					
					//return result_JSON.toJSONString();
					return result;
					*/	
					
					//version C
					return ConvertResult.switchService(CMD.WEB_SEARCH, NLU_result, PARAMETERS.SEARCH, 
							PARAMETERS.WEBSEARCH_REQUEST, NLU_result.getParameter(PARAMETERS.SEARCH));
				}
				//------------------------------------------------
					
				//new url call
				url = "https://" + api.language + ".wikipedia.org/w/api.php" + "?action=query" + "&prop=extracts|pageimages" + "&format=json" + "&pithumbsize=100" + "&exintro=&explaintext=&redirects=&titles=" + search;
				//System.out.println("wiki url: " + url);			//debug
				response = Connectors.httpGET(url);
				//System.out.println(response.toJSONString());		//debug
				
				//isolate result text of article
				query = (JSONObject)response.get("query");
				JSONObject pages = (JSONObject)query.get("pages");
				String firstPage = pages.toJSONString().replaceFirst(".\"(.*?)\".*", "$1");
				//check for result
				String wikiText_clean, wikiText_short_clean, wikiText, wikiTitle, wikiURL, wikiURL_m;
				JSONObject wikiThumb_source;
				String[] splitTxt = null;
				if (!firstPage.matches("-1")){
					
					JSONObject content = (JSONObject)pages.get(firstPage);
					wikiText = (String) content.get("extract");				
					wikiTitle = (String) content.get("title");
					wikiURL = "https://" + api.language + ".wikipedia.org/wiki/" + URLEncoder.encode(wikiTitle.replaceAll("\\s+", "_"), "UTF-8");
					wikiURL_m = "https://" + api.language + ".m" + ".wikipedia.org/wiki/" + URLEncoder.encode(wikiTitle.replaceAll("\\s+", "_"), "UTF-8");
					wikiThumb_source = (JSONObject) content.get("thumbnail");
					if (wikiThumb_source != null){
						//wikiThumb = (String) wikiThumb_source.get("source");		//overwrite default image with article image
					}
					//remove 1 level nested '[' brackets:
					wikiText_clean = wikiText.replaceAll("\\[.*?\\]", "");
					//remove 2 level nested '(' brackets:
					wikiText_clean = wikiText_clean.replaceAll("\\([^(]*?\\)", "");
					wikiText_clean = wikiText_clean.replaceAll("\\([^(]*?\\)", "");
					wikiText_clean = wikiText_clean.replaceAll("\\s+", " ").replaceAll(" \\. ",". ").trim();
					wikiText = wikiText.replaceAll("\\n", "<br><br>");
					//System.out.println(wikiText);		//debug
					
					//check if it is a "real" answer or a multiple-choice - DEPENDS ON LANGUAGE!
					boolean isGoodResult = true;
					if (wikiText.contains("steht für:") || wikiText.contains("bezeichnet<br><br>") 
									|| wikiText.contains("refer to:") || wikiText.contains("refer to<br><br>")
									|| wikiText.contains(":<br><br>")){
						isGoodResult = false;
					}
					
					if (isGoodResult){
						//we want to read only the first 3 sentences of the clean result:
						
						//split text at "." but excluding some stuff (no number, no title, no big letter, etc.:
						splitTxt = NluTools.splitSentence(wikiText_clean);
						//limit to 3 sentences
						int merge_i = splitTxt.length;
						if (merge_i > 3) merge_i = 3;
						//merge for short text
						wikiText_short_clean = "";
						for (int i=0; i<merge_i; i++){
							wikiText_short_clean += splitTxt[i].trim() + ". ";
						}
						//check if it is too long, if so make it shorter (120 seems a good value)
						if (merge_i > 1 && wikiText_short_clean.length() > 120){
							wikiText_short_clean = "";
							if (merge_i > 0) merge_i--;
							for (int i=0; i<merge_i; i++){
								wikiText_short_clean += splitTxt[i].trim() + ". ";
							}
						}
						wikiText_short_clean = wikiText_short_clean.replaceAll("\\s,", ",");
						
						//the display result can just be cut at a certain length:
						String wikiText_short = wikiText.substring(0, Math.min(wikiText.length(), 280)) + " ...";
						
						//answer - in this API the text IS! the answer so we don't need an get_answer() request.
						api.answer = wikiText_short.trim();
						api.answerClean = wikiText_short_clean.trim();
						
						//button action
						//api.actionInfo_add_action(ACTIONS.OPEN_IN_APP_BROWSER);
						//api.actionInfo_put_info("url", wikiURL);

						api.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
						api.putActionInfo("url", wikiURL); 
						api.putActionInfo("title", getButtonText(api.language));
						
						//options
						int i=0;
						for (String t : titles){
							if (++i > 1){	break;	}
							api.addAction(ACTIONS.BUTTON_CMD);
							api.putActionInfo("title", t);
							api.putActionInfo("info", "direct_cmd");
							api.putActionInfo("cmd", CmdBuilder.getWiki(t));
						}
						
						api.hasAction = true;
					
					}else{
						//answer
						api.answer = Config.answers.getAnswer(NLU_result, "knowledgeB_1a");
						api.answerClean = Converters.removeHTML(api.answer);
						
						//URL action
						api.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
						api.putActionInfo("url", wikiURL); 
						api.putActionInfo("title", getButtonText(api.language));
						
						//options
						int i=0;
						for (String t : titles){
							if (++i > 2){	break;	}
							api.addAction(ACTIONS.BUTTON_CMD);
							api.putActionInfo("title", t);
							api.putActionInfo("info", "direct_cmd");
							api.putActionInfo("cmd", CmdBuilder.getWiki(t));
						}
						
						api.hasAction = true;
					}
					
					//build html
					if (isGoodResult){
						if (relatedInfo.isEmpty()){
							api.htmlInfo = "<div><b>Wikipedia: <a href='" + wikiURL + "'>" + wikiTitle + "</a></b></div>";
						}else{
							api.htmlInfo = "<div><b>Wikipedia: <a href='" + wikiURL + "'>" + wikiTitle + "</a></b><br><br>" + relatedInfo + "</div>";
						}
					}else{
						if (CLIENTS.hasWebView(NLU_result.input.client_info)){
							api.htmlInfo = "<object type='text/html' style='width: 100%; height: 300%; overflow-y: hidden;' data='" + wikiURL_m + "'></object>";
						}else{
							api.htmlInfo = wikiURL_m;
						}
					}
					api.hasInfo = true;	
					//System.out.println(wikiURL);	//debug
					
					//build card
					/*
					String veryShort_text = "";
					if (isGoodResult){
						veryShort_text = splitTxt[0].trim().replaceAll("\\s,", ",") + ". ";		//the first sentence
					}
					//card
					Card card = new Card();
					String card_text = "<b>Wikipedia: " + wikiTitle + "</b><br><br>" + veryShort_text;
					card.addElement(card_text, wikiURL, wikiThumb);
					//add it
					api.cardInfo = card.cardInfo;
					api.hasCard = false;
					*/
					
					//is there more? - everything I forgot can be added here in the future
					//more.put(...);
				
					//if we made it till here it must have been a success :-)
					api.status = "success";
					
				//no result
				}else{
					api.answer = Config.answers.getAnswer(NLU_result, "knowledgeB_0a", NLU_result.getParameter("search"));
					api.answerClean = Converters.removeHTML(api.answer);
					api.htmlInfo = "";
					api.hasInfo = false;	api.hasCard = false;	
					
					api.addAction(ACTIONS.BUTTON_CMD);
					api.putActionInfo("title", "Websearch");
					api.putActionInfo("info", "direct_cmd");
					api.putActionInfo("cmd", CmdBuilder.getWebSearch(search));
					api.putActionInfo("options", JSON.make(ACTIONS.SKIP_TTS, true));
					
					api.hasAction = true;	
					
					//is there more?
					//more.put(...)
				}
			}
			
		//Error / no result - error handling still needs improvement 
		} catch (Exception e) {
			api.answer = Config.answers.getAnswer(NLU_result, "knowledgeB_0a", NLU_result.getParameter("search"));
			api.answerClean = Converters.removeHTML(api.answer);
			api.htmlInfo = "";
			api.hasInfo = false;	api.hasCard = false;	api.hasAction = false;
			
			//is there more?
			//more.put(...)

			e.printStackTrace();
		}
		
		//finally build the API_Result
		ApiResult result = api.buildApiResult();
		
		//System.out.println("WIKI RES: " + result.result_JSON.toJSONString()); 		//DEBUG
		return result;
	}

}
