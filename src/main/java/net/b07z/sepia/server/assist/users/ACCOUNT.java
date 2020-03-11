package net.b07z.sepia.server.assist.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.services.ServiceAccessManager;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Account statics, that means basic key entry fields in database to set and retrieve certain values.
 * Everything that is not in this class must be treated carefully and tracked somewhere to avoid confusion!
 * Every change to this class can have dramatic impact on database integrity!!! because these values might be
 * used directly inside the database itself if there is no layer in between changing them. 
 * 
 * @author Florian Quirin
 *
 */
public class ACCOUNT {
	
	//Statics - many names correspond directly to User Class naming
	
	//--core account--
	public static final String GUUID = "Guuid";			//global unique user id 
	public static final String EMAIL = "Email";			//email aka second search key
	public static final String PHONE = "Phone";			//phone number aka to be third search key
	public static final String PASSWORD = "pwd";		//hashed password
	public static final String PWD_SALT = "pwd_salt";				//salt for hashed password
	public static final String PWD_ITERATIONS = "pwd_iteration";	//iterations for hashed password
	public static final String TOKENS = "tokens";		//different tokens for security purposes
	public static final String APIS = "apis";			//map to manage API accesses
	public static final String USER_NAME = "uname";		//user name as seen in User class: "<nickname>Boss<firstname>...<lastname>...";
	public static final String INFOS = "infos";			//collect info of all kinds of stuff
	public static final String ROLES = "uroles";		//user roles like "user", "developer", "tester", ...
	public static final String STATISTICS = "statistics";	//collect user statistics
	//--mixed--
	public static final String ADDRESSES = "addresses";		//all addresses of the user like home, work, pois etc.
	//--user data--
	public static final String LISTS = "lists";			//different lists storing data like to-do and shopping lists
	public static final String CONTACTS = "contacts";	//contact list
	public static final String CALENDARS = "calendars";	//Calendars
	public static final String SOCIALS = "socials";		//collect stuff that connects people, like phone, homepage, facebook, skype ...
	public static final String BANKING = "banking";		//map with all the banking stuff (Name, IBAN, BIC, balance)
	public static final String INSURANCE = "insurance";	//map with all the insurance stuff (Name, account number, etc.)
	public static final String HEALTH = "health";		//map with all the health stuff
	public static final String SERVICES = "services";	//(custom) services data, e.g. services.[my-custom-service-class-name]
	
	//--subs--
	
	//names
	public static final String USER_NAME_FIRST = USER_NAME + "." + Name.FIRST;		//first name
	public static final String USER_NAME_LAST = USER_NAME + "." + Name.LAST;		//last name
	public static final String USER_NAME_NICK = USER_NAME + "." + Name.NICK;		//nick name
	
	//infos
	public static final String USER_BIRTH = INFOS + ".birth";						//birthday - CAREFUL! Its hard coded inside account load basics
	public static final String USER_PREFERRED_LANGUAGE = INFOS + ".lang_code";		//preferred language ISO code - CAREFUL! Its hard coded inside account load basics
	public static final String USER_GENDER = INFOS + ".gender";						//gender - not (yet?) in basics
	public static final String USER_PREFERRED_UNIT_TEMP = INFOS + ".unit_pref_temp";	//preferred unit for temperature
	public static final String USER_APP_SETTINGS = INFOS + ".app_settings";				//user settings for different apps - NOTE: followed by ".deviceId"
	//infos - bot
	public static final String BOT_CHARACTER = INFOS + ".bot_char";					//preset assistant character
	//infos - app settings
	public static final String APP_SETTINGS = INFOS + ".app_settings";				//settings for user clients, sorted by device ID
	
	//special tokens
	public static final String ACCESS_LVL_TOKEN = "incAccLvl";			//token (in tokens) generated to increase access level
	public static final String ACCESS_LVL_TOKEN_TS = "incAccLvl_ts";	//time stamp of access level token
	
	//--Elasticsearch--
	
	//infos - command-to-services mappings
	public static final String SERVICES_MAP_CUSTOM = INFOS + ".servicesMap.custom";
	public static final String SERVICES_MAP_SYSTEM = INFOS + ".servicesMap.system";
	
	//addresses - can be combined with LOCATION, e.g.: USER_HOME + "." + LOCATION.city
	public static final String USER_HOME = ADDRESSES + ".uhome";	//home address
	public static final String USER_WORK = ADDRESSES + ".uwork";	//work address
	
	//lists
	/*
	public static final String LIST_SHOPPING = LISTS + ".shop";		//reference to list with shopping items
	public static final String LIST_TODO = LISTS + ".todo";			//reference to list with to-do items
	public static final String LIST_REMINDER = LISTS + ".remind";	//reference to list with reminders
	public static final String LIST_FAVORITES = LISTS + ".favs";	//reference to list with favorites/personal info !!(add + language_code)!!
	*/
	
	//------Collections to handle write access-------
	
