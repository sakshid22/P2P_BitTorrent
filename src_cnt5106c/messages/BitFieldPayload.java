package cnt5106p2p.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BitFieldPayload implements Externalizable {

	String payload;
	
	public BitFieldPayload() {
		// TODO Auto-generated constructor stub
	}
	
	public BitFieldPayload(String payload) {
		this.payload = payload;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		payload = (String)in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(payload);
	}
	
}
