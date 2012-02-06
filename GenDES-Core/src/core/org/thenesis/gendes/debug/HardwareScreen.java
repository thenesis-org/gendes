package org.thenesis.gendes.debug;


public class HardwareScreen extends Screen {
    // Colors.
    public static final byte
    	COLOR_BG=                  0, // Background.
    	COLOR_FG=                  1, // Foreground.
    	COLOR_CURSOR_LINE_BG=      2, // Cursor background.
    	COLOR_CURSOR_LINE_FG=      3, // Cursor foreground.
    	COLOR_CURSOR_BG=           4, // Cursor background.
    	COLOR_CURSOR_FG=           5, // Cursor foreground.
    	COLOR_CURSOR2_BG=          6, // Cursor background when the field is selected.
    	COLOR_CURSOR2_FG=          7, // Cursor foreground when the field is selected.
    	COLOR_EDIT_BG=             8, // Background in edition mode.
    	COLOR_EDIT_FG=             9, // Foreground in edition mode.
    	COLOR_CURSOR_LINE_EDIT_BG= 10, // Cursor background.
    	COLOR_CURSOR_LINE_EDIT_FG= 11, // Cursor foreground.
    	COLOR_CURSOR_EDIT_BG=      12, // Cursor background in edition mode.
    	COLOR_CURSOR_EDIT_FG=      13, // Cursor foreground in edition mode.
    	COLOR_CURSOR2_EDIT_BG=     14, // Cursor background when the field is selected in edition mode.
    	COLOR_CURSOR2_EDIT_FG=     15; // Cursor foreground when the field is selected in edition mode.
    
    // Line list type.
    protected static final int
    	TYPE_GROUP=			0,
    	TYPE_REGISTER=		1,
    	TYPE_FIELD=			2;

    protected static class Item {
    	public Item parent, previous, next, children;
    	public int childrenNb;

    	public int type;
    	public String name;
    	public boolean expanded;
    	
    	protected Item(int type, String name) {
    		this.type=type;
    		this.name=name;
    	}
    }

    protected static class Group extends Item {
    	public Group(String name) {
    		super(TYPE_GROUP, name);
    	}
    }
    
    protected static class Register extends Item {
    	public int address, length;
    	public String valueString[];
    	public int value;
    	
    	public Register(String name, int address, int length) {
    		super(TYPE_REGISTER, name);
    		this.address=address; this.length=length;
    	}

    	public Register(String name, int address, int length, String vs[]) {
    		super(TYPE_REGISTER, name);
    		this.address=address; this.length=length;
    		this.valueString=vs;
    	}
    }
    
    protected static class Bitfield extends Item {
    	public int bitStart, bitLength;
    	public String valueString[];
    	public int value;
    	
    	public Bitfield(String name, int bitStart, int bitLength) {
    		super(TYPE_FIELD, name);
    		this.bitStart=bitStart; this.bitLength=bitLength;
    	}
    	
    	public Bitfield(String name, int bitStart, int bitLength, String vs[]) {
    		super(TYPE_FIELD, name);
    		this.bitStart=bitStart; this.bitLength=bitLength;
    		this.valueString=vs;
    	}
    }
    
    // Fields.
    private static final int
    	FIELD_NAME=         0,
    	FIELD_ADDRESS=      1,
    	FIELD_VALUE=        2,
    	FIELD_VALUE_NAME=   3,
		FIELD_NB=        	4;
     
    private boolean insertionModeFlag; // True if insertion mode is activated. 
    private int nameLength; // Number of characters for the name field.
    private int addressLength; // Number of characters for the address field.
    private boolean autoResize; // True to adapt the screen to the dimensions of the screen.
    private boolean dirtyFlag; // True if the screen needs to be updated.    
    private final int screenFields[][]=new int[FIELD_NB][2]; // Dimensions of each field.
    private int visibleLength;
    
    private Item items[];
    private Item root=new Group("Root");
    
