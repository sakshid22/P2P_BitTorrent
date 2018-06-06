package cnt5106p2p.workers;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.HashSet;
import cnt5106p2p.DataHolder;
import cnt5106p2p.ThreadLoggerStrap;
import cnt5106p2p.Util;
import cnt5106p2p.messages.P2PMessages;

public class PreferredNeighbors extends ThreadLoggerStrap implements Runnable {
	private HashMap<Integer, LinkedBlockingQueue<P2PMessages>> seederQueues;
	private boolean taskComplete;
	private static final Logger LOGGER = Logger.getLogger("cnt5106p2p");

	
	// private static final Logger LOGGER =
	// Logger.getLogger(PreferedNeighbhors.class.getName());

	public PreferredNeighbors(HashMap<Integer, LinkedBlockingQueue<P2PMessages>> seedersQueues) {
		super(MessageFormat.format("[PreferredNeighorThread:{0}]: ", DataHolder.peerId));
		this.seederQueues = seedersQueues;
		taskComplete = false;
	}
	
	
	public void setTaskComplete(boolean taskComplete) {
		this.taskComplete = taskComplete;
	}
	
	public boolean getTaskComplete() {
		return taskComplete;
	}


	@Override
	public void run() {
		LOGGER.info(MessageFormat.format("{0} thread started", threadName));
		preferredNeighbors();
		LOGGER.info(MessageFormat.format("{0} exiting of peerId: {1}", threadName, DataHolder.peerId));
	}
	
	public void preferredNeighbors() {
		HashSet<Integer> prefferedNeighbhors = new HashSet<>();
		while (!getTaskComplete()) {
			HashSet<Integer> curPreferredNeighbors = null;
			HashSet<Integer> newPreferredNeighbhors = null;
			HashSet<Integer> oldPrefferedNeighbhors = null;
			try {
				if (!DataHolder.getHaveFullFile()){
					Thread.sleep(DataHolder.unchokeInterval);
					LOGGER.info("Before checking preferredNeighbors, going to sleep for " + DataHolder.unchokeInterval);
				}
				
				if(DataHolder.getHaveFullFile()) {
					curPreferredNeighbors = DataHolder.genRandomPrefferedNeighbhors();
				}else {
					curPreferredNeighbors = DataHolder.genPrefferedNeighbors();
				}
				if (prefferedNeighbhors.size() == 0) {
					//first time around make old equal to new neighbors
					prefferedNeighbhors = curPreferredNeighbors;
					newPreferredNeighbhors = curPreferredNeighbors;
					oldPrefferedNeighbhors = new HashSet<>();
				}
				else {
					newPreferredNeighbhors = new HashSet<>(curPreferredNeighbors);
					newPreferredNeighbhors.removeAll(prefferedNeighbhors);
					oldPrefferedNeighbhors = new HashSet<>(prefferedNeighbhors);
					oldPrefferedNeighbhors.removeAll(curPreferredNeighbors);
					prefferedNeighbhors = curPreferredNeighbors;
				}

				for (int peerId : newPreferredNeighbhors) {
					seederQueues.get(peerId).put(P2PMessages.genUnChokeMessages());
					LOGGER.info(MessageFormat.format("{0} unchoked peer: {1}", threadName, peerId));
				}

				for (int peerId : oldPrefferedNeighbhors) {
					seederQueues.get(peerId).put(P2PMessages.genChokeMessage());
					LOGGER.info(MessageFormat.format("{0} choked peer: {1}", threadName, peerId));
				}
				
				StringBuilder prefNeighbors = new StringBuilder();
				for (int peerId : curPreferredNeighbors) {
					prefNeighbors.append(MessageFormat.format("{0},", peerId));
				}
				
				//correction to see if prefNeighbors exists
				if(prefNeighbors.length() != 0) {
				prefNeighbors.replace(prefNeighbors.length() - 1, prefNeighbors.length(), "");
				}
				LOGGER.info(MessageFormat.format("Peer [{0}] has the preferred neighbors [{1}]", DataHolder.peerId,
						prefNeighbors.toString()));
				if (DataHolder.getHaveFullFile()){
					
					Thread.sleep(DataHolder.unchokeInterval);
				}
			} catch (InterruptedException e) {
				LOGGER.warning(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
			}

		}
	}
}
