package org.thenesis.emulation.midp2;

import java.io.InputStream;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.thenesis.gendes.EmulationSystem;
import org.thenesis.gendes.EmulationThread;
import org.thenesis.gendes.EmulationTimer;
import org.thenesis.gendes.SystemMillisecondTimer;
import org.thenesis.gendes.gameboy.GameBoy;
import org.thenesis.gendes.gameboy.GameBoyEvent;
import org.thenesis.gendes.gameboy.GameBoyListener;


/**
 * A Gameboy emulator MIDlet.
 * 
 * @author Guillaume Legris
 * @author Mathieu Legris
 */
public class GameboyMidlet extends MIDlet implements GameBoyListener {
    
    private EmulationSystem emulationSystem;
    private EmulationThread emulationThread;
    private EmulationTimer emulationTimer;
    private GameBoy gameBoy;
    
    // Video.
    private int videoOutputImages[];
    
    // Audio.
    private int audioOutputBufferLength;
    private byte audioOutputBuffers[];
    
    // Performance.
    private long lastFrameTime, averageLastFrameTime;
    private int nbFrames;
    private volatile double frameTime, averageFrameTime;
    private boolean firstFrame = true;
    private int frameSkipCount;

	// UI
    private Display display;
	private GameBoyScreen gbScreen;
	

	public GameboyMidlet() {
		super();
		display = Display.getDisplay(this);
	}

	protected void startApp() throws MIDletStateChangeException {

		try {
			initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void pauseApp() {
		// TODO Auto-generated method stub

	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		// TODO Auto-generated method stub

	}
	
	public void initialize() throws Exception {
        initializeGUI();
        initializeEmulation();
    }
    
    private void initializeGUI() {
        gbScreen = new GameBoyScreen(false);
        display.setCurrent((Displayable) gbScreen);
    }

    public void initializeEmulation() throws Exception {
        // Emulation system.
        emulationSystem=new EmulationSystem();
        
        // Video.
        videoOutputImages=new int[GameBoy.SCREEN_WIDTH*GameBoy.SCREEN_HEIGHT];
        
        // Audio.
        audioOutputBufferLength=1024;
        audioOutputBuffers=new byte[2*audioOutputBufferLength];
                 
        // Cartridge.
        String romFileName = "game.gb";
		InputStream romInputStream = getClass().getResourceAsStream("/" + romFileName);

        // Game Boy.
        gameBoy=new GameBoy();
        emulationSystem.addDevice(gameBoy);
        gameBoy.setListener(this, new GameBoyEvent());
        gameBoy.setModel(GameBoy.MODEL_GAME_BOY_COLOR);
        gameBoy.setVideoOutputImage(videoOutputImages);
        gameBoy.setAudioOutputBuffer(0, audioOutputBufferLength, audioOutputBuffers);
        gameBoy.setCartridge(romInputStream);
        gameBoy.switchPower(true);

        // Emulation thread.
        emulationTimer=new SystemMillisecondTimer();
        emulationThread=new EmulationThread(emulationSystem, emulationTimer);
        emulationThread.enableThrottling(false);
        
        //emulationThread.pause();
        emulationThread.start();
    }
    
//  ------------------------------------------------------------------------------
    // Emulation execution.
    //------------------------------------------------------------------------------
    public void emulationRun() {
        //System.out.println("emulationRun");
        if (emulationThread.isPaused()) {
            emulationThread.resume();
        }
    }
    
    public void emulationPause() {
        if (!emulationThread.isPaused()) {
            emulationThread.pause();            
        }
    }
    
//  ------------------------------------------------------------------------------
    // Callbacks. Must not be called directly.
    //------------------------------------------------------------------------------
    public void onEndOfVideoFrame(GameBoyEvent e) {
        final double FREQUENCY=1000.0;
        long t=System.currentTimeMillis(), d=t-lastFrameTime, ad=t-averageLastFrameTime;
        if (d<=0) d=1;
        frameTime=d/FREQUENCY;
        lastFrameTime=t;
        nbFrames++;
        double adt=ad/FREQUENCY;
        if (adt>1.0) {
            averageFrameTime=adt/nbFrames;
            averageLastFrameTime=t;
            nbFrames=0;
        }
        
        if (frameSkipCount==0) gbScreen.update(videoOutputImages);
        frameSkipCount++;
        if (frameSkipCount>=4) frameSkipCount=0;        
    }
    
    public void onEndOfAudioBuffer(GameBoyEvent e) {
        gameBoy.setAudioOutputBuffer(0, audioOutputBufferLength, audioOutputBuffers);
    }
    
    public void onBreak(GameBoyEvent e) {
        //System.out.println("onBreak");
        emulationSystem.breakExecution();
        emulationThread.pause();
    }

	
	/**
	 * The Gameboy screen.
	 */
	private class GameBoyScreen extends GameCanvas  {
		
	    private boolean isScreenDrawn = true;
	    private boolean isScreenCleared = true;
        private boolean isVideoEnabled = true;
        	  	
		public GameBoyScreen(boolean suppressKeyEvents) {
			super(suppressKeyEvents);
		}
		
		public void update(int[] imageArray) {
		    int w = getWidth();
			int h = getHeight();
			
	        //int x = (w / 2) - (GameBoy.SCREEN_WIDTH / 2);
	        //int y = (h / 2) - (GameBoy.SCREEN_HEIGHT / 2);
		    
	        Graphics g = getGraphics();
	        
	        // Clear offscreen image
	        if(isScreenCleared) {
	            g.setColor(0, 0, 0);
	            g.fillRect(0, 0, w, h);
	        }
		    
		    // Draw image
		    if(isScreenDrawn) {
		        g.drawRGB(imageArray, 0, GameBoy.SCREEN_WIDTH, 0, 0, GameBoy.SCREEN_WIDTH, GameBoy.SCREEN_HEIGHT, false);
		    }
		    
		    // Draw emulation infos
		    int s=(int)(emulationThread.getAverageSpeed()*100.0);

	        g.setColor(255, 0, 0);
	        g.drawString("Speed="+s+"%", 0, h, Graphics.BOTTOM | Graphics.LEFT);

	        int fps=(averageFrameTime>0) ? (int)(1.0/averageFrameTime) : 0;
	        g.drawString("FPS=" + fps, w, h, Graphics.BOTTOM | Graphics.RIGHT);
	        		    
		    // Show the offscreen image
	        flushGraphics();
		}
		
		public void keyPressed(int key) {
			if (key == GameCanvas.KEY_NUM1) {
			    isScreenDrawn = !isScreenDrawn;
			} else if (key == GameCanvas.KEY_NUM2) {
			    isScreenCleared = !isScreenCleared;
			} else if (key == GameCanvas.KEY_NUM3) {
                isVideoEnabled = !isVideoEnabled;
			    gameBoy.enableVideoOutput(isVideoEnabled);
            }			
		}
		
		public void keyReleased(int key) {
			switch (key) {
			case GameCanvas.KEY_NUM1:
			case GameCanvas.KEY_NUM2:
			case GameCanvas.KEY_NUM3:
			case GameCanvas.KEY_NUM4:
			case GameCanvas.KEY_NUM5:
			case GameCanvas.KEY_NUM6:
			case GameCanvas.KEY_NUM7:
			case GameCanvas.KEY_NUM8:
			case GameCanvas.KEY_NUM9:
			}
		}

	}

}