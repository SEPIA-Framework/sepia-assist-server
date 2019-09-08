package net.b07z.sepia.server.assist.users;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceAccessManager;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.JSONWriter;

public class Test_ACCOUNT {

	public static void main(String[] args) {
		ServiceAccessManager sam = Config.superuserServiceAccMng;
		String firstService = (ServiceAccessManager.SDK_PACKAGE + ".uid1007.MyService").replaceAll("\\.", "_");
		String secondService = (ServiceAccessManager.SDK_PACKAGE + ".uid1008.OtherService").replaceAll("\\.", "_");
		
		//DATA
		JSONObject dataToWrite = JSON.make(
				ACCOUNT.USER_NAME, JSON.make(
						"first", "Testy",
						"last", "Testor"
				),
				ACCOUNT.SERVICES, JSON.make(
						firstService, JSON.make(
								"data", "Test1",
								"uid1007", JSON.make("tasks", "Task1")
						),
						secondService, JSON.make("data", "Test2")
				)
		);
		String[] keysToRead = new String[]{
			ACCOUNT.USER_NAME, ACCOUNT.USER_NAME_FIRST, ACCOUNT.USER_NAME_LAST,
			ACCOUNT.SERVICES + "." + firstService,
			ACCOUNT.SERVICES + "." + firstService + ".uid1007.tasks",
			ACCOUNT.SERVICES + "." + secondService
		};
		
		//WRITE
		System.out.println("check write filter (expect all keys)");
		System.out.println(JSONWriter.getPrettyString((ACCOUNT.filterServiceWriteData(sam, dataToWrite))));
		
		System.out.println("\n----------");
		System.out.println("try write with flat JSON (expect all keys)");
		JSONObject flatJson = JSON.makeFlat(dataToWrite, "", null);
		System.out.println(JSONWriter.getPrettyString((ACCOUNT.filterServiceWriteData(sam, flatJson))));
		
		//READ
		System.out.println("\n----------");
		System.out.println("check read filter (expect all keys)");
		System.out.println(ACCOUNT.filterServiceReadData(sam, keysToRead));
		
		//WRITE
		System.out.println("\n----------");
		System.out.println("try write again with 'realistic' name (expect 3 keys)");
		sam.debugOverwriteServiceName(firstService);
		System.out.println(JSONWriter.getPrettyString((ACCOUNT.filterServiceWriteData(sam, dataToWrite))));
		
		System.out.println("\n----------");
		System.out.println("try write with flat JSON and 'realistic' name (expect 3 keys)");
		System.out.println(JSONWriter.getPrettyString((ACCOUNT.filterServiceWriteData(sam, flatJson))));
		
		//READ
		System.out.println("\n----------");
		System.out.println("check read filter with 'realistic' name (expect 3 keys)");
		System.out.println(ACCOUNT.filterServiceReadData(sam, keysToRead));
		
		//FAIL
		System.out.println("\n----------\n");
		System.out.println("this should fail now: ");
		sam.debugOverwriteServiceName(secondService);
	}

}
