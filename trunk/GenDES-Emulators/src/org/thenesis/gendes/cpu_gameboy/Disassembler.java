package org.thenesis.gendes.cpu_gameboy;

import org.thenesis.gendes.FastString;


// TODO: support invalid addresses and min/max address.
public abstract class Disassembler implements InstructionSet {
    // Decoder.
    private final Decoder decoder=new Decoder();

    // Address of the instruction.
    public int instructionAddress;
    // Length of the instruction.
    public int instructionLength;
    // Opcode bytes.
    public final int opcodeBytes[]=new int[4];
    // Instruction String.
    public final FastString opcodeString=new FastString();
    // Destination addressing mode string.
    public final FastString dstAMString=new FastString();
    // Source addressing mode string.
    public final FastString srcAMString=new FastString();
    
    /** Constructor. */
    public Disassembler() {
    	decoder.build();
        opcodeString.setCapacity(8);
        srcAMString.setCapacity(64);
        dstAMString.setCapacity(64);
    }

    /** Used to fetch opcode bytes. */
    public abstract int read8(int address);
    
    /** Disassemble. */
    public final int disassemble(int address) {
        if (decoder==null) return 0;
        int opcode;

        // Read opcode.
        opcode=read8(address); opcodeBytes[0]=opcode;
        // Get instruction length.
        instructionLength=decoder.opcodeToLength[opcode];
        // Read all bytes of the instruction.
        for (int i=1; i<instructionLength; i++) opcodeBytes[i]=read8(address+i);
        
        // Build the string.
        disassembleInstruction(address);

        return instructionLength;
    }

    /** Disassemble previous instruction. */
    public final int disassemblePrevious(int address) {
        if (decoder==null) return 0;
        int opcode;

        // Find the longest match.
        instructionLength=1;
        for (int i=1; i<3; i++) {
            opcode=read8(address-i);
            opcodeBytes[3-i]=opcode;
            if (decoder.opcodeToLength[opcode]==i) instructionLength=i;
        }

        // Shift the opcode bytes array.
        int shift=3-instructionLength;
        if (shift>0) {
            for (int i=0; i<instructionLength; i++) opcodeBytes[i]=opcodeBytes[shift+i];
        }

        // Build the string.
        disassembleInstruction(address);

        return instructionLength;
    }
    
    /** Return the length of the instruction at address. */
    public final int findInstructionLength(int address) {
        if (decoder==null) return 0;
        int opcode=read8(address);
        return decoder.opcodeToLength[opcode];
    }
    
    /** Return the length of the instruction before address. */
    public final int findPreviousInstructionLength(int address) {
       if (decoder==null) return 0;
        int opcode, length;
        
        // Find the longest match.
        length=1;
        for (int i=1; i<3; i++) {
            opcode=read8(address-i);
            if (decoder.opcodeToLength[opcode]==i) length=i;
        }
        
        return length;
    }

    /** Print the opcode bytes. */
    public final void printOpcodeBytes(FastString outputString) {
        switch (instructionLength) {
        case 1:
        	outputString.append(opcodeBytes[0], 8);
            outputString.append("    ");
            break;
        case 2:
        	outputString.append((opcodeBytes[0]<<8)|opcodeBytes[1], 16);
            outputString.append("  ");
            break;
        case 3:
        	outputString.append((opcodeBytes[0]<<16)|(opcodeBytes[1]<<8)|opcodeBytes[2], 24);
            break;
        }    	
    }
    
    /** Print the instruction. */
    public final void printInstruction(FastString outputString) {
        if (decoder==null) return;
        outputString.append(opcodeString);
        outputString.append(" ");
        if (dstAMString.length==0) {
            outputString.append(srcAMString);  
        } else if (srcAMString.length==0) {
            outputString.append(dstAMString);
        } else {
            outputString.append(dstAMString);
            outputString.append(",");
            outputString.append(srcAMString);
        }
    }

