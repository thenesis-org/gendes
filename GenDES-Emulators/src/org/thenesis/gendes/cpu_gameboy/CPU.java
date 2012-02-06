package org.thenesis.gendes.cpu_gameboy;

import org.thenesis.gendes.debug.Breakpoint;
import org.thenesis.gendes.debug.BreakpointList;

//import static org.thenesis.emulation.cpu_gameboy.InstructionSet.*;

//********************************************************************************
// Main CPU class.
//********************************************************************************
public abstract class CPU implements InstructionSet {
	// Registers.
	public int rPC; // Program counter (16 bits).
	public int rSP; // Stack pointer (16 bits).
	public int rA; // Accumulator (8 bits).
	public int rB; // General purpose register (8 bits).
	public int rC; // General purpose register (8 bits).
	public int rD; // General purpose register (8 bits).
	public int rE; // General purpose register (8 bits).
	public int rF; // Flags register (8 bits).
	public int rHL; // General purpose register (16 bits).

	// Register F flags.
	public static final short
		F_ZERO = 		0x80, // Zero Flag.
		F_SUBTRACT = 	0x40, // Subtract/negative flag.
		F_HALFCARRY = 	0x20, // Half carry flag.
		F_CARRY = 		0x10; // Carry flag.

	// Trace modes.
    public static final int
		TRACE_MODE_RUN=         0,
		TRACE_MODE_STEP_INTO=   1,
		TRACE_MODE_STEP_OVER=   2,
		TRACE_MODE_STEP_OUT=    3,
		TRACE_MODE_STEP_INTO_I= 4,
		TRACE_MODE_STEP_OUT_I=  5;
    
/*
	// States of the CPU.
	private static final int
		STATE_RESET=			0,
		STATE_INTERRUPT=		1,
		STATE_HALTED=			2,
		STATE_INSTRUCTION=		3;
*/

    // Global state of the CPU.
//    public int state=STATE_RESET;
    // True if the CPU is running.
    public boolean running=false;
	// True if we must break execution.
    public boolean breakExecution=false;
	// True if the CPU is halted.
    public boolean executionHalted=false;
    // True if a halt instruction has just been executed while interrupts was disabled and an interrupt is requested.
    public boolean haltFlag=false;
    // True if a reset is requested.
//    public boolean resetRequested=true;
	// True if interrupts are enabled.
    public boolean interruptsEnabled=false;
    // True when an ei or rti instruction has just been executed.
    public boolean interruptsDelayed=false;
	// True if an interruption has been requested.
    public boolean interruptRequested=false;
	
	// Number of cycles elapsed since the last call to execute().
	public int cycles;

    // Decoder.
    private byte opcodeToInstruction[];
    private byte opcodeToSrcAM[];
    private byte opcodeToDstAM[];

    // Listener.
    private CPUListener listener;
    private CPUEvent event;
    
