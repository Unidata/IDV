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


import ucar.unidata.geoloc.*;

import ucar.unidata.idv.*;
import ucar.unidata.idv.control.ReadoutInfo;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.view.geoloc.*;

import ucar.visad.GeoUtils;
import ucar.visad.display.*;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
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

    /** _more_          */
    NavigatedViewManager vm;

    /** _more_ */
    protected JWindow window;

    /** _more_ */
    protected JLabel label;

    /** _more_ */
    protected int windowWidth = 0;

    /** _more_          */
    private EarthLocation lastEarthLocation;

    /** _more_          */
    private boolean ignoreMissing = true;


    /**
     * _more_
     *
     * @param vm _more_
     */
    public CursorReadoutWindow(NavigatedViewManager vm) {
        this(vm, true);
    }


    /**
     * _more_
     *
     * @param vm _more_
     * @param ignoreMissing _more_
     */
    public CursorReadoutWindow(NavigatedViewManager vm,
                               boolean ignoreMissing) {
        this.vm            = vm;
        this.ignoreMissing = ignoreMissing;
        label              = GuiUtils.getFixedWidthLabel("");
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
     */
    private void setWindowLocation() {
        JComponent contents   = (JComponent) vm.getContents();
        Rectangle  b          = contents.bounds();
        Point      loc        = contents.getLocationOnScreen();
        Dimension  screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int        left       = loc.x;
        if (left < 0) {
            left = 0;
        }

        int right = left + window.getBounds().width;
        if (right > screenSize.width) {
            left += screenSize.width - right;
        }


        int top    = loc.y + contents.bounds().height;
        int bottom = top + window.getBounds().height;

        if (bottom > screenSize.height) {
            top += screenSize.height - bottom - 10;
        }


        window.setLocation(left, top);

    }

    /**
     * _more_
     *
     * @param e _more_
     */
    public void handleMousePressedOrDragged(MouseEvent e) {
        if (window == null) {
            JComponent contents = (JComponent) vm.getContents();
            Window     parent   = GuiUtils.getWindow(contents);
            window = new JWindow(parent);
            window.pack();
            setWindowLocation();
            window.show();
        }
        double[] box =
            vm.getNavigatedDisplay().getSpatialCoordinatesFromScreen(
                e.getX(), e.getY());
        lastEarthLocation = vm.getNavigatedDisplay().getEarthLocation(box[0],
                box[1], box[2], true);

        updateReadout();
    }


    /**
     * _more_
     */
    public void updateReadout() {
        if ((lastEarthLocation == null) || (window == null)) {
            return;
        }
        String readout = getReadout(lastEarthLocation, true, false,
                                    new ArrayList<ReadoutInfo>());
        if (readout == null) {
            readout = "";
        }

        final String theReadout = readout;
        GuiUtils.invokeInSwingThread(new Runnable() {
            public void run() {
                label.setText(theReadout);
                window.getContentPane().removeAll();
                JComponent wrapper = GuiUtils.inset(label,
                                         new Insets(2, 5, 1, 5));
                wrapper.setBorder(
                    BorderFactory.createBevelBorder(BevelBorder.RAISED));
                window.getContentPane().add(wrapper);
                window.pack();
                setWindowLocation();
                window.toFront();
            }
        });
    }




    /**
     * _more_
     *
     * @param earthLocation _more_
     * @param showDisplays _more_
     * @param showAlt _more_
     * @param samples _more_
     *
     * @return _more_
     */
    public String getReadout(EarthLocation earthLocation,
                             boolean showDisplays, boolean showAlt,
                             List<ReadoutInfo> samples) {
        if (earthLocation == null) {
            return "";
        }
        List         controls  = vm.getControls();
        StringBuffer sb        = new StringBuffer();
        Animation    animation = vm.getAnimation();
        int          step      = animation.getCurrent();
        Real         aniValue  = animation.getAniValue();

        boolean      didone    = false;

        try {
            if (showDisplays) {
                for (int i = 0; i < controls.size(); i++) {
                    DisplayControl display = (DisplayControl) controls.get(i);
                    List readout = display.getCursorReadout(earthLocation,
                                       aniValue, step, samples);
                    if ((readout != null) && (readout.size() > 0)) {
                        didone = true;
                        sb.append(StringUtil.join("", readout));
                    }

                }
            }

            if ( !didone) {
                //                window.setVisible(false);
                //                return;
            } else {
                //                window.setVisible(true);
                //                window.toFront();

            }


            String llp = ((earthLocation == null)
                          ? ""
                          : vm.getIdv().getDisplayConventions()
                              .formatEarthLocation(earthLocation, showAlt));
            llp = StringUtil.padRight(llp, 6 * 100, "&nbsp;");

            return "<html>Location: " + llp + (didone
                    ? "<hr>"
                    : "") + "<table cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">"
                          + sb + "</table></html>";
        } catch (Exception exc) {
            vm.logException("Getting cursor readouts", exc);
            return "";
        }
    }



}
