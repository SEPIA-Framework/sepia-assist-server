package net.b07z.sepia.server.assist.events;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.JSON;

/**
 * Event that holds information about changed data.
 * 
 * @author Florian Quirin
 *
 */
public class ChangeEvent {
	
	/**
	 * Change event type like "timeEvents" or "productivity" (e.g. to-do lists) etc.
	 */
	public static enum Type {
		timeEvents, 		//related: Section.timeEvents
		productivity,		//related: Section.productivity
		addresses,			//related to? Section.personalInfo
		unknown;
		
		public static ChangeEvent.Type typeOf(String typeName){
			try{
				return Type.valueOf(typeName);
			}catch (Exception e){
				return unknown;
			}
		}
	}
	
	private Type type;
	private String typeName;
	private String id;
	private JSONObject data;
	
	/**
	 * Create new change event with type and id.
	 * @param type - {@link Type}
	 * @param id
	 */
	public ChangeEvent(Type type, String id){
		this.type = type;
		this.id = id;
	}
	/**
	 * Create new change event with type and id and data.
	 * @param type - {@link Type}
	 * @param id
	 * @param data
	 */
	public ChangeEvent(Type type, String id, JSONObject data){
		this(type, id);
		this.data = data;
	}
	/**
	 * Create new change event with type and id.
	 * @param type - {@link Type}
	 * @param id
	 */
	public ChangeEvent(String type, String id){
		this.type = Type.typeOf(type);
		if (this.type.equals(Type.unknown)){
			this.typeName = type;
		}
		this.id = id;
	}
	/**
	 * Create new change event with type and id and data.
	 * @param type - {@link Type}
	 * @param id
	 * @param data
	 */
	public ChangeEvent(String type, String id, JSONObject data){
		this(type, id);
		this.data = data;
	}
	
	public Type getType(){
		return type;
	}
	public void setType(String type){
		this.type = Type.typeOf(type);
		if (this.type.equals(Type.unknown)){
			this.typeName = type;
		}
	}
	
	public String getId(){
		return id;
	}
	public void setId(String id){
		this.id = id;
	}
	
	public JSONObject getData(){
		return data;
	}
	public void setData(JSONObject data){
		this.data = data;
	}
	
	@Override
	public String toString(){
		String s = "type: " + this.type.name() + " - id: " + this.id;
		if (this.typeName != null){
			s += " - typeName: " + this.typeName;
		}
		return s;
	}
	
	public JSONObject getJson(){
		JSONObject jo = JSON.make(
			"id", id, 
			"type", type.name()
		);
		if (this.typeName != null){
			JSON.put(jo, "typeName", this.typeName);
		}
		if (this.data != null){
			JSON.put(jo, "data", this.data);
		}
		return jo;
	}
}
