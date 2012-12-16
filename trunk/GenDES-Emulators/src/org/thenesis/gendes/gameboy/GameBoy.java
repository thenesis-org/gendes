package org.thenesis.gendes.gameboy;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.thenesis.gendes.EmulationClock;
import org.thenesis.gendes.EmulationDevice;
import org.thenesis.gendes.EmulationEvent;
import org.thenesis.gendes.EmulationState;
import org.thenesis.gendes.TimeStamp;
import org.thenesis.gendes.cpu_gameboy.CPU;


//import static org.thenesis.emulation.gameboy.CartridgeInfos.*;
//import static org.thenesis.emulation.gameboy.CPUDecoderBuilder.*;
//import static org.thenesis.emulation.gameboy.HWRDecoderBuilder.*;


/**
 * Main Game Boy emulator class.
 * TODO: use a common event for CPU and video. 
 */
public final class GameBoy extends EmulationDevice {
    /**
     * Constructor.
     */
    public GameBoy() {
        super("GameBoy");
	    globalInitialize();
	}

    protected void register() {}
    
    protected void unregister() {}
    
    protected void saveState(EmulationState s) {    
    }
    
    protected void restoreState(EmulationState s) {    
    }
    
    //--------------------------------------------------------------------------------
    // Debugging.
    //--------------------------------------------------------------------------------
    /**
     * Address spaces
     */
    public static final int
    	ADDRESS_SPACE_CPU=				0, // CPU.
    	ADDRESS_SPACE_WRAM=             1, // Work ram.
    	ADDRESS_SPACE_VRAM=             2, // Video RAM.
    	ADDRESS_SPACE_CROM=             3, // Cartridge ROM.
    	ADDRESS_SPACE_CRAM=		        4; // Cartridge RAM.

    /**
     * Debug read.
     * Do not call asynchronously.
     */
    public int debugRead8(int addressSpace, int address) {
        int data=0;
        globalDebugAccess=true;
        
        switch (addressSpace) {
        case ADDRESS_SPACE_CPU: data=cpu.cpuRead8(address); break;
        case ADDRESS_SPACE_WRAM: break;
        case ADDRESS_SPACE_VRAM: break;
        case ADDRESS_SPACE_CROM: break;
        case ADDRESS_SPACE_CRAM: break;
        }
        
        globalDebugAccess=false;
        return data;
    }
    
    /**
     * Debug write.
     * Do not call asynchronously.
     */
    public void debugWrite8(int addressSpace, int address, int data) {
        globalDebugAccess=true;
        clockCurrentCycle=clock.timeToCycle(system.getCurrentTime());
        
        switch (addressSpace) {
        case ADDRESS_SPACE_CPU: cpu.cpuWrite8(address, data); break;
        case ADDRESS_SPACE_WRAM: break;
        case ADDRESS_SPACE_VRAM: break;
        case ADDRESS_SPACE_CROM: break;
        case ADDRESS_SPACE_CRAM: break;
        }
        
        globalDebugAccess=false;
    }

    //--------------------------------------------------------------------------------
    // Global.
    //--------------------------------------------------------------------------------
    /**
     * Game Boy models.
     */
    public static final int
    	MODEL_GAME_BOY=				0,
    	MODEL_GAME_BOY_POCKET=		1,
    	MODEL_SUPER_GAME_BOY=		2,
    	MODEL_SUPER_GAME_BOY_2=		3,
    	MODEL_GAME_BOY_COLOR=		4;
    
    // Game Boy model.
    protected int globalGameBoyModel;
    // Whether Game Boy color features are activated.
    protected boolean globalColorModeEnabled;
    // Whether Super Game Boy features are activated.
    protected boolean globalSuperModeEnabled;
    // Global state.
    protected boolean globalPowered=false;

    // Listener.
    private GameBoyListener globalListener;
    private GameBoyEvent globalEvent;
    
    // Debug access.
    private boolean globalDebugAccess=false;

    /**
     * Set the listener.
     * Do not call asynchronously.
     * @param l a listener for GameBoy device events
     * @param e an event send as a parameter when a method of the listener is called 
     */
    public void setListener(GameBoyListener l, GameBoyEvent e) {
        globalListener=l;
        globalEvent=e;
    }
    
	/**
     * Set the model of Game Boy.
     * The GameBoy must be switched off before calling this function otherwise it has no effect.
     * Do not call asynchronously.
     * @param model the model of Game Boy.
     */
	public void setModel(int model) {
	    if (globalPowered) return;
	    globalGameBoyModel=model;
        hwrAddressDecoderRebuildFlag=true;
	}

	/**
	 * Get the model of Game Boy.
	 * @return the model of Game Boy
	 */
	public int getModel() {
	    return globalGameBoyModel;
	}

	/**
     * Switch the power.
     * This method has no effect if you try to switch on while the power is already on or
     * if you try to switch off while the power is already off. 
     * Do not call asynchronously.
     * @param on <code>false</code> to switch power off, <code>true</code> to switch power on
     */
	public void switchPower(boolean on) {
	    if (on && !globalPowered) {
            if (cpuAreasDecoderRebuildFlag) {
                buildCpuDecoder(cpuAreasDecoderR, cpuAreasDecoderW, cartridgeController, cartridgeFeatures);
                cpuAreasDecoderRebuildFlag=false;
            }
		    if (hwrAddressDecoderRebuildFlag) {
                buildHWRDecoder(hwrAddressDecoder, globalGameBoyModel);
                hwrAddressDecoderRebuildFlag=false;
            }

	        globalPowered=true;
	        reset();
	    } else if (!on && globalPowered) { 
	        globalPowered=false;
            removeAllEvents();
	    }
	}
	
	/**
	 * Give the state of the power switch.
	 * @return <code>false</code> if power is off, <code>true</code> if power is on
	 */
	public boolean isPowered() {
	    return globalPowered;
	}
	
	/**
     * Reset.
     * Do not call asynchronously.
     */
	public void reset() {
	    if (!globalPowered) return;
        clockCurrentCycle=clock.timeToCycle(system.getCurrentTime());
        globalReset();
	}
    
    // Initialize the emulator. Must be done before any other call.
    private void globalInitialize() {
        addChildDevice(clock);

        globalGameBoyModel=MODEL_GAME_BOY;
        globalPowered=false;
        
        cartridgeInitialize();
        
        padKeys=0x00;
    
        audioInitialize();
        
        // Default colors for the Game Boy mode. 
        for (int i=0; i<4; i++) {
            short t=(short)videoColor32To15(DEFAULT_PALETTE[i]);
            videoBPColors[i]=t;
            videoOPColors[0][i]=t; videoOPColors[1][i]=t;
        }
        videoOutputEnabled=true;

        cpuAreasDecoderRebuildFlag=true;
        hwrAddressDecoderRebuildFlag=true;
    }
    
    // Global reset of the Game Boy.
    private void globalReset() {
        // Global.
        globalColorModeEnabled=false;
        
        // Internal ROM.
        internalROMDisabled=false;

        // Work RAM.
        workRamBank=1;
        for (int r=0; r<0x8000; r++) workRam[r]=0;
        
        // You must think a little before changing the order of these calls.
        for (int i=0; i<0x0100; i++) hwRegisters[i]=0; // Hardware registers.
        cartridgeReset();
        clockReset();
        timerReset();
        padReset();
        serialReset();
        infraredReset();
        odmaReset();
        hdmaReset();
        videoReset();
        audioReset();
        interruptsReset();
        cpuReset();
        internalRomReset();        
    }

	//--------------------------------------------------------------------------------
    // Clock.
	//--------------------------------------------------------------------------------    
	// Whether we are in double speed mode.
	protected byte clockDoubleSpeedFlag; // KEY1[7].
	protected byte clockDoubleSpeedSwitchFlag; // KEY1[0].
	// Give the clock frequency for a given model.
    public static final int CLOCK_FREQUENCY_DMG=4194304, CLOCK_FREQUENCY_SGB=4295454;
    protected static final int CLOCK_FREQUENCY_FOR_MODEL[]={
        CLOCK_FREQUENCY_DMG, CLOCK_FREQUENCY_DMG, CLOCK_FREQUENCY_SGB, CLOCK_FREQUENCY_SGB, CLOCK_FREQUENCY_DMG
    };
	// Current clock.
    protected int clockFrequency;
    protected final EmulationClock clock=new EmulationClock("GameBoy clock");
    protected long clockCurrentCycle;

    // Event.
    // We need a specific event because the switch generates changes that cannot be done while the cpu is running.
    private final EmulationEvent clockSwitchEvent=new EmulationEvent(this, "Clock switch") {
        public void processEvent() {
            audioUpdate();
            clockDoubleSpeedFlag^=1;
            clockDoubleSpeedSwitchFlag=0;
            clockFrequency=CLOCK_FREQUENCY_FOR_MODEL[globalGameBoyModel];
            if (clockDoubleSpeedFlag!=0) clockFrequency<<=1;
            this.clock.setFrequency(clockFrequency);
            audioScheduleEvent();
        }
        
        public void breakEvent() {}
    };
    
    // Reset the clock.
    private void clockReset() {
        system.removeEvent(clockSwitchEvent);
        clockDoubleSpeedFlag=0;
        clockDoubleSpeedSwitchFlag=0;
        clockFrequency=CLOCK_FREQUENCY_FOR_MODEL[globalGameBoyModel];
        clock.setFrequency(clockFrequency);
    }
    
    // Switch the speed of a Game Boy Color.
    private void clockSwitchSpeed() {
        cpuSetCurrentCycle();
        clockSwitchEvent.clockCycle=clockCurrentCycle; clock.addEvent(clockSwitchEvent);
    }
    
	//--------------------------------------------------------------------------------
    // Internal ROM.
	//--------------------------------------------------------------------------------
	// Whether the internal ROM is disabled.
    protected boolean internalROMLoaded=false, internalROMDisabled;
	// Internal ROM data.
    protected final byte internalROM[]=new byte[256];

    // Used to draw "Nintendo". 
    private static final short INTERNAL_ROM_NINTENDO_LOGO[]={
        0xCE, 0xED, 0x66, 0x66, 0xCC, 0x0D, 0x00, 0x0B, 0x03, 0x73, 0x00, 0x83, 0x00, 0x0C, 0x00, 0x0D, 
        0x00, 0x08, 0x11, 0x1F, 0x88, 0x89, 0x00, 0x0E, 0xDC, 0xCC, 0x6E, 0xE6, 0xDD, 0xDD, 0xD9, 0x99,
        0xBB, 0xBB, 0x67, 0x63, 0x6E, 0x0E, 0xEC, 0xCC, 0xDD, 0xDC, 0x99, 0x9F, 0xBB, 0xB9, 0x33, 0x3E 
    };
    
    // Used to draw the '(r)' symbol next to "Nintendo".
    private static final short INTERNAL_ROM_NINTENDO_LOGO_2[]={
        0x3C, 0x42, 0xB9, 0xA5, 0xB9, 0xA5, 0x42, 0x3C
    };
    
    // Setup the Game Boy like the ROM would do.
    private void internalRomReset() {
        // If we don't have a ROM, we will use some default values.
    	if (internalROMLoaded) return;
    	        
        globalSuperModeEnabled=((globalGameBoyModel==MODEL_SUPER_GAME_BOY) || (globalGameBoyModel==MODEL_SUPER_GAME_BOY_2)) && cartridgeSuperFeaturesFlag;
        globalColorModeEnabled=(globalGameBoyModel==MODEL_GAME_BOY_COLOR) && cartridgeColorFeaturesFlag;
        
        // Initialize CPU registers.
        cpu.rPC=cartridgeInserted ? 0x0100 : 0x00fa;
        cpu.rSP=0xFFFE;
        switch (globalGameBoyModel) {
        case GameBoy.MODEL_GAME_BOY: cpu.rA=0x01; break;
        case GameBoy.MODEL_GAME_BOY_POCKET: cpu.rA=0xff; break;
        case GameBoy.MODEL_SUPER_GAME_BOY: cpu.rA=0x01; break;
        case GameBoy.MODEL_SUPER_GAME_BOY_2: cpu.rA=0xff; break;
        case GameBoy.MODEL_GAME_BOY_COLOR: cpu.rA=0x11; break;
        }
        cpu.rB=0x00;
        cpu.rC=0x13;
        cpu.rD=0x00;
        cpu.rE=0xd8;
        cpu.rF=0xb0;
        cpu.rHL=0x014d;
        
        // Initialize internal ROM.
        for (int i=0; i<256; i++) internalROM[i]=0x00;
        internalROM[0x00fa]=0x18; internalROM[0x00fb]=-2; // Lockup: jr $fe (jump in place).
        internalROMDisabled=cartridgeInserted;
        
        // Initialize audio.
        if ((globalGameBoyModel!=MODEL_SUPER_GAME_BOY) && (globalGameBoyModel!=MODEL_SUPER_GAME_BOY_2)) {
            audioGlobalEnableFlag=1;
            audioChannelToSOFlags=(byte)0xf3;
            audioSO1OutputLevel=7; audioSO2OutputLevel=7;
            audioCh1Enabled=1;
            audioCh1LengthUsed=0; audioCh1Length=0;
            audioCh1FrequencyInitialValue=0x7c1;
            audioCh1Duty=2;
            audioCh1EnvelopeVolumeInitialValue=15;
            audioCh1EnvelopeSweepsDirection=0; audioCh1EnvelopeSweepsStepLength=3;
        }
        
        // Initialize video.
        {
            if (!globalColorModeEnabled) {
                videoSetBackgroundPalette(0xfc);
                videoSetObjectPalette(0xff, 0);
                videoSetObjectPalette(0xff, 1);
            }
         
            // Set nintendo logo data.
            {
                // Write the "Nintendo" part.
                int address=0x0010;
                for (int i=0; i<48; i++) {
                    int din=INTERNAL_ROM_NINTENDO_LOGO[i], dout=0;
                    for (int j=0; j<2; j++) {
                        // Double horizontally.
                        for (int k=0; k<4; k++) {
                            int bit=(din>>7)&1;
                            dout=(dout<<1)|bit;
                            dout=(dout<<1)|bit;
                            din<<=1;
                        }
                        // Double vertically. So a 4-bit nibble represents 2 lines of a tile.
                        videoRam[address]=(byte)dout; address+=2;
                        videoRam[address]=(byte)dout; address+=2;
                    }
                }
                
                // Write the '(r)' symbol part.
                for (int i=0; i<8; i++) {
                    int dout=INTERNAL_ROM_NINTENDO_LOGO_2[i];
                    videoRam[address]=(byte)dout; address+=2; 
                }
            }
            
            // Setup tile map.
            {
                videoRam[0x1910]=0x19; // (r) logo.
                int address=0x1904, tile=1;
                for (int y=0; y<2; y++, address+=0x20) {
                    for (int x=0; x<12; x++) videoRam[address+x]=(byte)tile++;
                }
            }
            
            hwrWrite(0xff42, 0x00); // Set vertical scroll register.
            hwrWrite(0xff40, 0x91); // Enable display.
        }
    }
    
	//--------------------------------------------------------------------------------
	// Work RAM.
	//--------------------------------------------------------------------------------
    protected byte workRamBank; // SVBK[0-2].
    protected final byte workRam[]=new byte[0x8000];

	//--------------------------------------------------------------------------------
	// Timer.
	//--------------------------------------------------------------------------------
    protected short timerDivider; // DIV.
    protected byte timerStartedFlag, timerClockSelect; // TAC[2], TAC[0-1].
    protected short timerCounter, timerReloadValue; // TIMA, TMA.

	// Last time that the timer registers have been updated.
	private long timerLastUpdateTime;
    // Cycles not counted for the divider and the counter.
    private int timerDividerCyclesRemainder, timerCounterCyclesRemainder;
	// Power of 2 of the number of clock cycles between 2 values of the timer counter.
	private static final int TIMER_INPUT_CLOCK_SHIFT[]={ 10, 4, 6, 8 };

    // Timer event.
	private final EmulationEvent timerOverflowEvent=new EmulationEvent(this, "Timer") {
        public final void processEvent() { clockCurrentCycle=clockCycle; timerUpdate(); }
        public final void breakEvent() {};
	};

	private void timerReset() {
        timerDivider=0;
        timerStartedFlag=0; timerClockSelect=0; timerCounter=0; timerReloadValue=0;
    	timerLastUpdateTime=clockCurrentCycle;
        timerDividerCyclesRemainder=0; timerCounterCyclesRemainder=0;
        timerScheduleEvent();
	}
	
    private void timerSetTAC(int data) {
        timerStartedFlag=(byte)((data>>2)&0x01);
        timerClockSelect=(byte)(data&0x03);
        timerDividerCyclesRemainder=0; // Not sure of this.
        if (timerStartedFlag!=0) timerScheduleEvent();                
        else system.removeEvent(timerOverflowEvent);
    }    
    
	private void timerUpdate() {
        int d=(int)TimeStamp.add(clockCurrentCycle, -timerLastUpdateTime), c;
        
        // Update the divider (which is a 16 bit counter but only upper 8 bits are visible).
	    timerLastUpdateTime=clockCurrentCycle;
        c=timerDividerCyclesRemainder+d; timerDividerCyclesRemainder=c&0xff;
        timerDivider=(short)((timerDivider+(c>>8))&0xff);
        
        // Update the timer.
	    if (timerStartedFlag!=0) {
            int shift=TIMER_INPUT_CLOCK_SHIFT[timerClockSelect], mask=(1<<shift)-1;
            c=timerCounterCyclesRemainder+d; timerCounterCyclesRemainder=c&mask;
	        c=timerCounter+(c>>shift);
	        if (c>0xff) {
	    	    timerCounter=(short)((timerReloadValue+(c-0x100))&0xff);
                timerScheduleEvent();
                interruptsTrigger(INTERRUPT_TIMER);
	        } else timerCounter=(short)c;
	    }
	}

	private void timerScheduleEvent() {
        int t=(0x100-timerCounter)<<TIMER_INPUT_CLOCK_SHIFT[timerClockSelect];
        timerOverflowEvent.clockCycle=(timerStartedFlag!=0) ? TimeStamp.add(timerLastUpdateTime, t) : CLOCK_FREQUENCY_DMG; // Always keep alive.
	    clock.addEvent(timerOverflowEvent);
	}
	
	//--------------------------------------------------------------------------------
    // Pad keys.
	//--------------------------------------------------------------------------------
    /**
     * Pad keys.
     */
    public static final int
    	PAD_RIGHT=		0x00000001,
    	PAD_LEFT=		0x00000002,
    	PAD_UP=			0x00000004,
    	PAD_DOWN=		0x00000008,
    	PAD_A=			0x00000010,
    	PAD_B=			0x00000020,
    	PAD_SELECT=		0x00000040,
    	PAD_START=		0x00000080,
    	PAD_ALL=		0x000000ff;
    
	protected byte padKeys;
	protected byte padP1; // P1 (0xff00).

	/**
     * Set the pad keys.
     * Do not call asynchronously.
     */
	public void setKeys(int keys) {
        padRefresh(padP1, keys);
    }

	/**
	 * Get the pad keys.
	 * @return the keys status
	 */
	public int getKeys() {
	    return padKeys;
	}

    private void padReset() {
        padP1=0x3f;
    }
    
    private void padRefresh(int p1, int keys) {
        p1&=0x30;
        if ((p1&0x10)==0) p1|=keys&0x0f;
        if ((p1&0x20)==0) p1|=(keys>>4)&0x0f;
        p1^=0x0f;
        if ((padP1&~p1&0x0f)!=0) interruptsTrigger(INTERRUPT_PAD);
        padP1=(byte)p1;
        padKeys=(byte)keys;
    }
    
	//--------------------------------------------------------------------------------
	// Serial communication.
	//--------------------------------------------------------------------------------
    protected static final short
    	SERIAL_START=         0x80, 
    	SERIAL_CLOCK_SPEED=   0x02, 
    	SERIAL_CLOCK=         0x01; 
    protected byte serialData; // SB (0xff01).
    protected byte serialControl; // SC (0xff02).

    // Serial event.
    private final EmulationEvent serialEvent=new EmulationEvent(this, "Serial") {
        public final void processEvent() {
            // TODO: implement serial transfer.
            serialData=(byte)0xff; // When not connected.
            serialControl&=~SERIAL_START;
            interruptsTrigger(INTERRUPT_SERIAL);
        }      
        public final void breakEvent() {}
    };
    
    private void serialReset() {
        system.removeEvent(serialEvent);
        serialData=0x00; serialControl=0x01;
    }
    
