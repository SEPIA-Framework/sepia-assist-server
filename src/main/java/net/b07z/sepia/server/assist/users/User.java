package net.b07z.sepia.server.assist.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LOCATION;
import net.b07z.sepia.server.assist.data.Address;
import net.b07z.sepia.server.assist.data.Name;
import net.b07z.sepia.server.assist.data.SentenceMatch;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.services.ServiceAccessManager;
import net.b07z.sepia.server.assist.tools.GeoCoding;
import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.ClassBuilder;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Class that manages the user info during client-server communication. Created from client info and account info.
 * The user should be created after authentication! with basic info about access level and ID.
 * This class includes a number of convenience methods to easily load user info.
 * 
 * @author Florian Quirin
 *
 */
public final class User {
	
	//note: all statics have been moved to ACCOUNT and LOCATION
	public String defaultName = "<user_name>";	//default name to use if nothing else is stored - leave it with <user_name>?
	private Map<String, Boolean> checked = new HashMap<String, Boolean>();	//tracks parameters already checked
	
	private String userId = "-1";			//unique ID of user (number, email, whatever ...), acquired during authentication, must not be changed afterwards!
	private int accessLvl = -1;			//level of access depending on user account authentication (0 is the lowest)
	private Authenticator token;			//security token - unchangeable identifier of the user
	//basics
	public String email = "";				//user email, can be used as unique id
	public String phone = "";				//user phone number, can be used as unique id
	public Name userName;
	public List<String> userRoles;
	public Address userLocation;
	private Map<String, Address> userTaggedAddresses  = new HashMap<>(); 	//map that holds addresses that have been loaded with specialTag search
	//private Map<String, Address> userNamedAddresses; 	//map that holds addresses that have been loaded with specialName search
	public long userTime = 0;
	public String language = "en";			//current language
	//more
	public Map<String, Object> info = new HashMap<>(); 	//info map for quick access (via ACCOUNT.xyz) and data that does not fit anywhere else
	//public String encrypt_key = "";			//might be useful to encrypt/decrypt user account data, could be generated from password
	
	//set account manager
	private AccountInterface accountData;
	private UserDataInterface userData;
	
	/**
	 * Create a user from scratch with no ID (-1) and no access (-1). This might be useful to create users in a database or obtain info of some sort.
	 */
	public User(){
		accountData = (AccountInterface) ClassBuilder.construct(Config.accountModule);
		userData = (UserDataInterface) ClassBuilder.construct(Config.userDataModule);
	}
	
