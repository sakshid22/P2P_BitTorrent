package cnt5106p2p.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import cnt5106p2p.peerProcess.MessageType;

public class HaveMessage extends P2PMessages {
	
	public int pieceIndex;
	
	public HaveMessage(int pieceIndex){
		super( Short.BYTES + Integer.BYTES, MessageType.HAVE);
		this.pieceIndex = pieceIndex;
	}
	
	public HaveMessage() {
		super();
	}
	
	public HaveMessage(HaveAndRequestMessagePayload payload) {
		super( Short.BYTES + Integer.BYTES, MessageType.HAVE);
		this.pieceIndex = payload.index;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		//TODO
		super.readExternal(in);
		pieceIndex = (int)in.readObject();
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(pieceIndex);
	}
}
