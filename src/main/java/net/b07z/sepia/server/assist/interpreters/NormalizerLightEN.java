package net.b07z.sepia.server.assist.interpreters;

import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.server.Config;

/**
 * Normalizer for English that does a rather careful job not being to extreme ;-)
 * Suitable for the keyword analyzer. 
 * 
 * @author Florian Quirin
 *
 */
public class NormalizerLightEN implements Normalizer {

	public String normalizeText(String text) {
		
		String assiName = Config.assistantName.toLowerCase();
		String assiRegEx = "(" + Pattern.quote(assiName) + "|<assistant_name>" + ")";
		
		text = text.replaceAll("(!(?!\\()|\\?(?!\\()|(?<![oO])'|,(?!\\d))", "").toLowerCase().trim();
		text = text.replaceAll("((?<!\\d)\\.$)", "").trim();
		text = text.replaceFirst("^" + assiRegEx + " ","").trim();
		text = text.replaceFirst("(?<!(on the name|is|for))\\s+" + assiRegEx + "$","").trim();
		text = text.replaceAll("^(hello |hi |hey |good day |good morning )","").trim();
		text = text.replaceAll("^(i said )","").trim();
		text = text.replaceAll("\\b(^can you please |^please |please$)\\b", "").trim();
		//special characters
		//TODO: use it or not?
		text = text.replaceAll("ß","ss").replaceAll("ä","ae").replaceAll("ü","ue").replaceAll("ö","oe");
		text = text.replaceAll("é","e").replaceAll("è","e").replaceAll("ê","e");
		text = text.replaceAll("á","a").replaceAll("í","i").replaceAll("ó","o").replaceAll("ñ","n").replaceAll("ú","u");
		//clean
		text = text.replaceAll("\\s+", " ").trim();
		
		return text;
	}

	@Override
	public String reconstructPhrase(String rawText, String phrase) {
		return NormalizerLight.recover(rawText, phrase);
	}
}
