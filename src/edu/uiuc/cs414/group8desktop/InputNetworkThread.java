package edu.uiuc.cs414.group8desktop;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

public class InputNetworkThread extends Thread{
		WebcamInterface parent;
		private ServerSocket serverSock;
		private Socket clientSock;
		private DataInputStream input = null;
		private boolean isConnected = false;
		private Queue<DataPacket> sendQueue;
		private int port;
		private int size;
		final static int MAX_LATENCY_MS = 500;
		private long initTimestamp;
		
	    final static int nameserverPort = 3825;
	    final static String nameserverIP = "192.17.255.225";
		
		
		public InputNetworkThread(WebcamInterface parent, int port) {
			this.parent = parent;
			this.port = port;
			sendQueue = new LinkedBlockingQueue<DataPacket>(10);
		}
		
		public void run() {
			System.out.println("Server NetworkThread started...");
			try {
				serverSock = new ServerSocket(port);
				
				clientSock = serverSock.accept();
				System.out.println("In Network Successfully connected to client " + clientSock.getInetAddress().toString());
				isConnected = true;
				
				input = new DataInputStream(clientSock.getInputStream());
				//out = new ObjectOutputStream(clientSock.getOutputStream());
				System.out.println("Entering Input network loop.");
				while (true) {
				try{
					System.out.println("In thread before read: ");
					size = input.readInt();
					System.out.println("Recieved packet of size " + size);
					if (size < 0)
						continue;
					byte[] bytes = new byte[size];
					input.readFully(bytes);
					DataPacket pkt = DataPacket.parseFrom(bytes);
					if (initTimestamp == 0)
						initTimestamp = (new Date()).getTime();

					System.out.println("Recieved Packet");
					//Log.d("Stream", "Latency: " + ((pkt.getTimestamp() - ((new Date()).getTime() - initTimestamp))));
					if (pkt.getType() == DataPacket.PacketType.VIDEO) {
						System.out.println("Recieved Video Packet");
						//	videoHandler.queueFrame(pkt); // modified from parent
					}
					else if (pkt.getType() == DataPacket.PacketType.AUDIO) {
						System.out.println("Recieved Audio Packet");
						parent.audioplay.queueFrame(pkt); //modified from parent
					}
				} catch (IOException e) {
					System.out.println("IOException in receiving the packet. Message: " + e.getStackTrace());
					clientSock.close();
					return;
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void queuePacket(DataPacket pkt) {
			if (sendQueue.size() == 10) {
				for (int i=0; i<10; i++) sendQueue.remove();
			}
			//System.out.println("pkt with timestamp " + pkt.getTimestamp() + " queued. qsize: " + sendQueue.size());
			sendQueue.add(pkt);
		}
		
		public boolean isConnected() {
			return isConnected;
		}
	}
