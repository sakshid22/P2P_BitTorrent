package cnt5106p2p.configparser;

import java.util.HashMap;

public abstract class CommonConfig {

	public abstract <T> HashMap<T, T> parseConfig(String filename) ;
	
	public static final String NUM_PREF_NEIGHBHORS = "NumberOfPreferredNeighbors"; 
	public static final String UNCHOKE_INTERVAL = "UnchokingInterval";
	public static final String OPTIMISTIC_UNCHOKE_INTERVAL = "OptimisticUnchokingInterval";
	public static final String FILE_NAME = "FileName";
	public static final String FILE_SIZE = "FileSize";
	public static final String PIECE_SIZE = "PieceSize";
}
