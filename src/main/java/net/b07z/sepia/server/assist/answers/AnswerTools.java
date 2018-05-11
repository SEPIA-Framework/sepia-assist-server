package net.b07z.sepia.server.assist.answers;

import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.tools.RandomGen;

/**
 * Tools to prepare answers.
 * 
 * @author Florian Quirin
 *
 */
public class AnswerTools {
	
	/**
	 * Typically inside the framework answers are pulled from the database linking it by giving a key like "test_0a" but the user
	 * can define his own answers and answer sets in apis or together with commands. For convenience user defined answers that don't call the database are
	 * tag-less meaning they come as plain text or start with only the tag &#60direct&#62 where as answers that refer to the database are
	 * enclosed within brackets completely. This method handles these command-specific answers and converts them to database-compatible format,
	 * it also handles the selection of random custom direct/database answers out of sets (separated by "||").<br>
	 * Typically this feature is used by passing the answer set in the command parameter "reply=..." like in chats commands.
	 * apis.Open_CustomLink is also making heavy use of this feature for answers and even question sets.
	 *  
	 * @param key_or_answer_set - a set of answer "keys" accessing the database (inside brackets) or plain direct answers
	 * @return internal standard format to put into "get_answer" method
	 */
	public static String handleUserAnswerSets(String key_or_answer_set){
		//choose a random answer if multiple answers are available
		String [] answers = key_or_answer_set.split("\\|\\|");
		String key_or_answer = answers[RandomGen.getInt(answers.length)].trim();		//random element in answer_set

		String result_key = "";
		//if the input comes in brackets <.*> it will be used to call a database link 
		if (key_or_answer.matches("^<.*>$") && !key_or_answer.matches(".+>.+$")){
			result_key = key_or_answer.replaceAll("(^<|>$)", "").trim();
			
		//if its already tagged with <direct> the question key will be replaced with a direct question which is "key" itself
		}else if (key_or_answer.matches("^<direct>.+")){
			result_key = key_or_answer;
			
		//tagged it with <direct> if it does not come inside brackets <.*>
		}else{
			result_key = "<direct>" + key_or_answer;
		}
		
		return result_key;
	}
	
	/**
	 * Replace all the user and assistant specific tags that can be obtained from the user account or server configuration like
	 * user and assistant names etc.
	 * @param answer - string to check for the tags
	 * @param info - gathered info on user as NLU_Result (including NLU_Input and input.User) 
	 * @return cleaned string
	 */
	public static String replaceSpecialTags(String answer, NluResult info){
		String userName = info.input.user.getName(Config.superuserApiMng);
		answer = answer.replaceAll("<user_name>", userName);
		answer = answer.replaceAll("<name>", Config.assistantName);
		return answer;
	}
	
	/**
	 * Clean the HTML answer from things like vocal smileys. Intended to be used AFTER answer_clean has been assigned
	 * and you want to change how the original answer is displayed in the chat.
	 */
	public static String cleanHtmlAnswer(String input){
		String output = input;
		//remove vocal smileys
		output = output.replaceAll("#\\w+\\d\\d#", "").trim();
		return output;
	}

}
