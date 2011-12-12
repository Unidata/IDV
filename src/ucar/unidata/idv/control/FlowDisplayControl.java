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

package ucar.unidata.idv.control;


/**
 * A plan view control for flow data (vector or wind barbs)
 *
 * @author IDV Development Team
 * @version $Revision: 1.65 $
 */
public interface FlowDisplayControl {


    /** property for sharing flow range */
    public static final String SHARE_FLOWRANGE =
        "FlowDisplayControl.SHARE_FLOWRANGE";

    /** property for sharing flow scale */
    public static final String SHARE_FLOWSCALE =
        "FlowDisplayControl.SHARE_FLOWRANGE";

}
