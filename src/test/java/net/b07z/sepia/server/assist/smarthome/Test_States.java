package net.b07z.sepia.server.assist.smarthome;

import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_States {

	public static void main(String[] args){
		//state check for toggle condition
		String[] tests = new String[]{"OPEN", "open", "wide open", "TRUE", "true", "false",
				"100", "100%", "0%", "50 pct", "0pct", "connected"};
		for (String s : tests){
			System.out.println(s + " is ON/OPEN? " + checkState(s));
		}
		System.out.println("");
		System.out.println("-----");
		
		//expression evaluations
		checkExpressions();
	}
	
	private static boolean checkState(String selectedDeviceState){
		if (SmartHomeDevice.isStateNonZeroNumber(selectedDeviceState)
				|| Is.typeEqualIgnoreCase(selectedDeviceState, SmartHomeDevice.State.on)
				|| selectedDeviceState.matches("(?i)(true|open|connected)")){
			return true;
		}else{
			return false;
		}
	}
	
	private static void checkExpressions(){
		System.out.println("ON: " + SmartHomeDevice.getStateFromJsonViaExpression("state", 
			JSON.make("state", "ON")));
		System.out.println("ON: " + SmartHomeDevice.getStateFromJsonViaExpression("<state>", 
			JSON.make("state", "ON")));
		System.out.println("200: " + SmartHomeDevice.getStateFromJsonViaExpression("<attr.bri>", 
			JSON.make("attr", JSON.make("bri", 200))));
		System.out.println("100: " + SmartHomeDevice.getStateFromJsonViaExpression("100*<attr.bri>/255", 
			JSON.make("attr", JSON.make("bri", "255"))));
		System.out.println("null: " + SmartHomeDevice.getStateFromJsonViaExpression("100*<attr.bri>/255", 
			JSON.make("attr", JSON.make("wr", "255"))));
		System.out.println("null: " + SmartHomeDevice.getStateFromJsonViaExpression("100*<attr.bri>/255", 
			JSON.make("attr", JSON.make("bri", false))));
		System.out.println("2.4: " + SmartHomeDevice.getStateFromJsonViaExpression("2.0*<attr.bri>", 
			JSON.make("attr", JSON.make("bri", "1.2"))));
		System.out.println("1.9: " + SmartHomeDevice.getStateFromJsonViaExpression("round(20*<attr.bri>)/10", 
			JSON.make("attr", JSON.make("bri", "0.94"))));
	}

}
