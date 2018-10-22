package net.b07z.sepia.server.assist.services;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class to test openHAB integration methods (get/set/match devices etc.).<br>
 * NOTE: Requires active openHAB server and might change device values during test!
 * 
 * @author Florian Quirin
 */
public class Test_OpenHabIntegration {

	public static void main(String[] args) {
		
		//load test config (for openHAB server URL mainly)
		Start.loadSettings(new String[]{"--test"});
		System.out.println("\nIntegration Test: openHAB\n");
		
		//get devices
		long tic = Debugger.tic();
		Map<String, JSONObject> devicesMap = SmartOpenHAB.getDevices(Config.openhab_host);
		
		//print all devices
		System.out.println("Devices found: ");
		Debugger.printMap(devicesMap);
		
		//search any light
		List<JSONObject> deviceMatches = SmartOpenHAB.getMatchingDevices(devicesMap, SmartDevice.Types.light.name(), "", -1);
		
		//show all
		System.out.println("Found lights: ");
		Debugger.printList(deviceMatches);
		
		//get first state
		String deviceLink = JSON.getString(deviceMatches.get(0), "link");
		JSONObject deviceData = SmartOpenHAB.getDeviceData(deviceLink);
		String orgState = JSON.getString(deviceData, "state");
		System.out.println("First light state: " + orgState);
		System.out.println("Took: " + Debugger.toc(tic) + "ms");
		
		//set device state 0
		System.out.println("Setting state 0");
		tic = Debugger.tic();
		SmartOpenHAB.setDeviceState(deviceLink, "0");
		System.out.println("Took: " + Debugger.toc(tic) + "ms - waiting 3s");
		Debugger.sleep(3000);
		
		//set device state 70
		System.out.println("Setting state 75 and storing memory-state");
		tic = Debugger.tic();
		SmartOpenHAB.setDeviceState(deviceLink, "75");
		SmartOpenHAB.setDeviceMemoryState(deviceData, "75");
		System.out.println("Took: " + Debugger.toc(tic) + "ms - waiting 3s");
		Debugger.sleep(3000);
		
		//set original device state, wait and get current
		System.out.println("Setting original state " + orgState);
		tic = Debugger.tic();
		SmartOpenHAB.setDeviceState(deviceLink, orgState);
		System.out.println("Took: " + Debugger.toc(tic) + "ms - waiting 3s");
		Debugger.sleep(3000);
		if (!orgState.equals("0")){
			SmartOpenHAB.setDeviceMemoryState(deviceData, orgState);
			//try again to test shortcut
			SmartOpenHAB.setDeviceMemoryState(deviceData, orgState);
		}
		tic = Debugger.tic();
		JSONObject finalData = SmartOpenHAB.getDeviceData(deviceLink);
		String finalState = JSON.getString(finalData, "state");
		System.out.println("Final light state: " + finalState);
		
		System.out.println("\nDONE");
	}

}
