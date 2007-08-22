/*
 * $Id: ObjectTreeNode.java,v 1.9 2007/07/06 20:45:32 jeffmc Exp $
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

package ucar.unidata.ui;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;



import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;


import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;





/**
 *
 * @author Metapps development team
 * @version $Revision: 1.9 $Date: 2007/07/06 20:45:32 $
 */


public class ObjectTreeNode extends DefaultMutableTreeNode {

    /** _more_ */
    private static final GuiUtils GU = null;

    /** _more_ */
    private static final LogUtil LU = null;

    /** _more_ */
    private Object object;


    /**
     * _more_
     *
     * @param label
     * @param object
     *
     */
    public ObjectTreeNode(String label, Object object) {
        super(label);
        this.object = object;
    }

    /**
     * _more_
     *
     * @param label
     */
    public void setLabel(String label) {
        setUserObject(label);
    }

    /**
     * _more_
     *
     * @param o
     */
    public void setObject(Object o) {
        object = o;
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getObject() {
        return object;
    }



}

