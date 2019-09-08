package net.b07z.sepia.server.assist.parameters;

import java.util.ArrayList;
import java.util.List;

import net.b07z.sepia.server.assist.interpreters.RegexParameterSearch;

public class Test_SlashCommandsAndInputMod {

	public static void main(String[] args) {
		
		//Slash-Commands:
		List<String> texts = new ArrayList<>(); 
		texts.add("saythis test");
		texts.add("hey sepia saythis test");
		texts.add("sepia saythis test");
		texts.add("saythis");
		texts.add("linkshare http:\\b07z.net");
		texts.add("http://b07z.net");
		texts.add("https://b07z.net");
		
		for (String s : texts){
			System.out.println("text: " + s + " - has cmd: " + RegexParameterSearch.contains_slashCMD(s));
		}
		
		System.out.println("\n---\n");
		
		//Slash-Commands:
		List<String> texts2 = new ArrayList<>(); 
		texts2.add("i18n:de test");
		texts2.add("i18n:de sepia test");
		texts2.add("sepia i18n:de test");
		texts2.add("i18n:de");
		
		for (String s : texts2){
			System.out.println("text: " + s + " - has mod.: " + RegexParameterSearch.find_input_modifier(s));
		}
	}

}
