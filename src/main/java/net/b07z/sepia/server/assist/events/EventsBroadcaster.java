package net.b07z.sepia.server.assist.events;

import java.util.HashSet;
import java.util.Set;

import net.b07z.sepia.server.assist.messages.Clients;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.assist.workers.ThreadManager;
import net.b07z.sepia.server.core.data.UserDataList.Section;
import net.b07z.sepia.server.core.tools.JSON;
import net.b07z.sepia.websockets.common.SocketMessage.RemoteActionType;

/**
 * Method to broadcast events like changed user data (e.g. created/deleted timers).
 * 
 * @author Florian Quirin
 *
 */
public class EventsBroadcaster {

	public enum ChangeEvent {
		timeEvents, 		//related: Section.timeEvents
		addresses			//related to? Section.personalInfo
	}
	
	/**
	 * Convenience method to build a set of change events.
	 * @param changeEvents - Arbitrary number of {@link ChangeEvent}s
	 * @return set of change events
	 */
	public static Set<String> buildChangeEventsSet(ChangeEvent... changeEvents){
		Set<String> ces = new HashSet<>();
		for (ChangeEvent ce : changeEvents){
			ces.add(ce.name());
		}
		return ces;
	}
	
	/**
	 * Send a notification to one or more clients about changed user data. For example a client could be notified to reload the alarms list.<br>
	 * NOTE: This method will send the events in a separate thread.
	 * @param changeEvents - Set of Strings representing a {@link ChangeEvent}
	 * @param originUser - user who triggered the event
	 * @param originDeviceId - device that triggered the event (and usually does not need to be notified again)
	 */
	public static void broadcastBackgroundDataSyncNotes(Set<String> changeEvents, User originUser, String originDeviceId){
		for (String ev : changeEvents){
			//supported for now:
			if (ev.equals(ChangeEvent.timeEvents.name())){
				timeEventsUpdateNote(originUser.getUserID(), originDeviceId);
			}
			//TODO: add more (here and in client app)
			//TODO: combine multiple notes (when more are added)
		}
	}
	
	private static void timeEventsUpdateNote(String userId, String senderDeviceId){
		long delay = 500;
		ThreadManager.scheduleBackgroundTaskAndForget(delay, () -> {
			Clients.sendAssistantRemoteAction(userId, RemoteActionType.sync.name(), JSON.make(
					"events", Section.timeEvents.name(),
					"forceUpdate", false
				).toJSONString(), 
				"<all>", "", senderDeviceId
			);
		});
	}
}
