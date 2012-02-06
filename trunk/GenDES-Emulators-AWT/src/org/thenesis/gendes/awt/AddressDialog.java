package org.thenesis.gendes.awt;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;


//******************************************************************************
// Address dialog.
//******************************************************************************
public class AddressDialog extends Dialog {
    private Frame parentFrame;
    public TextField addressTextField;
    public Button okButton, cancelButton;
    public boolean okFlag=false;
    
    public ActionListener okActionListener=new ActionListener() {
        public void actionPerformed(ActionEvent e) { okFlag=true; setVisible(false); dispose(); }
    };
    
    public ActionListener cancelActionListener=new ActionListener() {
        public void actionPerformed(ActionEvent e) { okFlag=false; setVisible(false); dispose(); }
    };
    
    public static String getValue(Frame f) {
        AddressDialog ad=new AddressDialog(f);
        ad.setVisible(true);
        String s=null;
        if (ad.okFlag) s=ad.addressTextField.getText();
        return s;
    }
    
    public AddressDialog(Frame f) {
        super(f, "Enter an address", true);
        parentFrame=f;
        
        addComponentListener(componentAdapter);
        
        GridBagLayout gridbag=new GridBagLayout();
        setLayout(gridbag);
        GridBagConstraints c=new GridBagConstraints();
        
        c.gridx=0; c.gridy=0; c.gridwidth=2; c.gridheight=1;
        c.weightx=1.0; c.weighty=1.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        addressTextField=new TextField("", 4);
        gridbag.setConstraints(addressTextField, c); add(addressTextField);
        
        c.gridx=0; c.gridy=1; c.gridwidth=1; c.gridheight=1;
        c.weightx=1.0; c.weighty=1.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        okButton=new Button("OK"); okButton.addActionListener(okActionListener);
        gridbag.setConstraints(okButton, c); add(okButton);
        
        c.gridx=1; c.gridy=1; c.gridwidth=1; c.gridheight=1;
        c.weightx=1.0; c.weighty=1.0; c.fill=GridBagConstraints.BOTH; c.anchor=GridBagConstraints.NORTHWEST;
        cancelButton=new Button("Cancel"); cancelButton.addActionListener(cancelActionListener);
        gridbag.setConstraints(cancelButton, c); add(cancelButton);        
    }
    
    private ComponentAdapter componentAdapter=new ComponentAdapter() {
        public void componentShown(ComponentEvent e) {        
            Point pp=parentFrame.getLocation();
            Dimension pd=parentFrame.getSize();
            Insets insets=AddressDialog.this.getInsets();
            
            int w=192+(insets.left+insets.right), h=48+(insets.top+insets.bottom);
            setSize(w, h);
            int x=pp.x+((pd.width-w)>>1), y=pp.y+((pd.height-h)>>1);
            setLocation(x, y);
        }        
    };
}
//******************************************************************************
