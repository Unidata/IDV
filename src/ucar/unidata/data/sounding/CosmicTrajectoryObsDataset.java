/*
 * $Id: CosmicTrajectoryObsDataset.java 63 2006-07-12 21:50:51Z edavis $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.sounding;


import ucar.ma2.Array;
import ucar.ma2.DataType;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TypedDatasetFactoryIF;

import ucar.nc2.dt.trajectory.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;

import java.io.IOException;

import java.util.Date;
import java.util.List;


/**
 * Implements TrajectoryDataset for datasets with these characteristics:
 * <ul>
 *   <li> it has only one dimension, the dimension is UNLIMITED and is named "MSL_alt"</li>
 *   <li> it has one coordinate variable, MSL_alt(MSL_alt)
 *   <li> Time is synthesized from the start and end times in the global
 *        attributes
 *   <li> has the variables Lat(MSL_alt), Lon(MSL_alt), and MSL_alt(MSL_alt)
 *        with units "deg", "deg", and "km", respectively.
 * </ul>
 *
 * @author dmurray
 * @since Feb 22, 2005T5:37:14 PM
 */
public class CosmicTrajectoryObsDataset extends SingleTrajectoryObsDataset implements TypedDatasetFactoryIF {

    /** Time dimension name */
    private static String timeDimName = "MSL_alt";

    /** Time variable name */
    private static String timeVarName = "Time";

    /** Latitude variable name */
    private static String latVarName = "Lat";

    /** Longitude variable name */
    private static String lonVarName = "Lon";

    /** Altitude (elevation) variable name */
    private static String elevVarName = "MSL_alt";

    /** Default name for the sounding */
    private static String trajId = "COSMIC sounding";

    /**
     * Is this the right format for this adapter
     *
     * @param ncd   NetcdfDataset to check
     *
     * @return true if the right format
     */
    static public boolean isValidFile(NetcdfDataset ncd) {
        //return (buildConfig(ncd) != null);
        // Check for "center" attribute w/ value of "UCAR/CDAAC".
        Attribute attrib = ncd.findGlobalAttributeIgnoreCase("center");
        if (attrib == null) {
            return false;
        }
        if ( !attrib.isString()) {
            return false;
        }
        if ( !attrib.getStringValue().equals("UCAR/CDAAC")) {
            return false;
        }
        return true;
    }

