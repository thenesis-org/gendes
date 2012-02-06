package org.thenesis.gendes.debug;

import org.thenesis.gendes.FastString;

public abstract class DisassemblerScreen extends Screen {
    // Colors.
    public static final int
    	COLOR_BG=               0,
    	COLOR_FG=               1,
    	COLOR_PC_BG=            2,
    	COLOR_PC_FG=            3,
    	COLOR_BREAKPOINT_BG=    4,
    	COLOR_BREAKPOINT_FG=    5,
    	COLOR_CURSOR_LINE_BG=   6,
    	COLOR_CURSOR_LINE_FG=   7;
    
    private BreakpointList breakpointList;

    // Display parameters.
    private int addressMask; // Mask for addresses.
    private int addressLength; // Number of characters for the address field.
    private int instructionLength; // Number of characters for the instruction dump field.

    private int lineAddress[]; // Address of each line of the screen.
    private int startAddress; // Start address of the screen.
    private int pageSize; // Size of the current page.
    private int cursorAddress; // .
    private boolean followPcFlag=true; // True if we must follow the PC.
    private boolean updatePcFlag=true; // True if we must update the PC.
    private boolean displayOpcodeBytesFlag=true; // True if we must display the opcode bytes.
    private boolean editionModeFlag=false; // True if edition mode is activated.
    
    private FastString opcodeBytesString=new FastString();
    private FastString instructionString=new FastString();
//    private FastString inputBuffer=new FastString();
    
    public DisassemblerScreen() {
        super();
        addressMask=0xffffffff;
        addressLength=4;
        instructionLength=4;
        screenDirty=true;
        
        opcodeBytesString.setCapacity(256);
        instructionString.setCapacity(256);
    }

    /**
     * Sets the screen height.
     * @param h the height of the screen in lines
     */
    public final void setScreenHeight(int h) {
        super.setScreenSize(64, h);
        if (screenHeight>0) lineAddress=new int[screenHeight];
        screenDirty=true;
    }
    
    /**
     * Sets the address space parameters.
     * @param mask the address mask. This represents the valid bits in an address.
     * @param length the address length in characters.
     *        This is the number of characters that will be used to display the address of each instruction.
     */
    public final void setAddressSpace(int mask, int length) {
    	if (length<1 || length>4) length=4;
    	addressMask=mask; addressLength=length;
    	screenDirty=true;
    }

    /**
     * Enables or disables the display of the opcode bytes.
     * @param flag <code>true</code> to enable the display of opcode bytes, <code>false</code> to disable it
     */
    public final void displayOpcodeBytes(boolean flag) {
    	displayOpcodeBytesFlag=flag;
    	screenDirty=true;
    }
    
    /**
     * Sets the maximum instruction length.
     * @param length the maximum instruction length in bytes
     */
    public final void setInstructionLength(int length) {
    	instructionLength=length;
    	screenDirty=true;
    }
    
    /**
     * Sets the list of breapoints.
     * @param bpl the list of breakpoints
     */
    public final void setBreakpointList(BreakpointList bpl) {
    	breakpointList=bpl;
    	screenDirty=true;
    }
    
    /**
     * Activate or desactivate edition mode.
     * @param flag <code>true</code> to activate edition mode, <code>false</code> to desactive it
     */
    public final void activateEditionMode(boolean flag) {
        editionModeFlag=flag;
    }
    
    /**
     * Checks if edition mode is activated.
     * @return <code>true</code> if edition mode is activated, <code>false</code> otherwise.
     */
    public final boolean isEditionModeActivated() {
        return editionModeFlag;
    }
    
    /**
     * Activates the following of the PC.
     * @param flag <code>true</code> to activate PC following, <code>false</code> to desactive it
     */
    public final void setPcFollowing(boolean flag) {
        followPcFlag=flag;
    }
        
    /**
     * Checks if the following of the PC is activated.
     * @return <code>true</code> if PC following is activated, <code>false</code> otherwise
     */
    public final boolean getPcFollowing() {
        return followPcFlag;
    }
    
    /**
     * Forces the screen to follow the PC.
     */
    public final void followPc() {
        updatePcFlag=true;
    }

    /**
     * Gets the start address.
     * @return the start address
     */
    public final int getStartAddress() {
        return startAddress;
    }
    
