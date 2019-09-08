package net.b07z.sepia.server.assist.server;

import net.b07z.sepia.server.assist.workers.ServiceBackgroundTask;
import net.b07z.sepia.server.assist.workers.ServiceBackgroundTaskManager;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_ServiceBackgroundTaskManager {
	
	//NOTE: this test methods HAS to be called from package: net.b07z.sepia.server.assist.server or it will fail due to security restrictions! 

	public static void main(String[] args) {
		//Generate tasks
		ServiceBackgroundTaskManager.runOnceInBackground("uid101", "my.service.class", 1000, () -> {
			System.out.println("Task 1 is done.");
		});
		ServiceBackgroundTaskManager.runOnceInBackground("uid101", "my.service.class", 3000, () -> {
			System.out.println("Task 2 is done.");
		});
		ServiceBackgroundTaskManager.runOnceInBackground("uid101", "my.service.class", 5000, () -> {
			System.out.println("Task 3 is done.");
		});
		ServiceBackgroundTask sbt4 = ServiceBackgroundTaskManager.runOnceInBackground("uid101", "my.service.class", 10000, () -> {
			System.out.println("Task 4 is done.");
		});
		
		//Check manager
		System.out.println("Tasks in manager now: " + ServiceBackgroundTaskManager.listAllScheduledTasks());
		Debugger.sleep(1500);
		
		//Cancel specific task
		System.out.println("Cancelling task: " + sbt4.getJson().toJSONString());
		if (ServiceBackgroundTaskManager.cancelScheduledTask(sbt4.getServerId(), sbt4.getServiceId(), sbt4.getTaskId())){
			System.out.println("Cancelled task: " + sbt4.getTaskId());
		}
		System.out.println("Tasks in manager after 1.5s: " + ServiceBackgroundTaskManager.listAllScheduledTasks());
		
		//Cancel rest of tasks
		Debugger.sleep(2000);
		if (ServiceBackgroundTaskManager.cancelAllScheduledTasks()){
			System.out.println("Remaining tasks cancelled.");
		}else{
			System.err.println("FAILED to cancel remaining tasks.");
		}
		System.out.println("Tasks in manager after 3.5s: " + ServiceBackgroundTaskManager.listAllScheduledTasks());
		System.out.println("Done");
	}

}
