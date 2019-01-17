package net.b07z.sepia.server.assist.services;

import java.util.TreeSet;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.services.ServiceBuilder;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.assist.services.ServiceResult;
import net.b07z.sepia.server.assist.services.ServiceInfo.Content;
import net.b07z.sepia.server.assist.services.ServiceInfo.Type;
import net.b07z.sepia.server.core.assistant.PARAMETERS;

/**
 * A service to connect to a SEPIA Mesh-Node server to e.g. call plugins.
 * This service is usually only called with direct-commands not text input (but it could ...).
 * 
 * @author Florian Quirin
 *
 */
public class MeshNodeConnector implements ServiceInterface{
	
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
		
		//get parameters
		String nodeUrl = (String) nluResult.getOptionalParameter(PARAMETERS.MESH_NODE_URL, "")
							.getDataFieldOrDefault(InterviewData.INPUT_RAW);
		String nodePluginName = (String) nluResult.getOptionalParameter(PARAMETERS.MESH_NODE_PLUGIN_NAME, "")
							.getDataFieldOrDefault(InterviewData.INPUT_RAW);
		String nodePluginData = (String) nluResult.getOptionalParameter(PARAMETERS.MESH_NODE_PLUGIN_DATA, "")
							.getDataFieldOrDefault(InterviewData.INPUT_RAW);
		
		String replySuccess = (String) nluResult.getOptionalParameter(PARAMETERS.REPLY_SUCCESS, "")
							.getDataFieldOrDefault(InterviewData.INPUT_RAW);
		String replyFail = (String) nluResult.getOptionalParameter(PARAMETERS.REPLY_FAIL, "")
							.getDataFieldOrDefault(InterviewData.INPUT_RAW);
		
		//Call plugin
		
		//TODO: implement
		//mesh_node_plugin
		//node_url, node_plugin_name, node_plugin_data, reply_success, reply_fail
		System.out.println("Mesh-Node service URL: " + nodeUrl);
		System.out.println("Mesh-Node service nodePluginName: " + nodePluginName);
		System.out.println("Mesh-Node service nodePluginData: " + nodePluginData);
		System.out.println("Mesh-Node service replySuccess: " + replySuccess);
		System.out.println("Mesh-Node service replyFail: " + replyFail);
		
		//all good
		api.setStatusSuccess();
		
		//build the API_Result
		ServiceResult result = api.buildResult();
		return result;
	}
}
