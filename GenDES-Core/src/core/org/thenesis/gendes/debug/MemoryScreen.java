package org.thenesis.gendes.debug;


import org.thenesis.gendes.Tools;
import org.thenesis.gendes.debug.Screen;


/**
 * Class To explore memory.
 *
 */
public abstract class MemoryScreen extends Screen {
    public static class Cursor {
        public int x, y;
        public int field;
        public int address, nibble;
    }
    
    public static class AddressSpaceState {
        public int addressMask, startAddress;
        public int cursorAddress, cursorAddressNibble; 
    }
        
    // Colors.
    public static final byte
    	COLOR_BG=                  0, // Background.
    	COLOR_FG=                  1, // Foreground.
    	COLOR_CURSOR_LINE_BG=      2, // Cursor background.
    	COLOR_CURSOR_LINE_FG=      3, // Cursor foreground.
    	COLOR_CURSOR_BG=           4, // Cursor background.
    	COLOR_CURSOR_FG=           5, // Cursor foreground.
    	COLOR_CURSOR2_BG=          6, // Cursor background when the field is selected.
    	COLOR_CURSOR2_FG=          7, // Cursor foreground when the field is selected.
    	COLOR_EDIT_BG=             8, // Background in edition mode.
    	COLOR_EDIT_FG=             9, // Foreground in edition mode.
    	COLOR_CURSOR_LINE_EDIT_BG= 10, // Cursor background.
    	COLOR_CURSOR_LINE_EDIT_FG= 11, // Cursor foreground.
    	COLOR_CURSOR_EDIT_BG=      12, // Cursor background in edition mode.
    	COLOR_CURSOR_EDIT_FG=      13, // Cursor foreground in edition mode.
    	COLOR_CURSOR2_EDIT_BG=     14, // Cursor background when the field is selected in edition mode.
    	COLOR_CURSOR2_EDIT_FG=     15; // Cursor foreground when the field is selected in edition mode.
    
    // Fields.
    private static final int
    	FIELD_ADDRESS=      0,
    	FIELD_HEXA=         1,
    	FIELD_ASCII=        2,
    	FIELD_NB=           3;
    
    // Display parameters.
    private int addressLength; // Number of characters for the address field.
    private int groupSize; // Number of bytes in a group.
    private boolean autoResize; // True to adapt the number of bytes in a line to the width of screen.
    private int lineSize; // Number of bytes in a line.
    private int pageSize; // Number of bytes in a page.
    private boolean dirtyFlag; // True if the screen needs to be updated.    
    private final int screenFields[][]=new int[3][2]; // Dimensions of each field.

    // Address.
    private int addressMask; // Mask for addresses.
    private int startAddress; // Start address of the screen.
    
    // Edition.
    private boolean insertionModeFlag; // True if insertion mode is activated. 
    private boolean editAsciiFlag; // True if we insert characters in the ascii field.      
    private int cursorAddress, cursorAddressNibble;
    private final Cursor position=new Cursor();
    
    public MemoryScreen() {
        super();
        addressLength=4;
        groupSize=1;
        autoResize=true;
        addressMask=0xffff;
        dirtyFlag=true;
    }
    
    
    public final void saveAddressSpaceState(AddressSpaceState s) {
        s.addressMask=addressMask; s.startAddress=startAddress;
        s.cursorAddress=cursorAddress; s.cursorAddressNibble=cursorAddressNibble;
    }
    
    public final void restoreAddressSpaceState(AddressSpaceState s) {
        addressMask=s.addressMask; startAddress=s.startAddress;
        cursorAddress=s.cursorAddress; cursorAddressNibble=s.cursorAddressNibble;
        dirtyFlag=true;
    }
        
    //--------------------------------------------------------------------------------
    // Address.
    //--------------------------------------------------------------------------------
    /**
     * Set the address mask.
     */
    public final void setAddressMask(int mask) {
        addressMask=mask;
        startAddress&=mask; cursorAddress&=mask;
        dirtyFlag=true;
    }

    /**
     * Return the start address.
     */
    public final int getStartAddress() {
        return startAddress;
    }
    
    /**
     * Check if the address is currently visible on the screen.
     */
    public final boolean isAddressVisible(int a) {
        a-=startAddress; a&=addressMask;
        return a<pageSize;
    }
    
    /**
     * Go to a given address.
     */
    public final void goTo(int a) {
        startAddress=a&addressMask;
        dirtyFlag=true;
    }
        