	/**
	 * Creates the user from server input and assigns some basic info like user ID and access level obtained during authentication.
	 * <br><br> 
	 * @param input - input usually built from user URL parameters send to server
	 * @param token - authentication token acquired during authentication, contains id etc. 
	 */
	public User(NluInput input, Authenticator token){
	//public User(NLU_Input input, String user_id, int access_lvl){
		
		accountData = (AccountInterface) ClassBuilder.construct(Config.accountModule);
		userData = (UserDataInterface) ClassBuilder.construct(Config.userDataModule);
		
		//this should be the only place where you can set ID and access level!
		this.token = token;
		this.userId = token.getUserID();
		this.accessLvl = token.getAccessLevel();
		//this.user_id = user_id;
		//this.access_lvl = access_lvl;
		
		//get some temporary local user info
		if (input != null){
			if (input.userLocation != null && !input.userLocation.isEmpty()){
				userLocation = new Address(Converters.json2HashMap(JSON.parseStringOrFail(input.userLocation)));
			}
			userTime = input.userTime;
			language = input.language;
		}
		
		//get basics from account if there are some - NOTE: this uses the generalized names BUT stores the classic ACCOUNT names in 'info' 
		if (token.getBasicInfo() != null){
			//NOTE: compared to "check" and "validate" from Start.java here the keys MUST have the account values
			//IDs
			if (token.getBasicInfo().containsKey(Authenticator.EMAIL)){
				email = (String) token.getBasicInfo().get(Authenticator.EMAIL);
				if (email.equals("-")){
					email = "";
				}else{
					info.put(ACCOUNT.EMAIL, email);
				}
				checked.put(ACCOUNT.EMAIL, true);
			}
			if (token.getBasicInfo().containsKey(Authenticator.PHONE)){
				phone = (String) token.getBasicInfo().get(Authenticator.PHONE);
				if (phone.equals("-")){
					phone = "";
				}else{
					info.put(ACCOUNT.PHONE, phone);
				}
				checked.put(ACCOUNT.PHONE, true);
			}
			//roles
			if (token.getBasicInfo().containsKey(Authenticator.USER_ROLES)){
				@SuppressWarnings("unchecked")
				List<Object> roleObjects = (List<Object>) token.getBasicInfo().get(Authenticator.USER_ROLES);
				List<String> roles = new ArrayList<>(); 
				for (Object o : roleObjects){
					roles.add(o.toString()); 		//we need to make a proper transformation
				}
				this.userRoles = roles;
				info.put(ACCOUNT.ROLES, roles);
				checked.put(ACCOUNT.ROLES, true);
			}
			//name
			if (token.getBasicInfo().containsKey(Authenticator.USER_NAME)){
				@SuppressWarnings("unchecked")
				Map<String, Object> nameJson = (Map<String, Object>) token.getBasicInfo().get(Authenticator.USER_NAME);
				this.userName = new Name(nameJson); 
				info.put(ACCOUNT.USER_NAME, nameJson); 		//Note: we gotta avoid confusion here :-|
				checked.put(ACCOUNT.USER_NAME, true);
			}
			//infos
			if (token.getBasicInfo().containsKey(Authenticator.USER_BIRTH)){
				info.put(ACCOUNT.USER_BIRTH, (String) token.getBasicInfo().get(Authenticator.USER_BIRTH));
				checked.put(ACCOUNT.USER_BIRTH, true);
			}
			if (token.getBasicInfo().containsKey(Authenticator.USER_LANGUAGE)){
				info.put(ACCOUNT.USER_PREFERRED_LANGUAGE, (String) token.getBasicInfo().get(Authenticator.USER_LANGUAGE));
				checked.put(ACCOUNT.USER_PREFERRED_LANGUAGE, true);
			}
			if (token.getBasicInfo().containsKey(Authenticator.BOT_CHARACTER)){
				info.put(ACCOUNT.BOT_CHARACTER, (String) token.getBasicInfo().get(Authenticator.BOT_CHARACTER));
				checked.put(ACCOUNT.BOT_CHARACTER, true);
			}
		}
	}
	
	/**
	 * Get access to user data (that does not belong to core account).
	 */
	public UserDataInterface getUserDataAccess(){
		return userData;
	}
	
	/**
	 * Get the token saved with the User.
	 * @return
	 */
	public Authenticator getToken(){
		return token;
	}
	
	/**
	 * Get unique ID of this user.
	 * Usually it is acquired during authentication and passed to User with the token. It must not be changed afterwards!
	 * @return
	 */
	public String getUserID(){
		return userId.toLowerCase();
	}
	/**
	 * Get email ID.
	 */
	public String getEmailID(){
		return email.toLowerCase();
	}
	/**
	 * Get phone ID.
	 */
	public String getPhoneID(){
		return phone.toLowerCase();
	}
	
	/**
	 * Get access level set during authentication.
	 * @return
	 */
	public int getAccessLevel(){
		return accessLvl;
	}
	
	/**
	 * Get the users name, first look for nick then first name. Loads from account if account has not been checked yet.
	 * 
	 * @param api - ServiceAccessManager for the API that is calling this ...
	 * @return nick name or first name or default
	 */
	public String getName(ServiceAccessManager api){
		String name = defaultName;
		if (userName == null && !checked.containsKey(ACCOUNT.USER_NAME)){
			loadInfoFromAccount(api, ACCOUNT.USER_NAME);
			checked.put(ACCOUNT.USER_NAME, true);
		}
		if (userName != null){
			if (userName.nick != null && !userName.nick.isEmpty()){
				return userName.nick;
			}else{
				if (userName.first != null && !userName.first.isEmpty()){
					return userName.first;
				}
			}
		}
		return name;
	}
	
