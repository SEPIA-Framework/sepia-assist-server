package net.b07z.sepia.server.assist.endpoints;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.json.simple.JSONArray;
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
import net.b07z.sepia.server.core.tools.Is;
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
	public static boolean storeSourceCodeAfterCompile = true;
	
	public static final String UPLOAD_FILE_KEY = "upload_file";
	public static final String UPLOAD_CODE_KEY = "upload_code";
	public static final String UPLOAD_CODE_CLASS_NAME = "upload_code_class_name"; 	//simple class name

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
	   		  "<style>"
	   				+ "#upload-service-box *{margin:5px;font-family:sans-serif;text-align:center;}"
	   				+ "#code-box {width:80%; min-height:100px; overflow:auto; margin:8px; padding:8px; text-align:left; white-space:nowrap;}"
	   		+ "</style>"
	   		+ "<body><div id='upload-service-box'>"
	   		 	+ "<h3>SEPIA Smart-Service Upload Interface</h3>"
	   		 	+ "<br>"
	   		 	+ "<form id='upload-form' action='" + uploadEndpointPath + "' method='post' enctype='multipart/form-data' target='_blank'>"
	   		 		+ "<label>Id: <input type='text' name='GUUID'></label>"
	   		 		+ "<label>Pwd: <input type='password' name='PWD'></label><br>"
	   		 		+ "<br>"
	   		 		+ "<input type='file' name='" + UPLOAD_FILE_KEY + "' accept='.class,.java,.sservice,.yaml'><br>"
	   		 		+ "<br>Or use:<br><br>"
	   		 		+ "<label>Class name: <input type='text' name='" + UPLOAD_CODE_CLASS_NAME + "'></label><br>"
	   		 		+ "<br>"
	   		 		+ "<label>Java source code: <br>"
	   		 		+ "<textarea id='code-box' form='upload-form' name='" + UPLOAD_CODE_KEY + "'>Add source code here ...</textarea>"
	   		 		+ "<br>"
	   		 		+ "<button class='interface_button' type='submit'>UPLOAD SERVICE</button>"
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
		//?? - required to read parameters properly 
	    req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
	    
	    //this endpoint requires a certain content type
	    String contentType = req.headers("Content-type");
	    if (!contentType.toLowerCase().contains("multipart/form-data")){
	    	JSONObject result = new JSONObject();
			JSON.add(result, "result", "fail");
			JSON.add(result, "error", "endpoint requires content-type 'multipart/form-data' but saw: " + contentType);
			return SparkJavaFw.returnResult(req, res, result.toJSONString(), 400);
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
		
		long tic = System.currentTimeMillis();
		try{
			//Check upload type
			RequestParameters params = new RequestGetOrFormParameters(req);
			String sourceCode = params.getString(UPLOAD_CODE_KEY);
			String sourceCodeClassName = null;
			boolean isSourceCodeTransfer = false;
			if (Is.notNullOrEmpty(sourceCode)){
				sourceCodeClassName = params.getString(UPLOAD_CODE_CLASS_NAME);
				if (sourceCodeClassName != null){
					isSourceCodeTransfer = true;
					//System.out.println("source-code: " + sourceCode);					//DEBUG
					//System.out.println("source-code-class: " + sourceCodeClassName);	//DEBUG
				}
			}
					
			String fileName = null;
			String classBaseName = null;
			try{
				//v1 - compile source code and store class file(s)
				if (isSourceCodeTransfer){
					classBaseName = sourceCodeClassName;
					fileName = sourceCodeClassName + ".class"; 		//this is what it will be after compilation
					compileServiceSourceCode(userId, sourceCodeClassName, sourceCode, storeSourceCodeAfterCompile);
				
				//v2 - receive and store file or compile and class file(s)
				}else{
					fileName = receiveServiceFile(req, params, userId);
					classBaseName = ClassBuilder.getSimpleClassNameFromFileName(fileName);
				}
				
	        	//validate and register service
	        	String className = ConfigServices.getCustomServicesPackage() + "." + userId + "." + classBaseName;
	        	ServiceUploadResult uploadRes = validateAndRegisterService(className, user);
	        	
	        	//stats
	          	Statistics.addOtherApiHit("upload-service");
	          	Statistics.addOtherApiTime("upload-service", tic);
	          	
	          	return SparkJavaFw.returnResult(req, res, JSON.make(
	          		"result", "success", 
	          		"message", "'" + classBaseName + "' has been uploaded for user: " + userId,
	          		"baseClassName", classBaseName,
	          		"canonicalClassName", uploadRes.getCanonicalName(),
	          		"userId", userId,
	          		"removedOldTriggers", uploadRes.getCleanedTriggers()
	          	).toJSONString(), 200);
	        	
	        }catch (Exception e){
	        	Debugger.println("upload-service - Issue in code transfer, compilation or validation: " + e.getMessage(), 1);
		      	Debugger.printStackTrace(e, 2);
	        	try{
	        		//Get file name again?
	        		if (!isSourceCodeTransfer && fileName == null){
	        			Part filePart = req.raw().getPart(UPLOAD_FILE_KEY);
	        			fileName = getFileName(filePart);
	        			classBaseName = ClassBuilder.getSimpleClassNameFromFileName(fileName);
	        		}
	        		File uploadDir = new File(ConfigServices.getCustomServicesBaseFolder() + userId);
	        		
	        		//Check stored class files and clean up
		        	List<Path> tempFiles = findCustomClassAndRelated(uploadDir, classBaseName);
	        		for (Path p : tempFiles){
	        			Files.delete(p);
	        			Debugger.println("upload-service - File '" + p.toString() + "' removed, was no proper service!", 3);
	        		}
	        	}catch (Exception e1) {
	        		Debugger.println("upload-service - File '" + fileName + "' (or related file) NOT removed: " + e1.getMessage(), 1);
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
			return SparkJavaFw.returnResult(req, res, JSON.make("result", "fail", "error", errorMsg).toJSONString(), 400);
		}
	}
	
	/**-- GET SERVICES POST --
	 * End-point to get all custom services of a user.  
	 */
	public static String getServicesPost(Request req, Response res){
		RequestParameters params = new RequestPostParameters(req);
		
		//authenticate
		Authenticator token = Start.authenticate(params, req);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(req, res, token.getErrorCode());
		}
		//create user
		User user = new User(null, token);
		//String userId = user.getUserID();
		
		long tic = System.currentTimeMillis();
		try{
			//get command mappings
			UserDataInterface userData = user.getUserDataAccess();
			List<CmdMap> customMap = userData.getCustomCommandMappings(user, null);
			if (customMap == null){
				throw new RuntimeException("Failed to load custom command mappings. Reason: unknown. Try again or check database please.");
			}
			
			JSONArray customCommandsAndServices = new JSONArray();
			for (CmdMap cmdMap : customMap){
				JSON.add(customCommandsAndServices, JSON.make(
						"command", cmdMap.getCommand(),
						"services", cmdMap.getServices()
				));
			}
			
			//stats
	      	Statistics.addOtherApiHit("get-services");
	      	Statistics.addOtherApiTime("get-services", tic);
	      	
	      	JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "success");
			JSON.add(msg, "commandsAndServices", customCommandsAndServices);
			return SparkJavaFw.returnResult(req, res, msg.toJSONString(), 200);
			
		}catch (Exception e){
			//stats
	      	Statistics.addOtherApiHit("get-services fail");
	      	Statistics.addOtherApiTime("get-services fail", tic);
	      	
	      	JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", e.getMessage());
			return SparkJavaFw.returnResult(req, res, msg.toJSONString(), 200);
		}
	}
	/**-- GET SERVICE SOURCE CODE POST --
	 * End-point to get source code of specific service of a user.  
	 */
	public static String getServiceSourceCodePost(Request req, Response res){
		RequestParameters params = new RequestPostParameters(req);
		
		//authenticate
		Authenticator token = Start.authenticate(params, req);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(req, res, token.getErrorCode());
		}
		//create user
		User user = new User(null, token);
		//String userId = user.getUserID();
		
		long tic = System.currentTimeMillis();
		try{
			//get source code from file
			String serviceName = params.getString("service");
			if (Is.nullOrEmpty(serviceName)){
				JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "fail");
				JSON.add(msg, "error", "missing 'service' parameter");
				return SparkJavaFw.returnResult(req, res, msg.toJSONString(), 200);
			}
			
			File sourceFile = new File(ConfigServices.getCustomServicesSourceFolder() + user.getUserID() + "/" + serviceName + ".java");
			if (!sourceFile.exists()){
				throw new RuntimeException("No source file found for '" + (user.getUserID() + "/" + serviceName) + "'");
			
			}else{
				String sourceCode = String.join("\n", FilesAndStreams.readFileAsList(sourceFile.getAbsolutePath()));
				if (Is.nullOrEmpty(sourceCode)){
					throw new RuntimeException("Could not load source code, result was corrupted or empty");
				}
				//stats
		      	Statistics.addOtherApiHit("get-service-source");
		      	Statistics.addOtherApiTime("get-service-source", tic);
		      	
		      	JSONObject msg = new JSONObject();
				JSON.add(msg, "result", "success");
				JSON.add(msg, "sourceCode", sourceCode);
				return SparkJavaFw.returnResult(req, res, msg.toJSONString(), 200);
			}
			
		}catch (Exception e){
			//stats
	      	Statistics.addOtherApiHit("get-service-source fail");
	      	Statistics.addOtherApiTime("get-service-source fail", tic);
	      	
	      	JSONObject msg = new JSONObject();
			JSON.add(msg, "result", "fail");
			JSON.add(msg, "error", e.getMessage());
			return SparkJavaFw.returnResult(req, res, msg.toJSONString(), 200);
		}
	}
	
	/**-- DELETE SERVICE POST --
	 * End-point to delete a custom service and clean up.  
	 */
	public static String deleteServicePost(Request req, Response res){
		RequestParameters params = new RequestPostParameters(req);
		
		//authenticate
		Authenticator token = Start.authenticate(params, req);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(req, res, token.getErrorCode());
		}
		//create user
		User user = new User(null, token);
		String userId = user.getUserID();
		
		//check role
		//not required ... a user should always be allowed to delete his services

		long tic = System.currentTimeMillis();
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
			long deletedSources = 0;
			long classFileErrors = 0;
			long sourceFileErrors = 0;
			if (servicesArray != null && servicesArray.length > 0){
				File uploadDir = new File(ConfigServices.getCustomServicesBaseFolder() + userId);
				for (String className : servicesArray){
					//Check stored class files and clean up
					try{
			        	List<Path> tempFiles = findCustomClassAndRelated(uploadDir, className);
		        		for (Path p : tempFiles){
		        			Files.delete(p);
		        			Debugger.println("delete-service - Class file '" + p.toString() + "' removed during clean-up!", 3);
		        			deletedFiles++;
		        		}
		        	}catch (Exception e1){
		        		Debugger.println("delete-service - Error during deletion of class file '" + userId + "/" + className + "'!", 1);
		        		classFileErrors++;
		        	}
					//check source code file
					try{
						File sourceFile = new File(ConfigServices.getCustomServicesSourceFolder() + userId + "/" + className + ".java");
						if (sourceFile.exists()){
							try{
								sourceFile.delete();
								Debugger.println("delete-service - Source file '" + sourceFile.getPath() + "' removed during clean-up!", 3);
								deletedSources++;
							}catch (Exception e2){
								Debugger.println("delete-service - Error during deletion of source file '" + userId + "/" + className + "'!", 1);
								sourceFileErrors++;
							}
						}
					}catch (Exception e){
						Debugger.printStackTrace(e, 3);
					}
				}
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
			JSON.add(msg, "deletedClassFiles", deletedFiles);
			JSON.add(msg, "deletedSourceFiles", deletedSources);
			JSON.add(msg, "classFileErrors", classFileErrors);
			JSON.add(msg, "sourceFileErrors", sourceFileErrors);
			
			Debugger.println("delete-service - User '" + userId 
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
	 * @return file name
	 */
	private static String receiveServiceFile(Request req, RequestParameters reqParams, String userId) throws IOException, ServletException{
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
        
        //Java file
        }else if (fileName.endsWith(".java")){
        	//Get source code
        	String sourceCode;
            try (InputStream input = filePart.getInputStream()){
        		sourceCode = FilesAndStreams.getStringFromStream(input, StandardCharsets.UTF_8, "\n");
        	}
            String classNameSimple = fileName.replace(".java", "");
            
        	//Compile code (or throw exception)
            compileServiceSourceCode(userId, classNameSimple, sourceCode, storeSourceCodeAfterCompile);
        	
        //Wrong or not supported file
        }else{
        	throw new RuntimeException("File '" + fileName + "' is NOT A VALID (or supported) SERVICE FILE!");
        }
        
        return fileName;
	}
	
	/**
	 * Compile source code of custom service and store class files in default folder.
	 * @param userId - ID of user
	 * @param simpleClassName - simple name of class (without any packages or modifiers)
	 * @param sourceCode - source code as String (with proper line-breaks)
	 * @param storeSource - store source code as file in sub-folder "services-source-code" (next to "services")
	 */
	private static void compileServiceSourceCode(String userId, String simpleClassName, String sourceCode, boolean storeSource){
		//Get right folder and make sure it exists
    	File uploadDir = new File(ConfigServices.getCustomServicesBaseFolder() + userId);
        uploadDir.mkdirs();
        Debugger.println("upload-service - Compiling class(es) to: " + uploadDir.getPath(), 3);
    	
    	//Compile file to target folder
    	String className = ConfigServices.getCustomServicesPackage() + "." + userId + "." + simpleClassName;
		String errors = ClassBuilder.compile(className, sourceCode, new File(Config.pluginsFolder));
		if (!errors.isEmpty()){
			throw new RuntimeException("Class '" + simpleClassName + "' - " + errors);
		}
		
		//Store source?
		if (storeSource){
			String sourceCodePath = ConfigServices.getCustomServicesSourceFolder() + userId;
			String sourceFileName = simpleClassName + ".java";
			boolean sourceWriteSuccess;
			try{
				Files.createDirectories(Paths.get(sourceCodePath));
				sourceWriteSuccess = FilesAndStreams.writeStringToFile(sourceCodePath, sourceFileName, sourceCode);
			}catch (Exception e){
				Debugger.printStackTrace(e, 3);
				sourceWriteSuccess = false;
			}
			if (sourceWriteSuccess){
				Debugger.println("upload-service - Stored source code at: " + sourceCodePath + "/" + sourceFileName, 3);
			}else{
				Debugger.println("upload-service - Failed to write source code to: " + sourceCodePath + "/" + sourceFileName, 1);
			}
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
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 */
	private static ServiceUploadResult validateAndRegisterService(String className, User user) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		//validate service
    	ServiceInterface service = (ServiceInterface) ConfigServices.addCustomServiceClassLoader(className)
    			.loadClass(className).getDeclaredConstructor().newInstance();
    	
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
