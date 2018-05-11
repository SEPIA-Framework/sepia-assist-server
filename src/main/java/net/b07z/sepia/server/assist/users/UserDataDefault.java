package net.b07z.sepia.server.assist.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.apis.ApiInfo;
import net.b07z.sepia.server.assist.apis.ApiInterface;
import net.b07z.sepia.server.assist.assistant.Assistant;
import net.b07z.sepia.server.assist.data.Address;
import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Statistics;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.data.CmdMap;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Default implementation of UserData_Interface.
 * 
 * @author Florian Quirin
 *
 */
public class UserDataDefault implements UserDataInterface {

	//--- PERSONAL COMMANDS ---
	
	@Override
	public JSONArray getPersonalCommands(User user, Map<String, Object> filter) {
		JSONArray cmds = new JSONArray();
		try {
			if (filter == null || filter.isEmpty()){
				filter = new HashMap<>();
				filter.put("language", user.language);
			}
			filter.put("userIds", user.getUserID());
			
			cmds = DB.getCommands(filter);
			return cmds;
		
		}catch (Exception e){
			e.printStackTrace();
			return cmds;
		}
	}
	@Override
	public boolean setPersonalCommand(User user, String command, String sentence, Map<String, Object> filters){
		if (filters == null || filters.isEmpty()){
			filters = new HashMap<>();
		}
		filters.put("userIds", user.getUserID());
		if (!filters.containsKey("cmd_summary"))filters.put("cmd_summary", command + ";;");
		if (!filters.containsKey("source")) filters.put("source", "assistAPI");
		String language = (String) filters.get("language");
		if (language == null || language.isEmpty()){
			language = user.language;
		}
		
		int code = DB.setCommand(command, sentence, language, true, filters);
		return (code == 0);
	}
	@Override
	public long deletePersonalSdkCommands(User user, String command, Map<String, Object> filters){
		return DB.deleteSdkCommands(user.getUserID(), command, filters);
	}
	
	//--- PERSONAL ANSWERS ---

	@Override
	public List<Answer> getPersonalAndSystemAnswersByType(User user, String type, Map<String, Object> filter) {
		long tic = System.currentTimeMillis();
		List<Answer> answers;
		
		try {
			//filters
			String users = user.getUserID();
			String lang = user.language;
			//boolean includePublic = true;
			boolean includeSystem = true; 		//is true by default
			if (filter != null && !filter.isEmpty()){
				if (filter.containsKey("language")){
					lang = (String) filter.get("language");
				}
				/*if (filter.containsKey("includePublic")){
					includePublic = Boolean.valueOf((String) filter.get("includePublic"));
				}*/
				if (filter.containsKey("includeSystem")){
					includeSystem = Boolean.valueOf((String) filter.get("includeSystem"));
				}
			}
			//add system default answers?
			if (includeSystem){
				users += "," + Config.assistantId;
			}
			//call DB
			JSONObject res = DB.getAnswersByType(type, lang, users);
			//System.out.println("Answers Result: " + res.toJSONString()); 		//debug
			
			//check
			if (res == null){
				Debugger.println("UserData.getPersonalAndSystemAnswersByType() - Failed to get data (see log for error message)", 1);
				Statistics.add_KDB_error_hit();
				answers = null;
			}else{
				answers = new ArrayList<>();
				try {
					JSONArray ja = (JSONArray) res.get("entries");
					if (ja != null && !ja.isEmpty()){
						for (int i=0; i<ja.size(); i++){
							JSONObject ans = JSON.getJObject(ja, i);
							JSONObject source = JSON.getJObject(ans, "_source");
							answers.add(Answer.importAnswerJSON(source));
						}
					}
				}catch (Exception e){
					Debugger.println("UserData.getPersonalAndSystemAnswersByType()"
							+ " - Invalid data, type: " + type + ", language: " + lang + ", users: " + users, 1);
					Statistics.add_KDB_error_hit();
					answers = null;
				}
			}
			
			//statistics
			Statistics.add_KDB_read_hit();
			Statistics.save_KDB_read_total_time(tic);
			
			return answers;
		
		}catch (Exception e){
			Debugger.println("UserData.getPersonalAnswersByType() - Failed to get data, error: " + e.getMessage(), 1);
			e.printStackTrace();
			return null;
		}
	}
	
