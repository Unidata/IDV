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

package ucar.visad.display;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.Util;

import visad.*;

import visad.bom.*;

import visad.java2d.*;

import visad.util.HersheyFont;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * Displayable for a color scale in a display.  The scale can be
 * horizontal or vertical and can have labels.
 *
 * @author IDV Development Team
 * @version $Revision: 1.20 $
 */
public class ColorScale extends DisplayableData {

    /**
     * Key for displaying the scale vertically
     */
    public static final int VERTICAL_ORIENT = 0;

    /**
     * Key for displaying the scale horizonatally
     */
    public static final int HORIZONTAL_ORIENT = 1;

    /**
     * Key for primary labeling side
     */
    public static final int PRIMARY = 0;

    /**
     * Key for secondary labeling side
     */
    public static final int SECONDARY = 1;

    /** Top Placement */
    public static final String TOP = "Top";

    /** Bottom Placement */
    public static final String BOTTOM = "Bottom";

    /** Left Placement */
    public static final String LEFT = "Left";

    /** Right Placement */
    public static final String RIGHT = "Right";

    /**
     * Thickness of color scale.  For VERTICAL_ORIENT, this is the
     * X width, for HORIZONTAL_ORIENT, this is the height
     */
    private static float BAR_THICKNESS = .017f;  // percent of view bounds

    /**
     * Height of color scale.  For VERTICAL_ORIENT, this is the
     * X height, for HORIZONTAL_ORIENT, this is the height
     */
    private static float BAR_LENGTH = .80f;  // percent of view bounds

    /**
     * X position of the scale (Java screen coordinates)
     */
    private float X;  // percent of view bounds

    /**
     * Y position of the scale (Java screen coordinates);
     */
    private float Y;  // percent of view bounds

    /** whether or not to label */
    private boolean labelVisible = true;

    /** flag for whether all ticks should be labeled */
    private boolean labelAllTicks = true;

    /** instance count */
    private static int instance = 0;

    /** mutex object for locking */
    private static Object INSTANCE_MUTEX = new Object();

    /** bounds of scale in VisADCoordinates */
    private Rectangle2D.Float scaleBounds = null;

    /** ScalarMap for the shapes in this ColorScale */
    private ScalarMap csMap = null;

    /** RealType for the shapes in this ColorScale */
    private RealType csType = null;

    /** ShapeControl for the ScalarMap */
    private ShapeControl shapeControl = null;

    /** shapes for this scale */
    private VisADGeometryArray[] shapes = null;

    /** color palette */
    private float[][] colorPalette = null;

    /** default orientation */
    private int orient = HORIZONTAL_ORIENT;

    /** low range for labels */
    private double lowRange = Double.NaN;

    /** high range for labels */
    private double highRange = Double.NaN;

    /** label table */
    private Hashtable labelTable = new Hashtable();

    /** label font */
    private Object labelFont = null;

    /** flag for whether to use user labels or not */
    private boolean userLabels = false;

    /** label size */
    private int labelSize = 10;

    /** label side */
    private int labelSide = PRIMARY;

    /** default color */
    public final static Color DEFAULT_LABEL_COLOR = Color.lightGray;

    /** default color */
    private Color labelColor = null;

    /** z position */
    private double zPosition = 2.0;

    /** use alpha when drawing */
    private boolean useAlpha = false;

