package net.b07z.sepia.server.assist.interpreters;

/**
 * Normalizer for German to add after default normalizer for a more aggressive finish.
 * 
 * @author Florian Quirin
 *
 */
public class NormalizerAddStrongDE implements Normalizer {

	public String normalize_text(String text) {
		/*
		String assiName = Config.assistant_name.toLowerCase();
		String assiRegEx = "(" + Pattern.quote(assiName) + "|<assistant_name>" + ")";
		text = text.replaceFirst("^" + assiRegEx + " ","").trim();
		text = text.replaceFirst(" " + assiRegEx + "$","").trim();
		*/
		
		text = text.replaceAll("^(ich |wir |)(wuerde(n|)|moechte(n|))( gerne|)\\b", "").trim();
		text = text.replaceAll("\\b(vielleicht|bitte)\\b", "").trim();

		//clean up
		text = text.replaceAll("\\s+", " ").trim();
		
		return text;
	}

	@Override
	public String reconstructPhrase(String rawText, String phrase) {
		return NormalizerLight.recover(rawText, phrase);
	}
}
