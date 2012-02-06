package org.thenesis.gendes.gameboy;

import org.thenesis.gendes.debug.Screen;

public final class GameBoyHardwareScreen extends Screen {
	public static final int SCREEN_WIDTH=50, SCREEN_HEIGHT=17;
	
    private GameBoy gameBoy;
    
    public GameBoyHardwareScreen(GameBoy gb) {
        super();
        super.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        gameBoy=gb;
    }
    
    public void updateScreen(boolean forceUpdate) {
        if (screenChar==null || gameBoy==null) return;
        int px, py;
        GameBoy gb=gameBoy; // Shortcut.
        
        currentChar=' '; currentBackgroundColor=0; currentForegroundColor=1;
        clear();        
        string.clear();
        
        int ie=gb.interruptEnableFlags, ir=gb.interruptRequestFlags;
        
        px=0; py=0;
        print8(px, py, "$00-P1   = ", gb.padP1); py++;
        print8(px, py, "$01-SB   = ", gb.serialData); py++;
        print8(px, py, "$02-SC   = ", gb.serialControl); py++;
        print8(px, py, "$04-DIV  = ", gb.timerDivider); py++;
        print8(px, py, "$05-TIMA = ", gb.timerCounter); py++;
        print8(px, py, "$06-TMA  = ", gb.timerReloadValue); py++;
        print8(px, py, "$07-TAC  = ", (gb.timerStartedFlag<<2) | gb.timerClockSelect); py++;
        print8(px, py, "$0F-IF   = ", ir); py++;
        print8(px, py, "$FF-IE   = ", ie); py++;
        
        py++;
        print(px, py, "IRQs      E F"); py++;
        print(px, py, "0-VBLANK= "); print1(px+10, py, ((ie>>0)&0x01)); print1(px+12, py, ((ir>>0)&0x01)); py++;
        print(px, py, "1-LCDC  = "); print1(px+10, py, ((ie>>1)&0x01)); print1(px+12, py, ((ir>>1)&0x01)); py++;
        print(px, py, "2-TIMER = "); print1(px+10, py, ((ie>>2)&0x01)); print1(px+12, py, ((ir>>2)&0x01)); py++;
        print(px, py, "3-SERIAL= "); print1(px+10, py, ((ie>>3)&0x01)); print1(px+12, py, ((ir>>3)&0x01)); py++;
        print(px, py, "4-JOYPAD= "); print1(px+10, py, ((ie>>4)&0x01)); print1(px+12, py, ((ir>>4)&0x01)); py++;
        
        px=15; py=0;
        print8(px, py, "$40-LCDC = ", gb.videoLCDControl); py++;
        print8(px, py, "$41-STAT = ", gb.videoLCDStatus); py++;
        print8(px, py, "$42-SCY  = ", gb.videoBackgroundY); py++;
        print8(px, py, "$43-SCX  = ", gb.videoBackgroundX); py++;
        print8(px, py, "$44-LY   = ", gb.videoCurrentLine); py++;
        print8(px, py, "$45-LYC  = ", gb.videoLineCompare); py++;
        print8(px, py, "$46-DMA  = ", gb.odmaAddress); py++;
        print8(px, py, "$47-BGP  = ", gb.videoBPData); py++;
        print8(px, py, "$48-OBP0 = ", gb.videoOPData[0]); py++;
        print8(px, py, "$49-OBP1 = ", gb.videoOPData[1]); py++;
        print8(px, py, "$4A-WY   = ", gb.videoWindowY); py++;
        print8(px, py, "$4B-WX   = ", gb.videoWindowX); py++;
        
        px=30; py=0;
        print8(px, py, "$4F-VBK  = ", gb.videoRamBank); py++;
        print8(px, py, "$51-HDMA1= ", gb.hdmaSrcAddress>>8); py++;
        print8(px, py, "$52-HDMA2= ", gb.hdmaSrcAddress); py++;
        print8(px, py, "$53-HDMA3= ", gb.hdmaDstAddress>>8); py++;
        print8(px, py, "$54-HDMA4= ", gb.hdmaDstAddress); py++;
        print8(px, py, "$55-HDMA5= ", (gb.hdmaNotActiveFlag<<7) | gb.hdmaLength); py++;        
        print8(px, py, "$68-BCPS = ", gb.videoBCPS); py++;
        print8(px, py, "$69-BCPD = ", gb.videoBCPD); py++;
        print8(px, py, "$6a-OCPS = ", gb.videoOCPS); py++;
        print8(px, py, "$6b-OCPD = ", gb.videoOCPD); py++;
        print8(px, py, "$70-SVBK = ", gb.workRamBank); py++;
    }

}
