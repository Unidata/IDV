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

package ucar.unidata.data.imagery;


import org.w3c.dom.Element;

import ucar.unidata.xml.XmlUtil;


import visad.*;


/**
 * Holds information about images
 */

public class ImageInfo {

    /** xml attr name */
    public static final String ATTR_NAME = "name";


    /** Upper left lat */
    public static final String ATTR_ULLAT = "ullat";


    /** Upper left lon */
    public static final String ATTR_ULLON = "ullon";

    /** Upper left lat */
    public static final String ATTR_ULALT = "ulalt";

    /** Lower right lat */
    public static final String ATTR_LRLAT = "lrlat";

    /** Lower right lon */
    public static final String ATTR_LRLON = "lrlon";

    /** Upper left lat */
    public static final String ATTR_LRALT = "lralt";

    /** xml attribute name */
    public static final String ATTR_URLAT = "urlat";

    /** xml attribute name */
    public static final String ATTR_URLON = "urlon";

    /** xml attribute name */
    public static final String ATTR_URALT = "uralt";


    /** xml attribute name */
    public static final String ATTR_LLLAT = "lllat";


    /** xml attribute name */
    public static final String ATTR_LLLON = "lllon";


    /** xml attribute name */
    public static final String ATTR_LLALT = "llalt";


    /** url of image_ */
    private String url;

    /** upper left lat */
    private double ulLat = Double.NaN;

    /** upper left lon */
    private double ulLon = Double.NaN;

    /** upper left alt */
    private double ulAlt = Double.NaN;


    /** lower right lat */
    private double lrLat = Double.NaN;

    /** lower right lon */
    private double lrLon = Double.NaN;

    /** lower right alt */
    private double lrAlt = Double.NaN;

    /** lower right lat */
    private double urLat = Double.NaN;

    /** lower right lon */
    private double urLon = Double.NaN;

    /** _upper right alt */
    private double urAlt = Double.NaN;

    /** lower right lat */
    private double llLat = Double.NaN;

    /** lower right lon */
    private double llLon = Double.NaN;

    /** lower left alt */
    private double llAlt = Double.NaN;


    /** Is this a shapefile */
    private boolean isShape = false;

    /** The dttm of the image. May be null */
    private DateTime date;

    /**
     * ctor
     */
    public ImageInfo() {}

    /**
     * Ctor from xml
     *
     * @param url the url
     * @param node The main node
     * @param parent The parent node to get defaults from. May be null.
     * @param date The date. May be null.
     *
     * @throws Exception On badness
     */
    public ImageInfo(String url, Element node, Element parent, DateTime date)
            throws Exception {
        this.url  = url;
        this.date = date;
        setPosition(parent);
        setPosition(node);

        //Set the alts
        if (defined(ulAlt) && !defined(urAlt)) {
            urAlt = ulAlt;
        } else if ( !defined(ulAlt) && defined(urAlt)) {
            ulAlt = urAlt;
        }

        if (defined(llAlt) && !defined(lrAlt)) {
            lrAlt = llAlt;
        } else if ( !defined(llAlt) && defined(lrAlt)) {
            llAlt = lrAlt;
        }


        if (hasAltitude()) {
            if (defined(ulLat) && !defined(llLat)) {
                llLat = ulLat;
            } else if ( !defined(ulLat) && defined(llLat)) {
                ulLat = llLat;
            }


            if (defined(ulLon) && !defined(llLon)) {
                llLon = ulLon;
            } else if ( !defined(ulLon) && defined(llLon)) {
                ulLon = llLon;
            }

            if (defined(urLat) && !defined(lrLat)) {
                lrLat = urLat;
            } else if ( !defined(urLat) && defined(lrLat)) {
                urLat = lrLat;
            }

            if (defined(urLon) && !defined(lrLon)) {
                lrLon = urLon;
            } else if ( !defined(urLon) && defined(lrLon)) {
                urLon = lrLon;
            }

        } else {
            if (defined(ulLat) && !defined(urLat)) {
                urLat = ulLat;
            } else if ( !defined(ulLat) && defined(urLat)) {
                ulLat = urLat;
            }


            if (defined(llLat) && !defined(lrLat)) {
                lrLat = llLat;
            } else if ( !defined(llLat) && defined(lrLat)) {
                llLat = lrLat;
            }

            if (defined(ulLon) && !defined(llLon)) {
                llLon = ulLon;
            } else if ( !defined(ulLon) && defined(llLon)) {
                ulLon = llLon;

            }

            if (defined(urLon) && !defined(lrLon)) {
                lrLon = urLon;
            } else if ( !defined(urLon) && defined(lrLon)) {
                urLon = lrLon;

            }
        }
        //        System.err.println(toString());
        if ( !allDefined()) {
            throw new IllegalArgumentException("Image "
                    + XmlUtil.getAttribute(node, ATTR_NAME, "")
                    + " does not have proper coordinates defined\n"
                    + toString());
        }
    }

