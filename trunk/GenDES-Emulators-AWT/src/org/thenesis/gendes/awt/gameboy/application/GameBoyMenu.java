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
        public void processEvent(ContextEvent ce) { globalMenuUpdate(); }
    };
    
    private Frame frame;
    
    //------------------------------------------------------------------------------
    // Global.
    //------------------------------------------------------------------------------
    private Menu globalMenu=new Menu("Global");
    private MyCheckboxMenu globalMenuModelMenu=new MyCheckboxMenu("Game Boy model") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            gameBoyContext.setModel(newItem.userValue);
            globalMenuUpdate();
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
            synchronized(systemContext.emulationSystem) {
            	gameBoyContext.switchPower(true);
            	globalMenuUpdate();
            }
        }
    };
    private MyMenuItem globalMenuPowerOffItem=new MyMenuItem("Power OFF") {
        protected void processActionEvent(ActionEvent e) {
            synchronized(systemContext.emulationSystem) {
            	gameBoyContext.switchPower(false);
            	globalMenuUpdate();
            }
        }       
    };
    private MyMenuItem globalMenuResetItem=new MyMenuItem("Reset") {
        protected void processActionEvent(ActionEvent e) {
        	gameBoyContext.reset();
        	globalMenuUpdate();
        }        
    };

    private void globalMenuUpdate() {
        globalMenuModelMenu.setCurrentItemByValue(gameBoyContext.gameBoy.getModel());
        
        boolean f=gameBoyContext.gameBoy.isPowered();
        globalMenuPowerOnItem.setEnabled(!f);
        globalMenuPowerOffItem.setEnabled(f);
        globalMenuResetItem.setEnabled(f);        
    }

    //------------------------------------------------------------------------------
    // Cartridge.
    //------------------------------------------------------------------------------
    private Menu cartridgeMenu=new Menu("Cartridge");
	private MyMenuItem cartridgeMenuInsertItem =new MyMenuItem("Insert cartridge") {
        protected void processActionEvent(ActionEvent e) {
            FileDialog fileDialog=new FileDialog(frame, "Load Game Boy cartridge", FileDialog.LOAD);
            cartridgeSelect(fileDialog);
            cartridgeMenuUpdate();
        }        
    };
    private MyMenuItem cartridgeMenuRemoveItem=new MyMenuItem("Remove cartridge") {
        protected void processActionEvent(ActionEvent e) {
            gameBoyContext.cartridgeRemove();
            cartridgeMenuUpdate();
        }                
    };
    private MyCheckboxMenuItem cartridgeMenuAutoRunItem=new MyCheckboxMenuItem("Auto run on insertion") {
        protected void processItemEvent(ItemEvent e) {
            gameBoyContext.cartridgeAutoRunFlag=!gameBoyContext.cartridgeAutoRunFlag;
            cartridgeMenuUpdate();
        }
    };
    
    private void cartridgeMenuUpdate() {
        cartridgeMenuRemoveItem.setEnabled(gameBoyContext.gameBoy.isCartridgeInserted());
        cartridgeMenuAutoRunItem.setState(gameBoyContext.cartridgeAutoRunFlag);
    }

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
            videoMenuUpdate();
        }        
    };
    private MyCheckboxMenuItem videoMenuPresentationItem=new MyCheckboxMenuItem("Activate video presentation") {
        protected void processItemEvent(ItemEvent e) {
            gameBoyContext.videoPresentationEnabled=!gameBoyContext.videoPresentationEnabled;
            videoMenuUpdate();
        }        
    };
    private MyCheckboxMenuItem videoMenuAutoFrameSkipModeItem=new MyCheckboxMenuItem("Auto frame skip mode") {
        protected void processItemEvent(ItemEvent e) {
            gameBoyContext.videoFrameSkipAutoModeEnabled=!gameBoyContext.videoFrameSkipAutoModeEnabled;
            videoMenuUpdate();
        }        
    };
    private static final int NB_FRAME_SKIP_VALUES=  8;
    private MyCheckboxMenu videoMenuFrameSkipMenu=new MyCheckboxMenu("Frame skip") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            gameBoyContext.videoFrameSkip=newItem.userValue;
            videoMenuUpdate();
        }        
    };
    private MyCheckboxMenuItem videoMenuFrameSkipMenuItems[]=new MyCheckboxMenuItem[NB_FRAME_SKIP_VALUES];

    private void videoMenuUpdate() {
        videoMenuGenerationItem.setState(gameBoyContext.videoGenerationEnabled);
        videoMenuPresentationItem.setState(gameBoyContext.videoPresentationEnabled);
        videoMenuAutoFrameSkipModeItem.setState(gameBoyContext.videoFrameSkipAutoModeEnabled);
        for (int i=0; i<NB_FRAME_SKIP_VALUES; i++) videoMenuFrameSkipMenuItems[i].setState(i==gameBoyContext.videoFrameSkip);
    }
    
    //------------------------------------------------------------------------------
    // Audio.
    //------------------------------------------------------------------------------
    private Menu audioMenu=new Menu("Audio");
    private MyCheckboxMenuItem audioMenuPresentationItem=new MyCheckboxMenuItem("Activate audio") {
        protected void processItemEvent(ItemEvent e) {
        	gameBoyContext.audioBackEnd.enable(!gameBoyContext.audioBackEnd.isEnabled());
        	audioMenuUpdate();
        }
    };
    private MyCheckboxMenu audioMenuFrequencyMenu=new MyCheckboxMenu("Frequency") {
        public void onItemChanged(MyCheckboxMenuItem lastItem, MyCheckboxMenuItem newItem) {
            gameBoyContext.audioBackEnd.setFrequency(newItem.userValue);
            audioMenuUpdate();
        }
    };
    private MyCheckboxMenuItem audioMenuFrequencyMenuItems[]={
        new MyCheckboxMenuItem("11025", 11025),
        new MyCheckboxMenuItem("22050", 22050),
        new MyCheckboxMenuItem("44100", 44100),
    };
    private MyCheckboxMenuItem audioMenuRecordingItem=new MyCheckboxMenuItem("Activate recording") {
        protected void processItemEvent(ItemEvent e) {
        	if (gameBoyContext.audioBackEnd.isRecording()) gameBoyContext.audioBackEnd.stopRecording();
        	else {
        		gameBoyContext.audioStartRecording();
        	}
        	audioMenuUpdate();
        }        
    };    
    
    private void audioMenuUpdate() {
        audioMenuPresentationItem.setState(gameBoyContext.audioBackEnd.isStarted());
        audioMenuFrequencyMenu.setCurrentItemByValue(gameBoyContext.audioBackEnd.getFrequency());
        audioMenuRecordingItem.setState(gameBoyContext.audioBackEnd.isRecording());
    }
    
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
    	globalMenuUpdate();
        cartridgeMenuUpdate();
        videoMenuUpdate();
        audioMenuUpdate();
    }
}
