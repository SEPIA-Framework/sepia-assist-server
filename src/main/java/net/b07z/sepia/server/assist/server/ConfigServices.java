package net.b07z.sepia.server.assist.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.assistant.Assistant;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.email.SendEmail;
import net.b07z.sepia.server.assist.endpoints.AuthEndpoint;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interviews.Interview;
import net.b07z.sepia.server.assist.interviews.InterviewServicesMap;
import net.b07z.sepia.server.assist.services.OpenDashboard;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.assistant.CMD;
import net.b07z.sepia.server.core.data.CmdMap;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.SandboxClassLoader;

/**
 * This class connects commands to interviews and interviews to services.
 * @see InterviewServicesMap
 * 
 * @author Florian Quirin
 *
 */
public class ConfigServices {
	
	private static final String CUSTOM_SERVICES_PACKAGE = "net.b07z.sepia.sdk.services";
	
	/**
	 * Get folder to "custom services package". It's usually the folder just before the user IDs start, e.g.:<br>
	 * "Xtensions/Plugins/net/b07z/sepia/sdk/services/"
	 */
	public static String getCustomServicesBaseFolder(){
		return Config.sdkClassesFolder + "services/";
	}
	
	/**
	 * Get package path to custom services (full name), e.g.:<br>
	 * "net.b07z.sepia.sdk.services"
	 */
	public static String getCustomServicesPackage(){
		return CUSTOM_SERVICES_PACKAGE;
	}
	
	//essential stand-alone services
	public static ServiceInterface dashboard = new OpenDashboard();
	
	//Note: loadInterviewService map has moved to interviews.InterviewServicesMap
	
	//custom services
	//see also UserData Objects
	public static Map<String, SandboxClassLoader> classLoaders = new HashMap<>();
	private static List<String> blackList;
	
	/**
	 * Return class loader for a certain class that was created previously or create a new one.
	 * @param className - binary name of class this loader was created for 
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static SandboxClassLoader getCustomServiceClassLoader(String className) throws ClassNotFoundException, IOException{
		//Inner classes will point to parent loader
		String loaderName = className.replaceAll("\\$.*", "").trim();
		//Check or create
		if (classLoaders.containsKey(loaderName)){
			return classLoaders.get(loaderName);
		}else{
			SandboxClassLoader sbcl = addCustomServiceClassLoader(className);
			return sbcl;
		}
	}
	/**
	 * Create or overwrite a sand-box class loader for a dynamically loaded class and 
	 * pre-load class and inner classes (TheClass and all TheClass$...) by searching package folder. 
	 * @param className - binary name of class this loader was created for (e.g. package + name + $inner)
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static SandboxClassLoader addCustomServiceClassLoader(String className) throws ClassNotFoundException, IOException{
		//Inner classes will point to parent loader
		String loaderName = className.replaceAll("\\$.*", "").trim();
		try (SandboxClassLoader classLoader = new SandboxClassLoader(new File(Config.pluginsFolder), blackList);){
			classLoaders.put(loaderName, classLoader);
			
			//Get all classes that belong to this loader based on its name:
			String simpleName = loaderName.replaceFirst(".*\\.", "").trim();
			System.out.println("Base class name: " + simpleName); 					//DEBUG
			List<File> allClassFiles = FilesAndStreams.directoryToFileList(getCustomServicesBaseFolder(), null, true);
			for (File f : allClassFiles){
				String fileName = f.getName();
				if (fileName.matches(Pattern.quote(simpleName) + "(\\$.*|)\\.class")){
					System.out.println("Found class file: " + fileName); 			//DEBUG
					String binaryClassName = f.getPath()
							.replaceAll(Pattern.quote(File.separator), ".")
							.replaceFirst(".*(" + Pattern.quote(CUSTOM_SERVICES_PACKAGE) + ")", "$1")
							.replace(".class", "").trim();
					System.out.println("Binary class name: " + binaryClassName);	//DEBUG
					//load
					classLoader.loadClass(binaryClassName);
				}
			}
			//Close the loader
			classLoader.close();
			
			return classLoader;
			
		}catch (ClassNotFoundException e){
			Debugger.println("Custom class loader could NOT be created due to ClassNotFoundException: " + e.getMessage(), 1);
			throw e;
		}catch (IOException e){
			Debugger.println("Custom class loader could NOT be created due to IOException: " + e.getMessage(), 1);
			throw e;
		}
	}
	
	/**
	 * Setup sand-boxed class loader by building blacklist.
	 */
	public static void setupSandbox(){
		blackList = new ArrayList<>();
		
		//System stuff:
		//blackList.add(Runtime.class.getPackage().getName());		//TODO: can only be handled by security manager?
		//blackList.add(Process.class.getPackage().getName());		//"		"		"
		
		//Framework stuff:
		blackList.add(Config.class.getPackage().getName()); 		//server.*
		blackList.add(AuthEndpoint.class.getPackage().getName());	//endpoints.*
		blackList.add(DB.class.getPackage().getName());				//database.*
		blackList.add(Interview.class.getPackage().getName()); 		//interviews.*
		blackList.add(SendEmail.class.getPackage().getName()); 		//email.*
		//TODO: complete blacklist
	}
	/**
	 * Add more classes or packages to blacklist.
	 * @param classOrPackageName
	 */
	public static void addToSandboxBlackList(String classOrPackageName){
    	blackList.add(classOrPackageName);
    }

