package org.thenesis.gendes.cpu_gameboy;

public class Assembler extends org.thenesis.gendes.debug.Assembler  implements InstructionSet {
    public int instructionAddress;
    // Length of the instruction.
    public int instructionLength;
    // Opcode bytes.
    public final int opcodeBytes[]=new int[4];

    
    private String opcodeTableStrings[];
    private int opcodeTableValue[];
    private String amTableTokens[][];
    private int amTableValue[];
    private int instructionTable[];  
    
    public void setAddress(int address) {
    	instructionAddress=address;
    }
    
    protected int parseInstruction() {
    	return ITEM_OPCODE;
    }

}


/*
if (c==':') {
	tokenId=TOKEN_LABEL;
	index++;
} else {
	for (int i=0; i<REGISTER_STRINGS.length; i++) {
		char s[]=REGISTER_STRINGS[i];
		int sl=s.length;
		if (sl!=tokenStringLength) continue;
		boolean flag=true;
		for (int k=0; k<sl; k++) if (s[k]!=tokenString[k]) { flag=false; break; }
		if (flag) { tokenId=REGISTER_TOKENS[i]; break; }
	}
}
*/    	
/*
TOKEN_A=			-8,
TOKEN_B=			-9,
TOKEN_C=			-10,
TOKEN_D=			-12,
TOKEN_E=			-13,
TOKEN_F=			-14,
TOKEN_H=			-15,
TOKEN_L=			-16,
TOKEN_BC=			-17,
TOKEN_DE=			-18,
TOKEN_HL=			-19,
TOKEN_SP=			-20,
TOKEN_AF=			-21;

private static final char REGISTER_STRINGS[][]={
{ 'A' }, { 'B' }, { 'C' }, { 'D' }, { 'E' }, { 'F' }, { 'H' }, { 'L' },
{ 'B', 'C' }, { 'D', 'E'}, { 'H', 'L'}, { 'S', 'P' }, { 'A', 'F'},
};

private static final int REGISTER_TOKENS[]={
TOKEN_A, TOKEN_B, TOKEN_C, TOKEN_D, TOKEN_E, TOKEN_F, TOKEN_H, TOKEN_L,
TOKEN_BC, TOKEN_DE, TOKEN_HL, TOKEN_SP, TOKEN_AF
};
*/

/*
private int parseOpcode() {
	return I_NOP;
}

private int parseAM() {
	return AM_A;
}

private int findInstruction(int opcode, int srcAm, int dstAm) {
	return 0;
}

private void buildInstruction(int k) {
	
}

private void buildTables() {
	// Sort instructions strings.
	
}

private int compareInstruction(int k0[], int k1[]) {
	if (k0[IT_ID]<k1[IT_ID]) return -1;
	if (k0[IT_ID]>k1[IT_ID]) return 1;
	if (k0[IT_DEBUG_DST_AM]<k1[IT_DEBUG_DST_AM]) return -1;
	if (k0[IT_DEBUG_DST_AM]>k1[IT_DEBUG_DST_AM]) return 1;
	if (k0[IT_DEBUG_SRC_AM]<k1[IT_DEBUG_SRC_AM]) return -1;
	if (k0[IT_DEBUG_SRC_AM]>k1[IT_DEBUG_SRC_AM]) return 1;
	return 0;
}
*/

