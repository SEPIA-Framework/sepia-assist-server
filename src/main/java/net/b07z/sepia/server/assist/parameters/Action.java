package net.b07z.sepia.server.assist.parameters;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.interpreters.NluInput;
import net.b07z.sepia.server.assist.interpreters.NluResult;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interviews.InterviewData;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.assistant.PARAMETERS;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class Action implements ParameterHandler{
	
	public static enum Type {
		on,
		off,
		pause,
		set,
		toggle,
		increase,
		decrease,
		show,
		add,
		remove,
		create,
		edit
	}
	
	//-------data-------
	public static HashMap<String, String> actions_de = new HashMap<>();
	public static HashMap<String, String> actions_en = new HashMap<>();
	static {
		actions_de.put("<on>", "anschalten");
		actions_de.put("<off>", "ausschalten");
		actions_de.put("<pause>", "pausieren");
		actions_de.put("<set>", "setzen");
		actions_de.put("<toggle>", "umschalten");
		actions_de.put("<increase>", "raufsetzen");
		actions_de.put("<decrease>", "runtersetzen");
		actions_de.put("<show>", "zeigen");
		actions_de.put("<add>", "hinzufügen");
		actions_de.put("<remove>", "entfernen");
		actions_de.put("<open>", "öffnen");
		actions_de.put("<close>", "schließen");
		actions_de.put("<create>", "erstellen");
		actions_de.put("<edit>", "editieren");
		
		actions_en.put("<on>", "turn on");
		actions_en.put("<off>", "turn off");
		actions_en.put("<pause>", "pause");
		actions_en.put("<set>", "set");
		actions_en.put("<toggle>", "toggle");
		actions_en.put("<increase>", "increase");
		actions_en.put("<decrease>", "decrease");
		actions_en.put("<show>", "show");
		actions_en.put("<add>", "add");
		actions_en.put("<remove>", "remove");
		actions_en.put("<open>", "open");
		actions_en.put("<close>", "close");
		actions_en.put("<create>", "create");
		actions_en.put("<edit>", "edit");
	}
	/**
	 * Translate generalized value.
	 * If generalized value is unknown returns empty string.
	 * @param value - generalized value 
	 * @param language - ISO language code
	 */
	public static String getLocal(String value, String language){
		String localName = "";
		String valueWithBrackets = value;
		if (!value.startsWith("<")){
			valueWithBrackets = "<" + value + ">";
		}
		if (language.equals(LANGUAGES.DE)){
			localName = actions_de.get(valueWithBrackets);
		}else if (language.equals(LANGUAGES.EN)){
			localName = actions_en.get(valueWithBrackets);
		}
		if (localName == null){
			Debugger.println("Action.java - getLocal() has no '" + language + "' version for '" + value + "'", 3);
			return "";
		}
		return localName;
	}
	//------------------

	NluInput nluInput;
	User user;
	String language;
	boolean buildSuccess = false;
	
	//keep that in mind
	String found = "";		//exact (not generalized) string found during extraction (or guess?)
	
	@Override
	public void setup(NluInput nluInput) {
		this.nluInput = nluInput;
		this.user = nluInput.user;
		this.language = nluInput.language;
	}
	@Override
	public void setup(NluResult nluResult) {
		this.nluInput = nluResult.input;
		this.user = nluResult.input.user;
		this.language = nluResult.language;
	}
	
	@Override
	public String extract(String input) {
		String action = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.ACTION);
		if (pr != null){
			action = pr.getExtracted();
			this.found = pr.getFound();
			
			return action;
		}
		
		String on, off, pause, increase, decrease, set, toggle, show, add, remove, create, edit;
		//German
		if (language.matches(LANGUAGES.DE)){
			on = "(mach|schalte|dreh) .*\\b(an|ein)|"
					+ "^\\w+\\b (an$|ein$)|"
					+ "oeffne|oeffnen|aktiviere|aktivieren|starte|starten|start|lade|laden|"
					+ "anschalten|einschalten|anmachen|an machen|aufmachen|auf machen|aufdrehen|auf drehen|spielen|spiele|spiel|abspielen|"
					+ "ausfuehren|aufrufen";
			off = "(mach|schalte|dreh) .*\\b(aus)|"
					+ "^\\w+\\b (aus$)|"
					+ "schliessen|schliesse|deaktivieren|deaktiviere|"
					+ "beenden|beende|ausschalten|aus schalten|ausmachen|aus machen|ausdrehen|aus drehen|stop(pen|pe|p|)|exit";
			pause = "pausieren|pause|anhalten|halte .*\\b(an)";
			increase = "(mach|dreh) .*\\b(auf|hoch)|"
					+ "(?<!(wie ))hoch|rauf|hoeher|groesser|erhoehen|aufdrehen|erhoehe|verstaerk(en|e)|heller|(?<!(ist ))schneller|(?<!(ist ))staerker|waermer|warm|lauter|laut";
			decrease = "(mach|dreh) .*\\b(runter|aus)|"
					+ "runterdrehen|runter|kleiner|niedriger|erniedrigen|erniedrige|abschwaechen|schwaech(er|en|e)|senk(en|e|)|dunkler|dimmen|dimme|(?<!(wie ))langsam|langsamer|kaelter|(?<!(wie ))kalt|leiser|leise";
			set = "setzen|setze|stelle|stellen|auswaehlen|waehlen|waehle|"
					+ "erinnere|weck(e|)|"
					+ "^lautstaerke auf ";
			toggle = "umschalten|schalten|schalte";
			show = "anzeigen|zeig|zeigen|check|checken|was sagt|wieviel|status";
			add = "fuege .*\\bhinzu|hinzufuegen|ergaenze|ergaenzen|eintragen|trage .*\\bein|"
					+ "auf .*\\b(\\w*list(e|)|\\w*zettel|\\w*note(s|))";		//tricky one for lists ... user can "mean" add, but not say it (milk on my list)
			remove = "entferne|entfernen|loesche|loeschen|nimm .*\\bvon";
			create = "erstellen|erstelle";
			edit = "(aendern|aendere|bearbeite(n|))(?! (" + add + "))"; 		//we need this now to compensate for the more exotic "add" actions
			
		//English and other
		}else{
			on = "(make|switch|turn) .*\\b(on)|"
					+ "^\\w+\\b (on$)|"
					+ "open|activate|start|play|load|run|execute|call";
			off = "(make|switch|turn) .*\\b(off)|"
					+ "^\\w+\\b (off$)|"
					+ "close|deactivate|end|exit|quit|stop|shut\\b.*? down";
			pause = "pause|onhold|on hold";
			increase = "(make|switch|turn) .*\\b(up)|"
					+ "^\\w+\\b (up$)|"
					+ "upwards|higher|bigger|increase|amplify|brighter|(?<!(is ))faster|(?<!(is ))stronger|fast|warmer|warm|louder|loud";
			decrease = "(make|switch|turn) .*\\b(down)|"
					+ "^\\w+\\b (down$)|"
					+ "downwards|smaller|lower|decrease|reduce|weaker|darker|dim|slow|(?<!(is ))slower|colder|cold|quieter|quiet";
			set = "set|put|select|choose|"
					+ "remind (\\w+) to|wake|"
					+ "^volume to ";
			toggle = "toggle|switch";
			show = "show|shows|display|check|what does .* say|how much|status";
			add = "add|enter|"
					+ "on .*\\b(\\w*list|\\w*note(s|))";		//tricky one for lists ... user can "mean" add, but not say it (milk on my list)
			remove = "remove|delete|take .*\\boff";
			create = "create|make";
			edit = "(change|edit)(?! (" + add + "))";			//we need this now to compensate for the more exotic "add" actions
		}
		
		String extracted = NluTools.stringFindFirst(input, set + "|" + on + "|" + off + "|" + pause + "|"
						+ increase + "|" + decrease + "|" + toggle + "|" + show + "|" + add + "|" + remove
						+ "|" + create + "|" + edit);
		
		if (!extracted.isEmpty()){
			//SET/TOGGLE 1
			if (NluTools.stringContains(extracted, set)){
				action = "<" + Type.set + ">";
			//ON
			}else if (NluTools.stringContains(extracted, on)){
				action = "<" + Type.on + ">";
			//OFF
			}else if (NluTools.stringContains(extracted, off)){
				action = "<" + Type.off + ">";
			//PAUSE
			}else if (NluTools.stringContains(extracted, pause)){
				action = "<" + Type.pause + ">";
			//INCREASE
			}else if (NluTools.stringContains(extracted, increase)){
				action = "<" + Type.increase + ">";
			//DECREASE
			}else if (NluTools.stringContains(extracted, decrease)){
				action = "<" + Type.decrease + ">";
			//SET/TOGGLE 2
			}else if (NluTools.stringContains(extracted, toggle)){
				action = "<" + Type.toggle + ">";
			//SHOW
			}else if (NluTools.stringContains(extracted, show)){
				action = "<" + Type.show + ">";
			//ADD
			}else if (NluTools.stringContains(extracted, add)){
				action = "<" + Type.add + ">";
			//REMOVE
			}else if (NluTools.stringContains(extracted, remove)){
				action = "<" + Type.remove + ">";
			//CREATE
			}else if (NluTools.stringContains(extracted, create)){
				action = "<" + Type.create + ">";
			//EDIT - note: SET might be triggered first for things like "change"
			}else if (NluTools.stringContains(extracted, edit)){
				action = "<" + Type.edit + ">";
			}else{
				action = "";
			}
		}
		this.found = extracted;
		
		//store it
		pr = new ParameterResult(PARAMETERS.ACTION, action, found);
		nluInput.addToParameterResultStorage(pr);
		
		return action;
	}
	
	@Override
	public String guess(String input) {
		return "";
	}

	@Override
	public String getFound() {
		return found;
	}

	@Override
	public String remove(String input, String found) {
		int words = found.split("\\s+").length;
		//if its only one word we can safely remove it
		if (words == 1){
			input = NluTools.stringRemoveFirst(input, Pattern.quote(found));
		}
		if (language.matches(LANGUAGES.DE)){
			input = input.replaceFirst("\\b(mach|schalte|dreh|nimm|fuege|trage)\\b", "");
			input = input.replaceFirst("\\b(an|ein|aus|auf|zu|von|hoch|runter|hinzu)$", "").trim();
		}else{
			input = input.replaceFirst("\\b(make|switch|turn|shut|take|put|add|enter)( on| off| up| down| to|)\\b", "");
			input = input.replaceFirst("\\b(on|off|up|down)$", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		//is accepted result?
		String inputLocal = getLocal(input, language);
		if (inputLocal.isEmpty()){
			return "";
		}
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			JSON.add(itemResultJSON, InterviewData.VALUE, input);
			//TODO: use GENERALIZED?
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, inputLocal);
		
		buildSuccess = true;
		return itemResultJSON.toJSONString();
	}

	@Override
	public boolean validate(String input) {
		if (input.matches("^\\{\".*\":.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
			//System.out.println("IS VALID: " + input); 		//debug
			return true;
		}else{
			return false;
		}
	}

	@Override
	public boolean buildSuccess() {
		return buildSuccess = true;
	}

}
