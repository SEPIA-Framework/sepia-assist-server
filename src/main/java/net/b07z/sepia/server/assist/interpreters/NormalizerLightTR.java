package net.b07z.sepia.server.assist.interpreters;

/**
 * General normalizer for different languages that does a rather careful job not being to extreme ;-)
 * Suitable for the keyword analyzer. Changes here should be made in all clients too! 
 * 
 * @author Florian Quirin
 *
 */
public class NormalizerLightTR implements Normalizer {
	
	public String normalizeText(String text) {
				
		//special characters - that fail to convert to small case correctly
		text = text.replaceAll("İ", "i");
		text = text.replaceAll("I", "ı");
				
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
		return NormalizerLight.recover(rawText, phrase);
	}
}
