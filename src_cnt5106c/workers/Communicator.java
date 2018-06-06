package cnt5106p2p.workers;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import cnt5106p2p.DataHolder;
import cnt5106p2p.ThreadLoggerStrap;
import cnt5106p2p.Util;
import cnt5106p2p.peerProcess.MessageType;
import cnt5106p2p.messages.BitFieldMessage;
import cnt5106p2p.messages.HandShakeMessage;
import cnt5106p2p.messages.HaveMessage;
import cnt5106p2p.messages.P2PMessages;
import cnt5106p2p.messages.PeerHaveMessage;
import cnt5106p2p.messages.PieceMessage;
import cnt5106p2p.messages.RequestMessage;

public class Communicator extends ThreadLoggerStrap implements Runnable {
	private int targetPeerId;
	private String hostName;
	private int portNumber;
	// private boolean serverMode;
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private ObjectOutputStream objectOut;
	private ObjectInputStream objectIn;
	private LinkedBlockingQueue<P2PMessages> seederQueue;
	private LinkedBlockingQueue<P2PMessages> leecherQueue;
	private final LinkedBlockingQueue<PeerHaveMessage> broadcasterQueue;
	private boolean taskComplete;

	// private static final Logger LOGGER =
	// Logger.getLogger(Communicator.class.getName());
	private static final Logger LOGGER = Logger.getLogger("cnt5106p2p");

	public Communicator(int peerId, String hostName, int port, Socket socket,
			LinkedBlockingQueue<P2PMessages> seederQueue, LinkedBlockingQueue<P2PMessages> leecherQueue,
			LinkedBlockingQueue<PeerHaveMessage> broadcasterQueue) throws IOException {
		super(MessageFormat.format("[Communicator Id: {0}]", peerId));

		this.targetPeerId = peerId;
		this.hostName = hostName;
		this.portNumber = port;
		this.socket = socket;
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();
		this.objectOut = new ObjectOutputStream(out);
		this.objectIn = new ObjectInputStream(in);
		this.seederQueue = seederQueue;
		this.leecherQueue = leecherQueue;
		this.broadcasterQueue = broadcasterQueue;
		taskComplete = false;
	}

	public Communicator(int peerId, Socket socket, LinkedBlockingQueue<P2PMessages> seederQueue,
			LinkedBlockingQueue<P2PMessages> leecherQueue, LinkedBlockingQueue<PeerHaveMessage> broadcasterQueue)
			throws IOException {
		super(MessageFormat.format("[Communicator Id: {0}]", peerId));
		this.targetPeerId = peerId;
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();
		this.objectOut = new ObjectOutputStream(out);
		this.objectIn = new ObjectInputStream(in);
		this.seederQueue = seederQueue;
		this.leecherQueue = leecherQueue;
		this.broadcasterQueue = broadcasterQueue;
		taskComplete = false;
	}

