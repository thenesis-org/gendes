package org.thenesis.gendes.gameboy;

import org.thenesis.gendes.debug.Screen;

public final class CartridgeScreen extends Screen {
	public static final int SCREEN_WIDTH=32, SCREEN_HEIGHT=10;

    private GameBoy gameBoy;
    
    public CartridgeScreen(GameBoy gb) {
        super();
        super.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        gameBoy=gb;
    }
    
    public void updateScreen() {
        if ((screenChar==null) || (gameBoy==null)) return;
        GameBoy gb=gameBoy; // Shortcut.
        int px=0, py=0;
        
        currentChar=' '; currentBackgroundColor=0; currentForegroundColor=1;
        clear();
        
        if (gb.isCartridgeInserted()) {
            CartridgeInfos ci=gb.getCartridgeInfos();
            
            // Cartridge title.
            print(px, py, "Title: "+ci.gameTitle); py++;
            
            // Game boy model features.
            int flags=0;
            if (!ci.colorOnlyFlag) {
            	flags|=0x1;
                if (ci.superFeaturesFlag) flags|=0x2;
            }
            if (ci.colorFeaturesFlag) flags|=0x4;
            string.clear();
            string.append("Models:");
            string.append(((flags&0x1)!=0) ?	" ORIGINAL" :	"         ");
            string.append(((flags&0x2)!=0) ?	" SUPER" :		"     ");
            string.append(((flags&0x4)!=0) ?	" COLOR" :		"     ");
            print(px, py); py++;
            
            // Controller type.
            print(px, py, "Controller: "+CartridgeInfos.controllerToString(ci.controller));
            if (ci.controller==CartridgeInfos.CTC_MBC1) {
                print(px+16, py, (gb.cartridgeMbc1MemoryModel!=0) ? "(4Mb/32KB)" : "(16Mb/8KB)");               
            }
            py++;
            
            // Cartridge features.
            print(px, py, "Features:"); py++;
            string.clear();
            string.append(((ci.features&CartridgeInfos.CTF_RAM)!=0) ? 		" RAM" :		"    ");
            string.append(((ci.features&CartridgeInfos.CTF_BATTERY)!=0) ?	" BATTERY" :	"        ");
            string.append(((ci.features&CartridgeInfos.CTF_RTC)!=0) ? 		" RTC" : 		"    ");
            string.append(((ci.features&CartridgeInfos.CTF_RUMBLE)!=0) ?	" RUMBLE" : 	"       ");
            string.append(((ci.features&CartridgeInfos.CTF_INFRARED)!=0) ?	" INFRARED" : 	"         ");
            print(px, py); py++;
            
            // Sizes.
            print(px, py, "ROM"); py++;
            print(px, py, "Banks: "+ci.nbRomBanks); py++;
            print(px, py, "Size:  "+ci.romSize); py++;
            print(px, py, "Bank:  "+gb.cartridgeRomBank); py++;
            py-=4;
            print(px+16, py, "RAM"); py++;
            print(px+16, py, "Banks: "+ci.nbRamBanks); py++;
            print(px+16, py, "Size:  "+ci.ramSize); py++;
            print(px+16, py, "Bank:  "+gb.cartridgeRamBank); py++;
        } else {
            print(px, py, "No cartridge.");            
        }
    }
}
