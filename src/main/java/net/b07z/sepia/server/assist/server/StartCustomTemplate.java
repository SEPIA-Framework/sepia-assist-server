package net.b07z.sepia.server.assist.server;

public class StartCustomTemplate extends Start {
	
	//my specific settings
	//...
	//e.g.: if (serverType.equals(CUSTOM_SERVER)) ... 
	
	//End-points that this server is offering
	public static void loadEndpoints(){
		Start.loadEndpoints();
		//add		
		//e.g.: get("/my-endpoint", (request, response) ->		myEndpointGet(request, response));
		//e.g.: post("/my-endpoint", (request, response) ->		myEndpointPost(request, response));
	}
	
	//All kinds of things that should be loaded on startup.
	public static void setupModules(){
		Start.setupModules();
		//add
		//e.g.: special custom scripts, workers, etc.
	}
	
	//You can add individual parameters here for example
	public static void setupServicesAndParameters(){
		//replace services in use, e.g.:
		/*
		Map<String, ArrayList<String>> systemInterviewServicesMap = new HashMap<>();
		
		//CONTROL DEVICES
		ArrayList<String> controlDevice = new ArrayList<String>();
			controlDevice.add(MyDeviceControlService.class.getCanonicalName());
		systemInterviewServicesMap.put(CMD.CONTROL, controlDevice);
		
		//BANKING
		ArrayList<String> banking = new ArrayList<String>();
			banking.add(MyBankingService.class.getCanonicalName());
		systemInterviewServicesMap.put(CMD.BANKING, banking);
			
		InterviewServicesMap.loadCustom(systemInterviewServicesMap);
		*/
		
		//defaults
		Start.setupServicesAndParameters();
		
		//add
		//e.g.: ParameterConfig.setHandler(PARAMETERS.ALARM_NAME, Config.parentPackage + ".parameters.AlarmName");
	}
	
	//Stuff to add to the default statistics output (e.g. from end-point hello).
	public static String addToStatistics(){
		String addedStats = Start.addToStatistics();
		//add
		//e.g.: addedStats += MyReport();
		return addedStats;
	}
	
	//MAIN
	public static void main(String[] args) {

		//load settings
		loadSettings(args);
		
		//load statics and workers and setup modules (loading stuff to memory etc.)
		setupModules();
		
		//setup services and parameters by connecting commands etc.
		setupServicesAndParameters();
		
		//check existence of universal accounts (superuser and assistant)
		checkCoreAccounts();
		
		//load updates to the framework that have no specific place yet
		loadUpdates();
		
		//setup server with port, cors and error handling etc. 
		setupServer();
		
		//SERVER END-POINTS
		loadEndpoints();
	}
	
	//----- End-point methods -----
	
	
	//add custom end-point here or in own class

}
