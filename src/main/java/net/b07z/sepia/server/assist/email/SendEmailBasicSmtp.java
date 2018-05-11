package net.b07z.sepia.server.assist.email;
import java.util.Properties;
import java.io.File;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.*;
import javax.mail.internet.*;

import net.b07z.sepia.server.assist.server.Config;
import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;

public class SendEmailBasicSmtp implements SendEmail {
	
	private final static boolean debug = false;
	
	private String mailhost = "";
	private String email_account = "";
	private String email_account_key = "";
	private String from = "";
	//private static final int port = 465;
	//private static final String mailhost = "mrvnet.kundenserver.de";
	private static final int port = 25;
	
	private static Map<String, String> templateCache;
	private static final String REGISTRATION = "registration";
	private static final String PWD_RESET = "reset";
	
	public String mailer = "msgsend";
	
	private Exception e = null;
	
	public SendEmailBasicSmtp(){
		//config
		this.mailhost = Config.emailHost;
		this.email_account = Config.emailAccount;
		this.email_account_key = Config.emailAccountKey;
		this.from = Config.emailAccount;
	}
	
	//get a template from server
	private String getTemplate(String templateName, String language){
		if (templateCache == null){
			templateCache = new HashMap<>();
		}
		templateName = templateName + "_" + language;
		String template = templateCache.get(templateName + "_" + language);
		if (template == null){
			template = Connectors.simpleHtmlGet(Config.urlWebFiles + "email-templates/" + (templateName + ".html"));
			if (!template.isEmpty()){
				templateCache.put(templateName, template);
			}
		}
		return template;
	}
	@Override
	public void refreshTemplates() {
		templateCache = new HashMap<>();
	}
	
	/**
	 * Load the default "please click this link"-registration message from URL template.
	 * @param token - registration token generated during request
	 * @return full HTML message to put in email body
	 */
	@Override
	public String loadDefaultRegistrationMessage(String language, String userid, String ticketid, String token, String timeStamp){
		if (userid.trim().isEmpty() || token.trim().isEmpty() || timeStamp.trim().isEmpty()){
			return "";
		}
		try{
			String redirect = Config.urlCreateUser 
					+ "?userid=" + URLEncoder.encode(userid, "UTF-8") 
					+ "&ticketid=" + URLEncoder.encode(ticketid, "UTF-8")
					+ "&time=" + URLEncoder.encode(timeStamp, "UTF-8") 
					+ "&token=" + URLEncoder.encode(token, "UTF-8")
					+ "&lang=" + URLEncoder.encode(language, "UTF-8")
					+ "&type=" + "email";
			
			if (Config.redirectEmail){
				Debugger.println("Registration URL: " + redirect, 3); 		//debug
			}
			
			String message = getTemplate(REGISTRATION, language);
			message = message.replaceFirst("<REG:LINK>", redirect);
		
			return message;
			
		}catch (Exception e){
			e.printStackTrace();
			return "";
		}
	}
	/**
	 * Load the reset-password "click this link" etc. template.
	 * @param token - reset token generated during request
	 * @return full HTML message to put in email body
	 */
	@Override
	public String loadPasswordResetMessage(String language, String userid, String ticketid, String token, String timeStamp){
		if (userid.trim().isEmpty() || token.trim().isEmpty() || timeStamp.trim().isEmpty()){
			return "";
		}
		try{
			String redirect = Config.urlChangePassword 
					+ "?userid=" + URLEncoder.encode(userid, "UTF-8") 
					+ "&ticketid=" + URLEncoder.encode(ticketid, "UTF-8")
					+ "&time=" + URLEncoder.encode(timeStamp, "UTF-8") 
					+ "&token=" + URLEncoder.encode(token, "UTF-8")
					+ "&lang=" + URLEncoder.encode(language, "UTF-8")
					+ "&type=" + "email";
			
			if (Config.redirectEmail){
				Debugger.println("Password reset URL: " + redirect, 3); 		//debug
			}
			
			String message = getTemplate(PWD_RESET, language);
			message = message.replaceFirst("<RES:LINK>", redirect);
		
			return message;
			
		}catch (Exception e){
			e.printStackTrace();
			return "";
		}
	}
	
	@Override
	public int send(String to, String message, String subject, File attachFile){
		//String cc = null;
		String bcc = Config.emailBCC;
		e = null;
		
		//test?
		if (Config.redirectEmail){
			Debugger.println(message, 1);
			return -1;
		}
		
		//check message
		if (to.trim().isEmpty() || message.trim().isEmpty()){
			return 1;
		}
		//check email address
		if (!to.matches(".+@.+")){
			return 2;
		}
		//GO!
		try{
			/*
		     * Initialize the JavaMail Session.
		     */
		    Properties props = System.getProperties();
		    // XXX - could use Session.getTransport() and Transport.connect()
		    // XXX - assume we're using SMTP
		    props.put("mail.transport.protocol","smtp");
			props.put("mail.smtp.host", mailhost);
			props.put("mail.smtp.port", port);
		    props.setProperty("mail.smtp.user", email_account);
		    props.setProperty("mail.smtp.password", email_account_key);
		    props.setProperty("mail.smtp.auth", "true");
		    props.setProperty("mail.smtp.starttls.enable", "true");

		    // Get a Session object
		    Session session = Session.getInstance(props, null);
		    if (debug)
		    	session.setDebug(true);

		    /*
		     * Construct the message and send it.
		     */
		    Message msg = new MimeMessage(session);
		    if (from != null){
		    	msg.setFrom(new InternetAddress(from, from));
		    	//System.out.println("from: " + new InternetAddress(from)); 	//debug
		    }else{
		    	msg.setFrom();
		    }

		    msg.setRecipients(Message.RecipientType.TO,	InternetAddress.parse(to, false));
		    /*
		    if (cc != null)
		    	msg.setRecipients(Message.RecipientType.CC,	InternetAddress.parse(cc, false));
		    */
		    if (bcc != null)
		    	msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc, false));

		    msg.setSubject(subject);

		    String text = message;

		    if (attachFile != null) {
				// Attach the specified file.
				// We need a multipart message to hold the attachment.
				MimeBodyPart mbp1 = new MimeBodyPart();
				mbp1.setText(text);
				MimeBodyPart mbp2 = new MimeBodyPart();
				mbp2.attachFile(attachFile);
				MimeMultipart mp = new MimeMultipart();
				mp.addBodyPart(mbp1);
				mp.addBodyPart(mbp2);
				msg.setContent(mp);
		    } else {
		    	// charset?
		    	//distinguish HTML and normal text:
		    	if (text.startsWith("<")){
		    		msg.setContent(text, "text/html; charset=utf-8");	//msg.setContent("<h1>This is actual message</h1>", "text/html" );
		    	}else{
		    		msg.setText(text);
		    	}
		    }

		    msg.setHeader("X-Mailer", mailer);
		    msg.setSentDate(new Date());

		    // send the thing off
		    //Transport.send(msg);
		    Transport.send(msg, email_account, email_account_key);

		    return 0; 	//no error - mail sent
		
		//ERRORS
		}catch (AddressException e){
			this.e = e;
			return 2;
		}catch (SendFailedException e){
			this.e = e;
			return 1;
		}catch (MessagingException e){
			this.e = e;
			e.printStackTrace();
			return 3;	//server error
			
		}catch (Exception e){
			this.e = e;
			e.printStackTrace();
			return 4; 	//unknown error
		}
	}
	
	/**
	 * If there was an error this can give you more info.
	 */
	public Exception getError(){
		return e;
	}

}
