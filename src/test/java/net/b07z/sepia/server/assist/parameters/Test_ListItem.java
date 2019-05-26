package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.List;

import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.parameters.Test_Parameters.TestResult;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_ListItem {

	public static void main(String[] args) {
		
		Start.setupServicesAndParameters();
		String[] parametersToTest = new String[]{ PARAMETERS.LIST_TYPE, PARAMETERS.ACTION, PARAMETERS.LIST_ITEM, PARAMETERS.LIST_SUBTYPE};
		
		String language = "de";
				
		ArrayList<String> texts = new ArrayList<>();
		texts.add("Setze BUgs suchen auf meine To-Do Liste");
		texts.add("setze noch mehr Bugs suchen auf meine zu erledigen liste");
		texts.add("füge Milch meiner Einkaufslist hinzu"); 	//note: it's "list" not "liste"
		texts.add("Brot auf meine Einkaufsliste");
		texts.add("Wasser auf Shoppingliste");
		texts.add("Setze Milch, Brot, Müsli und Wasser und Pudding auf die Shoppingliste");
		texts.add("Setze Milch, Brot,Müsli und Wasser auf die Shoppingliste");
		texts.add("Setze 3,1415 auf meine Pi Liste");
		texts.add("Zeig mir die Bugs, Features und Release Liste.");
		texts.add("Zeig mir die Bugs und Features und Release Liste.");
		texts.add("Zeig mir die Bugs oder Features Liste.");

		printTestResults(texts, parametersToTest, language);
		
		language = "en";
		
		texts = new ArrayList<>();
		texts.add("Set search bugs on my To-Do list");
		texts.add("Put even more bugs searching on my to-do list");
		texts.add("add milk to my shoppinglist");
		texts.add("Bread on my shoppinglist");
		texts.add("Water on shoppinglist");
		texts.add("Put milk, water, bread and juice on my supermarket list");
		texts.add("Put 3,1415 on my Pi list");
		texts.add("Show me the Bugs, Features and Release List.");
		texts.add("Show me the Bugs and Features and Release List.");
		texts.add("Show me the Bugs or Features List.");

		printTestResults(texts, parametersToTest, language);
	}
	
	static void printTestResults(List<String> texts, String[] parametersToTest, String language){
		for (String text : texts){
			NluInput input = ConfigTestServer.getFakeInput("test", language);
			
			//normalize text
			Normalizer normalizer = Config.inputNormalizers.get(language);
			if (normalizer != null){
				input.textRaw = text;
				text = normalizer.normalizeText(text);
				input.text = text;
			}
			
			System.out.println("\ntext: " + text);
			TestResult tr = Test_Parameters.testAbstractParameterSearch(input, true, parametersToTest);
			System.out.println("score: " + tr.score);
			System.out.println("EXTRACTED: ");
			Debugger.printMap(tr.pv);
			System.out.println("BUILT: ");
			Debugger.printMap(tr.pvBuild);
			System.out.println("");
		}
	}

}
