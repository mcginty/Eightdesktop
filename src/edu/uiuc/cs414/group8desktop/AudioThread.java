package edu.uiuc.cs414.group8desktop;

import java.util.Date;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.google.protobuf.ByteString;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket.PacketType;

public class AudioThread extends Thread {
	AudioInputStream audioInput;
	WebcamInterface parent;
	
	public AudioThread(WebcamInterface parent) {
		this.parent = parent;
	}
	
	public void run() {
		System.out.println("Server AudioThread started...");
		try {
			float sampleRate = 8000;
			int sampleSize = 16;
			int channels = 1;
			boolean signed = true;
			boolean bigEndian = false;
			AudioFormat format =  new AudioFormat(
												sampleRate, 
												sampleSize, 
												channels, 
												signed, 
												bigEndian);
			
		      Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
		      Mixer mixer = AudioSystem.getMixer(mixerInfo[2]);
			
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			TargetDataLine line = (TargetDataLine) mixer.getLine(info);
			line.open(format);
			line.start();
			//int bufferSize = (int)format.getSampleRate() * format.getFrameSize();
			int bufferSize = 4096;
			System.out.println("bufferSize selected as: " + bufferSize);
			byte buffer[] = new byte[bufferSize];
			while (true) {
				if (parent.initialTimestamp == 0)
					parent.initialTimestamp = (new Date()).getTime();
				line.read(buffer, 0, bufferSize);
				if (parent.net.isConnected()) {
					ByteString buf = ByteString.copyFrom(buffer);
					DataPacket proto = DataPacket.newBuilder()
						.setTimestamp((new Date()).getTime() - parent.initialTimestamp)
						.setServertime((new Date()).getTime() - parent.initialTimestamp)
						.setType(PacketType.AUDIO)
						.setData(buf).build();
					parent.net.queuePacket(proto);
				}
			}
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}

	}
}
