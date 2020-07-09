package net.b07z.sepia.server.assist.events;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.messages.Clients;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.workers.ThreadManager;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.websockets.common.SocketMessage.RemoteActionType;

/**
 * Method to broadcast events like changed user data (e.g. created/deleted timers).
 * 
 * @author Florian Quirin
 *
 */
public class EventsBroadcaster {

	/**
	 * Convenience method to build a list of change events.
	 * @param changeEvents - Arbitrary number of {@link ChangeEvent}s
	 * @return list of change events
	 */
	public static List<ChangeEvent> buildChangeEventsSet(ChangeEvent... changeEvents){
		List<ChangeEvent> ces = new ArrayList<>();
		for (ChangeEvent ce : changeEvents){
			ces.add(ce);
		}
		return ces;
	}
	
	/**
	 * Send a notification to one or more clients about changed user data. For example a client could be notified to reload the alarms list.<br>
	 * NOTE: This method will send the events in a separate thread.
	 * @param changeEvents - List of {@link ChangeEvent}s
	 * @param originUser - user who triggered the event
	 * @param originDeviceId - device that triggered the event (and usually does not need to be notified again)
	 */
	public static void broadcastBackgroundDataSyncNotes(List<ChangeEvent> changeEvents, User originUser, String originDeviceId){
		for (ChangeEvent ev : changeEvents){
			//supported for now:
			ChangeEvent.Type type = ev.getType();
			if (type.equals(ChangeEvent.Type.timeEvents)){
				timeEventsUpdateNote(originUser.getUserID(), originDeviceId, ev);
			}else if (type.equals(ChangeEvent.Type.productivity)){
				productivityUpdateNote(originUser.getUserID(), originDeviceId, ev);
			}
			//System.out.println("EVENT: " + ev.toString() + " - data: " + ev.getData());		//DEBUG
			//TODO: add more (here and in client app)
			//TODO: combine multiple notes (when more are added)
		}
	}
	
	private static void timeEventsUpdateNote(String userId, String senderDeviceId, ChangeEvent cev){
		long delay = 1000;
		JSONObject data = JSON.make(
			"events", Section.timeEvents.name(),
			"forceUpdate", false
		);
		addBasicDetails(data, cev);
		scheduleRemoteActionEvent(delay, userId, senderDeviceId, data);
	}
	
	private static void productivityUpdateNote(String userId, String senderDeviceId, ChangeEvent cev){
		long delay = 500;
		JSONObject data = JSON.make(
			"events", Section.productivity.name()
		);
		addBasicDetails(data, cev);
		scheduleRemoteActionEvent(delay, userId, senderDeviceId, data);
	}
	
	private static void addBasicDetails(JSONObject data, ChangeEvent cev){
		JSONObject details = JSON.make("groupId", cev.getId());
		if (Is.notNullOrEmpty(cev.getData())){
			JSON.put(details, "eventId", cev.getData().get("eventId"));
		}
		JSON.put(data, "details", details);
	}
	
	private static void scheduleRemoteActionEvent(long delay, String userId, String senderDeviceId, JSONObject data){
		ThreadManager.scheduleBackgroundTaskAndForget(delay, () -> {
			Clients.sendAssistantRemoteAction(userId, RemoteActionType.sync.name(), data.toJSONString(), 
				"<all>", "", senderDeviceId
			);
		});
	}
}
