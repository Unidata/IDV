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


import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.radar.RadarConstants;

import ucar.unidata.metdata.NamedStation;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;



import ucar.visad.display.Displayable;
import ucar.visad.display.LineProbe;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.WindBarbDisplayable;

import visad.*;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.util.DataUtility;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.Hashtable;

import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.*;


/**
 * for WSR-88D Level II VAD wind profile (VWP)
 *
 * @author Unidata IDV development
 * @version $Revision: 1.14 $
 */
public class RadarVWPControl extends WindTimeHeightControl {

    /** station name */
    private String stationName;

    /**
     *  Default constructor; does nothing. See init() for creation actions.
     */
    public RadarVWPControl() {}

    /**
     * Construct the DisplayMaster, Displayable, frame, and controls.
     *
     * @param dataChoice the DataChoice to use
     * @return true if DataChoice is ok.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        boolean result = super.init(dataChoice);

        if ( !result) {
            return result;
        }

        EarthLocationTuple elt =
            (EarthLocationTuple) boxToEarth(new double[] { 0.0,
                0.0, -1.0 });
        LatLonPoint llp     = elt.getLatLonPoint();
        int         nameLen = Math.min(3, dataChoice.getName().length());
        stationName = dataChoice.getName().substring(0, nameLen);
        if (llp != null) {
            latitude  = (float) elt.getLatitude().getValue();
            longitude = (float) elt.getLongitude().getValue();
        }
        return result;
    }

    /**
     * Initialize the data.
     *
     * @param dataChoice  choice for the data
     * @return   true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean initStation(DataChoice dataChoice)
            throws VisADException, RemoteException {
        super.initStation(dataChoice);

        // start new thread to load data in display
        Misc.run(new Runnable() {
            public void run() {
                showWaitCursor();
                try {

                    fieldImpl      = (FieldImpl) getDataInstance().getData();
                    haveNewStation = true;
                    if (latitude >= 0.0) {
                        // set NWS wind barb style to that used in
                        // the northern hemisphere
                        wbDisplayable.setBarbOrientation(
                            FlowControl.NH_ORIENTATION);
                    } else {
                        wbDisplayable.setBarbOrientation(
                            FlowControl.SH_ORIENTATION);
                    }
                    setLabel();
                    if (wbDisplayable != null) {
                        wbDisplayable.loadData(fieldImpl);
                    }
                    setXAxisValues(fieldImpl.getDomainSet());
                } catch (Exception excp) {
                    logException(" load wind vectors ", excp);
                }
                showNormalCursor();
                haveNewStation = false;
            }
        });

        return true;
    }

    /**
     * Get the request properties associated with this control
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        props.put(RadarConstants.PROP_VWP, new Float(0.0));
        return props;
    }

    /**
     * Make widgets for check box for latest data time on left of x axis
     * @param controlWidgets list of widgets to fill
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        JCheckBox toggle = new JCheckBox("", isLatestOnLeft);
        toggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isLatestOnLeft = ((JCheckBox) e.getSource()).isSelected();
                try {
                    setXAxisValues();
                } catch (VisADException ve) {
                    userMessage("Couldn't set order");
                }
            }
        });
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Latest data on left: "),
                GuiUtils.leftCenter(toggle, GuiUtils.filler())));

    }

    /**
     * Set the label for this display
     */
    protected void setLabel() {
        if (plotLabel != null) {
            plotLabel.setText(stationName + "  Lat " + latitude + "  Long "
                              + longitude);
        }
    }

    /**
     * Get the title for this display
     * @return  title text
     */
    protected String getTitle() {
        return stationName + " VAD Wind Profile";
    }
}
