package net.b07z.sepia.server.assist.server;

import java.util.Map.Entry;

import net.b07z.sepia.server.core.tools.FilesAndStreams;

public class Test_SetupYamlConfig {

	public static void main(String[] args) throws Exception {
		//Load settings
		Start.loadSettings(args);

		//Get YAML
		SetupYamlConfig yaml = FilesAndStreams.readYamlFile(Config.xtensionsFolder + "/auto-setup-template.yaml", SetupYamlConfig.class);
		
		//Print result
		System.out.println("YAML result:\n");
		System.out.println("Tasks: " + yaml.getTasks());
		System.out.println("Users:");
		for (Entry<String, SetupUserData> e : yaml.getUsers().entrySet()){
			System.out.println("  " + e.getKey() + ":");
			System.out.println("    " + e.getValue());
		}
		System.out.println("DNS:");
		for (Entry<String, String> e : yaml.getDns().entrySet()){
			System.out.println("  " + e.getKey() + "=" + e.getValue());
		}
		
		System.out.println("\nTest user:");
		SetupUserData sud = new SetupUserData();
		sud.setPassword("<random>");
		System.out.println("Email or default: " + sud.getEmailOrDefault("test2"));
		System.out.println("Password or random: " + sud.getPasswordOrRandom());
		System.out.println("Roles or default: " + sud.getRolesOrDefault());
	}

}
