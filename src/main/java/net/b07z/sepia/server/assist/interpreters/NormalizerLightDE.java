package net.b07z.sepia.server.assist.interpreters;

import java.util.regex.Pattern;

import net.b07z.sepia.server.assist.server.Config;

/**
 * Normalizer for German that does a rather careful job not being to extreme ;-)
 * Suitable for the keyword analyzer. 
 * 
 * @author Florian Quirin
 *
 */
public class NormalizerLightDE implements Normalizer {

	public String normalizeText(String text) {
		
		String assiName = Config.assistantName.toLowerCase();
		String assiRegEx = "(" + Pattern.quote(assiName) + "|<assistant_name>" + ")";
		
		text = text.replaceAll("(!|\\?|(?<![oO])'|,(?!\\d))", "").toLowerCase().trim();
		text = text.replaceAll("((?<!\\d)\\.$)", "").trim();
		text = text.replaceFirst("^" + assiRegEx + " ","").trim();
		text = text.replaceFirst(" " + assiRegEx + "$","").trim();
		text = text.replaceAll("^(hallo |hi |hey |guten tag |guten morgen )","").trim();
		text = text.replaceAll("^(ich habe gesagt |ich hab gesagt |ich sagte )","").trim();
		text = text.replaceAll("\\b(^kannst du bitte |^bitte |bitte$)\\b", "").trim();
		//special characters
		//TODO: use it or not?
		text = text.replaceAll("ß","ss").replaceAll("ä","ae").replaceAll("ü","ue").replaceAll("ö","oe");
		text = text.replaceAll("é","e").replaceAll("è","e").replaceAll("ê","e");
		text = text.replaceAll("á","a").replaceAll("í","i").replaceAll("ó","o").replaceAll("ñ","n").replaceAll("ú","u");
		//Umgangssprache
		text = text.replaceAll("\\b(such)\\b","suche");
		text = text.replaceAll("\\b(krieg)\\b","kriege");
		text = text.replaceAll("\\b(find)\\b","finde");
		text = text.replaceAll("\\b(brauch)\\b","brauche");
		text = text.replaceAll("\\b(hab)\\b","habe");
		text = text.replaceAll("\\b(gibts)\\b","gibt es");
		text = text.replaceAll("\\b(gehts)\\b","geht es");
		text = text.replaceAll("\\b(zeige)\\b","zeig");
		text = text.replaceAll("\\b(bringe)\\b","bring");
		text = text.replaceAll("\\b(loesch)\\b","loesche");
		text = text.replaceAll("\\b(erinner)\\b","erinnere");
		text = text.replaceAll("\\b(vergesse)\\b","vergiss");
		text = text.replaceAll("\\b(setz)\\b","setze");
		text = text.replaceAll("\\b(mache)\\b","mach");
		text = text.replaceAll("\\b(drehe)\\b","dreh");
		text = text.replaceAll("\\b(uebersetz)\\b","uebersetze");
		text = text.replaceAll("\\b(gruess)\\b","gruesse");
		text = text.replaceAll("\\b(gern)\\b","gerne");
		//clean up
		text = text.replaceAll("\\s+", " ").trim();
		
		return text;
	}

	@Override
	public String reconstructPhrase(String rawText, String phrase) {
		return NormalizerLight.recover(rawText, phrase);
	}
}
