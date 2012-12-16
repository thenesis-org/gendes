package org.thenesis.gendes.contexts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine.Info;

import org.thenesis.gendes.EmulationSystem;
import org.thenesis.gendes.EmulationThread;
import org.thenesis.gendes.awt.Utilities;

public class AudioBackEnd {
    private EmulationSystem emulationSystem;
    private EmulationThread emulationThread;
	
    private boolean initialized=false;
    private boolean enabled=false;
    private int sampleRate=11025;
    
    // Output buffer.
    private int outputBufferLength;
    private int outputBufferLengthRemaining;
    private int outputBufferLengthAvailable;
    private int outputBufferLengthUsed;
    private byte outputBuffer[];
    
    // Audio presentation.
    private boolean lineStopped, lineHalted;
    private int lineBufferLength;
    private int lineOverflow, lineUnderflow;
    private float lineBufferLevel;
    private AudioFormat lineFormat;
    private Info lineInfo;    
    private SourceDataLine line;
    
    // Audio recording.
    private boolean recordingEnabled=false;
    private OutputStream recordingOutputStream=null;
    private final AudioFile file=new AudioFile();

    AudioBackEnd() {
    }
    
    public void attach(EmulationSystem es, EmulationThread et) {
    	boolean reinit = initialized;
    	if (emulationSystem != null) finalize();
    	emulationSystem = es;
    	emulationThread = et;
    	if (reinit) initialize();
    }
    
    public int getOutputBufferLength() {
    	return outputBufferLength;
    }

    public int getOutputBufferLengthRemaining() {
    	return outputBufferLengthRemaining;
    }

    public int getOutputBufferLengthAvailable() {
    	return outputBufferLengthAvailable;
    }

    public int getOutputBufferLengthUsed() {
    	return outputBufferLengthUsed;
    }

    public byte[] getOutputBuffer() {
    	return outputBuffer;
    }
    
