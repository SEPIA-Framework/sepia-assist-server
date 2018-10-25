package net.b07z.sepia.server.assist.users;

import net.b07z.sepia.server.assist.database.GUID;
import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Security;

/**
 * Handle user ID types.
 * 
 * @author Florian Quirin
 *
 */
public class ID {
	
	/**
	 * Valid IDs to authenticate the user.
	 */
	public class Type {
		public static final String uid = "uid";
		public static final String email = "email";
		public static final String phone = "phone";
	}

	/**
	 * Auto-detect user id type (email, uid, phone?) 
	 * @return id type or empty string
	 */
	public static String autodetectType(String id){
		id = clean(id);
		if (id.matches("^" + Config.userIdPrefix + "\\d+")){
			return Type.uid;
		}else if(id.matches(".*\\w+\\@.*\\..*\\w+")){
			return Type.email;
		}else if (id.matches("(\\+|\\-|)\\d+")){
			return Type.phone;
		}else{
			Debugger.println("ID type - autodetectType(...) failed! Id: " + id, 1);
			return "";
		}
	}
	
	/**
	 * Clean up the ID before trying to store or match it. Throws error on "-" and empty.
	 */
	public static String clean(String id){
		id = id.replaceAll("\\s+", "").trim().toLowerCase().trim();
		if (id.isEmpty() || id.equals("-")){
			throw new RuntimeException("cleanID(..) reports invalid ID: " + id);
		}else{
			return id;
		}
	}
	
	/**
	 * Generate a new ID, holding a new global ID and a securely hashed password.
	 */
	public static class Generator {
		public String guuid;
		public byte[] saltBytes;
		public String salt;
		public int iterations;
		public String pwd;
		
		/**
		 * Make new ID-object with unique id. Depending on how you handle your password it is expected to be hashed here
		 * with the client method.
		 */
		public Generator(String pwd) throws Exception{
			guuid = GUID.getUserGUID();			//get a new unique ID for the user
			iterations = 20000;
			saltBytes = Security.getRandomSalt(32);
			salt = Security.bytearrayToHexString(saltBytes);
			this.pwd = hashPassword_server(pwd, saltBytes, iterations);
		}
		/**
		 * Make new ID-object with defined id. Depending on how you handle your password it is expected to be hashed here
		 * with the client method.
		 */
		public Generator(String guuid, String pwd) throws Exception{
			this.guuid = guuid;
			iterations = 20000;
			saltBytes = Security.getRandomSalt(32);
			salt = Security.bytearrayToHexString(saltBytes);
			this.pwd = hashPassword_server(pwd, saltBytes, iterations);
		}
		/**
		 * Rebuild old password. Depending on how you handle your password it is expected to be hashed here
		 * with the client method.
		 */
		public Generator(String pwd, String salt, int N) throws Exception{
			if (N<1){
				throw new RuntimeException("Invalid N!");
			}
			iterations = N;
			saltBytes = Security.hexToByteArray(salt);
			this.pwd = hashPassword_server(pwd, saltBytes, iterations);
		}
	}

	/**
	 * Hash the submitted password. This is the server-side implementation. If the database is hacked this is the last 
	 * instance to keep the password save as the client side hashing can never be fully hidden.
	 * @param pwd - password to hash. Depending on how you handle your password it is expected to be hashed here with the client method.
	 * @param salt - random byte-array salt
	 * @param N - hash iterations of implemented algorithm
	 * @return
	 */
	private static String hashPassword_server(String pwd, byte[] salt, int N){
		try {
			//return Security.bytearrayToHexString(Security.getSha256(userid + pwd + "galliwho1"));
			return Security.bytearrayToHexString(Security.getEncryptedPassword("galli" + pwd + "who1", salt, N, 32));
		} catch (Exception e) {
			throw new RuntimeException("secure password generation failed! (server)", e);
		}
	}
}
