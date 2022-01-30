package net.b07z.sepia.server.assist.smarthome;

public class Test_States {

	public static void main(String[] args){
		//state check for toggle condition
		String[] tests = new String[]{"OPEN", "open", "wide open", "TRUE", "true", "false", "100", "100%", "0%", "50 pct", "0pct", "connected"};
		for (String s : tests){
			System.out.println(s + " is ON? " + checkState(s));
		}
	}
	
	private static boolean checkState(String selectedDeviceState){
		if ((selectedDeviceState.matches("(%|pct|)( |)\\d+( |)(%|pct|)") && !selectedDeviceState.matches("(%|pct|)( |)(0)( |)(%|pct|)"))
				|| selectedDeviceState.matches("(?i)(true|on|open|connected)")){
			return true;
		}else{
			return false;
		}
	}

}
