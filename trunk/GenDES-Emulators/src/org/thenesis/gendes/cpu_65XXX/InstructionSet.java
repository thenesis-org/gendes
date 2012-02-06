package org.thenesis.gendes.cpu_65XXX;


interface InstructionSet {
	// Addressing modes.
	static final int
		AM_ILL=0, // Illegal.
		AM_IMP=1, // Implied.
		AM_ACC=2, // Accumulator: A.
		AM_X=3, // X.
		AM_Y=4, // Y.
		AM_P=0, // P.
		AM_S=0, // S.
		AM_ZERO=0, // Zero.
		AM_IMM=0, // Immediate: #0x??.
		AM_REL=0, // Relative: pc+0x??.
		AM_Z=0, // Zeropage: 0x00??.
		AM_ZX=0, // Zeropage X: 0x00??+X.
		AM_ZY=0, // Zeropage Y: 0x00??+Y.
		AM_ZI=0, // Zeropage Indirect: (0x00??).
		AM_ZXI=0, // Zeropage X Indirect: (0x00??+X).
		AM_ZIY=0, // Zeropage Indirect Y: (0x00??)+Y.
		AM_ZREL=0, // Zeropage and relative: 0x00??, pc+0x??.
		AM_A=0, // Absolute: 0x????.
		AM_AX=0, // Absolute X: 0x????+X.
		AM_AY=0, // Absolute Y: 0x????+Y.
		AM_AI1=0, // Absolute Indirect (for 6502): (0x????).
		AM_AI2=0, // Absolute Indirect (for 65c02): (0x????).
		AM_AXI=0, // Absolute X Indirect: (0x????+X).
		AM_BIT=0, // Bit: for RMB, BBR, BBS.
		AM_BREAK=0, // Only for break instruction (to skip the dummy byte).
		AM_COUNT=0; // Number of addressing modes.

	// Instructions.
	static final int
		I_ILLEGAL=0,
		I_NOP=1,
		I_LDA=2, I_LDX=3, I_LDY=4,
		I_STA=3, I_STX=3, I_STY=3, I_STZ=4,
		I_PHA=0, I_PHX=0, I_PHY=0, I_PHP=0,
		I_PLA=0, I_PLX=0, I_PLY=0, I_PLP=0,
		I_TAX=7, I_TAY=8, I_TXA=9, I_TYA=0, I_TSX=0, I_TXS=0,
		I_ADC_02=0, I_ADC_C02=0, I_SBC_02=0, I_SBC_C02=0,
		I_CMP=0, I_CMPX=0, I_CMPY=0,
		I_INC=0, I_INCA=0, I_INCX=0, I_INCY=18,
		I_DEC=0, I_DECA=0, I_DECX=0, I_DECY=0,
		I_AND=0, I_EOR=0, I_OR=0,
		I_ASL=0, I_ASLA=0,
		I_LSR=0, I_LSRA=0,
		I_ROL=0, I_ROLA=0,
		I_ROR=0, I_RORA=0,
		I_RMB=0, I_SMB=0, I_TRB=0, I_TSB=0, I_BIT=0, I_BIT_IMM=0,
		I_BRA=0,
		I_BCC=0, I_BCS=0, I_BEQ=0, I_BNE=0, I_BMI=0, I_BPL=0, I_BVC=0, I_BVS=0,
		I_BBR=0, I_BBS=0,
		I_JMP=0, I_JMP_AI1=0, I_JMP_AI2=0,
		I_JSR=0, I_RTS=0,
		I_CLC=0, I_CLD=0, I_CLI=0, I_CLV=0, I_SEC=0, I_SED=0, I_SEI=0,
		I_RTI=0, I_BRK_02=0, I_BRK_C02=0, I_WAI=0, I_STP=0;

	// Model mask.
	static final int
		M_6502=			1<<CPU.MODEL_6502,
		M_65C02=		1<<CPU.MODEL_65C02,
		M_65SC02=		1<<CPU.MODEL_65SC02,
		M_65816=		1<<CPU.MODEL_65816,
		M_65C02_UP=		0xffffffff-(1<<CPU.MODEL_6502),
		M_ALL=			0xffffffff;

	// Instruction table fields.
	static final int
		IT_OPCODE=			0,
		IT_ID=				1,
		IT_DST_AM=			2,
		IT_SRC_AM=			3,
		IT_LENGTH=			4,
		IT_CYCLES=			5,
		IT_MODELS=			6;