	/**
	 * Get user roles as List.
	 */
	public List<String> getUserRoles(){
		if (userRoles != null){
			return userRoles;
		}else{
			return new ArrayList<>();
		}
	}
	/**
	 * Check if user has this role.
	 */
	public boolean hasRole(String role){
		return getUserRoles().contains(role.toLowerCase());
	}
	/**
	 * Check if user has this role.
	 */
	public boolean hasRole(Role role){
		return getUserRoles().contains(role.name().toLowerCase());
	}
	
	/**
	 * Add an address with special tag (e.g. user_home) to user class.<br>
	 * NOTE: this does NOT store the address in user-data, it just adds it here temporary to be loaded in services
	 * or something.<br> 
	 * Also sets the 'checked' state.
	 * @param tag - e.g. Address.USER_HOME_TAG (user_home)
	 * @param adr - address data
	 */
	public void addTaggedAddress(String tag, Address adr){
		checked.put(ACCOUNT.ADDRESSES + "_tagged_" + tag, true);
		userTaggedAddresses.put(tag, adr);
	}
	/**
	 * Return address that has been given a special tag (like user_home).
	 * If the address is not loaded to user class yet and has not been 'checked' already 
	 * we try to load it here from user-data.
	 * @param tag - e.g. Address.USER_HOME_TAG (user_home)
	 * @param forceReload - skipped any cached results
	 * @return address or null
	 */
	public Address getTaggedAddress(String tag, boolean forceReload){
		if (tag.equals("user_location")){
			//that one is special
			return userLocation;
		}
		Address adr = userTaggedAddresses.get(tag);
		if (adr == null){
			if (forceReload || checked.get(ACCOUNT.ADDRESSES + "_tagged_" + tag) == null){
				//try to load
				adr = userData.getAddressByTag(this, tag);
			}
		}
		return adr;
	}
	
	/**
	 * Add data to "info" map of user.
	 */
	public void addInfo(String key, Object value){
		info.put(key, value);
		checked.put(key, true);
	}
	/**
	 * Clear all data from 'info' and 'checked'.
	 */
	public void clearInfo(){
		info.clear();
		checked.clear();
	}
	/**
	 * Has the key in the info-map already been or tried to be loaded?
	 */
	public boolean hasCheckedInfoAbout(String key){
		return checked.containsKey(key);
	}
	
	/**
	 * Export (part of) user account data to JSON string.
	 * @param onlyBasics - reduce information to basics: id, name, prefLanguage
	 */
	public JSONObject exportJSON(boolean onlyBasics){
		JSONObject account = new JSONObject();
		JSON.add(account, "userId", userId);
		JSON.add(account, "userName", userName.buildJSON());
		String upl = (String) info.get(ACCOUNT.USER_PREFERRED_LANGUAGE);
		if (upl != null && !upl.isEmpty()){
			JSON.add(account, "prefLanguage", upl);
		}
		if (!onlyBasics){
			JSON.add(account, "email", email);
			JSON.add(account, "phone", phone);
			JSON.add(account, "accessLevel", accessLvl);
			List<String> u_roles = Converters.object2ArrayListStr(info.get(ACCOUNT.ROLES));
			if (u_roles != null && !u_roles.isEmpty()){
				JSON.add(account, "userRoles", JSON.stringListToJSONArray(u_roles));
			}
			String ub = (String) info.get(ACCOUNT.USER_BIRTH);
			if (ub != null && !ub.isEmpty()){
				JSON.add(account, "userBirth", ub);
			}
		}
		return account;
	}
	/**
	 * Import account from JSONObject. Make sure user was empty before!
	 */
	public void importJSON(JSONObject account){
		//reset just to make sure
		info = new HashMap<String, Object>();
		//ID, LVL, NAME
		userId = (String) account.get("userId");
		email = (String) account.get("email");
		phone = (String) account.get("phone");
		accessLvl = Converters.obj2IntOrDefault(account.get("accessLevel"), -1);
		JSONObject uname = (JSONObject) account.get("userName");
		if (uname != null){
			userName = new Name(Converters.json2HashMap(uname));
			info.put(ACCOUNT.USER_NAME, userName);
			checked.put(ACCOUNT.USER_NAME, true);
		}
		//pref. LANG
		String pl = (String) account.get("prefLanguage");
		if (pl != null && !pl.isEmpty()){
			info.put(ACCOUNT.USER_PREFERRED_LANGUAGE, pl);
			checked.put(ACCOUNT.USER_PREFERRED_LANGUAGE, true);
		}
		//BIRTH
		String ub = (String) account.get("userBirth");
		if (ub != null && !ub.isEmpty()){
			info.put(ACCOUNT.USER_BIRTH, ub);
			checked.put(ACCOUNT.USER_BIRTH, true);
		}
		//ROLES
		JSONArray ja = (JSONArray) account.get("userRoles");
		ArrayList<String> all_roles = new ArrayList<>();
		if (ja != null && !ja.isEmpty()){
			for (Object o : ja){
				all_roles.add(((String) o).toLowerCase());
			}
		}
		info.put(ACCOUNT.ROLES, all_roles);
		checked.put(ACCOUNT.ROLES, true);
	}
	
