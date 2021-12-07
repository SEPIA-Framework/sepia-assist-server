package net.b07z.sepia.server.assist.users;

import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.database.DB;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.users.SharedAccessItem;

/**
 * Class to help get shared access permissions from user account etc..
 * 
 * @author Florian Quirin
 *
 */
public class SharedAccess {
	
	public static final String DT_REMOTE_ACTIONS = "remoteActions";		//NOTE: "remoteActions" not "remoteAction" !

	/**
	 * Check if user has a certain shared access permission set.
	 * @param userIdToCheck - user ID of receiver (that has to give permission)
	 * @param dataType - parent request type, e.g. {@link #DT_REMOTE_ACTIONS}
	 * @param sharedAccessItem - detailed info about required access permission
	 * @return
	 */
	public static boolean checkPermissions(String userIdToCheck, String dataType, SharedAccessItem sharedAccessItem){
		String senderId = sharedAccessItem.getUser();
		if (Is.nullOrEmpty(senderId)){
			throw new RuntimeException("'senderId' required!");
		}
		//check if access is allowed
		boolean allowAccess = false;
		Map<String, Object> accPermsData = DB.getAccountInfos(userIdToCheck, ACCOUNT.SHARED_ACCESS_PERMISSIONS);
		JSONObject accPerms = (accPermsData != null)? ((JSONObject) accPermsData.get(ACCOUNT.SHARED_ACCESS_PERMISSIONS)) : null;
		if (accPerms != null && accPerms.containsKey(dataType)){
			JSONArray remoteAccPerms = (JSONArray) accPerms.get(dataType);
			//System.out.println("remoteAccPerms: " + remoteAccPerms);		//DEBUG
			if (remoteAccPerms != null){
				for (Object obj : remoteAccPerms){
					//allow sender to execute RA for receiver?
					SharedAccessItem foundSai = SharedAccessItem.fromJson((JSONObject) obj);
					//System.out.println("foundSai: " + foundSai);			//DEBUG
					String allowedUser = foundSai.getUser();
					if (allowedUser != null && allowedUser.equals(senderId)){
						String allowedDeviceId = foundSai.getDevice();
						//device ID null or same?
						if (Is.nullOrEmpty(allowedDeviceId) || allowedDeviceId.equals(sharedAccessItem.getDevice())){
							JSONObject allowedDetails = foundSai.getDetails();
							if (Is.nullOrEmpty(allowedDetails)){
								//no details means no further restrictions
								allowAccess = true;
								break;
							}else{
								//given details requires identity check (all keys and values of 'allowedDetails')
								JSONObject reqDetails = sharedAccessItem.getDetails();
								if (Is.notNullOrEmpty(reqDetails)){
									//TODO: test and improve?
									int different = allowedDetails.size();
									for (Object ko : allowedDetails.entrySet()){
										Object refVal = allowedDetails.get(ko);
										Object checkVal = reqDetails.get(ko);
										if (refVal != null && checkVal != null && refVal.equals(checkVal)){
											different--;
										}
									}
									if (different == 0){
										allowAccess = true;
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		return allowAccess;
	}
}
