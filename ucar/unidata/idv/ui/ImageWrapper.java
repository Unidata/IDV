/*
 * $Id: ImageSequenceGrabber.java,v 1.96 2007/08/13 18:38:55 jeffmc Exp $
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


package ucar.unidata.idv.ui;


import ucar.unidata.data.GeoLocationInfo;



import visad.DateTime;

import java.io.File;

import java.util.List;
import java.util.ArrayList;

/**
 * Class ImageWrapper Holds information about captured images
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ImageWrapper {

    /** The image file path */
    private String path;

    /** The date/time of the image */
    private DateTime dttm;

    /** The bounds */
    private GeoLocationInfo bounds;

    /** the viewpoint */
    private double[] position;

    /**
     * ctor
     *
     * @param path the image path
     */
    public ImageWrapper(String path) {
        this(path, null, null, null);
    }

    public ImageWrapper(String path,DateTime dttm) {
        this(path, dttm, null, null);
    }


    /**
     * ctor
     *
     * @param path image path
     * @param dttm date time
     * @param bounds bounds
     */
    public ImageWrapper(String path, DateTime dttm, GeoLocationInfo bounds) {
        this(path, dttm, bounds, null);
    }

    /**
     * ctor
     *
     * @param path image path
     * @param dttm date/time
     * @param bounds bounds
     * @param position viewpoint position
     */
    public ImageWrapper(String path, DateTime dttm, GeoLocationInfo bounds,
                        double[] position) {
        this.path     = path;
        this.dttm     = dttm;
        this.bounds   = bounds;
        this.position = position;
    }

    public static List<ImageWrapper> makeList(List source) {
        List<ImageWrapper>result = new ArrayList<ImageWrapper>();
        for(int i=0;i<source.size();i++) {
            Object o = source.get(i);
            if(o instanceof ImageWrapper) {
                result.add((ImageWrapper)o);
            } else {
                result.add(new ImageWrapper(o.toString()));
            }
        }
        return result;
    }


    public String toString() {
        return path;
    }

    public void deleteFile() {
        new File(path).delete();
    }


    /**
     * Set the Path property.
     *
     * @param value The new value for Path
     */
    public void setPath(String value) {
        path = value;
    }

    /**
     * Get the Path property.
     *
     * @return The Path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the Dttm property.
     *
     * @param value The new value for Dttm
     */
    public void setDttm(DateTime value) {
        dttm = value;
    }

    /**
     * Get the Dttm property.
     *
     * @return The Dttm
     */
    public DateTime getDttm() {
        return dttm;
    }

    /**
     * Set the Bounds property.
     *
     * @param value The new value for Bounds
     */
    public void setBounds(GeoLocationInfo value) {
        bounds = value;
    }

    /**
     * Get the Bounds property.
     *
     * @return The Bounds
     */
    public GeoLocationInfo getBounds() {
        return bounds;
    }

    /**
     * Set the Position property.
     *
     * @param value The new value for Position
     */
    public void setPosition(double[] value) {
        position = value;
    }

    /**
     * Get the Position property.
     *
     * @return The Position
     */
    public double[] getPosition() {
        return position;
    }




}

