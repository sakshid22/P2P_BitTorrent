package cnt5106p2p.configparser;

import java.util.TreeMap;
import java.util.logging.Logger;

import java.io.*;

public class PeerInfoConfigParser{

	// private HashMap<String, PeerInfoConfigParser> peer = new HashMap<String,
	// PeerInfoConfigParser>();
	public static final String CONFIG_FILE = new File(System.getProperty("user.dir")).getParent() + "/configuration/PeerInfo.cfg";
	private static final Logger LOGGER = Logger.getLogger(PeerInfoConfigParser.class.getName());

	public PeerInfoConfigParser() {

	}

	/**
	 * @return HashMap<String, String> of property name and values
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Integer, PeerInfoConfig> parseConfig() throws Exception {
		TreeMap<Integer, PeerInfoConfig> peerInfoMap = new TreeMap<Integer, PeerInfoConfig>();
		File file1 = new File(PeerInfoConfigParser.CONFIG_FILE);

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(file1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line;
		try {
			// read file line by line
			while ((line = reader.readLine()) != null) {
				// split line by spaces and insert into an array
				String[] delim = line.split(" ");
				// get ID (key)
				Integer key_name = Integer.parseInt(delim[0]);
				// host name
				String hostName = delim[1];

				// incoming port and convert to an int
				String portNumber = delim[2];
				int portNumberInt = Integer.parseInt(portNumber);

				// get if it has file or not
				String hasFileBit = delim[3];
				// convert str to bool
				boolean hasFile = !"0".equals(hasFileBit);
				// create new obj for peer by getting the three attributes
				// following the ID, that is:
				// host name, incoming port, has file or not
				PeerInfoConfig peerInfo = new PeerInfoConfig(hostName, portNumberInt, hasFile);
				peerInfoMap.put(key_name, peerInfo);
			}
			reader.close();

		} catch (Exception e) {
			LOGGER.severe("Error reading Common config file on line ");
			throw e;
		}
		// returns the peerInfoMap
		return peerInfoMap;

	}

}
