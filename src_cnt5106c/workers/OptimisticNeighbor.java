package cnt5106p2p.workers;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import cnt5106p2p.DataHolder;
import cnt5106p2p.ThreadLoggerStrap;
import cnt5106p2p.Util;
import cnt5106p2p.messages.P2PMessages;

public class OptimisticNeighbor extends ThreadLoggerStrap implements Runnable {

	private HashMap<Integer, LinkedBlockingQueue<P2PMessages>> seederQueues;
	private boolean taskComplete;
	private static final Logger LOGGER = Logger.getLogger("cnt5106p2p");
	public OptimisticNeighbor(HashMap<Integer, LinkedBlockingQueue<P2PMessages>> seederQueues) {
		super(MessageFormat.format("[OptimisticNeighbhorThread:{0}]:", DataHolder.peerId));
		this.seederQueues = seederQueues;
		this.taskComplete = false;
	}

	
	public void setTaskComplete(boolean taskComplete) {
		LOGGER.info(MessageFormat.format("{0} task completed: {1}", threadName, taskComplete));
		this.taskComplete = taskComplete;
	}
	
	public boolean getTaskComplete() {
		return taskComplete;
	}

	
	@Override
	public void run() {
		LOGGER.info(MessageFormat.format("{0} thread started", threadName));
		optimisticUnchoke();
		LOGGER.info(MessageFormat.format("{0} exiting of peerId: {1}", threadName, DataHolder.peerId));
	}

	public void optimisticUnchoke() {
		while (!getTaskComplete()) {
			try {
				if (!DataHolder.getHaveFullFile()){
					LOGGER.info("Going to sleep for " + DataHolder.optUnchokeInterval + ", no full file");
					Thread.sleep(DataHolder.optUnchokeInterval);
				}
				int optimisticallyUnchokedPeer = -1;
				optimisticallyUnchokedPeer = DataHolder.genOptimisticPreferredNeighbors();
				if (optimisticallyUnchokedPeer != -1){
					DataHolder.setOptimisticNeighbor(optimisticallyUnchokedPeer);
					seederQueues.get(optimisticallyUnchokedPeer).put(P2PMessages.genUnChokeMessages());
					LOGGER.info(MessageFormat.format("{0} Peer [{1}] has the optimistically unchoked neighbor [{2}]",
							threadName, DataHolder.peerId, optimisticallyUnchokedPeer));
				}
				if (DataHolder.getHaveFullFile()){
					LOGGER.info("Going to sleep for " + DataHolder.optUnchokeInterval + ", have full file");
					Thread.sleep(DataHolder.optUnchokeInterval);
				}
			} catch (InterruptedException e) {
				LOGGER.warning(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
			}
		}
	}

}
