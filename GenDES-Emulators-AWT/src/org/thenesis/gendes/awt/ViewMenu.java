package org.thenesis.gendes.awt;

import java.awt.Menu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.thenesis.gendes.contexts.ContextNode;

public class ViewMenu extends Menu {
    private final ContextNode node=new ContextNode() {
    	public void detach() { ViewMenu.this.detach(); }
    };
    
    private ViewCanvas viewCanvas;
    
    private final MyCheckboxMenu zoomModeMenu=new MyCheckboxMenu("Zoom mode") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            if (viewCanvas==null) return;
            viewCanvas.setZoomMode(newItem.userValue);
            viewCanvas.updateZoom();
        }
    };
    private final MyCheckboxMenuItem zoomModeMenuItems[]={
        new MyCheckboxMenuItem("Custom", ViewCanvas.ZOOM_MODE_CUSTOM),
        new MyCheckboxMenuItem("Fit", ViewCanvas.ZOOM_MODE_FIT),
    };
    private final MyCheckboxMenu zoomMenu=new MyCheckboxMenu("Zoom") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            if (viewCanvas==null) return;
            viewCanvas.setZoom(newItem.userValue);
            viewCanvas.updateZoom();
        }        
    };
    private static final int NB_ZOOM_VALUES=        8;
    private final MyCheckboxMenuItem zoomMenuItems[]=new MyCheckboxMenuItem[NB_ZOOM_VALUES];
    private final MyMenuItem snapshotItem=new MyMenuItem("Snapshot") {
        protected void processActionEvent(ActionEvent e) { if (viewCanvas!=null) viewCanvas.snapshot(); }        
    };
    private final MyMenuItem fullscreenItem=new MyMenuItem("Fullscreen") {
        protected void processActionEvent(ActionEvent e) { if (viewCanvas!=null) viewCanvas.switchFullScreen(); }
    };
    
    private final ActionListener onZoomChangedListener=new ActionListener() {
        public void actionPerformed(ActionEvent e) { updateMenu(); }
    };
    
    public ViewMenu() {
        super("View");
        buildMenu();
    }
    
    public void attach(ViewCanvas vc) {
        detach();
        viewCanvas=vc;
        viewCanvas.context.add(node);
        viewCanvas.addOnZoomChangedListener(onZoomChangedListener);
        updateMenu();
    }
    
    public void detach() {
        if (viewCanvas==null) return;
        viewCanvas.context.remove(node);
        viewCanvas=null;
    }
    
    private void buildMenu() {
        zoomModeMenu.setItems(zoomModeMenuItems);
        add(zoomModeMenu);
        for (int i=0; i<NB_ZOOM_VALUES; i++) zoomMenuItems[i]=new MyCheckboxMenuItem(String.valueOf(1+i), 1+i);
        zoomMenu.setItems(zoomMenuItems);
        add(zoomMenu);
        add(fullscreenItem);
        add(snapshotItem);
    }
    
    private void updateMenu() {
        if (viewCanvas==null) return;
        zoomModeMenu.setCurrentItemByValue(viewCanvas.zoomMode);
        zoomMenu.setCurrentItemByValue(viewCanvas.zoom);
    }
   
}