    /**
     * Checks if the address is currently visible on the screen.
     * @param a the address to check
     * @return <code>true</code> if the address is visible, <code>false</code> otherwise
     */
    public final boolean isAddressVisible(int a) {
        if (screenDirty) updateLineAddress();
        return (((a-startAddress)&addressMask)<pageSize);
    }
    
    /**
     * Gives the address corresponding to a line.
     * @param line the line whose we want the address
     * @return the address of line or 0 if the line is invalid
     */
    public final int addressFromLine(int line) {
        if (line<0 || line>=screenHeight) return 0;
        if (screenDirty) updateLineAddress();
        return lineAddress[line];
    }

    /**
     * Checks if the PC is on the given line.
     * @param line the line to check
     * @return <code>true</code> if the PC is on the line, <code>false</code> otherwise
     */
    public final boolean isPcAt(int line) {
        if (line<0 || line>=screenHeight) return false;
        int address=addressFromLine(line);
        return getPC()==address;
    }
    
    /**
     * Goes to a given address.
     * @param a the address where to go
     */
    public final void goTo(int a) {
        startAddress=a&addressMask; pageSize=0;
        screenDirty=true;
    }
    
    /**
     * Goes to the PC address.
     */
    public final void goToPc() {
        startAddress=getPC();
        screenDirty=true;
    }
    
    /**
     * Moves to the previous instruction address.
     */
    public final void goToPreviousInstruction() {
        int l=findPreviousInstructionLength(startAddress);
        startAddress=(startAddress-l)&addressMask;
        screenDirty=true;
    }
    
    /**
     * Moves to the next instruction address.
     */
    public final void goToNextInstruction() {
        int l=findNextInstructionLength(startAddress);
        startAddress=(startAddress+l)&addressMask;
        screenDirty=true;        
    }
    
    /**
     * Moves to the previous page address.
     */
    public final void goToPreviousPage() {
        for (int i=0; i<screenHeight; i++) {
            int l=findPreviousInstructionLength(startAddress);
            startAddress=(startAddress-l)&addressMask;
        }
        screenDirty=true;
    }

    /**
     * Moves to the next page address.
     */
    public final void goToNextPage() {
        for (int i=0; i<screenHeight; i++) {
            int l=findNextInstructionLength(startAddress);
            startAddress=(startAddress+l)&addressMask;
        }
        screenDirty=true;
    }
            
    //--------------------------------------------------------------------------------
    // Display.
    //--------------------------------------------------------------------------------
    /**
     * Updates the screen.
     * @param forceUpdate <code>true</code> to force an update of the screen, <code>false</code> otherwise
     */
    public final void updateScreen(boolean forceUpdate) {
        if (screenChar==null || (!screenDirty && !forceUpdate)) return; 
        
        int pcAddress=getPC();
        if (followPcFlag && updatePcFlag) {
            if (!isAddressVisible(pcAddress)) { startAddress=pcAddress; screenDirty=true; }
        }
        updatePcFlag=false;
        
        currentChar=' ';
        
        int x, y, a=startAddress&addressMask, length, totalLength=0;
        for (y=0; y<screenHeight; y++) {
            char pcChar, breakpointChar;

            // Normal colors.
            currentBackgroundColor=COLOR_BG; currentForegroundColor=COLOR_FG; 

            // PC.
            if (a==pcAddress) {
            	pcChar='>';
                currentBackgroundColor=COLOR_PC_BG; currentForegroundColor=COLOR_PC_FG; 
            } else {
            	pcChar=' ';
            }
            
            // Breakpoint.
            if (hasBreakpointAtAddress(a)) {
            	breakpointChar='*'; 
                if (currentBackgroundColor==COLOR_BG) { currentBackgroundColor=COLOR_BREAKPOINT_BG; currentForegroundColor=COLOR_BREAKPOINT_FG; }            	
            } else breakpointChar=' ';
            
            // Cursor.
            if (cursorAddress==a && currentBackgroundColor==COLOR_BG) { currentBackgroundColor=COLOR_CURSOR_LINE_BG; currentForegroundColor=COLOR_CURSOR_LINE_FG; }

            // Instruction.
            length=disassemble(a, opcodeBytesString, instructionString);

            x=0;
            clearLine(y);
            // Print PC cursor.
            print(x, y, pcChar); x+=1;
            // Print breakpoint.
            print(x, y, breakpointChar); x+=1;
            // Print address.
            print(x, y, a, addressLength<<2); x+=addressLength;
            if (displayOpcodeBytesFlag) {
	            // Separator.
	            //print(x+1, y, '|'); x+=3;
	            x+=1;
	            // Print opcode bytes.
	            print(x, y, opcodeBytesString); x+=instructionLength<<1;
            }
            // Separator.
            //print(x+1, y, '|'); x+=3;
            x+=1;
            // Print instruction.
            print(x, y, instructionString); x+=instructionString.length;
            
            a=(a+length)&addressMask;
            totalLength+=length;
        }
        pageSize=totalLength;
    }
    
