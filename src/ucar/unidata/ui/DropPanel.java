/*
 * $Id: DndTree.java,v 1.9 2007/07/06 20:45:30 jeffmc Exp $
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




import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import javax.swing.text.*;
import javax.swing.tree.*;


/**
 * A panel that handles drop actions
 * @author IDV Development Team
 * @version $Revision: 1.9 $
 */
public class DropPanel extends JPanel implements  DropTargetListener {

    /** Some object for the DND */
    private DropTarget dropTarget;

    private boolean doBorder;

    /**
     * Create me
     */
    public DropPanel() {
        this(null);
    }

    public DropPanel(JComponent comp) {
        this(comp, true);
    }

    public DropPanel(JComponent comp, boolean doBorder) {
        super(new BorderLayout());
        this.doBorder = doBorder;
        if(doBorder)
            setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        dropTarget = new DropTarget(this, this);
        if(comp!=null) this.add(BorderLayout.CENTER, comp);
    }




    /**
     * Handle the drag enter event
     *
     * @param dtde The event
     */
    public void dragEnter(DropTargetDragEvent dtde) {
        DataFlavor[] flavors = dtde.getCurrentDataFlavors();
        if ((flavors.length == 0) || !(flavors[0] instanceof ObjectFlavor)) {
            return;
        }
        dtde.acceptDrag(DnDConstants.ACTION_MOVE | DnDConstants.ACTION_COPY);
    }

    /**
     * Drag has left the building
     *
     * @param dte The event
     */
    public void dragExit(DropTargetEvent dte) {
        if(doBorder)
            setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
    }



    /**
     * Handle the DND event
     *
     * @param dtde The event
     */
    public void dragOver(DropTargetDragEvent dtde) {
        dtde.acceptDrag(DnDConstants.ACTION_MOVE
                        | DnDConstants.ACTION_COPY);
        Object object = getObject(dtde.getCurrentDataFlavors());
        if(object == null) return;
        if(okToDrop(object)) {
            if(doBorder)
                setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.blue));
        }
    }

    private Object getObject(DataFlavor[] flavors) {
        if(flavors == null || flavors.length==0 || !(flavors[0] instanceof ObjectFlavor)) return null;
        Object object = ((ObjectFlavor)flavors[0]).getObject();        
        return object;
    }

    public boolean okToDrop(Object object) {
        return false;
    }


    /**
     * Handle the drop event
     *
     * @param dtde The event
     */
    public void drop(DropTargetDropEvent dtde) {
        if(doBorder)
            setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        dtde.acceptDrop(DnDConstants.ACTION_MOVE | DnDConstants.ACTION_COPY);
        Object object = getObject(dtde.getCurrentDataFlavors());
        if(!okToDrop(object)) {
            return;
        }

        if(object == null) return;
        handleDrop(object);
    }

    public void handleDrop(Object object) {
    }


    /**
     * Some DND event
     *
     * @param dtde The event_
     */
    public void dropActionChanged(DropTargetDragEvent dtde) {}


}

