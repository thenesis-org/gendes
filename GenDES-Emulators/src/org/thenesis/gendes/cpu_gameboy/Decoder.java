package org.thenesis.gendes.cpu_gameboy;

//import static org.thenesis.emulation.cpu_gameboy.InstructionSet.*;

final class Decoder implements InstructionSet { 
    
    byte opcodeToSrcAM[]=new byte[2*256];
    byte opcodeToDstAM[]=new byte[2*256];
    byte opcodeToInstruction[]=new byte[2*256];
    byte opcodeToLength[]=new byte[2*256];
    byte opcodeToDebugSrcAM[]=new byte[2*256];
    byte opcodeToDebugDstAM[]=new byte[2*256];
    
    // Build the decoder tables.
    void build() {
        
        // Initialize all tables.
        for (int i=0; i<2*256; i++) {
            opcodeToSrcAM[i]=AM_IMP;
            opcodeToDstAM[i]=AM_IMP;
            opcodeToInstruction[i]=I_ILLEGAL;
            opcodeToLength[i]=1;
            opcodeToDebugSrcAM[i]=AM_IMP;
            opcodeToDebugDstAM[i]=AM_IMP;
        }
        // CB instructions always have length 2. So we could use an array of 256 instead of 2*256.
        opcodeToLength[0xcb]=2; // 
        
        // Build.
        int n=INSTRUCTIONS_TABLE.length;
        for (int i=0; i<n; i++) {
            
            int it[]=INSTRUCTIONS_TABLE[i];
            int opcode=it[IT_OPCODE]&0xffff, mask=it[IT_MASK]&0xffff, t;
            if ((opcode>>8)==0xcb) {
                opcode&=0xff; mask&=0xff; t=256;
            } else {
                opcode>>=8; mask>>=8; t=0;
            }
            
            int j=0;
            while (true) {
                int k=opcode | j;
                opcodeToSrcAM[t+k]=developAM(k, it[IT_SRC_AM]);
                opcodeToDstAM[t+k]=developAM(k, it[IT_DST_AM]);
                opcodeToDebugSrcAM[t+k]=developAM(k, it[IT_DEBUG_SRC_AM]);
                opcodeToDebugDstAM[t+k]=developAM(k, it[IT_DEBUG_DST_AM]);
                opcodeToInstruction[t+k]=(byte)it[IT_ID];
                opcodeToLength[t+k]=(byte)it[IT_LENGTH];
                if (j>=mask) break;
                j=((j|~mask)+1)&mask; // Carry propagation: cf. Graphics Gems II, VIII.3 "Of integers, fields and bit counting".
            }
        }
        
    }
    
    // Given an opcode and a factored addressing mode, it returns the developped addressing mode.
    private static byte developAM(int opcode, int am) {
        switch (am) {
        case AM_R8L: return DEVELOPPED_AM_R8[opcode&0x07];
        case AM_R8H: return DEVELOPPED_AM_R8[(opcode>>3)&0x07];
        case AM_R16: return DEVELOPPED_AM_R16[(opcode>>4)&0x03];
        case AM_R16SP: return DEVELOPPED_AM_R16SP[(opcode>>4)&0x03];
        }
        return (byte)am;
    }
    
    // Table for developping 8 bit registers.
    private static final byte DEVELOPPED_AM_R8[]={
    	AM_B, AM_C, AM_D, AM_E, AM_H, AM_L, AM_HLI, AM_A
    };
    
    // Table for developping 16 bit registers.
    private static final byte DEVELOPPED_AM_R16[]={
        AM_BC, AM_DE, AM_HL, AM_SP
    };
    
    // Table for developping 16 bit registers with SP.
    private static final byte DEVELOPPED_AM_R16SP[]={
        AM_BC, AM_DE, AM_HL, AM_AF
    };

}