    /**
     * Move to the previous page address.
     */
    public final void goToPreviousLine() {
        startAddress=(startAddress-lineSize)&addressMask;
        dirtyFlag=true;
    }

    /**
     * Move to the next page address.
     */
    public final void goToNextLine() {
        startAddress=(startAddress+lineSize)&addressMask;
        dirtyFlag=true;
    }
    
    /**
     * Move to the previous page address.
     */
    public final void goToPreviousPage() {
        startAddress=(startAddress-pageSize)&addressMask;
        dirtyFlag=true;
    }

    /**
     * Move to the next page address.
     */
    public final void goToNextPage() {
        startAddress=(startAddress+pageSize)&addressMask;
        dirtyFlag=true;
    }

    //--------------------------------------------------------------------------------
    // Edition.
    //--------------------------------------------------------------------------------
    public final void activateInsertionMode(boolean flag) {
        insertionModeFlag=flag;
        dirtyFlag=true;
    }
    
    public final boolean isInsertionModeActivated() {
        return insertionModeFlag;
    }
    
    public final void setAsciiFieldEdition(boolean flag) {
        editAsciiFlag=flag;
        dirtyFlag=true;
    }
    
    public final boolean isAsciiFieldEdited() {
        return editAsciiFlag;
    }
    
    /**
     * Insert a key.
     */
    public final void insertKey(char c) {
        if (!insertionModeFlag) return;
        if (editAsciiFlag) {
            if (c>127) return;
            write8(cursorAddress, (int)c);
        } else {
            int digit=0;
            if ((c>='0') && (c<='9')) digit=c-'0';
            else if ((c>='a') && (c<='f')) digit=10+(c-'a');
            else if ((c>='A') && (c<='F')) digit=10+(c-'A');
            else return;
            int data=read8(cursorAddress);
            data=(cursorAddressNibble==0) ? (data&0x0f)|(digit<<4) : (data&0xf0)|digit;
            write8(cursorAddress, data);
        }
        moveCursorRight();
    }
    
    /**
     * Return whether the cursor is visible.
     */
    public final boolean isCursorVisible() {
        int a=(cursorAddress-startAddress)&addressMask;
        return (a<pageSize);
    }

    /**
     * Reset the cursor.
     */
    public final void resetCursor() {
        int a=(cursorAddress-startAddress)&addressMask;
        if (a>=pageSize) cursorAddress=startAddress;        
        dirtyFlag=true;
    }
    
    
    /**
     * Set the cursor address.
     */
    public final void setCursorAddress(int address, int nibble) {
        int a=(address-startAddress)&addressMask;
        cursorAddress=address&addressMask; cursorAddressNibble=nibble&1;
        if (a>=pageSize) startAddress=cursorAddress;
        dirtyFlag=true;
    }
    
    /**
     * Set the cursor address.
     */
    public final void setCursorPosition(int x, int y) {
        position.x=x; position.y=y;
        if (!positionToAddress(position)) {
            cursorAddress=position.address; cursorAddressNibble=position.nibble;
            editAsciiFlag=position.field==FIELD_ASCII;
            int a=(cursorAddress-startAddress)&addressMask;
            if (a>=pageSize) startAddress=cursorAddress;
            dirtyFlag=true;
        }
    }
    
    /**
     * Get the cursor address.
     */
    public final void getCursorAddress(Cursor position) {
        position.address=cursorAddress; position.nibble=cursorAddressNibble;
    }
    
    /**
     * Move the cursor up.
     */
    public final void moveCursorUp() {
        int a=(cursorAddress-startAddress)&addressMask;
        cursorAddress=(cursorAddress-lineSize)&addressMask;
        if (a>=pageSize) startAddress=cursorAddress;
        else if (a<lineSize) startAddress=(startAddress-lineSize)&addressMask;
        dirtyFlag=true;
    }
    
    /**
     * Move the cursor down.
     */
    public final void moveCursorDown() {
        int a=(cursorAddress-startAddress)&addressMask;
        cursorAddress=(cursorAddress+lineSize)&addressMask;
        if (a>=pageSize) startAddress=cursorAddress;
        else if (a>=(pageSize-lineSize)) startAddress=(startAddress+lineSize)&addressMask;
        dirtyFlag=true;        
    }
    
