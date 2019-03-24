package net.b07z.sepia.server.assist.tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;
import net.b07z.sepia.server.core.tools.Is;

public class Test_RssRomeTools {
	
	//Test a faulty RSS feed or improve reader
	public static void main(String[] args) {
		
		String url = "http://rss.golem.de/rss.php?feed=RSS2.0";
		//String url = "https://www.rollingstone.com/music/feed/";
		//String url = "https://www.wired.de/feed/latest";
		long tic = Debugger.tic();
		
		String statusLine = "";
		String errorRedirect = "";

		int N = 2;
		for (int i=0; i<N; i++){
			System.out.println("-- Test: " + (i+1));
			
			//Direct stream reader
			tic = Debugger.tic();
			try {
		        try (CloseableHttpClient client = HttpClients.createMinimal()) {
		        	HttpUriRequest request = new HttpGet(url);
		        	try (CloseableHttpResponse response = client.execute(request);
		        		InputStream stream = response.getEntity().getContent()) {
		
		        		//FEED STUFF
		        		statusLine = response.getStatusLine().toString();
		        		if (response.getStatusLine().getStatusCode() == 301){
		        			errorRedirect = response.getFirstHeader("Location").getValue();
		        			statusLine += (", NEW URI: " + errorRedirect);
		        			Debugger.println("RSS-FEED: '" + url + "' has REDIRECT to: " + errorRedirect, 1);
		        		}
		            	SyndFeedInput input = new SyndFeedInput();
		    			SyndFeed feed = input.build(new XmlReader(stream, true, "UTF-8"));
		    			System.out.println("Title: " + feed.getTitle());					//DEBUG
		    			System.out.println("Took: " + Debugger.toc(tic));					//DEBUG
		        	}
		        }
	        } catch (Exception e) {
				Debugger.println("Failed at URL: " + url 
						+ ", status: " + statusLine + ", msg: " + e.getMessage(), 1);
				e.printStackTrace();
	        }
			
			//Indirect stream reader
			tic = Debugger.tic();
			try {
       			//FEED STUFF
				String content = Connectors.apacheHttpGET(url, null).content;
				if (Is.notNullOrEmpty(content)){
					System.out.println(content.substring(0, 50));
					InputStream newStream = new ByteArrayInputStream(content.getBytes());
					
					SyndFeedInput input = new SyndFeedInput();
	    			SyndFeed feed = input.build(new XmlReader(newStream, true, "UTF-8"));
	    			System.out.println("Title: " + feed.getTitle());					//DEBUG
				}
    			System.out.println("Took: " + Debugger.toc(tic));					//DEBUG
	
	        }catch (Exception e){
				Debugger.println("Failed at URL: " + url 
						+ ", status: " + statusLine + ", msg: " + e.getMessage(), 1);
				e.printStackTrace();
	        }
		}
	}

}
