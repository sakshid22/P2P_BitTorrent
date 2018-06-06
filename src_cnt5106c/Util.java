package cnt5106p2p;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cnt5106p2p.peerProcess.MessageType;
import cnt5106p2p.messages.HandShakeMessage;

public class Util {

	public static boolean verifyHandShakeMessage(int peerId, HandShakeMessage handShakeMessage) {
		if(peerId == handShakeMessage.peerId && handShakeMessage.protocol_signature.equals(Constants.PROTOCOL_SIGNATURE))
		{
			return true;
		}
		return false;
	}
	
	public static String exceptionToString(Exception exception) {
		/*
		StackTraceElement[] stack = exception.getStackTrace();
		String exceptionString = exception.getMessage() + "\n" + exception.getCause() + "\n";
		for (StackTraceElement element: stack) {
			exceptionString += element.toString() + "\n\t\t";
		}
		*/
		StringWriter sw = new StringWriter();
		exception.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	public static float calculateDownloadRate(long start, long end, int pieceSize) {
		float downloadRate = (float) (end - start)/(float)pieceSize; 
		return downloadRate;
	}

	public static MessageType getMessageType(short messageTypeShort) {
		
		switch (messageTypeShort) {
		case 0:
			return MessageType.CHOKE;
		case 1:
			return MessageType.UNCHOKE;
		case 2:
			return MessageType.INTERESTED;
		case 3:
			return MessageType.NOT_INTERESTED;
		case 4:
			return MessageType.HAVE;
		case 5:
			return MessageType.BIT_FIELD;
		case 6:
			return MessageType.REQUEST;
		case 7:
			return MessageType.PIECE;
		case -1:
			return MessageType.SHUTDOWN;
		default:
			return MessageType.UNDEFINED;
		}
		
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

}
