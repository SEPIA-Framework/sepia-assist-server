package net.b07z.sepia.server.assist.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.IndexType;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_ListDataCRUD {

	public static void main(String[] args){
		
		//init
		Start.loadSettings(new String[]{"--test"});
		Config.setupDatabases(false);
		
		//User and list type
		String userId = "uid1007";
		Section section = Section.productivity; //Section.timeEvents;
		String indexType = IndexType.newsFavorites.name(); //IndexType.alarms.name();
		Map<String, Object> filters = new HashMap<>();
		
		//-------- Create list entries --------
		System.out.println("\nCreating lists ...");
		UserDataList udl1 = new UserDataList(userId, section, indexType, "My test news 1", JSON.makeArray(
				JSON.make("outlet", "Test Outlet 1", "outletId", 1),
				JSON.make("outlet", "Test Outlet 2", "outletId", 2)
		));
		System.out.println(DB.setListData(null, userId, section, indexType, udl1.getJSON()));
		
		//-------- Read one or more lists --------
		Debugger.sleep(1000);
		System.out.println("\nReading lists ...");
		List<UserDataList> foundLists = DB.getListData(userId, section, indexType, filters);
		
		System.out.println("\nFound lists: " + foundLists.size());
		List<String> idsToDelete = new ArrayList<>();
		JSONObject listToUpdate = null;
		for (UserDataList udl : foundLists){
			JSON.prettyPrint(udl.getJSON());
			idsToDelete.add(udl.getId());
			if (idsToDelete.size() == 1){
				listToUpdate = udl.getJSON();
			}
		}
		
		//-------- Update one item --------
		System.out.println("\nUpdating item in lists ...");
		JSONObject listDataEntry = new JSONObject();
		System.out.println(DB.updateListData(userId, idsToDelete.get(0), false, listDataEntry));
		
		System.out.println("\nChecking item update ...");
		Debugger.sleep(1000);
		filters = new HashMap<>();
		filters.put("title", listToUpdate.get("title"));
		foundLists = DB.getListData(userId, section, indexType, filters);
		JSONObject updatedList = foundLists.get(0).getJSON();

		System.out.println("--- Initial list data: ");
		JSON.prettyPrint(listToUpdate);
		System.out.println("--- New list data: ");
		JSON.prettyPrint(updatedList);
		System.out.println("--- ID and timestamp: ");
		if (!updatedList.get("_id").equals(listToUpdate.get("_id")) 
				|| (JSON.getLongOrDefault(updatedList, "lastEdit", 0) <= JSON.getLongOrDefault(listToUpdate, "lastEdit", 0))){
			Debugger.sleep(50);
			System.err.println("ID or timestamp seem to be wrong!");
			Debugger.sleep(50);
		}
		System.out.println("ID: " + updatedList.get("_id") + " ?= " + listToUpdate.get("_id"));
		System.out.println("TS: " + JSON.getLongOrDefault(updatedList, "lastEdit", 0) + " ?> " + JSON.getLongOrDefault(listToUpdate, "lastEdit", 0));
		listToUpdate = updatedList;
		
		//-------- Update all items --------
		System.out.println("\nUpdating complete list ...");
		UserDataList newUdl1 = new UserDataList(userId, section, indexType, "My test news 1", JSON.makeArray(
				JSON.make("outlet", "Test Outlet 3", "outletId", 3)
		));
		System.out.println(DB.setListData(JSON.getString(listToUpdate, "_id"), userId, section, indexType, newUdl1.getJSON()));
		
		System.out.println("\nChecking item replacement update ...");
		Debugger.sleep(1000);
		filters = new HashMap<>();
		filters.put("title", listToUpdate.get("title"));
		foundLists = DB.getListData(userId, section, indexType, filters);
		updatedList = foundLists.get(0).getJSON();

		System.out.println("--- Initial list data: ");
		System.out.println(listToUpdate.get("data"));
		System.out.println("--- New list data: ");
		System.out.println(updatedList.get("data"));
				
		//-------- Delete all --------
		//TODO
		
		System.out.println("\nDONE");
	}

}