    // Debug mode flag.
    private boolean debugModeActivated=false;
    // Trace mode.
    private int traceMode=TRACE_MODE_RUN;
    // List of instruction breakpoints.
    private BreakpointList breakpoints=new BreakpointList();
    // Subroutine recursion depth.
    private int subroutineRecursionDepth;
    // Interrupt recursion depth.
    private int interruptRecursionDepth;
    // Indicates if we must trace interrupt.
    private boolean traceInterruptsFlag;
    
	
	//********************************************************************************
	// User methods.
	//********************************************************************************
	public CPU() {
		Decoder decoder=new Decoder();
		decoder.build();
		opcodeToInstruction=decoder.opcodeToInstruction;
		opcodeToSrcAM=decoder.opcodeToSrcAM;
		opcodeToDstAM=decoder.opcodeToDstAM;
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
/*
    public final void reset2() {
		resetRequested=true;
		updateState();
	}
*/	
    /** Reset the CPU. */
	public final void reset() {
        // FIXME: reset should be implemented in the execute routine.
        // TODO: reset works like a mode 0 interrupt.
	    rPC=0x0000;
		breakExecution=false;
		executionHalted=false;
        haltFlag=false;
		interruptsEnabled=false;
        interruptsDelayed=false;
	}
 
    public final void requestInterrupt(boolean flag) {
        interruptRequested=flag;
        if (flag) executionHalted=false;
//		updateState();
    }
/*
    private final void updateState() {
    	if (resetRequested) state=STATE_RESET;
    	else if (interruptsEnabled && interruptRequested) state=STATE_INTERRUPT;
    	else if (executionHalted) state=STATE_HALTED;
    	else state=STATE_INSTRUCTION;
    }
*/
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
        if (executionHalted) return requestedCycles;

        running=true;
        breakExecution=false;
        cycles=0;
        
        do {
            // Interrupts.
            if (interruptRequested && interruptsEnabled) {
                if (debugModeActivated) debugInterrupt();
                
                // Note:
                // The Game Boy CPU use the mode 0 interrupt response of the Z80 (not sure in fact...).
                // This means that during the interrupt acknowledge operation, the interrupting
                // device provides an instruction (up to 3 bytes) on the data bus.
                // For the Game Boy CPU, this is apparently a jmp instruction.
                interruptsEnabled=false; interruptsDelayed=false;
                cpuWrite8(rSP-1, rPC>>8); cpuWrite8(rSP-2, rPC); rSP=(rSP-2)&0xffff;
                rPC=acknowledgeInterrupt();
                cycles+=2*4+3*4; // 2 instruction cycles for pushing PC + 3 JMP instruction cycles.
//                updateState();
            } else {
                interruptsEnabled=interruptsDelayed;
//              updateState();

        	    int opcode, srcOperand, dstOperand;

                // Read and decode the opcode.
    			opcode=cpuRead8(rPC); cycles+=4;
                if (!haltFlag) rPC++; haltFlag=false;
    			if (opcode==0xcb) { opcode=256+cpuRead8(rPC++); cycles+=4; }

    			// Read source operand.
    			srcOperand=0;
    			switch (opcodeToSrcAM[opcode]) {
    			case AM_IMP: break;
    
    			case AM_R8L: break; // Should not occur.
    			case AM_R8H: break; // Should not occur.
    			case AM_A: srcOperand=rA; break;
    			case AM_B: srcOperand=rB; break;
    			case AM_C: srcOperand=rC; break;
    			case AM_D: srcOperand=rD; break;
    			case AM_E: srcOperand=rE; break;
    			case AM_F: srcOperand=rF; break;
    			case AM_H: srcOperand=(rHL>>8)&0xff; break;
    			case AM_L: srcOperand=rHL&0xff; break;
    
    			case AM_R16: break; // Should not occur.
    			case AM_R16SP: break; // Should not occur.
    			case AM_BC: srcOperand=(rB<<8)|rC; break;
    			case AM_DE: srcOperand=(rD<<8)|rE; break;
    			case AM_HL: srcOperand=rHL; break;
    			case AM_AF: srcOperand=(rA<<8)|rF; break;
    			case AM_SP: srcOperand=rSP; break;
    
    			case AM_HLI: srcOperand=cpuRead8(rHL); cycles+=1*4; break;
    			case AM_HLII: srcOperand=cpuRead8(rHL); rHL=(rHL+1)&0xffff; cycles+=1*4; break;
    			case AM_HLDI: srcOperand=cpuRead8(rHL); rHL=(rHL-1)&0xffff; cycles+=1*4; break;
    			case AM_BCI: srcOperand=cpuRead8((rB<<8)|rC); cycles+=1*4; break;
    			case AM_DEI: srcOperand=cpuRead8((rD<<8)|rE); cycles+=1*4; break;
    			case AM_FFCI: srcOperand=cpuRead8(0xff00|rC); cycles+=1*4; break;
    			case AM_FFNNI:
    			    srcOperand=0xff00|cpuRead8(rPC++);
    			    srcOperand=cpuRead8(srcOperand); cycles+=2*4;
    			    break;
    			case AM_NNNNI:
    			    srcOperand=cpuRead8(rPC) | (cpuRead8(rPC+1)<<8); rPC+=2;
    			    srcOperand=cpuRead8(srcOperand); cycles+=3*4;
    			    break;
    
    			case AM_NN: srcOperand=cpuRead8(rPC++); cycles+=1*4; break;
    			case AM_SNN: // No break !
    			case AM_RNN: srcOperand=(byte)cpuRead8(rPC++); cycles+=1*4; break;
    			case AM_NNNN: srcOperand=cpuRead8(rPC) | (cpuRead8(rPC+1)<<8); rPC+=2; cycles+=2*4; break;
    
    			case AM_CC:
    			    switch ((opcode>>3)&0x03) {
    				case 0: srcOperand=((rF&F_ZERO)==0) ? 1 : 0; break;
    				case 1: srcOperand=((rF&F_ZERO)!=0) ? 1 : 0; break;
    				case 2: srcOperand=((rF&F_CARRY)==0) ? 1 : 0; break;
    				case 3: srcOperand=((rF&F_CARRY)!=0) ? 1 : 0; break;
    			    }
    			    break;
    			case AM_BIT: break; // Should not occur.
    			case AM_SPNN: srcOperand=((byte)cpuRead8(rPC++))&0xffff; cycles+=4; break;
    			case AM_RSTN: srcOperand=opcode&0x38; break;
    			}
    
    			// Execute instruction.
    			dstOperand=0;
       			switch (opcodeToInstruction[opcode]) {
    			case I_NOP:
    			    break;
    			case I_LD:
    			    dstOperand=srcOperand;
    			    break;
    			case I_LD16:
    			    dstOperand=srcOperand;
    			    break;
                case I_LD_SP_HL:
                    dstOperand=srcOperand; cycles+=1*4;
                    break;
    			case I_LD_HL_SPNN:
    			    // FIXME: carry and halfcarry may be wrong.
    			    rF=0;
    			    if (((rSP&0x0fff)+(srcOperand&0x0fff))>0x0fff) rF|=F_HALFCARRY;
    			    dstOperand=rSP+srcOperand;
    				if ((dstOperand<0) || (dstOperand>0xffff)) rF=F_CARRY;
    				dstOperand&=0xffff;
    			    break;
                case I_LD_NNNNI_SP:
                    dstOperand=cpuRead8(rPC) | (cpuRead8(rPC+1)<<8); rPC+=2;
                    cpuWrite8(dstOperand, srcOperand); cpuWrite8(dstOperand+1, srcOperand>>8);
                    cycles+=4*4;
                    break;
    			case I_PUSH:
    			    cycles+=3*4;
    				cpuWrite8(rSP-1, srcOperand>>8); cpuWrite8(rSP-2, srcOperand); rSP=(rSP-2)&0xffff;
    			    break;
    			case I_POP:
    			    cycles+=2*4;
    				dstOperand=cpuRead8(rSP) | (cpuRead8(rSP+1)<<8); rSP=(rSP+2)&0xffff;
    			    break;
    			case I_ADDC:
    				if ((rF&F_CARRY)!=0) srcOperand++;
    			    // No break !
    			case I_ADD:
    				rF=0;
    				if (((rA&0x0f)+(srcOperand&0x0f))>0x0f) rF|=F_HALFCARRY;
    				dstOperand=rA+srcOperand;
    				if (dstOperand>0xff) rF|=F_CARRY;
    				dstOperand&=0x00ff;
    				if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_SUBC:
    				if ((rF&F_CARRY)!=0) srcOperand++;
    			    // No break !
    			case I_SUB:
    				rF=F_SUBTRACT;
    				if ((rA&0x0f)<(srcOperand&0x0f)) rF|=F_HALFCARRY;
    				dstOperand=rA-srcOperand;
    				if (dstOperand<0) rF|=F_CARRY;
    				dstOperand&=0x00ff;
    				if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_AND:
    			    dstOperand=rA&srcOperand;
                    rF=F_HALFCARRY;
                    if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_XOR:
    			    dstOperand=rA^srcOperand;
    				rF=(dstOperand==0) ? F_ZERO : 0;
    			    break;
    			case I_OR:
    			    dstOperand=rA|srcOperand;
    				rF=(dstOperand==0) ? F_ZERO : 0;
    			    break;
    			case I_CP:
    				rF=F_SUBTRACT;
    				if ((rA&0x0f)<(srcOperand&0x0f)) rF|=F_HALFCARRY;
    				if (rA<srcOperand) rF|=F_CARRY;
    				if (rA==srcOperand) rF|=F_ZERO;
    			    break;
    			case I_INC:
    				rF&=F_CARRY;
    				if ((srcOperand&0x0f)==0x0f) rF|=F_HALFCARRY;
    				dstOperand=(srcOperand+1)&0xff;
    				if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_DEC:
    				rF=(rF&F_CARRY) | F_SUBTRACT;
    				if ((srcOperand&0x0f)==0x00) rF|=F_HALFCARRY;
    				dstOperand=(srcOperand-1)&0xff;
    				if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_DAA:
    			    daa();
    			    break;
    			case I_CPL:
    				dstOperand=((~srcOperand) & 0xff);
    				rF=((rF&(F_CARRY|F_ZERO)) | F_SUBTRACT | F_HALFCARRY);
    			    break;
    			case I_SCF:
    				rF=(rF&F_ZERO) | F_CARRY;
    			    break;
    			case I_CCF:
    				rF=(rF&(F_ZERO | F_CARRY)) ^ F_CARRY;
    			    break;
    			case I_ADD_HL_R16:
    				rF&=F_ZERO;
    				if (((rHL&0x0fff)+(srcOperand&0x0fff))>0x0fff) rF|=F_HALFCARRY;
    				dstOperand=rHL+srcOperand;
    				if (dstOperand>0xffff) rF|=F_CARRY;
    				dstOperand&=0xffff;
                    cycles+=4;
    			    break;
    			case I_ADD_SP_SNN:
    			    // FIXME: carry and halfcarry may be wrong.
    				rF=0;
    				if (((rSP&0x0fff)+(srcOperand&0x0fff))>0x0fff) rF|=F_HALFCARRY;
    				dstOperand=rSP+srcOperand; 
    				if ((dstOperand<0) || (dstOperand>0xffff)) rF|=F_CARRY;
    				dstOperand&=0xffff;
                    cycles+=4;
    			    break;
    			case I_INC16:
    			    dstOperand=(srcOperand+1)&0xffff; cycles+=1*4;
    			    break;
    			case I_DEC16:
    			    dstOperand=(srcOperand-1)&0xffff; cycles+=1*4;
    			    break;
    			case I_RLCA:
                    rF=0;
                    dstOperand=srcOperand<<1;
                    if ((srcOperand&0x80)!=0) { rF|=F_CARRY; dstOperand|=1; }
                    dstOperand&=0xFF;
                    break;
    			case I_RLC:
    				rF=0;
    				dstOperand=srcOperand<<1;
    				if ((srcOperand&0x80)!=0) { rF|=F_CARRY; dstOperand|=1; }
    				dstOperand&=0xFF;
    				if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_RRCA:
                    rF=0;
                    dstOperand=srcOperand>>1;
                    if ((srcOperand&0x01)!=0) { rF|=F_CARRY; dstOperand|=0x80; }
                    break;
    			case I_RRC:
    				rF=0;
    				dstOperand=srcOperand>>1;
    				if ((srcOperand&0x01)!=0) { rF|=F_CARRY; dstOperand|=0x80; }
    				if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_RLA:
                    dstOperand=srcOperand<<1;
                    if ((rF&F_CARRY)!=0) dstOperand|=1;
                    rF=((srcOperand&0x80)!=0) ? F_CARRY : 0;
                    dstOperand&=0xff;
                    break;
    			case I_RL:
    				dstOperand=srcOperand<<1;
    			    if ((rF&F_CARRY)!=0) dstOperand|=1;
    				rF=((srcOperand&0x80)!=0) ? F_CARRY : 0;
    				dstOperand&=0xff;
    				if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_RRA:
                    dstOperand=srcOperand>>1;
                    if ((rF&F_CARRY)!=0) dstOperand|=0x80;
                    rF=((srcOperand&0x01)!=0) ? F_CARRY : 0;
                    break;
    			case I_RR:
    				dstOperand=srcOperand>>1;
    			    if ((rF&F_CARRY)!=0) dstOperand|=0x80;
    				rF=((srcOperand&0x01)!=0) ? F_CARRY : 0;
    				if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_SLA:
    				rF=((srcOperand&0x80)!=0) ? F_CARRY : 0;
    				dstOperand=(srcOperand<<1)&0xff;
                    if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_SRA:
    				rF=((srcOperand&0x01)!=0) ? F_CARRY : 0;
    				dstOperand=((byte)srcOperand>>1)&0xff;
                    if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_SWAP:
    			    dstOperand=(((srcOperand&0x0f)<<4) | ((srcOperand&0xf0)>>4));
    				rF=(dstOperand==0) ? F_ZERO : 0;
    			    break;
    			case I_SRL:
    				rF=((srcOperand&0x01)!=0) ? F_CARRY : 0;
    				dstOperand=(srcOperand>>1)&0xff;
                    if (dstOperand==0) rF|=F_ZERO;
    			    break;
    			case I_BIT:
    				rF=(rF&F_CARRY) | F_HALFCARRY;
    				if ((srcOperand & (1<<((opcode>>3)&0x07)))==0) rF|=F_ZERO;
    			    break;
    			case I_RES:
    				dstOperand=srcOperand & ~(1<<((opcode>>3)&0x07));
    			    break;
    			case I_SET:
    				dstOperand=srcOperand | (1<<((opcode>>3)&0x07));
    			    break;
    			case I_JP:
    			    cycles+=4;
    			    rPC=srcOperand;
    			    break;
    			case I_JP_CC:
    			    if (srcOperand!=0) {
    				    cycles+=3*4;
    				    srcOperand=cpuRead8(rPC) | (cpuRead8(rPC+1)<<8);
    				    rPC=srcOperand;
    			    } else {
    				    cycles+=2*4;
    				    rPC+=2;
    			    }
    			    break;
    			case I_JR:
    			    cycles+=1*4;
    			    rPC+=srcOperand;
    			    break;
    			case I_JR_CC:
    			    if (srcOperand!=0) {
    				    cycles+=2*4;
    				    rPC+=1+(byte)cpuRead8(rPC);
    			    } else {
    				    cycles+=1*4;
    				    rPC+=1;
    			    }
    			    break;
    			case I_CALL:
    		        cycles+=3*4;
    		        cpuWrite8(rSP-1, rPC>>8); cpuWrite8(rSP-2, rPC); rSP=(rSP-2)&0xffff;
    				rPC=srcOperand;
                    if (debugModeActivated) subroutineRecursionDepth++;
    			    break;
    			case I_CALL_CC:
    			    if (srcOperand!=0) {
    			        cycles+=3*4;
    			        srcOperand=cpuRead8(rPC) | (cpuRead8(rPC+1)<<8); rPC+=2;
    				    cpuWrite8(rSP-1, rPC>>8); cpuWrite8(rSP-2, rPC); rSP=(rSP-2)&0xffff;
    					rPC=srcOperand;
                        if (debugModeActivated) subroutineRecursionDepth++;
    			    } else {
    			        cycles+=2*4;
    					rPC+=2;			        
    			    }
    			    break;
    			case I_RET:
    			    cycles+=3*4;
    				rPC = cpuRead8(rSP) | (cpuRead8(rSP+1)<<8); rSP=(rSP+2)&0xffff;
                    if (debugModeActivated) debugRET();
    			    break;
    			case I_RET_CC:
    			    if (srcOperand!=0) {
    			        cycles+=4*4;
    			        rPC=cpuRead8(rSP) | (cpuRead8(rSP+1)<<8); rSP=(rSP+2)&0xffff;
                        if (debugModeActivated) debugRET();
    			    } else {
    			        cycles+=1*4;
    			    }                
    			    break;
    			case I_RETI:
    			    cycles+=3*4;
    				rPC=cpuRead8(rSP) | (cpuRead8(rSP+1)<<8); rSP=(rSP+2)&0xffff;
                    interruptsDelayed=true; // TODO: docs don't say that there is a delay but it seems to exist anyway.                    
                    if (debugModeActivated) debugRTI();
    			    break;
    			case I_RST:
    			    cycles+=3*4;
    			    cpuWrite8(rSP-1, rPC>>8); cpuWrite8(rSP-2, rPC); rSP=(rSP-2)&0xffff;
    				rPC=srcOperand;
    			    break;
    			case I_HALT:
                    srcOperand=(interruptsEnabled || interruptsDelayed) ? 1 : 0;
                    srcOperand|=interruptRequested ? 2 : 0;
                    switch (srcOperand) {
                    case 0: // Interrupt not requested, interrupts disabled.
                    case 1: // Interrupt not requested, interrupts enabled.
                        executionHalted=true;
//                        updateState();
                        if (cycles<requestedCycles) cycles=requestedCycles;
                        break;
                    case 2: // Interrupt requested, interrupts disabled.
                        haltFlag=true; // Halt bug.
                        break;
                    case 3: // Interrupt requested, interrupts enabled.
                        break;
                    }
    			    break;
    			case I_STOP:
    			    rPC++; // Skip second byte.
                    if (stop()) {
                        if (!interruptRequested || (!interruptsEnabled && !interruptsDelayed)) {
                            executionHalted=true;
//                            updateState();
                            if (cycles<requestedCycles) cycles=requestedCycles;
                        }
                    }
    			    break;
    			case I_DI:
    				interruptsEnabled=false; interruptsDelayed=false; // DI cancel pending EI.
//                  updateState();
    				break;
    			case I_EI:
    				interruptsDelayed=true; // We need to delay interrupts after an EI instruction.
    			    break;
    			case I_ILLEGAL:
                    event.type=CPUEvent.EVENT_ILLEGAL; event.address=rPC&0xffff; event.breakpoint=null;
                    listener.onIllegal(event);
    			    break;
    			}
    
    			// Write destination operand.
    			switch (opcodeToDstAM[opcode]) {
    			case AM_IMP: break;
    
    			case AM_R8L: break; // Should not occur.
    			case AM_R8H: break; // Should not occur.
    			case AM_A: rA=dstOperand; break;
    			case AM_B: rB=dstOperand; break;
    			case AM_C: rC=dstOperand; break;
    			case AM_D: rD=dstOperand; break;
    			case AM_E: rE=dstOperand; break;
    			case AM_F: rF=dstOperand; break;
    			case AM_H: rHL=(dstOperand<<8) | (rHL&0x00ff); break;
    			case AM_L: rHL=(rHL&0xff00) | dstOperand; break;
    
    			case AM_R16: break; // Should not occur.
    			case AM_R16SP: break; // Should not occur.
    			case AM_BC: rB=(dstOperand>>8)&0xff; rC=dstOperand&0xff; break;
    			case AM_DE: rD=(dstOperand>>8)&0xff; rE=dstOperand&0xff; break;
    			case AM_HL: rHL=dstOperand; break;
    			case AM_AF: rA=(dstOperand>>8)&0xff; rF=dstOperand&0xff;; break;
    			case AM_SP: rSP=dstOperand; break;
    
    			case AM_HLI: cpuWrite8(rHL, dstOperand); cycles+=1*4; break;
    			case AM_HLII: cpuWrite8(rHL, dstOperand); rHL=(rHL+1)&0xffff; cycles+=1*4; break;
    			case AM_HLDI: cpuWrite8(rHL, dstOperand); rHL=(rHL-1)&0xffff; cycles+=1*4; break;
    			case AM_BCI: cpuWrite8((rB<<8)|rC, dstOperand); cycles+=1*4; break;
    			case AM_DEI: cpuWrite8((rD<<8)|rE, dstOperand); cycles+=1*4; break;
    			case AM_FFCI: cpuWrite8(0xff00|rC, dstOperand); cycles+=1*4; break;
    			case AM_FFNNI:
    			    srcOperand=0xff00|cpuRead8(rPC++);
    			    cpuWrite8(srcOperand, dstOperand); cycles+=2*4;
    			    break;
    			case AM_NNNNI:
    			    srcOperand=cpuRead8(rPC) | (cpuRead8(rPC+1)<<8); rPC+=2;
    			    cpuWrite8(srcOperand, dstOperand); cycles+=3*4;
    			    break;
    
    			case AM_NN: break; // Should not occur.
    			case AM_SNN: break; // Should not occur.
    			case AM_RNN: break; // Should not occur.
    			case AM_NNNN: break; // Should not occur.
    
    			case AM_CC: break; // Should not occur.
    			case AM_BIT: break; // Should not occur.
    			case AM_SPNN: break; // Should not occur.
    			case AM_RSTN: break; // Should not occur.
    			}
            }
            
            // Debugging.
            if (debugModeActivated) debugInstruction();
        } while (cycles<requestedCycles && !breakExecution);

        running=false;
		requestedCycles=cycles;
		cycles=0;
		return requestedCycles;
	}
	
