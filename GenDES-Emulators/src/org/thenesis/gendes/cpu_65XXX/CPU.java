package org.thenesis.gendes.cpu_65XXX;

import org.thenesis.gendes.cpu_gameboy.CPUEvent;
import org.thenesis.gendes.cpu_gameboy.CPUListener;
import org.thenesis.gendes.debug.Breakpoint;
import org.thenesis.gendes.debug.BreakpointList;

public abstract class CPU implements InstructionSet {
	// Models.
	public static final int
		MODEL_6502=			0,
		MODEL_65C02=		1,
		MODEL_65SC02=		2,
		MODEL_65816=		3,
		MODELS_COUNT=		4;
	
	// PS register flags.
	public static final int 
		PS_C=	0x01, // Status flag: Carry.
		PS_Z=	0x02, // Status flag: Zero.
		PS_I=	0x04, // Status flag: Interrupt request disable.
		PS_D=	0x08, // Status flag: Decimal mode.
		PS_B=	0x10, // Status flag: Break command.
		PS_V=	0x40, // Status flag: oVerflow.
		PS_N=	0x80; // Status flag: Negative.
	
	// Vectors.
	public static final int
		VECTOR_NMI=		0xfffa,
		VECTOR_BOOT=	0xfffc,
		VECTOR_IRQ=		0xfffe;
	
	// Trace mode.
    public static final int
    	TRACE_MODE_RUN=         0,
    	TRACE_MODE_STEP_INTO=   1,
    	TRACE_MODE_STEP_OVER=   2,
    	TRACE_MODE_STEP_OUT=    3,
    	TRACE_MODE_STEP_INTO_I= 4,
    	TRACE_MODE_STEP_OUT_I=  5;
	
	public int model;

	// Registers.
	public int rPC; // Program Counter (16 or 24 bits).
	public int rSP; // Stack Pointer (8 bits).
	public int rPS; // Processor status register (8 bits) (not used here).
	public int rA; // Accumulator (8 bits).
	public int rX; // X index register (8 bits).
	public int rY; // Y index register (8 bits).

	// Signals.
	public int sIRQ; // Interrupt request (input): 0=normal, 1=interrupt request.
	public int sNMI; // Non-Maskable Interrupt (input): 0=normal, 1=NMI request.
	public int sNMI_last;
	public int sRST; // Reset (input): 0=normal, 1=reset request.

	public int sRDYi; // Ready (input): 0=normal, 1=halt the processor at the end of the current cycle.
	public int sRDYo; // Ready (output): signal a wait.
	public int sSOB; // Set overflow bit in PS (rarely used) (input).

	public int sRW; // Read/Write signal (output): 0=write, 1=read.
	public int sML; // Memory lock (output): 0=normal, 1=indicates a read-modify-write instruction (ASL, DEC, INC, LSR, ROL, ROR, TRB, TSB) (rarely used).
	public int sSYNC; // Synchronize with opcode fetch (output): 0=normal, 1=indicates an opcode fetch.

	public int cpuCycles; // Number of CPU cycles for the current instruction.
		
    // True if the CPU is running.
    public boolean running=false;
	// True if we must break execution.
    public boolean breakExecution=false;
	// True if the CPU is halted.
    public boolean executionHalted=false;
    // True if a halt instruction has just been executed while interrupts was disabled and an interrupt is requested.
    public boolean waitFlag=false;
	// True if interrupts are enabled.
    public boolean interruptsEnabled=false;
    // True when an ei or rti instruction has just been executed.
    public boolean interruptsDelayed=false;
	// True if an interruption has been requested.
    public boolean interruptRequested=false;
	
	// Number of cycles elapsed since the last call to execute().
	public int cycles;

    // Decoder.
    public Decoder decoder=new Decoder();
    
    // Listener.
    private CPUListener listener;
    private CPUEvent event;
    