	//allow flexible access for things like email, name, address, info (language, settings, etc.)
	public static boolean allowFlexAccess(String key){
		//TODO: Rework this?
		String flexKey = NluTools.stringFindFirst(key,
				"^(" + GUUID + "|" + EMAIL + "|" + PHONE + ")|" +
				USER_NAME + "(\\..*|$)|" +
				INFOS + "(\\..*|$)|" +
				ADDRESSES + "(\\..*|$)|" + 
				SERVICES + "(\\..*|$)"
			);
		if (flexKey.isEmpty()){
			return false;
		}else{
			return true;
		}
	}
	
	/**
	 * When a service tries to write data you can use this method to make sure it only writes what it is allowed to!<br>
	 * NOTE: Can currently check for partially allowed objects up to ONE LEVEL (e.g. 'service' and 'service.[my-class]' 
	 * but NOT 'service.[my-class].someField'. 
	 * @param sam - {@link ServiceAccessManager}
	 * @param data - JSON object to write 
	 * @return filtered JSON object
	 */
	public static JSONObject filterServiceWriteData(ServiceAccessManager sam, JSONObject data){
		return filterServiceWriteData(sam, data, null);
	}
	private static JSONObject filterServiceWriteData(ServiceAccessManager sam, JSONObject data, String prefix){
		JSONObject filteredData = new JSONObject();
		for (Object kObj : data.keySet()){
			String kOrg = kObj.toString();
			String k = Is.nullOrEmpty(prefix)? kOrg : (prefix + "." + kOrg);
			String kReduced = k.replaceFirst("(.*?\\..*?)\\..*", "$1");
			//System.out.println("key: " + kReduced); 		//DEBUG
			if (restrictWriteAccess.contains(k.replaceFirst("\\..*", "").trim())){
				//restrict access to critical database fields
				Debugger.println("Service: " + sam.getServiceName() + " - write access to '" + k + "' has been denied! (never allowed)", 3);
				continue;
			}
			if (!allowFlexAccess(k)){
				//password, tokens etc. can NOT be written here! NEVER!
				Debugger.println("Service: " + sam.getServiceName() + " - write access to '" + k + "' has been denied! (no flex access)", 3);
				continue;
			}
			if (sam.isAllowedToAccessPartially(k)){
				//we may have access to a child entry
				JSONObject subData = JSON.getJObject(data, kOrg);
				JSONObject subDataFiltered = filterServiceWriteData(sam, subData, k);
				if (Is.notNullOrEmpty(subDataFiltered)){
					JSON.put(filteredData, kOrg, subDataFiltered);
				}
			//NOTE: This is required to make the method useful for all (both) database implementations, but forces the ONE LEVEL nesting limit (mentioned above)
			}else if (sam.isAllowedToAccess(kReduced)){
				//this is explicitly allowed for services
				JSON.put(filteredData, kOrg, data.get(kOrg));
			}else{
				//no access for this service
				Debugger.println("Service: " + sam.getServiceName() + " is NOT! allowed to access field " + k, 3);
				continue;
			}
		}
		return filteredData;
	}
	/**
	 * When a service tries to read data you can use this method to make sure it only reads what it is allowed to!<br>
	 * NOTE: ...
	 * @param sam - {@link ServiceAccessManager}
	 * @param keys
	 * @return
	 */
	public static ArrayList<String> filterServiceReadData(ServiceAccessManager sam, String... keys){
		return filterServiceReadData(null, sam, keys);
	}
	private static ArrayList<String> filterServiceReadData(String prefix, ServiceAccessManager sam, String... keys){
		ArrayList<String> checkedFields = new ArrayList<>();
		for (String s : keys){
			//restrict access to database
			if (restrictReadAccess.contains(s.replaceFirst("\\..*", "").trim())){
				//password and tokens retrieval is NOT allowed! NEVER! only if you run this as an authenticator (own class)
				Debugger.println("Service: " + sam.getServiceName() + " - read access to '" + s + "' has been denied! (never allowed)", 3);
				continue;
			}
			String sReduced = s.replaceFirst("(.*?\\..*?)\\..*", "$1");
			if (!sam.isAllowedToAccess(sReduced)){
				Debugger.println("Service: " + sam.getServiceName() + " is NOT! allowed to access field " + sReduced, 3);
				continue;
			}
			checkedFields.add(s);
		}
		return checkedFields;
	}
	
	//these here MUST be restricted at any cost and can only be written by secure server methods
	public static final List<String> restrictWriteAccess = Arrays.asList(
			//NOTE: this is applied on top level, don't use nesting here
			GUUID, EMAIL, PHONE, PASSWORD, PWD_SALT, PWD_ITERATIONS, TOKENS, APIS, ROLES
	);
	//these here MUST be restricted at any cost and can only be read by secure server methods
	public static final List<String> restrictReadAccess = Arrays.asList(
			//NOTE: this is applied on top level, don't use nesting here
			PASSWORD, PWD_SALT, PWD_ITERATIONS, TOKENS, APIS, ROLES
	);
	//these can be shown to admin, e.g. when listing users
	public static final List<String> allowedToShowAdmin = Arrays.asList(
			GUUID, EMAIL, USER_NAME, ROLES, STATISTICS 			//TODO: should be extended?
	);
}
