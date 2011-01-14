/*
 * $Id: DndImageButton.java,v 1.8 2007/07/06 20:45:29 jeffmc Exp $
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
import ucar.unidata.util.Resource;

import java.awt.*;
import java.awt.datatransfer.*;


import java.awt.dnd.*;
import java.awt.event.*;

import javax.swing.*;


/**
 * @author Jeff McWhirter
 * @version $Id: DndImageButton.java,v 1.8 2007/07/06 20:45:29 jeffmc Exp $
 */
public class DragPanel extends JPanel implements DragSourceListener,
        DragGestureListener, Transferable {

    /** _more_ */
    private DragSource dragSource = null;

    /** _more_ */
    private Object data;

    /** _more_ */
    private String mimeType;


    /**
     * _more_
     *
     * @param data
     * @param component _more_
     *
     */
    public DragPanel(Object data, JComponent component) {
        super(new BorderLayout());
        if (component != null) {
            this.add(BorderLayout.CENTER, component);
        }
        this.mimeType = mimeType;
        this.data     = data;
        dragSource    = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this,
                DnDConstants.ACTION_MOVE, this);
    }

    /**
     * _more_
     *
     * @param event
     */
    public void dragGestureRecognized(DragGestureEvent event) {
        dragSource.startDrag(event, DragSource.DefaultMoveDrop, this, this);
    }

    /**
     * _more_
     *
     * @param event _more_
     */
    public void dropActionChanged(DragSourceDragEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void dragDropEnd(DragSourceDropEvent event) {
        //      IllegalArgumentException iae = new  IllegalArgumentException ("****DONE****");
        //      iae.printStackTrace ();
        //      System.err.println ("dragDropEnd");
    }

    /**
     * _more_
     *
     * @param event
     */
    public void dragEnter(DragSourceDragEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void dragExit(DragSourceEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void dragOver(DragSourceDragEvent event) {}



    /**
     * _more_
     *
     * @param flavor
     * @return _more_
     */
    public Object getTransferData(DataFlavor flavor) {
        return data;
    }

    /**
     * _more_
     * @return _more_
     */
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { new ObjectFlavor(data) };
    }



    //TODO:Fix this

    /**
     * _more_
     *
     * @param flavor
     * @return _more_
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return true;
    }
}

