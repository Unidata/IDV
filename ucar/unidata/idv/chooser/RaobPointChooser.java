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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.adde.AddeRaobPointDataChooser;





/**
 * @deprecated Use {@link ucar.unidata.idv.chooser.adde.AddeRaobPointDataChooser}
 *
 * @author IDV development team
 * @version $Revision: 1.21 $Date: 2007/07/06 20:40:17 $
 */
public class RaobPointChooser extends AddeRaobPointDataChooser {


    /**
     * Construct a <code>Point Chooser</code> using the manager
     * and the root XML that defines this object.
     *
     * @param mgr  <code>IdvChooserManager</code> that controls this chooser.
     * @param root root element of the XML that defines this object
     */
    public RaobPointChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }

}
