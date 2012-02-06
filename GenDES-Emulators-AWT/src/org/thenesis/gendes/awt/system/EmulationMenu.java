package org.thenesis.gendes.awt.system;

import java.awt.Frame;
import java.awt.Menu;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

import org.thenesis.gendes.EmulationState;
import org.thenesis.gendes.awt.MyCheckboxMenu;
import org.thenesis.gendes.awt.MyCheckboxMenuItem;
import org.thenesis.gendes.awt.MyMenuItem;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.SystemEmulationContext;

public class EmulationMenu extends Menu {    
    private SystemEmulationContext systemContext;
    
    private boolean emulationAutoSaveSessionFlag=true;
    private int emulationCurrentStateSlot=0;
    private final EmulationState emulationStateSlots[]=new EmulationState[NB_STATE_SLOTS];
        
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { EmulationMenu.this.detach(); }
    };
    
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { updateRunMenu(); }
    };
    
    private final ContextEventListener onResumeListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { updateRunMenu(); }
    };
    
    @SuppressWarnings("unused")
	private Frame frame;
    
    private final MyMenuItem saveSessionItem=new MyMenuItem("Save session");
    private final MyMenuItem loadSessionItem=new MyMenuItem("Load session");
    private final MyCheckboxMenuItem autoSaveSessionItem=new MyCheckboxMenuItem("Activate session auto save");
    private static final int NB_STATE_SLOTS=        8;
    private final MyCheckboxMenu currentStateSlotMenu=new MyCheckboxMenu("Current state slot") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            emulationCurrentStateSlot=newItem.userValue;
        }
    };
    private final MyCheckboxMenuItem currentStateSlotMenuItems[]=new MyCheckboxMenuItem[NB_STATE_SLOTS];
    private final MyMenuItem captureStateItem=new MyMenuItem("Capture state");
    private final MyMenuItem restoreStateItem=new MyMenuItem("Restore state");
    private final MyMenuItem clearStateItem=new MyMenuItem("Clear state");
    private final MyMenuItem saveStateToFileItem=new MyMenuItem("Save state to file");
    private final MyMenuItem loadStateFromFileItem=new MyMenuItem("Load state from file");
    private final MyMenuItem runItem=new MyMenuItem("Run") {
        protected void processActionEvent(ActionEvent e) {
            if (systemContext==null) return;
            systemContext.emulationThread.resume();
        }
    };
    private final MyMenuItem pauseItem=new MyMenuItem("Pause") {
        protected void processActionEvent(ActionEvent e) {
            if (systemContext==null) return;
            systemContext.emulationThread.pause();
        }        
    };
    private final MyCheckboxMenuItem throttlingItem=new MyCheckboxMenuItem("Activate throttling") {
        protected void processItemEvent(ItemEvent e) {
            if (systemContext==null) return;
            boolean f=!systemContext.emulationThread.isThrottlingEnabled();
            systemContext.emulationThread.enableThrottling(f);
            System.out.println("Throttling changed");
            setState(f);
        }
    };
    private final MyCheckboxMenu speedMenu=new MyCheckboxMenu("Speed") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            if (systemContext==null) return;
            systemContext.emulationThread.setSpeed(newItem.userValue/100.0);
        }
    };
    private final MyCheckboxMenuItem speedMenuItems[]={
            new MyCheckboxMenuItem(" 25%", 25),
            new MyCheckboxMenuItem(" 50%", 50),
            new MyCheckboxMenuItem("100%", 100),
            new MyCheckboxMenuItem("200%", 200),            
            new MyCheckboxMenuItem("300%", 300),            
            new MyCheckboxMenuItem("400%", 400),            
    };
    private final MyMenuItem exitItem=new MyMenuItem("Exit") {
        protected void processActionEvent(ActionEvent e) {}
    };
    
    public EmulationMenu() {
        super("Emulation");
        buildMenu();
    }
    
    public void attach(SystemEmulationContext sec, Frame frame) {
        detach();
        this.frame=frame;
        systemContext=sec;
        systemContext.add(gameBoyNode);
        systemContext.onPauseDispatcher.addListener(onPauseListener);        
        systemContext.onResumeDispatcher.addListener(onResumeListener);
        updateMenu();
    }
    
    public void detach() {
        if (systemContext!=null) {
            systemContext.onPauseDispatcher.removeListener(onPauseListener);        
            systemContext.onResumeDispatcher.removeListener(onResumeListener);
            systemContext.remove(gameBoyNode);
            systemContext=null;
            frame=null;
        }
    }
    
    void buildMenu() {
        add(saveSessionItem);
        add(loadSessionItem);
        add(autoSaveSessionItem);
        addSeparator();
        for (int i=0; i<NB_STATE_SLOTS; i++) currentStateSlotMenuItems[i]=new MyCheckboxMenuItem(String.valueOf(i), i);
        currentStateSlotMenu.setItems(currentStateSlotMenuItems);
        add(currentStateSlotMenu);
        add(captureStateItem);
        add(restoreStateItem);
        add(clearStateItem);
        add(saveStateToFileItem);
        add(loadStateFromFileItem);        
        addSeparator();
        add(runItem);
        add(pauseItem);
        add(throttlingItem);
        speedMenu.setItems(speedMenuItems);
        add(speedMenu);
        addSeparator();
        add(exitItem);        
    }
    
    void updateMenu() {
        if (systemContext==null) return;
        autoSaveSessionItem.setState(emulationAutoSaveSessionFlag);
        currentStateSlotMenu.setCurrentItemByValue(emulationCurrentStateSlot);
        restoreStateItem.setEnabled(emulationStateSlots[emulationCurrentStateSlot]!=null);
        clearStateItem.setEnabled(emulationStateSlots[emulationCurrentStateSlot]!=null);
        saveStateToFileItem.setEnabled(emulationStateSlots[emulationCurrentStateSlot]!=null);
        updateRunMenu();
        throttlingItem.setState(systemContext.emulationThread.isThrottlingEnabled());
        speedMenu.setCurrentItemByValue(100);
    }
    
    private void updateRunMenu() {
        if (systemContext==null) return;
        boolean f=systemContext.emulationThread.isPaused();
        runItem.setEnabled(f);
        pauseItem.setEnabled(!f);
    }
}
