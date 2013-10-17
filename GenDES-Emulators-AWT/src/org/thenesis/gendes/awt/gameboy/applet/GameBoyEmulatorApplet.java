package org.thenesis.gendes.awt.gameboy.applet;

import java.applet.Applet;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.thenesis.gendes.awt.HttpFileDownloader;
import org.thenesis.gendes.awt.Utilities;
import org.thenesis.gendes.awt.gameboy.VideoCanvas;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.gameboy.GameBoy;


//******************************************************************************
// Main class.
//******************************************************************************
@SuppressWarnings("serial")
public class GameBoyEmulatorApplet extends Applet {	
	private SystemEmulationContext systemContext;
	private GameBoyEmulationContext gameBoyContext;
    
	private boolean appletFlag=true;
	private Frame mainFrame;
	private VideoCanvas videoCanvas=new VideoCanvas() {
	    protected void snapshot() {
	        if (gameBoyContext!=null && !appletFlag) gameBoyContext.videoSnapshot();                                 
	    }
	    
	    protected void onDropString(String s) {
	    	if (gameBoyContext!=null && !appletFlag) loadCartridge(s);    	
	    }
	    
	    protected void onDropFile(File f) {
	    	if (gameBoyContext!=null && !appletFlag) loadCartridge(f.getAbsolutePath());
	    }		
	};
	// Common.
	private static final int
		OPERATION_NONE=0,
		OPERATION_DOWNLOADING_LIST=1,
		OPERATION_DOWNLOADING_CARTRIDGE=2;
	private int currentOperation=OPERATION_NONE;
	private final HttpFileDownloader downloader=new HttpFileDownloader();
	// Emulation.
	private Button runButton, pauseButton, resetButton;
	// Cartridge.
	private Button loadButton, removeButton;
	private TextField cartridgeNameTextField;
	private String cartridgeName="";
	// Server.
	private Button updateButton, downloadButton, cancelButton;
	private TextField serverAddressTextField;
	private List backupsList;
	// Infos label.
	private TextField infosTextField;
	private String infosMessage;
	
    //------------------------------------------------------------------------------
	// Application.
    //------------------------------------------------------------------------------
    // Application main routine.
    public static void main(String[] args) {
    	GameBoyEmulatorApplet applet=null;
        try {
            applet=new GameBoyEmulatorApplet();
            applet.appletFlag=false;
            applet.init();
            applet.start();

            if (args.length==1) applet.loadCartridge(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) ;
    }
    
    private void shutdown() {
    	destroy();
    	System.exit(0);    	
    }

    //------------------------------------------------------------------------------
    // Applet.
    //------------------------------------------------------------------------------
    public void init() {
        try {
            emulationInitialize();
            guiInitialize();
        } catch (Exception e) {
        	removeAll();
        	TextArea ta=new TextArea("Cannot initialize applet.");
        	add(ta);
        }
    }
    
    public void destroy() {
    	guiShutdown();
    	emulationShutdown();
    }
    
    public void start() {
    	
    }
    
    public void stop() {
    	
    }
    
    //------------------------------------------------------------------------------
    // Emulation.
    //------------------------------------------------------------------------------
    private ContextEventListener onCartridgeRemovedListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { saveCartridgeBackup(); }
    };
    
