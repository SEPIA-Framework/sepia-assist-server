package net.b07z.sepia.server.assist.server;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.apis.Open_Dashboard;
import net.b07z.sepia.server.assist.assistant.Assistant;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.email.SendEmail;
import net.b07z.sepia.server.assist.endpoints.AuthEndpoint;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interviews.Interview;
import net.b07z.sepia.server.assist.interviews.InterviewServicesMap;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.data.CmdMap;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.DateTime;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.SandboxClassLoader;

/**
 * This class connects commands to interviews and interviews to services.
 * 
 * @author Florian Quirin
 *
 */
public class ConfigServices {
	
	private static String CUSTOM_PACKAGE = "services";
	
	/**
	 * Get package path to custom services (full name).
	 */
	public static String getCustomPackage(){
		return CUSTOM_PACKAGE;
	}
	
	//essential stand-alone services
	public static ApiInterface dashboard = new Open_Dashboard();
	
	//Note: loadInterviewService map has moved to interviews.InterviewServicesMap
	
	//custom services
	//see also UserData Objects
	public static HashMap<String, SandboxClassLoader> classLoaders = new HashMap<>();
	private static ArrayList<String> blackList;
	public static SandboxClassLoader getCustomClassLoader(String className){
		if (classLoaders.containsKey(className)){
			return classLoaders.get(className);
		}else{
			return addCustomClassLoader(className);
		}
	}
	public static SandboxClassLoader addCustomClassLoader(String className){
		try{
			SandboxClassLoader classLoader = new SandboxClassLoader(new File(Config.pluginsFolder), blackList);
			classLoaders.put(className, classLoader);
			return classLoader;
			
		}catch (MalformedURLException e){
			Debugger.println("Custom class loader could NOT be created!", 1);
			Debugger.printStackTrace(e, 3);
			//e.printStackTrace();
			throw new RuntimeException(DateTime.getLogDate() + " - Custom class loader ERROR: " + e.getMessage(), e);
		}
	}
	public static void setupSandbox(){
		blackList = new ArrayList<>();
		blackList.add(Config.class.getPackage().getName()); 		//server.*
		blackList.add(AuthEndpoint.class.getPackage().getName());	//endpoints.*
		blackList.add(DB.class.getPackage().getName());				//database.*
		blackList.add(Interview.class.getPackage().getName()); 		//interviews.*
		blackList.add(SendEmail.class.getPackage().getName()); 		//email.*
		//TODO: complete blacklist
	}
	public static void addToSandboxBlackList(String classOrPackageName){
    	blackList.add(classOrPackageName);
    }

	/**
	 * Get custom services of a user as a list.
	 * @param user - User
	 * @param nluInput - NLU input
	 */
	public static ArrayList<ApiInterface> getCustomServicesList(NluInput nluInput, User user){
		ArrayList<ApiInterface> services = new ArrayList<>();

		List<CmdMap> customMap = restoreOrLoadCustomCommandMapping(nluInput, user);
		for (CmdMap cm : customMap){
			ArrayList<String> cmList = (ArrayList<String>) cm.getServices();
			services.addAll(buildCustomServices(user, cmList));
		}
		return services;
	}
	/**
	 * Get connected services for a command by first searching user custom services and then system services.
	 * @param user - User
	 * @param cmd - command
	 */
	public static ArrayList<ApiInterface> getCustomOrSystemServices(NluInput nluInput, User user, String cmd){
		ArrayList<ApiInterface> services = null;
		//SYSTEM
		if (!cmd.contains(".")){		//if (!cmd.matches("(" + Config.user_id_prefix + "|gig)\\d\\d\\d\\d+\\..*")){
			services = buildServices(cmd);
		
		}else{
			List<CmdMap> customMap;
			
			//ASSISTANT
			if (cmd.startsWith(Config.assistantId + ".")){
				customMap = restoreOrLoadCustomCommandMapping(nluInput, Config.getAssistantUser());
				
			//CUSTOM
			}else{
				customMap = restoreOrLoadCustomCommandMapping(nluInput, user);
			}
			
			//search service for command
			for (CmdMap cm : customMap){
				if (cm.getCommand().equals(cmd)){
					ArrayList<String> cmList = (ArrayList<String>) cm.getServices();
					//System.out.println("getCustomOrSystemServices - FOUND CUSTOM SERVICE(S): " + cmList); 		//debug
					services = buildCustomServices(user, cmList);
				}
			}
		}
		return services;
	}
	/**
	 * Try to restore custom command->services mapping from cache or load it. 
	 */
	private static List<CmdMap> restoreOrLoadCustomCommandMapping(NluInput nluInput, User user){
		//cached?
		List<CmdMap> customMap;
		boolean isAssistant = (user.getUserID().equals(Config.assistantId));
		if (isAssistant){
			customMap = Assistant.customCommandsMap;		//note: reset in UserData_xy.registerCustomService(..)
		}else{
			customMap = nluInput.getCustomCommandToServicesMappings();
		}
		if (customMap == null){
			UserDataInterface userData = user.getUserDataAccess();
			customMap = userData.getCustomCommandMappings(user, null);
			//Error? Might just mean that the user never used the feature, TODO: we could just write something to the DB here, but if the error is real ...
			if (customMap == null){
				customMap = new ArrayList<>();
			}
			//cache result
			if (isAssistant){
				Assistant.customCommandsMap = customMap;
			}else{
				nluInput.setCustomCommandToServicesMappings(customMap);
			}
		}
		return customMap;
	}
	
	/**
	 * Take a String list of services from the custom services and build the classes via the custom class loader. 
	 * @param user 
	 * @param refList - list of services
	 */
	public static ArrayList<ApiInterface> buildCustomServices(User user, ArrayList<String> refList){
		ArrayList<ApiInterface> apiList = new ArrayList<>();
		for (String className : refList){
			try{
				ApiInterface service = (ApiInterface) getCustomClassLoader(className).loadClass(className).newInstance();
				//check if service is public or the creator asks for it
				if (service.getInfo("").makePublic || className.startsWith(CUSTOM_PACKAGE + "." + user.getUserID() + ".")){
					apiList.add(service);
				}else{
					Debugger.println("buildCustomServices - user '" + user.getUserID() + "' tried to load non-public service: " + className, 1);
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return apiList;
	}
	/**
	 * Take a String list of services assigned to an interview and build the classes. 
	 * @param refList - list of services (String)
	 */
	public static ArrayList<ApiInterface> buildServices(ArrayList<String> refList){
		ArrayList<ApiInterface> apiList = new ArrayList<>();
		for (String a : refList){
			apiList.add((ApiInterface) ClassBuilder.construct(a));
		}
		return apiList;
	}
	/**
	 * Return a list of services connected to a command (CMD...)
	 */
	public static ArrayList<ApiInterface> buildServices(String command){
		//exceptions
		if (command.equals(CMD.ABORT) || command.equals(CMD.NO_RESULT) || command.equals(CMD.OPEN_LINK)){
			return new ArrayList<ApiInterface>();
		}

		ArrayList<String> refList = InterviewServicesMap.get().get(command);
		if (refList == null || refList.isEmpty()){
			Debugger.println("Command: '" + command + "' has no services connected to be handled by interview module!", 1);
			return new ArrayList<ApiInterface>();
		}else{
			return buildServices(InterviewServicesMap.get().get(command));
		}
	}
	/**
	 * Get master (first) service connected to a command (CMD...)
	 */
	public static ApiInterface getMasterService(String command){
		String masterApiService = InterviewServicesMap.get().get(command).get(0);
		return (ApiInterface) ClassBuilder.construct(masterApiService);
	}
}
