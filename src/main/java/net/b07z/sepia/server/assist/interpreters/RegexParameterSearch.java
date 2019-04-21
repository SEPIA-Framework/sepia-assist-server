package net.b07z.sepia.server.assist.interpreters;

import java.util.HashMap;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.assistant.CURRENCY;
import net.b07z.sepia.server.assist.assistant.LANGUAGES;
import net.b07z.sepia.server.assist.parameters.Action;
import net.b07z.sepia.server.assist.parameters.DateAndTime;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.assist.users.User;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.Debugger;

//TODO: this class is ancient, many methods are deprecated and replaced by the Parameter system, but there are still some functions of it in use!
//Move everything to parameter classes!

/**
 * This class contains all the parameter search routines that can be used to either give an additional hint on what command you are dealing with
 * or simply to identify parameters in a text/text-piece. There are also methods to identify [user_home], [user_work] etc.<br>
 * Note: many of the methods only "clean up" a string, so it might well be that they return an unmodified string instead of an empty string
 * when there is no parameter inside. E.g. compare get_search(..) and get_locations(..) which apply quiet different approaches. The first assumes
 * that you are doing a search and only modifies the string (or does nothing) where as the other looks for certain patterns and returns empty strings
 * if it does not find them.
 * 
 */
public class RegexParameterSearch {
	
