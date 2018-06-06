package cnt5106p2p.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class HaveAndRequestMessagePayload implements Externalizable {

	int index;
	
	public HaveAndRequestMessagePayload() {
	}
	
	public HaveAndRequestMessagePayload( int index) {
		this.index = index;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		index = in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(index);
	}

}
