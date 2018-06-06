package cnt5106p2p.configparser;

import java.util.HashMap;


public abstract class ConfigParser {
	public abstract <T> HashMap<T, T> parseConfig() ;

}