	/**
	 * Check if input contains a special slash command like "saythis".
	 * @param input - user input
	 * @return true/false
	 */
	public static boolean contains_slashCMD(String input){
		//TODO: improve all slash commands!
		if (input.toLowerCase().matches("(" + Pattern.quote(Config.assistantName.toLowerCase()) + " |^)(saythis)\\b.*")){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Is it an abort command?
	 * @param input
	 * @return true/false
	 */
	public static boolean is_abort(String input, String language){
		if (input.matches("(abort\\b.*|.*\\babort|abbrechen\\b.*|.*\\babbrechen|zurueck|back|aufhoeren|stop|stop it|end|schluss)")){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * Is yes or no?
	 * @param input - string with answer
	 * @return "yes" or "no" or empty string
	 */
	public static String yes_or_no(String input, String language){
		String yes = "";
		String no = "";
		
		//German
		if (language.matches("de")){
			yes = NluTools.stringFindFirst(input, "ja|definitiv|auf jeden fall|immer doch|ok|okay|gut|tue es|tu es|mach es|korrekt|bestaetig(en|e|t)|richtig|yes|genau|wahr|jop|jo|joa|sicher|^go$|klar");
			no = NluTools.stringFindFirst(input, "nein|nee|ne|niemals|noe|no|nicht|auf keinen fall|abbrechen|lass es|falsch|schlecht|nope|spaeter");
			if (!no.isEmpty()){
				return "no";
			}else if (!yes.isEmpty()){
				return "yes";
			}else{
				return "";
			}
		
		//English
		}else if (language.matches("en")){
			yes = NluTools.stringFindFirst(input, "yes|definitely|ok|okay|good|do it|true|correct|confirmed|confirm|right|exactly|ja|jop|jo|sure|go ahead|^go$");
			no = NluTools.stringFindFirst(input, "no|not|abort|never|leave it|dont|don't|wrong|nope|later|go away");
			if (!no.isEmpty()){
				return "no";
			}else if (!yes.isEmpty()){
				return "yes";
			}else{
				return "";
			}
		
		//no result/missing language support ...
		}else{
			return "";
		}
	}
	
	/**
	 * Get the first number in a string and return it as string. Tries to capture decimals too.
	 * Returns ONE connected letter as well, e.g. "3b", "1h" or "2L" but also "10€" and "80%"! You can use "[^0-9\\.,\\+\\-]" to remove it.
	 * @param input - string with number inside
	 * @return number as string or empty string
	 */
	@Deprecated
	public static String get_number(String input){
		String number = "";
		number = NluTools.stringFindFirst(input, "(\\W|)(\\-|\\+|\\.|,|)\\d+(\\.|,|)\\d*(\\w|\\W|)"); 	//the \\w is for street numbers e.g. 3b
		number = number.replaceAll(",", "."); 	//Column is equal to .
		/*
		if (input.matches(".*\\d.*")){
			number = input.replaceAll(",", "."); 	//Column is equal to .
			number = number.replaceAll(".*?((\\-|\\+|\\.|,|)\\d+(\\.|,|)\\d*).*", "$1");
		}
		*/
		return number;
	}
	/**
	 * Replace a textual number like "one" by real number IF it is followed by "followedBy". Works with numbers from "null/zero" to "twenty-four".
	 * @param input - text input
	 * @param followedBy - regEx giving the followed elements, e.g. "(hour|day|week)".
	 * @param language - language code
	 * @return original input with replaced number if there was any
	 */
	public static String replace_text_number_followed_by(String input, String followedBy, String language){
		followedBy = "(" + followedBy + ")";
		//German
		if (language.matches("de")){
			input = input
				.replaceAll("\\b(null) " + followedBy, "0 $2")
				.replaceAll("\\b(einer|eine|einem|eins|ein) " + followedBy, "1 $2")
				.replaceAll("\\b(zwei) " + followedBy, "2 $2")
				.replaceAll("\\b(drei) " + followedBy, "3 $2")
				.replaceAll("\\b(vier) " + followedBy, "4 $2")
				.replaceAll("\\b(fuenf) " + followedBy, "5 $2")
				.replaceAll("\\b(sechs) " + followedBy, "6 $2")
				.replaceAll("\\b(sieben) " + followedBy, "7 $2")
				.replaceAll("\\b(acht) " + followedBy, "8 $2")
				.replaceAll("\\b(neun) " + followedBy, "9 $2")
				.replaceAll("\\b(zehn) " + followedBy, "10 $2")
				.replaceAll("\\b(elf) " + followedBy, "11 $2")
				.replaceAll("\\b(zwoelf) " + followedBy, "12 $2")
				.replaceAll("\\b(dreizehn) " + followedBy, "13 $2")
				.replaceAll("\\b(vierzehn) " + followedBy, "14 $2")
				.replaceAll("\\b(fuenfzehn) " + followedBy, "15 $2")
				.replaceAll("\\b(sechszehn) " + followedBy, "16 $2")
				.replaceAll("\\b(siebzehn) " + followedBy, "17 $2")
				.replaceAll("\\b(achtzehn) " + followedBy, "18 $2")
				.replaceAll("\\b(neunzehn) " + followedBy, "19 $2")
				.replaceAll("\\b(zwanzig) " + followedBy, "20 $2")
				.replaceAll("\\b(einundzwanzig) " + followedBy, "21 $2")
				.replaceAll("\\b(zweiundzwanzig) " + followedBy, "22 $2")
				.replaceAll("\\b(dreiundzwanzig) " + followedBy, "23 $2")
				.replaceAll("\\b(vierundzwanzig) " + followedBy, "24 $2")
				.replaceAll("\\b(fuenfundzwanzig) " + followedBy, "25 $2")
				.replaceAll("\\b(sechsundzwanzig) " + followedBy, "26 $2")
				.replaceAll("\\b(siebenundzwanzig) " + followedBy, "27 $2")
				.replaceAll("\\b(achtundzwanzig) " + followedBy, "28 $2")
				.replaceAll("\\b(neunundzwanzig) " + followedBy, "29 $2")
				.replaceAll("\\b(dreissig) " + followedBy, "30 $2")
				;
			return input;
		
		//English - and missing language support ...
		}else{
			input = input
				.replaceAll("\\b(null) " + followedBy, "0 $2")
				.replaceAll("\\b(zero) " + followedBy, "0 $2")
				.replaceAll("\\b(one) " + followedBy, "1 $2")
				.replaceAll("\\b(two) " + followedBy, "2 $2")
				.replaceAll("\\b(three) " + followedBy, "3 $2")
				.replaceAll("\\b(four) " + followedBy, "4 $2")
				.replaceAll("\\b(five) " + followedBy, "5 $2")
				.replaceAll("\\b(six) " + followedBy, "6 $2")
				.replaceAll("\\b(seven) " + followedBy, "7 $2")
				.replaceAll("\\b(eight) " + followedBy, "8 $2")
				.replaceAll("\\b(nine) " + followedBy, "9 $2")
				.replaceAll("\\b(ten) " + followedBy, "10 $2")
				.replaceAll("\\b(eleven) " + followedBy, "11 $2")
				.replaceAll("\\b(twelve) " + followedBy, "12 $2")
				.replaceAll("\\b(thirteen) " + followedBy, "13 $2")
				.replaceAll("\\b(fourteen) " + followedBy, "14 $2")
				.replaceAll("\\b(fifteen) " + followedBy, "15 $2")
				.replaceAll("\\b(sixteen) " + followedBy, "16 $2")
				.replaceAll("\\b(seventeen) " + followedBy, "17 $2")
				.replaceAll("\\b(eighteen) " + followedBy, "18 $2")
				.replaceAll("\\b(nineteen) " + followedBy, "19 $2")
				.replaceAll("\\b(twenty(-| )one) " + followedBy, "21 $2")
				.replaceAll("\\b(twenty(-| )two) " + followedBy, "22 $2")
				.replaceAll("\\b(twenty(-| )three) " + followedBy, "23 $2")
				.replaceAll("\\b(twenty(-| )four) " + followedBy, "24 $2")
				.replaceAll("\\b(twenty) " + followedBy, "20 $2")
				.replaceAll("\\b(thirty) " + followedBy, "30 $2")
				;
			return input;
		}
	}
	/**
	 * Replace a textual number like "one" by real number. Works with numbers from "null/zero" to "fifteen".
	 * @param input - text input
	 * @param language - language code
	 * @return original input with replaced number if there was any
	 */
	public static String replace_text_number(String input, String language){
		//German
		if (language.matches("de")){
			input = input
				.replaceAll("\\b(null)\\b", "0")
				.replaceAll("\\b(eins)\\b", "1")
				.replaceAll("\\b(zwei)\\b", "2")
				.replaceAll("\\b(drei)\\b", "3")
				.replaceAll("\\b(vier)\\b", "4")
				.replaceAll("\\b(fuenf)\\b", "5")
				.replaceAll("\\b(sechs)\\b", "6")
				.replaceAll("\\b(sieben)\\b", "7")
				.replaceAll("\\b(acht)\\b", "8")
				.replaceAll("\\b(neun)\\b", "9")
				.replaceAll("\\b(zehn)\\b", "10")
				.replaceAll("\\b(elf)\\b", "11")
				.replaceAll("\\b(zwoelf)\\b", "12")
				.replaceAll("\\b(dreizehn)\\b", "13")
				.replaceAll("\\b(vierzehn)\\b", "14")
				.replaceAll("\\b(fuenfzehn)\\b", "15")
				.replaceAll("\\b(zwanzig)\\b", "20")
				.replaceAll("\\b(dreissig)\\b", "30")
				;
			return input;
		
		//English - and missing language support ...
		}else{
			input = input
				.replaceAll("\\b(null|zero)\\b", "0")
				.replaceAll("\\b(one)\\b", "1")
				.replaceAll("\\b(two)\\b", "2")
				.replaceAll("\\b(three)\\b", "3")
				.replaceAll("\\b(four)\\b", "4")
				.replaceAll("\\b(five)\\b", "5")
				.replaceAll("\\b(six)\\b", "6")
				.replaceAll("\\b(seven)\\b", "7")
				.replaceAll("\\b(eight)\\b", "8")
				.replaceAll("\\b(nine)\\b", "9")
				.replaceAll("\\b(ten)\\b", "10")
				.replaceAll("\\b(eleven)\\b", "11")
				.replaceAll("\\b(twelve)\\b", "12")
				.replaceAll("\\b(thirteen)\\b", "13")
				.replaceAll("\\b(fourteen)\\b", "14")
				.replaceAll("\\b(fifteen)\\b", "15")
				.replaceAll("\\b(twenty)\\b", "20")
				.replaceAll("\\b(thirty)\\b", "30")
				;
			return input;
		}
	}
	
	 /**
	 * Get an amount of something with phrases like "more than 5 kg" or "not more than 100 PS" and "at least 50 €"
	 * @param input - input text
	 * @param type - amount of "weight", "power", "energy", "money", "distance"
	 * @param language - language code
	 * @return HashMap with tags: amount, type 
	 * <br>where amount can contain special characters (&lt;lt&gt;, &lt;gt&gt; &lt;to&gt;)!
	 */
	public static HashMap<String, String> get_amount_of(String input, String type, String language){
		//store all parameters
		HashMap<String, String> pv = new HashMap<String, String>();
		String found_type_tag = "";
		String amount = "";
		String amount_from = "";
		String amount_to = "";
		boolean less_than = false;
		boolean more_than = false;
		
		String number_regex = "(\\-|\\+|>|<|\\.|,|)\\d+(\\.|,|)\\d*(\\s|)"; 	//compare: get_number(..)
		String type_regex_higher = "";
		String type_regex_lower = "";
		
		//TODO: some abbreviations might be problematic when everything is lower-case, e.g. MJ, mJ ...
		//TODO: add temperature, volume
		//TODO: add cleaned text or identified parts to be able to remove the parameters from the text 
		
		//German
		if (language.matches("de")){
			//weight (kg, lb, g, ...)
			if (type.equals("weight")){
				found_type_tag = NluTools.stringFindFirst(input, "(mega|kilo|milli|mikro|zenti) (gramm|gramms|pfund|pfunde|tonne|tonnen)|"
						+ "(mega|kilo|milli|mikro|zenti|)(gramm|gramms|pfund|pfunde(n|)|tonne(n|))|"
						+ "(\\d+|)(kilo|kilos|kg|mg|µg|g|t|lbs|lb|lb\\.)");
				
				type_regex_higher = "nicht leichter( ist| sind|) als |(?<!nicht )schwerer( ist| sind|) als |nicht weniger (wiegt|wiegen|gewicht( haben|)) als |(?<!nicht )mehr (wiegt|wiegen|gewicht( haben|)) als ";
				type_regex_lower = "nicht schwerer( ist| sind|) als |(?<!nicht )leichter( ist| sind|) als |nicht mehr (wiegt|wiegen|gewicht( haben|)) als |(?<!nicht )weniger (wiegt|wiegen|gewicht( haben|)) als ";
			
			//distance (m, cm, mm, ...)
			}else if (type.equals("distance")){
				found_type_tag = NluTools.stringFindFirst(input, "(kilo|milli|mikro|zenti|centi|)(meter(n|)|meile(n|)|fuss|fuesse(n|)|yard(s|))|"
						+ "(\\d+|)(km|cm|µm|mm|m|ft|yd)");
				
				type_regex_higher = "nicht (naeher|nah|kuerzer)( dran|)( ist| sind|) als |(?<!nicht )(weiter|weit|laenger)( entfernt|)( ist| sind|) als ";
				type_regex_lower = "nicht (weiter|weit|laenger)( entfernt|)( ist| sind|) als |(?<!nicht )(naeher|nah|kuerzer)( dran|)( ist| sind|) als ";
				
			//power (kw, ps, J, ...)
			}else if (type.equals("power")){
				found_type_tag = NluTools.stringFindFirst(input, "(mega|kilo|milli|mikro) (watt|watts)|"
						+ "(mega|kilo|milli|mikro)(watt|watts)|"
						+ "(\\d+|)(watt|watts|pferdestaerke(n|)|kw|ps|w)");
				
				type_regex_higher = "nicht schwaecher( ist| sind|) als |(?<!nicht )staerker( ist| sind|) als |nicht weniger (leistet|leisten|(leistung|kraft|staerke)( haben|)) als |(?<!nicht )mehr (leistet|leisten|(leistung|kraft|staerke)( haben|)) als ";
				type_regex_lower = "nicht staerker( ist| sind|) als |(?<!nicht )schwaecher( ist| sind|) als |nicht mehr (leistet|leisten|(leistung|kraft|staerke)( haben|)) als |(?<!nicht )weniger (leistet|leisten|(leistung|kraft|staerke)( haben|)) als ";
			
			//energy (J, liter ...)
			}else if (type.equals("energy")){
				found_type_tag = NluTools.stringFindFirst(input, "(mega|kilo|milli|mikro|kubik|zenti) (joule|joules|liter)|"
						+ "(mega|kilo|milli|mikro|kubik|zenti)(joule|joules|liter)|"
						+ "(\\d+|)(joule|joules|liter|mj|µj|j|l)");
				
				type_regex_higher = "nicht weniger (verbraucht|verbrauchen|(verbrauch|energie|kraft|staerke)( haben|)) als |(?<!nicht )mehr (verbraucht|verbrauchen|(verbrauch|energie|kraft|staerke)( haben|)) als ";
				type_regex_lower = "nicht mehr (verbraucht|verbrauchen|(verbrauch|energie|kraft|staerke)( haben|)) als |(?<!nicht )weniger (verbraucht|verbrauchen|(verbrauch|energie|kraft|staerke)( haben|)) als ";
			
			//money (€, $, EUR, USD, ...)
			}else if (type.equals("money")){
				found_type_tag = NluTools.stringFindFirst(input, "((\\d+|)" + CURRENCY.TAGS_DE + "|" + CURRENCY.TAGS_DE + "\\d+)");
				
				type_regex_higher = "nicht billiger( ist| sind|) als |(?<!nicht )teurer( ist| sind|) als |nicht weniger (kostet|kosten( haben|)|wert( haben|)) als |(?<!nicht )mehr (kostet|kosten( haben|)|wert( haben|)) als ";
				type_regex_lower = "nicht teurer( ist| sind|) als |(?<!nicht )billiger( ist| sind|) als |nicht mehr (kostet|kosten( haben|)|wert( haben|)) als |(?<!nicht )weniger (kostet|kosten( haben|)|wert( haben|)) als ";
			}
			
			//search amount:
			if (!found_type_tag.isEmpty()){
				found_type_tag = found_type_tag.replaceAll("\\d+", "").trim();
				//from - to
				String num_regex_from = number_regex + "(" + Pattern.quote(found_type_tag) + "|)";
				String num_regex_to = number_regex + Pattern.quote(found_type_tag);
				if (NluTools.stringContains(input, "(von |mit |zwischen )" + num_regex_from + " (bis|und) " + num_regex_to)){
					amount_from = NluTools.stringFindFirst(input, "(von |mit |zwischen )" + num_regex_from);
					amount_from = amount_from.replaceFirst("(von |mit |zwischen )", "").trim();
					amount_to = NluTools.stringFindFirst(input, "(bis|und) " + num_regex_to);
					amount_to = amount_to.replaceFirst("(bis|und)", "").trim();
					amount = amount_from.replaceFirst(Pattern.quote(found_type_tag), "").trim() + "<to>" + amount_to.replaceFirst(Pattern.quote(found_type_tag), "").trim();
				}
				if (amount.isEmpty()){
					amount = NluTools.stringFindFirst(input, number_regex + Pattern.quote(found_type_tag)).trim();
					//bigger - smaller
					if (!amount.isEmpty()){
						if (NluTools.stringContains(input, "(nicht (weniger|niedriger|kleiner)( ist| sind| hat| haben|) als |(?<!nicht )(mehr|hoeher|groesser)( ist| sind| hat| haben|) als |(mindestens |minimal |minimum )|(?<!nicht )ueber |"
														+ type_regex_higher + ")" + Pattern.quote(amount))){
							more_than = true;
						}else if (NluTools.stringContains(input, "((?<!nicht )(weniger|niedriger|kleiner)( ist| sind| hat| haben|) als |nicht (mehr|hoeher|groesser)( ist| sind| hat| haben|) als |(hoechstens |maximal |maximum )|(?<!nicht )unter |"
														+ type_regex_lower + ")" + Pattern.quote(amount))){
							less_than = true;
						}
					}
					amount = amount.replaceFirst(Pattern.quote(found_type_tag), "").trim();
				}
			}
		
		//English - and missing language support ...
		}else{
			//weight (kg, lb, g, ...)
			if (type.equals("weight")){
				found_type_tag = NluTools.stringFindFirst(input, "(mega|kilo|milli|micro|centi) (gram|grams|gramme|grammes|pound|pounds|ton|tons)|"
						+ "(mega|kilo|milli|micro|centi)(gram|grams|gramme|grammes|pound|pounds|ton|tons)|"
						+ "(\\d+|)(kilo|kilos|gram|grams|gramme|grammes|pound|pounds|ton|tons|kg|mg|µg|g|t|lbs|lb|lb\\.)");
				
				type_regex_higher = "(not |not \\w+ )lighter( is| are|) than |(?<!(not |not \\w{1,20} ))heavier( is| are|) than |(not |not \\w+ )less weight than |(?<!(not |not \\w{1,20} ))more weight than ";
				type_regex_lower = "(not |not \\w+ )heavier( is| are|) than |(?<!(not |not \\w{1,20} ))lighter( is| are|) than |(not |not \\w+ )more weight than |(?<!(not |not \\w{1,20} ))less weight than ";
			
			//distance (m, cm, mm, ...)
			}else if (type.equals("distance")){
				found_type_tag = NluTools.stringFindFirst(input, "(kilo|milli|micro|zenti|centi|)(meter(s|)|metre(s|)|mile(s|)|feet|foot|yard(s|))|"
						+ "(\\d+|)(km|cm|µm|mm|m|ft|yd)");
				
				type_regex_higher = "(not |not \\w+ )(closer|shorter)( is| are|) than |(?<!(not |not \\w{1,20} ))(further|longer)( away|)( is| are|) than ";
				type_regex_lower = "(not |not \\w+ )(further|longer)( away|)( is| are|) than |(?<!(not |not \\w{1,20} ))(closer|shorter)( is| are|) than ";
				
			//power (kw, ps, J, ...)
			}else if (type.equals("power")){
				found_type_tag = NluTools.stringFindFirst(input, "(mega|kilo|milli|micro) (watt|watts)|"
						+ "(mega|kilo|milli|micro)(watt|watts)|"
						+ "(\\d+|)(watt|watts|horse power(s|)|horsepower(s|)|kw|ps|hp|w)");
				
				type_regex_higher = "(not |not \\w+ )weaker( is| are|) than |(?<!(not |not \\w{1,20} ))stronger( is| are|) than |not less power than |(?<!(not |not \\w{1,20} ))more power than ";
				type_regex_lower = "(not |not \\w+ )stronger( is| are|) than |(?<!(not |not \\w{1,20} ))weaker( is| are|) than |not more power than |(?<!(not |not \\w{1,20} ))less power than ";
			
			//energy (J, liter ...)
			}else if (type.equals("energy")){
				found_type_tag = NluTools.stringFindFirst(input, "(mega|kilo|milli|micro|cubic|centi) (joule|joules|liter(s|)|litre(s|))|"
						+ "(mega|kilo|milli|micro|cubic|centi|)(joule|joules|liter(s|)|litre(s|))|"
						+ "(\\d+|)(mj|µj|j|l)");
				
				type_regex_higher = "(not |not \\w+ )less (consumption|consume|energy) than |(?<!(not |not \\w{1,20} ))more (consumption|consume|energy) than ";
				type_regex_lower = "(not |not \\w+ )more (consumption|consume|energy) than |(?<!(not |not \\w{1,20} ))less (consumption|consume|energy) than ";
			
			//money (€, $, EUR, USD, ...)
			}else if (type.equals("money")){
				found_type_tag = NluTools.stringFindFirst(input, "((\\d+|)" + CURRENCY.TAGS_EN + "|" + CURRENCY.TAGS_DE + "\\d+)");
				
				type_regex_higher = "(not |not \\w+ )(cheaper|less expensive|less value|tawdrier)( is| are|) than |(?<!(not |not \\w{1,20} ))(dearer|pricier|more expensive|more value)( is| are|) than ";
				type_regex_lower = "(not |not \\w+ )(dearer|pricier|more expensive|more value)( is| are|) than |(?<!(not |not \\w{1,20} ))(cheaper|less expensive|less value|tawdrier)( is| are|) than ";
			}
			
			//search amount:
			if (!found_type_tag.isEmpty()){
				found_type_tag = found_type_tag.replaceAll("\\d+", "").trim();
				//from - to
				String num_regex_from = "(" + number_regex + "(" + Pattern.quote(found_type_tag) + "|)" + "|" + Pattern.quote(found_type_tag) + number_regex + ")";
				String num_regex_to = "(" + number_regex + Pattern.quote(found_type_tag) + "|" + Pattern.quote(found_type_tag) + number_regex + ")";
				if (NluTools.stringContains(input, "(from |with |between )" + num_regex_from + " (to|and) " + num_regex_to)){
					amount_from = NluTools.stringFindFirst(input, "(from |with |between )" + num_regex_from);
					amount_from = amount_from.replaceFirst("(from |with |between )", "").trim();
					amount_to = NluTools.stringFindFirst(input, "(to|and) " + num_regex_to);
					amount_to = amount_to.replaceFirst("(to|and)", "").trim();
					amount = amount_from.replaceFirst(Pattern.quote(found_type_tag), "").trim() + "<to>" + amount_to.replaceFirst(Pattern.quote(found_type_tag), "").trim();
				}
				if (amount.isEmpty()){
					amount = NluTools.stringFindFirst(input, number_regex + Pattern.quote(found_type_tag)).trim();
					//bigger - smaller
					if (!amount.isEmpty()){
						if (NluTools.stringContains(input, "((not |not \\w+ )(less|lower|smaller)( is| are| has| have|) than |(?<!(not |not \\w{1,20} ))(more|higher|bigger)( is| are| has| have|) than |(at least |min |minimal |minimum )|(?<!(not |not \\w{1,20} ))over |"
														+ type_regex_higher + ")" + Pattern.quote(amount))){
							more_than = true;
						}else if (NluTools.stringContains(input, "((?<!(not |not \\w{1,20} ))(less|lower|smaller)( is| are| has| have|) than |(not |not \\w+ )(more|higher|bigger)( is| are| has| have|) than |(at most |max |maximal |maximum )|(?<!(not |not \\w{1,20} ))under |"
														+ type_regex_lower + ")" + Pattern.quote(amount))){
							less_than = true;
						}
					}
					amount = amount.replaceFirst(Pattern.quote(found_type_tag), "").trim();
				}
			}
		}
		//check chars
		if (!amount.isEmpty()){
			if (amount.startsWith("<") && !amount.startsWith("<gt>")){
				amount = amount.replaceFirst("<", "<lt>");
			}else if (amount.startsWith(">")){
				amount = amount.replaceFirst(">", "<gt>");
			}
		}
		
		//add bigger/less than symbols
		if (!amount.isEmpty() && more_than){
			amount = "<gt>" + amount;
		}else if (!amount.isEmpty() && less_than){
			amount = "<lt>" + amount;
		}
		
		pv.put("amount", amount.trim());
		pv.put("type", found_type_tag);
		return pv;
	}
	/**
	 * Convert any amount-type to its default type or return empty string. Handles special characters in amount like (&lt;lt&gt;, &lt;gt&gt; &lt;to&gt;)!
	 * @param convert_type - type as given to "get_amount_of(...)"
	 * @param amount_type - type as returned by "get_amount_of(...)"
	 * @param amount - string value to convert to default
	 * @param language - language code
	 * @return converted amount or empty
	 */
	public static String convert_amount_to_default(String convert_type, String amount_type, String amount, String language){
		
		//TODO: improve! complete! missing e.g.: weight, age, energy 
		
		if (amount.isEmpty()){
			return "";
		}
		
		//split on special chars
		if (amount.contains("<to>")){
			String[] amounts = amount.split("<to>");
			if (amounts.length > 1){
				String a1 = convert_amount_to_default(convert_type, amount_type, amounts[0], language);
				String a2 = convert_amount_to_default(convert_type, amount_type, amounts[1], language);
				if (a1.isEmpty() || a2.isEmpty()){
					return "";
				}
				return (a1 + "<to>" + a2);
			}
		}else if (amount.matches("^(<gt>|<lt>).+")){
			String c = amount.replaceFirst(">.*", "") + ">";
			return (c + convert_amount_to_default(convert_type, amount_type, amount.replaceFirst(Pattern.quote(c), "").trim(), language));
		}
		
		//German
		if (language.matches("de")){
			//--money to €€
			if (convert_type.equals("money")){
				if (NluTools.stringContains(amount_type, "euro(s|)|€|eur")){
					//all good
				//$$
				}else if (NluTools.stringContains(amount_type, "(us |amerikanische(r|) |)dollar(s|)|\\$|usd")){
					double p = CURRENCY.convertCurrency("usd", "eur", amount);
					amount = (p != 0.0d)? Converters.smartRound(p, false) : "";
				//££
				}else if (NluTools.stringContains(amount_type, "(gb |britische(s|) |)pfund|£|gbp")){
					double p = CURRENCY.convertCurrency("gbp", "eur", amount);
					amount = (p != 0.0d)? Converters.smartRound(p, false) : "";
				//FAIL
				}else{
					Debugger.println("missing convert_amount_to_default(..) support for " + amount_type, 3);
					amount = "";
				}
			//--distance to km
			}else if (convert_type.equals("distance")){
				if (NluTools.stringContains(amount_type, "kilometer|km")){
					//all good
				//meter
				}else if (NluTools.stringContains(amount_type, "(meter|m)")){
					double m = Converters.obj2DoubleOrDefault(amount, Double.NEGATIVE_INFINITY);
					amount = (m != Double.NEGATIVE_INFINITY)? Converters.smartRound(m / 1000.0d, false) : "";
				//miles
				}else if (NluTools.stringContains(amount_type, "(meile(n|))")){
					double m = Converters.obj2DoubleOrDefault(amount, Double.NEGATIVE_INFINITY);
					amount = (m != Double.NEGATIVE_INFINITY)? Converters.smartRound(m * 1.609344d, false) : "";
				//FAIL
				}else{
					Debugger.println("missing convert_amount_to_default(..) support for " + amount_type, 3);
					amount = "";
				}	
			//--power to kW
			}else if (convert_type.equals("power")){
				if (NluTools.stringContains(amount_type, "kilowatt|kilo watt|kw")){
					//all good
				//watt
				}else if (NluTools.stringContains(amount_type, "(watt|w)")){
					double w = Converters.obj2DoubleOrDefault(amount, Double.NEGATIVE_INFINITY);
					amount = (w != Double.NEGATIVE_INFINITY)? Converters.smartRound(w / 1000.0d, false) : "";
				//hp, ps
				}else if (NluTools.stringContains(amount_type, "(ps|hp|pferdestaerke(n|)|pferde staerke(n|))")){
					double ps = Converters.obj2DoubleOrDefault(amount, Double.NEGATIVE_INFINITY);
					amount = (ps != Double.NEGATIVE_INFINITY)? Converters.smartRound(ps * 0.735499d, false) : "";
				//FAIL
				}else{
					Debugger.println("missing convert_amount_to_default(..) support for " + amount_type, 3);
					amount = "";
				}
			//--MISSING
			}else{
				Debugger.println("missing convert_amount_to_default(..) support for ct: " + convert_type + " at: " + amount_type, 3);
				amount = "";
			}

		//English and rest
		}else{
			//--money to €€
			if (convert_type.equals("money")){
				if (NluTools.stringContains(amount_type, "euro(s|)|€|eur")){
					//all good
				//$$
				}else if (NluTools.stringContains(amount_type, "(us |american |)dollar(s|)|\\$|usd")){
					double p = CURRENCY.convertCurrency("usd", "eur", amount);
					amount = (p != 0.0d)? Converters.smartRound(p, false) : "";
				//££
				}else if (NluTools.stringContains(amount_type, "(gb |british |)pound(s|)|£|gbp")){
					double p = CURRENCY.convertCurrency("gbp", "eur", amount);
					amount = (p != 0.0d)? Converters.smartRound(p, false) : "";
				//FAIL
				}else{
					Debugger.println("missing convert_amount_to_default(..) support for " + amount_type, 3);
					amount = "";
				}
			//--distance to km
			}else if (convert_type.equals("distance")){
				if (NluTools.stringContains(amount_type, "kilometer(s|)|kilometre(s|)|km")){
					//all good
				//meter
				}else if (NluTools.stringContains(amount_type, "(meter(s|)|metre(s|)|m)")){
					double m = Converters.obj2DoubleOrDefault(amount, Double.NEGATIVE_INFINITY);
					amount = (m != Double.NEGATIVE_INFINITY)? Converters.smartRound(m / 1000.0d, false) : "";
				//miles
				}else if (NluTools.stringContains(amount_type, "(mile(s|))")){
					double m = Converters.obj2DoubleOrDefault(amount, Double.NEGATIVE_INFINITY);
					amount = (m != Double.NEGATIVE_INFINITY)? Converters.smartRound(m * 1.609344d, false) : "";
				//FAIL
				}else{
					Debugger.println("missing convert_amount_to_default(..) support for " + amount_type, 3);
					amount = "";
				}	
			//--power to kW
			}else if (convert_type.equals("power")){
				if (NluTools.stringContains(amount_type, "kilowatt|kilo watt|kw")){
					//all good
				//watt
				}else if (NluTools.stringContains(amount_type, "(watt(s|)|w)")){
					double w = Converters.obj2DoubleOrDefault(amount, Double.NEGATIVE_INFINITY);
					amount = (w != Double.NEGATIVE_INFINITY)? Converters.smartRound(w / 1000.0d, false) : "";
				//hp, ps
				}else if (NluTools.stringContains(amount_type, "(ps|hp|horsepower(s|)|horse power(s|))")){
					double ps = Converters.obj2DoubleOrDefault(amount, Double.NEGATIVE_INFINITY);
					amount = (ps != Double.NEGATIVE_INFINITY)? Converters.smartRound(ps * 0.735499d, false) : "";
				//FAIL
				}else{
					Debugger.println("missing convert_amount_to_default(..) support for " + amount_type, 3);
					amount = "";
				}
			//--MISSING
			}else{
				Debugger.println("missing convert_amount_to_default(..) support for ct: " + convert_type + " at: " + amount_type, 3);
				amount = "";
			}
		}
		//return as string
		return amount;
		/*
		//return as double
		if (!amount.isEmpty()){
			double d = Converters.obj_2_double(amount);
			return (d == Double.NEGATIVE_INFINITY)? 0.0d : d;
		}else{
			return 0.0d;
		}
		*/
	}
	
	/**
	 * Get an age of something with phrases like "5 years old" or "older than 1999" and "not older than 1 year"
	 * @param input - input text
	 * @param language - language code
	 * @return HashMap with tags:<br>
	 * -age_y, less_than_y, more_than_y<br> 
	 * where the result can contain special chars (&lt;lt&gt;, &lt;gt&gt; &lt;to&gt;)!
	 */
	public static HashMap<String, String> get_age(String input, String language){
		//store all parameters
		HashMap<String, String> pv = new HashMap<String, String>();
		String tag_y = "";
		String age_y = "";
		boolean less_than_y = false;
		boolean more_than_y = false;
		
		//German
		if (language.matches("de")){
			//years (from 1999, 50 years ...)
			input = replace_text_number_followed_by(input, "(jahr(en|e|))", language);
			tag_y = NluTools.stringFindFirst(input, "(jahr(en|e|))");
			//age_y = NLU_Tools.stringFindFirst(input, "(<|>|)(\\d+ " + tag_y + "|1\\d\\d\\d|2\\d\\d\\d)");
			String age_y_regex;
			//TODO: unit exceptions!
			if (!tag_y.isEmpty()){
				age_y_regex = "(^|\\s)(<|>|-|)(\\d+ " + tag_y + "|(1\\d\\d\\d|2\\d\\d\\d)(-|\\s|$)(?!(kw|hp|ps|€|\\$|£|¥|eur|gbp|usd|km|m)))";
			}else{
				age_y_regex = "(^|\\s)(<|>|-|)(1\\d\\d\\d|2\\d\\d\\d)(-|\\s|$)(?!(kw|hp|ps|€|\\$|£|¥|eur|gbp|usd|km|m))";
			}
			
			//search age:
			//from - to year
			if (NluTools.stringContains(input, "(von |zwischen )" + "\\d+( |)" + "(" + Pattern.quote(tag_y) + "|)" + " (bis|und) " + age_y_regex)){
				String from = NluTools.stringFindFirst(input, "(von |zwischen )" + "\\d+ " + "(" + Pattern.quote(tag_y) + "|)");
				from = from.replaceFirst("(von |zwischen )", "").trim();
				String to = NluTools.stringFindFirst(input, "(bis |und )" + age_y_regex);
				to = to.replaceFirst("(bis |und )", "").trim();
				age_y = from.replaceFirst(Pattern.quote(tag_y), "").trim() + "<to>" + to.replaceFirst(Pattern.quote(tag_y), "").trim();
			
			}else{
				age_y = NluTools.stringFindFirst(input, age_y_regex).trim();
				if (!age_y.isEmpty()){
					if (NluTools.stringContains(input, "(nicht (juenger|weniger|kleiner|neuer|frueher)( ist| sind|) als |(?<!nicht )(aelter|spaeter|mehr|groesser)( ist| sind|) als |(mindestens |minimal |minimum )|(?<!nicht )ueber )" + Pattern.quote(age_y))){
						more_than_y = true;
					}else if (NluTools.stringContains(input, "((?<!nicht )(juenger|weniger|kleiner|neuer|frueher)( ist| sind|) als |nicht (mehr|aelter|spaeter|groesser)( ist| sind|) als |(hoechstens |maximal |maximum )|(?<!nicht )unter )" + Pattern.quote(age_y))){
						less_than_y = true;
					}
				}
				age_y = age_y.replaceFirst(Pattern.quote(tag_y), "").trim();
			}
		
		//English - and missing language support ...
		}else{
			//years (from 1999, 50 years ...)
			input = replace_text_number_followed_by(input, "(years|year)", language);
			tag_y = NluTools.stringFindFirst(input, "(years|year)");
			//age_y = NLU_Tools.stringFindFirst(input, "(<|>|)(\\d+ " + tag_y + "|1\\d\\d\\d|2\\d\\d\\d)");
			String age_y_regex;
			//TODO: unit exceptions!
			if (!tag_y.isEmpty()){
				age_y_regex = "(^|\\s)(<|>|-|)(\\d+ " + tag_y + "|(1\\d\\d\\d|2\\d\\d\\d)(-|\\s|$)(?!(kw|hp|ps|€|\\$|£|¥|eur|gbp|usd|km|m)))";
			}else{
				age_y_regex = "(^|\\s)(<|>|-|)(1\\d\\d\\d|2\\d\\d\\d)(-|\\s|$)(?!(kw|hp|ps|€|\\$|£|¥|eur|gbp|usd|km|m))";
			}
			
			//search age:
			//from - to year
			if (NluTools.stringContains(input, "(from |between )" + "\\d+( |)" + "(" + Pattern.quote(tag_y) + "|)" + " (to|and) " + age_y_regex)){
				String from = NluTools.stringFindFirst(input, "(from |between )" + "\\d+ " + "(" + Pattern.quote(tag_y) + "|)");
				from = from.replaceFirst("(from |between )", "").trim();
				String to = NluTools.stringFindFirst(input, "(to |and )" + age_y_regex);
				to = to.replaceFirst("(to |and )", "").trim();
				age_y = from.replaceFirst(Pattern.quote(tag_y), "").trim() + "<to>" + to.replaceFirst(Pattern.quote(tag_y), "").trim();
			
			}else{
				age_y = NluTools.stringFindFirst(input, age_y_regex).trim();
				if (!age_y.isEmpty()){
					if (NluTools.stringContains(input, "(not (younger|smaller|lower|less|newer|earlier)( is| are|) than |(?<!not )(older|more|bigger|higher|later)( is| are|) than |(at least |min |minimal |minimum )|(?<!not )over )" + Pattern.quote(age_y))){
						more_than_y = true;
					}else if (NluTools.stringContains(input, "((?<!not )(younger|smaller|lower|less|newer|earlier)( is| are|) than |not (more|older|bigger|higher|later)( is| are|) than |(at most |max |maximal |maximum )|(?<!not )under )" + Pattern.quote(age_y))){
						less_than_y = true;
					}
				}
				age_y = age_y.replaceFirst(Pattern.quote(tag_y), "").trim();
			}
		}
		
		//check chars
		if (!age_y.isEmpty()){
			if (age_y.startsWith("<")){
				age_y = age_y.replaceFirst("<", "<lt>");
			}else if (age_y.startsWith(">")){
				age_y = age_y.replaceFirst(">", "<gt>");
			}
		}
		
		//add bigger/less than symbols
		if (!age_y.isEmpty() && more_than_y){
			age_y = "<gt>" + age_y;
		}else if (!age_y.isEmpty() && less_than_y){
			age_y = "<lt>" + age_y;
		}
		
		pv.put("age_y", age_y.trim());
		return pv;
	}
	
	/**
	 * Search text for locations and store as keys in map: location, location_start, location_end, ... etc. (see return)
	 * @param input - user text input or any text to be searched for locations
	 * @param language - ... language code as usual 
	 * @return HashMap with keys: 
	 * 			location, location_start, location_waypoint, location_end, 
	 * 			poi, travel_type, travel_time, travel_time_end
	 */
	public static HashMap<String, String> get_locations(String input, String language){
		//store all parameters
		HashMap<String, String> pv = new HashMap<String, String>();
		String location = "";
		String location_start = "";
		String location_waypoint = "";
		boolean is_local_waypoint = false;		//keeps track if the wp should be close to the start
		String location_end = "";
		String poi = "";
		String travel_type = "";
		String travel_time = "";
		String travel_time_end = "";
		
		String location_input = "";
		HashMap<String, String> dates = new HashMap<String, String>();
		
		//German
		if (language.matches("de")){
			//prepare
			location_input = input.replaceFirst("\\b(zu hause)\\b", "hause");
			location_input = replace_personal_locations(location_input, language);
			
			//get travel type and remove
			travel_type = NluTools.stringFindFirst(location_input, "zug|fernbus|reisebus|bus|ice|deutsche(n|) bahn|(s-|u-|s|u)bahn|strassenbahn|bahn|tram|nahverkehr|fahrrad|bike|biken|flugzeug|fluege|flug|fliegen|auto|fahren|automobil|zu fuss|laufen|mitfahrgelegenheit");
			if (!travel_type.trim().isEmpty()){
				location_input = location_input.replaceFirst("\\b(und |)(mit dem |mit der |mit |)(" + Pattern.quote(travel_type) + ")\\b", "").trim();
				//clean up
				location_input = location_input.replaceFirst("\\b(laufen|fahren|fliegen|reisen)\\b", "").trim();
			}
			
			//clean more - stupid verbs ^^ - TODO: remove verbs!
			location_input = location_input.replaceFirst("\\b(suche nach)\\b", "suche").trim();
			location_input = location_input.replaceFirst("\\b(zu )(fahren|kommen|gelangen|laufen|biken|fliegen|buchen)\\b", "").trim();
			location_input = location_input.replaceFirst("\\b(etwas|was) (zu essen)\\b", "restaurant").trim();
			location_input = location_input.replaceFirst("\\b(zeigen$|suchen$|finden$|komme$|gelange$|buchen$|(sein |)wird$)\\b", "").trim();
			location_input = location_input.replaceFirst("\\b(kommen|fahren|gelangen|gehen|laufen|biken|buchen) (kann)\\b", "").trim();
			
			//get travel time and remove
			dates = get_N_dates(location_input, 2, language);
			if (dates.size() >= 2+1){
				travel_time = dates.get("date_tag_1");
				travel_time_end = dates.get("date_tag_2");
			}else{
				travel_time = dates.get("date_tag_1");
			}
			if (!travel_time.isEmpty()){
				location_input = dates.get("clean_text");
			}
			
			//get waypoint and remove
			if (location_input.matches(".*\\b(auf (dem|meinem) weg|ueber|(und mit|und|mit) (einem |)(zwischen |zwischen|)(stopp|stop|halt))\\b.*")){
				//POI stop
				location_waypoint = NluTools.stringFindFirst(location_input, "tanken|einkaufen|kaufen|geld (abheben|besorgen|holen|abholen)");
				String stop_phrase = NluTools.stringFindFirst(location_input, "nach|zum|zu|zur|bis");
				//arbitrary stop
				if (location_waypoint.isEmpty() && location_input.matches(".*\\b(zwischen |zwischen|)(stopp|stop|halt)( in| ueber|)\\b.*")){
					location_waypoint = location_input.replaceFirst(".*\\b(zwischen |zwischen|)(stopp|stop|halt)( in| ueber|)\\b", "").trim();
				}else if (location_waypoint.isEmpty() && location_input.matches(".*\\b(ueber)\\b.*")){
					location_waypoint = location_input.replaceFirst(".*\\b(ueber)\\b", "").trim();
					location_waypoint = location_waypoint.replaceFirst("\\b(bis|nach|zu|zum|zur)\\b.*", "").trim();
				}else if (location_waypoint.isEmpty() && location_input.matches(".*\\b(noch (zu|zum|zur))\\b.*")){
					location_waypoint = location_input.replaceFirst(".*\\b(noch (zu|zum|zur))\\b", "").trim();
					location_waypoint = location_waypoint.replaceFirst("\\b(der|dem|den)\\b.*", "").trim();
				}
				if (!location_waypoint.isEmpty()){
					//remove - check order first
					if (NluTools.checkOrder(location_input, stop_phrase, location_waypoint)){
						location_input = location_input.replaceFirst(""
								+ "\\b((muss|muesste) ich( noch|) |noch |und mit |und |mit |ueber |" + Pattern.quote(location_waypoint) + ")\\b.*", "").trim();
					}
					//clean up
					if (location_waypoint.matches("tanken")){
						location_waypoint = "Tankstelle";
						is_local_waypoint = true;
					}else if (location_waypoint.matches("einkaufen|kaufen")){
						location_waypoint = "Supermarkt";
						is_local_waypoint = true;
					}else if (location_waypoint.matches("geld (abheben|besorgen|holen|abholen)")){
						location_waypoint = "Geldautomat";
						is_local_waypoint = true;
					}
				}
			}
			
			//get start, end and common location
			if (location_input.matches(".*\\b(von|vom)\\s.*\\b(bis nach|nach|zu|zum|zur|bis)\\s.*")){
				location_input = location_input.replaceFirst(".*?\\b(von|vom)\\b", "").trim();
				location_start = location_input.replaceFirst("\\b(bis nach|nach|zu|zum|zur|bis)\\b.*", "").trim();
				location_end = location_input.replaceFirst(".*?\\b(bis nach|nach|zu|zum|zur|bis)\\b", "").trim();
				
			}else if (location_input.matches(".*\\b(bis nach|nach|zu|zum|zur|bis)\\s.*\\b(von|vom)\\s.*")){
				location_input = location_input.replaceFirst(".*?\\b(bis nach|nach|zu|zum|zur|bis)\\b", "").trim();
				location_start = location_input.replaceFirst("\\b(von|vom)\\b.*", "").trim();
				location_end = location_input.replaceFirst(".*?\\b(von|vom)\\b", "").trim();
				
			}else if (location_input.matches(".*\\b(zwischen)\\s.*\\b(und)\\s.*")){
				location_input = location_input.replaceFirst(".*?\\b(zwischen)\\b", "").trim();
				location_start = location_input.replaceFirst("\\b(und)\\b.*", "").trim();
				location_end = location_input.replaceFirst(".*?\\b(und)\\b", "").trim();
				
			}else if (location_input.matches(".*\\b(von|vom|ab)\\s.*")){
				location_start = location_input.replaceFirst(".*?\\b(von|vom|ab)\\b", "").trim();
				
			//}else if (location_input.matches(".*\\b((wo|suche|zeig|finde|gibt es) .*(\\b)("+ get_POI_list(language) +")\\b.*)")){
			//	location = NLU_Tools.stringFindFirst(location_input, get_POI_list(language));
				
			}else if (location_input.matches(".*\\b(bis nach|nach|zu|zum|zur|bis|richtung)\\s.*")){
				location_end = location_input.replaceFirst(".*?\\b(bis nach|nach|zu|zum|zur|bis|richtung)\\b", "").trim();
				
			}else if (location_input.matches(".*\\b((adresse|ort) (des|der|von)|am|in|im|an|fuer|auf(?!( (der|einer) (karte|map)| maps)))\\s.*")){
				location = location_input.replaceFirst(".*?\\b((adresse|ort) (des|der|von)|am|in|im|an|fuer|auf)\\b", "").trim();
				location = location.replaceFirst("\\b(auf (der|einer) (karte|map)$|auf maps$)", "").trim();
				
			}else if (location_input.matches(".*\\b(der |die |das |ein |eine |einen |den |dem |)(nahes|naechste(n|)|naechstgelegene(n|))\\s.*")){
				location = location_input.replaceFirst(".*?\\b(nahes|naechste(n|)|naechstgelegene(n|))\\b", "").trim();	
				location = location.replaceFirst("\\b(auf (der|einer) (karte|map)$|auf maps$)", "").trim();
				location = location.replaceFirst("\\b(ist$|sind$|liegt$|liegen$)", "").trim();
				
			}else if (location_input.matches(".*\\b(suche|zeig|finde) .*(\\b)(auf|in) (der |einer |)(karte|map|maps|naehe)\\b.*")){
				location = location_input.replaceFirst(".*?\\b((suche|zeig|finde)( mir|)"
						+ "( auf (der |einer |)(karte|map|maps)|))\\b", "").trim();
				location = location.replaceFirst("^(der|die|das|eine|ein|einen|den)\\b", "").trim();
				location = location.replaceFirst("\\b(auf (der|einer) (karte|map)$|auf maps$)", "").trim();
				location = location.replaceFirst("\\b(in der naehe$)", "").trim();
				
			}else if (location_input.matches(".*\\b(wo (ist|sind|gibt es|liegt|liegen|befindet|befinden|auf (der|einer|) (karte|map|maps)))\\b.*")){
				location = location_input.replaceFirst(".*?\\b(wo (auf (der |einer |)(karte|map|maps) |)"
						+ "(ist|sind|liegt|liegen|gibt es|finde ich|befinde(t|n) sich)( hier|))\\b", "").trim();
				location = location.replaceFirst("^(der|die|das|eine|ein|einen|den)\\b", "").trim();
				location = location.replaceFirst("\\b(auf (der|einer) (karte|map)$|auf maps$)", "").trim();
				location = location.replaceFirst("\\b(in der naehe$)", "").trim();
			
			}else if (location_input.matches(".*\\b(wo .* (ist|sind|liegt|liegen|befindet|befinden))\\b.*")){
				location = location_input.replaceFirst(".*?\\b(wo (sich|))\\b", "").trim();
				location = location.replaceFirst("^(der|die|das|eine|ein|einen|den)\\b", "").trim();
				location = location.replaceFirst("\\b(ist|sind|liegt|liegen|befindet|befinden)\\b", "").trim();
				location = location.replaceFirst("\\b(auf (der|einer) (karte|map)$|auf maps$)", "").trim();
				location = location.replaceFirst("\\b(in der naehe$)", "").trim();
			}
						
			//clean up a bit
			if (!location_end.isEmpty()){
				location_end = location_end.replaceFirst("\\b(auf (der|einer) karte$|auf maps$|auf (der|einer) map$)", "").trim();
				location_end = location_end.replaceFirst("^(bis)\\b", "").trim();
				location_end = location_end.replaceFirst("^(zu|zum|zur)\\b", "").trim();
				location_end = location_end.replaceFirst("^(der|die|das|dem|den|einer|einem)\\b", "").trim();
				location_end = location_end.replaceFirst("^(nahes|naechste(n|)|naechstgelegene(n|))\\b", "").trim();
			}
			if (!location_start.isEmpty()){
				location_start = location_start.replaceFirst("\\b(auf (der|einer) karte$|auf maps$|auf (der|einer) map$)", "").trim();
				location_start = location_start.replaceFirst("^(von)\\b", "").trim();
				location_start = location_start.replaceFirst("^(der|die|das|dem|den|einer|einem)\\b", "").trim();
				location_start = location_start.replaceFirst("^(nahes|naechste(n|)|naechstgelegene(n|))\\b", "").trim();
			}
		
		//English
		}else if (language.matches("en")){
			location_input = input.replaceFirst("\\b(at home)\\b", "home");
			location_input = input.replaceFirst("\\b(way home)\\b", "way to home");
			location_input = input.replaceFirst("\\b(me home)\\b", "me to home");
			location_input = replace_personal_locations(location_input, language);
			//get travel type and remove
			travel_type = NluTools.stringFindFirst(location_input, "train|tram|bus|rail|transit|ice|deutsche bahn|s-bahn|bike|bicycle|bicycling|plane|flights|flight|fly|flying|car|drive|driving|automobile|by foot|on foot|afoot|walk|walking|shared ride|ride");
			if (!travel_type.trim().isEmpty()){
				location_input = location_input.replaceFirst("\\b(and |)(by |with the |with |)(" + Pattern.quote(travel_type) + ")", "").trim();
			}
			
			//clean more - stupid verbs ^^ - TODO: remove verbs!
			location_input = location_input.replaceFirst("\\b((look|looking|search|searching|watch out|watch)( for))\\b", "search").trim();
			location_input = location_input.replaceFirst("\\b(i need to|i have to|to go|to drive|to walk|to find|to do|to bring|to (look|search)|to book|start to)\\b", "").trim();
			location_input = location_input.replaceFirst("\\b((will |)(become|be)$)\\b", "").trim();
			location_input = location_input.replaceFirst("\\b(something |)(to eat)\\b", "restaurant").trim();
			
			//get travel time and remove
			dates = get_N_dates(location_input, 2, language);
			if (dates.size() >= 2+1){
				travel_time = dates.get("date_tag_1");
				travel_time_end = dates.get("date_tag_2");
			}else{
				travel_time = dates.get("date_tag_1");
			}
			if (!travel_time.isEmpty()){
				location_input = dates.get("clean_text");
			}
			
			//get waypoint and remove
			if (location_input.matches(".*\\b(on (the|my) way|over|(and with|with|and) (a |)(intermediate stop|stop|stopover|over|halt))\\b.*")){
				//POI stop
				location_waypoint = NluTools.stringFindFirst(location_input, "get gas|get petrol|buy|shop|shopping|get money|get cash");
				String stop_phrase = NluTools.stringFindFirst(location_input, "to");
				//arbitrary stop
				if (location_waypoint.isEmpty() && location_input.matches(".*\\b(intermediate stop|stop over|stopover|stop|halt) (in|at|)\\b.*")){
					location_waypoint = location_input.replaceFirst(".*\\b(intermediate stop|stop over|stopover|stop|halt) (in|at|)\\b", "").trim();
				}else if (location_waypoint.isEmpty() && location_input.matches(".*\\b(over)\\b.*")){
					location_waypoint = location_input.replaceFirst(".*\\b(over)\\b", "").trim();
					location_waypoint = location_waypoint.replaceFirst("\\b(to)\\b.*", "").trim();
				}
				if (!location_waypoint.isEmpty()){
					//remove - check order first
					if (NluTools.checkOrder(location_input, stop_phrase, location_waypoint)){
						location_input = location_input.replaceFirst(""
								+ "\\b(i need to |i have to |)(get |buy |find |and with |and |with |(intermediate stop|stop over|stopover|halt) |"
								+ "over |"
								+ Pattern.quote(location_waypoint) + ")\\b.*", "").trim();
					}
					//clean up
					if (location_waypoint.matches("get gas|get petrol")){
						location_waypoint = "gas station";
						is_local_waypoint = true;
					}else if (location_waypoint.matches("buy|shop|shopping")){
						location_waypoint = "supermarket";
						is_local_waypoint = true;
					}else if (location_waypoint.matches("get cash|get money")){
						location_waypoint = "atm";
						is_local_waypoint = true;
					}
				}
			}
			
			//get start, end and common location
			if (location_input.matches(".*\\b(from)\\s.*\\b(to)\\s.*")){
				location_input = location_input.replaceFirst(".*?\\b(from)\\b", "").trim();
				location_start = location_input.replaceFirst("\\b(to)\\b.*", "").trim();
				location_end = location_input.replaceFirst(".*?\\b(to)\\b", "").trim();
				
			}else if (location_input.matches(".*\\b(between)\\s.*\\b(and)\\s.*")){
				location_input = location_input.replaceFirst(".*?\\b(between)\\b", "").trim();
				location_start = location_input.replaceFirst("\\b(and)\\b.*", "").trim();
				location_end = location_input.replaceFirst(".*?\\b(and)\\b", "").trim();
				
			}else if (location_input.matches(".*\\b(from)\\s.*")){
				location_start = location_input.replaceFirst(".*?\\b(from)\\b", "").trim();
				
			//}else if (location_input.matches(".*\\b((where|show|find|search|are there) .*(\\b)("+ get_POI_list(language) +")\\b.*)")){
			//	location = NLU_Tools.stringFindFirst(location_input, get_POI_list(language));
				
			}else if (location_input.matches(".*\\b((?<!(close |near ))to)\\s.*")){ 		//negative lookbehind "close to" "near to"
				location_end = location_input.replaceFirst(".*?\\b(to)\\b", "").trim();
				
			}else if (location_input.matches(".*\\b((address|location) (of)|in|at|for|on(?!( (the|a) map| maps)))\\s.*")){
				location = location_input.replaceFirst(".*?\\b((address|location) (of)|in|at|for|on)\\b", "").trim();
				location = location.replaceFirst("\\b(on (the |a |)(map|maps)$)", "").trim();
				
			}else if (location_input.matches(".*\\b(the )(closest|nearest|next)\\s.*")){
				location = location_input.replaceFirst(".*?\\b(closest|nearest|next)\\b", "").trim();	
				location = location.replaceFirst("\\b(on (the |a |)(map|maps)$)", "").trim();
				location = location.replaceFirst("\\b(is|are|lies)( here|)$", "").trim();
			
			}else if (location_input.matches(".*\\b((show|search|find|look(up| for|)) .*(\\b)(on (the |a |)(map|maps)|(close|near|around)))\\b.*")){
				location = location_input.replaceFirst(".*?\\b((show|search|find|look(up| for|))( me|)"
						+ "( on (the |a |)(map|maps)|))\\b", "").trim();
				location = location.replaceFirst("^(the|a)\\b", "").trim();
				location = location.replaceFirst("\\b(on (the |a |)(map|maps)$)", "").trim();
				location = location.replaceFirst("\\b(close|near|around)( to| by|)( me|)$", "").trim();
				
			}else if (location_input.matches(".*\\b(where (is|lies|are|does|can (i|one) (find|get)|on (the |a |)(map|maps)))\\b.*")){
				location = location_input.replaceFirst(".*?\\b(where (on (the |a |)(map|maps) |)"
						+ "(is|lies|are|does|can (i|one) (get|find)))\\b", "").trim();
				location = location.replaceFirst("^(the|a)\\b", "").trim();
				location = location.replaceFirst("^(lie|lay)\\b", "").trim();
				location = location.replaceFirst("\\b(on (the |a |)(map|maps)$)", "").trim();
				location = location.replaceFirst("\\b(close|near|around)( to| by|)( me|)( here|)$", "").trim();

			}else if (location_input.matches(".*\\b(where .* (is|are|lie|lies|lay))\\b.*")){
				location = location_input.replaceFirst(".*?\\b(where (does|))\\b", "").trim();
				location = location.replaceFirst("^(the|a)\\b", "").trim();
				location = location.replaceFirst("\\b(is|are|lie|lies|lay)\\b", "").trim();
				location = location.replaceFirst("\\b(on (the |a |)(map|maps)$)", "").trim();
				location = location.replaceFirst("\\b(close|near|around)( to| by|)( me|)( here|)$", "").trim();
			}
			
			//clean up a bit
			if (!location_end.isEmpty()){
				location_end = location_end.replaceFirst("\\b(on (the|a) map$|on maps$)", "").trim();
				location_end = location_end.replaceFirst("^(to)\\b", "").trim();
				location_end = location_end.replaceFirst("^(a|the)\\b", "").trim();
				location_end = location_end.replaceFirst("^(closest|nearest|next)\\b", "").trim();
			}
			if (!location_start.isEmpty()){
				location_start = location_start.replaceFirst("\\b(on (the|a) map$|on maps$)", "").trim();
				location_start = location_start.replaceFirst("^(from)\\b", "").trim();
				location_start = location_start.replaceFirst("^(a|the)\\b", "").trim();
				location_start = location_start.replaceFirst("^(closest|nearest|next)\\b", "").trim();
			}
		
		//no result/missing language support ...
		}else{
			//leave it empty
		}
		
		//get POI from list
		//TODO: it is too simple! e.g. POI "Hotel" could well be "XY Hotel" or "Hotel XY" ... 
		//TODO: how does that work together with Place.getPOI... method again???
		poi = NluTools.stringFindFirst(location_input, get_POI_list(language));
		//try to predict POI - not that you cannot replace afterwards (see description)
		if (poi.isEmpty()){
			poi = get_POI_guess(input, language);
		}
		//double check poi against location
		if (!location.isEmpty() && location.contains(poi)){
			poi = "";
		}
		
		//check once more for personal locations
		if (location.isEmpty() && location_end.isEmpty() && location_start.isEmpty()){
			String personal_loc = User.containsUserSpecificLocation(location_input, null);
			if (!personal_loc.isEmpty()){
				location = personal_loc;
			}
		}
		
		//TODO: should be replace this result by its own class?
		pv.put("location", location.trim());
		pv.put("location_start", location_start.trim());
		pv.put("location_waypoint", location_waypoint.trim());
		pv.put("is_local_waypoint", String.valueOf(is_local_waypoint).trim());
		pv.put("location_end", location_end.trim());
		pv.put("poi", poi.trim());
		pv.put("travel_type", travel_type.trim());
		pv.put("travel_time", travel_time.trim());
		pv.put("travel_time_end", travel_time_end.trim());
		return pv;
	}
	
	/**
	 * Clean up input that contains typical search parameters. The cleaned up input is stored as "search" in the map.
	 * @param input - user text input or any text to be searched
	 * @param language - ... language code as usual 
	 * @return
	 */
	public static HashMap<String, String> get_search(String input, String language){
		//store all parameters
		HashMap<String, String> pv = new HashMap<String, String>();
		String search = "";
		
		//German
		if (language.matches("de")){
			String search_input = input.replaceFirst(".*?\\b(internet durchsuchen|durchsuche das (web|internet)|web suche|websuche|suche|google|bing|finde|zeig mir|zeig)( online| im web| im netz| im internet|)", "").trim();
			search_input = search_input.replaceFirst("\\b(durchsuchen$|suchen$|finden$|zeigen$)\\b", "").trim();
			search_input = search_input.replaceFirst("\\b(suchen |finden |zeigen |)(online$|im internet$|im web$|im netz$)\\b", "").trim();
			search_input = search_input.replaceFirst("\\b(zu$)\\b", "").trim();
			search = search_input.replaceFirst(".*?\\b(^der|^die|^das|^ein|^einen|^eine|^einem|^den|^nach|^fuer|^fuers)\\b", "").trim();
		
		//English
		}else if (language.matches("en")){
			String search_input = input.replaceFirst(".*?\\b(web search|websearch|google|bing|find|show me|show|(look|search|looking|searching) for|search)"
												+ "( online| the web| on the internet| on the net|)", "").trim();
			search_input = search_input.replaceFirst("\\b(online$|on the web$|on the internet$|on the net$)\\b", "").trim();
			search = search_input.replaceFirst(".*?\\b(^a|^an|^the|^for)\\b", "").trim();
		
		//no result/missing language support ...
		}else{
			//leave it empty
		}
		
		pv.put("search", search.trim());
		return pv;
	}
	
	/**
	 * Search text for the first date, day or time. The result is saved as "date_tag" and "time_tag" in the map. Can be an empty string.
	 * TODO: (misses local date) Try to convert the date_tag into a real date in default format relative to user local date and save as "date".
	 * @param input - user text input or any text to be searched
	 * @param language - ... language code as usual 
	 * @return
	 */
	public static HashMap<String, String> get_date(String text, String language){
		//store all parameters
		HashMap<String, String> pv = new HashMap<String, String>();
		String date_tag ="";
		String time_tag ="";
		
		//Check UNIX time first (we are more tolerant than required here ^^)
		String unixTimeString = NluTools.stringFindFirst(text, "\\b\\d{12,14}\\b");
		if (!unixTimeString.isEmpty()){
			pv.put("date_tag", unixTimeString);
			pv.put("time_tag", "");
			return pv;
		}
		
		//Common
		text = replace_all_months_by_numbers(text, language);
		
		//German
		if (language.matches(LANGUAGES.DE)){
			//note: make sure you use the same replacements in "remove_date(..)" too!
			text = replace_text_number_followed_by(text, DateAndTime.TIME_TAGS_DE, language);
			
			time_tag = NluTools.stringFindFirst(text, DateAndTime.TIME_UNSPECIFIC_TAGS_DE);
			if (!time_tag.isEmpty()) text = NluTools.stringRemoveFirst(text, time_tag);
			
			String relativeOrSpecificDay = "(" + DateAndTime.DAY_TAGS_RELATIVE_DE + "( " + DateAndTime.DAY_TAGS_DE + "|)|" + DateAndTime.DAY_TAGS_DE + ")";
			date_tag = NluTools.stringFindFirst(text, ""
					+ DateAndTime.DATE_DIRECT_TAGS 	+ "( (um |fuer |ab |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_DE + ")" + "|)|"
					+ relativeOrSpecificDay 		+ " (um |fuer |ab |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_DE + ")" + "|"
					+ "(in |fuer |auf |um |ab |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_DE + ")" 
														+ "( (am |an dem |dem |den |)" + relativeOrSpecificDay
														+ "| (in) \\d+" + DateAndTime.TIME_TAGS_LARGE_DE + ")|"
					+ relativeOrSpecificDay + "|"
					+ "(in |fuer |auf |um |ab |)(\\d+" + DateAndTime.TIME_TAGS_DE + ") (und |)(\\d+" + DateAndTime.TIME_TAGS_DE + ")|"
					+ "(in |fuer |auf |um |ab |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_DE + ") (und |)(\\d+" + DateAndTime.TIME_TAGS_DE + ")|"
					+ "(in |fuer |auf |um |ab |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_DE + ") (am |an dem |dem |den |)" + DateAndTime.DATE_DIRECT_TAGS + "|"
					+ "(in |fuer |auf |um |ab |)(\\d+" + DateAndTime.TIME_TAGS_SHORT + ") (und |)(\\d+" + DateAndTime.TIME_TAGS_SHORT + ")|"
					+ "(in |fuer |auf |um |ab |)(\\d+" + DateAndTime.TIME_TAGS_DE + ")" + "( (um |fuer |ab |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_DE + ")" + "|)|"
					+ "(in |fuer |auf |um |ab |)(\\d+" + DateAndTime.TIME_TAGS_SHORT + ")|"
					+ "(in |fuer |auf |um |ab |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_DE + ")|"
					+ "(^(\\d{1,2}:\\d\\d|\\d{1,2})$)");
			//System.out.println(text); 			//DEBUG
			//System.out.println(date_tag); 		//DEBUG
			
		//English and non-supported language
		}else{
			//note: make sure you use the same replacements in "remove_date(..)" too!
			text = replace_text_number_followed_by(text, DateAndTime.TIME_TAGS_EN, language);
			
			time_tag = NluTools.stringFindFirst(text, DateAndTime.TIME_UNSPECIFIC_TAGS_EN);
			if (!time_tag.isEmpty()) text = NluTools.stringRemoveFirst(text, time_tag);
			
			String relativeOrSpecificDay = "(" + DateAndTime.DAY_TAGS_RELATIVE_EN + "( " + DateAndTime.DAY_TAGS_EN + "|)|" + DateAndTime.DAY_TAGS_EN + ")";
			date_tag = NluTools.stringFindFirst(text, "" 
					+ DateAndTime.DATE_DIRECT_TAGS	+ "( (at |for |from |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_EN + ")" + "|)|"
					+ "(\\d\\dth|\\dth|1st|2nd|3rd)"	+ "( (at |for |from |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_EN + ")" + "|)|"
					+ relativeOrSpecificDay + " (at |for |from |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_EN + ")" + "|"
					+ "(in |for |at |to |from |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_EN + ")" 
														+ "( (at the |at |the |on |)" + relativeOrSpecificDay
														+ "| (in) \\d+" + DateAndTime.TIME_TAGS_LARGE_EN + ")|"
					+ relativeOrSpecificDay + "|"
					+ "(in |for |at |to |from |)(\\d+" + DateAndTime.TIME_TAGS_EN + ") (and |)(\\d+" + DateAndTime.TIME_TAGS_EN + ")|"
					+ "(in |for |at |to |from |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_EN + ") (and |)(\\d+" + DateAndTime.TIME_TAGS_EN + ")|"
					+ "(in |for |at |to |from |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_EN + ") (at the |at |the |)" + DateAndTime.DATE_DIRECT_TAGS + "|"
					+ "(in |for |at |to |from |)(\\d+" + DateAndTime.TIME_TAGS_SHORT + ") (and |)(\\d+" + DateAndTime.TIME_TAGS_SHORT + ")|"
					+ "(in |for |at |to |from |)(\\d+" + DateAndTime.TIME_TAGS_EN + ")" + "( (at |for |from |)((\\d+:\\d+|\\d+)" + DateAndTime.CLOCK_TAGS_EN + ")" + "|)|"
					+ "(in |for |at |to |from |)(\\d+" + DateAndTime.TIME_TAGS_SHORT + ")|"
					+ "(in |for |at |to |from |)((\\d{1,2}:\\d\\d|\\d{1,2})" + DateAndTime.CLOCK_TAGS_EN + ")|"
					+ "(^(\\d{1,2}:\\d\\d|\\d{1,2})$)");
			//System.out.println(text); 					//DEBUG
			//System.out.println(date_tag); 				//DEBUG
		}
		
		/*
		String date = "";
		if (!date_tag.trim().isEmpty()){
			date = NLU_parameter_search.convert_date(date, Config.default_sdf, input)
		}
		*/
		pv.put("date_tag", date_tag.trim());
		pv.put("time_tag", time_tag.trim());
		return pv;
	}
	/**
	 * Search text for the N date or day tags by repeating get_date -> remove_date.
	 * The result is saved as "date_tag_1..2..3.." in the map. If there is none the loop is stopped and the size of the map is smaller than N+1.
	 * The final (left over) text is saved as "clean_text" in the map too.
	 * @param input - user text input or any text to be searched
	 * @param N - number of dates to search
	 * @param language - ... language code as usual 
	 * @return map with found dates and size smaller or equal to N+1 (+1 because the clean text is always there)
	 */
	public static HashMap<String, String> get_N_dates(String text, int N, String language){
		//store all parameters
		HashMap<String, String> pv = new HashMap<String, String>();
		String date_tag ="";
		
		//loop N times
		for (int i=1; i<=N; i++){
			date_tag = get_date(text, language).get("date_tag").trim(); 		//TODO: add time_tag !!!
			if (date_tag.isEmpty()){
				pv.put("date_tag_" + i, date_tag);
				break;
			}else{
				pv.put("date_tag_" + i, date_tag);
				text = remove_date(text, date_tag, language);
			}
		}
		
		pv.put("clean_text", text);
		return pv;
	}
	/**
	 * Remove a found date from a string.
	 * @param text - text that needs to be cleaned
	 * @param date - the date as it was previously found in the text (not cleaned or converted!)
	 * @param language - ... code as usual
	 * @return cleaned up input
	 */
	public static String remove_date(String text, String date, String language){
		//Common
		text = replace_all_months_by_numbers(text, language);
		
		//English
		if (language.equals(LANGUAGES.EN)){
			text = replace_text_number_followed_by(text, DateAndTime.TIME_TAGS_EN, language);
			text = text.replaceFirst("\\b(from the |from |(starting|arrival) at (the |))(" + Pattern.quote(date.trim()) + ")(\\s|$)", "").trim();
			text = text.replaceFirst("\\b(until the |until |till the |till |(departure) at (the |)|to the |to )(" + Pattern.quote(date.trim()) + ")(\\s|$)", "").trim();
			text = text.replaceFirst("\\b(for the |for |in the |during the |at the |at |the |this |in |on |)(" + Pattern.quote(date.trim()) + ")(\\s|$)", "").trim();
			return text;
			
		//German
		}else if (language.equals(LANGUAGES.DE)){
			text = replace_text_number_followed_by(text, DateAndTime.TIME_TAGS_DE, language);
			text = text.replaceFirst("\\b(von dem |vom |ab dem |ab |von |anreise (am|an dem) )(" + Pattern.quote(date.trim()) + ")(\\s|$)", "").trim();
			text = text.replaceFirst("\\b(bis zu dem |bis zum |bis |abreise (am|an dem) )(" + Pattern.quote(date.trim()) + ")(\\s|$)", "").trim();
			text = text.replaceFirst("\\b(fuer den |fuer |in dem |in den |waehrend der |an dem |der |die |dem |diesem |diesen |dieser |in |am |)(" + Pattern.quote(date.trim()) + ")(\\s|$)", "").trim();
			return text;
			
		//non-supported language
		}else{
			return text;
		}
	}
	
	/**
	 * Convert a date found with get_date(..) to the desired format. Use it e.g. to make "today" to "dd.MM.yyyy".
	 * @param date_input - input date string found by using get_date(...)
	 * @param format - desired format like "dd.MM.yyyy"
	 * @param nlu_input - input sent by client, required for local time
	 * @return date string in desired format or empty
	 */
	/*
	public static String convert_date(String date_input, String format, NLU_Input nlu_input){
		String date = date_input.trim();
		String language = nlu_input.language;
		
		//language independent - TODO: improve
		if (date_input.isEmpty()){
			return "";//Tools_DateTime.getToday(format, nlu_input);
		}else if (date.matches(Config.default_sdf_regex)){
			return Tools_DateTime.convertDateFormat(date, Config.default_sdf, format);
		}
		//eu
		else if (date.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4,}")){
			return Tools_DateTime.convertDateFormat(date, "dd.MM.yyyy", format);
		}else if (date.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{2,}")){
			return Tools_DateTime.convertDateFormat(date, "dd.MM.yy", format);
		}else if (date.matches("\\d{1,2}\\.\\d{1,2}(\\.|)")){
			String year = Tools_DateTime.getToday("yyyy", nlu_input);
			return Tools_DateTime.convertDateFormat((date+"."+year).replaceAll("\\.\\.", "."), "dd.MM.yyyy", format);
		}else if (date.matches("\\d{1,2}\\.\\d{4,}")){
			String day = Tools_DateTime.getToday("dd", nlu_input);
			return Tools_DateTime.convertDateFormat((day+"."+date).replaceAll("\\.\\.", "."), "dd.MM.yyyy", format);
		}else if (date.matches("\\d{1,2}(\\.|)")){
			String month_year = Tools_DateTime.getToday("MM.yyyy", nlu_input);
			return Tools_DateTime.convertDateFormat((date+"."+month_year).replaceAll("\\.\\.", "."), "dd.MM.yyyy", format);
		}
		//us
		else if (date.matches("\\d{1,2}/\\d{1,2}/\\d{4,}")){
			return Tools_DateTime.convertDateFormat(date, "MM/dd/yyyy", format);
		}else if (date.matches("\\d{1,2}/\\d{1,2}/\\d{2,}")){
			return Tools_DateTime.convertDateFormat(date, "MM/dd/yy", format);
		}else if (date.matches("\\d{1,2}/\\d{4,}")){
			String day = Tools_DateTime.getToday("dd", nlu_input);
			return Tools_DateTime.convertDateFormat(day+"/"+date, "dd/MM/yyyy", format);
		}else if (date.matches("\\d{1,2}/\\d{1,2}")){
			String year = Tools_DateTime.getToday("yyyy", nlu_input);
			return Tools_DateTime.convertDateFormat(date+"/"+year, "MM/dd/yyyy", format);
		}
		
		//English
		if (language.matches("en")){
			//today
			if (NLU_Tools.stringContains(date, "today|now")){
				return Tools_DateTime.getToday(format, nlu_input);
			//day after tomorrow 
			}else if (NLU_Tools.stringContains(date, "day after tomorrow|2 days")){
				return Tools_DateTime.getDayAfterTomorrow(format, nlu_input);
			//tomorrow
			}else if (NLU_Tools.stringContains(date, "tomorrow|1 day")){
				return Tools_DateTime.getTomorrow(format, nlu_input);
			//monday to sunday
			}else if (NLU_Tools.stringContains(date, "monday|tuesday|wednesday|thursday|friday|saturday|sunday|weekend")){
				int dow = 0;
				if (NLU_Tools.stringContains(date, "monday")){
					dow = Calendar.MONDAY;
				}else if (NLU_Tools.stringContains(date, "tuesday")){
					dow = Calendar.TUESDAY;
				}else if (NLU_Tools.stringContains(date, "wednesday")){
					dow = Calendar.WEDNESDAY;
				}else if (NLU_Tools.stringContains(date, "thursday")){
					dow = Calendar.THURSDAY;
				}else if (NLU_Tools.stringContains(date, "friday")){
					dow = Calendar.FRIDAY;
				}else if (NLU_Tools.stringContains(date, "saturday|weekend")){
					dow = Calendar.SATURDAY;
				}else if (NLU_Tools.stringContains(date, "sunday")){
					dow = Calendar.SUNDAY;
				}
				return Tools_DateTime.getDateForDayOfWeek(dow, format, nlu_input);
			//in X hours and X minutes
			}else if (NLU_Tools.stringContains(date, "\\d+ hour(s|) (and |)\\d+ minute(s|)|"
									+ "\\d+(h| h|hr| hr) (and |)\\d+(min| min)")){
				String h = NLU_Tools.stringFindFirst(date, "\\d+ hour(s|)|\\d+(h| h|hr| hr)");
				String min = NLU_Tools.stringFindFirst(date, "\\d+ minute(s|)|\\d+(min| min)");
				int Xh = Integer.parseInt(h.replaceAll("\\D", "").trim()) * 60;
				int Xm = Integer.parseInt(min.replaceAll("\\D", "").trim());
				return Tools_DateTime.getToday_plus_X_minutes(format, nlu_input, Xh + Xm);
			//in X minutes and X seconds
			}else if (NLU_Tools.stringContains(date, "\\d+ minute(s|) (and |)\\d+ second(s|)|"
									+ "\\d+(min| min) (and |)\\d+(s| s|sec| sec)")){
				String min = NLU_Tools.stringFindFirst(date, "\\d+ minute(s|)|\\d+(min| min)");
				String sec = NLU_Tools.stringFindFirst(date, "\\d+ second(s|)|\\d+(s| s|sec| sec)");
				int Xm = Integer.parseInt(min.replaceAll("\\D", "").trim()) * 60;
				int Xs = Integer.parseInt(sec.replaceAll("\\D", "").trim());
				return Tools_DateTime.getToday_plus_X_seconds(format, nlu_input, Xm + Xs);
			//in X days
			}else if (NLU_Tools.stringContains(date, "\\d+ day(s|)|\\d+(d| d)")){
				String d = NLU_Tools.stringFindFirst(date, "\\d+ day(s|)|\\d+(d| d)");
				int X = Integer.parseInt(d.replaceAll("\\D", "").trim()) * 60 * 24;
				return Tools_DateTime.getToday_plus_X_minutes(format, nlu_input, X);
			//in X hours
			}else if (NLU_Tools.stringContains(date, "\\d+ hour(s|)|\\d+(h| h|hr| hr)")){
				String h = NLU_Tools.stringFindFirst(date, "\\d+ hour(s|)|\\d+(h| h|hr| hr)");
				int X = Integer.parseInt(h.replaceAll("\\D", "").trim()) * 60;
				return Tools_DateTime.getToday_plus_X_minutes(format, nlu_input, X);
			//in X minutes
			}else if (NLU_Tools.stringContains(date, "\\d+ minute(s|)|\\d+(min| min)")){
				String min = NLU_Tools.stringFindFirst(date, "\\d+ minute(s|)|\\d+(min| min)");
				int X = Integer.parseInt(min.replaceAll("\\D", "").trim());
				return Tools_DateTime.getToday_plus_X_minutes(format, nlu_input, X);
			//in X seconds
			}else if (NLU_Tools.stringContains(date, "\\d+ second(s|)|\\d+(s| s|sec| sec)")){
				String sec = NLU_Tools.stringFindFirst(date, "\\d+ second(s|)|\\d+(s| s|sec| sec)");
				int X = Integer.parseInt(sec.replaceAll("\\D", "").trim());
				return Tools_DateTime.getToday_plus_X_seconds(format, nlu_input, X);
			
			//found nothing
			}else{
				return "";
			}
			
		//German
		}else if (language.matches("de")){
			//today
			if (NLU_Tools.stringContains(date, "heute morgen|heute|jetzt")){
				return Tools_DateTime.getToday(format, nlu_input);
			//day after tomorrow 
			}else if (NLU_Tools.stringContains(date, "uebermorgen morgen|uebermorgen|2 tagen|2 tage")){
				return Tools_DateTime.getDayAfterTomorrow(format, nlu_input);
			//tomorrow
			}else if (NLU_Tools.stringContains(date, "morgen|1 tag")){
				return Tools_DateTime.getTomorrow(format, nlu_input);
			//monday to sunday
			}else if (NLU_Tools.stringContains(date, "montag|dienstag|mittwoch|donnerstag|freitag|samstag|sonntag|wochenende")){
				int dow = 0;
				if (NLU_Tools.stringContains(date, "montag")){
					dow = Calendar.MONDAY;
				}else if (NLU_Tools.stringContains(date, "dienstag")){
					dow = Calendar.TUESDAY;
				}else if (NLU_Tools.stringContains(date, "mittwoch")){
					dow = Calendar.WEDNESDAY;
				}else if (NLU_Tools.stringContains(date, "donnerstag")){
					dow = Calendar.THURSDAY;
				}else if (NLU_Tools.stringContains(date, "freitag")){
					dow = Calendar.FRIDAY;
				}else if (NLU_Tools.stringContains(date, "samstag|wochenende")){
					dow = Calendar.SATURDAY;
				}else if (NLU_Tools.stringContains(date, "sonntag")){
					dow = Calendar.SUNDAY;
				}
				return Tools_DateTime.getDateForDayOfWeek(dow, format, nlu_input);
			//in X hours and X minutes
			}else if (NLU_Tools.stringContains(date, "\\d+ stunde(n|) (und |)\\d+ minute(n|)|"
									+ "\\d+(h| h|hr| hr) (und |)\\d+(min| min)")){
				String h = NLU_Tools.stringFindFirst(date, "\\d+ stunde(n|)|\\d+(h| h|hr| hr)");
				String min = NLU_Tools.stringFindFirst(date, "\\d+ minute(n|)|\\d+(min| min)");
				int Xh = Integer.parseInt(h.replaceAll("\\D", "").trim()) * 60;
				int Xm = Integer.parseInt(min.replaceAll("\\D", "").trim());
				return Tools_DateTime.getToday_plus_X_minutes(format, nlu_input, Xh + Xm);
			//in X minutes and X seconds
			}else if (NLU_Tools.stringContains(date, "\\d+ minute(n|) (und |)\\d+ sekunde(n|)|"
									+ "\\d+(min| min) (und |)\\d+(s| s|sec| sec)")){
				String min = NLU_Tools.stringFindFirst(date, "\\d+ minute(n|)|\\d+(min| min)");
				String sec = NLU_Tools.stringFindFirst(date, "\\d+ sekunde(n|)|\\d+(s| s|sec| sec)");
				int Xm = Integer.parseInt(min.replaceAll("\\D", "").trim()) * 60;
				int Xs = Integer.parseInt(sec.replaceAll("\\D", "").trim());
				return Tools_DateTime.getToday_plus_X_seconds(format, nlu_input, Xm + Xs);
			//in X days
			}else if (NLU_Tools.stringContains(date, "\\d+ tag(en|e|)|\\d+(d| d)")){
				String d = NLU_Tools.stringFindFirst(date, "\\d+ tag(en|e|)|\\d+(d| d)");
				int X = Integer.parseInt(d.replaceAll("\\D", "").trim()) * 60 * 24;
				return Tools_DateTime.getToday_plus_X_minutes(format, nlu_input, X);
			//in X hours
			}else if (NLU_Tools.stringContains(date, "\\d+ stunde(n|)|\\d+(h| h|hr| hr)")){
				String h = NLU_Tools.stringFindFirst(date, "\\d+ stunde(n|)|\\d+(h| h|hr| hr)");
				int X = Integer.parseInt(h.replaceAll("\\D", "").trim()) * 60;
				return Tools_DateTime.getToday_plus_X_minutes(format, nlu_input, X);
			//in X minutes
			}else if (NLU_Tools.stringContains(date, "\\d+ minute(n|)|\\d+(min| min)")){
				String min = NLU_Tools.stringFindFirst(date, "\\d+ minute(n|)|\\d+(min| min)");
				int X = Integer.parseInt(min.replaceAll("\\D", "").trim());
				return Tools_DateTime.getToday_plus_X_minutes(format, nlu_input, X);
			//in X seconds
			}else if (NLU_Tools.stringContains(date, "\\d+ sekunde(n|)|\\d+(s| s|sec| sec)")){
				String sec = NLU_Tools.stringFindFirst(date, "\\d+ sekunde(n|)|\\d+(s| s|sec| sec)");
				int X = Integer.parseInt(sec.replaceAll("\\D", "").trim());
				return Tools_DateTime.getToday_plus_X_seconds(format, nlu_input, X);
			
			//found nothing
			}else{
				return "";
			}
			
		//non-supported language
		}else{
			return "";
		}
	}
	*/
	/**
	 * Search first month in input and return a String array with [0]: month number string (e.g. January=01, -1 for no result)
	 * and [1]: month name found in input. 
	 * @param input - string to search
	 * @param language - language code
	 * @return String array<br>
	 * [0]: month number string (e.g. January=01, -1 for no result)<br>
	 * [1]: month name found in input.
	 */
	public static String[] get_month(String input, String language){
		int month_nbr;
		String month;
		String[] result = new String[2];
		
		//German
		if (language.matches("de")){
			month = NluTools.stringFindFirst(input, "januar|februar|maerz|april|mai|juni|juli|august|september|oktober|november|dezember");
			switch (month) {
			    case "januar": 		month_nbr=1;  break;
				case "februar": 	month_nbr=2;  break;
				case "maerz":		month_nbr=3;  break;
				case "april":		month_nbr=4;  break;
				case "mai":			month_nbr=5;  break;
				case "juni":		month_nbr=6;  break;
				case "juli":		month_nbr=7;  break;
				case "august":		month_nbr=8;  break;
				case "september":	month_nbr=9;  break;
				case "oktober":		month_nbr=10;  break;
				case "november":	month_nbr=11;  break;
				case "dezember": 	month_nbr=12;  break;
				default: month_nbr=-1;  break;
			}
			
		//English and other
		}else{
			month = NluTools.stringFindFirst(input, "january|february|march|april|may|june|july|august|september|october|november|december");
			switch (month) {
			    case "january": 	month_nbr=1;  break;
				case "february": 	month_nbr=2;  break;
				case "march":		month_nbr=3;  break;
				case "april":		month_nbr=4;  break;
				case "may":			month_nbr=5;  break;
				case "june":		month_nbr=6;  break;
				case "july":		month_nbr=7;  break;
				case "august":		month_nbr=8;  break;
				case "september":	month_nbr=9;  break;
				case "october":		month_nbr=10;  break;
				case "november":	month_nbr=11;  break;
				case "december": 	month_nbr=12;  break;
				default: month_nbr=-1;  break;
			}			
		}
		
		if (month_nbr > 0 && month_nbr < 10){
			result[0] = "0" + month_nbr;
		}else{
			result[0] = String.valueOf(month_nbr);
		}
		result[1] = month;
		return result;
	}
	/**
	 * Replace all month names in a string with their corresponding numbers (January=01.).
	 * If the month is followed or preceded by a day like "first of February" or "February the first" it is converted to "01.02."
	 * @param input - string to search
	 * @param language - language code
	 * @return string with names replaced by numbers 
	 */
	public static String replace_all_months_by_numbers(String input, String language){
		String tmp[];
		//TODO: what did I do here?? This method became utterly clumsy o_O
		
		//German
		if (language.matches("de")){
			String num_strings = "(erste|erster|ersten|zweiter|zweite|zweiten|dritter|dritte|dritten|\\d+(\\.|))";
			while (!(tmp = get_month(input, language))[1].isEmpty()){
				String date_block = NluTools.stringFindFirst(input, num_strings + " " + Pattern.quote(tmp[1]));
				if (!date_block.isEmpty()){
					input = input.replaceFirst(date_block, date_block.trim().replaceFirst("(\\.\\s|\\s)", ".").trim());
				}
				input = input.replaceFirst(tmp[1], tmp[0]+".");
			}
			input = input.replaceAll("\\b(erste|erster|ersten)(\\.)", "01.");
			input = input.replaceAll("\\b(zweiter|zweite|zweiten)(\\.)", "02.");
			input = input.replaceAll("\\b(dritter|dritte|dritten)(\\.)", "03.");
			input = input.replaceAll("\\b(vierter|vierte|vierten)(\\.)", "04.");
			input = input.replaceAll("\\b(fuenfter|fuenfte|fuenften)(\\.)", "05.");
			input = input.replaceAll("\\b(sechster|sechste|sechsten)(\\.)", "06.");
			input = input.replaceAll("\\b(siebter|siebte|siebten)(\\.)", "07.");
			input = input.replaceAll("\\b(achter|achte|achten)(\\.)", "08.");
			input = input.replaceAll("\\b(neunter|neunte|neunten)(\\.)", "09.");
			input = input.replaceAll("\\b(zehnter|zehnte|zehnten)(\\.)", "10.");
			input = input.replaceAll("\\b(elfter|elfte|elfen)(\\.)", "11.");
			input = input.replaceAll("\\b(zwoelfter|zwoelfte|zwoelften)(\\.)", "12.");
			//...continue?
		
		//English and rest
		}else{
			String num_strings = "(first|1st|second|2nd|third|3rd|\\d+th|\\d+(\\.|))";
			while (!(tmp = get_month(input, language))[1].isEmpty()){
				String date_block = NluTools.stringFindFirst(input, num_strings + "( of | )" + Pattern.quote(tmp[1]));
				if (!date_block.isEmpty()){
					input = input.replaceFirst(date_block, date_block.replaceFirst("(\\. of | of | )", ".").trim());
				}else{
					date_block = NluTools.stringFindFirst(input, Pattern.quote(tmp[1]) + "( the | )" + num_strings);
					if (!date_block.isEmpty()){
						input = input.replaceFirst(date_block, date_block.replaceFirst(".*?( the\\s|\\s)(.*?)(\\.$|$)", "$2").trim() + "." + tmp[1]);
					}
				}
				input = input.replaceFirst(tmp[1], tmp[0]+".");
			}
			input = input.replaceAll("\\b(first|1st)(\\.)", "01.");
			input = input.replaceAll("\\b(second|2nd)(\\.)", "02.");
			input = input.replaceAll("\\b(third|3rd)(\\.)", "03.");
			input = input.replaceAll("\\b(forth)(\\.)", "04.");
			input = input.replaceAll("\\b(fifth)(\\.)", "05.");
			input = input.replaceAll("\\b(sixth)(\\.)", "06.");
			input = input.replaceAll("\\b(seventh)(\\.)", "07.");
			input = input.replaceAll("\\b(eighth)(\\.)", "08.");
			input = input.replaceAll("\\b(ninth)(\\.)", "09.");
			input = input.replaceAll("\\b(tenth)(\\.)", "10.");
			input = input.replaceAll("\\b(eleventh)(\\.)", "11.");
			input = input.replaceAll("\\b(twelfth)(\\.)", "12.");
			input = input.replaceAll("\\b(\\d+)th(\\.)", "$1.");
			//...continue?
		}
		
		//check for a year at the end
		input = input.replaceAll("\\b(\\d{1,2}\\.)(\\d{1,2}\\.)\\s(\\d{4})\\b", "$1$2$3");
		
		return input;
	}
	
	/**
	 * Search string for list type.
	 * @param input - string to search
	 * @param language - language code
	 * @return tag for list type ("shopping", "todo", ...) or empty
	 */ /*
	public static String get_List_type(String input, String language){
		String type = "";
		//German
		if (language.matches("de")){
			if (NLU_Tools.stringContains(input, "to do|to-do|to-do-liste|todo|todo-liste")){
				type = "to-do";
			}else if (NLU_Tools.stringContains(input, "einkaufen|einkaufsliste|einkaufszettel|kaufen|kaufliste|shoppingliste|shopping")){
				type = "shopping";
			}
		//English and other
		}else{
			if (NLU_Tools.stringContains(input, "to do list|to-do|todo|todo-list")){
				type = "to-do";
			}else if (NLU_Tools.stringContains(input, "shopping|shoppinglist|shop|buy")){
				type = "shopping";
			}
		}
		return type;
	}*/
	/**
	 * Search string for list action.
	 * @param input - string to search
	 * @param language - language code
	 * @return tag for list action ("add", "remove", "open", ...) or empty
	 */	/*
	public static String get_List_action(String input, String language){
		String action = "";
		//German
		if (language.matches("de")){
			if (NLU_Tools.stringContains(input, "fuege .*\\bhinzu|hinzufuegen|setze|setzen|stelle|stellen|ergaenze|ergaenzen")){
				action = "add";
			}else if (NLU_Tools.stringContains(input, "entferne|entfernen|loesche|loeschen|nimm .*\\bvon")){
				action = "remove";
			}else if (NLU_Tools.stringContains(input, "oeffne|oeffnen|anzeigen|zeig|zeigen|check|checken")){
				action = "open";
			}
		//English and other
		}else{
			if (NLU_Tools.stringContains(input, "add|put|set")){
				action = "add";
			}else if (NLU_Tools.stringContains(input, "remove|delete|take")){
				action = "remove";
			}else if (NLU_Tools.stringContains(input, "open|show|display")){
				action = "open";
			}
		}
		return action;
	}*/
	
	/**
	 * Search the string for a favorite or personal item. E.g. "my personal medicine".
	 * @param input - user input text
	 * @param language - code as usual ... its clear or?
	 * @return Array size 2 with 0: short version (e.g. "medicine"), 1: long version (e.g. "my personal medicine")  or empty strings
	 */
	public static String[] search_my_info_item(String input, String language){
		String item = "";			//short version of item
		String item_long = "";		//long version including "my personal ..."
		
		//German
		if (language.matches("de")){
			item = NluTools.stringFindFirst(input, "lieblings\\w+|lieblings \\w+\\b");
			if (item.isEmpty()){
				item = NluTools.stringFindFirst(input, "(mein |meine |meinen |meinem |meiner )"
												+ "(persoenliche(r|s|n|) |private(r|s|n|) )(lieblings \\w+\\b|\\w+\\b)");
				item = item.replaceFirst("^(mein|meine|meinen|meinem|meiner)\\b", "").trim();
				item = item.replaceFirst("^(persoenliche(r|s|n|)|private(r|s|n|))\\b", "").trim();
			}
			if (!item.isEmpty()){
				item_long = NluTools.stringFindFirst(input, "(mein |meine |meinen |meinem |meiner ).*?\\b(" + item + ")");
			}
			
		//English
		}else if (language.matches("en")){
			item = NluTools.stringFindFirst(input, "my (personal |private |favorite )(\\w+\\b)");
			item = item.replaceFirst("^(my)\\b", "").trim();
			item = item.replaceFirst("^(personal|private)\\b", "").trim();
			if (!item.isEmpty()){
				item_long = NluTools.stringFindFirst(input, "(my ).*?\\b(" + item + ")");
			}
		
		//no result/missing language support ...
		}else{
			item = input;
			item_long = input;
		}
		//TODO: replace by a specific class for favorites
		return new String[]{item, item_long}; 
	}
	/**
	 * Clean up the string containing a favorite or personal item. E.g. my favorite "item" is ... "info".
	 * @param input - user input text
	 * @param language - code as usual ... its clear or?
	 * @return cleaned up input
	 */
	public static String get_my_info_item(String input, String language){
		String item = "";
		
		//German
		if (language.matches("de")){
			//i removed "meines" because of the nasty Genitive "s" at the end of the item
			item = input.replaceFirst(".*?\\b(mein |meine |meinen |meinem |meiner )(persoenliche(r|s|n|)|private(r|s|n|)|)\\b", "").trim();
			item = item.replaceFirst("\\b(ist|sind|lautet|lauten|heisst|heissen)\\b.*", "").trim();
			
		//English
		}else if (language.matches("en")){
			item = input.replaceFirst(".*?\\b(my personal|my private|my favorites|my info|my)\\b", "");
			item = item.replaceFirst("\\b(is|are)\\b.*", "").trim();
		
		//no result/missing language support ...
		}else{
			item = input;
		}
		return item; 
	}
	/**
	 * Clean up the string containing a favorite or personal info. E.g. my favorite "item" is ... "info".
	 * @param input - user input text
	 * @param language - code as usual ... its clear or?
	 * @return cleaned up input
	 */
	public static String get_my_info(String input, String language){
		String info = "";
		
		//German
		if (language.matches("de")){
			info = input.replaceFirst(".*?\\b(ist|sind|lautet|lauten|heisst|heissen)", "").trim();
			info = info.replaceFirst("\\b^(ein|eine)\\s", "").trim();
			
		//English
		}else if (language.matches("en")){
			info = input.replaceFirst(".*?\\b(is|are)\\b", "").trim();
			info = info.replaceFirst("\\b^(a|an)\\s", "").trim();
		
		//no result/missing language support ...
		}else{
			info = input;
		}
		return info; 
	}
	
	/**
	 * Search string for control types.
	 * @param input - string to search
	 * @param language - language code
	 * @return types like on, off, increase, decrease, set/toggle, complex - or empty string
	 */
	@Deprecated
	public static String[] get_control_action(String input, String language){
		String action = "";
		String extracted = "";
		String on, off, increase, decrease, set, toggle;
		//German
		if (language.matches("de")){
			on = "(mach|schalte) .*\\b(an|ein)|"
					+ "^\\w+\\b (an$|ein$)|"
					+ "oeffne|oeffnen|aktiviere|aktivieren|starte|starten|start|"
					+ "anschalten|einschalten|anmachen|spielen|spiele|spiel|abspielen";
			off = "(mach|schalte) .*\\b(aus)|"
					+ "^\\w+\\b (aus$)|"
					+ "schliessen|schliesse|deaktivieren|deaktiviere|"
					+ "beenden|beende|ausschalten|ausmachen|stoppen|stoppe|stop";
			increase = "(?<!(wie ))hoch|rauf|hoeher|groesser|erhoehen|erhoehe|verstaerken|verstaerke|heller|(?<!(ist ))schneller|(?<!(ist ))staerker|waermer|warm";
			decrease = "runter|kleiner|niedriger|erniedrigen|erniedrige|abschwaechen|schwaecher|schwaeche|dunkler|dimmen|dimme|(?<!(wie ))langsam|langsamer|kaelter|(?<!(wie ))kalt";
			set = "setzen|setze|stelle|stellen|aendern|aendere|auswaehlen|waehlen|waehle";
			toggle = "umschalten|schalten|schalte";
			extracted = NluTools.stringFindFirst(input, set + "|" + on + "|" + off + "|" 
													+ increase + "|" + decrease + "|" + toggle);
			
		//English and other
		}else{
			on = "(make|switch|turn) .*\\b(on)|"
					+ "^\\w+\\b (on$)|"
					+ "open|activate|start|play";
			off = "(make|switch|turn) .*\\b(off)|"
					+ "^\\w+\\b (off$)|"
					+ "close|deactivate|end|stop|shut\\b.*? down";
			increase = "(make|switch|turn) .*\\b(up)|"
					+ "^\\w+\\b (up$)|"
					+ "upwards|higher|bigger|increase|amplify|brighter|(?<!(is ))faster|(?<!(is ))stronger|fast|warmer|warm";
			decrease = "(make|switch|turn) .*\\b(down)|"
					+ "^\\w+\\b (down$)|"
					+ "downwards|smaller|lower|decrease|reduce|weaker|darker|dim|slow|(?<!(is ))slower|colder|cold";
			set = "set|put|change|select|choose";
			toggle = "toggle|switch";
			extracted = NluTools.stringFindFirst(input, set + "|" + on + "|" + off + "|" 
													+ increase + "|" + decrease + "|" + toggle);
		}
		//SET/TOGGLE 1
		if (NluTools.stringContains(extracted, set)){
			action = "<" + Action.Type.set + ">";
		//ON
		}else if (NluTools.stringContains(extracted, on)){
			action = "<" + Action.Type.on + ">";
		//OFF
		}else if (NluTools.stringContains(extracted, off)){
			action = "<" + Action.Type.off + ">";
		//INCREASE
		}else if (NluTools.stringContains(extracted, increase)){
			action = "<" + Action.Type.increase + ">";
		//DECREASE
		}else if (NluTools.stringContains(extracted, decrease)){
			action = "<" + Action.Type.decrease + ">";
		//
		//TODO: CHECK
		//	
		//SET/TOGGLE 2
		}else if (NluTools.stringContains(extracted, toggle)){
			action = "<" + Action.Type.toggle + ">";
		}else{
			action = "";
		}
		return new String[]{action, extracted};
	}	
	/**
	 * Search string for a device, program or item of some sort to control.
	 * @param input - string to search
	 * @param language - language code
	 * @return item/device/app ... or empty
	 */
	public static String get_control_type(String input, String language){
		String type = "";
		//German
		if (language.matches("de")){
			type = NluTools.stringFindFirst(input, ""
					+ "licht|lichter|lampe|lampen|"
					+ "heizung|heizungen|heizkoerper|temperatur|grad|"
					+ "tuer|tueren|tor|garage|garagentor|garagentuer|"
					+ "rollade|rolladen|fenster|vorhang|vorhaenge|"
					+ "alarmanlage|sprinkler|sprinkleranlage|dusche|badewanne|klima anlage|klimaanlage|"
					+ "kaffeemaschine|fernseher|tv|glotze|stereoanlage|radio|hi-fi anlage|musik|"
					+ "facebook|whatsapp|instagram|twitter");
			//App
			if (NluTools.stringContains(type, "facebook|whatsapp|instagram|twitter")){
				type = "<app_social>" + "die App";
			//Lights
			}else if (NluTools.stringContains(type, "licht|lichter|lampe|lampen")){
				type = "<device_light>" + "das Licht";
			//Heater
			}else if (NluTools.stringContains(type, "heizung|heizungen|heizkoerper|temperatur|grad")){
				type = "<device_heater>" + "der Heizkörper";
			//Door
			}else if (NluTools.stringContains(type, "tuer|tueren|tor|garage|garagentor|garagentuer")){
				type = "<device_door>" + "die Tür";
			//Window
			}else if (NluTools.stringContains(type, "fenster")){
				type = "<device_window>" + "das Fenster";
			//Shutter
			}else if (NluTools.stringContains(type, "rollade|rolladen|vorhang|vorhaenge")){
				type = "<device_shutter>" + "der Sichtschutz";
			//Multimedia
			}else if (NluTools.stringContains(type, "fernseher|tv|glotze")){
				type = "<device_tv>" + "der Fernseher";
			}else if (NluTools.stringContains(type, "stereoanlage|radio|hi-fi anlage|musik")){
				type = "<device_music>" + "die Musik";
			//Other
			}else if (NluTools.stringContains(type, "alarmanlage|sprinkler|sprinkleranlage|dusche|badewanne|klima anlage|klimaanlage|"
												+ "kaffeemaschine")){
				type = "<device_other>" + "das Gerät";
			//Unknown
			}else{
				type = "";
			}
			
		//English and other
		}else{
			type = NluTools.stringFindFirst(input, "light|lights|lighting|lamp|lamps|"
					+ "heater|heaters|heating|temperature|degrees|degree|"
					+ "door|doors|gate|gates|garage|garagedoor|"
					+ "shutter|shutters|window blinds|window|windows|curtains|curtain|"
					+ "alarm system|burglar alarm|sprinkler|sprinklers|sprinklersystem|rain gun|shower|bathtub|smoke detector|patio awning|air condition|air conditioning|"
					+ "coffee maschine|coffee brewer|tv|television|stereo|soundsystem|radio|hi-fi system|music|"
					+ "facebook|whatsapp|instagram|twitter");
			//App
			if (NluTools.stringContains(type, "facebook|whatsapp|instagram|twitter")){
				type = "<app_social>" + type;
			//Lights
			}else if (NluTools.stringContains(type, "light|lights|lighting|lamp|lamps")){
				type = "<device_light>" + type;
			//Heater
			}else if (NluTools.stringContains(type, "heater|heaters|heating|temperature|degrees|degree")){
				type = "<device_heater>" + type;
			//Door
			}else if (NluTools.stringContains(type, "door|doors|gate|gates|garage|garagedoor")){
				type = "<device_door>" + type;
			//Shutter
			}else if (NluTools.stringContains(type, "shutter|shutters|window blinds|curtains|curtain")){
				type = "<device_shutter>" + type;
			//Window
			}else if (NluTools.stringContains(type, "window|windows")){
				type = "<device_window>" + type;
			//Multimedia
			}else if (NluTools.stringContains(type, "tv|television")){
				type = "<device_tv>" + type;		
			}else if (NluTools.stringContains(type, "stereo|soundsystem|radio|hi-fi system|music")){
				type = "<device_music>" + type;			
			//Other
			}else if (NluTools.stringContains(type, "alarm system|burglar alarm|sprinkler|sprinklers|sprinklersystem|rain gun|shower|bathtub|smoke detector|patio awning|air condition|air conditioning|"
											+ "coffee maschine|coffee brewer")){
				type = "<device_other>" + type;
			//Unknown
			}else{
				type = "";
			}
		}
		return type;
	}
	/**
	 * Search string for a room name.
	 * @param input - string to search
	 * @param language - language code
	 * @return living room etc. ... or empty
	 */
	public static String get_control_location(String input, String language){
		//TODO: distinguish smart location and control info?
		String room = "";
		//German
		if (language.matches("de")){
			room = NluTools.stringFindFirst(input, "wohnzimmer|schlafzimmer|esszimmer|"
					+ "bad|badezimmer|eingang|flur|vorne|hinten|herrenzimmer|arbeitszimmer|arbeitsraum|"
					+ "dachgeschoss|unterm dach|keller|garage|schuppen|garten|kueche|pool|swimmingpool|konferenzraum|konferenzsaal");
			
		//English and other
		}else{
			room = NluTools.stringFindFirst(input, "parlor|parlour|living room|dining room|bedroom|bath|bathroom|entrance|"
					+ "hall|hallway|front|back|corridor|study|under the roof|attic|cellar|garage|garden|kitchen|"
					+ "pool|swimmingpool|conference room|workroom");
		}
		return room;
	}
	
	/**
	 * Search string for banking actions.
	 * @param input - string to search
	 * @param language - language code
	 * @return types like send, show, create, pay
	 */
	public static String get_banking_action(String input, String language){
		String action = "";
		//German
		if (language.matches("de")){
			//SHOW
			if (NluTools.stringContains(input, "zeig|zeigen|anzeigen|check|checken|oeffne|oeffnen|wie viel|wie viele|wieviel|habe ich|kontostand")){
				action = "<show>";
			//SEND
			}else if (NluTools.stringContains(input, "ueberweise|ueberweisen|ueberweisung|banktransfer|bankueberweisung|"
									+ "transferieren|transferiere|transfer|sende|senden|uebertragen|schicken|schicke")){
				action = "<send>";
			//PAY
			}else if (NluTools.stringContains(input, "bezahle(n|)|bezahlung|zahlung|zahle(n|)|qr code|qr-code|abheben|heb(e|) .* ab")){
				action = "<pay>";
			}
		//English and other
		}else{
			//SHOW
			if (NluTools.stringContains(input, "show|check|display|open|how much|how many|do i have|do i still have|balance")){
				action = "<show>";
			//SEND
			}else if (NluTools.stringContains(input, "transfer|send|wire|remit|assign")){
				action = "<send>";
			//PAY
			}else if (NluTools.stringContains(input, "pay|payment|withdraw|qr code|qr-code")){
				action = "<pay>";
			}
		}
		return action;
	}	
	
	/**
	 * Extract radio stations from input.
	 * @param input - user text input or any text to be searched
	 * @param language - ... language code as usual 
	 * @return station name or genre
	 */
	@Deprecated
	public static String get_radio_station(String input, String language){
		//store all parameters
		String search = "";
		String genre = get_music_genre(input, language);
		
		//German
		if (language.matches("de")){
			String radio = NluTools.stringFindFirst(input, "radiokanal|radio kanal|radiostation|radio station|radiosender|radio sender|radiostream|radio stream|"
														+ "musikstation|musik station|musiksender|musik sender|kanal|sender|radio|station");
			String action1 = NluTools.stringFindFirst(input, "einschalten|anmachen|oeffnen|starten|an|ein|hoeren|spielen|aktivieren|aufdrehen");
			String action2 = NluTools.stringFindFirst(input, "oeffne|starte|spiel|spiele|aktiviere");
			//v1
			if (!radio.isEmpty() && !action1.isEmpty()){
				search = NluTools.stringFindFirst(input, radio + "\\s(.*?\\s.*?|.*?)\\s" + action1);
				search = search.replaceFirst(".*?\\b" + radio + "\\s", "").trim();
				search = search.replaceFirst("\\s" + action1 + "\\b.*", "").trim();
			}
			//v2
			if (search.trim().isEmpty() && !radio.isEmpty() && !action1.isEmpty()){
				search = NluTools.stringFindFirst(input, "\\b(.*?\\s.*?|.*?)\\s" + radio + "\\s" + action1);
				search = search.replaceFirst("\\s" + radio + "\\b.*", "").trim();
			}
			//v3
			if (search.trim().isEmpty() && !radio.isEmpty() && !action2.isEmpty()){
				search = NluTools.stringFindFirst(input, action2 + "\\s" + radio + "\\s(.*?\\s\\w+|\\w+)$");
				search = search.replaceFirst(".*?\\s" + radio + "\\s", "").trim();
			}
			//v4
			if (search.trim().isEmpty() && !radio.isEmpty() && !action2.isEmpty()){
				search = NluTools.stringFindFirst(input, action2 + "\\s" + "(.*?)\\s" + radio + "$");
				search = search.replaceFirst(".*?\\b" + action2 + "\\s", "").trim();
				search = search.replaceFirst("\\s" + radio, "").trim();
			}
			//v5
			if (search.trim().isEmpty() && !radio.isEmpty() && action1.isEmpty() && action2.isEmpty()){
				String possibleStation = NluTools.stringRemoveFirst(input, radio);
				if (NluTools.countWords(possibleStation) <= 3){
					search = possibleStation;
				}
			}
			
			//optimize
			if (!search.trim().isEmpty()){
				search = search.replaceFirst(".*?\\b(einen|ein|eine|die|den|das)\\b", "").trim();
			}
			//last check
			if (search.trim().isEmpty() && !genre.isEmpty()){
				search = genre;
			}
			//System.out.println("Final Search: " + search); 		//debug
		
		//English
		}else if (language.matches("en")){
			String radio = NluTools.stringFindFirst(input, "radiochannel|radiostation|radio station|radio channel|radiostream|radio stream|"
														+ "music station|music channel|channel|sender|radio|station");
			String action1 = NluTools.stringFindFirst(input, "on");
			String action2 = NluTools.stringFindFirst(input, "open|start|play|activate|tune in to|turn on|switch on");
			//v1
			if (!radio.isEmpty() && !action1.isEmpty()){
				search = NluTools.stringFindFirst(input, radio + "\\s(.*?\\s.*?|.*?)\\s" + action1);
				search = search.replaceFirst(".*?\\b" + radio + "\\s", "").trim();
				search = search.replaceFirst("\\s" + action1 + "\\b.*", "").trim();
			}
			//v2
			if (search.trim().isEmpty() && !radio.isEmpty() && !action1.isEmpty()){
				search = NluTools.stringFindFirst(input, "\\b(.*?\\s.*?|.*?)\\s" + radio + "\\s" + action1);
				search = search.replaceFirst("\\s" + radio + "\\b.*", "").trim();
			}
			//v3
			if (search.trim().isEmpty() && !radio.isEmpty() && !action2.isEmpty()){
				search = NluTools.stringFindFirst(input, action2 + "\\s" + radio + "\\s(.*?\\s\\w+|\\w+)$");
				search = search.replaceFirst(".*?\\s" + radio + "\\s", "").trim();
			}
			//v4
			if (search.trim().isEmpty() && !radio.isEmpty() && !action2.isEmpty()){
				search = NluTools.stringFindFirst(input, action2 + "\\s" + "(.*?)\\s" + radio + "$");
				search = search.replaceFirst(".*?\\b" + action2 + "\\s", "").trim();
				search = search.replaceFirst("\\s" + radio, "").trim();
			}
			//v5
			if (search.trim().isEmpty() && !radio.isEmpty() && action1.isEmpty() && action2.isEmpty()){
				String possibleStation = NluTools.stringRemoveFirst(input, radio);
				if (NluTools.countWords(possibleStation) <= 3){
					search = possibleStation;
				}
			}
			
			//optimize
			if (!search.trim().isEmpty()){
				search = search.replaceFirst(".*?\\b(a|an|the|to)\\b", "").trim();
			}
			//last check
			if (search.trim().isEmpty() && !genre.isEmpty()){
				search = genre;
			}
			//System.out.println("Final Search: " + search); 		//debug
		
		//no result/missing language support ...
		}else{
			//leave it empty
		}
		
		return search;
	}
	
	/**
	 * Get movie genre.
	 * @param input - input text
	 * @param language - language code
	 * @return genre as string or empty string
	 */
	public static String get_movie_genre(String input, String language){
		String genre = "";
		//German
		if (language.matches("de")){
			genre = NluTools.stringFindFirst(input, "action|action\\w+|aktion|comedy|comedies|komoedie(n|)|dokumentation(en|)|doku(s|)|"
					+ "sport|gameshow(s|)|drama(s|)|thriller(s|)|science fiction|science-fiction|fantasy|fantasy\\w+");
			
		//English and other
		}else{
			genre = NluTools.stringFindFirst(input, "action|action\\w+|comedy|comedies|documentar(y|ies)|documentation(s|)|docu|"
					+ "sport(s|)|gameshow(s|)|drama(s|)|thriller(s|)|science fiction|science-fiction|fantasy|fantasy\\w+");
		}
		return genre;
	}
	/**
	 * Get music genre.
	 * @param input - input text
	 * @param language - language code
	 * @return genre as string or empty string
	 */
	@Deprecated
	public static String get_music_genre(String input, String language){
		String genre = "";
		//German
		if (language.matches("de")){
			genre = NluTools.stringFindFirst(input, "klassik|pop|hard-rock|hardrock|hardcore|rock|metal|"
					+ "disco|acid jazz|jazz|hip-hop|hiphop|hip hop|rnb|r&b|blues|trance|elektro|deep house|"
					+ "house|eurodance|dance|gabba");
			
		//English and other
		}else{
			genre = NluTools.stringFindFirst(input, "classic|pop|hard-rock|hardrock|hardcore|rock|metal|"
					+ "disco|acid jazz|jazz|hip-hop|hiphop|hip hop|rnb|r&b|blues|trance|electro|deep house|"
					+ "house|eurodance|dance|gabba");
		}
		return genre;
	}
	
	/**
	 * Get creator like an artist, e.g. "music by Jimi" - creator: Jimi 
	 * @param input - input text
	 * @param language - language code
	 * @return creator as string or empty string
	 */
	public static String get_creator(String input, String language){
		String creator = "";
		//German
		if (language.matches("de")){
			if (!input.matches(".*\\b(von|vom)\\b.*(nach|bis)\\b.*")){
				creator = NluTools.stringFindFirst(input, "(von|vom) .*");
				creator = creator.replaceAll("^(von|vom)", "");
				creator = creator.replaceAll("^(der|die|das|dem|den|einer|eine|einem)", "");
			}
			
		//English and other
		}else{
			if (!input.matches(".*\\b(from|of|by)\\b.*(to|till|until)\\b.*")){
				creator = NluTools.stringFindFirst(input, "(from|of|by) .*");
				creator = creator.replaceAll("^(from|of|by)", "");
				creator = creator.replaceAll("^(the|a|an)", "");
			}
		}
		return creator.trim();
	}
	
	/**
	 * Get something that can be started or played ... e.g. play song by artist 
	 * @param input - input text
	 * @param language - language code
	 * @return startable as string or empty string
	 */
	public static String get_startable(String input, String language){
		String startable = "";
		//German
		if (language.matches("de")){
			input = input.replaceFirst("\\b(von|vom)\\b.*", "").trim();
			String trigger = NluTools.stringFindFirst(input, "(starte |spiele |spiel |oeffne )");
			if (!trigger.isEmpty()){
				startable = NluTools.stringFindFirst(input, trigger + ".*");
				startable = startable.replaceFirst(".*?" + "\\b" + trigger, "");
			}
			
		//English and other
		}else{
			input = input.replaceFirst("\\b(from|of|by)\\b.*", "").trim();
			String trigger = NluTools.stringFindFirst(input, "(start |play |open )");
			if (!trigger.isEmpty()){
				startable = NluTools.stringFindFirst(input, trigger + ".*");
				startable = startable.replaceFirst(".*?" + "\\b" + trigger, "");
			}
		}
		return startable.trim();
	}
	
	/**
	 * Get currency.
	 * @param input - input text
	 * @param language - language code
	 * @return currency from CURRENCY class or empty string
	 */
	public static String get_currency(String input, String language){
		String currency = "";
		//English
		if (language.matches("en")){
			if (NluTools.stringContains(input, "cent")){
				currency = CURRENCY.DOLLAR_CENT;
			}else if (NluTools.stringContains(input, "euro|euros|(\\d+|)€|€\\d+")){
				currency = CURRENCY.EURO;
			}else if (NluTools.stringContains(input, "pound|pounds|(\\d+|)£|£\\d+")){
				currency = CURRENCY.POUND_GB;
			}else if (NluTools.stringContains(input, "dollar|dollars|(\\d+|)\\$|\\$\\d+")){
				currency = CURRENCY.DOLLAR_US;
			}
		//German and other
		}else{
			if (NluTools.stringContains(input, "cent")){
				currency = CURRENCY.EURO_CENT;
			}else if (NluTools.stringContains(input, "euro|euros|eur|(\\d+|)€|€\\d+")){
				currency = CURRENCY.EURO;
			}else if (NluTools.stringContains(input, "pfund|pfunds|(\\d+|)£|£\\d+")){
				currency = CURRENCY.POUND_GB;
			}else if (NluTools.stringContains(input, "dollar|dollars|(\\d+|)\\$|\\$\\d+")){
				currency = CURRENCY.DOLLAR_US;
			}
		}
		return currency;
	}
	
	/**
	 * Get event-type for things like cinema, concert, superbowl, etc...
	 * @param input - input text
	 * @param language - language code
	 * @return type as tag like &#60;cinema_event&#62;, &#60;culture_event&#62;, &#60;sport_event&#62; or empty string
	 */
	public static String get_event_type(String input, String language){
		String ticket_type = "";
		//German
		if (language.matches("de")){
			if (NluTools.stringContains(input, "kino.*|cinema.*|film.*|movie.*|blockbuster.*|lichtspiel.*")){
				ticket_type = "<movie_event>";
			}else if (NluTools.stringContains(input, "konzert.*|event.*|theater.*|festival.*|oper.*|opern.*|schauspiel.*|auftritt")){
				ticket_type = "<show_event>";
			}else if (NluTools.stringContains(input, "fussball.*|tennis.*|handball.*|hockey.*|basketball.*|baseball.*|football.*|cricket.*|weltmeisterschaft.*|europameisterschaft.*|world series|superbowl")){
				ticket_type = "<sports_event>";
			}
		//English and other
		}else{
			if (NluTools.stringContains(input, "cinema.*|movie.*|blockbuster.*")){
				ticket_type = "<movie_event>";
			}else if (NluTools.stringContains(input, "concert.*|event.*|theater.*|theatre.*|festival.*|opera.*")){
				ticket_type = "<show_event>";
			}else if (NluTools.stringContains(input, "soccer.*|tennis.*|handball.*|hockey.*|basketball.*|baseball.*|football.*|cricket.*|championchip.*|world series|superbowl")){
				ticket_type = "<sports_event>";
			}
		}
		return ticket_type;
	}
	
	/**
	 * Get first color in String.
	 * @param input - input text
	 * @param language - language code
	 * @return first color as color tag, e.g. &#60;blue&#62;
	 */
	public static String get_color(String input, String language){
		//German
		if (language.matches("de")){
			String color = NluTools.stringFindFirst(input, 
					"(blau|rot|gelb|gruen|orange|schwarz|weiss|lila|violett|magenta|cyan|silber|gold|rosa|pink|braun)(\\w+|)\\b"
			);
			String end = "(\\w+|)";  //(en|es|e|er|)
			if (NluTools.stringContains(color, "blau" + end))		return "<blue> " + color;
			if (NluTools.stringContains(color, "rot" + end))		return "<red> " + color;
			if (NluTools.stringContains(color, "gelb" + end))		return "<yellow> " + color;
			if (NluTools.stringContains(color, "gruen" + end))		return "<green> " + color;
			if (NluTools.stringContains(color, "orange" + end))	return "<orange> " + color;
			if (NluTools.stringContains(color, "schwarz" + end))	return "<black> " + color;
			if (NluTools.stringContains(color, "weiss" + end))		return "<white> " + color;
			if (NluTools.stringContains(color, "lila" + end))		return "<purple> " + color;
			if (NluTools.stringContains(color, "violett" + end))	return "<violet> " + color;
			if (NluTools.stringContains(color, "magenta" + end))	return "<magenta> " + color;
			if (NluTools.stringContains(color, "cyan" + end))		return "<cyan> " + color;
			if (NluTools.stringContains(color, "silber" + end))	return "<silver> " + color;
			if (NluTools.stringContains(color, "gold" + end))		return "<gold> " + color;
			if (NluTools.stringContains(color, "rosa" + end))		return "<pink> " + color;
			if (NluTools.stringContains(color, "pink" + end))		return "<pink> " + color;
			if (NluTools.stringContains(color, "braun" + end))		return "<brown> " + color;
			else return "";

		//English and other
		}else{
			String color = NluTools.stringFindFirst(input, 
					"(blue|red|yellow|green|orange|black|white|purple|violet|magenta|cyan|silver|gold|rose|pink|brown)(\\w+|)\\b"
			);
			String end = "(\\w+|)";  //(s|)
			if (NluTools.stringContains(color, "blue" + end))		return "<blue> " + color;
			if (NluTools.stringContains(color, "red" + end))		return "<red> " + color;
			if (NluTools.stringContains(color, "yellow" + end))		return "<yellow> " + color;
			if (NluTools.stringContains(color, "green" + end))		return "<green> " + color;
			if (NluTools.stringContains(color, "orange" + end))	return "<orange> " + color;
			if (NluTools.stringContains(color, "black" + end))	return "<black> " + color;
			if (NluTools.stringContains(color, "white" + end))		return "<white> " + color;
			if (NluTools.stringContains(color, "purple" + end))		return "<purple> " + color;
			if (NluTools.stringContains(color, "violet" + end))	return "<violet> " + color;
			if (NluTools.stringContains(color, "magenta" + end))	return "<magenta> " + color;
			if (NluTools.stringContains(color, "cyan" + end))		return "<cyan> " + color;
			if (NluTools.stringContains(color, "silver" + end))	return "<silver> " + color;
			if (NluTools.stringContains(color, "gold" + end))		return "<gold> " + color;
			if (NluTools.stringContains(color, "rose" + end))		return "<pink> " + color;
			if (NluTools.stringContains(color, "pink" + end))		return "<pink> " + color;
			if (NluTools.stringContains(color, "brown" + end))		return "<brown> " + color;
			else return "";
		}
	}
	
	/**
	 * Replace personal location tags like "here", "my place", "my work" ... with standard tags like [user_location] or
	 * data from the user database.
	 *  
	 * @param input - input string
	 * @param language - ... is obvious or?
	 * @return tagged/replaced string
	 */
	public static String replace_personal_locations(String input, String language){
		//German
		if (language.matches("de")){
			input = input.replaceAll("\\b(nahe zu |nah zu |der naehe (von |)|)(mein |meinem |)(heim|zuhause|zu hause|hause|wo ich wohne)\\b", "<user_home>");
			input = input.replaceAll("\\b(nahe zu |nah zu |der naehe (von |)|)(meiner |meine |)(arbeit|wo ich arbeite|nahe meiner arbeit)\\b", "<user_work>");
			input = input.replaceAll("\\b(wo ich bin|(meiner|der) naehe|(nahe zu |nah zu |)(hier|diesem ort|meinem standort|standort))\\b", "<user_location>");
		
		//English
		}else if (language.matches("en")){
			input = input.replaceAll("\\b(close to |near to |)(my |)(my place|home|where i live)\\b", "<user_home>");
			input = input.replaceAll("\\b(close to |near to |)(my work|work|where i work)\\b", "<user_work>");
			input = input.replaceAll("\\b((close to (me |)|near to (me |)|)(here|this place|my location|my position|where i am))\\b", "<user_location>");
		
		//no result/missing language support ...
		}else{
			//leave it
		}
		
		return input;
	}
	
	/**
	 * Return a list of POIs (places of interest) in a certain language to use for parameter search methods.
	 * @param language - language code
	 * @return String ready to use for search method, e.g. "bar|restaurant|..."
	 */
	public static String get_POI_list(String language){
		String pois = "";
		if (language.matches("de")){
			pois = "bar|bars|pub|pubs|cafe|cafes|kaffee|coffeeshop|coffee-shop|baeckerei|baecker|"
					+ "krankenhaus|krankenhaeuser|apotheke|"
					+ "(pizza |doener |pommes |schnitzel )(laden|laeden|bude|buden)|" + "(\\b\\w{3,}(sches |sche )|)(restaurant|restaurants)|"
					+ "tankstelle|tankstellen|kiosk|disco|discos|club|clubs|"
					+ "polizei|hotel|hotels|sehenswuerdigkeit|sehenswuerdigkeiten|burg|burgen|"
					+ "aldi|lidl|penny|edeka|netto|rewe|supermarkt|supermaerkte";
		}else{
			pois = "bar|bars|pub|pubs|cafe|cafes|coffee|coffeeshop|coffee-shop|coffeehouse|bakery|bakers|bakehouse|"
					+ "hospital|hospitals|pharmacy|drugstore|"
					+ "(pizza |doener |pommes |schnitzel )(restaurant|restaurants|place|places)|" + "(\\b\\w{3,}an |\\b\\w{3,}nese |)(restaurant|restaurants)|"
					+ "(gas|petrol) station|(gas|petrol) stations|disco|discos|club|clubs|food|"
					+ "police|hotel|hotels|places of interest|landmark|landmarks|castle|castles|"
					+ "aldi|lidl|penny|edeka|netto|rewe|supermarket|shop|shops|store|stores";
		}
		return pois;
	}
	/**
	 * Try to guess a POI from sentence if you didn't find it with the list. Note that this cannot be used to replace POI afterwards 
	 * as it might change the words (e.g. eat to restaurant).
	 * @param input - a sentence
	 * @param language - language code
	 * @return String or empty
	 */
	public static String get_POI_guess(String input, String language){
		String poi = "";
		if (language.matches("de")){
			input = input.replaceFirst("\\b(etwas |was |irgendwas |)(zu |)essen\\b", "restaurant");
					
			if (input.matches(".*\\b(gibts |gibt es |kriege ich |kann man ).*\\b(in der naehe|hier|in)\\b.*")){
				//poi = input.replaceFirst(".*\\b(gibts |gibt es |kriege ich )(hier |)(irgendwo |)(.*?)\\b((hier |)(in der naehe|)( hier|))\\b.*", "$4").trim();
				poi = input.replaceFirst(".*\\b(gibts |gibt es |kriege ich |kann man )", "").trim();
				poi = poi.replaceFirst("^((hier |)(irgendwo |)(irgendwo |)(hier |))", "").trim();
				poi = poi.replaceFirst("^(der|die|das|ein|einen|eine)\\b", "");
				poi = poi.replaceFirst("\\b((hier |)(irgendwo |)(in der naehe|hier|))$", "").trim();
				poi = poi.replaceFirst("\\s(in)\\s.*", "").trim();
			}
		}else{
			input = input.replaceFirst("\\b(some |something |anything |)(to |)(eat|food)( something|)\\b", "restaurant");
			
			if (input.matches(".*\\b(is there |are there |can i get |where can )(.*)\\b(close|around|near|here|in)\\b.*")){
				poi = input.replaceFirst(".*\\b(is there |are there |can (i|we) get |where can (i|we))\\b", "").trim();
				poi = poi.replaceFirst("\\b((close|around|near|)( here|))$", "").trim();
				poi = poi.replaceFirst("\\s(in)\\s.*", "").trim();
				poi = poi.replaceFirst("^(a|any|the)", "");
			}
		}
		return poi.trim();
	}
	

}
