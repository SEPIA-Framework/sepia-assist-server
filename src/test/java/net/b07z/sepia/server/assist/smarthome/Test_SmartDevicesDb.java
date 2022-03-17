package net.b07z.sepia.server.assist.smarthome;

import java.util.Map;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_SmartDevicesDb {

	public static void main(String[] args) throws Exception{
		
		//load custom config
		Start.loadSettings(new String[]{"--test"});
		Config.setupDatabases(true);
		
		//Load devices
		System.out.println("\nLOAD ALL ...\n");
		Map<String, SmartHomeDevice> devices = DB.getSmartDevicesDb().getCustomDevices(null);
		
		//Show
		devices.keySet().forEach(id -> {
			System.out.println("ID: " + id);
			JSON.prettyPrint(devices.get(id).getDeviceAsJson());
		});
		
		//Load specific
		String someId = devices.keySet().iterator().next();
		System.out.println("\nLOAD ONE: " + someId + "\n");
		JSON.prettyPrint(DB.getSmartDevicesDb().getCustomDevice(someId).getDeviceAsJson());

		//Exit
		System.out.println("\nDONE\n");
	}

}
