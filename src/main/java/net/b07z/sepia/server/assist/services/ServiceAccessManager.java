package net.b07z.sepia.server.assist.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.ACCOUNT;

/**
 * Class similar to "User" managing database access of services.<br>
 * NOTE: This class is not fully implemented yet (check references) and probably needs some conceptual and technical 
 * re-work before it can be used. 
 * 
 * @author Florian Quirin
 *
 */
public final class ServiceAccessManager {
	
	private String serviceName = "";
	private String serviceAccessKey = "";
	private boolean signed = false;
	
	//list of elements allowed to access (by default or by service)
	public static final List<String> defaultAllowedElements = Arrays.asList(
			ACCOUNT.USER_NAME_FIRST, ACCOUNT.USER_NAME_NICK		//TODO: load this during server setup
	);
	private List<String> allowedElements;
	
	//TODO: check net.b07z.sepia.server.core.data.CmdMap and make use of permissions data ...
	
	/**
	 * Default constructor taking the calling binary class name and a private key to sign the manager and get allowed database elements.
	 * @param key - key acquired during registration of service
	 */
	public ServiceAccessManager(String key){
		//authenticate API and get allowed commands list
		this.serviceName = getCallerClassName();
		this.serviceAccessKey = key;
		
		//TODO: implement properly and get allowed elements too
		this.signed = true;
		//
		this.allowedElements = new ArrayList<String>();
		allowedElements.add(serviceName);		//every signed API can have its own field inside the user account with full access
	}
	private String getCallerClassName(){
		try{
			return new Exception().getStackTrace()[2].getClassName(); 
		}catch(Exception e){
			return null;
		}
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
		if (allowedElements.contains(request) || defaultAllowedElements.contains(request) 
				|| serviceName.equals(Config.class.getName())){ 		//NOTE: Config.class is the former API_BOSS
			return true;
		}else{
			return false;
		}
	}
}
