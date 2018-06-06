package cnt5106p2p;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.TreeMap;
import java.util.logging.*;

import cnt5106p2p.configparser.CommonConfigParser;
import cnt5106p2p.configparser.PeerInfoConfigParser;
import cnt5106p2p.messages.HaveMessage;
import cnt5106p2p.messages.P2PMessages;
import cnt5106p2p.messages.PeerHaveMessage;
import cnt5106p2p.workers.Communicator;
import cnt5106p2p.workers.OptimisticNeighbor;
import cnt5106p2p.workers.PreferredNeighbors;
import cnt5106p2p.configparser.PeerInfoConfig;

public class peerProcess {
	/**
	 * 
	 * Type of messages
	 */
	public static enum MessageType {
		CHOKE((short) 0, "CHOKE_MESSAGE"), UNCHOKE((short) 1, "UNCHOKE_MESSAGE"), INTERESTED((short) 2,
				"INTERESTED_MESSAGE"), NOT_INTERESTED((short) 3, "NOT_INTERESTED"), HAVE((short) 4,
						"HAVE_MESSAGE"), BIT_FIELD((short) 5, "BIT_FIELD_MESSAGE"), REQUEST((short) 6,
								"REQUEST_MESSAGE"), PIECE((short) 7, "PIECE_MESSAGE"), UNDEFINED((short) 0,
										"UNDEFINED_MESSAGE"), SHUTDOWN((short) -1, "SHUTDOWN_MESSAGE");

		MessageType(short type, String description) {
			this.type = type;
			this.description = description;
		}

		public final short type;
		public final String description;

		short getMessageType() {
			return type;
		}
	}

	/**
	 * Exception thrown by peer.
	 */
	@SuppressWarnings("serial")
	public static class PeerException extends Exception {
		public PeerException(String exception) {
			super(exception);
		}
	}

	// private static final Logger LOGGER =
	// Logger.getLogger(cnt5106p2p.peerProcess.class.getName());
	private static final Logger LOGGER = Logger.getLogger("cnt5106p2p");
	// private static final Logger PARENT_LOGGER =
	// Logger.getLogger(cnt5106p2p.peerProcess.class.getPackage().getName());
	private HashMap<Integer, LinkedBlockingQueue<P2PMessages>> seederQueues;
	private HashMap<Integer, LinkedBlockingQueue<P2PMessages>> leecherQueues;
	private LinkedBlockingQueue<PeerHaveMessage> broadcastersQueue;

	public peerProcess() {
		System.out.println(peerProcess.class.getName());
		seederQueues = new HashMap<>();
		leecherQueues = new HashMap<>();
		broadcastersQueue = new LinkedBlockingQueue<>();
	}

