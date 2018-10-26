package net.b07z.sepia.server.assist.answers;

import net.b07z.sepia.server.assist.answers.AnswerLoader;
import net.b07z.sepia.server.assist.answers.AnswerLoaderFile;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_Answers {

	public static void main(String[] args) {
		
		//take time
		long tic = Debugger.tic();
		
		//set answer loader
		//Config.setup_answers();
		//AnswerLoader loader = Config.answers;
		AnswerLoader loader = new AnswerLoaderFile();
		
		//print time
		System.out.println("loaded in ms: " + Debugger.toc(tic));
		tic = Debugger.tic();
		
		//construct input and result
		NluInput input = new NluInput();
		input.lastCmd = "default";
		input.lastCmdN = 0;
		input.mood = 5;
		input.language = "de";
		
		NluResult res = new NluResult(input);
		res.cmdSummary = "test";
		
		//get Answer
		System.out.println("answer: " + loader.getAnswer(res, "<direct>Hallo <user_name>, es klappt!"));
		System.out.println("answer: " + loader.getAnswer(res, "test_0a"));
		System.out.println("answer: " + loader.getAnswer(res, "test_0a"));
		System.out.println("answer: " + loader.getAnswer(res, "test_0a"));
		System.out.println("answer: " + loader.getAnswer(res, "test_0a"));
		System.out.println("answer: " + loader.getAnswer(res, "test_0a"));
		System.out.println("answer: " + loader.getAnswer(res, "test_0a"));

		//print time
		System.out.println("found in ms: " + Debugger.toc(tic));
	}

}