    /**
     * Construct a new <code>ColorScale</code> with the given name
     * and default orientation.
     * @param name  name for this color scale object.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public ColorScale(String name) throws VisADException, RemoteException {
        this(name, VERTICAL_ORIENT);
    }

    /**
     * Construct a new <code>ColorScale</code> with the given name
     * and orientation.
     * @param name    name for this color scale object.
     * @param orient  orientation for this <code>ColorScale</code>
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public ColorScale(String name, int orient)
            throws VisADException, RemoteException {
        this(name, orient, null);
    }

    /**
     * Construct a new <code>ColorScale</code> with the given name
     * and orientation and color table.
     * @param name    name for this color scale object.
     * @param orient  orientation for this <code>ColorScale</code>
     * @param table   color table that defines the image
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public ColorScale(String name, int orient, float[][] table)
            throws VisADException, RemoteException {
        this(name, orient, getX(orient, getDefaultPlace(orient)),
             getY(orient, getDefaultPlace(orient)), table);
    }

    /**
     * Construct a new <code>ColorScale</code> with the given name
     * and orientation and position.
     * @param name    name for this color scale object.
     * @param orient  orientation for this <code>ColorScale</code>
     * @param x  X position on the screen, % away from upper left corner
     * @param y  Y position on the screen, % away from upper left corner
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public ColorScale(String name, int orient, double x, double y)
            throws VisADException, RemoteException {
        this(name, orient, x, y, null);
    }

    /**
     * Construct a new <code>ColorScale</code> with the given name
     * and orientation and position, using the color table.
     * @param name    name for this color scale object.
     * @param orient  orientation for this <code>ColorScale</code>
     * @param x       X position on the screen, % away from upper left corner
     * @param y       Y position on the screen, % away from upper left corner
     * @param table   color table that defines the image
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public ColorScale(String name, int orient, double x, double y,
                      float[][] table)
            throws VisADException, RemoteException {
        super(name);
        this.orient  = orient;
        X            = (float) x;
        Y            = (float) y;
        colorPalette = table;
        setUpScalarMaps();
        makeShapes();
    }

    /**
     * Construct a new <code>ColorScale</code> from the ColorScaleInfo
     * @param info    color scale info
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public ColorScale(ColorScaleInfo info)
            throws VisADException, RemoteException {
        super(info.getName());
        this.orient  = info.getOrientation();
        X            = info.getX();
        Y            = info.getY();
        colorPalette = info.getColorPalette();
        labelFont    = info.getLabelFont();
        labelColor   = info.getLabelColor();
        labelSide    = info.getLabelSide();
        labelVisible = info.getLabelVisible();
        useAlpha     = info.getUseAlpha();
        setVisible(info.getIsVisible());
        setUpScalarMaps();
        makeShapes();
    }

    /**
     * Set the range of values for the color table.
     * @param  low   minimum value
     * @param  hi   maximum value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRangeForColor(double low, double hi)
            throws VisADException, RemoteException {
        lowRange  = low;
        highRange = hi;
        makeShapes();
    }

    /**
     * Get the orientation of the ColorScale.
     * @return orientation (HORIZONTAL_ORIENT, VERTICAL_ORIENT)
     */
    public int getOrientation() {
        return orient;
    }

    /**
     * Set the orientation of the ColorScale.
     * @param orient  orientation (HORIZONTAL_ORIENT, VERTICAL_ORIENT)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setOrientation(int orient)
            throws RemoteException, VisADException {
        this.orient = orient;
        makeShapes();
    }

    /**
     * Set the parameters for this scale
     * @param info ColorScaleInfo
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setColorScaleInfo(ColorScaleInfo info)
            throws RemoteException, VisADException {
        orient       = info.getOrientation();
        X            = (float) info.getX();
        Y            = (float) info.getY();
        colorPalette = info.getColorPalette();
        labelFont    = info.getLabelFont();
        labelColor   = info.getLabelColor();
        labelSide    = info.getLabelSide();
        labelVisible = info.getLabelVisible();
        useAlpha = info.getUseAlpha();
        setVisible(info.getIsVisible());
        makeShapes();
    }

    /**
     * Set the color of the labels.
     * @param color color for labels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelColor(Color color)
            throws RemoteException, VisADException {
        labelColor = color;
        makeShapes();
    }

    /**
     * Get the color of the labels
     * @return label color (may be null)
     */
    public Color getLabelColor() {
        return labelColor;
    }

    /**
     * Set the visibility of the labels
     * @param visible true to show labels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelVisible(boolean visible)
            throws RemoteException, VisADException {
        labelVisible = visible;
        makeShapes();
    }

    /**
     * Get the label visbility
     * @return the label visibility
     */
    public boolean getLabelVisble() {
        return labelVisible;
    }

    /**
     *     Gets the useAlpha property
     *    
     *     @return the useAlpha
     */
    public boolean getUseAlpha() {
        return useAlpha;
    }

    /**
     * Sets the useAlpha property
     * @param useAlpha the useAlpha to set
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setUseAlpha(boolean useAlpha)
            throws RemoteException, VisADException {
        this.useAlpha = useAlpha;
        makeShapes();
    }

    /**
     * Set the labeling side.
     * @param side labeling side (PRIMARY, SECONDARY);
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelSide(int side)
            throws RemoteException, VisADException {
        labelSide = side;
        makeShapes();
    }

    /**
     * Get the color of the labels
     * @return label color
     */
    public int getLabelSide() {
        return labelSide;
    }

    /**
     * This method sets the color palette
     * according to the color table in argument;
     * pair this method with setRange(low,high) to get
     * a fixed association of color table and range of values.
     *
     * @param colorPalette     the color table or color-alpha table desired
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setColorPalette(float[][] colorPalette)
            throws RemoteException, VisADException {
        this.colorPalette = colorPalette;
        makeShapes();
    }

    /* make the scalar maps */

