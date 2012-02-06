package org.thenesis.gendes.awt.gameboy.application;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.IOException;

import org.thenesis.gendes.awt.MyCheckboxMenu;
import org.thenesis.gendes.awt.MyCheckboxMenuItem;
import org.thenesis.gendes.awt.MyMenuItem;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.gameboy.GameBoy;

public class GameBoyMenu extends Menu {    
    private SystemEmulationContext systemContext;
    private GameBoyEmulationContext gameBoyContext;
    
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { GameBoyMenu.this.detach(); }
    };
    
    private ContextEventListener onResetListener=new ContextEventListener() {
        public void processEvent(ContextEvent ce) { updatePowerMenu(); }
    };
    
    private Frame frame;
    
    //------------------------------------------------------------------------------
    // Global.
    //------------------------------------------------------------------------------
    private Menu globalMenu=new Menu("Global");
    private MyCheckboxMenu globalMenuModelMenu=new MyCheckboxMenu("Game Boy model") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            gameBoyContext.setModel(newItem.userValue);
        }        
    };
    private MyCheckboxMenuItem globalMenuModelMenuItems[]={
            new MyCheckboxMenuItem("Game Boy (DMG)", GameBoy.MODEL_GAME_BOY),
            new MyCheckboxMenuItem("Game Boy Pocket (MGB)", GameBoy.MODEL_GAME_BOY_POCKET),
            new MyCheckboxMenuItem("Super Game Boy 1 (SGB1)", GameBoy.MODEL_SUPER_GAME_BOY),
            new MyCheckboxMenuItem("Super Game Boy 2 (SGB2)", GameBoy.MODEL_SUPER_GAME_BOY_2),
            new MyCheckboxMenuItem("Game Boy Color (CGB)", GameBoy.MODEL_GAME_BOY_COLOR),
    };
    private MyMenuItem globalMenuPowerOnItem=new MyMenuItem("Power ON") {
        protected void processActionEvent(ActionEvent e) {
            synchronized(systemContext.emulationSystem) { gameBoyContext.switchPower(true); }
        }
    };
    private MyMenuItem globalMenuPowerOffItem=new MyMenuItem("Power OFF") {
        protected void processActionEvent(ActionEvent e) {
            synchronized(systemContext.emulationSystem) { gameBoyContext.switchPower(false); }
        }       
    };
    private MyMenuItem globalMenuResetItem=new MyMenuItem("Reset") {
        protected void processActionEvent(ActionEvent e) { gameBoyContext.reset(); }        
    };
    
    //------------------------------------------------------------------------------
    // Cartridge.
    //------------------------------------------------------------------------------
    private Menu cartridgeMenu=new Menu("Cartridge");
    private MyMenuItem cartridgeMenuInsertItem=new MyMenuItem("Insert cartridge") {
        protected void processActionEvent(ActionEvent e) {
            FileDialog fileDialog=new FileDialog(frame, "Load Game Boy cartridge", FileDialog.LOAD);
            cartridgeSelect(fileDialog);
        }        
    };
    private MyMenuItem cartridgeMenuRemoveItem=new MyMenuItem("Remove cartridge") {
        protected void processActionEvent(ActionEvent e) {
            gameBoyContext.cartridgeRemove();
            setEnabled(gameBoyContext.gameBoy.isCartridgeInserted());
        }                
    };
    private MyCheckboxMenuItem cartridgeMenuAutoRunItem=new MyCheckboxMenuItem("Auto run on insertion") {
        protected void processItemEvent(ItemEvent e) {
            gameBoyContext.cartridgeAutoRunFlag=!gameBoyContext.cartridgeAutoRunFlag;
            setState(gameBoyContext.cartridgeAutoRunFlag);
        }
    };
    
    private void cartridgeSelect(FileDialog fileDialog) {
        fileDialog.setModal(true);
        fileDialog.setVisible(true);
        String file=fileDialog.getFile(), directory=fileDialog.getDirectory();
        if (file!=null) {
        	try {
        		gameBoyContext.cartridgeInsert(directory+'/'+file);
        	} catch (IOException e) {
        		
        	}
        }
    }
    
    //------------------------------------------------------------------------------
    // Video.
    //------------------------------------------------------------------------------
    private Menu videoMenu=new Menu("Video");
    private MyCheckboxMenuItem videoMenuGenerationItem=new MyCheckboxMenuItem("Activate video generation") {
        protected void processItemEvent(ItemEvent e) {
            gameBoyContext.videoGenerationEnabled=!gameBoyContext.videoGenerationEnabled;
            setState(gameBoyContext.videoGenerationEnabled);
        }        
    };
    private MyCheckboxMenuItem videoMenuPresentationItem=new MyCheckboxMenuItem("Activate video presentation") {
        protected void processItemEvent(ItemEvent e) {
            gameBoyContext.videoPresentationEnabled=!gameBoyContext.videoPresentationEnabled;
            setState(gameBoyContext.videoPresentationEnabled);
        }        
    };
    private MyCheckboxMenuItem videoMenuAutoFrameSkipModeItem=new MyCheckboxMenuItem("Auto frame skip mode") {
        protected void processItemEvent(ItemEvent e) {
            gameBoyContext.videoFrameSkipAutoModeEnabled=!gameBoyContext.videoFrameSkipAutoModeEnabled;
            setState(gameBoyContext.videoFrameSkipAutoModeEnabled);
        }        
    };
    private static final int NB_FRAME_SKIP_VALUES=  8;
    private MyCheckboxMenu videoMenuFrameSkipMenu=new MyCheckboxMenu("Frame skip") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            gameBoyContext.videoFrameSkip=newItem.userValue;
        }        
    };
    private MyCheckboxMenuItem videoMenuFrameSkipMenuItems[]=new MyCheckboxMenuItem[NB_FRAME_SKIP_VALUES];

    //------------------------------------------------------------------------------
    // Audio.
    //------------------------------------------------------------------------------
    private Menu audioMenu=new Menu("Audio");
    private MyCheckboxMenuItem audioMenuPresentationItem=new MyCheckboxMenuItem("Activate audio") {
        protected void processItemEvent(ItemEvent e) {
        	gameBoyContext.audioEnable(!gameBoyContext.audioEnabled);
            setState(gameBoyContext.audioEnabled);
        }                
    };
    private MyCheckboxMenu audioMenuFrequencyMenu=new MyCheckboxMenu("Frequency") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            gameBoyContext.audioSetFrequency(newItem.userValue);
        }
    };
    private MyCheckboxMenuItem audioMenuFrequencyMenuItems[]={
        new MyCheckboxMenuItem("11025", 11025),
        new MyCheckboxMenuItem("22050", 22050),
        new MyCheckboxMenuItem("44100", 44100),
    };
    private MyCheckboxMenuItem audioMenuRecordingItem=new MyCheckboxMenuItem("Activate recording") {
        protected void processItemEvent(ItemEvent e) {
        	if (gameBoyContext.audioRecordingEnabled) gameBoyContext.audioStopRecording();
        	else gameBoyContext.audioStartRecording();
            setState(gameBoyContext.audioRecordingEnabled);
        }        
    };    
    
    //------------------------------------------------------------------------------
    // Joypad.
    //------------------------------------------------------------------------------
    private Menu joypadMenu=new Menu("Joypad");
    private MyMenuItem joypadMenuConfigureItem=new MyMenuItem("Configure");
    
    //------------------------------------------------------------------------------
    // Serial.
    //------------------------------------------------------------------------------
    private Menu serialMenu=new Menu("Serial");
    
    //------------------------------------------------------------------------------
    // Infrared.
    //------------------------------------------------------------------------------
    private Menu infraredMenu=new Menu("Infrared");
    
    //------------------------------------------------------------------------------
    // Common.
    //------------------------------------------------------------------------------
    public GameBoyMenu() {
        super("Game Boy");
        buildMenu();
    }
    
    public void attach(GameBoyEmulationContext gbc, Frame frame) {
        detach();
        this.frame=frame; systemContext=gbc.systemContext; gameBoyContext=gbc;
        gameBoyContext.add(gameBoyNode);
        gameBoyContext.onResetDispatcher.addListener(onResetListener);        
        updateMenu();
    }    
    
    public void detach() {
        if (gameBoyContext!=null) {
            gameBoyContext.onResetDispatcher.removeListener(onResetListener);
            gameBoyContext.remove(gameBoyNode);
            frame=null; systemContext=null; gameBoyContext=null;
        }
    }
 
    private void buildMenu() {
        globalMenuModelMenu.setItems(globalMenuModelMenuItems);
        globalMenu.add(globalMenuModelMenu);
        globalMenu.add(globalMenuPowerOnItem);
        globalMenu.add(globalMenuPowerOffItem);
        globalMenu.add(globalMenuResetItem);
        add(globalMenu);
        
        cartridgeMenu.add(cartridgeMenuInsertItem);
        cartridgeMenu.add(cartridgeMenuRemoveItem);
        cartridgeMenu.add(cartridgeMenuAutoRunItem);
        add(cartridgeMenu);
        
        videoMenu.add(videoMenuGenerationItem);
        videoMenu.add(videoMenuPresentationItem);
        videoMenu.add(videoMenuAutoFrameSkipModeItem);
        for (int i=0; i<NB_FRAME_SKIP_VALUES; i++) videoMenuFrameSkipMenuItems[i]=new MyCheckboxMenuItem(String.valueOf(i), i);
        videoMenuFrameSkipMenu.setItems(videoMenuFrameSkipMenuItems);
        videoMenu.add(videoMenuFrameSkipMenu);
        add(videoMenu);
        
        audioMenu.add(audioMenuPresentationItem);
        audioMenuFrequencyMenu.setItems(audioMenuFrequencyMenuItems);
        audioMenu.add(audioMenuFrequencyMenu);
        audioMenu.add(audioMenuRecordingItem);
        add(audioMenu);
        
        joypadMenu.add(joypadMenuConfigureItem);
        add(joypadMenu);
        
        add(serialMenu);
        
        add(infraredMenu);        
    }
    
    private void updateMenu() {
        globalMenuModelMenu.setCurrentItemByValue(gameBoyContext.gameBoy.getModel());
        updatePowerMenu();

        cartridgeMenuRemoveItem.setEnabled(gameBoyContext.gameBoy.isCartridgeInserted());
        cartridgeMenuAutoRunItem.setState(gameBoyContext.cartridgeAutoRunFlag);
        
        videoMenuGenerationItem.setState(gameBoyContext.videoGenerationEnabled);
        videoMenuPresentationItem.setState(gameBoyContext.videoPresentationEnabled);
        videoMenuAutoFrameSkipModeItem.setState(gameBoyContext.videoFrameSkipAutoModeEnabled);
        for (int i=0; i<NB_FRAME_SKIP_VALUES; i++) videoMenuFrameSkipMenuItems[i].setState(i==gameBoyContext.videoFrameSkip);

        audioMenuPresentationItem.setState(gameBoyContext.audioLineStopped);
        audioMenuFrequencyMenu.setCurrentItemByValue(gameBoyContext.audioSampleRate);
        audioMenuRecordingItem.setState(gameBoyContext.audioRecordingEnabled);
    }
    
    private void updatePowerMenu() {
        boolean f=gameBoyContext.gameBoy.isPowered();
        globalMenuPowerOnItem.setEnabled(!f);
        globalMenuPowerOffItem.setEnabled(f);
        globalMenuResetItem.setEnabled(f);        
    }
}
