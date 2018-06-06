# NetworksProject
Group Members: Vijay Prakash, Sakshi Dubey, Anthony Colas

## Setup and Start
Setup the connections to your peers in the PeerInfo.cfg and the StartRemotePeers.java file.

This includes CISE username, private key path, and network addresses (which will be the peers).

Configuration for the File/Common settings and Peer settings should also be setup before start and are described below.

### Compile Command
```
javac -cp "src:lib/\*" -sourcepath src src/cnt5106p2p/StartRemotePeers.java
```

### Run Command
```
java -cp "src:lib/\*" cnt5106p2p.StartRemotePeers [user ID] [private key]
```

### Kill any running java processes
```
for pid in `ps -ef | grep java | awk '{print $2}/'` ; do kill -9 $pid ; done
```

### Clean Class Files Command
```
find src/ -name \"\*\.class\" -type f -delete
```

## Input
The only "input" is a file which will be shared by the peers. This file's name should also be in the Common.cfg file as (without the quotes) "FileName [tab] [name_of_file.extension]"

## Common.cfg
The Common.cfg file has the following parameters:
*NumberOfPreferredNeighbors*
*UnchokingInterval* (in seconds)
*OptimisticUnchokingInterval* (in seconds)
*FileName*
*FileSize* (in bytes)
*PieceSize* (in bytes)

For an example please see the Common.cfg file which already has defined parameters.

## PeerInfo.cfg
The PeerInfo.cfg file has the following parameters:
*[peer ID]* *[host name]* *[listening port]* *[has file or not]*

## Output
There are three main output components: The piece files, merged file, and log files.

There are ceiling(FileSize/PieceSize) number of piece files and one merged file. The merged file is the same file which is selected as the FileName in Common.cfg. The piece files and merged file are contained in their respective peer folder, e.g., peer_1001 if the peer ID is 1001. Log files are stored in the main project directory as log_peer_[peer ID].log and store the logs of what each peer performs.

## Logs
Log files contain logs for the following:
Main Logs:
1. TCP connection: Whenever a peer makes a TCP connection to other peer, it generates the following log: [Time]: Peer [peer_ID 1] makes a connection to Peer [peer_ID 2].
2. Change of preferred neighbors: Whenever a peer changes its preferred neighbors, it generates the following log: [Time]: Peer [peer_ID] has the preferred neighbors [preferred neighbor ID list].
3. Change of optimistically unchoked neighbor: Whenever a peer changes its optimistically unchoked neighbor, it generates the following log:
[Time]: Peer [peer_ID] has
4. Unchoking: Whenever a peer is unchoked by a neighbor (which means when a peer receives an unchoking message from a neighbor), it generates the following log:
[Time]: Peer [peer_ID 1] is unchoked by [peer_ID 2].
5. codehoking: Whenever a peer is choked by a neighbor (which means when a peer receives a choking message from a neighbor), it generates the following log:
[Time]: Peer [peer_ID 1] is choked by [peer_ID 2].
6. Receiving ‘have’ message: Whenever a peer receives a ‘have’ message, it generates the following log:
[Time]: Peer [peer_ID 1] received the ‘have’ message from [peer_ID 2] for the piece [piece index].
7. Receiving ‘interested’ message: Whenever a peer receives an ‘interested’ message, it generates the following log: [Time]: Peer [peer_ID 1] received the ‘interested’ message from [peer_ID 2].
8. Receiving ‘not interested’ message: Whenever a peer receives a ‘not interested’ message, it generates the following log: [Time]: Peer [peer_ID 1] received the ‘not interested’ message from [peer_ID 2].
9. Downloading a piece: Whenever a peer finishes downloading a piece, it generates the following log:
[Time]: Peer [peer_ID 1] has downloaded the piece [piece index] from [peer_ID 2]. Now the number of pieces it has is [number of pieces].
10. Completion of download: Whenever a peer downloads the complete file, it generates the following log: [Time]: Peer [peer_ID] has downloaded the complete file.

We also have logs to keep track of more specific functionalities in the code.


## Project Overview
In this project, you are asked to write a P2P file sharing software similar to BitTorrent. You can complete the project in Java or C/C++. There will be no extra credit for C/C++.
BitTorrent is a popular P2P protocol for file distribution. Among its interesting features, you are asked to implement the choking-unchoking mechanism which is one of the most important features of BitTorrent. In the following Protocol Description section, you can read the protocol description, which has been modified a little bit from the original BitTorrent protocol. After reading the protocol description carefully, you must follow the implementation specifics shown in the Implementation Specifics section.

## Protocol Description
This section outlines the protocol used to establish the file management operations between peers. All operations are assumed to be implemented using a reliable transport protocol (i.e. TCP). The interaction between two peers is symmetrical: Messages sent in both directions look the same.
The protocol consists of a handshake followed by a never-ending stream of length- prefixed messages.
Whenever a connection is established between two peers, each of the peers of the connection sends to the other one the handshake message before sending other messages.

See project_description.pdf for me more details.
