/*
 * $Id: ParcelMode.java,v 1.7 2005/05/13 18:33:34 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.util.ArrayList;

import org.w3c.dom.Element;

import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectFactory;
import ucar.unidata.xml.XmlPersistable;


/**
 * An enumerated-type for how the initial conditions (pressure, temperature,
 * moisture content) of an air parcel is determined.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:34 $
 */
public final class ParcelMode implements XmlPersistable {

    /** name */
    private final String name;

    /**
     * Compute a path for a lifted parcel whose initial conditions are based on
     * the pressure and temperature of the input point and the mixing-ratio of
     * the dew-point profile at the input pressure.
     */
    public static final ParcelMode POINT = new ParcelMode("point");

    /**
     * Compute a path for a lifted parcel whose initial conditions are based on
     * the temperature and dew-point of the profiles at the input pressure.
     * This differs from {@link #POINT} in that the initial temperature is taken
     * from the temperature profile rather than the input point.
     */
    public static final ParcelMode PRESSURE = new ParcelMode("pressure");

    /**
     * Compute a path for a lifted parcel whose initial conditions are based on
     * the mean pressure, potential-temperature, and mixing-ratio of the
     * atmospheric layer below the input pressure.
     */
    public static final ParcelMode LAYER = new ParcelMode("layer");

    /**
     * Compute a path for a lifted parcel whose initial conditions are based on
     * the pressure, potential-temperature, and mixing-ratio of the sounding
     * at maximum pressure. That is, the bottom of the sounding.
     */
    public static final ParcelMode BOTTOM = new ParcelMode("bottom");

    /**
     * Create a new ParcelMode
     *
     * @param name   name of the parcel mode
     */
    private ParcelMode(String name) {
        this.name = name;
    }

    /**
     * Returns the XML element corresponding to this instance.
     *
     * @param encoder               The XML encoder.
     * @return                      The corresponding XML element.
     */
    public Element createElement(XmlEncoder encoder) {

        Element factoryNode = encoder.createFactoryElement(Factory.class);

        factoryNode.setAttribute("name", name);

        return factoryNode;
    }

    /**
     * <p>Returns an instance corresponding to an XML element.  Because this
     * class encodes instances using the "factory" tag, this method should never
     * be invoked.</p>
     *
     * <p>This implementation always returns <code>true<code>.</p>
     *
     * @param encoder               The XML encoder.
     * @param elt                   The XML element
     * @return                      <code>true</code> if it is OK to do the
     *                              default processing for this XML element.
     */
    public boolean initFromXml(XmlEncoder encoder, Element elt) {
        return true;
    }

    /**
     * Factory class for decoding an XML element into an instance of the
     * enclosing class.
     */
    public static class Factory implements XmlObjectFactory {

        /**
         * Constructs from nothing.  Necessary for factory creation by
         * XmlPersistable.
         */
        public Factory() {}

        /**
         * Returns an instance of the enclosing class corresponding to an XML
         * element.
         *
         * @param encoder                   The XML encoder.
         * @param elt                       The XML element to be decoded.
         * @return                          The object corresponding to the XML
         *                                  element.
         * @throws IllegalArgumentException if the element can't be decoded.
         */
        public Object getObject(XmlEncoder encoder, Element elt) {

            String name = elt.getAttribute("name");

            if (POINT.name.equals(name)) {
                return POINT;
            }

            if (PRESSURE.name.equals(name)) {
                return PRESSURE;
            }

            if (LAYER.name.equals(name)) {
                return LAYER;
            }

            if (BOTTOM.name.equals(name)) {
                return BOTTOM;
            }

            throw new IllegalArgumentException(name);
        }
    }
}







