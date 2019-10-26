package net.b07z.sepia.server.assist.services;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.core.tools.JSON;

/**
 * Contains information about certain requirements this service has to run properly.
 * 
 * @author Florian Quirin
 *
 */
public class ServiceRequirements {
	
	public String htmlClientMinVersion = "";
	public String htmlClientMaxVersion = "";
	public String serverMinVersion = "";
	public String serverMaxVersion = "";
	public JSONArray abilities = new JSONArray();
	public JSONArray apiAccess = new JSONArray();
	
	public enum Abilities {
		display,
		speaker,
		microphone
	}
	public enum Apis {
		googlemaps,
		graphhopper,
		darksky,
		spotify,
		openhab,
		fhem,
		iobroker
	}
	
	/**
	 * Initialize requirements builder.
	 */
	public ServiceRequirements(){};
	
	/**
	 * Get JSON object of requirements.
	 * @return
	 */
	public JSONObject toJson(){
		return JSON.make(
				"htmlClient", JSON.make(
						"vMin", this.htmlClientMinVersion,
						"vMax", this.htmlClientMaxVersion
				),
				"server", JSON.make(
						"vMin", this.serverMinVersion,
						"vMax", this.serverMaxVersion
				),
				"abilities", abilities,
				"apiAccess", apiAccess
		);
	}
	
	/**
	 * Minimum required version of public HTML client.
	 * @param version - e.g. 0.19.1
	 */
	public ServiceRequirements htmlClientMinVersion(String version){
		this.htmlClientMinVersion = version;
		return this;
	}
	/**
	 * Maximum supported version of public HTML client.
	 * @param version - e.g. 0.18.0
	 */
	public ServiceRequirements htmlClientMaxVersion(String version){
		this.htmlClientMaxVersion = version;
		return this;
	}
	
	/**
	 * Minimum required version of SEPIA Home server stack.
	 * @param version - e.g. 2.3.1
	 */
	public ServiceRequirements serverMinVersion(String version){
		this.serverMinVersion = version;
		return this;
	}
	/**
	 * Maximum supported version of SEPIA Home server stack.
	 * @param version - e.g. 2.2.2
	 */
	public ServiceRequirements serverMaxVersion(String version){
		this.serverMaxVersion = version;
		return this;
	}
	
	/**
	 * Required ability like client with "display".
	 */
	public ServiceRequirements ability(Abilities ability){
		JSON.add(abilities, ability.name());
		return this;
	}
	/**
	 * Required custom ability (any custom string).
	 */
	public ServiceRequirements customAbility(String ability){
		JSON.add(abilities, ability);
		return this;
	}
	
	/**
	 * Required access to integrated API, e.g. Google-Maps via API-Key or OpenHAB via local server. 
	 */
	public ServiceRequirements apiAccess(Apis api){
		JSON.add(apiAccess, api.name());
		return this;
	}
	/**
	 * Required access to custom API (any custom string).
	 */
	public ServiceRequirements customApiAccess(String api){
		JSON.add(apiAccess, api);
		return this;
	}
}
