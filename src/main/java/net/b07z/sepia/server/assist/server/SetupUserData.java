package net.b07z.sepia.server.assist.server;

import java.util.Arrays;
import java.util.List;

import net.b07z.sepia.server.core.data.Role;
import net.b07z.sepia.server.core.tools.Is;
import net.b07z.sepia.server.core.tools.Security;

/**
 * Class used to create users during automatic setup.
 */
public class SetupUserData {
	
	private String nickname;
	private String email;
	private String password;
	private List<String> roles;

	public SetupUserData(){}
	
	//getters and setters

	public String getNickname(){
		return nickname;
	}
	public void setNickname(String nickname){
		this.nickname = nickname;
	}

	public String getEmail(){
		return email;
	}
	/**
	 * Get email or build default email with user tag.
	 * @param userTag - tag of user put before the "@" when using default.
	 */
	public String getEmailOrDefault(String userTag){
		if (Is.nullOrEmpty(email)){
			return userTag + "@sepia.localhost"; 
		}else{
			return email;
		}
	}
	public void setEmail(String email){
		this.email = email;
	}
	
	public String getPassword(){
		return password;
	}
	/**
	 * Get password. If password is null, empty or '&lt;random&gt;' create
	 * a random password.
	 */
	public String getPasswordOrRandom(){
		if (Is.nullOrEmpty(password) || password.equals("<random>")){
			return Security.createRandomString(24, Security.simplePasswdChars);
		}
		return password;
	}
	public void setPassword(String password){
		this.password = password;
	}

	public List<String> getRoles(){
		return roles;
	}
	/**
	 * Get roles. If roles are null or empty return array with single entry: 'user'.
	 */
	public List<String> getRolesOrDefault(){
		if (Is.nullOrEmpty(roles)){
			return Arrays.asList(Role.user.name());
		}
		return roles;
	}
	public void setRoles(List<String> roles){
		this.roles = roles;
	}
	
	@Override
	public String toString(){
		return "Nickname: " + this.nickname + " - Email: " + this.email 
			+ " - Password: " + this.password + " - Roles: " + this.roles;
	}
}
