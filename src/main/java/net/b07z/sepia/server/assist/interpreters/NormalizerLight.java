package net.b07z.sepia.server.assist.interpreters;

/**
 * General normalizer for different languages that does a rather careful job not being to extreme ;-)
 * Suitable for the keyword analyzer. Changes here should be made in all clients too! 
 * 
 * @author Florian Quirin
 *
 */
public class NormalizerLight implements Normalizer {
	
	//NOTE: DO NOT MODIFY THIS WITH ANYTHING ELSE BESIDES SINGLE CHARACTER NORMALIZATION (e.g. don't change words)  

	@Override
	public String normalizeText(String text) {
				
		//special characters - that fail to convert to small case correctly
		text = text.replaceAll("İ", "i");
		//text = text.replaceAll("I", "ı"); 	//not good for general languages
				
		text = text.replaceAll("(!(?!\\()|¿|¡|\\?(?!\\()|,(?!\\d))", "").toLowerCase().trim();
		//text = text.replaceAll("(?<![oO])'", "").trim();		//TODO: use it or not?
		text = text.replaceAll("((?<!\\d)\\.$)", "").trim();
		
		//special characters
		//TODO: use it or not?
		text = text.replaceAll("ß","ss").replaceAll("ä","ae").replaceAll("ü","ue").replaceAll("ö","oe");
		text = text.replaceAll("é","e").replaceAll("è","e").replaceAll("ê","e");
		text = text.replaceAll("á","a").replaceAll("í","i").replaceAll("ó","o").replaceAll("ñ","n").replaceAll("ú","u");
		
		//clean up
		text = text.replaceAll("\\s+", " ").trim();
		
		return text;
	}
	
	@Override
	public String reconstructPhrase(String rawText, String phrase) {
		return recover(rawText, phrase);
	}
	
	/**
	 * Use this to do the "reconstructPhrase" for similar normalizers.
	 */
	public static String recover(String rawText, String phrase){
		String escapeCharsRegexp = "(\\\\|\\.|\\[|\\]|\\{|\\}|\\(|\\)|\\*|\\+|\\-|\\?|\\^|\\$|\\|)";
		String phraseOrg = phrase;
		//TODO: if the words start with the "special" replacements it does not work
		phrase = phrase.replaceAll(escapeCharsRegexp, "\\\\$1")//.replaceAll("\"", "\\\\\"")
				.replaceAll("ss","(ss|ß)")
				.replaceAll("ae","(ae|Ä|ä)").replaceAll("ue","(ue|Ü|ü)").replaceAll("oe","(oe|Ö|ö)")	
				.replaceAll("e","(e|é|è|ê)")
				.replaceAll("a","(a|á)").replaceAll("i","(i|í)").replaceAll("o","(o|ó)").replaceAll("n","(n|ñ)").replaceAll("u","(u|ú)");
		if (rawText.contains(",")){
			phrase = phrase.replaceAll(" ", "( |, )");
		}
		//System.out.println(phrase); 		//debug
		if (rawText.matches("(?i).*?(^|\\b|\\s)(" + phrase + ")($|\\b|\\s).*")){
			return rawText.replaceFirst("(?i).*?(^|\\b|\\s)(" + phrase + ")($|\\b|\\s).*", "$2");
		}else{
			return phraseOrg;
		}
	}

}