    /**
     * Set up the ScalarMaps for this ColorScale
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setUpScalarMaps() throws VisADException, RemoteException {

        int myInstance;
        synchronized (INSTANCE_MUTEX) {
            myInstance = instance++;
        }
        csType = RealType.getRealType("Color_Scale" + myInstance);
        csMap  = new ScalarMap(csType, Display.Shape);
        csMap.addScalarMapListener(new ScalarMapListener() {
            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {
                int id = event.getId();
                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    shapeControl = (ShapeControl) csMap.getControl();
                    if (shapeControl != null) {
                        makeShapes();
                    }

                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {}
        });
        addScalarMap(csMap);
        setData(new Integer1DSet(csType, 3));

    }

    /**
     * Set the position on the screen
     *
     * @throws VisADException   problem creating VisAD object
     */
    private void setPositionData() throws VisADException {

        // System.out.println("zPosition = " + zPosition);
        try {
            addConstantMaps(new ConstantMap[] {
                new ConstantMap(scaleBounds.x, Display.XAxis),
                new ConstantMap(scaleBounds.y, Display.YAxis),
                new ConstantMap(zPosition, Display.ZAxis) });  // set at top of box
        } catch (RemoteException re) {}  // shouldn't happen
    }

    /* set the shapes in the control */

    /**
     * Set the shapes in the control
     *
     * @param shapes  shapes to set
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setShapesInControl(VisADGeometryArray[] shapes)
            throws VisADException, RemoteException {
        if ((shapeControl != null) && (shapes != null)) {
            if (shapes.length > 0) {
                shapeControl.setShapeSet(new Integer1DSet(shapes.length));
                shapeControl.setShapes(shapes);
            } else {
                VisADLineArray shape = new VisADLineArray();
                shape.coordinates = new float[] {};
                shapes            = new VisADGeometryArray[] { shape };
                shapeControl.setShapeSet(new Integer1DSet(shapes.length));
                shapeControl.setShapes(shapes);

            }
        }
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        //TODO: make this work
        return this;
    }

    /**
     * Redisplay the colorbar using the defaults
     */
    public void reDisplay() {
        try {
            makeShapes();
        } catch (VisADException excp) {}
    }

    /**
     * Make the shapes needed for this color scale.
     *
     * @throws VisADException   problem creating VisAD object
     */
    private void makeShapes() throws VisADException {

        if (getDisplay() == null) {
            return;
        }
        calculateScaleBounds();
        Vector shapeVector = new Vector();
        if (colorPalette != null) {
            VisADTriangleArray bar = createTriangles(scaleBounds.width,
                                         scaleBounds.height);
            if ((bar != null) && (bar.coordinates != null)) {
                shapeVector.add(bar);
            }
        }
        if (labelVisible && (colorPalette != null)) {
            VisADGeometryArray[] labels = createLabels(scaleBounds.width,
                                              scaleBounds.height);
            if (labels != null) {
                for (int i = 0; i < labels.length; i++) {
                    VisADGeometryArray ith = labels[i];
                    if ((ith != null) && (ith.coordinates != null)) {
                        shapeVector.add(ith);
                    }
                }
            }
        }
        if (false) {
            VisADLineArray outline = createOutline(scaleBounds.width,
                                         scaleBounds.height);
            if ((outline != null) && (outline.coordinates != null)) {
                shapeVector.add(outline);
            }
        }
        shapes = new VisADGeometryArray[shapeVector.size()];
        try {
            shapes = (VisADGeometryArray[]) shapeVector.toArray(shapes);
            setShapesInControl(shapes);
            setPositionData();
        } catch (Exception t) {
            System.err.println("unable to display colorscale");
        }
    }