    /**
     * Move the cursor left.
     */
    public final void moveCursorLeft() {
        int a=(cursorAddress-startAddress)&addressMask;
        if (editAsciiFlag) {
            cursorAddressNibble=0;
            cursorAddress=(cursorAddress-1)&addressMask;
        } else {
            cursorAddressNibble=1-cursorAddressNibble;
            cursorAddress=(cursorAddress-cursorAddressNibble)&addressMask;
        }
        if (a>=pageSize) startAddress=cursorAddress;
        else if ((a<1) && (cursorAddressNibble!=0)) startAddress=(startAddress-lineSize)&addressMask;
        dirtyFlag=true;        
    }
    
    /**
     * Move the cursor right.
     */
    public final void moveCursorRight() {
        int a=(cursorAddress-startAddress)&addressMask;
        if (editAsciiFlag) {
            cursorAddress=(cursorAddress+1)&addressMask;
            cursorAddressNibble=0;
        } else {
            cursorAddress=(cursorAddress+cursorAddressNibble)&addressMask;
            cursorAddressNibble=1-cursorAddressNibble;            
        }
        if (a>=pageSize) startAddress=cursorAddress;
        else if ((a>=(pageSize-1)) && (cursorAddressNibble==0)) startAddress=(startAddress+lineSize)&addressMask;
        dirtyFlag=true;
    }
    
    /**
     * Move the cursor to the previous page.
     */
    public final void moveCursorToPreviousPage() {
        int a=(cursorAddress-startAddress)&addressMask;
        cursorAddress=(cursorAddress-pageSize)&addressMask;
        if (a>=pageSize) startAddress=cursorAddress;
        else startAddress=(startAddress-pageSize)&addressMask;
        dirtyFlag=true;
    }
    
    /**
     * Move the cursor to the next page.
     */
    public final void moveCursorToNextPage() {
        int a=(cursorAddress-startAddress)&addressMask;
        cursorAddress=(cursorAddress+pageSize)&addressMask;
        if (a>=pageSize) startAddress=cursorAddress;
        else startAddress=(startAddress+pageSize)&addressMask;
        dirtyFlag=true;        
    }
    
    /**
     * Move the cursor to the start of the line.
     */
    public final void moveCursorToStartOfLine() {
        int a=(cursorAddress-startAddress)&addressMask;
        if (a>=pageSize) startAddress=cursorAddress;
        else { int l=a/lineSize; cursorAddress=(startAddress+l*lineSize)&addressMask; cursorAddressNibble=0; }
        dirtyFlag=true;
    }
    
    /**
     * Move the cursor to the end of the line.
     */
    public final void moveCursorToEndOfLine() {
        int a=(cursorAddress-startAddress)&addressMask;
        if (a>=pageSize) startAddress=cursorAddress;
        else { int l=a/lineSize; cursorAddress=(startAddress+l*lineSize+lineSize-1)&addressMask; cursorAddressNibble=1; }
        dirtyFlag=true;
    }
    
    //--------------------------------------------------------------------------------
    // Display.
    //--------------------------------------------------------------------------------
    public final void setScreenSize(int w, int h) {
        super.setScreenSize(w, h);
        if (autoResize) {
            if ((w>0) && (h>0)) {
                lineSize=computeLineSizeFromWidth(addressLength, groupSize, w);
                pageSize=lineSize*screenHeight;
            } else {
                lineSize=0; pageSize=0;
            }
        }
        updateFields();
        dirtyFlag=true;
    }
    
    public final void setAutoResize(boolean flag) {
        autoResize=flag;
        dirtyFlag=true;
    }
    
    public final boolean getAutoResize() {
        return autoResize;
    }
    
    public final void setAddressLength(int as) {
        if (as<0 || as>8) return;
        addressLength=as;
        updateFields();
        dirtyFlag=true;
    }
    
    public final int getAddressLength() {
        return addressLength;
    }
    
    public final void setGroupSize(int gs) {
        if (gs<0) gs=0;
        groupSize=gs;
        updateFields();
        dirtyFlag=true;
    }
    
    public final int getGroupSize() {
        return groupSize;
    }
    
    public final void setLineSize(int ls) {
        lineSize=ls;
        pageSize=lineSize*screenHeight;
        updateFields();
        dirtyFlag=true;
    }
    
    public final int getLineSize() {
        return lineSize;
    }
    
