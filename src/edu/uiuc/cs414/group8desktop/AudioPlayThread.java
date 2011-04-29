package edu.uiuc.cs414.group8desktop;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.google.protobuf.ByteString;

import edu.uiuc.cs414.group8desktop.DataProto.DataPacket;
import edu.uiuc.cs414.group8desktop.DataProto.DataPacket.PacketType;

public class AudioPlayThread extends Thread {

	private WebcamInterface parent;
	Queue<DataPacket> q;
	
	
	public AudioPlayThread(WebcamInterface parent) {
		q = new LinkedBlockingQueue<DataPacket>(10);
		this.parent = parent;
	}
	
	public void run() {
		
		DataPacket packet;
		System.out.println("Server AudioPlayThread started...");
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
		      Mixer mixer = AudioSystem.getMixer(mixerInfo[1]);
			      System.out.println("Available mixers:");
			      for(int cnt = 0; cnt < mixerInfo.length;
			                                          cnt++){
			      	System.out.println(mixerInfo[cnt].
			      	                              getName());
			      }//end for loop
			     

			      DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
			SourceDataLine line = (SourceDataLine) mixer.getLine(dataLineInfo);
			
			System.out.println("AudioSystem.getLine succeeded");
			line.open(format);
			System.out.println("Line.open succeeded");
			line.start();
			
			System.out.println("Server Audio Play Thread opened the audio line successfully");
			//int bufferSize = (int)format.getSampleRate() * format.getFrameSize();
			int bufferSize = 1024;
			System.out.println("bufferSize selected as: " + bufferSize);
			byte buffer[] = new byte[bufferSize];
			while (true) {	
					while (q.size() > 10) q.remove();
					if ( !q.isEmpty() ) {
						packet = q.poll();
						//Log.d("Audio", "Grabbed some AUDIO packeterrr with timestamp " + packet.getTimestamp());
						byte[] audio = packet.getData().toByteArray();
						line.write(audio, 0, audio.length);
					}
				}
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}

	}
	public void queueFrame(DataPacket pkt) {
		q.add(pkt);
	}
}
