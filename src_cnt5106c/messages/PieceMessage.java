package cnt5106p2p.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import cnt5106p2p.peerProcess.MessageType;

public class PieceMessage extends P2PMessages {
	public int pieceIndex;
	public byte[] content; 
	
	public PieceMessage() {
		super();
	}
	
	public PieceMessage(int pieceIndex, byte[] content) throws IOException, ClassNotFoundException{
		super(content.length + Short.BYTES + Integer.BYTES, MessageType.PIECE);
		this.pieceIndex = pieceIndex;
		this.content = content;
	}
	
	public PieceMessage(PieceMessagePayload payload) {
		super(payload.content.length + Short.BYTES + Integer.BYTES, MessageType.PIECE);
		this.pieceIndex = payload.pieceIndex;
		this.content = payload.content;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		//ToDO
		super.readExternal(in);
		pieceIndex = (int)in.readObject();
		content = (byte [])in.readObject();
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(pieceIndex);
		out.writeObject(content);
	}
	//TODO 
}