    private void updateFields() {
        if (groupSize>0) {
            screenFields[FIELD_ADDRESS][0]=0; screenFields[FIELD_ADDRESS][1]=addressLength;
            screenFields[FIELD_HEXA][0]=screenFields[FIELD_ADDRESS][0]+screenFields[FIELD_ADDRESS][1]+3; screenFields[FIELD_HEXA][1]=(lineSize<<1)+lineSize/groupSize;
            screenFields[FIELD_ASCII][0]=screenFields[FIELD_HEXA][0]+screenFields[FIELD_HEXA][1]+2; screenFields[FIELD_ASCII][1]=lineSize;
        } else {
            screenFields[FIELD_ADDRESS][0]=0; screenFields[FIELD_ADDRESS][1]=addressLength;
            screenFields[FIELD_HEXA][0]=screenFields[FIELD_ADDRESS][0]+screenFields[FIELD_ADDRESS][1]+3; screenFields[FIELD_HEXA][1]=(lineSize<<1);
            screenFields[FIELD_ASCII][0]=screenFields[FIELD_HEXA][0]+screenFields[FIELD_HEXA][1]+3; screenFields[FIELD_ASCII][1]=lineSize;            
        }
    }
    
    /**
     * Compute the line size in bytes for a given width in characters.
     * as: address size in hexadecimal characters.
     * gs: group size in bytes.
     * w: width in characters.
     */
    public static int computeLineSizeFromWidth(int as, int gs, int w) {
        // ls is always a multiple of gs.
        // If gs!=0:
        // Address field size: adfs=as
        // Hexa field size: hfs=ls*2+ls/gs;
        // Ascii field size: asfs=ls;
        // w=adfs+3+hfs+2+asfs+2.
        // w=as+3+ls*2+ls/gs+2+ls+2.
        // ls=(w-(as+3+2+2))/(2+1/gs+1)=gs*(w-as-7)/(3*gs+1).
        // If gs=0:
        // w=as+3+ls*2+3+ls+2.
        // ls=(w-(as+3+3+2))/(2+1)=(w-8)/3.
        int ls;
        if (gs>0) {
            ls=gs*(w-as-7)/(3*gs+1);
            ls=(ls/gs)*gs;
            if (ls<gs) ls=gs;
        } else {
            ls=(w-as-8)/3;
            if (ls<1) ls=1;
        }
        return ls;
    }
    
    /**
     * Compute the width in characters for a given line size in bytes.
     * as: address size in hexadecimal characters.
     * gs: group size in bytes.
     * ls: line size in bytes.
     */
    public static int computeWidthFromLineSize(int as, int gs, int ls) {
        int w;
        if (gs>0) {
            w=as+7+ls*3+ls/gs;
        } else {
            w=as+8+ls*3;
        }
        return w;
    }
    
    /**
     * Return the field that corresponds to a given character position.
     */
    public final int positionToField(int x, int y) {
        if ((x<0) || (x>=screenWidth) || (y<0) || (y>=screenHeight)) return -1;
        for (int i=0; i<FIELD_NB; i++) if ((x>=screenFields[i][0]) && (x<(screenFields[i][0]+screenFields[i][1]))) return i;
        return -1;
    }
    
    /**
     * Return the address that corresponds to a given character position.
     */
    public final boolean positionToAddress(Cursor p) {
        int field=positionToField(p.x, p.y);
        if (field<FIELD_HEXA) return true;
        int c=p.x-screenFields[field][0]; // Character position on line in the field.
        int address=startAddress+lineSize*p.y, nibble=0, group, groupOffset, groupLength;
        switch (field) {
        case FIELD_HEXA:
            if (groupSize!=0) {
                groupLength=(groupSize<<1); // Length of a group.
                group=c/(groupLength+1); // Group.
                groupOffset=c-group*(groupLength+1); // Offset in the group.
                if (groupOffset>groupLength) return true; // This is a space character.
                address+=group*groupSize+(groupOffset>>1); nibble=groupOffset&1;
            } else {
                address+=c>>1; nibble=c&1;
            }
            break;
        case FIELD_ASCII:
            address+=c;
            break;
        }
        p.address=address&addressMask; p.nibble=nibble; p.field=field; 
        return false;
    }
    
    /**
     * Give the hexadecimal position corresponding to a given address.
     * Return true if the address is offscreen.
     */
    public final boolean addressToHexaPosition(Cursor p) {
        int a=(p.address-startAddress)&addressMask, line, b, group, groupOffset;
        if (a>=pageSize) return true;
        line=a/lineSize; b=a-line*lineSize;
        if (groupSize!=0) { group=b/groupSize; groupOffset=b-group*groupSize; } else { group=0; groupOffset=b; }
        p.y=line;
        p.x=screenFields[FIELD_HEXA][0]+(group*((groupSize<<1)+1)+(groupOffset<<1))+p.nibble;
        return false;
    }
    
