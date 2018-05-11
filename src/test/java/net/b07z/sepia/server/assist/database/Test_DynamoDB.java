package net.b07z.sepia.server.assist.database;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.DynamoDB;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.AuthenticationDynamoDB;
import net.b07z.sepia.server.assist.users.ID;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.server.core.tools.Security;

public class Test_DynamoDB {

	static AuthenticationDynamoDB auth;
	static String table = "users";
	static String userid;
	static String pwd;
	static String idType;
	
	public static void main(String[] args) {
		
		initialize();
		
		testUserCreation();
		testUserExistence();
		testAuthentication();
		
		testQueryIndex();
		testGetItem();
	}
	
	
	public static void initialize(){
		auth = new AuthenticationDynamoDB();
		userid = "test@b07z.net";
		//userid = "uid10008";
		//userid = "+49 177 77 77 77";
		pwd = Security.hashClientPassword("dev12345");
		idType = ID.Type.email;
		//idType = ID.Type.uid;
		//idType = ID.Type.phone;
	}
	
	public static void testUserCreation(){
		JSONObject requestRes = auth.registrationByEmail(userid);
		//System.out.println(requestRes);
		if (!requestRes.containsKey("token")){
			throw new RuntimeException("Registration request failed with code: " + auth.getErrorCode());
		}
		JSON.put(requestRes, "pwd", pwd);
		if (!auth.createUser(requestRes)){
			throw new RuntimeException("Creating user failed with code: " + auth.getErrorCode());
		}
		System.out.println("user created!");
	}
	
	public static void testUserExistence(){
		String guid = auth.userExists(userid, idType);
		System.out.println("user exists? " + (guid.isEmpty()? "false" : guid));
	}
	
	public static void testAuthentication(){
		JSONObject info = new JSONObject();
			JSON.add(info, "userId", userid);
			JSON.add(info, "pwd", pwd);
			JSON.add(info, "idType", idType);
			JSON.add(info, "client", Config.defaultClientInfo);

		auth.authenticate(info);
		String userID = auth.getUserID();
		int accessLvl = auth.getAccessLevel();
		HashMap<String, Object>	basicInfo = auth.getBasicInfo();
		int	errorCode = auth.getErrorCode(); 
		
		if (errorCode != 0){
			System.out.println("auth. failed!");
		}else{
			System.out.println("auth. success!");
		}
		System.out.println("id: " + userID);
		System.out.println("accessLvl: " + accessLvl);
		System.out.println("errorCode: " + errorCode);
		System.out.print("basicInfo: ");
		if (basicInfo != null) Debugger.printMap_SO(basicInfo); else System.out.println("null");
	}
	
	public static void testQueryIndex(){
		JSONObject res = DynamoDB.queryIndex(table, "Email", userid, "uname");
		System.out.println("queryIndex res: " + res.toJSONString());
	}
	
	public static void testGetItem(){
		JSONObject res = DynamoDB.getItem(table, "Email", userid, "uname");
		System.out.println("getItem res: " + res.toJSONString());
	}
	
	//test DB build-method for complex test entry
	public static void testComplexEntryBuild(){
		//map
		HashMap<String, Object> hm = new HashMap<String, Object>();
		//sub map
		HashMap<String, Object> sub = new HashMap<String, Object>();
		sub.put("firstN", "Mister");
		sub.put("lastN", "Tester2");
		sub.put("nickN", "Boss");
		sub.put("age", new Integer(33));
		sub.put("ranking", new Integer(1));
		sub.put("relation", "brother from another mother");
		hm.put("test_contacts", sub);
		//list
		ArrayList<Object> list = new ArrayList<Object>();
		//sub list
		ArrayList<Object> sublist = new ArrayList<Object>();
		sublist.add("sub1");
		sublist.add("sub2");
		sublist.add("sub3");
		list.add(sublist);
		list.add("top1");
		list.add("top2");
		hm.put("test_lists", list);
		//check
		System.out.println(DynamoDB.mapToJSON(hm).toJSONString());
	}
	
