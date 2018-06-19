package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;

/**
 * Class to collect a parameter handler result and store it, e.g. in NLU_Input
 * 
 * @author Florian Quirin
 *
 */
public class ParameterResult {

	String name;
	String extracted;
	HashMap<String, String> extractedStringMap;
	Object extractedObject;
	String found;
	
	public ParameterResult(String name, String extracted, String found){
		this.name = name;
		this.extracted = extracted;
		this.found = found;
	}
	public ParameterResult(String name, Object extractedObject, String found){
		this.name = name;
		this.extractedObject = extractedObject;
		this.found = found;
	}
	public ParameterResult(String name, HashMap<String, String> extractedStringMap, String found){
		this.name = name;
		this.extractedStringMap = extractedStringMap;
		this.found = found;
	}
	
	public String getName(){
		return name;
	}
	
	public String getExtracted(){
		return extracted;
	}
	public HashMap<String, String> getExtractedMap(){
		return extractedStringMap;
	}
	public Object getExtractedObject(){
		return extractedObject;
	}
	
	public String getFound(){
		return found;
	}
	
	/**
	 * Return existing result or extract result of a given parameter.
	 * @param nluInput - NLU_Input that hold necessary info
	 * @param pName - name of the parameter to extract (as seen in PARAMETERS.XYZ)
	 * @param input - text to look for parameter
	 */
	public static ParameterResult getResult(NluInput nluInput, String pName, String input){
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(pName);
		if (pr != null){
			
			//TODO: make an input comparison first to check if its the same input as it was when cached?
			
			return pr;
		}
		
		Parameter p = new Parameter(pName);
		Parameter_Handler handler = p.getHandler();
		handler.setup(nluInput);
		
		String ex = handler.extract(input);
		String found = handler.getFound();
		pr = new ParameterResult(pName, ex, found);
		
		return pr;
	}
	
	/**
	 * Build the parameter handler, extract the result and remove it from input.
	 * @param nluInput - NLU_Input that hold necessary info
	 * @param pName - name of the parameter to extract (as seen in PARAMETERS.XYZ)
	 * @param input - text to look for parameter
	 * @param toClean - text to clean of parameter
	 * @return clean input or original
	 */
	public static String cleanInputOfParameter(NluInput nluInput, String pName, String input, String toClean){
		Parameter p = new Parameter(pName);
		Parameter_Handler handler = p.getHandler();
		handler.setup(nluInput);
		
		//TODO: add storage check here? Or do we want to force a clean extraction?
		
		handler.extract(input);
		String found = handler.getFound();
		if (!found.isEmpty()){
			return handler.remove(toClean, found);
		}else{
			return input;
		}
	}
	/**
	 * Submit found result, build the parameter handler and remove it from input.
	 * @param nluInput - NLU_Input that hold necessary info
	 * @param pName - name of the parameter to extract (as seen in PARAMETERS.XYZ)
	 * @param pr - ParameterResult of previous extraction process
	 * @param toClean - text to clean of parameter
	 * @return clean input or original 'toClean'
	 */
	public static String cleanInputOfFoundParameter(NluInput nluInput, String pName, ParameterResult pr, String toClean){
		Parameter p = new Parameter(pName);
		Parameter_Handler handler = p.getHandler();
		handler.setup(nluInput);
		
		String found = pr.getFound();
		if (!found.isEmpty()){
			return handler.remove(toClean, found);
		}else{
			return toClean;
		}
	}
}
