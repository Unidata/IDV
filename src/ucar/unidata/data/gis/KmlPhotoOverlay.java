/*
 * $Id: KmlPhotoOverlay.java,v 1.12 2006/12/01 20:42:31 jeffmc Exp $
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

package ucar.unidata.data.gis;


import org.w3c.dom.*;

import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPoint;

import ucar.unidata.data.grid.GridUtil;


import ucar.unidata.data.imagery.ImageXmlDataSource;


import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;



import visad.*;

import visad.util.DataUtility;

import java.awt.*;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;



/**
 * Class KmlPhotoOverlay represents a photo overlay in KML
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.12 $
 */
public class KmlPhotoOverlay extends KmlImageElement {


    /** xml tags */
    public static final String TAG_NEAR = "near";

    public static final String TAG_CAMERA = "Camera";

    public static final String TAG_LONGITUDE = "longitude";
    public static final String TAG_LATITUDE = "latitude";
    public static final String TAG_ALTITUDE = "altitude";
    public static final String TAG_HEADING = "heading";
    public static final String TAG_TILT = "tilt";
    public static final String TAG_ROLL = "roll";


    public static final String TAG_VIEWVOLUME = "ViewVolume";



    public static final String TAG_LEFTFOV = "leftFov";

    public static final String TAG_RIGHTFOV = "rightFov";
    public static final String TAG_BOTTOMFOV = "bottomFov";
    public static final String TAG_TOPFOV = "topFov";


    /** xml tags */
    public static final String TAG_ROTATION = "rotation";

    /** properties from kml */
    private double rotation;

    private double longitude;
    private double latitude;
    private double altitude;
    private double tilt;
    private double roll;
    private double heading;
    private double near;
    private double leftFov;
    private double rightFov;
    private double bottomFov;
    private double topFov;


    /**
     * ctor
     */
    public KmlPhotoOverlay() {}



    /**
     * ctor
     *
     * @param node kml node
     * @param displayCategory display category
     * @param baseUrl url of the kml doc
     */
    public KmlPhotoOverlay(Element node, String displayCategory,
                            String baseUrl) {
        super(node, displayCategory, baseUrl);



        Element viewNode = XmlUtil.findChild(node, TAG_VIEWVOLUME);
        near= getValue(viewNode, TAG_NEAR);
        leftFov= getValue(viewNode, TAG_LEFTFOV);
        rightFov= getValue(viewNode, TAG_RIGHTFOV);
        bottomFov= getValue(viewNode, TAG_BOTTOMFOV);
        topFov= getValue(viewNode, TAG_TOPFOV);


        Element cameraNode = XmlUtil.findChild(node, TAG_CAMERA);
        longitude = getValue(cameraNode, TAG_LONGITUDE);
        latitude= getValue(cameraNode, TAG_LATITUDE);
        altitude= getValue(cameraNode, TAG_ALTITUDE);
        heading= getValue(cameraNode, TAG_HEADING);
        tilt= getValue(cameraNode, TAG_TILT);
        roll= getValue(cameraNode, TAG_ROLL);
    }

    private double getValue(Element node, String childTag) {
        Element child  = XmlUtil.findChild(node,
                                           childTag);
        if(child!=null) return new Double(XmlUtil.getChildText(child)).doubleValue();
        return 0.0;
    }

    private double squared(double d) {
        return d*d;
    }