    // Build all separate fields of an instruction.
    private void disassembleInstruction(int address) {
        int opcode, instruction, srcAM, dstAM;

        // Save the instruction address.
        instructionAddress=address;

        // Find instruction and addressing modes in the corresponding tables.
        opcode=opcodeBytes[0];
        if (opcode==0xcb) opcode=256+opcodeBytes[1];
        instruction=decoder.opcodeToInstruction[opcode];
        srcAM=decoder.opcodeToDebugSrcAM[opcode];
        dstAM=decoder.opcodeToDebugDstAM[opcode];

        // Build the instruction string.
        opcodeString.clear();
        opcodeString.append(INSTRUCTION_TO_STRING[instruction]);
        amToString(address, dstAM, dstAMString);
        amToString(address, srcAM, srcAMString);
    }

    // Return the string corresponding to an addressing mode.
    private void amToString(int address, int am, FastString s) {
        int opcode=(opcodeBytes[0]==0xcb) ? opcodeBytes[1] : opcodeBytes[0];
        int t;
        
        s.clear();

        switch (am) {
        case AM_IMP: break;

        case AM_R8L: s.append(AM_R8_TO_STRING[opcode&0x07]); break;
        case AM_R8H: s.append(AM_R8_TO_STRING[(opcode>>3)&0x07]); break;
        case AM_A: s.append("A"); break;
        case AM_B: s.append("B"); break;
        case AM_C: s.append("C"); break;
        case AM_D: s.append("D"); break;
        case AM_E: s.append("E"); break;
        case AM_F: s.append("F"); break;
        case AM_H: s.append("H"); break;
        case AM_L: s.append("L"); break;

        case AM_R16: s.append(AM_R16_TO_STRING[(opcode>>4)&0x03]); break;
        case AM_R16SP: s.append(AM_R16SP_TO_STRING[(opcode>>4)&0x03]); break;
        case AM_BC: s.append("BC"); break;
        case AM_DE: s.append("DE"); break;
        case AM_HL: s.append("HL"); break;
        case AM_SP: s.append("SP"); break;
        case AM_AF: s.append("AF"); break;

        case AM_HLI: s.append("(HL)"); break;
        case AM_HLII: s.append("(HL+)"); break;
        case AM_HLDI: s.append("(HL-)"); break;
        case AM_BCI: s.append("(BC)"); break;
        case AM_DEI: s.append("(DE)"); break;
        case AM_FFCI: s.append("($FF00+C)"); break;
        case AM_FFNNI: s.append("($FF00+"); s.append(opcodeBytes[1], 8); s.append(")"); break;
        case AM_NNNNI: s.append("($"); s.append((opcodeBytes[2]<<8) | opcodeBytes[1], 16); s.append(")"); break;
        
        case AM_NN: s.append("$"); s.append(opcodeBytes[1], 8); break;
        case AM_SNN:
            t=(byte)opcodeBytes[1];
            s.append("$"); s.append(t, 8);
            s.append(" [");
            if (t<0) { s.append("-"); s.append(-t, 8); }
            else { s.append("+"); s.append(+t, 8); }
            s.append("]");
            break;
        case AM_RNN:
            t=(byte)opcodeBytes[1]+2;
            s.append("$"); s.append(address+t, 16);
            s.append(" [");
            if (t<0) { s.append("-"); s.append(-t, 8); }
            else { s.append("+"); s.append(+t, 8); }
            s.append("]");
            break;
        case AM_NNNN: s.append("$"); s.append((opcodeBytes[2]<<8) | opcodeBytes[1], 16); break;

        case AM_CC: s.append(AM_CC_TO_STRING[(opcode>>3)&0x03]); break;
        case AM_BIT: s.append("#"); s.append((opcode>>3)&0x7, 4); break;
        case AM_SPNN:
            t=(byte)opcodeBytes[1];
            s.append("SP+$"); s.append(t, 8);
            s.append(" [");
            if (t<0) { s.append("-"); s.append(-t, 8); }
            else { s.append("+"); s.append(+t, 8); }
            s.append("]");
            break;
        case AM_RSTN: s.append("$"); s.append(opcode&0x38, 8); break;
        default: s.append("Internal error"); break;
        }
    }    
}
