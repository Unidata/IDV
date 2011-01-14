/*
 * $Id: ShapeGlyph.java,v 1.33 2007/04/16 20:53:47 jeffmc Exp $
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

package ucar.unidata.idv.control.drawing;


import org.w3c.dom.Element;


import ucar.unidata.idv.control.DrawingControl;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

//import ucar.visad.FrontDrawer;
import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.List;

import javax.swing.event.ChangeEvent;



/**
 * Class ShapeGlyph. Displays a shape.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.33 $
 */
public class ShapeGlyph extends LineGlyph {

    /** Xml attribute name */
    public static final String ATTR_SHAPETYPE = "shapetype";

    /** Shape type */
    public static final int SHAPE_RECTANGLE = 0;

    /** Shape type */
    public static final int SHAPE_OVAL = 1;

    /** Shape type */
    public static final int SHAPE_LINE = 2;

    /** Shape type */
    public static final int SHAPE_DIAMOND = 3;

    /** Shape type */
    public static final int SHAPE_HARROW = 4;

    /** Shape type */
    public static final int SHAPE_VARROW = 5;


    /** Shape types */
    public static final int[] SHAPES = { SHAPE_RECTANGLE, SHAPE_OVAL,
                                         SHAPE_LINE, SHAPE_DIAMOND };

    /** Shape type names */
    public static final String[] SHAPE_NAMES = { "Rectangle", "Oval", "Line",
            "Diamond" };


    /** The shape type */
    private int shapeType = SHAPE_RECTANGLE;


    /**
     * Ctor
     */
    public ShapeGlyph() {}


    /**
     * The ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public ShapeGlyph(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        super(control, event);
    }



    /**
     * The ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     * @param shapeType The shape type
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public ShapeGlyph(DrawingControl control, DisplayEvent event,
                      int shapeType)
            throws VisADException, RemoteException {
        this(control, event);
        this.shapeType = shapeType;
        setFilled(control.getFilled());
    }




    /**
     * Can we display distance
     *
     * @return can do distance
     */
    public boolean canShowDistance() {
        return shapeType == SHAPE_LINE;
    }

    /**
     * Can we display area
     *
     * @return can do area
     */
    public boolean canShowArea() {
        return shapeType == SHAPE_RECTANGLE;
    }


    /**
     * Do we have a valid number of points
     *
     * @return Is this glyph valid
     */
    public boolean isValid() {
        return (points != null) && (points.size() >= 2);
    }


    /**
     * Handle event
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMouseDragged(DisplayEvent event)
            throws VisADException, RemoteException {
        if ((points.size() == 1) || (points.size() == 0)) {
            points.add(getPoint(event));
        } else {
            points.set(1, getPoint(event));
        }
        updateLocation();
        return this;
    }




    /**
     * Init from xml
     *
     *
     * @param control The control I'm in.
     * @param node The xml node
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initFromXml(DrawingControl control, Element node)
            throws VisADException, RemoteException {
        super.initFromXml(control, node);
        String type = XmlUtil.getAttribute(node, ATTR_SHAPETYPE,
                                           "Rectangle").toLowerCase();
        for (int i = 0; i < SHAPES.length; i++) {
            if (SHAPE_NAMES[i].toLowerCase().equals(type)) {
                shapeType = SHAPES[i];
                break;
            }
        }
    }



    /**
     * Is glyph constrained to 2d
     *
     * @return Constrained to 2d
     */
    protected boolean constrainedTo2D() {
        return true;
    }


    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(Element e) {
        super.addAttributes(e);
        for (int i = 0; i < SHAPES.length; i++) {
            if (SHAPES[i] == shapeType) {
                e.setAttribute(ATTR_SHAPETYPE, SHAPE_NAMES[i]);
                break;
            }
        }
    }



    /**
     * Get xml tag name to use
     *
     * @return Xml tag name
     */
    public String getTagName() {
        return TAG_SHAPE;
    }


    /**
     * By default we don't interpolate
     *
     * @return 0
     */
    protected int getNumInterpolationPoints() {
        return 0;
    }


    /**
     * Utility to interpolate cnt number of times between the start and end points
     *
     * @param cnt Number of interpolation points inculding start and end point
     * @param startXYZ Start point
     * @param endXYZ End point
     *
     * @return Interpolated points
     */
    public static float[][] interpolate(int cnt, float[] startXYZ,
                                        float[] endXYZ) {
        float[][] lineVals = new float[3][cnt];
        for (int i = 0; i < 3; i++) {
            lineVals[i] = Misc.interpolate(cnt, startXYZ[i], endXYZ[i]);
        }
        return lineVals;
    }