	//---------ACCOUNT LOADING/SAVING---------
	
	/**
	 * Load specific (key) information of user from account database. Depending on what you load you will find it in one of the basic
	 * variables (user_name, ...) and/or in the dynamic store "info". Use user.info.get(...) afterwards. If the account has no info about
	 * a specific key the field will be left empty or null.
	 * 
	 * @param api - ServiceAccessManager for the API that is calling this ...
	 * @param keys - info array to look for in database
	 * @return resultCode (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error)
	 */
	public int loadInfoFromAccount(ServiceAccessManager api, Object... keys){
		//filter keys for already loaded and add to checked
		ArrayList<String> filtered = new ArrayList<String>();
		for (Object o : keys){
			String s = (String) o;
			if (!checked.containsKey(s)){
				checked.put(s, true);
				filtered.add(s);
			}
		}
		if (!filtered.isEmpty()){
			//load from account
			int resultCode = accountData.getInfos(this, api, filtered.toArray(new String[0]));
			return resultCode;
		}else{
			return 0;
		}
	}
	/**
	 * Save data to user account. The JSON data object will be checked key-by-key for access permission and the database document will then be
	 * updated or set. Typically a service can always write to the field {@link ServiceAccessManager#getServiceName}, but other fields might be
	 * restricted.
	 *  
	 * @param api - ServiceAccessManager for the API that is calling this ...
	 * @param data - JSON with (full or partial) document data to set/update
	 * @return resultCode (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error)
	 */
	public int saveInfoToAccount(ServiceAccessManager api, JSONObject data){
		int resultCode = accountData.setInfos(this, api, data);
		return resultCode;
	}
	/**
	 * Save key object to user account. 
	 * Please note that dots in the key will be transformed, e.g. level1.level2.key -> { "level1" : { "level2" : { "key" : value } } }.
	 *  
	 * @param api - ServiceAccessManager for the API that is calling this ...
	 * @param key - info to look for in database. Supports dot-path, see description above.
	 * @param object - object to save at key position
	 * @return resultCode (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error)
	 */
	public int saveInfoToAccount(ServiceAccessManager api, String key, Object object){
		int resultCode = accountData.setInfoObject(this, api, key, object);
		return resultCode;
	}
	/**
	 * Delete fields (keys) from user account.
	 *  
	 * @param api - ServiceAccessManager for the API that is calling this ...
	 * @param keys - fields in account to delete. Can use "." to delete single fields from objects.
	 * @return resultCode (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error)
	 */
	public int deleteInfoFromAccount(ServiceAccessManager api, Object... keys){
		ArrayList<String> filtered = new ArrayList<String>();
		for (Object o : keys){
			String s = (String) o;
			//we can introduce some checks here... (right now we just need the object -> string conversion)
			filtered.add(s);
		}
		int resultCode = accountData.deleteInfos(this, api, filtered.toArray(new String[0]));
		return resultCode;
	}
	
