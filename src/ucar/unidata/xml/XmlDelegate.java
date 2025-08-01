/*
 * $Id: XmlDelegate.java,v 1.6 2005/05/13 18:33:53 jeffmc Exp $
 *
 * Copyright  1997-2025 Unidata Program Center/University Corporation for
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
 * An interface for an object that knows how to handle the xml encoding of  certain classes of objects.
 * @author Metapps development team
 * @version $Revision: 1.6 $Date: 2005/05/13 18:33:53 $
 */



public interface XmlDelegate {

    /**
     *  Create the xml element for the given object.
     *
     *  @param encoder The XmlEncoder that is calling this method.
     *  @param object The Object to encode.
     *  @return The xml element that defines the given object.
     */
    public Element createElement(XmlEncoder encoder, Object object);

    /**
     *  Create the Object defined by the given xml element.
     *  @param encoder The XmlEncoder that is calling this method.
     *  @param element The xml that defines the object.
     *  @return The Object defined by the xml.
     */

    public Object createObject(XmlEncoder encoder, Element element);
}





