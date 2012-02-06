package org.thenesis.gendes.gameboy;

/**
 * @author Guillaume
 * @author Mathieu
 */
public interface GameBoyListener {
    // Called on end of video frame.
    void onEndOfVideoFrame(GameBoyEvent e);
    // Called on end of audio buffer.
    void onEndOfAudioBuffer(GameBoyEvent e);
    // Called when the emulation is broken.
    void onBreak(GameBoyEvent e);
}