    /**
     * get the image data
     *
     * @param dataSource data source
     * @param loadId for loading
     *
     * @return image data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data getData(KmlDataSource dataSource, Object loadId)
            throws VisADException, RemoteException {
        System.err.println(latitude + " " + longitude + " " + near + " " +Math.cos(Math.toRadians(leftFov)));
        System.err.println("h=" + near/Math.cos(Math.toRadians(leftFov)));

        LatLonPoint llp2 = Bearing.findPoint(latitude, longitude,
                                             rotation, near/1000.0,null);
        System.err.println("llp:" + llp2.getLatitude() + " " + llp2.getLongitude());

        double lat2 = latitude;
        double lon2 = longitude;

        double left = longitude +Math.sqrt(near*near*(1/squared(Math.cos(Math.toRadians(leftFov)))-1));
        double right = longitude -Math.sqrt(near*near*(1/squared(Math.cos(Math.toRadians(rightFov)))-1));
        double top = 0 +Math.sqrt(near*near*(1/squared(Math.cos(Math.toRadians(topFov)))-1));
        double bottom = 0 -Math.sqrt(near*near*(1/squared(Math.cos(Math.toRadians(bottomFov)))-1));


        System.err.println("b:" + left +  " " + right + " " + top + " " + bottom);
        Image image = getImage(dataSource);
        if (image == null) {
            return null;
        }
        /*
        Trace.call1("makeField");
        try {
            FieldImpl xyData = DataUtility.makeField(image);
            Trace.call2("makeField");
            Linear2DSet domain = (Linear2DSet) xyData.getDomainSet();
            Linear2DSet imageDomain =
                new Linear2DSet(RealTupleType.SpatialEarth2DTuple, getWest(),
                                getEast(), domain.getX().getLength(),
                                getNorth(), getSouth(),
                                domain.getY().getLength());



            FieldImpl field = GridUtil.setSpatialDomain(xyData, imageDomain,
                                  true);

            return field;
        } catch (Exception iexc) {
            LogUtil.logException("Error making image data", iexc);
        }
        */
        return null;


    }



    /**
     * Set the Rotation property.
     *
     * @param value The new value for Rotation
     */
    public void setRotation(double value) {
        rotation = value;
    }

    /**
     * Get the Rotation property.
     *
     * @return The Rotation
     */
    public double getRotation() {
        return rotation;
    }

/**
Set the Longitude property.

@param value The new value for Longitude
**/
public void setLongitude (double value) {
	longitude = value;
}

/**
Get the Longitude property.

@return The Longitude
**/
public double getLongitude () {
	return longitude;
}

/**
Set the Latitude property.

@param value The new value for Latitude
**/
public void setLatitude (double value) {
	latitude = value;
}

/**
Get the Latitude property.

@return The Latitude
**/
public double getLatitude () {
	return latitude;
}

/**
Set the Altitude property.

@param value The new value for Altitude
**/
public void setAltitude (double value) {
	altitude = value;
}

/**
Get the Altitude property.

@return The Altitude
**/
public double getAltitude () {
	return altitude;
}

/**
Set the Tilt property.

@param value The new value for Tilt
**/
public void setTilt (double value) {
	tilt = value;
}

/**
Get the Tilt property.

@return The Tilt
**/
public double getTilt () {
	return tilt;
}

/**
Set the Roll property.

@param value The new value for Roll
**/
public void setRoll (double value) {
	roll = value;
}

/**
Get the Roll property.

@return The Roll
**/
public double getRoll () {
	return roll;
}

/**
Set the Heading property.

@param value The new value for Heading
**/
public void setHeading (double value) {
	heading = value;
}

/**
Get the Heading property.

@return The Heading
**/
public double getHeading () {
	return heading;
}

/**
Set the Near property.

@param value The new value for Near
**/
public void setNear (double value) {
	near = value;
}

/**
Get the Near property.

@return The Near
**/
public double getNear () {
	return near;
}

/**
Set the LeftFov property.

@param value The new value for LeftFov
**/
public void setLeftFov (double value) {
	leftFov = value;
}

/**
Get the LeftFov property.

@return The LeftFov
**/
public double getLeftFov () {
	return leftFov;
}

/**
Set the RightFov property.

@param value The new value for RightFov
**/
public void setRightFov (double value) {
	rightFov = value;
}

/**
Get the RightFov property.

@return The RightFov
**/
public double getRightFov () {
	return rightFov;
}

/**
Set the BottomFov property.

@param value The new value for BottomFov
**/
public void setBottomFov (double value) {
	bottomFov = value;
}

/**
Get the BottomFov property.

@return The BottomFov
**/
public double getBottomFov () {
	return bottomFov;
}

/**
Set the TopFov property.

@param value The new value for TopFov
**/
public void setTopFov (double value) {
	topFov = value;
}

/**
Get the TopFov property.

@return The TopFov
**/
public double getTopFov () {
	return topFov;
}





}