    private boolean initialize() {
    	synchronized (emulationSystem) {
	        outputBufferLength=sampleRate/10;
	        outputBuffer=new byte[2*outputBufferLength];
	        outputBufferLengthAvailable = 0;
	        outputBufferLengthRemaining = 0;
	        outputBufferLengthUsed = 0;
	        
    		lineBufferLength=2*outputBufferLength;
	        lineFormat=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, 8, 2, 2, sampleRate, false);
	        lineInfo=new DataLine.Info(SourceDataLine.class, lineFormat, lineBufferLength<<1);
	        try {
	            line=(SourceDataLine)AudioSystem.getLine(lineInfo);
	            line.open(lineFormat, lineBufferLength<<1);
	            initialized=true;
		        lineStopped=(emulationThread == null) ? true : emulationThread.isPaused();
		        lineHalted=true;
		        lineOverflow=0; lineUnderflow=0;
		        lineBufferLevel=0.0f;
		        if (!lineStopped) line.start();
		        return false;
	        } catch (LineUnavailableException e) {
//	            System.out.println("Cannot open an audio line.");
	            finalize();
	            return true;
	        }
    	}
    }
    
    public void finalize() {
    	if (emulationSystem == null) return;
    	synchronized (emulationSystem) {
	    	stopRecording();

	    	initialized=false;

	    	lineStopped=true;
	        lineHalted=true;
	        lineFormat=null;
	        lineInfo=null;
	        if (line!=null) {
	        	line.stop();
	        	line.flush();
	            line.close();
	            line=null;
	        }
	        lineOverflow=0; lineUnderflow=0;
	        lineBufferLevel=0.0f;

	        outputBufferLength=0;
	        outputBufferLengthRemaining=0;
	        outputBufferLengthAvailable=0;
	        outputBufferLengthUsed=0;
	        outputBuffer=null;
    	}
    }
    
    public void reset() {
    	if (emulationSystem == null) return;
    	synchronized (emulationSystem) {
    		if (enabled) {
	    		finalize();
	    		initialize();
    		}
    	}
    }
    
    public void update() {
        if (!initialized || lineStopped) return;
        // Length generated (l), available (la), used (lu) and remaining (lr).
    	int l=outputBufferLength, la=line.available()>>1, lu=l;
    
    	lineBufferLevel=(float)(lineBufferLength-la)/(float)lineBufferLength;
    	if (!line.isRunning() && !lineHalted) lineUnderflow=(lineUnderflow+1)&0x7fffffff;
        lineHalted=false;
        
    	if (la==0) lineOverflow=(lineOverflow+1)&0x7fffffff;
    	else if (l>la) lu=la;

    	// Record.
        if (recordingEnabled) {
        	if (file.write(0, lu<<1, outputBuffer)) stopRecording();
        }
        
        // Play.
		if (la!=0) line.write(outputBuffer, 0, lu<<1);
		
        // Shift the buffer if necessary.
		int lr=l-lu;
        if (lr>0) {
        	int lr2=lr<<1, lu2=lu<<1;
    		for (int i=0; i<lr2; i++) outputBuffer[i]=outputBuffer[lu2+i]; // Shift. TODO: use a circular buffer.
        }
        
        outputBufferLengthRemaining = lr;
        outputBufferLengthAvailable = la;
        outputBufferLengthUsed = lu;
    }
    
    public boolean isEnabled() {
    	return enabled;
    }

    public void enable(boolean flag) {
    	if (emulationSystem == null) return;
        synchronized (emulationSystem) {
	    	if (flag) {
	    		if (!enabled) { initialize(); enabled=true; }
	    	} else {
	    		if (enabled) { finalize(); enabled=false; }
	    	}
        }
    }
    
    public int getFrequency() {
    	return sampleRate;
    }
    
    public void setFrequency(int frequency) {
        switch (frequency) {
        case 11025: case 22050: case 44100: break;
        default: return;
        }
        
        synchronized (emulationSystem) {	        
        	sampleRate=frequency;
        	reset();
        }
    }
    
    public boolean isStarted() {
    	return !lineStopped;
    }
    
    public void start() {
    	if (emulationSystem == null) return;
    	synchronized (emulationSystem) {
    		if (lineStopped && initialized) {
	    		lineStopped=false;
	    		line.start();
    		}
    	}
    }
    
    public void stop() {
    	if (emulationSystem == null) return;
    	synchronized (emulationSystem) {
    		if (!lineStopped && initialized) {
	            lineStopped=true;
	    		line.stop();
    		}
    	}
    }
    
    public boolean isRecording() {
    	return recordingEnabled;
    }
    
    public void startRecording(String fileName) {
    	if (emulationSystem == null) return;
    	stopRecording();
    	synchronized (emulationSystem) {
        	if (!initialized) return;
        	
        	String audioFilename=Utilities.replaceFileExtension(fileName, ".au");        	
            try {
            	File f=new File(audioFilename);
            	recordingOutputStream=new FileOutputStream(f);
            	file.start(recordingOutputStream, sampleRate);
            } catch (IOException e) {
//	                System.out.println("Cannot open a file for audio recording.");
                return;
            }
        	recordingEnabled=true;
    	}
    }
    
    public void stopRecording() {
    	if (emulationSystem == null) return;
    	synchronized (emulationSystem) {
        	if (!initialized || !recordingEnabled) return;
        	
	        try {
	        	file.stop();
	        	recordingOutputStream.close();
	        } catch (IOException e) {}
	        recordingOutputStream=null;
	        recordingEnabled=false;
    	}
    }
/*
    public synchronized void update(int length, byte data[]) {
        if (!initialized) return;
    	
        long currentTimer=timer.getTimer();
        float dt=(float)timer.computeDifference(lastUpdateTimer, currentTimer);
       	int la=line.available()>>1, lr=lineBufferLength-la; // Length available (la) and remaining (lr).

		lastUpdateInputSamples+=length;            
        if (dt>=updatePeriod) {
            inputRate=(lastUpdateInputSamples)/dt;
            lineRate=(lastUpdateOutputSamples-lr)/dt;
            lastUpdateTimer=currentTimer;
            lastUpdateInputSamples=0;
            lastUpdateOutputSamples=lr;
        }
        
    	lineBufferLevel=(float)(lineBufferLength-la)/(float)lineBufferLength;
    	if (!line.isRunning()) lineUnderflow=(lineUnderflow+1)&0x7fffffff;            
    	if (la<length) {
    		lineOverflow=(lineOverflow+1)&0x7fffffff;
    	} else {
    		lastUpdateOutputSamples+=length;
    		line.write(buffer, 0, length<<1);
    	}

    }

 */
    
    public float getBufferLevel() {
    	return lineBufferLevel;
    }
    
    public float getLatency() {
    	if (sampleRate <= 0) return 0.0f;
    	return lineBufferLevel*(float)lineBufferLength/(float)sampleRate;
    }
    
    public int getUnderflowCounter() {
    	return lineUnderflow;
    }

    public int getOverflowCounter() {
    	return lineOverflow;
    }
}
