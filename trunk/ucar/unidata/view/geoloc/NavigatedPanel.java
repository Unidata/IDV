/**
 * $Id: NavigatedPanel.java,v 1.60 2007/07/25 21:56:52 jeffmc Exp $
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


package ucar.unidata.view.geoloc;


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.ui.BAMutil;
import ucar.unidata.ui.Rubberband;
import ucar.unidata.ui.RubberbandRectangle;

import ucar.unidata.ui.drawing.Glyph;

import ucar.unidata.util.Format;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.ListenerManager;



import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.print.*;

import java.io.*;

import java.util.*;

import javax.swing.*;


/**
 * Implements a "navigated" JPanel within which a user can zoom and pan.
 *
 * The mapping of the screen area to world coordinates is called "navigation", and
 * it's NavigatedPanel's job to keep track of the navigation as the user zooms and pans.
 * It throws NewMapAreaEvent to indicate that the user has changed the Map area,
 * and the display needs to be redrawn. It throws PickEvents when the user double clicks
 * on the panel. <br>
 * <br>
 * NavigatedPanel has a standard JToolbar that can be displayed.
 * It also implements a "reference" point and fast updating of the
 * status of the mouse position relative to the reference point. <br>
 * <br>
 * A user typically adds a NavigatedPanel and its toolbar to its frame/applet, and
 * registers itself for NewMapAreaEvent's. When an event occurs, the user obtains
 * a Graphics2D (through the getBufferedImageGraphics() method) to draw into. The
 * AffineTransform of the Graphics2D has been set correctly to map projection coords
 * to screen coords, based on the current zoomed and panned Map area.
 * The renderer can use the AffineTransform if needed, but more typically just works in
 * projection coordinates. The renderer can also get a clipping rectangle by calling
 * g.getClip() and casting the Shape to a Rectangle2D, eg:
 * <pre>
 *       Rectangle2D clipRect = (Rectangle2D) g.getClip();
 * </pre>
 *
 * Our "world coordinates" are the same as java2D's "user coordinates".
 * In general, the world coordinate plane is a projective geometry surface, typically
 * in units of "km on the projection surface".
 * The transformation from lat/lon to the projection plane is handled by a ProjectionImpl object.
 * If a user selects a different projection, NavigatedPanel.setProjection() should be called.
 * The default projection is "Cylindrical Equidistant" or "LatLon" which simply maps lat/lon
 * degrees linearly to screen coordinates. A peculiarity of this projection is that the "seam"
 * of the cylinder shifts as the user pans around. Currently our implementation sends
 * a NewMapAreaEvent whenever this happens. <br>
 * <br>
 * <br>
 *
 * @author John Caron
 * @version $Id: NavigatedPanel.java,v 1.60 2007/07/25 21:56:52 jeffmc Exp $
 */

