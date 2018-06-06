package cnt5106p2p;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import cnt5106p2p.messages.BitFieldMessage;
import cnt5106p2p.messages.PieceMessage;

public class DataHolder {
	private volatile static ArrayList<Integer> interestedPeers = new ArrayList<>();
	public static final Object INTERESTED_PEERS_MUTEX = new Object();
	private volatile static HashSet<Integer> preferredPeers = new HashSet<>();
	private static final Object PREFERRED_PEERS_MUTEX = new Object();
	private volatile static int optimisticNeighbor = -1;
	private static final Object OPTIMISTIC_NEIGHBOR_MUTEX = new Object();
	private volatile static StringBuilder pieceIndices = new StringBuilder();
	private static final Object PIECE_INDICES_MUTEX = new Object();
	private volatile static int numOfAvailablePieces = 0;
	private static final Object NUM_OF_AVAILABLE_PIECES_MUTEX = new Object();
	// map of piece availability for every host, array only contain indices that
	// are available at that host
	private volatile static HashMap<Integer, HashSet<Integer>> peersPieceIndicesAvailibility = new HashMap<Integer, HashSet<Integer>>();
	private static final Object PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX = new Object();
	private volatile static TreeMap<Integer, String> pieceFileNames;
	private static final Object SHARED_FILE_PIECE_FILE_NAME_MUTEX = new Object();
	private volatile static HashMap<Integer, Float> seedersMap = new HashMap<>();
	private static final Object SEEDER_MAP_MUTEX = new Object();
	private volatile static boolean haveFullFile = false;
	private static final Object HAVE_FILE_MUTEX = new Object();

	// map to keep track of download rates
	// TODO: set it initially in PreferredNeighbors
	private static ConcurrentHashMap<Integer, Integer> downloadMap = new ConcurrentHashMap<>();
	private static final Object DOWNLOAD_MAP_MUTEX = new Object();
	private static ConcurrentHashMap<Integer, Long> startTimes = new ConcurrentHashMap<>();
	private static final Object START_TIME_MUTEX = new Object();
	/*
	 * private static final ReentrantReadWriteLock downloadMapReadWriteLock =
	 * new ReentrantReadWriteLock(); private static final Lock
	 * readDownoadMapLock = downloadMapReadWriteLock.readLock(); private static
	 * final Lock writeDownoadMapLock = downloadMapReadWriteLock.writeLock();
	 */
	public static int numPreferredNeighbhor = -1;
	public static int unchokeInterval = -1;
	public static int optUnchokeInterval = -1;
	public static String shareFileName = "";
	public static int fileSize = 0;
	public static int pieceSize = 0;
	public static int peerId = -1;
	public static String hostName = "";
	public static int portNumber = -1;
	public static int numOfPieces = 0;
	public static ArrayList<Integer> peerIds = null;

	// private static final Logger LOGGER =
	// Logger.getLogger(DataHolder.class.getName());
	private static final Logger LOGGER = Logger.getLogger("cnt5106p2p");

	public static int getDownloadablePieceIndexFromPeer(int targetPeerId) {
		HashSet<Integer> targetPieceIndices = new HashSet<>();
		synchronized (PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX) {
			targetPieceIndices = new HashSet<>(DataHolder.peersPieceIndicesAvailibility.get(targetPeerId));
			Iterator<Integer> itr = targetPieceIndices.iterator();
			while (itr.hasNext()) {
				LOGGER.info("Target piece index: " + Integer.toString(itr.next()));
			}
			LOGGER.info(
					"Target piece indices size: " + targetPieceIndices.size() + " with target peer ID " + targetPeerId);
			// targetPieceIndices.removeAll(DataHolder.peersPieceIndicesAvailibility.get(DataHolder.peerId));
			HashSet<Integer> peerIdPieces = new HashSet<>(
					DataHolder.peersPieceIndicesAvailibility.get(DataHolder.peerId));
			Iterator<Integer> itrpeerId = peerIdPieces.iterator();
			LOGGER.info("Peer piece index size: " + peerIdPieces.size() + " with peer ID " + peerId);
			while (itrpeerId.hasNext()) {
				LOGGER.info("Current piece index set: " + Integer.toString(itrpeerId.next()) + " for peerId " + peerId);
			}
			targetPieceIndices.removeAll(peerIdPieces);
			Iterator<Integer> itr2 = targetPieceIndices.iterator();
			while (itr2.hasNext()) {
				LOGGER.info("Target piece index after remove: " + Integer.toString(itr2.next()) + " for targetPeerId "
						+ peerId);
			}

		}
		LOGGER.info("Target piece index size: " + targetPieceIndices.size());
		if (targetPieceIndices.size() == 0)
			return -1;
		return (int) targetPieceIndices.toArray()[0];
	}

