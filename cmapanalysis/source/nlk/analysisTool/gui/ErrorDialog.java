package nlk.analysisTool.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Creates a Dialog for displaying errors
 * 
 * -modify by @author: Maggie Breedy <KAoS team>
 * @version: $Revision  1.0$  
 * 
 */

public class ErrorDialog extends JDialog
{
    public static final int OK_SELECTED = 1;
    public static final int CANCEL_SELECTED = 0;
    private static final int  INITIAL_WINDOW_WIDTH  = 600; //800
    private static final int  INITIAL_WINDOW_HEIGHT = 300; //400

    public ErrorDialog (JFrame frame, String title, String message)
    {
        super (frame);
        this.setModal(true);
        this.setSize(INITIAL_WINDOW_WIDTH,INITIAL_WINDOW_HEIGHT);

        // Position the dialog nicely
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width  - INITIAL_WINDOW_WIDTH)/2;    // centered left/right
        int y = (screen.height - INITIAL_WINDOW_HEIGHT)/4;   // 1/4 from top
        this.setBounds(x, y, INITIAL_WINDOW_WIDTH, INITIAL_WINDOW_HEIGHT + 24); // 24 for titlebar?


        // create namespace stuff
        JLabel directionsLbl = new JLabel(title);
        directionsLbl.setFont (new Font ("Frame", Font.BOLD, 14));

        JTextArea errorTA = new JTextArea(message);
        errorTA.setLineWrap(true);
        errorTA.setEditable(false);
        errorTA.setWrapStyleWord(true);
        JScrollPane errorScrollPane = new JScrollPane(errorTA);
        errorScrollPane.setPreferredSize(new Dimension((INITIAL_WINDOW_WIDTH - 40),(INITIAL_WINDOW_HEIGHT - 80)));

        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent ae) {
                setVisible (false);
            }
        });

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        this.getContentPane().setLayout(gbl);

        // column 0
        gbc.insets = new Insets(13,13,5,13);
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.getContentPane().add(directionsLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(5,13,5,13);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.getContentPane().add (errorScrollPane, gbc);

        gbc.gridy = 2;
         gbc.insets = new Insets(5,150,5,150);
        this.getContentPane().add (okBtn, gbc);

        this.pack();
    }

    public int showDialog()
    {
        this.setVisible (true);
        return _selection;
    }

    private int _selection = OK_SELECTED;

}  //ErrorDialog