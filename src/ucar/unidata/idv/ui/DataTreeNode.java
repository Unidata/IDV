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

package ucar.unidata.idv.ui;


import ucar.unidata.data.CompositeDataChoice;

import javax.swing.tree.*;


/**
 * This class is the node used in the DataTree and holds an object
 * which may be a DataSource, DataCategory or DataChoice
 *
 * @author IDV development team
 * @version $Revision: 1.17 $Date: 2007/04/11 15:58:21 $
 */
public class DataTreeNode extends DefaultMutableTreeNode {


    /** The data tree I am part of */
    private DataTree dataTree;

    /**
     * Have I creatred my subtee. We have this here for the dynamically
     *   expanding composite data choices
     */
    private boolean hasCreatedSubtree = false;


    /** The object this node holds */
    private Object object;

    /**
     *   Does this node hold a DerivedDataChoice as its object.
     *   The node itself doesn't use this information, just
     *   other code.
     */
    private boolean isDerived = false;


    /**
     * Create a new tree node with the given label
     *
     *
     * @param dataTree My data tree
     * @param label The node's label
     *
     */
    public DataTreeNode(DataTree dataTree, String label) {
        this(dataTree, label, (Object) null);
    }

    /**
     * Create a new tree node with the given label and data object
     *
     *
     * @param dataTree My data tree
     * @param label The node's label
     * @param object The data object
     *
     */
    public DataTreeNode(DataTree dataTree, String label, Object object) {
        super(label);
        this.dataTree = dataTree;
        this.object   = object;
    }

    /**
     * Expand if needed
     *
     * @param dataTree The tree
     */
    public void checkExpansion(DataTree dataTree) {
        if ( !hasCreatedSubtree) {
            if (object instanceof CompositeDataChoice) {
                hasCreatedSubtree = true;
                removeAllChildren();
                CompositeDataChoice comp = (CompositeDataChoice) object;
                dataTree.getIdv().getIdvUIManager().showWaitCursor();
                comp.expandIfNeeded();
                dataTree.getIdv().getIdvUIManager().showNormalCursor();
                dataTree.createSubtree(comp, this);
            }
        }
    }

    /**
     * Does this node hold a DerivedDataChoice
     *
     * @return Holds a DerivedDataChoice
     */
    public boolean getIsDerived() {
        return isDerived;
    }

    /**
     * Set if  this node hold a DerivedDataChoice
     *
     * @param b Does it hold a DDC
     */
    public void setIsDerived(boolean b) {
        isDerived = b;
    }

    /**
     * Set the label to be used for display
     *
     * @param label The display label
     */
    public void setLabel(String label) {
        setUserObject(label);
    }

    /**
     * Return the object this node holds
     *
     * @return The data object
     */
    public Object getObject() {
        return object;
    }


}