	public static void initPeersPieceIndicesAvailibility() {
		synchronized (PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX) {
			for (int peerId : peerIds) {
				if (!peersPieceIndicesAvailibility.containsKey(peerId)) {
					peersPieceIndicesAvailibility.put(peerId, new HashSet<>());
				}
			}
			LOGGER.info("initialized peersPieceIndicesAvailability for " + peerIds.size() + " peers.");
		}
	}

	// TODO: CHECK double check this function
	public static boolean peersContainAllPieces() {
		synchronized (PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX) {
			Iterator<Entry<Integer, HashSet<Integer>>> peerEntryIter = peersPieceIndicesAvailibility.entrySet()
					.iterator();
			Entry<Integer, HashSet<Integer>> entry = null;
			while (peerEntryIter.hasNext()) {
				entry = peerEntryIter.next();
				for (int i = 0; i < numOfPieces; i++) {
					if (!entry.getValue().contains(i)) {
						return false;
					}

				}
			}
			return true;
		}
	}

	public static boolean getPeerPieceAvailiblity(int targetPeerId, int pieceIndex) {
		synchronized (PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX) {
			return peersPieceIndicesAvailibility.get(targetPeerId).contains(pieceIndex);
		}
	}

	public static void setPeerPieceAvailibility(int targetPeerId, int pieceIndex) {
		synchronized (PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX) {
			if (peersPieceIndicesAvailibility.get(targetPeerId) == null) {
				HashSet<Integer> pieceSet = new HashSet<>();
				pieceSet.add(pieceIndex);
				peersPieceIndicesAvailibility.put(targetPeerId, pieceSet);
			} else {
				LOGGER.info("Adding piece: " + pieceIndex + "to peer " + targetPeerId);
				peersPieceIndicesAvailibility.get(targetPeerId).add(pieceIndex);
			}
		}
	}

	public static void setPeerPiecesAvailibility(int targetId, String allPieces) {
		synchronized (PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX) {
			try {
				for (int peerId: peersPieceIndicesAvailibility.keySet()){
					LOGGER.info(MessageFormat.format("peers IDs: {0}", peerId));
				}
				for (int i = 0; i < allPieces.length(); i++) {
					if (allPieces.charAt(i) == '1') {
						LOGGER.info("Setting peer pieces availbility " + targetId + " with pieces " + allPieces);
						peersPieceIndicesAvailibility.get(targetId).add(i);
					}
				}
			} catch (NullPointerException e) {
				LOGGER.severe(Util.exceptionToString(e));
				LOGGER.info(MessageFormat.format("PeerID: {0} not present", targetId));
				peersPieceIndicesAvailibility.put(targetId, new HashSet<>());
				for (int i = 0; i < allPieces.length(); i++) {
					if (allPieces.charAt(i) == '1') {
						LOGGER.info("Setting peer pieces availbility " + targetId + " with pieces " + allPieces);
						peersPieceIndicesAvailibility.get(targetId).add(i);
					}
				}
			}
		}
	}

	public static void setPeerPieceFull(int targetPeerId) {
		synchronized (PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX) {
			HashSet<Integer> pieceSet = new HashSet<>();
			for (int i = 0; i < numOfPieces; i++) {
				pieceSet.add(i);
			}
			peersPieceIndicesAvailibility.put(targetPeerId, pieceSet);
			LOGGER.info("Setting peer pieces availbility" + targetPeerId + " size: "
					+ peersPieceIndicesAvailibility.get(targetPeerId).size() + " num pieces " + numOfPieces);
			HashSet<Integer> fullPiece = new HashSet<>();
			fullPiece = new HashSet<>(DataHolder.peersPieceIndicesAvailibility.get(targetPeerId));
			Iterator<Integer> itr = fullPiece.iterator();
			while (itr.hasNext()) {
				LOGGER.info("Full piece index is: " + Integer.toString(itr.next()));
			}
		}

	}

	public static boolean checkPeerHaveFullFile(int targetPeerId) {
		synchronized (PEERS_PIECE_INDICES_AVAILIBILITY_MUTEX) {
			for (int i = 0; i < numOfPieces; i++) {
				if (!peersPieceIndicesAvailibility.get(targetPeerId).contains(i)) {
					LOGGER.info("Peer doesn't have piece number " + i);
					return false;
				}
			}
			return true;
		}
	}

