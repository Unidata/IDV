/*
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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


import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
//import ucar.nc2.rewrite.Rewrite;
import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.radar.CDMRadarDataSource;
import ucar.unidata.data.radar.RadarConstants;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.metdata.NamedStation;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.RadarMapProjection;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;

import visad.*;

import visad.bom.Radar2DCoordinateSystem;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;


import java.awt.Component;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;



/**
 * Class for making and controlling the display of color shaded plots of
 * WSR-88D Level II radar sweeps in 2D or in 3D.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.40 $
 */
public class RadarSweepControl extends ColorPlanViewControl {

    /** property for sharing angles */
    public static final String SHARE_ANGLE = "RadarSweepControl.SHARE_ANGLE";

    /** label for station information */
    private JLabel stationLabel = new JLabel("   ");

    /**
     *  When we have angles this holds the current angle.
     */
    private double currentAngle = -1.0;

    /**
     *  Do we request 3d or 2d data.
     */
    private boolean use3D = true;

    /**
     * Default constructor.
     */
    public RadarSweepControl() {}

    /**
     * Set the data in the control.
     *
     * @param choice   data choice description
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {

        Hashtable props = choice.getProperties();

        if ( !super.setData(choice)) {
            return false;
        }

        NamedStation ns = null;
        if (props != null) {
            ns = (NamedStation) props.get(RadarConstants.STATION_LOCATION);
        }
        int    shortNameLen = Math.min(3, choice.getName().length());
        String shortName    = choice.getName().substring(0, shortNameLen);
        // set location label
        if (ns != null) {
            stationLabel.setText(
                ns.getIdentifier() + " "
                + getDisplayConventions().formatEarthLocation(
                    (EarthLocation) ns.getNamedLocation(), true));
        } else {
            stationLabel.setText(shortName);
        }
        updateLegendAndList();
        return true;
    }

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

        if (use3D && !isDisplay3D()) {
            userMessage("Can't display 3D sweep in 2D display");
            return false;
        }
        if(dataChoice.getName().startsWith("ucar.unidata.util"))
            dataChoice.setName(dataChoice.getDescription());
        return super.init(dataChoice);
    }

    /**
     * Override the base class method that creates request properties
     * and add in the appropriate 2d/3d request parameter.
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        props.put(RadarConstants.PROP_2DOR3D, use3D
                ? RadarConstants.VALUE_3D
                : RadarConstants.VALUE_2D);
        props.put(RadarConstants.PROP_VOLUMEORSWEEP,
                  RadarConstants.VALUE_SWEEP);
        return props;
    }


    /**
     * If we have a volume then we'll add in an angles choosing combobox.
     *
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Station:"),
                                             stationLabel));
    }


    /**
     * We got a new angle or sweep tilt - from the user.
     *
     * @param newAngle   new sweep angle
     */
    private void applyNewAngle(double newAngle) {

        if (newAngle != currentAngle) {
            currentAngle = newAngle;
            getRequestProperties().put(RadarConstants.PROP_ANGLE,
                                       Double.valueOf(currentAngle));
            updateLegendAndList();
            try {
                resetData();
                doShare(SHARE_ANGLE, Double.valueOf(currentAngle));
            } catch (Exception exc) {
                logException("Getting new angle", exc);
            }
        }
    }

    /**
     * Method called by other classes that share the selector.
     *
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if ( !getHaveInitialized()) {
            return;
        }
        if (dataId.equals(SHARE_ANGLE)) {
            try {
                applyNewAngle(((Double) data[0]).doubleValue());
            } catch (Exception exc) {
                logException("receiveShareData.angle", exc);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }

    /**
     * Overwrite base class method to determine if the display and
     * gui should have a Z position.
     *
     * @return Should use z position
     */
    protected boolean shouldUseZPosition() {
        return !use3D;
    }


    /**
     * Override the base class method to include the station name,
     * param type, and the elevation angle
     * This is used to label the display legend and the gui box title
     *
     * @param labels List to add labels to
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        if (currentAngle >= 0.0) {
            labels.add((use3D
                        ? "3D"
                        : "2D") + " sweep, elev "
                                + getDisplayConventions().formatAngle(
                                    currentAngle));
        } else {
            labels.add((use3D
                        ? "3D"
                        : "2D") + " sweep");
        }
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
                List levelsList = dataChoice.getAllLevels(getDataSelection());
                if ((levelsList != null) && (levelsList.size() > 0)) {
                    currentLevel = levelsList.get(0);
                }
            }
        }
        getDataSelection().setLevel(currentLevel);
        return super.doMakeDataInstance(dataChoice);
    }


    /**
     * Dont show the z selector
     *
     * @return false
     */
    protected boolean shouldShowZSelector() {
        return false;
    }


