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

package ucar.unidata.idv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayControl;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewDescriptor;

import ucar.unidata.ui.FineLineBorder;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
//import ucar.unidata.data.DataSource;
import ucar.unidata.util.Misc;


import visad.*;


import java.applet.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.io.IOException;

import java.net.*;
import java.net.MalformedURLException;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import java.util.Properties;

import java.util.StringTokenizer;

import javax.media.*;





import javax.media.format.VideoFormat;
import javax.media.protocol.*;
import javax.media.util.BufferToImage;

import javax.swing.*;

import javax.swing.border.*;

import javax.swing.event.*;


/**
 * Control for displaying a movie (QuickTime).
 *
 * @author IDV development team
 * @version $Revision: 1.12 $
 */
public class MovieDisplayControl extends DisplayControlImpl implements ControllerListener {


    // media SimplePlayer

    /** media SimplePlayer */
    Player player = null;

    /** component in which video is playing */
    Component visualComponent = null;

    /** controls gain, position, start, stop */
    Component controlComponent = null;

    /** displays progress during download */
    Component progressBar = null;

    /** flag for first time */
    boolean firstTime = true;

    /** caching size */
    long CachingSize = 0L;

    /** main panel */
    JPanel panel = null;

    /** control panel height */
    int controlPanelHeight = 0;

    /** video width */
    int videoWidth = 0;

    /** video height */
    int videoHeight = 0;

    /** media file */
    String mediaFile;


    /**
     * Default contstructor; does nothing
     */
    public MovieDisplayControl() {}


    /**
     * Initialize the control.
     *
     * @param dataChoice  choice for data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        mediaFile = dataChoice.getStringId();
        //Make it a url format
        if ((new File(mediaFile)).exists()) {
            mediaFile = "file:" + mediaFile;
        }





        // URL for our media file
        //mediaFile = "http://java.sun.com/products/java-media/jmf/2.1.1/samples/samples/media/darkcity.7.160x120.11khz.mov";
        MediaLocator mrl = null;

        try {
            // Create a media locator from the file name
            if ((mrl = new MediaLocator(mediaFile)) == null) {
                userMessage("Can't build URL for " + mediaFile);
                return false;
            }

            try {
                player = Manager.createPlayer(mrl);
                //                System.err.println ("player:" + player.getClass().getName());
            } catch (NoPlayerException e) {
                logException("Could not create player for " + mrl, e);
                return false;
            }
        } catch (MalformedURLException e) {
            userMessage("Invalid media file URL!");
        } catch (IOException e) {
            userMessage("IO exception creating player for " + mrl);
            return false;
        }


        return true;
    }


    /**
     * Called after init().  Start the player.
     */
    public void initDone() {
        super.initDone();
        // Add ourselves as a listener for a player's events
        player.addControllerListener(this);
        player.start();
    }

    /**
     * Make the UI contents for this control.
     * @return  UI contents
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        panel = new JPanel();
        panel.setLayout(null);
        panel.setBounds(0, 0, 320, 240);
        return panel;
    }


    /**
     * This controllerUpdate function must be defined in order to
     * implement a ControllerListener interface. This
     * function will be called whenever there is a media event
     *
     * @param event   event to listen for
     */
    public synchronized void controllerUpdate(ControllerEvent event) {
        // If we're getting messages from a dead player,  just leave
        if (player == null) {
            return;
        }


        //        Misc.printStack("MDC.update",10,null);


        // When the player is Realized, get the visual 
        // and control components and add them to the Applet
        if (event instanceof RealizeCompleteEvent) {
            if (progressBar != null) {
                panel.remove(progressBar);
                progressBar = null;
            }

            int width  = 320;
            int height = 0;
            if (controlComponent == null) {
                if ((controlComponent = player.getControlPanelComponent())
                        != null) {
                    controlPanelHeight =
                        controlComponent.getPreferredSize().height;
                    panel.add(controlComponent);
                    height += controlPanelHeight;
                }
            }
            if (visualComponent == null) {
                if ((visualComponent = player.getVisualComponent()) != null) {
                    //                    System.err.println("vc:" + visualComponent.getClass().getName());
                    panel.add(visualComponent);
                    Dimension videoSize = visualComponent.getPreferredSize();
                    videoWidth  = videoSize.width;
                    videoHeight = videoSize.height;
                    width       = videoWidth;
                    height      += videoHeight;
                    visualComponent.setBounds(0, 0, videoWidth, videoHeight);
                }
            }
            //      panel.setBounds(0, 0, width, height);
            panel.setPreferredSize(new Dimension(width, height));
            panel.setMinimumSize(new Dimension(width, height));
            if (controlComponent != null) {
                controlComponent.setBounds(0, videoHeight, width,
                                           controlPanelHeight);
                controlComponent.invalidate();
            }
            redoGuiLayout();

        } else if (event instanceof CachingControlEvent) {
            if (player.getState() > Controller.Realizing) {
                return;
            }
            // Put a progress bar up when downloading starts, 
            // take it down when downloading ends.
            CachingControlEvent e  = (CachingControlEvent) event;
            CachingControl      cc = e.getCachingControl();

            // Add the bar if not already there ...
            if (progressBar == null) {
                if ((progressBar = cc.getControlComponent()) != null) {
                    panel.add(progressBar);
                    panel.setSize(progressBar.getPreferredSize());
                    redoGuiLayout();
                }
            }
        } else if (event instanceof EndOfMediaEvent) {
            // We've reached the end of the media; rewind and
            // start over
            player.setMediaTime(new Time(0));
            player.start();
        } else if (event instanceof ControllerErrorEvent) {
            // Tell TypicalPlayerApplet.start() to call it a day
            player = null;
            userMessage(((ControllerErrorEvent) event).getMessage());
        } else if (event instanceof ControllerClosedEvent) {
            panel.removeAll();
        }
    }


    /**
     * Remove from the IDV.  Cleans up
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        if (player != null) {
            player.removeControllerListener(this);
            player.stop();
            player.close();
            player = null;
        }
    }

}