    /**
     * Creates a visad triangle array for the signal strength scale.
     *
     * @param height the height of the scale
     * @param width the width of the scale
     *
     * @return a visad triangle array that makes up the signal strength
     *      scale
     * @throws VisADException there's a problem with VisAD
     */
    private VisADTriangleArray createTriangles(float width, float height)
            throws VisADException {

        // Get the color table used by the signalStrength scalar map.
        int     numColours           = colorPalette[0].length;
        boolean hasAlpha             = (colorPalette.length == 4) && useAlpha;

        float   delta                = ((orient == HORIZONTAL_ORIENT)
                                        ? width
                                        : height) / (float) numColours;
        int     numPointsPerTriangle = 3;
        int     numValuesPerPoint    = 3;
        int     numColoursPerPoint   = hasAlpha
                                       ? 4
                                       : 3;
        int     numTriangles         = numColours * 2;
        float[] triangles =
            new float[numTriangles * numPointsPerTriangle * numValuesPerPoint];
        byte[] colors =
            new byte[numTriangles * numPointsPerTriangle * numColoursPerPoint];

        int index      = 0;
        int colorIndex = 0;

        for (int i = 0; i < numColours; ++i) {

            byte red   = (byte) (colorPalette[0][i] * 255);
            byte green = (byte) (colorPalette[1][i] * 255);
            byte blue  = (byte) (colorPalette[2][i] * 255);
            byte alpha = (byte) 0;
            if (hasAlpha) {
                alpha = (byte) (colorPalette[3][i] * 255);
            }


            // The right-half triangle
            // 1     2
            // .......
            //  .    .
            //   .   .
            //    .  .
            //     . .
            //      ..
            //       .
            //       3

            // HORIZONTAL goes left->right, VERTICAL goes bottom->top
            if (orient == HORIZONTAL_ORIENT) {

                // First point in triangle
                triangles[index++] = delta * i;
                triangles[index++] = 0.0f;
                triangles[index++] = 0.0f;

                // Second point in triangle
                triangles[index++] = delta * (i + 1);  // delta*i + delta;
                triangles[index++] = 0.0f;
                triangles[index++] = 0.0f;

                // Third point in triangle
                triangles[index++] = delta * (i + 1);  // delta*i + delta;
                triangles[index++] = height;
                triangles[index++] = 0.0f;

            } else {                                   // VERTICAL_ORIENT

                // First point in triangle
                triangles[index++] = 0.0f;
                triangles[index++] = -height + delta * (i + 1);
                triangles[index++] = 0.0f;

                // Second point in triangle
                triangles[index++] = width;
                triangles[index++] = -height + delta * (i + 1);
                triangles[index++] = 0.0f;

                // Third point in triangle
                triangles[index++] = width;
                triangles[index++] = -height + delta * i;
                triangles[index++] = 0.0f;
            }

            // The left-half triangle
            // 1
            // .
            // ..
            // . .
            // .  .
            // .   .
            // .    .
            // .......
            // 3     2

            if (orient == HORIZONTAL_ORIENT) {

                // First point in triangle
                triangles[index++] = delta * i;
                triangles[index++] = 0.0f;
                triangles[index++] = 0.0f;

                // Second point in triangle
                triangles[index++] = delta * (i + 1);  // delta*i + delta;
                triangles[index++] = height;
                triangles[index++] = 0.0f;


                // Third point in triangle
                triangles[index++] = delta * i;
                triangles[index++] = height;
                triangles[index++] = 0.0f;

            } else {  // VERTICAL_ORIENT

                // First point in triangle
                triangles[index++] = 0.0f;
                triangles[index++] = -height + delta * (i + 1);
                triangles[index++] = 0.0f;

                // Second point in triangle
                triangles[index++] = width;
                triangles[index++] = -height + delta * i;
                triangles[index++] = 0.0f;


                // Third point in triangle
                triangles[index++] = 0.0f;
                triangles[index++] = -height + delta * i;
                triangles[index++] = 0.0f;
            }

            for (int n = 0; n < 6; n++) {  // set colors for the 6 points
                colors[colorIndex++] = red;
                colors[colorIndex++] = green;
                colors[colorIndex++] = blue;
                if (hasAlpha) {
                    colors[colorIndex++] = alpha;
                }
            }

        }                                  // for (i<numColours)


        // Set all the normal vectors to (0,0,1) for each
        // vertex of each triangle.

        float[] normals =
            new float[numTriangles * numPointsPerTriangle * numValuesPerPoint];

        index = 0;
        for (int i = 0; i < numTriangles; ++i) {

            // First point in triangle.
            normals[index++] = 0.0f;
            normals[index++] = 0.0f;
            normals[index++] = 1.0f;

            // Second point in triangle.
            normals[index++] = 0.0f;
            normals[index++] = 0.0f;
            normals[index++] = 1.0f;

            // Third point in triangle.
            normals[index++] = 0.0f;
            normals[index++] = 0.0f;
            normals[index++] = 1.0f;

        }  // for (i<numTriangles)

        VisADTriangleArray triangleArray = new VisADTriangleArray();
        triangleArray.coordinates = triangles;
        triangleArray.normals     = normals;
        triangleArray.colors      = colors;
        triangleArray.vertexCount = numTriangles * numPointsPerTriangle;

        return triangleArray;

    }  // end createTriangles()

