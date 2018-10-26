package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.HashMap;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.Normalizer;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.server.ConfigTestServer;
import net.b07z.sepia.server.assist.server.Start;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;

public class Test_Parameters {

	public static void main(String[] args) {		
		Start.setupServicesAndParameters();
		
		String language = "de";
				
		ArrayList<String> texts = new ArrayList<>();
		texts.add("füge Zahnbürste meiner Supermarktliste hinzu");
		texts.add("füge Zahnbürste meiner Supermarkt Liste hinzu");
		texts.add("Zahnbürste auf meine Supermarktliste bitte");
		texts.add("Wasser auf meine Shoppingliste");
		texts.add("Wasser auf meine Shopping-Liste");
		texts.add("Wasser auf meine Shopping Liste");
		texts.add("Wasser auf meine Supermarkt Shoppingliste");
		texts.add("Wasser auf meine Supermarkt Shopping-Liste");
		texts.add("Wasser auf meine Supermarkt Shopping Liste");
		texts.add("füge Zahnarzt meiner Heute Todo Liste hinzu");
		texts.add("füge Zahnarzt meiner Heute Todo-Liste hinzu");
		texts.add("füge Zahnarzt meiner Heute To-do-Liste hinzu");
		texts.add("Zahnarzt auf meine Todoliste");
		texts.add("Zahnarzt auf meine Todo Liste");
		texts.add("Zahnarzt auf meine To-Do-Liste");
		for (String text : texts){
			NluInput input = ConfigTestServer.getFakeInput("test", language);
			
			//normalize text
			Normalizer normalizer = Config.inputNormalizers.get(language);
			if (normalizer != null){
				input.textRaw = text;
				text = normalizer.normalizeText(text);
				input.text = text;
			}
			
			TestResult tr = testAbstractParameterSearch(input, false, PARAMETERS.ACTION, 
					PARAMETERS.LIST_TYPE, PARAMETERS.LIST_SUBTYPE, PARAMETERS.LIST_ITEM);
			System.out.println("text: " + text);
			System.out.println("score: " + tr.score);
			Debugger.printMap(tr.pv);
			System.out.println("");
		}
	}
	
	//test abstract parameter search
	static TestResult testAbstractParameterSearch(NluInput input, boolean buildParam, String... params){
		HashMap<String, String> pv = new HashMap<String, String>();
		AbstractParameterSearch aps = new AbstractParameterSearch()
				.setParameters(params)
				.setup(input, pv);
		aps.getParameters();
		
		TestResult tr = new TestResult(pv, aps);
		if (buildParam){
			tr.buildParams();
		}
		return tr;
	}
	
	//test result
	static class TestResult{
		HashMap<String, String> pv;
		HashMap<String, String> pvBuild;
		int score;
		AbstractParameterSearch aps;
		
		TestResult(HashMap<String, String> pv, AbstractParameterSearch aps){
			this.pv = pv;
			this.aps = aps;
			this.score = aps.getScore();
			this.pvBuild = new HashMap<>();
		}
		public void buildParams(){
			for (Parameter p : aps.parameters){
				ParameterHandler paramHandler = p.getHandler();
				paramHandler.setup(aps.nluInput);
				String pName = p.getName();
				String pExVal = pv.get(pName);
				if (pExVal != null && !pExVal.isEmpty()){
					System.out.println("Building '" + pName + "' with input-value: " + pExVal);
					String buildRes = paramHandler.build(pExVal);
					pvBuild.put(pName, buildRes);
				}
			}
		}
	}

}
