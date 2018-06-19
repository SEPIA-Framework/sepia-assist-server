package net.b07z.sepia.server.assist.users;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_User {
	
	//--------------MAIN---------------
	public static void main(String[] args) {
		
		long tic = Debugger.tic();
		
		//fake input
		NluInput input = ConfigTestServer.getFakeInput("test", "de");
		User user = ConfigTestServer.getTestUser(ConfigTestServer.email_id1, input, false, true);
		
		int code;
		//test demo user accounts handling
		/*
		System.out.println("-----ACCOUNT TESTING------");
		Account_Demo.initializeStorage();
		Account_Interface data = new Account_Demo();
		data.getInfos(input.user, ACCOUNT.LIST_SHOPPING);
		ArrayList<String> sList= (ArrayList<String>) input.user.info.get(ACCOUNT.LIST_SHOPPING);
		printList(sList);
		sList.add("Pizza");
		data.setInfoObject(input.user, ACCOUNT.LIST_SHOPPING, sList);
		sList= (ArrayList<String>) input.user.info.get(ACCOUNT.LIST_SHOPPING);
		printList(sList);
		*/
		
		//test geo coding 
		/*
		System.out.println("-----ACCOUNT + GEO CODING TEST------");
		Tools_GeoCoding.test_get_address("-33.956", "18.462", "de");
		Tools_GeoCoding.test_get_coordinates("koeln", "de");
		*/
		
		//test Contacts matching
		/*
		Object[] res = User.containsContact("Jim Smith", input.user);
		System.out.println("match: " + res[0]);
		System.out.println("all: " + res[1]);
		System.out.println("all meta: " + res[2]);
		System.out.println("N: " + res[3]);
		System.out.println("N_best: " + res[4]);
		System.out.println("best_score: " + res[5]);
		if (((double)res[5]) == 1.0){
			JSONObject meta = ((ArrayList<JSONObject>) res[2]).get((int) res[4]);
			System.out.println("meta JSON: " + meta);
			if (meta != null){
				String id = (String) meta.get("item");
				System.out.println("id: " + id);
				//get info of contact
				HashMap<String, Object> infos = DB.getAccountInfos(id, ACCOUNT.INFOS);
				System.out.println("id infos: " + infos);
			}
		}
		*/
		
		System.out.println("Total time needed: " + Debugger.toc(tic) + "ms");
	}
}