    /**
     * Creates a VisADLineArray for the outline of the scale
     *
     * @param height the height of the scale
     * @param width the width of the scale
     *
     * @return a VisADLineArray that makes up the outline
     * @throws VisADException there's a problem with VisAD
     */
    private VisADLineArray createOutline(float width, float height)
            throws VisADException {

        int numPoints = 4;  // four corners, plus the start again to close
        int numValuesPerPoint = 3;
        int numPointsPerLine  = 2;

        float[] outline =
            new float[numPoints * numPointsPerLine * numValuesPerPoint];

        float[] rgb;
        if (labelColor == null) {
            if (getDisplay() != null) {
                rgb = getDisplay().getDisplayRenderer().getRendererControl()
                    .getForegroundColor();
            } else {
                rgb = DEFAULT_LABEL_COLOR.getColorComponents(null);
            }
        } else {
            rgb = labelColor.getColorComponents(null);
        }
        byte red   = ShadowType.floatToByte(rgb[0]);
        byte green = ShadowType.floatToByte(rgb[1]);
        byte blue  = ShadowType.floatToByte(rgb[2]);
        byte[] colors =
            new byte[numPoints * numPointsPerLine * numValuesPerPoint];
        int   colorIndex = 0;
        int   index      = 0;
        float xdelta     = width;  //= (orient == HORIZONTAL_ORIENT)
        //  ? width
        //  : -width;

        float ydelta = (orient == HORIZONTAL_ORIENT)
                       ? height
                       : -height;

        for (int i = 0; i < numPoints; i++) {

            switch (i) {

              case 0 :
                  // First & last point in outline
                  outline[index++] = 0.0f;
                  outline[index++] = 0.0f;
                  outline[index++] = 0.0f;
                  outline[index++] = xdelta;
                  outline[index++] = 0.0f;
                  outline[index++] = 0.0f;
                  break;

              case 1 :
                  outline[index++] = xdelta;
                  outline[index++] = 0.0f;
                  outline[index++] = 0.0f;
                  outline[index++] = xdelta;
                  outline[index++] = ydelta;
                  outline[index++] = 0.0f;
                  break;

              case 2 :
                  outline[index++] = xdelta;
                  outline[index++] = ydelta;
                  outline[index++] = 0.0f;
                  outline[index++] = 0.0f;
                  outline[index++] = ydelta;
                  outline[index++] = 0.0f;
                  break;

              case 3 :
                  outline[index++] = 0.0f;
                  outline[index++] = ydelta;
                  outline[index++] = 0.0f;
                  outline[index++] = 0.0f;
                  outline[index++] = 0.0f;
                  outline[index++] = 0.0f;
                  break;

              default :
                  break;
            }

            // set the colors
            for (int j = 0; j < numPointsPerLine; j++) {
                colors[colorIndex++] = red;
                colors[colorIndex++] = green;
                colors[colorIndex++] = blue;
            }


        }  // for (i<numColours)

        VisADLineArray lineArray = new VisADLineArray();
        lineArray.coordinates = outline;
        lineArray.colors      = colors;
        lineArray.vertexCount = numPoints * numPointsPerLine;

        return lineArray;

    }  // end createOutline()

