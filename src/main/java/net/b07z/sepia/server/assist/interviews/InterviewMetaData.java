package net.b07z.sepia.server.assist.interviews;

/**
 * This class holds certain meta data to be used client-side to optimize responses,
 * for example switching ASR models via 'dialog_task' etc..
 *  
 * @author Florian Quirin
 *
 */
public class InterviewMetaData {
	
	private String dialogTask;		//default: null or "default"
	
	/**
	 * Set dialog task.
	 * @param taskName - a short task name that describes a topic etc., e.g.: "music_search" or "navigation"
	 * @return
	 */
	public InterviewMetaData setDialogTask(String taskName){
		this.dialogTask = taskName;
		return this;
	}
	
	/**
	 * Get dialog task.
	 */
	public String getDialogTask(){
		return this.dialogTask;
	}
}
