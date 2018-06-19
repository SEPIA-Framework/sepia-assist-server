package net.b07z.sepia.server.assist.users;

import net.b07z.sepia.server.core.tools.Security;

public class Test_passwordGenerator {

	public static void main(String[] args){
		
		String guuid = "uid1002";
		String pwd = "";
		String pwdHash = Security.hashClientPassword(pwd);
		
		try{
			ID.Generator idGen = new ID.Generator(guuid, pwdHash);
			
			System.out.println("Guuid: " + idGen.guuid);
			System.out.println("Pwd: " + idGen.pwd);
			System.out.println("Salt: " + idGen.salt);
			System.out.println("Iterations: " + idGen.iterations);
		
		}catch (Exception e) {
			throw new RuntimeException("Error: " + e.getMessage());
		}
	}
}
