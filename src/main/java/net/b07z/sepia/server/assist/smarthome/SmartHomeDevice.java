package net.b07z.sepia.server.assist.smarthome;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.JSON;

public class SmartHomeDevice {
	
	private String name;
	private String type; 		//see: net.b07z.sepia.server.assist.parameters.SmartDevice.Types
	private String room;		//see: net.b07z.sepia.server.assist.parameters.Room.Types
	private String state;		//e.g.: ON, OFF, 1-100, etc.
	private String stateMemory;		//state storage for e.g. default values after restart etc.
	private String link;		//e.g. HTTP direct URL to device
	private JSONObject meta;	//space for custom stuff
	
	public SmartHomeDevice(String name, String type, String room, String state, String stateMemory, String link, JSONObject meta){
		this.name = name;
		this.type = type;
		this.room = room;
		this.state = state;
		this.stateMemory = stateMemory;
		this.link = link;
		this.meta = meta;
	}
	public SmartHomeDevice(){}
	
	/**
	 * Device name
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Device type
	 * @return
	 */
	public String getType() {
		return type;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Device room
	 * @return
	 */
	public String getRoom() {
		return room;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setRoom(String room) {
		this.room = room;
	}
	
	/**
	 * Device state
	 * @return
	 */
	public String getState() {
		return state;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setState(String state) {
		this.state = state;
	}
	/**
	 * Write state to HUB.
	 * @param hub - HUB to write to
	 * @param newState - state to write
	 * @return success/error
	 */
	public boolean writeState(SmartHomeHub hub, String newState){
		return hub.setDeviceState(this, newState);
	}
	
	/**
	 * Device state memory
	 * @return
	 */
	public String getStateMemory() {
		return stateMemory;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setStateMemory(String stateMemory) {
		this.stateMemory = stateMemory;
	}
	/**
	 * Write state memory to HUB.
	 * @param hub - HUB to write to
	 * @param newStateMem - state to write
	 * @return success/error
	 */
	public boolean writeStateMemory(SmartHomeHub hub, String newStateMem){
		return hub.setDeviceStateMemory(this, newStateMem);
	}
	
	/**
	 * Device direct link
	 * @return
	 */
	public String getLink() {
		return link;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setLink(String link) {
		this.link = link;
	}
	
	/**
	 * Device custom meta data
	 * @return
	 */
	public JSONObject getMeta() {
		return meta;
	}
	/**
	 * Get certain value of meta data as string.
	 * @param key
	 * @return
	 */
	public String getMetaValueAsString(String key){
		if (meta != null){
			return JSON.getString(meta, key);
		}else{
			return null;
		}
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setMeta(JSONObject meta) {
		this.meta = meta;
	}
	/**
	 * Set object variable (no write to HUB!)
	 */
	public void setMetaValue(String key, Object value) {
		if (meta == null){
			meta = new JSONObject();
		}
		JSON.put(meta, key, value);
	}
	
	/**
	 * Export device to JSON.
	 * @return
	 */
	public JSONObject getDeviceAsJson(){
		//create common object
		JSONObject newDeviceObject = JSON.make(
				"name", name, 
				"type", type, 
				"room", room, 
				"state", state, 
				"link", link
		);
		JSON.put(newDeviceObject, "state-memory", stateMemory);
		JSON.put(newDeviceObject, "meta", meta);
		return newDeviceObject;
	}
	/**
	 * Import device from JSON obejct.
	 * @param deviceJson
	 */
	public void importJsonDevice(JSONObject deviceJson){
		this.name = JSON.getString(deviceJson, "name");
		this.type = JSON.getString(deviceJson, "type");
		this.room = JSON.getString(deviceJson, "room");
		this.state = JSON.getString(deviceJson, "state");
		this.stateMemory = JSON.getString(deviceJson, "state-memory");
		this.link = JSON.getString(deviceJson, "link");
		this.meta = JSON.getJObject(deviceJson, "meta");
	}
}
