package net.b07z.sepia.server.assist.server;

import java.util.List;
import java.util.Map;

/**
 * Class representing the YAML file for automatic setup settings.
 * 
 * @author FQ
 *
 */
public class SetupYamlConfig {
	
	private List<String> tasks;
	private Map<String, SetupUserData> users;
	private Map<String, String> dns;
	
	public SetupYamlConfig(){}

	public List<String> getTasks(){
		return tasks;
	}
	public void setTasks(List<String> tasks){
		this.tasks = tasks;
	}
	
	public Map<String, SetupUserData> getUsers(){
		return users;
	}
	public void setUsers(Map<String, SetupUserData> users){
		this.users = users;
	}
	
	public Map<String, String> getDns(){
		return dns;
	}
	public void setDns(Map<String, String> duckDns){
		this.dns = duckDns;
	}

	@Override
	public String toString(){
		return "Tasks: " + tasks + "\nUsers:\n" + users + "\nDns: " + dns;
	}
}