    /**
     * Updates the addresses associated to each screen line.
     */
    private void updateLineAddress() {
        int address=startAddress;
        int l, tl=0;
        for (int y=0; y<screenHeight; y++) {
            lineAddress[y]=address;
            l=findNextInstructionLength(address);
            tl+=l;
            address=(address+l)&addressMask;
        }
        pageSize=tl;
    }
    
    //--------------------------------------------------------------------------------
    // Breakpoints.
    //--------------------------------------------------------------------------------
    /**
     * Sets a breakpoint at a given line of screen.
     * If the line is invalid, nothing is done.
     * @param line the line at which to set the breakpoint
     */
    public final void setBreakpoint(int line) {
        if (breakpointList==null || line<0 || line>=screenHeight) return;
        int address=addressFromLine(line);
        Breakpoint bp=breakpointList.findBreakpoint(address);
        if (bp==null) {
            bp=new Breakpoint();
            breakpointList.addBreakpoint(bp, address);
        } else {
            breakpointList.removeBreakpoint(bp);
        }        
    }

    /**
     * Gets the breakpoint positioned at a given line if any.
     * @param line the line at which we must find a breakpoint
     * @return the breakpoint or null if no breakpoint is set at this line
     */
    public final Breakpoint getBreakpoint(int line) {
        if (breakpointList==null || line<0 || line>=screenHeight) return null;
        int address=addressFromLine(line);
        return breakpointList.findBreakpoint(address);        
    }
 
    /**
     * Checks if a line has an associated breakpoint.
     * @param line the line at which we must check if there is a breakpoint
     * @return <code>true</code> if there is a breakpoint at this line, <code>false</code> otherwise
     */
    public final boolean hasBreakpointAtLine(int line) {
        if (breakpointList==null || line<0 || line>=screenHeight) return false;
        int address=addressFromLine(line);
        return breakpointList.findBreakpoint(address)!=null;
    }
    
    /**
     * Checks if an address has an associated breakpoint.
     * @param address the address at which we must check if there is a breakpoint
     * @return <code>true</code> if there is a breakpoint at this address, <code>false</code> otherwise
     */
    public final boolean hasBreakpointAtAddress(int address) {
    	if (breakpointList==null) return false;
        return breakpointList.findBreakpoint(address)!=null;
    }
    
    //--------------------------------------------------------------------------------
    // Abstract routines.
    //--------------------------------------------------------------------------------
    /**
     * Reads a byte.
     * @param address the address at which we must read the byte
     * @return the byte
     */
    protected abstract int read8(int address);
    
    /**
     * Writes a byte.
     * @param address the address at which we must write a byte
     * @param data the byte to write
     */
    protected abstract void write8(int address, int data);
    
    /**
     * Gets the PC address.
     * @return the current PC address
     */
    protected abstract int getPC();
    
    /**
     * Returns the length of the previous instruction.
     * @param address address of the end of the instruction
     * @return Returns the length of the instruction
     */
    protected abstract int findPreviousInstructionLength(int address);
    
    /**
     * Returns the length of the next instruction.
     * @param address address of the instruction
     * @return the length of the instruction
     */
    protected abstract int findNextInstructionLength(int address);
        
    /**
     * Disassembles an instruction.
     * @param address address of the instruction
     * @param opcode string into which the opcode bytes string is appended
     * @param instruction string into which the instruction string is appended
     * @return the length of the instruction in bytes
     */
    protected abstract int disassemble(int address, FastString opcode, FastString instruction);
}