    /**
     * Create the labels for the scale.  The return will be a
     * an array of length 2.  The first component is the line arrays
     * for the labels, the second is the triangles (for labels with
     * fonts.
     *
     * @param width   width of the labels
     * @param height  height of the labels
     * @return  shapes for the labels
     *
     * @throws VisADException   problem creating VisAD object
     */
    private VisADGeometryArray[] createLabels(float width, float height)
            throws VisADException {

        // System.out.println("w = " + width + " h = " + height);

        // compute graphics positions
        // these are {x, y, z} vectors
        double[] base   = null;  // vector from one character to another
        double[] up     = null;  // vector from bottom of character to top
        double[] startn = null;  // -1.0 position along axis
        double[] startp = null;  // +1.0 position along axis
        // by default, all labels rendered centered
        TextControl.Justification justification = (orient
                                                   == HORIZONTAL_ORIENT)
                ? TextControl.Justification.CENTER
                : TextControl.Justification.LEFT;

        Vector lineArrayVector  = new Vector();  // vector for line drawings
        Vector labelArrayVector = new Vector();
        double ONE              = (lowRange > highRange)
                                  ? -1.0
                                  : 1.0;
        double min              = Math.min(lowRange, highRange);
        double max              = Math.max(lowRange, highRange);
        double range            = Math.abs(max - min);
        double majorTickSpacing = Misc.computeTickSpacing(min, max);
        double firstValue       = lowRange;
        double SCALE;
        double fontScale = 1.0;
        if ((labelFont != null) && (labelFont instanceof Font)) {
            fontScale = ((Font) labelFont).getSize() / 12.;
        }

        Rectangle r = getDisplayMaster().getScreenBounds();
        if (orient == HORIZONTAL_ORIENT) {
            SCALE = height * .8 * fontScale;
            if (labelSide == PRIMARY) {
                base   = new double[] { SCALE, 0.0, 0.0 };
                up     = new double[] { 0.0, SCALE, 0.0 };
                startn = new double[] { 0, -height - SCALE * .5, 0 };
                startp = new double[] { width, -height - SCALE * .5, 0 };
            } else {
                base   = new double[] { SCALE, 0.0, 0.0 };
                up     = new double[] { 0.0, SCALE, 0.0 };
                startn = new double[] { 0, height + SCALE * .5, 0 };
                startp = new double[] { width, height + SCALE * .5, 0 };
            }
        } else {  // VERTICAL_ORIENT
            SCALE = width * .8 * fontScale;
            if (labelSide == PRIMARY) {
                base   = new double[] { SCALE, 0.0, 0.0 };
                up     = new double[] { 0.0, SCALE, 0.0 };
                startn = new double[] { width + SCALE * .5, -height, 0 };
                startp = new double[] { width + SCALE * .5, 0, 0 };
            } else {
                base          = new double[] { SCALE, 0.0, 0.0 };
                up            = new double[] { 0.0, SCALE, 0.0 };
                startn        = new double[] { -SCALE * .5, -height, 0 };
                startp        = new double[] { -SCALE * .5, 0, 0 };
                justification = TextControl.Justification.RIGHT;
            }
        }
        //Misc.printArray("startn", startn);
        //Misc.printArray("startp", startp);
        //Misc.printArray("up", up);
        //Misc.printArray("base", base);


        double   dist  =  // dist from the color bar in up direction
            (orient == HORIZONTAL_ORIENT)
            ? SCALE + SCALE / 10.
            : width + SCALE / 10.;
        double[] updir = up;

        // Draw the labels.  If user hasn't defined their own, make defaults.
        if ( !userLabels) {
            createStandardLabels(max, min, min, (labelAllTicks == false)
                    ? (range)
                    : majorTickSpacing, false);
        }
        if (labelTable.isEmpty()) {
            return null;
        }

        for (Enumeration e = labelTable.keys(); e.hasMoreElements(); ) {
            Double Value;
            try {
                Value = (Double) e.nextElement();
            } catch (ClassCastException cce) {
                throw new VisADException("Invalid keys in label hashtable");
            }
            double test = Value.doubleValue();
            if ((test > max) || (test < min)) {
                continue;  // don't draw labels beyond range
            }

            double val = Math.abs(test - lowRange) / (range);  // pos along the scale

            double[] point = new double[3];
            for (int j = 0; j < 3; j++) {
                point[j] = (1.0 - val) * startn[j] + val * startp[j]
                           - dist * up[j];

            }
            /*
            System.out.println("For label = " + Value.doubleValue() + "(" + val + "), point is (" + point[0] + "," + point[1] + "," + point[2] + ")");
            */

            if (labelFont == null) {
                VisADLineArray label =
                    PlotText.render_label((String) labelTable.get(Value),
                                          point, base, updir, justification);
                lineArrayVector.add(label);
            } else if (labelFont instanceof Font) {
                VisADTriangleArray label =
                    PlotText.render_font((String) labelTable.get(Value),
                                         (Font) labelFont, point, base,
                                         updir, justification);
                labelArrayVector.add(label);

            } else if (labelFont instanceof HersheyFont) {
                VisADLineArray label =
                    PlotText.render_font((String) labelTable.get(Value),
                                         (HersheyFont) labelFont, point,
                                         base, updir, justification);
                lineArrayVector.add(label);
            }
        }
        // merge the line arrays
        VisADLineArray     lineLabels = null;
        VisADTriangleArray triLabels  = null;
        if ( !lineArrayVector.isEmpty()) {
            VisADLineArray[] arrays =
                (VisADLineArray[]) lineArrayVector.toArray(
                    new VisADLineArray[lineArrayVector.size()]);
            lineLabels = VisADLineArray.merge(arrays);
            // set the color for the label arrays
            float[] rgb;
            if (labelColor == null) {
                if (getDisplay() != null) {
                    rgb = getDisplay().getDisplayRenderer()
                        .getRendererControl().getForegroundColor();
                } else {
                    rgb = DEFAULT_LABEL_COLOR.getColorComponents(null);
                }
            } else {
                rgb = labelColor.getColorComponents(null);
            }
            byte   red    = ShadowType.floatToByte(rgb[0]);
            byte   green  = ShadowType.floatToByte(rgb[1]);
            byte   blue   = ShadowType.floatToByte(rgb[2]);
            int    n      = 3 * lineLabels.vertexCount;
            byte[] colors = new byte[n];
            for (int i = 0; i < n; i += 3) {
                colors[i]     = red;
                colors[i + 1] = green;
                colors[i + 2] = blue;
            }
            lineLabels.colors = colors;
        }

        // merge the label arrays
        if ( !(labelArrayVector.isEmpty())) {
            VisADTriangleArray[] labelArrays =
                (VisADTriangleArray[]) labelArrayVector.toArray(
                    new VisADTriangleArray[labelArrayVector.size()]);
            triLabels = VisADTriangleArray.merge(labelArrays);
            // set the color for the label arrays
            float[] rgb    = labelColor.getColorComponents(null);
            byte    red    = ShadowType.floatToByte(rgb[0]);
            byte    green  = ShadowType.floatToByte(rgb[1]);
            byte    blue   = ShadowType.floatToByte(rgb[2]);
            int     n      = 3 * triLabels.vertexCount;
            byte[]  colors = new byte[n];
            for (int i = 0; i < n; i += 3) {
                colors[i]     = red;
                colors[i + 1] = green;
                colors[i + 2] = blue;
            }
            triLabels.colors = colors;
        }
        return new VisADGeometryArray[] { lineLabels, triLabels };

    }

