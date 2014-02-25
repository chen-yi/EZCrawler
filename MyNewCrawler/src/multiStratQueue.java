import java.io.BufferedWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

public class multiStratQueue {
    double MAXINVALIDRATIO = .5;
    int MINSIZEFORCLEAN = 1000;
    class URLInfo {
		int depth;
		URLInfo(int depth) {
			this.depth = depth;
		}
		
	};
    ArrayList<String> 	al = 		new ArrayList<String>();
    ArrayList<Boolean> 	valid = 	new ArrayList<Boolean>();
    ArrayList<Double> 	values = 	new ArrayList<Double>();
    ArrayList<URLInfo> 	metaInfo = 	new ArrayList<URLInfo>();
    
    public void printQueue(String filename) {
    	BufferedWriter output = null;
		try{
	         File f = new File(filename);
	         if (f.exists()) {
	        	 f.delete();
	         }
	         f.createNewFile();
	         output = new BufferedWriter(new FileWriter(f));
	         Iterator<String> it = al.iterator();
	         int i = 0;
	         while (it.hasNext()) {
	        	 String s = it.next();
	        	 if (valid.get(i)) {
	        		 output.write(s +"\t" + values.get(i) + "\n");
	        	 }
        		 i++;
	         }
			 output.close();					            
	      } catch(Exception e){
	    	 try {output.close();} catch (IOException eio) {};
	         e.printStackTrace();
	    }finally{
	    }
    }
    
    public boolean readQueue(String filename) {
    	boolean ok = true;
    	InputStream is = null; 

	    InputStreamReader isr = null;
	    BufferedReader br = null;
	    int read = 0;
	    try{
	         is = new FileInputStream(filename);
	         isr = new InputStreamReader(is);
	         br = new BufferedReader(isr);
	         String line = null;
	         while (null != (line = br.readLine())) {
	        	 try {

	        		 String[] components = line.trim().split("\\t");
	        		 if ((components == null) ||
	        				 (components.length != 2) ||
	        				 (components[0].trim().length() == 0) ||
	        				 (components[1].trim().length() == 0)) {
	        			 continue;
	        		 }
	        	    al.add(components[0]);
	        	    valid.add(true);
	        	    values.add(Double.parseDouble(components[1]));
	        	    
	        		read++;
	        	 } catch (Exception ignoreMinor) {	        		 
	        	 }
	         }  
	      }catch(Exception e){
	    	 ok = false;
	         e.printStackTrace();
	      }finally{
	    	  try {
			         if(is!=null)
			            is.close();
			         if(isr!=null)
			            isr.close();
			         if(br!=null)
			            br.close();
	    	  } catch (IOException ioe) {ok = false;} finally {}
	      } 	
	    System.out.println(read + " seeds read.");
		return ok;
    }
    
    public void lockExternally() {
    
    }
    public void unlockExternally() {
    
    }
    
    int invalid = 0;
    
    public String enqueue(String s, double value, int depth) {
    	al.add(s);
    	valid.add(true);
    	values.add(value);
    	metaInfo.add(new URLInfo(depth));
    	return s;
    }
    
    public String enqueue(String s, int depth) {
    	return enqueue(s, Double.MIN_VALUE, depth);
    }
    
    public String[] dequeue(String method) {
    	String[] ret = new String[3];
    	String s = null;
    	int depth = -1;
    	Double val = Double.MIN_VALUE;
    	
    	if (al.size() >  invalid) {
	    	if (method.equalsIgnoreCase("first")) {
	    		int start = 0;
	    		while (start < al.size()) {
	    		    if (valid.get(start)) {
	    		    	s = al.get(start);
	    		    	val = values.get(start);
	    		    	valid.set(start, false);
	    			    invalid++;
	    			    depth = metaInfo.get(start).depth;
	    		    	break;
	    		    }
	    		    start++;
	    		}
	    	}
	    	else if (method.equalsIgnoreCase("last")) {
	    		int start = al.size() - 1;
	    		while (start >= 0) {
	    		    if (valid.get(start)) {
	    		    	s = al.get(start);
	    		    	val = values.get(start);
	    		    	valid.set(start, false);
	    			    invalid++;
	    			    depth = metaInfo.get(start).depth;
	    		    	break;
	    		    }
	    		    start--;
	    		}
	    	}
	    	else if (method.equalsIgnoreCase("random")) {
				int start = (int) (Math.random() * al.size());
				int tried = 0;
				while ((tried++ < al.size()) && (!valid.get(start))) {
				    start = (start + 1) % al.size();
				}
				if (valid.get(start)) {
				    s = al.get(start);
				    val = values.get(start);
				    valid.set(start, false);
				    depth = metaInfo.get(start).depth;
				    invalid++;
				}
	    	} 
	    	else if (method.equalsIgnoreCase("bestofmany")) {
	    		int tried = 0;
	    		int many = 1 + (int) ((al.size() - invalid) * .1);
	    		if (many == 0) {
	    			many = 1;
	    		}
	    		
	    		int bestindex  = 0;
	    		while (!valid.get(bestindex)) {
	    			bestindex++;
	    			if (bestindex >= al.size()) {
	    				//bugbug
	    			}
    			}
	    		double bestval   = values.get(bestindex);
	    		int index = (int) (Math.random() * al.size());
	    		while (tried++ < many) {
	    			while (!valid.get(index)) {
	    				index = (index + 1) % al.size();
	    			}
	    			if (bestval < values.get(index)) {
	    				bestindex = index;
	    				bestval = values.get(index);
	    			}
	    			index = (index + 1) % al.size();
	    		}
	    		
				s = al.get(bestindex);
				val = values.get(bestindex);
	    		valid.set(bestindex, false);
	    		depth = metaInfo.get(bestindex).depth;
	    		invalid++;
	    	} else {
	    		String tmp[] = dequeue("first");
	    		s = tmp[0];
	    		val = Double.parseDouble(tmp[1]);
	    	}
    	}
    	int alsize = al.size();
		if ((alsize > MINSIZEFORCLEAN) && (invalid > alsize * MAXINVALIDRATIO)) {
				requestClean();
		}
		ret[0] = s;
		ret[1] = ""+val;
		ret[2] = ""+depth;
		if (Math.random() < .01) {
			System.out.println(s + " " + val);
		}
		return ret;
    }
    
    public void printStatus() {
		System.out.print("Size: " + al.size() + " invalid: " + invalid +" invalid ratio: ");
		System.out.print(invalid/(al.size() + 0.00000001));
		System.out.println();
    }
    
    private void requestClean() {
		if ((al.size() > MINSIZEFORCLEAN) && (invalid > al.size() * MAXINVALIDRATIO)) {
		    ArrayList<String> al1 = new ArrayList<String>();
		    ArrayList<Boolean> valid1 = new ArrayList<Boolean>();
		    ArrayList<Double> values1 = 	new ArrayList<Double>();
		    for (int i = 0; i < al.size(); i++) {
		    	if (valid.get(i)) {
		    		al1.add(al.get(i));
		    		valid1.add(true);
		    		values1.add(values.get(i));
		    	}
		    }
		    al = al1;
		    valid = valid1;
		    invalid = 0;
		    values = values1;
		}
    }
    
    public static void main(String[] args) {
    	multiStratQueue Q = new multiStratQueue();
    	for (int i = 0; i < 1000; i++) {
    		Double d = Math.random();
    		Q.enqueue(""+d, d, 0);
    	}
    	
    	for (int i = 0; i < 1005; i++) {
    		System.out.println(i + " " + Q.dequeue("bestofmany"));
    	}
    }
}
