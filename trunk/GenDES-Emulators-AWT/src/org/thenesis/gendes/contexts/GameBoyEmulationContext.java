package org.thenesis.gendes.contexts;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine.Info;

import org.thenesis.gendes.awt.Utilities;
import org.thenesis.gendes.cpu_gameboy.CPU;
import org.thenesis.gendes.cpu_gameboy.CPUEvent;
import org.thenesis.gendes.cpu_gameboy.CPUListener;
import org.thenesis.gendes.gameboy.CartridgeInfos;
import org.thenesis.gendes.gameboy.GameBoy;
import org.thenesis.gendes.gameboy.GameBoyEvent;
import org.thenesis.gendes.gameboy.GameBoyListener;


public final class GameBoyEmulationContext extends Context {
    public SystemEmulationContext systemContext;
    public GameBoy gameBoy;
    
    private final ContextNode node=new ContextNode() {
    	public void detach() { GameBoyEmulationContext.this.detach(); }
    };
    
    // Cartridge.
    public String cartridgeFilename;
    public boolean cartridgeAutoRunFlag=true;
    
    // CPU.
    public boolean cpuTraceInterrupts;
    public boolean cpuBreakOnIllegalOpcode=true;
    
    // Audio.
    public boolean audioInitialized=false;
    public boolean audioEnabled=false;
    public int audioSampleRate=11025;
    private int audioOutputBufferLength;
    private byte audioOutputBuffer[];
    // Audio presentation.
    public boolean audioLineStopped, audioLineHalted;
    public int audioLineBufferLength;
    public int audioLineOverflow, audioLineUnderflow;
    public float audioLineBufferLevel;
    private AudioFormat audioLineFormat;
    private Info audioLineInfo;    
    private SourceDataLine audioLine;
    // Audio recording.
    public boolean audioRecordingEnabled=false;
    private OutputStream audioRecordingOutputStream=null;
    private final AudioFile audioFile=new AudioFile();
    
    // Video.
    public boolean videoGenerationEnabled=true;
    public boolean videoBreakOnEndOfFrameFlag=false;
    // Presentation.
    public boolean videoInitialized=false;
    public boolean videoPresentationEnabled=true;
    public boolean videoUpdateOnBreakFlag=true;
    private final Object videoSemaphore=new Object();
    private volatile boolean videoImageLocked=false;
    public int videoCurrentOutputImage;
    public final BufferedImage videoOutputImage[]=new BufferedImage[2];
    public final int videoOutputDataBuffer[][]=new int[2][];
    // Frame skip.
    public boolean videoFrameSkipAutoModeEnabled=false;
    public int videoFrameSkip=0, videoFrameSkipCount;
    // Video performance.
    public long videoLastFrameTimer, videoAverageLastFrameTimer;
    public int videoNbGeneratedFrames, videoNbFrames;
    public volatile double videoFrameDuration, videoAverageFrameDuration;
    // Snapshots.
    public int videoSnapshotIndex=0;
            
    // Events dispatcher.
    public final ContextEventDispatcher onResetDispatcher=new ContextEventDispatcher(); // Called after a reset.
    public final ContextEventDispatcher onEndOfFrameDispatcher=new ContextEventDispatcher(); // Called at the end of frame.
    public final ContextEventDispatcher onCartridgeRemovedDispatcher=new ContextEventDispatcher(); // Called before a cartridge is removed.
    public final ContextEventDispatcher onCartridgeInsertedDispatcher=new ContextEventDispatcher(); // Called after a cartridge is reset.
    
    //------------------------------------------------------------------------------
    // Listeners.
    //------------------------------------------------------------------------------
    private final CPUListener cpuListener=new CPUListener() {
        public void onTrace(CPUEvent e) { systemContext.breakEmulation(); }
        public void onIllegal(CPUEvent e) { if (cpuBreakOnIllegalOpcode) systemContext.breakEmulation(); }
    };
    