    private void serialSetSC(int data) {
        serialControl=(byte)(data&0x83);
        if ((data&SERIAL_START)!=0) {
            int t=0;
            if ((serialControl&SERIAL_CLOCK)!=0) {
                if (globalColorModeEnabled) {
                    t=((serialControl&SERIAL_CLOCK_SPEED)!=0) ? CLOCK_FREQUENCY_DMG/262144 : CLOCK_FREQUENCY_DMG/8192;
                } else t=CLOCK_FREQUENCY_DMG/8192;
                serialEvent.clockCycle=TimeStamp.add(clockCurrentCycle, t);
                clock.addEvent(serialEvent);
            } else {
                // TODO: implement external clock.
            }
        }
    }
    
	//--------------------------------------------------------------------------------
    // Infrared port.
	//--------------------------------------------------------------------------------
    protected byte infraredOutput; // RP[0].
    protected byte infraredInput; // RP[1]
    protected byte infraredEnableFlag; // RP[6-7].

    private void infraredReset() {
        infraredEnableFlag=0;
        infraredOutput=0;
        infraredInput=0;        
    }
    
    private void infraredSetRP(int data) {
        // TODO: implement infrared transfer.
        infraredEnableFlag=(byte)((data>>6)&0x03);
        infraredInput=(byte)((data>>1)&0x01);
        infraredOutput=(byte)(data&0x01);        
    }
    
	//--------------------------------------------------------------------------------
	// Cartridge.
	//-------------------------------------------------------------------------------- 
    // MBC1 memory models.
    protected static final int CARTRIDGE_MBC1_16x8=0, CARTRIDGE_MBC1_4x32=1;

    protected boolean cartridgeInserted;
    protected final CartridgeInfos cartridgeInfos=new CartridgeInfos();
    
    protected boolean cartridgeSuperFeaturesFlag; // .
    protected boolean cartridgeColorFeaturesFlag; // .
    protected boolean cartridgeColorOnlyFlag; // .
    
    protected int cartridgeController; // Controller used by the cartridge.
    protected int cartridgeFeatures; // Features of the cartridge.
    protected int cartridgeMbc1MemoryModel; // Indicates, for MBC1 only, which memory model is currently active.
    
    protected int cartridgeNbRomBanks; // Number of 16KB ROM banks.
    protected int cartridgeRomSize; // Size of the ROM.
    protected int cartridgeRomBank; // Current ROM bank mapped to 0x4000-0x7fff.
    protected int cartridgeRealRomBank; // Real current ROM bank mapped to 0x4000-0x7fff.
    protected byte[] cartridgeRom; // Cartridge ROM.
    
    protected int cartridgeNbRamBanks; // Number of 8KB RAM banks.
    protected int cartridgeRamSize; // Size of the RAM.
    protected int cartridgeRamBank; // Current RAM bank mapped to 0xa000-0xbfff. 
    protected int cartridgeRealRamBank; // Real current RAM bank mapped to 0xa000-0xbfff. 
    protected boolean cartridgeRamEnabled; // True if the RAM is currently enabled.
    protected byte[] cartridgeRam; // Cartridge RAM.
    
	/**
     * Sets the cartridge.
     * Do not call asynchronously.
     * @param inputStream the input stream used to read data
     * @return <code>false</code> if the cartridge has been correctly inserted, <code>true</code> otherwise.
     */
	public boolean setCartridge(InputStream inputStream) throws IOException {
	    if (globalPowered) return true;
        cartridgeInitialize();
        cpuAreasDecoderRebuildFlag=true;
        
        try {            
            // Buffer for the cartridge header.
            byte[] header=new byte[CartridgeInfos.CARTRIDGE_HEADER_SIZE];
    
            // Read the cartridge header.
            int total=CartridgeInfos.CARTRIDGE_HEADER_SIZE;
            do {
            	int l=inputStream.read(header, CartridgeInfos.CARTRIDGE_HEADER_SIZE-total, total);
            	if (l<0) throw new IOException();
                total-=l;
            } while (total>0);
            
            if (cartridgeInfos.getInfos(header)) throw new IOException();

            cartridgeSuperFeaturesFlag=cartridgeInfos.superFeaturesFlag;
            cartridgeColorFeaturesFlag=cartridgeInfos.colorFeaturesFlag;
            cartridgeColorOnlyFlag=cartridgeInfos.colorOnlyFlag;
            cartridgeController=cartridgeInfos.controller;
            cartridgeFeatures=cartridgeInfos.features;
            cartridgeNbRomBanks=cartridgeInfos.nbRomBanks;
            cartridgeRomSize=cartridgeInfos.romSize;
            cartridgeNbRamBanks=cartridgeInfos.nbRamBanks;
            cartridgeRamSize=cartridgeInfos.ramSize;
            
            // Allocate buffers for ROM and RAM.
            cartridgeRom=(cartridgeRomSize>0) ? new byte[cartridgeRomSize] : null;
            cartridgeRam=(cartridgeRamSize>0) ? new byte[cartridgeRamSize] : null;
            
            // Copy header in the rom buffer.
            System.arraycopy(header, 0, cartridgeRom, 0, CartridgeInfos.CARTRIDGE_HEADER_SIZE);
            total=cartridgeRomSize-CartridgeInfos.CARTRIDGE_HEADER_SIZE;
            do {
            	int l=inputStream.read(cartridgeRom, cartridgeRomSize-total, total);
            	if (l<0) throw new IOException();
                total-=l;
            } while (total>0);

            cartridgeInserted=true;            
        } catch (IOException e) {
            cartridgeInitialize();
            throw e;
        }

        return false;
    }

    /**
     * Removes the current cartridge.
     * Do not call asynchronously.
     */
    public void removeCartridge() {
        if (globalPowered) return;
        cartridgeInitialize();
        cpuAreasDecoderRebuildFlag=true;
    }
    
    /**
     * Returns true if a cartridge is inserted.
     * @return <code>false</code> if no cartridge is inserted, <code>true</code> otherwise
     */
    public boolean isCartridgeInserted() {
        return cartridgeInserted;
    }

    /**
     * Returns the cartridge infos.
     * @return a CartridgeInfos object
     */
    public CartridgeInfos getCartridgeInfos() {
        return cartridgeInfos;
    }
    
    /**
     * Checks the cartridge checksum. Return false if there is an error.
     * @return <code>false</code> if the cartridge checksum is correct, <code>true</code> otherwise
     */
    public boolean checkCartridgeChecksum() {
        if (cartridgeRom!=null) return CartridgeInfos.verifyChecksum(cartridgeRom);
        else return false;
    }
    
    /**
     * Checks if the cartridge has backup memory.
     * @return <code>true</code> if the cartridge has backup memory, <code>false</code> otherwise.
     */
    public boolean hasCartridgeBackup() {
    	return (cartridgeFeatures&CartridgeInfos.CTF_BATTERY)!=0 && cartridgeRam!=null;
    }
    
    /**
     * Loads the memory backup of the current cartridge.
     * Do not call asynchronously.
     * @return <code>false</code> if the cartridge backup has been effectively loaded, <code>true</code> otherwise
     */
    public boolean loadCartridgeBackup(InputStream inputStream) throws IOException {
        if ((cartridgeFeatures&CartridgeInfos.CTF_BATTERY)==0 || cartridgeRam==null) return true;
        int total=cartridgeRamSize;
        do {
            total-=inputStream.read(cartridgeRam, cartridgeRamSize-total, total);
        } while (total>0);
        return false;        
    }
    
    /**
     * Saves the memory backup of the current cartridge.
     * Do not call asynchronously.
     * @return <code>false</code> if the cartridge backup has been effectively saved, <code>true</code> otherwise
     */
    public boolean saveCartridgeBackup(OutputStream outputStream) throws IOException {
        if ((cartridgeFeatures&CartridgeInfos.CTF_BATTERY)==0 || cartridgeRam==null) return true;
        outputStream.write(cartridgeRam);
        return false;        
    }

    private void cartridgeInitialize() {
        cartridgeInserted=false;
        cartridgeInfos.resetInfos();
        
        cartridgeSuperFeaturesFlag=false;
        cartridgeColorFeaturesFlag=false;
        cartridgeColorOnlyFlag=false;

        cartridgeController=CartridgeInfos.CTC_NONE;
        cartridgeFeatures=0;
        
        cartridgeNbRomBanks=0;
        cartridgeRomSize=0;
        cartridgeRom=null;
        
        cartridgeNbRamBanks=0;
        cartridgeRamSize=0;
        cartridgeRam=null;
    }
        
    private void cartridgeReset() {
        cartridgeMbc1MemoryModel=CARTRIDGE_MBC1_16x8;
        cartridgeRomBank=1; cartridgeRealRomBank=1;
        cartridgeRamBank=0; cartridgeRealRamBank=0;
        cartridgeRamEnabled=false;
    }

	//--------------------------------------------------------------------------------
    // Sound.
    // Note: it seems that there is a global clock divider for all circuits.
	//--------------------------------------------------------------------------------
    // Audio output generation.
    private long audioLastUpdateCycle;
    private int audioSampleCyclesRemainder, audioSampleCycles;
    
    // Output audio buffer.
    public static final int AUDIO_MIN_FREQUENCY=64, AUDIO_MAX_FREQUENCY=65536;
    protected boolean audioOutputEnabled=true;
    protected int audioOutputFrequency=44100;
    protected int audioOutputBufferLength, audioOutputBufferOffset, audioOutputBufferPosition;
    protected byte[] audioOutputBuffer;

    protected static final int AUDIO_MAX_CYCLES_STEP=                 1<<14;
    protected static final int AUDIO_FREQUENCY_SWEEP_STEP_CYCLES=     1<<(22-7);
    protected static final int AUDIO_LENGTH_STEP_CYCLES=              1<<(22-8);
    protected static final int AUDIO_ENVELOPE_SWEEP_STEP_CYCLES=      1<<(22-6);
    
    protected static final byte AUDIO_DUTY[]={ 1, 2, 4, 6 };
    protected static final byte AUDIO_WAVES[][]={
    	{ 0, 0, 1, 0, 0, 0, 0, 0 },
    	{ 0, 1, 1, 0, 0, 0, 0, 0 },
    	{ 0, 1, 1, 1, 1, 0, 0, 0 },
    	{ 1, 0, 0, 1, 1, 1, 1, 1 },
    };
    
    
    //
    // All channels.
    //
    
    protected byte audioGlobalEnableFlag; // NR52[7].
    protected byte audioChannelToSOFlags; // NR51.
    protected byte audioSO1OutputLevel; // Right channel output level (NR50[0-2]).
    protected byte audioSO1VinFlag; // NR50[3].
    protected byte audioSO2OutputLevel; // Left channel output level (NR50[4-6]).
    protected byte audioSO2VinFlag; // NR50[7].
    protected final short audioWavesSum[][]=new short[4][8+1];
    
    //
    // Channel 1
    //
    
    private int audioCh1Sample;
    private int audioCh1NextUpdateCycles, audioCh1NextUpdateCyclesCounter;
    
    protected byte audioCh1Enabled;
    
    protected byte audioCh1LengthUsed; // NR14[6].
    protected byte audioCh1Length; // NR11[0-3].
    private int audioCh1LengthCycles; // Internal.
    
    private int audioCh1Divider4Counter; // Counter of the divider by 4 (internal).
    protected short audioCh1FrequencyInitialValue; // Initial frequency (NR13[0-7] and NR14[0-2]).
    private int audioCh1Frequency, audioCh1FrequencyCounter; // Current frequency (internal).
    protected byte audioCh1FrequencySweepsNumber, audioCh1FrequencySweepsDirection, audioCh1FrequencySweepsDuration; // NR10[0-2], NR10[3], NR10[4-6].
    private int audioCh1FrequencySweepsStepCycles; // Internal.
        
    protected byte audioCh1Duty; // NR11[6-7].
    private int audioCh1DutyCounter;
    private int audioCh1WaveLastValue;
    
    protected byte audioCh1EnvelopeSweepsStepLength, audioCh1EnvelopeSweepsDirection, audioCh1EnvelopeVolumeInitialValue; // NR12[0-2], NR12[3], NR12[4-7].
    private int audioCh1EnvelopeSweepsStepCycles, audioCh1EnvelopeVolume; // Internal.
    
    //
    // Channel 2.
    //
    
    private int audioCh2Sample;
    private int audioCh2NextUpdateCycles, audioCh2NextUpdateCyclesCounter;
        
    protected byte audioCh2Enabled;
    
    protected byte audioCh2LengthUsed; // NR24[6].
    protected byte audioCh2Length; // NR21[0-3].
    private int audioCh2LengthCycles; // Internal.

    private int audioCh2Divider4Counter; // Counter of the divider by 4. 
    protected short audioCh2FrequencyInitialValue; // NR23[0-7] and NR24[0-2].
    private int audioCh2Frequency, audioCh2FrequencyCounter; // Current frequency (internal).
    
    protected byte audioCh2Duty; // NR21[6-7].
    private int audioCh2DutyCounter;
    private int audioCh2WaveLastValue;
    
    protected byte audioCh2EnvelopeSweepsStepLength, audioCh2EnvelopeSweepsDirection, audioCh2EnvelopeVolumeInitialValue; // NR22[0-2], NR22[3], NR22[4-7].
    private int audioCh2EnvelopeSweepsStepCycles, audioCh2EnvelopeVolume; // Internal.

    //
    // Channel 3.
    //
    
    private int audioCh3Sample;
    private int audioCh3NextUpdateCycles, audioCh3NextUpdateCyclesCounter;
    
    protected byte audioCh3Enabled;
    
    protected byte audioCh3SoundOnFlag; // NR30[7].    
    protected byte audioCh3LengthUsed; // NR34[6].
    protected short audioCh3Length; // NR31[0-7].
    private int audioCh3LengthCycles; // Internal.
    
    private int audioCh3Divider2Counter; // Counter of the divider by 4. 
    protected short audioCh3FrequencyInitialValue; // NR33[0-7] and NR34[0-2].
    private int audioCh3Frequency, audioCh3FrequencyCounter; // Current frequency (internal).

    protected byte audioCh3OutputLevel; // NR32[5-6].
    private int audioCh3WaveCounter; // Counter in the wave RAM (internal).
    private int audioCh3WaveLastValue;
    private boolean audioCh3WaveDirty=true;
    protected final byte audioCh3WaveData[]=new byte[32]; // Wave RAM.
    private final short audioCh3WaveSum[]=new short[32+1];
    
    private static final byte AUDIO_CH3_LEVEL_SHIFT[]={ 4, 0, 1, 2 };
    
    private static final byte AUDIO_DMG_WAVE[]={
        0xa, 0xc, 0xd, 0xd, 0xd, 0xa, 0x4, 0x8,
        0x3, 0x6, 0x0, 0x2, 0xc, 0xf, 0x1, 0x6,
        0x2, 0xc, 0x0, 0x4, 0xe, 0x5, 0x2, 0xc,
        0xa, 0xc, 0xd, 0xd, 0xd, 0xa, 0x4, 0x8
    };

    private static final byte AUDIO_CGB_WAVE[]={
        0x0, 0x0, 0xf, 0xf, 0x0, 0x0, 0xf, 0xf,
        0x0, 0x0, 0xf, 0xf, 0x0, 0x0, 0xf, 0xf,
        0x0, 0x0, 0xf, 0xf, 0x0, 0x0, 0xf, 0xf,
        0x0, 0x0, 0xf, 0xf, 0x0, 0x0, 0xf, 0xf,
    };
    
    //
    // Channel 4.
    //
    
    private int audioCh4Sample;
    private int audioCh4NextUpdateCycles, audioCh4NextUpdateCyclesCounter;
    
    protected byte audioCh4Enabled;
    
    protected byte audioCh4LengthUsed; // NR44[6].
    protected byte audioCh4Length; // NR41[0-5].
    private int audioCh4LengthCycles; // Internal.
    
    protected byte audioCh4FrequencyClock; // NR43[4-7].
    protected byte audioCh4FrequencyRatio; // NR43[0-2].
    protected byte audioCh4PolynomialCounterSteps; // NR43[3].
    private int audioCh4PolynomialCounter, audioCh4FrequencyCounter; // Internal.
    private final short audioCh4Wave7Sum[]=new short[128+1];
    private final short audioCh4Wave15Sum[]=new short[32768+1];
    
    protected byte audioCh4EnvelopeSweepsStepLength, audioCh4EnvelopeSweepsDirection, audioCh4EnvelopeVolumeInitialValue; // NR42[0-2], NR42[3], NR42[4-7].
    private int audioCh4EnvelopeSweepsStepCycles, audioCh4EnvelopeVolume; // Internal.

    private static final int AUDIO_CH4_RATIO_CYCLES[]={ 4, 8, 16, 24, 32, 40, 48, 54 }; 
    
    private final EmulationEvent audioEvent=new EmulationEvent(this, "Audio") {
        public final void processEvent() {
            clockCurrentCycle=clockCycle;
            audioUpdate();
        }
        
        public final void breakEvent() {}
    };
    
	/**
     * Sets the audio output frequency.
     * <code>frequency</code> must be >=AUDIO_MIN_FREQUENCY and <=AUDIO_MAX_FREQUENCY.
     * The current buffer length is reduced to frequency if it was greater than frequency.
     * Do not call asynchronously.
     * @param frequency the frequency between AUDIO_MIN_FREQUENCY and AUDIO_MAX_FREQUENCY
     */
    // FIXME: The current buffer length is reduced to frequency if it was greater than frequency.
    // I cannot remember why.
	public void setAudioOutputFrequency(int frequency) {
		if (frequency<AUDIO_MIN_FREQUENCY || frequency>AUDIO_MAX_FREQUENCY) return;
		if (audioOutputBufferLength>frequency) audioOutputBufferLength=frequency;
	    audioOutputFrequency=frequency;
		if (globalPowered) {
	    	clockCurrentCycle=clock.timeToCycle(system.getCurrentTime());
		    audioScheduleEvent();
		}
	}
	
	/**
	 * Gets the audio output frequency.
	 * @return the audio output frequency
	 */
	public int getAudioOutputFrequency() {
	    return audioOutputFrequency;
	}
	
	/**
     * Sets the audio output buffer.
     * If <code>offset<0</code> or <code>length<=0</code> or <code>buffer==null</code> the sound output will be disabled.
     * Do not call asynchronously.
     * @param offset the index of the first sample. It must be >=0.
     * @param length the number of samples that can be written. It must be >0.
     * @param buffer the array in which the samples are written. It must not be <code>null</code>.
     */
	public void setAudioOutputBuffer(int offset, int length, byte buffer[]) {
		if (offset<0 || length<=0 || buffer==null) {
		    audioOutputBufferLength=0;
	        audioOutputBufferOffset=0;
		    audioOutputBufferPosition=0;
		    audioOutputBuffer=null;
		} else {
		    audioOutputBufferLength=length;
	        audioOutputBufferOffset=offset;
		    audioOutputBufferPosition=offset;
		    audioOutputBuffer=buffer;
		}
		if (globalPowered) {
	        clockCurrentCycle=clock.timeToCycle(system.getCurrentTime());
		    audioScheduleEvent();
		}
	}

    /**
     * Enables audio output.
     * Do not call asynchronously.
     * @param flag <code>false</code> to disable sound output, <code>true</code> otherwise
     */
    public void enableAudioOutput(boolean flag) {
        audioOutputEnabled=flag;
		if (globalPowered) {
	        clockCurrentCycle=clock.timeToCycle(system.getCurrentTime());
		    audioScheduleEvent();
		}
    }
    
    /**
     * Returns whether audio output is enabled.
     * @return <code>false</code> is sound output is disabled, <code>true</code> otherwise
     */
    public boolean isAudioOutputEnabled() {
        return audioOutputEnabled;
    }
    
    // Returns the (rounded up) number of cycles corresponding to a given number of samples.
    // 0<sampleRate<=65536 and 0<=nbSamples<=sampleRate. 
    private static int audioSamplesToCycles(int sampleRate, int nbSamples) {
        return (int)(((long)nbSamples*(long)CLOCK_FREQUENCY_DMG+(long)(sampleRate-1))/(long)sampleRate);
    }
    
