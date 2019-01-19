package net.b07z.sepia.server.assist.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.endpoints.AuthEndpoint;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.StringTools;

/**
 * A service to connect to a SEPIA Mesh-Node server to e.g. call plugins.
 * This service is usually only called with direct-commands not text input (but it could ...).
 * 
 * @author Florian Quirin
 *
 */
public class MeshNodeConnector implements ServiceInterface{
	
	public static final String MESH_NODE_PLUGIN_PACKAGE = "net.b07z.sepia.server.mesh.plugins";
	public static final String MESH_NODE_PLUGIN_STATUS_KEY = "status";
	public static final String SUCCESS = "success";
	public static final String FAIL = "fail";
	
	//Define some sentences for testing:
	
	@Override
	public TreeSet<String> getSampleSentences(String lang){
		TreeSet<String> samples = new TreeSet<>();
		return samples;
	}
	
	//Basic service setup:

	@Override
	public ServiceInfo getInfo(String language) {
		//Type of service (for descriptions, choose what you think fits best)
		ServiceInfo info = new ServiceInfo(Type.program, Content.data, false);
		
		//Parameters (we use only optional and take care of the rest ourself)
		Parameter p1 = new Parameter(PARAMETERS.MESH_NODE_URL);
		Parameter p2 = new Parameter(PARAMETERS.MESH_NODE_PLUGIN_NAME);
		Parameter p3 = new Parameter(PARAMETERS.MESH_NODE_PLUGIN_DATA);
		Parameter p4 = new Parameter(PARAMETERS.REPLY_SUCCESS);
		Parameter p5 = new Parameter(PARAMETERS.REPLY_FAIL);
		info.addParameter(p1).addParameter(p2).addParameter(p3).addParameter(p4).addParameter(p5);
		
		//Default answers
		info.addSuccessAnswer("ok_0c")
			.addFailAnswer("error_0a")
			.addOkayAnswer("default_not_possible_0a");
		
		return info;
	}
	
	@Override
	public ServiceResult getResult(NluResult nluResult) {
		//initialize result
		ServiceBuilder api = new ServiceBuilder(nluResult, 
				getInfoFreshOrCache(nluResult.input, this.getClass().getCanonicalName()));
		
		//TODO: add a USER ROLE restriction?
		
		//get parameters
		String nodeUrl = (String) nluResult.getOptionalParameter(PARAMETERS.MESH_NODE_URL, "")
							.getDataFieldOrDefault(InterviewData.VALUE);
		String nodePluginName = (String) nluResult.getOptionalParameter(PARAMETERS.MESH_NODE_PLUGIN_NAME, "")
							.getDataFieldOrDefault(InterviewData.VALUE);
		String nodePluginData = (String) nluResult.getOptionalParameter(PARAMETERS.MESH_NODE_PLUGIN_DATA, "")
							.getDataFieldOrDefault(InterviewData.VALUE);
		
		String replySuccess = (String) nluResult.getOptionalParameter(PARAMETERS.REPLY_SUCCESS, "")
							.getDataFieldOrDefault(InterviewData.VALUE);
		String replyFail = (String) nluResult.getOptionalParameter(PARAMETERS.REPLY_FAIL, "")
							.getDataFieldOrDefault(InterviewData.VALUE);
		
		//Check parameters:
		
		//has URL and plugin name?
		if (Is.nullOrEmpty(nodeUrl) || Is.nullOrEmpty(nodePluginName)){
			//ABORT (missing parameters)
			api.setStatusFail();
			ServiceResult result = api.buildResult();
			return result;
		}
		//is name canonical?
		if (!nodePluginName.contains(".")){
			//auto-complete name
			nodePluginName = MESH_NODE_PLUGIN_PACKAGE + "." + nodePluginName;
		}
		//is data JSON?
		JSONObject nodePluginDataJson;
		if (Is.notNullOrEmpty(nodePluginData) && nodePluginData.startsWith("{")){
			nodePluginDataJson = JSON.parseString(nodePluginData);
		}else{
			nodePluginDataJson = new JSONObject();
		}
		//add some default info
		if (!nodePluginDataJson.containsKey("language")){
			JSON.put(nodePluginDataJson, "language", nluResult.language);
		}
		if (!nodePluginDataJson.containsKey("client")){
			JSON.put(nodePluginDataJson, "client", nluResult.input.clientInfo);
		}
		if (!nodePluginDataJson.containsKey("environment")){
			JSON.put(nodePluginDataJson, "environment", nluResult.environment);
		}
		
		//Call plugin:
		
		//Prepare a special, temporary auth. token
		AuthEndpoint.TemporaryToken tToken;
		try{
			//If the plugin uses authentication as security step it can use this token to "allow" the call.
			//NOTE: this will only work on servers with SAME CLUSTER KEY when sent back to an Assist-API!
			tToken = new AuthEndpoint.TemporaryToken(nluResult.input.user);
			
		}catch (Exception e){
			//ABORT (failed to create token ... this should never happen O_o)
			Debugger.println("FAILED to create temporary auth. key!", 1);
			Debugger.printStackTrace(e, 3);
			api.setStatusFail();
			ServiceResult result = api.buildResult();
			return result;
		}
		
		//build request body:
		
		//-authentication data
		JSONObject requestBody = JSON.make(
				"client", nluResult.input.clientInfo,
				"tToken", tToken.getJson()
		);
		//-plugin data
		JSON.put(requestBody, "canonicalName", nodePluginName);
		JSON.put(requestBody, "data", nodePluginDataJson);
		
		String requestBodyString = requestBody.toJSONString();
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		headers.put("Content-Length", Integer.toString(requestBodyString.getBytes().length));
		
		JSONObject nodeResponse = Connectors.httpPOST(
				nodeUrl + "/execute-plugin", 
				requestBodyString, 
				headers
		);
		if (!Connectors.httpSuccess(nodeResponse)){
			//ABORT (communication problems)
			Debugger.println("Mesh-Node communication failed with response: " + nodeResponse.toJSONString(), 1);
			api.setStatusFail();
			ServiceResult result = api.buildResult();
			return result;
		}
		
		//Get data and answer:

		JSONObject nodeResponseData = JSON.getJObject(nodeResponse, "data");
		String nodeResStatus = JSON.getString(nodeResponseData, MESH_NODE_PLUGIN_STATUS_KEY);
		String answer = "";
		boolean success = true;
		if (nodeResStatus.equals(SUCCESS)){
			//SUCCESS
			success = true;
			if (Is.notNullOrEmpty(replySuccess)){
				answer = replySuccess;
			}
		}else{
			//FAIL
			success = false;
			if (Is.notNullOrEmpty(replyFail)){
				answer = replyFail;
			}
		}
		//replace variables
		if (!answer.isEmpty()){
			List<String> answerTags = StringTools.findAllRexEx(answer, "<result_.*?>");
			for (String tag : answerTags){
				String tagClean = tag.replaceFirst("<result_(.*?)>", "$1").trim();
				String value = JSON.getObject(nodeResponseData, tagClean.split("\\.")).toString();
				answer = answer.replaceFirst("(<result_.*?>)", value);
			}
			api.setCustomAnswer("<direct>" + answer);
		}
		
		//Build:
		if (success){
			//all good
			api.setStatusSuccess();
		}else{
			//failed (but not error so its 'okay')
			api.setStatusOkay();
		}
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
}
