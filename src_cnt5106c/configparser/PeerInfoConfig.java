package cnt5106p2p.configparser;

public class PeerInfoConfig {
	public String hostName;
	public int listeningPort;
	public boolean hasFile;

	// constructor for PeerInfo file which has all attributes except ID
	// ID will be used as the key in the hashmap
	public PeerInfoConfig(String hostName, int listeningPort, boolean hasFile) {
		this.hostName = hostName;
		this.listeningPort = listeningPort;
		this.hasFile = hasFile;
	}
}
