package net.b07z.sepia.server.assist.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.b07z.sepia.server.assist.messages.Clients;
import net.b07z.sepia.server.assist.workers.ServiceBackgroundTaskManager;
import net.b07z.sepia.server.assist.workers.Workers;
import net.b07z.sepia.server.core.server.BasicStatistics;
import net.b07z.sepia.server.core.tools.ThreadManager;

/**
 * Track all sorts of statistics like total hits, time authorization took, API calls, etc.
 * 
 * @author Florian Quirin
 *
 */
public class Statistics extends BasicStatistics {
	
	static long apiErrorThreshold = 2000; 
	
	public static String getInfo(){
		int nlp_hits = nlp_total_hits.get();		int nlp_hits_a = nlp_auth_hits.get();		int nlp_hits_err = nlp_possible_errors.get();
		int api_hits = api_total_hits.get();		int api_hits_a = api_auth_hits.get();		int api_hits_err = api_possible_errors.get();
		int tts_hits = tts_total_hits.get();		int tts_hits_a = tts_auth_hits.get(); 		int tts_hits_err = tts_err_hits.get();
		int db_hits_a = db_total_hits.get();						int db_hits_err = db_possible_errors.get();
		int kdb_read_hits_a = kdb_read_total_hits.get();			int kdb_r_hits_err = kdb_r_possible_errors.get();
		int kdb_write_hits_a = kdb_write_total_hits.get();			int kdb_w_hits_err = kdb_w_possible_errors.get();
		int kdb_errors = kdb_error_hits.get();
		int auth_hits_a = nlp_hits_a + api_hits_a + tts_hits_a + tts_hits_err;
		long auth_time = auth_total_time.get();		long db_time = db_total_time.get();		
		long nlp_time = nlp_total_time.get();		long api_time = api_total_time.get();
		long tts_time = tts_total_time.get();
		long kdb_read_time = kdb_read_total_time.get();
		long kdb_write_time = kdb_write_total_time.get();
		String msg = 
				"Total hits (NLP,API,TTS): " + (nlp_hits + api_hits + tts_hits + tts_hits_err) + "<br>" +
				"Registration Req.: " + reg_total_hits.get() + "<br>" +
				"Forgotten passwords.: " + forgot_pwd_total_hits.get() + "<br>" +
				"Authentications: " + auth_hits_a + "<br>" +
				"Auth. Time: " + ((double)auth_time)/auth_hits_a + "ms (" + auth_time + "ms)" + "<br>" +
				"<br>" +
				"NLP (only) hits: " + nlp_hits_a + " (" + nlp_hits + ")" + "<br>" +
				"NLP poss. errors (>2s): " + nlp_hits_err + "<br>" +
				"NLP Time (all): " + ((double)nlp_time)/(nlp_hits_a-nlp_hits_err+api_hits_a-api_hits_err) + "ms (" + nlp_time + "ms)" + "<br>" +
				"<br>" +
				"API hits: " + api_hits_a + " (" + api_hits + ")" + "<br>" +
				"API poss. errors (>2s): " + api_hits_err + "<br>" +
				"API Time: " + ((double)api_time)/(api_hits_a-api_hits_err) + "ms (" + api_time + "ms)" + "<br>" +
				"<br>" +
				"TTS success: " + tts_hits_a + " (" + tts_hits + ")" + "<br>" +
				"TTS errors: " + tts_hits_err + " (" + tts_hits + ")" + "<br>" +
				"TTS Time: " + ((double)tts_time)/tts_hits_a + "ms (" + tts_time + "ms)" + "<br>" +
				"<br>" +
				"DB hits: " + db_hits_a + "<br>" +
				"DB poss. errors (>2s): " + db_hits_err + "<br>" +
				"DB Time: " + ((double)db_time)/(db_hits_a-db_hits_err) + "ms (" + db_time + "ms)" + "<br>" +
				"<br>" +
				"KDB read hits: " + kdb_read_hits_a + "<br>" +
				"KDB poss. errors (>2s): " + kdb_r_hits_err + "<br>" +
				"KDB read Time: " + ((double)kdb_read_time)/(kdb_read_hits_a-kdb_r_hits_err) + "ms (" + kdb_read_time + "ms)" + "<br>" +
				"KDB write hits: " + kdb_write_hits_a + "<br>" +
				"KDB poss. errors (>2s): " + kdb_w_hits_err + "<br>" +
				"KDB write Time: " + ((double)kdb_write_time)/(kdb_write_hits_a-kdb_w_hits_err) + "ms (" + kdb_write_time + "ms)" + "<br>" +
				"KDB conf. errors: " + kdb_errors + "<br>" +
				"<br>" +
				"WebSocket connection info:<br>" + Clients.getAssistantSocketClientStats() +
				Start.addToStatistics() +
				"<br>"
		;
		
		//add basics
		msg += getBasicInfo();
		
		//add workers
		msg += "Processing threads:<br>";
		msg += "Active threads now: " + (ThreadManager.getNumberOfCurrentlyActiveThreads() + ServiceBackgroundTaskManager.getNumberOfCurrentlyActiveThreads()) + "<br>";
		msg += "Max. active threads: " + (ThreadManager.getMaxNumberOfActiveThreads() + ServiceBackgroundTaskManager.getMaxNumberOfActiveThreads()) + "<br>";
		msg += "Scheduled or active custom tasks: " + ThreadManager.getNumberOfScheduledTasks() + "<br>";
		msg += "Scheduled or active service tasks: " + ServiceBackgroundTaskManager.getNumberOfScheduledTasks() + "<br>";
		msg += "<br>";
		msg += "Workers and Connections:<br>";
		msg += Workers.getStatsReport();
		
		return msg;
	}
	
