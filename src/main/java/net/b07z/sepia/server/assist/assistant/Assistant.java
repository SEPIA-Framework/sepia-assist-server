package net.b07z.sepia.server.assist.assistant;

import java.util.List;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.data.CmdMap;

/**
 * Some statics for "Config.assistant_id" and tools to influence the behavior of the assistant, like e.g. the mood
 * 
 * @author Florian Quirin
 *
 */
public class Assistant {
	
	//NAME: is in server.Statics
	
	//Some buffered values usually read from database
	public static List<CmdMap> customCommandsMap;			//note: reset in UserData_xy.registerCustomService(..)
	
	//--MOOD--
	
	/**
	 * Decreases mood by one. Returns minimum 0.
	 * @return mood integer
	 */
	public static int mood_decrease(int mood){
		if (mood>0){
			mood--;
		}
		return mood;
	}
	/**
	 * Increases mood by one. Returns max 10.
	 * @return mood integer
	 */
	public static int mood_increase(int mood){
		if (mood>=0 && mood<10){
			mood++;
		}
		return mood;
	}
	
	//--CONTEXT--
	
	/**
	 * Adds the new context to the last input context. Maximum storage is 3 contexts separated by a ";;" as delimiter.
	 * @param context - new context
	 * @param result - NLP result containing the old input context
	 * @return new context String with max. 3 contexts (...;...;...)
	 */
	public static String addContext(String context, NluResult result){
		String old_context = result.input.context;
		String[] split_context = old_context.split(";;",4);
		String new_context = "";
		if (split_context.length>=2){
			new_context = context + ";;" + split_context[0] + ";;" + split_context[1];
		}else if (split_context.length>=1){
			new_context = context + ";;" + split_context[0];
		}else{
			new_context = context;
		}
		return new_context;
	}
	/**
	 * Returns the most recent context of a set of contexts found in either NLU_Input or NLU_Result 
	 * @param contexts - String with contexts separated by ";;"
	 * @return most recent context
	 */
	public static String getContext_newest(String contexts){
		String[] split_context = contexts.split(";;",4);
		return split_context[0];
	}
	/**
	 * Returns the context with index i (starting from 0). If there is no such context returns an empty string.
	 * @param contexts - String with contexts separated by ";;" as found in NLU_Result or NLU_Input
	 * @param index - index i of contexts
	 * @return context with index i or empty string
	 */
	public static String getContext_i(String contexts, int index){
		String[] split_context = contexts.split(";;",4);
		if (split_context.length>=index){
			return split_context[index];
		}else{
			return "";
		}
	}
	//TODO: does it make sense to have these methods in 3 classes?
	/**
	 * Try to get a context by searching the last_command (this is exactly the same method used in User class).
	 * @param input - NLU_Input send to server by client
	 * @param user - we can also check the user history (can be null)
	 * @param key - PARAMETER to look for in last_command
	 * @return value for key or empty string
	 */
	public static String getLastContextParameter(NluInput input, User user, String key){
		String context = "";
		if (!input.last_cmd.isEmpty()){
			context = input.last_cmd.replaceFirst(".*"+ key + "=(.*?)(;;|$).*", "$1").trim();
		}else if (user != null){
			user.getLastContextParameter(key);
		}
		return context;
	}
	/**
	 * Get a context command by searching last_command(s) (this is exactly the same method used in User class).
	 * @param input - NLU_Input send to server by client
	 * @param user - we can also check the user history (can be null)
	 * @param cmd - CMD to look for in last_command
	 * @return cmd or empty string
	 */
	public static String getLastContextCommand(NluInput input, User user, String cmd){
		String context = "";
		if (!input.last_cmd.isEmpty()){
			if (input.last_cmd.replaceFirst(";;.*", "").trim().equals(cmd)){
				context = cmd;
			}
		}else if (user != null){
			context = user.getLastContextCommand(cmd);
		}
		return context;
	}
}