	/**
	 * Save some basic statistics when the user was created like last log-in and total usage. This method is asynchronous and does not
	 * return anything. Errors are written to the log. We have to keep an eye on memory to make sure the thread is not going crazy ...!
	 */
	public void saveStatistics(){
		Thread thread = new Thread(){
		    public void run(){
		    	boolean ok = accountData.writeBasicStatistics(userId);
				if (!ok){
					Debugger.println("USER STATISTICS FAILED! - USER: " + userId + " - TIME: " + System.currentTimeMillis(), 1);
				}/*else{
					Debugger.println("USER STATISTICS WRITTEN! - USER: " + user_id + " - TIME: " + System.currentTimeMillis(), 1);
				}*/
		    }
		};
		thread.start();
	}
	
	//---- return data that has been loaded ----
	
	/**
	 * Load account info first via loadInfoFromAccount(...) then use this with the desired key you used in load...
	 * Returns an object of the key value if possible or null. Type conversion has to be done manually. 
	 * @param key - key, previously loaded with loadInfo...()
	 * @return object or null
	 */
	public Object getInfo(String key){
		Object o = info.get(key);
		return o;
	}
	/**
	 * Load account info first via loadInfoFromAccount(...) then use this with the desired key you used in load...
	 * Returns a string with the key value if possible or default string. 
	 * @param key - key, previously loaded with loadInfo...()
	 * @param def - default
	 * @return string or default
	 */
	public String getInfoAsString(String key, String def){
		Object o = info.get(key);
		return (o != null) ? o.toString() : def;
	}
	/**
	 * Load account info first via loadInfoFromAccount(...) then use this with the desired key you used in load...
	 * Returns a long number with the key value if possible or default 
	 * @param key - key, previously loaded with loadInfo...()
	 * @param def - default
	 * @return long or default
	 */
	public long getInfoAsLong(String key, Long def){
		Object o = info.get(key);
		return Converters.obj2LongOrDefault(o, def);
	}
	/**
	 * Load account info first via loadInfoFromAccount(...) then use this with the desired key you used in load...
	 * Returns an integer number with the key value if possible or default! 
	 * @param key - key, previously loaded with loadInfo...()
	 * @param def - default
	 * @return integer or default
	 */
	public int getInfoAsInteger(String key, Integer def){
		Object o = info.get(key);
		return Converters.obj2IntOrDefault(o, def);
	}
	/**
	 * Load account info first via loadInfoFromAccount(...) then use this with the desired key you used in load...
	 * Returns a double number with the key value if possible or default! 
	 * @param key - key, previously loaded with loadInfo...()
	 * @param def - default
	 * @return double or default
	 */
	public double getInfoAsDouble(String key, Double def){
		Object o = info.get(key);
		return Converters.obj2DoubleOrDefault(o, def);
	}
	
