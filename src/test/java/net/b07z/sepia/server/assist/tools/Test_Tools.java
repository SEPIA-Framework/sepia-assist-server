package net.b07z.sepia.server.assist.tools;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.assistant.CmdBuilder;
import net.b07z.sepia.server.assist.data.SentenceMatch;
import net.b07z.sepia.server.assist.data.Word;
import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;
import net.b07z.sepia.server.assist.interpreters.NormalizerLight;
import net.b07z.sepia.server.assist.parameters.Place;
import net.b07z.sepia.server.assist.users.ACCOUNT;
import net.b07z.sepia.server.core.tools.Converters;
import net.b07z.sepia.server.core.tools.RandomGen;

public class Test_Tools {

	public static void main(String[] args) throws Exception {
		
		String test = "";
		String test2 = "";
		
		//test anything
		
		//Random generator
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int i=0; i<40; i++){
			int rnd = RandomGen.getInt(5, 10);
			if (rnd < min) min = rnd;
			if (rnd > max) max = rnd;
			System.out.print(rnd + ", ");
		}
		System.out.println("\nRandomGen min: " + min + ", max: " + max);
		
		//test CMD summary creation methods for buttons
		System.out.println("---------------------");
		System.out.println(CmdBuilder.getWayHomeInfo("driving", "de"));
		System.out.println(CmdBuilder.getWayToWorkInfo("", "de"));
		System.out.println(CmdBuilder.getSoccerTable("bundesliga"));
		
		System.out.println("---------------------");
		test = "#THROAT01# kann ich kurz stören";
		test = test.replaceAll("#\\w+\\d\\d#", "").trim();
		System.out.println("test: " + test);

