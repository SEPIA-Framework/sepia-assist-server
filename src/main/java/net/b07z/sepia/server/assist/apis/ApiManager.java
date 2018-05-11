package net.b07z.sepia.server.assist.apis;

import java.util.ArrayList;

/**
 * Class similar to "User" managing APIs especially allowed access to database.
 * 
 * @author Florian Quirin
 *
 */
public class ApiManager {
	
	private String api_name = "";
	private String api_key = "";
	private boolean signed = false;
	
	private ArrayList<String> allowed_elements;		//list of elements allowed to access
	
	/**
	 * Default constructor taking the API name and private key to sign the manager and get allowed database elements.
	 * @param name - name of API
	 * @param key - key acquired during registration of API
	 */
	public ApiManager(String name, String key){
		//authenticate API and get allowed commands list
		api_name = name;
		api_key = key;
		//
		signed = true;		//TODO: implement and get allowed elements too
		//
		allowed_elements = new ArrayList<String>();
		allowed_elements.add(api_name);		//every signed API can have its own field inside the user account with full access
	}
	
	/**
	 * Get API name.
	 * @return
	 */
	public String getName(){
		return api_name;
	}
	
	/**
	 * Get API key. As the API_Manager is only to used inside an API itself this should be not a security risk. 
	 * @return
	 */
	public String getKey(){
		return api_key;
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
		if (allowed_elements.contains(request) || api_name.equals("API_BOSS")){
			return true;
		}else{
			return false;
		}
	}
}