	/**
	 * Get all master services of the system.
	 * @return services or empty list
	 */
	public static List<ServiceInterface> getAllSystemMasterServices(){
		List<ServiceInterface> services = new ArrayList<>();

		//get all commands with a service
		Set<String> mappedCommands = InterviewServicesMap.getAllMappedCommands();
		for (String c : mappedCommands){
			//get master
			ServiceInterface si = getMasterService(c);
			if (si != null){
				services.add(si);
			}
		}
		return services;
	}
	
	/**
	 * Get custom services of a user as a list.
	 * @param user - User
	 * @param nluInput - NLU input
	 */
	public static List<ServiceInterface> getCustomServicesList(NluInput nluInput, User user){
		List<ServiceInterface> services = new ArrayList<>();

		List<CmdMap> customMap = restoreOrLoadCustomCommandMapping(nluInput, user);
		for (CmdMap cm : customMap){
			List<String> cmList = (ArrayList<String>) cm.getServices();
			services.addAll(buildCustomServices(user, cmList));
		}
		return services;
	}
	/**
	 * Get connected services for a command. If the command is a system default command (is not of the format "[userId].[cmd_name]")
	 * it returns the default {@link InterviewServicesMap} as list, else it looks for personal/custom/sdk commands in the 
	 * assistant- or user-data.
	 * @param user - User with personal commands or null for auto-select assistant
	 * @param cmd - {@link CMD} entry or custom command in format "[userId].[cmd_name]"
	 */
	public static List<ServiceInterface> getCustomOrSystemServices(NluInput nluInput, User user, String cmd){
		List<ServiceInterface> services = null;
		//SYSTEM
		if (!cmd.contains(".")){		//if (!cmd.matches("(" + Config.user_id_prefix + "|gig)\\d\\d\\d\\d+\\..*")){
			services = buildServices(cmd);
		
		}else{
			List<CmdMap> customMap;
			
			//ASSISTANT
			if (cmd.startsWith(Config.assistantId + ".") || user == null){
				customMap = restoreOrLoadCustomCommandMapping(nluInput, Config.getAssistantUser());
				
			//CUSTOM
			}else{
				customMap = restoreOrLoadCustomCommandMapping(nluInput, user);
			}
			
			//search service for command
			for (CmdMap cm : customMap){
				if (cm.getCommand().equals(cmd)){
					List<String> cmList = (ArrayList<String>) cm.getServices();
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
			customMap = nluInput.getCustomCommandToServicesMappings(); 		//TODO: this is only a session based storage
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
	 * @param refList - list of services (canonical names of service classes)
	 */
	public static List<ServiceInterface> buildCustomServices(User user, List<String> refList){
		List<ServiceInterface> apiList = new ArrayList<>();
		for (String className : refList){
			try{
				ServiceInterface service = (ServiceInterface) getCustomServiceClassLoader(className).loadClass(className).newInstance();
				//check if service is public or the creator asks for it
				if (service.getInfo("").makePublic || className.startsWith(CUSTOM_SERVICES_PACKAGE + "." + user.getUserID() + ".")){
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
	public static List<ServiceInterface> buildServices(List<String> refList){
		List<ServiceInterface> apiList = new ArrayList<>();
		for (String a : refList){
			apiList.add((ServiceInterface) ClassBuilder.construct(a));
		}
		return apiList;
	}
	/**
	 * Return a list of services connected to a command (CMD...)
	 */
	public static List<ServiceInterface> buildServices(String command){
		//exceptions
		if (command.equals(CMD.ABORT) || command.equals(CMD.NO_RESULT) || command.equals(CMD.OPEN_LINK)){
			return new ArrayList<ServiceInterface>();
		}

		List<String> refList = InterviewServicesMap.get().get(command);
		if (refList == null || refList.isEmpty()){
			Debugger.println("Command: '" + command + "' has no services connected to be handled by interview module!", 1);
			return new ArrayList<ServiceInterface>();
		}else{
			return buildServices(InterviewServicesMap.get().get(command));
		}
	}
	/**
	 * Get master (first) service connected to a command (CMD...)
	 */
	public static ServiceInterface getMasterService(String command){
		String masterApiService = InterviewServicesMap.get().get(command).get(0);
		return (ServiceInterface) ClassBuilder.construct(masterApiService);
	}
}
