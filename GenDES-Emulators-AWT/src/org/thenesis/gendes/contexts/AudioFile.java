package org.thenesis.gendes.contexts;

import java.io.DataOutputStream;
import java.io.OutputStream;

public class AudioFile {

	private DataOutputStream outputStream=null;
	
	public AudioFile() {		
	}
	
    public boolean start(OutputStream os, int sampleRate) {
    	if (outputStream!=null) stop();

    	// Write a SUN .au audio file.
    	DataOutputStream dos=new DataOutputStream(os);
    	try {
	    	dos.writeInt(0x2e736e64); // Magic number: '.snd'.
	    	dos.writeInt(24); // Data offset: 24.
	    	dos.writeInt(0xffffffff); // Length: unknown.
	    	dos.writeInt(2); // Encoding: 2 (8-bit PCM).
	    	dos.writeInt(sampleRate); // Sample rate.
	    	dos.writeInt(2); // Number of channels: 2.
	    	outputStream=dos;
    		return false;
    	} catch (Exception e) {
    		return true;
    	}
    }
    
    public void stop() {
    	outputStream=null;
    }
	
    public boolean write(int offset, int length, byte data[]) {
    	if (outputStream==null) return true;
    	
    	try {
    		outputStream.write(data, 0, length);
    		return false;
    	} catch (Exception e) {
    		stop();
    		return true;
    	}
    }
}
