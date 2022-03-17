package net.b07z.sepia.server.assist.email;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.email.SendEmail;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.tools.ClassBuilder;

public class Test_Email {
	
	public static void main(String[] args) throws Exception {
		
		//Load settings
		Start.loadSettings(args);
		
		//Do some custom settings before loading the rest from file (note: file-settings overwrite custom)
		Config.redirectEmail = false;
		
		//Load interface
		SendEmail emailClient = (SendEmail) ClassBuilder.construct(Config.emailModule);

		String receiver = "test@sepia.localhost";			//replace with your email for testing
		sendRegistration(emailClient, receiver, "de");
		//sendRegistration(emailClient, receiver, "en");
		//sendChangePassword(emailClient, receiver, "de");
		//sendChangePassword(emailClient, receiver, "en");
	}
	
	public static void sendRegistration(SendEmail client, String receiver, String lang){
		String subject = "Please confirm your e-mail address and off we go";
		if (lang.equals(LANGUAGES.DE)){
			subject = "Bitte bestätige deine E-Mail Adresse und los geht's";
		}
		String message = client.loadDefaultRegistrationMessage(lang, "id", "ticketid", "token", "time");
		//-send
		int code = client.send(receiver, message, subject, null);
		System.out.println("Registration mail sent with code: " + code);
		System.out.println("Error: " + ((client.getError() != null)? client.getError().getMessage() : "-"));
	}
	
	public static void sendChangePassword(SendEmail client, String receiver, String lang){
		String subject = "Here is the link to change your password";
		if (lang.equals(LANGUAGES.DE)){
			subject = "Hier der Link zum Ändern deines Passworts";
		}
		String message = client.loadPasswordResetMessage(lang, "id", "ticketid", "token", "time");
		//-send
		int code = client.send(receiver, message, subject, null);
		System.out.println("Change password mail sent with code: " + code);
		System.out.println("Error: " + ((client.getError() != null)? client.getError().getMessage() : "-"));
	}

}
