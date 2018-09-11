package net.b07z.sepia.server.assist.interpreters;

import net.b07z.sepia.server.assist.server.Config;

public class Test_Normalizers {
	
	public static void main(String[] args){
		
		Config.assistantName = "Sepia";
		String lang = "de";
		String text = "Hi Sepia";
		String match = "hi";
		
		Normalizer normalizer_light = Config.inputNormalizersLight.get(lang);
		Normalizer normalizer_default = Config.inputNormalizers.get(lang);
	
		String tn1 = normalizer_light.normalizeText(text);
		String tn2 = normalizer_default.normalizeText(text);
		
		System.out.println("Match this: " + match);
		System.out.println("n1: " + tn1);
		System.out.println("match: " + match.equals(tn1));
		System.out.println("n2: " + tn2);
		System.out.println("match: " + match.equals(tn2));
		System.out.println("-----------------");
		String raw = "München";
		String test = "muenchen";
		String recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
		System.out.println("-----------------");
		raw = "Manüllen, Manuel?, ";
		test = "manuel";
		recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
		System.out.println("-----------------");
		raw = "Der Text.Norm Test";
		test = "text.norm";
		recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
		System.out.println("-----------------");
		raw = "Hallo {\"Text.Norm\"} Test";
		test = "{\"text.norm\"}";
		recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
		System.out.println("-----------------");
		raw = "Dies ist ein {\"ABSTRAKTES Ball.Spiel\"} Ergebnis";
		test = "{\"abstraktes ball.spiel\"}";
		recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
		System.out.println("-----------------");
		raw = "Kaffee, Milch, Zucker und Wasser.";
		test = "kaffee milch";
		recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
		System.out.println("-----------------");
		raw = "Birnen, Äpfel, Pfirsiche";
		test = "aepfel";
		recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
		System.out.println("-----------------");
		raw = "Êber, Éber";
		test = "eber";
		recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
		System.out.println("-----------------");
		raw = "Über uber";
		test = "ueber";
		recon = normalizer_light.reconstructPhrase(raw, test);
		System.out.println("raw: " + raw + " - test: " + test + " - recon.: " + recon);
	}

}
