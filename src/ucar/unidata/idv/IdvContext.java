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

package ucar.unidata.idv;


/**
 * A base context interface for some common methods
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.13 $Date: 2006/12/27 20:14:07 $
 */

public interface IdvContext {

    /**
     * Get the given property name and convert to to a boolean.
     * If not found return dflt
     *
     * @param name Property to look up
     * @param dflt Default value to use if not found
     * @return The property as a boolean or the dflt value if not found
     */
    public boolean getProperty(String name, boolean dflt);

    /**
     * Get the given property name as a String
     * If not found return dflt
     *
     * @param name Property to look up
     * @param dflt Default value to use if not found
     * @return The property as a String or the dflt value if not found
     */
    public String getProperty(String name, String dflt);


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public java.awt.Color getColorProperty(String name, java.awt.Color dflt);


}
