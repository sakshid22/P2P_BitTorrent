package cnt5106p2p.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PieceMessagePayload implements Externalizable {
	public int pieceIndex;
	public byte[] content; 
	
	public PieceMessagePayload() {
	}
	
	public PieceMessagePayload(int pieceIndex, byte[] content) {
		this.pieceIndex = pieceIndex;
		this.content = content;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		pieceIndex = (int)in.readObject();
		content = (byte [])in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(pieceIndex);
		out.writeObject(content);
	}

}
