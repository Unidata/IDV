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

import ucar.unidata.data.radar.RadarConstants;

import visad.*;

import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;




/**
 * Class for controlling the display of CAPPIs of WSR-88D Level II data
 *
 * @author IDV Development Team
 * @version $Revision: 1.9 $
 */
public class CappiControl extends ColorPlanViewControl {

    /** levels of cappis; used in level selector box */
    private static Real[] cappiLevels = null;


    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public CappiControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT);
    }

    /**
     * Make sure the labels all get set.
     */
    public void initDone() {
        super.initDone();
        updateLabels();
    }

    /**
     * Get (and make if necessary)
     * the requester Hastable of properties that is carried along with
     * the dataInstance; sets cappi level.
     * @return  Hashtable of request properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        if (currentLevel == null) {
            currentLevel = getCappiLevels()[0];
        }
        props.put(RadarConstants.PROP_CAPPI_LEVEL, currentLevel);
        return props;
    }

    /**
     * Set the levels for this controls
     *
     * @param levels  array of levels
     */
    public void setLevels(Object[] levels) {
        super.setLevels(getCappiLevels());
    }

    /**
     * Don't show the z selector
     *
     * @return  false
     */
    protected boolean shouldShowZSelector() {
        return false;
    }



    /**
     * Load data at the level specified.  Overrides the superclass
     * to update the legend label.
     *
     * @param level  level to load at
     *
     * @throws  VisADException  illegal level or other VisAD error
     * @throws  RemoteException  RMI error
     */
    public void loadDataAtLevel(Object level)
            throws VisADException, RemoteException {
        super.loadDataAtLevel(level);
        //if (!getHaveInitialized()) return;
        updateLabels();
    }

    /**
     * Update the labels.
     */
    private void updateLabels() {
        setLevelReadoutLabel("Current level: " + formatLevel(currentLevel));
        updateLegendAndList();
    }


    /**
     * A utility to create the fixed levels
     *
     * @return The levels to use
     */
    private static Real[] getCappiLevels() {
        if (cappiLevels == null) {
            try {
                double[] levels = {
                    1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000,
                    10000, 12000, 14000, 16000
                };
                cappiLevels = new Real[levels.length];
                for (int i = 0; i < levels.length; i++) {
                    cappiLevels[i] = new Real(RealType.Altitude, levels[i],
                            CommonUnit.meter);
                }
            } catch (VisADException ve) {}
        }
        return cappiLevels;
    }


    /**
     * Override the super class to set the initial level
     *
     * @param dataChoice  the data choice to use
     *
     * @return  the DataInstance
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   unable to create the VisAD object
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {

        if (currentLevel == null) {
            currentLevel = getDataSelection().getFromLevel();
            if (currentLevel == null) {
                currentLevel = getCappiLevels()[0];
            }
        }
        getDataSelection().setLevel(currentLevel);
        return super.doMakeDataInstance(dataChoice);
    }

    /**
     * Get the data projection label
     *
     * @return  the data projection label
     */
    protected String getDataProjectionLabel() {
        return "Use Radar Projection";
    }


}
