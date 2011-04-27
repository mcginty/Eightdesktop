package edu.uiuc.cs414.group8desktop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.concurrent.Executors;

import com.google.protobuf.ByteString;

import codeanticode.gsvideo.GSCapture;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket.PacketType;
import processing.core.*;
public class WebcamInterface extends PApplet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	GSCapture cam;
	long initialTimestamp;
	
	NetworkThread net;
	
	public void setup() {
		size(320, 240);
		frameRate(5);
		cam = new GSCapture(this, 320, 240, "/dev/video1");
		int[][] res = cam.resolutions();
		for (int i = 0; i < res.length; i++) {
			println(res[i][0] + "x" + res[i][1]);
		}
		String[] fps = cam.framerates();
		for (int i = 0; i < fps.length; i++) {
			println(fps[i]);
		}
		net = new NetworkThread(this, 6666);
		net.start();
		
		initialTimestamp = 0;
	}
	
	/**
	 * Processing's main draw function. Everything UI-controlling goes into this function.
	 */
	public void draw() {
		if (cam.available()) {
			cam.read();
		    
			set(0, 0, cam); // set() is faster than image() for no-modification stuff. Good as it gets.
		    
			if (net.isConnected()) {
			    /**
			     * Using save to write to a temporary jpg file, using processing's built in JPEG compression.
			     * This file will nearly-immediately be read from and spewed through socketmagic too the receiver.
			     */
			    save("imgtemp.jpg");
			    
			    /**
			     * Immediately read the file back, and create a packet to queue up for network sending.
			     */
				byte[] bytes = null;
				try {
					File file = new File("imgtemp.jpg");
					InputStream is = new FileInputStream(file);
					bytes = new byte[(int) file.length()];
				    // Read in the bytes
				    int offset = 0;
				    int numRead = 0;
					while (offset < bytes.length
					       && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
					    offset += numRead;
					}
				    if (offset < bytes.length) {
				        throw new IOException("Could not completely read file "+file.getName());
				    }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (initialTimestamp == 0) initialTimestamp = (new Date()).getTime();
			    ByteString buf = ByteString.copyFrom(bytes);
				DataPacket proto = DataPacket.newBuilder()
									.setTimestamp((new Date()).getTime() - initialTimestamp)
									.setServertime((new Date()).getTime() - initialTimestamp)
									.setType(PacketType.VIDEO)
									.setData(buf).build();
				
				net.queuePacket(proto);
		    }
		}
	}
	
	public void mousePressed() {
		println("clicky");
	}
	
	/**
	 * For when this is run as an application instead of an applet.
	 * @param args
	 */
	public static void main(String args[]) {
	    PApplet.main(new String[] { "--present", "edu.uiuc.cs414.group8desktop.WebcamInterface" });
	  }
}
