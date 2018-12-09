package net.b07z.sepia.server.assist.users;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.data.Address;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.data.Answer;
import net.b07z.sepia.server.core.data.CmdMap;
import net.b07z.sepia.server.core.data.UserDataList;
import net.b07z.sepia.server.core.data.UserDataList.Section;

/**
 * An interface meant to organize retrieval and writing of user data from different sources.
 * Should help to manage the data methods in User better.
 * 
 * @author Florian Quirin
 *
 */
public interface UserDataInterface {
	
	/**
	 * Get personal commands saved by the user. Can also be used for assistant-commands with the proper superuser name.
	 * @param user - personal commands of who?
	 * @param filter - HashMap with possible filters ("userId", "language", "includePublic", "searchText", etc.)
	 * @return JSONArray with results or empty Array on error
	 */
	public JSONArray getPersonalCommands(User user, Map<String, Object> filter);
	/**
	 * Set personal command.
	 * @param user - personal commands of who?
	 * @param command - name of the command
	 * @param sentence - sentence that triggers the command
	 * @param filters - HashMap with possible filters ("language", "tagged_sentence", "includePublic", "source", etc.)
	 * @return success true/false
	 */
	public boolean setPersonalCommand(User user, String command, String sentence, Map<String, Object> filters);
	/**
	 * Delete all trigger sentences (commands) that are connected to a service that has been uploaded via the SDK. It is kind of an "unregister service" method.
	 * @param user - commands of who?
	 * @param command - name of the command created by SDK
	 * @param filters - optional filters (currently not in use)
	 * @return a value indicating how many commands have been deleted or -1 if there was an error  
	 */
	public long deletePersonalSdkCommands(User user, String command, Map<String, Object> filters);
	
	/**
	 * Get personal answers saved by the user. By default it also includes system-default answers (includeSystem=true).
	 * @param user - personal answers of who?
	 * @param type - type of answer, e.g. "chat_hello_0a"
	 * @param filter - HashMap with possible filters ("includeSystem" (true/false), "language", ...tbd)
	 * @return List with results (can be empty) or null on error
	 */
	public List<Answer> getPersonalAndSystemAnswersByType(User user, String type, Map<String, Object> filter);
	
	/**
	 * Register a new service for a user.
	 * @param user - register services for who?
	 * @param serviceInfo - API_Info object with all the service configuration
	 * @param clazz - newly created class
	 * @return saved successfully? true/false
	 */
	public void registerCustomService(User user, ServiceInfo serviceInfo, ServiceInterface clazz);
	/**
	 * Get a user specific list of custom command-to-services mappings. 
	 * @param user - get custom services of who?
	 * @param filters - HashMap with possible filters like a certain command (?)
	 * @return null if there was an error, else a list with command mappings List&lt;CmdMap&gt;
	 */
	public List<CmdMap> getCustomCommandMappings(User user, Map<String, Object> filters);
	/**
	 * Add the new mappings to the existing command-services mapping overwriting old ones if needed.
	 * @param user - custom mapping for who?
	 * @param mappings - a set of CmdMap objects
	 * @return true/false
	 */
	public boolean setCustomCommandMappings(User user, Set<CmdMap> mappings);
	
	/**
	 * Set a user specific address.<br>
	 * NOTE: since this is an update call you should make sure that you set ALL required fields properly.<br> 
	 * When you change an address completely set some fields to empty strings if necessary or delete the entry first!
	 * @param user - who?
	 * @param tag - 'specialTag' given by user to address (e.g. user_home (with or without brackets))
	 * @param name - 'specialName' given to this address (e.g. Monday soccer, favorite restaurant)
	 * @param adrData - address JSON data to write/update
	 * @return JSONObject with "status" and optionally "_id" if the doc was newly created
	 */
	public JSONObject setOrUpdateSpecialAddress(User user, String tag, String name, JSONObject adrData);
	/**
	 * Get addresses of user that fits to tag. Also adds the address to the user class so it can be called
	 * with 'getTaggedAddress'.
	 * @param user - who?
	 * @param tag - 'specialTag' given by user to address (e.g. user_home (with or without brackets))
	 * @return null if there was an error, else address
	 */
	public Address getAddressByTag(User user, String tag);
	/**
	 * Delete an address by 'user' and 'id' or by special filters.
	 * @param user - list of who?
	 * @param docId - id of the document, usually found right in the address data
	 * @param filters - tbd
	 * @return a value indicating how many documents have been deleted or -1 if there was an error
	 */
	long deleteAddress(User user, String docId, Map<String, Object> filters);
	
	/**
	 * Get a user specific data list. Basically this can be anything from shopping list to reminders or alarms etc.
	 * @param user - list of who?
	 * @param section - a userDataList section that groups certain lists and prevents cross-talk when reading without indexType
	 * @param indexType - IndexType like "shopping" or "alarms" etc.
	 * @param filters - e.g. "title" of list or "resultsSize" and "resultsFrom" (for pagination)
	 * @return null if there was an error, else filtered lists of the indexType
	 */
	public List<UserDataList> getUserDataList(User user, Section section, String indexType, Map<String, Object> filters);
	/**
	 * Set or add a user data list to the database.
	 * @param user - list of who?
	 * @param section - a userDataList section that groups certain lists and prevents cross-talk when reading without indexType
	 * @param userDataList - list data with all required parameters (like "indexType", "title", "data" ... and optionally "_id")
	 * @return JSONObject with some feedback info like "success" and "_id" if a new list was created.
	 */
	public JSONObject setUserDataList(User user, Section section, UserDataList userDataList);
	/**
	 * Delete a user data list by 'user' and 'id' or by special filters.
	 * @param user - list of who?
	 * @param docId - id of the document, usually found right in the data list
	 * @param filters - tbd
	 * @return a value indicating how many documents have been deleted or -1 if there was an error
	 */
	public long deleteUserDataList(User user, String docId, Map<String, Object> filters);
}
