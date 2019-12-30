package net.b07z.sepia.server.assist.database;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.database.DatabaseInterface;

/**
 * Class to handle an Elasticsearch node.
 * 
 * @author Florian Quirin
 *
 */
public class Elasticsearch extends net.b07z.sepia.server.core.database.Elasticsearch implements DatabaseInterface {
	
	/**
	 * Create Elasticsearch class with server defined during Start.loadSettings(). 
	 */
	public Elasticsearch(){
		//override value of base class
		super(ConfigElasticSearch.getEndpoint(Config.defaultRegion), ConfigElasticSearch.getAuthType(), ConfigElasticSearch.getAuthData());
	}
	public Elasticsearch(String server){
		//the constructor is not supported in this explicit implementation
		throw new RuntimeException("Not allowed in this explicit implementation. Use 'core.database.Elasticsearch' instead.");
	}
	
	//everything here moved to: net.b07z.sepia.server.core.database.Elasticsearch
}