    /**
     * Build the configuration from the dataset
     *
     * @param ncd   NetcdfDataset
     *
     * @return  the trajectory configuration
     */
    private static Config buildConfig(NetcdfDataset ncd) {

        // already did this in isValid, but we'll keep here for later refactor
        Attribute attrib = ncd.findGlobalAttributeIgnoreCase("center");
        if (attrib == null) {
            return null;
        }
        if ( !attrib.isString()) {
            return null;
        }
        if ( !attrib.getStringValue().equals("UCAR/CDAAC")) {
            return null;
        }

        // Check for start_time, stop_time
        attrib = ncd.findGlobalAttributeIgnoreCase("start_time");
        if (attrib == null) {
            return null;
        }
        if (attrib.isString()) {
            return null;
        }
        double startTime = attrib.getNumericValue().doubleValue();
        attrib = ncd.findGlobalAttributeIgnoreCase("stop_time");
        if (attrib == null) {
            return null;
        }
        if (attrib.isString()) {
            return null;
        }
        double endTime = attrib.getNumericValue().doubleValue();


        // Check that only one dimension and that it is the alt dimension.
        List list = ncd.getRootGroup().getDimensions();
        if (list.size() != 1) {
            return null;
        }
        Dimension d = (Dimension) list.get(0);
        if ( !d.getName().equals(timeDimName)) {
            return null;
        }

        Config trajConfig = new Config();
        trajConfig.setTimeDim(d);


        // Check for latitude variable with time dimension and units convertable to "degrees_north".
        Variable var = ncd.getRootGroup().findVariable(latVarName);
        if (var == null) {
            return null;
        }
        list = var.getDimensions();
        if (list.size() != 1) {
            return null;
        }
        d = (Dimension) list.get(0);
        if ( !d.getName().equals(timeDimName)) {
            return null;
        }
        String units = var.findAttribute("units").getStringValue();
        if ( !SimpleUnit.isCompatible(units, "degrees_north")) {
            return null;
        }

        trajConfig.setLatVar(var);

        // Make the time Variable
        int      numTimes = d.getLength();
        double[] times    = new double[numTimes];
        //Variable timeVar = new Variable(var);
        //timeVar.setName(timeVarName);
        VariableDS timeVar =
            new VariableDS(ncd, ncd.getRootGroup(), null, timeVarName,
                           DataType.DOUBLE, timeDimName,
                           "seconds since 1980-01-06 00:00:00",
                           "Time coordinate");
        //Variable timeVar = new Variable(ncd, ncd.getRootGroup(), null,
        //                              timeVarName);
        //timeVar.setDataType(DataType.DOUBLE);
        //timeVar.setDimensions(list);
        //Attribute newUnits =
        //    new Attribute("units", "seconds since 1980-01-06 00:00:00");
        //timeVar.addAttribute(newUnits);
        timeVar.setCachedData(Array.makeArray(DataType.DOUBLE, numTimes,
                endTime, ((startTime - endTime) / numTimes)), true);
        ncd.addVariable(ncd.getRootGroup(), timeVar);
        trajConfig.setTimeVar(timeVar);

        // Check for longitude variable with time dimension and units convertable to "degrees_east".
        var = ncd.getRootGroup().findVariable(lonVarName);
        if (var == null) {
            return null;
        }
        list = var.getDimensions();
        if (list.size() != 1) {
            return null;
        }
        d = (Dimension) list.get(0);
        if ( !d.getName().equals(timeDimName)) {
            return null;
        }
        units = var.findAttribute("units").getStringValue();
        if ( !SimpleUnit.isCompatible(units, "degrees_east")) {
            return null;
        }

        trajConfig.setLonVar(var);

        // Check for altitude variable with time dimension and units convertable to "m".
        var = ncd.getRootGroup().findVariable(elevVarName);
        if (var == null) {
            return null;
        }
        list = var.getDimensions();
        if (list.size() != 1) {
            return null;
        }
        d = (Dimension) list.get(0);
        if ( !d.getName().equals(timeDimName)) {
            return null;
        }
        units = var.findAttribute("units").getStringValue();
        if ( !SimpleUnit.isCompatible(units, "meters")) {
            return null;
        }

        trajConfig.setElevVar(var);

        trajConfig.setTrajectoryId(trajId);

        return trajConfig;
    }

    /////////////////////////////////////////////////
    // TypedDatasetFactoryIF

    /**
     * Is this my type of file?
     *
     * @param ds  dataset to check
     *
     * @return  true if format is correct
     */
    public boolean isMine(NetcdfDataset ds) {
        return isValidFile(ds);
    }

    /**
     * Open the NetcdfDataset as a TrajectoryObsDataset
     *
     * @param ncd   dataset
     * @param task  cancel task
     * @param errlog  where to put errors
     *
     * @return  TypedDataset or null
     *
     * @throws IOException   problem opening the file
     */
    public TypedDataset open(NetcdfDataset ncd,
                             ucar.nc2.util.CancelTask task,
                             StringBuilder errlog)
            throws IOException {
        return new CosmicTrajectoryObsDataset(ncd);
    }

    /**
     * Get the CDM scientific data type
     *
     * @return the datatype
     */
    public ucar.nc2.constants.FeatureType getScientificDataType() {
        return ucar.nc2.constants.FeatureType.TRAJECTORY;
    }

    /**
     * Default constructor.
     */
    public CosmicTrajectoryObsDataset() {}

    /**
     * Create a new TrajectoryObsDataset from the generic one
     *
     * @param ncd   dataset to decorate
     *
     * @throws IOException   problem reading data
     */
    public CosmicTrajectoryObsDataset(NetcdfDataset ncd) throws IOException {
        super(ncd);
        Config trajConfig = buildConfig(ncd);
        this.setTrajectoryInfo(trajConfig);
    }
}

