package org.thenesis.gendes;

public class FastString {
    public int capacity, length;
    public char buffer[];

    public void free() {
    	capacity=0; length=0; buffer=null;    	
    }
    
    public void setCapacity(int c) {
    	capacity=c; length=0; buffer=new char[c];
    }
    
	public void clear() {
    	length=0;
    }
    
	public void clamp(int l) {
		if (l<0) l=0;
		if (l<length) length=l;
	}
	
    public void append(char c) {
    	if (length<capacity) {
    		buffer[length]=c; length++;
    	}
    }
    
    public void append(FastString s) {
    	int l=s.length, dc=capacity-length;
    	if (l>dc) l=dc;
    	for (int i=0; i<l; i++) buffer[length+i]=s.buffer[i];
		length+=l;
    }
    
    public void append(FastString s, int start, int l) {
    	int sc=s.length-start, dc=capacity-length;
    	if (sc<0) return;
    	if (l>sc) l=sc;
    	if (l>dc) l=dc;
    	for (int i=0; i<l; i++) buffer[length+i]=s.buffer[start+i];
		length+=l;
    }
    
    public void append(String s) {
    	int l=s.length(), dc=capacity-length;
    	if (l>dc) l=dc;
		s.getChars(0, l, buffer, length);
		length+=l;
    }

    public void append(String s, int start, int l) {
    	int sc=s.length()-start, dc=capacity-length;
    	if (sc<0) return;
    	if (l>sc) l=sc;
    	if (l>dc) l=dc;
		s.getChars(start, l, buffer, length);
		length+=l;
    }
    
    public void append(int word, int l) {
    	l=(l+3)&~0x3;
        for (int b=l-4; length<capacity && b>=0; length++, b-=4) {
        	buffer[length]=HEXADECIMAL_DIGITS[(word>>b)&0xf];
        }
    }
    
    public void append(long word, int l) {
    	l=(l+3)&~0x3;
        for (int b=l-4; length<capacity && b>=0; length++, b-=4) {
        	buffer[length]=HEXADECIMAL_DIGITS[(int)((word>>b)&0xf)];
        }
    }
    
    // Used to convert to hexadecimal number.
    private static final char[] HEXADECIMAL_DIGITS={
        '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

}
