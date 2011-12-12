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
 * Class DndTree provides basic drag-and-drop  facilities within a JTree.
 * To use this facliity  derive a class from this class. Overwrite
 * the okToDrag, okToDrop and doDrop methods. Make sure that the tree nodes
 * in the tree model are DefaultMutableTreeNode-s
 *
 * @author IDV Development Team
 * @version $Revision: 1.9 $
 */
public class DndTree extends JTree implements DragGestureListener,
        DragSourceListener, DropTargetListener {

    /** The node we're dragging */
    private DefaultMutableTreeNode sourceNode;

    /** The node we're currently over */
    private DefaultMutableTreeNode overNode;

    /** _more_ */
    private Point dragPoint;

    /** Some object for the DND */
    private DragSource dragSource;

    /** Some object for the DND */
    private DropTarget dropTarget;

    /**
     * Create me
     */
    public DndTree() {
        init();
    }

    /**
     * Create me with the given model
     *
     * @param treeModel The tree model
     */
    public DndTree(TreeModel treeModel) {
        super(treeModel);
        init();
    }

    /**
     * Initialize
     */
    private void init() {
        dropTarget = new DropTarget(this, this);
        dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(this,
                DnDConstants.ACTION_MOVE | DnDConstants.ACTION_COPY, this);
    }

    /**
     * Ok to drag
     *
     * @param sourceNode The node to drag
     *
     * @return Is it ok to drag the given node
     */
    protected boolean okToDrag(DefaultMutableTreeNode sourceNode) {
        return true;
    }

    /**
     * Ok to drop
     *
     *
     * @param sourceNode From where
     * @param destNode To where
     * @param onNode _more_
     * @return Is it ok to drop the given source node on the dest node.
     */
    protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                               DefaultMutableTreeNode destNode,
                               boolean onNode) {
        return okToDrop(sourceNode, destNode);
    }



    /**
     * Ok to drop
     *
     *
     * @param sourceNode From where
     * @param destNode To where
     * @return Is it ok to drop the given source node on the dest node.
     */
    protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                               DefaultMutableTreeNode destNode) {
        return true;
    }

    /**
     * This is called when the user drops the node
     *
     * @param sourceNode From where
     * @param destNode To where
     * @param onNode _more_
     */
    protected void doDrop(DefaultMutableTreeNode sourceNode,
                          DefaultMutableTreeNode destNode, boolean onNode) {
        doDrop(sourceNode, destNode);
    }


    /**
     * This is called when the user drops the node
     *
     * @param sourceNode From where
     * @param destNode To where
     */
    protected void doDrop(DefaultMutableTreeNode sourceNode,
                          DefaultMutableTreeNode destNode) {}



    /**
     * Initialize the drag
     *
     * @param e The drag event
     */
    public void dragGestureRecognized(DragGestureEvent e) {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return;
        }
        sourceNode = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        if (sourceNode == null) {
            return;
        }
        if ( !okToDrag(sourceNode)) {
            sourceNode = null;
            return;
        }
        Transferable transferable = new TreeTransferable(this, sourceNode);
        Cursor       cursor       = selectCursor(e.getDragAction());
        dragSource.startDrag(e, cursor, transferable, this);
    }

    /**
     * Set the cursor for the drag
     *
     * @param action The drag action
     *
     * @return The drag cursor
     */
    protected Cursor selectCursor(int action) {
        return (action == DnDConstants.ACTION_MOVE)
               ? DragSource.DefaultMoveDrop
               : DragSource.DefaultCopyDrop;
    }


    /**
     * Some DND interface method
     *
     * @param dsde The event
     */
    public void dragDropEnd(DragSourceDropEvent dsde) {}

    /**
     * Some DND interface method
     *
     * @param dsde The event
     */
    public void dragEnter(DragSourceDragEvent dsde) {}

    /**
     * Some DND interface method
     *
     * @param dse The event
     */
    public void dragExit(DragSourceEvent dse) {}

    /**
     * Some DND interface method
     *
     * @param dsde The event
     */
    public void dragOver(DragSourceDragEvent dsde) {}


    /**
     * Some DND interface method
     *
     * @param dsde The event
     */
    public void dropActionChanged(DragSourceDragEvent dsde) {}


    /**
     * Handle the drag enter event
     *
     * @param dtde The event
     */
    public void dragEnter(DropTargetDragEvent dtde) {
        DataFlavor[] flavors = dtde.getCurrentDataFlavors();
        if ((flavors.length == 0) || !(flavors[0] instanceof TreeFlavor)) {
            return;
        }
        //Only accept local dnds
        if (((TreeFlavor) flavors[0]).tree != this) {
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
        overNode  = null;
        dragPoint = null;
        clearSelection();
        repaint();
    }



    /**
     * Handle the DND event
     *
     * @param dtde The event
     */
    public void dragOver(DropTargetDragEvent dtde) {
        if (sourceNode == null) {
            return;
        }
        Point dropPoint = dtde.getLocation();
        TreePath dropPath = getClosestPathForLocation(dropPoint.x,
                                dropPoint.y);
        DefaultMutableTreeNode destNode =
            (DefaultMutableTreeNode) dropPath.getLastPathComponent();
        overNode  = destNode;
        dragPoint = dropPoint;

        clearSelection();
        if (destNode == sourceNode) {
            destNode = null;
        }
        if (destNode != null) {
            boolean on = isOn(dropPoint, destNode);
            if ( !okToDrop(sourceNode, destNode, on)) {
                destNode = null;
            } else {
                if (on) {
                    setSelectionPath(new TreePath(overNode.getPath()));
                } else {}
            }
        }
        if (destNode == null) {
            dtde.rejectDrag();
        } else {
            dtde.acceptDrag(DnDConstants.ACTION_MOVE
                            | DnDConstants.ACTION_COPY);
        }
        repaint();
    }

    /**
     * _more_
     *
     * @param dropPoint _more_
     * @param node _more_
     *
     * @return _more_
     */
    private boolean isOn(Point dropPoint, DefaultMutableTreeNode node) {
        int       row    = getRowForLocation(dropPoint.x, dropPoint.y);
        Rectangle bounds = getRowBounds(row);
        if (bounds == null) {
            return false;

        }
        int delta = (int) ((double) bounds.height * 0.25);
        return (dropPoint.y >= (bounds.y + delta))
               && (dropPoint.y <= (bounds.y + bounds.height - delta));
    }


    /**
     * Handle the drop event
     *
     * @param dtde The event
     */
    public void drop(DropTargetDropEvent dtde) {
        if (sourceNode == null) {
            return;
        }
        dtde.acceptDrop(DnDConstants.ACTION_MOVE | DnDConstants.ACTION_COPY);
        Point dropPoint = dtde.getLocation();
        TreePath dropPath = getClosestPathForLocation(dropPoint.x,
                                dropPoint.y);
        DefaultMutableTreeNode destNode =
            (DefaultMutableTreeNode) dropPath.getLastPathComponent();

        boolean on = false;
        if (destNode != null) {
            on = isOn(dropPoint, destNode);
            if ( !okToDrop(sourceNode, destNode, on)) {
                destNode = null;
            }
        }
        clearSelection();
        if (destNode != null) {
            int row = getRowForPath(new TreePath(overNode.getPath()));
            doDrop(sourceNode, destNode, on);
        }

        dtde.getDropTargetContext().dropComplete(true);
        overNode   = null;
        sourceNode = null;
        dragPoint  = null;
        repaint();
    }

    /**
     * Some DND event
     *
     * @param dtde The event_
     */
    public void dropActionChanged(DropTargetDragEvent dtde) {}


    /**
     * Overwrite paint to draw the drag line
     *
     * @param g The graphics
     */
    public void paint(Graphics g) {
        super.paint(g);
        if (overNode != null) {
            int       row = getRowForPath(new TreePath(overNode.getPath()));
            Rectangle b   = getRowBounds(row);
            if (b != null) {
                g.setColor(Color.black);
                g.drawLine(b.x, b.y + b.height - 1, b.x + 40,
                           b.y + b.height - 1);
                g.drawLine(b.x, b.y + b.height, b.x + 40, b.y + b.height);
            }
            if ((dragPoint != null) && (sourceNode != null)) {
                String text = sourceNode.toString();
                row = getRowForPath(new TreePath(sourceNode.getPath()));
                b   = getRowBounds(row);
                g.setColor(Color.gray);
                g.drawRect(b.x, b.y, b.width, b.height);
            }

        }
    }


    /**
     * Class TreeTransferable
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.9 $
     */
    public static class TreeTransferable implements Transferable {

        /** The tree */
        DndTree tree;

        /** The node */
        DefaultMutableTreeNode treeNode;

        /**
         * Create the transferable
         *
         *
         * @param tree The tree
         * @param treeNode The node being dragged
         */
        public TreeTransferable(DndTree tree,
                                DefaultMutableTreeNode treeNode) {
            this.tree     = tree;
            this.treeNode = treeNode;
        }

        /**
         * Get the data
         *
         * @param flavor The flavor
         *
         * @return The dragged tree node
         */
        public Object getTransferData(DataFlavor flavor) {
            return treeNode;
        }

        /**
         * Get the flavors
         *
         * @return The flavors
         */
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { new TreeFlavor(tree) };
        }

        /**
         * Is flavor supported
         *
         * @param flavor The flavor_
         *
         * @return Is flavor supported
         */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return true;
        }
    }

    /**
     * Class TreeFlavor
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.9 $
     */
    private static class TreeFlavor extends DataFlavor {

        /** The tree */
        DndTree tree;

        /**
         * Create me
         *
         * @param tree The tree
         */
        public TreeFlavor(DndTree tree) {
            super(DndTree.class, "DndTree");
            this.tree = tree;
        }
    }

}

