package net.b07z.sepia.server.assist.smarthome;

import java.util.Map;

import net.b07z.sepia.server.assist.smarthome.SmartHomeHub.AuthType;

public class Test_HomeAssistant {

	public static void main(String[] args) {
		
		String host = "http://rpi4b.local:8123";
		String authToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJiZGU2ZDRmYWMwNGU0NzM5OTBlMDU1NjUxZWY2ODIwOSIsImlhdCI6MTY1OTY1MDc2MSwiZXhwIjoxOTc1MDEwNzYxfQ.qplcFP_HqgD3VwftMLIxY1ejOc6uqGAcWCvXma-IEv4";
		
		SmartHomeHub smartHomeHub = new HomeAssistant(host);
		smartHomeHub.setAuthenticationInfo(AuthType.Bearer.name(), authToken);
		
		Map<String, SmartHomeDevice> devices = getDevices(smartHomeHub);
		
		SmartHomeDevice foundShd = devices.values().iterator().next();
		loadDeviceData(smartHomeHub, foundShd);
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
	
	private static SmartHomeDevice loadDeviceData(SmartHomeHub smartHomeHub, SmartHomeDevice shd){
		SmartHomeDevice shdTest = smartHomeHub.loadDeviceData(shd);
		System.out.println("Device name: " + shdTest.getName()
		+ " - type: " + shdTest.getType() 
		+ " - room: " + shdTest.getRoom() 
		+ " - state-type: " + shdTest.getStateType()
		+ " - state: " + shdTest.getState());
		return shdTest;
	}

}
