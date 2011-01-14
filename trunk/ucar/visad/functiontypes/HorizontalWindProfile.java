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

package ucar.visad.functiontypes;



import visad.MathType;

import visad.VisADException;


/**
 * Provides support for profiles of the horizontal wind.
 *
 * @author Steven R. Emmerson
 * @version $Id: HorizontalWindProfile.java,v 1.9 2005/05/13 18:34:50 jeffmc Exp $
 */
public abstract class HorizontalWindProfile extends WindProfile {

    /**
     * Constructs from MathType-s for the domain and range.
     *
     * @param domainType        The MathType of the domain.
     * @param rangeType         The MathType of the range.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected HorizontalWindProfile(MathType domainType, MathType rangeType)
            throws VisADException {
        super(domainType, rangeType);
    }
}
