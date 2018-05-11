package net.b07z.sepia.server.assist.database;

import net.b07z.sepia.server.assist.server.Config;

/**
 * Set some common configuration variables for AWS DynamoDB.
 * 
 * @author Florian Quirin
 *
 */
public class ConfigDynamoDB {
	
	//note: check DB.java for table and key configurations

	public static String service = "dynamodb";
	public static String region_us1 = "us-east-1"; 					//Virginia
	public static String region_eu1 = "eu-central-1"; 				//Frankfurt
	public static String region_custom = "http://localhost:8000";	//Custom
	
	public static String host = service + "." + region_eu1 + ".amazonaws.com";
	public static String endpoint = "https://" + host;
	
	public static String getRegion(String region){
		if (region.startsWith("eu")){
			return region_eu1;
		}else if (region.startsWith("us")){
			return region_us1;
		}else{
			return region_custom;
		}
	}
	
	public static String getHost(){
		String region = getRegion(Config.defaultRegion);
		if (region.startsWith("http")){
			host = region;
			return region;
		}else{
			host = service + "." + region + ".amazonaws.com";
			return host;
		}
	}
	
	public static String getEndpoint(){
		String host = getHost();
		if (host.startsWith("http")){
			endpoint = host;
			return endpoint;
		}else{
			endpoint = "https://" + host;
			return endpoint;
		}
	}

}