    /**
     * Are all values defined
     *
     * @return all defined
     */
    private boolean allDefined() {
        if ( !defined(ulLat) || !defined(ulLon) || !defined(llLat)
                || !defined(llLon)) {
            return false;
        }
        if ( !defined(urLat) || !defined(urLon) || !defined(lrLat)
                || !defined(lrLon)) {
            return false;
        }
        return true;
    }


    /**
     * Get altitude in meters
     *
     * @param node xml node
     * @param attr altitude attr name
     * @param dflt default value if none defined
     *
     * @return alt value in meters
     *
     * @throws Exception On badness
     */
    private double getAltMeters(Element node, String attr, double dflt)
            throws Exception {
        String s = XmlUtil.getAttribute(node, attr, (String) null);
        if (s == null) {
            return dflt;
        }
        s = s.trim();
        if (s.indexOf("[") >= 0) {
            Real r = ucar.visad.Util.toReal(s);
            return r.getValue(CommonUnit.meter);
        } else {
            return new Double(s).doubleValue();
        }
    }


    /**
     * Is the given value defined
     *
     * @param v value
     *
     * @return defined
     */
    private boolean defined(double v) {
        return v == v;
    }




    /**
     * set the position values from the given node
     *
     * @param node node
     *
     * @throws Exception On badness
     */
    private void setPosition(Element node) throws Exception {
        if (node == null) {
            return;
        }
        ulLat = XmlUtil.getAttribute(node, ATTR_ULLAT, ulLat);
        ulLon = XmlUtil.getAttribute(node, ATTR_ULLON, ulLon);
        ulAlt = getAltMeters(node, ATTR_ULALT, ulAlt);

        urLat = XmlUtil.getAttribute(node, ATTR_URLAT, urLat);
        urLon = XmlUtil.getAttribute(node, ATTR_URLON, urLon);
        urAlt = getAltMeters(node, ATTR_URALT, urAlt);


        lrLat = XmlUtil.getAttribute(node, ATTR_LRLAT, lrLat);
        lrLon = XmlUtil.getAttribute(node, ATTR_LRLON, lrLon);
        lrAlt = getAltMeters(node, ATTR_LRALT, lrAlt);

        llLat = XmlUtil.getAttribute(node, ATTR_LLLAT, llLat);
        llLon = XmlUtil.getAttribute(node, ATTR_LLLON, llLon);
        llAlt = getAltMeters(node, ATTR_LLALT, llAlt);

    }



    /**
     * do the corners form a rectangle. i.e., are the upper lat the same,
     * are the left longs the same, etc.
     *
     * @return isRectilinear
     */
    public boolean isRectilinear() {
        if ( !allDefined()) {
            return true;
        }
        return (ulLat == urLat) && (llLat == lrLat) && (ulLon == llLon)
               && (urLon == lrLon);
    }


    /**
     * Do we have any altitudes
     *
     * @return has altitude defined
     */
    public boolean hasAltitude() {
        return ulAlt == ulAlt;
    }


    /**
     * ctor
     *
     * @param url The image url
     * @param isShape Is this a shapefile
     */
    public ImageInfo(String url, boolean isShape) {
        this(url, null, isShape);
    }

    /**
     * ctor
     *
     * @param url The image url
     * @param date Datetime of the image_
     * @param isShape Is this a shapefile
     */
    public ImageInfo(String url, DateTime date, boolean isShape) {
        this.url     = url;
        this.isShape = isShape;
        this.date    = date;
    }

    /**
     * ctor
     *
     * @param url The image url
     * @param ulLat ul lat
     * @param ulLon ul lon
     * @param lrLat lr lat
     * @param lrLon lrlon
     */
    public ImageInfo(String url, double ulLat, double ulLon, double lrLat,
                     double lrLon) {
        this(url, null, ulLat, ulLon, lrLat, lrLon);
    }

    /**
     * ctor
     *
     * @param url The image url
     * @param date Datetime of the image
     * @param ulLat ul lat
     * @param ulLon ul lon
     * @param lrLat lr lat
     * @param lrLon lrlon
     */
    public ImageInfo(String url, DateTime date, double ulLat, double ulLon,
                     double lrLat, double lrLon) {
        this.url   = url;
        this.date  = date;
        this.ulLat = ulLat;
        this.ulLon = ulLon;
        this.lrLat = lrLat;
        this.lrLon = lrLon;
    }

