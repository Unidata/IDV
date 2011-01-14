/*
 * $Id: Test2.java,v 1.5 2005/05/13 18:33:58 jeffmc Exp $
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

import org.w3c.dom.Element;

import java.util.ArrayList;


/**
 * Class Test2
 *
 *
 * @author IDV development team
 */
public class Test2 extends Test implements XmlPersistable {

    /** Field myX */
    int myX;

    /**
     * ctor
     *
     * @param x x
     *
     */
    public Test2(int x) {

        myX = x;
    }

    /**
     *  This creates and returns a dom that looks like:
     *  <object class="ucar.unidata.xml.Test2">
     *       <constructor>
     *         <int>myX</int>
     *       </constructor>
     *  </object>
     *
     * @param encoder encoder
     * @return Dom element
     */
    public Element createElement(XmlEncoder encoder) {

        // Because we have a constructor that takes a primitive type
        // we use 2 arrays to hold the argument data and type
        // so we can use the Integer.TYPE class
        ArrayList arguments = new ArrayList();
        ArrayList types     = new ArrayList();
        arguments.add(new Integer(myX));
        types.add(Integer.TYPE);

        // Create the "<object class=...>" tag
        Element result = encoder.createObjectElement(getClass());

        // Now create the "<constructor> tag with the given arguments and types
        Element ctorElement = encoder.createConstructorElement(arguments,
                                  types);
        result.appendChild(ctorElement);

        return result;
    }

    /**
     *  Do nothing, return true to tell the encoder that it is ok to process
     *  any methods or properties.
     *
     * @param encoder encoder
     * @param node node
     * @return boolean value
     */
    public boolean initFromXml(XmlEncoder encoder, Element node) {

        return true;
    }
}

