    private int lineListCapacity, lineListLength;
    private Item lineList[];
    private int lineListPosition;

    private int cursorLine;

    public HardwareScreen() {
        super();
        nameLength=8;
        addressLength=4;
        autoResize=true;
        dirtyFlag=true;
    }

    //--------------------------------------------------------------------------------
    // Items.
    //--------------------------------------------------------------------------------
    protected final void setItems(Item it[]) {
    	items=it;
    	int capacity=0;
    	for (int i=0; i<it.length; i++) {
    		if (it[i]!=null) capacity++;
    	}
    	lineListCapacity=capacity;
    	lineListPosition=0;
    	lineList=new Item[capacity];
    	
    	buildTree();
    	updateLineList();
    }
    
    private void buildTree() {
    	int level=0;
    	
    	// Setup the hierarchy.
		root.parent=null;
		root.previous=null;
		root.next=null;
		root.children=null;
		root.childrenNb=0;
		root.expanded=true;

		Item parent=root;
    	Item item=null;
    	for (int itemIndex=0; itemIndex<items.length; itemIndex++) {
        	item=items[itemIndex];
    		if (item==null) {
    			if (parent.parent!=null) {
    				parent=parent.parent;
    				level--;
    			}
    		} else {
        		item.parent=parent;
        		item.children=null;
        		item.childrenNb=0;

        		parent.childrenNb++;
    			if (parent.children!=null) {
    				item.previous=parent.children.previous;
    				item.next=parent.children;
    				parent.children.previous.next=item;
    				parent.children.previous=item;
    			} else {
    				parent.children=item;
    				item.previous=item;
    				item.next=item;
    			}

    			parent=item;

    			//for (int i=0; i<level; i++) System.out.print(" ");
    			//System.out.println(item.name);
    			level++;
    		}
    	}
    }

    private Item nextPreOrderItem(Item top, Item current, boolean skipCurrentChildren) {
    	if (root==null) return null;

    	Item item;

    	// If current==null, find the first item that is the top.
    	if (current==null) {
    		if (top==null) item=root;
    		else item=top;
    		return item;
    	}

    	// If it has a child, process it.
    	if (!skipCurrentChildren) {
	    	item=current.children;
	    	if (item!=null) return item;
    	}

    	while (true) {
    		// If it has a sibling, process it.
    		if (!isAtTail(current)) {
    			item=current.next;
    			break;
    		}

    		// If we have reached the root (or the top), we have completely traversed the tree (or the subtree).
    		item=current.parent;
    		if (item==top) {
    			item=null;
    			break;
    		}

    		current=item;
    	}

    	return item;
    }
    
    private boolean isAtTail(Item item) {
    	Item parent=item.parent;
    	if (parent==null) return (item.next==root);
    	else return (item.next==parent.children);
    }
    
    // Build the list of visible items.
    private void updateLineList() {
    	Item item=root;
    	lineListLength=0;
    	while (true) {
    		item=nextPreOrderItem(root, item, !item.expanded);
    		if (item==null) break;
    		lineList[lineListLength]=item;
    		lineListLength++;
    	}
    }
    
    //--------------------------------------------------------------------------------
    // Edition.
    //--------------------------------------------------------------------------------
    public final void activateInsertionMode(boolean flag) {
        insertionModeFlag=flag;
    }
    
    public final boolean isInsertionModeActivated() {
        return insertionModeFlag;
    }
    
    
    /**
     * Inserts a key.
     */
    public final void insertKey(char c) {
        if (!insertionModeFlag) return;
/*        
        int digit=0;
        if ((c>='0') && (c<='9')) digit=c-'0';
        else if ((c>='a') && (c<='f')) digit=10+(c-'a');
        else if ((c>='A') && (c<='F')) digit=10+(c-'A');
        else return;
        int data=read8(cursorAddress);
        data=(cursorAddressNibble==0) ? (data&0x0f)|(digit<<4) : (data&0xf0)|digit;
        write8(cursorAddress, data);
        moveCursorRight();
*/
    }
    
