package org.thenesis.emulation.console;

import java.io.IOException;
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
	private static final double SAMPLE_RATE = 4000;

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

	// Console backend;
	Ansi ansiConsole = new Ansi();

	public void initializeEmulation() throws Exception {

		// Console backend;
		ansiConsole = new Ansi();
		//System.out.println(ansiConsole.eraseScreen());
		//ansiConsole.setScreenMode();

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
		//String romFileName = "/Super_Mario_Land.gb";
		//String romFileName = "/Demo-AGO-Dimension_of_Miracles.gb";
		String romFileName = "/Demo-AGO-Realtime.gb";
		InputStream romInputStream = getClass().getResourceAsStream(romFileName);

		// Game Boy.
		gameBoy = new GameBoy();
		emulationSystem.addDevice(gameBoy);
		gameBoy.setListener(this, new GameBoyEvent());
		gameBoy.setModel(GameBoy.MODEL_GAME_BOY_COLOR);
		gameBoy.setVideoOutputImage(videoOutputImages);
		gameBoy.setAudioOutputFrequency((int) SAMPLE_RATE);
		gameBoy.setAudioOutputBuffer(0, audioOutputBufferLength, audioOutputBuffers);
		gameBoy.setCartridge(romInputStream);
		gameBoy.switchPower(true);

		timeSliceDuration.fromDouble(1000.0 / FPS);
		//System.out.println("toto");
	}

	TimeStamp timeSliceDuration = new TimeStamp();

	public void run() {

		/* Read input and change state */
//    	try {
//			if (System.in.available() > 0) {
//				int c = System.in.read();
//				if (c == 'e') {
//					gameBoy.setKeys(GameBoy.PAD_START);
//				} else if (c == 's') {
//					gameBoy.setKeys(GameBoy.PAD_A);
//				} else if (c == 'd') {
//					gameBoy.setKeys(GameBoy.PAD_B);
//				}
//				System.out.println("Key: " + c);
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		/* Run a tick */
		emulationSystem.run(timeSliceDuration);

		/* Render */

	}

//  ------------------------------------------------------------------------------
	// Callbacks. Must not be called directly.
	// ------------------------------------------------------------------------------
//    int fpsCount;
//    long lastTime = System.currentTimeMillis(); 
//    int lastFpsCount = 0;
//    public void onEndOfVideoFrame(GameBoyEvent e) {
//        fpsCount++;
//        if (fpsCount % 10 == 0) {
////            long currentTime = System.currentTimeMillis();
////            float fps = ((float)((fpsCount - lastFpsCount) * 1000)) / (currentTime - lastTime);
////            System.out.println("onEndOfVideoFrame: fps=" + fps);
////            lastTime = currentTime;
////            lastFpsCount = fpsCount;
//            System.out.println("onEndOfVideoFrame: frame count=" + fpsCount);
//        }
//        
//        
////        // Fill the offscreen buffer
////        int dstStride = baseWidth;
////        int srcStride = GameBoy.SCREEN_WIDTH;
////        for (int j = 0; j < GameBoy.SCREEN_HEIGHT; j++) {
////            int srcIndex = j * srcStride;
////            int dstIndex = j * dstStride;
////            for (int i = 0; i < GameBoy.SCREEN_WIDTH; i++) {
////                videoBuffer[dstIndex + i] = videoOutputImages[srcIndex + i];
////            }
////        }
//    }

	private int frameCount = 0;

	public void onEndOfVideoFrame(GameBoyEvent e) {
		//       System.out.println("onEndOfVideoFrame");
//        try {
//            serialBackend.sendScreenPackets(videoOutputImages, GameBoy.SCREEN_WIDTH, GameBoy.SCREEN_HEIGHT);
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }

		frameCount++;

		/* Read input and change state */
		try {
			while (System.in.available() > 0) {
				// FIXME Detect release and unset keys !
				int c = System.in.read();
				if (c == 'p') {
					gameBoy.setKeys(GameBoy.PAD_START) ;//gameBoy.getKeys() & ~GameBoy.PAD_START);
				} else if (c == 's') {
					gameBoy.setKeys(GameBoy.PAD_LEFT);
				} else if (c == 'f') {
					gameBoy.setKeys(GameBoy.PAD_RIGHT);
				} else if (c == 'e') {
					gameBoy.setKeys(GameBoy.PAD_UP);
				} else if (c == 'd') {
					gameBoy.setKeys(GameBoy.PAD_DOWN);
				}
				//System.out.println("Key: " + c);
			}
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}

		
		if (frameCount % 20 == 0) {
			ansiConsole.clear();
			ansiConsole.eraseScreen();
			int scale = 1;
			int height = GameBoy.SCREEN_HEIGHT / scale;
			int width = GameBoy.SCREEN_WIDTH / scale;
			for (int j = 0; j < height - 1; j = j + 2) {
				ansiConsole.cursor(j/2 + 1, 1); // 1-based
				for (int i = 0; i < width; i++) {
					int topPixelIndex = (j * GameBoy.SCREEN_WIDTH + i) * scale;
					int topPixelArgb = videoOutputImages[topPixelIndex];
					int bottomPixelIndex = ((j + 1) * GameBoy.SCREEN_WIDTH + i) * scale;
					int bottomPixelArgb = videoOutputImages[bottomPixelIndex];
					ansiConsole.fgRgb(topPixelArgb).bgRgb(bottomPixelArgb).a('\u2594'); // Top half block
//    			if ((argb & 0xFFFFFF) != 0) {
//    				ansiConsole.bg(Color.WHITE).a(' ');
//    			} else {
//    				ansiConsole.bg(Color.BLACK).a(' ');
//    			}
				//	ansiConsole.bgRgb(argb).a(' ');
				}
			}
			ansiConsole.cursor(1, 1).reset().a(frameCount);
			System.out.println(ansiConsole);
		}

		try {
			Thread.sleep((long) (1000 / FPS));
		} catch (InterruptedException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
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
			while (true) {
				core.run();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