    private void audioInitialize() {
    	// Initialize wave sums for channel 1 and 2.
		for (int j=0; j<4; j++) {
	        int sum=0;
	        for (int i=0; i<8; i++) { audioWavesSum[j][i]=(short)sum; sum+=AUDIO_WAVES[j][i]; }
	        audioWavesSum[j][8]=(short)sum;
		}

		// Initialize wave sums for channel 4.
		{
	        int sum, pc, t;
	        
	        sum=0; pc=1;
	        for (int i=0; i<128; i++) {
	            audioCh4Wave7Sum[i]=(short)sum;
	            sum+=pc&1;
	            t=(pc^(pc>>6))&0x01;
	            pc=(t<<6)|(pc>>1);
	        }
	        audioCh4Wave7Sum[128]=(short)sum;
	        
	        sum=0; pc=1;
	        for (int i=0; i<32768; i++) {
	            audioCh4Wave15Sum[i]=(short)sum;
	            sum+=pc&1;
	            t=(pc^(pc>>14))&0x01;
	            pc=(t<<14)|(pc>>1);
	        }
	        audioCh4Wave15Sum[32768]=(short)sum;
		}
    }

    private void audioReset() {
        audioScheduleEvent();
        audioLastUpdateCycle=clockCurrentCycle;
        audioSampleCyclesRemainder=0;
        audioSampleCycles=0;
        
        audioGlobalEnableFlag=0;
        audioChannelToSOFlags=0;
        audioSO1OutputLevel=0; audioSO1VinFlag=0; audioSO2OutputLevel=0; audioSO2VinFlag=0;
        
        audioCh1Sample=0;
        audioCh1NextUpdateCycles=0; audioCh1NextUpdateCyclesCounter=0;
        audioCh1Enabled=0;
        audioCh1LengthUsed=0; audioCh1Length=0; audioCh1LengthCycles=0;
        audioCh1Divider4Counter=0;
        audioCh1FrequencyInitialValue=0; audioCh1Frequency=0; audioCh1FrequencyCounter=0;
        audioCh1FrequencySweepsNumber=0; audioCh1FrequencySweepsDirection=0; audioCh1FrequencySweepsDuration=0; audioCh1FrequencySweepsStepCycles=0;
        audioCh1Duty=0; audioCh1DutyCounter=0;
        audioCh1WaveLastValue=0;
        audioCh1EnvelopeVolumeInitialValue=0; audioCh1EnvelopeVolume=0;
        audioCh1EnvelopeSweepsDirection=0; audioCh1EnvelopeSweepsStepLength=0; audioCh1EnvelopeSweepsStepCycles=0;

        audioCh2Sample=0;
        audioCh2NextUpdateCycles=0; audioCh2NextUpdateCyclesCounter=0;
        audioCh2Enabled=0;
        audioCh2LengthUsed=0; audioCh2Length=0; audioCh2LengthCycles=0;
        audioCh2Divider4Counter=0;
        audioCh2FrequencyInitialValue=0; audioCh2Frequency=0; audioCh2FrequencyCounter=0;
        audioCh2Duty=0; audioCh2DutyCounter=0;
        audioCh2WaveLastValue=0;
        audioCh2EnvelopeVolumeInitialValue=0; audioCh2EnvelopeVolume=0;
        audioCh2EnvelopeSweepsDirection=0; audioCh2EnvelopeSweepsStepLength=0; audioCh2EnvelopeSweepsStepCycles=0;

        audioCh3Sample=0;
        audioCh3NextUpdateCycles=0; audioCh3NextUpdateCyclesCounter=0;
        audioCh3Enabled=0;
        audioCh3SoundOnFlag=0;
        audioCh3LengthUsed=0; audioCh3Length=0; audioCh3LengthCycles=0;
        audioCh3Divider2Counter=0;
        audioCh3FrequencyInitialValue=0;
        audioCh3OutputLevel=0;
        audioCh3WaveCounter=0;
        audioCh3WaveLastValue=0;
        byte w[]=globalColorModeEnabled ? AUDIO_CGB_WAVE : AUDIO_DMG_WAVE;
        for (int i=0; i<32; i++) audioCh3WaveData[i]=w[i];
        audioCh3WaveDirty=true;
        audioCh3UpdateWaveSum();

        audioCh4Sample=0;
        audioCh4NextUpdateCycles=0; audioCh4NextUpdateCyclesCounter=0;
        audioCh4Enabled=0;
        audioCh4LengthUsed=0; audioCh4Length=0; audioCh4LengthCycles=0;
        audioCh4FrequencyRatio=0; audioCh4FrequencyClock=0; audioCh4FrequencyCounter=0;
        audioCh4PolynomialCounterSteps=0; audioCh4PolynomialCounter=0;
        audioCh4EnvelopeSweepsStepLength=0; audioCh4EnvelopeSweepsDirection=0; audioCh4EnvelopeVolumeInitialValue=0;
        audioCh4EnvelopeSweepsStepCycles=0; audioCh4EnvelopeVolume=0;
    }
    
    // Schedule the next audio update event.
    private void audioScheduleEvent() {
    	if (audioOutputEnabled) {
			int r=audioOutputBufferLength-(audioOutputBufferPosition-audioOutputBufferOffset);
	        int t=audioSamplesToCycles(audioOutputFrequency, r);
	        if (t<=0) t=1;
	        if (clockDoubleSpeedFlag!=0) t<<=1;
	        audioEvent.clockCycle=TimeStamp.add(clockCurrentCycle, t);
	        clock.addEvent(audioEvent);    		
    	} else  {
    		// We can remove the event because it is only used to produce sound regularly.
    		system.removeEvent(audioEvent);
    	}
    }

    // Update audio circuits up to the current time.
    private void audioUpdate() {
        // - Overflow during integration: 
        //  There are 4 voices that are added together (this uses 2 bits).
        //  The Game Boy uses 4-bit values for audio.
        //  There is a 3-bit volume control.
        //  The result is shifted 6 bits left to obtain a 8-bit value.
        //  So cyclesStep must not be greater than 2^(31-2-4-3-6)-1=2^16-1=65535 !!!
        //  This means that audioOutputFrequency cannot be lower than 2^(22-16)=2^6=64 Hz.
        // - Overflow during calculation of cyclesStep:
        //  audioOutputFrequency must not be greater than 65536 and cyclesStep must be <=32767.
        if (clockCurrentCycle==audioLastUpdateCycle) return;
        
        // Find the number of cycles.
        int cycles=(int)TimeStamp.add(clockCurrentCycle, -audioLastUpdateCycle);
        if (clockDoubleSpeedFlag!=0) {
            audioLastUpdateCycle=TimeStamp.add(clockCurrentCycle, -(cycles&1));
            cycles>>=1;
        } else {
            audioLastUpdateCycle=clockCurrentCycle;            
        }

        // Update loop.
        while (cycles>0) {
            boolean nextSampleFlag;
            int cyclesStep;        	
        	
        	// Compute the number of cycles for this iteration.
        	{
	            int c=CLOCK_FREQUENCY_DMG-audioSampleCyclesRemainder; // Remaining time in this sample.
	            nextSampleFlag=false;
	            cyclesStep=(cycles>AUDIO_MAX_CYCLES_STEP) ? AUDIO_MAX_CYCLES_STEP : cycles;
	            if ((cyclesStep*audioOutputFrequency)>c) {
	                cyclesStep=(c+audioOutputFrequency-1)/audioOutputFrequency; // Round up.
	                nextSampleFlag=true;
	                audioSampleCyclesRemainder-=CLOCK_FREQUENCY_DMG;
	            }
	            cycles-=cyclesStep;
	            audioSampleCyclesRemainder+=cyclesStep*audioOutputFrequency;
	            audioSampleCycles+=cyclesStep;
        	}
            
            // Integrate all channels.
            if (audioGlobalEnableFlag!=0) {
                audioCh1Integrate(cyclesStep);
                audioCh2Integrate(cyclesStep);
                audioCh3Integrate(cyclesStep);
                audioCh4Integrate(cyclesStep);
            }
            
            // Write a new sample.
            if (nextSampleFlag) {
                if (audioOutputEnabled) { 
                    int left=0, right=0;
                    
                    // Panning.
                    {
                        int t=audioChannelToSOFlags;
                        if ((t&0x01)!=0) right+=audioCh1Sample; if ((t&0x10)!=0) left+=audioCh1Sample;
                        if ((t&0x02)!=0) right+=audioCh2Sample; if ((t&0x20)!=0) left+=audioCh2Sample;
                     	if ((t&0x04)!=0) right+=audioCh3Sample; if ((t&0x40)!=0) left+=audioCh3Sample;
                     	if ((t&0x08)!=0) right+=audioCh4Sample; if ((t&0x80)!=0) left+=audioCh4Sample;
                    }

                    // Volume.
                    {
                        int t=7*15*4*audioSampleCycles; // 7 for the volume, 15 for the number of intensity levels, 4 for the number of voices. 
                        right=(audioSO1OutputLevel*right*127)/t;
                        left=(audioSO2OutputLevel*left*127)/t;
                    }
					
                    // Write final sample value if there is room in the buffer. Note that we lose the first sample if the buffer has not been setup.
                    int room = audioOutputBufferLength - (audioOutputBufferPosition-audioOutputBufferOffset);
                    if (room>0) {
	                    int t=(audioOutputBufferPosition<<1);
	                    audioOutputBuffer[t]=(byte)left; audioOutputBuffer[t+1]=(byte)right;
	                    audioOutputBufferPosition++;
                    }
                    // If the buffer is full, flush it.
                    if (room<=1) {
                        if (globalListener!=null) globalListener.onEndOfAudioBuffer(globalEvent);
                    }
                }

                audioSampleCycles=0; // Reset number of cycles in the current sample.
                audioCh1Sample=0; audioCh2Sample=0; audioCh3Sample=0; audioCh4Sample=0; // Reset sample values for each channel.
            }
        }
    }

    private void audioSetNR50(int data) {
        audioSO1OutputLevel=(byte)(data&0x07);
        audioSO1VinFlag=(byte)((data>>3)&0x01);
        audioSO2OutputLevel=(byte)((data>>4)&0x07);
        audioSO2VinFlag=(byte)((data>>7)&0x01);
    }
    
    private void audioCh1SetNR10(int data) {
        audioCh1NextUpdateCycles=audioCh1NextUpdateCyclesCounter;
        audioCh1FrequencySweepsNumber=(byte)(data&0x07);
        audioCh1FrequencySweepsDirection=(byte)((data>>3)&0x01);
        audioCh1FrequencySweepsDuration=(byte)((data>>4)&0x07);
    }
    
    private void audioCh1SetNR11(int data) {
        audioCh1NextUpdateCycles=audioCh1NextUpdateCyclesCounter;
        audioCh1Length=(byte)(data&0x3f);
        audioCh1Duty=(byte)((data>>6)&0x03);        
    }
    
    private void audioCh1SetNR12(int data) {
        audioCh1NextUpdateCycles=audioCh1NextUpdateCyclesCounter;
        audioCh1EnvelopeSweepsStepLength=(byte)(data&0x07);
        audioCh1EnvelopeSweepsDirection=(byte)((data>>3)&0x01);
        audioCh1EnvelopeVolumeInitialValue=(byte)((data>>4)&0x0f);
        if ((audioCh1EnvelopeVolumeInitialValue==0) && (audioCh1EnvelopeSweepsDirection==0)) audioCh1Enabled=0;
    }
    
    private void audioCh1SetNR13(int data) {
        audioCh1NextUpdateCycles=audioCh1NextUpdateCyclesCounter;
        audioCh1FrequencyInitialValue=(short)((audioCh1FrequencyInitialValue&0x0700) | data);
        audioCh1Frequency=audioCh1FrequencyInitialValue;
//        audioCh1Frequency=(short)((audioCh1Frequency&0x0700) | data);
    }
    
    private void audioCh1SetNR14(int data) {
        audioCh1NextUpdateCycles=audioCh1NextUpdateCyclesCounter;
        audioCh1FrequencyInitialValue=(short)(((data&0x07)<<8) | (audioCh1FrequencyInitialValue&0x00ff));
        audioCh1Frequency=audioCh1FrequencyInitialValue;
//        audioCh1Frequency=(short)(((data&0x07)<<8) | (audioCh1Frequency&0x00ff));
        audioCh1LengthUsed=(byte)((data>>6)&0x01);
        if ((data&0x80)!=0) {
            audioCh1LengthCycles=(64-audioCh1Length)*AUDIO_LENGTH_STEP_CYCLES;
            audioCh1Divider4Counter=0;
            audioCh1FrequencyCounter=audioCh1Frequency;
//            audioCh1DutyCounter=0;
            audioCh1FrequencySweepsStepCycles=audioCh1FrequencySweepsDuration*AUDIO_FREQUENCY_SWEEP_STEP_CYCLES;
            audioCh1EnvelopeVolume=audioCh1EnvelopeVolumeInitialValue;
            audioCh1EnvelopeSweepsStepCycles=audioCh1EnvelopeSweepsStepLength*AUDIO_ENVELOPE_SWEEP_STEP_CYCLES;
            audioCh1Enabled=1;
        }
    }
       
    private void audioCh1Integrate(int cycles) {
        int stepCycles=cycles, c;
        while (cycles>0) {            
            stepCycles=cycles; c=audioCh1NextUpdateCycles-audioCh1NextUpdateCyclesCounter;
            if (stepCycles>c) stepCycles=c;
            if (stepCycles>0) {
                audioCh1NextUpdateCyclesCounter+=stepCycles;
                cycles-=stepCycles;
                audioCh1IntegrateWave(stepCycles);
            } else audioCh1Update();
        }
    }

	private void audioCh1IntegrateWave(int cycles) {
    	int value;
    	
        if (audioCh1Enabled==0) value=audioCh1WaveLastValue*cycles;
        else {        
	        int ff=2048-audioCh1Frequency, cyclesPerSample=ff<<2, frequencyCounter=audioCh1FrequencyCounter-audioCh1Frequency;
	    	int cyclesPhase, samplesPhase, samplesStartPhase, samplesEndPhase;
	        
	        // Compute the new value of each counter.
	    	{
		        int ci, co;
		        // cyclesPhase can be <0 if the frequency counter is smaller than the frequency (this means that frequency has been changed during replay).
		        cyclesPhase=(frequencyCounter<<2)+audioCh1Divider4Counter;
		        samplesStartPhase=audioCh1DutyCounter;
		        samplesPhase=(samplesStartPhase+1)&0x7;
		        ci=audioCh1Divider4Counter+cycles; co=ci>>2; audioCh1Divider4Counter=ci&0x3;
		        ci=frequencyCounter+co; co=(ci>0) ? ci/ff : 0; audioCh1FrequencyCounter=audioCh1Frequency+ci-(co*ff);
		        ci=audioCh1DutyCounter+co; audioCh1DutyCounter=ci&0x7;
		        audioCh1WaveLastValue=AUDIO_WAVES[audioCh1Duty][audioCh1DutyCounter];
		        samplesEndPhase=audioCh1DutyCounter;
	    	}
		        
	        // Integrate.
	        {
	        	int cyclesHead, cyclesTail;
	        	int samplesHead, samplesTail;
	        	int samples, waves;
		    	int t;
	        	
		    	// Number of heading cycles.
		    	cyclesHead=cyclesPerSample-cyclesPhase; // If cyclesPhase<0, this code works correcly too.
		    	if (cyclesHead>=cycles) cyclesHead=cycles;
		    	// Number of full samples.
		        t=cycles-cyclesHead;
		        samples=t/cyclesPerSample;
		        // Number of tailing cycles.
		        cyclesTail=t-samples*cyclesPerSample;
		        
		        // Number of heading samples.
		        samplesHead=8-samplesPhase;
		        if (samplesHead>=samples) samplesHead=samples;
		        // Number of full waves.
		        t=samples-samplesHead;
		        waves=t>>3;
		        // Number of tailing samples.
		        samplesTail=t-(waves<<3);
	        
		        short waveSum[]=audioWavesSum[audioCh1Duty];
		        t=(waveSum[samplesPhase+samplesHead]-waveSum[samplesPhase]); // Head samples.
		        t+=waveSum[8]*waves; // Full waves.
		        t+=waveSum[samplesTail]; // Tail samples.
		        value=t*cyclesPerSample;
		        value+=(waveSum[samplesStartPhase+1]-waveSum[samplesStartPhase])*cyclesHead; // Head cycles.
		    	value+=(waveSum[samplesEndPhase+1]-waveSum[samplesEndPhase])*cyclesTail; // Tail cycles.
	        }
        }
        
        // Shift to center square wave around 0, scale by volume and add.
    	audioCh1Sample+=(value-(cycles-value))*audioCh1EnvelopeVolume;
    }

    /*
    private void audioCh1IntegrateWave(int cycles) {
        if (audioCh1Enabled==0) return;
        
        int ff=2048-audioCh1Frequency, ft=ff<<5, limit=AUDIO_DUTY[audioCh1Duty], limitPhase=(limit*ft)>>3;
        int ci, co, t, startingPhase, headCycles, fullWaveCycles, tailCycles;
        
        // This should work even if audioCh1FrequencyCounter<audioCh1Frequency.
        
        // Compute the starting phase of the wave in cycles.
        startingPhase=audioCh1Divider4Counter+(((audioCh1FrequencyCounter-audioCh1Frequency)+audioCh1DutyCounter*ff)<<2);
        // Compute the new value of each counter.
        ci=audioCh1Divider4Counter+cycles; co=ci>>2; audioCh1Divider4Counter=ci&0x3;
        ci=audioCh1FrequencyCounter-audioCh1Frequency+co; co=(ci>=ff) ? ci/ff : 0; audioCh1FrequencyCounter=audioCh1Frequency+ci-(co*ff);
        ci=audioCh1DutyCounter+co; audioCh1DutyCounter=ci&0x7;
        
        // Compute the number of full waves and the number of remaining cycles at head and tail.
        t=cycles-(ft-startingPhase);
        if (t<0) t=0;
        headCycles=cycles-t; fullWaveCycles=(t/ft)*ft; tailCycles=t-fullWaveCycles;
        // Compute the total number of cycles where the wave is >0.
        if (startingPhase>=limitPhase) headCycles=0;
        else if (startingPhase+headCycles>limitPhase) headCycles=limitPhase-startingPhase;
        if (tailCycles>limitPhase) tailCycles=limitPhase;
        t=headCycles+((fullWaveCycles*limit)>>3)+tailCycles;
        // Add the contribution to the current sample.
        audioCh1Sample+=(t-(cycles-t))*audioCh1EnvelopeVolume;
    }
	*/
	
    private void audioCh1Update() {
        int c=CLOCK_FREQUENCY_DMG;

        // Length.
        if (audioCh1LengthUsed!=0) {
            if (audioCh1LengthCycles>audioCh1NextUpdateCyclesCounter) {
                audioCh1LengthCycles-=audioCh1NextUpdateCyclesCounter;
                if (c>audioCh1LengthCycles) c=audioCh1LengthCycles;
            } else {
                audioCh1LengthCycles=0;
                audioCh1Enabled=0;
            }
        }
        
        // Frequency sweep.
        if ((audioCh1FrequencySweepsNumber>0) && (audioCh1FrequencySweepsDuration>0)) {
            if (audioCh1FrequencySweepsStepCycles>audioCh1NextUpdateCyclesCounter) {
                audioCh1FrequencySweepsStepCycles-=audioCh1NextUpdateCyclesCounter;
                if (c>=audioCh1FrequencySweepsStepCycles) c=audioCh1FrequencySweepsStepCycles;
            } else {
                audioCh1FrequencySweepsStepCycles=audioCh1FrequencySweepsDuration*AUDIO_FREQUENCY_SWEEP_STEP_CYCLES;
                if (c>=audioCh1FrequencySweepsStepCycles) c=audioCh1FrequencySweepsStepCycles;
                
                int frequencyDelta=audioCh1Frequency>>audioCh1FrequencySweepsNumber;
                if (audioCh1FrequencySweepsDirection!=0) {
                    if ((audioCh1Frequency-frequencyDelta)>=0x000) audioCh1Frequency-=frequencyDelta;
                } else {
                    if ((audioCh1Frequency+frequencyDelta)<=0x7ff) audioCh1Frequency+=frequencyDelta;
                    else audioCh1Enabled=0;
                }
            }
        }
        
        // Envelope.
        if (audioCh1EnvelopeSweepsStepLength>0) {
            if (audioCh1EnvelopeSweepsStepCycles>audioCh1NextUpdateCyclesCounter) {
                audioCh1EnvelopeSweepsStepCycles-=audioCh1NextUpdateCyclesCounter;
            } else {
                audioCh1EnvelopeSweepsStepCycles=audioCh1EnvelopeSweepsStepLength*AUDIO_ENVELOPE_SWEEP_STEP_CYCLES;
                if (audioCh1EnvelopeSweepsDirection!=0) {
                    if (audioCh1EnvelopeVolume<0x0f) audioCh1EnvelopeVolume++;
                } else {
                    if (audioCh1EnvelopeVolume>0x00) audioCh1EnvelopeVolume--;
                }
            }
            if (c>=audioCh1EnvelopeSweepsStepCycles) c=audioCh1EnvelopeSweepsStepCycles;
        }

        audioCh1NextUpdateCycles=c; audioCh1NextUpdateCyclesCounter=0;
    }
    
