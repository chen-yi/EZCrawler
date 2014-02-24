import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;

public class CleanerMyCrawler {
    String directory = null;
    final int SUBDIRMININDEX = 0;
    final int SUBDIRMAXINDEX = 20;
    private java.util.concurrent.locks.ReentrantLock L = new java.util.concurrent.locks.ReentrantLock();
    private static XMLReader reader = null;
    
    public XMLReader getConfigReader() {
    	return reader;
    }
    
    private void checkAndCreateDirectories () {
    	for (int i = 0; i < Integer.parseInt(reader.get("configuration|NumberOfDownloadSubDirs")); i++) {
		    File theDir = new File(directory + "/" + i);
		    if (!theDir.exists()) {
		    	theDir.mkdir();  
		    }
    	}
    }
    
    public String md5Hash(String s) {
		String ret = "MD5FAILED"; 
		try {
		    MessageDigest md = MessageDigest.getInstance("MD5");
		    StringBuffer sb = new StringBuffer();
		    for (byte b : md.digest(s.getBytes("UTF-8"))) {
		    	sb.append(Integer.toHexString((int) (b & 0xff)));
		    }
		    ret = sb.toString();
		} catch (Exception e) {
		}
		return ret;
    }
    
    private class Worker implements Runnable {
		Hashtable<String, Boolean> doneH = null;
		multiStratQueue todoQ = null;
		
		public Worker(int id, multiStratQueue q, Hashtable<String, Boolean> h) {
		    doneH = h;
		    todoQ = q;
		}
		
		private double computeValue(String s) {
		    double ret = 5;
		    return ret;
		}
		
		public void run() {
		    while (true) {
				String url = null;
				int tried = 0;
				JavaParser jp = new JavaParser();
				while ((tried++ < Integer.parseInt(reader.get("configuration|NumberOfTriesBeforeGivingUpOnQueue")) && 
						(url == null))) {
				    L.lock();
				    String[] tmp = todoQ.dequeue("bestofmany");
				    url = tmp[0];
				    System.out.println("URL found in queue: " + url);
				    L.unlock();
				    try {
				    	Thread.sleep(Integer.parseInt(reader.get("configuration|PolitenessInMS")));
				    } catch (Exception e) {
				    }
				}
				
				if (url == null) {
				    System.out.println("I got nothing.\n");
				    return;
				} else {
				    String md5 = md5Hash(url);
				    int h = 0;//BUGBUG fix this hashing function if severely unbalanced.
				    for (int i = SUBDIRMININDEX; i <= SUBDIRMAXINDEX; i++) {
				    	h += md5.charAt(i);
				    }
				    h = h % Integer.parseInt(reader.get("configuration|NumberOfDownloadSubDirs"));
		
				    String filename = directory + h + "/" + md5 + ".txt";
				    System.out.println("Creating file: " + filename);
				    tried = 0;
				    while ((tried < Integer.parseInt(reader.get("configuration|NumberOfTriesOnURLBeforeGiveUp")))) {	
						tried++;
						if (jp.process(url)) {
						    System.out.println(url + " processed successfully. Tried " + tried + " times.");
						    ArrayList<String> al = jp.getLinks("IGNORE THIS", true);
						    Iterator<String> it = al.iterator();
						    int linksAdded = 0;
						    while ((it.hasNext()) && (linksAdded < Integer.parseInt(reader.get("configuration|MaxBreadth")))) {
								String s = it.next();
								String hashv = md5Hash(s);
								double newval = computeValue(s);
								L.lock();
								if ((newval > 0) && (!doneH.containsKey(hashv))) {
								    linksAdded++;
								    doneH.put(hashv, true);
								    todoQ.enqueue(s, newval);
								    System.out.println(s + " added to queue.\n");
								}
								L.unlock();		
								try {
								    File file = new File(filename);
								    if (!file.exists()) {
										file.createNewFile();	
										BufferedWriter output = new BufferedWriter(new FileWriter(file));
										output.write(url.length() + "\t" + jp.getText().length() + "\t" + url + "\n");
										output.write(jp.getText()+"\n");/*use Yi's html parser*/
										output.close();
								    }
								} catch (Exception e) {
								    System.out.println("Could not open " + filename + " for url");
								}
						    }
						} else {
							System.out.println(url + " was not processed successfully. Tried " + tried + " times.");
						    try {
						    	Thread.sleep(Integer.parseInt(reader.get("configuration|PolitenessInMS")));
						    } catch (Exception e) {
						    }
						}
				    }
				    if (tried >= Integer.parseInt(reader.get("configuration|NumberOfTriesOnURLBeforeGiveUp"))) {
				    	try {
						    File file = new File(filename);
						    if (!file.exists()) {
								file.createNewFile();	
								BufferedWriter output = new BufferedWriter(new FileWriter(file));
								output.write(url + " could not be downloaded.\n");
								output.close();
						    }
						} catch (Exception e) {
						    System.out.println("Could not open " + filename + " for url");
						}
				    }
				}
		    }
		}
    }
    
    Hashtable<String, Boolean> doneH = null;
    multiStratQueue todoQ = null;
	    
    public CleanerMyCrawler() {
		doneH = new Hashtable<String, Boolean>();
		todoQ = new multiStratQueue();
    }
	 	
    public String zip () {
		String ret = "";
		for (int i = 0; i < 5; i++) {
		    ret = ret + (int) (10 * Math.random());
		}
		return ret;	
    }
 	
       
    public void msSleepSafely(int MS) {
		try {
		    Thread.sleep(MS);
		} catch (Exception e) {
		}
    }
    
    public void dowork() {
		int i = 0;
		BufferedReader br = null;
		try {
		    br = new BufferedReader(new FileReader(reader.get("configuration|CrawlSeedFile")));
		    String url = null;
		    while ((url = br.readLine()) != null) {
				url = url.trim();
				if (url.length()==0) {
				    continue;
				}
				if (url.indexOf('#') == 0) {
				    continue;
				}
				todoQ.enqueue(url, Integer.parseInt(reader.get("configuration|DefaultWeighttOfSeed")));
				doneH.put(md5Hash(url), true);
				System.out.println(url + " added.\n");
		    }
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    try {
				if (br != null) {
				    br.close();
				}
		    } catch (IOException e) {
		    }
		}
	
		for (i = 0; i < Integer.parseInt(reader.get("configuration|NumberOfThreads")); i++) {
		    System.out.println("Starting worker " + i);
		    new Thread(new Worker(i, todoQ, doneH), "My Thread").start();
		    msSleepSafely(Integer.parseInt(reader.get("configuration|PolitenessInMS")));
		}
    }
	
    public static void main(String[] args) {
		CleanerMyCrawler c = new CleanerMyCrawler();
		reader = new XMLReader();
		c.directory = reader.get("configuration|DownloadDirectory");
		c.checkAndCreateDirectories();
		c.dowork();
    }
} 

