/*
 * $Id: ThreeDSurfaceControl.java,v 1.106 2007/08/21 11:32:08 jeffmc Exp $
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



package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;

import ucar.unidata.idv.DisplayConventions;

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

    /** _more_          */
    private FieldImpl field;

    /** _more_          */
    JTabbedPane tabbedPane = new JTabbedPane();

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
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void createTables() throws VisADException, RemoteException {
        tabbedPane.removeAll();
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
                FlatFieldTable table = new FlatFieldTable(ff);
                tabbedPane.addTab("" + dt, new JScrollPane(table));
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        createTables();
        return tabbedPane;
        //        return new JLabel("");
    }





}

