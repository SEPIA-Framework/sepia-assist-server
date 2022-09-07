package net.b07z.sepia.server.assist.tools;

import java.util.Map;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.fathzer.soft.javaluator.StaticVariableSet;

/**
 * A class helping with calculations and parsing string expressions.
 * 
 * @author Florian Quirin
 *
 */
public class Calculator {

	/**
	 * Parse a string expression, evaluate and return result as Double value.
	 * @param expression - a mathematical expression as string, e.g. "2*x + 1"
	 * @param variablesMap - a map of variables, e.g. ("x", 10)
	 * @return result as Double
	 */
	public static Double parseExpression(String expression, Map<String, Double> variablesMap) throws IllegalArgumentException {
		//evaluator
		final DoubleEvaluator eval = new DoubleEvaluator();
		//variables
		if (variablesMap != null){
			//transfer map to variables set (required for this implementation)
			final StaticVariableSet<Double> variables = new StaticVariableSet<>();
			variablesMap.forEach((k, v) -> {
				variables.set(k, v);
			});
			//process
			return eval.evaluate(expression, variables);
		}else{
			//process
			return eval.evaluate(expression);
		}
	}
	/**
	 * Parse a string expression, evaluate and return result as Double value.
	 * @param expression - a mathematical expression as string, e.g. "11 * 14"
	 * @return result as Double
	 */
	public static Double parseExpression(String expression) throws IllegalArgumentException {
		return parseExpression(expression, null);
	}
}
