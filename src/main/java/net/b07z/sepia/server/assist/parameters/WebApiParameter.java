package net.b07z.sepia.server.assist.parameters;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.interviews.InterviewData;
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
		
		//TODO: call endpoint and parse result
		
		//store it
		pr = new ParameterResult(parameterName, extracted, found);
		this.nluInput.addToParameterResultStorage(pr);
		
		return input;
	}

	@Override
	public String build(String input){
		String foundDuringExtraction = "";
		String valueDefinedDuringExtraction = ""; 	//could be a generalized value for example
		
		//build default result
		JSONObject itemResultJSON = JSON.make(
				InterviewData.INPUT_RAW, nluInput.textRaw,
				InterviewData.VALUE, valueDefinedDuringExtraction,
				InterviewData.FOUND, foundDuringExtraction
		);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}
}
