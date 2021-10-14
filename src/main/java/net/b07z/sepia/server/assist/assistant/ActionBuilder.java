package net.b07z.sepia.server.assist.assistant;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.core.assistant.ACTIONS;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to build actions for services (e.g. commands, buttons etc.) in a simpler way.
 * 
 * @author Florian Quirin
 *
 */
public class ActionBuilder {

	private JSONArray actionArray;
	
	/**
	 * Initialize action builder using {@link ServiceBuilder}.
	 * @param serviceBuilder - builder used inside a service
	 */
	public ActionBuilder(ServiceBuilder serviceBuilder){
		this.actionArray = serviceBuilder.actionInfo;
	}
	/**
	 * Initialize action builder using the 'actionInfo' array of a {@link ServiceResult} or {@link ServiceBuilder}.
	 * @param actionInfo - JSONArray holding all actions of a service
	 */
	public ActionBuilder(JSONArray actionInfo){
		this.actionArray = actionInfo;
	}
	
	/**
	 * Add a new client action to a service.
	 * @param actionName - one String of {@link ACTIONS}
	 * @param actionParameters - parameters specific to this action (or new JSONObject())
	 */
	public ActionBuilder addAction(String actionName){
		return addAction(actionName, new JSONObject(), null);
	}
	/**
	 * Add a new client action to a service.
	 * @param actionName - one String of {@link ACTIONS}
	 * @param actionParameters - parameters specific to this action (or new JSONObject())
	 */
	public ActionBuilder addAction(String actionName, JSONObject actionParameters){
		return addAction(actionName, actionParameters, null);
	}
	/**
	 * Add a new client action to a service including some extra options.
	 * @param actionName - one String of {@link ACTIONS}
	 * @param actionParameters - parameters specific to this action (or new JSONObject())
	 * @param options - one or more {@link ACTIONS}.OPTION_... with specific value (or null)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ActionBuilder addAction(String actionName, JSONObject actionParameters, JSONObject options){
		JSONObject action = actionParameters;
		action.put("type", actionName);		//NOTE: "type" can NEVER be a parameter! - this is a bit of a bad design decision but ... 
		if (options != null && !options.isEmpty()){
			action.put("options", options);
		}
		JSON.add(actionArray, action);
		return this;
	}
	
	/**
	 * Get the reference to the 'actionInfo' array created with this builder.<br>
	 * NOTE: Use this only if you want to further modify the result.
	 */
	public JSONArray getArray(){
		return this.actionArray;
	}
	
	//--- some convenience methods ---

	/**
	 * Get the default "Click here to open" text in local version.
	 * @param language - language code
	 */
	public static String getDefaultButtonText(String language){
		if (language.equals(LANGUAGES.DE)){
			return "Zum Öffnen bitte hier klicken";
		}else{
			return "Click here to open";
		}
	}
	
	/**
	 * Add basic web-search.
	 * @param service - ServiceBuilder used to create service result
	 * @param searchTerm - term to search
	 */
	public static void addWebSearchButton(ServiceBuilder service, String searchTerm, String customTitle){
		//add button that links to help
		service.addAction(ACTIONS.BUTTON_CMD);
		service.putActionInfo("title", Is.notNullOrEmpty(customTitle)? customTitle : "Web Search");
		service.putActionInfo("info", "direct_cmd");
		service.putActionInfo("cmd", CmdBuilder.getWebSearch(searchTerm));
		service.putActionInfo("options", JSON.make(ACTIONS.OPTION_SKIP_TTS, true));
	}

	/**
	 * Add a button-action that links to API-Key help page.
	 * @param service - ServiceBuilder used to create service result
	 */
	public static void addApiKeyInfoButton(ServiceBuilder service){
		//add button that links to help
		service.addAction(ACTIONS.BUTTON_IN_APP_BROWSER);
		service.putActionInfo("url", "https://github.com/SEPIA-Framework/sepia-docs/wiki/API-keys");
		service.putActionInfo("title", "Info: API-Keys");
	}
}
