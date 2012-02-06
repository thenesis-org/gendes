package org.thenesis.gendes.gameboy;

import org.thenesis.gendes.debug.HardwareScreen;

public class GameBoyHardwareScreen2 extends HardwareScreen {
	public static final int SCREEN_WIDTH=50, SCREEN_HEIGHT=17;
	
    private GameBoy gameBoy;
    
    public GameBoyHardwareScreen2(GameBoy gb) {
        super();
        super.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        gameBoy=gb;
        setItems(ITEMS);
    }

    
    private Item ITEMS[]={
    	new Group("Ram"),
    		new Register("SVBK", 0xff70, 8), null,
    	null,
    	
    	new Group("Interrupts"),
    		new Register("IF", 0xff0f, 8),
    			new Bitfield("VBLANK", 0, 1), null,
    			new Bitfield("STAT", 1, 1), null,
    			new Bitfield("Timer", 2, 1), null,
    			new Bitfield("Serial", 3, 1), null,
    			new Bitfield("Pad", 4, 1), null,
    		null,
   			new Register("IE", 0xffff, 8),
   				new Bitfield("VBLANK", 0, 1), null,
   				new Bitfield("STAT", 1, 1), null,
   				new Bitfield("Timer", 2, 1), null,
   				new Bitfield("Serial", 3, 1), null,
   				new Bitfield("Pad", 4, 1), null,
   			null,
   		null,
    	
    	new Group("Timer"),
    		new Register("DIV", 0xff04, 8), null,
    		new Register("TIMA", 0xff05, 8), null,
    		new Register("TMA", 0xff06, 8), null,
    		new Register("TAC", 0xff07, 8),
    			new Bitfield("Enable", 2, 1), null,
    			new Bitfield("Frequency", 0, 2), null,
    		null,
    	null,
    	
    	new Group("Pad"),
    		new Register("P1", 0xff00, 8), null,    	
    	null,
    	
    	new Group("Serial"),
    		new Register("SB (serial data)", 0xff01, 8), null,
    		new Register("SC (serial control)", 0xff02, 8),
    			new Bitfield("Clock source", 0, 1, new String[]{"external", "internal"}), null,
    			new Bitfield("Clock frequency", 1, 1, new String[]{"8KHz", "256KHz"}), null,
    			new Bitfield("Transfer start", 7, 1), null,
    		null,
    	null,
    	
    	new Group("Video"),
    		new Register("LCDC", 0xff40, 8),
    			new Bitfield("BG display enable", 0, 1), null,
    			new Bitfield("OBJ display enable", 1, 1), null,
    			new Bitfield("OBJ structure", 2, 1, new String[]{"8x8", "8x16"}), null,
    			new Bitfield("BG bank", 3, 1, new String[]{"$9800", "$9c00"}), null,
    			new Bitfield("BG character", 4, 1, new String[]{"$8800", "$8000"}), null,
    			new Bitfield("Window display enable", 5, 1), null,
    			new Bitfield("Window bank", 6, 1, new String[]{"$9800", "$9c00"}), null,
    			new Bitfield("Display enable", 7, 1), null,
    		null,
    		new Register("STAT", 0xff41, 8),
    			new Bitfield("Mode", 0, 2), null,
    			new Bitfield("LYC line", 2, 1), null,
    			new Bitfield("Mode 0 interrupt enable", 3, 1), null,
    			new Bitfield("Mode 1 interrupt enable", 4, 1), null,
    			new Bitfield("Mode 2 interrupt enable", 5, 1), null,
    			new Bitfield("LYC interrupt enable", 6, 1), null,
    		null,
    		new Register("SCY", 0xff42, 8), null,
    		new Register("SCX", 0xff43, 8), null,
    		new Register("LY", 0xff44, 8), null,
    		new Register("LYC", 0xff45, 8), null,
    		new Register("DMA", 0xff46, 8), null,
    		new Register("BGP", 0xff47, 8), null,
    		new Register("OBP0", 0xff48, 8), null,
    		new Register("OBP1", 0xff49, 8), null,
    		new Register("WY", 0xff4a, 8), null,
    		new Register("WX", 0xff4b, 8), null,
    		new Register("VBK", 0xff4f, 8), null,
    		new Register("BCPS", 0xff68, 8), null,
    		new Register("BCPD", 0xff69, 8), null,
    		new Register("OCPS", 0xff6a, 8), null,
    		new Register("OCPD", 0xff6b, 8), null,
    	null,
    		
    	new Group("HDMA"),
    		new Register("HDMA1", 0xff51, 8), null,
    		new Register("HDMA2", 0xff52, 8), null,
    		new Register("HDMA3", 0xff53, 8), null,
    		new Register("HDMA4", 0xff54, 8), null,
    		new Register("HDMA5", 0xff55, 8), null,
    	null,
    	
    	new Group("Audio"),
//    		new Register("", 0xff, 8), null,
//    		new Register("", 0xff, 8), null,
    	null,
    };
}
