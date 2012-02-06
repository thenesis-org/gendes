package org.thenesis.gendes.cpu_gameboy;


interface InstructionSet {
    // Addressing modes.
    static final byte
    	AM_IMP=0,
    	AM_R8L=1, AM_R8H=2, AM_A=3, AM_B=4, AM_C=5, AM_D=6, AM_E=7, AM_F=8, AM_H=9, AM_L=10,
    	AM_R16=11, AM_R16SP=12, AM_BC=13, AM_DE=14, AM_HL=15, AM_SP=16, AM_AF=17,
    	AM_HLI=18, AM_HLII=19, AM_HLDI=20, AM_BCI=21, AM_DEI=22, AM_FFCI=23, AM_FFNNI=24, AM_NNNNI=25,
    	AM_NN=26, AM_SNN=27, AM_RNN=28, AM_NNNN=29,
    	AM_CC=30, AM_BIT=33, AM_SPNN=34, AM_RSTN=35;
    
    // Instructions.
    static final byte
    	I_NOP=0,
    	I_LD=1,
    	I_LD16=2, I_LD_SP_HL=3, I_LD_HL_SPNN=4, I_LD_NNNNI_SP=5,
    	I_PUSH=6, I_POP=7,
    	I_ADDC=8, I_ADD=9, I_SUBC=10, I_SUB=11, I_AND=12, I_XOR=13, I_OR=14, I_CP=15,
    	I_INC=16, I_DEC=17,
    	I_DAA=18, I_CPL=19, I_SCF=20, I_CCF=21,
    	I_ADD_HL_R16=22, I_ADD_SP_SNN=23, I_INC16=24, I_DEC16=25,
    	I_RLCA=26, I_RLC=27, I_RRCA=28, I_RRC=29, I_RLA=30, I_RL=31, I_RRA=32, I_RR=33,
    	I_SLA=34, I_SRA=35, I_SWAP=36, I_SRL=37,
    	I_BIT=38, I_RES=39, I_SET=40,
    	I_JP=41, I_JP_CC=42, I_JR=43, I_JR_CC=44,
    	I_CALL=45, I_CALL_CC=46, I_RET=47, I_RET_CC=48,
    	I_RETI=49, I_RST=50, I_HALT=51, I_STOP=52, I_DI=53, I_EI=54,
    	I_ILLEGAL=55;
    
    //  String corresponding to a given instruction.
    static final String INSTRUCTION_TO_STRING[]={
        "nop ",
        "ld  ",
        "ld  ", "ld  ", "ld  ", "ld  ",
        "push", "pop ",
        "addc", "add ", "subc", "sub ", "and ", "xor ", "or  ", "cp  ",
        "inc ", "dec ",
        "daa ", "cpl ", "scf ", "ccf ",
        "add ", "add ", "inc ", "dec ",
        "rlca", "rlc ", "rrca", "rrc ", "rla ", "rl  ", "rra ", "rr  ",
        "sla ", "sra ", "swap", "srl ",
        "bit ", "res ", "set ",
        "jp  ", "jp  ", "jr  ", "jr  ",
        "call", "call", "ret ", "ret ",
        "reti", "rst ", "halt", "stop", "di  ", "ei  ",
        "??? "
    };
    
    // String corresponding to AM_R8L or AM_R8H addressing modes.
    static final String AM_R8_TO_STRING[]={
        "B", "C", "D", "E", "H", "L", "(HL)", "A"
    };
    
    // String corresponding to AM_R16 addressing mode.
    static final String AM_R16_TO_STRING[]={
        "BC", "DE", "HL", "SP"
    };
    
    // String corresponding to AM_R16SP addressing mode.
    static final String AM_R16SP_TO_STRING[]={
        "BC", "DE", "HL", "AF"
    };
    
    // String corresponding to AM_CC, AM_CC_SNN or AM_CC_NNNN addressing modes.
    static final String AM_CC_TO_STRING[]={
        "NZ", "Z", "NC", "C"
    };

    // Instruction table fields.
    static final int
    	IT_ID=           0,
    	IT_OPCODE=       1,
    	IT_MASK=         2,
    	IT_DST_AM=       3,
    	IT_SRC_AM=       4,
    	IT_DEBUG_DST_AM= 5,
    	IT_DEBUG_SRC_AM= 6,
    	IT_LENGTH=       7;
    
