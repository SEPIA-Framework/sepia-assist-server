package net.b07z.sepia.server.assist.database;

import net.b07z.sepia.server.assist.database.GUID;

public class Test_GUID {

	public static void main(String[] args) {
		
		String id1 = GUID.getUserGUID();
		String id2 = GUID.getTicketGUID();
		
		System.out.println(id1);
		System.out.println(id2);
	}

}
