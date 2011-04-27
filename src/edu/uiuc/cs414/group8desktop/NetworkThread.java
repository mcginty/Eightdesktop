package edu.uiuc.cs414.group8desktop;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

public class NetworkThread extends Thread {
	private final int port;
	private ServerSocket serverSock;
	private Socket clientSock;
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private boolean isConnected = false;
	private Queue<DataPacket> sendQueue;
	
	public NetworkThread(int port) {
		this.port = port;
		sendQueue = new LinkedBlockingQueue<DataPacket>();
	}
	
	public void run() {
		System.out.println("Server NetworkThread started...");
		try {
			serverSock = new ServerSocket(port);
			clientSock = serverSock.accept();
			
			System.out.println("Successfully connected to client " + clientSock.getInetAddress().toString());
			isConnected = true;
			
			//in = new ObjectInputStream(clientSock.getInputStream());
			out = new ObjectOutputStream(clientSock.getOutputStream());
			System.out.println("Entering loop.");
			while (true) {
				//System.out.println("Attempting write of bytes.");
				if (!sendQueue.isEmpty()) {
					DataPacket pkt = sendQueue.poll();
					int size = pkt.getSerializedSize();
					System.out.println("Sending a packet of size " + size + " to client.");
					out.writeInt(size);
					out.write(pkt.toByteArray());
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void queuePacket(DataPacket pkt) {
		System.out.println("pkt with timestamp " + pkt.getTimestamp() + " queued. qsize: " + sendQueue.size());
		sendQueue.add(pkt);
	}
	
	public boolean isConnected() {
		return isConnected;
	}
}
