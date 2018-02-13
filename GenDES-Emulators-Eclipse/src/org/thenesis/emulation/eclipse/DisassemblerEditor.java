package org.thenesis.emulation.eclipse;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.thenesis.gendes.contexts.ContextEvent;
import org.thenesis.gendes.contexts.ContextEventListener;
import org.thenesis.gendes.contexts.SystemEmulationContext;
import org.thenesis.gendes.debug.DisassemblerScreen;

public abstract class DisassemblerEditor extends EditorPart {
	
	public DisassemblerEditor() {        
        screenPalette[0]=0x00ffffff; screenPalette[1]=0x00000000; screenPalette[2]=0x00ffff00; screenPalette[3]=0x00000000;
        screenPalette[4]=0x0000ffff; screenPalette[5]=0x00000000; screenPalette[6]=0x0000ff00; screenPalette[7]=0x00000000;        
	}

	//--------------------------------------------------------------------------------
	// Implementation specific.
	//--------------------------------------------------------------------------------
	protected boolean attachedFlag=false;
	protected SystemEmulationContext systemContext;
    protected final int screenPalette[]=new int[256];    
    protected DisassemblerScreen disassemblerScreen;

    protected abstract void onCreateEditor();
	protected abstract void onDestroyEditor();

	//--------------------------------------------------------------------------------
	// Local components.
	//--------------------------------------------------------------------------------
	protected Combo addressSpaceCombo;
	protected Combo addressCombo;
	protected Button addressButton;
	protected ScreenComposite screenComposite;

	//--------------------------------------------------------------------------------
	// WorkbenchPart.
	//--------------------------------------------------------------------------------
	protected Composite parentComposite;

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
		
		onCreateEditor();
	}

    public void dispose() {
    	onDestroyEditor();
        super.dispose();
    }

	public void setFocus() {
		screenComposite.canvas.requestFocus();
	}

	//--------------------------------------------------------------------------------
	// EditorPart.
	//--------------------------------------------------------------------------------
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
	}

	public boolean isDirty() {
		return false;
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public void doSave(IProgressMonitor monitor) {
	}

	public void doSaveAs() {
	}

	//--------------------------------------------------------------------------------
    // Context listeners.
	//--------------------------------------------------------------------------------
    private final ContextEventListener onPauseListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) {
            disassemblerScreen.followPc();
            disassemblerScreen.updateScreen(true);
            screenComposite.redrawScreen();
        }
    };
    
    private final ContextEventListener onResetListener=new ContextEventListener() {
        public void processEvent(ContextEvent e) {
            screenComposite.redrawScreen();
        }
    };
    
	//--------------------------------------------------------------------------------
	// .
	//--------------------------------------------------------------------------------
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
    		disassemblerScreen.goTo(address);
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
    		int width=disassemblerScreen.getScreenPixelsWidth(), height=disassemblerScreen.getScreenPixelsHeight();
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
            if (disassemblerScreen.isDirty()) {
            	updateScreenImgFlag=true;
            	synchronized (systemContext.emulationSystem) {
            		disassemblerScreen.updateScreen(true);
            	}
            }
            if (updateScreenImgFlag) {
                DataBufferInt dataBuffer=(DataBufferInt)screenImg.getRaster().getDataBuffer();
    	        disassemblerScreen.drawScreen(dataBuffer.getData(), screenImg.getWidth(), screenImg.getHeight(), screenPalette);
            }

            g.drawImage(screenImg, 1, 1, this);
        }
        
        private void drawForeground(java.awt.Graphics g) {
        	g.setColor(this.hasFocus() ? java.awt.Color.BLUE : java.awt.Color.LIGHT_GRAY);
        	g.drawRect(0, 0, getWidth()-1, getHeight()-1);
        }

        private void resizeScreen() {
            if (!attachedFlag) return;

            int h=getHeight();
            int sh=h/disassemblerScreen.getFontHeight();
            if (sh<1) sh=1;
            disassemblerScreen.setScreenHeight(sh);        
            
            int imgWidth=disassemblerScreen.getScreenPixelsWidth(), imgHeight=disassemblerScreen.getScreenPixelsHeight();
            if (imgWidth<=0) imgWidth=1; if (imgHeight<=0) imgHeight=1;
            screenImg=new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        }
        
        private boolean positionToCharacter(int x, int y, org.thenesis.gendes.debug.Point c) {
            int cx=x/disassemblerScreen.getFontWidth(), cy=y/disassemblerScreen.getFontHeight();
            if (disassemblerScreen.isCharacterVisible(cx, cy)) {
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
                case '#':
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.setPcFollowing(!disassemblerScreen.getPcFollowing());
                	}
                	screenDirty=true; repaint(); break;
                case '=':
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.goToPc();
                	}
                	screenDirty=true; repaint(); break;
                }
            }
            
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (!attachedFlag) return;
                
                int key=e.getKeyCode();
                switch (key) {
                case java.awt.event.KeyEvent.VK_INSERT: // TODO: implement edition.
                    
                    break;
                case java.awt.event.KeyEvent.VK_UP:
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.goToPreviousInstruction();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_DOWN:
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.goToNextInstruction();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_LEFT:
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.goTo(disassemblerScreen.getStartAddress()-1);
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_RIGHT:
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.goTo(disassemblerScreen.getStartAddress()+1);
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_PAGE_UP:
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.goToPreviousPage();
                	}
                	screenDirty=true; repaint(); break;
                case java.awt.event.KeyEvent.VK_PAGE_DOWN:
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.goToNextPage();
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

                if (mouseCharacter.x<=1 || e.getClickCount()==2) {
                	synchronized (systemContext.emulationSystem) {
                		disassemblerScreen.setBreakpoint(mouseCharacter.y);
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
    	        		for (int i=0; i<count; i++) disassemblerScreen.goToPreviousInstruction();
    	        	} else {
    	        		for (int i=0; i<count; i++) disassemblerScreen.goToNextInstruction();
    	        	}
            	}
                screenDirty=true; repaint();
        	}
        };
    };
    

}
