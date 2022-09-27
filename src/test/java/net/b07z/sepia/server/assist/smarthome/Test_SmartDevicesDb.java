package net.b07z.sepia.server.assist.smarthome;

import java.util.HashMap;
import java.util.Map;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_SmartDevicesDb {

	public static void main(String[] args) throws Exception{
		
		//load custom config
		Start.loadSettings(new String[]{"--test"});
		Config.setupDatabases(true);
		
		//Load all devices
		System.out.println("\nLOAD ALL ...\n");
		Map<String, SmartHomeDevice> devices = DB.getSmartDevicesDb().getCustomDevices(null);
		devices.keySet().forEach(id -> {
			System.out.println("ID: " + id);
			JSON.prettyPrint(devices.get(id).getDeviceAsJson());
		});
		
		//Load some devices
		System.out.println("\nLOAD SOME (FILTERED) ...\n");
		Map<String, Object> filters = new HashMap<>();
		filters.put(SmartHomeDevice.FILTER_TYPE_ARRAY,
			SmartDevice.Types.light.name() + ", " + SmartDevice.Types.heater.name());
		Map<String, SmartHomeDevice> devices2 = DB.getSmartDevicesDb().getCustomDevices(filters);
		devices2.keySet().forEach(id -> {
			System.out.println("ID: " + id);
			JSON.prettyPrint(devices2.get(id).getDeviceAsJson());
		});
		
		//Load specific
		String someId = devices.keySet().iterator().next();
		System.out.println("\nLOAD ONE: " + someId + "\n");
		JSON.prettyPrint(DB.getSmartDevicesDb().getCustomDevice(someId).getDeviceAsJson());

		//Exit
		System.out.println("\nDONE\n");
	}

}
