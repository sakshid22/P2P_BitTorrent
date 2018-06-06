package cnt5106p2p.messages;


public class PeerHaveMessage extends HaveMessage {
	public int peerId;
	
	public PeerHaveMessage(int peerId, HaveMessage protoHaveMessage){
		super(protoHaveMessage.pieceIndex);
		this.peerId = peerId;
	}
}