    // Instructions table.
    static final int INSTRUCTIONS_TABLE[][]={
        {   I_NOP,          0x0000, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 },
        
        // 8-bit load.
        {   I_LD,           0x0600, 0x3800, AM_R8H,     AM_NN,      AM_R8H,     AM_NN,      2 },
        
        {   I_LD,           0x4000, 0x3f00, AM_R8H,     AM_R8L,     AM_R8H,     AM_R8L,     1 },
        
        {   I_LD,           0xea00, 0x0000, AM_NNNNI,   AM_A,       AM_NNNNI,   AM_A,       3 },
        {   I_LD,           0xfa00, 0x0000, AM_A,       AM_NNNNI,   AM_A,       AM_NNNNI,   3 },
        
        {   I_LD,           0x0a00, 0x0000, AM_A,       AM_BCI,     AM_A,       AM_BCI,     1 },
        {   I_LD,           0x1a00, 0x0000, AM_A,       AM_DEI,     AM_A,       AM_DEI,     1 },
        {   I_LD,           0x0200, 0x0000, AM_BCI,     AM_A,       AM_BCI,     AM_A,       1 },
        {   I_LD,           0x1200, 0x0000, AM_DEI,     AM_A,       AM_DEI,     AM_A,       1 },
        
        {   I_LD,           0xe200, 0x0000, AM_FFCI,    AM_A,       AM_FFCI,    AM_A,       1 },
        {   I_LD,           0xf200, 0x0000, AM_A,       AM_FFCI,    AM_A,       AM_FFCI,    1 },
        
        {   I_LD,           0x2a00, 0x0000, AM_A,       AM_HLII,    AM_A,       AM_HLII,    1 },
        {   I_LD,           0x3a00, 0x0000, AM_A,       AM_HLDI,    AM_A,       AM_HLDI,    1 },
        {   I_LD,           0x2200, 0x0000, AM_HLII,    AM_A,       AM_HLII,    AM_A,       1 },
        {   I_LD,           0x3200, 0x0000, AM_HLDI,    AM_A,       AM_HLDI,    AM_A,       1 },
        
        {   I_LD,           0xe000, 0x0000, AM_FFNNI,   AM_A,       AM_FFNNI,   AM_A,       2 },
        {   I_LD,           0xf000, 0x0000, AM_A,       AM_FFNNI,   AM_A,       AM_FFNNI,   2 },
        
        // 16-bit load.
        {   I_LD16,         0x0100, 0x3000, AM_R16,     AM_NNNN,    AM_R16,     AM_NNNN,    3 },
        
        {   I_LD_NNNNI_SP,  0x0800, 0x0000, AM_IMP,     AM_SP,      AM_NNNNI,   AM_SP,      3 },
        {   I_LD_HL_SPNN,   0xf800, 0x0000, AM_HL,      AM_SPNN,    AM_HL,      AM_SPNN,    2 },
        {   I_LD_SP_HL,     0xf900, 0x0000, AM_SP,      AM_HL,      AM_SP,      AM_HL,      1 },
        
        {   I_PUSH,         0xc500, 0x3000, AM_IMP,     AM_R16SP,   AM_R16SP,   AM_IMP,     1 },
        {   I_POP,          0xc100, 0x3000, AM_R16SP,   AM_IMP,     AM_R16SP,   AM_IMP,     1 },
        
        // 8-bit arithmetic and logic.
        {   I_ADD,          0x8000, 0x0700, AM_A,       AM_R8L,     AM_A,       AM_R8L,     1 },
        {   I_ADDC,         0x8800, 0x0700, AM_A,       AM_R8L,     AM_A,       AM_R8L,     1 },
        {   I_SUB,          0x9000, 0x0700, AM_A,       AM_R8L,     AM_A,       AM_R8L,     1 },
        {   I_SUBC,         0x9800, 0x0700, AM_A,       AM_R8L,     AM_A,       AM_R8L,     1 },
        {   I_AND,          0xa000, 0x0700, AM_A,       AM_R8L,     AM_A,       AM_R8L,     1 },
        {   I_XOR,          0xa800, 0x0700, AM_A,       AM_R8L,     AM_A,       AM_R8L,     1 },
        {   I_OR,           0xb000, 0x0700, AM_A,       AM_R8L,     AM_A,       AM_R8L,     1 },
        {   I_CP,           0xb800, 0x0700, AM_IMP,     AM_R8L,     AM_A,       AM_R8L,     1 },
        
        {   I_ADD,          0xc600, 0x0000, AM_A,       AM_NN,      AM_A,       AM_NN,      2 },
        {   I_ADDC,         0xce00, 0x0000, AM_A,       AM_NN,      AM_A,       AM_NN,      2 },
        {   I_SUB,          0xd600, 0x0000, AM_A,       AM_NN,      AM_A,       AM_NN,      2 },
        {   I_SUBC,         0xde00, 0x0000, AM_A,       AM_NN,      AM_A,       AM_NN,      2 },
        {   I_AND,          0xe600, 0x0000, AM_A,       AM_NN,      AM_A,       AM_NN,      2 },
        {   I_XOR,          0xee00, 0x0000, AM_A,       AM_NN,      AM_A,       AM_NN,      2 },
        {   I_OR,           0xf600, 0x0000, AM_A,       AM_NN,      AM_A,       AM_NN,      2 },
        {   I_CP,           0xfe00, 0x0000, AM_IMP,     AM_NN,      AM_A,       AM_NN,      2 },
        
        {   I_INC,          0x0400, 0x3800, AM_R8H,     AM_R8H,     AM_R8H,     AM_IMP,     1 },
        {   I_DEC,          0x0500, 0x3800, AM_R8H,     AM_R8H,     AM_R8H,     AM_IMP,     1 },
        
        {   I_DAA,          0x2700, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 },
        {   I_CPL,          0x2f00, 0x0000, AM_A,       AM_A,       AM_A,       AM_IMP,     1 },
        {   I_SCF,          0x3700, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 },
        {   I_CCF,          0x3f00, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 },
        
        // 16-bit arithmetic.
        {   I_ADD_HL_R16,   0x0900, 0x3000, AM_HL,      AM_R16,     AM_HL,      AM_R16,     1 },
        {   I_ADD_SP_SNN,   0xe800, 0x0000, AM_SP,      AM_SNN,     AM_SP,      AM_SNN,     2 },
        {   I_INC16,        0x0300, 0x3000, AM_R16,     AM_R16,     AM_R16,     AM_IMP,     1 },
        {   I_DEC16,        0x0b00, 0x3000, AM_R16,     AM_R16,     AM_R16,     AM_IMP,     1 },
        
        // Swap, shift, rotate.
        {   I_RLCA,         0x0700, 0x0000, AM_A,       AM_A,       AM_A,       AM_IMP,     1 },
        {   I_RRCA,         0x0f00, 0x0000, AM_A,       AM_A,       AM_A,       AM_IMP,     1 },
        {   I_RLA,          0x1700, 0x0000, AM_A,       AM_A,       AM_A,       AM_IMP,     1 },
        {   I_RRA,          0x1f00, 0x0000, AM_A,       AM_A,       AM_A,       AM_IMP,     1 },
        
        {   I_RLC,          0xcb00, 0x0007, AM_R8L,     AM_R8L,     AM_R8L,     AM_IMP,     2 },
        {   I_RRC,          0xcb08, 0x0007, AM_R8L,     AM_R8L,     AM_R8L,     AM_IMP,     2 },
        {   I_RL,           0xcb10, 0x0007, AM_R8L,     AM_R8L,     AM_R8L,     AM_IMP,     2 },
        {   I_RR,           0xcb18, 0x0007, AM_R8L,     AM_R8L,     AM_R8L,     AM_IMP,     2 },
        
        {   I_SLA,          0xcb20, 0x0007, AM_R8L,     AM_R8L,     AM_R8L,     AM_IMP,     2 },
        {   I_SRA,          0xcb28, 0x0007, AM_R8L,     AM_R8L,     AM_R8L,     AM_IMP,     2 },
        {   I_SWAP,         0xcb30, 0x0007, AM_R8L,     AM_R8L,     AM_R8L,     AM_IMP,     2 },
        {   I_SRL,          0xcb38, 0x0007, AM_R8L,     AM_R8L,     AM_R8L,     AM_IMP,     2 },
        
        // Bit manipulation.
        {   I_BIT,          0xcb40, 0x003f, AM_IMP,     AM_R8L,     AM_BIT,     AM_R8L,     2 },
        {   I_RES,          0xcb80, 0x003f, AM_R8L,     AM_R8L,     AM_BIT,     AM_R8L,     2 },
        {   I_SET,          0xcbc0, 0x003f, AM_R8L,     AM_R8L,     AM_BIT,     AM_R8L,     2 },
        
        // Instruction flow control.
        {   I_JP,           0xc300, 0x0000, AM_IMP,     AM_NNNN,    AM_NNNN,    AM_IMP,     3 },
        {   I_JP_CC,        0xc200, 0x1800, AM_IMP,     AM_CC,      AM_CC,      AM_NNNN,    3 },
        {   I_JP,           0xe900, 0x0000, AM_IMP,     AM_HL,      AM_HL,      AM_IMP,     1 },
        {   I_JR,           0x1800, 0x0000, AM_IMP,     AM_RNN,     AM_RNN,     AM_IMP,     2 },
        {   I_JR_CC,        0x2000, 0x1800, AM_IMP,     AM_CC,      AM_CC,      AM_RNN,     2 },
        
        {   I_CALL,         0xcd00, 0x0000, AM_IMP,     AM_NNNN,    AM_NNNN,    AM_IMP,     3 },
        {   I_CALL_CC,      0xc400, 0x1800, AM_IMP,     AM_CC,      AM_CC,      AM_NNNN,    3 },
        {   I_RET,          0xc900, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 },
        {   I_RET_CC,       0xc000, 0x1800, AM_IMP,     AM_CC,      AM_CC,      AM_IMP,     1 },
        
        // System.
        {   I_RETI,         0xd900, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 },
        {   I_RST,          0xc700, 0x3800, AM_IMP,     AM_RSTN,    AM_IMP,     AM_RSTN,    1 },
        {   I_HALT,         0x7600, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 }, // Must be after "LD r8,r8" (because it is LD (HL),(HL)).
        {   I_STOP,         0x1000, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     2 },
        {   I_DI,           0xf300, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 },
        {   I_EI,           0xfb00, 0x0000, AM_IMP,     AM_IMP,     AM_IMP,     AM_IMP,     1 },
    };

}