    private void audioCh2SetNR21(int data) {
        audioCh2NextUpdateCycles=audioCh2NextUpdateCyclesCounter;
        audioCh2Length=(byte)(data&0x3f);
        audioCh2Duty=(byte)((data>>6)&0x03);        
    }
    
    private void audioCh2SetNR22(int data) {
        audioCh2NextUpdateCycles=audioCh2NextUpdateCyclesCounter;
        audioCh2EnvelopeSweepsStepLength=(byte)(data&0x07);
        audioCh2EnvelopeSweepsDirection=(byte)((data>>3)&0x01);
        audioCh2EnvelopeVolumeInitialValue=(byte)((data>>4)&0x0f);
        if ((audioCh2EnvelopeVolumeInitialValue==0) && (audioCh2EnvelopeSweepsDirection==0)) audioCh2Enabled=0;
    }
    
    private void audioCh2SetNR23(int data) {
        audioCh2NextUpdateCycles=audioCh2NextUpdateCyclesCounter;
        audioCh2FrequencyInitialValue=(short)((audioCh2FrequencyInitialValue&0x0700) | data);        
        audioCh2Frequency=audioCh2FrequencyInitialValue;
    }
    
    private void audioCh2SetNR24(int data) {
        audioCh2NextUpdateCycles=audioCh2NextUpdateCyclesCounter;
        audioCh2FrequencyInitialValue=(short)(((data&0x07)<<8) | (audioCh2FrequencyInitialValue&0x00ff));
        audioCh2Frequency=audioCh2FrequencyInitialValue;
        audioCh2LengthUsed=(byte)((data>>6)&0x01);
        if ((data&0x80)!=0) {
            audioCh2LengthCycles=(64-audioCh2Length)*AUDIO_LENGTH_STEP_CYCLES;
            audioCh2Divider4Counter=0;
            audioCh2FrequencyCounter=audioCh2Frequency;
//            audioCh2DutyCounter=0;
            audioCh2EnvelopeVolume=audioCh2EnvelopeVolumeInitialValue;            
            audioCh2EnvelopeSweepsStepCycles=audioCh2EnvelopeSweepsStepLength*AUDIO_ENVELOPE_SWEEP_STEP_CYCLES;
            audioCh2Enabled=1;
        }
    }
    
    private void audioCh2Integrate(int cycles) {
        int stepCycles=cycles, c;
        while (cycles>0) {            
            stepCycles=cycles; c=audioCh2NextUpdateCycles-audioCh2NextUpdateCyclesCounter;
            if (stepCycles>c) stepCycles=c;
            if (stepCycles>0) {
                audioCh2NextUpdateCyclesCounter+=stepCycles;
                cycles-=stepCycles;
                audioCh2IntegrateWave(stepCycles);
            } else audioCh2Update();
        }
    }

	private void audioCh2IntegrateWave(int cycles) {
    	int value;
    	
        if (audioCh2Enabled==0) value=audioCh2WaveLastValue*cycles;
        else {        
	        int ff=2048-audioCh2Frequency, cyclesPerSample=ff<<2, frequencyCounter=audioCh2FrequencyCounter-audioCh2Frequency;
	    	int cyclesPhase, samplesPhase, samplesStartPhase, samplesEndPhase;
	        
	        // Compute the new value of each counter.
	    	{
		        int ci, co;
		        // cyclesPhase can be <0 if the frequency counter is smaller than the frequency (this means that frequency has been changed during replay).
		        cyclesPhase=(frequencyCounter<<2)+audioCh2Divider4Counter;
		        samplesStartPhase=audioCh2DutyCounter;
		        samplesPhase=(samplesStartPhase+1)&0x7;
		        ci=audioCh2Divider4Counter+cycles; co=ci>>2; audioCh2Divider4Counter=ci&0x3;
		        ci=frequencyCounter+co; co=(ci>0) ? ci/ff : 0; audioCh2FrequencyCounter=audioCh2Frequency+ci-(co*ff);
		        ci=audioCh2DutyCounter+co; audioCh2DutyCounter=ci&0x7;
		        audioCh2WaveLastValue=AUDIO_WAVES[audioCh2Duty][audioCh2DutyCounter];
		        samplesEndPhase=audioCh2DutyCounter;
	    	}
		        
	        // Integrate.
	        {
	        	int cyclesHead, cyclesTail;
	        	int samplesHead, samplesTail;
	        	int samples, waves;
		    	int t;
	        	
		    	// Number of heading cycles.
		    	cyclesHead=cyclesPerSample-cyclesPhase; // If cyclesPhase<0, this code works correcly too.
		    	if (cyclesHead>=cycles) cyclesHead=cycles;
		    	// Number of full samples.
		        t=cycles-cyclesHead;
		        samples=t/cyclesPerSample;
		        // Number of tailing cycles.
		        cyclesTail=t-samples*cyclesPerSample;
		        
		        // Number of heading samples.
		        samplesHead=8-samplesPhase;
		        if (samplesHead>=samples) samplesHead=samples;
		        // Number of full waves.
		        t=samples-samplesHead;
		        waves=t>>3;
		        // Number of tailing samples.
		        samplesTail=t-(waves<<3);
	        
		        short waveSum[]=audioWavesSum[audioCh2Duty];
		        t=(waveSum[samplesPhase+samplesHead]-waveSum[samplesPhase]); // Head samples.
		        t+=waveSum[8]*waves; // Full waves.
		        t+=waveSum[samplesTail]; // Tail samples.
		        value=t*cyclesPerSample;
		        value+=(waveSum[samplesStartPhase+1]-waveSum[samplesStartPhase])*cyclesHead; // Head cycles.
		    	value+=(waveSum[samplesEndPhase+1]-waveSum[samplesEndPhase])*cyclesTail; // Tail cycles.
	        }
        }
        
        // Shift to center square wave around 0, scale by volume and add.
    	audioCh2Sample+=(value-(cycles-value))*audioCh2EnvelopeVolume;
    }

    /*
    private void audioCh2IntegrateWave(int cycles) {
        if (audioCh2Enabled==0) return;
        
        int ff=2048-audioCh2Frequency, ft=ff<<5, limit=AUDIO_DUTY[audioCh2Duty], limitPhase=(limit*ft)>>3;
        int ci, co, t, startingPhase, headCycles, fullWaveCycles, tailCycles;
        
        // This should work even if audioCh2FrequencyCounter<audioCh2Frequency.
        
        // Compute the starting phase of the wave in cycles.
        startingPhase=audioCh2Divider4Counter+(((audioCh2FrequencyCounter-audioCh2Frequency)+audioCh2DutyCounter*ff)<<2);
        // Compute the new value of each counter.
        ci=audioCh2Divider4Counter+cycles; co=ci>>2; audioCh2Divider4Counter=ci&0x3;
        ci=audioCh2FrequencyCounter-audioCh2Frequency+co; co=(ci>=ff) ? ci/ff : 0; audioCh2FrequencyCounter=audioCh2Frequency+ci-(co*ff);
        ci=audioCh2DutyCounter+co; audioCh2DutyCounter=ci&0x7;
        
        // Compute the number of full waves and the number of remaining cycles at head and tail.
        t=cycles-(ft-startingPhase);
        if (t<0) t=0;
        headCycles=cycles-t; fullWaveCycles=(t/ft)*ft; tailCycles=t-fullWaveCycles;
        // Compute the total number of cycles where the wave is >0.
        if (startingPhase>=limitPhase) headCycles=0;
        else if (startingPhase+headCycles>limitPhase) headCycles=limitPhase-startingPhase;
        if (tailCycles>limitPhase) tailCycles=limitPhase;
        t=headCycles+((fullWaveCycles*limit)>>3)+tailCycles;
        // Add the contribution to the current sample.
        audioCh2Sample+=(t-(cycles-t))*audioCh2EnvelopeVolume;
    }
	*/
    
    private void audioCh2Update() {
        int c=CLOCK_FREQUENCY_DMG;

        // Length.
        if (audioCh2LengthUsed!=0) {
            if (audioCh2LengthCycles>audioCh2NextUpdateCyclesCounter) {
                audioCh2LengthCycles-=audioCh2NextUpdateCyclesCounter;
                if (c>audioCh2LengthCycles) c=audioCh2LengthCycles;
            } else {
                audioCh2LengthCycles=0;
                audioCh2Enabled=0;
            }
        }
        
        // Envelope.
        if (audioCh2EnvelopeSweepsStepLength>0) {
            if (audioCh2EnvelopeSweepsStepCycles>audioCh2NextUpdateCyclesCounter) {
                audioCh2EnvelopeSweepsStepCycles-=audioCh2NextUpdateCyclesCounter;
            } else {
                audioCh2EnvelopeSweepsStepCycles=audioCh2EnvelopeSweepsStepLength*AUDIO_ENVELOPE_SWEEP_STEP_CYCLES;                
                if (audioCh2EnvelopeSweepsDirection!=0) {
                    if (audioCh2EnvelopeVolume<0x0f) audioCh2EnvelopeVolume++;
                } else {
                    if (audioCh2EnvelopeVolume>0x00) audioCh2EnvelopeVolume--;                        
                }
            }
            if (c>=audioCh2EnvelopeSweepsStepCycles) c=audioCh2EnvelopeSweepsStepCycles;
        }

        audioCh2NextUpdateCycles=c; audioCh2NextUpdateCyclesCounter=0;
    }
    
    private void audioCh3SetNR30(int data) {
        audioCh3NextUpdateCycles=audioCh3NextUpdateCyclesCounter;
        audioCh3SoundOnFlag=(byte)((data>>7)&0x01);
        if (audioCh3SoundOnFlag==0) audioCh3Enabled=0;
    }
    
    private void audioCh3SetNR31(int data) {
        audioCh3NextUpdateCycles=audioCh3NextUpdateCyclesCounter;
        audioCh3Length=(short)data;
    }
           
    private void audioCh3SetNR32(int data) {
        audioCh3NextUpdateCycles=audioCh3NextUpdateCyclesCounter;
        audioCh3OutputLevel=(byte)((data>>5)&0x03);
    }

    private void audioCh3SetNR33(int data) {
        audioCh3NextUpdateCycles=audioCh3NextUpdateCyclesCounter;
        audioCh3FrequencyInitialValue=(short)((audioCh3FrequencyInitialValue&0x0700) | data);
        audioCh3Frequency=audioCh3FrequencyInitialValue;
    }

    private void audioCh3SetNR34(int data) {
        audioCh3NextUpdateCycles=audioCh3NextUpdateCyclesCounter;
        audioCh3FrequencyInitialValue=(short)(((data&0x07)<<8) | (audioCh3FrequencyInitialValue&0x00ff));
        audioCh3Frequency=audioCh3FrequencyInitialValue;
        audioCh3LengthUsed=(byte)((data>>6)&0x01);
        if ((data&0x80)!=0) {
            audioCh3LengthCycles=(256-audioCh3Length)*AUDIO_LENGTH_STEP_CYCLES;
            audioCh3Divider2Counter=0;
            audioCh3FrequencyCounter=audioCh3Frequency;
            audioCh3WaveCounter=0;
            audioCh3Enabled=1;
        }
    }
    
    private void audioCh3SetWaveRam(int b, int data) {
        b<<=1; audioCh3WaveData[b]=(byte)((data>>4)&0x0f); audioCh3WaveData[b+1]=(byte)(data&0x0f);
        audioCh3WaveDirty=true;
    }
    
    private void audioCh3Integrate(int cycles) {
        int stepCycles=cycles, c;
        
        if ((audioCh3Enabled!=0) && audioCh3WaveDirty) audioCh3UpdateWaveSum();

        while (cycles>0) {            
            stepCycles=cycles; c=audioCh3NextUpdateCycles-audioCh3NextUpdateCyclesCounter;
            if (stepCycles>c) stepCycles=c;
            if (stepCycles>0) {
                audioCh3NextUpdateCyclesCounter+=stepCycles;
                cycles-=stepCycles;
                audioCh3IntegrateWave(stepCycles);
            } else audioCh3Update();
        }
    }
    	
    private void audioCh3IntegrateWave(int cycles) {
    	int value;
    	
        if (audioCh3Enabled==0) value=audioCh3WaveLastValue*cycles;
        else {        
	        int ff=2048-audioCh3Frequency, cyclesPerSample=ff<<1, frequencyCounter=audioCh3FrequencyCounter-audioCh3Frequency;
	    	int cyclesPhase, samplesPhase, samplesStartPhase, samplesEndPhase;
	        
	        // Compute the new value of each counter.
	    	{
		        int ci, co;
		        // cyclesPhase can be <0 if the frequency counter is smaller than the frequency (this means that frequency has been changed during replay).
		        cyclesPhase=(frequencyCounter<<1)+audioCh3Divider2Counter;
		        samplesStartPhase=audioCh3WaveCounter;
		        samplesPhase=(samplesStartPhase+1)&0x1f;
		        ci=audioCh3Divider2Counter+cycles; co=ci>>1; audioCh3Divider2Counter=ci&0x1;
		        ci=frequencyCounter+co; co=(ci>0) ? ci/ff : 0; audioCh3FrequencyCounter=audioCh3Frequency+ci-(co*ff);
		        ci=audioCh3WaveCounter+co; audioCh3WaveCounter=ci&0x1f;
		        audioCh3WaveLastValue=audioCh3WaveData[audioCh3WaveCounter];
		        samplesEndPhase=audioCh3WaveCounter;
	    	}
		        
	        // Integrate.
	        {
	        	int cyclesHead, cyclesTail;
	        	int samplesHead, samplesTail;
	        	int samples, waves;
		    	int t;
	        	
		    	// Number of heading cycles.
		    	cyclesHead=cyclesPerSample-cyclesPhase; // If cyclesPhase<0, this code works correcly too.
		    	if (cyclesHead>=cycles) cyclesHead=cycles;
		    	// Number of full samples.
		        t=cycles-cyclesHead;
		        samples=t/cyclesPerSample;
		        // Number of tailing cycles.
		        cyclesTail=t-samples*cyclesPerSample;
		        
		        // Number of heading samples.
		        samplesHead=32-samplesPhase;
		        if (samplesHead>=samples) samplesHead=samples;
		        // Number of full waves.
		        t=samples-samplesHead;
		        waves=t>>5;
		        // Number of tailing samples.
		        samplesTail=t-(waves<<5);
	        
		        short waveSum[]=audioCh3WaveSum;
		        t=(waveSum[samplesPhase+samplesHead]-waveSum[samplesPhase]); // Head samples.
		        t+=waveSum[32]*waves; // Full waves.
		        t+=waveSum[samplesTail]; // Tail samples.
		        value=t*cyclesPerSample;
		        value+=(waveSum[samplesStartPhase+1]-waveSum[samplesStartPhase])*cyclesHead; // Head cycles.
		    	value+=(waveSum[samplesEndPhase+1]-waveSum[samplesEndPhase])*cyclesTail; // Tail cycles.
	        }
        }
        
        // Shift to center square wave around 0, scale by volume and add.
        int shift=AUDIO_CH3_LEVEL_SHIFT[audioCh3OutputLevel];
        audioCh3Sample+=((value>>shift)<<1)-cycles*(15>>shift);
    }
    
    private void audioCh3Update() {
        int c=CLOCK_FREQUENCY_DMG;

        // Length.
        if (audioCh3LengthUsed!=0) {
            if (audioCh3LengthCycles>audioCh3NextUpdateCyclesCounter) {
                audioCh3LengthCycles-=audioCh3NextUpdateCyclesCounter;
                if (c>audioCh3LengthCycles) c=audioCh3LengthCycles;
            } else {
                audioCh3LengthCycles=0;
                audioCh3Enabled=0;
            }
        }
        
        audioCh3NextUpdateCycles=c; audioCh3NextUpdateCyclesCounter=0;
    }
    
    private void audioCh3UpdateWaveSum() {
        int sum=0;
        for (int i=0; i<32; i++) { audioCh3WaveSum[i]=(short)sum; sum+=audioCh3WaveData[i]; }
        audioCh3WaveSum[32]=(short)sum;
        audioCh3WaveDirty=false;
    }
    
    private void audioCh4SetNR41(int data) {
        audioCh4NextUpdateCycles=audioCh4NextUpdateCyclesCounter;
        audioCh4Length=(byte)(data&0x3f);        
    }
           
    private void audioCh4SetNR42(int data) {
        audioCh4NextUpdateCycles=audioCh4NextUpdateCyclesCounter;
        audioCh4EnvelopeSweepsStepLength=(byte)(data&0x07);
        audioCh4EnvelopeSweepsDirection=(byte)((data>>3)&0x01);
        audioCh4EnvelopeVolumeInitialValue=(byte)((data>>4)&0x0f);
        if ((audioCh4EnvelopeVolumeInitialValue==0) && (audioCh4EnvelopeSweepsDirection==0)) audioCh4Enabled=0;
    }

    private void audioCh4SetNR43(int data) {
        audioCh4NextUpdateCycles=audioCh4NextUpdateCyclesCounter;
        audioCh4FrequencyRatio=(byte)(data&0x07);
        audioCh4PolynomialCounterSteps=(byte)((data>>3)&0x01);
        audioCh4FrequencyClock=(byte)((data>>4)&0x0f);
        audioCh4FrequencyCounter=0;        
    }

    private void audioCh4SetNR44(int data) {
        audioCh4NextUpdateCycles=audioCh4NextUpdateCyclesCounter;
        audioCh4LengthUsed=(byte)((data>>6)&0x01);
        if ((data&0x80)!=0) {
            audioCh4LengthCycles=(64-audioCh4Length)*AUDIO_LENGTH_STEP_CYCLES;
//            audioCh4FrequencyCounter=0;
//            audioCh4PolynomialCounter=0;
            audioCh4EnvelopeVolume=audioCh4EnvelopeVolumeInitialValue;            
            audioCh4EnvelopeSweepsStepCycles=audioCh4EnvelopeSweepsStepLength*AUDIO_ENVELOPE_SWEEP_STEP_CYCLES;
            audioCh4Enabled=1;
        }
    }
    
    private void audioCh4Integrate(int cycles) {
        int stepCycles=cycles, c;
        while (cycles>0) {            
            stepCycles=cycles; c=audioCh4NextUpdateCycles-audioCh4NextUpdateCyclesCounter;
            if (stepCycles>c) stepCycles=c;
            if (stepCycles>0) {
                audioCh4NextUpdateCyclesCounter+=stepCycles;
                cycles-=stepCycles;
                audioCh4IntegrateWave(stepCycles);
            } else audioCh4Update();
        }
    }
    
