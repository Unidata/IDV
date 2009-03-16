/*
 *
 * Copyright 1997-2009 Unidata Program Center/University Corporation for
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


package ucar.unidata.idv.control.storm;


import ucar.unidata.data.DataChoice;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.drawing.DrawingGlyph;


import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.display.PointProbe;
import ucar.visad.display.SelectorPoint;

import visad.*;


import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.List;


import javax.swing.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Mar 10, 2009
 * Time: 1:03:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormIntensityControl extends DisplayControlImpl {

    /** _more_ */
    private LatLonWidget latLonWidget;

    /** _more_ */
    private LatLonPoint probeLocation;

    /** the probe */
    //    private SelectorPoint probe;
    private PointProbe probe;

    /**
     * _more_
     */
    public StormIntensityControl() {}






    /**
     * _more_
     *
     * @param choice _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public boolean init(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.init(choice)) {
            return false;
        }


        //        probe = new SelectorPoint ("",new RealTuple(RealTupleType.SpatialEarth3DTuple,
        //                           new double[] {0,0,0}));

        probe =
            new PointProbe(new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                         new double[] { 0,
                0, 0 }));

        probe.setManipulable(false);
        probe.setVisible(false);
        probe.setAutoSize(true);

        probe.setPointSize(getDisplayScale());
        addDisplayable(probe, FLAG_COLOR);


        setContents(doMakeContents());
        updateProbeLocation();
        return true;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public Container doMakeContents() {
        latLonWidget = new LatLonWidget(GuiUtils.makeActionListener(this,
                "latLonWidgetChanged", null));
        return latLonWidget;
    }


    /**
     * _more_
     */
    public void latLonWidgetChanged() {
        try {
            System.err.println("widget changed");
            String message = latLonWidget.isValidValues();
            if (message != null) {
                userMessage(message);
                return;
            }

            probeLocation =
                ucar.visad.Util.makeEarthLocation(latLonWidget.getLat(),
                    latLonWidget.getLon()).getLatLonPoint();
        } catch (Exception e) {
            logException("Handling LatLonWidget changed", e);
        }



    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean shouldAddDisplayListener() {
        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean shouldAddControlListener() {
        return true;
    }


    /**
     * Should we handle display events
     *
     * @return Ok to handle events
     */
    protected boolean canHandleEvents() {
        if ( !getHaveInitialized()
                || (getMakeWindow() && !getWindowVisible())) {
            return false;
        }
        return isGuiShown();
    }

    /**
     * Listen for DisplayEvents
     *
     * @param event The event
     */
    public void handleDisplayChanged(DisplayEvent event) {
        super.handleDisplayChanged(event);

        if ( !canHandleEvents()) {
            return;
        }


        int        id         = event.getId();
        InputEvent inputEvent = event.getInputEvent();

        try {
            if (id == DisplayEvent.MOUSE_PRESSED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }
                probeLocation = toEarth(event).getLatLonPoint();
                updateProbeLocation();
            }
        } catch (Exception e) {
            logException("Handling display event changed", e);
        }


    }


    /**
     * _more_
     */
    private void updateProbeLocation() {
        try {
            if (probeLocation == null) {
                return;
            }
            double lon =
                probeLocation.getLongitude().getValue(CommonUnit.degree);
            double lat =
                probeLocation.getLatitude().getValue(CommonUnit.degree);
            probe.setPosition(
                new RealTuple(
                    RealTupleType.SpatialEarth3DTuple, new double[] { lon,
                    lat, 0 }));

            //            probe.setPoint(new RealTuple(RealTupleType.SpatialEarth3DTuple,
            //                                            new double[] {lon,
            //                                                          lat,0}));

            probe.setVisible(true);



            if (latLonWidget != null) {
                latLonWidget.setLat(
                    getDisplayConventions().formatLatLon(
                        probeLocation.getLatitude().getValue(
                            CommonUnit.degree)));

                latLonWidget.setLon(
                    getDisplayConventions().formatLatLon(
                        probeLocation.getLongitude().getValue(
                            CommonUnit.degree)));


            }
            Misc.run(this, "doAnalysis");

        } catch (Exception e) {
            logException("Handling probe changed", e);
        }
    }


    /**
     * _more_
     */
    public void doAnalysis() {
        if (probeLocation == null) {
            return;
        }
        //Put your stuff here
    }


    /**
     * Map the screen x/y of the event to an earth location
     *
     * @param event The event
     *
     * @return The earth location
     *
     * @throws java.rmi.RemoteException When bad things happen
     * @throws visad.VisADException When bad things happen
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public EarthLocation toEarth(DisplayEvent event)
            throws VisADException, RemoteException {
        NavigatedDisplay d = getNavigatedDisplay();
        return (d == null)
               ? null
               : d.getEarthLocation(toBox(event));
    }


    /**
     * _more_
     *
     * @param el _more_
     */
    public void addADOT(EarthLocation el) {
        final JDialog    dialog   = GuiUtils.createDialog("RUN ADOT", true);
        String           question = "Please select storm center";
        String           label    = "latitude: ";
        String           label1   = "longitude: ";
        final JTextField field    = new JTextField("", 10);
        final JTextField field1   = new JTextField("", 10);


        ObjectListener   listener = new ObjectListener(new Boolean(false)) {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if ((ae.getSource() == field)
                        || cmd.equals(GuiUtils.CMD_OK)) {
                    theObject = new Boolean(true);
                } else {
                    theObject = new Boolean(false);
                }
                dialog.setVisible(false);
            }
        };
        ObjectListener listener1 = new ObjectListener(new Boolean(false)) {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if ((ae.getSource() == field1)
                        || cmd.equals(GuiUtils.CMD_OK)) {
                    theObject = new Boolean(true);
                } else {
                    theObject = new Boolean(false);
                }
                dialog.setVisible(false);
            }
        };
        field.addActionListener(listener);
        field.addActionListener(listener1);
        List comps = new ArrayList();

        comps.add(GuiUtils.left(GuiUtils.inset(new JLabel(question), 4)));

        JPanel topb = GuiUtils.doLayout(new Component[] {
                          GuiUtils.rLabel(label),
                          GuiUtils.hbox(field, GuiUtils.filler()),
                          GuiUtils.rLabel(label1),
                          GuiUtils.hbox(field1, GuiUtils.filler()) }, 4,
                              GuiUtils.WT_NYNY, GuiUtils.WT_N);

        comps.add(topb);


        JComponent contents =
            GuiUtils.inset(GuiUtils.centerBottom(GuiUtils.vbox(comps),
                GuiUtils.makeOkCancelButtons(listener1)), 4);

        GuiUtils.packDialog(dialog, contents);
        Dimension ss  = Toolkit.getDefaultToolkit().getScreenSize();

        Point     ctr = new Point(ss.width / 2 - 100, ss.height / 2 - 100);
        dialog.setLocation(ctr);
        dialog.setVisible(true);


    }

    /**
     * Set the ProbeLocation property.
     *
     * @param value The new value for ProbeLocation
     */
    public void setProbeLocation(LatLonPoint value) {
        probeLocation = value;
    }

    /**
     * Get the ProbeLocation property.
     *
     * @return The ProbeLocation
     */
    public LatLonPoint getProbeLocation() {
        return probeLocation;
    }




}

