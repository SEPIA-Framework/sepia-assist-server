package net.b07z.sepia.server.assist.database;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Setup;
import net.b07z.sepia.server.assist.server.Start;

public class Test_DatabaseSetup {

	public static void main(String[] args) throws Exception {
		
		//load custom config
		Start.loadSettings(new String[]{"--test"});
		Config.setupDatabases(false);
		
		//Setup DynamoDB
		if (Config.authAndAccountDB.equals("dynamo_db")){
			Setup.writeDynamoDbIndices();
		}

		//Setup Elasticsearch
		Setup.writeElasticsearchMapping(null);
	}

}
