package net.b07z.sepia.server.assist.tools;

import java.util.List;
import java.util.Random;

public class RandomGen {
	
	public static final Random RANDOM = new Random();
	
	/**
	 * Generate a random integer value between 'min' (inclusive) and 'max' (inclusive). 
	 */
	public static int getInt(int min, int max){
		int x = RANDOM.nextInt((max-min)+1);
		return (min + x);
	}
	/**
	 * Returns a pseudo-random, uniformly distributed int value between 0 (inclusive) and the specified value N (exclusive). 
	 * If N==0 returns 0.
	 * @param N - upper border (exclusive)
	 * @return random int between 0-N
	 */
	public static int getInt(int N){
		if (N==0){
			return 0;
		}
		Random randomGenerator = new Random();
	    int index = randomGenerator.nextInt(N);
	    return index;
	}
	
	/**
	 * Get random value of an enumerator.
	 * @param clazz - enum class
	 * @return random value
	 */
	public static <T extends Enum<?>> T enumValue(Class<T> clazz){
        int x = RANDOM.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }
	
	/**
	 * Get random list entry
	 * @param list - entry of this list
	 * @return random entry
	 */
	public static Object listValue(List<?> list){
		int x = RANDOM.nextInt(list.size());
		return list.get(x);
	}
}
