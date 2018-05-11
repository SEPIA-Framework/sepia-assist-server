package net.b07z.sepia.server.assist.endpoints;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.assist.users.Authenticator;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.users.UserDataInterface;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.server.SparkJavaFw;
import net.b07z.sepia.server.core.tools.Debugger;
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
	
	public static String uploadEndpointPath = "/upload-service";		//keep this in sync with the endpoint definitions

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
	   		 		+ "<input type='file' name='uploaded_file' accept='.class'><br>"
	   		 		+ "<button class='interface_button'>UPLOAD SERVICE</button>"
	   		 	+ "</form>"
	        + "</div></body>";
	   	//stats
	  	Statistics.addOtherApiHit("uploadService form");
	  	Statistics.addOtherApiTime("uploadService form", tic);
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
		
		//authenticate
		Authenticator token = Start.authenticate(req);
		if (!token.authenticated()){
			return SparkJavaFw.returnNoAccess(req, res, token.getErrorCode());
		}
		//create user
		User user = new User(null, token);
		String userId = user.getUserID();
		
		//check role
		if (!user.hasRole(Role.developer)){
			Debugger.println("unauthorized access attempt to uploadService endpoint! User: " + userId, 3);
			return SparkJavaFw.returnNoAccess(req, res);
		}
		try{
	        Part filePart = req.raw().getPart("uploaded_file");		//getPart needs to use same "name" as input field in form
	        String fileName = SdkEndpoint.getFileName(filePart);
	        if (!fileName.endsWith(".class")){
	        	throw new RuntimeException("NOT A CLASS FILE!");
	        }
	        
	    	String dir = Config.pluginsFolder + "services/" + userId;
	    	File uploadDir = new File(dir);
	        uploadDir.mkdir(); // create the upload directory if it doesn't exist
	
	        //Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
	        Path tempFile = Paths.get(uploadDir.toString(), fileName);
	        Debugger.println("uploadService - Uploading file to " + dir + "/" + tempFile.getFileName(), 3);
	
	        try (InputStream input = filePart.getInputStream()){
	            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
	        }
	        Debugger.println("uploadService - File '" + fileName + "' saved as '" + tempFile.toString() + "'", 3);
	        
	        try{
	        	//validate service
	        	String className = ConfigServices.getCustomPackage() + "." + userId + "." + fileName.replaceFirst("\\.class", "");
	        	ApiInterface service = (ApiInterface) ConfigServices.addCustomClassLoader(className).loadClass(className).newInstance();
	        	//loader.close();
	        	
	        	//remove all 'old' commands
	        	ApiInfo info = service.getInfo(user.language);
	        	UserDataInterface userData = user.getUserDataAccess();
	        	long deletedTriggers = userData.deletePersonalSdkCommands(user, info.intendedCommand, null);
	        	
	        	//register service (again)
	        	userData.registerCustomService(user, info, service);
	        	
	        	//stats
	          	Statistics.addOtherApiHit("uploadService");
	          	Statistics.addOtherApiTime("uploadService", tic);
	          	
	          	return service.getClass().getCanonicalName() + " has been uploaded! (old triggers removed: " + deletedTriggers + ")";
	        	
	        }catch (Exception e){
	        	Debugger.println("uploadService - " + e.getMessage(), 1);
		      	Debugger.printStackTrace(e, 2);
	        	try{ 
	        		//loader.close();
	        		Files.delete(tempFile);
	        		Debugger.println("uploadService - File '" + fileName + "' removed, was no proper service!", 3);
	        	}catch (Exception e1) {}
	        	throw new RuntimeException(e);
	        }
		
		}catch(Exception e){
			Debugger.println("uploadService - " + e.getMessage(), 1);
	      	//Debugger.printStackTrace(e, 2);
	      	
			//stats
	      	Statistics.addOtherApiHit("uploadService fail");
	      	Statistics.addOtherApiTime("uploadService fail", tic);
	      	
	      	String errorMsg = "ERROR: " + e.getMessage() 
				+ "<br><br>Check-list:"
				+ "<ul>"
					+ "<li>Is the package name of my class: services." + userId +"?</li>"
					+ "<li>Does my class implement the API_Interface properly?</li>"
					+ "<li>Did I set the 'intendedCommand' variable inside my class with format: user_id.command?</li>"
				+ "</ul>";
	      	return errorMsg;
		}
	}

	//------------ helpers ------------
	
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
