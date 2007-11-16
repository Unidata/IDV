/*
 * $Id: NavigatedViewManager.java,v 1.38 2007/06/11 21:28:48 jeffmc Exp $
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


package ucar.unidata.idv.ui;

import ucar.unidata.idv.*;
import ucar.visad.display.*;

import ucar.unidata.geoloc.*;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.view.geoloc.*;

import ucar.visad.GeoUtils;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 *
 * @author IDV development team
 */

public class CursorReadoutWindow {

    NavigatedViewManager vm;

    /** _more_ */
    protected JWindow window;

    /** _more_ */
    protected JLabel label;

    /** _more_ */
    protected int windowWidth = 0;

    public CursorReadoutWindow(NavigatedViewManager vm) {
        this.vm = vm;
    }



    /**
     * _more_
     *
     * @param e _more_
     */
    public void handleMouseReleased(MouseEvent e) {
        if (window != null) {
            window.dispose();
            window      = null;
            windowWidth = -1;
        }
    }



    /**
     * _more_
     *
     * @param e _more_
     */
    public void handleMousePressedOrDragged(MouseEvent e) {
        if (window == null) {
            JComponent contents = (JComponent)vm.getContents();
            Window parent = GuiUtils.getWindow(contents);
            window = new JWindow(parent);
            window.pack();
            Rectangle b   = contents.bounds();
            Point     loc = contents.getLocationOnScreen();
            window.setLocation(
                (int) loc.getX(),
                (int) (loc.getY() /*+contents.bounds().height*/));
            window.show();
        }
        double[] box =
            vm.getNavigatedDisplay().getSpatialCoordinatesFromScreen(e.getX(),
                e.getY());
        EarthLocation el = vm.getNavigatedDisplay().getEarthLocation(box[0],
                               box[1], box[2], true);

        List         controls = vm.getControls();
        StringBuffer sb       = new StringBuffer();
        Animation animation = vm.getAnimation();
        int       step      = animation.getCurrent();
        Real      aniValue  = animation.getAniValue();

        boolean   didone    = false;
        try {
            for (int i = 0; i < controls.size(); i++) {
                DisplayControl display = (DisplayControl) controls.get(i);
                List readout = display.getCursorReadout(el, aniValue, step);
                if ((readout != null) && (readout.size() > 0)) {
                    didone = true;
                    sb.append(StringUtil.join("", readout));
                }

            }

            if ( !didone) {
                //                window.setVisible(false);
                //                return;
            } else {
                //                window.setVisible(true);
                //                window.toFront();
                
            }
            label = new JLabel("<html>Location: " + el +(didone?"<hr>":"") +"<table width=\"100%\">"+sb + "</table></html>");
            label.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

            window.getContentPane().removeAll();
            window.getContentPane().add(label);
            window.pack();
            window.toFront();
        } catch (Exception exc) {
            vm.logException("Getting cursor readouts", exc);
        }
    }



}