	//--- CUSTOM TRIGGERS MAP ---
	/*
	private static Map<String, TreeMap<String, String>> customTriggersMapAll = new ConcurrentHashMap<>();
	public static TreeMap<String, String> getCustomTriggers(User user){
		return customTriggersMapAll.get(user.getUserID());
	}
	private static void addCustomTriggers(String userId, String command, User user, API_Info serviceInfo){
		ArrayList<String> triggersDE = serviceInfo.customTriggerSentences.get(LANGUAGES.DE);
		Normalizer_Interface normalizerDE = Config.input_normalizers_light.get(LANGUAGES.DE);
		ArrayList<String> triggersEN = serviceInfo.customTriggerSentences.get(LANGUAGES.EN);
		Normalizer_Interface normalizerEN = Config.input_normalizers_light.get(LANGUAGES.EN);
		TreeMap<String, String> customTriggersMap = customTriggersMapAll.get(userId);
		if (customTriggersMap == null){
			customTriggersMap = new TreeMap<>(Collections.reverseOrder());
			customTriggersMapAll.put(userId, customTriggersMap);
		}
		if (triggersDE != null){
			for (String s : triggersDE){
				String cmd = command;
				String sent = normalizerDE.normalize_text(s);
				customTriggersMap.put(sent, cmd);
			}
		}
		if (triggersEN != null){
			for (String s : triggersEN){
				String cmd = command;
				String sent = normalizerEN.normalize_text(s);
				customTriggersMap.put(sent, cmd);
			}
		}
		Debugger.println("registerCustomService - REPLACE THIS - registered custom trigger sentences - num: " + customTriggersMap.size() + " - user: " + userId, 3);
	}
	*/
	
	//--- CUSTOM SERVICE ---

