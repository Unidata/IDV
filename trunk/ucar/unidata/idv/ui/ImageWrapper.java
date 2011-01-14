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

package ucar.unidata.idv.ui;


import ucar.unidata.data.GeoLocationInfo;



import visad.DateTime;

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;


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

    /** _more_          */
    private Hashtable properties;

    /**
     * ctor
     *
     * @param path the image path
     */
    public ImageWrapper(String path) {
        this(path, null, null, null);
    }

    /**
     * ctor
     *
     * @param path image file path
     * @param dttm date
     */
    public ImageWrapper(String path, DateTime dttm) {
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

    /**
     * utility to return a list of the image paths in the given list of ImageWrappers
     *
     * @param wrappers image wrappers
     *
     * @return image file paths
     */
    public static List<String> makeFileList(List<ImageWrapper> wrappers) {
        List<String> files = new ArrayList<String>();
        for (ImageWrapper imageWrapper : wrappers) {
            files.add(imageWrapper.path);
        }
        return files;
    }


    /**
     * utility to create a list of ImageWrapper from a list of file names
     *
     * @param source list of file names
     *
     * @return list of image wrappers
     */
    public static List<ImageWrapper> makeImageWrappers(List source) {
        List<ImageWrapper> result = new ArrayList<ImageWrapper>();
        for (int i = 0; i < source.size(); i++) {
            Object o = source.get(i);
            if (o instanceof ImageWrapper) {
                result.add((ImageWrapper) o);
            } else {
                result.add(new ImageWrapper(o.toString()));
            }
        }
        return result;
    }


    /**
     * to string
     *
     * @return to string
     */
    public String toString() {
        return path;
    }

    /**
     * delete the image file
     */
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


    /**
     *  Set the Properties property.
     *
     *  @param value The new value for Properties
     */
    public void setProperties(Hashtable value) {
        properties = value;
    }

    /**
     *  Get the Properties property.
     *
     *  @return The Properties
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key) {
        if (properties == null) {
            return null;
        }
        return properties.get(key);
    }


}
