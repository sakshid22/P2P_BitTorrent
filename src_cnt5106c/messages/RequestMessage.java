package cnt5106p2p.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import cnt5106p2p.peerProcess.MessageType;

public class RequestMessage extends P2PMessages {
	
	public int pieceIndex;
	public RequestMessage() {
		super();
	}
	
	public RequestMessage(int index) {
		super( Short.BYTES + Integer.BYTES, MessageType.REQUEST);
		this.pieceIndex = index;
	}
	
	public RequestMessage(HaveAndRequestMessagePayload payload) {
		super( Short.BYTES + Integer.BYTES, MessageType.REQUEST);
		this.pieceIndex = payload.index;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		pieceIndex = (int)in.readObject();
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(pieceIndex);
	}
}
