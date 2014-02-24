import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;


public class XMLReader {
	final private String filename = "config.xml";
	private Document config;
	private Element root;
	public SAXReader reader = new SAXReader();
	
	
	public XMLReader() {
		config = null;
	}
	
	/*
	 * New interface for all value
	 * If content more than one line, divide them by adding " "
	 * and trim whitespace before use
	 * @param destination should start from root and separate by '|'
	 */
	public String get(String destination){
		if (config == null) {
			try {
				config = reader.read(filename);
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//root = config.getRootElement();		
		}
		String[] dirs = destination.split("\\|");
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<dirs.length; i++){
			sb.append("/");
			sb.append(dirs[i]);
		}
		String path = sb.toString();
		//System.out.println(path);
		Node node =  config.selectSingleNode(path);
		String result = node.getText();
		result = result.replaceAll("\\n", " ");
		result = result.trim();
		return result;	
	}
	
	
	
	public int getIntValue(String element){
		return Integer.parseInt(root.elementText(element));
	}
	
	public String getStringValue(String element){
		return root.elementText(element);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getListValue(String element, String subelement){
		Element e = root.element(element);
		ArrayList<String> strings = new ArrayList<String>();
		Iterator<Element> it = e.elementIterator(subelement);		
		while(it.hasNext()){
			Element temp = it.next();
			if(temp.getText() != ""){
				strings.add(temp.getText());
			}		
		}
		return strings;
	}
	
	public Hashtable<String, Double> getListPairValue(String element, String subelement){
		Hashtable<String, Double> pairs = new Hashtable<String, Double>();
		ArrayList<String> strings = getListValue(element, subelement);
		for(int i=0; i<strings.size(); i++){
			StringTokenizer st = new StringTokenizer(strings.get(i));
			String key = "";
			Double value = 0.0;
			while(st.hasMoreTokens()){
				key = st.nextToken("\t");
				value = Double.parseDouble(st.nextToken("\t"));				
			}
			pairs.put(key, value);
		}
		return pairs;
	}
	
//	public static void main(String[] args){
//		XMLReader reader;
//		try {
//			reader = new XMLReader();
//			String test = reader.get("configuration|PreferedURLRegexes");
//			//System.out.println(test);
//			
//			String[] a = test.split("\\s+");
//			for(int i=0;i<a.length;i++){
//				//System.out.println(i);
//				System.out.println(a[i]);				
//			}
//		} catch (DocumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}	
//	}
	
//	public static void main(String[] args){		
//		try {
//			Hashtable<String, Double> hs = reader.getListPairValue("PreferedURLText", "Text");
//			Enumeration<String> items = hs.keys();
//			while(items.hasMoreElements()){
//				String key = items.nextElement();
//				System.out.println(key);
//				Double value = hs.get(key);
//				System.out.println(value);
//			}
//		} catch (DocumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}		
//		
//	}
	
}


