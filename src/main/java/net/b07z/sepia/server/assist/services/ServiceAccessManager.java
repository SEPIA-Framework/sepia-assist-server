package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.List;

/**
 * Class similar to "User" managing database access of services.<br>
 * NOTE: This class is not fully implemented yet (check references) and probably needs some conceptual and technical 
 * re-work before it can be used.
 * 
 * @author Florian Quirin
 *
 */
public class ServiceAccessManager {
	
	private String serviceName = "";
	private String serviceAccessKey = "";
	private boolean signed = false;
	
	private List<String> allowedElements;		//list of elements allowed to access
	
	/**
	 * Default constructor taking the service name and private key to sign the manager and get allowed database elements.
	 * @param name - name of service
	 * @param key - key acquired during registration of service
	 */
	public ServiceAccessManager(String name, String key){
		//authenticate API and get allowed commands list
		serviceName = name;
		serviceAccessKey = key;
		//
		signed = true;		//TODO: implement and get allowed elements too
		//
		allowedElements = new ArrayList<String>();
		allowedElements.add(serviceName);		//every signed API can have its own field inside the user account with full access
	}
	
	/**
	 * Get service name.
	 * @return
	 */
	public String getName(){
		return serviceName;
	}
	
	/**
	 * Get service access key. As the manager is only to used inside a service itself this should be not a security risk. 
	 * @return
	 */
	public String getKey(){
		return serviceAccessKey;
	}
	
	/**
	 * Is the service registered and has been successfully signed?
	 * @return true/false
	 */
	public boolean isSigned(){
		return signed;
	}
	
	/**
	 * Check if the database request is allowed for this service. Users may further restrict access.
	 * The field with the value of "getName()" is always allowed. 
	 * @param request - a request like ACCOUNT.USER_NAME_LAST, etc. ... (case-sensitive!)
	 * @return true/false
	 */
	public boolean isAllowedToAccess(String request){
		if (allowedElements.contains(request) || serviceName.equals("API_BOSS")){
			return true;
		}else{
			return false;
		}
	}
}
