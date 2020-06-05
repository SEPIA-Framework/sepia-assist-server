package net.b07z.sepia.server.assist.workers;

import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.JSON;

public class Test_DuplexConnection {

	public static void main(String[] args) throws Exception{
		
		boolean autoReconnect = false;
		boolean keepOpen = true;
		
		System.out.println("Creating duplex connection 1");
		DuplexConnectionInterface dCon1 = new MqttConnection("tcp://localhost:1883", "pub_client", "password", autoReconnect);
		dCon1.setup();
		
		//Method 1 (500ms delay with onConnected callback)
		System.out.println("\nConnecting 1 ...");
		dCon1.connect(500, () -> {
			
			System.out.println("\nRegistering message handlers 1 ...");
			dCon1.addMessageHandler("sepia/smart-devices/#", msg -> {
				System.out.println("dCon1 saw: " + msg);
			});

			System.out.println("\nREADY Player One");
		});
		
		System.out.println("\nCreating duplex connection 2");
		DuplexConnectionInterface dCon2 = new MqttConnection("tcp://localhost:1883", "sub_client", "password", autoReconnect);
		dCon2.setup();
		
		//Method 2 (0 delay with direct check)
		System.out.println("\nConnecting 2 ...");
		dCon2.connect();
		boolean connected2 = dCon2.waitForState(0, 10000);
		
		if (!connected2){
			System.err.println("connection 2 timeout");
			System.err.println("status 2: " + dCon2.getStatusDescription()); 
		}else{
			System.out.println("\nRegistering message handlers 2 ...");
			dCon2.addMessageHandler("sepia/smart-devices/test/1", msg -> {
				System.out.println("dCon2 saw: " + msg);
			});

			System.out.println("\nREADY Player Two");
		}
		
		if (!dCon1.waitForState(0, 10000)){
			System.err.println("connection 1 timeout");
			System.err.println("status 1: " + dCon1.getStatusDescription());
		}
		
		if (dCon1.getStatus() == 0 && dCon2.getStatus() == 0){
			System.out.println("\nSending messages ...\n");
			
			dCon1.sendMessage(JSON.make("state", "70"), "sepia/smart-devices/test/1", null);
			dCon1.sendMessage(JSON.make("text", "other"), "sepia/other/text", null);
			dCon2.sendMessage(JSON.make("state", "30"), "sepia/smart-devices/test/2", null);
			dCon2.sendMessage(JSON.make("text", "other2"), "sepia/other/text2", null);
		}
		
		Debugger.sleep(3000);
		
		if (!keepOpen){
			System.out.println("\nClosing ...");
			dCon1.disconnect();
			dCon1.waitForState(2, 5000);
			dCon2.disconnect();
			dCon2.waitForState(2, 5000);
	
			System.out.println("\nBYE");
		
		}else{
			System.out.println("\nFINISHED");
		}
	}

}
