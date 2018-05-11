package net.b07z.sepia.server.assist.assistant;

import net.b07z.sepia.server.assist.interpreters.NluTools;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Currencies (statics) and methods to convert money.
 * 
 * @author Florian Quirin
 */
public class CURRENCY {
	
	//excahnge rates
	private static double GBP_TO_EUR = 1.2769d;
	private static double USD_TO_EUR = 0.87966d;
	
	//Currencies
	public static final String EURO = "euro";
	public static final String EURO_CENT = "cent_eu";
	public static final String DOLLAR_US = "dollar_us";
	public static final String DOLLAR_CENT = "cent_us";
	public static final String POUND_GB = "pound_gb";

	//check NLU_parameter_search.get_amount_of(...,"money",...) as well!
	public static final String TAGS_DE = "(bitcoin|bitcoins|btc|euro|euros|eur|€|eurocent(s|)|cent(s|)|ct|"
						+ "(us |amerikanische(r|) |)dollar(s|)|usd|\\$|gb pfund|britische(s|) pfund|gbp|£|"
						+ "yen|jpy|¥|yuan|renminbi|cny|cn¥|rmb|russischer rubel|rubel|rub|dirham|aed)";
	public static final String TAGS_EN = "(bitcoin|bitcoins|btc|euro|euros|eur|€|eurocent(s|)|cent(s|)|ct|"
						+ "(us |american |)dollar(s|)|usd|\\$|gb pound(s|)|british pound(s|)|gbp|£|"
						+ "yen|jpy|¥|yuan|renminbi|cny|cn¥|rmb|russian (rubel|rouble)(s|)|rubel(s|)|rouble(s|)|rub|dirham|aed)";
	
	/**
	 * Convert currency from one to another. Values are fix (updated from time to time) and should not be considered exact but 
	 * rather as a rough indication.
	 * @param from - starting currency, currently: gbp|£, usd|$, eur|€
	 * @param to - target currency, currently: gbp|£, usd|$, eur|€
	 * @param amount - value to be converted
	 * @return converted double or 0.0d if missing support.
	 */
	public static double convertCurrency(String from, String to, double amount){
		//to euro
		if (NluTools.stringContains(from, "gbp|£")){
			amount = (amount * GBP_TO_EUR);
		}else if (NluTools.stringContains(from, "usd|\\$")){
			amount = (amount * USD_TO_EUR);
		}else if (NluTools.stringContains(from, "eur|€")){
			//no change
		}else{
			Debugger.println("Missing convertCurrency() support from '" + from + "' to '" + to + "'", 3);
			return 0.0d;
		}
		//to target
		if (NluTools.stringContains(to, "eur|€")){
			return amount;
		}else if (NluTools.stringContains(to, "gbp|£")){
			return (amount / GBP_TO_EUR);
		}else if (NluTools.stringContains(to, "usd|\\$")){
			return (amount / USD_TO_EUR);
		}else{
			Debugger.println("Missing convertCurrency() support from '" + from + "' to '" + to + "'", 3);
			return 0.0d;
		}
	}
	/**
	 * Helper to convert currency by parsing "amount"-String to double before calling 
	 * convertCurrency(String from, String to, double amount). Returns 0.0d on parsing error, so 
	 * be sure to check that you got the right value!
	 */
	public static double convertCurrency(String from, String to, String amount){
		try {
			double amt = Double.parseDouble(amount);
			return convertCurrency(from, to, amt);
		}catch (Exception e){
			Debugger.println("convertCurrency(..) error while trying to parse " + amount, 1);
			return 0.0d;
		}
	}
}