    /**
     * The glyph moved. Update location.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {

        if (points.size() < 2) {
            return;
        }
        MathType mathType = null;
        if (isInXYSpace()) {
            mathType = RealTupleType.SpatialCartesian3DTuple;
        } else if (isInLatLonSpace()) {
            mathType = RealTupleType.LatitudeLongitudeAltitude;
        } else {
            //??
        }

        float     ONE_THIRD = 0.33333f;
        float     TWO_THIRD = 0.66666f;

        float[][] lineVals  = getPointValues();
        if (shapeType == SHAPE_LINE) {
            if (getNumInterpolationPoints() > 0) {
                lineVals = interpolate(2 + getNumInterpolationPoints(),
                                       new float[] { lineVals[0][0],
                        lineVals[1][0], lineVals[2][0] }, new float[] {
                            lineVals[0][1],
                            lineVals[1][1], lineVals[2][1] });

            }

            Data theData = new Gridded3DSet(mathType, lineVals,
                                            lineVals[0].length);
            lineDisplayable.setData(theData);
        } else if (shapeType == SHAPE_RECTANGLE) {
            float[][] pts = makeRectangle(lineVals);
            setActualPoints(pts);
            Data theData = new Gridded3DSet(mathType, pts, 5);
            lineDisplayable.setData(tryToFill(pts, theData));
        } else if (shapeType == SHAPE_HARROW) {
            float[][] pts = new float[3][8];
            for (int i = 0; i < pts[0].length; i++) {
                pts[IDX_Z][i] = lineVals[IDX_Z][0];
            }
            int   idxX   = (isInXYSpace()
                            ? IDX_X
                            : IDX_LON);
            int   idxY   = (isInXYSpace()
                            ? IDX_Y
                            : IDX_LAT);

            float left   = lineVals[idxX][0];
            float right  = lineVals[idxX][1];
            float top    = lineVals[idxY][0];
            float bottom = lineVals[idxY][1];
            float width  = right - left;
            float height = bottom - top;
            pts[idxX][0] = left;
            pts[idxX][1] = left + TWO_THIRD * width;
            pts[idxX][2] = left + TWO_THIRD * width;
            pts[idxX][3] = right;
            pts[idxX][4] = left + TWO_THIRD * width;
            pts[idxX][5] = left + TWO_THIRD * width;
            pts[idxX][6] = left;
            pts[idxX][7] = left;


            pts[idxY][0] = top + ONE_THIRD * height;
            pts[idxY][1] = top + ONE_THIRD * height;
            pts[idxY][2] = top;
            pts[idxY][3] = top + 0.5f * height;
            pts[idxY][4] = bottom;
            pts[idxY][5] = top + TWO_THIRD * height;
            pts[idxY][6] = top + TWO_THIRD * height;
            pts[idxY][7] = top + ONE_THIRD * height;

            setActualPoints(pts);
            Data theData = new Gridded3DSet(mathType, pts, pts[0].length);
            lineDisplayable.setData(tryToFill(pts, theData));

        } else if (shapeType == SHAPE_VARROW) {
            float[][] pts = new float[3][8];
            for (int i = 0; i < pts[0].length; i++) {
                pts[IDX_Z][i] = lineVals[IDX_Z][0];
            }
            int   idxX   = (isInXYSpace()
                            ? IDX_X
                            : IDX_LON);
            int   idxY   = (isInXYSpace()
                            ? IDX_Y
                            : IDX_LAT);

            float left   = lineVals[idxX][0];
            float right  = lineVals[idxX][1];
            float top    = lineVals[idxY][0];
            float bottom = lineVals[idxY][1];
            float width  = right - left;
            float height = bottom - top;

            pts[idxX][0] = left;
            pts[idxX][1] = left + 0.5f * width;
            pts[idxX][2] = right;
            pts[idxX][3] = left + TWO_THIRD * width;
            pts[idxX][4] = left + TWO_THIRD * width;
            pts[idxX][5] = left + ONE_THIRD * width;
            pts[idxX][6] = left + ONE_THIRD * width;
            pts[idxX][7] = left;


            pts[idxY][0] = top + ONE_THIRD * height;
            pts[idxY][1] = top;
            pts[idxY][2] = top + ONE_THIRD * height;
            pts[idxY][3] = top + ONE_THIRD * height;
            pts[idxY][4] = bottom;
            pts[idxY][5] = bottom;
            pts[idxY][6] = top + ONE_THIRD * height;
            pts[idxY][7] = top + ONE_THIRD * height;

            setActualPoints(pts);
            Data theData = new Gridded3DSet(mathType, pts, pts[0].length);
            lineDisplayable.setData(tryToFill(pts, theData));



        } else if (shapeType == SHAPE_DIAMOND) {
            float[][] pts = new float[3][5];
            pts[IDX_X][0] = lineVals[IDX_X][0]
                            + (lineVals[IDX_X][1] - lineVals[IDX_X][0])
                              / 2.0f;
            pts[IDX_Y][0] = lineVals[IDX_Y][0];
            pts[IDX_Z][0] = lineVals[IDX_Z][0];

            pts[IDX_X][1] = lineVals[IDX_X][1];
            pts[IDX_Y][1] = lineVals[IDX_Y][0]
                            + (lineVals[IDX_Y][1] - lineVals[IDX_Y][0])
                              / 2.0f;
            pts[IDX_Z][1] = lineVals[IDX_Z][1];

            pts[IDX_X][2] = pts[IDX_X][0];
            pts[IDX_Y][2] = lineVals[IDX_Y][1];
            pts[IDX_Z][2] = lineVals[IDX_Z][1];

            pts[IDX_X][3] = lineVals[IDX_X][0];
            pts[IDX_Y][3] = pts[IDX_Y][1];
            pts[IDX_Z][3] = lineVals[IDX_Z][0];

            for (int i = 0; i < 3; i++) {
                pts[i][4] = pts[i][0];
            }
            setActualPoints(pts);
            Data theData = new Gridded3DSet(mathType, pts, 5);
            lineDisplayable.setData(tryToFill(pts, theData));
        } else if (shapeType == SHAPE_OVAL) {}

        super.updateLocation();


    }



    /**
     * Utility to make a rectangle from 2 points
     *
     * @param lineVals The 2 points
     *
     * @return 5 points
     */
    public static float[][] makeRectangle(float[][] lineVals) {
        float[][] pts = new float[lineVals.length][5];
        for (int i = 0; i < lineVals.length; i++) {
            pts[i][0] = lineVals[i][0];
            pts[i][4] = lineVals[i][0];
            pts[i][2] = lineVals[i][1];
        }
        pts[IDX_X][1] = lineVals[IDX_X][1];
        pts[IDX_Y][1] = lineVals[IDX_Y][0];
        pts[IDX_X][3] = lineVals[IDX_X][0];
        pts[IDX_Y][3] = lineVals[IDX_Y][1];

        if (lineVals.length > 2) {
            pts[IDX_Z][1] = lineVals[IDX_Z][0];
            pts[IDX_Z][3] = lineVals[IDX_Z][1];
        }
        return pts;
    }

