package net.b07z.sepia.server.assist.interpreters;

/**
 * Normalizer for English to add after default normalizer for a more aggressive finish. 
 * 
 * @author Florian Quirin
 *
 */
public class NormalizerAddStrongEN implements Normalizer {

	public String normalizeText(String text) {
		
		/*
		String assiName = Config.assistant_name.toLowerCase();
		String assiRegEx = "(" + Pattern.quote(assiName) + "|<assistant_name>" + ")";
		text = text.replaceFirst("^" + assiRegEx + " ","").trim();
		text = text.replaceFirst(" " + assiRegEx + "$","").trim();
		*/
		
		text = text.replaceAll("^(i |id |we |wed |)(would |)(like|love)( to|)\\b", "").trim();
		text = text.replaceAll("\\b(maybe|please)\\b", "").trim();
		
		//clean
		text = text.replaceAll("\\s+", " ").trim();
		
		return text;
	}

	@Override
	public String reconstructPhrase(String rawText, String phrase) {
		return NormalizerLight.recover(rawText, phrase);
	}
}