    /**
     *  Set the Use3D property.
     *
     *  @param value The new value for Use3D
     */
    public void setUse3D(boolean value) {
        use3D = value;
    }

    /**
     *  Get the Use3D property.
     *
     *  @return The Use3D
     */
    public boolean getUse3D() {
        return use3D;
    }

    /**
     *  Set the CurrentAngle property.
     *
     *  @param value The new value for CurrentAngle
     */
    public void setCurrentAngle(double value) {
        currentAngle = value;
    }

    /**
     *  Get the CurrentAngle property.
     *
     *  @return The CurrentAngle
     */
    private double getCurrentAngle() {
        return currentAngle;
    }

    /**
     * Get whether we can smooth this display
     *
     * @return false
     */
    public boolean getAllowSmoothing() {
        return false;
    }

    /**
     * Get the label for the levels box.
     * @return the label
     */
    public String getLevelsLabel() {
        return "Elevation Angles:";
    }

    /**
     * Get the data projection label
     *
     * @return  the data projection label
     */
    protected String getDataProjectionLabel() {
        return "Use Radar Projection";
    }
    /**
     * Get the cursor data
     *
     * @param el  earth location
     * @param animationValue   the animation value
     * @param animationStep  the animation step
     * @param samples the list of samples
     *
     * @return  the list of readout data
     *
     * @throws Exception  problem getting the data
     */
    protected List getCursorReadoutInner11(EarthLocation el,
                                         Real animationValue,
                                         int animationStep,
                                         List<ReadoutInfo> samples)
            throws Exception {
        if (currentSlice == null) {
            return null;
        }       
        RadarMapProjection rp = (RadarMapProjection) getDataProjection();
        LatLonPoint radarLocation = rp.getCenterLatLon();
        FlatField d = (FlatField)currentSlice.getSample(0);
        Gridded2DSet ds = (Gridded2DSet)d.getDomainSet();
        Radar2DCoordinateSystem rcoord = (Radar2DCoordinateSystem)d.getDomainCoordinateSystem();
        float [] center = rcoord.getCenterPoint();
        float [] values = d.getFloats()[0];
        float   lat1        = (float) el.getLatitude().getValue();
        float   lon1        = (float) el.getLongitude().getValue();
         
        Bearing b1          = getBearing(radarLocation, lat1, lon1);         
        double  azimuth1    = b1.getAngle();
        double  range1      = b1.getDistance(); 
        double [][] pvalues = new double[2][1];
        pvalues[0][0] = range1;
        pvalues[1][0] = azimuth1;
        int [] pidx = ds.doubleToIndex(pvalues);
        currentSlice.getSample(0);
        List result = new ArrayList();
        Real r = GridUtil.sampleToReal(
                currentSlice, el, animationValue,
                getSamplingModeValue(
                        getObjectStore().get(
                                PREF_SAMPLING_MODE, DEFAULT_SAMPLING_MODE)));
        if (r != null) {
            ReadoutInfo readoutInfo = new ReadoutInfo(this, r, el,
                    animationValue);
            readoutInfo.setUnit(getDisplayUnit());
            readoutInfo.setRange(getRange());
            samples.add(readoutInfo);
        }

        if ((r != null) && !r.isMissing()) {

            result.add("<tr><td>" + getMenuLabel()
                    + ":</td><td  align=\"right\">"
                    + formatForCursorReadout(r) + ((currentLevel != null)
                    ? ("@" + currentLevel)
                    : "") + "</td></tr>");
        }
        return result;
    }

    public Bearing getBearing(LatLonPoint radarLocation, double lat, double lon) {
        Bearing b1 =
                Bearing.calculateBearing(radarLocation.getLatitude().getValue(),
                        radarLocation.getLongitude().getValue(),
                        lat, lon, null);
        return b1;
    }

    /**
     * @override
     *
     * @return _more_
     */
    protected boolean canDoProgressiveResolution() {
        return false;
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public void doExport(String what, String filename) throws Exception {
        if (what.contains("netcdf")) {
                List sources = this.getDataSources();
            CDMRadarDataSource cdmRadar = (CDMRadarDataSource)sources.get(0);
            String fileIn = cdmRadar.getDataPaths().toString();

            NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(fileIn, null);

            NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3;

            NetcdfFileWriter ncOut = NetcdfFileWriter.createNew(version, filename);

            //Rewrite rewrite = new Rewrite(ncfileIn, ncOut);
            //rewrite.rewrite();
            ncfileIn.close();
        }
    }
}
