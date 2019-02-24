package net.b07z.sepia.server.assist.tools;

import java.io.InputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import net.b07z.sepia.server.core.tools.Debugger;

public class Test_RssRomeTools {

	//Test a faulty RSS feed or imrpve reader
	public static void main(String[] args) {
		
		String url = "https://www.rollingstone.com/music/feed/";
		
		//TODO: try
		//String xml = "<?xml ...";
		//xml = xml.trim().replaceFirst("^([\\W]+)<","<");
		//stream string ...
		
		String statusLine = "";
		String errorRedirect = "";

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
	        	}
	        }
        } catch (Exception e) {
			Debugger.println("Failed at URL: " + url 
					+ ", status: " + statusLine + ", msg: " + e.getMessage(), 1);
			e.printStackTrace();
        }

	}

}
