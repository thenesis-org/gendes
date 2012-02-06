package org.thenesis.gendes.awt.gameboy.debug;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Timer;
import java.util.TimerTask;

import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.debug.Screen;


//******************************************************************************
// Performance panel.
//******************************************************************************
public final class PerformancePanel extends DebugPanel {
    private SystemEmulationContext systemContext;
    private GameBoyEmulationContext gameBoyContext;
    
    private BufferedImage screenImg;
    private final int palette[]=new int[256];
    private final Screen screen=new Screen();
    
    private final Timer timer=new Timer();
    private int timerFrequency=500;
    private TimerTask timerTask;
    private final class UpdateTimerTask extends TimerTask {
        public void run() {
            synchronized (this) {
                if (gameBoyContext==null) return;
	            gameBoyContext.updatePerformance();
	            repaint();
            }
        }
    };
    
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { PerformancePanel.this.detach(); }
    };
    
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    private final ContextEventListener onResumeListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { repaint(); }
    };
    
    public PerformancePanel() {
        super("Main");
                
        int sw=32, sh=13, w, h;
        screen.setScreenSize(sw, sh);
        w=screen.getScreenPixelsWidth(); h=screen.getScreenPixelsHeight();
        screenImg=new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        palette[0]=0x00ffffff; palette[1]=0x00000000;
        
        Dimension d=new Dimension();
        d.setSize(w+2, h+2);
        setMinimumSize(d); setPreferredSize(d);
        
        addKeyListener(keyAdapter);
    }
    
    public void attach(GameBoyEmulationContext gbc) {
        detach();
        
        systemContext=gbc.systemContext; gameBoyContext=gbc;
        
        synchronized (systemContext.emulationSystem) {
            gameBoyContext.add(gameBoyNode);
            
	        systemContext.onPauseDispatcher.addListener(onPauseListener);
	        systemContext.onResumeDispatcher.addListener(onResumeListener);
        }
        
        timerTask=new UpdateTimerTask();
        timer.schedule(timerTask, 1000, timerFrequency);
        
        repaint();
    }
    
    public void detach() {
        if (gameBoyContext==null) return;
    	timerTask.cancel();
    	synchronized (timerTask) {
            systemContext.onPauseDispatcher.removeListener(onPauseListener);
            systemContext.onResumeDispatcher.removeListener(onResumeListener);	            
            gameBoyContext.remove(gameBoyNode);
            systemContext=null; gameBoyContext=null;            
    	}
        repaint();
    }
    
    private void updateScreen() {
        if (gameBoyContext==null) return;
        Screen s=screen; // Shortcut.
        if (s.isEmpty()) return;
        int py=0, t;
        
        s.currentChar=' '; s.currentBackgroundColor=0; s.currentForegroundColor=1;
        s.clear();
        
        s.currentBackgroundColor=1; s.currentForegroundColor=0;
        s.clearLine(py);
        s.print(0, py, "Emulation"); py++;
        s.currentBackgroundColor=0; s.currentForegroundColor=1;
        s.print(0, py, "[F8]Execution:  "+(systemContext.emulationThread.isPaused() ? "PAUSED" : "RUNNING")); py++;
        s.print(0, py, "[F9]Throttling: "+(systemContext.emulationThread.isThrottlingEnabled() ? "ON" : "OFF")); py++;
        t=(int)(systemContext.emulationThread.getAverageSpeed()*100.0+0.5);
        s.print(0, py,  "Speed: "+t+"%");
        t=(int)(systemContext.emulationThread.getAverageLoad()*100.0+0.5);
        s.print(16, py, "Load:  "+t+"%"); py++;
        t=(int)(systemContext.emulationThread.getAverageTimeSlice()*1000.0+0.5);
        s.print(0, py,  "Slice: "+t+"ms");
        t=(int)(systemContext.emulationThread.getAverageSleepTime()*1000.0+0.5);
        s.print(16, py, "Sleep: "+t+"ms"); py++;
        
        s.currentBackgroundColor=1; s.currentForegroundColor=0;
        s.clearLine(py);
        s.print(0, py, "Video"); py++;
        s.currentBackgroundColor=0; s.currentForegroundColor=1;
        s.print(0, py, "[F10]Update:    "+(gameBoyContext.videoGenerationEnabled ? "ON" : "OFF")); py++;        
        s.print(0, py, "[F11]Draw:      "+(gameBoyContext.videoPresentationEnabled ? "ON" : "OFF")); py++;        
        s.print(0, py, "[-/+]Skip:      "+gameBoyContext.videoFrameSkip); py++;
        t=(gameBoyContext.videoAverageFrameDuration>0) ? (int)(1.0/gameBoyContext.videoAverageFrameDuration) : 0;
        s.print(0, py, "FPS:            "+t); py++;
        
        s.currentBackgroundColor=1; s.currentForegroundColor=0;
        s.clearLine(py);
        s.print(0, py, "Audio"); py++;
        s.currentBackgroundColor=0; s.currentForegroundColor=1;
        t=(int)(gameBoyContext.audioLineBufferLevel*100.0f);
        s.print(0, py,  "Buffer:    "+t+"%");
        if (gameBoyContext.audioSampleRate==0) t=0;
        else t=(int)(1000.0f*gameBoyContext.audioLineBufferLevel*(float)gameBoyContext.audioLineBufferLength/(float)gameBoyContext.audioSampleRate);
        s.print(16, py, "Latency:  "+t+" ms"); py++;
        s.print(0, py,  "Underflow: "+gameBoyContext.audioLineUnderflow);        
        s.print(16, py, "Overflow: "+gameBoyContext.audioLineOverflow); py++;
        
        DataBufferInt dataBuffer=(DataBufferInt)screenImg.getRaster().getDataBuffer();
        screen.drawScreen(dataBuffer.getData(), screenImg.getWidth(), screenImg.getHeight(), palette);
    }
    
    private void draw(Graphics g) {
        if (gameBoyContext==null) return;
        updateScreen();
        g.drawImage(screenImg, 1, 1, this);    
    }
    
    public void paint(Graphics g) { drawBackground(g); draw(g); }
    public void update(Graphics g) { paint(g); }

    private final KeyAdapter keyAdapter=new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            if (gameBoyContext==null) return;
            
            int key=e.getKeyCode();
            
            switch (key) {
            case KeyEvent.VK_SUBTRACT:
                if (gameBoyContext.videoFrameSkip>0) gameBoyContext.videoFrameSkip--;
                repaint();
                break;
            case KeyEvent.VK_ADD:
                if (gameBoyContext.videoFrameSkip<59) gameBoyContext.videoFrameSkip++;
                repaint();
                break;
            }
        }        
    };
}
//******************************************************************************
