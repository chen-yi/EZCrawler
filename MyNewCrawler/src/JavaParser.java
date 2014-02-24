import org.jsoup.Jsoup;
//import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

public class JavaParser {
	public  Document d;
	boolean status = false;
	
	public  boolean process(String url) {
		 try {
			d = Jsoup.connect(url).get();
			status = true;
		} catch (Exception e) {
			status = false;
		}
		return status;
	}
	
	public ArrayList<String> getLinks(String grepFilter, Boolean ignoreGrepFilter) {
		ArrayList<String> al = new ArrayList<String>();
		Hashtable<String, Integer> h = new Hashtable<String, Integer>();
		String[] substrs = grepFilter.split("\\s");
		if (status) {
			Elements links = d.select("a[href]");
	        for (Element link : links) {
	        	if (!ignoreGrepFilter) {
	        		for (int i = 0; i < substrs.length; i++) {
	        			String tmp = link.attr("abs:href").toString().toLowerCase();
	        			if (h.containsKey(link.attr("abs:href").toString())) {
	        				continue;
	        			}
	        			if ((substrs[i].length() > 0) && 
	        					(tmp.contains(substrs[i].toLowerCase()))){
	        				al.add(link.attr("abs:href").toString());
	        				h.put(link.attr("abs:href").toString(), 1);
	        				break;
	        			}
	        		}
	        	} else {
	        		if (h.containsKey(link.attr("abs:href").toString())) {
        				continue;
        			}
	        		al.add(link.attr("abs:href").toString());
    				h.put(link.attr("abs:href").toString(), 1);
	        	}
	        }
		}
		return al;
	}
	
	public  String getText() {
		if (!status) {
			return null;
		}
		return d.body().text();
	}
	
	public static void main (String[] args) {
		JavaParser j = new JavaParser();
		String u = "http://www.bing.com/news/search?q=accident";
		if (j.process(u)) {
			//System.out.println(j.getText().toLowerCase());
			ArrayList<String> al = j.getLinks("foxnews examiner wsbt sportingnews", false);
			Iterator<String> it = al.iterator();
			while (it.hasNext()) {
				System.out.println(it.next());
			}
		}
	}
}
