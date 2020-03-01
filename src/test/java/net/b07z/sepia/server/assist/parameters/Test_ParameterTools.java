package net.b07z.sepia.server.assist.parameters;

import net.b07z.sepia.server.core.tools.Debugger;

public class Test_ParameterTools {

	public static void main(String[] args){
		
		runPerformanceProfilerTest();
	}

	private static void runPerformanceProfilerTest(){
		//Config.parameterPerformanceMode = 0;
		System.out.println("Parameter performance profiler test:\n");
		int N = 13;
		for (int testRunN=0; testRunN<N; testRunN++){
			Long res = ParameterTools.runOrSkipPerformanceCriticalMethod(1001l, (i) -> {
				System.out.println("test: " + i);
				Debugger.sleep(i * 20l);
				return 0l;
			}, testRunN);
			if (res == null){
				System.out.println("skipped: " + testRunN);
			}
		}
		System.out.println("Parameter performance profiler wait for reset:");
		Debugger.sleep(8000l);
		Long res = ParameterTools.runOrSkipPerformanceCriticalMethod(1001l, (j) -> {
			System.out.println("test: " + j);
			Debugger.sleep(j * 7l);
			return 0l;
		}, N);
		if (res == null){
			System.out.println("skipped: " + N);
		}
		System.out.println("Parameter performance profiler wait for reset:");
		Debugger.sleep(8000l);
		for (int testRunN=N+1; testRunN<(N+12); testRunN++){
			ParameterTools.runOrSkipPerformanceCriticalMethod(1001l, (j) -> {
				System.out.println("test: " + j);
				Debugger.sleep(j * 6l);
				return 0l;
			}, testRunN);
		}
	}
}
