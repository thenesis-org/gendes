package org.thenesis.emulation.headless;

import java.io.InputStream;

import org.thenesis.gendes.EmulationSystem;
import org.thenesis.gendes.TimeStamp;
import org.thenesis.gendes.gameboy.GameBoy;
import org.thenesis.gendes.gameboy.GameBoyEvent;
import org.thenesis.gendes.gameboy.GameBoyListener;

/**
 * A headless Gameboy emulator.
 * 
 * @author Guillaume Legris
 * @author Mathieu Legris
 */
public class GameboyApplication implements GameBoyListener {
    
    private static final double FPS = 60;
    private static final double SAMPLE_RATE = 44100;

    int baseWidth = 320;
    int baseHeight = 240;
    
    private EmulationSystem emulationSystem;
    private GameBoy gameBoy;

    // Video.
    private int[] videoBuffer;
    private int videoOutputImages[];

    // Audio.
    private short[] audioBuffer;
    private int audioOutputBufferLength;
    private byte audioOutputBuffers[];

    public void initializeEmulation() throws Exception {
        // Emulation system.
        emulationSystem = new EmulationSystem();

        // Video.
        videoBuffer = new int[baseWidth * baseHeight];
        videoOutputImages = new int[GameBoy.SCREEN_WIDTH * GameBoy.SCREEN_HEIGHT];

        // Audio.
        int audioBufferSize = (int) (SAMPLE_RATE / FPS);
        audioBuffer = new short[audioBufferSize];
        audioOutputBufferLength = (int) (SAMPLE_RATE / FPS);
        audioOutputBuffers = new byte[2 * audioOutputBufferLength];

        // Cartridge.
        String romFileName = "Demo-AGO-Realtime.gb";
        InputStream romInputStream = getClass().getResourceAsStream(romFileName);

        // Game Boy.
        gameBoy = new GameBoy();
        emulationSystem.addDevice(gameBoy);
        gameBoy.setListener(this, new GameBoyEvent());
        gameBoy.setModel(GameBoy.MODEL_GAME_BOY_COLOR);
        gameBoy.setVideoOutputImage(videoOutputImages);
        gameBoy.setAudioOutputFrequency((int)SAMPLE_RATE);
        gameBoy.setAudioOutputBuffer(0, audioOutputBufferLength, audioOutputBuffers);
        gameBoy.setCartridge(romInputStream);
        gameBoy.switchPower(true);
        
        timeSliceDuration.fromDouble(1000.0 / FPS);
        
    }
    

    
    TimeStamp timeSliceDuration = new TimeStamp();
    
    
    public void run() {

        /* Read input and change state */

        /* Run a tick */
         emulationSystem.run(timeSliceDuration);
       
         /* Render */
         
    }


//  ------------------------------------------------------------------------------
    // Callbacks. Must not be called directly.
    // ------------------------------------------------------------------------------
    public void onEndOfVideoFrame(GameBoyEvent e) {
        System.out.println("onEndOfVideoFrame");
        
        // Fill the offscreen buffer
        int dstStride = baseWidth;
        int srcStride = GameBoy.SCREEN_WIDTH;
        for (int j = 0; j < GameBoy.SCREEN_HEIGHT; j++) {
            int srcIndex = j * srcStride;
            int dstIndex = j * dstStride;
            for (int i = 0; i < GameBoy.SCREEN_WIDTH; i++) {
                videoBuffer[dstIndex + i] = videoOutputImages[srcIndex + i];
            }
        }
    }

    public void onEndOfAudioBuffer(GameBoyEvent e) {
        gameBoy.setAudioOutputBuffer(0, audioOutputBufferLength, audioOutputBuffers);
    }

    public void onBreak(GameBoyEvent e) {
        System.out.println("onBreak");
    }

    public static void main(String[] args) {
        GameboyApplication core = new GameboyApplication();
        
        try {
            core.initializeEmulation();
            while(true) {
                core.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
       
         
    }
    
}