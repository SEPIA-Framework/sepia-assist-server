package net.b07z.sepia.server.assist.database;

/**
 * Set some common configuration variables for ElasticSearch.
 * 
 * @author Florian Quirin
 *
 */
public class ConfigElasticSearch {

	//note: check DB.java for table and key configurations
	
	//EU-cluster: active
	public static String endpoint_eu1 = "";
	//US-cluster: active
	public static String endpoint_us1 = "";
	//Test-cluster
	public static String endpoint_custom = "https://localhost:9999";
	
	public static String getEndpoint(String region){
		//EU
		if (region.startsWith("eu")){
			return endpoint_eu1;
		//US
		}else if (region.startsWith("us")){
			return endpoint_us1;
		//TEST
		}else{
			return endpoint_custom;
		}
	}

	//Auth. type and data
	public static String auth_type = null;
	public static String auth_data = null;
	public static String getAuthType(){
		return auth_type;
	}
	public static String getAuthData(){
		return auth_data;
	}
}
