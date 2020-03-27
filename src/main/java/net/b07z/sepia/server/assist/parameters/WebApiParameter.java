package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interviews.Interview;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A parameter that uses a web API to extract the result.<br>
 * NOTE: Extend this class and overwrite 'getApiUrl()' to use it as your custom parameter! 
 * 
 * @author Florian Quirin
 *
 */
public abstract class WebApiParameter extends CustomParameter implements ParameterHandler {
	
	/**
	 * Get URL of the web API. 
	 * NOTE: we use this instead a constructor because the system will always create custom classes without submitting parameters. 
	 */
	public abstract String getApiUrl();

	@Override
	public String extract(String input){
		//define name
		String parameterName = this.getClass().getName();
		String extracted = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(parameterName);
		if (pr != null){
			this.found = pr.getFound();
			return pr.getExtracted();
		}
		
		//call endpoint and parse result
		JSONObject response = Connectors.httpPOST(getApiUrl(), this.nluInput.getJson().toJSONString(), null);
		if (Connectors.httpSuccess(response)){
			//System.out.println(response.toJSONString());						//DEBUG
			//System.out.println(nluResult.getBestResultJSON().toJSONString());	//DEBUG
			String result = JSON.getStringOrDefault(response, "result", "fail");
			if (!result.equals("fail")){
				response.remove("result");
				extracted = Interview.INPUT_EXTRACTED + ";;" + response.toJSONString();
			}else{
				return "";
			}
		}else{
			Debugger.println("WebApiParameter - extract FAILED with msg.: " + response.toJSONString(), 1);
			return "";
		}
		
		//store it
		pr = new ParameterResult(parameterName, extracted, this.found);
		this.nluInput.addToParameterResultStorage(pr);
		
		return extracted;
	}

	@Override
	public String build(String input){
		//extract again/first? - this should only happen via predefined parameters (e.g. from direct triggers)
		if (Is.nullOrEmpty(input)){
			return "";
		}
		if (!input.startsWith("<")){
			input = extract(input);
			if (Is.nullOrEmpty(input)){
				return "";
			}
		}
		try {
			JSONObject extJson = JSON.parseStringOrFail(input.split(";;", 2)[1]);				
			
			//build default result - we could just use 'extJson' but this gives us more control ...
			JSONObject itemResultJSON = JSON.make(
					InterviewData.INPUT_RAW, this.nluInput.textRaw,
					InterviewData.FOUND, extJson.get(InterviewData.FOUND),				//exact match found during extraction - String
					InterviewData.VALUE, extJson.get(InterviewData.VALUE),				//could be a generalized value for example - String
					InterviewData.VALUE_LOCAL, extJson.get(InterviewData.VALUE_LOCAL),	//local translation of given value for answers etc. - String
					InterviewData.EXTRAS, extJson.get(InterviewData.EXTRAS)				//custom extra data as required - JSON object
			);
			this.buildSuccess = true;
			return itemResultJSON.toJSONString();
		
		}catch(Exception ex){
			Debugger.println("WebApiParameter - FAILED to create build result! Error: " + ex.getMessage(), 1);
			Debugger.printStackTrace(ex, 5);
			return "";
		}
	}
}