	private final void daa() {
 		// The Z80 documentation do not specify the result for all values of A, F.C and F.HC.
 		// This code performs the same for the cases given in the documentation but it can be
 		// different for the other cases.
	    // With this routine, the result is always a BCD number, even with an invalid input.
	    // The half-carry is the bit 4 of the low digit.
	    // The carry is the bit 4 of the high digit.
 		int hi=(rA>>4), lo=rA&0x0f;
 		int t=rF & (F_SUBTRACT | F_CARRY); // Carry must not be cleared (and halfcarry too maybe) !
 		if ((rF&F_SUBTRACT)!=0) {
	 		if (lo>0x09 || ((rF&F_HALFCARRY)!=0 && lo>=0x06)) lo-=0x06;
	 		if (hi>0x09 || ((rF&F_CARRY)!=0 && hi>=0x06)) hi-=0x06;
	 	} else {
	 		if (lo>0x09 || ((rF&F_HALFCARRY)!=0 && lo<=0x03)) lo+=0x06;
	 		if (lo>0x09) hi++;
	 		if (hi>0x09 || ((rF&F_CARRY)!=0 && hi<=0x03)) hi+=0x06;
	 		if (hi>0x09) t|=F_CARRY;
	 	}
 		rA=((hi&0x0f)<<4) | (lo&0x0f);
 		if (rA==0) t|=F_ZERO;
 		rF=t;
	}