    /**
     * Give the ascii position corresponding to a given address.
     * Return true if the address is offscreen.
     */
    public final boolean addressToAsciiPosition(Cursor p) {
        int a=(p.address-startAddress)&addressMask, line, b;
        if (a>=pageSize) return true;
        line=a/lineSize; b=a-line*lineSize;
        p.y=line;
        p.x=screenFields[FIELD_ASCII][0]+b;
        return false;
    }
    
    /**
     * Update the screen.
     */
    public final void updateScreen(boolean forceUpdate) {
        if (screenChar==null || (!dirtyFlag && !forceUpdate)) return;
                
        int x, y, a=startAddress&addressMask;
        for (y=0; y<screenHeight; y++) {
            // Highlight the line of the cursor.
            {
                int a0=(cursorAddress-a)&addressMask, cl=0;
                if (a0<lineSize) cl+=1; if (insertionModeFlag) cl+=2;
                currentBackgroundColor=COLORS_L_BG[cl]; currentForegroundColor=COLORS_L_FG[cl];
                currentChar=' ';
                clearLine(y);
            }
            
            x=0;
            
            // Address.
            print(x, y, a, addressLength<<2); x+=addressLength;
            
            // Separator.
            print(x+1, y, '|'); x+=3;
            
            // Hexadecimal dump.
            {
                int a0=a, ng, gs;
                if (groupSize>0) {
                    ng=lineSize/groupSize;
                    gs=groupSize;
                } else {
                    ng=1;
                    gs=lineSize;
                }
                for (int g=0; g<ng; g++, a0+=groupSize) {
                    for (int i=0; i<gs; i++) {
                        int a1=(a0+i)&addressMask;
                        int d=read8(a1);
                        print8(x, y, d); x+=2;
                    }
                    x++;
                }
            }
            
            // Separator.
            print(x, y, '|'); x+=2;            
            
            // ASCII dump.
            for (int i=0; i<lineSize; i++) {
                int a1=(a+i)&addressMask;
                char d=(char)(read8(a1)&0xff);
                if (!Tools.isPrintable(d)) d=' ';
                print(x, y, d); x++;
            }
            
            // Separator.
            print(x+1, y, '|'); x++;
            
            a=(a+lineSize)&addressMask;
        }
        
        // Highlight cursor.
        getCursorAddress(position);
        if (!addressToHexaPosition(position)) {
            int k=position.y*screenWidth+position.x;
            int cl=0; if (!editAsciiFlag) cl+=1; if (insertionModeFlag) cl+=2;
            screenBackgroundColor[k]=COLORS_C_BG[cl]; screenForegroundColor[k]=COLORS_C_FG[cl];
        }
        if (!addressToAsciiPosition(position)) {
            int k=position.y*screenWidth+position.x;
            int cl=0; if (editAsciiFlag) cl+=1; if (insertionModeFlag) cl+=2;
            screenBackgroundColor[k]=COLORS_C_BG[cl]; screenForegroundColor[k]=COLORS_C_FG[cl];
        }
    }
    
    private static final byte COLORS_L_BG[]={ COLOR_BG, COLOR_CURSOR_LINE_BG, COLOR_EDIT_BG, COLOR_CURSOR_LINE_EDIT_BG };
    private static final byte COLORS_L_FG[]={ COLOR_FG, COLOR_CURSOR_LINE_FG, COLOR_EDIT_FG, COLOR_CURSOR_LINE_EDIT_FG };
    private static final byte COLORS_C_BG[]={ COLOR_CURSOR_BG, COLOR_CURSOR2_BG, COLOR_CURSOR_EDIT_BG, COLOR_CURSOR2_EDIT_BG };
    private static final byte COLORS_C_FG[]={ COLOR_CURSOR_FG, COLOR_CURSOR2_FG, COLOR_CURSOR_EDIT_FG, COLOR_CURSOR2_EDIT_FG };
       
    //--------------------------------------------------------------------------------
    // Abstract routines.
    //--------------------------------------------------------------------------------
    /**
     * 
     * @param address
     * @return
     */
    protected abstract int read8(int address);
    
    /**
     * 
     * @param address
     * @return
     */
    protected abstract void write8(int address, int data);

}