    private void audioCh4IntegrateWave(int cycles) {
        if (audioCh4Enabled==0 || audioCh4FrequencyClock==14 || audioCh4FrequencyClock==15) return;
        
        int waveBits, waveLength, waveMask;
        short waveSum[];
        if (audioCh4PolynomialCounterSteps!=0) {
        	waveBits=7; waveSum=audioCh4Wave7Sum;
        } else {
        	waveBits=15; waveSum=audioCh4Wave15Sum;
        }
        waveLength=(1<<waveBits); waveMask=waveLength-1;
        
        int cyclesPerSample=(1<<(audioCh4FrequencyClock+1))*AUDIO_CH4_RATIO_CYCLES[audioCh4FrequencyRatio];
    	int cyclesPhase, samplesPhase, samplesStartPhase, samplesEndPhase;
        
        // Compute the new value of each counter.
        {
	        int ci, co;
	        cyclesPhase=audioCh4FrequencyCounter;
	        samplesStartPhase=audioCh4PolynomialCounter&waveMask;
	        samplesPhase=(samplesStartPhase+1)&waveMask;
	        ci=audioCh4FrequencyCounter+cycles; co=ci/cyclesPerSample; audioCh4FrequencyCounter=ci-(co*cyclesPerSample);
	        ci=audioCh4PolynomialCounter+co; audioCh4PolynomialCounter=ci&waveMask;
	        samplesEndPhase=audioCh4PolynomialCounter;
		}
		
        // Integrate.
        int value;
        {
        	int cyclesHead, cyclesTail;
        	int samplesHead, samplesTail;
        	int samples, waves;
	    	int t;
        	
	    	// Number of heading cycles.
	    	cyclesHead=cyclesPerSample-cyclesPhase;
	    	if (cyclesHead>=cycles) cyclesHead=cycles;
	    	// Number of full samples.
	        t=cycles-cyclesHead;
	        samples=t/cyclesPerSample;
	        // Number of tailing cycles.
	        cyclesTail=t-samples*cyclesPerSample;
	        
	        // Number of heading samples.
	        samplesHead=waveLength-samplesPhase;
	        if (samplesHead>=samples) samplesHead=samples;
	        // Number of full waves.
	        t=samples-samplesHead;
	        waves=t>>waveBits;
	        // Number of tailing samples.
	        samplesTail=t-(waves<<waveBits);
        
	        t=(waveSum[samplesPhase+samplesHead]-waveSum[samplesPhase]); // Head samples.
	        t+=waveSum[waveLength]*waves; // Full waves.
	        t+=waveSum[samplesTail]; // Tail samples.
	        value=t*cyclesPerSample;
	        value+=(waveSum[samplesStartPhase+1]-waveSum[samplesStartPhase])*cyclesHead; // Head cycles.
	    	value+=(waveSum[samplesEndPhase+1]-waveSum[samplesEndPhase])*cyclesTail; // Tail cycles.
        }
        
        // Shift to center square wave around 0, scale by volume and add.
    	audioCh4Sample+=(value-(cycles-value))*audioCh4EnvelopeVolume;
    }
    
    
    private void audioCh4Update() {
        int c=CLOCK_FREQUENCY_DMG;

        // Length.
        if (audioCh4LengthUsed!=0) {
            if (audioCh4LengthCycles>audioCh4NextUpdateCyclesCounter) {
                audioCh4LengthCycles-=audioCh4NextUpdateCyclesCounter;
                if (c>audioCh4LengthCycles) c=audioCh4LengthCycles;
            } else {
                audioCh4LengthCycles=0;
                audioCh4Enabled=0;
            }
        }
        
        // Envelope.
        if (audioCh4EnvelopeSweepsStepLength>0) {
            if (audioCh4EnvelopeSweepsStepCycles>audioCh4NextUpdateCyclesCounter) {
                audioCh4EnvelopeSweepsStepCycles-=audioCh4NextUpdateCyclesCounter;
            } else {
                audioCh4EnvelopeSweepsStepCycles=audioCh4EnvelopeSweepsStepLength*AUDIO_ENVELOPE_SWEEP_STEP_CYCLES;                
                if (audioCh4EnvelopeSweepsDirection!=0) {
                    if (audioCh4EnvelopeVolume<0x0f) audioCh4EnvelopeVolume++;
                } else {
                    if (audioCh4EnvelopeVolume>0x00) audioCh4EnvelopeVolume--;
                }
            }
            if (c>=audioCh4EnvelopeSweepsStepCycles) c=audioCh4EnvelopeSweepsStepCycles;
        }

        audioCh4NextUpdateCycles=c; audioCh4NextUpdateCyclesCounter=0;
    }

    /*
    // DO NOT DELETE !
    // Routine to integrate a wave (with a precomputed table).
    // It is not used but it is kept here as a reminder.
    // cycles: length of the integration in cycles.
    // phase: phase in cycles in a single wave.
    // waveSum: summed wave. Must have length: waveLength+1. waveSum[waveLength] is the total sum of all the samples in the wave.
    // waveLength: length of the wave in samples.
    // waveCyclesPerSample: number of cycles in one sample. 
    // Notes:
    // Wave :   |               0               |               1               |               2               |
    // Sample : |   0   |   1   |   2   |   3   |   0   |   1   |   2   |   3   |   0   |   1   |   2   |   3   |
    // Cycle:   |0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|0|1|2|3|
    // cycles:            |*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|*|
    // samples:                 |*******|*******|*******|*******|*******|*******|*******|*******|*******|
    // waves:                                   |*******************************|
    private static int audioIntegrateWave(int cycles, int phase, short waveSum[], int waveLength, int cyclesPerSample) {
    	int cyclesPhase, cyclesHead, cyclesTail;
    	int samplesPhase, samplesHead, samplesTail, samples;
    	int waves;
    	int t;

    	int samplesStartPhase, samplesEndPhase;
    	// Phase in samples of the first sample.
    	t=phase/cyclesPerSample;
    	samplesStartPhase=t%waveLength;
    	// Phase in cycles from the beginning of the first sample.
    	cyclesPhase=phase-t*cyclesPerSample;
    	// Phase in samples of the second sample.
        samplesPhase=(samplesStartPhase+1)%waveLength;
    	// Phase in samples of the last sample.
    	t=(phase+cycles)/cyclesPerSample;
    	samplesEndPhase=t%waveLength;

    	// Number of heading cycles.
    	cyclesHead=cyclesPerSample-cyclesPhase;
    	if (cyclesHead>=cycles) cyclesHead=cycles;
    	// Number of full samples.
        t=cycles-cyclesHead;
        samples=t/cyclesPerSample;
        // Number of tailing cycles.
        cyclesTail=t-samples*cyclesPerSample;
        
    	// Phase in samples.
//        t=(phase+cyclesHead)/waveCyclesPerSample;
//        samplesPhase=t%waveLength;

        // Number of heading samples.
        samplesHead=waveLength-samplesPhase;
        if (samplesHead>=samples) samplesHead=samples;
        // Number of full waves.
        t=samples-samplesHead;
        waves=t/waveLength;
        // Number of tailing samples.
        samplesTail=t-waves*waveLength;

        // Check if cycles==cyclesHead+(samplesHead+waves*waveLength+samplesTail)*waveCyclesPerSample+cyclesTail.
//        t=cyclesHead+(samplesHead+waves*waveLength+samplesTail)*waveCyclesPerSample+cyclesTail;
//        if (t!=cycles) {
//        	System.out.println("Argh !");
//        }
        
        t=(waveSum[samplesPhase+samplesHead]-waveSum[samplesPhase]); // Head samples.
        t+=waveSum[waveLength]*waves; // Full waves.
        t+=waveSum[samplesTail]; // Tail samples.
        t*=cyclesPerSample;
        t+=(waveSum[samplesStartPhase+1]-waveSum[samplesStartPhase])*cyclesHead; // Head cycles.
    	t+=(waveSum[samplesEndPhase+1]-waveSum[samplesEndPhase])*cyclesTail; // Tail cycles.
        
    	return t;
    }
	*/
    
    //--------------------------------------------------------------------------------
    // ODMA.
    //--------------------------------------------------------------------------------
    protected byte odmaAddress; // DMA (0xff46).
    protected boolean odmaStartedFlag; // Indicates that OAM DMA is in progress.
    private final EmulationEvent odmaEvent=new EmulationEvent(this, "Video OAM DMA") {
        public void processEvent() { odmaStartedFlag=false; }
        public void breakEvent() {}
    };

    private void odmaReset() {
        system.removeEvent(odmaEvent);
        odmaAddress=0x00;        
    }
    
    // Perform OAM DMA.
    private void odmaPerformTransfer() {
        odmaStartedFlag=true;
        odmaEvent.clockCycle=TimeStamp.add(clockCurrentCycle, 160*4);
        clock.addEvent(odmaEvent);
        int srcAddress=(odmaAddress<<8)&0xffff;
        if (srcAddress>=0xe000) srcAddress-=0x2000; // Not sure of it but we shadow work RAM. 
        for (int i=0; i<160; i++) videoOam[i]=(byte)cpu.cpuRead8(srcAddress+i);
    }
        
    //--------------------------------------------------------------------------------
    // HDMA.
    //--------------------------------------------------------------------------------
    // TODO: use an event for HDMA completion.
    
    protected byte hdmaNotActiveFlag; // HDMA5[7].
    protected int hdmaSrcAddress; // HDMA1[0-7] and HDMA2[0-7].
    protected int hdmaDstAddress; // HDMA3[0-7] and HDMA4[0-7].
    protected int hdmaLength; // HDMA5[0-6].
    protected boolean hdmaGMAStarted;
    
    private final EmulationEvent hdmaEvent=new EmulationEvent(this, "HDMA") {
        public void processEvent() {}
        public void breakEvent() {}
    };
    
    private void hdmaReset() {
        hdmaNotActiveFlag=1;
        hdmaSrcAddress=0;
        hdmaDstAddress=0;
        hdmaLength=0;
    }
    
    private void hdmaSetHDMA5(int data) {
        // TODO: In the Virtual Game Boy history, they say that HDMA can be stopped by writting $ff while HDMA is running. Check this.
        hdmaLength=data&0x7f;
        if ((data&0x80)!=0) {
        	hdmaNotActiveFlag=0; // Start HDMA.
        	if ((videoLCDStatus&0x3)==VIDEO_MODE_HBLANK) hdmaPerformHDMA(); // HBLANK DMA can be manually started in the HBLANK period.
        } else {
            if (hdmaNotActiveFlag!=0) {
                // Start GDMA.
            	hdmaGMAStarted=true;
            	hdmaPerformGDMA();
            } else hdmaNotActiveFlag=1; // Stop HDMA.
        }
    }
    
    private void hdmaPerformHDMA() {
        if ((hdmaSrcAddress<0x7fff) || (hdmaSrcAddress>=0xa000)&&(hdmaSrcAddress<0xdfff)) {
            int dstAddress=(videoRamBank<<13);
            for (int i=0; i<16; i++) videoRam[dstAddress+((hdmaDstAddress+i)&0x1fff)]=(byte)cpu.cpuRead8(hdmaSrcAddress+i);
        }
        hdmaSrcAddress=(hdmaSrcAddress+16)&0xfff0;
        hdmaDstAddress=(hdmaDstAddress+16)&0x1ff0;
        if (hdmaLength==0x00) {
            hdmaLength=0x7f;
            hdmaNotActiveFlag=1;
        } else {
            hdmaLength--;
        }    
    }
    
    private void hdmaPerformGDMA() {
    	// TODO: GDMA must halt the CPU and transfer can only occur during HBLANK or VBLANK.
        int dstAddress=(videoRamBank<<13);
        int n=(hdmaLength+1)<<4;
        for (int i=0; i<n; i++) videoRam[dstAddress+((hdmaDstAddress+i)&0x1fff)]=(byte)cpu.cpuRead8(hdmaSrcAddress+i);
        hdmaSrcAddress=(hdmaSrcAddress+n)&0xfff0;
        hdmaDstAddress=(hdmaDstAddress+n)&0x1ff0;
        hdmaLength=0x7f;
        hdmaNotActiveFlag=1;    	
    }
    
    //--------------------------------------------------------------------------------
    // Graphics.
	//--------------------------------------------------------------------------------
    /**
     * Screen size.
     */
    public static final int SCREEN_WIDTH=160, SCREEN_HEIGHT=144;

    // Video memory (0x8000 - 0x9FFF). 
    protected final byte[] videoRam=new byte[2*0x2000];

	// Objects memory (0xfe00 - 0xfe9f).
    protected static final int VIDEO_NB_SPRITES=40;
    protected final byte[] videoOam=new byte[VIDEO_NB_SPRITES*4];
    
    protected byte videoLCDControl; // LCDC register (0xff40).
    protected static final short
    	HWR_LCDC_DE=		0x80, // [1] Display enabled.
    	HWR_LCDC_WTMS=		0x40, // [1] Window tile map select.
    	HWR_LCDC_WDE=		0x20, // [1] Window display enabled.
    	HWR_LCDC_WBTDS=		0x10, // [1] Window and background tile data select.
    	HWR_LCDC_BTMS=		0x08, // [1] Background tile map select.
    	HWR_LCDC_OS=		0x04, // [1] Objects size.
    	HWR_LCDC_ODE=		0x02, // [1] Object display enabled.
    	HWR_LCDC_BDE=		0x01; // [1] Background display enabled.

    protected byte videoLCDStatus; // LCDS register (0xff41)).
    protected static final short
    	HWR_LCDS_LIE=		0x40, // [1] Line interrupt enabled.
    	HWR_LCDS_OIE=		0x20, // [1] OAM interrupt enabled.
    	HWR_LCDS_VIE=		0x10, // [1] V-Blank interrupt enabled.
    	HWR_LCDS_HIE=		0x08, // [1] H-Blank interrupt enabled.
    	HWR_LCDS_LCF=		0x04, // [1] Line coincidence flag.
    	HWR_LCDS_LM=		0x03; // [2] LCD mode.

 	// Video modes.
    protected static final byte
    	VIDEO_MODE_HBLANK=		0,
    	VIDEO_MODE_VBLANK=		1,
    	VIDEO_MODE_OAM=			2,
    	VIDEO_MODE_LCD=			3;
 	
    // Video states.
    private static final byte
    	VIDEO_STATE_OAM=              			1,
    	VIDEO_STATE_LCD=               			2,
    	VIDEO_STATE_LCD_DRAW=					3,
    	VIDEO_STATE_HBLANK=        				4,
    	VIDEO_STATE_LINE_END=          			5,
    	VIDEO_STATE_VBLANK=            			6,
    	VIDEO_STATE_VBLANK_IT=         			7,
    	VIDEO_STATE_VBLANK_LINE_START= 			8,
    	VIDEO_STATE_VBLANK_LINE_END=   			9,
    	VIDEO_STATE_VBLANK_LAST_LINE_START=     10,
    	VIDEO_STATE_VBLANK_LAST_LINE_MIDDLE=    11;
    	
    protected byte videoRamBank; // VBK (0xff4f).

    protected short videoCurrentLine; // LY register (0xff44). Current video line.
    protected short videoLineCompare; // LYC register (0xff45). Compare video line.
    protected byte videoState; // Video state (internal).
     	
    protected short videoBackgroundY, videoBackgroundX; // SCY (0xff42) and SCX (0xff43) registers.
    protected short videoWindowY, videoWindowX; // WY (0xff50) and WX (0xff51) registers.
//	public short videoWindowYLatched; // (internal).
    private boolean videoDrawBackgroundFlag, videoDrawWindowFlag, videoDrawSpritesFlag; // (internal).

    protected byte videoBCPS, videoBCPD; // BCPS register (0xff68) and BCPD register (0xff69).
    protected byte videoBPData; // BGP register (0xff47). Background and window palette.
    protected final short videoBPColors[]=new short[4]; // Background and window colors in Game Boy mode (internal).
	protected final short videoBCPData[][]=new short[8][4]; // Background and window color palettes in Game Boy Color format.
	protected final int videoIBCPData[][]=new int[8][4]; // Background and window color palettes in Game Boy Color format (internal).

	protected byte videoOCPS, videoOCPD; // OCPS (0xff6a),  OCPD (0xff6b).
	protected final byte videoOPData[]=new byte[2]; // OBP0 and OBP1 registers (0xff48 and 0xff49). Object palette 0 & 1.
	protected final short videoOPColors[][]=new short[2][4]; // Objects colors in Game Boy mode.
	protected final short videoOCPData[][]=new short[8][4]; // Color object palettes in Game Boy Color format.
	protected final int videoIOCPData[][]=new int[8][4]; // Color object palettes in our internal format.

	// Event when H-Blank begins.
	private final EmulationEvent videoLineEvent=new EmulationEvent(this, "Video line") {
	    public final void processEvent() { clockCurrentCycle=clockCycle; videoUpdate(); }
        public final void breakEvent() {}
	};
	
    // Video output Flag.
    private boolean videoOutputEnabled;
	// Output video image.
	private int videoOutputImage[];
	// Priority buffer.
	private final short videoPriorityBuffer[]=new short[SCREEN_WIDTH];
	// Active sprites list.
//	private byte videoActiveSpritesListHead=-1;
//	private byte videoActiveSpritesList[]=new byte[40];
	
	// If no palette is provided, this one is used.
    private static final int DEFAULT_PALETTE[]={ 0xffffffff, 0xffaaaaaa, 0xff555555, 0xff000000 };
	
    /**
     * Sets the video output image.
     * @param image an array of type <code>int</code> and of size SCREEN_WIDTH*SCREEN_HEIGHT
     */
	public void setVideoOutputImage(int image[]) {
	    videoOutputImage=image;
	}

	/**
	 * Enables video output.
	 * @param flag if <code>false</code>, the video image will not be generated
	 */
    public void enableVideoOutput(boolean flag) {
        videoOutputEnabled=flag;
    }
    
    /**
     * Returns whether video output is enabled.
     * @return <code>false</code> if video output is disabled, <code>true</code> otherwise
     */
    public boolean isVideoOutputEnabled() {
        return videoOutputEnabled;
    }

    /**
     * Returns the current video line.
     * @return the current video line from 0 to 153
     */
    public int getCurrentVideoLine() {
    	return videoCurrentLine;
    }
    
	// Reset the video subsystem.
	private void videoReset() {
        system.removeEvent(videoLineEvent);

        for (int i=0; i<2*0x2000; i++) videoRam[i]=0;
        for (int i=0; i<4*VIDEO_NB_SPRITES; i++) videoOam[i]=0;
        
	    videoLCDControl=(byte)0x00;
	    videoLCDStatus=(byte)0x00;
		videoRamBank=0;
		videoCurrentLine=0; videoLineCompare=0;
        videoState=VIDEO_STATE_OAM;
		videoBackgroundY=0; videoBackgroundX=0;
		videoWindowY=0; videoWindowX=0; // videoWindowYLatched=0;

		// Reset the palettes.
		for (int i=0; i<8; i++) {
		    for (int j=0; j<4; j++) {
		        int c32=DEFAULT_PALETTE[j];
		        short c16=(short)videoColor32To15(c32);
				videoBCPData[i][j]=c16;
				videoIBCPData[i][j]=c32;
				videoOCPData[i][j]=c16;
				videoIOCPData[i][j]=c32;
		    }
		}
		
        videoBPData=0x00;
        videoSetBCPS(0x00);
        videoOPData[0]=0x00; videoOPData[1]=0x00;
        videoSetOCPS(0x00);
        
		videoClearOutputImage();
		
		videoUpdate();
	}
	
    private void videoSetLCDC(int data) {
        int oldLCDC=videoLCDControl;
        videoLCDControl=(byte)data;
        if (((data^oldLCDC)&HWR_LCDC_DE)!=0) {
        	if ((data&HWR_LCDC_DE)!=0) videoClearOutputImage();
            videoCurrentLine=0;
            videoState=VIDEO_STATE_OAM;
            videoUpdate();
        }
    }
    
    private void videoSetSTAT(int data) {
        int mode=videoLCDStatus&0x03;
        videoLCDStatus=(byte)((data&0x78) | (videoLCDStatus&0x07));
        if ((videoLCDControl&HWR_LCDC_DE)!=0) {
            boolean triggerInterrupt=false;
            // DMG STAT write bug: write STAT in HBLANK or VBLANK always generates an interrupt.
            if (globalGameBoyModel!=MODEL_GAME_BOY_COLOR && (mode&0x02)==0) triggerInterrupt=true;
            // Trigger interrupt depending of current mode.
			switch (mode) {
            case 0: if ((videoLCDStatus&HWR_LCDS_HIE)!=0) triggerInterrupt=true; break;
            case 1: if ((videoLCDStatus&HWR_LCDS_VIE)!=0) triggerInterrupt=true; break;
            case 2: if ((videoLCDStatus&HWR_LCDS_OIE)!=0) triggerInterrupt=true; break;
            case 3: break;
            }
            // Trigger the LCDC interrupt.
            if (triggerInterrupt) interruptsTrigger(INTERRUPT_LCDC);
        }
    }
    
    private void videoSetLYC(int data) {
        data&=0xff; videoLineCompare=(short)data;
        if (videoCurrentLine==data) {
            videoLCDStatus|=HWR_LCDS_LCF;
            if ((videoLCDStatus&HWR_LCDS_LIE)!=0) interruptsTrigger(INTERRUPT_LCDC);
        } else videoLCDStatus&=~HWR_LCDS_LCF;
    }

