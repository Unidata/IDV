/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv.ui;



import ucar.unidata.idv.*;


import ucar.unidata.ui.RovingProgress;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;





import java.awt.*;
import java.awt.event.*;


//JDK1.4
import java.beans.*;

import java.io.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;






/**
 * A class for the splash screen
 *
 *
 * @author IDV development team
 * @version $Revision: 1.39 $Date: 2006/12/01 19:54:03 $
 */


public class IdvSplash extends JWindow {

    /** Reference to the IDV */
    IntegratedDataViewer idv;

    /** The JLabel to show messages */
    JLabel splashLbl;

    /** The text to use in the splash screen */
    private String splashTitle = null;

    /** The icon to use in the splash screen */
    private ImageIcon splashIcon;

    /** The icon to use when the mouse rolls over the splash icon */
    private ImageIcon splashRolloverIcon;

    /**
     *  Keep the splash progress bar around to tell it to stop.
     */
    private RovingProgress splashProgressBar;


    /**
     * Create the splash screen
     *
     * @param idv The IDV
     *
     */
    public IdvSplash(IntegratedDataViewer idv) {
        this.idv = idv;
        init();
    }


    /**
     *  Show a message in the splash screen (if it exists)
     *
     * @param m The message
     */
    public void splashMsg(String m) {
        if (splashLbl != null) {
            splashLbl.setText(" " + Msg.msg(m) + " ");
        }
    }

    /**
     *  Close and dispose of the splash window (if it has been created).
     */
    public void doClose() {
        if (splashProgressBar != null) {
            splashProgressBar.stop();
        }
        setVisible(false);
        dispose();
    }

    /** done the audio once */
    boolean playedOnce = false;


    /**
     *  Create and return (if not in test mode) the splash screen.
     */
    private void init() {

        try {
            splashTitle = idv.getProperty("idv.ui.splash.title", "");
            splashTitle =
                idv.getResourceManager().getResourcePath(splashTitle);
            splashTitle =
                StringUtil.replace(splashTitle, "%IDV.TITLE%",
                                   (String) idv.getProperty("idv.title",
                                       "Unidata IDV"));

            splashIcon =
                GuiUtils.getImageIcon(idv.getProperty("idv.ui.splash.icon",
                    "/ucar/unidata/idv/images/logo.gif"));
            splashRolloverIcon = GuiUtils.getImageIcon(
                idv.getProperty(
                    "idv.ui.splash.iconroll",
                    "/ucar/unidata/idv/images/logo_rollover.gif"));
        } catch (Exception exc) {}


        JLabel image = ((splashIcon != null)
                        ? new JLabel(splashIcon)
                        : new JLabel("Unidata IDV"));
        if ((splashIcon != null) && (splashRolloverIcon != null)) {
            int width = Math.max(splashIcon.getIconWidth(),
                                 splashRolloverIcon.getIconWidth());
            int height = Math.max(splashIcon.getIconHeight(),
                                  splashRolloverIcon.getIconHeight());
            image.setPreferredSize(new Dimension(width, height));
        }

        image.addMouseListener(new ObjectListener(image) {
            public void mouseEntered(MouseEvent e) {
                if (splashRolloverIcon != null) {
                    ((JLabel) e.getSource()).setIcon(splashRolloverIcon);
                }
                /*else*/
                if (false && !playedOnce) {
                    Misc.run(new Runnable() {
                        public void run() {
                            try {
                                String audioFile =
                                    idv.getStore().getTmpFile("splash.wav");
                                IOUtil.writeTo(
                                    IOUtil.getInputStream(
                                        "/auxdata/ui/icons/test.gif"), new FileOutputStream(
                                        audioFile));
                                ucar.unidata.ui.AudioPlayer audioPlayer =
                                    new ucar.unidata.ui.AudioPlayer();
                                audioPlayer.setFile(audioFile);
                                audioPlayer.startPlaying();
                                playedOnce = true;
                            } catch (Exception exc) {}
                        }
                    });
                }

            }

            public void mouseExited(MouseEvent e) {
                if (splashIcon != null) {
                    ((JLabel) e.getSource()).setIcon(splashIcon);
                }
            }

            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() > 1) && e.isControlDown()) {
                    ImageIcon beerImage =
                        new ImageIcon(
                            Resource.getImage(
                                "/ucar/unidata/idv/images/beer.gif"));
                    JDialog d        = new JDialog();
                    JPanel  contents = new JPanel(new BorderLayout());
                    contents.add(
                        "North",
                        new JLabel(
                            "Congratulations, you found the Easter egg!"));
                    contents.add(
                        "Center",
                        new JLabel(
                            idv.getProperty("idv.ui.splash.eastermsg", "")));
                    JButton b = new JButton(beerImage);
                    b.setPreferredSize(
                        new Dimension(
                            beerImage.getIconHeight(),
                            beerImage.getIconWidth()));
                    b.addActionListener(new ObjectListener(d) {
                        public void actionPerformed(ActionEvent event) {
                            ((JDialog) theObject).setVisible(false);
                        }
                    });
                    contents.add("East", GuiUtils.inset(b, 4));
                    GuiUtils.packDialog(d, contents);
                    Dimension ss =
                        Toolkit.getDefaultToolkit().getScreenSize();
                    d.setLocation(ss.width / 2 - 100, ss.height / 2 - 100);
                    d.show();
                }
            }
        });

        splashLbl = GuiUtils.cLabel(" ");
        splashLbl.setForeground(Color.gray);

        splashProgressBar = new RovingProgress();
        splashProgressBar.start();
        splashProgressBar.setBorder(
            BorderFactory.createLineBorder(Color.gray));


        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        });

        if ((splashTitle == null) || splashTitle.trim().equals("")) {
            String version = idv.getStateManager().getVersion();
            String title   = idv.getStateManager().getTitle();
            splashTitle = title + " " + version;
        }

        JLabel versionLabel = GuiUtils.cLabel("<html><center><b>"
                                  + splashTitle + "</center></b></html>");

        JPanel imagePanel = GuiUtils.inset(image, new Insets(4, 35, 0, 35));
        JPanel titlePanel = GuiUtils.center(versionLabel);

        JPanel barPanel = GuiUtils.inset(splashProgressBar,
                                         new Insets(4, 1, 1, 1));

        JPanel topPanel = GuiUtils.vbox(imagePanel, titlePanel, barPanel);
        topPanel = GuiUtils.centerBottom(topPanel, splashLbl);
        JPanel contents =
            GuiUtils.topCenter(topPanel,
                               GuiUtils.inset(GuiUtils.wrap(cancelButton),
                                   4));
        JPanel outer = GuiUtils.center(contents);
        contents.setBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED));
        outer.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,
                Color.gray, Color.gray));
        getContentPane().add(outer);
        pack();
        Dimension size       = getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - size.width / 2,
                    screenSize.height / 2 - size.height / 2);

        ucar.unidata.util.Msg.translateTree(this);


        show();
        toFront();




    }

}
