package net.b07z.sepia.server.assist.email;

import java.io.File;

public interface SendEmail {

	/**
	 * Load the default "please click this link"-registration message from URL template.
	 * @param ticketid - global unique ticket id
	 * @param token - registration token generated during request
	 * @return full HTML message to put in email body
	 */
	public String loadDefaultRegistrationMessage(String language, String userid, String ticketid, String token, String timeStamp);
	
	/**
	 * Load the reset-password "click this link" etc. template.
	 * @param ticketid - global unique ticket id
	 * @param token - reset token generated during request
	 * @return full HTML message to put in email body
	 */
	public String loadPasswordResetMessage(String language, String userid, String ticketid, String token, String timeStamp);
	
	/**
	 * In case the implementation uses pre-loaded templates this can be used to refresh them.
	 */
	public void refreshTemplates();
	
	/**
	 * Send message to receiver and return result code.
	 * @param to - email address
	 * @param message - message to send (can include HTML)
	 * @param subject - subject of message
	 * @param attachFile - attachment (or null)
	 * @return result code:<br>
	 * 		-1 not sent (for reasons like testing or other)<br>
	 * 		 0 no error<br>
	 * 		 1 SendFailedException (send failed or empty message/receiver)<br>
	 * 		 2 AddressException (wrong mail)<br>
	 * 		 3 MessagingException (message not sent)<br>
	 * 		 4 other error (unknown)<br>  
	 */
	public int send(String to, String message, String subject, File attachFile);
	
	/**
	 * If there was an error this should give you more info.
	 */
	public Exception getError();
}
