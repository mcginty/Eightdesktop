package edu.uiuc.cs414.group8desktop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;

public class NetworkThread extends Thread {
	WebcamInterface parent;
	private final int port;
	private ServerSocket serverSock;
	private Socket clientSock;
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private boolean isConnected = false;
	private Queue<DataPacket> sendQueue;
	final static int MAX_LATENCY_MS = 500;
	
    final static int nameserverPort = 3825;
    final static String nameserverIP = "192.17.255.225";
	
	
	public NetworkThread(WebcamInterface parent, int port) {
		this.parent = parent;
		this.port = port;
		sendQueue = new LinkedBlockingQueue<DataPacket>(10);
	}
	
	public void run() {
		System.out.println("Server NetworkThread started...");
		try {		
			//nameserverConnect("alice","add",nameserverIP,nameserverPort);
			
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
					System.out.println("Latency: "+ (((new Date()).getTime() - parent.initialTimestamp) - pkt.getTimestamp()));
					if (((new Date()).getTime() - parent.initialTimestamp) - pkt.getTimestamp() < MAX_LATENCY_MS) {
						int size = pkt.getSerializedSize();
						System.out.println("Sending a packet of size " + size + " and type " + pkt.getType().toString() + " to client.");
						out.writeInt(size);
						out.write(pkt.toByteArray());
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String nameserverConnect(String sname, String stype, String nsIP, int nsPort) throws IOException {
		String TrimmedIP;
	    Socket nssock;
	    DataInputStream nsinput;
	    DataOutputStream nsoutput;

		System.out.println("Hello World");
	    
		while(true){ //Try again if this was the server's first connection
			nssock = new Socket(nsIP, nsPort);
		
			nsoutput = new DataOutputStream(nssock.getOutputStream());
			nsinput = new DataInputStream(nssock.getInputStream());

			System.out.println("Connected to nameserver");
			
			byte[] name = new byte[16];
			byte[] tname = sname.getBytes("US-ASCII");
			byte[] type = new byte[16];
			byte[] ttype = stype.getBytes("US-ASCII");
			byte[] ip = new byte[48];
			
			java.lang.System.arraycopy(tname, 0, name, 0, tname.length);
			java.lang.System.arraycopy(ttype, 0, type, 0, ttype.length);
			
			nsoutput.write(name);
			System.out.println("Name Written: " + name.length + " bytes");				
			nsoutput.write(type);
			System.out.println("Type Written: " + type.length + " bytes");			
			nsinput.readFully(ip);

			System.out.println("Communicated with nameserver");
			
			String ServerIP = new String(ip,0,0,48);
			ServerIP.trim();
			TrimmedIP = ServerIP.replace("\0", "");

			nssock.close();
			nsoutput.close();
			nsinput.close();
			
			System.out.println("Nameserver connection closed");
			System.out.println("nameserver IP " + ServerIP );

			if(ServerIP.codePointAt(0) == 'x')
				continue;
			else
				break;
			}
		
		return TrimmedIP;
	}
	
	public void queuePacket(DataPacket pkt) {
		if (sendQueue.size() == 10) {
			for (int i=0; i<10; i++) sendQueue.remove();
		}
		System.out.println("pkt with timestamp " + pkt.getTimestamp() + " queued. qsize: " + sendQueue.size());
		sendQueue.add(pkt);
	}
	
	public boolean isConnected() {
		return isConnected;
	}
}