	public static void main(String[] args) {

		DataHolder.peerId = Integer.parseInt(args[0]);
		File peerFolder = new File(new File(System.getProperty("user.dir")).getParent() + "/peer_" + DataHolder.peerId);
		peerFolder.mkdir();
		try {
			// LOGGER.setUseParentHandlers(false);
			FileHandler fh = new FileHandler(String.format("../log_peer_%d.log", DataHolder.peerId), 1024 * 1024 * 10, 5,
					false);
			fh.setFormatter(new SimpleFormatter() {
				private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %4$s %5$s %6$s %n";

				@Override
				public synchronized String format(LogRecord lr) {
					return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(),
							lr.getSourceClassName(), lr.getSourceMethodName(), lr.getThreadID(), lr.getMessage());
				}
			});
			fh.setLevel(Level.FINE);
			LOGGER.addHandler(fh);
			LOGGER.setLevel(Level.FINE);
			// PARENT_LOGGER.addHandler(fh);
			// PARENT_LOGGER.setLevel(Level.FINE);
			// PARENT_LOGGER.setUseParentHandlers(false);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		PeerInfoConfigParser peerInfoCfgParser = new PeerInfoConfigParser();
		TreeMap<Integer, PeerInfoConfig> peerConfig = null;
		try {
			peerConfig = peerInfoCfgParser.parseConfig();
		} catch (Exception e1) {
			System.exit(255);
		}
		if (!peerConfig.containsKey(DataHolder.peerId)) {
			LOGGER.severe("Specified peer ID doesn't exist in peer config file.");
			System.exit(1);
		}
		CommonConfigParser commonCfgParser = new CommonConfigParser();
		HashMap<String, String> commonConfig = commonCfgParser.parseConfig();
		// set the common configurations
		DataHolder.numPreferredNeighbhor = Integer.parseInt(commonConfig.get("NumberOfPreferredNeighbors"));
		DataHolder.unchokeInterval = Integer.parseInt(commonConfig.get("UnchokingInterval")) * 1000;
		DataHolder.optUnchokeInterval = Integer.parseInt(commonConfig.get("OptimisticUnchokingInterval")) * 1000;
		DataHolder.shareFileName = commonConfig.get("FileName");
		DataHolder.fileSize = Integer.parseInt(commonConfig.get("FileSize"));
		DataHolder.pieceSize = Integer.parseInt(commonConfig.get("PieceSize"));
		// set the peer configurations
		DataHolder.hostName = peerConfig.get(DataHolder.peerId).hostName;
		DataHolder.portNumber = peerConfig.get(DataHolder.peerId).listeningPort;
		DataHolder.numOfPieces = (int) Math.ceil((double) DataHolder.fileSize / DataHolder.pieceSize);
		DataHolder.initNumPieceIndices();

		if (peerConfig.get(DataHolder.peerId).hasFile) {
			DataHolder.setHaveFullFile(true);
			/*File originalPath = new File(
					new File(System.getProperty("user.dir")).getParent() + "/" + DataHolder.shareFileName);
			originalPath.renameTo(new File(new File(System.getProperty("user.dir")).getParent() + "/peer_"
					+ DataHolder.peerId + '/' + DataHolder.shareFileName));
			*/
			new File(new File(System.getProperty("user.dir")).getParent() + "/peer_"
					+ DataHolder.peerId).mkdirs();
			DataHolder.setAllPieces();
		} else {
			DataHolder.setHaveFullFile(false);
		}

		///////////////////////////////////
		peerProcess p2PPeer = new peerProcess();
		if (DataHolder.getHaveFullFile()) {
			File fullFile = new File(new File(System.getProperty("user.dir")).getParent() + "/"+ DataHolder.shareFileName);

			System.out.println(fullFile);
			try {
				DataHolder.setPieceFileNames(p2PPeer.splitIntoPieces(fullFile, DataHolder.pieceSize));
			} catch (IOException e) {
				LOGGER.severe(MessageFormat.format("{0}", Util.exceptionToString(e)));
				System.exit(254);
			}
			DataHolder.setPeerPieceFull(DataHolder.peerId);
			for(int i = 0; i < DataHolder.numOfPieces; i++){
				DataHolder.incrementNumOfAvailablePieces(1, i, DataHolder.peerId);
			}
		} else {
			// If file doesn't have full file set piece file names to empty
			TreeMap<Integer, String> pieceFileNames = new TreeMap<>();
			for (int i = 0; i < DataHolder.numOfPieces; i++) {
				pieceFileNames.put(i, "");
			}
			DataHolder.setPieceFileNames(pieceFileNames);
		}

		ArrayList<Integer> peerIds = new ArrayList<>();
		Iterator<Entry<Integer, PeerInfoConfig>> entries = peerConfig.entrySet().iterator();
		Entry<Integer, PeerInfoConfig> entry = null;
		ArrayList<Communicator> communicators = new ArrayList<>();
		ArrayList<Socket> sockets = new ArrayList<>();
		while (entries.hasNext()) {
			entry = entries.next();
			peerIds.add(entry.getKey());
			if (entry.getKey() == DataHolder.peerId) {
				break;
			}
			try {
				p2PPeer.seederQueues.put(entry.getKey(), new LinkedBlockingQueue<>());
				p2PPeer.leecherQueues.put(entry.getKey(), new LinkedBlockingQueue<>());
				Socket clientSocket = new Socket(entry.getValue().hostName, entry.getValue().listeningPort);
				LOGGER.info(MessageFormat.format("Peer [peer_ID {0}] make a connection to Peer [peer_ID {1}]",
						DataHolder.peerId, entry.getKey()));
				Communicator communicator = new Communicator(entry.getKey(), clientSocket,
						p2PPeer.seederQueues.get(entry.getKey()), p2PPeer.leecherQueues.get(entry.getKey()),
						p2PPeer.broadcastersQueue);
				communicators.add(communicator);
				//(new Thread(communicator)).start();
				sockets.add(clientSocket);
			} catch (IOException e) {
				LOGGER.severe("Couldn't start socket for " + entry.getValue().hostName + " at port "
						+ entry.getValue().listeningPort);
				LOGGER.severe(Util.exceptionToString(e));
				System.exit(2);
			}
		}
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(DataHolder.portNumber);
		} catch (IOException e1) {
			LOGGER.severe(Util.exceptionToString(e1));
		}
		while (entries.hasNext()) {
			entry = entries.next();
			peerIds.add(entry.getKey());
			try {
				p2PPeer.seederQueues.put(entry.getKey(), new LinkedBlockingQueue<>());
				p2PPeer.leecherQueues.put(entry.getKey(), new LinkedBlockingQueue<>());
				Socket socket = serverSocket.accept();
				System.out.println(entries.hasNext());
				LOGGER.info(MessageFormat.format("Peer [peer_ID {0}] is connected from Peer [peer_ID {1}]",
						DataHolder.peerId, entry.getKey()));
				Communicator communicator = new Communicator(entry.getKey(), socket,
						p2PPeer.seederQueues.get(entry.getKey()), p2PPeer.leecherQueues.get(entry.getKey()),
						p2PPeer.broadcastersQueue);
				communicators.add(communicator);
				sockets.add(socket);
			} catch (IOException e) {
				LOGGER.severe(Util.exceptionToString(e));
			}
		}
		DataHolder.peerIds = peerIds;
		DataHolder.initPeersPieceIndicesAvailibility();
		DataHolder.initDownloadMap();
		DataHolder.initDownloadStartTimes();
		for (Communicator communicator: communicators){
			(new Thread(communicator)).start();
		}
		PreferredNeighbors preferredNeighbors = new PreferredNeighbors(p2PPeer.seederQueues);
		Thread  preferredNeighborsThread = new Thread(preferredNeighbors);
		preferredNeighborsThread.start();
		OptimisticNeighbor optimisticNeighbor = new OptimisticNeighbor(p2PPeer.seederQueues);
		Thread optimisticNeighborThread = new Thread(optimisticNeighbor);
		optimisticNeighborThread.start();
		LOGGER.info("Main thread in going take messages from broadcaster queue");
		while (true) {
			try {
				PeerHaveMessage broadcastMessage = p2PPeer.broadcastersQueue.take();
				LOGGER.info(MessageFormat.format("{0} Got a broadcast message for peerID {1}", DataHolder.peerId,
						broadcastMessage.peerId));
				if (broadcastMessage.peerId == DataHolder.peerId) {
					Iterator<Entry<Integer, LinkedBlockingQueue<P2PMessages>>> queueIterator = p2PPeer.seederQueues
							.entrySet().iterator();
					while (queueIterator.hasNext()) {
						queueIterator.next().getValue().put(new HaveMessage(broadcastMessage.pieceIndex));
						LOGGER.info("Notified Seeder about the download " + broadcastMessage.pieceIndex);
					}
					LOGGER.info("" + DataHolder.getNumOfAvailablePieces() + " , " + DataHolder.numOfPieces);
					if (DataHolder.getNumOfAvailablePieces() == DataHolder.numOfPieces) {
						LOGGER.info(MessageFormat.format("{0} Peer [peer_ID {1}] has downloaded full file", "main",
								DataHolder.peerId));
						try {
							p2PPeer.mergePieces(peerFolder, DataHolder.shareFileName, DataHolder.pieceSize);
						} catch (IOException e) {
							e.printStackTrace();
						}
						DataHolder.setHaveFullFile(true);
					}
				}
				if (DataHolder.peersContainAllPieces()) {
					LOGGER.info("All the peers have all the pieces of file. Peer exiting...");

					for (Communicator communicator : communicators) {
						communicator.setTaskComplete(true);
						communicator.writeToSocket(new P2PMessages(1, MessageType.SHUTDOWN));
						Thread.sleep(100);
						communicator.closeSocket();
					}
					preferredNeighbors.setTaskComplete(true);
					optimisticNeighbor.setTaskComplete(true);
					preferredNeighborsThread.join();
					optimisticNeighborThread.join();
					/*
					 * for (Socket socket: sockets) { socket.close(); } if
					 * (serverSocket != null) serverSocket.close();
					 */
					LOGGER.info("All thread exited. Peer " + DataHolder.peerId + " out.");
					serverSocket.close();
					System.exit(0);
				}
			} catch (InterruptedException e) {
				LOGGER.warning(Util.exceptionToString(e));
			} catch (IOException e) {
				LOGGER.warning(Util.exceptionToString(e));
			}
		}
	}

	private TreeMap<Integer, String> splitIntoPieces(File fullFile, int pieceSize) throws IOException {
		RandomAccessFile randAccessFile = new RandomAccessFile(fullFile, "r");
		long sourceLength = fullFile.length();
		int numPieces = (int) (sourceLength / pieceSize);
		long leftOverBytes = 0;
		if (numPieces != 0) {
			leftOverBytes = sourceLength % pieceSize;
		} else {
			numPieces = 1;
		}

		TreeMap<Integer, String> splitFileNames = new TreeMap<>();

		for (int i = 0; i < numPieces; i++) {
			String pieceFileName = DataHolder.generataPieceFileName(i);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pieceFileName));
			byte[] piece = new byte[pieceSize];
			int exists = randAccessFile.read(piece);
			if (exists != -1) {
				splitFileNames.put(i, pieceFileName);
				bos.write(piece);
			}
			bos.close();
		}
		if (leftOverBytes > 0) {
			String pieceFileName = DataHolder.generataPieceFileName(numPieces);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pieceFileName));
			byte[] piece = new byte[(int) leftOverBytes];
			int exists = randAccessFile.read(piece);
			if (exists != -1) {
				splitFileNames.put(numPieces, pieceFileName);
				bos.write(piece);
			}
			bos.close();
		}
		randAccessFile.close();
		return splitFileNames;
	}

	private void mergePieces(File directoryPath, String mergedFileName, int pieceSize) throws IOException {
		String path = directoryPath.getPath();

		String mergedFileString = path + "/" + mergedFileName;
		BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(mergedFileString, false));
		long fullFileSize = 0;
		for (Entry<Integer, String> entry : DataHolder.getPieceFileNameMap().entrySet()) {
			RandomAccessFile randAccessFile = new RandomAccessFile(entry.getValue(), "r");
			long curFileSize = randAccessFile.length();
			byte[] piece = new byte[(int) curFileSize];
			int exists = randAccessFile.read(piece);
			if (exists != 1) {
				bufOut.write(piece);
			}
			randAccessFile.close();
			/*
			BufferedReader br = new BufferedReader(new FileReader(entry.getValue()));
			String line;
			while((line = br.readLine()) != null){
				bufOut.write(line.getBytes());
				curFileSize += line.length();
			}
			br.close();
			*/
			fullFileSize += curFileSize;
			LOGGER.info("Size of current piece file: "  + entry.getValue() + " is: " + curFileSize);
			LOGGER.info("Total size of merged file so far: " + fullFileSize);
		}
		bufOut.close();
	}

}
