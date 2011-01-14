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


/**
 * A wrapper around {@link ucar.unidata.idv.chooser.adde.AddeImageChooser}
 *
 *
 *
 * @deprecated use AddeImageChooser directly
 * @author IDV development team
 * @version $Revision: 1.40 $Date: 2007/07/06 20:40:17 $
 */

public class ImageChooser extends ucar.unidata.idv.chooser.adde
    .AddeImageChooser {

    /**
     * ctor
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public ImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }

}