    // Update the video subsystem.
    // This routine is always called ((144*3)+10)*60=26520 times per second when video is enabled.
    // The events order is:
    // (0)OAM->LCD->HBLANK-> ... (143)OAM->LCD->HBLANK-> (144)VBLANK-> ... (153)VBLANK-> (0)OAM->LCD->HBLANK-> ...
    private void videoUpdate() {
        int newLCDStatus=videoLCDStatus&0x78, mode=videoLCDStatus&0x03; // Clear the video mode and the line coincidence flag.
        int ly=videoCurrentLine;
        boolean triggerLCDCInterruptFlag=false, checkLyInterruptFlag=false;
        int nextUpdateCycle=0;    
        boolean exit=false;

        do {
            switch (videoState) {
            case VIDEO_STATE_OAM:
                if ((newLCDStatus&HWR_LCDS_OIE)!=0) triggerLCDCInterruptFlag=true; // Trigger OAM interrupt if enabled.
                mode=VIDEO_MODE_OAM; videoState=VIDEO_STATE_LCD; nextUpdateCycle=80; exit=true;
                break;
            case VIDEO_STATE_LCD:
                mode=VIDEO_MODE_LCD; videoState=VIDEO_STATE_LCD_DRAW; nextUpdateCycle=86; exit=true;
                break;
            case VIDEO_STATE_LCD_DRAW:
            	// We draw the line in the middle to avoid a lot of problems with various games.
                if ((videoLCDControl&HWR_LCDC_DE)!=0 && videoOutputEnabled) videoDrawLine(ly); // Draw the next line.
                videoState=VIDEO_STATE_HBLANK; nextUpdateCycle=86; exit=true;
            	break;
            case VIDEO_STATE_HBLANK:
//                if (videoOutputEnabled) videoDrawLine(ly); // Draw the previous line.
                if (hdmaNotActiveFlag==0) hdmaPerformHDMA(); // Perform HDMA if enabled.
                if ((newLCDStatus&HWR_LCDS_HIE)!=0) triggerLCDCInterruptFlag=true; // Trigger horizontal blanking interrupt if enabled.
                mode=VIDEO_MODE_HBLANK; videoState=VIDEO_STATE_LINE_END; nextUpdateCycle=204; exit=true;
                break;
            case VIDEO_STATE_LINE_END:
            	if ((videoLCDControl&HWR_LCDC_DE)!=0) ly++; // Increment current line if display enabled.
                if (ly<144) { videoState=VIDEO_STATE_OAM; checkLyInterruptFlag=true; } // Why checking LY interrupt only when LY<144 ?
                else videoState=VIDEO_STATE_VBLANK;
                break;
            case VIDEO_STATE_VBLANK:
                if (globalListener!=null) globalListener.onEndOfVideoFrame(globalEvent);
                mode=VIDEO_MODE_VBLANK; videoState=VIDEO_STATE_VBLANK_IT; nextUpdateCycle=20; exit=true;
                break;
            case VIDEO_STATE_VBLANK_IT:
                interruptsTrigger(INTERRUPT_VBLANK);
                checkLyInterruptFlag=true; // Delayed ? Why ?
                if ((newLCDStatus&HWR_LCDS_VIE)!=0) triggerLCDCInterruptFlag=true;
                videoState=VIDEO_STATE_VBLANK_LINE_END; nextUpdateCycle=436; exit=true;
                break;
            case VIDEO_STATE_VBLANK_LINE_START:
                videoState=VIDEO_STATE_VBLANK_LINE_END; nextUpdateCycle=456; exit=true;
                break;
            case VIDEO_STATE_VBLANK_LINE_END:
            	ly++; checkLyInterruptFlag=true;
            	videoState=(ly<153) ? VIDEO_STATE_VBLANK_LINE_START : VIDEO_STATE_VBLANK_LAST_LINE_START;
                break;
            case VIDEO_STATE_VBLANK_LAST_LINE_START:
            	videoState=VIDEO_STATE_VBLANK_LAST_LINE_MIDDLE; nextUpdateCycle=56; exit=true;
            	break;
            case VIDEO_STATE_VBLANK_LAST_LINE_MIDDLE:
            	ly=0; checkLyInterruptFlag=true;
            	videoState=VIDEO_STATE_OAM; nextUpdateCycle=400; exit=true;
            	break;
            }
        } while (!exit);

        if (ly==videoLineCompare) {
        	// TODO: no LY interrupts after line 145 (cf gnuboy) ?
            newLCDStatus|=HWR_LCDS_LCF; // Set the line coincidence flag.
            if (checkLyInterruptFlag && ((newLCDStatus&HWR_LCDS_LIE)!=0)) triggerLCDCInterruptFlag=true; // Trigger line coincidence interrupt if enabled.
        }
        
        // Update current line.
        videoCurrentLine=(short)ly;
        // Update LCD status.
        videoLCDStatus=(byte)(newLCDStatus|mode);
        // LCDC interrupt.
        if (triggerLCDCInterruptFlag && (videoLCDControl&HWR_LCDC_DE)!=0) interruptsTrigger(INTERRUPT_LCDC);
        // Next event.
        if (clockDoubleSpeedFlag!=0) nextUpdateCycle<<=1;
        videoLineEvent.clockCycle=TimeStamp.add(clockCurrentCycle, nextUpdateCycle);
        clock.addEvent(videoLineEvent);
    }
    		
    // Convert a 15-bit color to a 32-bit color.
    private int videoColor15To32(int c15) {
        int b=((c15>>10)&0x1f)*255/31, g=((c15>>5)&0x1f)*255/31, r=(c15&0x1f)*255/31;
        return 0xff000000 | (r<<16) | (g<<8) | b;
    }

    // Convert a 32-bit color to a 15-bit color.
    private int videoColor32To15(int c32) {
        int r=(c32>>19)&0x1f, g=(c32>>11)&0x1f, b=(c32>>3)&0x1f;
        return (b<<10) | (g<<5) | r;
    }

    // Set the background palette in original Game Boy mode.
    private void videoSetBackgroundPalette(int data) {
        videoBPData=(byte)data;
        for (int i=0, e=0; i<4; i++, e+=2) {
            short t=videoBPColors[(data>>e)&0x03];
            videoBCPData[0][i]=t;
            videoIBCPData[0][i]=videoColor15To32(t);
        }
    }

    // Set the background color palette selection register.
    private void videoSetBCPS(int data) {
        int number=(data>>3)&0x07, entry=(data>>1)&0x3, halve=data&0x01;
        videoBCPS=(byte)(data&0xbf);
        videoBCPD=(byte)((videoBCPData[number][entry]>>(halve<<3))&0xff);
    }
    
    // Set a background palette byte in Game Boy Color mode.
    private void videoSetBCPD(int data) {
        int s=videoBCPS, increment=(s>>7)&0x01, number=(s>>3)&0x07, entry=(s>>1)&0x3, halve=s&0x01;
        int d=videoBCPData[number][entry];
        d=(halve!=0) ? (((data&0xff)<<8) | (d&0x00ff)) : ((d&0xff00) | (data&0xff));
        videoBCPData[number][entry]=(short)d; videoIBCPData[number][entry]=videoColor15To32(d);
        
        // Auto-increment if enabled.
        if (increment!=0) {
            s=(s&0x80) | ((s+1)&0x3f); number=(s>>3)&0x07; entry=(s>>1)&0x3; halve=s&0x01;
            videoBCPS=(byte)s;
            videoBCPD=(byte)((videoBCPData[number][entry]>>(halve<<3))&0xff);
        } else videoBCPD=(byte)data;
    }

    // Set the object palette in original Game Boy mode.
    private void videoSetObjectPalette(int data, int palette) {
       videoOPData[palette]=(byte)data;
        for (int i=0, e=0; i<4; i++, e+=2) {
            short t=videoOPColors[palette][(data>>e)&0x03];
            videoOCPData[palette][i]=t;
            videoIOCPData[palette][i]=videoColor15To32(t);
        }
    }

    // Set the object color palette selection register.
    private void videoSetOCPS(int data) {
        int number=(data>>3)&0x07, entry=(data>>1)&0x3, halve=data&0x01;
        videoOCPS=(byte)(data&0xbf);
        videoOCPD=(byte)((videoOCPData[number][entry]>>(halve<<3))&0xff);
    }
    
    // Set an object palette byte in Game Boy Color mode.
    private void videoSetOCPD(int data) {
        int s=videoOCPS, increment=(s>>7)&0x01, number=(s>>3)&0x07, entry=(s>>1)&0x3, halve=s&0x01;
        int d=videoOCPData[number][entry];
        d=(halve!=0) ? (((data&0xff)<<8) | (d&0x00ff)) : ((d&0xff00) | (data&0xff));
        videoOCPData[number][entry]=(short)d; videoIOCPData[number][entry]=videoColor15To32(d);
        
        // Auto-increment if enabled.
        if (increment!=0) {
            s=(s&0x80) | ((s+1)&0x3f); number=(s>>3)&0x07; entry=(s>>1)&0x3; halve=s&0x01;
            videoOCPS=(byte)s;
            videoOCPD=(byte)((videoOCPData[number][entry]>>(halve<<3))&0xff);
        } else videoOCPD=(byte)data;
    }
    
    private void videoClearOutputImage() {
    	if (videoOutputImage==null) return;
        int outputAddress=0;
        for (int y=0; y<SCREEN_HEIGHT; y++, outputAddress+=SCREEN_WIDTH) {
            for (int x=0; x<SCREEN_WIDTH; x++) videoOutputImage[outputAddress+x]=0xffffffff;
        }
    }
        
    // Draw the current video line.
    private void videoDrawLine(int y) {
        if (videoOutputImage==null) return;

        boolean lowPriority=false;
        videoDrawBackgroundFlag=(videoLCDControl&HWR_LCDC_BDE)!=0;
        videoDrawWindowFlag=(videoLCDControl&HWR_LCDC_WDE)!=0;
        videoDrawSpritesFlag=(videoLCDControl&HWR_LCDC_ODE)!=0;

        if (globalColorModeEnabled) {
            // CGB with color features enabled.
            lowPriority=!videoDrawBackgroundFlag;
            videoDrawBackgroundFlag=true;
        } else if (globalGameBoyModel==MODEL_GAME_BOY_COLOR) {
            // CGB with color features disabled.
            if (!videoDrawBackgroundFlag) videoDrawWindowFlag=false;
        }

        // FIXME: in normal GB mode, the window is always above sprites !
        if (videoDrawBackgroundFlag) videoDrawTileMapLine(y, false, lowPriority);
        else videoClearBackgroundLine(y);
        if (videoDrawWindowFlag) videoDrawTileMapLine(y, true, lowPriority);
        
        if (videoDrawSpritesFlag) videoDrawSpritesLine(y);
    }
    
 	private void videoDrawTileMapLine(int y, boolean window, boolean lowPriority) {
        int tileIndexSwap, tileAddress, tileIndex, tileAttributes;
        int characters, characterAddress; 
        int outputAddress;
        int x, bx, by, tpx, tpy, tpdx, w, w0, tx;
        int palette[];
        int pixel0, pixel1;
        int pixel, priorityShift;
        
        if (window) {
            // Window tile map.
            by=y-(videoWindowY&0xff);
            if (by<0) return;
            x=(videoWindowX&0xff)-7;
            if (x<0) x=0;
            bx=0;
            w0=SCREEN_WIDTH-x;
            tileAddress=((videoLCDControl&HWR_LCDC_WTMS)==0) ? 0x1800 : 0x1c00;
        } else {
            // Background tile map.
            w0=SCREEN_WIDTH;
            if (videoDrawWindowFlag) {
                // Draw only until the window to avoid overdraw.
                if ((y-(videoWindowY&0xff))>=0) {
                    w0=(videoWindowX&0xff)-7;
                    if (w0>SCREEN_WIDTH) w0=SCREEN_WIDTH;
                }
            }
            x=0;
            bx=videoBackgroundX&0xff; by=(videoBackgroundY+y)&0xff;
            tileAddress=((videoLCDControl&HWR_LCDC_BTMS)==0) ? 0x1800 : 0x1c00;
        }
        tileAddress+=(by>>3)<<5;
                
        // Character data.
        if ((videoLCDControl&HWR_LCDC_WBTDS)!=0) { characters=0x0000; tileIndexSwap=0x00; }
        else { characters=0x0800; tileIndexSwap=0x80; }
        
        outputAddress=(y*SCREEN_WIDTH)+x;
        
        while (w0>0) {
            // Find tile.
            tileIndex=(videoRam[tileAddress+(bx>>3)]&0xff)^tileIndexSwap;
            characterAddress=characters+(tileIndex<<4);
            tileAttributes=(globalColorModeEnabled) ? videoRam[0x2000+tileAddress+(bx>>3)] : 0;
            if (lowPriority) tileAttributes&=0x7f;
            if ((tileAttributes&0x08)!=0) characterAddress+=0x2000; // Bank 1.

            // Flip X.
            tpx=bx&0x7;
            if ((tileAttributes&0x20)!=0) { tpdx=1; }
            else { tpx^=0x07; tpdx=-1; }
    
            // Flip Y.
            tpy=by&0x7;
            if ((tileAttributes&0x40)!=0) { tpy^=0x07; }
            characterAddress+=tpy<<1;

            // Clip.
            w=8-(bx&0x7);
            if (w>w0) w=w0;
            w0-=w;
            bx=(bx+w)&0xff;
            
            // Draw.
            palette=videoIBCPData[tileAttributes&0x07];
            priorityShift=((tileAttributes&0x80)!=0) ? 6 : 2;
            pixel0=videoRam[characterAddress]; pixel1=videoRam[characterAddress+1];
            for (tx=0; tx<w; tx++, tpx+=tpdx) {
                pixel=(((pixel1>>tpx)&0x01)<<1) | ((pixel0>>tpx)&0x01);
                videoPriorityBuffer[x+tx]=(short)(pixel<<priorityShift);
                videoOutputImage[outputAddress+tx]=palette[pixel];
            }
            
            outputAddress+=w; x+=w;
        }
    }

    private void videoDrawSpritesLine(int y) {
        boolean doubleSize;
        int tileIndexMask;
        int x;
        int tileAttributes;
        int characterAddress, spriteAddress; 
        int sx, sy, sh;
        int tpx, tpy, tpw, tpdx;
        int palette[];
        int outputAddress;
        int pixel0, pixel1;
        int pixel, priorityShift;
    
        // In 8x16 sprite mode, the lower bit of each sprite tile index is ignored.
        if ((videoLCDControl&HWR_LCDC_OS)!=0) {
            doubleSize=true; sh=16; tileIndexMask=0xfe;
        } else {
            doubleSize=false; sh=8; tileIndexMask=0xff;
        }
        
        for (int i=VIDEO_NB_SPRITES-1; i>=0; i--) {
            spriteAddress=i<<2;
            sx=(videoOam[spriteAddress+1]&0xff)-8; sy=(videoOam[spriteAddress]&0xff)-16;
            if ((sx<=-8) || (sx>=160) || (y<sy) || (y>=(sy+sh))) continue;
             
            characterAddress=(videoOam[spriteAddress+2]&tileIndexMask)<<4;
            tileAttributes=videoOam[spriteAddress+3]&0xff;
            if (globalColorModeEnabled) {
                if ((tileAttributes&0x08)!=0) characterAddress+=0x2000; // Bank 1.
            } else {
                tileAttributes=(tileAttributes&0xe0) | ((tileAttributes>>4)&0x01);                 
            }
            
            // Clip X.
            tpx=sx; tpw=sx+8;
            if (tpx<0) tpx=0; x=tpx;
            if (tpw>SCREEN_WIDTH) tpw=SCREEN_WIDTH;
            tpw-=tpx; tpx-=sx;
                
            outputAddress=y*SCREEN_WIDTH+x;
            
            // Flip X.
            if ((tileAttributes&0x20)!=0) { tpdx=1; }
            else { tpx^=0x07; tpdx=-1; }
    
            // Flip Y.
            tpy=y-sy;
            if ((tileAttributes&0x40)!=0) tpy^=(doubleSize) ? 0x0f : 0x07;
            characterAddress+=tpy<<1;
    
            // Draw.
            palette=videoIOCPData[tileAttributes&0x07];
            priorityShift=((tileAttributes&0x80)!=0) ? 0 : 4;
            pixel0=videoRam[characterAddress]; pixel1=videoRam[characterAddress+1];
            for (sx=0; sx<tpw; sx++, tpx+=tpdx) {
                pixel=(((pixel1>>tpx)&0x01)<<1) | ((pixel0>>tpx)&0x01);
                if ((pixel<<priorityShift)>videoPriorityBuffer[x+sx]) {
                    videoOutputImage[outputAddress+sx]=palette[pixel];
                }
            }
        }
    }

    private void videoClearBackgroundLine(int y) {
        int outputAddress, x, w0;
        
        w0=SCREEN_WIDTH;
        if (videoDrawWindowFlag) {
            // Draw only until the window to avoid overdraw.
            if ((y-(videoWindowY&0xff))>=0) {
                w0=(videoWindowX&0xff)-7;
                if (w0>SCREEN_WIDTH) w0=SCREEN_WIDTH;
            }
        }

        outputAddress=y*SCREEN_WIDTH;
        for (x=0; x<w0; x++) {
            videoOutputImage[outputAddress+x]=0xffffffff;
            videoPriorityBuffer[x]=0x00;
        }
    }


    //--------------------------------------------------------------------------------
    // CPU.
    //--------------------------------------------------------------------------------
    private boolean cpuAreasDecoderRebuildFlag=true;
	private final byte cpuAreasDecoderR[]=new byte[256];
    private final byte cpuAreasDecoderW[]=new byte[256];
    
    // Reset the CPU.
    private void cpuReset() {
        cpu.reset();
        cpuStart();
    }
    
    private void cpuStart() {
        cpuEvent.clockCycle=clockCurrentCycle;
        clock.addEvent(cpuEvent);    	
    }
    
    private void cpuStop() {
    	system.removeEvent(cpuEvent);
    }
    
    private void cpuUpdate() {
        long cycles=system.getRemainingCycles(clock.frequency);
        if (cycles<1) cycles=1;
        if (cycles>Integer.MAX_VALUE) cycles=Integer.MAX_VALUE;
        cycles=cpu.execute((int)cycles);
        cpuEvent.clockCycle=TimeStamp.add(cpuEvent.clockCycle, cycles);
        clock.addEvent(cpuEvent);
    }
    
    // Update current time with the CPU time.
    private void cpuSetCurrentCycle() {
        clockCurrentCycle=TimeStamp.add(cpuEvent.clockCycle, cpu.cycles);
    }
    
    // Get the current CPU time.
    public long cpuGetCurrentCycle() {
        return TimeStamp.add(cpuEvent.clockCycle, cpu.cycles);
    }
    
    // Event.
    private final EmulationEvent cpuEvent=new EmulationEvent(this, "CPU") {
        public final void processEvent() { cpuUpdate(); }
        
        public final void breakEvent() {
            if (cpu.running) cpu.breakExecution=true;
        }
    };
    