    private ContextEventListener onCartridgeInsertedListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) { loadCartridgeBackup(); }
    };
	
    // Initialize emulation stuff.
    private void emulationInitialize() throws Exception {
        systemContext=new SystemEmulationContext();
        systemContext.emulationThread.pause();
        systemContext.emulationThread.start();
        
        gameBoyContext=new GameBoyEmulationContext(systemContext);
        gameBoyContext.audioBackEnd.enable(true);
        gameBoyContext.setModel(GameBoy.MODEL_GAME_BOY_COLOR);
        gameBoyContext.onCartridgeRemovedDispatcher.addListener(onCartridgeRemovedListener);
        gameBoyContext.onCartridgeInsertedDispatcher.addListener(onCartridgeInsertedListener);
    	gameBoyContext.switchPower(true);
    }

    // Shutdown emulation stuff.
    private void emulationShutdown() {
    	systemContext.shutdown();
    }
    
    //------------------------------------------------------------------------------
    // GUI.
    //------------------------------------------------------------------------------
    // Initialize the GUI.
    private void guiInitialize() throws Exception {    	
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyEventDispatcher);
        addFocusListener(focusAdapter);

        // The GridBagLayout contains the whole interface.
        GridBagLayout gridbagLayout=new GridBagLayout();
        GridBagConstraints c=new GridBagConstraints();
        Dimension d=new Dimension();
        setLayout(gridbagLayout);
        int gridy=0;
        
        // The video canvas show the Game Boy screen.
        c.gridx=0; c.gridy=gridy; c.gridwidth=3; c.gridheight=1;
        c.weightx=0.0; c.weighty=1.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        videoCanvas.attach(gameBoyContext);
        d.setSize(320+16, 288+16);
        videoCanvas.setMinimumSize(d); videoCanvas.setPreferredSize(d);    	
        add(videoCanvas, c);
        gridy++;

        // The buttonPanel contains the main buttons.
        {
	        c.gridx=0; c.gridy=gridy; c.gridwidth=3; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        Panel mainButtonPanel=new Panel();
	        GridLayout gridLayout=new GridLayout(1, appletFlag ? 2 : 4);
	        mainButtonPanel.setLayout(gridLayout);
	        add(mainButtonPanel, c);
	        gridy++;

	        // The "Run" button runs the emulation.
	        runButton=new Button("Run");
	        runButton.addActionListener(actionListener);
	        mainButtonPanel.add(runButton);
	        
	        // The "Pause" button pauses the emulation.        
	        pauseButton=new Button("Pause");
	        pauseButton.addActionListener(actionListener);
	        mainButtonPanel.add(pauseButton);
	        
	        // The "Reset" button resets the Game Boy.        
	        resetButton=new Button("Reset");
	        resetButton.addActionListener(actionListener);
	        mainButtonPanel.add(resetButton);
        }
        
        // Cartridge fields.
        {
	        // Label for the cartridge fields.
	        c.gridx=0; c.gridy=gridy; c.gridwidth=3; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        Label cartridgeFieldsLabel=new Label("Cartridge:");
	        cartridgeFieldsLabel.setBackground(Color.LIGHT_GRAY);
	        add(cartridgeFieldsLabel, c);
	        gridy++;

	        // Cartridge name.
	        c.gridx=0; c.gridy=gridy; c.gridwidth=1; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        Label cartridgeLabel=new Label("Name:");
	        add(cartridgeLabel, c);
	        c.gridx=1; c.gridy=gridy; c.gridwidth=2; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        cartridgeNameTextField=new TextField(cartridgeName);
	        cartridgeNameTextField.setEditable(false);
	        add(cartridgeNameTextField, c);
	        gridy++;
	        
            // Cartridge button panel.
	        c.gridx=0; c.gridy=gridy; c.gridwidth=3; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        Panel cartridgeButtonPanel=new Panel();
	        GridLayout gridLayout=new GridLayout(1, 3);
	        cartridgeButtonPanel.setLayout(gridLayout);
	        add(cartridgeButtonPanel, c);
	        gridy++;

            // The "Load" button is only used in application mode to load a file locally.
	        loadButton=new Button("Load");
	        loadButton.addActionListener(actionListener);
	        cartridgeButtonPanel.add(loadButton);
	        
	        // The "Download" button downloads the current backup selected. 
	        downloadButton=new Button("Download");
	        downloadButton.addActionListener(actionListener);
	        cartridgeButtonPanel.add(downloadButton);
	        
            // The "remove" button allows to remove the current cartridge.
	        removeButton=new Button("Remove");
	        removeButton.addActionListener(actionListener);
	        removeButton.setEnabled(false);
	        cartridgeButtonPanel.add(removeButton);
	        
        }

        // Server fields.
        {
	        // Label for the server fields. 
	        c.gridx=0; c.gridy=gridy; c.gridwidth=3; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        Label serverFieldsLabel=new Label("Server:");
	        serverFieldsLabel.setBackground(Color.LIGHT_GRAY);
	        add(serverFieldsLabel, c);
	        gridy++;
	        
	        // Server address.
	        c.gridx=0; c.gridy=gridy; c.gridwidth=1; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        Label serverAddressNameLabel=new Label("Address:");
	        add(serverAddressNameLabel, c);
	        c.gridx=1; c.gridy=gridy; c.gridwidth=2; c.gridheight=1;
	        c.weightx=1.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
//	        serverAddressTextField=new TextField("http://localhost/gbroms");
//	        String serverAddress=getParameter("serverAddress");
//	        if (serverAddress==null) serverAddress="https://sites.google.com/site/thenesisgendes/game-boy-emulator";
	        String serverAddress="https://sites.google.com/site/thenesisgendes/game-boy-emulator";
	        serverAddressTextField=new TextField(serverAddress);
	        add(serverAddressTextField, c);
	        gridy++;
        }
        
        // Backups list.
        {
            // Label of the backups list.
            c.gridx=0; c.gridy=gridy; c.gridwidth=2; c.gridheight=1;
            c.weightx=1.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
            Label cartridgeBackupsLabel=new Label("Backups:");
            cartridgeBackupsLabel.setBackground(Color.LIGHT_GRAY);
            add(cartridgeBackupsLabel, c);
	        // The "Update" button allows to update the list of backups.
	        c.gridx=2; c.gridy=gridy; c.gridwidth=1; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        updateButton=new Button("Update");
	        updateButton.addActionListener(actionListener);
	        add(updateButton, c);
	        gridy++;
	        	        	        
	        // List of backups.
	        c.gridx=0; c.gridy=gridy; c.gridwidth=3; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        backupsList=new List(4, false);
			backupsList.addActionListener(itemActionListener);
			backupsList.addItemListener(itemListener);
	        add(backupsList, c);
	        gridy++;
        }

        // Status line.
        {	        
            c.gridx=0; c.gridy=gridy; c.gridwidth=1; c.gridheight=1;
            c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
            Label statusLabel=new Label("Status:");
            statusLabel.setBackground(Color.LIGHT_GRAY);
            add(statusLabel, c);
	        c.gridx=1; c.gridy=gridy; c.gridwidth=1; c.gridheight=1;
	        c.weightx=1.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        infosTextField=new TextField(infosMessage);
	        infosTextField.setEditable(false);
	        add(infosTextField, c);
	        // The "Cancel" button allows to cancel a pending update.
	        c.gridx=2; c.gridy=gridy; c.gridwidth=1; c.gridheight=1;
	        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
	        cancelButton=new Button("Cancel");
	        cancelButton.addActionListener(actionListener);
	        add(cancelButton, c);
	        gridy++;
        }
        
        updateMainButtons();
