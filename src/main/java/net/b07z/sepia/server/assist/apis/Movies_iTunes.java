package net.b07z.sepia.server.assist.apis;

import net.b07z.sepia.server.assist.apis.ApiInfo.Content;
import net.b07z.sepia.server.assist.apis.ApiInfo.Type;
import net.b07z.sepia.server.assist.data.Card;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

import java.net.URLEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Performs Movie searches with iTunes API.<br>
 * <br>This is the REFERENCE API for everything to come :-)
 * 
 * @author Florian Quirin
 *
 */
public class Movies_iTunes implements ApiInterface {
	
	//info
	public ApiInfo getInfo(String language){
		return new ApiInfo(Type.REST, Content.data, true);
	}

	//result
	public ApiResult getResult(NluResult NLU_result){
		//initialize result
		API api = new API(NLU_result);
		
		//make the external API call and build API_Result
		try {
			//get parameters
			String search = NLU_result.getParameter(PARAMETERS.SEARCH);
			Debugger.println("cmd: Movies, search: " + search, 2);				//debug
			
			String attribute = "&attribute=movieTerm";			//iTunes attribute defining the search (genre, actor, movie, etc...)
			//get info - for now: overwrites attribute
			String info = URLEncoder.encode(NLU_result.getParameter("info"), "UTF-8");
			if (!info.isEmpty()){
				if (info.matches("actor"))
					attribute = "&attribute=actorTerm";
				else if (info.matches("director"))
					attribute = "&attribute=directorTerm";
			}else{
				//get genre - for now: only applies when info is empty and overwrites search
				String type = NLU_result.getParameter("type");
				if (!type.isEmpty()){
					search = type;
					attribute = "&attribute=genreTerm";
				}
			}
			
			//set country (determines the store too)
			String country = "&country=US";
			if (api.language.matches("de")){
				country = "&country=" + api.language;
			}
			
			//make the HTTP GET call to iTunes API
			int limit = 14;
			String url = "https://itunes.apple.com/search?term=" + URLEncoder.encode(search, "UTF-8") + "&entity=movie&limit=" + limit + attribute + country;
			JSONObject response = Connectors.httpGET(url.trim());
			JSONArray movie_results = (JSONArray) response.get("results");
			
			//run through all movie results
			if (movie_results !=null && movie_results.size()>0){
				//init variables to collect some stuff for answer and "cards"
				int hits = movie_results.size();
				int t3_size = Math.min(hits, 3);
				String[] title_top3 = new String[t3_size];		//top 3 or less depending on results
				String[] url_top3 = new String[t3_size];		//
				String ref_top3 = "";						//
				String[] imgs_top3 = new String[t3_size];		//
				int j=0;
				
				//build HTML info
				api.htmlInfo = "<div style='width:100%; height:100%;'>iTunes results:<br>";
				for (int i=0; i<hits; i++){
					JSONObject o = (JSONObject) movie_results.get(i);
					//images
					String image = (String) o.get("artworkUrl100");
					image = image.replace("100x100", "227x227");		//get the bigger artwork
					//links
					String link = (String) o.get("trackViewUrl");
					//title
					String title = (String) o.get("trackName");
					//get top 3 for answer and cards
					if (j<3){
						title_top3[j] = title;
						url_top3[j] = link;
						imgs_top3[j] = image;
						if (hits==1)
							ref_top3 += "<a href='" + link + "'>" + title + ". </a><br>";
						else if (j==2)
							ref_top3 += (j+1) + ". " + "<a href='" + link + "'>" + title + ". </a><br>";
						else
							ref_top3 += (j+1) + ". " + "<a href='" + link + "'>" + title + ", </a><br>";
						j++;
					}
					api.htmlInfo += "<a href='" + link +"' title='" + title + "'><img src='" +  image + "' width='48%'/></a> ";
				}
				api.htmlInfo += "</div>";
				api.hasInfo = true;
				
				//build card
				Card card = new Card();
				for (int i=0; i<title_top3.length; i++){
					String card_text = title_top3[i];
					String card_img = imgs_top3[i];
					String card_url = url_top3[i];
					if (card_text != null && !card_text.isEmpty()){
						card.addElement(card_text, card_url, card_img);
					}
				}
				//add it
				api.cardInfo = card.cardInfo;
				//activate it?
				if (api.cardInfo.isEmpty()){
					api.hasCard = false;
				}else{
					api.hasCard = true;
					//System.out.println(card_info.toJSONString());		//debug
				}
				
				//build answer - I found foo movies for foo with the top foo names like foo1, foo2, foo3
				if (hits == 1){
					api.answer = Config.answers.getAnswer(NLU_result, "movies_1b", hits, search, title_top3.length, ref_top3);
				}else if (hits == limit){
					api.answer = Config.answers.getAnswer(NLU_result, "movies_1c", hits, search, title_top3.length, ref_top3);
				}else{
					api.answer = Config.answers.getAnswer(NLU_result, "movies_1a", hits, search, title_top3.length, ref_top3);
				}
				
				//build clean answer
				api.answer_clean = Converters.removeHTML(api.answer);
				//System.out.println("answer clean: " + answer_clean);		//debug
				
				//is there more? - everything that changed during the evaluation (e.g. mood), was added later or is API specific can be added here
				//any API specific info ... api.more.put("key", "value")
				api.addRequestTag("movie search for " + search);
				//changes in mood ... api.mood
				//changes in context ... api.context
				//changes in environment ... api.environment
				//etc. ...
			
				//if we made it till here it must have been a success :-)
				api.status = "success";
			
			//no results obtained from iTunes
			}else{
				api.answer = Config.answers.getAnswer(NLU_result, "movies_0a", search);		//answer = "sorry, no results";
				api.answer_clean = Converters.removeHTML(api.answer);
				api.htmlInfo = "";
				api.hasInfo = false;	api.hasCard = false;	api.hasAction = false;
				
				api.status = "success";		//its kind of a success because the query went well, it just did not give an answer ^^.
			}
		
		//some error occurred somewhere - error handling still needs some improvement 
		} catch (Exception e) {
			api.answer = Config.answers.getAnswer(NLU_result, "movies_0a", NLU_result.getParameter("search"));		//answer = "sorry, no results";
			api.answer_clean = Converters.removeHTML(api.answer);
			api.htmlInfo = "";
			api.hasInfo = false;	api.hasCard = false;	api.hasAction = false;

			e.printStackTrace();
		}
		
		//finally build the API_Result
		ApiResult result = api.build_API_result();
		
		//return result_JSON.toJSONString();
		return result;
	}
	
}
