package org.thenesis.gendes;


/**
 * @author Guillaume Legris
 */
public final class Tools {

    // Used to convert to hexadecimal number.
    private static final char[] HEXADECIMAL_DIGITS={
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };
    
    // Table used to test if a character is printable or not.
    private static final int CHARACTER_PRINTABLE[]={
        0x00000000, 0xffffffff, 0xffffffff, 0x7fffffff
    };

    // Test if a character is printable or not.
    public static boolean isPrintable(char c) {
        if (c>127) return false;
        return (CHARACTER_PRINTABLE[c>>5]&(1<<(c&0x1f)))!=0;
    }
    
    // Convert a 8-bit word to a string with hexadecimal digits.
    public static void printWord8(char array[], int offset, int word) {
        for (int i=0, b=4; b>=0; i++, b-=4) array[offset+i]=HEXADECIMAL_DIGITS[(word>>b)&0xf];
    }
    
    // Convert a 16-bit word to a string with hexadecimal digits.
    public static void printWord16(char array[], int offset, int word) {
        for (int i=0, b=12; b>=0; i++, b-=4) array[offset+i]=HEXADECIMAL_DIGITS[(word>>b)&0xf];
    }

    // Convert a 32-bit word to a string with hexadecimal digits.
    public static void printWord32(char array[], int offset, int word) {
        for (int i=0, b=28; b>=0; i++, b-=4) array[offset+i]=HEXADECIMAL_DIGITS[(word>>b)&0xf];
    }

    // Convert a 64-bit word to a string with hexadecimal digits.
    public static void printWord64(char array[], int offset, long word) {
        for (int i=0, b=60; b>=0; i++, b-=4) array[offset+i]=HEXADECIMAL_DIGITS[(int)(word>>b)&0xf];
    }
    
    // Convert a 64-bit word to a string with hexadecimal digits.
    public static void printWordN(char array[], int offset, long word, int length) {
    	length=(length+3)&~0x3;
        for (int i=0, b=length-4; b>=0; i++, b-=4) array[offset+i]=HEXADECIMAL_DIGITS[(int)(word>>b)&0xf];
    }
    
    // Convert a 8-bit word to a string with hexadecimal digits.
    public static void appendWord8(StringBuffer s, int word) {
        for (int b=4; b>=0; b-=4) s.append(HEXADECIMAL_DIGITS[(word>>b)&0xf]);
    }
    
    // Convert a 16-bit word to a string with hexadecimal digits.
    public static void appendWord16(StringBuffer s, int word) {
        for (int b=12; b>=0; b-=4) s.append(HEXADECIMAL_DIGITS[(word>>b)&0xf]);
    }

    // Convert a 32-bit word to a string with hexadecimal digits.
    public static void appendWord32(StringBuffer s, int word) {
        for (int b=28; b>=0; b-=4) s.append(HEXADECIMAL_DIGITS[(word>>b)&0xf]);
    }

    // Convert a 64-bit word to a string with hexadecimal digits.
    public static void appendWord64(StringBuffer s, long word) {
        for (int b=60; b>=0; b-=4) s.append(HEXADECIMAL_DIGITS[(int)(word>>b)&0xf]);
    }
    
    // Convert an N-bit word to a string with hexadecimal digits.
    public static void appendWordN(StringBuffer s, long word, int length) {
    	length=(length+3)&~0x3;
        for (int b=length-4; b>=0; b-=4) s.append(HEXADECIMAL_DIGITS[(int)(word>>b)&0xf]);
    }
    
    // Convert an hexadecimal string to an int.
    public static int stringToInt(String s) {
        int n=s.length();
        int k=0;
        for (int i=0; i<n; i++) {
            char c=s.charAt(i);
            if ((c>='0')&&(c<='9')) k=(k<<4)|(c-'0');    
            else if ((c>='A')&&(c<='F')) k=(k<<4)|(10+c-'A');
            else if ((c>='a')&&(c<='f')) k=(k<<4)|(10+c-'a');
            else break;
        }
        return k;
    }

}