	private final void debugInterrupt() {
        interruptRecursionDepth++;
        if (traceMode==TRACE_MODE_STEP_INTO_I) {
            event.type=CPUEvent.EVENT_STEP_INTO_I; event.address=rPC&0xffff; event.breakpoint=null;
            listener.onTrace(event);
        }		
	}

	private final void debugRET() {
        if (traceMode==TRACE_MODE_STEP_OUT && subroutineRecursionDepth==0) {
            event.type=CPUEvent.EVENT_STEP_OUT; event.address=rPC&0xffff; event.breakpoint=null;
            listener.onTrace(event);
        }
        subroutineRecursionDepth--;
        if (subroutineRecursionDepth<0) subroutineRecursionDepth=0;
	}

	private final void debugRTI() {
        switch (traceMode) {
        case TRACE_MODE_STEP_OUT:
            event.type=CPUEvent.EVENT_STEP_OUT; event.address=rPC&0xffff; event.breakpoint=null;
            listener.onTrace(event);
            subroutineRecursionDepth=0;
            break;
        case TRACE_MODE_STEP_OUT_I:
            if (interruptRecursionDepth==0) {
                event.type=CPUEvent.EVENT_STEP_OUT_I; event.address=rPC&0xffff; event.breakpoint=null;
                listener.onTrace(event);
            }
            break;
        }
        interruptRecursionDepth--;
        if (interruptRecursionDepth<0) interruptRecursionDepth=0;		
	}
	
	private final void debugInstruction() {
        Breakpoint breakpoint=breakpoints.findBreakpoint(rPC&0xffff);
        if (breakpoint!=null && listener!=null) {
            event.type=CPUEvent.EVENT_BREAKPOINT; event.address=rPC&0xffff; event.breakpoint=breakpoint;
            listener.onTrace(event);
        }
        
        if (traceInterruptsFlag || interruptRecursionDepth<=0) {
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
}

