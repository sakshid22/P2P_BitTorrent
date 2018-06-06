package cnt5106p2p.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import cnt5106p2p.Constants;

public class HandShakeMessage implements Externalizable {
	public String protocol_signature ;
	public int peerId;

	public HandShakeMessage()
	{
		// empty is required to work with Externalizable interface
	}
	
	public HandShakeMessage(int peerId) {
		this.peerId = peerId;
		this.protocol_signature = Constants.PROTOCOL_SIGNATURE;
	}

	public static String genHandShakeMessage(int peerId) {
		return String.format("%s%d", Constants.PROTOCOL_SIGNATURE, peerId);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		protocol_signature = (String)in.readObject();
		peerId = in.readInt(); 
	}


	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(protocol_signature);
		out.writeInt(peerId);
	}
}
