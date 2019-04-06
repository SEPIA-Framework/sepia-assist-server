package net.b07z.sepia.server.assist.users;

import java.util.Arrays;
import java.util.List;

import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.interpreters.NluTools;

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
	
	//--subs--
	
	//names
	public static final String USER_NAME_FIRST = USER_NAME + "." + Name.FIRST;		//first name
	public static final String USER_NAME_LAST = USER_NAME + "." + Name.LAST;		//last name
	public static final String USER_NAME_NICK = USER_NAME + "." + Name.NICK;		//nick name
	
	//infos
	public static final String USER_BIRTH = INFOS + ".birth";						//birthday - CAREFUL! Its hard coded inside account load basics
	public static final String USER_PREFERRED_LANGUAGE = INFOS + ".lang_code";		//preferred language ISO code - CAREFUL! Its hard coded inside account load basics
	public static final String USER_GENDER = INFOS + ".gender";						//gender - not (yet?) in basics
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
				ADDRESSES + "(\\..*|$)"
			);
		if (flexKey.isEmpty()){
			return false;
		}else{
			return true;
		}
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

}
