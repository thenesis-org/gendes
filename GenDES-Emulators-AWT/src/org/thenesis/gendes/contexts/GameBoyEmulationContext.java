package org.thenesis.gendes.contexts;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    public AudioBackEnd audioBackEnd=new AudioBackEnd();
    
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
        public void onEndOfAudioBuffer(GameBoyEvent e) {
        	audioBackEnd.update();
    		gameBoy.setAudioOutputBuffer(audioBackEnd.getOutputBufferLengthRemaining(), audioBackEnd.getOutputBufferLengthUsed(), audioBackEnd.getOutputBuffer());
        }        
        public void onBreak(GameBoyEvent e) { systemContext.breakEmulation(); }
    };
        
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent ce) {
            videoBreakOnEndOfFrameFlag=false;
            gameBoy.cpu.setTraceMode(CPU.TRACE_MODE_RUN, cpuTraceInterrupts);
            
            audioBackEnd.stop();
            
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
        public void processEvent(ContextEvent ce) { audioBackEnd.start(); }
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
	        	        
	        audioBackEnd.attach(systemContext.emulationSystem, systemContext.emulationThread);
//	        audioBackEnd.reset();
	        
	        videoInitialize();
        }
    }
    
    public void detach() {
    	audioBackEnd.stop();
    	switchPower(false);
    	cartridgeRemove();    	
        detachAll();
        synchronized (systemContext.emulationSystem) {
	        videoFinalize();
	        
	        gameBoy.setAudioOutputBuffer(0, 0, null);
	        audioBackEnd.finalize();
	        
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
    public void audioStartRecording() {
        CartridgeInfos ci=gameBoy.getCartridgeInfos();
        if ((cartridgeFilename!=null) && ci.headerValid) {
        	audioBackEnd.startRecording(cartridgeFilename);
        }
    }
    
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
       	FileInputStream fis=new FileInputStream(new File(filename));
       	cartridgeInsert(fis, filename);
        fis.close();
    }
        
    public void cartridgeInsert(File f) throws IOException {
        FileInputStream fis=new FileInputStream(f);
       	cartridgeInsert(fis, f.getPath());
        fis.close();
    }
    
    public void cartridgeInsert(InputStream fis, String fileName) throws IOException {
        synchronized (systemContext.emulationSystem) {
        	boolean reset=false;
            if (gameBoy.isPowered()) { reset=true; gameBoy.switchPower(false); }
            if (gameBoy.isCartridgeInserted()) onCartridgeRemovedDispatcher.dispatch(null);
            cartridgeFilename=null; // Must be after the call to onCartridgeRemovedDispatcher.dispatch() !
            
            try {
            	if (gameBoy.setCartridge(fis)) { throw new IOException(); }
            } finally {
                if (gameBoy.isCartridgeInserted()) onCartridgeInsertedDispatcher.dispatch(null);
                if (cartridgeAutoRunFlag) { reset=true; gameBoy.switchPower(true); }
                if (reset) onResetDispatcher.dispatch(null);            	
            }

            cartridgeFilename=fileName;
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
