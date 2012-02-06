package org.thenesis.gendes.awt;

import java.awt.AWTEventMulticaster;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;

import org.thenesis.gendes.contexts.Context;

public abstract class ViewCanvas extends Canvas {

    public final Context context=new Context();
    
    public ViewCanvas() {
        super();
        
        addComponentListener(componentAdapter);
        addFocusListener(focusAdapter);
    }
     
    public void detach() {
        context.detachAll();
    }
    
    //--------------------------------------------------------------------------------
    // Callbacks.
    //--------------------------------------------------------------------------------
    /**
     * Lock and returns the image.
     * It must be derived.
     */
    protected abstract Image lockImage();
    
    /**
     * Lock and returns the image.
     * It must be derived.
     */
    protected abstract void unlockImage();
    
    /**
     * Get a snapshot.
     * It must be derived.
     */
    protected void snapshot() {}

    /**
     * Called when a string is dropped.
     * @param s
     */
    protected void onDropString(String s) {}

    /**
     * Called when a file is dropped.
     * @param f
     */
    protected void onDropFile(File f) {}
    
    /**
     * Called when entering fullscreen mode.
     *
     */
    protected boolean onEnterFullScreen() { return true; }

    /**
     * Called when exiting fullscreen mode.
     *
     */
    protected boolean onExitFullScreen() { return false; }
    
    //--------------------------------------------------------------------------------
    // Image.
    //--------------------------------------------------------------------------------
    public int imageWidth, imageHeight, imageX, imageY;
    
    public boolean resizeDownWindow=true, resizeUpWindow=true, fullScreenFlag=false;
    private final Dimension windowDimension=new Dimension();    
    
    public static final int ZOOM_MODE_CUSTOM=       0;
    public static final int ZOOM_MODE_FIT=          1;
    public int zoomMode=ZOOM_MODE_CUSTOM;
    
    public static final int ZOOM_MAX=               8;
    public int zoom=2;
        
    private ActionListener onZoomChangedDispatcher;
    
    public void setZoom(int z) {
        if (z<1) z=1;
        if (z>ZOOM_MAX) z=ZOOM_MAX;
        this.zoom=z;
    }
    
    public void setZoomMode(int zm) {
        switch (zoomMode) {
        case ZOOM_MODE_CUSTOM:
        case ZOOM_MODE_FIT:
            zoomMode=zm;
            break;
        }
    }
    
    public void switchZoomMode() {
        switch (zoomMode) {
        case ZOOM_MODE_CUSTOM: zoomMode=ZOOM_MODE_FIT; break;
        case ZOOM_MODE_FIT: default: zoomMode=ZOOM_MODE_CUSTOM; break;
        }
    }
    
    public void updateZoom() {
//      windowDimension.setSize(GameBoy.SCREEN_WIDTH*zoom+8, GameBoy.SCREEN_HEIGHT*zoom+8);
      setPreferredSize(windowDimension);
      setMinimumSize(resizeUpWindow ? windowDimension : null);
      setMaximumSize(resizeDownWindow ? windowDimension : null); 
      backgroundDirty=true;
      repaint();
      notifyZoomChange();
    }
    
    public void addOnZoomChangedListener(ActionListener al) {
        onZoomChangedDispatcher=AWTEventMulticaster.add(onZoomChangedDispatcher, al);
    }
    
    public void removeOnZoomChangedListener(ActionListener al) {
        onZoomChangedDispatcher=AWTEventMulticaster.remove(onZoomChangedDispatcher, al);
    }

    private void notifyZoomChange() {
        if (onZoomChangedDispatcher!=null) onZoomChangedDispatcher.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));        
    }
    
    public void setFullScreen(boolean flag) {
	    if (flag) {
	        if (!fullScreenFlag) {
	        	if (!onEnterFullScreen()) fullScreenFlag=true;
	        }
	    } else {
	        if (fullScreenFlag) {
	        	if (!onExitFullScreen()) fullScreenFlag=false;
	        }
	    }
    }
    
    public void switchFullScreen() {
        setFullScreen(!fullScreenFlag);
    }
    
    //--------------------------------------------------------------------------------
    // AWT events.
    //--------------------------------------------------------------------------------
    private boolean backgroundDirty;
    
    private void drawBackground(Graphics g) {
        int w=getWidth(), h=getHeight();
        g.setColor(hasFocus() ? Color.LIGHT_GRAY : Color.WHITE);
        g.fillRect(0, 0, w, h);        
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, w-1, h-1);
    }
    
    private void drawView(Graphics g) {
        Image image=lockImage();
        if (image==null) return;
        
        int ww, wh, iw, ih;
        
        // Image Size.
        imageWidth=image.getWidth(this); imageHeight=image.getHeight(this);
        if (imageWidth<=0 || imageHeight<=0) return;
        
        // Window size.
        ww=getWidth(); wh=getHeight();
        if (ww<=0 || wh<=0) return;
        
        iw=imageWidth; ih=imageHeight;
        switch (zoomMode) {
        case ZOOM_MODE_CUSTOM:
            iw=imageWidth*zoom; ih=imageHeight*zoom;
            break;
        case ZOOM_MODE_FIT:
            iw=ww; ih=imageHeight*ww/imageWidth;
            if (ih>wh) { ih=wh; iw=imageWidth*wh/imageHeight; }
            break;
        }
        
        // Image position.
        imageX=(ww-iw)>>1; imageY=(wh-ih)>>1;
        
        // Draw.
        g.drawRect(imageX-1, imageY-1, iw+1, ih+1);
        if (zoom==1 && zoomMode==ZOOM_MODE_CUSTOM) g.drawImage(image, imageX, imageY, this);
        else g.drawImage(image, imageX, imageY, imageX+iw, imageY+ih, 0, 0, imageWidth, imageHeight, this);
        
        unlockImage();
    }
    
    public void paint(Graphics g) {
        drawBackground(g);
        drawView(g);
    }
    
    public void update(Graphics g) {
        if (backgroundDirty) {
            drawBackground(g);
            backgroundDirty=false;
        }
        drawView(g);
    }
        
    private final ComponentAdapter componentAdapter=new ComponentAdapter() {
        public void componentResized(ComponentEvent e) { backgroundDirty=true; repaint(); }
    };
    
    private final FocusAdapter focusAdapter=new FocusAdapter() {
        public void focusGained(FocusEvent arg0) { backgroundDirty=true; repaint(); }
        public void focusLost(FocusEvent arg0) { backgroundDirty=true; repaint(); }        
    };

    // Not used currently.
//    private final DropManager dropManager=new DropManager(this) {
//        public void onDropFile(File f) { ViewCanvas.this.onDropFile(f); }
//        public void onDropString(String s) { ViewCanvas.this.onDropString(s); }
//    };
}