    private final GameBoyListener gameBoyListener=new GameBoyListener() {
        public void onEndOfVideoFrame(GameBoyEvent e) { videoUpdate(); }        
        public void onEndOfAudioBuffer(GameBoyEvent e) { audioUpdate(); }        
        public void onBreak(GameBoyEvent e) { systemContext.breakEmulation(); }
    };
        
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent ce) {
            videoBreakOnEndOfFrameFlag=false;
            gameBoy.cpu.setTraceMode(CPU.TRACE_MODE_RUN, cpuTraceInterrupts);
            
            audioStop();
            
            if (videoInitialized && videoUpdateOnBreakFlag) {
                int srcData[], dstData[];
                srcData=videoOutputDataBuffer[videoCurrentOutputImage^1];
                dstData=videoOutputDataBuffer[videoCurrentOutputImage];
                int l=gameBoy.getCurrentVideoLine();
                if (l>=GameBoy.SCREEN_HEIGHT) l=0;
                System.arraycopy(srcData, 0, dstData, 0, l*GameBoy.SCREEN_WIDTH);
                onEndOfFrameDispatcher.dispatch(null);
            }
        }
    };

    private final ContextEventListener onResumeListener=new ContextEventListener() {
        public void processEvent(ContextEvent ce) { audioStart(); }
    };

    //------------------------------------------------------------------------------
    // Initialization.
    //------------------------------------------------------------------------------
    public GameBoyEmulationContext(SystemEmulationContext sec) {
        systemContext=sec;
        synchronized (systemContext.emulationSystem) {	        
	        // Game Boy.
	        gameBoy=new GameBoy();
	        gameBoy.setListener(gameBoyListener, new GameBoyEvent());
	        // Game Boy CPU.
	        gameBoy.cpu.setListener(cpuListener, new CPUEvent());
	        cpuTraceInterrupts=false;
	        
	        systemContext.add(node);
	        systemContext.onPauseDispatcher.addListener(onPauseListener);
	        systemContext.onResumeDispatcher.addListener(onResumeListener);
	        systemContext.emulationSystem.addDevice(gameBoy);
	        	        
	        videoInitialize();
        }
    }
    
    public void detach() {
    	audioStop();
    	switchPower(false);
    	cartridgeRemove();    	
        detachAll();
        synchronized (systemContext.emulationSystem) {
	        videoFinalize();
	        audioFinalize();
	        
	        onResetDispatcher.removeAllListener();
	        onEndOfFrameDispatcher.removeAllListener();
	        onCartridgeRemovedDispatcher.removeAllListener();
	        onCartridgeInsertedDispatcher.removeAllListener();
	        
	        systemContext.onPauseDispatcher.removeListener(onPauseListener);
	        systemContext.onResumeDispatcher.removeListener(onResumeListener);
	        systemContext.emulationSystem.removeDevice(gameBoy);
	        systemContext.remove(node);
	        
	        gameBoy=null;
	        systemContext=null;
        }
    }

    //------------------------------------------------------------------------------
    // Global.
    //------------------------------------------------------------------------------
    public void setModel(int model) {
        synchronized (systemContext.emulationSystem) {
            boolean f=gameBoy.isPowered();
            if (f) gameBoy.switchPower(false);
            gameBoy.setModel(model);
            if (f) {
            	gameBoy.switchPower(true);
            	onResetDispatcher.dispatch(null);
            }
        }
    }
    
    public void switchPower(boolean f) {
        synchronized (systemContext.emulationSystem) {
        	boolean powered=gameBoy.isPowered();
        	if (powered&&!f || !powered&&f) {
	            gameBoy.switchPower(f);
	            onResetDispatcher.dispatch(null);
        	}
        }
    }
    
    public void reset() {
        synchronized (systemContext.emulationSystem) {
            gameBoy.reset();
            onResetDispatcher.dispatch(null);
        }
    }
    
    public void activateDebugMode(boolean flag) {
        synchronized (systemContext.emulationSystem) {
        	gameBoy.cpu.activateDebugMode(flag);
        }
    }
    
    public void updatePerformance() {
        long t=systemContext.emulationTimer.getTimer();
        double adt=systemContext.emulationTimer.computeDifference(videoAverageLastFrameTimer, t);
        if (adt>1.0) {
            videoAverageFrameDuration=(videoNbGeneratedFrames==0) ? adt : adt/videoNbGeneratedFrames;
            videoAverageLastFrameTimer=t;
            videoNbFrames=0; videoNbGeneratedFrames=0;
        }        
    }
        
    //------------------------------------------------------------------------------
    // Audio.
    //------------------------------------------------------------------------------
    private void audioInitialize() {
    	synchronized (systemContext.emulationSystem) {
	        audioOutputBufferLength=audioSampleRate/10;
	        audioOutputBuffer=new byte[2*audioOutputBufferLength];
	        
    		audioLineBufferLength=2*audioOutputBufferLength;
	        audioLineFormat=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, 8, 2, 2, audioSampleRate, false);
	        audioLineInfo=new DataLine.Info(SourceDataLine.class, audioLineFormat, audioLineBufferLength<<1);
	        try {
	            audioLine=(SourceDataLine)AudioSystem.getLine(audioLineInfo);
	            audioLine.open(audioLineFormat, audioLineBufferLength<<1);
	            gameBoy.setAudioOutputFrequency(audioSampleRate);
	            gameBoy.setAudioOutputBuffer(0, audioOutputBufferLength, audioOutputBuffer);
	            audioInitialized=true;
		        audioLineStopped=systemContext.emulationThread.isPaused();
		        audioLineHalted=true;
		        audioLineOverflow=0; audioLineUnderflow=0;
		        audioLineBufferLevel=0.0f;
		        if (!audioLineStopped) audioLine.start();
	        } catch (LineUnavailableException e) {
//	            System.out.println("Cannot open an audio line.");
	            audioFinalize();
	        }
    	}
    }
    
    private void audioFinalize() {        
    	synchronized (systemContext.emulationSystem) {
	    	audioStopRecording();
	    	audioInitialized=false;
	    	audioLineStopped=true;
	        audioLineHalted=true;
	        gameBoy.setAudioOutputBuffer(0, 0, null);
	        audioOutputBuffer=null;
	        audioLineFormat=null;
	        audioLineInfo=null;
	        if (audioLine!=null) {
	        	audioLine.stop();
	        	audioLine.flush();
	            audioLine.close();
	            audioLine=null;
	        }
	        audioLineOverflow=0; audioLineUnderflow=0;
	        audioLineBufferLevel=0.0f;
    	}
    }
    
    private void audioReset() {
    	synchronized (systemContext.emulationSystem) {
    		if (audioEnabled) {
	    		audioFinalize();
	    		audioInitialize();
    		}
    	}
    }
    
    private void audioUpdate() {
        if (!audioInitialized || audioLineStopped) return;
    	int l=audioOutputBufferLength, la=audioLine.available()>>1, lu=l; // Length generated (l), available (la), used (lu) and remaining (lr).
    
    	audioLineBufferLevel=(float)(audioLineBufferLength-la)/(float)audioLineBufferLength;
    	if (!audioLine.isRunning() && !audioLineHalted) audioLineUnderflow=(audioLineUnderflow+1)&0x7fffffff;
        audioLineHalted=false;
        
    	if (la==0) audioLineOverflow=(audioLineOverflow+1)&0x7fffffff;
    	else if (l>la) lu=la;

    	// Record.
        if (audioRecordingEnabled) {
        	if (audioFile.write(0, lu<<1, audioOutputBuffer)) audioStopRecording();
        }
        
        // Play.
		if (la!=0) audioLine.write(audioOutputBuffer, 0, lu<<1);
		
        // Shift the buffer if necessary.
		int lr=l-lu;
        if (lr>0) {
        	int lr2=lr<<1, lu2=lu<<1;
    		for (int i=0; i<lr2; i++) audioOutputBuffer[i]=audioOutputBuffer[lu2+i]; // Shift. TODO: use a circular buffer.
        }
        
        // Set the buffer.
		gameBoy.setAudioOutputBuffer(lr, lu, audioOutputBuffer);
    }
    
    public void audioEnable(boolean flag) {
        synchronized (systemContext.emulationSystem) {
	    	if (flag) {
	    		if (!audioEnabled) { audioInitialize(); audioEnabled=true; }
	    	} else {
	    		if (audioEnabled) { audioFinalize(); audioEnabled=false; }
	    	}
        }
    }
    
    public void audioSetFrequency(int frequency) {
        switch (frequency) {
        case 11025: case 22050: case 44100: break;
        default: return;
        }
        
        synchronized (systemContext.emulationSystem) {	        
        	audioSampleRate=frequency;
        	audioReset();
        }
    }
    
    public void audioStart() {
    	synchronized (systemContext.emulationSystem) {
    		if (audioLineStopped && audioInitialized) {
	    		audioLineStopped=false;
	    		audioLine.start();
    		}
    	}
    }
    
    public void audioStop() {
    	synchronized (systemContext.emulationSystem) {
    		if (!audioLineStopped && audioInitialized) {
	            audioLineStopped=true;
	    		audioLine.stop();
    		}
    	}
    }
    
    public void audioStartRecording() {
    	synchronized (systemContext.emulationSystem) {
        	if (!audioInitialized) return;
        	
	        CartridgeInfos ci=gameBoy.getCartridgeInfos();
	        if ((cartridgeFilename!=null) && ci.headerValid) {
	        	String audioFilename=Utilities.replaceFileExtension(cartridgeFilename, ".au");        	
	            try {
	            	File f=new File(audioFilename);
	            	audioRecordingOutputStream=new FileOutputStream(f);
	            	audioFile.start(audioRecordingOutputStream, audioSampleRate);
	            } catch (IOException e) {
//	                System.out.println("Cannot open a file for audio recording.");
	                return;
	            }
	        	audioRecordingEnabled=true;
	        }
    	}
    }
    
    public void audioStopRecording() {
    	synchronized (systemContext.emulationSystem) {
        	if (!audioInitialized || !audioRecordingEnabled) return;
        	
	        try {
	        	audioFile.stop();
	        	audioRecordingOutputStream.close();
	        } catch (IOException e) {}
	        audioRecordingOutputStream=null;
	        audioRecordingEnabled=false;
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
    //------------------------------------------------------------------------------
    // Video.
    //------------------------------------------------------------------------------
    private void videoInitialize() {
    	synchronized (systemContext.emulationSystem) {
	        videoCurrentOutputImage=0;
	        for (int i=0; i<2; i++) {
	            videoOutputImage[i]=new BufferedImage(GameBoy.SCREEN_WIDTH, GameBoy.SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
	            int data[]=((DataBufferInt)(videoOutputImage[i].getRaster().getDataBuffer())).getData();
	            videoOutputDataBuffer[i]=data;
	            for (int y=0; y<GameBoy.SCREEN_HEIGHT; y++) {
	                for (int x=0; x<GameBoy.SCREEN_WIDTH; x++) data[y*GameBoy.SCREEN_WIDTH+x]=0xffffffff;
	            }
	        }
	        gameBoy.setVideoOutputImage(videoOutputDataBuffer[1]);	        
	        videoInitialized=true;
	        videoBreakOnEndOfFrameFlag=false;
    	}
    }
    
    private void videoFinalize() {
    	synchronized (systemContext.emulationSystem) {
	        videoOutputImage[0]=null; videoOutputImage[1]=null;
	        videoOutputDataBuffer[0]=null; videoOutputDataBuffer[1]=null;
	        gameBoy.setVideoOutputImage(null);
	        videoInitialized=false;
	        videoBreakOnEndOfFrameFlag=false;
    	}
    }
    
    private void videoUpdate() {
        long t=systemContext.emulationTimer.getTimer();
        videoFrameDuration=systemContext.emulationTimer.computeDifference(videoLastFrameTimer, t);
        videoLastFrameTimer=t;
        videoNbFrames++;
        
        // Presentation.
        if (videoInitialized && videoPresentationEnabled && videoFrameSkipCount==0) {
        	if (videoTryLockImage()) {
                gameBoy.setVideoOutputImage(videoOutputDataBuffer[videoCurrentOutputImage]);
                videoCurrentOutputImage=(videoCurrentOutputImage==0) ? 1 : 0;
                onEndOfFrameDispatcher.dispatch(null);
                videoUnlockImage();
            }
        }
        
        // Frame skip management.
        if ((videoFrameSkipCount<0) || (videoFrameSkipCount>=videoFrameSkip)) {
            videoFrameSkipCount=0;
            videoNbGeneratedFrames++;
            gameBoy.enableVideoOutput(videoGenerationEnabled);
        } else {
            videoFrameSkipCount++;
            gameBoy.enableVideoOutput(false);
        }
        
        if (videoBreakOnEndOfFrameFlag) systemContext.breakEmulation();
    }
    
    public boolean videoTryLockImage() {
    	synchronized (videoSemaphore) {
    		if (videoImageLocked) return false;
    		videoImageLocked=true;
        	return true;
    	}
    }
    
    public void videoLockImage() {
    	synchronized (videoSemaphore) {
    		while (videoImageLocked) try { videoSemaphore.wait(); } catch (InterruptedException e) {}     	
    		videoImageLocked=true;    		
    	}
    }
    
    public void videoUnlockImage() {
    	synchronized (videoSemaphore) {
    		videoImageLocked=false;
    		videoSemaphore.notify();
    	}    	
    }
    
    public void videoSnapshot() {
        String snapshotFilename=Utilities.findFreeFilename(cartridgeFilename, videoSnapshotIndex, ".png");
        if (snapshotFilename==null) return;
        
        videoLockImage();
        Utilities.saveImage(videoOutputImage[videoCurrentOutputImage], snapshotFilename);
        videoUnlockImage();
        
        videoSnapshotIndex++;
    }
    
    //------------------------------------------------------------------------------
    // Cartridge.
    //------------------------------------------------------------------------------
    public void cartridgeInsert(String filename) throws IOException {
        synchronized (systemContext.emulationSystem) {
        	boolean reset=false;
            if (gameBoy.isPowered()) { reset=true; gameBoy.switchPower(false); }
            if (gameBoy.isCartridgeInserted()) onCartridgeRemovedDispatcher.dispatch(null);
            cartridgeFilename=null; // Must be after the call to onCartridgeRemovedDispatcher.dispatch() !
            
            try { 
                FileInputStream fis=new FileInputStream(new File(filename));
                if (gameBoy.setCartridge(fis)) { fis.close(); throw new IOException(); }
                fis.close();
                cartridgeFilename=filename;
            } finally {
                if (gameBoy.isCartridgeInserted()) onCartridgeInsertedDispatcher.dispatch(null);
                if (cartridgeAutoRunFlag) { reset=true; gameBoy.switchPower(true); }
                if (reset) onResetDispatcher.dispatch(null);
            }
        }
    }
        
    public void cartridgeInsert(File f) throws IOException {
        synchronized (systemContext.emulationSystem) {
        	boolean reset=false;
            if (gameBoy.isPowered()) { reset=true; gameBoy.switchPower(false); }
            if (gameBoy.isCartridgeInserted()) onCartridgeRemovedDispatcher.dispatch(null);
            cartridgeFilename=null; // Must be after the call to onCartridgeRemovedDispatcher.dispatch() !
            
            try {
                FileInputStream fis=new FileInputStream(f);
                if (gameBoy.setCartridge(fis)) { fis.close(); throw new IOException(); }
                fis.close();
                cartridgeFilename=f.getPath();
            } finally {
                if (gameBoy.isCartridgeInserted()) onCartridgeInsertedDispatcher.dispatch(null);
                if (cartridgeAutoRunFlag) { reset=true; gameBoy.switchPower(true); }
                if (reset) onResetDispatcher.dispatch(null);            	
            }
        }
    }
    
    public void cartridgeInsert(InputStream fis) throws IOException {
        synchronized (systemContext.emulationSystem) {
        	boolean reset=false;
            if (gameBoy.isPowered()) { reset=true; gameBoy.switchPower(false); }
            if (gameBoy.isCartridgeInserted()) onCartridgeRemovedDispatcher.dispatch(null);
            cartridgeFilename=null; // Must be after the call to onCartridgeRemovedDispatcher.dispatch() !
            
            try {
            	gameBoy.setCartridge(fis);
            } finally {
                if (gameBoy.isCartridgeInserted()) onCartridgeInsertedDispatcher.dispatch(null);
                if (cartridgeAutoRunFlag) { reset=true; gameBoy.switchPower(true); }
                if (reset) onResetDispatcher.dispatch(null);            	
            }
        }
    }
    
    public void cartridgeRemove() {
        synchronized (systemContext.emulationSystem) {
        	if (!gameBoy.isCartridgeInserted()) return;
        	
        	boolean reset=false;
            if (gameBoy.isPowered()) { reset=true; gameBoy.switchPower(false); }
            onCartridgeRemovedDispatcher.dispatch(null);
            cartridgeFilename=null; // Must be after the call to onCartridgeRemovedDispatcher.dispatch() !
            
            gameBoy.removeCartridge();

            if (cartridgeAutoRunFlag) { reset=true; gameBoy.switchPower(true); }
            if (reset) onResetDispatcher.dispatch(null);
        }
    }

    /**
     * Save the current cartridge backup memory as a file with the name of the cartridge and extension ".sav". 
     * @return </code>false<code> if the backup has been saved and </code>true<code> if there is no backup to save
     * @throws IOException
     */
    public boolean cartridgeSaveBackup() throws IOException {
        synchronized (systemContext.emulationSystem) {
	        if (!gameBoy.hasCartridgeBackup() || cartridgeFilename==null) return true;
	        
            String backupFilename=Utilities.replaceFileExtension(cartridgeFilename, ".sav");
            File f=new File(backupFilename);
            FileOutputStream fos=new FileOutputStream(f);
            gameBoy.saveCartridgeBackup(fos);
            fos.close();
            
	        return false;
        }
    }

    /**
     * Save the current cartridge backup memory in an output stream. 
     * @param os the output stream.
     * @return <code>false</code> if the backup has been saved and <code>true</code> if there is no backup to save
     * @throws IOException
     */
    public boolean cartridgeSaveBackup(OutputStream os) throws IOException {
        synchronized (systemContext.emulationSystem) {
            return gameBoy.saveCartridgeBackup(os);
        }
    }
    
    public boolean cartridgeLoadBackup() throws IOException {
        synchronized (systemContext.emulationSystem) {
	        if (!gameBoy.hasCartridgeBackup() || cartridgeFilename==null) return true;

	        String backupFilename=Utilities.replaceFileExtension(cartridgeFilename, ".sav");
            File f=new File(backupFilename);
            if (!f.exists()) return true;
            FileInputStream fis=new FileInputStream(f);
            if (gameBoy.loadCartridgeBackup(fis)) { fis.close(); throw new IOException(); }
            fis.close();

	        return false;
        }
    }
    
    public boolean cartridgeLoadBackup(InputStream is) throws IOException {
        synchronized (systemContext.emulationSystem) {
            return gameBoy.loadCartridgeBackup(is);
        }
    }

    //------------------------------------------------------------------------------
    // Tracing.
    //------------------------------------------------------------------------------    
    public void cpuStepInto() {
        if (systemContext.emulationThread.isPaused()) {
            gameBoy.cpu.setTraceMode(CPU.TRACE_MODE_STEP_INTO, cpuTraceInterrupts);
            systemContext.emulationThread.resume();
        }
    }
    
    public void cpuStepOver() {
        if (systemContext.emulationThread.isPaused()) {
            gameBoy.cpu.setTraceMode(CPU.TRACE_MODE_STEP_OVER, cpuTraceInterrupts);
            systemContext.emulationThread.resume();
        }
    }
    
    public void cpuStepOut() {
        if (systemContext.emulationThread.isPaused()) {
            gameBoy.cpu.setTraceMode(CPU.TRACE_MODE_STEP_OUT, cpuTraceInterrupts);
            systemContext.emulationThread.resume();
        }
    }
    
    public void cpuStepIntoi() {
        if (systemContext.emulationThread.isPaused()) {
            gameBoy.cpu.setTraceMode(CPU.TRACE_MODE_STEP_INTO_I, cpuTraceInterrupts);
            systemContext.emulationThread.resume();
        }
    }
    
    public void cpuStepOuti() {
        if (systemContext.emulationThread.isPaused()) {
            gameBoy.cpu.setTraceMode(CPU.TRACE_MODE_STEP_OUT_I, cpuTraceInterrupts);
            systemContext.emulationThread.resume();
        }
    }
    
    public void videoBreakOnEndOfFrame() {
        if (systemContext.emulationThread.isPaused()) {
            videoBreakOnEndOfFrameFlag=true;
            gameBoy.cpu.setTraceMode(CPU.TRACE_MODE_RUN, cpuTraceInterrupts);
            systemContext.emulationThread.resume();
        }        
    }
}
