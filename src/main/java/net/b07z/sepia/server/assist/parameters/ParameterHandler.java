package net.b07z.sepia.server.assist.parameters;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;

public interface ParameterHandler {
	
	/**
	 * Setup the handler by adding some data (NLU_Result or NLU_Input) required to check for account specific data, context, environment, etc..
	 * <br>Note: Store the data in the class to make it accessible for the other methods.
	 */
	public void setup(NluResult nluResult);
	/**
	 * Setup the handler by adding some data (NLU_Result or NLU_Input) required to check for account specific data, context, environment, etc..
	 * <br>Note: Store the data in the class to make it accessible for the other methods.
	 */
	public void setup(NluInput nluInput);
	
	//TODO: add "isLanguageSupported(String language);
	
	/**
	 * Is this parameter using a generic handler meaning a handler that integrates into an interview properly but cannot extract data,
	 * so that it always returns VALUE=[normalized input] and INPUT_RAW=[original raw input]. 
	 */
	public default boolean isGeneric(){
		return false;
	}
	
	/**
	 * A generic handler can have custom extractions.
	 */
	public default boolean hasCustomMethods(){
		return false;
	}
	
	/**
	 * Try to extract the parameter from the given input. The result is usually the input for the 'build' method.
	 */
	public String extract(String input);
	
	/**
	 * If extraction fails you can try this method to still get a result. It is usually a non-database backed method where as "extract" is likely db-backed. 
	 * The result should be compatible input for the 'build' method.
	 */
	public String guess(String input);
	
	/**
	 * Return the exact string (not any modified version) that has been found during extraction.<br>
	 * Note: this is usually only available during the NLU interpretation phase (not anymore in the build-phase).
	 */
	public String getFound();
	
	/**
	 * Try to remove the "found" parameter from input.
	 */
	public String remove(String input, String found);
	
	/**
	 * Try to clean the result of a response without doing too much NLU. E.g.: in "search for XYZ" removes the "... for ".<br>
	 * NOTE: The response tweaker will only be used if the extract method does not give a result.<br>
	 * The result should be compatible input for the 'build' method.<br>
	 * <br>
	 * TODO: maybe the responseTweaker should be removed and its functionality placed into the 'build' method ...
	 */
	public String responseTweaker(String input);
	
	/**
	 * Build a default parameter result by transforming specific data like "my favorite restaurant", "&lt;user_home&gt;", "&lt;color tags&gt;", personal or contacts data as required. 
	 * If none of these apply just build a result out of the input. 
	 * Output of the 'extract', 'guess' and 'responseTweaker' methods can be complex (see DateAndTime) so this method needs to be able to handle that. 
	 * @return default result string in JSON format or an "action" like "add", "select", "confirm" ...
	 */
	public String build(String input);
	
	/**
	 * Check if parameter is in default format (JSON string)
	 */
	public boolean validate(String input);
	
	/**
	 * Was the parameter successfully built or is the result an action, error, comment (in the form of an API_Result)?
	 */
	public boolean buildSuccess();
}
