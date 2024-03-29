package org.thenesis.gendes.malta;

import java.util.Arrays;

import org.thenesis.gendes.mips32.CpuConstants;
import org.thenesis.gendes.mips32.CpuExceptionParams;
import org.thenesis.gendes.mips32.Device;
import org.thenesis.gendes.mips32.util.Logger;

/**
 * the intel 82371AB (PIIX4) southbridge (and bits of the SMSC FDC37M81 super io
 * controller)
 */
public class PIIX4 extends MultiDevice {
	
	// I8259 programmable interrupt controller
	public static final int M_PIC_MASTER = 0x20;
	
	// I8253 programmable interval timer
	public static final int M_PIT = 0x40;
	
	// I8042 ps/2 keyboard/mouse microcontroller (provided by superio)
	/** keyboard data read/write */
	public static final int M_KEYBOARD = 0x60;
	
	// real time clock
	public static final int M_RTC = 0x70;
	
	public static final int M_PIC_SLAVE = 0xa0;
	
	public static final int M_DMA2_MASK_REG = 0xd4;
	
	// 16650 uarts (provided by superio)
	public static final int M_COM2 = 0x2f8;
	public static final int M_COM1 = 0x3f8;
	
	private static final Logger log = new Logger("PIIX4");
	
	private final Uart com1;
	private final Uart com2;
	private final PIC pic1;
	private final PIC pic2;
	private final KBC kbc;
	private final RTC rtc;
	private final PIT pit;
	
	public PIIX4(final Device parent, final int baseAddr) {
		super(parent, baseAddr);
		this.com1 = new Uart(this, baseAddr + M_COM1, 1, "Uart:COM1");
		this.com1.setConsole(true);
		this.com2 = new Uart(this, baseAddr + M_COM2, 1, "Uart:COM2");
		this.pic1 = new PIC(this, baseAddr + M_PIC_MASTER, true);
		this.pic2 = new PIC(this, baseAddr + M_PIC_SLAVE, false);
		this.kbc = new KBC(this, baseAddr + M_KEYBOARD);
		this.rtc = new RTC(this, baseAddr + M_RTC);
		this.pit = new PIT(this, baseAddr + M_PIT);
		this.devices.addAll(Arrays.asList(com1, com2, pic1, pic2, kbc, rtc, pit));
	}
	
	@Override
	public void init () {
		getCpu().getSymbols().init(PIIX4.class, "M_", null, baseAddr, 1);
		super.init();
	}
	
	@Override
	public boolean isMapped (final int addr) {
		final int offset = offset(addr);
		return offset >= 0 && offset < 0xd00;
	}
	
	@Override
	public void storeByte (final int addr, final byte value) {
		final int offset = offset(addr);
		
		switch (offset) {
			case M_DMA2_MASK_REG:
				// information in asm/dma.h
				log.println("enable dma channel 4+ ignored" + value);
				return;
				
			default:
				super.storeByte(addr, value);
		}
	}
	
	@Override
	public void fire (int irq) {
		if (irq >= 0 && irq < 8) {
			if (pic1.isMasked(irq)) {
				log.println("irq masked: %d", irq);
				return;
			}
		} else if (irq >= 8 && irq < 16) {
			if (pic1.isMasked(MaltaUtil.IRQ_CASCADE) || pic2.isMasked(irq - 8)) {
				log.println("irq masked: %d", irq);
				return;
			}
		} else {
			throw new RuntimeException("invalid irq " + irq);
		}
		
		// should this queue the interrupts here?
		// should this set the irq in the gt?
		getCpu().addException(new CpuExceptionParams(CpuConstants.EX_INTERRUPT, MaltaUtil.INT_SOUTHBRIDGE, irq));
		return;
	}
}
