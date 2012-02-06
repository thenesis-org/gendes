package org.thenesis.gendes.debug;


import org.thenesis.gendes.FastString;

public abstract class Assembler {
	// Type of parsed item.
	public static final int
		ITEM_END=		0,
		ITEM_ERROR=		1,
		ITEM_LABEL=		2,
		ITEM_OPCODE=	3;
	
	// Item type.
	public int itemType;
    // Last position.
    public int instructionStringIndex;
    // Error position.
    public int errorStartIndex, errorEndIndex;
    // Error string.
    public String errorString;
    // Identifier name for label.
    public char identifier[]=new char[256];
    public int identifierLength;

    protected FastString instructionString;
    
    protected static final int MAX_TOKEN_LENGTH=255;
    protected boolean tokenUngetFlag=false;
    protected int tokenId;
    protected int tokenStartIndex, tokenEndIndex;
    protected long tokenValue;
    protected char tokenString[]=new char[256];
    protected int tokenStringLength;
    
    private static final int EXPRESSION_MAX_LENGTH=256;
    protected long expressionValue;
    private ExpressionNode expressionTree;
    private static class ExpressionNode {
    	public ExpressionNode parent;
    	public final ExpressionNode children[]=new ExpressionNode[4];
    	public int childrenNb;
    	public int type;
    	public long value;
    }
    private static final int
		NODE_EXPRESSION=0,
    	NODE_MINUS=1,
    	NODE_PLUS=2,
    	NODE_ADD=3,
    	NODE_SUB=4,
    	NODE_MUL=5,
    	NODE_DIV=6,
    	NODE_NOT=7,
    	NODE_AND=8,
    	NODE_OR=9,
    	NODE_XOR=10;
    
    public Assembler() {}
    
    public final void reset(FastString string, int index) {
    	if (string.buffer==null || index<0 || index>string.length) {
            instructionString=null;
            instructionStringIndex=0;    		
    	} else {
	        instructionString=string;
	        instructionStringIndex=index;
    	}
    	tokenUngetFlag=false;
    }
    
    public final int nextItem() {
    	if (instructionString==null) return ITEM_END;
    	
    	errorStartIndex=-1; errorEndIndex=-1;
        errorString="";
        
        skipWhitespaces();
        int tk=getToken();
        
        switch (tk) {
        default:
        	itemType=ITEM_ERROR;
        	break;
        case TOKEN_EOS:
        	itemType=ITEM_END;
        	break;
        case TOKEN_IDENTIFIER:
        	for (int i=0; i<tokenStringLength; i++) identifier[i]=tokenString[i];
        	identifierLength=tokenStringLength;
        	
        	tk=getToken();
        	if (tk==':') itemType=ITEM_LABEL;
        	else {
        		ungetToken();
        		itemType=parseInstruction();
        	}
        	break;
        }
        
    	return itemType;
    }

    //--------------------------------------------------------------------------------
    // Parsing.
    //--------------------------------------------------------------------------------
    protected abstract int parseInstruction();
    
    protected final boolean parseExpression() {
    	expressionTree=null;
    	while (true) {
    		skipWhitespaces();
    		int tk=getToken();
    		switch (tk) {
    		case TOKEN_EOS:
//    			if (expressionTree==null || )
    			break;
    		case TOKEN_IDENTIFIER:
    		}
    		break;
    	}
    	return false;
    }
    
    protected final void skipWhitespaces() {
    	while (getToken()==TOKEN_SPACE) ;
    	ungetToken();
    	while (instructionString.buffer[instructionStringIndex]==' ') instructionStringIndex++;
    }
    
    //--------------------------------------------------------------------------------
    // Token.
    //--------------------------------------------------------------------------------
    protected static final int
		TOKEN_EOS=			0,
		TOKEN_ERROR=		-1,
		TOKEN_SPACE=		-2,
		TOKEN_NEWLINE=		-3,
		TOKEN_IDENTIFIER=	-4,
		TOKEN_NUMBER=		-5;
    
    protected final int getToken() {
    	if (tokenUngetFlag) { tokenUngetFlag=false; return tokenId; }
    	
        tokenStartIndex=instructionStringIndex;
        
    	char c=getChar();
    	switch (c) {
    	default:
    		tokenId=c;
    		nextChar();
    		break;
    	case 0:
    		tokenId=TOKEN_EOS;
    		break;
    	case ' ':
    		do { c=nextChar(); } while (c==' ');
    		tokenId=TOKEN_SPACE;
    		break;
    	case '\n':
    		c=nextChar();
    		tokenId=TOKEN_NEWLINE;
    		break;
    	case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
    		tokenValue=0;
    		do {
    			tokenValue=(tokenValue*10)+(c-'0');
        		c=nextChar();
    		} while (c>='0' && c<='9');
			tokenId=TOKEN_NUMBER;    		
    		break;
    	case '$':
    		c=nextChar();
    		int v=hexadecimalDigit(c);
    		if (v<0) {
    			errorString="Invalid hexadecimal number. At least one hexadecimal digit is required.";
    			tokenId=TOKEN_ERROR;
    		} else {
	    		do {
	    			tokenValue=(tokenValue*16)+v;
	        		c=nextChar();
	    			v=hexadecimalDigit(c);
	    		} while (v>=0);
				tokenId=TOKEN_NUMBER;
    		}
    		break;
    	case '_':
    	case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm':
    	case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
    	case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K': case 'L': case 'M':
    	case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
    		tokenStringLength=0;
    		do {
        		tokenString[tokenStringLength++]=c;
        		c=nextChar();
    		} while (c=='_' || (c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9') || tokenStringLength<MAX_TOKEN_LENGTH);
    		tokenString[tokenStringLength]=0;
			tokenId=TOKEN_IDENTIFIER;
    		break;
    	case '/':
    		c=nextChar();
    		switch (c) {
    		default:
    			tokenId='/';
    			break;
    		case '/': // Single line comment.
    			do { c=nextChar(); } while (c!='\n' && c!=0);
    			tokenId=TOKEN_SPACE;
    			break;
    		case '*': // Multi line comment.
    			do {
    				c=nextChar();
    				if (c=='*') {
    					c=nextChar();
    					if (c=='/') break;
    				}
    			} while (c!='\n' && c!=0);
    			tokenId=TOKEN_SPACE;
    			break;
    		}
    		break;
    	}

    	tokenEndIndex=instructionStringIndex;
    	return tokenId;
	}

    protected final void ungetToken() {
    	tokenUngetFlag=true;
    }
    
    //--------------------------------------------------------------------------------
    // Character.
    //--------------------------------------------------------------------------------
    protected final char getChar() {
    	if (instructionStringIndex>=instructionString.length) return 0;
    	return instructionString.buffer[instructionStringIndex];
    }
    
    protected final char nextChar() {
    	if (instructionStringIndex>=instructionString.length) return 0;
    	return instructionString.buffer[++instructionStringIndex];
    }

    protected final int hexadecimalDigit(char c) {
    	if (c>='0' && c<='9') return c-'0';
    	if (c>='a' && c<='f') return c-'a'+10;
    	if (c>='A' && c<='F') return c-'A'+10;
    	return -1;
    }    
}
