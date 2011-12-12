/*
 * $Id: GraphPaperLayout.java,v 1.12 2007/07/06 20:45:30 jeffmc Exp $
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

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * The <code>GraphPaperLayout</code> class is a layout manager that
 * lays out a container's components in a rectangular grid, similar
 * to GridLayout.  Unlike GridLayout, however, components can take
 * up multiple rows and/or columns.  The layout manager acts as a
 * sheet of graph paper.  When a component is added to the layout
 * manager, the location and relative size of the component are
 * simply supplied by the constraints as a Rectangle.
 * <p><code><pre>
 * import java.awt.*;
 * import java.applet.Applet;
 * public class ButtonGrid extends Applet {
 *     public void init() {
 *         setLayout(new GraphPaperLayout(new Dimension(5,5)));
 *         // Add a 1x1 Rect at (0,0)
 *         add(new Button("1"), new Rectangle(0,0,1,1));
 *         // Add a 2x1 Rect at (2,0)
 *         add(new Button("2"), new Rectangle(2,0,2,1));
 *         // Add a 1x2 Rect at (1,1)
 *         add(new Button("3"), new Rectangle(1,1,1,2));
 *         // Add a 2x2 Rect at (3,2)
 *         add(new Button("4"), new Rectangle(3,2,2,2));
 *         // Add a 1x1 Rect at (0,4)
 *         add(new Button("5"), new Rectangle(0,4,1,1));
 *         // Add a 1x2 Rect at (2,3)
 *         add(new Button("6"), new Rectangle(2,3,1,2));
 *     }
 * }
 * </pre></code>
 *
 * @author      Michael Martak
 */

public class GraphPaperLayout implements LayoutManager2 {

    /** _more_ */
    int hgap;  //horizontal gap

    /** _more_ */
    int vgap;  //vertical gap

    /** _more_ */
    Dimension gridSize;  //grid size in logical units (n x m)

    /** _more_ */
    Hashtable compTable;  //constraints (Rectangles)

    /**
     * Creates a graph paper layout with a default of a 1 x 1 graph, with no
     * vertical or horizontal padding.
     */
    public GraphPaperLayout() {
        this(new Dimension(1, 1));
    }

    /**
     * Creates a graph paper layout with the given grid size, with no vertical
     * or horizontal padding.
     *
     * @param gridSize
     */
    public GraphPaperLayout(Dimension gridSize) {
        this(gridSize, 0, 0);
    }

    /**
     * Creates a graph paper layout with the given grid size and padding.
     * @param gridSize size of the graph paper in logical units (n x m)
     * @param hgap horizontal padding
     * @param vgap vertical padding
     */
    public GraphPaperLayout(Dimension gridSize, int hgap, int vgap) {
        if ((gridSize.width <= 0) || (gridSize.height <= 0)) {
            throw new IllegalArgumentException(
                "dimensions must be greater than zero");
        }
        this.gridSize = new Dimension(gridSize);
        this.hgap     = hgap;
        this.vgap     = vgap;
        compTable     = new Hashtable();
    }

    /**
     * @return the size of the graph paper in logical units (n x m)
     */
    public Dimension getGridSize() {
        return new Dimension(gridSize);
    }

    /**
     * Set the size of the graph paper in logical units (n x m)
     *
     * @param d
     */
    public void setGridSize(Dimension d) {
        setGridSize(d.width, d.height);
    }

    /**
     * Set the size of the graph paper in logical units (n x m)
     *
     * @param width
     * @param height
     */
    public void setGridSize(int width, int height) {
        gridSize = new Dimension(width, height);
    }

    /**
     * _more_
     *
     * @param comp
     * @param constraints
     */
    public void setConstraints(Component comp, Rectangle constraints) {
        compTable.put(comp, new Rectangle(constraints));
    }

    /**
     * Adds the specified component with the specified name to
     * the layout.  This does nothing in GraphPaperLayout, since constraints
     * are required.
     *
     * @param name
     * @param comp
     */
    public void addLayoutComponent(String name, Component comp) {}

    /**
     * Removes the specified component from the layout.
     * @param comp the component to be removed
     */
    public void removeLayoutComponent(Component comp) {
        compTable.remove(comp);
    }

