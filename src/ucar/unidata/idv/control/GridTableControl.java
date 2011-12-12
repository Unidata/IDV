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


import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;

import ucar.unidata.util.ColorTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;


import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;

import ucar.visad.display.Grid3DDisplayable;

import ucar.visad.display.GridDisplayable;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 * A MetApps Display Control with Displayable and controls for
 * one 3D isosurface display of one parameter.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.106 $
 */

public class GridTableControl extends GridDisplayControl {

    /** The flat field */
    private FieldImpl field;

    /** gui component */
    private GuiUtils.CardLayoutPanel cardLayoutPanel =
        new GuiUtils.CardLayoutPanel();

    /** gui component */
    private JCheckBox nativeCoordsCbx;

    /** show native coordinates */
    private boolean showNativeCoordinates = false;

    /**
     * Default constructor; does nothing.  See init() for class initialization
     */
    public GridTableControl() {}


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        super.init(dataChoice);
        setData(dataChoice);
        return true;
    }



    /**
     * Set the data in the display control from the data choice
     *
     * @param choice   choice describing data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.setData(choice)) {
            return false;
        }
        field = getGridDataInstance().getGrid();
        return true;
    }



    /**
     * Create the tables
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void createTables() throws VisADException, RemoteException {
        List dates = new ArrayList();
        cardLayoutPanel.removeAll();
        if (GridUtil.isTimeSequence(field)) {
            SampledSet timeSet  = (SampledSet) GridUtil.getTimeSet(field);
            double[][] times    = timeSet.getDoubles(false);
            Unit       timeUnit = timeSet.getSetUnits()[0];
            int        numTimes = timeSet.getLength();
            for (int timeIdx = 0; timeIdx < numTimes; timeIdx++) {
                DateTime  dt = new DateTime(times[0][timeIdx], timeUnit);
                FlatField ff = (FlatField) field.getSample(timeIdx);
                if (ff == null) {
                    continue;
                }
                dates.add(dt);
                FlatFieldTable table = new FlatFieldTable(ff,
                                           showNativeCoordinates);
                table.setDefaultRenderer(Number.class,
                                         new TableNumberCellRenderer());
                cardLayoutPanel.addCard(new JScrollPane(table));
            }
        } else {
            FlatFieldTable table = new FlatFieldTable((FlatField) field,
                                       showNativeCoordinates);
            table.setDefaultRenderer(Number.class,
                                     new TableNumberCellRenderer());
            cardLayoutPanel.addCard(new JScrollPane(table));
        }
        setAnimationSet(dates);
    }


    /**
     * handle the animation time change
     *
     * @param time the new time
     */
    public void timeChanged(Real time) {
        try {
            super.timeChanged(time);
            int current = getInternalAnimation(null).getCurrent();
            if (current >= 0) {
                cardLayoutPanel.show(current);
            }
        } catch (Exception exc) {
            logException("Time changed", exc);
        }
    }

    /**
     * Make the gui
     *
     * @return the gui
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        createTables();
        nativeCoordsCbx = new JCheckBox("Show native coordinates",
                                        getShowNativeCoordinates());
        nativeCoordsCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showNativeCoordinates = nativeCoordsCbx.isSelected();
                try {
                    createTables();
                } catch (Exception exc) {
                    logException("Creating tables", exc);
                }
            }
        });
        return GuiUtils.topCenter(
            GuiUtils.leftRight(
                getAnimationWidget().getContents(),
                nativeCoordsCbx), cardLayoutPanel);
    }


    /**
     *  Set the ShowNativeCoordinates property.
     *
     *  @param value The new value for ShowNativeCoordinates
     */
    public void setShowNativeCoordinates(boolean value) {
        showNativeCoordinates = value;
    }

    /**
     *  Get the ShowNativeCoordinates property.
     *
     *  @return The ShowNativeCoordinates
     */
    public boolean getShowNativeCoordinates() {
        return showNativeCoordinates;
    }


}
