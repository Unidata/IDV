/*
 * $Id: PrototypeManager.java,v 1.2 2006/05/05 19:19:37 jeffmc Exp $
 *
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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



package ucar.unidata.util;



/**
 * An interface for writing out protoypes
 * @author IDV development group.
 *
 * @version $Revision: 1.2 $
 */
public interface PrototypeManager {

    /**
     * Write the object as a prototype
     *
     * @param object the object
     */
    public void writePrototype(Object object);

    /**
     * Try to read in the prototype for the given class
     *
     * @param c The class
     *
     * @return The prototype object or null
     */
    public Object getPrototype(Class c);
}

