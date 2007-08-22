/*
 * $Id: XmlObjectFactory.java,v 1.6 2005/05/13 18:33:54 jeffmc Exp $
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



package ucar.unidata.xml;



import org.w3c.dom.Element;



/**
 * One can define within the encoded object xml the class of an object that implements XmlObjectFactory.
 * This factory object is instantiated and used to create the object.
 * @author Metapps development team
 * @version $Revision: 1.6 $Date: 2005/05/13 18:33:54 $
 */

public interface XmlObjectFactory {

    /**
     * Returns an object corresponding to an XML element.
     *
     * @param encoder                   The XML encoder.
     * @param node                       The XML element to be decoded.
     * @return                          The object corresponding to the XML
     *                                  element.
     */
    public Object getObject(XmlEncoder encoder, Element node);
}





