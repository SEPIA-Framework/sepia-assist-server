package net.b07z.sepia.server.assist.tools;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestCalculator {

	@Test
	public void test() {
		Map<String, Double> variables1 = new HashMap<>();
		variables1.put("x", 5d);
		variables1.put("m", 2d);
		variables1.put("n", 10d);
		
		String expression1 = "1+1";
		String expression2 = "2+3*3";
		String expression3 = "5 * (10 + 2) / 6";
		String expression4 = "5*x + 6";
		String expression5 = "0.39 * x";
		String expression6 = "0,39 * x";
		String expression7 = "100 * x / 255";
		String expression8 = "m*x + n";
		String expression9 = "m*x + m";
		String expression10 = "m*x + z";
		String expression11 = "round(x*11.1)/10";
		
		assertTrue(Calculator.parseExpression(expression1).doubleValue() == 2d);
		assertTrue(Calculator.parseExpression(expression1).toString().equals("2.0"));
		assertTrue(Calculator.parseExpression(expression2).doubleValue() == 11d);
		assertTrue(Calculator.parseExpression(expression3).doubleValue() == 10d);
		assertTrue(Calculator.parseExpression(expression3).toString().equals("10.0"));
		
		assertTrue(Calculator.parseExpression(expression4, variables1).doubleValue() == 31d);
		
		assertTrue(Calculator.parseExpression(expression5, variables1).floatValue() == 1.95f);
		assertThrows(IllegalArgumentException.class, () -> {
			Calculator.parseExpression(expression6, variables1).floatValue();
		});
		
		double res1 = Calculator.parseExpression(expression7, variables1).doubleValue();
		assertTrue(res1 > 1.96 && res1 < 1.961);
		assertTrue((float) res1 == 1.9607843f);
		
		assertTrue(Calculator.parseExpression(expression8, variables1).doubleValue() == 20d);
		assertTrue(Calculator.parseExpression(expression9, variables1).doubleValue() == 12d);
		
		assertThrows(IllegalArgumentException.class, () -> {
			Calculator.parseExpression(expression10, variables1);
		});
		
		assertThrows(IllegalArgumentException.class, () -> {
			Calculator.parseExpression("Hello World");
		});
		
		assertTrue(Calculator.parseExpression(expression11, variables1).floatValue() == 5.6f);
	}

}
