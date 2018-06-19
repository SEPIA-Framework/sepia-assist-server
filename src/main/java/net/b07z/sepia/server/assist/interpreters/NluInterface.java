package net.b07z.sepia.server.assist.interpreters;

public interface NluInterface {
	
	String language = "en";				//interpret this language
	String context = "default";			//context of what has been said before
	String environment = "default";		//environments like phone, home, car, space shuttle, ...
	
	/**
	 * Method used to interpret the user input and return results.
	 * @param input - user input as NLU_Input class
	 * @return NLU_Result containing all information from command to parameters etc.
	 */
	public NluResult interpret(NluInput input);
	
	/**
	 * Returns a value from 0-1 indicating how certain the NL-Processor is with his result. This may or may not be accurate depending on the 
	 * NLP in use.
	 * @return double value from 0.0-1.0 where 1 is the highest certainty
	 */
	public double getCertaintyLevel(NluResult result);

}