    public static Rectangle2D.Float makeRectangle2D(float[][] pts) {
        float  minX     = Float.POSITIVE_INFINITY;
        float  minY     = Float.POSITIVE_INFINITY;
        float  maxX     = Float.NEGATIVE_INFINITY;
        float  maxY     = Float.NEGATIVE_INFINITY;
        for(int i=0;i<pts[0].length;i++) {
            minX = (float)Math.min(minX, pts[IDX_LON][i]);
            maxX = (float)Math.max(maxX, pts[IDX_LON][i]);
            minY = (float)Math.min(minY, pts[IDX_LAT][i]);
            maxY = (float)Math.max(maxY, pts[IDX_LAT][i]);
        }

        float width = (maxX - minX);
        float height = (maxY - minY);
        return  new Rectangle2D.Float( minX, minY,  width, height);
    }


    /**
     * Handle event
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        points.add(getPoint(event));
        return this;
    }





    /**
     * Name to use for this glyph
     *
     * @return The glyph type name
     */
    public String getTypeName() {
        return "Shape";
    }

    /**
     * Get the description of this glyph
     *
     * @return The description
     */
    public String getDescription() {
        for (int i = 0; i < SHAPES.length; i++) {
            if (shapeType == SHAPES[i]) {
                return (getFilled()
                        ? "Filled "
                        : "") + SHAPE_NAMES[i];
            }
        }
        return getTypeName();
    }


    /**
     * Get the shape type
     *
     * @return Shape type
     */
    public int getShapeType() {
        return shapeType;
    }

    /**
     * Set the shapea
     *
     * @param s Shape type
     */
    public void setShapeType(int s) {
        shapeType = s;
    }


}

