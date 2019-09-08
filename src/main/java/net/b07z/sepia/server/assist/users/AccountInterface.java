package net.b07z.sepia.server.assist.users;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.services.ServiceAccessManager;

/**
 * Interface for any account management class.
 * 
 * @author Florian Quirin
 *
 */
public interface AccountInterface {
	
	/**
	 * Get a bunch of user values from account database at the same time to reduce traffic and write them to user variable.
	 * If a key is not found the value remains null.
	 * The database implementation has to check if the user is authenticated correctly (and not just a user created with an ID and access level)! 
	 * 
	 * @param user - the user we are looking for and the variable we modify
	 * @param api - ServiceAccessManager used to get API specific, allowed database access
	 * @param keys - string array of keys that describe database entries
	 * 
	 * @return error code (0 - no error, 1 - can't reach database, 2 - access denied (or invalid parameters), 3 - no account found, 4 - other error (e.g. wrong key combination)
	 */
	public int getInfos(User user, ServiceAccessManager api, String... keys);
	
	/**
	 * Set a bunch of user values in account database at the same time to reduce traffic. 
	 * The data JSON object will be checked key-by-key for authorized access and the database object will then be set/updated.
	 * The database implementation has to check if the user is authenticated correctly (and not just a user created with an ID and access level)!
	 * 
	 * @param user - the user we are looking for
	 * @param api - ServiceAccessManager used to get API specific, allowed database access
	 * @param data - JSON data with (partial or full) document to set/update
	 * 
	 * @return error code (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error)
	 */
	public int setInfos(User user, ServiceAccessManager api, JSONObject data);
	
	/**
	 * Get any user specific object from account database.
	 * The database implementation has to check if the user is authenticated correctly (and not just a user created with an ID and access level)!
	 * 
	 * @param user - get info about this user
	 * @param api - ServiceAccessManager used to get API specific, allowed database access
	 * @param key - the key of the database entry we are looking for
	 * 
	 * @return info object or null if it is not found
	 */
	public Object getInfoObject(User user, ServiceAccessManager api, String key);
	
	/**
	 * Set any user specific object in account database.
	 * The database implementation has to check if the user is authenticated correctly (and not just a user created with an ID and access level)!
	 * 
	 * @param user - set info about this user
	 * @param api - ServiceAccessManager used to get API specific, allowed database access
	 * @param key - the key of the database entry we want to change
	 * @param object - the object to add to the database
	 * 
	 * @return error code (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error)
	 */
	public int setInfoObject(User user, ServiceAccessManager api, String key, Object object);
	
	/**
	 * Delete one or more fields from database.
	 * 
	 * @param user - the user we are looking for
	 * @param api - ServiceAccessManager used to get API specific, allowed database access
	 * @param keys - fields in the database (can include '.' to access single fields in objects)
	 * @return error code (0 - no error, 1 - can't reach database, 2 - access denied, 3 - no account found, 4 - other error (e.g. wrong key combination)) 
	 */
	public int deleteInfos(User user, ServiceAccessManager api, String... keys);
	
	/**
	 * Write basic statistics for a user like last log-in and total usage etc. ...
	 * @param userID - user to track
	 * @return
	 */
	public boolean writeBasicStatistics(String userID);

}
