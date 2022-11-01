package net.b07z.sepia.server.assist.interpreters;

public class Test_NluTools {

	public static void main(String[] args) {
		findFirst("temperature sensor", "(sensor|temperature)");
		findFirst("temperature sensor", "(temperature|temperature sensor)");
	}

	private static void findFirst(String input, String regExp){
		System.out.println(input + " - " + NluTools.stringFindFirst(input, regExp));
	}
}
