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
 * A parameter to find media player controls like "next", "stop", "play", "volume up" etc.<br> 
 * This is probably as close as it gets to a reference implementation of a {@link ParameterHandler}.
 * 
 * @author Florian Quirin
 *
 */
public class MediaControls implements ParameterHandler {
	
	public static enum Type {
		play,
		stop,
		pause,
		close,
		next,
		previous,
		resume,
		repeat,
		volume_up,
		volume_down,
		volume_set
	}
	
	//-------data-------
	public static HashMap<String, String> local_de = new HashMap<>();
	public static HashMap<String, String> local_en = new HashMap<>();
	static {
		local_de.put("<play>", "abspielen");
		local_de.put("<stop>", "stoppen");
		local_de.put("<pause>", "pausieren");
		local_de.put("<close>", "schließen");
		local_de.put("<next>", "weiter");
		local_de.put("<previous>", "zurück");
		local_de.put("<resume>", "fortsetzen");
		local_de.put("<repeat>", "wiederholen");
		local_de.put("<volume_up>", "lauter");
		local_de.put("<volume_down>", "leiser");
		local_de.put("<volume_set>", "Lautstärke auf ");
		
		local_en.put("<play>", "play");
		local_en.put("<stop>", "stop");
		local_en.put("<pause>", "pause");
		local_en.put("<close>", "close");
		local_en.put("<next>", "next");
		local_en.put("<previous>", "back");
		local_en.put("<resume>", "resume");
		local_en.put("<repeat>", "repeat");
		local_en.put("<volume_up>", "volume up");
		local_en.put("<volume_down>", "volume down");
		local_en.put("<volume_set>", "volume to ");
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
			localName = local_de.get(valueWithBrackets);
		}else if (language.equals(LANGUAGES.EN)){
			localName = local_en.get(valueWithBrackets);
		}
		if (localName == null){
			Debugger.println("MediaControls.java - getLocal() has no '" + language + "' version for '" + value + "'", 3);
			return "";
		}
		return localName;
	}
	//------------------

	public User user;
	public NluInput nluInput;
	public String language;
	public boolean buildSuccess = false;
	
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
		String mediaControl = "";
		
		//check storage first
		ParameterResult pr = nluInput.getStoredParameterResult(PARAMETERS.MEDIA_CONTROLS);
		if (pr != null){
			mediaControl = pr.getExtracted();
			this.found = pr.getFound();
			return mediaControl;
		}
		
		String play, pause, stop, close, next, previous, resume, repeat, vol_up, vol_down, vol_set;
		//German
		if (language.matches(LANGUAGES.DE)){
			play = "(spiele(n|)|abspielen|starten|oeffne(n|)|play)(?!.*\\b(naechste(\\w|)|vorherige(\\w|)))";
			pause = "pausieren|pause|anhalten";
			stop = "stoppen|stop(p|)";
			close = "schliesse(n|)";
			next = "naechste(\\w|)|vorwaerts|vor|next";
			previous = "zurueck|vorherige(\\w|)";
			resume = "weiter|fortsetzen";
			repeat = "wiederholen";
			vol_set = "lautstaerke (\\w+ |)auf( |$)";
			vol_up = "lauter|lautstaerke( .* | )(erhoehen|rauf|hoch|plus|groesser)|(vergroessern|erhoehen|rauf mit|hoch mit|(hoeher|groesser) machen)( der | )(lautstaerke)";
			vol_down = "leiser|lautstaerke( .* | )(erniedrigen|runter|niedriger|minus|kleiner)|(verkleinern|erniedrigen|runter mit|(niedriger|kleiner) machen)( der | )(lautstaerke)";
			
		//English and other
		}else{
			play = "(play|start|open)(?!.*\\b(next|previous))";
			pause = "pause";
			stop = "stop|end";
			close = "close";
			next = "next|forward";
			previous = "back|previous";
			resume = "continue|resume";
			repeat = "repeat";
			vol_set = "(set |)(the |)volume( \\w+ | )to( |$)";
			vol_up = "louder|(turn |)(the |)volume( .* | )(up|increase|plus)|(increase|(turn |)up)( the | )volume";
			vol_down = "quieter|(turn |)(the |)volume( .* | )(down|decrease|minus)|(decrease|(turn |)down)( the | )volume";
		}
		
		String extracted = NluTools.stringFindFirst(input, 
				pause + "|" +
				stop + "|" + 
				close + "|" +
				next + "|" +
				previous + "|" +
				resume + "|" +
				repeat + "|" +
				vol_set + "|" +		//before up, down!
				vol_up + "|" +
				vol_down + "|" +
				play				//at the end!
		);
		
		if (!extracted.isEmpty()){
			//PAUSE
			if (NluTools.stringContains(extracted, pause)){
				mediaControl = "<" + Type.pause + ">";
			//STOP
			}else if (NluTools.stringContains(extracted, stop)){
				mediaControl = "<" + Type.stop + ">";
			//CLOSE
			}else if (NluTools.stringContains(extracted, close)){
				mediaControl = "<" + Type.close + ">";
			//NEXT
			}else if (NluTools.stringContains(extracted, next)){
				mediaControl = "<" + Type.next + ">";
			//PREVIOUS
			}else if (NluTools.stringContains(extracted, previous)){
				mediaControl = "<" + Type.previous + ">";
			//RESUME
			}else if (NluTools.stringContains(extracted, resume)){
				mediaControl = "<" + Type.resume + ">";
			//REPEAT
			}else if (NluTools.stringContains(extracted, repeat)){
				mediaControl = "<" + Type.repeat + ">";
			//VOLUME SET - NOTE: do this before vol_up, vol_down, because user could say "increase volume to 11"
			}else if (NluTools.stringContains(extracted, vol_set)){
				mediaControl = "<" + Type.volume_set + ">";
			//VOLUME UP
			}else if (NluTools.stringContains(extracted, vol_up)){
				mediaControl = "<" + Type.volume_up + ">";
			//VOLUME DOWN
			}else if (NluTools.stringContains(extracted, vol_down)){
				mediaControl = "<" + Type.volume_down + ">";
			//PLAY - NOTE: put this at the end for things like "play next song"
			}else if (NluTools.stringContains(extracted, play)){
					mediaControl = "<" + Type.play + ">";
			}else{
				mediaControl = "";
			}
		}
		this.found = extracted;
		
		//store it - currently we assume this is only used in client-control service
		pr = new ParameterResult(PARAMETERS.MEDIA_CONTROLS, mediaControl, found);
		nluInput.addToParameterResultStorage(pr);
		
		if (Is.notNullOrEmpty(this.found) && Is.notNullOrEmpty(mediaControl)){
			return (mediaControl + ";;" + this.found);
		}else{
			return "";
		}
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
		return NluTools.stringRemoveFirst(input, Pattern.quote(found));
	}
	
	@Override
	public String responseTweaker(String input){
		return input;
	}

	@Override
	public String build(String input) {
		String ex = "";
		//String foundInExtract = "";
		if (input.contains(";;")){
			String[] array = input.split(";;");
			ex = array[0];
			//foundInExtract = array[1];
		}else{
			ex = input;
		}
		//is accepted result?
		String inputLocal = getLocal(ex, language);
		if (inputLocal.isEmpty()){
			return "";
		}
		//build default result
		JSONObject itemResultJSON = new JSONObject();
			//JSON.add(itemResultJSON, InterviewData.INPUT_RAW, nluInput.textRaw);
			JSON.add(itemResultJSON, InterviewData.VALUE, ex);
			JSON.add(itemResultJSON, InterviewData.VALUE_LOCAL, inputLocal);
			//JSON.add(itemResultJSON, InterviewData.FOUND, foundInExtract);
		
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
		return buildSuccess;
	}

}
