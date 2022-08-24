package net.b07z.sepia.server.assist.smarthome;

import java.util.Map;

import net.b07z.sepia.server.assist.smarthome.SmartHomeHub.AuthType;

public class Test_HomeAssistant {

	public static void main(String[] args) {
		
		String host = "http://rpi4b.local:8123";
		String authToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJiZGU2ZDRmYWMwNGU0NzM5OTBlMDU1NjUxZWY2ODIwOSIsImlhdCI6MTY1OTY1MDc2MSwiZXhwIjoxOTc1MDEwNzYxfQ.qplcFP_HqgD3VwftMLIxY1ejOc6uqGAcWCvXma-IEv4";
		
		SmartHomeHub smartHomeHub = new HomeAssistant(host);
		smartHomeHub.setAuthenticationInfo(AuthType.Bearer.name(), authToken);
		
		getDevices(smartHomeHub);
	}
	
	private static Map<String, SmartHomeDevice> getDevices(SmartHomeHub smartHomeHub){
		Map<String, SmartHomeDevice> devicesMap = smartHomeHub.getDevices();
		if (devicesMap != null && devicesMap.size() > 0){
			for (String id : devicesMap.keySet()){
				SmartHomeDevice shd = devicesMap.get(id);
				System.out.println("Device name: " + shd.getName()
				+ " - type: " + shd.getType() 
				+ " - room: " + shd.getRoom() 
				+ " - state-type: " + shd.getStateType()
				+ " - state: " + shd.getState());
			}
		}
		return devicesMap;
	}

}
