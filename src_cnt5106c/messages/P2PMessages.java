package cnt5106p2p.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import cnt5106p2p.peerProcess.MessageType;
import cnt5106p2p.Util;

public class P2PMessages implements Externalizable{

	public int length;
	public short flag;
	
	public P2PMessages() {
		
	}
	
	public P2PMessages(int length, MessageType flag) {
		this.length = length;
		this.flag = flag.type;
	}
	
	
	public static P2PMessages genChokeMessage() {
		return new P2PMessages(1, MessageType.CHOKE);
	}
	
	public static P2PMessages genUnChokeMessages() {
		return new P2PMessages(1, MessageType.UNCHOKE);
	}
	
	public static P2PMessages genInterestedMessage() {
		return new P2PMessages(1, MessageType.INTERESTED);
	}
	
	public static P2PMessages genNotInterestedMessage() {
		return new P2PMessages(1, MessageType.NOT_INTERESTED);
	}
	
	public static MessageType getMessageType(P2PMessages p2pMessage) {
		return Util.getMessageType(p2pMessage.flag);
	}


	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		length = in.readInt();
		flag = in.readShort();
	}


	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(length);
		out.writeShort(flag);
	}

}
