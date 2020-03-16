package net.b07z.sepia.server.assist.smarthome;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.Elasticsearch;
import net.b07z.sepia.server.core.database.DatabaseInterface;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.EsQueryBuilder;
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
			JSONObject sdInterface = JSON.make(
					"id", id,
					"type", type,
					"host", JSON.getString(data, "host"),
					"name", JSON.getString(data, "name")
			);
			JSON.put(sdInterface, "authType", JSON.getString(data, "authType"));
			JSON.put(sdInterface, "authData", JSON.getString(data, "authData"));
			JSON.put(sdInterface, "info", JSON.getJObject(data, "info"));
			
			int code = getDB().updateItemData(SmartDevicesDb.INTERFACES, DEFAULT_TYPE, id, sdInterface);
			return code;	//0 - all good, 1 - no connection or some error 
		}
	}

	@Override
	public int removeInterface(String id){
		int code = getDB().deleteItem(SmartDevicesDb.INTERFACES, DEFAULT_TYPE, id);
		return code;
	}

	@Override
	public Map<String, SmartHomeHub> loadInterfaces(){
		JSONObject result = getDB().searchByJson(SmartDevicesDb.INTERFACES + "/" + DEFAULT_TYPE + "/", EsQueryBuilder.matchAll.toJSONString());
		if (Connectors.httpSuccess(result)){
			Map<String, SmartHomeHub> interfacesMap = new HashMap<>();
			JSONArray hits = JSON.getJArray(result, new String[]{"hits", "hits"});
			if (hits != null){
				try{
					for (Object o : hits){
						//re-mapping (mostly to validate code - add importJson method?)
						JSONObject jo = (JSONObject) o;
						String id = JSON.getString(jo, "id");
						String type = JSON.getString(jo, "type");
						String host = JSON.getString(jo, "host");
						SmartHomeHub shh = SmartHomeHub.getHub(type, host);
						if (shh != null){
							shh.setAuthenticationInfo(JSON.getString(jo, "authType"), JSON.getString(jo, "authData"));
							shh.setInfo(JSON.getJObject(jo, "info"));
							interfacesMap.put(id, shh);
						}
					}
				}catch (Exception e){
					Debugger.println("Smart Interfaces - failed to load interfaces! Msg.: " + e.getMessage(), 1);
					Debugger.printStackTrace(e, 3);
				}
			}
			return interfacesMap;
		}else{
			return null;
		}
	}

	@Override
	public int addOrUpdateCustomDevice(JSONObject data){
		String id = JSON.getString(data, "id");
		//import to validate code
		SmartHomeDevice shd = new SmartHomeDevice();
		shd.importJsonDevice(data);
		//TODO: add interface field
		//store
		int code;
		if (Is.nullOrEmpty(id)){
			JSONObject res = getDB().setAnyItemData(SmartDevicesDb.DEVICES, DEFAULT_TYPE, shd.getDeviceAsJson());
			code = JSON.getIntegerOrDefault(res, "code", 2);
		}else{
			code = getDB().updateItemData(SmartDevicesDb.DEVICES, DEFAULT_TYPE, id, shd.getDeviceAsJson());
		}
		return code;	//0 - all good, 1 - no connection or some error, 2 - unknown error
	}

	@Override
	public int removeCustomDevice(String id){
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public SmartHomeDevice getCustomDevice(String id){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, SmartHomeDevice> getCustomDevices(Map<String, Object> filters){
		// TODO Auto-generated method stub
		return null;
	}
}