    // CPU.
    public final CPU cpu=new CPU() {

        public final int cpuRead8(int address) {
            int offset, data;
            address&=0xffff;
            
            switch (cpuAreasDecoderR[address>>8]) {
            case AREA_ILLEGAL:
                break;
            case AREA_ROM: break; // Should not occur.
            case AREA_CROM:
            	return (internalROMDisabled || (address>0x00ff)) ? 0xff : internalROM[address&0xff]&0xff;
            case AREA_TILE_DATA: // No break !
            case AREA_TILE_INDEX:
                return videoRam[(videoRamBank<<13)+(address&0x1fff)]&0xff;
            case AREA_CRAM:
            	return 0xff;
            case AREA_WRAM_B0:
                return workRam[address&0x0fff]&0xff;
            case AREA_WRAM_BN:
                return workRam[(workRamBank<<12)+(address&0x0fff)]&0xff;
            case AREA_OAM:
                offset=address&0xff;
                return (offset<0xa0) ? videoOam[offset]&0xff : 0xff;
            case AREA_IO:
                return hwrRead(address)&0xff;
            // Common to some cartridges.
            case AREA_MBCX_ROM_B0:
            	return (internalROMDisabled || (address>0x00ff)) ? cartridgeRom[address&0x3fff]&0xff : internalROM[address&0xff]&0xff;
            case AREA_MBCX_ROM_BN:
                return cartridgeRom[(cartridgeRealRomBank<<14)+(address&0x3fff)]&0xff;
            case AREA_MBCX_RAM:
                if (cartridgeRamEnabled) {
                    offset=(cartridgeRamSize<0x2000) ? (address&0x7ff) : (address&0x1fff); // For 2KB RAM.
                    data=cartridgeRam[(cartridgeRealRamBank<<13)+offset]&0xff;
                } else data=0xff;
                return data;
            // No MBC.
            case AREA_NOMBC_ROM:
            	return (internalROMDisabled || (address>0x00ff)) ? cartridgeRom[address&0x7fff]&0xff : internalROM[address&0xff]&0xff;            
            // MBC1.
            case AREA_MBC1_RAM_ENABLE: break; // Should not occur.
            case AREA_MBC1_ROM_BANK: break; // Should not occur.
            case AREA_MBC1_RAM_BANK: break; // Should not occur.
            case AREA_MBC1_MODEL: break; // Should not occur.
            // MBC2.
            case AREA_MBC2_RAM_ENABLE: break; // Should not occur.
            case AREA_MBC2_ROM_BANK: break; // Should not occur.
            case AREA_MBC2_RAM:
                return (cartridgeRamEnabled) ? cartridgeRam[address&0x01ff]&0xff : 0xff;
            // MBC3.
            case AREA_MBC3_RAM_ENABLE: break; // Should not occur.
            case AREA_MBC3_ROM_BANK: break; // Should not occur.
            case AREA_MBC3_RAM_BANK: break; // Should not occur.
            case AREA_MBC3_RTC: break; // Should not occur.
            // MBC5.
            case AREA_MBC5_RAM_ENABLE: break; // Should not occur.
            case AREA_MBC5_ROM_BANK0: break; // Should not occur.
            case AREA_MBC5_ROM_BANK1: break; // Should not occur.
            case AREA_MBC5_RAM_BANK: break; // Should not occur.
            }
            
//            if ((debugFlags&DEBUG_IO)!=0) debug("Illegal area read access.");
            return 0xff;
        }
        
        public final void cpuWrite8(int address, int data) {
            int offset;
            data&=0xff;
            address&=0xffff;
            
            switch (cpuAreasDecoderW[address>>8]) {
            case AREA_ILLEGAL:
                break;
            case AREA_ROM: break; // Should not occur.
            case AREA_CROM: break;
            case AREA_TILE_DATA: // No break !
            case AREA_TILE_INDEX:
                videoRam[(videoRamBank<<13)+(address&0x1fff)]=(byte)data;
                return;
            case AREA_CRAM: break;
            case AREA_WRAM_B0:
                workRam[address&0x0fff]=(byte)data;
                return;
            case AREA_WRAM_BN:
                workRam[(workRamBank<<12)+(address&0x0fff)]=(byte)data;
                return;
            case AREA_OAM: // FIXME: check if OAM address range fea0-feff can be written. 
                offset=address&0xff;
                if (offset<0xa0) videoOam[offset]=(byte)data;
                return;
            case AREA_IO:
                hwrWrite(address, data);
                return;
            // Common to some cartridges.
            case AREA_MBCX_ROM_B0: break; // Should not occur.
            case AREA_MBCX_ROM_BN: break; // Should not occur.
            case AREA_MBCX_RAM:
                if (cartridgeRamEnabled) {
                    offset=(cartridgeRamSize<0x2000) ? (address&0x7ff) : (address&0x1fff); // For 2KB RAM.
                    cartridgeRam[(cartridgeRealRamBank<<13)+offset]=(byte)data;
                }
                return;
            // No MBC.
            case AREA_NOMBC_ROM: break; // Should not occur.
            // MBC1.
            case AREA_MBC1_RAM_ENABLE:
                cartridgeRamEnabled=((data&0x0f)==0x0a); return;
            case AREA_MBC1_ROM_BANK:
                data&=0x1F; if (data==0) data=1;
                cartridgeRomBank=((cartridgeRomBank&0x60) | data);
                cartridgeRealRomBank=(cartridgeRomBank<cartridgeNbRomBanks) ? cartridgeRomBank : cartridgeRomBank&(cartridgeNbRomBanks-1);
                return;
            case AREA_MBC1_RAM_BANK:
                if (cartridgeMbc1MemoryModel==CARTRIDGE_MBC1_4x32) {
                    cartridgeRamBank=(data&0x03);
                    cartridgeRealRamBank=(cartridgeRamBank<cartridgeNbRamBanks) ? cartridgeRamBank : cartridgeRamBank&(cartridgeNbRamBanks-1);
                } else {
                    cartridgeRomBank=((data&0x03)<<5) | (cartridgeRomBank&0x1f);
                    cartridgeRealRomBank=(cartridgeRomBank<cartridgeNbRomBanks) ? cartridgeRomBank : cartridgeRomBank&(cartridgeNbRomBanks-1);
                }
                return;
            case AREA_MBC1_MODEL:
                cartridgeMbc1MemoryModel=(data&0x01);
                return;
            // MBC2.
            case AREA_MBC2_RAM_ENABLE:
                if ((address&0x0100)==0) cartridgeRamEnabled=((data&0x0f)==0x0a);
                return;
            case AREA_MBC2_ROM_BANK:
                if ((address&0x0100)!=0) {
                    cartridgeRomBank=data&0x1f;
                    if (cartridgeRomBank==0) cartridgeRomBank=1;
                    cartridgeRealRomBank=(cartridgeRomBank<cartridgeNbRomBanks) ? cartridgeRomBank : cartridgeRomBank&(cartridgeNbRomBanks-1);
                }
                return;
            case AREA_MBC2_RAM:
                if (cartridgeRamEnabled) cartridgeRam[address&0x01ff]=(byte)data;
                return;
            // MBC3.
            case AREA_MBC3_RAM_ENABLE:
                cartridgeRamEnabled=((data&0x0f)==0x0a);
                return;
            case AREA_MBC3_ROM_BANK:
                data&=0x7F; if (data==0) data=1;
                cartridgeRomBank=data;
                cartridgeRealRomBank=(cartridgeRomBank<cartridgeNbRomBanks) ? cartridgeRomBank : cartridgeRomBank&(cartridgeNbRomBanks-1);
                return;
            case AREA_MBC3_RAM_BANK:
                cartridgeRamBank=(data&0x0f);
                cartridgeRealRamBank=(cartridgeRamBank<cartridgeNbRamBanks) ? cartridgeRamBank : cartridgeRamBank&(cartridgeNbRamBanks-1);
                return;
            case AREA_MBC3_RTC:
                // TODO: implement RTC.
                return;
/*            case AREA_MBC3_RAM:
                if ((cartridgeRamBank&0x08)!=0) {
                    switch (cartridgeRamBank-0x08) {
                    case 0x00:
                    case 0x01:
                    case 0x02:
                    case 0x03:
                    case 0x04:
                    }
                } else {
                    if (cartridgeRamEnabled) {
                        offset=(cartridgeRamSize<0x2000) ? (address&0x7ff) : (address&0x1fff); // For 2KB RAM.
                        cartridgeRam[((cartridgeRamBank&0x03)<<13)+offset]=(byte)data;
                    }
                }
                return;
*/
            // MBC5.
            case AREA_MBC5_RAM_ENABLE:
                cartridgeRamEnabled=((data&0x0f)==0x0a);
                return;
            case AREA_MBC5_ROM_BANK0:
                cartridgeRomBank=(cartridgeRomBank&0x0100) | data;
                cartridgeRealRomBank=(cartridgeRomBank<cartridgeNbRomBanks) ? cartridgeRomBank : cartridgeRomBank&(cartridgeNbRomBanks-1);
                return;
            case AREA_MBC5_ROM_BANK1:
                cartridgeRomBank=((data&0x01)<<8) | (cartridgeRomBank&0x00ff);
                cartridgeRealRomBank=(cartridgeRomBank<cartridgeNbRomBanks) ? cartridgeRomBank : cartridgeRomBank&(cartridgeNbRomBanks-1);
                return;
            case AREA_MBC5_RAM_BANK:
                cartridgeRamBank=((cartridgeFeatures&CartridgeInfos.CTF_RUMBLE)!=0) ? (data&0x07) : (data&0x0f);
                cartridgeRealRamBank=(cartridgeRamBank<cartridgeNbRamBanks) ? cartridgeRamBank : cartridgeRamBank&(cartridgeNbRamBanks-1);
                return;
            }

//            if ((debugFlags&DEBUG_IO)!=0) debug("Illegal area write access.");
        }
        
        // Acknowledge an interrupt. Returns the address of the interrupt routine.
        public final int acknowledgeInterrupt() {
            int address, isf=interruptEnableFlags&interruptRequestFlags, i;
            for (i=0; i<=4; i++) if ((isf&(1<<i))!=0) break;
            address=0x0040+(i<<3);
            interruptRequestFlags-=(1<<i);
            cpu.requestInterrupt((interruptRequestFlags&interruptEnableFlags)!=0);
            return address;
        }
        
        public final boolean stop() {
            if (globalColorModeEnabled && (clockDoubleSpeedSwitchFlag!=0)) { clockSwitchSpeed(); return false; }
            else return true;
        }
    };
    
    //--------------------------------------------------------------------------------
    // Interrupts.
    //--------------------------------------------------------------------------------
    protected byte interruptRequestFlags; // Register IF[0-4] (0xff0f).
    protected byte interruptEnableFlags; // Register IE[0-4] (0xffff).

    // Interrupt flags.
    protected static final byte
    	INTERRUPT_VBLANK=  0x01, // Vertical blank interrupt.
    	INTERRUPT_LCDC=    0x02, // LCD Coincidence interrupt.
    	INTERRUPT_TIMER=   0x04, // Timer interrupt.
    	INTERRUPT_SERIAL=  0x08, // Serial interrupt.
    	INTERRUPT_PAD=     0x10; // Pad interrupt.

    private void interruptsReset() {
        interruptRequestFlags=0x00;
        interruptEnableFlags=0x00;
    }

    private void interruptsUpdate() {
        cpu.requestInterrupt((interruptRequestFlags&interruptEnableFlags)!=0);
    }
    
    private void interruptsTrigger(int interrupt) {
        interruptRequestFlags|=interrupt;
        cpu.requestInterrupt((interruptRequestFlags&interruptEnableFlags)!=0);
    }
    
    //********************************************************************************
	// Hardware registers access.
	//********************************************************************************
    // FIXME: check if other addresses can be read/written.
    // TODO: implement debug read & write for:
    // - Color palette registers.
    
    // Hardware registers page.
    protected final byte hwRegisters[]=new byte[0x0100];
    // For IO address decoding.
    private boolean hwrAddressDecoderRebuildFlag=true;
    private final byte hwrAddressDecoder[]=new byte[0x0100];
    
    private int hwrRead(int address) {
        int t;
        cpuSetCurrentCycle();
        
 		switch (hwrAddressDecoder[address&0x00ff]) {

 		case HWR_IF: return interruptRequestFlags;
 		case HWR_IE: return interruptEnableFlags;

 		case HWR_KEY1: return (clockDoubleSpeedFlag<<7) | clockDoubleSpeedSwitchFlag;

 		case HWR_SVBK:  return hwRegisters[0x70]; // return workRamBank;

 		case HWR_DIV: timerUpdate(); return timerDivider;
 		case HWR_TIMA: timerUpdate(); return timerCounter;
 		case HWR_TMA: return timerReloadValue;
 		case HWR_TAC: return (timerStartedFlag<<2) | (timerClockSelect);
 		
 		case HWR_P1: return padP1;
 		
 		case HWR_SB: return serialData;
 		case HWR_SC: return serialControl;
 		
 		case HWR_RP: return (infraredEnableFlag<<6) | (infraredInput<<1) | infraredOutput;
 		
 		case HWR_NR10: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh1FrequencySweepsDuration<<4) | (audioCh1FrequencySweepsDirection<<3) | audioCh1FrequencySweepsNumber | 0x80; } else return 0xff;
 		case HWR_NR11: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh1Duty<<6) | 0x3f; } else return 0xff;
 		case HWR_NR12: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh1EnvelopeVolumeInitialValue<<4) | (audioCh1EnvelopeSweepsDirection<<3) | audioCh1EnvelopeSweepsStepLength; } else return 0xff;
 		case HWR_NR13: return 0xff;
 		case HWR_NR14: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh1LengthUsed<<6) | 0xbf; } else return 0xff;
 		
 		case HWR_NR21: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh2Duty<<6) | 0x3f; } else return 0xff;
 		case HWR_NR22: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh2EnvelopeVolumeInitialValue<<4) | (audioCh2EnvelopeSweepsDirection<<3) | audioCh2EnvelopeSweepsStepLength; } else return 0xff;
 		case HWR_NR23: return 0xff;
 		case HWR_NR24: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh2LengthUsed<<6) | 0xbf; } else return 0xff;
 		
 		case HWR_NR30: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh3SoundOnFlag<<7) | 0x7f; } else return 0xff;
 		case HWR_NR31: return 0xff;
 		case HWR_NR32: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh3OutputLevel<<5) | 0x9f; } else return 0xff;
 		case HWR_NR33: return 0xff;
 		case HWR_NR34: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh3LengthUsed<<6) | 0xbf; } else return 0xff;
 		
 		case HWR_NR41: return 0xff;
 		case HWR_NR42: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh4EnvelopeVolumeInitialValue<<4) | (audioCh4EnvelopeSweepsDirection<<3) | audioCh4EnvelopeSweepsStepLength; } else return 0xff;
 		case HWR_NR43: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh4FrequencyClock<<4) | (audioCh4PolynomialCounterSteps<<3) | audioCh4FrequencyRatio; } else return 0xff;
 		case HWR_NR44: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioCh4LengthUsed<<6) | 0xbf; } else return 0xff;
 		
 		case HWR_NR50: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioSO2VinFlag<<7) | (audioSO2OutputLevel<<4) | (audioSO1VinFlag<<3) | audioSO1OutputLevel; } else return 0xff;
 		case HWR_NR51: if (audioGlobalEnableFlag!=0) { audioUpdate(); return audioChannelToSOFlags; } else return 0xff;
 		case HWR_NR52: if (audioGlobalEnableFlag!=0) { audioUpdate(); return (audioGlobalEnableFlag<<7) | (audioCh4Enabled<<3) | (audioCh3Enabled<<2) | (audioCh2Enabled<<1) | audioCh1Enabled; } else return 0xff;
 		
 		case HWR_WAVERAM: t=(address&0x0f)<<1; return (audioCh3WaveData[t]<<4) | (audioCh3WaveData[t+1]);
 		
 		case HWR_LCDC: return videoLCDControl;
 		case HWR_STAT: return videoLCDStatus;
 		case HWR_LY: return videoCurrentLine;
 		case HWR_LYC: return videoLineCompare;
 		case HWR_DMA: return odmaAddress; // FIXME: write only ?
 		case HWR_VBK: return videoRamBank;
 		
 		case HWR_SCY: return videoBackgroundY;
 		case HWR_SCX: return videoBackgroundX;
 		case HWR_WY: return videoWindowY;
 		case HWR_WX: return videoWindowX;
 		
 		case HWR_BGP: return videoBPData; // FIXME: write only ?
 		case HWR_BCPS: return videoBCPS;
 		case HWR_BCPD: return videoBCPD;
 		case HWR_OBP0: return videoOPData[0]; // FIXME: write only ?
 		case HWR_OBP1: return videoOPData[1]; // FIXME: write only ?
 		case HWR_OCPS: return videoOCPS;
 		case HWR_OCPD: return videoOCPD;

 		case HWR_HDMA1: return (hdmaSrcAddress>>8); // FIXME: write only ?
 		case HWR_HDMA2: return hdmaSrcAddress; // FIXME: write only ?
 		case HWR_HDMA3: return (hdmaDstAddress>>8); // FIXME: write only ?
 		case HWR_HDMA4: return hdmaDstAddress; // FIXME: write only ?
 		case HWR_HDMA5: return (hdmaNotActiveFlag<<7) | hdmaLength; // TODO: write only ?

 		case HWR_IRAM: return hwRegisters[address&0xff];
 		case HWR_IROM: return 0; // FIXME: to implement. Write only ?
 		
 		default:
/* 		    if ((debugFlags&DEBUG_IO)!=0) {
                debugString.setLength(0);
                debugString.append("Illegal read access at $");
                Tools.appendWord16(debugString, address);
            }
            */
 			break;
 		}

 		return 0xff; // For non readable addresses.
 	}
  
 	private void hwrWrite(int address, int data) {
        cpuSetCurrentCycle();

 		switch (hwrAddressDecoder[address&0x00ff]) {

 		case HWR_IF: interruptRequestFlags=(byte)(data&0x1f); interruptsUpdate(); return;
 		case HWR_IE: interruptEnableFlags=(byte)(data&0x1f); interruptsUpdate(); return;

 		case HWR_KEY1: clockDoubleSpeedSwitchFlag=(byte)(data&0x01); break;

 		case HWR_SVBK:
 		    data&=0x07;
 		    workRamBank=(byte)((data<2) ? 1 : data); 
   			hwRegisters[0x70]=(byte)data;
   			return;

 		case HWR_DIV: timerUpdate(); timerDivider=0; timerDividerCyclesRemainder=0; return;
 		case HWR_TIMA: timerUpdate(); timerCounter=(short)(data&0xff); timerScheduleEvent(); return;
 		case HWR_TMA: timerUpdate(); timerReloadValue=(short)(data&0xff); timerScheduleEvent(); return;
 		case HWR_TAC: timerUpdate(); timerSetTAC(data); return; 		    
 		
 		case HWR_P1: padRefresh(data, padKeys); return;

 		case HWR_SB: serialData=(byte)data; return;
 		case HWR_SC: serialSetSC(data); return;

 		case HWR_RP: infraredSetRP(data); return;

 		case HWR_NR10: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh1SetNR10(data); }; return;
 		case HWR_NR11: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh1SetNR11(data); }; return;
 		case HWR_NR12: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh1SetNR12(data); }; return;
 		case HWR_NR13: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh1SetNR13(data); }; return;
 		case HWR_NR14: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh1SetNR14(data); }; return;
 		
        case HWR_NR21: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh2SetNR21(data); }; return;
        case HWR_NR22: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh2SetNR22(data); }; return;
        case HWR_NR23: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh2SetNR23(data); }; return;
        case HWR_NR24: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh2SetNR24(data); }; return;
         		
        case HWR_NR30: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh3SetNR30(data); }; return;
        case HWR_NR31: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh3SetNR31(data); }; return;
        case HWR_NR32: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh3SetNR32(data); }; return;
        case HWR_NR33: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh3SetNR33(data); }; return;
        case HWR_NR34: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh3SetNR34(data); }; return;
        
        case HWR_NR41: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh4SetNR41(data); }; return;
        case HWR_NR42: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh4SetNR42(data); }; return;
        case HWR_NR43: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh4SetNR43(data); }; return;
        case HWR_NR44: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh4SetNR44(data); }; return;
        
 		case HWR_NR50: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioSetNR50(data); }; return;
 		case HWR_NR51: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioChannelToSOFlags=(byte)data; } return;
 		case HWR_NR52: audioUpdate(); audioGlobalEnableFlag=(byte)((data>>7)&0x01); return;
 		
 		case HWR_WAVERAM: if (audioGlobalEnableFlag!=0) { audioUpdate(); audioCh3SetWaveRam(address&0x0f, data); } return;

 		case HWR_LCDC: videoSetLCDC(data); return;
 		case HWR_STAT: videoSetSTAT(data); return;
 		case HWR_LY: return;
 		case HWR_LYC: videoSetLYC(data); return;
 		case HWR_DMA: odmaAddress=(byte)data; odmaPerformTransfer(); return;
 		case HWR_VBK: videoRamBank=(byte)(data&0x01); return;

 		case HWR_SCY: videoBackgroundY=(byte)data; return;
 		case HWR_SCX: videoBackgroundX=(byte)data; return;
 		case HWR_WY: videoWindowY=(byte)data; return;
 		case HWR_WX: videoWindowX=(byte)data; return;

 		case HWR_BGP: if (!globalColorModeEnabled) videoSetBackgroundPalette(data); return;
 		case HWR_BCPS: videoSetBCPS(data); return;
 		case HWR_BCPD: videoSetBCPD(data); return;
 		case HWR_OBP0: if (!globalColorModeEnabled) videoSetObjectPalette(data, 0); return;
 		case HWR_OBP1: if (!globalColorModeEnabled) videoSetObjectPalette(data, 1); return;
 		case HWR_OCPS: videoSetOCPS(data); return;
 		case HWR_OCPD: videoSetOCPD(data); return;

 		case HWR_HDMA1: hdmaSrcAddress=((data&0xff)<<8) | (hdmaSrcAddress&0x00f0); return;
 		case HWR_HDMA2: hdmaSrcAddress=(hdmaSrcAddress&0xff00) | (data&0xf0); return;
 		case HWR_HDMA3: hdmaDstAddress=((data&0x1f)<<8) | (hdmaDstAddress&0x00f0); return;
 		case HWR_HDMA4: hdmaDstAddress=(hdmaDstAddress&0x1f00) | (data&0xf0); return;
 		case HWR_HDMA5: hdmaSetHDMA5(data); return;
 		
 		case HWR_IRAM: hwRegisters[address&0xff]=(byte)data; return;
 		case HWR_IROM: if ((data&0x01)!=0) internalROMDisabled=true; return;
 		
 		default:
 			/*
 		    if ((debugFlags&DEBUG_IO)!=0) {
                debugString.setLength(0);
                debugString.append("Illegal write access at $");
                Tools.appendWord16(debugString, address);
                debugString.append(" with value $");
                Tools.appendWord8(debugString, address);
            }
            */
 			return;
 		}
 	}
    
    //--------------------------------------------------------------------------------
    // Builder for CPU address space decoding.
    //--------------------------------------------------------------------------------
    // Build the decoder.
    private static void buildCpuDecoder(byte areasR[], byte areasW[], int controller, int features) {
    	for (int i=0; i<256; i++) { areasR[i]=AREA_ILLEGAL; areasW[i]=AREA_ILLEGAL; }

    	int n=ADRESS_AREA_TABLE.length;
    	for (int i=0; i<n; i++) {
    		int area[]=ADRESS_AREA_TABLE[i];
    		int name=area[ATF_NAME];
    		int ac=area[ATF_CONTROLLER], af=area[ATF_FEATURES];
    		if ((ac==controller || ac==CartridgeInfos.CTC_ANY) && ((af&features)==af)) {
    			int address=area[ATF_ADDRESS]>>8, size=area[ATF_SIZE]>>8, access=area[ATF_ACCESS_MODE];
//  			if ((access&AM_RO)!=0) for (int a=0; a<size; a++) { if (areasR[address+a]==AREA_ILLEGAL) areasR[address+a]=(byte)name; }
//  			if ((access&AM_WO)!=0) for (int a=0; a<size; a++) { if (areasW[address+a]==AREA_ILLEGAL) areasW[address+a]=(byte)name; }
    			if ((access&AM_RO)!=0) for (int a=0; a<size; a++) { areasR[address+a]=(byte)name; }
    			if ((access&AM_WO)!=0) for (int a=0; a<size; a++) { areasW[address+a]=(byte)name; }
    		}
    	}
    }

    private static final byte
		// System areas.
		AREA_ILLEGAL=         	0, // An illegal area.
		AREA_ROM=         		1, // Internal ROM area.
		AREA_CROM=            	2, // RW: 0x0000-0x7fff. Cartridge ROM.
		AREA_TILE_DATA=       	3, // RW: 0x8000-0x97ff. Tile data.
		AREA_TILE_INDEX=      	4, // RW: 0x9800-0x9fff. Tile maps.
		AREA_CRAM=            	5, // RW: 0xa000-0xbfff. Cartridge RAM.
		AREA_WRAM_B0=         	6, // RW: 0xc000-0xcfff. Work RAM bank 0.
		AREA_WRAM_BN=         	7, // RW: 0xd000-0xdfff. Work RAM bank N.
		AREA_OAM=             	8, // RW: 0xfe00-0xfeff. OAM.
		AREA_IO=             	9, // RW: 0xff00-0xffff. IO.
		// Common to some cartridges.
		AREA_MBCX_ROM_B0=		10, // RO: 0x0000-0x3fff. All controllers.
		AREA_MBCX_ROM_BN=       11, // RO: 0x4000-0x7fff. All controllers.
		AREA_MBCX_RAM=			12, // RW: 0xa000-0xbfff. MBC1, MBC3, MBC5.
		// No MBC.
		AREA_NOMBC_ROM=			13, // WO: 0x0000-0x7fff.
		// MBC1 only.
		AREA_MBC1_RAM_ENABLE=   14, // WO: 0x0000-0x1fff.
		AREA_MBC1_ROM_BANK=     15, // WO: 0x2000-0x3fff.
		AREA_MBC1_RAM_BANK=     16, // WO: 0x4000-0x5fff.
		AREA_MBC1_MODEL=        17, // WO: 0x6000-0x7fff.
		// MBC2 only.
		AREA_MBC2_RAM_ENABLE=   18, // WO: 0x0000-0x1fff.
		AREA_MBC2_ROM_BANK=     19, // WO: 0x2000-0x3fff.
		AREA_MBC2_RAM=          20, // RW: 0xa000-0xbfff.
		// MBC3 only.
		AREA_MBC3_RAM_ENABLE=   21, // WO: 0x0000-0x1fff.
		AREA_MBC3_ROM_BANK=     22, // WO: 0x2000-0x3fff.
		AREA_MBC3_RAM_BANK=     23, // WO: 0x4000-0x5fff. 
		AREA_MBC3_RTC=          24, // WO: 0x6000-0x7fff.
		// MBC5 only.
		AREA_MBC5_RAM_ENABLE=   25, // WO: 0x0000-0x1fff.
		AREA_MBC5_ROM_BANK0=    26, // WO: 0x2000-0x2fff.
		AREA_MBC5_ROM_BANK1=    27, // WO: 0x3000-0x3fff.
		AREA_MBC5_RAM_BANK=     28; // WO: 0x4000-0x5fff.
    
//  Area access Mode.
    private static final byte AM_RO=0x01, AM_WO=0x02, AM_RW=0x03;

//  Table fields.
    private static final int
	    ATF_NAME=                 0,
	    ATF_ADDRESS=              1,
	    ATF_SIZE=                 2,
	    ATF_ACCESS_MODE=          3,
	    ATF_CONTROLLER=           4, // Cartridge controller.
	    ATF_FEATURES=             5; // Cartridge features.

    private static final int ADRESS_AREA_TABLE[][]={
    	// Common.
//  	{ AREA_ROM,                 0x0000, 0x00ff, AM_RO, CTC_ANY,     0 }, // Not used !
    	{ AREA_CROM,                0x0000, 0x8000, AM_RW, CartridgeInfos.CTC_NONE,    0 }, // When there is no cartridge.
    	{ AREA_TILE_DATA,           0x8000, 0x1800, AM_RW, CartridgeInfos.CTC_ANY,     0 },
    	{ AREA_TILE_INDEX,          0x9800, 0x0800, AM_RW, CartridgeInfos.CTC_ANY,     0 },
    	{ AREA_CRAM,                0xa000, 0x2000, AM_RW, CartridgeInfos.CTC_NONE,    0 }, // When there is no cartridge.
    	{ AREA_WRAM_B0,             0xc000, 0x1000, AM_RW, CartridgeInfos.CTC_ANY,     0 },
    	{ AREA_WRAM_BN,             0xd000, 0x1000, AM_RW, CartridgeInfos.CTC_ANY,     0 },
    	{ AREA_WRAM_B0,             0xe000, 0x1e00, AM_RW, CartridgeInfos.CTC_ANY,     0 }, // Echo.
    	{ AREA_OAM,                 0xfe00, 0x0100, AM_RW, CartridgeInfos.CTC_ANY,     0 },
    	{ AREA_IO,                  0xff00, 0x0100, AM_RW, CartridgeInfos.CTC_ANY,     0 },
    	// No MBC.
    	{ AREA_NOMBC_ROM,           0x0000, 0x8000, AM_RO, CartridgeInfos.CTC_NOMBC,   CartridgeInfos.CTF_ROM },
    	{ AREA_MBCX_RAM,            0xa000, 0x2000, AM_RW, CartridgeInfos.CTC_NOMBC,   CartridgeInfos.CTF_RAM },
    	// MBC1.
    	{ AREA_MBCX_ROM_B0,         0x0000, 0x4000, AM_RO, CartridgeInfos.CTC_MBC1,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBCX_ROM_BN,         0x4000, 0x4000, AM_RO, CartridgeInfos.CTC_MBC1,    CartridgeInfos.CTF_ROM },        
    	{ AREA_MBC1_RAM_ENABLE,     0x0000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC1,    CartridgeInfos.CTF_RAM },
    	{ AREA_MBC1_ROM_BANK,       0x2000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC1,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBC1_RAM_BANK,       0x4000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC1,    CartridgeInfos.CTF_RAM },
    	{ AREA_MBC1_MODEL,          0x6000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC1,    CartridgeInfos.CTF_RAM },
    	{ AREA_MBCX_RAM,            0xa000, 0x2000, AM_RW, CartridgeInfos.CTC_MBC1,    CartridgeInfos.CTF_RAM },
    	// MBC2.
    	{ AREA_MBCX_ROM_B0,         0x0000, 0x4000, AM_RO, CartridgeInfos.CTC_MBC2,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBCX_ROM_BN,         0x4000, 0x4000, AM_RO, CartridgeInfos.CTC_MBC2,    CartridgeInfos.CTF_ROM },        
    	{ AREA_MBC2_RAM_ENABLE,     0x0000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC2,    CartridgeInfos.CTF_RAM },
    	{ AREA_MBC2_ROM_BANK,       0x2000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC2,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBC2_RAM,            0xa000, 0x2000, AM_RW, CartridgeInfos.CTC_MBC2,    CartridgeInfos.CTF_RAM },
    	// MBC3.
    	{ AREA_MBCX_ROM_B0,         0x0000, 0x4000, AM_RO, CartridgeInfos.CTC_MBC3,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBCX_ROM_BN,         0x4000, 0x4000, AM_RO, CartridgeInfos.CTC_MBC3,    CartridgeInfos.CTF_ROM },        
    	{ AREA_MBC3_RAM_ENABLE,     0x0000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC3,    CartridgeInfos.CTF_RAM },
    	{ AREA_MBC3_ROM_BANK,       0x2000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC3,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBC3_RAM_BANK,       0x4000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC3,    CartridgeInfos.CTF_RAM },
    	{ AREA_MBC3_RTC,            0x6000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC3,    CartridgeInfos.CTF_RTC },
    	{ AREA_MBCX_RAM,            0xa000, 0x2000, AM_RW, CartridgeInfos.CTC_MBC3,    CartridgeInfos.CTF_RAM },
    	// MBC5.
    	{ AREA_MBCX_ROM_B0,         0x0000, 0x4000, AM_RO, CartridgeInfos.CTC_MBC5,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBCX_ROM_BN,         0x4000, 0x4000, AM_RO, CartridgeInfos.CTC_MBC5,    CartridgeInfos.CTF_ROM },        
    	{ AREA_MBC5_RAM_ENABLE,     0x0000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC5,    CartridgeInfos.CTF_RAM },
    	{ AREA_MBC5_ROM_BANK0,      0x2000, 0x1000, AM_WO, CartridgeInfos.CTC_MBC5,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBC5_ROM_BANK1,      0x3000, 0x1000, AM_WO, CartridgeInfos.CTC_MBC5,    CartridgeInfos.CTF_ROM },
    	{ AREA_MBC5_RAM_BANK,       0x4000, 0x2000, AM_WO, CartridgeInfos.CTC_MBC5,    CartridgeInfos.CTF_RAM },
    	{ AREA_MBCX_RAM,            0xa000, 0x2000, AM_RW, CartridgeInfos.CTC_MBC5,    CartridgeInfos.CTF_RAM },
    };


    //--------------------------------------------------------------------------------
    // Builder for harware addresses.
    //--------------------------------------------------------------------------------
    // Hardware registers. 
    private static final short
	    // Interrupt Flag register.
	    HWR_IF=              0,
	    // Interrupt enable.
	    HWR_IE=              1,
	    // CPU speed select.
	    HWR_KEY1=            2,
	    // RAM bank select.
	    HWR_SVBK=            3,
	    // Timer registers.
	    HWR_DIV=             4,
	    HWR_TIMA=            5,
	    HWR_TMA=             6,
	    HWR_TAC=             7,
	    // Joypad register.
	    HWR_P1=              8,
	    // Serial transfer registers.
	    HWR_SB=              9,
	    HWR_SC=              10,
	    // Infrared.
	    HWR_RP=              11,
	    // Audio registers.
	    HWR_NR10=            12,
	    HWR_NR11=            13,
	    HWR_NR12=            14,
	    HWR_NR13=            15,
	    HWR_NR14=            16,
	    HWR_NR21=            17,
	    HWR_NR22=            18,
	    HWR_NR23=            19,
	    HWR_NR24=            20,
	    HWR_NR30=            21,
	    HWR_NR31=            22,
	    HWR_NR32=            23,
	    HWR_NR33=            24,
	    HWR_NR34=            25,
	    HWR_NR41=            26,
	    HWR_NR42=            27,
	    HWR_NR43=            28,
	    HWR_NR44=            29,
	    HWR_NR50=            30,
	    HWR_NR51=            31,
	    HWR_NR52=            32,
	    HWR_WAVERAM=         33,
	    // Video registers.
	    HWR_LCDC=            34,
	    HWR_STAT=            35,
	    HWR_LY=              36,
	    HWR_LYC=             37,
	    HWR_DMA=             38,
	    HWR_VBK=             39, // VRAM bank select.
	    HWR_SCY=             40,
	    HWR_SCX=             41,
	    HWR_WY=              42,
	    HWR_WX=              43,
	    HWR_BGP=             44,
	    HWR_BCPS=            45,
	    HWR_BCPD=            46,
	    HWR_OBP0=            47,
	    HWR_OBP1=            48,
	    HWR_OCPS=            49,
	    HWR_OCPD=            50,
	    // HDMA.
	    HWR_HDMA1=           51,
	    HWR_HDMA2=           52,
	    HWR_HDMA3=           53,
	    HWR_HDMA4=           54,
	    HWR_HDMA5=           55,
	    // Internal RAM.
	    HWR_IRAM=            56,  
	    // Internal ROM.
	    HWR_IROM=            57, // FIXME: Game Boy patent indicates address ff00 instead !
	    // Illegal.
	    HWR_ILLEGAL=         58;   

    // Build an array containing the decoded address.
    private static void buildHWRDecoder(byte addressDecoder[], int model) {
    	int n=HWR_ADDRESS_AREAS_MAP.length;
    	int baseAddress=0xff00, areaSize=0x0100;

    	// Initialize.
    	for (int i=0; i<areaSize; i++) addressDecoder[i]=HWR_ILLEGAL;

    	// Build the decoder.
    	for (int i=0; i<n; i++) {
    		int area[]=HWR_ADDRESS_AREAS_MAP[i];
    		if ((area[HAT_MODELS]&(1<<model))!=0) {
    			int a=area[HAT_ADDRESS]-baseAddress, l=area[HAT_LENGTH];
    			int name=area[HAT_NAME];
    			for (int j=0; j<l; j++) addressDecoder[a+j]=(byte)name;
    		}
    	}
    }

    // Models mask.
    private static final short
	    MDMG=                  1<<GameBoy.MODEL_GAME_BOY,
	//  MMGB=                  1<<GameBoy.MODEL_GAME_BOY_POCKET,
	//  MSGB=                  1<<GameBoy.MODEL_SUPER_GAME_BOY,
	    MCGB=                  1<<GameBoy.MODEL_GAME_BOY_COLOR;

    // Address areas map fields.
    private static final int
	    HAT_ADDRESS=     0,
	    HAT_LENGTH=      1,
	    HAT_NAME=        2,
	    HAT_MODELS=      3;

    // List of registers.
    private static final int HWR_ADDRESS_AREAS_MAP[][]={
    	{   0xff00, 0x0001, HWR_P1,         MDMG |  MCGB    },

    	{   0xff01, 0x0001, HWR_SB,         MDMG |  MCGB    },
    	{   0xff02, 0x0001, HWR_SC,         MDMG |  MCGB    },

    	{   0xff04, 0x0001, HWR_DIV,        MDMG |  MCGB    },
    	{   0xff05, 0x0001, HWR_TIMA,       MDMG |  MCGB    },
    	{   0xff06, 0x0001, HWR_TMA,        MDMG |  MCGB    },
    	{   0xff07, 0x0001, HWR_TAC,        MDMG |  MCGB    },

    	{   0xff0f, 0x0001, HWR_IF,         MDMG |  MCGB    },

    	{   0xff10, 0x0001, HWR_NR10,       MDMG |  MCGB    },
    	{   0xff11, 0x0001, HWR_NR11,       MDMG |  MCGB    },
    	{   0xff12, 0x0001, HWR_NR12,       MDMG |  MCGB    },
    	{   0xff13, 0x0001, HWR_NR13,       MDMG |  MCGB    },
    	{   0xff14, 0x0001, HWR_NR14,       MDMG |  MCGB    },

    	{   0xff16, 0x0001, HWR_NR21,       MDMG |  MCGB    },
    	{   0xff17, 0x0001, HWR_NR22,       MDMG |  MCGB    },
    	{   0xff18, 0x0001, HWR_NR23,       MDMG |  MCGB    },
    	{   0xff19, 0x0001, HWR_NR24,       MDMG |  MCGB    },

    	{   0xff1a, 0x0001, HWR_NR30,       MDMG |  MCGB    },
    	{   0xff1b, 0x0001, HWR_NR31,       MDMG |  MCGB    },
    	{   0xff1c, 0x0001, HWR_NR32,       MDMG |  MCGB    },
    	{   0xff1d, 0x0001, HWR_NR33,       MDMG |  MCGB    },
    	{   0xff1e, 0x0001, HWR_NR34,       MDMG |  MCGB    },

    	{   0xff20, 0x0001, HWR_NR41,       MDMG |  MCGB    },
    	{   0xff21, 0x0001, HWR_NR42,       MDMG |  MCGB    },
    	{   0xff22, 0x0001, HWR_NR43,       MDMG |  MCGB    },
    	{   0xff23, 0x0001, HWR_NR44,       MDMG |  MCGB    },

    	{   0xff24, 0x0001, HWR_NR50,       MDMG |  MCGB    },
    	{   0xff25, 0x0001, HWR_NR51,       MDMG |  MCGB    },
    	{   0xff26, 0x0001, HWR_NR52,       MDMG |  MCGB    },

    	{   0xff30, 0x0010, HWR_WAVERAM,    MDMG |  MCGB    },

    	{   0xff40, 0x0001, HWR_LCDC,       MDMG |  MCGB    },
    	{   0xff41, 0x0001, HWR_STAT,       MDMG |  MCGB    },
    	{   0xff42, 0x0001, HWR_SCY,        MDMG |  MCGB    },
    	{   0xff43, 0x0001, HWR_SCX,        MDMG |  MCGB    },
    	{   0xff44, 0x0001, HWR_LY,         MDMG |  MCGB    },
    	{   0xff45, 0x0001, HWR_LYC,        MDMG |  MCGB    },
    	{   0xff46, 0x0001, HWR_DMA,        MDMG |  MCGB    },
    	{   0xff47, 0x0001, HWR_BGP,        MDMG |  MCGB    },
    	{   0xff48, 0x0001, HWR_OBP0,       MDMG |  MCGB    },
    	{   0xff49, 0x0001, HWR_OBP1,       MDMG |  MCGB    },
    	{   0xff4a, 0x0001, HWR_WY,         MDMG |  MCGB    },
    	{   0xff4b, 0x0001, HWR_WX,         MDMG |  MCGB    },

    	{   0xff4d, 0x0001, HWR_KEY1,               MCGB    },

    	{   0xff4f, 0x0001, HWR_VBK,                MCGB    },

    	{   0xff50, 0x0001, HWR_IROM,       MDMG |  MCGB    },

    	{   0xff51, 0x0001, HWR_HDMA1,              MCGB    },
    	{   0xff52, 0x0001, HWR_HDMA2,              MCGB    },
    	{   0xff53, 0x0001, HWR_HDMA3,              MCGB    },
    	{   0xff54, 0x0001, HWR_HDMA4,              MCGB    },
    	{   0xff55, 0x0001, HWR_HDMA5,              MCGB    },

    	{   0xff56, 0x0001, HWR_RP,                 MCGB    },

    	{   0xff68, 0x0001, HWR_BCPS,               MCGB    },
    	{   0xff69, 0x0001, HWR_BCPD,               MCGB    },
    	{   0xff6a, 0x0001, HWR_OCPS,               MCGB    },
    	{   0xff6b, 0x0001, HWR_OCPD,               MCGB    },

    	{   0xff70, 0x0001, HWR_SVBK,               MCGB    },

    	{   0xff80, 0x007f, HWR_IRAM,       MDMG |  MCGB    },

    	{   0xffff, 0x0001, HWR_IE,         MDMG |  MCGB    },
    };

}

//final class CPUDecoderBuilder { }

//final class HWRDecoderBuilder { }