    /**
     * Checks if the cursor is visible.
     */
    public final boolean isCursorVisible() {
    	return cursorLine>=lineListPosition && cursorLine<(lineListPosition+visibleLength);
    }

    /**
     * Resets the cursor.
     */
    public final void resetCursor() {
    }
    
    /**
     * Sets the cursor position.
     */
    public final void setCursorPosition(int x, int y) {
        dirtyFlag=true;
    	setCursorLine(y);
        if (!isCursorVisible()) {
        	lineListPosition=cursorLine;
        }
    }

    /**
     * Moves the cursor up.
     */
    public final void moveCursorUp() {
        dirtyFlag=true;
        setCursorLine(cursorLine-1);
        if (!isCursorVisible()) {
        	lineListPosition=cursorLine;
        }
    }
    
    /**
     * Moves the cursor down.
     */
    public final void moveCursorDown() {
        dirtyFlag=true;        
        setCursorLine(cursorLine+1);
        if (!isCursorVisible()) {
        	lineListPosition=cursorLine;
        }
    }
    
    /**
     * Moves the cursor left.
     */
    public final void moveCursorLeft() {
        dirtyFlag=true;        
    }
    
    /**
     * Moves the cursor right.
     */
    public final void moveCursorRight() {
        dirtyFlag=true;
    }
    
    /**
     * Moves the cursor to the previous page.
     */
    public final void moveCursorToPreviousPage() {
        dirtyFlag=true;
    }
    
    /**
     * Moves the cursor to the next page.
     */
    public final void moveCursorToNextPage() {
        dirtyFlag=true;        
    }
    
    /**
     * Moves the cursor to the start of the line.
     */
    public final void moveCursorToStartOfLine() {
        dirtyFlag=true;
    }
    
    /**
     * Moves the cursor to the end of the line.
     */
    public final void moveCursorToEndOfLine() {
        dirtyFlag=true;
    }

    private final void setCursorLine(int y) {
    	if (y>=visibleLength) y=visibleLength-1;
    	if (y<0) y=0;
    	cursorLine=y;
    }

    //--------------------------------------------------------------------------------
    // Display.
    //--------------------------------------------------------------------------------
    public final void setScreenSize(int w, int h) {
        super.setScreenSize(w, h);
        visibleLength=screenHeight;
        if (autoResize) {
        	nameLength=computeNameLength(w, addressLength);
        }
        updateFields();
        dirtyFlag=true;
    }
    
    public final void setAutoResize(boolean flag) {
        autoResize=flag;
    }
    
    public final boolean getAutoResize() {
        return autoResize;
    }
    
    public final void setNameLength(int nl) {
        if (nl<0) return;
    	nameLength=nl;
        updateFields();
        dirtyFlag=true;
    }
    
    public final int getNameLength() {
        return addressLength;
    }
        
    public final void setAddressLength(int as) {
        if (as<0 || as>8) return;
        addressLength=as;
        updateFields();
        dirtyFlag=true;
    }
    
    public final int getAddressLength() {
        return addressLength;
    }
    
    private final void updateFields() {
    	int s[][]=screenFields; // For readability.
        s[FIELD_NAME][0]=0;
        s[FIELD_NAME][1]=nameLength+3;
        s[FIELD_ADDRESS][0]=s[FIELD_NAME][0]+s[FIELD_NAME][1]+2;
        s[FIELD_ADDRESS][1]=addressLength<8 ? 8 : addressLength;
        s[FIELD_VALUE][0]=s[FIELD_ADDRESS][0]+s[FIELD_ADDRESS][1]+3;
        s[FIELD_VALUE][1]=8;
    }

