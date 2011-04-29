package edu.uiuc.cs414.group8desktop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket.ControlCode;
import edu.uiuc.cs414.group8desktop.DataProto.ControlPacket.ControlType;

public class ControlThread extends Thread {
	WebcamInterface parent;
	private final int port;
	private ServerSocket serverSock;
	private Socket clientSock;
	private DataOutputStream out = null;
	private DataInputStream in = null;
	private boolean isConnected = false;
	final static int MAX_LATENCY_MS = 500;
	
	public ControlThread(WebcamInterface parent, int port) {
		this.parent = parent;
		this.port = port;
	}
	
	public void run() {
		System.out.println("Server ControlThread started...");
		try {
			serverSock = new ServerSocket(port);
			clientSock = serverSock.accept();
			
			System.out.println("Control Successfully connected to client " + clientSock.getInetAddress().toString());
			isConnected = true;
			
			in = new DataInputStream(clientSock.getInputStream());
			out = new DataOutputStream(clientSock.getOutputStream());
			System.out.println("Entering control loop.");
			while (true) {
				int size = in.readInt();					
				System.out.println("Received control packet of size:"+size);
				byte[] bytes = new byte[size];
				in.readFully(bytes);

				ControlPacket pkt = ControlPacket.parseFrom(bytes);
				if (pkt.getType() == ControlPacket.ControlType.REMOTE) {
					runRemoteCtrl(pkt.getControl());
				}
				else if (pkt.getType() == ControlPacket.ControlType.LATENCY) {
					System.out.println("Latency:"+pkt.getLatency()+" ms");
				}
				else if (pkt.getType() == ControlPacket.ControlType.PING) {
					sendPing();
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendPing(){
		System.out.println("Received ping from phone...");
		try {
			ControlPacket pkt = ControlPacket.newBuilder()
			   .setType(ControlType.PING)
			   .setServertime((new Date()).getTime())
			   .build();
			int size = pkt.getSerializedSize();
			out.writeInt(size);
			out.write(pkt.toByteArray());
		} catch (IOException e) {
			System.out.println("Failed to send Ping back to phone");
			e.printStackTrace();
		}

	}
	
	public void runRemoteCtrl(ControlCode code){
		System.out.println("Running remote control...");
		Runtime rt = Runtime.getRuntime();
		//Process p;
		String str = "";
		switch(code) {
		case UP:
			str = "u";
			break;
		case DOWN:
			str = "d";
			break;
		case LEFT:
			str = "l";
			break;
		case RIGHT:
			str = "r";
			break;
		}
		try {
			rt.exec(System.getProperty("user.dir")+"/pantilt "+str);
		} catch (IOException e) {
			System.err.println("Unable to execute remote control command");
			e.printStackTrace();
		}
	}
	
	public boolean isConnected() {
		return isConnected;
	}
}