	//test AWS DynamoDB read
	/*
	System.out.println("-----ACCOUNT TESTING READ: DynamoDB------");
	Account_Interface data = new Account_DynamoDB();
	code = data.getInfos(input.user, Config.universal_api_mng, "isTest", ACCOUNT.USER_NAME, ACCOUNT.USER_HOME, ACCOUNT.USER_WORK);
	//int code = data.getInfos(input.user, Statics.universal_api_mng, ACCOUNT.USER_NAME_FIRST, ACCOUNT.USER_HOME + "." + LOCATION.CITY);
	//int code = data.getInfos(input.user, Statics.universal_api_mng, "tests.test_lists", "tests.test_contacts.nickN");
	//data.getInfos(input.user, test_api_mng, "isTest");
	//data.getInfos(input.user, test_api_mng, ACCOUNT.USER_NAME_FIRST, ACCOUNT.USER_NAME_LAST, ACCOUNT.USER_NAME_NICK);
	//data.getInfos(input.user, test_api_mng, ACCOUNT.USER_NAME, ACCOUNT.USER_HOME, ACCOUNT.USER_WORK, "adr.whatever");
	//data.getInfos(input.user, test_api_mng, (ACCOUNT.USER_HOME +"."+ LOCATION.LAT), (ACCOUNT.USER_WORK +"."+ LOCATION.CITY));
	//data.getInfos(input.user, test_api_mng, (ACCOUNT.USER_HOME +"."+ LOCATION.POSTAL_CODE), (ACCOUNT.USER_WORK +"."+ LOCATION.STREET_NBR));
	//data.getInfos(input.user, test_api_mng, ACCOUNT.LIST_SHOPPING, ACCOUNT.LIST_TODO);
	
	System.out.println("Result code: " + code);
	System.out.println("Error code: " + test_token.getErrorCode());
	System.out.println("user access level: " + input.user.getAccessLevel());
	//System.out.println("user_name: " + input.user.user_name);
	//System.out.println("user_home: " + input.user.user_home);
	//System.out.println("user_work: " + input.user.user_work);
	//System.out.println("name in use: " + input.user.getName(test_api_mng));
	//System.out.println("to-do list: " + input.user.info.get(ACCOUNT.LIST_TODO));
	//NOTE: pwd retrieval is NOT allowed!
	System.out.println("user info content:");		printHM2(input.user.info);
	
	System.out.println("Time needed: " + Debugger.toc(tic) + "ms");
	*/
	
	//test AWS DynamoDB write
	/*
	System.out.println("-----ACCOUNT TESTING WRITE: DynamoDB------");
	//Account_Interface data = new Account_DynamoDB();
	//int code = data.setInfos(input.user, test_api_mng, new String[]{"midnight"}, new Object[]{"is not there"});
	//int code = data.setInfos(input.user, test_api_mng, new String[]{"contacts"}, new Object[]{Account_DynamoDB.mapToJSON(hm)});
	//int code = data.setInfos(input.user, test_api_mng, new String[]{"contacts", "contacts.mrtest.nickN"}, new Object[]{"mrtest", "Bossy"});
	//int code = data.setInfos(input.user, test_api_mng, new String[]{"tests"}, new Object[]{hm});
	//int code = data.setInfos(input.user, test_api_mng, new String[]{"tests.test_contacts.age","tests.test_contacts.nickN"}, new Object[]{"","Bossy"});
	//code = data.setInfos(input.user, Statics.universal_api_mng, new String[]{"midnight"}, new Object[]{"is not there"});
	//note: token write via setInfos is not allowed anymore!
	//code = data.setInfos(input.user, Statics.universal_api_mng, new String[]{ACCOUNT.TOKEN_ACCESS_LVL, ACCOUNT.TOKEN_ACCESS_LVL_TS}, new Object[]{"abc",System.currentTimeMillis()});
	code = data.setInfos(input.user, Statics.universal_api_mng, new String[]{"uname.firstN", "adr.uhome.city"}, new Object[]{"Mister","München"});
	
	System.out.println("Result code: " + code);
	System.out.println("Time needed: " + Debugger.toc(tic) + "ms");
	*/
	
	//test AWS DynamoDB deleteItem
	//TODO: not possible because of test-token ...
	/*
	System.out.println("-----ACCOUNT TESTING DELETE: DynamoDB------");
	Authentication_Interface auth = new Authentication_DynamoDB();
	code = auth.deleteUser(null)
	
	System.out.println("Result code: " + code);
	System.out.println("Time needed: " + Debugger.toc(tic) + "ms");
	*/

}
