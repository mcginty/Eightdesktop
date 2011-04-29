package edu.uiuc.cs414.group8desktop;

import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import processing.core.PApplet;
import processing.core.PImage;
import codeanticode.gsvideo.GSCapture;

import com.google.protobuf.ByteString;

import controlP5.ControlEvent;
import controlP5.ControlP5;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket.PacketType;



public class WebcamInterface extends PApplet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	GSCapture cam;
	long initialTimestamp;
	long newTimestamp;
	long deltaTimestamp;
	ControlP5 controlP5;
	Mixer mixer;
	NetworkThread net;
	AudioThread audio;
	ControlThread control;
	InputNetworkThread innet;
	AudioPlayThread audioplay;
	secondApplet s;
	Queue<DataPacket> videoQueue;
	
	int outBW = 0;
	int inBW = 0;
	
	public void setup() {
		size(320, 240);
		frameRate(5);
		cam = new GSCapture(this, 320, 240, "/dev/video0");
		int[][] res = cam.resolutions();
		for (int i = 0; i < res.length; i++) {
			println(res[i][0] + "x" + res[i][1]);
		}
		String[] fps = cam.framerates();
		for (int i = 0; i < fps.length; i++) {
			println(fps[i]);
		}
		
	      Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
	      mixer = AudioSystem.getMixer(mixerInfo[0]);
		
	    videoQueue = new LinkedBlockingQueue<DataPacket>();
		net = new NetworkThread(this, 6666);
		net.start();

		initialTimestamp = 0;
		newTimestamp = 0;
		
		audio = new AudioThread(this);
		audio.start();
		
		control = new ControlThread(this, 6667);
		control.start();
		
		Timer bwTimer = new Timer();
        bwTimer.scheduleAtFixedRate(new BwTask(), 1000, 1000); 
		
		innet = new InputNetworkThread(this, 6668);
		innet.start();
		
		audioplay = new AudioPlayThread(this);
		audioplay.start();
		
		new PFrame();
	}
	
	private class BwTask extends TimerTask {

		@Override
		public void run() {
			if(net.isConnected()) {
				System.out.println("Outgoing bandwidth: "+outBW+"Bytes/Sec");
				outBW = 0;
				System.out.println("Incoming bandwidth: "+inBW+"Bytes/Sec");
				inBW = 0;
			}
		}
	}
	
	public void updateOutBandwidth(int newBand) {
		outBW += newBand;
	}
	
	/**
	 * Processing's main draw function. Everything UI-controlling goes into this function.
	 */
	public void draw() {
		if (cam.available()) {
			long latencyTime = (new Date()).getTime();
			newTimestamp = (new Date()).getTime();
			deltaTimestamp = newTimestamp - initialTimestamp;
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
					e.printStackTrace();
				}
				//newTimestamp = (new Date()).getTime();
			    ByteString buf = ByteString.copyFrom(bytes);
				DataPacket proto = DataPacket.newBuilder()
									.setTimestamp(deltaTimestamp)
									.setServertime(latencyTime)
									.setType(PacketType.VIDEO)
									.setData(buf).build();
				if (initialTimestamp != 0)
					net.queuePacket(proto);
		    }
		}
	}
	
	public class PFrame extends Frame {
		private static final long serialVersionUID = -1600857993908936580L;

		public PFrame() {
			setBounds(320, 0, 432,768);
			s = new secondApplet();
			add(s);
			s.init();
			setVisible(true); // was show();
		}
	}

	public class secondApplet extends PApplet {
		private static final long serialVersionUID = -3637569539554282527L;

		public void setup() {
			size(432,768);
			frameRate(15);
		}

		public void draw() {
			if (!videoQueue.isEmpty()) {
				DataPacket pkt = videoQueue.poll();
				
				loadPixels();
				
				byte []imgdata = pkt.getData().toByteArray();
				
				
				
				for (int i=0; i<imgdata.length; i+=2) {
					short imgpixel = imgdata[2*i];
					int color = ((imgpixel<<3)>>3);
					pixels[i] = color(color, color, color);
				}
				updatePixels();
				
				System.out.println("Video packet received of type: " + pkt.getSerializedSize());
				

			}
			
			redraw();
		}
	}
	
	public void mousePressed() {
		println("clicky");
	}
	
	public void controlEvent(ControlEvent theEvent) {
		println(theEvent.controller().name());
	}
	
	public void Start(int theValue) {
		println("a button event from Start: "+theValue);
	}
	
	/**
	 * For when this is run as an application instead of an applet.
	 * @param args
	 */
	public static void main(String args[]) {
	    PApplet.main(new String[] { "--present", "edu.uiuc.cs414.group8desktop.WebcamInterface" });
	  }
}
