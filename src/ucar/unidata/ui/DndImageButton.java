/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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



import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;

import javax.swing.JLabel;


/**
 * The Class DndImageButton.
 *
 * @author Jeff McWhirter
 */
public class DndImageButton extends JLabel implements DragSourceListener,
        DragGestureListener, Transferable {

    /** The drag source. */
    DragSource dragSource = null;

    /** The data. */
    Object data;

    /** The mime type. */
    String mimeType;

    /** The icon name. */
    private static String iconName = "/ucar/unidata/ui/images/dnd.gif";
    //    private static String iconName = "/auxdata/ui/icons/Move16.gif";

    /**
     * Instantiates a new dnd image button.
     *
     * @param label the label
     * @param data the data
     * @param mimeType the mime type
     */
    public DndImageButton(String label, Object data, String mimeType) {
        super(label);
        init(data, mimeType);
    }

    /**
     * Instantiates a new dnd image button.
     *
     * @param data the data
     * @param mimeType the mime type
     */
    public DndImageButton(Object data, String mimeType) {
        this(data, mimeType, iconName);
    }

    /**
     * Instantiates a new dnd image button.
     *
     * @param data the data
     * @param mimeType the mime type
     * @param icon the icon
     */
    public DndImageButton(Object data, String mimeType, String icon) {
        super(GuiUtils.getImageIcon(icon, false));
        init(data, mimeType);
    }

    /**
     * Inits the.
     *
     * @param data the data
     * @param mimeType the mime type
     */
    private void init(Object data, String mimeType) {
        this.mimeType = mimeType;
        this.data     = data;
        dragSource    = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this,
                DnDConstants.ACTION_MOVE, this);
        setToolTipText("Click to drag-and-drop");
    }

    /**
     * {@inheritDoc}
     */
    public void dragGestureRecognized(DragGestureEvent event) {
        dragSource.startDrag(event, DragSource.DefaultMoveDrop, this, this);
    }

    /**
     * {@inheritDoc}
     */
    public void dragDropEnd(DragSourceDropEvent event) {
        //      IllegalArgumentException iae = new  IllegalArgumentException ("****DONE****");
        //      iae.printStackTrace ();
        //      System.err.println ("dragDropEnd");
    }

    /**
     * {@inheritDoc}
     */
    public void dragEnter(DragSourceDragEvent event) {}

    /**
     * {@inheritDoc}
     */
    public void dragExit(DragSourceEvent event) {}

    /**
     * {@inheritDoc}
     */
    public void dragOver(DragSourceDragEvent event) {}

    /**
     * {@inheritDoc}
     */
    public void dropActionChanged(DragSourceDragEvent event) {}

    /**
     * {@inheritDoc}
     */
    public Object getTransferData(DataFlavor flavor) {
        return data;
    }

    /**
     * {@inheritDoc}
     */
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { new ObjectFlavor(data) };
        //        return new DataFlavor[] { new DataFlavor(mimeType, mimeType) };
    }

    //TODO:Fix this

    /**
     * {@inheritDoc}
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return true;
    }
}
