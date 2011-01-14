/*
 * $Id: CloudCoverageSymbol.java,v 1.17 2007/05/22 20:00:21 jeffmc Exp $
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

package ucar.unidata.ui.symbol;



import ucar.unidata.ui.drawing.DisplayCanvas;

import java.awt.*;
import java.awt.geom.*;


/**
 * Cloud coverage meteorological symbols.
 *
 * @author Metapps development team
 * @version $Revision: 1.17 $
 */
public class CloudCoverageSymbol extends MetSymbol {

    /** Example cover value */
    private double coverage = 0.0;  // must lie between 0 and 100


    /**
     * Default constructor
     */
    public CloudCoverageSymbol() {}

    /**
     * Create a cloud coverage symbol at the x and y position.  Position
     * is relative to the center point.  Use default parameter and description.
     * @param  x   x offset from center
     * @param  y   y offset from center
     */
    public CloudCoverageSymbol(int x, int y) {
        this(null, x, y);
    }

    /**
     * Create a cloud coverage symbol at the x and y position on the
     * supplied canvas.  Position is relative to the center point.
     * Use default parameter and description.
     * @param  canvas   <code>DisplayCanvas</code> to draw on
     * @param  x        x offset from center
     * @param  y        y offset from center
     */
    public CloudCoverageSymbol(DisplayCanvas canvas, int x, int y) {
        this(canvas, x, y, "CC", "Coverage parameter");
    }


    /**
     * Create a cloud coverage symbol at the x and y position.  Position
     * is relative to the center point.  Use parameter and description
     * provided.
     * @param  param  parameter name for this <code>CloudCoverageSymbol</code>
     * @param  paramDesc  description (long name) of <code>param</code>
     * @param  x   x offset from center
     * @param  y   y offset from center
     */
    public CloudCoverageSymbol(int x, int y, String param, String paramDesc) {
        this(null, x, y, param, paramDesc);
    }

    /**
     * Create a cloud coverage symbol at the x and y position on the
     * <code>DisplayCanvas</code> supplied.  Position
     * is relative to the center point.  Use parameter and description
     * provided.
     * @param  canvas   <code>DisplayCanvas</code> to draw on
     * @param  param  parameter name for this <code>CloudCoverageSymbol</code>
     * @param  paramDesc  description (long name) of <code>param</code>
     * @param  x   x offset from center
     * @param  y   y offset from center
     */
    public CloudCoverageSymbol(DisplayCanvas canvas, int x, int y,
                               String param, String paramDesc) {
        super(canvas, x, y, new String[] { param },
              new String[] { paramDesc });
        coverage = 0.0;
        setSize(20, 20);
    }



    /**
     * Get whether this <code>MetSymbol</code> can be stretched or not.
     * @return true if can be stretched.
     */
    public boolean getStretchy() {
        return !getBeingCreated();
    }


    /**
     * Get whether this <code>MetSymbol</code> has equals sides
     * (width and height).
     * @return  true
     */
    public boolean getEqualSides() {
        return true;
    }

    /**
     * Get whether this <code>MetSymbol</code> should be stretched
     * symetrically or not.
     * @return true
     */
    public boolean getSymetricReshape() {
        return true;
    }

    /**
     * Get the coverage value : lies between 0 and 100 (%)
     * @return coverage in percent
     */
    public double getCoverage() {
        return coverage;
    }

    /**
     * Set the coverage value : lies between 0 and 100 (%)
     * @param coverage  coverage in percent
     */
    public void setCoverage(double coverage) {
        this.coverage = coverage;
    }

    /**
     * Get the parameter value at the index specified.
     * @param  index  index into param array
     * @return always returns the coverage as a <code>Double</code>
     */
    public Object getParamValue(int index) {
        return new Double(coverage);
    }

    /**
     * Set the parameter value at the index specified.  Since this
     * <code>MetSymbol</code> only has one parameter, it sets the
     * value at index 0.
     * @param  index  parameter index.  Ignored in this object.
     * @param  v      value for the parameter (String representation of
     *                coverage).
     * @see #setCoverage(double)
     */
    public void setParamValue(int index, Object v) {
        setCoverage(new Double(v.toString()).doubleValue());
    }


    /**
     * draw the symbol at the specified location
     * @param g Graphics2D object
     * @param x x position
     * @param y y position
     * @param width width of symbol
     * @param height height of symbol
     */
    public void draw(Graphics2D g, int x, int y, int width, int height) {
        int cx = x + width / 2;
        int cy = y + height / 2;


        g.setColor(getForeground());
        g.drawOval(x, y, width, height);
        if (coverage == 0.0) {
            return;
        }

        int arcAngle = 0;
        int icover   = (int) (coverage / 10.0);

        switch (icover) {

          case 0 :   // vertical line
          case 1 :   // vertical line
              g.drawLine(cx, y, cx, y + height);
              break;

          case 2 :
          case 3 :   // quarter fill
              arcAngle = -90;
              break;

          case 4 :   // vertical line and quarter fill
              g.drawLine(cx, cy, cx, y + height);
              arcAngle = -90;
              break;

          case 5 :   // half fill
              arcAngle = -180;
              break;

          case 6 :   // horiz line and half fill
              g.drawLine(x, cy, x + width, cy);
              arcAngle = -180;
              break;

          case 7 :
          case 8 :   // three quarter fill
              arcAngle = -270;
              break;


          case 9 :   // full with inverse vertical line
              g.fillArc(x, y, width, height, 0, 360);
              g.setColor(getForeground());
              g.drawLine(cx, y - 1, cx, y + height + 1);
              break;

          case 10 :  // all filled
              arcAngle = -360;
              break;

          default :  // an X
              int ux = (int) (cx + .4 * width) - 1;
              int lx = (int) (cx - .4 * width) + 1;
              int uy = (int) (cy + .4 * height) - 1;
              int ly = (int) (cy - .4 * height) + 1;
              g.drawLine(ux, uy, lx, ly);
              g.drawLine(ux, ly, lx, uy);
              break;
        }

        if (arcAngle != 0) {
            g.fillArc(x, y, width, height, 90, arcAngle);
        }
    }
}

