package edu.ufl.cise.p2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Peer implements Runnable {

	private CommonPeerProperties commonProps;
	private String id;
	private String host;
	private int port;
	private AtomicBoolean hasFile;
	private PeerHandler peerHandler;
	private FileHandler fileHandler;
	private Map<String, RemotePeer> remotePeerMap;
	private AtomicBoolean terminate;

	public Peer() {

	}

	public void init() {
		peerHandler.sendChokeAndUnchokeMessages();
		if (hasFile.get()) {
			fileHandler.splitFile(commonProps.getFileName());
			fileHandler.setAllPieces();
		} else {
			fileHandler.calculateRequiredPieces();
		}

	}

	public Peer(String id, String host, int port, Boolean hasFile,
			List<RemotePeer> remotePeers, CommonPeerProperties commonProps)
			throws NumberFormatException, IOException {
		this.id = id;
		this.host = host;
		this.port = port;
		this.hasFile = hasFile ? new AtomicBoolean(true) : new AtomicBoolean(
				false);
		this.remotePeerMap = new ConcurrentHashMap<String, RemotePeer>();
		for (RemotePeer rPeer : remotePeers) {
			remotePeerMap.put(rPeer.getPeerId(), rPeer);
		}
		this.commonProps = commonProps;
		this.peerHandler = new PeerHandler(new ArrayList<RemotePeer>(
				remotePeers), commonProps, this, Integer.parseInt(id));
		this.fileHandler = new FileHandler(commonProps.getPieceSize(),
				commonProps.getFileSize(), commonProps.getFileName(), id);
		this.terminate = new AtomicBoolean(false);
	}

	public CommonPeerProperties getCommonProps() {
		return commonProps;
	}

	public void setCommonProps(CommonPeerProperties commonProps) {
		this.commonProps = commonProps;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public AtomicBoolean getHasFile() {
		return hasFile;
	}

	public void setHasFile(AtomicBoolean hasFile) {
		this.hasFile = hasFile;
	}

	public void run() {
		// boolean shouldRun = true;
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Server started on address:" + host + " port:"
					+ port);
			while (!terminate.get()) {
				createNewConnection(serverSocket.accept(), id, "", false);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private PeerConnection createNewConnection(Socket socket, String peerId,
			String remotePeerId, boolean isClient) {
		PeerConnection connection = null;
		try {
			connection = new PeerConnection(socket, id, remotePeerId, isClient,
					fileHandler, remotePeerMap, this, peerHandler);
			new Thread(connection).start();

		} catch (IOException e) {
			System.out.println("IO Exception while creating a new Connection");
			e.printStackTrace();
		}
		return connection;
	}

	public void connectToRemotePeers(List<RemotePeer> remotePeers) {
		for (RemotePeer rPeer : remotePeers) {
			try {
				if (rPeer.getPeerId().compareTo(id) == 0)
					break;
				System.out.println("Connecting to peer:" + rPeer.getIpAddress()
						+ " at:" + rPeer.getPort());
				Socket socket = new Socket(rPeer.getIpAddress(),
						rPeer.getPort());
				PeerConnection connection = createNewConnection(socket, id,
						rPeer.getPeerId(), true);
				rPeer.setConnection(connection);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public AtomicBoolean getTerminate() {
		return terminate;
	}

	public void setTerminate(AtomicBoolean terminate) {
		this.terminate = terminate;
	}

}