	public int getTargetPeerID() {
		return targetPeerId;
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
		try {
			if (!p2pHandShake()) {
				LOGGER.severe(MessageFormat.format(
						"Handshake with peer: {0} is failed " + "because peer didn't send correct handshake message",
						targetPeerId));
			}
			BitFieldMessage peerBitFieldMessage = p2pBitfieldExchange();

			if (DataHolder.isPeerInteresting(peerBitFieldMessage)) {
				sendInterstedMessage();
			} else {
				sendNotInterestedMessage();
			}

			Leecher leecher = new Leecher(targetPeerId, leecherQueue, broadcasterQueue, this);
			Seeder seeder = new Seeder(targetPeerId, seederQueue, this);
			//LinkedBlockingQueue<P2PMessages> requesterLeecherQueue = new LinkedBlockingQueue<>();
			//Leecher requesterLeecher = new Leecher(targetPeerId, requesterLeecherQueue, broadcasterQueue, this);
			new Thread(seeder).start();
			new Thread(leecher).start();
			LOGGER.info(MessageFormat.format("{0} started leecher and seeder threads", threadName));
			while (!getTaskComplete()) {
				P2PMessages message = (P2PMessages) objectIn.readObject();
				short messageType = message.flag;
				int lenOfMessage = message.length;
				LOGGER.info(MessageFormat.format("{0} Received message of type: {1} of length: {2}", threadName,
						messageType, lenOfMessage));
				switch (Util.getMessageType(messageType)) {
				case CHOKE:
					//requesterLeecher.setUnchoked(false);
					leecherQueue.put(P2PMessages.genChokeMessage());
					break;
				case UNCHOKE:
					leecher.setUnchoked(true);
					leecherQueue.put(P2PMessages.genUnChokeMessages());
					//Thread requestLeecherThread = new Thread(requesterLeecher);
					//requestLeecherThread.start();
					//requesterLeecherQueue.put(P2PMessages.genUnChokeMessages());
					break;
				case INTERESTED:
					processInterestedMessage();
					break;
				case NOT_INTERESTED:
					processUninterestedMessage();
					break;
				case HAVE:
					// HaveMessage haveMessage = new
					// HaveMessage((HaveAndRequestMessagePayload)
					// objectIn.readObject());
					HaveMessage haveMessage = (HaveMessage) message;
					leecherQueue.put(haveMessage);
					break;
				case REQUEST:
					// RequestMessage requestMessage = new RequestMessage(
					// (HaveAndRequestMessagePayload) objectIn.readObject());
					RequestMessage requestMessage = (RequestMessage) message;
					LOGGER.info("Received request message from peer: " + targetPeerId + " for pieceIndex: "
							+ requestMessage.pieceIndex);
					seederQueue.put(requestMessage);
					break;
				case PIECE:
					// PieceMessage pieceMessage = new
					// PieceMessage((PieceMessagePayload)
					// objectIn.readObject());
					PieceMessage pieceMessage = (PieceMessage) message;
					leecherQueue.put(pieceMessage);
					if (leecher.getUnchoked())
						leecherQueue.put(P2PMessages.genUnChokeMessages());
					break;
				case SHUTDOWN:
					if(socket != null) {
					    socket.close();
					}
					LOGGER.info(MessageFormat.format("{0} from peer: {1} closed connection with peer {2}", threadName,
							DataHolder.peerId, targetPeerId));
					return;
				default:
					LOGGER.warning(MessageFormat.format("{0} Recived a unsupported message from peer: {1}", threadName,
							targetPeerId));
				}
			}
			leecher.setTaskComplete(true);
			seeder.setTaskComplete(true);
			LOGGER.info("Stoped leecher and seeder. Communicator out.");

		} catch (ClassNotFoundException e) {
			LOGGER.severe(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
		} catch (IOException e) {
			LOGGER.severe(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
		} catch (InterruptedException e) {
			LOGGER.severe(MessageFormat.format("{0} {1}", threadName, Util.exceptionToString(e)));
		}
	}

	private boolean p2pHandShake() throws IOException, ClassNotFoundException {
		HandShakeMessage handShakeMessage = new HandShakeMessage(DataHolder.peerId);
		writeToSocket(handShakeMessage);
		HandShakeMessage peerHandShakeMessage = (HandShakeMessage) objectIn.readObject();
		if (!Util.verifyHandShakeMessage(targetPeerId, peerHandShakeMessage)) {
			return false;
		}
		return true;
	}

	private BitFieldMessage p2pBitfieldExchange() throws IOException, ClassNotFoundException, InterruptedException {
		BitFieldMessage bitFieldMessage = new BitFieldMessage(DataHolder.getPieceIndices());
		LOGGER.info(MessageFormat.format("Peer {0} piece Indice string:{1}", DataHolder.peerId,
				DataHolder.getPieceIndices()));
		writeToSocket(bitFieldMessage);
		Thread.sleep(100);
		BitFieldMessage bitFieldMessage2 = (BitFieldMessage) objectIn.readObject();
		LOGGER.info("payload bitfield pieces: " + bitFieldMessage2.payload);
		DataHolder.setPeerPiecesAvailibility(targetPeerId, bitFieldMessage2.payload);
		return bitFieldMessage2;
	}

	public synchronized void writeToSocket(Externalizable message) throws IOException {
		if (socket != null &&( socket.isClosed() || socket.isOutputShutdown())){
			LOGGER.info("Socket is already closed. Not writing the message");
			return;
		}
		if (!(message instanceof HandShakeMessage)) {
			LOGGER.info(threadName + " " + ((P2PMessages) message).flag);
		}
		objectOut.writeObject(message);
	}

	public void closeSocket() throws IOException, InterruptedException{
		if(socket != null) {
		leecherQueue.put(new P2PMessages(1, MessageType.SHUTDOWN));
		seederQueue.put(new P2PMessages(1, MessageType.SHUTDOWN));
		socket.close();
		}
	}
	
	private void sendInterstedMessage() throws IOException {
		LOGGER.info(MessageFormat.format("peer {0} is sending interested message", DataHolder.peerId));
		writeToSocket(P2PMessages.genInterestedMessage());
		LOGGER.info(MessageFormat.format("{0} send interested message to peer: {1}", threadName, targetPeerId));
	}

	private void sendNotInterestedMessage() throws IOException {
		writeToSocket(P2PMessages.genNotInterestedMessage());
		LOGGER.info(MessageFormat.format("{0} send not interested message to peer: {1}", threadName, targetPeerId));
	}

	private void processInterestedMessage() {
		LOGGER.info(MessageFormat.format("{0} Peer [peer_ID {1}] received the 'interested' message from [peer_ID {2}]",
				threadName, DataHolder.peerId, targetPeerId));
		DataHolder.updateInterestedPeers(targetPeerId, true);
		LOGGER.info(MessageFormat.format("{0} marked peer: {1} as interested.", threadName, targetPeerId));
	}

	private void processUninterestedMessage() {
		LOGGER.info(
				MessageFormat.format("{0} Peer [peer_ID {1}] received the 'not interested' message from [peer_ID {2}]",
						threadName, DataHolder.peerId, targetPeerId));
		DataHolder.updateInterestedPeers(targetPeerId, false);
		LOGGER.info(MessageFormat.format("{0} marked peer: {1} as not interested.", threadName, targetPeerId));
	}
}