public class NavigatedPanel extends JPanel implements MouseListener,
        MouseMotionListener, KeyListener {


    /** _more_          */
    private static Color disabledColor = new Color(230, 230, 230);


    /* Implementation Notes:
       NavigatedPanel uses an image to buffer the image.
    */

    // public actions;

    /** set reference action */
    public AbstractAction setReferenceAction;

    // main delegates

    /** navigation */
    private Navigation navigate = null;

    /** projection used */
    private ProjectionImpl project = null;

    /** manage pick listeners */
    private ListenerManager lmPick;

    /** manage move listeners */
    private ListenerManager lmMove;

    /** Should we be able to select a region */
    private boolean selectRegionMode = false;

    /** This tells us what rectangle point we are moving */
    private String regionDragPoint = null;

    /** Work object when we are dragging */
    private Rectangle2D screenRegion;

    /** The selected region */
    //    private LatLonRect selectedRegion;
    private ProjectionRect selectedRegion;

    //    private LatLonRect selectedRegionBounds;   

    /** Bounds of the user selected region */
    private ProjectionRect selectedRegionBounds;

    // ui stuff

    /** image for buffering */
    private BufferedImage bImage = null;

    /** default background color */
    private Color backColor = Color.white;

    /** status label */
    private JLabel statusLabel = null;

    /** image observer */
    private myImageObserver imageObs = new myImageObserver();

    /** navigation toolbar */
    private NToolBar toolbar = null;

    /** menu for menus */
    private JMenu menu;

    /** scheduled redraw */
    private javax.swing.Timer redrawTimer = null;

    // state flags

    /** flag for changes since drawn */
    private boolean changedSinceDraw = true;

    /** flag for users changes */
    private boolean changeable = true;

    /** dragging and zooming state */
    protected int startX, startY, deltax, deltay;

    /** flag for dragging mode */
    private boolean draggingMode = false;

    /** flag for zooming mode */
    private boolean zoomingMode = false;

    /** rubber band */
    private Rubberband zoomRB;


    /** track reference point */
    private boolean setReferenceMode = false,
                    hasReference     = false;

    /** working world reference point */
    private ProjectionPointImpl refWorld = new ProjectionPointImpl();

    /** working lat/lon point */
    private LatLonPointImpl refLatLon = new LatLonPointImpl();

    /** working screen point */
    private Point2D refScreen = new Point2D.Double();

    /** reference size */
    private int referenceSize = 12;

    /** reference cursor */
    private Cursor referenceCursor = null;

    /** reference cursor flag */
    private static final int REFERENCE_CURSOR = -100;

    // some working objects to minimize excessive garbage collection

    /** working string buffer */
    private StringBuffer sbuff = new StringBuffer(100);

    /** working projection point */
    private ProjectionPointImpl workW = new ProjectionPointImpl();

    /** working lat/lon point */
    private LatLonPointImpl workL = new LatLonPointImpl();

    /** working bearing */
    private Bearing workB = new Bearing();

    /** working bounds rectangle */
    private Rectangle myBounds = new Rectangle();

    /** working bounding box */
    private ProjectionRect boundingBox = new ProjectionRect();

    // DnD

    /** drag and drop drop target */
    private DropTarget dropTarget;

    //debug

    /** debug counter */
    private int repaintCount = 0;

    /** debug flags */
    private final boolean
        debugDraw   = false,
        debugEvent  = false,
        debugThread = false,
        debugStatus = false;

    /** more debug flags */
    private final boolean
        debugTime     = false,
        debugPrinting = false,
        debugBB       = false,
        debugZoom     = false;

    /** debug bounding box flag */
    private final boolean debugBounds = false;

    /** The default constructor. */
    public NavigatedPanel() {
        setDoubleBuffered(false);  // we do our own dubble buffer

        // default navigation and projection
        navigate = new Navigation();
        project  = new LatLonProjection("Cyl.Eq");  // default projection
        navigate.setMapArea(normalizeRectangle(project.getDefaultMapArea()));

        // toolbar actions
        makeActions();

        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        this.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent event) {
                int notches = event.getWheelRotation();
                if (notches < 0) {
                    doZoomIn();
                } else {
                    doZoomOut();
                }
            }
        });



        // catch resize events
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                setNewBounds(getBounds(), false);
            }
        });

        // rubberbanding
        zoomRB = (Rubberband) new RubberbandRectangle(this);

        // DnD
        /*
          dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY,
          new myDropTargetListener());
        */


        lmMove = new ucar.unidata.util.ListenerManager(
            "ucar.unidata.view.geoloc.CursorMoveEventListener",
            "ucar.unidata.view.geoloc.CursorMoveEvent", "actionPerformed");
    }


    /**
     * _more_
     *
     * @param enabled _more_
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setNewBounds(getBounds(), true);
        repaint();
    }


    /**
     * Utility to create a lmpick
     *
     * @return lmpick
     */
    private ListenerManager getLMPick() {
        if (lmPick == null) {
            lmPick = new ucar.unidata.util.ListenerManager(
                "ucar.unidata.view.geoloc.PickEventListener",
                "ucar.unidata.view.geoloc.PickEvent", "actionPerformed");
        }
        return lmPick;
    }






    /**
     * Register a CursorMoveEventListener.
     *
     * @param l  listener to register
     */
    public void addCursorMoveEventListener(CursorMoveEventListener l) {
        lmMove.addListener(l);
    }

    /**
     * Remove a CursorMoveEventListener.
     *
     * @param l  listener to remove
     */
    public void removeCursorMoveEventListener(CursorMoveEventListener l) {
        lmMove.removeListener(l);
    }

    /**
     * Register a PickEventListener.
     *
     * @param l  listener to register
     */
    public void addPickEventListener(PickEventListener l) {
        getLMPick().addListener(l);
    }

    /**
     * Remove a PickEventListener.
     *
     * @param l  listener to remove
     */
    public void removePickEventListener(PickEventListener l) {
        getLMPick().removeListener(l);
    }

    /**
     * Register a NewMapAreaListener.
     *
     * @param l  listener to register
     */
    public void addNewMapAreaListener(NewMapAreaListener l) {
        navigate.addNewMapAreaListener(l);
    }

    /**
     * Remove a NewMapAreaListener.
     *
     * @param l  listener to remove
     */
    public void removeNewMapAreaListener(NewMapAreaListener l) {
        navigate.removeNewMapAreaListener(l);
    }

    // accessor methods






    /**
     * Get the background color of the NavigatedPanel.
     *
     * @return background color
     */
    public Color getBackgroundColor() {
        return (isEnabled()
                ? backColor
                : disabledColor);
    }

    /**
     * Return the Navigation held by this object.
     * @return the Navigation
     */
    public Navigation getNavigation() {
        return navigate;
    }

    /*
     * Set the Map Area by converting LatLonBoundingBox to a MapArea.
     *   @param  llbb the LatLonBoundingBox
     *   public void setLatLonBoundingBox(LatLonRect llbb) {
     *   if (debugBB) System.out.println("setLatLonBoundingBox "+ llbb);
     *   navigate.setMapArea( project.latLonToProjBB(llbb));
     *   }
     */

    /**
     * Get the current Map Area.
     * @return the current Map Area
     */
    public ProjectionRect getMapArea() {
        if (debugBB) {
            System.out.println("NPgetMapArea "
                               + navigate.getMapArea(boundingBox));
        }
        return navigate.getMapArea(boundingBox);
    }

    /**
     * Set the Map Area.
     * @param ma  the MapArea
     */
    public void setMapArea(ProjectionRect ma) {
        if (debugBB) {
            System.out.println("NPsetMapArea " + ma);
        }
        navigate.setMapArea(normalizeRectangle(ma));
    }

    /**
     * kludgy thing to shift LatLon seam
     *
     * @param wx_center  world center point
     */
    public void setWorldCenterX(double wx_center) {
        navigate.setWorldCenterX(wx_center);
    }


    /**
     * Return whether the focus is traversable
     * @return true if so
     */
    public boolean isFocusTraversable() {
        return true;
    }


    /**
     * Get the current Projection.
     *
     * @return the current Projection
     */
    public ProjectionImpl getProjectionImpl() {
        return project;
    }

    /**
     * If the projection is a LatLonProjection then this routine
     * normalizes the rectangle to be between -180/180
     *
     * @param bb Incoming rectangle
     *
     * @return The input bb if not in LatLon, else the bb normalized
     */
    public ProjectionRect normalizeRectangle(ProjectionRect bb) {


        if ((bb == null) || (project == null)
            || !project.isLatLon()) {
            return bb;
        }
        ProjectionRect newRect          = new ProjectionRect(bb);
        double         maxLon           = newRect.x + newRect.width;
        double         normalizedMaxLon = LatLonPointImpl.lonNormal(maxLon);


        newRect.x += (normalizedMaxLon - maxLon);

        double         minLon           = newRect.x;
        double         normalizedMinLon = LatLonPointImpl.lonNormal(minLon);

        newRect.x += (normalizedMinLon - minLon);


        //Try to normalize the rectangle
        while(newRect.x+newRect.width>360) {
            newRect.x-= 360;
        }

        return newRect;
    }




    /**
     * Set the Projection, change the Map Area to the projection's default
     *
     * @param p the Projection
     */
    public void setProjectionImpl(ProjectionImpl p) {
        // switch projections
        project = p;
        navigate.setMapArea(normalizeRectangle(project.getDefaultMapArea()));
        // transfer reference point to new coord system
        if (hasReference) {
            refWorld.setLocation(project.latLonToProj(refLatLon));
        }
    }

    /**
     * The status label is where the lat/lon position of the mouse
     * is displayed. May be null.
     *
     * @param l  the Jlabel to write into
     */
    public void setPositionLabel(JLabel l) {
        this.statusLabel = l;
    }

    /**
     * Get the navigation toolbar for this panel
     *
     * @return the "Navigate" toolbar
     */
    public JToolBar getNavToolBar() {
        return new NToolBar();
    }

    /**
     * Get the move toolbar
     *
     * @return the "Move" toolbar
     */
    public JToolBar getMoveToolBar() {
        return new MoveToolBar();
    }

    /**
     * Add all of the toolbar's actions to a menu.
     * @param menu the menu to add the actions to
     */
    public void addActionsToMenu(JMenu menu) {
        BAMutil.addActionToMenu(menu, zoomIn);
        BAMutil.addActionToMenu(menu, zoomOut);
        BAMutil.addActionToMenu(menu, zoomBack);
        BAMutil.addActionToMenu(menu, zoomDefault);

        menu.addSeparator();

        BAMutil.addActionToMenu(menu, moveUp);
        BAMutil.addActionToMenu(menu, moveDown);
        BAMutil.addActionToMenu(menu, moveRight);
        BAMutil.addActionToMenu(menu, moveLeft);

        menu.addSeparator();

        BAMutil.addActionToMenu(menu, setReferenceAction);
    }

    /**
     * Make sure we dont get overwhelmed by redraw calls
     * from panning, so wait delay msecs before doing the redraw.
     *
     * @param delay  time delay (milliseconds)
     */
    private void redrawLater(int delay) {
        boolean already = (redrawTimer != null) && (redrawTimer.isRunning());
        if (debugThread) {
            System.out.println("redrawLater isRunning= " + already);
        }
        if (already) {
            return;
        }

        // initialize Timer the first time
        if (redrawTimer == null) {
            redrawTimer = new javax.swing.Timer(0, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    drawG();
                    redrawTimer.stop();  // one-shot timer
                }
            });
        }
        // start the timer running
        redrawTimer.setDelay(delay);
        redrawTimer.start();
    }

    /**
     * Sets whether the user can zoom/pan on this NavigatedPanel. Default = true.
     * @param mode  set to false if user can't zoom/pan
     */
    public void setChangeable(boolean mode) {
        if (mode == changeable) {
            return;
        }
        changeable = mode;
        if (toolbar != null) {
            toolbar.setEnabled(mode);
        }
    }

    /**
     * Catch repaints - for debugging
     *
     * @param tm       not used
     * @param x        x value of the dirty region
     * @param y        y value of the dirty region
     * @param width    width of the dirty region
     * @param height   height of the dirty region
     */
    // note: I believe that the RepaintManager is not used on JPanel subclasses ???
    public void repaint(long tm, int x, int y, int width, int height) {
        if (debugDraw) {
            System.out.println("REPAINT " + repaintCount + " x " + x + " y "
                               + y + " width " + width + " heit " + height);
        }
        if (debugThread) {
            System.out.println(" thread = " + Thread.currentThread());
        }
        repaintCount++;
        super.repaint(tm, x, y, width, height);
    }

    /**
     * System-triggered redraw.
     *
     * @param g    graphics to paint
     */
    public void paintComponent(Graphics g) {
        if (debugDraw) {
            System.out.println("System called paintComponent clip= "
                               + g.getClipBounds());
        }
        draw((Graphics2D) g);
    }

    /**
     * This is used to do some fancy tricks with double buffering
     * @return buffered image
     */
    public BufferedImage getBufferedImage() {
        return bImage;
    }

    /**
     * User must get this Graphics2D and draw into it when panel needs redrawing
     * @return get the graphics for the buffered image
     */
    public Graphics2D getBufferedImageGraphics() {
        if (bImage == null) {
            return null;
        }
        Graphics2D g2 = bImage.createGraphics();

        // set clipping rectangle into boundingBox
        navigate.getMapArea(boundingBox);
        if (debugBB) {
            System.out.println(" getBufferedImageGraphics BB = "
                               + boundingBox);
        }

        // set graphics attributes
        g2.setTransform(navigate.getTransform());
        g2.setStroke(new BasicStroke(0.0f));  // default stroke size is one pixel
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_SPEED);
        g2.setClip(boundingBox);  // normalized coord system, because transform is applied

        Color foo = Color.red;
        //        g2.setBackground(backColor);
        g2.setBackground(foo);

        return g2;
    }

    //////////////////////// printing ////////////////////////////////

    /**
     * utility routine for printing.
     * @param pwidth    width of the page, units are arbitrary
     * @param pheight   height of the page, units are arbitrary
     * @return true if we want to rotate the page
     */
    public boolean wantRotate(double pwidth, double pheight) {
        return navigate.wantRotate(pwidth, pheight);
    }

    /**
     * This calculates the Affine Transform that maps the current map area
     * (in Projection Coordinates) to a display area (in arbitrary units).
     * @param rotate should the page be rotated?
     * @param displayX  upper right X coord of corner of display area
     * @param displayY  upper right Y coord of corner of display area
     * @param displayWidth  display area width
     * @param displayHeight display area height
     *
     * @see Navigation#calcTransform
     * @return transform
     */
    public AffineTransform calcTransform(boolean rotate, double displayX,
                                         double displayY,
                                         double displayWidth,
                                         double displayHeight) {
        return navigate.calcTransform(rotate, displayX, displayY,
                                      displayWidth, displayHeight);
    }


    // LOOK! change this to an inner class ?

    /*
     * Render to a printer. part of Printable interface
     *   @param g        the Graphics context
     *   @param pf       describes the page format
     *   @param pi       page number
     *
     * @param b
     *
     *   public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
     *   if (pi >= 1) {
     *   return Printable.NO_SUCH_PAGE;
     *   }
     *   Graphics2D g2 = (Graphics2D) g;
     *   g2.setColor(Color.black);
     *
     *   double pheight = pf.getImageableHeight();
     *   double pwidth = pf.getImageableWidth();
     *   double px = pf.getImageableX();
     *   double py = pf.getImageableY();
     *   g2.drawRect( (int) px, (int) py, (int) pwidth, (int)pheight);
     *
     *   AffineTransform orgAT = g2.getTransform();
     *   if (debugPrinting) System.out.println(" org transform = "+orgAT);
     *
     *   //  set clipping rectangle LOOK ????
     *   //navigate.getMapArea( boundingBox);
     *   //g2.setClip( boundingBox);
     *
     *   boolean rotate = navigate.wantRotate(pwidth, pheight);
     *   AffineTransform at2 = navigate.calcTransform(rotate, px, py, pwidth, pheight);
     *   g2.transform( at2);
     *   AffineTransform at = g2.getTransform();
     *   if (debugPrinting) System.out.println(" use transform = "+at);
     *   double scale = at.getScaleX();
     *
     *   // if we need to rotate, also rotate the original transform
     *   if (rotate)
     *   orgAT.rotate( -Math.PI/2, px + pwidth/2, py + pheight/2);
     *
     *   // set graphics attributes                           // LOOK! hanging printer
     *   //g2.setStroke(new BasicStroke((float)(2.0/scale)));  // default stroke size is two pixels
     *   g2.setStroke(new BasicStroke(0.0f));  // default stroke size is two pixels
     *   g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
     *   g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
     *
     *   // draw the image to the buffer
     *   //render.draw(g2, orgAT);
     *
     *   if (debugPrinting) {
     *   System.out.println("  Graphics clip "+ g2.getClipBounds());
     *   System.out.println("  Page Format     "+ pf.getOrientation());
     *   System.out.println("  getH/W          "+ pf.getHeight()+ " "+ pf.getWidth());
     *   System.out.println("  getImageableH/W "+ pf.getImageableHeight()+ " "+ pf.getImageableWidth());
     *   System.out.println("  getImageableX/Y "+ pf.getImageableX()+ " "+ pf.getImageableY());
     *
     *   /* Paper paper = pf.getPaper();
     *   System.out.println("  Paper     ");
     *   System.out.println("  getH/W          "+ paper.getHeight()+ " "+ paper.getWidth());
     *   System.out.println("  getImageableH/W "+ paper.getImageableHeight()+ " "+ paper.getImageableWidth());
     *   System.out.println("  getImageableX/Y "+ paper.getImageableX()+ " "+ paper.getImageableY());
     *   }
     *
     *   return Printable.PAGE_EXISTS;
     *   }
     */

    ///////////////////////////////////////////////////////////////////////////////////
    // private methods

    // when component resizes we need a new buffer

    /**
     * Set the new bounds
     *
     * @param b new bounds
     * @param force _more_
     */
    private void setNewBounds(Rectangle b, boolean force) {
        boolean sameSize = (b.width == myBounds.width)
                           && (b.height == myBounds.height);
        if (debugBounds) {
            System.out.println("NavigatedPanel setBounds old= " + myBounds);
        }
        if ( !force && sameSize && (b.x == myBounds.x)
                && (b.y == myBounds.y)) {
            return;
        }

        myBounds.setBounds(b);
        if ( !force && sameSize) {
            return;
        }

        if (debugBounds) {
            System.out.println("  newBounds = " + b);
        }

        // create new buffer the size of the window
        //if (bImage != null)
        //  bImage.dispose();

        if ((b.width > 0) && (b.height > 0)) {
            bImage = new BufferedImage(b.width, b.height,
                                       BufferedImage.TYPE_INT_RGB);  // why RGB ?
        } else {  // why not device dependent?
            bImage = null;
        }
        navigate.setScreenSize(b.width, b.height);
    }

    // draw and drawG are like "paintImmediately()"

    /**
     * Draw on the default graphics
     */
    public void drawG() {
        Graphics g = getGraphics();  // bypasses double buffering ?
        if (null != g) {
            draw((Graphics2D) g);
            g.dispose();
        }
    }

    /**
     * Draw on the specified graphics
     *
     * @param g
     */
    private void draw(Graphics2D g) {
        if (bImage == null) {
            return;
        }
        boolean   redrawReference = true;
        Rectangle bounds          = getBounds();


        Color     color           = getBackgroundColor();

        if (draggingMode) {
            if (debugDraw) {
                System.out.println("draw draggingMode ");
            }
            // Clear the image.
            g.setBackground(color);
            g.clearRect(0, 0, bounds.width, bounds.height);
            g.drawImage(bImage, deltax, deltay, color, imageObs);
            redrawReference = false;
        } else {
            if (debugDraw) {
                System.out.println("draw copy ");
            }
            g.drawImage(bImage, 0, 0, color, imageObs);
        }

        if (hasReference && redrawReference) {
            refWorld.setLocation(project.latLonToProj(refLatLon));
            navigate.worldToScreen(refWorld, refScreen);
            int px = (int) refScreen.getX();
            int py = (int) refScreen.getY();
            g.setColor(Color.red);
            g.setStroke(new BasicStroke(2.0f));
            // g.drawImage( referenceCursor.getImage(), px, py, Color.red, imageObs);
            g.drawLine(px, py - referenceSize, px, py + referenceSize);
            g.drawLine(px - referenceSize, py, px + referenceSize, py);
        }

        if (selectedRegion != null) {
            Rectangle2D screenRect = navigate.worldToScreen(selectedRegion);
            g.setColor(Color.cyan);
            Stroke stroke = g.getStroke();
            g.setStroke(new BasicStroke(2.0f));
            g.draw(screenRect);
            g.setStroke(stroke);
            if (selectRegionMode) {
                g.setColor(Color.black);
                Glyph.paintSelectionPoints(g, screenRect, 6);
            }
        }

        // clean up
        changedSinceDraw = false;
    }

    /**
     * Utility to convert Java screen coordinates to earth coordinates
     *
     * @param r screen
     *
     * @return earth
     */
    private LatLonRect screenToEarth(RectangularShape r) {
        LatLonPoint ul = screenToEarth(new Point2D.Double(r.getX(),
                             r.getY()));
        LatLonPoint lr = screenToEarth(new Point2D.Double(r.getX()
                             + r.getWidth(), r.getY() + r.getHeight()));
        LatLonPoint ur = screenToEarth(new Point2D.Double(r.getX()
                             + r.getWidth(), r.getY()));
        LatLonPoint ll = screenToEarth(new Point2D.Double(r.getX(),
                             r.getY() + r.getHeight()));
        //        return new LatLonRect(ul, lr);
        return new LatLonRect(ll, ur);
    }

    /**
     * Utility to convert earth coordinates to Java screen coordinates
     *
     * @param llr Earth
     *
     * @return Screen
     */
    private Rectangle2D earthToScreen(LatLonRect llr) {
        if (llr == null) {
            return null;
        }
        Point2D ul = earthToScreen(llr.getUpperLeftPoint());
        Point2D lr = earthToScreen(llr.getLowerRightPoint());
        Point2D ll = earthToScreen(llr.getLowerLeftPoint());
        Point2D ur = earthToScreen(llr.getUpperRightPoint());

        /*
        int     x1 = (int) Math.min(ul.getX(), lr.getX());
        int     x2 = (int) Math.max(ul.getX(), lr.getX());
        int     y1 = (int) Math.min(ul.getY(), lr.getY());
        int     y2 = (int) Math.max(ul.getY(), lr.getY());
        */

        int x1 = (int) Math.min(ll.getX(), ur.getX());
        int x2 = (int) Math.max(ll.getX(), ur.getX());
        int y1 = (int) Math.min(ll.getY(), ur.getY());
        int y2 = (int) Math.max(ll.getY(), ur.getY());
        return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
    }


    /**
     * Utility to convert Java screen coordinates to earth coordinates
     *
     * @param p Screen point
     *
     * @return Earth point
     */
    public LatLonPoint screenToEarth(Point2D p) {
        ProjectionPointImpl ppi = navigate.screenToWorld(p,
                                      new ProjectionPointImpl());
        return project.projToLatLon(ppi, new LatLonPointImpl());
    }


    /**
     * Utility to convert earth coordinates to Java screen coordinates
     *
     * @param llp Earth
     *
     * @return Screen
     */
    public Point2D earthToScreen(LatLonPoint llp) {
        ProjectionPointImpl ppi =
            (ProjectionPointImpl) project.latLonToProj(llp,
                new ProjectionPointImpl());
        return navigate.worldToScreen(ppi);
    }


    /**
     * Set the cursor to the specified cursor
     *
     * @param what   cursor to specify (e.g., REFERENCE_CURSOR)
     */
    private void setCursor(int what) {
        if (what == REFERENCE_CURSOR) {
            if (null == referenceCursor) {
                referenceCursor =
                    ucar.unidata.ui.BAMutil.makeCursor("ReferencePoint");
                if (null == referenceCursor) {
                    return;
                }
            }
            super.setCursor(referenceCursor);
        } else {
            super.setCursor(Cursor.getPredefinedCursor(what));
        }
    }

    /**
     * Show the status at the mouse coordinates
     *
     * @param mousex    mouse x position
     * @param mousey    mouse y position
     */
    private void showStatus(int mousex, int mousey) {
        if ((statusLabel == null) && !lmMove.hasListeners()) {
            return;
        }
        navigate.screenToWorld(new Point2D.Double(mousex, mousey), workW);
        workL.set(project.projToLatLon(workW));
        if (lmMove.hasListeners()) {
            lmMove.sendEvent(new CursorMoveEvent(this, workW));
        }

        if (statusLabel == null) {
            return;
        }

        sbuff.setLength(0);
        sbuff.append(workL.toString());
        if (ucar.unidata.util.Debug.isSet("projection.showPosition")) {
            sbuff.append(" " + workW);
        }
        if (hasReference) {
            Bearing.calculateBearing(refLatLon, workL, workB);
            sbuff.append("  (");
            sbuff.append(Format.dfrac(workB.getAngle(), 0));
            sbuff.append(" deg ");
            sbuff.append(Format.d(workB.getDistance(), 4, 5));
            sbuff.append(" km)");
        }

        statusLabel.setText(sbuff.toString());
    }

    /**
     * Set whether we are in the reference mode or not
     */
    private void setReferenceMode() {
        if (setReferenceMode) {  // toggle
            setReferenceMode = false;
            setCursor(Cursor.DEFAULT_CURSOR);
            statusLabel.setToolTipText("position at cursor");
            drawG();
        } else {
            hasReference     = false;
            setReferenceMode = true;
            setCursor(Cursor.CROSSHAIR_CURSOR);
            statusLabel.setToolTipText("position (bearing)");
        }
    }



    /**
     * Handle mouse clicked event
     *
     * @param e  event to handle
     */
    public void mouseClicked(MouseEvent e) {
        if ( !isEnabled()) {
            return;
        }
        // pick event
        if (SwingUtilities.isRightMouseButton(e)) {
            LatLonPoint llp = screenToEarth(new Point2D.Double(e.getX(),
                                  e.getY()));
            return;
        }

        if ( !setReferenceMode) {
            navigate.screenToWorld(new Point2D.Double(e.getX(), e.getY()),
                                   workW);
            if (lmPick != null) {
                lmPick.sendEvent(new PickEvent(NavigatedPanel.this, workW,
                        e));
            }
        }
    }




    /**
     * Are we in select region mode for the given event
     *
     * @param e event
     *
     * @return should be selecting
     */
    public boolean shouldSelectRegion(MouseEvent e) {
        return (selectRegionMode && !SwingUtilities.isRightMouseButton(e)
                && !e.isControlDown() && !e.isShiftDown());
    }


    /**
     * Change earth rect to world rect
     *
     * @param llr earth
     *
     * @return world rect
     */
    public ProjectionRect earthToWorld(LatLonRect llr) {
        Rectangle2D screenRegion = earthToScreen(llr);
        return new ProjectionRect(navigate.screenToWorld(screenRegion));
    }



    /**
     * Handle the mouse pressed event
     *
     * @param e  event to handle
     */
    public void mousePressed(MouseEvent e) {
        if ( !isEnabled()) {
            return;
        }
        requestFocus();
        startX          = e.getX();
        startY          = e.getY();
        regionDragPoint = null;

        if (shouldSelectRegion(e)) {
            if (selectedRegion == null) {
                screenRegion = new Rectangle2D.Double(startX - 2, startY - 2,
                        4, 4);
                selectedRegion =
                    new ProjectionRect(navigate.screenToWorld(screenRegion));
                regionDragPoint = Glyph.PT_LR;
            } else {
                screenRegion = navigate.worldToScreen(selectedRegion);
                double minDistance = GuiUtils.distance((double) startX,
                                         (double) startY, screenRegion);
                if (minDistance < 25) {
                    regionDragPoint = Glyph.getStretchPoint(screenRegion,
                            startX, startY);
                }
            }
            if (regionDragPoint != null) {
                setCursor(Glyph.getCursor(regionDragPoint));
            }
            selectedRegionChanged();
            return;
        }



        if ( !changeable) {
            return;
        }




        if (setReferenceMode && !SwingUtilities.isRightMouseButton(e)) {
            hasReference = true;
            refScreen.setLocation(startX, startY);
            navigate.screenToWorld(refScreen, refWorld);
            refLatLon.set(project.projToLatLon(refWorld));
            setCursor(REFERENCE_CURSOR);
            drawG();
        } else {

            if ( !SwingUtilities.isRightMouseButton(e)) {
                zoomRB.setActive(true);
                zoomRB.anchor(e.getPoint());
                zoomingMode = true;
            } else {
                draggingMode = true;
                setCursor(Cursor.MOVE_CURSOR);
            }
        }

        if (debugEvent) {
            System.out.println("mousePressed " + startX + " " + startY);
        }
    }


    /**
     * Handle the selected region changed.  Subclasses should implement
     */
    protected void selectedRegionChanged() {
        repaint();
    }


    /**
     * Handle the mouse released event.
     *
     * @param e  event to handle
     */
    public void mouseReleased(MouseEvent e) {
        if ( !isEnabled()) {
            return;
        }

        if (shouldSelectRegion(e) && (screenRegion != null)) {
            selectedRegion =
                new ProjectionRect(navigate.screenToWorld(screenRegion));
            selectedRegionChanged();
        }


        if ( !changeable) {
            return;
        }

        setCursor(Cursor.DEFAULT_CURSOR);
        deltax = e.getX() - startX;
        deltay = e.getY() - startY;
        if (debugEvent) {
            System.out.println("mouseReleased " + e.getX() + " " + e.getY()
                               + "=" + deltax + " " + deltay);
        }

        if (selectRegionMode) {
            //      return;
        }
        if (draggingMode) {
            navigate.pan(-deltax, -deltay);
            draggingMode = false;
            setCursor(Cursor.DEFAULT_CURSOR);
        }


        if (zoomingMode) {
            zoomRB.setActive(false);
            zoomRB.end(e.getPoint());
            zoomingMode = false;
            if (debugZoom) {
                System.out.println("mouseReleased " + e.getX() + " "
                                   + e.getY() + " bounds = " + myBounds);
            }
            if ( !myBounds.contains(e.getPoint())) {  // point is off the screen
                return;
            }
            // "start" must be upper left
            startX = Math.min(startX, e.getX());
            startY = Math.min(startY, e.getY());
            if ((lmPick != null) && (e.isControlDown() || e.isShiftDown())) {
                //This is a region select, not a zoom
                int w = Math.abs(deltax);
                int h = Math.abs(deltay);
                Rectangle2D screenRect = new Rectangle2D.Double(startX,
                                             startY, w, h);
                lmPick.sendEvent(new PickEvent(NavigatedPanel.this,
                        screenRect, e));
            } else {
                navigate.zoom(startX, startY, Math.abs(deltax),
                              Math.abs(deltay));
            }
        }
        //drawG();
    }


    /**
     * Handle the mouse dragged event.
     *
     * @param e  event to handle
     */
    public void mouseDragged(MouseEvent e) {
        if ( !isEnabled()) {
            return;
        }

        if (regionDragPoint != null) {
            int       newX = e.getX();
            int       newY = e.getY();
            Rectangle r    = Glyph.toRect(screenRegion);
            if (regionDragPoint.equals(Glyph.PT_CENTER)) {
                regionDragPoint = Glyph.stretchTo(r, newX - startX,
                        newY - startY, regionDragPoint);
            } else {
                regionDragPoint = Glyph.stretchTo(r, newX, newY,
                        regionDragPoint);
            }
            screenRegion = new Rectangle2D.Double(r.x, r.y, r.width,
                    r.height);

            clipScreenRegion();
            selectedRegion =
                new ProjectionRect(navigate.screenToWorld(screenRegion));
            selectedRegionChanged();
            startX = newX;
            startY = newY;
            repaint();
            return;
        }


        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        if ( !changeable) {
            return;
        }
        if (zoomingMode) {
            return;  // handled by Rubberband class
        }

        deltax = e.getX() - startX;
        deltay = e.getY() - startY;
        if (debugEvent) {
            System.out.println("mouseDragged " + e.getX() + " " + e.getY()
                               + "=" + deltax + " " + deltay);
        }
        repaint();
        //redrawLater(100); // schedule redraw in 100 msecs
    }

    /**
     * Clip to screen region
     */
    private void clipScreenRegion() {
        if (selectedRegionBounds != null) {
            Rectangle2D bounds  =
                navigate.worldToScreen(selectedRegionBounds);
            Rectangle2D clipped = bounds.createIntersection(screenRegion);
            screenRegion = new Rectangle2D.Double(clipped.getX(),
                    clipped.getY(), clipped.getWidth(), clipped.getHeight());
        }
    }

    /**
     * Handle the mouse moved event.
     *
     * @param e  event to handle
     */
    public void mouseMoved(MouseEvent e) {
        if ( !isEnabled()) {
            return;
        }
        if ( !draggingMode) {
            showStatus(e.getX(), e.getY());
        }
    }




    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseExited(MouseEvent e) {}



    /**
     * Noop
     *
     * @param e The event
     */
    public void keyPressed(KeyEvent e) {
        if (selectRegionMode) {
            if (GuiUtils.isDeleteEvent(e)) {
                selectedRegion = null;
                selectedRegionChanged();
                repaint();
            } else if ((e.getKeyCode() == KeyEvent.VK_R)
                       && e.isControlDown()) {
                selectedRegion = getMapArea();
                screenRegion   = navigate.worldToScreen(selectedRegion);
                clipScreenRegion();
                selectedRegion =
                    new ProjectionRect(navigate.screenToWorld(screenRegion));
                selectedRegionChanged();
                repaint();

            }
        }
    }




    /**
     * Noop
     *
     * @param e The event
     */
    public void keyReleased(KeyEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void keyTyped(KeyEvent e) {}





    /**
     * Class myImageObserver
     *
     * @author Unidata development team
     */
    private class myImageObserver implements ImageObserver {

        /**
         * Called when the image needs updating.
         *
         * @param image    the image being observed.
         * @param flags    the bitwise inclusive OR of the following flags:
         *                 WIDTH, HEIGHT, PROPERTIES, SOMEBITS, FRAMEBITS,
         *                 ALLBITS, ERROR, ABORT.
         * @param x        the x coordinate
         * @param y        the y coordinate
         * @param width    the width
         * @param height   the height
         * @return true
         */
        public boolean imageUpdate(Image image, int flags, int x, int y,
                                   int width, int height) {
            return true;
        }
    }

    //DnD

    /**
     * Class myDropTargetListener
     *
     * @author Unidata development team
     */
    private class myDropTargetListener implements DropTargetListener {

        /**
         * Called while a drag operation is ongoing, when the mouse pointer
         * enters the operable part of the drop site for the DropTarget
         * registered with this listener.
         *
         * @param e  the DropTargetDragEvent to handle
         */
        public void dragEnter(DropTargetDragEvent e) {
            System.out.println(" NP dragEnter active = "
                               + dropTarget.isActive());
            e.acceptDrag(DnDConstants.ACTION_COPY);
        }

        /**
         * Called when the drag operation has terminated with a drop on the
         * operable part of the drop site for the DropTarget  registered
         * with this listener.
         *
         * @param e  the DropTargetDropEvent to handle
         */
        public void drop(DropTargetDropEvent e) {
            try {
                if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    Transferable tr = e.getTransferable();
                    e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    String s =
                        (String) tr.getTransferData(DataFlavor.stringFlavor);
                    //dropList.add(s);
                    System.out.println(" NP myDropTargetListener got " + s);
                    e.dropComplete(true);
                } else {
                    e.rejectDrop();
                }
            } catch (IOException io) {
                io.printStackTrace();
                e.rejectDrop();
            } catch (UnsupportedFlavorException ufe) {
                ufe.printStackTrace();
                e.rejectDrop();
            }
        }

        /**
         * Called while a drag operation is ongoing, when the mouse pointer
         * has exited the operable part of the drop site for the DropTarget
         * registered with this listener.
         *
         * @param e  the DropTargetEvent to handle
         */
        public void dragExit(DropTargetEvent e) {}

        /**
         * Called when a drag operation is ongoing, while the mouse pointer
         * is still over the operable part of the drop site for the
         * DropTarget  registered with this listener.
         *
         * @param e  the DropTargetDragEvent to handle
         */
        public void dragOver(DropTargetDragEvent e) {}

        /**
         * Called if the user has modified the current drop gesture.
         *
         * @param e  the DropTargetDragEvent to handle
         */
        public void dropActionChanged(DropTargetDragEvent e) {}
    }

    //////////////////////////////////////////////////////////////////////////////
    // toolbars

    /** toolbar actions */
    private AbstractAction zoomIn, zoomOut, zoomDefault, zoomBack;

    /** more toolbar actions */
    private AbstractAction moveUp, moveDown, moveLeft, moveRight;

    /**
     * Zoom in
     */
    public void doZoomIn() {
        navigate.zoomIn();
        drawG();
    }


    /**
     * Zoom out
     */
    public void doZoomOut() {
        navigate.zoomOut();
        drawG();
    }

    /**
     * Zoom by the given factor
     *
     * @param zoomFactor zoom factor
     */
    public void zoom(double zoomFactor) {
        navigate.zoom(zoomFactor);
        drawG();
    }

    /**
     * Translate up
     *
     * @param factor by
     */
    public void doMoveUp(double factor) {
        navigate.moveUp(factor);
        drawG();
    }

    /**
     * Translate down
     *
     * @param factor by
     */
    public void doMoveDown(double factor) {
        navigate.moveDown(factor);
        drawG();
    }

    /**
     * Translate  right
     *
     * @param factor by
     */
    public void doMoveRight(double factor) {
        navigate.moveRight(factor);
        drawG();
    }

    /**
     * Translate left
     *
     * @param factor by
     */
    public void doMoveLeft(double factor) {
        navigate.moveLeft(factor);
        drawG();
    }



    /**
     * Make the default actions
     */
    private void makeActions() {
        // add buttons/actions
        zoomIn = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doZoomIn();
            }
        };
        BAMutil.setActionProperties(zoomIn, "ZoomIn16", "Zoom In", false,
                                    'I', KeyEvent.VK_ADD);

        zoomOut = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doZoomOut();
            }
        };
        BAMutil.setActionProperties(zoomOut, "ZoomOut16", "Zoom Out", false,
                                    'O', KeyEvent.VK_SUBTRACT);

        zoomBack = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                navigate.zoomPrevious();
                drawG();
            }
        };
        BAMutil.setActionProperties(zoomBack, "Undo16", "Previous map area",
                                    false, 'P', KeyEvent.VK_BACK_SPACE);

        zoomDefault = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                resetZoom();
            }
        };
        BAMutil.setActionProperties(zoomDefault, "Home16", "Home map area",
                                    false, 'H', KeyEvent.VK_HOME);

        moveUp = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doMoveUp(2);
            }
        };
        BAMutil.setActionProperties(moveUp, "Up16", "Move view up", false,
                                    'U', KeyEvent.VK_UP);

        moveDown = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doMoveDown(2);
            }
        };
        BAMutil.setActionProperties(moveDown, "Down16", "Move view down",
                                    false, 'D', KeyEvent.VK_DOWN);

        moveLeft = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doMoveLeft(2);
            }
        };
        BAMutil.setActionProperties(moveLeft, "Left16", "Move view left",
                                    false, 'L', KeyEvent.VK_LEFT);

        moveRight = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                doMoveRight(2);
            }
        };
        BAMutil.setActionProperties(moveRight, "Right16", "Move view right",
                                    false, 'R', KeyEvent.VK_RIGHT);

        setReferenceAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setReferenceMode();
                drawG();
            }
        };
        BAMutil.setActionProperties(setReferenceAction, "ReferencePoint",
                                    "Set reference Point", true, 'P', 0);
    }

    /**
     *  Reset the  zoom and projection
     */
    public void resetZoom() {
        navigate.setMapArea(normalizeRectangle(project.getDefaultMapArea()));
        drawG();
    }



    /**
     * Class NToolBar - toolbar for navigation
     *
     * @author Unidata development team
     */
    class NToolBar extends JToolBar {

        /**
         * Create a new toolbar for zooming
         */
        NToolBar() {
            setFloatable(false);
            AbstractButton b = BAMutil.addActionToContainer(this, zoomIn);
            b.setName("zoomIn");

            b = BAMutil.addActionToContainer(this, zoomOut);
            b.setName("zoomOut");

            b = BAMutil.addActionToContainer(this, zoomBack);
            b.setName("zoomBack");

            b = BAMutil.addActionToContainer(this, zoomDefault);
            b.setName("zoomHome");
        }
    }

    /**
     * Class MoveToolBar  - toolbar for moving (panning, etc)
     *
     * @author Unidata development team
     */
    class MoveToolBar extends JToolBar {

        /**
         * Create a new toobar for moving
         *
         */
        MoveToolBar() {
            setFloatable(false);
            AbstractButton b = BAMutil.addActionToContainer(this, moveUp);
            b.setName("moveUp");

            b = BAMutil.addActionToContainer(this, moveDown);
            b.setName("moveDown");

            b = BAMutil.addActionToContainer(this, moveLeft);
            b.setName("moveLeft");

            b = BAMutil.addActionToContainer(this, moveRight);
            b.setName("moveRight");
        }
    }

    /*   public void setEnabled( boolean mode) {
         for (int i=0; i< getComponentCount(); i++) {
         Component c = getComponentAtIndex(i);
         c.setEnabled( mode);
         }
         }

         public void remove(String which) {
         // find which
         for (int i=0; i< getComponentCount(); i++) {
         Component c = getComponentAtIndex(i);
         if (which.equals(c.getName()))
         remove(c);
         }
         }

         } // end inner class */

    /**
     * Set the SelectRegionMode property.
     *
     * @param value The new value for SelectRegionMode
     */
    public void setSelectRegionMode(boolean value) {
        selectRegionMode = value;
        if (value) {
            setToolTipText(
                "<html>Drag to select region<br>Press 'Delete' (or Control-D) to clear selected region<br>Press Control-R to reset selected region</html>");
        }
    }


    /**
     * Define a bounding rectangle, in world coordinates, that the
     * selection region is limited to.
     *
     * @param bounds The bounds
     */
    public void setSelectedRegionBounds(ProjectionRect bounds) {
        selectedRegionBounds = normalizeRectangle(bounds);
    }



    /**
     * Get the SelectRegionMode property.
     *
     * @return The SelectRegionMode
     */
    public boolean getSelectRegionMode() {
        return selectRegionMode;
    }


    /**
     * Set the SelectedRegion property.
     *
     * @param value The new value for SelectedRegion
     */
    public void setSelectedRegion(ProjectionRect value) {
        selectedRegion = value;
    }


    /**
     * Set the SelectedRegion property.
     *
     *
     * @param llr The new region
     */
    public void setSelectedRegion(LatLonRect llr) {
        if (llr == null) {
            setSelectedRegion((ProjectionRect) null);
        } else {
            setSelectedRegion(earthToWorld(llr));
        }
    }



    /**
     * Get the SelectedRegion property.
     *
     * @return The SelectedRegion
     */
    public ProjectionRect getSelectedRegion() {
        return selectedRegion;
    }




    /**
     * Get the SelectedRegion property.
     *
     * @return The SelectedRegion
     */
    public LatLonRect getSelectedEarthRegion() {
        if (selectedRegion == null) {
            return null;
        }
        return screenToEarth(navigate.worldToScreen(selectedRegion));
    }




}