    /**
     *  Set the Url property.
     *
     *  @param value The new value for Url
     */
    public void setUrl(String value) {
        url = value;
    }

    /**
     *  Get the Url property.
     *
     *  @return The Url
     */
    public String getUrl() {
        return url;
    }


    /**
     * Set the UrLat property.
     *
     * @param value The new value for UrLat
     */
    public void setUrLat(double value) {
        urLat = value;
    }

    /**
     * Get the UrLat property.
     *
     * @return The UrLat
     */
    public double getUrLat() {
        return urLat;
    }

    /**
     * Set the UrLon property.
     *
     * @param value The new value for UrLon
     */
    public void setUrLon(double value) {
        urLon = value;
    }

    /**
     * Get the UrLon property.
     *
     * @return The UrLon
     */
    public double getUrLon() {
        return urLon;
    }

    /**
     * Set the UrAlt property.
     *
     * @param value The new value for UrAlt
     */
    public void setUrAlt(double value) {
        urAlt = value;
    }

    /**
     * Get the UrAlt property.
     *
     * @return The UrAlt
     */
    public double getUrAlt() {
        return urAlt;
    }

    /**
     * Set the LrLat property.
     *
     * @param value The new value for LrLat
     */
    public void setLrLat(double value) {
        lrLat = value;
    }

    /**
     * Get the LrLat property.
     *
     * @return The LrLat
     */
    public double getLrLat() {
        return lrLat;
    }

    /**
     * Set the LrLon property.
     *
     * @param value The new value for LrLon
     */
    public void setLrLon(double value) {
        lrLon = value;
    }

    /**
     * Get the LrLon property.
     *
     * @return The LrLon
     */
    public double getLrLon() {
        return lrLon;
    }

    /**
     * Set the LrAlt property.
     *
     * @param value The new value for LrAlt
     */
    public void setLrAlt(double value) {
        lrAlt = value;
    }

    /**
     * Get the LrAlt property.
     *
     * @return The LrAlt
     */
    public double getLrAlt() {
        return lrAlt;
    }



    /**
     * Set the LlLat property.
     *
     * @param value The new value for LlLat
     */
    public void setLlLat(double value) {
        llLat = value;
    }

    /**
     * Get the LlLat property.
     *
     * @return The LlLat
     */
    public double getLlLat() {
        return llLat;
    }

    /**
     * Set the LlLon property.
     *
     * @param value The new value for LlLon
     */
    public void setLlLon(double value) {
        llLon = value;
    }

    /**
     * Get the LlLon property.
     *
     * @return The LlLon
     */
    public double getLlLon() {
        return llLon;
    }

    /**
     * Set the LlAlt property.
     *
     * @param value The new value for LlAlt
     */
    public void setLlAlt(double value) {
        llAlt = value;
    }

    /**
     * Get the LlAlt property.
     *
     * @return The LlAlt
     */
    public double getLlAlt() {
        return llAlt;
    }




    /**
     * Set the UlLat property.
     *
     * @param value The new value for UlLat
     */
    public void setUlLat(double value) {
        ulLat = value;
    }

    /**
     * Get the UlLat property.
     *
     * @return The UlLat
     */
    public double getUlLat() {
        return ulLat;
    }

    /**
     * Set the UlLon property.
     *
     * @param value The new value for UlLon
     */
    public void setUlLon(double value) {
        ulLon = value;
    }

    /**
     * Get the UlLon property.
     *
     * @return The UlLon
     */
    public double getUlLon() {
        return ulLon;
    }

    /**
     * Set the UlAlt property.
     *
     * @param value The new value for UlAlt
     */
    public void setUlAlt(double value) {
        ulAlt = value;
    }

    /**
     * Get the UlAlt property.
     *
     * @return The UlAlt
     */
    public double getUlAlt() {
        return ulAlt;
    }


    /**
     * Set the IsShape property.
     *
     * @param value The new value for IsShape
     */
    public void setIsShape(boolean value) {
        isShape = value;
    }

    /**
     * Get the IsShape property.
     *
     * @return The IsShape
     */
    public boolean getIsShape() {
        return isShape;
    }


    /**
     * Set the Date property.
     *
     * @param value The new value for Date
     */
    public void setDate(DateTime value) {
        date = value;
    }

    /**
     * Get the Date property.
     *
     * @return The Date
     */
    public DateTime getDate() {
        return date;
    }






    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        return "ImageInfo:" + "ul: " + ulLat + "/" + ulLon + "/" + ulAlt
               + " " + "ur: " + urLat + "/" + urLon + "/" + urAlt + " "
               + "ll: " + llLat + "/" + llLon + "/" + llAlt + " " + "lr: "
               + lrLat + "/" + lrLon + "/" + lrAlt + " ";
    }

}
