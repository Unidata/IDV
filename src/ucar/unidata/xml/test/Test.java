/*
 * $Id: Test.java,v 1.6 2005/05/13 18:33:58 jeffmc Exp $
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

package ucar.unidata.xml.test;



import ucar.unidata.xml.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;

import java.lang.reflect.*;

import org.w3c.dom.Element;


/**
 * Class Test
 *
 *
 * @author IDV development team
 */
public class Test {

    /** Field v1 */
    Double v1;

    /** Field v2 */
    Double v2;

    /** Field v3 */
    Double v3;

    /**
     * FILLTHISIN
     *
     */
    public Test() {}

    /**
     *  ctor
     *
     * @param p1 p1
     * @param p2 p2
     * @param p3 p3
     *
     */
    public Test(Double p1, Double p2, Double p3) {

        v1 = p1;
        v2 = p2;
        v3 = p3;
    }

    /**
     *  Set the V1 property.
     *
     *  @param value The new value for V1
     */
    public void setV1(Double value) {

        v1 = value;
    }

    /**
     *  Get the V1 property.
     *
     *  @return The V1
     */
    public Double getV1() {

        return v1;
    }

    /**
     *  Set the V2 property.
     *
     *  @param value The new value for V2
     */
    public void setV2(Double value) {

        v2 = value;
    }

    /**
     *  Get the V2 property.
     *
     *  @return The V2
     */
    public Double getV2() {

        return v2;
    }

    /**
     *  Set the V3 property.
     *
     *  @param value The new value for V3
     */
    public void setV3(Double value) {

        v3 = value;
    }

    /**
     *  Get the V3 property.
     *
     *  @return The V3
     */
    public Double getV3() {

        return v3;
    }

    /**
     * Overwrite toString
     *
     * @return The string representation
     */
    public String toString() {

        return "v1=" + v1 + " v2=" + v2 + " v3=" + v3;
    }
}

























