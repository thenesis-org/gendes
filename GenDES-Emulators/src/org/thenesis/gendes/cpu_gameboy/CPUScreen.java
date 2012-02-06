package org.thenesis.gendes.cpu_gameboy;

import org.thenesis.gendes.debug.Screen;

public class CPUScreen extends Screen {
	public static final int SCREEN_WIDTH=32, SCREEN_HEIGHT=10;

    private CPU cpu;
    
    public CPUScreen(CPU c) {
        super();
        super.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        cpu=c;
    }
    
    public void updateScreen() {
    	if ((screenChar==null) || (cpu==null)) return;
        currentChar=' '; currentBackgroundColor=0; currentForegroundColor=1;
        clear();
        
        int px, py;
                
        px=0; py=0;        
        print16(px, py, "PC = ", cpu.rPC); py++;
        print16(px, py, "SP = ", cpu.rSP); py++;
        print16(px, py, "BC = ", (cpu.rB<<8) | cpu.rC); py++;
        print16(px, py, "DE = ", (cpu.rD<<8) | cpu.rE); py++;
        print16(px, py, "AF = ", (cpu.rA<<8) | cpu.rF); py++;
        print16(px, py, "HL = ", cpu.rHL); py++;
        px=11; py=0;
        print8(px, py, "A = ", cpu.rA); py++;
        print8(px, py, "B = ", cpu.rB); py++;
        print8(px, py, "C = ", cpu.rC); py++;
        print8(px, py, "D = ", cpu.rD); py++;
        print8(px, py, "E = ", cpu.rE); py++;
        print8(px, py, "F = ", cpu.rF); py++;
        print8(px, py, "H = ", cpu.rHL>>8); py++;
        print8(px, py, "L = ", cpu.rHL); py++;
        px=19; py=0;
        print(px, py,  "F"); py++;
        print1(px, py, "4-C = ", cpu.rF>>4); py++;
        print1(px, py, "5-H = ", cpu.rF>>5); py++;
        print1(px, py, "6-S = ", cpu.rF>>6); py++;
        print1(px, py, "7-Z = ", cpu.rF>>7); py++;
        px=19; py=6;
        print1(px, py, "IME = ", cpu.interruptsEnabled ? 1 : 0); py++;
        print1(px, py, "IMR = ", cpu.interruptRequested ? 1 : 0); py++;
    }
}
