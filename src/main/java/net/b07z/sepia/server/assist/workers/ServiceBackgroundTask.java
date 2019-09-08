package net.b07z.sepia.server.assist.workers;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * A background task that can be started from a service.
 * 
 * @author Florian Quirin
 *
 */
public class ServiceBackgroundTask {

	private String serverId;
	private String serviceId;
	private String taskId;
	
	private ScheduledFuture<?> future;
	private BooleanSupplier cancelFunction;
	
	public ServiceBackgroundTask(String serviceId, String taskId){
		this.serviceId = serviceId;
		this.taskId = taskId;
		this.serverId = Config.localName;
	}
	public ServiceBackgroundTask(String serviceId, String taskId, ScheduledFuture<?> future, BooleanSupplier cancelFunction){
		this(serviceId, taskId);
		this.future = future;
		this.cancelFunction = cancelFunction;
	}
	
	/**
	 * Cancel scheduled task.<br> 
	 * Note: this will usually only stop a scheduled task BEFORE it started, but can be more advanced depending on the cancel function defined.  
	 */
	public boolean cancelTask(){
		if (this.cancelFunction != null){
			return this.cancelFunction.getAsBoolean();
		}else{
			return false;
		}
	}
	/**
	 * Returns true if task is completed, cancelled or terminated in some way.
	 */
	public boolean isDone(){
		return future.isDone();
	}
	/**
	 * Returns remaining time until execution in milliseconds. Zero or negative means it is already over. 
	 */
	public long timeToExecution(){
		return future.getDelay(TimeUnit.MILLISECONDS);
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServerId() {
		return serverId;
	}
		
	public String getTaskId() {
		return taskId;
	}
		
	public JSONObject getJson(){
		return JSON.make(
			"serverId", this.serverId,
			"serviceId", this.serviceId,
			"taskId", this.taskId
		);
	}
}
