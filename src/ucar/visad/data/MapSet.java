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

package ucar.visad.data;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;


import visad.*;

import visad.util.DataUtility;

import java.rmi.RemoteException;


import java.util.Hashtable;
import java.util.List;


/**
 * This is a FloatField that caches to disk its float array.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.9 $ $Date: 2007/08/08 17:14:56 $
 */
public class MapSet extends Gridded2DSet {

    /** _more_          */
    private Hashtable properties;

    /** _more_          */
    private List propertyNames;

    /**
     * Create a new CachedFlatField
     *
     * @param type Function type
     * @param points _more_
     * @param numPoints _more_
     * @param cs _more_
     * @param units _more_
     * @param errors _more_
     * @param copy _more_
     *
     * @throws VisADException On badness
     */
    public MapSet(RealTupleType type, float[][] points, int numPoints,
                  CoordinateSystem cs, Unit[] units, ErrorEstimate[] errors,
                  boolean copy)
            throws VisADException {
        super(type, points, numPoints, cs, units, errors, copy);
    }


    /**
     * _more_
     *
     * @param names _more_
     */
    public void setPropertyNames(List names) {
        propertyNames = names;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getPropertyNames() {
        return propertyNames;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getProperties() {
        return properties;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void setProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(key, value);
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