	@Override
	public void registerCustomService(User user, ApiInfo serviceInfo, ApiInterface clazz){
		String command = serviceInfo.intendedCommand;
		if (command == null || command.isEmpty() || !command.matches(Config.userIdPrefix + "\\d\\d\\d\\d+\\..*")){
			throw new RuntimeException("NO (VALID) INTENDED COMMAND!");
		}
		String userId = user.getUserID();
		
		//add service
		ArrayList<String> services = new ArrayList<>();
			services.add(clazz.getClass().getCanonicalName());
		ArrayList<String> permissions = new ArrayList<>();
			//TODO: add permissions here already?
		CmdMap cm = new CmdMap(command, services, permissions);
		Set<CmdMap> cmSet = new HashSet<>();
			cmSet.add(cm);
		boolean addedMapping = setCustomCommandMappings(user, cmSet);
		if (addedMapping){
			//check user id
			if (userId.equals(Config.assistantId)){
				//RESET buffered assistant mappings
				Assistant.customCommandsMap = null;
			}
			Debugger.println("UserData.registerCustomService - registered class: " 
					+ clazz.getClass().getCanonicalName() + ", cmd: " + command + ", user: " + userId, 3);
		}else{
			throw new RuntimeException("FAILED TO REGISTER! - possible database problem, please try again!");
		}
		
		//add triggers
		int i=0, j=0;
		for (Entry<String, List<String>> entry : serviceInfo.customTriggerSentences.entrySet()){
			List<String> triggers = entry.getValue();
			String language = entry.getKey();
			//Normalizer_Interface normalizer = Config.input_normalizers_light.get(language);
			Map<String, Object> filters = new HashMap<>();
			filters.put("language", language);
			filters.put("source", ("SDK " + userId + " " + command));
			if (triggers != null){
				for (String s : triggers){
					//String sent = normalizer.normalize_text(s);
					if (setPersonalCommand(user, command, s, filters)){
						i++; 
					}
					j++;
				}
			}
		}
		Debugger.println("UserData.registerCustomService - successfully registered " 
				+ i + " of " + j + " custom trigger sentences for user: " + userId, 3);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CmdMap> getCustomCommandMappings(User user, Map<String, Object> filters) {
		Map<String, Object> params = new HashMap<>();
		params.put("customOrSystem", CmdMap.CUSTOM);
		params.put("userIds", user.getUserID());
		List<CmdMap> customMap;
		if (user.hasCheckedInfoAbout(ACCOUNT.SERVICES_MAP_CUSTOM)){
			customMap = (List<CmdMap>) user.getInfo(ACCOUNT.SERVICES_MAP_CUSTOM);
		}else{
			customMap = DB.getCommandMappings(params);
			user.addInfo(ACCOUNT.SERVICES_MAP_CUSTOM, customMap);
		}
		//TODO: filters could be used to reduce results
		return customMap;
	}
	@Override
	public boolean setCustomCommandMappings(User user, Set<CmdMap> mappings){
		Map<String, Object> params = new HashMap<>();
		params.put("customOrSystem", CmdMap.CUSTOM);
		params.put("userIds", user.getUserID());
		
		return (DB.setCommandMappings(true, mappings, params) == 0);
	}
	
	//--- ADDRESSES ---
	
	@Override
	public JSONObject setOrUpdateSpecialAddress(User user, String tag, String name, JSONObject adrData){
		if (tag == null || tag.isEmpty()){
			throw new RuntimeException("USER - setOrUpdateSpecialAddress - requires 'tag' right now!");
		}
		//Check if address already exists:
		String docId = null;
		Address adr = user.getTaggedAddress(tag, true);
		if (adr != null){
			docId = adr.dbId;
		}
		//Update:
		JSONObject res = DB.setAddressWithTagAndName(docId, user.getUserID(), tag, name, adrData);
		if (JSON.getIntegerOrDefault(res, "code", -1) == 0){
			JSON.put(res, "status", "success");
		}else{
			JSON.put(res, "status", "fail");
		}
		return res;
	}
	@Override
	public Address getAddressByTag(User user, String tag){
		List<Address> adrList = DB.getAddressesByTag(user.getUserID(), Arrays.asList(tag), null);
		if (adrList == null){
			//connection error
			return null;
		}
		if (adrList.isEmpty()){
			//no match found
			Address adr = new Address(true); 
			user.addTaggedAddress(tag, null);
			return adr;
			
		}else{
			//invalid match
			if (adrList.size() != 1){
				throw new RuntimeException("USER - setOrUpdateSpecialAddress - inconsistency in data! "
						+ "Tag: " + tag + ", user: " + user.getUserID());
			}
			//good result
			Address adr = adrList.get(0); 
			user.addTaggedAddress(tag, adr);
			return adr;
		}
	}
	@Override
	public long deleteAddress(User user, String docId, Map<String, Object> filters) {
		String userId = user.getUserID();
		return DB.deleteAddress(userId, docId, filters);
	}
	//TODO: add delete address by tag or name ?
	
	//--- LISTS ---
	
	@Override
	public List<UserDataList> getUserDataList(User user, Section section, String indexType, Map<String, Object> filters) {
		String userId = user.getUserID();
		return DB.getListData(userId, section, indexType, filters);
	}
	@Override
	public long deleteUserDataList(User user, String docId, Map<String, Object> filters) {
		String userId = user.getUserID();
		return DB.deleteListData(userId, docId, filters);
	}
	@Override
	public JSONObject setUserDataList(User user, Section section, UserDataList userDataList) {
		String docId = userDataList._id;
		String userId = user.getUserID();
		String indexType = userDataList.indexType;
		//safety overwrite
		userDataList.section = section;
		JSONObject result = DB.setListData(docId, userId, section, indexType, userDataList.getJSON());
		if (JSON.getIntegerOrDefault(result, "code", -1) == 0){
			JSON.put(result, "status", "success");
		}else{
			JSON.put(result, "status", "fail");
		}
		return result;
	}
}