    // Debug mode flag.
    private boolean debugModeActivated=false;
    // Trace mode.
    private int traceMode=TRACE_MODE_RUN;
    // List of instruction breakpoints.
    BreakpointList breakpoints=new BreakpointList();
    // Subroutine recursion depth.
    int subroutineRecursionDepth;
    // Interrupt recursion depth.
    int interruptRecursionDepth;
    // Indicates if we must trace interrupt.
    boolean traceInterruptsFlag;
    
	
	//********************************************************************************
	// User methods.
	//********************************************************************************
	public CPU() {
//        DecoderBuilder.build(decoder);
	}

	public final void setModel(int m) {
		switch (m) {
		case MODEL_6502:
		case MODEL_65C02:
		case MODEL_65SC02:
		case MODEL_65816: break;
		default: return;
		}
		model=m;


		// Initialize opcode table.
/*		{
			const OpcodeTableEntry *entry;

			// Initialize all opcodes to illegal.
			entry=&OPCODE_TABLE[ILLEGAL_OPCODE[model]];
			for (uint32 i=0; i<256; i++) opcodeTable[i]=entry;

			// Set opcodes supported by the current model.
			uint32 modelMask=(uint32)(1<<model);
			entry=OPCODE_TABLE;
			while (entry->routine!=NULL) {
				if ((entry->models&modelMask)!=0) {
					uint32 o=(uint32)(entry->opcode&0xff);
					opcodeTable[o]=entry;
				}
				entry++;
			}
		}
*/		

		rPC=0x0000;
		rSP=0xff;
		rPS=0x20;
		rA=0;
		rX=0;
		rY=0;

		// Input signals.
		sIRQ=0;
		sNMI=0; sNMI_last=0;
		sRST=0;
		sRDYi=0;
		sSOB=0;

		// Output signals.
		sRDYo=0;
		sRW=1;
		sML=0;
		sSYNC=1;

		cpuCycles=0;

		// Internal state.
		waitFlag=false;
	}
	
    /** Set the listener. */
    public final void setListener(CPUListener l, CPUEvent e) {
        if (running) return;
        listener=l;
        event=e;
    }

    /** Activate the debug mode when flag is true. */
    public final void activateDebugMode(boolean flag) {
        if (running) return;
        debugModeActivated=flag;
    }

    /** Set the step mode. Only works when the debug mode is activated. */
    public final void setTraceMode(int sm, boolean traceInt) {
        if (running) return;
        
        switch (sm) {
        case TRACE_MODE_RUN:
            traceMode=sm;
            break;
        case TRACE_MODE_STEP_OVER:
            traceMode=sm;
            break;
        case TRACE_MODE_STEP_INTO:
            traceMode=sm;
            break;
        case TRACE_MODE_STEP_OUT:
            traceMode=sm;
            break;
        case TRACE_MODE_STEP_INTO_I:
            traceMode=sm;
            break;
        case TRACE_MODE_STEP_OUT_I:
            traceMode=sm;
            break;
        default: return;
        }
        traceInterruptsFlag=traceInt;
        subroutineRecursionDepth=0;
        interruptRecursionDepth=0;
    }

    /** Get the list of breakpoints. */
    public final BreakpointList getBreakpoints() {
        return breakpoints;
    }
    
    /** Reset the CPU. */
	public final void reset() {
        // FIXME: reset should be implemented in the execute routine.
		
		// Registers.
		rSP=0xff;
		rPS=0x34;
		rA=0;
		rX=0;
		rY=0;

		// Input signals.
		sNMI_last=0;
		
		// Output signals.
		sRDYo=0;
		sRW=1;
		sML=0;
		sSYNC=1;

		// Load PC.
		rPC=(cpuRead8(VECTOR_BOOT)<<8) | cpuRead8(VECTOR_BOOT+1);

		breakExecution=false;
		executionHalted=false;
        waitFlag=false;
		interruptsEnabled=false;
        interruptsDelayed=false;
	}
 
    public final void requestInterrupt(boolean flag) {
        interruptRequested=flag;
        if (flag) executionHalted=false;
    }
    
	//********************************************************************************
	// Abstract routines.
	//********************************************************************************