    /**
     * Calculates the preferred size dimensions for the specified
     * panel given the components in the specified parent container.
     * @param parent the component to be laid out
     *
     * @see #minimumLayoutSize
     * @return _more_
     */
    public Dimension preferredLayoutSize(Container parent) {
        return getLayoutSize(parent, true);
    }

    /**
     * Calculates the minimum size dimensions for the specified
     * panel given the components in the specified parent container.
     * @param parent the component to be laid out
     * @see #preferredLayoutSize
     * @return _more_
     */
    public Dimension minimumLayoutSize(Container parent) {
        return getLayoutSize(parent, false);
    }

    /**
     * Algorithm for calculating layout size (minimum or preferred).
     * <p>
     * The width of a graph paper layout is the largest cell width
     * (calculated in <code>getLargestCellSize()</code> times the number of
     * columns, plus the horizontal padding times the number of columns
     * plus one, plus the left and right insets of the target container.
     * <p>
     * The height of a graph paper layout is the largest cell height
     * (calculated in <code>getLargestCellSize()</code> times the number of
     * rows, plus the vertical padding times the number of rows
     * plus one, plus the top and bottom insets of the target container.
     *
     * @param parent the container in which to do the layout.
     * @param isPreferred true for calculating preferred size, false for
     *                    calculating minimum size.
     * @return the dimensions to lay out the subcomponents of the specified
     *         container.
     * @see #getLargestCellSize
     */
    protected Dimension getLayoutSize(Container parent, boolean isPreferred) {
        Dimension largestSize = getLargestCellSize(parent, isPreferred);
        Insets    insets      = parent.getInsets();
        largestSize.width = (largestSize.width * gridSize.width)
                            + (hgap * (gridSize.width + 1)) + insets.left
                            + insets.right;
        largestSize.height = (largestSize.height * gridSize.height)
                             + (vgap * (gridSize.height + 1)) + insets.top
                             + insets.bottom;
        return largestSize;
    }

    /**
     * Algorithm for calculating the largest minimum or preferred cell size.
     * <p>
     * Largest cell size is calculated by getting the applicable size of each
     * component and keeping the maximum value, dividing the component's width
     * by the number of columns it is specified to occupy and dividing the
     * component's height by the number of rows it is specified to occupy.
     *
     * @param parent the container in which to do the layout.
     * @param isPreferred true for calculating preferred size, false for
     *                    calculating minimum size.
     * @return the largest cell size required.
     */
    protected Dimension getLargestCellSize(Container parent,
                                           boolean isPreferred) {
        int       ncomponents = parent.getComponentCount();
        Dimension maxCellSize = new Dimension(0, 0);
        for (int i = 0; i < ncomponents; i++) {
            Component c    = parent.getComponent(i);
            Rectangle rect = (Rectangle) compTable.get(c);
            if ((c != null) && (rect != null)) {
                Dimension componentSize;
                if (isPreferred) {
                    componentSize = c.getPreferredSize();
                } else {
                    componentSize = c.getMinimumSize();
                }
                // Note: rect dimensions are already asserted to be > 0 when the
                // component is added with constraints
                maxCellSize.width = Math.max(maxCellSize.width,
                                             componentSize.width
                                             / rect.width);
                maxCellSize.height = Math.max(maxCellSize.height,
                        componentSize.height / rect.height);
            }
        }
        return maxCellSize;
    }

    /**
     * Lays out the container in the specified container.
     * @param parent the component which needs to be laid out
     */
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets      = parent.getInsets();
            int    ncomponents = parent.getComponentCount();

            if (ncomponents == 0) {
                return;
            }

            // Total parent dimensions
            Dimension size   = parent.getSize();
            int       totalW = size.width - (insets.left + insets.right);
            int       totalH = size.height - (insets.top + insets.bottom);

            // Cell dimensions, including padding
            int totalCellW = totalW / gridSize.width;
            int totalCellH = totalH / gridSize.height;

            // Cell dimensions, without padding
            int cellW = (totalW - ((gridSize.width + 1) * hgap))
                        / gridSize.width;
            int cellH = (totalH - ((gridSize.height + 1) * vgap))
                        / gridSize.height;