    /**
     * Get the renderer for this ColorScale
     * @return  renderer
     *
     * @throws VisADException problem creating renderer
     */
    protected DataRenderer getDataRenderer() throws VisADException {

        DataRenderer renderer = (getDisplay().getDisplayRenderer()
                                 instanceof DisplayRendererJ2D)
                                ? (DataRenderer) new DefaultRendererJ2D()
                                : new visad.bom.ScreenLockedRendererJ3D();
        makeShapes();
        return renderer;

    }

    /**
     * Calculates the starting position of the scale.
     */
    private void calculateScaleBounds() {
        DisplayImpl display = (DisplayImpl) getDisplay();
        scaleBounds = new Rectangle2D.Float();
        boolean isPerspective =
            display.getGraphicsModeControl().getProjectionPolicy()
            == visad.java3d.DisplayImplJ3D.PERSPECTIVE_PROJECTION;
        try {
            Rectangle r = getDisplayMaster().getScreenBounds();
            if ( !r.isEmpty()) {
                // System.out.println("screen bounds = " + r);
                int x = r.x + (int) (X * r.width);  // where on the screen x is
                int y = r.y + (int) (Y * r.height);  // where on the screen y is
                // System.out.println("x = " + x + " , y = " + y);
                double ratio = (double) r.width / (double) r.height;
                // System.out.println("ratio = " + ratio);
                MouseBehavior behavior =
                    ((DisplayImpl) getDisplayMaster().getDisplay())
                        .getMouseBehavior();
                double[] matrix = getDisplayMaster().getProjectionMatrix();
                //getDisplayMaster().printMatrix("matrix", matrix);
                boolean is2D =
                    getDisplayMaster().getDisplay().getDisplayRenderer()
                        .getMode2D();
                double[] rot   = new double[3];
                double[] scale = new double[3];
                double[] trans = new double[3];
                behavior.instance_unmake_matrix(rot, scale, trans, matrix);
                double defScale = is2D
                                  ? ProjectionControl.SCALE2D
                                  : .5;
                double scalex   = scale[0] / defScale;
                double scaley   = scale[1] / defScale;
                double transx   = trans[0] / defScale;
                double transy   = trans[1] / defScale;

                // starting position
                double[] ray = Util.getVWorldCoords(display, x, y, null);
                //Util.getRayPositionAtZ(behavior.findRay(x, y), -1.0);
                // Misc.printArray("starting position" , ray);
                // lower left
                double[] ray1 = Util.getVWorldCoords(display, r.x,
                                    r.y + r.height, null);
                //Util.getRayPositionAtZ(
                //    behavior.findRay(r.x, r.y + r.height), -1.0);
                // Misc.printArray("lower left" , ray1);
                // upper right
                double[] ray2 = Util.getVWorldCoords(display, r.x + r.width,
                                    r.y, null);
                //Util.getRayPositionAtZ(
                // Misc.printArray("upper right" , ray2);
                if (orient == HORIZONTAL_ORIENT) {
                    scaleBounds.setRect(ray[0], ray[1],
                                        BAR_LENGTH * (ray2[0] - ray1[0]),
                                        BAR_THICKNESS * ratio
                                        * (ray2[1] - ray1[1]));
                    /*
                    ray[0] * scalex + transx, ray[1] * scaley + transy,
                    BAR_LENGTH * (ray2[0] - ray1[0]) * scalex,
                    BAR_THICKNESS * ratio * (ray2[1] - ray1[1]) * scaley);
                    */
                } else {  // VERTICAL_ORIENT
                    scaleBounds.setRect(ray[0], ray[1],
                                        BAR_THICKNESS * (ray2[0] - ray1[0]),
                                        BAR_LENGTH * (ray2[1] - ray1[1]));
                    /*
                    ray[0] * scalex + transx, ray[1] * scaley + transy,
                    BAR_THICKNESS * (ray2[0] - ray1[0]) * scalex,
                    BAR_LENGTH * (ray2[1] - ray1[1]) * scaley);
                    */
                }
                // System.out.println("isPerspective = " + isPerspective);
                zPosition = isPerspective
                            ? ray[2]
                            : 2.0;
                // System.out.println("scaleBounds " + scaleBounds);
            }
        } catch (Exception e) {
            LogUtil.logException("Trying to calc starting position ", e);
            //e.printStackTrace();
        }
    }