	/**
	 * Load account info first via loadInfoFromAccount(...) then use this with the desired key you used in load...
	 * Returns an ArrayList[String] of the key value if possible or null.
	 * @param listKey - key to identify list, use ACCOUNT.LIST_xyz to find the right key.
	 * @return ArrayList with strings or null
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getInfoAsList(String listKey){
		Object o = info.get(listKey);
		ArrayList<String> list = (o != null)? (ArrayList<String>) info.get(listKey) : null;
		return list;
	}
	
	/**
	 * Load account info first via loadInfoFromAccount(...) then use this with the desired key you used in load...
	 * Returns an HashMap[String, String] of the key value if possible or null.
	 * @param mapKey - key to identify map, use ACCOUNT.xyz to find the right key.
	 * @return HashMap with strings or null
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, String> getInfoAsMap(String mapKey){
		Object o = info.get(mapKey);
		HashMap<String, String> map = (o != null)? (HashMap<String, String>) info.get(mapKey) : null;
		return map;
	}
	
	//--------------User Location Methods---------------
	
	/**
	 * Check if input contains a user specific location like &#60user_home&#62 etc... and returns the first match.
	 * It also loads info from the user account to User class if it has not been checked already, so you can access it right
	 * after this check with "getUserSpecificLocation(...).
	 * @param input - check this string
	 * @param user - user to check account for or null to skip account loading
	 * @return first match (location parameter) or empty string
	 */
	public static String containsUserSpecificLocation(String input, User user){
		if (input.isEmpty()){
			return "";
		}
		String match = NluTools.stringFindFirst(input, "<user_location>|<user_home>|<user_work>"); 		//TODO: make it work for all user addresses?
		if (!match.isEmpty()){
			if (user != null){
				//load from account - because Home and work are special they are easier to load :-)
				Address taggedAdr = user.getTaggedAddress(match.replaceAll("^<|>$", ""), false);
				//check for Latitude/Longitude and if it is empty, load it and save it
				if (taggedAdr != null && (
						taggedAdr.latitude == null || taggedAdr.latitude.isEmpty() ||
						taggedAdr.longitude == null || taggedAdr.longitude.isEmpty()
				)){
					//use geo locator to get lat long and store it.
					String geo_search = LOCATION.getFullAddress(user, match, ", ");
					if (geo_search != null && !geo_search.isEmpty()){
						
						//GET COORDINATES, CHECK THEM AND ADD THEM TO USER ACCOUNT
						//System.out.println("do geo search for: " + geo_search);		//debug
						
						//GET
						Map<String, Object> geo_info = GeoCoding.getCoordinates(geo_search, user.language);
						Object latitude = geo_info.get(LOCATION.LAT);
						Object longitude = geo_info.get(LOCATION.LNG);
						String f_city = (String) geo_info.get(LOCATION.CITY);
						String f_country = (String) geo_info.get(LOCATION.COUNTRY);
						String f_street = (String) geo_info.get(LOCATION.STREET);
						//System.out.println("found: " + latitude + ", " + longitude + ", city: " + f_city + ", country: " + f_country);		//debug
						
						//CHECK
						String city = user.getUserSpecificLocation(match, LOCATION.CITY);
						String country = user.getUserSpecificLocation(match, LOCATION.COUNTRY);
						String street = user.getUserSpecificLocation(match, LOCATION.STREET);
						if ( (!city.isEmpty() && f_city != null && !f_city.isEmpty()) 
								&& (!country.isEmpty() && f_country != null && !f_country.isEmpty())
								&& (!street.isEmpty() && f_street != null && !f_street.isEmpty())
							){
							//System.out.println("check: " + city + " = " + f_city + ", street: " + street + " = " + f_street +	", country: " + country + " = " + f_country);		//debug
							SentenceMatch streetMatch = new SentenceMatch(street, f_street).getEditDistance();
							double streetNoMatchProb = (double) streetMatch.editDistance/(double) Math.max(street.length(), f_street.length());
							//System.out.println("no match prob. " + streetNoMatchProb); 		//debug
							if (city.toLowerCase().trim().equals(f_city.toLowerCase().trim()) 
								&& country.toLowerCase().trim().equals(f_country.toLowerCase().trim())
								//&& street.toLowerCase().trim().equals(f_street.toLowerCase().trim())
								&& (streetNoMatchProb < 0.5f)
									){
								
								//Update data on-the-fly to have it available NOW ... then save it to DB
								taggedAdr.latitude = latitude.toString();
								taggedAdr.longitude = longitude.toString();
								taggedAdr.buildJSON();		//we need to update this because the "key"-lookup is done in JSON - TODO: upgrade
								
								//TODO: here we need to replace stuff with the new system (make it work for all addresses?)
								
								//SAVE HOME
								if (match.equals("<user_home>")){
									user.getUserDataAccess().setOrUpdateSpecialAddress(user, Address.USER_HOME_TAG, null, JSON.make(
											LOCATION.LAT, latitude,
											LOCATION.LNG, longitude,
											LOCATION.STREET, f_street
									));
									taggedAdr.latitude = latitude.toString();
									taggedAdr.longitude = longitude.toString();
									taggedAdr.street = f_street;
							
								//SAVE WORK
								}else if (match.equals("<user_work>")){
									user.getUserDataAccess().setOrUpdateSpecialAddress(user, Address.USER_WORK_TAG, null, JSON.make(
											LOCATION.LAT, latitude,
											LOCATION.LNG, longitude,
											LOCATION.STREET, f_street
									));
									taggedAdr.latitude = latitude.toString();
									taggedAdr.longitude = longitude.toString();
									taggedAdr.street = f_street;
								}
								Debugger.println("GEO TOOLS - SAVED COORDINATES: " + f_country + ", " + f_city + ", " + f_street + " - Lat.: " + latitude + ", Lng.: " + longitude, 3);		//debug
							}
						}
					}
				}
			}
			return match;
		}else{
			return "";
		}
	}
	
