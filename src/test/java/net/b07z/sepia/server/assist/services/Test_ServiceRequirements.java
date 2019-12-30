package net.b07z.sepia.server.assist.services;

import net.b07z.sepia.server.assist.services.ServiceRequirements;

public class Test_ServiceRequirements {

	public static void main(String[] args){
		System.out.println(new ServiceRequirements()
			.serverMinVersion("2.3.2")
			.apiAccess(ServiceRequirements.Apis.fhem)
			.toJson()
		);
	}

}
