package cnt5106p2p;

import java.util.logging.Logger;

public abstract class ThreadLoggerStrap {

	public final Logger LOGGER = Logger.getLogger(this.getClass().getSimpleName());
	public final String threadName;
	
	public ThreadLoggerStrap( String threadName) {
		this.threadName = threadName;
	}
}
