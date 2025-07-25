/*
 * $Id: DndImageButton.java,v 1.8 2007/07/06 20:45:29 jeffmc Exp $
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

package ucar.unidata.ui;



import ucar.unidata.util.Resource;

import java.awt.*;
import java.awt.datatransfer.*;


import java.awt.dnd.*;
import java.awt.event.*;

import javax.swing.*;

    /**
     * Holds DnD data
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.9 $
     */
    public class ObjectFlavor extends DataFlavor {

        /** The tree */
        Object object;

        /**
         * Create me
         *
         * @param object the object
         */
        public ObjectFlavor(Object object) {
            super(object.getClass(), "Object");
            this.object = object;
        }
        
        public Object getObject() {
            return object;
        }

    }

