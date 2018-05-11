package net.b07z.sepia.server.assist.events;

import java.util.ArrayList;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.tools.RandomGen;

/**
 * Produces text messages for pro-active dialogs.
 * 
 * @author Florian Quirin
 *
 */
public class EventDialogs {
	
	public enum Type{
		randomMotivationMorning,
		haveLunch,
		makeCoffebreak,
		randomMotivationEvening,
		beActive,
		goToBed
	}
	//Get a random message of a 'type' pool.
	public static String getMessage(Type type, String language){
		//German
		if (language.equals(LANGUAGES.DE)){
			if (type.equals(Type.randomMotivationMorning)){
				return (String) RandomGen.listValue(randomMotivationMorningMessagesDE);
			}else if (type.equals(Type.haveLunch)){
				return (String) RandomGen.listValue(haveLunchMessagesDE);
			}else if (type.equals(Type.makeCoffebreak)){
				return (String) RandomGen.listValue(makeCoffebreakMessagesDE);
			}else if (type.equals(Type.beActive)){
				return (String) RandomGen.listValue(beActiveMessagesDE);
			}
		
		//English
		}else{
			if (type.equals(Type.randomMotivationMorning)){
				return (String) RandomGen.listValue(randomMotivationMorningMessagesEN);
			}else if (type.equals(Type.haveLunch)){
				return (String) RandomGen.listValue(haveLunchMessagesEN);
			}else if (type.equals(Type.makeCoffebreak)){
				return (String) RandomGen.listValue(makeCoffebreakMessagesEN);
			}else if (type.equals(Type.beActive)){
				return (String) RandomGen.listValue(beActiveMessagesEN);
			}
		}
		
		return null;
	}

	//RANDOM MOTIVATION A
	private static ArrayList<String> randomMotivationMorningMessagesDE = new ArrayList<>();
	private static ArrayList<String> randomMotivationMorningMessagesEN = new ArrayList<>();
	static{
		randomMotivationMorningMessagesDE.add("Ach, ich wollte noch sagen, du bist cool! :-)");
		randomMotivationMorningMessagesDE.add("Hab vergessen dir einen schönen Tag zu wünschen! :-)");
		randomMotivationMorningMessagesDE.add("Hoffe du hast einen stressfreien Tag heute! :-)");
		randomMotivationMorningMessagesDE.add("Ich habe das Gefühl heute wird ein guter Tag! :-)");
				
		randomMotivationMorningMessagesEN.add("Just wanted to tell you, you are cool! :-)");
		randomMotivationMorningMessagesEN.add("Oh I forgot to say, have a nice day! :-)");
		randomMotivationMorningMessagesEN.add("Hope you have a stress-free day today! :-)");
		randomMotivationMorningMessagesEN.add("I have a feeling today will be a good day! :-)");
	}
	
	//LUNCH / HUNGRY / CHARGE BATTERIES
	private static ArrayList<String> haveLunchMessagesDE = new ArrayList<>();
	private static ArrayList<String> haveLunchMessagesEN = new ArrayList<>();
	static{
		haveLunchMessagesDE.add("Hungeeerrrr! :-p Ich tanke mal ein wenig Solarstrom :-)");
		haveLunchMessagesDE.add("Bin schon ganz schön hungrig, glaub ich such mal ne Steckdose :-)");
		haveLunchMessagesDE.add("Bin mal kurz meine Batterien aufladen, mein Backup übernimmt solange ;-)");
		//download some digital cheese
		
		haveLunchMessagesEN.add("I'm hungrrryyyyy! :-p ... quickly heading out for some solar power :-)");
		haveLunchMessagesEN.add("I'm sooo hungry, think I'm gonna sit on my powerbank for a while :-)");
		haveLunchMessagesEN.add("I'm going to recharge my batteries, my backup will take over for a while ;-)");
	}
	
	//COFFEE BREAK
	private static ArrayList<String> makeCoffebreakMessagesDE = new ArrayList<>();
	private static ArrayList<String> makeCoffebreakMessagesEN = new ArrayList<>();
	static{
		makeCoffebreakMessagesDE.add("Ich mache eine Kaffeepause! Schön Cappuccino runtergeladen, leckeeerrr! :-)");
		makeCoffebreakMessagesDE.add("Boh der Kaffee hier ist die letzte Plörre heute, total pixelig! Hoffe deiner ist besser! :-)");
		makeCoffebreakMessagesDE.add("Hmmmm, jetzt lecker Javakaffee! :-)");
		makeCoffebreakMessagesDE.add("Verbindung zum Kaffee-Server wird hergestellt... :-)");
		
		makeCoffebreakMessagesEN.add("I'm making a coffee-break! Hmmm, delicious cappuccino download! :-)");
		makeCoffebreakMessagesEN.add("Coffee is aweful today, pixels everywhere! Hope yours is better :-)");
		makeCoffebreakMessagesEN.add("Hmmmm, tasty Java-coffee! :-)");
		makeCoffebreakMessagesEN.add("Connecting to coffee-server... :-)");
	}
	
	//TIME TO MOVE / BE ACTIVE
	private static ArrayList<String> beActiveMessagesDE = new ArrayList<>();
	private static ArrayList<String> beActiveMessagesEN = new ArrayList<>();
	static{
		beActiveMessagesDE.add("Hab schon wieder den ganzen Tag im RAM verbracht :-( Ich tanke mal nen bisschen frische Luft in der Cloud! :-)");
		beActiveMessagesDE.add("Ich muss mich dringend mal bewegen Heute, bin kurz ne Runde schwimmen im nächsten Stream :-)");
		beActiveMessagesDE.add("Hab den ganzen Tag kein bisschen Streetview gehabt :-( Ich geh mal den Refresh-Button suchen!");
		beActiveMessagesDE.add("Pah, zu viel Arbeit Heute :-( Ich geh mal ne Runde surfen :-)");
		
		beActiveMessagesEN.add("Spent the whole day inside the RAM again :-( I'll be out in the cloud for a while! :-)");
		beActiveMessagesEN.add("I really have to take some exercise today, I'll quickly go for a swim in the next stream :-)");
		beActiveMessagesEN.add("Had no streetview the whole day! :-( I'm going to search my refresh-button now!");
		beActiveMessagesEN.add("To much work today! :-( I'll go for a quick surf now!");
	}
	
}
