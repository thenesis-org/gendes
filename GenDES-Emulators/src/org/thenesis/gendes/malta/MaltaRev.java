package org.thenesis.gendes.malta;

import org.thenesis.gendes.mips32.Device;

public class MaltaRev extends Device {
	
	public MaltaRev (Device parent, final int baseAddr) {
		super(parent, baseAddr);
	}
	
	@Override
	public boolean isMapped (int addr) {
		return offset(addr) == 0;
	}
	
	@Override
	public int loadWord (final int addr) {
		return 1;
	}
}
