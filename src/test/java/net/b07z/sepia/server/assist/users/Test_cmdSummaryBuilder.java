package net.b07z.sepia.server.assist.users;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.JSONWriter;

public class Test_cmdSummaryBuilder {

	public static void main(String[] args){
		String cmd = "uid1007.test";
		JSONObject params1 = JSON.make(
				"p1", "val1",
				"p2", "val2"
		);
		JSONObject params2 = new JSONObject(); 
		JSONObject params3 = JSON.make(
				"uid.test_b", "<2>",
				"device", "<type>;;name",
				"test2_c", "3"
		);
				
		String cmdSum1 = Converters.makeCommandSummary(cmd, params1);
		String cmdSum2 = Converters.makeCommandSummary(cmd, params2);
		String cmdSum3 = Converters.makeCommandSummary(cmd, params3);
		
		System.out.println(cmdSum1);
		System.out.println(cmdSum2);
		System.out.println(cmdSum3);
		
		JSONObject paramsRestore1 = Converters.getParametersFromCommandSummary(cmd, cmdSum1);
		JSONObject paramsRestore2 = Converters.getParametersFromCommandSummary(cmd, cmdSum2);
		JSONObject paramsRestore3 = Converters.getParametersFromCommandSummary(cmd, cmdSum3);
		
		System.out.println("" + ((paramsRestore1 != null)? JSONWriter.getPrettyString(paramsRestore1) : "null"));
		System.out.println("" + ((paramsRestore2 != null)? JSONWriter.getPrettyString(paramsRestore2) : "null"));
		System.out.println("" + ((paramsRestore3 != null)? JSONWriter.getPrettyString(paramsRestore3) : "null"));
	}

}
