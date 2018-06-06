package cnt5106p2p.workers;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import cnt5106p2p.DataHolder;
import cnt5106p2p.ThreadLoggerStrap;
import cnt5106p2p.Util;
import cnt5106p2p.messages.HaveMessage;
import cnt5106p2p.messages.P2PMessages;
import cnt5106p2p.messages.PeerHaveMessage;
import cnt5106p2p.messages.PieceMessage;
import cnt5106p2p.messages.RequestMessage;

public class Leecher extends ThreadLoggerStrap implements Runnable {

	private final LinkedBlockingQueue<P2PMessages> queue;
	private final LinkedBlockingQueue<PeerHaveMessage> broadcasterQueue;
	private final int targetPeerId;
	// private static final Logger LOGGER =
	// Logger.getLogger(Leecher.class.getName());
	private static final Logger LOGGER = Logger.getLogger("cnt5106p2p");
	private final Communicator communicator;
	private boolean taskComplete;
	private boolean unchoked;

	public Leecher(int targetPeerId, LinkedBlockingQueue<P2PMessages> p2pMessagesQueue,
			LinkedBlockingQueue<PeerHaveMessage> broadcasterQueue, Communicator comm) {
		super(MessageFormat.format("[LeecherID: {0}]", targetPeerId));
		this.targetPeerId = targetPeerId;
		this.queue = p2pMessagesQueue;
		this.broadcasterQueue = broadcasterQueue;
		this.communicator = comm;
		taskComplete = false;
		unchoked = false;
	}

	public int getTargetPeerId() {
		return targetPeerId;
	}

	public void setTaskComplete(boolean taskComplete) {
		this.taskComplete = taskComplete;
	}

	public boolean getTaskComplete() {
		return taskComplete;
	}

	public void setUnchoked(boolean status) {
		unchoked = status;
		LOGGER.info(MessageFormat.format("{0} Set the unchoke status to {1}", threadName, unchoked));
	}

	public boolean getUnchoked() {
		return unchoked;
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
			}
		}
	}

	public void processMessage(P2PMessages p2pMessage) throws ClassNotFoundException, InterruptedException {
		// TODO process message, generate response, send the response
		P2PMessages responseMessage = null;
		try {
			switch (Util.getMessageType(p2pMessage.flag)) {
			case CHOKE: {
				setUnchoked(false);
				LOGGER.info(MessageFormat.format("{0} Peer [peer_ID {1}] is choked by peer [peer_ID {2}]", threadName,
						DataHolder.peerId, targetPeerId));
				int requesting_piece_index = DataHolder.getDownloadablePieceIndexFromPeer(targetPeerId);
				if (requesting_piece_index == -1)
					return;
				LOGGER.info(MessageFormat.format("CHOKE This peer is interested", DataHolder.peerId));
				responseMessage = P2PMessages.genInterestedMessage();
			}
				break;
			case UNCHOKE:
				// setUnchoked(true);
				LOGGER.info(MessageFormat.format("{0} Peer [peer_ID {1}] is unchoked by peer [peer_ID {2}]", threadName,
						DataHolder.peerId, targetPeerId));
			// while(getUnchoked())
			{
				int requesting_piece_index = DataHolder.getDownloadablePieceIndexFromPeer(targetPeerId);
				if (requesting_piece_index == -1) {
					responseMessage = P2PMessages.genNotInterestedMessage();
					// communicator.writeToSocket(responseMessage);
					// break;
				} else {
					responseMessage = new RequestMessage(requesting_piece_index);
					LOGGER.info(MessageFormat.format("{0} Requested piece index : {1}", threadName,
							requesting_piece_index));
				}
				// communicator.writeToSocket(responseMessage);
			}
				// responseMessage = null;
				// setTaskComplete(true);
				break;
			case HAVE:
				HaveMessage haveMessage = (HaveMessage) p2pMessage;
				LOGGER.info(MessageFormat.format(
						"{0} Peer [peer_ID {1}] received the 'have' message from [peer_ID {2}] for the piece [{3}]",
						threadName, DataHolder.peerId, targetPeerId, haveMessage.pieceIndex));
				if (!DataHolder.getPeerPieceAvailiblity(targetPeerId, haveMessage.pieceIndex)) {
					DataHolder.setPeerPieceAvailibility(targetPeerId, haveMessage.pieceIndex);
					LOGGER.info(" Does 1001 have the pieces: "
							+ DataHolder.getPeerPieceAvailiblity(1001, haveMessage.pieceIndex));
					LOGGER.info(" Does 1002 have the pieces: "
							+ DataHolder.getPeerPieceAvailiblity(1002, haveMessage.pieceIndex));
				}
				broadcasterQueue.put(new PeerHaveMessage(targetPeerId, new HaveMessage(haveMessage.pieceIndex)));
				int requesting_piece_index = DataHolder.getDownloadablePieceIndexFromPeer(targetPeerId);
				if (requesting_piece_index != -1) {
					LOGGER.info(MessageFormat.format("HAVE This peer is interested", DataHolder.peerId));
					responseMessage = P2PMessages.genInterestedMessage();
				} else
					return;
				break;
			case PIECE:
				PieceMessage pieceMessage = (PieceMessage) p2pMessage;
				if (DataHolder.getPeerPieceAvailiblity(DataHolder.peerId, pieceMessage.pieceIndex)) {
					LOGGER.info(
							MessageFormat.format("{0} Ignoring the piece {1}", threadName, pieceMessage.pieceIndex));
					break;
				}
				DataHolder.updateShareFileContent(pieceMessage, pieceMessage.pieceIndex, DataHolder.peerId);
				DataHolder.incrementNumOfAvailablePieces(1, pieceMessage.pieceIndex, DataHolder.peerId);
				DataHolder.setPeerPieceAvailibility(DataHolder.peerId, pieceMessage.pieceIndex);
				DataHolder.setPieceIndex(pieceMessage.pieceIndex);
				DataHolder.updateDownloadSizeForPeer(targetPeerId, pieceMessage.length - Short.BYTES - Integer.BYTES);
				DataHolder.updateDownloadStartTimeForPeer(targetPeerId, System.currentTimeMillis());
				LOGGER.info(MessageFormat.format(
						"{0} Peer [peer_ID {1}] has downloaded the piece" + " [{2}] from [peer_ID {3}]", threadName,
						DataHolder.peerId, pieceMessage.pieceIndex, targetPeerId));
				LOGGER.info(MessageFormat.format("{0} Now the number of pieces it has [{1}]", threadName,
						DataHolder.getNumOfAvailablePieces()));

				LOGGER.info(MessageFormat.format("{0} {1}", threadName, new String(pieceMessage.content)));
				broadcasterQueue.put(new PeerHaveMessage(DataHolder.peerId, new HaveMessage(pieceMessage.pieceIndex)));
				LOGGER.info("Notified the main thread about piece download");
				break;
			case SHUTDOWN:
				setTaskComplete(true);
				LOGGER.info(
						MessageFormat.format("{0} exiting from thread in peer: {1}", threadName, DataHolder.peerId));
				return;
			default:
				LOGGER.warning(MessageFormat.format("{0} unexpected case: {1}", threadName,
						Util.getMessageType(p2pMessage.flag).description));
			}
			if (responseMessage != null)
				communicator.writeToSocket(responseMessage);
		} catch (IOException e) {
			LOGGER.severe(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
		}
	}
}
