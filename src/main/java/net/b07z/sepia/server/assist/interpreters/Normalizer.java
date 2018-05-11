package net.b07z.sepia.server.assist.interpreters;

/**
 * Normalize text by removing elements that can be considered "junk" (like a "hello," at the beginning or a "please" at the end ;-)
 *  
 * @author Florian Quirin
 *
 */
public interface Normalizer {
	
	/**
	 * Do the normalization and return new text.
	 * @param text - input to normalize
	 * @return
	 */
	public String normalize_text(String text);
	
	/**
	 * Reconstruct the original as good as possible, e.g. recover "München" from "muenchen".
	 * @param rawText - original text
	 * @param phrase - phrase to recover
	 * @return part of the original that matches the phrase or the input (if recon. fails)
	 */
	public String reconstructPhrase(String rawText, String phrase);

}
