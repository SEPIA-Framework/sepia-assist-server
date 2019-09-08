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
	
	public static final String SDK_PACKAGE = "net.b07z.sepia.sdk.services";
	private final String ADMIN_ACCESS = Config.class.getName().replaceAll("\\.", "_"); 	
	//NOTE: Config.class is the former API_BOSS and is allowed to access everything (because it is only available from system services)
	
	private String serviceName = "";
	private String serviceGroup = "";		//TODO: Could be used to grant read access to services of same user ... if we want this
	
	private String serviceAccessKey = "";
	private boolean signed = false;
	
	//list of elements allowed to access (by default or by service) - TODO: load this during server setup?
	public static final List<String> fieldsWithPartiallyAllowedElements;
	public static final List<String> defaultAllowedElements;
	static {
		//full path to allowed fields
		//NOTE / TODO: use only ROOT or 1ST CLASS children! (e.g. 'services' or 'services.[my-class]' NOT 'services.[my-class].data.someField')
		defaultAllowedElements = Arrays.asList(
				ACCOUNT.USER_NAME_FIRST, 
				ACCOUNT.USER_NAME_NICK
		);
		//resulting (and additional) parent fields 
		fieldsWithPartiallyAllowedElements = Arrays.asList(
				ACCOUNT.USER_NAME, 		//to access 'uname.first' u need partial access to 'uname'
				ACCOUNT.SERVICES		//each service can access its own field in 'services'
		);
	}
	private List<String> allowedElements;
	
	//TODO: check net.b07z.sepia.server.core.data.CmdMap and make use of permissions data ...
	
	/**
	 * Default constructor taking the calling binary class name and a private key to sign the manager and get allowed database elements.
	 * @param key - key acquired during registration of service
	 */
	public ServiceAccessManager(String key){
		//authenticate API and get allowed commands list
		String caller = getCallerClassName();
		this.serviceName = caller.replaceAll("\\.", "_");
		this.serviceGroup = this.serviceName.replaceFirst("(.*)_\\w+$", "$1");
		this.serviceAccessKey = key;
		
		//TODO: implement properly and get allowed elements too
		this.signed = true;
		//
		this.allowedElements = new ArrayList<String>();
		if (!this.serviceName.equals(ADMIN_ACCESS)){
			//every custom service can have its own field inside the user account with full access
			this.allowedElements.add(ACCOUNT.SERVICES + "." + this.serviceName);
		}
	}
	private String getCallerClassName(){
		try{
			return new Exception().getStackTrace()[2].getClassName(); 
		}catch(Exception e){
			return null;
		}
	}
	public void debugOverwriteServiceName(String newName){
		if (this.serviceName.equals(ADMIN_ACCESS)){
			this.serviceName = newName;
			this.serviceGroup = this.serviceName.replaceFirst("(.*)_\\w+$", "$1");
			this.allowedElements = new ArrayList<String>();
			if (!this.serviceName.equals(ADMIN_ACCESS)){
				//every custom service can have its own field inside the user account with full access
				this.allowedElements.add(ACCOUNT.SERVICES + "." + this.serviceName);
			}
			//DEBUG
			System.out.println("serviceName: " + this.serviceName);
			System.out.println("serviceGroup: " + this.serviceGroup);
			System.out.println("allowedElements: " + this.allowedElements);
		}else {
			throw new RuntimeException("NOT ALLOWED to overwrite service name!");
		}
	}
	
	/**
	 * Get service name.
	 * @return
	 */
	public String getServiceName(){
		return serviceName;
	}
	/**
	 * Get service group.
	 * @return
	 */
	public String getServiceGroup(){
		return serviceGroup;
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
	 * Check if the database request is maybe allowed for this service when looking deeper into the data object.
	 * This can be the case e.g. when trying to write to USER_NAME via USER_NAME_LAST (restricted) and USER_NAME_FIRST (allowed).
	 * @param request - a request like ACCOUNT.USER_NAME_LAST, etc. ... (case-sensitive!)
	 * @return true/false
	 */
	public boolean isAllowedToAccessPartially(String request){
		if (fieldsWithPartiallyAllowedElements.contains(request)){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Check if the database request is allowed for this service. Users may further restrict access.
	 * A request with the value of "getServiceName()" is always allowed for the same service. 
	 * @param request - a request like ACCOUNT.USER_NAME_LAST, etc. ... (case-sensitive!)
	 * @return true/false
	 */
	public boolean isAllowedToAccess(String request){
		if (serviceName.equals(ADMIN_ACCESS)){
			return true; 
		}else if (allowedElements.contains(request) || defaultAllowedElements.contains(request)){
			return true;
		}else{
			return false;
		}
	}
}
