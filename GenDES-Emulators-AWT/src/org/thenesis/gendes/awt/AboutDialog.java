package org.thenesis.gendes.awt;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AboutDialog extends Dialog {
//  LogoCanvas logoCanvas=new LogoCanvas();
    
    TextArea textArea=new TextArea(
            "GenDES based Game Boy emulator\n"+
            "Version 0.4\n"+
            "Copyright (c) 2012 Guillaume & Mathieu Legris\n"+
            "\n"+
            "Key mapping (for Game Boy buttons):\n"+
            "-[Up arrow]: Up\n"+
            "-[Down arrow]: Down\n"+
            "-[Left arrow]: Left\n"+
            "-[Right arrow]: Right\n"+
            "-[E]: A button\n"+
            "-[R]: B button\n"+
            "-[D]: Start button\n"+
            "-[F]: Select button\n"+
            "\n"+
            "Options:\n"+
            "-[-]: zoom out\n"+
            "-[+]: zoom in\n"+
            "-[*]: change zoom mode\n"
    );

    Button okButton=new Button("OK");
    ActionListener okButtonListener=new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            AboutDialog.this.setVisible(false);
            AboutDialog.this.dispose();
        }
    };    
    
    public AboutDialog(Frame owner) {
        super(owner, "About GenDES Game Boy emulator", true);
        addWindowListener(windowAdapter);
        
        GridBagLayout gridbag=new GridBagLayout();
        GridBagConstraints c=new GridBagConstraints();
        setLayout(gridbag);
        setSize(320, 192);
        /*        
         c.gridx=0; c.gridy=0; c.gridwidth=1; c.gridheight=1;
         c.weightx=1.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
         gridbag.setConstraints(logoCanvas, c);
         logoCanvas.setSize(256, 64);
         add(logoCanvas);
         */
        c.gridx=0; c.gridy=1; c.gridwidth=1; c.gridheight=1;
        c.weightx=1.0; c.weighty=1.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(textArea, c);
        textArea.setSize(256, 300);
        add(textArea);
        
        c.gridx=0; c.gridy=2; c.gridwidth=1; c.gridheight=1;
        c.weightx=1.0; c.weighty=0.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(okButton, c);
        okButton.addActionListener(okButtonListener);
        add(okButton);
    }
    
    private WindowAdapter windowAdapter=new WindowAdapter() {
        public void windowOpened(WindowEvent e) { /* logoCanvas.start(); */ }
        public void windowClosing(WindowEvent e) { setVisible(false); dispose(); }
        public void windowClosed(WindowEvent e) { /* logoCanvas.stop(); */ }        
    };
}
