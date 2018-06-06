package cnt5106p2p;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class StartRemotePeers {

	//private static final String userName = "vprakash";

	// private static final String scriptPrefix = "java -cp
	// .:/cise/homes/acolas/CN2NetworksProject/lib/jsch-0.1.54.jar
	// cnt5106p2p.peerProcess ";
	private static final String PEER_INFO_CONFIG_FILE = "configuration/PeerInfo.cfg";

	public static class PeerInfo {

		private String peerID;
		private String hostName;

		public PeerInfo(String peerID, String hostName) {
			super();
			this.peerID = peerID;
			this.hostName = hostName;
		}

		public String getPeerID() {
			return peerID;
		}

		public void setPeerID(String peerID) {
			this.peerID = peerID;
		}

		public String getHostName() {
			return hostName;
		}

		public void setHostName(String hostName) {
			this.hostName = hostName;
		}

	}

	public static void main(String[] args) {
		ArrayList<PeerInfo> peerList = new ArrayList<>();

		// String ciseUser = "vprakash"; // change with your CISE username
		if (args.length != 2) {
			System.out.println("Please provide the username and identity-key as arguments");
			System.out.println(StartRemotePeers.class.getName() + " username path_to_identity_key");
			System.exit(1);
		}
		String ciseUser = args[0];
		String identityKeyPath = args[1];
		String scriptPrefix = "cd NetworksProject/src/; javac -cp .:/cise/homes/" + ciseUser
				+ "/NetworksProject/lib/jsch-0.1.54.jar cnt5106p2p/*.java; java -cp .:/cise/homes/" + ciseUser
				+ "/NetworksProject/lib/jsch-0.1.54.jar cnt5106p2p.peerProcess ";

		/**
		 * Make sure the below peer hostnames and peerIDs match those in
		 * PeerInfo.cfg in the remote CISE machines. Also make sure that the
		 * peers which have the file initially have it under the 'peer_[peerID]'
		 * folder.
		 */
		try {
			BufferedReader br = new BufferedReader(new FileReader(PEER_INFO_CONFIG_FILE));
			String line;
			while ((line = br.readLine()) != null) {
				String[] peerConfigs = line.split(" ");
				peerList.add(new PeerInfo(peerConfigs[0], peerConfigs[1]));
				System.out.println("parsed peerId: " + peerConfigs[0] + " and peer hostname: " + peerConfigs[1]);
			}
			/*
			 * peerList.add(new PeerInfo("1001", "lin114-11.cise.ufl.edu"));
			 * peerList.add(new PeerInfo("1002", "lin114-09.cise.ufl.edu"));
			 * peerList.add(new PeerInfo("1003", "lin114-08.cise.ufl.edu"));
			 * peerList.add(new PeerInfo("1004", "lin114-07.cise.ufl.edu"));
			 * peerList.add(new PeerInfo("1005", "lin114-06.cise.ufl.edu"));
			 */
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (PeerInfo remotePeer : peerList) {
			try {
				JSch jsch = new JSch();
				/*
				 * Give the path to your private key. Make sure your public key
				 * is already within your remote CISE machine to ssh into it
				 * without a password. Or you can use the corressponding method
				 * of JSch which accepts a password.
				 */
				// jsch.addIdentity("C:\\Users/vjpra/AppData/Local/lxss/home/vijay/.ssh/id_rsa_2018",
				// "");
				//jsch.addIdentity("/home/vijay/.ssh/id_rsa_2018", "");
				jsch.addIdentity(identityKeyPath, "");
				Session session = jsch.getSession(ciseUser, remotePeer.getHostName(), 22);
				Properties config = new Properties();
				config.put("StrictHostKeyChecking", "no");
				session.setConfig(config);

				session.connect();

				System.out.println("Session to peer# " + remotePeer.getPeerID() + " at " + remotePeer.getHostName());

				Channel channel = session.openChannel("exec");
				System.out.println("remotePeerID" + remotePeer.getPeerID());
				// ((ChannelExec) channel).setCommand(changeDir);
				// ((ChannelExec) channel).setCommand(scriptCompile);
				((ChannelExec) channel).setCommand(scriptPrefix + remotePeer.getPeerID());

				channel.setInputStream(null);
				((ChannelExec) channel).setErrStream(System.err);

				InputStream input = channel.getInputStream();
				channel.connect();

				System.out.println("Channel Connected to peer# " + remotePeer.getPeerID() + " at "
						+ remotePeer.getHostName() + " server with commands");

				(new Thread() {
					@Override
					public void run() {

						InputStreamReader inputReader = new InputStreamReader(input);
						BufferedReader bufferedReader = new BufferedReader(inputReader);
						String line = null;

						try {

							while ((line = bufferedReader.readLine()) != null) {
								System.out.println(remotePeer.getPeerID() + ">:" + line);
							}
							bufferedReader.close();
							inputReader.close();
						} catch (Exception ex) {
							System.out.println(remotePeer.getPeerID() + " Exception >:");
							ex.printStackTrace();
						}

						channel.disconnect();
						session.disconnect();
					}
				}).start();

			} catch (JSchException e) {
				// TODO Auto-generated catch block
				System.out.println(remotePeer.getPeerID() + " JSchException >:");
				e.printStackTrace();
			} catch (IOException ex) {
				System.out.println(remotePeer.getPeerID() + " Exception >:");
				ex.printStackTrace();
			}

		}
	}

}
