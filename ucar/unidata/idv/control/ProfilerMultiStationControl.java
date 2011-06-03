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
import ucar.unidata.data.profiler.*;
import ucar.unidata.idv.ControlContext;



import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.DisplayInfo;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Range;

import ucar.visad.display.Displayable;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.LineProbe;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.WindBarbDisplayable;

import visad.*;

import visad.data.units.Parser;

import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;

import visad.util.DataUtility;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;

import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Given an earth-located VisAD Field of multi-station NOAA Profiler data,
 * make a mapped wind data display, and make related controls.
 *
 * Uses FieldImpl with VisAD function (Time -> ((LAT,LON,Z) -> (DIR, SPD))
 * where time values are
 * VisAD Datetime objects, Z has RealType RealType.Altitude, DIR and
 * SPD have RealType Display.Flow1Azimuth and Display.Flow1Radial,
 *
 * The data display is in the main IDV 3D view manager.
 *
 * @author Unidata IDV development
 * @version $Revision: 1.44 $
 */
public class ProfilerMultiStationControl extends ProfilerControl {


    /**
     *  The isPlanView property.
     */
    private boolean isPlanView = true;

    /** vertical interval */
    private Real currentVerticalInt = new Real(125.0);

    /** raw data and working data */
    private FieldImpl fieldImpl, workingFI;

    /** flag for a time sequence of data */
    private boolean isSequence = false;

    /** current level */
    private float currentLevel;

    /** level */
    private float levelValue = 17000.0f;

    /** level */
    private float initLevelValue = 3000.0f;

    /** displayable for the data depiction */
    private FlowDisplayable mappedDisplayable;

    /** flag for a 3D display */
    private boolean displayIs3D = false;

    /**
     *  Default constructor; does nothing. See init() for creation actions.
     */
    public ProfilerMultiStationControl() {}

    /**
     * Construct the Displayable and controls; get and load data in display.
     *
     * @param dataChoice the DataChoice to use
     *
     * @return boolean true if DataChoice is ok.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        if ( !setData(dataChoice)) {
            return false;
        }

        // get range type of data to tell the displyable
        FunctionType ftype = (FunctionType) fieldImpl.getType();


        // ftype should be (Time -> ((LAT,LON,Z) -> (DisplayFlow1Azimuth, 
        //                                       DisplayFlow1Radial)))
        FunctionType obFT = (FunctionType) ftype.getRange();
        // obFT should be (LAT,LON,Z)
        //                    -> (DisplayFlow1Azimuth, DisplayFlow1Radial)))
        RealTupleType rtt = (RealTupleType) obFT.getFlatRange();

        mappedDisplayable = new WindBarbDisplayable("profiler", rtt, true);



        // add an associated color table item in the gui, a pull down menu
        addAttributedDisplayable(mappedDisplayable, FLAG_COLORTABLE);

        mappedDisplayable.setBarbOrientation(FlowControl.NH_ORIENTATION);

        mappedDisplayable.setVisible(true);

        addDisplayable(mappedDisplayable);

        displayIs3D = isDisplay3D();

        // check and set data level
        while ( !checkDataLevelValue(levelValue) ) {
            levelValue         = levelValue *0.9f;
            currentVerticalInt = new Real(levelValue / 25.0);
        }

        if (levelValue < 5000) {
            initLevelValue = 1000;
        }

        loadData();

        /*
        // put the data to display in the displayable;
        // if 3D plot, put all the data at all levels in the display;
        // if plan view, set initial level at 3000 m above MSL
        if (getIsPlanView()) {
            //System.out.println (" resetDataVerticalLocation to level 3000");
            resetDataVerticalLocation(new Real(0.0), 3000.0f, false);
            mappedDisplayable.loadData(workingFI);
        } else {
            //System.out.println (" make full 3d view");
            mappedDisplayable.loadData(fieldImpl);
        }
        */

        return true;
    }

    /**
     * check the input zlevel is not too high for the obs data point
     *
     * @param zlevel _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public boolean checkDataLevelValue(float zlevel)
            throws VisADException, RemoteException {

        // internal working "grids"
        FlatField oneTimeFF;

        float     interval = (float) currentVerticalInt.getValue();

        // or will use desired level limits for plan view
        float zmin    = zlevel - interval,
              zmax    = zlevel + interval;

        Set   timeSet = fieldImpl.getDomainSet();

        // extract the multi-position FlatField of obs for each time step
        for (int i = 0; i < timeSet.getLength(); i++) {
            // get Profiler winds  for this one time step
            oneTimeFF = (FlatField) fieldImpl.getSample(i);

            // for each ob in this time's collection
            for (int j = 0; j < oneTimeFF.getLength(); j++) {

                Gridded3DSet tds     =
                    (Gridded3DSet) oneTimeFF.getDomainSet();
                float[][]    latlonz = tds.getSamples(false);
                // test if this z altitude value matches either spacings
                // for 3D plot; or single level value for Plan View.
                if ((latlonz[2][j] >= zmin) && (latlonz[2][j] <= zmax)) {
                    return true;

                }  // if this ob's altitude is ok, save it
            }      // end checking each ob in this oneTimeFF
            return false;
        }
        return false;
    }

    /**
     * Load the data
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException  error loading data
     */
    public void loadData() throws VisADException, RemoteException {
        // put the data to display in the displayable;
        // if 3D plot, put all the data at all levels in the display;
        // if plan view, set initial level at 3000 m above MSL
        if (getIsPlanView()) {
            //System.out.println (" resetDataVerticalLocation to level 3000");
            resetDataVerticalLocation(new Real(0), initLevelValue, false);
            mappedDisplayable.loadData(workingFI);
        } else {
            //System.out.println (" make full 3d view");
            mappedDisplayable.loadData(fieldImpl);
        }
    }

    /**
     * A utility to cast the getDataInstance as a GridDataInstance
     *
     * @return the GridDataInstance
     */
    public GridDataInstance getGridDataInstance() {
        return (GridDataInstance) getDataInstance();
    }


    /**
     * Creates and returns the {@link ucar.unidata.data.grid.GridDataInstance}
     * corresponding to a {@link ucar.unidata.data.DataChoice}. Returns
     * <code>null</code> if the {@link ucar.unidata.data.DataInstance} was
     * somehow invalid.
     *
     * @param dataChoice       The {@link ucar.unidata.data.DataChoice}
     *                         from which to create a
     *                         {@link ucar.unidata.data.DataInstance}.
     *
     * @return                 The created
     *                         {@link ucar.unidata.data.DataInstance} or
     *                         <code>null</code>.
     *
     * @throws VisADException  if a VisAD Failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        return new GridDataInstance(dataChoice, null, getRequestProperties());
    }


    /**
     * Get FieldImpl with data to display, from the DataChoice.
     *
     * @param dataChoice the DataChoice to use
     *
     * @return true if DataChoice is ok.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !super.setData(dataChoice)) {
            return false;
        }
        fieldImpl = getGridDataInstance().getGrid();
        if (mappedDisplayable != null) {
            loadData();
        }
        return true;
    }



    /**
     * Make extra component for UI
     * @return extra UI component
     */
    protected JComponent doMakeExtraComponent() {
        if (getIsPlanView()) {
            JComboBox box = GuiUtils.createValueBox(this, CMD_LEVEL,
                                (int) initLevelValue,
                                Misc.createIntervalList(500,
                                    (int) levelValue, 500), true);
            return GuiUtils.label("Plan level (m MSL): ", GuiUtils.wrap(box));
        } else {
            return doMakeVerticalIntervalComponent();
        }
    }



    /**
     *  Set the IsPlanView property.
     *
     *  @param value The new value for IsPlanView
     */
    public void setIsPlanView(boolean value) {
        isPlanView = value;
    }

    /**
     *  Get the IsPlanView property.
     *
     *  @return The IsPlanView property
     */
    public boolean getIsPlanView() {
        return isPlanView;
    }



    /**
     * Override the base class method to catch any events.
     *
     *  @param e The action event.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            if (cmd.equals(CMD_LEVEL)) {
                setLevel(GuiUtils.getBoxValue((JComboBox) e.getSource()));
            } else if (cmd.equals(CMD_INTERVAL)) {
                setVerticalInterval(
                    GuiUtils.getBoxValue((JComboBox) e.getSource()));
            } else {
                super.actionPerformed(e);
            }
        } catch (NumberFormatException nfe) {
            userErrorMessage("Incorrect number format");
        }

    }





    /**
     * Set the value of the level.
     *
     * @param value the level.
     */
    public void setLevel(float value) {
        levelValue = value;

        if (mappedDisplayable != null) {
            try {
                resetDataVerticalLocation(new Real(0.0), levelValue, false);
                // put the resampled data in the displayable
                mappedDisplayable.loadData(workingFI);
            } catch (Exception ve) {
                logException("setLevel", ve);
            }
        }
    }


    /**
     *  Get the value of the level.
     *  @return the level.
     */
    public float getLevel() {
        return levelValue;
    }


    /**
     * Change vertical separation of wind barbs in meters,
     * to a different vertical interval.
     * Used for 3D Views of Profiler wind - not for plan view(station plot)
     *
     * @param value a float the vertical separation in meters
     */
    public void setVerticalInterval(float value) {
        super.setVerticalInterval(value);
        if ( !isPlanView && (mappedDisplayable != null)) {
            try {
                //System.out.println ("setVerticalInterval:" + value);
                resetDataVerticalLocation(new Real(value), 0.0f, true);
                // put the resampled data in the displayable
                mappedDisplayable.loadData(workingFI);
            } catch (Exception ve) {
                logException("setVerticalInterval", ve);
            }
        }
    }




    /**
     * Sample the data grid for values at the vertical interval or spacing
     * set by input value;
     * load that possibly-reduced data in the displayable.
     *
     * If use3D is true, show data which are near the spacing given by
     * (float) verticalInt.getValue()
     *
     * If use3D is false, show data only at zLevel or within 125 meters of it
     *
     * @param verticalInt sampling interval such as 500 m; IF use3D==true
     * @param zlevel - plan view level of winds from Profiler; IF use3D false
     * @param use3D boolean true for all data in 3d; false for plan view
     * at one level above MSL only.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void resetDataVerticalLocation(Real verticalInt, float zlevel,
                                          boolean use3D)
            throws VisADException, RemoteException {

        // do nothing if no verticalInt is available, 
        // or no change in verticalInt

        // internal working "grids"
        FlatField oneTimeFF, newFF;

        // lists of locations and dir-spd wind obs at the locations
        ArrayList locList = new ArrayList();
        ArrayList dsList  = new ArrayList();
        if (verticalInt.getValue() != 0.0) {
            currentVerticalInt = verticalInt;
        }
        currentLevel = zlevel;
        float interval = (float) currentVerticalInt.getValue();

        // complete data's fieldImpl is of function type 
        // (DateTime -> ((lat,lon,z)->(dir,spd)) )

        // get desired spacing interval for 3D plot
        float spacing = (float) verticalInt.getValue();

        // or will use desired level limits for plan view
        float zmin    = zlevel - interval,
              zmax    = zlevel + interval;

        Set   timeSet = fieldImpl.getDomainSet();

        // extract the multi-position FlatField of obs for each time step
        for (int i = 0; i < timeSet.getLength(); i++) {
            // get Profiler winds  for this one time step
            oneTimeFF = (FlatField) fieldImpl.getSample(i);

            locList.clear();
            dsList.clear();

            // for each ob in this time's collection
            for (int j = 0; j < oneTimeFF.getLength(); j++) {

                Gridded3DSet tds = (Gridded3DSet) oneTimeFF.getDomainSet();

                // get big array of all lat lon altitude values that
                // are the domain of this field
                float[][] latlonz = tds.getSamples(false);

                // test if this z altitude value matches either spacings
                // for 3D plot; or single level value for Plan View.
                if ((use3D && ((latlonz[2][j] + interval)
                        % spacing <= interval || (latlonz[2][j]
                            % spacing) <= interval)) || ( !use3D && (latlonz[2][j] >= zmin) && (latlonz[2][j] <= zmax))) {
                    // have an ob within 124 meters of the desired level
                    // or spacing altitude; retain and display this observation

                    RealTuple location = new RealTuple(new Real[] {
                                             new Real(latlonz[0][j]),
                                             new Real(latlonz[1][j]),
                                             new Real(latlonz[2][j]), });
                    locList.add(location);

                    RealTuple dirspd = (RealTuple) oneTimeFF.getSample(j);
                    dsList.add(dirspd);

                }  // if this ob's altitude is ok, save it

            }      // end checking each ob in this oneTimeFF


            // make range array of dir-spd
            Data[] ds = new RealTuple[dsList.size()];
            for (int jj = 0; jj < dsList.size(); jj++) {
                ds[jj] = (RealTuple) dsList.get(jj);
            }

            if (ds.length == 0) {
                //System.err.println("  no profiler data at level "+zlevel);
                return;
            }

            //make the FunctionType(MathType domain, MathType range) 
            FunctionType newonetimeFT =
                new FunctionType(RealTupleType.LatitudeLongitudeAltitude,
                                 ds[0].getType());
            //   sortedDS[0].getType());

            // do cstr FlatField(FunctionType type, Set domain_set)

            // make new Gridded3DSet of the accepted level locations
            int       numPoints = locList.size();
            float[][] points    = new float[3][numPoints];
            int       curPoint  = 0;
            while (curPoint < numPoints) {
                points[0][curPoint] =
                    (float) ((Real) ((RealTuple) locList.get(
                        curPoint)).getComponent(0)).getValue();
                points[1][curPoint] =
                    (float) ((Real) ((RealTuple) locList.get(
                        curPoint)).getComponent(1)).getValue();
                points[2][curPoint] =
                    (float) ((Real) ((RealTuple) locList.get(
                        curPoint)).getComponent(2)).getValue();
                curPoint++;
            }
            Gridded3DSet locset =
                new Gridded3DSet(RealTupleType.LatitudeLongitudeAltitude,
                                 points, numPoints);

            FlatField newonetimeFF = new FlatField(newonetimeFT, locset);

            newonetimeFF.setSamples(ds /*sortedDS*/, false);

            //System.out.println("   profiler time index "+i+
            //                 " has "+newonetimeFF.getLength()+
            //                 " observed wind values in this plot");

            if ((i == 0) || (workingFI == null)) {
                // define working grid, as yet empty of data)
                workingFI = new FieldImpl(
                    new FunctionType(
                        ((SetType) timeSet.getType()).getDomain(),
                        ((newonetimeFF).getType())), timeSet);
            }
            // set data, this collection of 
            // ok levels' FlatFields, into the time sequence
            workingFI.setSample(i, newonetimeFF);

        }  // end loop on timeset by i

        return;
    }


    /**
     * Set the length of the wind barb
     * @param value the length of the wind barb
     */
    public void setFlowScale(float value) {
        super.setFlowScale(value);
        if (mappedDisplayable != null) {
            //System.err.println ("flowScale:" + value);
            mappedDisplayable.setFlowScale(value * scaleFactor);
        }
    }


    /**
     * Get the initial range for the color table
     * @return  the initial range
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
        Range range = getDisplayConventions().getParamRange(paramName,
                          displayUnit);
        if (range == null) {
            range = getRangeFromColorTable();
        }

        if (range == null) {
            return new Range(0, 100);
        }
        return range;
    }


    /**
     * Get the MapProjection for this data; if have a single point data object
     * make synthetic map projection for location
     * @return MapProjection  for the data
     */
    public MapProjection getDataProjection() {
        MapProjection mp = null;

        try {
            Data data = fieldImpl;  //info.getDisplayable().getData();
            if ((data != null) && (data instanceof FieldImpl)) {
                try {
                    mp = GridUtil.getNavigation(
                        GridUtil.getWholeSpatialDomain((FieldImpl) data));
                } catch (Exception e) {
                    mp = null;
                }

            }
        } catch (Exception e) {
            logException("Getting projection from data", e);
        }

        if (mp == null) {
            return null;
        }

        Rectangle2D.Float r2d    = (Rectangle2D.Float) mp.getDefaultMapArea();
        float             height = r2d.height;
        float             width  = r2d.width;
        float             xx     = r2d.x;
        float             yy     = r2d.y;

        if ((height == 0) && (width == 0)) {
            // data MapProjection has 0 width and height, so you 
            // have a single point data object;
            // make synthetic map projection for location
            // centered on the point, 2 geographic degrees wide.
            try {
                width  = 2.0f;  // units geographic degrees
                height = 2.0f;
                mp = new TrivialMapProjection(
                    RealTupleType.SpatialEarth2DTuple,
                    new Rectangle2D.Float(
                        xx - 1.0f, yy - 1.0f, width, height));
            } catch (Exception e) {
                logException(" getDataProjection", e);
            }
        }

        return mp;
    }


    /**
     * Converts the given lat/lon location to a RealTuple
     * of VisAD x,y coords.
     * Used intially to display location of Profiler data, on map view.
     *
     * @param rlat   latitude (degrees)
     * @param rlon   longitude (degrees)
     * @return  XY position in display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private RealTuple getRealTupleForPoint(float rlat, float rlon)
            throws VisADException, RemoteException {
        RealTuple visadTup = earthToBoxTuple(new EarthLocationTuple(rlat,
                                 rlon, 0.0));
        Real[] reals = visadTup.getRealComponents();

        // reset VisAD altitude Real value from 0.0 to 
        // 1.0 to put point at top of visad box (use -.95 for bottom)
        Real altreal = new Real(((RealType) (reals[2]).getType()), 1.0);
        // RealTuple (new Real[] {reals[0], reals[1], altreal});

        // LineProbe can handle only x,y VisAD coords, not x,y,z
        return new RealTuple(new Real[] { reals[0], reals[1] });
    }

}
