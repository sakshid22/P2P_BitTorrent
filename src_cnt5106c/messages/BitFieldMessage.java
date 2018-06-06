package cnt5106p2p.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import cnt5106p2p.peerProcess.MessageType;

public class BitFieldMessage extends P2PMessages {

	public String payload;
	
	public BitFieldMessage() {
		
	}
	
	public BitFieldMessage(String indices) {
		super(indices.length() + Short.BYTES, MessageType.BIT_FIELD);
		this.payload = indices;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		payload = (String)in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(payload);
	}
	
}
