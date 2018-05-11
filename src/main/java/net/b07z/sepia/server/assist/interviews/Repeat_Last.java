package net.b07z.sepia.server.assist.interviews;

import net.b07z.sepia.server.assist.apis.ApiResult;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;

/**
 * The API to reconstruct the last command and sends it again.
 * Basically its not a real API but just result switcher and thus has to be used before 
 * handling all other results.<br>
 * Note: There is no last_context, last_environment, last_mood, etc. yet so this might actually
 * give different results when the previous result was based on context for example.
 * 
 * @author Florian Quirin
 *
 */
public class Repeat_Last {

	public static NluResult reload(NluResult NLU_result) {
		
		String cmd_summary = NLU_result.input.last_cmd;
		
		//System.out.println("repeat old command: " + cmd_summary);		//debug
		
		NluResult new_result;
		if (cmd_summary.matches("")){
			//no last command available
			new_result = null;
		}else{
			//reconstruct last command
			new_result = NluTools.cmd_summary_to_result(NLU_result.input, cmd_summary);
			
			//TODO: this is in principle not complete with a restoration of all old parameters like context, environment, mood, etc...
		}
		
		//System.out.println("reconstructed cmd_sum: " + new_result.cmd_summary);		//debug
		//System.out.println("reconstructed command: " + new_result.get_command());	//debug
		
		return new_result;
	}
	
	public static ApiResult failed(NluResult NLU_result) {
		ApiResult result = NoResult.get(NLU_result, "repeat_0a");
		return result;
	}
}