    /**
     * private copy to allow program to create table, but not remake scale
     *
     * @param max   maximum label value
     * @param min   minimum label value
     * @param base  base for labels
     * @param increment  increment between labels
     * @param byuser  true if by user, false if programatic
     */
    private void createStandardLabels(double max, double min, double base,
                                      double increment, boolean byuser) {
        labelTable = Misc.createLabelTable(max, min, base, increment);
        if (byuser) {
            try {
                userLabels = true;
                makeShapes();  // update the display
            } catch (VisADException ve) {
                ;
            }
        }
    }

    /**
     * create the default string for a value
     *
     * @param value  create a string from the value
     * @return  String representing the value
     */
    private String createLabelString(double value) {
        return Misc.format(value);
    }

    /**
     * Set the font used for rendering the labels
     * @param font  new font to use
     */
    public void setFont(Font font) {
        Object oldFont = labelFont;
        labelFont = font;
        //if ((labelFont == null && oldFont != null) || !labelFont.equals(oldFont))
        if ((labelFont != null) && !labelFont.equals(oldFont)) {
            if (labelFont instanceof java.awt.Font) {
                labelSize = ((Font) labelFont).getSize();
            }
            try {
                makeShapes();  // update the display
            } catch (VisADException ve) {
                ;
            }
        }
    }

    /**
     * Set the font used for rendering the labels
     * @param font  new font to use
     */
    public void setFont(HersheyFont font) {
        Object oldFont = labelFont;
        labelFont = font;
        //if ((labelFont == null && oldFont != null) || !labelFont.equals(oldFont))
        if ((labelFont != null) && !labelFont.equals(oldFont)) {
            labelSize = 12;
            try {
                makeShapes();  // update the display
            } catch (VisADException ve) {
                ;
            }
        }
    }

    /**
     * Get the font used for rendering the labels
     * @return  font use or null if using default text plot
     */
    public Font getFont() {
        return (labelFont instanceof Font)
               ? (Font) labelFont
               : null;
    }

    /**
     * Set the flags for whether the Displayable uses it's methods
     * to render quickly (eg, not account for projection seams).
     *
     * @param fastRender Should the rendering be quick (and possibly
     *                   inaccurate)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setUseFastRendering(boolean fastRender)
            throws VisADException, RemoteException {
        super.setUseFastRendering(true);  // no need to adjust longitude
    }

    /**
     * Get the default place for the given orientation
     *
     * @param orient  orientation
     *
     * @return  the default place for the orientation
     */
    public static String getDefaultPlace(int orient) {
        return (orient == HORIZONTAL_ORIENT)
               ? BOTTOM
               : LEFT;
    }

    /**
     * Get the default orientation for the given placement
     *
     * @param place placement
     *
     * @return  the default orientation for place
     */
    public static int getDefaultOrient(String place) {
        if (place == null) {
            place = TOP;
        }
        return (place.equals(LEFT) || place.equals(RIGHT))
               ? VERTICAL_ORIENT
               : HORIZONTAL_ORIENT;
    }

    /**
     * Get the X position
     *
     * @param orient orientation
     * @param placement  placement
     *
     * @return corresponding X position
     */
    public static float getX(int orient, String placement) {
        float x = 0.10f;  // HORIZONTAL_ORIENT
        if (orient == VERTICAL_ORIENT) {
            x = placement.equals(LEFT)
                ? .01f
                : 0.99f - BAR_THICKNESS;
        }
        return x;
    }

    /**
     * Get the Y position
     *
     * @param orient orientation
     * @param placement  placement
     *
     * @return corresponding Y position
     */
    public static float getY(int orient, String placement) {
        float y = .10f;  // VERTICAL ORIENTATION
        if (orient == HORIZONTAL_ORIENT) {
            y = placement.equals(TOP)
                ? .01f + BAR_THICKNESS
                : 0.99f - 2.f * BAR_THICKNESS;
        }
        return y;
    }
}
