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
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.JSON;

/**
 * Parameter handling actions like on, off, add, remove, open, close, etc..<br>
 * See {@link MediaControls} for more specific actions.
 * 
 * @author FQ
 *
 */
public class Action implements ParameterHandler {
	
	public static enum Type {
		on,
		off,
		pause,
		resume,
		cancel,
		set,
		toggle,
		increase,
		decrease,
		show,
		add,
		remove,
		create,
		edit,
		open,
		close
		//TODO: OPEN and CLOSE are currently included in ON and OFF, this might need to be updated at some point
	}
	/**
	 * Get generalized action type value in format of extraction method.
	 */
	public static String getExtractedValueFromType(Type actionType){
		return ("<" + actionType.name() + ">");
	}
	
	//-------data-------
	public static HashMap<String, String> actions_de = new HashMap<>();
	public static HashMap<String, String> actions_en = new HashMap<>();
	static {
		actions_de.put("<on>", "anschalten");
		actions_de.put("<off>", "ausschalten");
		actions_de.put("<pause>", "pausieren");
		actions_de.put("<resume>", "fortsetzen");
		actions_de.put("<cancel>", "abbrechen");
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
		actions_en.put("<resume>", "resume");
		actions_en.put("<cancel>", "cancel");
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
		
		String on, open, off, close, pause, resume, cancel, 
			increase, decrease, set, toggle, show, add, remove, create, edit;
		//German
		if (language.matches(LANGUAGES.DE)){
			on = "(mach|schalte|dreh|(setze|stelle)) .*\\b(an|ein)|"
					//+ "(^|^\\w+ |auf )(an|ein)$|"
					+ "(^| )(an|ein)$|"
					+ "aktiviere|aktivieren|starte|starten|start|lade|laden|"
					+ "anschalten|einschalten|anmachen|an machen|aufdrehen|spielen|spiele|spiel|abspielen|"
					+ "ausfuehren|aufrufen";
			open = "(mach|dreh) .*\\b(auf)|auf( |)drehen|^auf|" 
					+ "oeffne|oeffnen|auf( |)machen";
			off = "(mach|schalte|dreh|(setze|stelle)) .*\\b(aus)|"
					//+ "(^|^\\w+ |auf )(aus)$|"
					+ "(^| )(aus)$|"
					+ "deaktivieren|deaktiviere|"
					+ "beenden|beende|(aus|ab)schalten|aus schalten|ausmachen|aus machen|ausdrehen|aus drehen|stop(pen|pe|p|)|exit";
			close = "(mach|dreh) .*\\b(zu)|zu( |)drehen|^zu|"
					+ "schliessen|schliesse|zu( |)machen";
			pause = "pausieren|pause|anhalten|halte .*\\b(an)";
			resume = "fortsetzen|weiter|setze .*\\b(fort)";
			cancel = "abbrechen|absagen|storniere(n|)|(brech|sag)(e|) .*\\b(ab)";
			increase = "(mach|dreh) .*\\b(auf|hoch)|"
					+ "(?<!(wie ))hoch|rauf|hoeher|groesser|erhoehen|aufdrehen|erhoehe|verstaerk(en|e)|heller|(?<!(ist ))schneller|(?<!(ist ))staerker|waermer|warm|lauter|laut";
			decrease = "(mach|dreh) .*\\b(runter|aus)|"
					+ "runterdrehen|runter|kleiner|niedriger|erniedrigen|erniedrige|abschwaechen|schwaech(er|en|e)|senk(en|e|)|dunkler|dimmen|dimme|(?<!(wie ))langsam|langsamer|kaelter|(?<!(wie ))kalt|leiser|leise";
			set = "setzen|(setze|stelle)(?! .* (aus|an|fort)$)|stellen|auswaehlen|waehlen|waehle|"
					+ "erinnere|weck(e|)|"
					+ "^lautstaerke (von .* |)(auf )|"
					+ "^wert(e|) (von .* |)(auf )";
			toggle = "umschalten|schalten|schalte";
			show = "anzeigen|zeig(en|e|)|check(en|e|)|pruefe(n|)|was sagt|sag mir|wie( |)viel|wie ist|"
					+ "(\\w+|)status(?! .* (" + set + "|(an|aus)$))|"
					+ "welche(n|r) wert(e|) (hat|haben)|"
					+ "wert (von|der)(?! .* (" + set + "))";
			add = "fuege .*\\bhinzu|hinzufuegen|ergaenze|ergaenzen|eintragen|trage .*\\bein|"
					+ "auf .*\\b(\\w*list(e|)|\\w*zettel|\\w*note(s|))";		//tricky one for lists ... user can "mean" add, but not say it (milk on my list)
			remove = "entferne|entfernen|loesche|loeschen|nimm .*\\bvon";
			create = "erstellen|erstelle";
			edit = "(aendern|aendere|bearbeite(n|))(?! (" + add + "))"; 		//we need this now to compensate for the more exotic "add" actions
			
		//English and other
		}else{
			on = "(make|switch|turn|set) .*\\b(on)|"
					//+ "(^\\w+ )(on$)|"
					+ "(^| )(on$)|"
					+ "(^|to )(active$)|"
					+ "activate|start|play|load|run|execute|call";
			open = "open";
			off = "(make|switch|turn|set) .*\\b(off)|"
					//+ "(^|^\\w+ |to )(off$)|"
					+ "(^| )(off$)|"
					+ "deactivate|end|exit|quit|stop|shut\\b.*? down";
			close = "close";
			pause = "pause|onhold|on hold";
			resume = "resume|continue";
			cancel = "cancel|abort";
			increase = "(make|switch|turn) .*\\b(up)|"
					//+ "(^\\w+ )(up$)|"
					+ "(^| )(up$)|"
					+ "upwards|higher|bigger|increase|amplify|brighter|(?<!(is ))faster|(?<!(is ))stronger|fast|warmer|warm|louder|loud";
			decrease = "(make|switch|turn) .*\\b(down)|"
					//+ "(^\\w+ )(down$)|"
					+ "(^| )(down$)|"
					+ "downwards|smaller|lower|decrease|reduce|weaker|darker|dim|slow|(?<!(is ))slower|colder|cold|quieter|quiet";
			set = "set(?! .* (off|on)$)|put|select|choose|"
					+ "remind (\\w+) to|wake|"
					+ "^volume (of .* |)(to )|"
					+ "^value (of .* |)(to )";
			toggle = "toggle|switch";
			show = "show|shows|display|check|what does .* say|how much|(what is|whats) the|tell me|"
					+ "status|state of|"
					+ "what value(s|) ((do|does) .* | )(has|have)|value(s|) of";
			add = "add|enter|"
					+ "on .*\\b(\\w*list|\\w*note(s|))";		//tricky one for lists ... user can "mean" add, but not say it (milk on my list)
			remove = "remove|delete|take .*\\boff";
			create = "create|make";
			edit = "(change|edit)(?! (" + add + "))";			//we need this now to compensate for the more exotic "add" actions
		}
		
		String extracted = NluTools.stringFindFirst(input,
				set + "|" + on + "|" + off	+ "|" + pause + "|" + resume + "|" + cancel + "|"
				+ open + "|" + close + "|" + increase + "|" + decrease + "|" 
				+ toggle + "|" + show + "|" + add + "|" + remove
				+ "|" + create + "|" + edit);
		
		if (!extracted.isEmpty()){
			//SET/TOGGLE 1
			if (NluTools.stringContains(extracted, set)){
				action = "<" + Type.set + ">";
			//ON
			}else if (NluTools.stringContains(extracted, on)){
				action = "<" + Type.on + ">";
			//OPEN
			}else if (NluTools.stringContains(extracted, open)){
				action = "<" + Type.open + ">";
			//OFF
			}else if (NluTools.stringContains(extracted, off)){
				action = "<" + Type.off + ">";
			//CLOSE
			}else if (NluTools.stringContains(extracted, close)){
				action = "<" + Type.close + ">";
			//PAUSE
			}else if (NluTools.stringContains(extracted, pause)){
				action = "<" + Type.pause + ">";
			//RESUME
			}else if (NluTools.stringContains(extracted, resume)){
				action = "<" + Type.resume + ">";
			//CANCEL
			}else if (NluTools.stringContains(extracted, cancel)){
				action = "<" + Type.cancel + ">";
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
			input = input.replaceFirst("\\b(mach|schalte|dreh|nimm|fuege|trage|brech(e|)|sag(e|))\\b", "");
			input = input.replaceFirst("\\b(an|ein|aus|ab|auf|zu|von|hoch|runter|hinzu)$", "").trim();
		}else{
			input = input.replaceFirst("\\b(make|switch|turn|shut|take|put|add|enter)( on| off| up| down| to|)\\b", "");
			input = input.replaceFirst("\\b(on|off|up|down)$", "").trim();
		}
		return input;
	}
	
	@Override
	public String responseTweaker(String input) {
		return input;
	}

	@Override
	public String build(String input) {
		//extract again/first? - this should only happen via predefined parameters (e.g. from direct triggers)
		if (Is.notNullOrEmpty(input) && !input.startsWith("<")){
			input = extract(input);
			if (Is.nullOrEmpty(input)){
				return "";
			}
		}
		
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
		if (input.matches("^\\{\".*\"(\\s|):.+\\}$") && input.contains("\"" + InterviewData.VALUE + "\"")){
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
