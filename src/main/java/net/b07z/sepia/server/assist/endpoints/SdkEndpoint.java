package net.b07z.sepia.server.assist.endpoints;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.data.CmdMap;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.server.RequestGetOrFormParameters;
import net.b07z.sepia.server.core.server.RequestParameters;
import net.b07z.sepia.server.core.server.RequestPostParameters;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.FilesAndStreams;
import net.b07z.sepia.server.core.tools.JSON;
import spark.Request;
import spark.Response;

/**
 * API endpoint to handle functions related to the services SDK.
 * 
 * @author Florian Quirin
 *
 */
public class SdkEndpoint {
	
	public static String uploadEndpointPath = "/upload-service"; 	//used in form, keep in sync with the endpoint definitions (in Start)!
	
	public static final String UPLOAD_FILE_KEY = "upload_file";
	public static final String UPLOAD_CODE_KEY = "upload_code";
	public static final String UPLOAD_CODE_CLASS_NAME = "upload_code_class_name";

	/**-- UPLOAD SERVICE HTML FORM --
	 * End-point that returns the form to upload services. 
	 */
	public static String uploadServiceGet(Request request, Response response){
		//Requirements
		if (!Config.enableSDK){
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "SDK not enabled on this server");
			return SparkJavaFw.returnResult(request, response, result.toJSONString(), 200);
		}
		long tic = System.currentTimeMillis();
	   	String html =
	   		"<style>#upload-service-box *{margin:5px;font-family:sans-serif;text-align:center;}</style>"
	   		+ "<body><div id='upload-service-box'>"
	   		 	+ "<h3>Service-module upload interface</h3>"
	   		 	+ "<form action='" + uploadEndpointPath + "' method='post' enctype='multipart/form-data' target='_blank'>"
	   		 		+ "<label>Id: <input type='text' name='GUUID'></label>"
	   		 		+ "<label>Pwd: <input type='password' name='PWD'></label><br>"
	   		 		+ "<input type='file' name='" + UPLOAD_FILE_KEY + "' accept='.class|.java|.service'><br>"
	   		 		+ "<button class='interface_button'>UPLOAD SERVICE</button>"
	   		 	+ "</form>"
	        + "</div></body>";
	   	//stats
	  	Statistics.addOtherApiHit("upload-service form");
	  	Statistics.addOtherApiTime("upload-service form", tic);
		return html;
	}

	/**-- UPLOAD SERVICE POST --
	 * End-point to send service class file to.  
	 */
	public static String uploadServicePost(Request req, Response res){
		//Requirements
		if (!Config.enableSDK){
			JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "SDK not enabled on this server");
			return SparkJavaFw.returnResult(req, res, result.toJSONString(), 200);
		}
		long tic = System.currentTimeMillis();
		//?? - required to read parameters properly 
	    req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
	    
	    //this endpoint requires a certain content type
	    String contentType = req.headers("Content-type");
	    if (!contentType.toLowerCase().contains("multipart/form-data")){
	    	JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "endpoint requires content-type 'multipart/form-data' but saw: " + contentType);
			return SparkJavaFw.returnResult(req, res, result.toJSONString(), 200);
	    }
		
		//authenticate
		Authenticator token = Start.authenticate(req);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(req, res, token.getErrorCode());
		}
		//create user
		User user = new User(null, token);
		String userId = user.getUserID();
		
		//check role
		if (!user.hasRole(Role.developer) && !user.hasRole(Role.assistant)){
			Debugger.println("unauthorized access attempt to upload-service endpoint (missing role)! User: " + userId, 3);
			return SparkJavaFw.returnNoAccess(req, res);
		}
		try{
			//Check upload type
			RequestParameters params = new RequestGetOrFormParameters(req);
			String codeKey = params.getString(UPLOAD_CODE_KEY);
			System.out.println("codeKey: " + codeKey); 		//DEBUG
					
	        //receive and store file
			List<Path> tempFiles = receiveServiceFile(req, params, userId);
			String baseFileName = tempFiles.get(0).getFileName().toString().replaceFirst("\\$.*\\.", ".");
	        
	        try{
	        	//validate and register service
	        	String className = ConfigServices.getCustomServicesPackage() + "." + userId + "." + baseFileName.replaceFirst("\\.(class|java|service)$", "");
	        	ServiceUploadResult uploadRes = validateAndRegisterService(className, user);
	        	
	        	//stats
	          	Statistics.addOtherApiHit("upload-service");
	          	Statistics.addOtherApiTime("upload-service", tic);
	          	
	          	return uploadRes.getCanonicalName() + " has been uploaded! (old triggers removed: " + uploadRes.getCleanedTriggers() + ")";
	        	
	        }catch (Exception e){
	        	Debugger.println("upload-service - Issue in validation step: " + e.getMessage(), 1);
		      	Debugger.printStackTrace(e, 2);
	        	try{ 
	        		for (Path p : tempFiles){
	        			Files.delete(p);
	        			Debugger.println("upload-service - File '" + p.toString() + "' removed, was no proper service!", 3);
	        		}
	        	}catch (Exception e1) {
	        		Debugger.println("upload-service - File '" + baseFileName + "' (or related file) NOT removed: " + e1.getMessage(), 1);
	        	}
	        	throw e;
	        }
		
		}catch(Exception e){
			Debugger.println("upload-service - " + e.getMessage(), 1);
	      	//Debugger.printStackTrace(e, 2);
	      	
			//stats
	      	Statistics.addOtherApiHit("upload-service fail");
	      	Statistics.addOtherApiTime("upload-service fail", tic);
	      	
	      	String errorMsg = "ERROR: " + e.getMessage(); 
	      		/*
	      		+ "\n"
				+ "<br><br>Check-list:"
				+ "<ul>"
					+ "<li>Is the package name of my class: ...services." + userId +"?</li>"
					+ "<li>Does my class implement the ServiceInterface properly?</li>"
					+ "<li>Did I set the 'intendedCommand' variable inside my class with format: user_id.command?</li>"
				+ "</ul>";
				*/
	      	return errorMsg;
		}
	}
	
	/**-- DELETE SERVICE POST --
	 * End-point to delete a custom service and clean up.  
	 */
	public static String deleteServicePost(Request req, Response res){
		long tic = System.currentTimeMillis();
		
		RequestParameters params = new RequestPostParameters(req);
		
		//authenticate
		Authenticator token = Start.authenticate(params, req);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(req, res, token.getErrorCode());
		}
		//create user
		User user = new User(null, token);
		
		//check role
		//not required ... a user should always be allowed to delete his services
		
		try{
			//Get commands to be removed
			String[] commandsArray = params.getStringArray("commands");
			if (commandsArray == null || commandsArray.length == 0){
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "Missing 'commands' array with commands to be removed.");
				return SparkJavaFw.returnResult(req, res, msg.toJSONString(), 200);
			}
			Set<CmdMap> removeThisMappings = new HashSet<>();
			Set<String> ignoredCommands = new HashSet<>();
			for (String cmd : commandsArray){
				//add only custom commands:
				if (cmd.contains(".")){
					removeThisMappings.add(new CmdMap(cmd, null, null));
				}else{
					ignoredCommands.add(cmd);
				}
			}
			
			//Remove them and their triggers
			UserDataInterface userData = user.getUserDataAccess();
			long deletedMappings = userData.deleteCustomCommandMappings(user, removeThisMappings);
			long deletedTriggers = 0;
			Set<String> failedTriggerRemoves = new HashSet<>();
			for (CmdMap cmdMap : removeThisMappings){
				long thisDeletedTriggers = userData.deletePersonalSdkCommands(user, cmdMap.getCommand(), null);
				if (thisDeletedTriggers == -1){
					failedTriggerRemoves.add(cmdMap.getCommand());
				}else{
					deletedTriggers += thisDeletedTriggers;
				}
			}
			
			//Delete service files
			String[] servicesArray = params.getStringArray("services");
			long deletedFiles = 0;
			if (servicesArray != null && servicesArray.length > 0){
				//TODO: delete files
			}
			
			if (deletedMappings == -1 || failedTriggerRemoves.size() > 0){
				throw new RuntimeException("delete-service failed (partially?) with deletedMappings=" + deletedMappings 
						+ ", deletedTriggers=" + deletedTriggers + " and/or issues with commands=" + failedTriggerRemoves.toString());
			}
			
			//stats
          	Statistics.addOtherApiHit("delete-service");
          	Statistics.addOtherApiTime("delete-service", tic);
          	
          	JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "success");
			JSON.add(msg, "deletedTriggers", deletedTriggers);
			JSON.add(msg, "deletedMappings", deletedMappings);
			JSON.add(msg, "ignoredCommands", ignoredCommands.toString());
			JSON.add(msg, "deletedFiles", deletedFiles);
			
			Debugger.println("delete-service - User '" + user.getUserID() 
				+ "' requested deletion of commands: " + String.join(", ", commandsArray), 3);
			
			return SparkJavaFw.returnResult(req, res, msg.toJSONString(), 200);
          	
		}catch (Exception e){
			Debugger.println("delete-service - FAILED due to: " + e.getMessage(), 1);
			Debugger.printStackTrace(e, 4);
			
			//stats
          	Statistics.addOtherApiHit("delete-service fail");
          	Statistics.addOtherApiTime("delete-service fail", tic);
          	
          	JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", e.getMessage());
			return SparkJavaFw.returnResult(req, res, msg.toJSONString(), 200);
		}
	}
	
	//---------------------------------------------------------------------------------
	
	/**
	 * Receive a file with proper service data.
	 * @param req - request
	 * @param reqParams - request parameters from form body
	 * @param userId - ID of user
	 * @throws IOException 
	 * @throws ServletException 
	 * @return path of stored files
	 */
	private static List<Path> receiveServiceFile(Request req, RequestParameters reqParams, String userId) throws IOException, ServletException{
		Part filePart = req.raw().getPart(UPLOAD_FILE_KEY);		//getPart needs to use same "name" as input field in form
        String fileName = getFileName(filePart);
        
        //Class file
        if (fileName.endsWith(".class")){
        	//Get right folder and make sure it exists
        	File uploadDir = new File(ConfigServices.getCustomServicesBaseFolder() + userId);
            uploadDir.mkdirs();

            //Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
            Path tempFile = Paths.get(uploadDir.toString(), fileName);
            Debugger.println("upload-service - Downloading file to: " + uploadDir.getPath(), 3);

            try (InputStream input = filePart.getInputStream()){
            	//Simply store file
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Debugger.println("upload-service - File '" + fileName + "' stored.", 3);
            return Arrays.asList(tempFile);
        
        //Java file
        }else if (fileName.endsWith(".java")){
        	//Get right folder and make sure it exists
        	File uploadDir = new File(ConfigServices.getCustomServicesBaseFolder() + userId);
            uploadDir.mkdirs();
            Debugger.println("upload-service - Compiling class(es) to: " + uploadDir.getPath(), 3);
        	
        	//Compile file to target folder
            String sourceCode;
            try (InputStream input = filePart.getInputStream()){
        		sourceCode = FilesAndStreams.getStringFromStream(input, StandardCharsets.UTF_8, "\n");
        	}
            String classNameSimple = fileName.replace(".java", "");
        	String className = ConfigServices.getCustomServicesPackage() + "." + userId + "." + classNameSimple;
    		ClassBuilder.compile(className, sourceCode, new File(Config.pluginsFolder));
    		
    		//Check stored class files
        	List<Path> tempFiles = findCustomClassAndRelated(uploadDir, classNameSimple);
        	return tempFiles;
        	
        //Wrong or not supported file
        }else{
        	throw new RuntimeException("File '" + fileName + "' is NOT A VALID (or supported) SERVICE FILE!");
        }
	}
	
	/**
	 * Find class files in upload directory that belong to a compiled class (including inner classes with $... in name). 
	 * @param uploadDir - custom services directory including user ID
	 * @param classNameSimple - simple name of class (used as base name)
	 * @return
	 */
	private static List<Path> findCustomClassAndRelated(File uploadDir, String classNameSimple){
		List<File> allClassFiles = FilesAndStreams.directoryToFileList(uploadDir.getPath(), null, false);
    	List<Path> tempFiles = new ArrayList<>();
    	for (File f : allClassFiles){
    		if (f.getName().matches(Pattern.quote(classNameSimple) + "(\\$.*|)\\.class$")){
    			tempFiles.add(f.toPath());
    			Debugger.println("upload-service - Compiled class file '" + f.getName() + "' stored.", 3);
    		}
    	}
    	return tempFiles;
	}
	
	/**
	 * Get loader for class, create new instance of class, remove all old triggers for intended command, 
	 * register custom service and return. 
	 * @param className - binary name of class (e.g. package + name + $inner) 
	 * @param user - User that this class will be registered for
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IOException 
	 */
	private static ServiceUploadResult validateAndRegisterService(String className, User user) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		//validate service
    	ServiceInterface service = (ServiceInterface) ConfigServices.addCustomServiceClassLoader(className).loadClass(className).newInstance();
    	
    	//remove all 'old' command triggers
    	ServiceInfo info = service.getInfo(user.language);
    	UserDataInterface userData = user.getUserDataAccess();
    	long deletedTriggers = userData.deletePersonalSdkCommands(user, info.intendedCommand, null);
    	//Debugger.println("upload-service - Old triggers removed: " + deletedTriggers, 3);
    	
    	//register service (again)
    	userData.registerCustomService(user, info, service);
    	return new ServiceUploadResult(service.getClass().getCanonicalName(), deletedTriggers);
	}

	//------------ helpers ------------
	
	public static class ServiceUploadResult {
		String className;
		long cleanedTriggers;
		
		public ServiceUploadResult(String className, long cleanedTriggers){
			this.className = className;
			this.cleanedTriggers = cleanedTriggers;
		}
		public String getCanonicalName(){
			return this.className;
		}
		public long getCleanedTriggers(){
			return this.cleanedTriggers;
		}
	}
	
	/**
	 * Get filename for file upload post.
	 */
	private static String getFileName(Part part) {
	    for (String cd : part.getHeader("content-disposition").split(";")) {
	        if (cd.trim().startsWith("filename")) {
	            return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
	        }
	    }
	    return null;
	}

}
