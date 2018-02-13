package org.thenesis.emulation.eclipse.gameboy;


import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.debug.MemoryScreen;
import org.thenesis.gendes.gameboy.GameBoy;

public class MemoryView extends ViewPart {
	
	public MemoryView() {
        palette[0]=0x00ffffff; palette[1]=0x00000000; palette[2]=0x00e0ffe0; palette[3]=0x00000000; palette[4]=0x0000ff00; palette[5]=0x00000000; palette[6]=0x00000000; palette[7]=0x0000ff00;
        palette[8]=0x00ffffff; palette[9]=0x00000000; palette[10]=0x00ffe0e0; palette[11]=0x00000000; palette[12]=0x00ff0000; palette[13]=0x00000000; palette[14]=0x00000000; palette[15]=0x00ff0000;
	}

	//--------------------------------------------------------------------------------
	// ViewPart.
	//--------------------------------------------------------------------------------
	Composite parentComposite;
	Combo addressSpaceCombo;
	Combo addressCombo;
	Button addressButton;
	ScreenComposite screenComposite;

	public void createPartControl(Composite parent) {
        parentComposite=parent;

        GridLayout layout=new GridLayout(3, false);
        GridData data;
        Label label;
        
        parent.setLayout(layout);

        label=new Label(parent, SWT.NONE);
        label.setText("Address space:");
		data=new GridData(SWT.FILL, SWT.FILL, false, false);
		label.setLayoutData(data);
		addressSpaceCombo=new Combo(parent, SWT.READ_ONLY);
		addressSpaceCombo.add("CPU", GameBoy.ADDRESS_SPACE_CPU);
		addressSpaceCombo.add("WRAM", GameBoy.ADDRESS_SPACE_WRAM);
		addressSpaceCombo.add("VRAM", GameBoy.ADDRESS_SPACE_VRAM);
		addressSpaceCombo.add("CROM", GameBoy.ADDRESS_SPACE_CROM);
		addressSpaceCombo.add("CRAM", GameBoy.ADDRESS_SPACE_CRAM);
		addressSpaceCombo.select(0);
		data=new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		addressSpaceCombo.setLayoutData(data);

        label=new Label(parent, SWT.NONE);
        label.setText("Go to address:");
		data=new GridData(SWT.FILL, SWT.FILL, false, false);
		label.setLayoutData(data);
		addressCombo=new Combo(parent, SWT.DROP_DOWN);
		addressCombo.addKeyListener(addressComboKeyListener);
		data=new GridData(SWT.FILL, SWT.FILL, true, false);
		addressCombo.setLayoutData(data);
		addressButton=new Button(parent, SWT.NONE);
		addressButton.setText("Go");
		addressButton.addSelectionListener(addressButtonSelectionListener);
		data=new GridData(SWT.FILL, SWT.FILL, false, false);
		addressButton.setLayoutData(data);

		screenComposite=new ScreenComposite(parent, SWT.NONE);
		data=new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		screenComposite.setLayoutData(data);

        GameBoyEmulationContext gbc=GameBoyPlugin.getDefault().getGameBoyContext();
        attach(gbc);
	}

    public void dispose() {
    	detach();
        super.dispose();
    }

	public void setFocus() {
		screenComposite.canvas.requestFocus();
	}

