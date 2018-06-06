package cnt5106p2p.configparser;

import java.util.HashMap;
import java.util.logging.Logger;

import java.io.*;


public class CommonConfigParser{

	public static final String CONFIG_FILE = new File (System.getProperty("user.dir")).getParent() + "/configuration/Common.cfg";
	private HashMap<String, String> common = new HashMap<String, String>();
	private static final Logger LOGGER = Logger.getLogger(CommonConfigParser.class.getName());
	
	public CommonConfigParser() {
	
	}
	
	public HashMap<String, String> parseConfig() {
		File file1 = new File(CommonConfigParser.CONFIG_FILE);
		FileInputStream stream = null;
        try {
            stream = new FileInputStream(file1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        
        String line; 
        
        try {
        		while((line  = reader.readLine()) != null ) {
        			String [] delim = line.split(" ");
        			String key_name = delim[0];
    				String value = delim[1];
    				this.common.put(key_name, value);
        		}
    			reader.close();

        }
        catch(Exception e) {
        		LOGGER.severe("Error reading Common config file on line ");
        }
        return this.common;
	}
	

	public int getNumberOfPrefferedNeighbors() {
		return Integer.parseInt(this.common.get("NumberOfPreferredNeighbors"));
	}

	public int getUnchokingInterval() {
		return Integer.parseInt(this.common.get("UnchokingInterval"))*1000;
	}
	
	public int getOptimisticUnchokingInterval() {
		return Integer.parseInt(this.common.get("OptimisticUnchokingInterval"))* 1000;
	}
	
	public String getFileName() {
		return this.common.get("FileName");
	}
	
	public int getFileSize() {
		return Integer.parseInt(this.common.get("FileSize"));
	}
	
	public int getPieceSize() {
		return Integer.parseInt(this.common.get("PieceSize"));
	}
	
}