	//Common
	private static AtomicLong auth_total_time = new AtomicLong(0);
	public static void save_Auth_total_time(long tic){
		long time = System.currentTimeMillis()-tic;
		auth_total_time.addAndGet(time);	//save total time needed to do authentication
	}
	
	//Registration requests
	private static AtomicInteger reg_total_hits = new AtomicInteger(0);
	public static void add_Registration_hit(){
		reg_total_hits.incrementAndGet();			//Registration API hit counter
	}
	//Forgot password requests
	private static AtomicInteger forgot_pwd_total_hits = new AtomicInteger(0);
	public static void add_forgot_pwd_hit(){
		forgot_pwd_total_hits.incrementAndGet();	//Forgot password API hit counter
	}
	
	//Interpreter API
	private static AtomicInteger nlp_total_hits = new AtomicInteger(0);
	private static AtomicInteger nlp_auth_hits = new AtomicInteger(0);
	private static AtomicInteger nlp_possible_errors = new AtomicInteger(0);	//possible error (>1s)
	private static AtomicLong nlp_total_time = new AtomicLong(0);
	public static void add_NLP_hit(){
		nlp_total_hits.incrementAndGet();	//Interpreter API hit counter
	}
	public static void add_NLP_hit_authenticated(){
		nlp_auth_hits.incrementAndGet();	//Interpreter API authenticated hit counter
	}
	public static void save_NLP_total_time(long tic){
		long time = System.currentTimeMillis()-tic;
		if (time > apiErrorThreshold){
			nlp_possible_errors.incrementAndGet();
		}else{
			nlp_total_time.addAndGet(time);		//save total time needed to do NLP (includes internal calls)
		}
	}
	
	//Answer API
	private static AtomicInteger api_total_hits = new AtomicInteger(0);
	private static AtomicInteger api_auth_hits = new AtomicInteger(0);
	private static AtomicInteger api_possible_errors = new AtomicInteger(0);	//possible error (>1s)
	private static AtomicLong api_total_time = new AtomicLong(0);
	public static void add_API_hit(){
		api_total_hits.incrementAndGet();	//Answer API hit counter
	}
	public static void add_API_hit_authenticated(){
		api_auth_hits.incrementAndGet();	//Answer API authenticated hit counter
	}
	public static void save_API_total_time(long tic){
		long time = System.currentTimeMillis()-tic;
		if (time > apiErrorThreshold){
			api_possible_errors.incrementAndGet();
		}else{
			api_total_time.addAndGet(time);		//save total time needed to do Answer API call
		}
	}
	
	//Account database calls
	private static AtomicInteger db_total_hits = new AtomicInteger(0);		//successful database hits
	private static AtomicInteger db_possible_errors = new AtomicInteger(0);	//possible error (>1s)
	private static AtomicLong db_total_time = new AtomicLong(0);
	public static void add_DB_hit(){
		db_total_hits.incrementAndGet();			//Database hit counter
	}
	public static void save_DB_total_time(long tic){
		long time = System.currentTimeMillis()-tic;
		if (time > apiErrorThreshold){
			db_possible_errors.incrementAndGet();
		}else{
			db_total_time.addAndGet(time);		//save total time needed to do successful database request
		}
	}
	//Knowledge database calls
	private static AtomicInteger kdb_write_total_hits = new AtomicInteger(0);		//successful database read
	private static AtomicInteger kdb_read_total_hits = new AtomicInteger(0);		//successful database write
	private static AtomicInteger kdb_w_possible_errors = new AtomicInteger(0);		//possible error (>1s)
	private static AtomicInteger kdb_r_possible_errors = new AtomicInteger(0);		//possible error (>1s)
	private static AtomicInteger kdb_error_hits = new AtomicInteger(0);				//definite errors
	private static AtomicLong kdb_read_total_time = new AtomicLong(0);
	private static AtomicLong kdb_write_total_time = new AtomicLong(0);
	public static void add_KDB_write_hit(){
		kdb_write_total_hits.incrementAndGet();			//Database hit counter
	}
	public static void add_KDB_read_hit(){
		kdb_read_total_hits.incrementAndGet();			//Database hit counter
	}
	public static void add_KDB_error_hit(){
		kdb_error_hits.incrementAndGet();			//Database error hit counter
	}
	public static void save_KDB_write_total_time(long tic){
		long time = System.currentTimeMillis()-tic;
		if (time > apiErrorThreshold){
			kdb_w_possible_errors.incrementAndGet();
		}else{
			kdb_write_total_time.addAndGet(time);		//save total time needed to do successful database request
		}
	}
	public static void save_KDB_read_total_time(long tic){
		long time = System.currentTimeMillis()-tic;
		if (time > apiErrorThreshold){
			kdb_r_possible_errors.incrementAndGet();
		}else{
			kdb_read_total_time.addAndGet(time);		//save total time needed to do successful database request
		}
	}
	
	//TTS API
	private static AtomicInteger tts_total_hits = new AtomicInteger(0);
	private static AtomicInteger tts_auth_hits = new AtomicInteger(0); 			//only added if result is not empty
	private static AtomicInteger tts_err_hits = new AtomicInteger(0);
	private static AtomicLong tts_total_time = new AtomicLong(0);				//only added if result is not empty
	public static void add_TTS_hit(){
		tts_total_hits.incrementAndGet();			//TTS hit counter
	}
	public static void add_TTS_hit_authenticated(){
		tts_auth_hits.incrementAndGet();	//TTS authenticated hit counter
	}
	public static void add_TTS_error(){
		tts_err_hits.incrementAndGet();	//TTS authenticated hit counter
	}
	public static void save_TTS_total_time(long tic){
		long time = System.currentTimeMillis()-tic;
		tts_total_time.addAndGet(time);		//save total time needed to do successful TTS request
	}
}