    public static int computeNameLength(int lineLength, int addressLength) {
    	// -Tree button: 3 ('+', '.+', '...')
    	// -Separator: 2 ("| ")
    	// -Address: addressLength
    	// -Separator: 3 (" | ")
    	// -Value: 8 ("12345678")
    	if (addressLength<8) addressLength=8;
    	int nl=lineLength-3-2-addressLength-3-8;
    	return nl<8 ? 8 : nl; 
    }
    
    /**
     * Returns the field that corresponds to a given character position.
     */
    public final int positionToField(int x, int y) {
        if (x<0 || x>=screenWidth || y<0 || y>=screenHeight) return -1;
        for (int i=0; i<FIELD_NB; i++) if (x>=screenFields[i][0] && x<(screenFields[i][0]+screenFields[i][1])) return i;
        return -1;
    }

    /**
     * Updates the screen.
     */
    public final void updateScreen(boolean forceUpdate) {
        if (screenChar==null || (!dirtyFlag && !forceUpdate)) return;
        updateLineList();
        
        int y;
        for (y=0; y<screenHeight; y++) {
        	int line=lineListPosition+y;
        	if (line>=lineListLength) break;
        	Item item=lineList[line];
        	Group group;
        	Register register;
        	Bitfield bitfield;
        	
            // Highlight the line of the cursor.
            {
                int cl=0;
                if (cursorLine==line) cl+=1; if (insertionModeFlag) cl+=2;
                currentBackgroundColor=COLORS_L_BG[cl]; currentForegroundColor=COLORS_L_FG[cl];
                currentChar=' ';
                clearLine(y);
            }
            
            // Name.
            char c;
        	string.clear();
        	if (item.childrenNb>0) c=item.expanded ? '-' : '+'; else c='.';
            switch (item.type) {
            case TYPE_GROUP:
            	group=(Group)item;
            	string.append(c);
            	string.append(item.name, 0, nameLength);
            	print(screenFields[FIELD_NAME][0], y);
            	break;
            case TYPE_REGISTER:
            	register=(Register)item;
            	string.append(" ");
            	string.append(c);
            	string.append(item.name, 0, nameLength);
            	print(screenFields[FIELD_NAME][0], y);
                print(screenFields[FIELD_ADDRESS][0], y, register.address, addressLength<<2);
                print(screenFields[FIELD_VALUE][0], y, register.value, register.length);
            	break;
            case TYPE_FIELD:
            	bitfield=(Bitfield)item;
            	string.append("  ");
            	string.append(c);
            	string.append(item.name, 0, nameLength);
            	print(screenFields[FIELD_NAME][0], y);
                print(screenFields[FIELD_ADDRESS][0], y, bitfield.bitStart+"-"+(bitfield.bitStart+bitfield.bitLength));
                print(screenFields[FIELD_VALUE][0], y, bitfield.value, bitfield.bitLength);
            	break;
            }
            print(screenFields[FIELD_NAME][0]+screenFields[FIELD_NAME][1], y, '|'); // Separator.
            print(screenFields[FIELD_ADDRESS][0]+screenFields[FIELD_ADDRESS][1]+1, y, '|'); // Separator.
        }
    }    
    
    private static final byte COLORS_L_BG[]={ COLOR_BG, COLOR_CURSOR_LINE_BG, COLOR_EDIT_BG, COLOR_CURSOR_LINE_EDIT_BG };
    private static final byte COLORS_L_FG[]={ COLOR_FG, COLOR_CURSOR_LINE_FG, COLOR_EDIT_FG, COLOR_CURSOR_LINE_EDIT_FG };
    private static final byte COLORS_C_BG[]={ COLOR_CURSOR_BG, COLOR_CURSOR2_BG, COLOR_CURSOR_EDIT_BG, COLOR_CURSOR2_EDIT_BG };
    private static final byte COLORS_C_FG[]={ COLOR_CURSOR_FG, COLOR_CURSOR2_FG, COLOR_CURSOR_EDIT_FG, COLOR_CURSOR2_EDIT_FG };

}
