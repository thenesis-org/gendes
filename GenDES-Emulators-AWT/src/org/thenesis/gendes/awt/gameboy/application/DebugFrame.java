package org.thenesis.gendes.awt.gameboy.application;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.thenesis.gendes.awt.gameboy.debug.CPUPanel;
import org.thenesis.gendes.awt.gameboy.debug.CartridgePanel;
import org.thenesis.gendes.awt.gameboy.debug.DisassemblerPanel;
import org.thenesis.gendes.awt.gameboy.debug.HardwarePanel;
import org.thenesis.gendes.awt.gameboy.debug.MemoryPanel;
import org.thenesis.gendes.awt.gameboy.debug.PerformancePanel;
import org.thenesis.gendes.contexts.ContextNode;
import org.thenesis.gendes.contexts.GameBoyEmulationContext;


//******************************************************************************
// Debug frame.
//******************************************************************************
public final class DebugFrame extends Frame {
    public GameBoyEmulationContext gameBoyContext;
    
    private final ContextNode gameBoyNode=new ContextNode() {
    	public void detach() { DebugFrame.this.detach(); }
    };
    
    public PerformancePanel performancePanel;
    public CartridgePanel cartridgePanel;
    public CPUPanel cpuPanel;
    public DisassemblerPanel disassemblerPanel;
    public MemoryPanel memoryPanel;
    public HardwarePanel hardwarePanel;
    
    public DebugFrame() {
        super("Java Game Boy emulator debugger");

        addComponentListener(componentAdapter);
        addWindowListener(windowAdapter);
        
        GridBagLayout gridbag=new GridBagLayout();
        GridBagConstraints c=new GridBagConstraints();
        setLayout(gridbag);

        c.gridx=0; c.gridy=0; c.gridwidth=1; c.gridheight=1;
        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        performancePanel=new PerformancePanel();
        gridbag.setConstraints(performancePanel, c);
        add(performancePanel);

        c.gridx=1; c.gridy=0; c.gridwidth=1; c.gridheight=1;
        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        cartridgePanel=new CartridgePanel();
        gridbag.setConstraints(cartridgePanel, c);
        add(cartridgePanel);

        c.gridx=0; c.gridy=1; c.gridwidth=2; c.gridheight=1;
        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        hardwarePanel=new HardwarePanel();
        gridbag.setConstraints(hardwarePanel, c);
        add(hardwarePanel);

        c.gridx=2; c.gridy=0; c.gridwidth=1; c.gridheight=1;
        c.weightx=0.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        cpuPanel=new CPUPanel();
        gridbag.setConstraints(cpuPanel, c);
        add(cpuPanel);
        
        c.gridx=2; c.gridy=1; c.gridwidth=1; c.gridheight=2;
        c.weightx=1.0; c.weighty=1.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        disassemblerPanel=new DisassemblerPanel();
        gridbag.setConstraints(disassemblerPanel, c);
        add(disassemblerPanel);
        
        c.gridx=0; c.gridy=2; c.gridwidth=2; c.gridheight=1;
        c.weightx=0.0; c.weighty=1.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        memoryPanel=new MemoryPanel();
        gridbag.setConstraints(memoryPanel, c);
        add(memoryPanel);
        
        doLayout();
    }
    
    public void attach(GameBoyEmulationContext gbc) {
        detach();
        
        gameBoyContext=gbc;        
       	gameBoyContext.add(gameBoyNode);

        performancePanel.attach(gameBoyContext);
        cartridgePanel.attach(gameBoyContext);
        hardwarePanel.attach(gameBoyContext);
        cpuPanel.attach(gameBoyContext);
        disassemblerPanel.attach(gameBoyContext, this);
        memoryPanel.attach(gameBoyContext, this);
        
        doLayout();
        repaint();
    }
    
    public void detach() {   
        if (gameBoyContext==null) return;
        
    	performancePanel.detach();
    	cartridgePanel.detach();
    	hardwarePanel.detach();
    	cpuPanel.detach();
    	disassemblerPanel.detach();
    	memoryPanel.detach();
    	
       	gameBoyContext.remove(gameBoyNode);
    	gameBoyContext=null;
    	
        repaint();
    }
    
    public void update(Graphics g) {}
    
    private ComponentAdapter componentAdapter=new ComponentAdapter() {
        public void componentResized(ComponentEvent arg0) { doLayout(); }        
    };

    private WindowAdapter windowAdapter=new WindowAdapter() {
        public void windowClosing(WindowEvent e) { setVisible(false); dispose(); }
        public void windowClosed(WindowEvent e) {  }
    };
}
//******************************************************************************
