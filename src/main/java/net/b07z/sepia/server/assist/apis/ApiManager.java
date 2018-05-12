package net.b07z.sepia.server.assist.apis;

import java.util.ArrayList;

/**
 * Class similar to "User" managing database access of APIs.<br>
 * NOTE: This class is not fully implemented yet (check references) and probably needs some conceptual and technical 
 * re-work before it can be used.
 * 
 * @author Florian Quirin
 *
 */
public class ApiManager {
	
	private String apiName = "";
	private String apiKey = "";
	private boolean signed = false;
	
	private ArrayList<String> allowedElements;		//list of elements allowed to access
	
	/**
	 * Default constructor taking the API name and private key to sign the manager and get allowed database elements.
	 * @param name - name of API
	 * @param key - key acquired during registration of API
	 */
	public ApiManager(String name, String key){
		//authenticate API and get allowed commands list
		apiName = name;
		apiKey = key;
		//
		signed = true;		//TODO: implement and get allowed elements too
		//
		allowedElements = new ArrayList<String>();
		allowedElements.add(apiName);		//every signed API can have its own field inside the user account with full access
	}
	
	/**
	 * Get API name.
	 * @return
	 */
	public String getName(){
		return apiName;
	}
	
	/**
	 * Get API key. As the API_Manager is only to used inside an API itself this should be not a security risk. 
	 * @return
	 */
	public String getKey(){
		return apiKey;
	}
	
	/**
	 * Is the API registered and has been successfully signed?
	 * @return true/false
	 */
	public boolean isSigned(){
		return signed;
	}
	
	/**
	 * Check if the database request is allowed for this API. Users may further restrict access.
	 * The field with the value of "getName()" is always allowed. 
	 * @param request - a request like ACCOUNT.USER_NAME_LAST, etc. ... (case-sensitive!)
	 * @return true/false
	 */
	public boolean isAllowedToAccess(String request){
		if (allowedElements.contains(request) || apiName.equals("API_BOSS")){
			return true;
		}else{
			return false;
		}
	}
}
