package cnt5106p2p.workers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import cnt5106p2p.DataHolder;
import cnt5106p2p.ThreadLoggerStrap;
import cnt5106p2p.Util;
import cnt5106p2p.messages.P2PMessages;
import cnt5106p2p.messages.PieceMessage;
import cnt5106p2p.messages.RequestMessage;

public class Seeder extends ThreadLoggerStrap implements Runnable {

	private final LinkedBlockingQueue<P2PMessages> queue;
	// private static final Logger LOGGER =
	// Logger.getLogger(Leecher.class.getName());
	private static final Logger LOGGER = Logger.getLogger("cnt5106p2p");
	private final Communicator communicator;
	private boolean taskComplete;

	public Seeder(int targetPeerId, LinkedBlockingQueue<P2PMessages> p2pMessageQueue, Communicator comm) {
		super(MessageFormat.format("[Seeder ID : {0}]", targetPeerId));
		this.queue = p2pMessageQueue;
		this.communicator = comm;
		this.taskComplete = false;
	}

	public int getTargetPeerId() {
		return communicator.getTargetPeerID();
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
		while (!getTaskComplete()) {
			try {
				processMessage(queue.take());
			} catch (InterruptedException e) {
				LOGGER.warning(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
			} catch (ClassNotFoundException e) {
				LOGGER.warning(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
			} catch (IOException e) {
				LOGGER.warning(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
			}
		}
	}

	public void processMessage(P2PMessages p2pMessage) throws ClassNotFoundException, IOException {
		P2PMessages responseMessage = null;
		switch (Util.getMessageType(p2pMessage.flag)) {
		case CHOKE:
			responseMessage = P2PMessages.genChokeMessage();
			break;
		case UNCHOKE:
			responseMessage = P2PMessages.genUnChokeMessages();
			break;
		case HAVE:
			// special case when Seeder receives a message stating now seeder
			// has a piece available
			// from the same peer where seed lies. If a have message is received
			// at socket it is relayed
			// to Leecher. Here Seeder needs to update it's target host that it
			// has this piece available now
			responseMessage = p2pMessage;
			break;
		case REQUEST:
			RequestMessage requestMessage = (RequestMessage) p2pMessage;
			String pieceFileName = DataHolder.getPieceFileNameByPieceIndex(requestMessage.pieceIndex);
			LOGGER.info(MessageFormat.format(
					"Recieved piece request for piece: {0} and file name of the piece is {1} from peerId: {2}",
					requestMessage, pieceFileName, getTargetPeerId()));
			RandomAccessFile randAccessFile = new RandomAccessFile(pieceFileName, "r");
			LOGGER.info(MessageFormat.format("{0} lenght of peice: {1}", threadName, randAccessFile.length()));
			byte[] pieceData = new byte[(int) randAccessFile.length()];
			int readSize = randAccessFile.read(pieceData);
			if (readSize == -1) {
				LOGGER.warning(MessageFormat.format("{0} couldn't read content of pieceId: {1} with file name: {2}",
						threadName, requestMessage.pieceIndex, pieceFileName));
			} else {
				responseMessage = new PieceMessage(requestMessage.pieceIndex, pieceData);
			}
			break;
		case SHUTDOWN:
			setTaskComplete(true);
			LOGGER.info(MessageFormat.format("{0} exiting from thread in peer: {1}", threadName, DataHolder.peerId));
			return;
		default:
			LOGGER.warning(MessageFormat.format("{0} unexpected case: {1}", threadName,
					Util.getMessageType(p2pMessage.flag).description));
		}
		if (responseMessage != null)
			communicator.writeToSocket(responseMessage);
	}

}
