package net.b07z.sepia.server.assist.users;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.core.users.AuthenticationInterface;

/**
 * Common methods and actions for all {@link AuthenticationInterface} implementations.
 */
public class AuthenticationCommons {
	
	/**
	 * Get basic read fields during authentication.<br>
	 * NOTE: If you add top-level fields here you might need to update the 'create' methods for new users (in each interface implementation).
	 */
	public static String[] getBasicReadFields(){
		//when you add stuff here add it further down AND to the constructor in USER.java as well  (variable 'checked')!
		String[] basics = new String[]{
			//Essentials
			ACCOUNT.GUUID, ACCOUNT.PASSWORD, ACCOUNT.PWD_SALT, ACCOUNT.PWD_ITERATIONS, ACCOUNT.TOKENS, 
			ACCOUNT.EMAIL, ACCOUNT.PHONE, ACCOUNT.ROLES,
			//Commons
			ACCOUNT.USER_NAME, ACCOUNT.USER_PREFERRED_LANGUAGE, ACCOUNT.SHARED_ACCESS_PERMISSIONS,
			//Rare
			ACCOUNT.USER_BIRTH,	ACCOUNT.BOT_CHARACTER, ACCOUNT.USER_PREFERRED_UNIT_TEMP
		};
		return basics;
	}
	
	/**
	 * Map basic info from DB result to given HashMap.
	 * @param basicInfo
	 * @param userId
	 * @param item
	 * @param itemGetMethod
	 */
	public static void mapBasicInfo(Map<String, Object> basicInfo, String userId, JSONObject item, 
			BiFunction<JSONObject, String, Object> itemGetMethod){
		//GUUID, EMAIL and PHONE
		basicInfo.put(Authenticator.GUUID, userId);
		String email = (String) itemGetMethod.apply(item, ACCOUNT.EMAIL);
		String phone = (String) itemGetMethod.apply(item, ACCOUNT.PHONE);
		if (email != null && !email.equals("-")){
			basicInfo.put(Authenticator.EMAIL, email);
		}
		if (phone != null && !phone.equals("-")){
			basicInfo.put(Authenticator.PHONE, phone);
		}
		
		//NAME
		JSONObject foundName = (JSONObject) itemGetMethod.apply(item, ACCOUNT.USER_NAME);
		if (foundName != null){
			basicInfo.put(Authenticator.USER_NAME, foundName);
		}
		//ROLES
		JSONArray foundRoles = (JSONArray) itemGetMethod.apply(item, ACCOUNT.ROLES);
		if (foundRoles != null && !foundRoles.isEmpty()){
			basicInfo.put(Authenticator.USER_ROLES, foundRoles);
		}
		//INFOS
		JSONObject foundInfos = (JSONObject) itemGetMethod.apply(item, ACCOUNT.INFOS);
		if (foundInfos != null){
			basicInfo.put(Authenticator.USER_BIRTH, foundInfos.get("birth"));
			basicInfo.put(Authenticator.USER_LANGUAGE, foundInfos.get("lang_code"));
			basicInfo.put(Authenticator.BOT_CHARACTER, foundInfos.get("bot_char"));
			basicInfo.put(Authenticator.USER_PREFERRED_UNIT_TEMP, foundInfos.get("unit_pref_temp"));
			//shared access permissions
			basicInfo.put(Authenticator.SHARED_ACCESS_PERMISSIONS, foundInfos.get("shared_access"));
		}
	}
	
	/**
	 * Copy basic info from {@link Authenticator#getBasicInfo()} to user instance.
	 * @param user
	 * @param tokenBasicInfo
	 * @param checked
	 */
	public static void copyBasicInfo(User user, Map<String, Object> tokenBasicInfo, Map<String, Boolean> checked){
		//NOTE: this uses the generalized names BUT stores the classic ACCOUNT names in 'info'
		if (tokenBasicInfo == null){
			return;
		}
		//IDs
		if (tokenBasicInfo.containsKey(Authenticator.EMAIL)){
			user.email = (String) tokenBasicInfo.get(Authenticator.EMAIL);
			if (user.email.equals("-")){
				user.email = "";
			}else{
				user.info.put(ACCOUNT.EMAIL, user.email);
			}
			checked.put(ACCOUNT.EMAIL, true);
		}
		if (tokenBasicInfo.containsKey(Authenticator.PHONE)){
			user.phone = (String) tokenBasicInfo.get(Authenticator.PHONE);
			if (user.phone.equals("-")){
				user.phone = "";
			}else{
				user.info.put(ACCOUNT.PHONE, user.phone);
			}
			checked.put(ACCOUNT.PHONE, true);
		}
		//roles
		if (tokenBasicInfo.containsKey(Authenticator.USER_ROLES)){
			@SuppressWarnings("unchecked")
			List<Object> roleObjects = (List<Object>) tokenBasicInfo.get(Authenticator.USER_ROLES);
			List<String> roles = new ArrayList<>(); 
			for (Object o : roleObjects){
				roles.add(o.toString()); 		//we need to make a proper transformation
			}
			user.userRoles = roles;
			user.info.put(ACCOUNT.ROLES, roles);
			checked.put(ACCOUNT.ROLES, true);
		}
		//name
		if (tokenBasicInfo.containsKey(Authenticator.USER_NAME)){
			@SuppressWarnings("unchecked")
			Map<String, Object> nameJson = (Map<String, Object>) tokenBasicInfo.get(Authenticator.USER_NAME);
			user.userName = new Name(nameJson); 
			user.info.put(ACCOUNT.USER_NAME, nameJson); 		//Note: we gotta avoid confusion here :-|
			checked.put(ACCOUNT.USER_NAME, true);
		}
		//infos
		if (tokenBasicInfo.containsKey(Authenticator.USER_BIRTH)){
			user.info.put(ACCOUNT.USER_BIRTH, (String) tokenBasicInfo.get(Authenticator.USER_BIRTH));
			checked.put(ACCOUNT.USER_BIRTH, true);
		}
		if (tokenBasicInfo.containsKey(Authenticator.USER_LANGUAGE)){
			user.info.put(ACCOUNT.USER_PREFERRED_LANGUAGE, (String) tokenBasicInfo.get(Authenticator.USER_LANGUAGE));
			checked.put(ACCOUNT.USER_PREFERRED_LANGUAGE, true);
		}
		if (tokenBasicInfo.containsKey(Authenticator.BOT_CHARACTER)){
			user.info.put(ACCOUNT.BOT_CHARACTER, (String) tokenBasicInfo.get(Authenticator.BOT_CHARACTER));
			checked.put(ACCOUNT.BOT_CHARACTER, true);
		}
		if (tokenBasicInfo.containsKey(Authenticator.USER_PREFERRED_UNIT_TEMP)){
			user.info.put(ACCOUNT.USER_PREFERRED_UNIT_TEMP, (String) tokenBasicInfo.get(Authenticator.USER_PREFERRED_UNIT_TEMP));
			checked.put(ACCOUNT.USER_PREFERRED_UNIT_TEMP, true);
		}
		if (tokenBasicInfo.containsKey(Authenticator.SHARED_ACCESS_PERMISSIONS)){
			user.info.put(ACCOUNT.SHARED_ACCESS_PERMISSIONS, tokenBasicInfo.get(Authenticator.SHARED_ACCESS_PERMISSIONS));
			checked.put(ACCOUNT.SHARED_ACCESS_PERMISSIONS, true);
		}
	}
}
