import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    int downLoaded = 0;
    private ReentrantLock L = new ReentrantLock();
    private static XMLReader reader = null;
    private String[] avoidURLRegexes = null;
    private String[] preferredURLRegexes = null;
    
    Hashtable<String, Boolean> doneH = null;
    multiStratQueue todoQ = null;
	    
    public CleanerMyCrawler() {
		doneH = new Hashtable<String, Boolean>();
		todoQ = new multiStratQueue();
		reader = new XMLReader();
    }
    
    private void initSetup () {
		directory = reader.get("configuration|DownloadDirectory");
    	for (int i = 0; i < Integer.parseInt(reader.get("configuration|NumberOfDownloadSubDirs")); i++) {
		    File theDir = new File(directory + "/" + i);
		    if (!theDir.exists()) {
		    	theDir.mkdir();  
		    }
    	}
 		avoidURLRegexes = reader.get("configuration|AvoidURLRegexes").split("\\s+");
 		preferredURLRegexes = reader.get("configuration|PreferredURLRegexes").split("\\s+");   	
    }
        
    
    // multithreading mechanism implemented
    private class Worker implements Runnable {
		Hashtable<String, Boolean> doneH = null;
		multiStratQueue todoQ = null;
		BufferedWriter dictionaryOutput = null;
		int myId = -1;
		
		public Worker(int id, multiStratQueue q, Hashtable<String, Boolean> h) {
			myId = id;
			todoQ = q;
		    doneH = h;		    
		}		
				
		public void run() {
			int  depthOfParent = -1;
			// each thread has a dictionary file
			String dictionaryFile = reader.get("configuration|PathToOutputDictionary") +
					reader.get("configuration|StemForDictionary") + myId + ".txt";								
			try {
				File file = new File(dictionaryFile);
			    if (!file.exists()) {
					file.createNewFile();	
					dictionaryOutput = new BufferedWriter(new FileWriter(dictionaryFile));	
			    } else {
			    	return; // in an error situation just give up.
			    }
			} catch (Exception e) {
				System.out.println("Could not open and write " + dictionaryFile + " for url");
			}
			
			boolean stop = false;
		    while (!stop) {
				String url = null;
				int tried = 0;
				JavaParser jp = new JavaParser();
				while ((tried++ < Integer.parseInt(reader.get("configuration|NumberOfTriesBeforeGivingUpOnQueue")) && 
						(url == null))) {
				    L.lock();
				    String[] tmp = todoQ.dequeue("bestofmany");
				    url = tmp[0];
				    depthOfParent = Integer.parseInt(tmp[2]);		    		
				    if (depthOfParent >= Integer.parseInt(reader.get("configuration|MaxDepth"))) {
				    	url = null;
				    }
				    L.unlock();
				    try {
				    	Thread.sleep(Integer.parseInt(reader.get("configuration|PolitenessInMS")));
				    } catch (Exception e) {
				    }
				}
				
				if (url == null) {
				    System.out.println("I got nothing. Exiting thread.\n");
				    stop = true;
				} else {
				    String md5 = md5Hash(url);
				    int h = 0; 
				    for (int i = SUBDIRMININDEX; i <= SUBDIRMAXINDEX; i++) {
				    	h += md5.charAt(i);
				    }
				    h = h % Integer.parseInt(reader.get("configuration|NumberOfDownloadSubDirs"));
		
				    String filename = directory + h + "/" + md5 + ".txt";
				    System.out.println("Creating file: " + filename);			    
				    tried = 0;
				    while ((tried++ < Integer.parseInt(reader.get("configuration|NumberOfTriesOnURLBeforeGiveUp")))) {
				    	System.out.println("into while: tried = " + tried + " myId: " + myId);
						if (downLoaded >= Integer.parseInt(reader.get("configuration|MaxURLsApproximately"))) {
							continue;
						}
						System.out.println("Before JP process");
						if (jp.process(url)) {
							downLoaded++;
							System.out.println("Downloaded " + downLoaded);
							try {								
								dictionaryOutput.write(url + "\t" + filename + "\n");
								dictionaryOutput.flush();
								System.out.println("writing dictionary " + myId);
							} catch (Exception e) {
							    System.out.println("Could not write " + dictionaryFile + " for url");
							}
							
						    System.out.println(url + " processed successfully. Tried " + tried + " times.");
						    ArrayList<String> links = jp.getLinks("IGNORE THIS", true);
						    Iterator<String> it = links.iterator();
						    int linksAdded = 0;
						    L.lock();
						    while ((it.hasNext()) && (linksAdded < Integer.parseInt(reader.get("configuration|MaxBreadth")))) {
								String s = it.next();
								if ((s.length() == 0) || (s.indexOf('#') == 0)) {
								    continue;
								}
								if (mustAvoid(s)) {
									continue;
								}								
								String hashV = md5Hash(s);
								double newVal = computeURLValue(s) + computeTextValue(jp.getText());
								if ((newVal > 0.0) && (!doneH.containsKey(hashV))) {
								    linksAdded++;
								    System.out.println("linksAdded: " + linksAdded);
								    doneH.put(hashV, true);
								    todoQ.enqueue(s, newVal, depthOfParent+1);
								    if (depthOfParent > 1) {
								    	System.out.println("New Depth " + depthOfParent + " " + s + " added to queue.\n");
								    }
								}
										
								try {
								    File file = new File(filename);
								    if (!file.exists()) {
										file.createNewFile();	
										BufferedWriter output = new BufferedWriter(new FileWriter(filename));
										output.write(url.length() + "\t" + jp.getText().length() + "\t" + url + "\n");
										output.write("<CRAWLERDOWNLOADEDHTML>" + "\n");
										if (reader.get("configuration|OutputFormat").equals("TEXTONLY")) {
											output.write("NOTREQUESTED_NOTREQUESTED" + "\n");
										} else {
											output.write(jp.getHTML() + "\n");
										}
										output.write("</CRAWLERDOWNLOADEDHTML>" + "\n");
										output.write("<CRAWLERPARSETEXT>" + "\n");
										if (reader.get("configuration|OutputFormat").equals("HTMLONLY")) {
											output.write("NOTREQUESTED_NOTREQUESTED" + "\n");
										} else {
											output.write(jp.getText() + "\n");
										}
										output.write("</CRAWLERPARSETEXT>" + "\n");
										output.close();
								    }
								} catch (Exception e) {
								    System.out.println("Could not open " + filename + " for url");
								}
						    }
						    L.unlock();
						    break;
						} else {
							System.out.println(url + " was not processed successfully. Tried " + tried + " times.");
						    try {
						    	Thread.sleep(Integer.parseInt(reader.get("configuration|PolitenessInMS")));
						    } catch (Exception e) {
						    }
						}
				    }
				    if (tried >= Integer.parseInt(reader.get("configuration|NumberOfTriesOnURLBeforeGiveUp"))) {
				    	System.out.println("Fail to download "+ url);
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
			if (dictionaryOutput != null) {
				try {					
					dictionaryOutput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		private boolean mustAvoid(String s) {
			for (int i = 0; i < avoidURLRegexes.length; i++) {
				if ((avoidURLRegexes[i].length() == 0) || (avoidURLRegexes[i].indexOf('#') == 0)) {
					continue;
				}
				Pattern pattern = Pattern.compile(avoidURLRegexes[i]);
				Matcher matcher = pattern.matcher(s);
				if (matcher.find()) {
				    return true;
				}
			}
			return false;
		}
    }
    
         
    public void msSleepSafely(int ms) {
		try {
		    Thread.sleep(ms);
		} catch (Exception e) {
		}
    }
    
    private double computeURLValue(String s) {  
	    for (int i = 0; i < preferredURLRegexes.length; i++) {
			if (preferredURLRegexes[i].indexOf('#') == 0) {
				continue;
			}
			Pattern pattern = Pattern.compile(preferredURLRegexes[i]);
			Matcher matcher = pattern.matcher(s);
			if (matcher.find()) {
			    return 1.0;
			}
		}
	    return 0.0;
	}
    
    private double computeTextValue(String s) {
	    double ret = 0.0;    
		String[] strings = reader.get("configuration|PreferedURLText").split("\\s+");
		for (int i=0; i<strings.length; i++) {	
			String key = strings[i++];
			Double value = Double.parseDouble(strings[i]);
			Pattern pattern = Pattern.compile(key);
			Matcher matcher = pattern.matcher(s);
			if(matcher.find()){
				ret += value;
			}					
		}		
	    return ret;
	}
    
    private String md5Hash(String s) {
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
    
    private void dowork() {
		int i = 0;
		BufferedReader br = null;
		try {
		    br = new BufferedReader(new FileReader(reader.get("configuration|CrawlSeedFile")));
		    String url = null;
		    while ((url = br.readLine()) != null) {
				url = url.trim();
				if ((url.length() == 0) || (url.indexOf('#') == 0)){
				    continue;
				}
				todoQ.enqueue(url, computeURLValue(url), 0);
				doneH.put(md5Hash(url), true);
				// System.out.println(url + " added.\n");
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
		c.initSetup();
		c.dowork();
    }
} 