    /**
     * Perform a CPU address space read.
     * It MUST returns a value between 0 and 255.
     * The address may be <0 or >0xffff so it MUST be anded with 0xffff.
     */
    public abstract int cpuRead8(int addr);

    /**
     * Performs a CPU address space write.
     * The data MAY NOT be in the range 0<=data<=255, so it must be anded if necessary. 
     * The address may be <0 or >0xffff so it MUST be anded with 0xffff
     */
    public abstract void cpuWrite8(int addr, int data);

    /**
     * Called when an interrupt is acknowledged.
     * It must return the address of the interrupt routine.
     */
    public abstract int acknowledgeInterrupt();

    /**
     * Called when the stop instruction is executed.
     * It must return true if the CPU can be stopped.
     */
    public abstract boolean stop();

    
	//********************************************************************************
	// Instruction execution.
	//********************************************************************************
    /**
     * Execute instructions until the number of cycles requested are elapsed.
     * It returns the number of cycles effectively elapsed. 
     */
	public final int execute(int requestedCycles) {
	    int opcode, instruction, addressingMode, operandAddress, srcOperand, dstOperand;
	    boolean read, write;
        Breakpoint breakpoint;
        
        if (executionHalted || (sRST!=0)) return requestedCycles;

        running=true;
        breakExecution=false;
        cycles=0;
  /*      
        do {
        	// NMI. The signal is edge sensitive.
        	if ((sNMI!=0)&&(sNMI_last==0)) { sNMI_last=sNMI; nmi(); continue; }
        	sNMI_last=sNMI;

        	// IRQ.
        	if (((rPS&PS_I)==0) && (sIRQ!=0)) { interrupt(); continue; }
            
        	sSYNC=1; opcode=cpuRead8(rPC++); sSYNC=0;
            instruction=0; addressingMode=0; read=false; write=false;
            
            operandAddress=0; srcOperand=0;
            switch (addressingMode) {
            case AM_ILL: break;
            case AM_IMP: break;
            case AM_ACC: srcOperand=rA; break;
            case AM_X: srcOperand=rX; break;
            case AM_Y: srcOperand=rY; break;
            case AM_ZERO: srcOperand=0; break;
            case AM_IMM: operandAddress=rPC++; break;
            case AM_REL: operandAddress=rPC++; break;
            case AM_Z: operandAddress=cpuRead8(rPC++); break;
            case AM_ZX: operandAddress=(cpuRead8(rPC++)+rX)&0xff; break;
            case AM_ZY: operandAddress=(cpuRead8(rPC++)+rY)&0xff; break;
            case AM_ZI: operandAddress=cpuRead8(rPC++); operandAddress=cpuRead8(operandAddress) | (cpuRead8(operandAddress+1)<<8); break;
            case AM_ZXI: operandAddress=(cpuRead8(rPC++)+rX)&0xff; operandAddress=cpuRead8(operandAddress) | (cpuRead8(operandAddress+1)<<8); break;
            case AM_ZIY: operandAddress=cpuRead8(rPC++); operandAddress=(cpuRead8(operandAddress) | (cpuRead8(operandAddress+1)<<8))+rY; break;
            case AM_ZREL: break; // ???
            case AM_A: operandAddress=cpuRead8(rPC++) | (cpuRead8(rPC++)<<8); break;
            case AM_AX: operandAddress=(cpuRead8(rPC++) | (cpuRead8(rPC++)<<8))+rX; break;
            case AM_AY: operandAddress=(cpuRead8(rPC++) | (cpuRead8(rPC++)<<8))+rY; break;
            case AM_AI1:
            case AM_AI2:
            case AM_AXI:
            case AM_BREAK:
            }
            
            if (read) srcOperand=cpuRead8(operandAddress);
            
            dstOperand=0;
            switch (instruction) {
            case I_ILLEGAL:
            	break;
            case I_NOP:
            	break;
            case I_LD:
            	dstOperand=srcOperand;
            	rPS&=~(PS_Z|PS_N); if (dstOperand==0) rPS|=PS_Z; if ((dstOperand&0x80)!=0) rPS|=PS_N;
            	break;
            case I_ST:
            	dstOperand=srcOperand;
            	break;
            case I_PH:
            	break;
            case I_PL:
            	break;
            case I_PLP:
            	break;
            case I_TAX:
            case I_TAY:
            case I_TXA:
            case I_TYA:
            case I_TSX:
            	dstOperand=srcOperand;
            	rPS&=~(PS_Z|PS_N); if (dstOperand==0) rPS|=PS_Z; if ((dstOperand&0x80)!=0) rPS|=PS_N;
            	break;
            case I_TXS:
            	break;
            case I_ADC_02:
            case I_ADC_C02:
            case I_SBC_02:
            case I_SBC_C02:
            case I_CMP:
            case I_INC:
            case I_DEC:
            case I_AND:
            case I_EOR:
            case I_OR:
            case I_ASL:
            case I_LSR:
            case I_ROL:
            case I_ROR:
            case I_RMB:
            case I_SMB:
            case I_TRB:
            case I_BIT:
            case I_BRA:
            case I_B:
            case I_BBR:
            case I_BBS:
            case I_JMP_4C:
            case I_JMP_6C_02:
            case I_JMP_6C_C02:
            case I_JMP_7C:
            case I_JSR:
            case I_RTS:
            case I_CLC:
            case I_CLD:
            case I_CLI:
            case I_CLV:
            case I_SEC:
            case I_SED:
            case I_SEI:
            case I_RTI:
            case I_BRK_02:
            case I_BRK_C02:
            case I_WAI:            
            case I_STP:
            }
            
            if (write) cpuWrite8(operandAddress, dstOperand);           
        	
            // Debugging.
            if (debugModeActivated) {
                breakpoint=breakpoints.findBreakpoint(rPC&0xffff);
                if ((breakpoint!=null) && (listener!=null)) {
                    event.type=CPUEvent.EVENT_BREAKPOINT; event.address=rPC&0xffff; event.breakpoint=breakpoint;
                    listener.onTrace(event);
                }
                
                if (traceInterruptsFlag || (interruptRecursionDepth<=0)) {
                    switch (traceMode) {
                    case TRACE_MODE_STEP_INTO:
                        event.type=CPUEvent.EVENT_STEP_INTO; event.address=rPC&0xffff; event.breakpoint=null;
                        listener.onTrace(event);
                        break;
                    case TRACE_MODE_STEP_OVER:
                        if (subroutineRecursionDepth<=0) {
                            subroutineRecursionDepth=0;
                            event.type=CPUEvent.EVENT_STEP_OVER; event.address=rPC&0xffff; event.breakpoint=null;
                            listener.onTrace(event);
                        }
                        break;
                    }
                }        
            }
            
        } while ((cycles<requestedCycles) && !breakExecution);
        */
        running=false;
		requestedCycles=cycles;
		cycles=0;
		return requestedCycles;
	}
	
	private final void nmi() {
        if (debugModeActivated) {
            interruptRecursionDepth++;
            if (traceMode==TRACE_MODE_STEP_INTO_I) {
                event.type=CPUEvent.EVENT_STEP_INTO_I; event.address=rPC&0xffff; event.breakpoint=null;
                listener.onTrace(event);
            }
        }
		
	}
	
	private final void interrupt() {
        if (debugModeActivated) {
            interruptRecursionDepth++;
            if (traceMode==TRACE_MODE_STEP_INTO_I) {
                event.type=CPUEvent.EVENT_STEP_INTO_I; event.address=rPC&0xffff; event.breakpoint=null;
                listener.onTrace(event);
            }
        }
		/*                
        // Note:
        interruptsEnabled=false; interruptsDelayed=false;
        cpuWrite8(rSP-1, rPC>>8); cpuWrite8(rSP-2, rPC); rSP=(rSP-2)&0xffff;
        rPC=acknowledgeInterrupt();
        cycles+=2*4+3*4; // 2 instruction cycles for pushing PC + 3 JMP instruction cycles.
*/                		
	}
}
