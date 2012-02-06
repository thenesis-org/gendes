package org.thenesis.gendes.awt.gameboy.application;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.thenesis.gendes.awt.AboutDialog;
import org.thenesis.gendes.awt.MyMenuItem;
import org.thenesis.gendes.awt.ViewMenu;
import org.thenesis.gendes.awt.gameboy.VideoCanvas;
import org.thenesis.gendes.awt.system.EmulationMenu;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;


//******************************************************************************
// Main frame.
//******************************************************************************
public final class MainFrame extends Frame {
	private GameBoyEmulatorApplication application;
    private SystemEmulationContext systemContext;
    private GameBoyEmulationContext gameBoyContext;
    
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { MainFrame.this.detach(); }
    };
    
    public MainFrame(GameBoyEmulatorApplication app) {
        super("Java Game Boy emulator");
        application=app;
        
        addWindowListener(windowAdapter);
        addFocusListener(focusAdapter);
        
        fullScreenFrame=new Frame("Full-screen window");
        fullScreenFrame.setResizable(false);
        fullScreenFrame.setUndecorated(true);
        
        buildMenu();
        
        add(videoPanel);
    }
    
    public void attach(GameBoyEmulationContext gbc) {
        detach();
        systemContext=gbc.systemContext; gameBoyContext=gbc;
        gameBoyContext.add(gameBoyNode);
        videoPanel.attach(gbc);
        emulationMenu.attach(systemContext, this);
        gameBoyMenu.attach(gameBoyContext, this);
        viewMenu.attach(videoPanel);
    }    
    
    public void detach() {
        if (gameBoyContext==null) return;
        emulationMenu.detach();
        gameBoyMenu.detach();
        viewMenu.detach();
        videoPanel.detach();
        gameBoyContext.remove(gameBoyNode);
        systemContext=null; gameBoyContext=null;
    }
    
    public void update(Graphics g) {}
    
    //------------------------------------------------------------------------------
    // Main frame events.
    //------------------------------------------------------------------------------
    private final WindowAdapter windowAdapter=new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
        	setVisible(false);
        	dispose();
            application.shutdown();
        }    
    };
    
    private final FocusAdapter focusAdapter=new FocusAdapter() {
        public void focusGained(FocusEvent arg0) { videoPanel.requestFocusInWindow(); }        
    };
    
    //------------------------------------------------------------------------------
    // Menu.
    //------------------------------------------------------------------------------
    private final MenuBar menuBar=new MenuBar();
    private final EmulationMenu emulationMenu=new EmulationMenu();
    private final GameBoyMenu gameBoyMenu=new GameBoyMenu();
    private final ViewMenu viewMenu=new ViewMenu();
        
    private final Menu helpMenu=new Menu("Help");
    private final MyMenuItem helpMenuAboutItem=new MyMenuItem("About") {
        protected void processActionEvent(ActionEvent e) {
            AboutDialog aboutDialog=new AboutDialog(MainFrame.this);
            aboutDialog.setVisible(true);
        }
    };
    
    private void buildMenu() {
        menuBar.add(emulationMenu);
        menuBar.add(gameBoyMenu);
        menuBar.add(viewMenu);        
        helpMenu.add(helpMenuAboutItem);
        menuBar.add(helpMenu);
        setMenuBar(menuBar);
    }
    
    //------------------------------------------------------------------------------
    // Video panel.
    //------------------------------------------------------------------------------
    private boolean fullScreenFlag=false;
    private Frame fullScreenFrame;
    
    private boolean enterFullScreen() {
    	if (fullScreenFlag) return false;
        GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd=ge.getDefaultScreenDevice();
        if (!gd.isFullScreenSupported()) return true;
        
        videoPanel.setVisible(false);
    	remove(videoPanel);
    	validate();
    	
    	fullScreenFrame.add(videoPanel);
        videoPanel.setVisible(true);
    	fullScreenFrame.validate();
    	fullScreenFrame.setVisible(true);
        gd.setFullScreenWindow(fullScreenFrame);
        
        videoPanel.requestFocusInWindow();
        fullScreenFlag=true;
    	return false;
    }
    
    private boolean exitFullScreen() {
        GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd=ge.getDefaultScreenDevice();
        
        videoPanel.setVisible(false);
    	fullScreenFrame.remove(videoPanel);
    	fullScreenFrame.validate();
    	fullScreenFrame.dispose();
    	
    	add(videoPanel);
        videoPanel.setVisible(true);
    	validate();
        gd.setFullScreenWindow(null);
//    	requestFocus();
        
        videoPanel.requestFocusInWindow();
        fullScreenFlag=false;
    	return false;
    }
    
    final VideoCanvas videoPanel=new VideoCanvas() {
        protected boolean onEnterFullScreen() { return enterFullScreen(); }
        protected boolean onExitFullScreen() { return exitFullScreen(); }
    };
}
//******************************************************************************
