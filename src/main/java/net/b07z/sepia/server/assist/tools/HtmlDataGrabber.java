package net.b07z.sepia.server.assist.tools;

import java.util.regex.Pattern;

import net.b07z.sepia.server.core.tools.Connectors;
import net.b07z.sepia.server.core.tools.Debugger;

/**
 * Class to try and load specific data from a web-site. If it works depends strongly on the site and how the content is generated.
 *   
 * @author Florian Quirin
 *
 */
public class HtmlDataGrabber {
	
	/**
	 * Get data of a static web-site by specifying start and end point marks.
	 */
	public static String getStaticPageData(String url, String beginData, String endData){
		String webSite = Connectors.simpleHtmlGet(url);
		String data = "";
		try{
			data = webSite.replaceAll(".*?" + Pattern.quote(beginData), "").trim();
			data = webSite.replaceAll(Pattern.quote(endData) + ".*", "").trim();
			return data;
			
		}catch (Exception e){
			Debugger.println("HtmlDataGrabber - getPageData() failed! No data could be extracted from url: " + url, 1);
			Debugger.printStackTrace(e, 3);
			return "";
		}
	}
	
	public static String getDynamicPageData(String url, String beginData, String endData){
		String data = "";
		//TODO: implement
		return data;
	}
}
