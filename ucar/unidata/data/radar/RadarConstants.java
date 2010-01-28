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

package ucar.unidata.data.radar;



/**
 * Holds a set of definitions concerning radar data.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.17 $ $Date: 2006/12/01 20:42:39 $
 */
public interface RadarConstants {

    /** Azimuth value property */
    public static final String PROP_AZIMUTH = "Level2RadarDataSource.azimuth";

    /** Azimuths property */
    public static final String PROP_AZIMUTHS =
        "Level2RadarDataSource.azimuths";

    /** Elevation angles property */
    public static final String PROP_ANGLES = "Level2RadarDataSource.angles";

    /** Elevation angle property */
    public static final String PROP_ANGLE = "Level2RadarDataSource.angle";

    /** CAPPI level property */
    public static final String PROP_CAPPI_LEVEL =
        "Level2RadarDataSource.cappilevel";

    /** CAPPI levels property */
    public static final String PROP_CAPPI_LEVELS =
        "Level2RadarDataSource.cappilevels";

    /** Time-Height property */
    public static final String PROP_TIMEHEIGHT =
        "Level2RadarDataSource.timeheight";

    /** VAD Wind Profile (VWP) property */
    public static final String PROP_VWP = "Level2RadarDataSource.VWP";

    /** Vertical Cross Section property */
    public static final String PROP_VCS =
        "Level2RadarDataSource.verticalcrosssection";

    /** Vertical Cross Section property */
    public static final String PROP_VCS_START =
        "Level2RadarDataSource.verticalcrosssectionstart";

    /** Vertical Cross Section property */
    public static final String PROP_VCS_END =
        "Level2RadarDataSource.verticalcrosssectionend";

    /** Property to define volumes or sweeps */
    public static final String PROP_VOLUMEORSWEEP =
        "Level2RadarDataSource.volumeorsweep";

    /** Volume property */
    public static final String VALUE_VOLUME = "Level2RadarDataSource.volume";

    /** Sweep property */
    public static final String VALUE_SWEEP = "Level2RadarDataSource.sweep";

    /** Property to define 2- or 3-D */
    public static final String PROP_2DOR3D = "Level2RadarDataSource.2Dor3D";

    /** 2-D property value */
    public static final String VALUE_2D = "Level2RadarDataSource.2D";

    /** 3-D property value */
    public static final String VALUE_3D = "Level2RadarDataSource.3D";

    /** Reflectivity moment identifier */
    public static final int REFLECTIVITY = Level2Record.REFLECTIVITY;

    /** Radial Velocity moment identifier */
    public static final int VELOCITY = Level2Record.VELOCITY;

    /** Spectrum Width moment identifier */
    public static final int SPECTRUM_WIDTH = Level2Record.SPECTRUM_WIDTH;

    /** Identifier for Station location */
    static final String STATION_LOCATION = "station location";

    /** Identifier for most recent properties */
    static final String RADAR_MOST_RECENT = "radar.mostrecent";

    /** Range identifier */
    public static final String RANGE = "Range";

    /** Azimuth Range identifier */
    public static final String AZIMUTH = "Azimuth";

    /** Elevation Angle Identifier */
    public static final String ELEVATION_ANGLE = "Elevation_Angle";

    /** Azimuth Angle Identifier */
    public static final String AZIMUTH_ANGLE = "Azimuth_Angle";

    /** Reflectivity name */
    public static final String REFLECTIVITY_NAME = "Reflectivity";

    /** Spectrum Width name */
    public static final String SPECTRUM_WIDTH_NAME = "Spectrum_Width";

    /** Velocity Width name */
    public static final String RADIAL_VELOCITY_NAME = "Radial_Velocity";
}
