package net.b07z.sepia.server.assist.services;

import java.util.List;
import java.util.Map;

import net.b07z.sepia.server.assist.parameters.SmartDevice;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.assist.smarthome.SmartHomeDevice;
import net.b07z.sepia.server.assist.smarthome.SmartHomeHub;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class to test openHAB integration methods (get/set/match devices etc.).<br>
 * NOTE: Requires active openHAB server and might change device values during test!
 * 
 * @author Florian Quirin
 */
public class Test_SmartHubIntegration {

	public static void main(String[] args) {
		
		//load test config (for openHAB server URL mainly)
		Start.loadSettings(new String[]{"--test"});
		System.out.println("\nIntegration Test: SmartHomeHub (OpenHAB, FHEM, tbd)");
		System.out.println("Name: " + Config.smarthome_hub_name + ", HUB address: " + Config.smarthome_hub_host + "\n");
		
		SmartHomeHub smartHomeHub = SmartHomeHub.getHubFromSeverConfig();
		if (smartHomeHub == null){
			System.err.println("Test aborted: HUB is not defined or data invalid!");
			return;
		}
		
		//TODO: add new tests: register and write attribute (new interface methods)
		
		//get devices
		long tic = Debugger.tic();
		Map<String, SmartHomeDevice> devicesMap = smartHomeHub.getDevices();
		
		//print all devices
		System.out.println("Devices found: ");
		Debugger.printMap(devicesMap);
		System.out.println("");
		
		//search any light
		List<SmartHomeDevice> deviceMatches = SmartHomeDevice.getMatchingDevices(devicesMap, SmartDevice.Types.light.name(), "", "", -1);
		
		//show all
		System.out.println("Found lights: ");
		Debugger.printList(deviceMatches);
		System.out.println("");
		
		//get light with number "1" in name or first result
		SmartHomeDevice firstDevice = SmartHomeDevice.findFirstDeviceWithNumberInNameOrDefault(deviceMatches, 1, 0);
		System.out.println("First light (light 1 or first in array): " + firstDevice.getName() + "\n");
		
		//get device state (load again)
		//String deviceLink = deviceMatches.get(0).getLink();
		SmartHomeDevice device = smartHomeHub.loadDeviceData(firstDevice);
		System.out.println(device);
		String orgState = device.getState();
		String orStateType = device.getStateType();
		System.out.println("First light state: " + orgState);
		System.out.println("Took: " + Debugger.toc(tic) + "ms");
		
		//set device state 0
		System.out.println("Setting state 0");
		tic = Debugger.tic();
		smartHomeHub.setDeviceState(device, "0", SmartHomeDevice.STATE_TYPE_NUMBER_PERCENT);
		System.out.println("Took: " + Debugger.toc(tic) + "ms - waiting 3s");
		Debugger.sleep(3000);
		
		//set device state 50
		System.out.println("Setting state 50 and storing memory-state");
		tic = Debugger.tic();
		smartHomeHub.setDeviceState(device, "50", SmartHomeDevice.STATE_TYPE_NUMBER_PERCENT);
		smartHomeHub.setDeviceStateMemory(device, "50");
		System.out.println("Took: " + Debugger.toc(tic) + "ms - waiting 3s");
		Debugger.sleep(3000);
		
		//set original device state, wait and get current
		System.out.println("Setting original state " + orgState);
		tic = Debugger.tic();
		smartHomeHub.setDeviceState(device, orgState, orStateType);
		System.out.println("Took: " + Debugger.toc(tic) + "ms - waiting 3s");
		Debugger.sleep(3000);
		if (!orgState.equals("0")){
			smartHomeHub.setDeviceStateMemory(device, orgState);
			//try again to test shortcut
			smartHomeHub.setDeviceStateMemory(device, orgState);
		}
		tic = Debugger.tic();
		SmartHomeDevice finalData = smartHomeHub.loadDeviceData(device);
		String finalState = finalData.getState();
		System.out.println("Final light state: " + finalState);
		
		System.out.println("\nDONE");
	}

}