	// Instruction table.
	static final int INSTRUCTIONS_TABLE[][]={
		{ 0x00,		I_ILLEGAL,				AM_ILL,		AM_ILL,		0,		0,		M_ALL },

		{ 0xea,		I_NOP,					AM_IMP,		AM_IMP,		0,		0,		M_ALL },

		// Load A.
		{ 0xa1,		I_LDA,					AM_ACC,		AM_ZXI,		0,		0,		M_ALL },
		{ 0xa5,		I_LDA,					AM_ACC,		AM_Z,		0,		0,		M_ALL },
		{ 0xa9,		I_LDA,					AM_ACC,		AM_IMM,		0,		0,		M_ALL },
		{ 0xad,		I_LDA,					AM_ACC,		AM_A,		0,		0,		M_ALL },
		{ 0xb1,		I_LDA,					AM_ACC,		AM_ZIY,		0,		0,		M_ALL },
		{ 0xb2,		I_LDA,					AM_ACC,		AM_ZI,		0,		0,		M_65C02_UP },
		{ 0xb5,		I_LDA,					AM_ACC,		AM_ZX,		0,		0,		M_ALL },
		{ 0xb9,		I_LDA,					AM_ACC,		AM_AY,		0,		0,		M_ALL },
		{ 0xbd,		I_LDA,					AM_ACC,		AM_AX,		0,		0,		M_ALL },
		// Load X.
		{ 0xa2,		I_LDX,					AM_X,		AM_IMM,		0,		0,		M_ALL },
		{ 0xa6,		I_LDX,					AM_X,		AM_Z,		0,		0,		M_ALL },
		{ 0xae,		I_LDX,					AM_X,		AM_A,		0,		0,		M_ALL },
		{ 0xb6,		I_LDX,					AM_X,		AM_ZY,		0,		0,		M_ALL },
		{ 0xbe,		I_LDX,					AM_X,		AM_AY,		0,		0,		M_ALL },
		// Load Y.
		{ 0xa0,		I_LDY,					AM_Y,		AM_IMM,		0,		0,		M_ALL },
		{ 0xa4,		I_LDY,					AM_Y,		AM_Z,		0,		0,		M_ALL },
		{ 0xac,		I_LDY,					AM_Y,		AM_A,		0,		0,		M_ALL },
		{ 0xb4,		I_LDY,					AM_Y,		AM_ZX,		0,		0,		M_ALL },
		{ 0xbc,		I_LDY,					AM_Y,		AM_AX,		0,		0,		M_ALL },
		// Store A.
		{ 0x81,		I_STA,					AM_ZXI,		AM_ACC,		0,		0,		M_ALL },
		{ 0x85,		I_STA,					AM_Z,		AM_ACC,		0,		0,		M_ALL },
		{ 0x8d,		I_STA,					AM_A,		AM_ACC,		0,		0,		M_ALL },
		{ 0x91,		I_STA,					AM_ZIY,		AM_ACC,		0,		0,		M_ALL },
		{ 0x92,		I_STA,					AM_ZI,		AM_ACC,		0,		0,		M_65C02_UP },
		{ 0x95,		I_STA,					AM_ZX,		AM_ACC,		0,		0,		M_ALL },
		{ 0x99,		I_STA,					AM_AY,		AM_ACC,		0,		0,		M_ALL },
		{ 0x9d,		I_STA,					AM_AX,		AM_ACC,		0,		0,		M_ALL },
		// Store X.
		{ 0x86,		I_STX,					AM_Z,		AM_X,		0,		0,		M_ALL },
		{ 0x8e,		I_STX,					AM_A,		AM_X,		0,		0,		M_ALL },
		{ 0x96,		I_STX,					AM_ZY,		AM_X,		0,		0,		M_ALL },
		// Store Y.
		{ 0x84,		I_STY,					AM_Z,		AM_Y,		0,		0,		M_ALL },
		{ 0x8c,		I_STY,					AM_A,		AM_Y,		0,		0,		M_ALL },
		{ 0x94,		I_STY,					AM_ZX,		AM_Y,		0,		0,		M_ALL },
		// Store zero.
		{ 0x64,		I_STZ,					AM_Z,		AM_IMP,		0,		0,		M_65C02_UP },
		{ 0x74,		I_STZ,					AM_ZX,		AM_IMP,		0,		0,		M_65C02_UP },
		{ 0x9c,		I_STZ,					AM_A,		AM_IMP,		0,		0,		M_65C02_UP },
		{ 0x9e,		I_STZ,					AM_AX,		AM_IMP,		0,		0,		M_65C02_UP },
		// Push.
		{ 0x08,		I_PHP,					AM_IMP,		AM_P,		0,		0,		M_ALL },
		{ 0x48,		I_PHA,					AM_IMP,		AM_ACC,		0,		0,		M_ALL },
		{ 0x5a,		I_PHY,					AM_IMP,		AM_Y,		0,		0,		M_65C02_UP },
		{ 0xda,		I_PHX,					AM_IMP,		AM_X,		0,		0,		M_65C02_UP },
		// Pop.
		{ 0x28,		I_PLP,					AM_P,		AM_IMP,		0,		0,		M_ALL },
		{ 0x68,		I_PLA,					AM_ACC,		AM_IMP,		0,		0,		M_ALL },
		{ 0x7a,		I_PLY,					AM_Y,		AM_IMP,		0,		0,		M_65C02_UP },
		{ 0xfa,		I_PLX,					AM_X,		AM_IMP,		0,		0,		M_65C02_UP },
		// Register move.
		{ 0x8a,		I_TXA,					AM_ACC,		AM_X,		0,		0,		M_ALL },
		{ 0x98,		I_TYA,					AM_ACC,		AM_Y,		0,		0,		M_ALL },
		{ 0x9a,		I_TXS,					AM_S,		AM_X,		0,		0,		M_ALL },
		{ 0xaa,		I_TAX,					AM_X,		AM_ACC,		0,		0,		M_ALL },
		{ 0xab,		I_TAY,					AM_Y,		AM_ACC,		0,		0,		M_ALL },
		{ 0xba,		I_TSX,					AM_X,		AM_S,		0,		0,		M_ALL },
		// Add.
		{ 0x61,		I_ADC_02,				AM_ACC,		AM_ZXI,		0,		0,		M_6502 },
		{ 0x65,		I_ADC_02,				AM_ACC,		AM_Z,		0,		0,		M_6502 },
		{ 0x69,		I_ADC_02,				AM_ACC,		AM_IMM,		0,		0,		M_6502 },
		{ 0x6d,		I_ADC_02,				AM_ACC,		AM_A,		0,		0,		M_6502 },
		{ 0x71,		I_ADC_02,				AM_ACC,		AM_ZIY,		0,		0,		M_6502 },
		{ 0x75,		I_ADC_02,				AM_ACC,		AM_ZX,		0,		0,		M_6502 },
		{ 0x79,		I_ADC_02,				AM_ACC,		AM_AY,		0,		0,		M_6502 },
		{ 0x7d,		I_ADC_02,				AM_ACC,		AM_AX,		0,		0,		M_6502 },
		{ 0x61,		I_ADC_C02,				AM_ACC,		AM_ZXI,		0,		0,		M_65C02_UP },
		{ 0x65,		I_ADC_C02,				AM_ACC,		AM_Z,		0,		0,		M_65C02_UP },
		{ 0x69,		I_ADC_C02,				AM_ACC,		AM_IMM,		0,		0,		M_65C02_UP },
		{ 0x6d,		I_ADC_C02,				AM_ACC,		AM_A,		0,		0,		M_65C02_UP },
		{ 0x71,		I_ADC_C02,				AM_ACC,		AM_ZIY,		0,		0,		M_65C02_UP },
		{ 0x72,		I_ADC_C02,				AM_ACC,		AM_ZI,		0,		0,		M_65C02_UP },
		{ 0x75,		I_ADC_C02,				AM_ACC,		AM_ZX,		0,		0,		M_65C02_UP },
		{ 0x79,		I_ADC_C02,				AM_ACC,		AM_AY,		0,		0,		M_65C02_UP },
		{ 0x7d,		I_ADC_C02,				AM_ACC,		AM_AX,		0,		0,		M_65C02_UP },
		// Sub.
		{ 0xe1,		I_ADC_02,				AM_ACC,		AM_ZXI,		0,		0,		M_6502 },
		{ 0xe5,		I_ADC_02,				AM_ACC,		AM_Z,		0,		0,		M_6502 },
		{ 0xe9,		I_ADC_02,				AM_ACC,		AM_IMM,		0,		0,		M_6502 },
		{ 0xed,		I_ADC_02,				AM_ACC,		AM_A,		0,		0,		M_6502 },
		{ 0xf1,		I_ADC_02,				AM_ACC,		AM_ZIY,		0,		0,		M_6502 },
		{ 0xf5,		I_ADC_02,				AM_ACC,		AM_ZX,		0,		0,		M_6502 },
		{ 0xf9,		I_ADC_02,				AM_ACC,		AM_AY,		0,		0,		M_6502 },
		{ 0xfd,		I_ADC_02,				AM_ACC,		AM_AX,		0,		0,		M_6502 },
		{ 0xe1,		I_ADC_C02,				AM_ACC,		AM_ZXI,		0,		0,		M_65C02_UP },
		{ 0xe5,		I_ADC_C02,				AM_ACC,		AM_Z,		0,		0,		M_65C02_UP },
		{ 0xe9,		I_ADC_C02,				AM_ACC,		AM_IMM,		0,		0,		M_65C02_UP },
		{ 0xed,		I_ADC_C02,				AM_ACC,		AM_A,		0,		0,		M_65C02_UP },
		{ 0xf1,		I_ADC_C02,				AM_ACC,		AM_ZIY,		0,		0,		M_65C02_UP },
		{ 0xf2,		I_ADC_C02,				AM_ACC,		AM_ZI,		0,		0,		M_65C02_UP },
		{ 0xf5,		I_ADC_C02,				AM_ACC,		AM_ZX,		0,		0,		M_65C02_UP },
		{ 0xf9,		I_ADC_C02,				AM_ACC,		AM_AY,		0,		0,		M_65C02_UP },
		{ 0xfd,		I_ADC_C02,				AM_ACC,		AM_AX,		0,		0,		M_65C02_UP },
		// Compare to A.
		{ 0xcd,		I_CMP,					AM_ACC,		AM_A,		0,		0,		M_ALL },
		{ 0xdd,		I_CMP,					AM_ACC,		AM_AX,		0,		0,		M_ALL },
		{ 0xd9,		I_CMP,					AM_ACC,		AM_AY,		0,		0,		M_ALL },
		{ 0xc9,		I_CMP,					AM_ACC,		AM_IMM,		0,		0,		M_ALL },
		{ 0xc5,		I_CMP,					AM_ACC,		AM_Z,		0,		0,		M_ALL },
		{ 0xc1,		I_CMP,					AM_ACC,		AM_ZXI,		0,		0,		M_ALL },
		{ 0xd5,		I_CMP,					AM_ACC,		AM_ZX,		0,		0,		M_ALL },
		{ 0xd2,		I_CMP,					AM_ACC,		AM_ZI,		0,		0,		M_65C02_UP },
		{ 0xd1,		I_CMP,					AM_ACC,		AM_ZIY,		0,		0,		M_ALL },
		// Compare to X.
		{ 0xec,		I_CMPX,					AM_ACC,		AM_A,		0,		0,		M_ALL },
		{ 0xe0,		I_CMPX,					AM_ACC,		AM_IMM,		0,		0,		M_ALL },
		{ 0xe4,		I_CMPX,					AM_ACC,		AM_Z,		0,		0,		M_ALL },
		// Compare to Y.
		{ 0xcc,		I_CMPY,					AM_ACC,		AM_A,		0,		0,		M_ALL },
		{ 0xc0,		I_CMPY,					AM_ACC,		AM_IMM,		0,		0,		M_ALL },
		{ 0xc4,		I_CMPY,					AM_ACC,		AM_Z,		0,		0,		M_ALL },
		// Decrement.
		{ 0xce,		I_DEC,					AM_A,		AM_IMP,		0,		0,		M_ALL },
		{ 0xde,		I_DEC,					AM_AX,		AM_IMP,		0,		0,		M_ALL },
		{ 0xc6,		I_DEC,					AM_Z,		AM_IMP,		0,		0,		M_ALL },
		{ 0xd6,		I_DEC,					AM_ZX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x3a,		I_DECA,					AM_ACC,		AM_IMP,		0,		0,		M_ALL },
		{ 0xca,		I_DECX,					AM_X,		AM_IMP,		0,		0,		M_ALL },
		{ 0x88,		I_DECY,					AM_Y,		AM_IMP,		0,		0,		M_ALL },
		// Increment.
		{ 0xee,		I_INC,					AM_A,		AM_IMP,		0,		0,		M_ALL },
		{ 0xfe,		I_INC,					AM_AX,		AM_IMP,		0,		0,		M_ALL },
		{ 0xe6,		I_INC,					AM_Z,		AM_IMP,		0,		0,		M_ALL },
		{ 0xf6,		I_INC,					AM_ZX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x1a,		I_INCA,					AM_ACC,		AM_IMP,		0,		0,		M_ALL },
		{ 0xe8,		I_INCX,					AM_X,		AM_IMP,		0,		0,		M_ALL },
		{ 0xc8,		I_INCY,					AM_Y,		AM_IMP,		0,		0,		M_ALL },
		// And.
		{ 0x2d,		I_AND,					AM_ACC,		AM_A,		0,		0,		M_ALL },
		{ 0x3d,		I_AND,					AM_ACC,		AM_AX,		0,		0,		M_ALL },
		{ 0x39,		I_AND,					AM_ACC,		AM_AY,		0,		0,		M_ALL },
		{ 0x29,		I_AND,					AM_ACC,		AM_IMM,		0,		0,		M_ALL },
		{ 0x25,		I_AND,					AM_ACC,		AM_Z,		0,		0,		M_ALL },
		{ 0x21,		I_AND,					AM_ACC,		AM_ZXI,		0,		0,		M_ALL },
		{ 0x35,		I_AND,					AM_ACC,		AM_ZX,		0,		0,		M_ALL },
		{ 0x32,		I_AND,					AM_ACC,		AM_ZI,		0,		0,		M_65C02_UP },
		{ 0x31,		I_AND,					AM_ACC,		AM_ZIY,		0,		0,		M_ALL },
		// Or.
		{ 0x0d,		I_OR,					AM_ACC,		AM_A,		0,		0,		M_ALL },
		{ 0x1d,		I_OR,					AM_ACC,		AM_AX,		0,		0,		M_ALL },
		{ 0x19,		I_OR,					AM_ACC,		AM_AY,		0,		0,		M_ALL },
		{ 0x09,		I_OR,					AM_ACC,		AM_IMM,		0,		0,		M_ALL },
		{ 0x05,		I_OR,					AM_ACC,		AM_Z,		0,		0,		M_ALL },
		{ 0x01,		I_OR,					AM_ACC,		AM_ZXI,		0,		0,		M_ALL },
		{ 0x15,		I_OR,					AM_ACC,		AM_ZX,		0,		0,		M_ALL },
		{ 0x12,		I_OR,					AM_ACC,		AM_ZI,		0,		0,		M_65C02_UP },
		{ 0x11,		I_OR,					AM_ACC,		AM_ZIY,		0,		0,		M_ALL },
		// Exclusive or.
		{ 0x4d,		I_EOR,					AM_ACC,		AM_A,		0,		0,		M_ALL },
		{ 0x5d,		I_EOR,					AM_ACC,		AM_AX,		0,		0,		M_ALL },
		{ 0x59,		I_EOR,					AM_ACC,		AM_AY,		0,		0,		M_ALL },
		{ 0x49,		I_EOR,					AM_ACC,		AM_IMM,		0,		0,		M_ALL },
		{ 0x45,		I_EOR,					AM_ACC,		AM_Z,		0,		0,		M_ALL },
		{ 0x41,		I_EOR,					AM_ACC,		AM_ZXI,		0,		0,		M_ALL },
		{ 0x55,		I_EOR,					AM_ACC,		AM_ZX,		0,		0,		M_ALL },
		{ 0x52,		I_EOR,					AM_ACC,		AM_ZI,		0,		0,		M_65C02_UP },
		{ 0x51,		I_EOR,					AM_ACC,		AM_ZIY,		0,		0,		M_ALL },
		// Arithmetic shift left.
		{ 0x0e,		I_ASL,					AM_A,		AM_IMP,		0,		0,		M_ALL },
		{ 0x1e,		I_ASL,					AM_AX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x06,		I_ASL,					AM_Z,		AM_IMP,		0,		0,		M_ALL },
		{ 0x16,		I_ASL,					AM_ZX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x0a,		I_ASLA,					AM_ACC,		AM_IMP,		0,		0,		M_ALL },
		// Logic shift right.
		{ 0x4e,		I_LSR,					AM_A,		AM_IMP,		0,		0,		M_ALL },
		{ 0x5e,		I_LSR,					AM_AX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x46,		I_LSR,					AM_Z,		AM_IMP,		0,		0,		M_ALL },
		{ 0x56,		I_LSR,					AM_ZX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x4a,		I_LSRA,					AM_ACC,		AM_IMP,		0,		0,		M_ALL },
		// Rotate left.
		{ 0x2e,		I_ROL,					AM_A,		AM_IMP,		0,		0,		M_ALL },
		{ 0x3e,		I_ROL,					AM_AX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x26,		I_ROL,					AM_Z,		AM_IMP,		0,		0,		M_ALL },
		{ 0x36,		I_ROL,					AM_ZX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x2a,		I_ROLA,					AM_ACC,		AM_IMP,		0,		0,		M_ALL },
		// Rotate right.
		{ 0x6e,		I_ROR,					AM_A,		AM_IMP,		0,		0,		M_ALL },
		{ 0x7e,		I_ROR,					AM_AX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x66,		I_ROR,					AM_Z,		AM_IMP,		0,		0,		M_ALL },
		{ 0x76,		I_ROR,					AM_ZX,		AM_IMP,		0,		0,		M_ALL },
		{ 0x6a,		I_RORA,					AM_ACC,		AM_IMP,		0,		0,		M_ALL },
		// Reset/set memory bit.
		{ 0x07,		I_RMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x17,		I_RMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x27,		I_RMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x37,		I_RMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x47,		I_RMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x57,		I_RMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x67,		I_RMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x77,		I_RMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x87,		I_SMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0x97,		I_SMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0xa7,		I_SMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0xb7,		I_SMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0xc7,		I_SMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0xd7,		I_SMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0xe7,		I_SMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		{ 0xf7,		I_SMB,					AM_IMP,		AM_BIT,		2,		5,		M_65C02 },
		// Test and set/reset bit.
		{ 0x1c,		I_TRB,					AM_A,		AM_IMP,		3,		6,		M_65C02_UP },
		{ 0x14,		I_TRB,					AM_Z,		AM_IMP,		2,		5,		M_65C02_UP },
		{ 0x0c,		I_TSB,					AM_A,		AM_IMP,		3,		6,		M_65C02_UP },
		{ 0x04,		I_TSB,					AM_Z,		AM_IMP,		2,		5,		M_65C02_UP },
		// Bit.
		{ 0x2c,		I_BIT,					AM_IMP,		AM_A,		3,		4,		M_ALL },
		{ 0x3c,		I_BIT,					AM_IMP,		AM_AX,		3,		4,		M_ALL },
		{ 0x24,		I_BIT,					AM_IMP,		AM_Z,		2,		3,		M_ALL },
		{ 0x34,		I_BIT,					AM_IMP,		AM_ZX,		2,		4,		M_ALL },
		{ 0x89,		I_BIT_IMM,				AM_IMP,		AM_IMM,		2,		2,		M_65C02_UP },
		// Branch.
		{ 0x10,		I_BPL,					AM_IMP,		AM_REL,		2,		2,		M_ALL },
		{ 0x30,		I_BMI,					AM_IMP,		AM_REL,		2,		2,		M_ALL },
		{ 0x50,		I_BVC,					AM_IMP,		AM_REL,		2,		2,		M_ALL },
		{ 0x70,		I_BVS,					AM_IMP,		AM_REL,		2,		2,		M_ALL },
		{ 0x90,		I_BCC,					AM_IMP,		AM_REL,		2,		2,		M_ALL },
		{ 0xb0,		I_BCS,					AM_IMP,		AM_REL,		2,		2,		M_ALL },
		{ 0xd0,		I_BNE,					AM_IMP,		AM_REL,		2,		2,		M_ALL },
		{ 0xf0,		I_BEQ,					AM_IMP,		AM_REL,		2,		2,		M_ALL },
		{ 0x80,		I_BRA,					AM_IMP,		AM_REL,		2,		3,		M_65C02_UP },
		// Test and branch.
		{ 0x0f,		I_BBR,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x1f,		I_BBR,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x2f,		I_BBR,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x3f,		I_BBR,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x4f,		I_BBR,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x5f,		I_BBR,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x6f,		I_BBR,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x7f,		I_BBR,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x8f,		I_BBS,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0x9f,		I_BBS,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0xaf,		I_BBS,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0xbf,		I_BBS,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0xcf,		I_BBS,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0xdf,		I_BBS,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0xef,		I_BBS,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		{ 0xff,		I_BBS,					AM_Z,		AM_REL,		3,		5,		M_65C02 },
		// Jump.
		{ 0x4c,		I_JMP,					AM_IMP,		AM_A,		3,		3,		M_ALL },
		{ 0x6c,		I_JMP,					AM_IMP,		AM_AI1,		3,		6,		M_6502 },
		{ 0x6c,		I_JMP,					AM_IMP,		AM_AI2,		3,		6,		M_65C02_UP },
		{ 0x7c,		I_JMP,					AM_IMP,		AM_AXI,		3,		6,		M_65C02_UP },
		// Jump to subroutine.
		{ 0x20,		I_JSR,					AM_IMP,		AM_A,		3,		6,		M_ALL },
		{ 0x60,		I_RTS,					AM_IMP,		AM_IMP,		1,		6,		M_ALL },
		// Status bits.
		{ 0x18,		I_CLC,					AM_IMP,		AM_IMP,		1,		2,		M_ALL },
		{ 0x38,		I_SEC,					AM_IMP,		AM_IMP,		1,		2,		M_ALL },
		{ 0x58,		I_CLC,					AM_IMP,		AM_IMP,		1,		2,		M_ALL },
		{ 0x78,		I_SEC,					AM_IMP,		AM_IMP,		1,		2,		M_ALL },
		{ 0xb8,		I_CLV,					AM_IMP,		AM_IMP,		1,		2,		M_ALL },
		{ 0xd8,		I_CLD,					AM_IMP,		AM_IMP,		1,		2,		M_ALL },
		{ 0xf8,		I_SED,					AM_IMP,		AM_IMP,		1,		2,		M_ALL },
		// System.
		{ 0x40,		I_RTI,					AM_IMP,		AM_IMP,		1,		6,		M_ALL },
		{ 0x00,		I_BRK_02,				AM_IMP,		AM_IMM,		2,		7,		M_6502 },
		{ 0x00,		I_BRK_C02,				AM_IMP,		AM_IMM,		2,		7,		M_65C02_UP },
		{ 0xcb,		I_WAI,					AM_IMP,		AM_IMP,		1,		3,		M_ALL },
		{ 0xdb,		I_STP,					AM_IMP,		AM_IMP,		1,		3,		M_ALL },
	};
}


/*
// Models shortcuts.
enum {
	M_02		=1<<CPU_65XX_Emulator::MODEL_6502,
	M_C02		=1<<CPU_65XX_Emulator::MODEL_65C02,
	M_SC02	=1<<CPU_65XX_Emulator::MODEL_65SC02
};

const int8 CPU_65XX_Emulator_P::INSTRUCTION_SIZE[AM_COUNT]={
	1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 2
};

const OpcodeTableEntry CPU_65XX_Emulator_P::INSTRUCTIONS_TABLE[]={
	{ i_illegal_6502, 	0x00, AM_ILL,	1,	2,	"???",	0 },
	{ i_illegal_65C02, 	0x00, AM_ILL,	1,	2,	"???",	0 },
	{ i_nop, 				0xea, AM_IMP,	1,	2,	"nop",	M_02|	M_C02|	M_SC02 },

	// Load.
	{ i_lda, 				0xa1, AM_ZXI,	2,	6,	"lda",	M_02|	M_C02|	M_SC02 },
	{ i_lda, 				0xa5, AM_Z,		2,	3,	"lda",	M_02|	M_C02|	M_SC02 },
	{ i_lda, 				0xa9, AM_IMM,	2,	2,	"lda",	M_02|	M_C02|	M_SC02 },
	{ i_lda, 				0xad, AM_A,		3,	4,	"lda",	M_02|	M_C02|	M_SC02 },
	{ i_lda, 				0xb5, AM_ZX,	2,	4,	"lda",	M_02|	M_C02|	M_SC02 },
	{ i_lda, 				0xb1, AM_ZIY,	2,	5,	"lda",	M_02|	M_C02|	M_SC02 },
	{ i_lda, 				0xb2, AM_ZI,	2,	5,	"lda",			M_C02|	M_SC02 },
	{ i_lda, 				0xb9, AM_AY,	3,	4,	"lda",	M_02|	M_C02|	M_SC02 },
	{ i_lda, 				0xbd, AM_AX,	3,	4,	"lda",	M_02|	M_C02|	M_SC02 },

	{ i_ldx, 				0xa2, AM_IMM,	2,	2,	"ldx",	M_02|	M_C02|	M_SC02 },
	{ i_ldx, 				0xa6, AM_Z,		2,	3,	"ldx",	M_02|	M_C02|	M_SC02 },
	{ i_ldx, 				0xb6, AM_ZY,	2,	4,	"ldx",	M_02|	M_C02|	M_SC02 },
	{ i_ldx, 				0xae, AM_A,		3,	4,	"ldx",	M_02|	M_C02|	M_SC02 },
	{ i_ldx, 				0xbe, AM_AY,	3,	4,	"ldx",	M_02|	M_C02|	M_SC02 },

	{ i_ldy, 				0xa0, AM_IMM,	2,	2,	"ldy",	M_02|	M_C02|	M_SC02 },
	{ i_ldy, 				0xa4, AM_Z,		2,	3,	"ldy",	M_02|	M_C02|	M_SC02 },
	{ i_ldy, 				0xb4, AM_ZX,	2,	4,	"ldy",	M_02|	M_C02|	M_SC02 },
	{ i_ldy, 				0xac, AM_A,		3,	4,	"ldy",	M_02|	M_C02|	M_SC02 },
	{ i_ldy, 				0xbc, AM_AX,	3,	4,	"ldy",	M_02|	M_C02|	M_SC02 },

	// Store.
	{ i_sta, 				0x85, AM_Z,		2,	3,	"sta",	M_02|	M_C02|	M_SC02 },
	{ i_sta, 				0x95, AM_ZX,	2,	4,	"sta",	M_02|	M_C02|	M_SC02 },
	{ i_sta, 				0x92, AM_ZI,	2,	5,	"sta",			M_C02|	M_SC02 },
	{ i_sta, 				0x81, AM_ZXI,	2,	6,	"sta",	M_02|	M_C02|	M_SC02 },
	{ i_sta, 				0x91, AM_ZIY,	2,	6,	"sta",	M_02|	M_C02|	M_SC02 },
	{ i_sta, 				0x8d, AM_A,		3,	4,	"sta",	M_02|	M_C02|	M_SC02 },
	{ i_sta, 				0x9d, AM_AX,	3,	5,	"sta",	M_02|	M_C02|	M_SC02 },
	{ i_sta, 				0x99, AM_AY,	3,	5,	"sta",	M_02|	M_C02|	M_SC02 },

	{ i_stx, 				0x86, AM_Z,		2,	3,	"stx",	M_02|	M_C02|	M_SC02 },
	{ i_stx, 				0x96, AM_ZY,	2,	4,	"stx",	M_02|	M_C02|	M_SC02 },
	{ i_stx, 				0x8e, AM_A,		3,	4,	"stx",	M_02|	M_C02|	M_SC02 },

	{ i_sty, 				0x84, AM_Z,		2,	3,	"sty",	M_02|	M_C02|	M_SC02 },
	{ i_sty, 				0x94, AM_ZX,	2,	4,	"sty",	M_02|	M_C02|	M_SC02 },
	{ i_sty, 				0x8c, AM_A,		3,	4,	"sty",	M_02|	M_C02|	M_SC02 },

	{ i_stz, 				0x64, AM_Z,		2,	3,	"stz",			M_C02|	M_SC02 },
	{ i_stz, 				0x74, AM_ZX,	2,	4,	"stz",			M_C02|	M_SC02 },
	{ i_stz, 				0x9c, AM_A,		3,	4,	"stz",			M_C02|	M_SC02 },
	{ i_stz, 				0x9e, AM_AX,	3,	5,	"stz",			M_C02|	M_SC02 },

	// Transfer.
	{ i_tax, 				0xaa, AM_IMP,	1,	2,	"tax",	M_02|	M_C02|	M_SC02 },
	{ i_tay, 				0xa8, AM_IMP,	1,	2,	"tay",	M_02|	M_C02|	M_SC02 },
	{ i_txa, 				0x8a, AM_IMP,	1,	2,	"txa",	M_02|	M_C02|	M_SC02 },
	{ i_tya, 				0x98, AM_IMP,	1,	2,	"tya",	M_02|	M_C02|	M_SC02 },
	{ i_txs, 				0x9a, AM_IMP,	1,	2,	"txs",	M_02|	M_C02|	M_SC02 },
	{ i_tsx, 				0xba, AM_IMP,	1,	2,	"tsx",	M_02|	M_C02|	M_SC02 },

	// Stack.
	{ i_php, 				0x08, AM_IMP,	1,	3,	"php",	M_02|	M_C02|	M_SC02 },
	{ i_pha, 				0x48, AM_IMP,	1,	3,	"pha",	M_02|	M_C02|	M_SC02 },
	{ i_phx, 				0xda, AM_IMP,	1,	3,	"phx",			M_C02|	M_SC02 },
	{ i_phy, 				0x5a, AM_IMP,	1,	3,	"phy",			M_C02|	M_SC02 },
	{ i_plp, 				0x28, AM_IMP,	1,	4,	"plp",	M_02|	M_C02|	M_SC02 },
	{ i_pla, 				0x68, AM_IMP,	1,	4,	"pla",	M_02|	M_C02|	M_SC02 },
	{ i_plx, 				0xfa, AM_IMP,	1,	4,	"plx",			M_C02|	M_SC02 },
	{ i_ply, 				0x7a, AM_IMP,	1,	4,	"ply",			M_C02|	M_SC02 },

	// Arithmetic operations.
	{ i_adc_6502, 			0x69, AM_IMM,	2,	2,	"adc",	M_02 },
	{ i_adc_6502, 			0x65, AM_Z,		2,	3,	"adc",	M_02 },
	{ i_adc_6502, 			0x75, AM_ZX,	2,	4,	"adc",	M_02 },
	{ i_adc_6502, 			0x61, AM_ZXI,	2,	6,	"adc",	M_02 },
	{ i_adc_6502, 			0x71, AM_ZIY,	2,	5,	"adc",	M_02 },
	{ i_adc_6502, 			0x6d, AM_A,		3,	4,	"adc",	M_02 },
	{ i_adc_6502, 			0x7d, AM_AX,	3,	4,	"adc",	M_02 },
	{ i_adc_6502, 			0x79, AM_AY,	3,	4,	"adc",	M_02 },

	{ i_adc_65C02, 		0x69, AM_IMM,	2,	2,	"adc",			M_C02|	M_SC02 },
	{ i_adc_65C02, 		0x65, AM_Z,		2,	3,	"adc",			M_C02|	M_SC02 },
	{ i_adc_65C02, 		0x75, AM_ZX,	2,	4,	"adc",			M_C02|	M_SC02 },
	{ i_adc_65C02, 		0x72, AM_ZI,	2,	5,	"adc",			M_C02|	M_SC02 },
	{ i_adc_65C02,			0x61, AM_ZXI,	2,	6,	"adc",			M_C02|	M_SC02 },
	{ i_adc_65C02, 		0x71, AM_ZIY,	2,	5,	"adc",			M_C02|	M_SC02 },
	{ i_adc_65C02, 		0x6d, AM_A,		3,	4,	"adc",			M_C02|	M_SC02 },
	{ i_adc_65C02, 		0x7d, AM_AX,	3,	4,	"adc",			M_C02|	M_SC02 },
	{ i_adc_65C02, 		0x79, AM_AY,	3,	4,	"adc",			M_C02|	M_SC02 },

	{ i_sbc_6502, 			0xe9, AM_IMM,	2,	2,	"sbc",	M_02 },
	{ i_sbc_6502, 			0xe5, AM_Z,		2,	3,	"sbc",	M_02 },
	{ i_sbc_6502, 			0xf5, AM_ZX,	2,	4,	"sbc",	M_02 },
	{ i_sbc_6502, 			0xe1, AM_ZXI,	2,	6,	"sbc",	M_02 },
	{ i_sbc_6502, 			0xf1, AM_ZIY,	2,	5,	"sbc",	M_02 },
	{ i_sbc_6502, 			0xed, AM_A,		3,	4,	"sbc",	M_02 },
	{ i_sbc_6502, 			0xfd, AM_AX,	3,	4,	"sbc",	M_02 },
	{ i_sbc_6502, 			0xf9, AM_AY,	3,	4,	"sbc",	M_02 },

	{ i_sbc_65C02, 		0xe9, AM_IMM,	2,	2,	"sbc",			M_C02|	M_SC02 },
	{ i_sbc_65C02, 		0xe5, AM_Z,		2,	3,	"sbc",			M_C02|	M_SC02 },
	{ i_sbc_65C02, 		0xf5, AM_ZX,	2,	4,	"sbc",			M_C02|	M_SC02 },
	{ i_sbc_65C02, 		0xf2, AM_ZI,	2,	5,	"sbc",			M_C02|	M_SC02 },
	{ i_sbc_65C02, 		0xe1, AM_ZXI,	2,	6,	"sbc",			M_C02|	M_SC02 },
	{ i_sbc_65C02, 		0xf1, AM_ZIY,	2,	5,	"sbc",			M_C02|	M_SC02 },
	{ i_sbc_65C02, 		0xed, AM_A,		3,	4,	"sbc",			M_C02|	M_SC02 },
	{ i_sbc_65C02, 		0xfd, AM_AX,	3,	4,	"sbc",			M_C02|	M_SC02 },
	{ i_sbc_65C02, 		0xf9, AM_AY,	3,	4,	"sbc",			M_C02|	M_SC02 },

	{ i_inca, 				0x1a, AM_ACC,	1,	2,	"inc",			M_C02|	M_SC02 },
	{ i_inx, 				0xe8, AM_X,		1,	2,	"inc",	M_02|	M_C02|	M_SC02 },
	{ i_iny, 				0xc8, AM_Y,		1,	2,	"inc",	M_02|	M_C02|	M_SC02 },
	{ i_inc, 				0xe6, AM_Z,		2,	5,	"inc",	M_02|	M_C02|	M_SC02 },
	{ i_inc, 				0xf6, AM_ZX,	2,	6,	"inc",	M_02|	M_C02|	M_SC02 },
	{ i_inc, 				0xee, AM_A,		3,	6,	"inc",	M_02|	M_C02|	M_SC02 },
	{ i_inc, 				0xfe, AM_AX,	3,	7,	"inc",	M_02|	M_C02|	M_SC02 },

	{ i_deca, 				0x3a, AM_ACC,	1,	2,	"dec",			M_C02|	M_SC02 },
	{ i_dex, 				0xca, AM_X,		1,	2,	"dec",	M_02|	M_C02|	M_SC02 },
	{ i_dey, 				0x88, AM_Y,		1,	2,	"dec",	M_02|	M_C02|	M_SC02 },
	{ i_dec, 				0xc6, AM_Z,		2,	5,	"dec",	M_02|	M_C02|	M_SC02 },
	{ i_dec, 				0xd6, AM_ZX,	2,	6,	"dec",	M_02|	M_C02|	M_SC02 },
	{ i_dec, 				0xce, AM_A,		3,	6,	"dec",	M_02|	M_C02|	M_SC02 },
	{ i_dec, 				0xde, AM_AX,	3,	7,	"dec",	M_02|	M_C02|	M_SC02 },

	// Logic operations.
	{ i_and, 				0x29, AM_IMM,	2,	2,	"and",	M_02|	M_C02|	M_SC02 },
	{ i_and, 				0x25, AM_Z,		2,	3,	"and",	M_02|	M_C02|	M_SC02 },
	{ i_and, 				0x35, AM_ZX,	2,	4,	"and",	M_02|	M_C02|	M_SC02 },
	{ i_and, 				0x32, AM_ZI,	2,	5,	"and",			M_C02|	M_SC02 },
	{ i_and, 				0x21, AM_ZXI,	2,	6,	"and",	M_02|	M_C02|	M_SC02 },
	{ i_and, 				0x31, AM_ZIY,	2,	5,	"and",	M_02|	M_C02|	M_SC02 },
	{ i_and, 				0x2d, AM_A,		3,	4,	"and",	M_02|	M_C02|	M_SC02 },
	{ i_and, 				0x3d, AM_AX,	3,	4,	"and",	M_02|	M_C02|	M_SC02 },
	{ i_and, 				0x39,	AM_AY,	3,	4,	"and",	M_02|	M_C02|	M_SC02 },

	{ i_eor, 				0x49, AM_IMM,	2,	2,	"eor",	M_02|	M_C02|	M_SC02 },
	{ i_eor, 				0x45, AM_Z,		2,	3,	"eor",	M_02|	M_C02|	M_SC02 },
	{ i_eor, 				0x55, AM_ZX,	2,	4,	"eor",	M_02|	M_C02|	M_SC02 },
	{ i_eor, 				0x52, AM_ZI,	2,	5,	"eor",			M_C02|	M_SC02 },
	{ i_eor, 				0x41, AM_ZXI,	2,	6,	"eor",	M_02|	M_C02|	M_SC02 },
	{ i_eor, 				0x51, AM_ZIY,	2,	5,	"eor",	M_02|	M_C02|	M_SC02 },
	{ i_eor, 				0x4d, AM_A,		3,	4,	"eor",	M_02|	M_C02|	M_SC02 },
	{ i_eor, 				0x59, AM_AY,	3,	4,	"eor",	M_02|	M_C02|	M_SC02 },
	{ i_eor, 				0x5d, AM_AX,	3,	4,	"eor",	M_02|	M_C02|	M_SC02 },

	{ i_ora, 				0x09, AM_IMM,	2,	2,	"ora",	M_02|	M_C02|	M_SC02 },
	{ i_ora, 				0x05, AM_Z,		2,	3,	"ora",	M_02|	M_C02|	M_SC02 },
	{ i_ora, 				0x15, AM_ZX,	2,	4,	"ora",	M_02|	M_C02|	M_SC02 },
	{ i_ora, 				0x12, AM_ZI,	2,	5,	"ora",			M_C02|	M_SC02 },
	{ i_ora, 				0x01, AM_ZXI,	2,	6,	"ora",	M_02|	M_C02|	M_SC02 },
	{ i_ora, 				0x11, AM_ZIY,	2,	5,	"ora",	M_02|	M_C02|	M_SC02 },
	{ i_ora, 				0x0d, AM_A,		3,	4,	"ora",	M_02|	M_C02|	M_SC02 },
	{ i_ora, 				0x1d, AM_AX,	3,	4,	"ora",	M_02|	M_C02|	M_SC02 },
	{ i_ora, 				0x19, AM_AY,	3,	4,	"ora",	M_02|	M_C02|	M_SC02 },

	{ i_asla, 				0x0a, AM_ACC,	1,	2,	"asl",	M_02|	M_C02|	M_SC02 },
	{ i_asl, 				0x06, AM_Z,		2,	5,	"asl",	M_02|	M_C02|	M_SC02 },
	{ i_asl, 				0x16, AM_ZX,	2,	6,	"asl",	M_02|	M_C02|	M_SC02 },
	{ i_asl, 				0x0e, AM_A,		3,	6,	"asl",	M_02|	M_C02|	M_SC02 },
	{ i_asl, 				0x1e, AM_AX,	3,	7,	"asl",	M_02|	M_C02|	M_SC02 },

	{ i_lsra, 				0x4a, AM_ACC,	1,	2,	"lsr",	M_02|	M_C02|	M_SC02 },
	{ i_lsr, 				0x46, AM_Z,		2,	5,	"lsr",	M_02|	M_C02|	M_SC02 },
	{ i_lsr, 				0x56, AM_ZX,	2,	6,	"lsr",	M_02|	M_C02|	M_SC02 },
	{ i_lsr, 				0x4e, AM_A,		3,	6,	"lsr",	M_02|	M_C02|	M_SC02 },
	{ i_lsr, 				0x5e, AM_AX,	3,	7,	"lsr",	M_02|	M_C02|	M_SC02 },

	{ i_rola, 				0x2a, AM_ACC,	1,	2,	"rol",	M_02|	M_C02|	M_SC02 },
	{ i_rol, 				0x26, AM_Z,		2,	5,	"rol",	M_02|	M_C02|	M_SC02 },
	{ i_rol, 				0x36, AM_ZX,	2,	6,	"rol",	M_02|	M_C02|	M_SC02 },
	{ i_rol, 				0x2e, AM_A,		3,	6,	"rol",	M_02|	M_C02|	M_SC02 },
	{ i_rol, 				0x3e, AM_AX,	3,	7,	"rol",	M_02|	M_C02|	M_SC02 },

	{ i_rora, 				0x6a, AM_ACC,	1,	2,	"ror",	M_02|	M_C02|	M_SC02 },
	{ i_ror, 				0x66, AM_Z,		2,	5,	"ror",	M_02|	M_C02|	M_SC02 },
	{ i_ror, 				0x76, AM_ZX,	2,	6,	"ror",	M_02|	M_C02|	M_SC02 },
	{ i_ror, 				0x6e, AM_A,		3,	6,	"ror",	M_02|	M_C02|	M_SC02 },
	{ i_ror, 				0x7e, AM_AX,	3,	7,	"ror",	M_02|	M_C02|	M_SC02 },

	{ i_rmb,					0x07, AM_Z,		2,	5,	"rmb0",			M_C02 },
	{ i_rmb,					0x17, AM_Z,		2,	5,	"rmb1",			M_C02 },
	{ i_rmb,					0x27, AM_Z,		2,	5,	"rmb2",			M_C02 },
	{ i_rmb,					0x37, AM_Z,		2,	5,	"rmb3",			M_C02 },
	{ i_rmb,					0x47, AM_Z,		2,	5,	"rmb4",			M_C02 },
	{ i_rmb,					0x57, AM_Z,		2,	5,	"rmb5",			M_C02 },
	{ i_rmb,					0x67, AM_Z,		2,	5,	"rmb6",			M_C02 },
	{ i_rmb,					0x77, AM_Z,		2,	5,	"rmb7",			M_C02 },

	{ i_smb,					0x87, AM_Z,		2,	5,	"smb0",			M_C02 },
	{ i_smb,					0x97, AM_Z,		2,	5,	"smb1",			M_C02 },
	{ i_smb,					0xa7, AM_Z,		2,	5,	"smb2",			M_C02 },
	{ i_smb,					0xb7, AM_Z,		2,	5,	"smb3",			M_C02 },
	{ i_smb,					0xc7, AM_Z,		2,	5,	"smb4",			M_C02 },
	{ i_smb,					0xd7, AM_Z,		2,	5,	"smb5",			M_C02 },
	{ i_smb,					0xe7, AM_Z,		2,	5,	"smb6",			M_C02 },
	{ i_smb,					0xf7, AM_Z,		2,	5,	"smb7",			M_C02 },

	{ i_trb, 				0x14, AM_Z,		2,	5,	"trb",			M_C02|	M_SC02 },
	{ i_trb, 				0x1c, AM_A,		3,	6,	"trb",			M_C02|	M_SC02 },
	{ i_tsb, 				0x04, AM_Z,		2,	5,	"tsb",			M_C02|	M_SC02 },
	{ i_tsb, 				0x0c, AM_A,		3,	3,	"tsb",			M_C02|	M_SC02 },

	{ i_bit_89, 			0x89, AM_IMM,	2,	2,	"bit",			M_C02|	M_SC02 },
	{ i_bit, 				0x24, AM_Z,		2,	3,	"bit",	M_02|	M_C02|	M_SC02 },
	{ i_bit, 				0x34, AM_ZX,	2,	4,	"bit",			M_C02|	M_SC02 },
	{ i_bit, 				0x2c, AM_A,		3,	4,	"bit",	M_02|	M_C02|	M_SC02 },
	{ i_bit, 				0x3c, AM_AX,	3,	4,	"bit",			M_C02|	M_SC02 },

	// Compare.
	{ i_cmp, 				0xc9, AM_IMM,	2,	2,	"cmp",	M_02|	M_C02|	M_SC02 },
	{ i_cmp, 				0xc5, AM_Z,		2,	3,	"cmp",	M_02|	M_C02|	M_SC02 },
	{ i_cmp, 				0xd5, AM_ZX,	2,	4,	"cmp",	M_02|	M_C02|	M_SC02 },
	{ i_cmp, 				0xd2, AM_ZI,	2,	5,	"cmp",			M_C02|	M_SC02 },
	{ i_cmp, 				0xc1, AM_ZXI,	2,	6,	"cmp",	M_02|	M_C02|	M_SC02 },
	{ i_cmp, 				0xd1, AM_ZIY,	2,	5,	"cmp",	M_02|	M_C02|	M_SC02 },
	{ i_cmp, 				0xcd, AM_A,		3,	4,	"cmp",	M_02|	M_C02|	M_SC02 },
	{ i_cmp, 				0xdd, AM_AX,	3,	4,	"cmp",	M_02|	M_C02|	M_SC02 },
	{ i_cmp, 				0xd9, AM_AY,	3,	4,	"cmp",	M_02|	M_C02|	M_SC02 },

	{ i_cpx, 				0xe0, AM_IMM,	2,	2,	"cpx",	M_02|	M_C02|	M_SC02 },
	{ i_cpx, 				0xe4, AM_Z,		2,	3,	"cpx",	M_02|	M_C02|	M_SC02 },
	{ i_cpx, 				0xec, AM_A,		3,	4,	"cpx",	M_02|	M_C02|	M_SC02 },

	{ i_cpy, 				0xc0, AM_IMM,	2,	2,	"cpy",	M_02|	M_C02|	M_SC02 },
	{ i_cpy, 				0xc4, AM_Z,		2,	3,	"cpy",	M_02|	M_C02|	M_SC02 },
	{ i_cpy, 				0xcc, AM_A,		3,	4,	"cpy",	M_02|	M_C02|	M_SC02 },

	// Instruction flow.
	{ i_bra, 				0x80, AM_REL,	2,	2,	"bra",			M_C02|	M_SC02 },
	{ i_bcc, 				0x90, AM_REL,	2,	2,	"bcc",	M_02|	M_C02|	M_SC02 },
	{ i_bcs, 				0xb0, AM_REL,	2,	2,	"bcs",	M_02|	M_C02|	M_SC02 },
	{ i_bne, 				0xd0, AM_REL,	2,	2,	"bne",	M_02|	M_C02|	M_SC02 },
	{ i_beq, 				0xf0, AM_REL,	2,	2,	"beq",	M_02|	M_C02|	M_SC02 },
	{ i_bpl, 				0x10, AM_REL,	2,	2,	"bpl",	M_02|	M_C02|	M_SC02 },
	{ i_bmi, 				0x30, AM_REL,	2,	2,	"bmi",	M_02|	M_C02|	M_SC02 },
	{ i_bvc, 				0x50, AM_REL,	2,	2,	"bvc",	M_02|	M_C02|	M_SC02 },
	{ i_bvs, 				0x70, AM_REL,	2,	2,	"bvs",	M_02|	M_C02|	M_SC02 },

	{ i_bbr,					0x0f, AM_ZREL,	5,	3,	"bbr0",			M_C02 },
	{ i_bbr,					0x1f, AM_ZREL,	5,	3,	"bbr1",			M_C02 },
	{ i_bbr,					0x2f, AM_ZREL,	5,	3,	"bbr2",			M_C02 },
	{ i_bbr,					0x3f, AM_ZREL,	5,	3,	"bbr3",			M_C02 },
	{ i_bbr,					0x4f, AM_ZREL,	5,	3,	"bbr4",			M_C02 },
	{ i_bbr,					0x5f, AM_ZREL,	5,	3,	"bbr5",			M_C02 },
	{ i_bbr,					0x6f, AM_ZREL,	5,	3,	"bbr6",			M_C02 },
	{ i_bbr,					0x7f, AM_ZREL,	5,	3,	"bbr7",			M_C02 },

	{ i_bbs,					0x8f, AM_ZREL,	5,	3,	"bbs0",			M_C02 },
	{ i_bbs,					0x9f, AM_ZREL,	5,	3,	"bbs1",			M_C02 },
	{ i_bbs,					0xaf, AM_ZREL,	5,	3,	"bbs2",			M_C02 },
	{ i_bbs,					0xbf, AM_ZREL,	5,	3,	"bbs3",			M_C02 },
	{ i_bbs,					0xcf, AM_ZREL,	5,	3,	"bbs4",			M_C02 },
	{ i_bbs,					0xdf, AM_ZREL,	5,	3,	"bbs5",			M_C02 },
	{ i_bbs,					0xef, AM_ZREL,	5,	3,	"bbs6",			M_C02 },
	{ i_bbs,					0xff, AM_ZREL,	5,	3,	"bbs7",			M_C02 },

	{ i_jmp_4c,				0x4c, AM_A,		3,	3,	"jmp",	M_02|	M_C02|	M_SC02 },
	{ i_jmp_6c_6502,		0x6c, AM_AI1,	3,	6,	"jmp",	M_02 },
	{ i_jmp_6c_65c02,		0x6c, AM_AI2,	3,	6,	"jmp",			M_C02|	M_SC02 },
	{ i_jmp_7c, 			0x7c, AM_AXI,	3,	6,	"jmp",			M_C02|	M_SC02 },

	{ i_jsr, 				0x20, AM_A,		3,	6,	"jsr",	M_02|	M_C02|	M_SC02 },
	{ i_rts, 				0x60, AM_IMP,	1,	6,	"rts",	M_02|	M_C02|	M_SC02 },

	// System.
	{ i_clc, 				0x18, AM_IMP,	1,	2,	"clc",	M_02|	M_C02|	M_SC02 },
	{ i_cld, 				0xd8, AM_IMP,	1,	2,	"cld",	M_02|	M_C02|	M_SC02 },
	{ i_cli, 				0x58, AM_IMP,	1,	2,	"cli",	M_02|	M_C02|	M_SC02 },
	{ i_clv, 				0xb8, AM_IMP,	1,	2,	"clv",	M_02|	M_C02|	M_SC02 },

	{ i_sec, 				0x38, AM_IMP,	1,	2,	"sec",	M_02|	M_C02|	M_SC02 },
	{ i_sed, 				0xf8, AM_IMP,	1,	2,	"sed",	M_02|	M_C02|	M_SC02 },
	{ i_sei, 				0x78, AM_IMP,	1,	2,	"sei",	M_02|	M_C02|	M_SC02 },

	{ i_rti, 				0x40, AM_IMP,	1,	5,	"rti",	M_02|	M_C02|	M_SC02 },
	{ i_brk_6502, 			0x00, AM_IMM,	2,	6,	"brk",	M_02 },
	{ i_brk_65c02,			0x00, AM_IMM,	2,	6,	"brk",			M_C02|	M_SC02 },
	{ i_wai, 				0xcb, AM_IMP,	1,	2,	"wai",			M_C02|	M_SC02 },
	{ i_stp, 				0xdb, AM_IMP,	1,	2,	"stp",			M_C02|	M_SC02 },

	// Sentinel.
	{ NULL, 					0x00, 0,			0,	0,	NULL,		0 }
};

/*
	static final int
		OP_ILLEGAL=		0,
		OP_NOP=			0, // EA
		
		OP_LDA_ZXI=		0, // A1
		OP_LDA_Z=		0, // A5
		OP_LDA_IMM=		0, // A9
		OP_LDA_A=		0, // AD
		OP_LDA_ZIY=		0, // B1
		OP_LDA_ZI=		0, // B2
		OP_LDA_ZX=		0, // B5
		OP_LDA_AY=		0, // B9
		OP_LDA_AX=		0, // BD
		
		OP_LDX_IMM=		0, // A2
		OP_LDX_Z=		0, // A6
		OP_LDX_A=		0, // AE
		OP_LDX_ZY=		0, // B6
		OP_LDX_AY=		0, // BE

		OP_LDY_IMM=		0, // A0
		OP_LDY_Z=		0, // A4
		OP_LDY_A=		0, // AC
		OP_LDY_ZX=		0, // B4
		OP_LDY_AX=		0, // BC
		
		OP_STA_ZXI=		0, // 81
		OP_STA_Z=		0, // 85
		OP_STA_A=		0, // 8D
		OP_STA_ZIY=		0, // 91
		OP_STA_ZI=		0, // 92
		OP_STA_ZX=		0, // 95
		OP_STA_AY=		0, // 99
		OP_STA_AX=		0, // 9D

		OP_STX_Z=		0, // 86
		OP_STX_A=		0, // 8E
		OP_STX_ZY=		0, // 96
		
		OP_STY_Z=		0, // 84
		OP_STY_A=		0, // 8C
		OP_STY_ZX=		0, // 94
	
		OP_STZ_Z=		0, // 64
		OP_STZ_ZX=		0, // 74
		OP_STZ_A=		0, // 9C
		OP_STZ_AX=		0, // 9E
*/	