	/**
	 * Take input containing a user specific location tag (&#60user_home&#62 etc...) and replace it with a key value (LOCATION.CITY, etc.)
	 * from user account (needs to be loaded previously). If the user information is not available the output will be empty.
	 * @param input - text or parameter input, can be empty for current location, should be identical to user_parameter if you just want the plain parameter
	 * @param user_parameter - parameter to replace (e.g. &#60user_home&#62), can be empty if input is empty
	 * @param key_location - key to get from location string (LOCATION.CITY, LOCATION.COUNTRY ...)
	 * @return info or empty
	 */
	public String replaceUserSpecificLocation(String input, String user_parameter, String key_location){
		String new_parameter = "";
		//if input is empty get current location
		if (input.isEmpty() && !key_location.isEmpty()){
			if (userLocation != null){
				new_parameter = userLocation.getFromJsonOrDefault(key_location, "");
				input = new_parameter;
			}
			
		}else if (!user_parameter.isEmpty() && !input.isEmpty() && !key_location.isEmpty()){
			//current
			if (userLocation != null && user_parameter.matches("<user_location>")){
				new_parameter = userLocation.getFromJsonOrDefault(key_location, "");
			
			//TODO: this needs to be reworked I guess
				
			//home	
			}else if (user_parameter.matches("<user_home>")){
				Address userHome = userTaggedAddresses.get(Address.USER_HOME_TAG);
				if (userHome != null){
					new_parameter = userHome.getFromJsonOrDefault(key_location, "");
				}
				
			//work
			}else if (user_parameter.matches("<user_work>")){
				Address userWork = userTaggedAddresses.get(Address.USER_WORK_TAG);
				if (userWork != null){
					new_parameter = userWork.getFromJsonOrDefault(key_location, "");
				}
			}
			
			if (!new_parameter.isEmpty()){
				input = input.replaceAll(user_parameter, new_parameter);
			}else{
				input = "";
			}
		}else{
			input = "";
		}
		return input;
	}
	
	/**
	 * Get a specific key parameter from user locations.<br>
	 * NOTE: Needs to be loaded first!
	 * @param user_param - parameter found during "inputContainsUserLocation(..)" like <user_home> etc. ...
	 * @param key - e.g. LOCATION.CITY, LOCATION.STREET, ...
	 * @return key of home location or empty string
	 */
	public String getUserSpecificLocation(String user_param, String key){
		return replaceUserSpecificLocation(user_param, user_param, key);
	}
	/**
	 * Get a specific key parameter from user home location.<br>
	 * NOTE: Needs to be loaded first!
	 * @param key - e.g. LOCATION.CITY, LOCATION.STREET, ...
	 * @return key of home location or empty string
	 */
	public String getHomeLocation(String key){
		return getUserSpecificLocation("<user_home>", key);
	}
	/**
	 * Get a specific key parameter from user work location.<br>
	 * NOTE: Needs to be loaded first!
	 * @param key - e.g. LOCATION.CITY, LOCATION.STREET, ...
	 * @return key of work location or empty string
	 */
	public String getWorkLocation(String key){
		return getUserSpecificLocation("<user_work>", key);
	}
	/**
	 * Get a specific key parameter from user current location.<br>
	 * NOTE: Needs to be loaded first!
	 * @param key - e.g. LOCATION.CITY, LOCATION.STREET, ...
	 * @return key of current location or empty string
	 */
	public String getCurrentLocation(String key){
		return getUserSpecificLocation("", key);
	}
	
	//---------------User CONTEXT methods
	
	/**
	 * Try to get a context parameter by searching user history.
	 * @param key - PARAMETER to look for in user history
	 * @return value for key or empty string
	 */
	public String getLastContextParameter(String key){
		String context = "";
		//TODO: did we store the history in any DB?
		return context;
	}
	/**
	 * Get a context command by searching last_command(s).
	 * @param cmd - CMD to look for in user history
	 * @return cmd or empty string
	 */
	public String getLastContextCommand(String cmd){
		String context = "";
		//TODO: did we store the history in any DB?
		return context;
	}

}
