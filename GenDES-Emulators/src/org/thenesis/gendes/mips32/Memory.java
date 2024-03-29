package org.thenesis.gendes.mips32;

import static org.thenesis.gendes.mips32.MemoryUtil.KSEG0;
import static org.thenesis.gendes.mips32.MemoryUtil.KSEG1;
import static org.thenesis.gendes.mips32.MemoryUtil.KSEG2;
import static org.thenesis.gendes.mips32.MemoryUtil.KSEG3;
import static org.thenesis.gendes.mips32.MemoryUtil.KSEG_MASK;

import java.io.PrintStream;

import org.thenesis.gendes.malta.Malta;
import org.thenesis.gendes.mips32.util.Logger;

/**
 * int[] backed memory as described in the MIPS32 4K processor core family
 * software user's manual.
 * <p>
 * although memory can be considered to be a device, it doesn't actually
 * implement the Device interface as it is a special case (referred to directly
 * by cpu).
 */
public final class Memory extends Device {
	
	private static final Logger log = new Logger("Memory");
	
	/** backing data as int (shift addr left 2 for index) */
	private final int[] data;
	private final int wordAddrXor;
	private final int halfWordAddrXor;
	private final boolean littleEndian;
	private final Entry[] entries = new Entry[16];
	private final Malta malta;
	private final Cpu cpu;
	
	private boolean kernelMode;
	private int asid;
	
	public Memory (Cpu cpu, int size, boolean littleEndian) {
		super(null, 0);
		this.cpu = cpu;
		this.data = new int[size >>> 2];
		this.littleEndian = littleEndian;
		this.wordAddrXor = littleEndian ? 0 : 3;
		this.halfWordAddrXor = littleEndian ? 0 : 2;
		for (int n = 0; n < entries.length; n++) {
			entries[n] = new Entry();
		}
		this.malta = new Malta(this, KSEG1);
	}
	
	public Entry getEntry (int n) {
		return entries[n];
	}
	
	public int getAsid () {
		return asid;
	}
	
	public void setAsid (int asid) {
		this.asid = asid;
	}
	
	@Override
	public void init () {
		log.println("init memory");
		getCpu().getSymbols().put(KSEG0, "KSEG0");
		getCpu().getSymbols().put(KSEG1, "KSEG1");
		getCpu().getSymbols().put(KSEG2, "KSEG2");
		getCpu().getSymbols().put(KSEG3, "KSEG3");
		malta.init();
	}
	
	public boolean isLittleEndian () {
		return littleEndian;
	}
	
	public boolean isKernelMode () {
		return kernelMode;
	}
	
	public void setKernelMode (boolean kernelMode) {
		this.kernelMode = kernelMode;
	}
	
	public Malta getMalta () {
		return malta;
	}
	
	@Override
	public final int loadWord (final int vaddr) {
		if ((vaddr & 3) == 0) {
			final int i = index(vaddr, false);
			if (i >= 0) {
				return data[i];
			} else {
				return malta.loadWord(vaddr);
			}
		} else {
			throw new CpuException(new CpuExceptionParams(CpuConstants.EX_ADDR_ERROR_LOAD, vaddr));
		}
	}
	
	@Override
	public final void storeWord (final int vaddr, final int value) {
		if ((vaddr & 3) == 0) {
			int i = index(vaddr, true);
			if (i >= 0) {
				data[i] = value;
			} else {
				malta.storeWord(vaddr, value);
			}
		} else {
			throw new CpuException(new CpuExceptionParams(CpuConstants.EX_ADDR_ERROR_STORE, vaddr));
		}
	}
	
	@Override
	public final short loadHalfWord (final int vaddr) {
		if ((vaddr & 1) == 0) {
			final int i = index(vaddr, false);
			if (i >= 0) {
				final int w = data[i];
				// 0,2 -> 2,0 -> 16,0
				final int s = ((vaddr & 2) ^ halfWordAddrXor) << 3;
				return (short) (w >>> s);
			} else {
				return malta.loadHalfWord(vaddr);
			}
		} else {
			throw new CpuException(new CpuExceptionParams(CpuConstants.EX_ADDR_ERROR_LOAD, vaddr));
		}
	}
	
	@Override
	public final void storeHalfWord (final int vaddr, final short value) {
		if ((vaddr & 1) == 0) {
			int i = index(vaddr, true);
			if (i >= 0) {
				final int w = data[i];
				// 0,2 -> 2,0 -> 16,0
				final int s = ((vaddr & 2) ^ halfWordAddrXor) << 3;
				final int andm = ~(0xffff << s);
				final int orm = (value & 0xffff) << s;
				data[i] = (w & andm) | orm;
			} else {
				malta.storeHalfWord(vaddr, value);
			}
		} else {
			throw new CpuException(new CpuExceptionParams(CpuConstants.EX_ADDR_ERROR_STORE, vaddr));
		}
	}
	
	/** load byte according to kernel mode and asid */
	@Override
	public final byte loadByte (final int vaddr) {
		final int i = index(vaddr, false);
		if (i >= 0) {
			final int w = data[i];
			// 0,1,2,3 xor 0 -> 0,1,2,3
			// 0,1,2,3 xor 3 -> 3,2,1,0
			// 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
			final int s = ((vaddr & 3) ^ wordAddrXor) << 3;
			return (byte) (w >>> s);
		} else {
			return malta.loadByte(vaddr);
		}
	}
	