//        assert(appletFlag);
        // The frame is only used in application mode in order to contain the applet. 
        if (!appletFlag) {
            mainFrame=new Frame("Java Game Boy emulator");
            mainFrame.addWindowListener(windowAdapter);
            mainFrame.add(this);    		
            mainFrame.pack();
            mainFrame.setVisible(true);
        }
    }
    
    private void guiShutdown() {
        if (!appletFlag) {
	    	mainFrame.setVisible(false);
	    	mainFrame.dispose();
        }
    }

    private synchronized void updateMainButtons() {
    	boolean paused=systemContext.emulationThread.isPauseRequested();
    	boolean cartridgeInserted=gameBoyContext.gameBoy.isCartridgeInserted();
    	
		runButton.setEnabled(currentOperation!=OPERATION_DOWNLOADING_CARTRIDGE && paused);
		pauseButton.setEnabled(currentOperation!=OPERATION_DOWNLOADING_CARTRIDGE && !paused);
		resetButton.setEnabled(currentOperation!=OPERATION_DOWNLOADING_CARTRIDGE);

		cartridgeNameTextField.setText(cartridgeName);
		loadButton.setEnabled(currentOperation!=OPERATION_DOWNLOADING_CARTRIDGE && !appletFlag);
		downloadButton.setEnabled(currentOperation!=OPERATION_DOWNLOADING_CARTRIDGE && backupsList.getSelectedItem()!=null);
		removeButton.setEnabled(currentOperation!=OPERATION_DOWNLOADING_CARTRIDGE && cartridgeInserted);

		cancelButton.setEnabled(currentOperation!=OPERATION_NONE);
    }

    private synchronized void setInfosMessage(String message) {
    	infosMessage=message;
    	if (infosTextField!=null) {
    		infosTextField.setText(message);
    		infosTextField.repaint();
    	}
    }
    
    private void cancelCurrentOperation() {
    	switch (currentOperation) {
    	case OPERATION_NONE: break;
    	case OPERATION_DOWNLOADING_LIST:
    	case OPERATION_DOWNLOADING_CARTRIDGE: downloader.stop(); break;
    	}
    }
    
    //--------------------------------------------------------------------------------
    // Backup list.
    //--------------------------------------------------------------------------------
    // Load the manifest file from the server and update the backups list.
    private void downloadBackupList() {
    	if (currentOperation!=OPERATION_NONE) return;
    	currentOperation=OPERATION_DOWNLOADING_LIST;
		backupsList.removeAll();
		updateMainButtons();
    	setInfosMessage("Downloading backup list...");
		downloader.start(serverAddressTextField.getText()+"/backups.manifest", endOfBackupListDownloadListener);
    }
    
    private void downloadBackupListEnd(byte data[], int length, int lengthReceived, boolean stopped) {    	
    	if (stopped) {
        	setInfosMessage("Backup list download cancelled");    		
    	} else if (data==null || length!=lengthReceived) {
        	setInfosMessage("Cannot download backup list");
    	} else {
    		ByteArrayInputStream bais=new ByteArrayInputStream(data);
    		InputStreamReader isr=new InputStreamReader(bais);
    		BufferedReader br=new BufferedReader(isr);

        	try {
	    		while (true) {
	    			String line=br.readLine();
	    			if (line==null) break;
	    			line=line.trim();
	    			if (line=="") continue;
	    			backupsList.add(line);
	    		}
        	} catch (IOException e) {
//        		System.out.println(e.getMessage());
        	}

        	setInfosMessage("Backup list downloaded");
    	}
        currentOperation=OPERATION_NONE;
		updateMainButtons();
    }
    
    //--------------------------------------------------------------------------------
    // Cartridge loading and downloading.
    //--------------------------------------------------------------------------------
    private void removeCartridge() {
    	gameBoyContext.cartridgeRemove();
    	cartridgeName="";
    	setInfosMessage("Cartridge removed");
    	updateMainButtons();
    }

    private void loadCartridge(String fileName) {
    	if (currentOperation!=OPERATION_NONE) return;
    	
    	try {
    		gameBoyContext.cartridgeInsert(fileName);
        	setInfosMessage("Cartridge loaded: "+fileName);
        	cartridgeName=Utilities.getFileName(fileName);
    	} catch (IOException e) {
        	setInfosMessage("Cannot load cartridge: "+fileName);
        	cartridgeName="";    		
    	}
    	
		updateMainButtons();
    }
        
    private void downloadCartridge(String name) {
    	if (currentOperation!=OPERATION_NONE) return;
    	currentOperation=OPERATION_DOWNLOADING_CARTRIDGE;    	
    	gameBoyContext.cartridgeRemove();
    	cartridgeName=name;
		updateMainButtons();
    	setInfosMessage("Downloading cartridge...");
		downloader.start(serverAddressTextField.getText()+"/"+name, endOfCartridgeDownloadListener);
    }
    
    private void downloadCartridgeEnd(byte data[], int length, int lengthReceived, boolean stopped) {
    	if (stopped) {
        	setInfosMessage("Cartridge download cancelled: "+cartridgeName);
        	cartridgeName="";
    	} else if (data==null || length!=lengthReceived) {
        	setInfosMessage("Cannot download cartridge: "+cartridgeName);
        	cartridgeName="";
    	} else {
    		try {
        		ByteArrayInputStream bais=new ByteArrayInputStream(data);
    			gameBoyContext.cartridgeInsert(bais, cartridgeName);
            	setInfosMessage("Cartridge downloaded: "+cartridgeName);
    		} catch (IOException e) {
            	setInfosMessage("Cannot download cartridge: "+cartridgeName);
            	cartridgeName="";    			
    		}
    	}
        currentOperation=OPERATION_NONE;
		updateMainButtons();
    }
    
    private void chooseCartridge() {
        LocalFileLoader fileLoader=new LocalFileLoader();
        fileLoader.show();
        String fileName=fileLoader.getFileName();
        if (fileName!=null) loadCartridge(fileLoader.getPathName());
    }
    
    // A local file loader (used in application mode).
    private static class LocalFileLoader {
        private Frame fakeFrame = new Frame();
        private FileDialog fileDialog;

        public LocalFileLoader() {
            fileDialog=new FileDialog(fakeFrame, "Choose a cartridge backup file", FileDialog.LOAD);
        }

        public void show() {
            fileDialog.setVisible(true);
        }

        public String getFileName() {
            return fileDialog.getFile();
        }
        
        public String getPathName() {
        	return fileDialog.getDirectory()+fileDialog.getFile();
        }
    }
    
    private void loadCartridgeBackup() {
    	if (!appletFlag) {
    		try {
    			gameBoyContext.cartridgeLoadBackup();
    		} catch (IOException e) {
    		}
    	}
    }
    
    private void saveCartridgeBackup() {
    	if (!appletFlag) {
    		try {
    			gameBoyContext.cartridgeSaveBackup();    	
    		} catch (IOException e) {
    		}
    	}
    }
        
    //--------------------------------------------------------------------------------
    // Listeners.
    //--------------------------------------------------------------------------------    
    private HttpFileDownloader.EndOfDownloadListener endOfBackupListDownloadListener=new HttpFileDownloader.EndOfDownloadListener() {
		public void onEndOfDownload(byte data[], int length, int lengthReceived, boolean stopped) {
			downloadBackupListEnd(data, length, lengthReceived, stopped);
		}
    };
    
    private HttpFileDownloader.EndOfDownloadListener endOfCartridgeDownloadListener=new HttpFileDownloader.EndOfDownloadListener() {
		public void onEndOfDownload(byte data[], int length, int lengthReceived, boolean stopped) {
			downloadCartridgeEnd(data, length, lengthReceived, stopped);
		}
    };
    
    private KeyEventDispatcher keyEventDispatcher=new KeyEventDispatcher() {
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID()!=KeyEvent.KEY_PRESSED) return false;
            
            int key=e.getKeyCode();
    
            switch (key) {
            case KeyEvent.VK_F1:
                gameBoyContext.reset();
                return true;
            case KeyEvent.VK_F8:
                if (systemContext.emulationThread.isPaused()) systemContext.emulationThread.resume();
                else systemContext.emulationThread.pause();
                updateMainButtons();
                return true;
            }
            
            return false;
        }    
    };
    
    private WindowAdapter windowAdapter=new WindowAdapter() {
        public void windowClosing(WindowEvent e) { shutdown(); }    
    };
    
	private FocusAdapter focusAdapter=new FocusAdapter() {
        public void focusGained(FocusEvent arg0) { videoCanvas.requestFocus(); }
        public void focusLost(FocusEvent arg0) {}
    };
	
    private ActionListener actionListener=new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		String command=e.getActionCommand();
            if (command.equals("Load")) {
            	if (!appletFlag) chooseCartridge();
            } else if (command.equals("Remove")) {
            	removeCartridge();
            } else if (command.equals("Update")) {
            	downloadBackupList();
            } else if (command.equals("Download")) {
            	String item=backupsList.getSelectedItem();
            	if (item!=null) downloadCartridge(item);
            } else if (command.equals("Run")) {
            	systemContext.emulationThread.resume();
            	updateMainButtons();
            } else if (command.equals("Pause")) {
            	systemContext.emulationThread.pause();
            	updateMainButtons();
            } else if (command.equals("Reset")) {
            	gameBoyContext.reset();
	        } else if (command.equals("Cancel")) {
	        	cancelCurrentOperation();
	        }
    	}
    };
    
    private ActionListener itemActionListener=new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		String command=e.getActionCommand();
    		downloadCartridge(command);
    	}    	
    };
    
    private ItemListener itemListener=new ItemListener() {
    	public void itemStateChanged(ItemEvent e) {
			downloadButton.setEnabled(backupsList.getSelectedItem()!=null);
    	}
    };    
}
//******************************************************************************