	private KeyListener addressComboKeyListener=new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
			if (e.character=='\r') {
				gotoAddress(addressCombo.getText());				
			}
		}
	};
	
	private SelectionListener addressButtonSelectionListener=new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e)  {
			gotoAddress(addressCombo.getText());
		}
	};
	
	private void gotoAddress(String s) {
		int address=0;
		
		try {
			address=Integer.parseInt(s, 16);
		} catch (NumberFormatException ex) {
			return;
		}

		synchronized (systemContext.emulationSystem) {
    		memoryScreen.goTo(address);
    	}
    	
    	// Find if the string is already present in the combo.
    	int n=addressCombo.getItemCount();
    	int found=-1;
    	for (int i=0; i<n; i++) {
    		String s2=addressCombo.getItem(i);
    		if (s2.equals(s)) { found=i; break; }
    	}
    	if (found>=0) {
    		addressCombo.remove(found);
    	}
    	if (n>=16) addressCombo.remove(n-1);
    	addressCombo.add(s, 0);

    	screenComposite.redrawScreen();
	}
	
	//--------------------------------------------------------------------------------
	// Context nodes.
	//--------------------------------------------------------------------------------
	boolean attachedFlag=false;
    SystemEmulationContext systemContext;
    GameBoyEmulationContext gameBoyContext;    
	
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { MemoryView.this.detach(); }
    };

    private void attach(GameBoyEmulationContext gbc) {
        detach();
        
        systemContext=gbc.systemContext; gameBoyContext=gbc;
    	synchronized (systemContext.emulationSystem) {	                    
	        systemContext.onPauseDispatcher.addListener(onPauseListener);
	        gameBoyContext.add(gameBoyNode);
	        gameBoyContext.onResetDispatcher.addListener(onResetListener);
    	}

        attachedFlag=true;
        
        screenComposite.redrawScreen();
    }

    private void detach() {
    	if (!attachedFlag) return;
        
    	synchronized (systemContext.emulationSystem) {
	        systemContext.onPauseDispatcher.removeListener(onPauseListener);
	        gameBoyContext.onResetDispatcher.removeListener(onResetListener);
	        gameBoyContext.remove(gameBoyNode);
    	}
        systemContext=null; gameBoyContext=null;
    	
        attachedFlag=false;
        
        screenComposite.redrawScreen();
    }

	//--------------------------------------------------------------------------------
    // Context listeners.
	//--------------------------------------------------------------------------------
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) {
            screenComposite.redrawScreen();
        }
    };
    
    private final ContextEventListener onResetListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) {
            screenComposite.redrawScreen();
        }
    };
    
	//--------------------------------------------------------------------------------
    // Memory screen.
	//--------------------------------------------------------------------------------
    private final int palette[]=new int[256];

    private final MemoryScreen memoryScreen=new MemoryScreen() {
        public int read8(int address) { return gameBoyContext.gameBoy.cpu.cpuRead8(address);  }
        public void write8(int address, int data) { gameBoyContext.gameBoy.cpu.cpuWrite8(address, data); }
    };
    
	//--------------------------------------------------------------------------------
    // Screen composite.
	//--------------------------------------------------------------------------------    
    class ScreenComposite extends Composite {
        java.awt.Frame frame;
        ScreenCanvas canvas;

        ScreenComposite(Composite parent, int style) {
    		super(parent, style | SWT.NO_BACKGROUND | SWT.EMBEDDED);
    		
            frame=SWT_AWT.new_Frame(this);
            canvas=new ScreenCanvas();
            frame.add(canvas);
    		
    		addDisposeListener(disposeListener);
    		addFocusListener(focusListener);
    	}

    	public Point computeSize(int wHint, int hHint, boolean changed) {
    		int width=memoryScreen.getScreenPixelsWidth(), height=memoryScreen.getScreenPixelsHeight();
    		return new Point(width+2, height+2);
    	}

    	public void redrawScreen() {
    		canvas.screenDirty=true;
    		canvas.repaint();
    	}
    	
    	private void widgetDisposed(DisposeEvent e) {
    		frame.dispose();
    		frame=null;
    		canvas=null;
    	}
    	
    	private void focusGained(FocusEvent e) {
    		canvas.requestFocus();
    	}
    	
    	private final DisposeListener disposeListener=new DisposeListener() {
    		public void widgetDisposed(DisposeEvent e) { ScreenComposite.this.widgetDisposed(e); }
    	};

        private final FocusListener focusListener=new FocusAdapter() {
        	public void focusGained(FocusEvent e) { ScreenComposite.this.focusGained(e); }
        };        
    }
    
	//--------------------------------------------------------------------------------
    // Screen canvas.
	//--------------------------------------------------------------------------------
    // We use an AWT canvas because SWT image operations are very slow. 
    // DO NOT call a SWT routine inside an AWT routine !!!
    class ScreenCanvas extends java.awt.Canvas {
		private boolean backgroundDirty=true, screenDirty=true, foregroundDirty=true;
        private final org.thenesis.gendes.debug.Point mouseCharacter=new org.thenesis.gendes.debug.Point();        
    	protected BufferedImage screenImg;

        ScreenCanvas() {
        	super();

            addComponentListener(componentListener);
            addFocusListener(focusListener);
            addKeyListener(keyListener);
            addMouseListener(mouseListener);
            addMouseWheelListener(mouseWheelListener);
        }
        
    	public void paint(java.awt.Graphics g) {
            drawBackground(g);
            drawScreen(g);
        	drawForeground(g);
            backgroundDirty=false;
        	screenDirty=false;
            foregroundDirty=false;                	
        }
        
        public void update(java.awt.Graphics g) {
            screenDirty=screenDirty || backgroundDirty;
            foregroundDirty=foregroundDirty || screenDirty || backgroundDirty;
        	
            if (backgroundDirty) {
                drawBackground(g);
                backgroundDirty=false;
            }

            if (screenDirty) {
            	drawScreen(g);
            	screenDirty=false;
            }

            if (foregroundDirty) {
            	drawForeground(g);
                foregroundDirty=false;                	
            }
        }

        private void drawBackground(java.awt.Graphics g) {
        	g.setColor(java.awt.Color.WHITE);
        	g.fillRect(1, 1, getWidth()-1, getHeight()-1);
        }
        
        private void drawScreen(java.awt.Graphics g) {
            if (!attachedFlag) return;

            boolean updateScreenImgFlag=false;
            if (screenImg==null) { updateScreenImgFlag=true; resizeScreen(); }
            if (memoryScreen.isDirty()) {
            	updateScreenImgFlag=true;
            	synchronized (systemContext.emulationSystem) {
            		memoryScreen.updateScreen(true);
            	}
            }
            if (updateScreenImgFlag) {
                DataBufferInt dataBuffer=(DataBufferInt)screenImg.getRaster().getDataBuffer();
                memoryScreen.drawScreen(dataBuffer.getData(), screenImg.getWidth(), screenImg.getHeight(), palette);
            }

            g.drawImage(screenImg, 1, 1, this);
        }
        
        private void drawForeground(java.awt.Graphics g) {
        	g.setColor(this.hasFocus() ? java.awt.Color.BLUE : java.awt.Color.LIGHT_GRAY);
        	g.drawRect(0, 0, getWidth()-1, getHeight()-1);
        }

        private void resizeScreen() {
            if (!attachedFlag) return;

            int fw=memoryScreen.getFontWidth(), fh=memoryScreen.getFontHeight(), w=getWidth(), h=getHeight();
            int sw=w/fw, sh=h/fh;
            if (sw<1) sw=1; if (sh<1) sh=1;
            memoryScreen.setScreenSize(sw, sh);
            
            int imgWidth=memoryScreen.getScreenPixelsWidth(), imgHeight=memoryScreen.getScreenPixelsHeight();
            if (imgWidth<=0) imgWidth=1; if (imgHeight<=0) imgHeight=1;
            screenImg=new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        }
        
        private boolean positionToCharacter(int x, int y, org.thenesis.gendes.debug.Point c) {
            int cx=x/memoryScreen.getFontWidth(), cy=y/memoryScreen.getFontHeight();
            if (memoryScreen.isCharacterVisible(cx, cy)) {
                c.x=cx; c.y=cy;
                return false;
            } else return true;
        }

        private final java.awt.event.ComponentListener componentListener=new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
            	resizeScreen();
            	backgroundDirty=screenDirty=foregroundDirty=true; repaint();
            }
        };

        private final java.awt.event.FocusListener focusListener=new java.awt.event.FocusListener() {
            public void focusGained(java.awt.event.FocusEvent e) {
            	foregroundDirty=true; repaint();
            }
            public void focusLost(java.awt.event.FocusEvent e) {
            	foregroundDirty=true; repaint();
            }        
        };
        
        private final java.awt.event.KeyListener keyListener=new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (!attachedFlag) return;
                
                char key=e.getKeyChar();
                switch (key) {
                case '=':
                	synchronized (systemContext.emulationSystem) {
                        memoryScreen.goTo(gameBoyContext.gameBoy.cpu.rPC);
                        if (memoryScreen.isInsertionModeActivated()) memoryScreen.resetCursor();
                	}
                	screenDirty=true; repaint(); break;
                default:
                	synchronized (systemContext.emulationSystem) {
                		memoryScreen.insertKey(key);
                	}
            		screenDirty=true; repaint(); break;
                }
            }
            
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (!attachedFlag) return;
                
                int key=e.getKeyCode();
                switch (key) {
                case java.awt.event.KeyEvent.VK_ALT:
                	synchronized (systemContext.emulationSystem) {
                		if (memoryScreen.isInsertionModeActivated()) memoryScreen.setAsciiFieldEdition(!memoryScreen.isAsciiFieldEdited());
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_INSERT:
                	synchronized (systemContext.emulationSystem) {
	                    memoryScreen.activateInsertionMode(!memoryScreen.isInsertionModeActivated());
	                    if (memoryScreen.isInsertionModeActivated()) memoryScreen.resetCursor();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_HOME:
                	synchronized (systemContext.emulationSystem) {
                		if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorToStartOfLine();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_END:
                	synchronized (systemContext.emulationSystem) {
                		if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorToEndOfLine();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_UP:
                	synchronized (systemContext.emulationSystem) {
	                    if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorUp(); 
	                    else memoryScreen.goToPreviousLine();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_DOWN:
                	synchronized (systemContext.emulationSystem) {
	                    if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorDown();
	                    else memoryScreen.goToNextLine();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_LEFT:
                	synchronized (systemContext.emulationSystem) {
	                    if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorLeft();
	                    else memoryScreen.goTo(memoryScreen.getStartAddress()-1);
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_RIGHT:
                	synchronized (systemContext.emulationSystem) {
	                    if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorRight();
	                    else memoryScreen.goTo(memoryScreen.getStartAddress()+1);
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_PAGE_UP:
                	synchronized (systemContext.emulationSystem) {
	                    if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorToPreviousPage();
	                    else memoryScreen.goToPreviousPage();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_PAGE_DOWN:
                	synchronized (systemContext.emulationSystem) {
	                    if (memoryScreen.isInsertionModeActivated()) memoryScreen.moveCursorToNextPage();
	                    else memoryScreen.goToNextPage();
                	}
                	screenDirty=true; repaint(); break;
                }
            }
        };
        
        private final java.awt.event.MouseListener mouseListener=new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!attachedFlag || e.getButton()!=1) return;
                
                int x=e.getX(), y=e.getY();
                if (positionToCharacter(x, y, mouseCharacter)) return;
                if (memoryScreen.isInsertionModeActivated()) {
                	synchronized (systemContext.emulationSystem) {
                		memoryScreen.setCursorPosition(mouseCharacter.x, mouseCharacter.y);
                	}
	                screenDirty=true; repaint();                	
                }
            }
        };
        
        private final java.awt.event.MouseWheelListener mouseWheelListener=new java.awt.event.MouseWheelListener() {
        	public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
            	if (!attachedFlag) return;
            	
            	int count=e.getUnitsToScroll();
            	synchronized (systemContext.emulationSystem) {
    	        	if (count<0) {
    	        		count=-count;
    	        		for (int i=0; i<count; i++) memoryScreen.goToPreviousLine();
    	        	} else {
    	        		for (int i=0; i<count; i++) memoryScreen.goToNextLine();
    	        	}
            	}
                screenDirty=true; repaint();
        	}
        };
    };

}