	@Override
	public final void storeByte (final int vaddr, final byte value) {
		int i = index(vaddr, true);
		if (i >= 0) {
			final int w = data[i];
			// if xor=0: 0,1,2,3 -> 0,8,16,24
			// if xor=3: 0,1,2,3 -> 3,2,1,0 -> 24,16,8,0
			final int s = ((vaddr & 3) ^ wordAddrXor) << 3;
			final int andm = ~(0xff << s);
			final int orm = (value & 0xff) << s;
			data[i] = (w & andm) | orm;
		} else {
			malta.storeByte(vaddr, value);
		}
	}
	
	/**
	 * translate virtual address to physical. store affects dirty bit if true
	 * and type of exception thrown if address is invalid.
	 */
	public final int index (final int vaddr, final boolean store) {
		final boolean km = this.kernelMode;
		// useg > kseg3 > kseg2 > kseg1 > kseg0
		if (km && vaddr < KSEG1) {
			// kseg0 (direct, fast)
			return (vaddr & KSEG_MASK) >> 2;
		} else if (vaddr >= 0 || (km && vaddr >= KSEG2)) {
			// useg/kuseg/kseg2/kseg3 (translated, slow)
			return lookup(vaddr, store) >> 2;
		} else if (km && vaddr < KSEG2) {
			// kseg1 (malta/direct, very slow)
			// really this should be same as kseg0, but we want to use the return value
			// to know if we should call malta instead
			//return vaddr & KSEG_MASK;
			return -1;
		} else {
			throw new RuntimeException("cannot translate kseg as user: " + Integer.toHexString(vaddr));
		}
	}
	
	public final int probe (final int vpn2) {
		for (int n = 0; n < entries.length; n++) {
			Entry e = entries[n];
			if (e.virtualPageNumber2 == vpn2 && (e.addressSpaceId == asid || e.global)) {
				log.println("tlb probe = " + n);
				return n;
			}
		}
		log.println("tlb probe miss");
		return -1;
	}
	
	/**
	 * lookup virtual address using tlb
	 */
	private final int lookup (final int vaddr, final boolean store) {
		int i = lookup1(vaddr, store);
		// need to populate the packed entries...
		//int j = lookup2(vaddr, store);
		return i;
	}
	
	private final int lookup1 (final int vaddr, final boolean store) {	
		final int vpn2 = CpuFunctions.vpn2(vaddr);
		final int eo = CpuFunctions.evenodd(vaddr);
		boolean refill = true;
		
		// log.debug("lookup vaddr=" + Integer.toHexString(vaddr) + " asid=" +
		// Integer.toHexString(asid) + " vpn2=" + Integer.toHexString(vpn2));
		
		for (int n = 0; n < entries.length; n++) {
			Entry e = entries[n];
			// log.debug("entry[" + n + "]=" + e);
			
			if (e.virtualPageNumber2 == vpn2 && (e.addressSpaceId == asid || e.global)) {
				// log.debug("tlb hit");
				EntryData d = e.data[eo];
				if (!d.valid) {
					log.println("entry invalid (not a refill)...");
					refill = false;
					break;
				}
				
				if (store && !d.dirty) {
					// XXX set dirty if write? how does os know?
					d.dirty = true;
				}
				
				final int paddr = (d.physicalFrameNumber << 12) | (vaddr & 0xfff);
				// log.debug("translated " + Integer.toHexString(vaddr) + " to "
				// + Integer.toHexString(paddr));
				return paddr;
			}
		}
		
		log.println("tlb miss");
		
		//		for (int n = 0; n < entries.length; n++) {
		//			Entry e = entries[n];
		//			log.println("entry[" + n + "]=" + e);
		//		}
		
		// TODO also need to throw modified exception if page is read only...
		throw new CpuException(new CpuExceptionParams(store ? CpuConstants.EX_TLB_STORE : CpuConstants.EX_TLB_LOAD, vaddr, refill));
	}
	
	/** load word without address translation */
	public final int loadWordKernel (final int vaddr) {
		final int i = (vaddr & KSEG_MASK) >>> 2;
				return i < data.length ? data[i] : 0;
	}
	
	/** load byte without address translation */
	public final byte loadByteKernel (final int vaddr) {
		final int w = loadWordKernel(vaddr);
		final int s = ((vaddr & 3) ^ wordAddrXor) << 3;
		return (byte) (w >>> s);
	}
	
	/** load boxed word, null if unmapped */
	public Integer loadWordSafe (final int vaddr) {
		// hack to translate addr
		// FIXME this doesn't translate
		final int a = vaddr & KSEG_MASK;
		final int i = a >>> 2;
		if (i >= 0 && i < data.length) {
			final int w = data[i];
			return Integer.valueOf(w);
			
		} else {
			return null;
		}
	}
	
	/** load boxed word, null if unmapped */
	public Long loadDoubleWordSafe (final int paddr) {
		final int i = paddr >>> 2;
				if (i >= 0 && i < data.length - 1) {
					final long w1 = data[i] & 0xffff_ffffL;
					final long w2 = data[i + 1] & 0xffff_ffffL;
					// XXX might need swap
					return Long.valueOf((w1 << 32) | w2);
					
				} else {
					return null;
				}
	}
	
	public void print (PrintStream ps) {
		ps.println("memory map");
		// for each 1mb block in words
		for (int j = 0; j < data.length; j += 0x40000) {
			float c = 0;
			for (int i = 0; i < 0x40000; i++) {
				if (data[j + i] != 0) {
					c++;
				}
			}
			ps.println("  addr 0x" + Integer.toHexString(j * 4) + " usage " + (c / 0x100000));
		}
	}
	
	@Override
	public Cpu getCpu () {
		return cpu;
	}
	
	@Override
	public String toString () {
		return String.format("Memory[size=%d le=%s]", data.length, littleEndian);
	}
}