		//test POI extraction
		System.out.println("----------de----------");
		test = "suche restaurants in Berlin";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche restaurants in der naehe von Berlin";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche eine doener bude in Essen";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche krankenhaeuser in der naehe";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche hotels hier";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche hotels die in Berlin sind";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche hotels nahe zu Berlin Dahlem";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche hotels auf Sylt-Downtown, Deutschland";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche hauptbahnhof Berlin";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		test = "suche elisabeth krankenhaus Essen";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "de").toJSONString());
		System.out.println("----------en----------");
		test = "search restaurants in Berlin";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "en").toJSONString());
		test = "search restaurants in vicinity of Berlin";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "en").toJSONString());
		test = "search hospitals close to here";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "en").toJSONString());
		test = "search hotels here";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "en").toJSONString());
		test = "search hotels near to Berlin Dahlem";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "en").toJSONString());
		test = "search hotels that are near to Berlin Dahlem";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "en").toJSONString());
		test = "search hotels on Sylt-Downtown, Deutschland";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "en").toJSONString());
		test = "search main station berlin";
		System.out.println("test: " + test);
		System.out.println(Place.getPOI(test, "en").toJSONString());
		System.out.println("-------------------------");
		
		//greater than regex bug
		test = "bring mich nach <X>";
		String c = NluTools.stringFindFirst(test, "bring mich nach ");
		String remove = test.replaceFirst(Pattern.quote(c), "");
		System.out.println("regex test: " + c);
		System.out.println("remove test: " + remove);
		System.out.println("-------------------------");
		
		//minus regex bug
		//String listRegEx = "(\\w+ |)(to do |\\w+(-| |)(\\w+-|))liste";
		String listRegEx = "(\\w+ |)((to do )|(\\w+-\\w+(-| ))|(\\w+(-| )))liste";
		test = "lesen auf meine buecher to-do liste";	System.out.println("regex test: " + test);		System.out.println("find first: " + NluTools.stringFindFirst(test, listRegEx));
		test = "lesen auf meine buecher to do liste";	System.out.println("regex test: " + test);		System.out.println("find first: " + NluTools.stringFindFirst(test, listRegEx));
		test = "lesen auf meine buecher to-do-liste";	System.out.println("regex test: " + test);		System.out.println("find first: " + NluTools.stringFindFirst(test, listRegEx));
		test = "lesen auf meine to-do-liste";			System.out.println("regex test: " + test);		System.out.println("find first: " + NluTools.stringFindFirst(test, listRegEx));
		test = "lesen auf meine to-do liste";			System.out.println("regex test: " + test);		System.out.println("find first: " + NluTools.stringFindFirst(test, listRegEx));
		test = "lesen auf meine to do liste";			System.out.println("regex test: " + test);		System.out.println("find first: " + NluTools.stringFindFirst(test, listRegEx));
		test = "bücher auf meine shopping liste";		System.out.println("regex test: " + test);		System.out.println("find first: " + NluTools.stringFindFirst(test, listRegEx));
		test = "bücher auf meine shopping-liste";		System.out.println("regex test: " + test);		System.out.println("find first: " + NluTools.stringFindFirst(test, listRegEx));
		System.out.println("-------------------------");
		
		//containsOneOf
		test = "bring mich nach <X>";
		boolean is = NluTools.stringContainsOneOf(test, "brug", "moch", "X", "hase");
		System.out.println("test: " + test);
		System.out.println("coontains? " + is);
		System.out.println("-------------------------");
		
		//SentenceMatch
		test = "bring mich nach hause";
		test2 = "erinnere mich daran <time> <place> <info>";
		System.out.println("test1: " + test);
		System.out.println("test2: " + test2);
		System.out.println("tag: " + Word.getTag(test2));
		System.out.println("removed tag: " + NluTools.stringRemoveFirst(test2, Pattern.quote("<" + Word.getTag(test2) + ">")));
		SentenceMatch sentenceMatch = new SentenceMatch(test, test2);
		sentenceMatch.getIdentity()
					.getBagOfWordsMatch()
					.getWordDistance()
					.getEditDistance();
		System.out.println("identical? " + sentenceMatch.isIdentical);
		System.out.println("words: " + sentenceMatch.numberOfWords);
		System.out.println("matched words: " + sentenceMatch.matchedWords_N);
		System.out.println("different words: " + sentenceMatch.differentWords_N);
		System.out.println("tag words: " + sentenceMatch.taggedWords_N);
		System.out.println("matched words (%): " + sentenceMatch.matchedWords_P);
		System.out.println("edit distance: " + sentenceMatch.editDistance);
		System.out.println("word distance: " + sentenceMatch.wordDistance);
		System.out.println("-----------------------");
		test = "nymphenburger essen deutschland";
		test2 = "nymphenburger straße essen deutschland";
		System.out.println("test1: " + test);
		System.out.println("test2: " + test2);
		sentenceMatch = new SentenceMatch(test, test2);
		sentenceMatch.getIdentity()
					.getEditDistance();
		System.out.println("identical? " + sentenceMatch.isIdentical);
		System.out.println("edit distance: " + sentenceMatch.editDistance);
		System.out.println("percent: " + (double)(test.length()-sentenceMatch.editDistance)/test.length());
		System.out.println("------------------------------------------------");
		
		//smartRound
		System.out.println("smartRound: 200000.001 = " + Converters.smartRound("200000.001", false));
		System.out.println("smartRound: 5123.7491 = " + Converters.smartRound("5123.7491", false));
		System.out.println("smartRound: 0.7491 = " + Converters.smartRound("0.7491", false));
		System.out.println("smartRound: 3.0491 = " + Converters.smartRound("3.0491", false));
		System.out.println("smartRound: 0.0711247000 = " + Converters.smartRound("0.0711247000", false));
		System.out.println("smartRound: 50.34 = " + Converters.smartRound("50.34", false));
		System.out.println("smartRound: 100.5 = " + Converters.smartRound("100.5", false));
		System.out.println("------------------------------------------------");
		
		//search regex
		System.out.println("regex test: " + NluTools.stringFindFirst("search motorcycles from 2012", "(\\d+|)" 
				+ "(bitcoin|bitcoins|btc|euro|euros|eur|€|eurocent(s|)|cent(s|)|ct|"
				+ "(us |american |)dollar(s|)|usd|\\$|gb pound(s|)|british pound(s|)|gbp|£|"
				+ "yen|jpy|¥|yuan|renminbi|cny|cn¥|rmb|russian (rubel|rouble)(s|)|rubel(s|)|rouble(s|)|rub|dirham|aed)"));
		System.out.println("------------------------------------------------");
		
		//normalizer and URL encoder
		test = "Bana New York'tan Chicago'ya gelen yol göstermek";
		NormalizerLight norm = new NormalizerLight();
		System.out.println(norm.normalizeText(test));
		System.out.println(URLEncoder.encode(test, "UTF-8"));
		System.out.println("------------------------------------------------");
		
		//month replacing
		test = "show me anything from the third of january till october the 31th";
		test = RegexParameterSearch.replace_all_months_by_numbers(test, "en");
		System.out.println(test);
		test = "show me anything from the 02. of july till october the 01.";
		test = RegexParameterSearch.replace_all_months_by_numbers(test, "en");
		System.out.println(test);
		test = "show me anything from the 5th of july till october the first";
		test = RegexParameterSearch.replace_all_months_by_numbers(test, "en");
		System.out.println(test);
		test = "zeig mir alles vom zweiten januar bis 12 oktober";
		test = RegexParameterSearch.replace_all_months_by_numbers(test, "de");
		System.out.println(test);
		test = "zeig mir alles vom ersten oktober bis 2. maerz";
		test = RegexParameterSearch.replace_all_months_by_numbers(test, "de");
		System.out.println(test);
		System.out.println("------------------------------------------------");
		
		//number extraction
		test = "1LIVE starten bitte oder Kanal 106.7 suchen";
		System.out.println(RegexParameterSearch.get_number(test));
		System.out.println(Converters.obj2IntOrDefault(RegexParameterSearch.get_number(test), -1));
		test = "-305.14";
		System.out.println(Converters.obj2IntOrDefault(RegexParameterSearch.get_number(test), -1));
		test = "1,75€ ist groesser als 1.57$";
		System.out.println(RegexParameterSearch.get_number(test));
		test = "you can have it for $75";
		System.out.println(RegexParameterSearch.get_number(test));
		test = "hausnummer 3b";
		System.out.println(RegexParameterSearch.get_number(test));
		test = "-305.00 leer";
		System.out.println(RegexParameterSearch.get_number(test));
		System.out.println("------------------------------------------------");
		
		//get creator
		test = "zeig mir alle songs von Jimi Hendrix";
		System.out.println(RegexParameterSearch.get_creator(test, "de"));
		test = "show me all songs of Jimi Hendrix";
		System.out.println(RegexParameterSearch.get_creator(test, "en"));
		System.out.println("------------------------------------------------");
		
		//get radio
		test = "play a prince radio station";
		System.out.println(RegexParameterSearch.get_radio_station(test, "en"));
		System.out.println("------------------------------------------------");
		
		test = "ios_app_v1.0_CF176ADC-C8ED-4DFF-BD42-E9428406C37F";
		String token_path = ACCOUNT.TOKENS + "." + test.replaceFirst("_v\\d.*?(_|$)", "_").trim().replaceFirst("_$", "").replaceAll("[\\W]", "").trim();
		System.out.println(token_path);
		System.out.println("------------------------------------------------");
		
		//pwd check
		/*
		System.out.println("PWD: " + Authentication_DynamoDB.hashPassword_client("JX5wuzi3"));
		
		//test string contains
		test = "this is my <user_home> ok";
		System.out.println("String contains it? " + NLU_Tools.stringContains(test, "<user_home>"));
		System.out.println("String match: " + NLU_Tools.stringFindFirst(test, "<user_home>"));
		//test = "hello, can you please add this to my shopping list if you have time";
		test = "add list";
		System.out.println("Found something? " + NLU_Tools.stringContains(test, "(add|open|show) .*\\b(list)"));
		System.out.println("Found: " + NLU_Tools.stringFindFirst(test, "(add|open|show) .*\\b(list)"));
		
		//test regex
		System.out.println("--REGEX TESTING---");
		//test = "bring mich von zu Hause zu meiner persoenlichen lieblings insel bitte";
		test = "wie ist das wetter auf meiner lieblingsinsel";
		System.out.println("item: " + NLU_parameter_search.search_my_info_item(test, "de")[0]);
		System.out.println("item: " + NLU_parameter_search.search_my_info_item(test, "de")[1]);
		test = "take me from home to my favorite island please";
		System.out.println("item: " + NLU_parameter_search.search_my_info_item(test, "en")[0]);
		System.out.println("item: " + NLU_parameter_search.search_my_info_item(test, "en")[1]);
		*/
		
		//yes_no?
		test = "nein leider nicht";
		System.out.println("yes or no? - " + RegexParameterSearch.yes_or_no(test, "de"));
		
		//SHA256 test
		//expected: 0b6257e30180d96eadd8372e10c88940d19eb62f4d565324c423a4eb46b9fafa
		//System.out.println("SHA256: " + Security.bytearrayToHexString(Security.getSha256("München")));
		//System.out.println("München (" + ("München").getBytes("UTF-8").length + ")" );
		
		//test converters
		/*
		System.out.println("----TEST CONVERTERS----");
		System.out.println("Long str: " + Converters.obj_2_long("27097856"));
		System.out.println("Long double: " + Converters.obj_2_long(new Double(2.709e7)));
		System.out.println("Long double: " + Converters.obj_2_long(new Double(-1000)));
		System.out.println("Int str: " + Converters.obj_2_long("2709.6"));
		System.out.println("Int double: " + Converters.obj_2_long(new Double(2.709e3)));
		System.out.println("Int double: " + Converters.obj_2_long(new Double(-2000.6)));
		*/
		//test control search
		/*
		String text =  "kannst du bitte die Heizung im Wohnzimmer auf 21.5 grad stellen?";
		HashMap<String, String> controls = new HashMap<String, String>();
		controls.put("control_type", NLU_parameter_search.get_control_type(text.toLowerCase(), "de"));
		controls.put("control_action", NLU_parameter_search.get_control_action(text.toLowerCase(), "de"));
		controls.put("control_info", NLU_parameter_search.get_control_location(text.toLowerCase(), "de"));
		controls.put("control_number", NLU_parameter_search.get_number(text.toLowerCase()));
		System.out.println(controls);
		*/
		
		//test flat eric
		/*
		JSONObject top = new JSONObject();
		JSONObject sub1a = new JSONObject();
		JSONObject sub1b = new JSONObject();
		JSONObject sub1c = new JSONObject();
		JSONObject sub2a = new JSONObject();
		JSONObject sub2b = new JSONObject();
		JSONObject sub2c = new JSONObject();
		JSON.add(sub2a, "sub2a1", "down");
		JSON.add(sub2a, "sub2a2", "to the");
		JSON.add(sub2a, "sub2a3", "bottom");
		JSON.add(sub2b, "sub2b1", new Integer(212));
		JSON.add(sub2b, "sub2b2", new Double(3.141));
		JSON.add(sub2c, "sub2c", "nice here");
		JSON.add(sub1a, "sub1a1", sub2a);
		JSON.add(sub1a, "sub1a2", "you have");
		JSON.add(sub1a, "sub1a3", "reached");
		JSON.add(sub1b, "sub1b", sub2b);
		JSON.add(sub1c, "sub1c", sub2c);
		JSON.add(top, "top1", sub1a);
		JSON.add(top, "top2", sub1b);
		JSON.add(top, "top3", sub1c);
		JSON.add(top, "top4", "finally");
		System.out.println("Deep: " + top.toJSONString());
		System.out.println("Flat: " + JSON.makeFlat(top, "", null));
		*/

		/*
		//try to break security chain:
		Authentication_Token tokenGen = new Authentication_Token();
				
		final class MyToken extends Authentication_Token{
			private String tokenHash = "";				//hashed token, generated from ???
			private long timeCreated = 0;				//System time on creation
			private boolean authenticated = true;		//is the user authenticated?
			private String userID = "test@example.com";	//user ID received from authenticator
			private int accessLvl = 10;					//user access level received from authenticator
			private HashMap<String, Object> basicInfo;		//basic info of the user acquired during authentication
			public MyToken(){
				tokenHash = "";
				timeCreated = System.currentTimeMillis();
			}
			public String signatureResponse(String test){
				//return tokenGen.signatureResponse(test);
				return "";
			}
			public boolean authenticated(){
				return authenticated;
			}
			public String getUserID(){
				return userID;
			}
			public int getAccessLevel(){
				return accessLvl;
			}
			public HashMap<String, Object> getBasicInfo(){
				return basicInfo;
			}
			public String getTokenHash(){
				return tokenHash;
			}
		}
		
		//fake input
		NLU_Input input = new NLU_Input("test", "en", "default", 5, "default");
		input.user_location = "<country>Germany<city>Essen<street>Somestreet 1<code>45138<latitude>51.6<longitude>7.1";
		input.user = new User(input, new MyToken());
		System.out.println(input.user.getUserID());
		System.out.println(input.user.loadInfoFromAccount(Test_Accounts.test_api_mng, "midnight"));
		System.out.println("midnight: " + input.user.info.get("midnight"));
		*/
		
		System.out.println("------------------------------------------------");
		String TAG_REGEX = "\\bsepia-[-\\w]+";
		String testTextForRegExp = "test1 test2 sepia-name sepia-type sepia-set-cmd sepia-state-type sepia-room test3 test4";
		System.out.println(testTextForRegExp);
		System.out.println(testTextForRegExp.replaceAll(TAG_REGEX, "").replaceAll("\\s+", " ").trim());
		
		System.out.println("\n------------------------------------------------");
		System.out.println(DateTimeConverters.getToday("dd.MM.yyyy", "2020.01.04_15:30:00"));
		System.out.println(DateTimeConverters.getToday("dd.MM.yyyy", "2020.01.04_03:30:00"));
		System.out.println(DateTimeConverters.getToday("MM/dd/yyyy", "2020.01.04_15:30:00"));
		System.out.println(DateTimeConverters.getToday("MM/dd/yyyy", "2020.01.04_03:30:00"));
		System.out.println(DateTimeConverters.getToday("HH:mm", "2020.01.04_15:30:00"));
		System.out.println(DateTimeConverters.getToday("HH:mm", "2020.01.04_03:30:00"));
		System.out.println(DateTimeConverters.getToday("H:mm", "2020.01.04_15:30:00"));
		System.out.println(DateTimeConverters.getToday("H:mm", "2020.01.04_03:30:00"));
	}
}
