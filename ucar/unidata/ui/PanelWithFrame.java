/*
 * $Id: PanelWithFrame.java,v 1.10 2007/07/06 20:45:32 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Resource;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.URL;

import java.rmi.RemoteException;


import java.util.*;

import java.util.ArrayList;

import javax.swing.*;

import javax.swing.event.*;


/**
 * Widget for configuring maps.  Maintains a CompositeDisplayable
 * of MapLines and LatLonLines.  The user can configure the visibility
 * and color of a particular set of map or lat/lon lines.  The increments
 * between lat/lon lines can be configured as well.  The widget is configured
 * through a properties file.  When the properties of the widget are set
 * and the "Apply" button is selected, a PropertyChangeEvent is thrown
 * with the CompositeDisplayable of all visible maps as the newValue.
 */

public class PanelWithFrame extends JPanel {

    /** _more_ */
    private JFrame frame;

    /**
     * _more_
     *
     */
    public PanelWithFrame() {}

    /**
     * _more_
     *
     * @param title
     */
    protected void makeFrame(String title) {
        frame = new JFrame(title);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeFrame();
            }
        });
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean haveFrame() {
        return (frame != null);
    }


    /**
     * _more_
     * @return _more_
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * _more_
     *
     * @param c
     */
    public void addContentsToFrame(Component c) {
        if (frame != null) {
            frame.getContentPane().add(c, BorderLayout.CENTER);
        }
    }

    /**
     * _more_
     */
    public void packFrame() {
        if (frame != null) {
            frame.pack();
        }

    }

    /**
     * _more_
     */
    public void closeFrame() {
        if (frame != null) {
            frame.setVisible(false);
        }
    }

    /**
     * _more_
     */
    public void destroyFrame() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean isFrameShowing() {
        if (frame != null) {
            return frame.isShowing();
        }
        return false;
    }


    /**
     * _more_
     */
    public void showFrame() {
        if (frame != null) {
            frame.show();
        }
    }

    /**
     * _more_
     *
     * @param l
     */
    public void addWindowListenerToFrame(WindowListener l) {
        if (frame != null) {
            frame.addWindowListener(l);
        }
    }


}

