package net.b07z.sepia.server.assist.smarthome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.Elasticsearch;
import net.b07z.sepia.server.core.database.DatabaseInterface;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.EsQueryBuilder;
import net.b07z.sepia.server.core.tools.EsQueryBuilder.QueryElement;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class that manages the database for smart home/device interfaces and custom devices/items.
 * 
 * @author Florian Quirin
 *
 */
public class SmartDevicesElasticsearch implements SmartDevicesDb {
	
	private static String DEFAULT_TYPE = "all";
	
	private static Map<String, SmartHomeHub> customInterfacesCached; // = new ConcurrentHashMap<>()	//<interfaceId, Hub>
	private static Map<String, Set<String>> bufferedDevicesByType = new ConcurrentHashMap<>();  	//<type, Set<name>>
	private static boolean isBufferedDeviceNameListRecent = false;

	/**
	 * Get the database to query. Loads the Elasticsearch connection defined by settings. 
	 */
	private DatabaseInterface getDB(){
		DatabaseInterface es = new Elasticsearch();
		return es;
	}

	@Override
	public int addOrUpdateInterface(JSONObject data){
		String id = JSON.getString(data, "id");
		String type = JSON.getString(data, "type");
		if (Is.nullOrEmpty(id) || Is.nullOrEmpty(type)){
			return 2;	//invalid data
		}else{
			//make sure ID is not "strange"
			String cleanId = id.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z_0-9]", "");
			JSON.put(data, "id", cleanId);
			//import to check validity
			SmartHomeHub shh = SmartHomeHub.importJson(data);
			//and write it (this will export shh back to JSON, kind of redundant but save)			
			int code = getDB().updateItemData(SmartDevicesDb.INTERFACES, DEFAULT_TYPE, cleanId, shh.toJson());
			if (code == 0){
				Debugger.println("Smart Interfaces - Created or updated interface with ID: " + id + " (type: " + type + ")", 3);
				//update cache
				shh.activate();
				getCachedInterfaces().put(cleanId, shh);
			}else{
				Debugger.println("Smart Interfaces - FAILED to create or update interface with ID: " + id, 1);
			}
			return code;	//0 - all good, 1 - no connection or some error 
		}
	}

	@Override
	public int removeInterface(String id){
		int code = getDB().deleteItem(SmartDevicesDb.INTERFACES, DEFAULT_TYPE, id);
		if (code == 0){
			Debugger.println("Smart Interfaces - Removed interface with ID: " + id, 3);
			//update cache
			try{
				getCachedInterfaces().get(id).deactivate();
				getCachedInterfaces().remove(id);
			}catch (Exception e){
				///ignore
			}
		}else{
			Debugger.println("Smart Interfaces - FAILED to removed interface with ID: " + id, 1);
		}
		return code;
	}

	@Override
	public Map<String, SmartHomeHub> loadInterfaces(){
		JSONObject result = getDB().searchByJson(SmartDevicesDb.INTERFACES + "/" + DEFAULT_TYPE + "/", EsQueryBuilder.matchAll.toJSONString());
		if (Connectors.httpSuccess(result)){
			Map<String, SmartHomeHub> interfacesMap = new ConcurrentHashMap<>();
			JSONArray hits = JSON.getJArray(result, new String[]{"hits", "hits"});
			if (hits != null){
				try{
					for (Object o : hits){
						//re-mapping (mostly to validate code)
						JSONObject jo = (JSONObject) o;
						String id = JSON.getStringOrDefault(jo, "_id", "");
						JSONObject data = JSON.getJObject(jo, "_source");
						SmartHomeHub shh = SmartHomeHub.importJson(data);
						shh.activate();
						if (Is.notNullOrEmpty(id) && shh != null){
							interfacesMap.put(id, shh);
						}
					}
					//update cache
					customInterfacesCached = interfacesMap;
					
				}catch (Exception e){
					Debugger.println("Smart Interfaces - failed to load interfaces! Msg.: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
				}
			}
			return interfacesMap;
		}else{
			Debugger.println("Smart Interfaces - failed to load interfaces! Msg.: " + result, 1);
			return null;
		}
	}
	@Override
	public Map<String, SmartHomeHub> getCachedInterfaces(){
		if (customInterfacesCached == null){
			loadInterfaces();
		}
		return customInterfacesCached;
	}

	@Override
	public JSONObject addOrUpdateCustomDevice(JSONObject data){
		//import to validate code
		SmartHomeDevice shd = new SmartHomeDevice();
		shd.importJsonDevice(data);
		//store
		JSONObject meta = JSON.getJObject(data, "meta");
		String id;
		if (meta != null){
			//first choice and fallback
			id = JSON.getStringOrDefault(meta, "sepiaId", JSON.getString(meta, "id"));
			//check some formats - NOTE: we rely on 'importJsonDevice' for now
		}else{
			//fallback 2
			id = JSON.getString(data, "id");
		}
		//JSON.prettyPrint(shd.getDeviceAsJson()); 		//DEBUG
		if (Is.nullOrEmpty(id)){
			JSONObject res = getDB().setAnyItemData(SmartDevicesDb.DEVICES, DEFAULT_TYPE, shd.getDeviceAsJson());
			int code = JSON.getIntegerOrDefault(res, "code", 2);
			String newId = JSON.getString(res, "_id");
			if (code == 0){
				Debugger.println("Smart Devices - Created new device (item) with ID: " + newId, 3);
				//update buffer
				updateDeviceTypeAndNameBuffer(shd, false);
			}else{
				Debugger.println("Smart Devices - FAILED to create device (item) with ID: " + id, 1);
			}
			return JSON.make(
				"id", newId, 
				"code", code
				//0 - all good, 1 - no connection or some error, 2 - unknown error
			);
		}else{
			int code = getDB().setItemData(SmartDevicesDb.DEVICES, DEFAULT_TYPE, id, shd.getDeviceAsJson());
			//int code = getDB().updateItemData(SmartDevicesDb.DEVICES, DEFAULT_TYPE, id, shd.getDeviceAsJson());	//this will not work as expected :-(
			if (code != 0){
				Debugger.println("Smart Devices - FAILED to updated device (item) with ID: " + id, 1);
			}
			return JSON.make( 
				"code", code	//0 - all good, 1 - no connection or some error, 2 - unknown error
			);
		}
	}

	@Override
	public int removeCustomDevice(String id){
		int code = getDB().deleteItem(SmartDevicesDb.DEVICES, DEFAULT_TYPE, id);
		if (code == 0){
			Debugger.println("Smart Devices - Removed device (item) with ID: " + id, 3);
			//update buffer
			clearDeviceTypeAndNameBuffer();
		}
		return code;
	}

	@Override
	public SmartHomeDevice getCustomDevice(String id){
		JSONObject result = getDB().getItem(SmartDevicesDb.DEVICES, DEFAULT_TYPE, id);
		if (Connectors.httpSuccess(result)){
			try{
				String foundId = JSON.getStringOrDefault(result, "_id", "");
				JSONObject data = JSON.getJObject(result, "_source");
				if (id.equals(foundId) && data != null){
					SmartHomeDevice shd = new SmartHomeDevice();
					shd.importJsonDevice(data);
					shd.setMetaValue("id", id);
					return shd;
				}else{
					Debugger.println("Smart Devices - failed to load device due to missing or invalid 'id' or data! Obj.: " + result, 1);
				}
			}catch (Exception e){
				Debugger.println("Smart Devices - failed to load device! Msg.: " + e.getMessage(), 1);
				Debugger.printStackTrace(e, 3);
			}
		}else{
			Debugger.println("Smart Devices - failed to load custom device (item) with ID '" + id + "'! Msg.: " + result, 1);
		}
		return null;
	}

	@Override
	public Map<String, SmartHomeDevice> getCustomDevices(Map<String, Object> filters){
		String query;
		if (filters != null && !filters.isEmpty()){
			//use filters
			List<QueryElement> qes = new ArrayList<>();
			filters.entrySet().forEach(e -> {
				Object v = e.getValue();
				if (v != null){
					qes.add(new QueryElement(e.getKey(), v));
				}
			});
			query = EsQueryBuilder.getBoolShouldMatch(qes).toJSONString();
		}else{
			query = EsQueryBuilder.matchAll.toJSONString();
		}
		//System.out.println("query: " + query); 			//DEBUG
		//request
		JSONObject result = getDB().searchByJson(SmartDevicesDb.DEVICES + "/" + DEFAULT_TYPE + "/", query);
		if (Connectors.httpSuccess(result)){
			//build Map<id, item>
			Map<String, SmartHomeDevice> customDevices = new HashMap<>();
			JSONArray hits = JSON.getJArray(result, new String[]{"hits", "hits"});
			if (hits != null){
				try{
					for (Object o : hits){
						//import
						JSONObject jo = (JSONObject) o;
						String id = JSON.getStringOrDefault(jo, "_id", "");
						JSONObject data = JSON.getJObject(jo, "_source");
						if (Is.notNullOrEmpty(id) && data != null){
							SmartHomeDevice shd = new SmartHomeDevice();
							shd.importJsonDevice(data);
							shd.setMetaValue("id", id);
							shd.setMetaValue("sepiaId", id);	//in case "id" gets overwritten
							customDevices.put(id, shd);
							//buffer
							updateDeviceTypeAndNameBuffer(shd, false);
						}else{
							Debugger.println("Smart Devices - failed to load device due to missing 'id' or data! Obj.: " + jo, 1);
						}
					}
				}catch (Exception e){
					Debugger.println("Smart Devices - failed to load device! Msg.: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
				}
			}
			return customDevices;
		}else{
			Debugger.println("Smart Devices - failed to load custom devices (items)! Msg.: " + result, 1);
			return null;
		}
	}

	@Override
	public Map<String, Set<String>> getBufferedDeviceNamesByType(){
		if (!isBufferedDeviceNameListRecent){
			//clean reload
			bufferedDevicesByType = new ConcurrentHashMap<>();
			if (getCustomDevices(null) != null){
				isBufferedDeviceNameListRecent = true;
			}
		}
		//DEBUG
		/* bufferedDevicesByType.entrySet().forEach(e -> {
			System.out.println("Type: " + e.getKey() + " - Names: " + e.getValue());
		}); */
		return bufferedDevicesByType;
	}
	
	//---- HELPERS ----
	
	/**
	 * Clear all buffered device type-name combinations and set flag so it will be reloaded when needed.
	 */
	public static void clearDeviceTypeAndNameBuffer(){
		isBufferedDeviceNameListRecent = false;
		bufferedDevicesByType = new ConcurrentHashMap<>();
	}
	/**
	 * Store the type-name combination in the buffer for fast access.
	 * @param shd - the {@link SmartHomeDevice}
	 * @param remove - set this to true to remove entry instead of add
	 */
	public static void updateDeviceTypeAndNameBuffer(SmartHomeDevice shd, boolean remove){
		String deviceType = shd.getType();
		String deviceName = shd.getName();
		if (Is.notNullOrEmpty(deviceName) && Is.notNullOrEmpty(deviceType)){
			//System.out.println("Device Type-Name Buffer Update: " + deviceType + "=" + deviceName);		//DEBUG
			Set<String> namesOfType = bufferedDevicesByType.get(shd.getType());
			if (namesOfType == null){
				if (remove){
					return;
				}else{
					namesOfType = ConcurrentHashMap.newKeySet();
					bufferedDevicesByType.put(deviceType, namesOfType);
				}
			}
			if (remove){
				namesOfType.remove(deviceName);
			}else{
				namesOfType.add(deviceName);
			}
		}
	}
}