            for (int i = 0; i < ncomponents; i++) {
                Component c    = parent.getComponent(i);
                Rectangle rect = (Rectangle) compTable.get(c);
                if (rect != null) {
                    int x = insets.left + (totalCellW * rect.x) + hgap;
                    int y = insets.top + (totalCellH * rect.y) + vgap;
                    int w = (cellW * rect.width) - hgap;
                    int h = (cellH * rect.height) - vgap;
                    c.setBounds(x, y, w, h);
                }
            }
        }
    }

    // LayoutManager2 /////////////////////////////////////////////////////////

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     * @param comp the component to be added
     * @param constraints  where/how the component is added to the layout.
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof Rectangle) {
            Rectangle rect = (Rectangle) constraints;
            if ((rect.width <= 0) || (rect.height <= 0)) {
                throw new IllegalArgumentException(
                    "cannot add to layout: rectangle must have positive width and height");
            }
            if ((rect.x < 0) || (rect.y < 0)) {
                throw new IllegalArgumentException(
                    "cannot add to layout: rectangle x and y must be >= 0");
            }
            setConstraints(comp, rect);
        } else if (constraints != null) {
            throw new IllegalArgumentException(
                "cannot add to layout: constraint must be a Rectangle");
        }
    }

    /**
     * Returns the maximum size of this component.
     * @see java.awt.Component#getMinimumSize()
     * @see java.awt.Component#getPreferredSize()
     * @see LayoutManager
     *
     * @param target
     * @return _more_
     */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param target
     * @return _more_
     */
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param target
     * @return _more_
     */
    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     *
     * @param target
     */
    public void invalidateLayout(Container target) {
        // Do nothing
    }


    /**
     * Class Location _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.12 $
     */
    public static class Location {

        /** _more_          */
        Rectangle rect;

        /** _more_          */
        Rectangle displayRectangle;

        /** _more_          */
        JComponent comp;

        /** _more_          */
        String label;

        /** _more_          */
        Image image;

        /** _more_          */
        Object object;

        /**
         * _more_
         *
         * @param comp _more_
         * @param object _more_
         * @param label _more_
         */
        public Location(JComponent comp, Object object, String label) {
            this.comp   = comp;
            this.object = object;
            this.label  = label;
            if (comp != null) {
                try {
                    //                    comp.setSize(comp.getPreferredSize());
                    image = ImageUtils.getImage(comp);
                } catch (Exception exc) {
                    //                    System.err.println ("err " + exc);
                }
            }
        }

        /**
         * _more_
         *
         * @param comp _more_
         * @param object _more_
         * @param label _more_
         * @param rect _more_
         */
        public Location(JComponent comp, Object object, String label,
                        Rectangle rect) {
            this(comp, object, label);
            this.rect = rect;
        }

        /**
         * _more_
         *
         * @param comp _more_
         * @param label _more_
         * @param x _more_
         * @param y _more_
         * @param width _more_
         * @param height _more_
         */
        public Location(JComponent comp, String label, int x, int y,
                        int width, int height) {
            this(comp, null, label, new Rectangle(x, y, width, height));
        }

        /**
         * _more_
         *
         * @param comp _more_
         * @param x _more_
         * @param y _more_
         */
        public Location(JComponent comp, int x, int y) {
            this(comp, null, "", new Rectangle(x, y, 1, 1));
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public String getLabel() {
            return label;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public JComponent getComponent() {
            return comp;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Rectangle getRect() {
            return rect;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Object getObject() {
            return object;
        }


    }

    /**
     * _more_
     *
     * @param locations _more_
     *
     * @return _more_
     */
    public static Dimension getDimensions(List locations) {
        int  maxCol    = -1;
        int  maxRow    = -1;
        List nullComps = new ArrayList();
        for (int i = 0; i < locations.size(); i++) {
            Location l = (Location) locations.get(i);
            if (l.comp == null) {
                continue;
            }
            if (l.rect == null) {
                nullComps.add(l);
                continue;
            }
            maxCol = Math.max(maxCol, l.rect.x + (l.rect.width - 1));
            maxRow = Math.max(maxRow, l.rect.y + (l.rect.height - 1));
        }

        for (int i = 0; i < nullComps.size(); i++) {
            Location l = (Location) nullComps.get(i);
            maxRow++;
            l.rect = new Rectangle(0, maxRow, 1, 1);
        }
        return new Dimension(Math.max(1, maxCol + 1),
                             Math.max(1, maxRow + 1));
    }

    /**
     * _more_
     *
     * @param locations _more_
     *
     * @return _more_
     */
    public static JComponent layout(List locations) {
        Dimension dim   = getDimensions(locations);
        JPanel    panel = new JPanel(new GraphPaperLayout(dim));
        for (int i = 0; i < locations.size(); i++) {
            Location l = (Location) locations.get(i);
            if (l.comp == null) {
                continue;
            }
            panel.add(l.comp, l.rect);
        }
        return panel;
    }



    /**
     * Class EditPanel _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.12 $
     */
    public static class EditPanel implements MouseListener,
                                             MouseMotionListener {

        /** _more_ */
        public static final String DRAG_LEFT = "LEFT";

        /** _more_          */
        public static final String DRAG_RIGHT = "RIGHT";

        /** _more_          */
        public static final String DRAG_TOP = "TOP";

        /** _more_          */
        public static final String DRAG_BOTTOM = "BOTTOM";

        /** _more_          */
        public static final Cursor MOVE_CURSOR =
            new Cursor(Cursor.MOVE_CURSOR);

        /** _more_          */
        public static final Cursor DEFAULT_CURSOR =
            new Cursor(Cursor.DEFAULT_CURSOR);


        /** _more_          */
        private int cols;

        /** _more_          */
        private int rows;

        /** _more_          */
        private List locations = new ArrayList();

        /** _more_          */
        private JComponent contents;

        /** _more_          */
        private JPanel paintPanel;

        /** _more_          */
        private Location currentLocation;

        /** _more_          */
        private String dragPoint;


        /**
         * _more_
         *
         * @param cols _more_
         * @param rows _more_
         * @param locs _more_
         */
        public EditPanel(int cols, int rows, List locs) {
            this.cols      = cols;
            this.rows      = rows;
            this.locations = locs;
            if (locations == null) {
                locations = new ArrayList();
                locations.add(new Location(new JButton("Button"),
                                           "The button", 0, 0, 2, 1));
                locations.add(new Location(new JTextField(" hello there"),
                                           "The field", 0, 1, 1, 1));
            }
            paintPanel = new JPanel() {
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    EditPanel.this.paintComponent(g);
                }
                public void update(Graphics g) {
                    paintComponent(g);
                }
            };
            paintPanel.setPreferredSize(new Dimension(300, 300));
            paintPanel.setBackground(Color.white);
            paintPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            paintPanel.addMouseListener(this);
            paintPanel.addMouseMotionListener(this);

            contents = GuiUtils.centerRight(
                paintPanel,
                GuiUtils.top(
                    GuiUtils.hbox(
                        new JLabel("Columns: "), makeButton("+", true, 1),
                        makeButton("-", true, -1))));
            contents = GuiUtils.centerBottom(
                contents,
                GuiUtils.left(
                    GuiUtils.hbox(
                        new JLabel("Rows: "), makeButton("+", false, 1),
                        makeButton("-", false, -1))));
            contents = GuiUtils.inset(contents, 5);
            if (cols == 0) {
                cols = 1;
            }
            if (rows == 0) {
                rows = 1;
            }
        }

        /**
         * _more_
         *
         * @param lbl _more_
         * @param forCol _more_
         * @param delta _more_
         *
         * @return _more_
         */
        private JButton makeButton(String lbl, boolean forCol, int delta) {
            String method = (forCol
                             ? "deltaColumn"
                             : "deltaRow");
            JButton btn = GuiUtils.makeButton(lbl, this, method,
                              new Integer(delta));
            if (forCol) {
                btn.setToolTipText(((delta > 0)
                                    ? "Add"
                                    : "Remove") + " a column");
            } else {
                btn.setToolTipText(((delta > 0)
                                    ? "Add"
                                    : "Remove") + " a row");
            }
            btn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            Font f = btn.getFont();
            btn.setFont(f.deriveFont((float) (f.getSize() + 2)));
            return btn;
        }

        /**
         * _more_
         *
         * @param i _more_
         */
        public void deltaColumn(Integer i) {
            cols = Math.max(1, cols + i.intValue());
            contents.repaint();
        }

        /**
         * _more_
         *
         * @param i _more_
         */
        public void deltaRow(Integer i) {
            rows = Math.max(1, rows + i.intValue());
            contents.repaint();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public JComponent getContents() {
            return contents;
        }

        /**
         * _more_
         *
         * @param e _more_
         */
        public void mouseDragged(MouseEvent e) {
            if (currentLocation == null) {
                return;
            }

            int col = xToColumn(e.getX());
            int row = yToRow(e.getY());
            if (dragPoint == null) {
                currentLocation.rect.x = col;
                currentLocation.rect.y = row;
            } else {
                if (dragPoint.equals(DRAG_LEFT)) {
                    int dcol = col - currentLocation.rect.x;
                    currentLocation.rect.x = col;
                    currentLocation.rect.width = Math.max(1,
                            currentLocation.rect.width - dcol);

                } else if (dragPoint.equals(DRAG_RIGHT)) {
                    int dcol = col
                               - (currentLocation.rect.x
                                  + (currentLocation.rect.width - 1));
                    currentLocation.rect.width = Math.max(1,
                            currentLocation.rect.width + dcol);
                } else if (dragPoint.equals(DRAG_TOP)) {
                    int drow = row - currentLocation.rect.y;
                    currentLocation.rect.y = row;
                    currentLocation.rect.height = Math.max(1,
                            currentLocation.rect.height - drow);
                } else if (dragPoint.equals(DRAG_BOTTOM)) {
                    int drow = row
                               - (currentLocation.rect.y
                                  + (currentLocation.rect.height - 1));
                    currentLocation.rect.height = Math.max(1,
                            currentLocation.rect.height + drow);
                }
            }
            paintPanel.repaint();
        }

        /**
         * _more_
         *
         * @param e _more_
         */
        public void mouseMoved(MouseEvent e) {}

        /**
         * _more_
         *
         * @param e _more_
         */
        public void mouseClicked(MouseEvent e) {}

        /**
         * _more_
         *
         * @param e _more_
         */
        public void mouseEntered(MouseEvent e) {}

        /**
         * _more_
         *
         * @param e _more_
         */
        public void mouseExited(MouseEvent e) {}

        /**
         * _more_
         *
         * @param e _more_
         */
        public void mouseReleased(MouseEvent e) {
            currentLocation = null;
            paintPanel.repaint();
            paintPanel.setCursor(DEFAULT_CURSOR);
        }


        /**
         * _more_
         *
         * @param e _more_
         */
        public void mousePressed(MouseEvent e) {
            int    x   = e.getX();
            int    y   = e.getY();
            double max = 5;
            currentLocation = null;
            dragPoint       = null;
            for (int i = locations.size() - 1; i >= 0; i--) {
                Location location = (Location) locations.get(i);
                currentLocation = location;
                Rectangle r = location.displayRectangle;
                if (GuiUtils.distance(r.x, r.y + r.height / 2, x, y) < max) {
                    dragPoint = DRAG_LEFT;
                    paintPanel.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
                    return;
                }
                if (GuiUtils.distance(r.x + r.width, r.y + r.height / 2, x,
                                      y) < max) {
                    dragPoint = DRAG_RIGHT;
                    paintPanel.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
                    return;
                }
                if (GuiUtils.distance(r.x + r.width / 2, r.y, x, y) < max) {
                    dragPoint = DRAG_TOP;
                    paintPanel.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
                    return;
                }
                if (GuiUtils.distance(r.x + r.width / 2, r.y + r.height, x,
                                      y) < max) {
                    dragPoint = DRAG_BOTTOM;
                    paintPanel.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
                    return;
                }
                if (r.contains(e.getX(), e.getY())) {
                    currentLocation = location;
                    paintPanel.setCursor(MOVE_CURSOR);
                    return;
                }
            }
            currentLocation = null;
        }




        /**
         * _more_
         *
         * @param x _more_
         *
         * @return _more_
         */
        private int xToColumn(int x) {
            Rectangle b        = paintPanel.getBounds();
            int       colWidth = b.width / cols;
            return Math.min(Math.max(0, x / colWidth), cols - 1);
        }

        /**
         * _more_
         *
         * @param y _more_
         *
         * @return _more_
         */
        private int yToRow(int y) {
            Rectangle b         = paintPanel.getBounds();
            int       rowHeight = b.height / rows;
            return Math.min(Math.max(0, y / rowHeight), rows - 1);
        }

        /**
         * _more_
         *
         * @param g _more_
         */
        public void paintComponent(Graphics g) {
            Rectangle b         = paintPanel.getBounds();
            int       width     = b.width;
            int       height    = b.height;

            int       off       = 4;
            int       off2      = 8;


            int       colWidth  = width / cols;
            int       rowHeight = height / rows;
            g.setColor(Color.gray);
            for (int i = 0; i < cols - 1; i++) {
                int x = colWidth * (i + 1);
                g.drawLine(x, 0, x, b.height);
            }

            for (int i = 0; i < rows - 1; i++) {
                int y = rowHeight * (i + 1);
                g.drawLine(0, y, b.width, y);
            }

            if (currentLocation != null) {
                locations.remove(currentLocation);
                locations.add(currentLocation);
            }

            Color c = Color.lightGray;
            //            c = new Color(c.getRed(), c.getGreen(),c.getBlue(),200);
            //            c = new Color(c.getRed(), c.getGreen(),c.getBlue(),200);
            for (int i = 0; i < locations.size(); i++) {
                Location  location = (Location) locations.get(i);
                Rectangle r        = location.rect;
                int       col      = r.x;
                int       row      = r.y;
                int       left     = col * colWidth + off;
                int       right    = left + r.width * colWidth - off2;
                int       top      = row * rowHeight + off;
                int       bottom   = top + r.height * rowHeight - off2;
                int       w        = right - left;
                int       h        = bottom - top;
                if (location == currentLocation) {
                    g.setColor(Color.black);
                    g.drawRect(left, top, right - left, bottom - top);
                } else {
                    g.setColor(c);
                    g.fillRect(left, top, right - left, bottom - top);
                }
                g.setColor(Color.black);
                location.displayRectangle = new Rectangle(left, top,
                        right - left, bottom - top);
                String s = location.getLabel();
                if (location.image != null) {
                    g.drawImage(location.image, left, top, w, h, null, null);
                }
                g.fillRect(left + w / 2 - 2, top - 2, 4, 4);
                g.fillRect(left + w / 2 - 2, bottom - 2, 4, 4);
                g.fillRect(left - 2, top + h / 2 - 2, 4, 4);
                g.fillRect(right - 2, top + h / 2 - 2, 4, 4);


                if (s != null) {
                    FontMetrics fm = g.getFontMetrics();
                    while ((s.length() > 0)
                            && (fm.stringWidth(s) > (w - 4 - 9))) {
                        s = s.substring(0, s.length() - 1);
                    }
                    if (s.length() > 0) {
                        int sh = (fm.getMaxDescent() + fm.getMaxAscent());
                        int sx = left + 5;
                        int sy = top + 5 + sh;
                        if (location.image != null) {
                            g.setColor(Color.white);
                            g.fillRect(sx - 1, sy - sh - 1,
                                       fm.stringWidth(s) + 2, sh + 2);
                            g.setColor(Color.black);
                            g.drawRect(sx - 1, sy - sh - 1,
                                       fm.stringWidth(s) + 2, sh + 2);
                        }
                        g.setColor(Color.black);
                        g.drawString(s, sx, sy);
                    }
                }
            }
        }
    }


    /**
     * _more_
     *
     * @param locations _more_
     * @param title _more_
     */
    public static void showDialog(List locations, String title) {
        Dimension dim       = GraphPaperLayout.getDimensions(locations);
        EditPanel editPanel = new EditPanel(dim.width, dim.height, locations);
        GuiUtils.showOkCancelDialog(null, title, editPanel.getContents(),
                                    null);
    }



    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        EditPanel editPanel = new EditPanel(4, 3, null);
        JFrame    f         = new JFrame();
        f.getContentPane().add(editPanel.getContents());
        f.pack();
        f.setLocation(100, 100);
        f.show();

    }


}

