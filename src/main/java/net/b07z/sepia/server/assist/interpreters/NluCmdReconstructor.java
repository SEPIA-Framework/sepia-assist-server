package net.b07z.sepia.server.assist.interpreters;

/**
 * This is not an interpreter in the conventional way but it reconstructs command summaries (cmd_summary)
 * in the form: <br><br>
 * cmd;paramA=1;paramB=2;<br>
 * <br>
 * In this case "text" inside the NLU_Input should be used to send the cmd_summary.
 * 
 * @author Florian Quirin
 *
 */
public class NluCmdReconstructor implements NluInterface {

	public NluResult interpret(NluInput input) {
		String cmd_summary = input.text;
		
		NluResult result = NluResult.cmdSummaryToResult(input, cmd_summary);
		double certainty_lvl = 1.0d;		//as this is the reconstructor it is 100% certain
		result.certaintyLvl = certainty_lvl;
		
		return result;
	}

	public double getCertaintyLevel(NluResult result) {
		return result.certaintyLvl;
	}

}