	public static void updateShareFileContent(PieceMessage pieceMessage, int pieceIndex, int peerId) throws IOException {
		if(getPeerPieceAvailiblity(peerId, pieceIndex)){
			LOGGER.info("Not updating the file content as it's already present.");
			return;
		}
		String pieceFileName = DataHolder.generataPieceFileName(pieceMessage.pieceIndex);
		synchronized (SHARED_FILE_PIECE_FILE_NAME_MUTEX) {
			pieceFileNames.put(pieceMessage.pieceIndex, pieceFileName);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pieceFileName));
			bos.write(pieceMessage.content);
			bos.close();
		}
	}

	public static String generataPieceFileName(int pieceId) {
		return (new File(System.getProperty("user.dir"))).getParent() + "/peer_" + DataHolder.peerId + "/piece_"
				+ pieceId;
	}

	public static void setPieceIndex(int index) {
		synchronized (PIECE_INDICES_MUTEX) {
			pieceIndices.setCharAt(index, '1');
		}
	}

	public static void initNumPieceIndices() {
		synchronized (PIECE_INDICES_MUTEX) {
			for (int i = 0; i < numOfPieces; i++) {
				pieceIndices.append('0');
			}
		}
	}

	public static void setAllPieces() {
		synchronized (PIECE_INDICES_MUTEX) {
			for (int i = 0; i < numOfPieces; i++) {
				pieceIndices.setCharAt(i, '1');
			}
		}
	}

	public static String getPieceIndices() {
		synchronized (PIECE_INDICES_MUTEX) {
			return pieceIndices.toString();
		}
	}

	/**
	 * check the piece indices send by peer and decide if peer is interesting or
	 * not
	 * 
	 * @param bitFieldMessage
	 * @return boolean stating if peer have an interesting piece
	 */
	public static boolean isPeerInteresting(BitFieldMessage bitFieldMessage) {
		boolean interestingPiece = false;
		synchronized (DataHolder.PIECE_INDICES_MUTEX) {
			for (int i = 0; i < bitFieldMessage.payload.length(); i++) {
				LOGGER.info(MessageFormat.format("The pieces I have are: {0}", pieceIndices));

				if (bitFieldMessage.payload.charAt(i) == '1' && pieceIndices.charAt(i) == '0') {
					interestingPiece = true;
					LOGGER.info("Found a interesting piece");
				}
			}
		}
		return interestingPiece;
	}

	public static void updateInterestedPeers(int peerId, boolean interested) {
		synchronized (INTERESTED_PEERS_MUTEX) {
			if (interested) {
				interestedPeers.add(peerId);
			} else {
				if (interestedPeers.contains(peerId)) {
					interestedPeers.remove(interestedPeers.indexOf(peerId));
				}
			}
		}
	}

	public static String getPieceFileNameByPieceIndex(int pieceIndex) {
		synchronized (SHARED_FILE_PIECE_FILE_NAME_MUTEX) {
			return pieceFileNames.get(pieceIndex);
		}
	}

	public static void setPieceFileNames(TreeMap<Integer, String> fileContent) {
		synchronized (SHARED_FILE_PIECE_FILE_NAME_MUTEX) {
			pieceFileNames = fileContent;
		}
	}

	public static TreeMap<Integer, String> getPieceFileNameMap() {
		synchronized (SHARED_FILE_PIECE_FILE_NAME_MUTEX) {
			return new TreeMap<>(pieceFileNames);
		}
	}

	public static boolean getHaveFullFile() {
		return haveFullFile;
	}

	public static void setHaveFullFile(boolean value) {
		haveFullFile = value;
	}

	public static void setOptimisticNeighbor(int optimisticallyUnchokedPeer) {
		synchronized (OPTIMISTIC_NEIGHBOR_MUTEX) {
			optimisticNeighbor = optimisticallyUnchokedPeer;
		}
	}

	public static void incrementNumOfAvailablePieces(int numOfPieces, int pieceIndex, int peerId) {
		if(getPeerPieceAvailiblity(peerId, pieceIndex)){
			LOGGER.info("Not incrementing the number of pieces as it's already present.");
			return;
		}
		synchronized (NUM_OF_AVAILABLE_PIECES_MUTEX) {
			numOfAvailablePieces += numOfPieces;
		}
	}

	public static int getNumOfAvailablePieces() {
		synchronized (NUM_OF_AVAILABLE_PIECES_MUTEX) {
			return numOfAvailablePieces;
		}
	}

	public static void initDownloadMap() {
		synchronized (DOWNLOAD_MAP_MUTEX) {
			for (int peerId : peerIds) {
				downloadMap.put(peerId, 0);
			}
		}
		LOGGER.info("Download size initilaized");
		LOGGER.info(" Does 1001 have the piece: " + DataHolder.getPeerPieceAvailiblity(1001, 0) + " num pieces "
				+ DataHolder.numOfPieces);

	}

	public static void initDownloadStartTimes() {
		synchronized (START_TIME_MUTEX) {
			for (int peerId : peerIds) {
				startTimes.put(peerId, (long) 0);
			}
		}
		LOGGER.info("Download start times initialized");
	}

	public static HashSet<Integer> genPrefferedNeighbors() {
		HashMap<Integer, Integer> curDownLoadMap = null;
		HashMap<Integer, Long> curStartTimeMap = null;
		long currentTime = System.currentTimeMillis();
		synchronized (DOWNLOAD_MAP_MUTEX) {
			curDownLoadMap = new HashMap<>(downloadMap);
			curStartTimeMap = new HashMap<>(startTimes);
			for (Entry<Integer, Integer> entry : downloadMap.entrySet()) {
				downloadMap.put(entry.getKey(), 0);
				startTimes.put(entry.getKey(), (long) 0);
			}
		}
		HashMap<Integer, Double> interestedPeersDownloadRates = new HashMap<>();
		synchronized (INTERESTED_PEERS_MUTEX) {
			for (int interestedPeer : interestedPeers) {
				LOGGER.info("Going to check download rate for Peer: " + interestedPeer);
				interestedPeersDownloadRates.put(interestedPeer, (double) (curDownLoadMap.get(interestedPeer)
						/ (currentTime - curStartTimeMap.get(interestedPeer))));
			}
		}
		LinkedHashMap<Integer, Double> sortedDownloadRates = (LinkedHashMap<Integer, Double>) Util
				.sortByValue(interestedPeersDownloadRates);
		int selectedPreferredNighbors = 0;
		HashSet<Integer> preferredNeighbors = new HashSet<>();
		for (Entry<Integer, Double> entry : sortedDownloadRates.entrySet()) {
			preferredNeighbors.add(entry.getKey());
			selectedPreferredNighbors++;
			if (selectedPreferredNighbors == numPreferredNeighbhor) {
				break;
			}
		}
		synchronized (PREFERRED_PEERS_MUTEX) {
			preferredPeers = preferredNeighbors;
		}
		return preferredNeighbors;
	}

	public static int updateDownloadSizeForPeer(int peerId, int additionalSize) {
		Integer curDownLoadSize = downloadMap.get(peerId);
		if (curDownLoadSize != null) {
			synchronized (DOWNLOAD_MAP_MUTEX) {
				downloadMap.put(peerId, curDownLoadSize + additionalSize);
			}
			return curDownLoadSize + additionalSize;
		}
		{
			return -1;
		}
	}

	public static long updateDownloadStartTimeForPeer(int peerId, long time) {
		Long startTime = startTimes.get(peerId);
		if (startTime != null) {
			if (startTime == 0) {
				synchronized (START_TIME_MUTEX) {
					startTimes.put(peerId, time);
				}
				LOGGER.info(MessageFormat.format("Reseted the download start time for peer: {0} to {1}", peerId, time));
			} else {
				if (LOGGER.isLoggable(Level.INFO)) {
					LOGGER.info(MessageFormat.format(
							"Not resetting the download start " + "time for peer: {0} as it's already set", peerId));
					time = 0;
				}
			}
			return time;
		} else {
			return -1;
		}
	}

	public static HashSet<Integer> genRandomPrefferedNeighbhors() {
		ArrayList<Integer> interested = null;
		synchronized (INTERESTED_PEERS_MUTEX) {
			interested = new ArrayList<>(interestedPeers);
		}
		Collections.shuffle(interested);
		HashSet<Integer> randomPreferred = new HashSet<>();
		for (int i = 0; i < numPreferredNeighbhor; i++) {
			if (i < interested.size())
				randomPreferred.add(interested.get(i));
			else
				break;
		}
		synchronized (PREFERRED_PEERS_MUTEX) {
			preferredPeers = randomPreferred;
		}
		return randomPreferred;
	}

	public static int genOptimisticPreferredNeighbors() {
		HashSet<Integer> chokedPeers = null;
		synchronized (INTERESTED_PEERS_MUTEX) {
			chokedPeers = new HashSet<>(interestedPeers);
		}
		for(int peerId: chokedPeers){
			LOGGER.info("Optimistic interested candidates: " + peerId);
		}
		synchronized (PREFERRED_PEERS_MUTEX) {
			chokedPeers.removeAll(preferredPeers);
		}
		for(int peerId: chokedPeers){
			LOGGER.info("Optimistic interested - prefereed: " + peerId);
		}
		synchronized (OPTIMISTIC_NEIGHBOR_MUTEX) {
			chokedPeers.remove(optimisticNeighbor);
		}
		for(int peerId: chokedPeers){
			LOGGER.info("Optimistic unchoke candidates: " + peerId);
		}
		ArrayList<Integer> candidatePeers = new ArrayList<>(chokedPeers);
		Collections.shuffle(candidatePeers);
		if (candidatePeers.size() > 0) {
			optimisticNeighbor = candidatePeers.get(0);
			return optimisticNeighbor;
		} else
			return -1;
	}
}
