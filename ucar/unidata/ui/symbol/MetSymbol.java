/*
 * $Id: MetSymbol.java,v 1.60 2007/05/22 20:00:22 jeffmc Exp $
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


import org.w3c.dom.Element;

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.ui.colortable.ColorTableCanvas;
import ucar.unidata.ui.colortable.ColorTableManager;

import ucar.unidata.ui.drawing.*;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import visad.Data;


import visad.Unit;

import visad.VisADGeometryArray;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;


import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Class MetSymbol
 *
 *
 * @author Unidata development team
 * @version $Revision: 1.60 $
 */
public abstract class MetSymbol extends RectangleGlyph {

    /** Symbol type attribute */
    public static final String ATTR_PARAMID = "paramid";


    /** Holds state for rotating around the x axis */
    private RotateInfo rotateXInfo;

    /** Holds state for rotating around the y axis */
    private RotateInfo rotateYInfo;

    /** Holds state for rotating around the z axis */
    private RotateInfo rotateZInfo;


    /** The canvas we are in */
    protected StationModelCanvas canvas;

    /** symbol properties dialog */
    protected PropertiesDialog propertiesDialog;

    /** The color table */
    private ColorTable colorTable;

    /** color table unit */
    private Unit colorTableUnit = null;

    /** color table unit name */
    private String colorTableUnitName = null;


    /** parameter for color by */
    private String colorTableParam;

    /** default range of the color table */
    Range colorTableRange = new Range(0, 100);

    /** parameter for color by */
    private String colorParam;

    /** parameter for color by */
    private List colorMappings;


    /** unit for rotate by parameter */
    private Unit rotateUnit = null;

    /** name of unit for rotate by parameter */
    private String rotateUnitName = null;


    /** parameter name to rotate by */
    private String rotateParam;

    /** default rotation range */
    Range rotateRange = new Range(0, 360);

    /** default range for data to rotate */
    Range rotateDataRange = new Range(0, 360);


    /** displayUnit */
    private Unit displayUnit = null;

    /** displayUnitName */
    private String displayUnitName = null;

    /** default scale */
    private double scale = 1.0;

    /** scale parameter */
    private String scaleParam;

    /** default range for scaling data */
    private Range scaleDataRange = new Range(0, 100);


    /** default range of data to scale */
    private Range scaleRange = new Range(1, 1);


    /** unit of scale param */
    private Unit scaleUnit = null;

    /** name of unit of scale param */
    private String scaleUnitName = null;

    /** Active flag */
    private boolean active = true;

    /** Missing flag */
    private boolean missing = false;

    /** Parameter ids */
    private String[] paramIds;

    /** Parameter descriptions */
    private String[] paramDescs;

    /** Offset from center */
    Point offset;

    /** Rectangle point */
    private String rectPoint = PT_MM;

    /**
     * Default constructor.
     */
    protected MetSymbol() {}


    /**
     * Constructor
     *
     * @param x Initial x position.
     * @param y Initial y position.
     */
    public MetSymbol(int x, int y) {
        super(x, y);
    }



    /**
     * The MetSymbol can be created in two ways.  If the canvas is
     * null, it is assumed that x and y are the offsets from the
     * center point.  Otherwise, they are relative to the canvas?
     * @param canvas     <code>DisplayCanvas</code> to draw on. May be null.
     * @param x          x position (either offset or canvas relative)
     * @param y          y position (either offset or canvas relative)
     * @param paramIds   array of parameter ids
     * @param paramDescs array of parameter descriptions (long names)
     */
    protected MetSymbol(DisplayCanvas canvas, int x, int y,
                        String[] paramIds, String[] paramDescs) {
        super(x, y);
        //Were we created directly. If so then the x/y pair is the middle point
        if (canvas == null) {
            offset = new Point(x, y);
        }
        setForeground(Color.magenta);
        setFilled(true);
        this.paramIds   = paramIds;
        this.paramDescs = paramDescs;
    }


    /**
     * Initialize this symbol from the xml that defined it.
     *
     * @param symbolNode The xml node that defines this symbol.
     */
    public void initialize(Element symbolNode) {}



    /**
     * Create, if needed, and show the properties dialog.
     *
     * @param canvas The canvas we are in.
     */
    public void showPropertiesDialog(StationModelCanvas canvas) {
        this.canvas = canvas;
        if (propertiesDialog == null) {
            propertiesDialog = new PropertiesDialog(this, canvas);
        }
        propertiesDialog.show();
    }

    /**
     * Close the properties dialog if it is open.
     */
    public void closePropertiesDialog() {
        if (propertiesDialog != null) {
            propertiesDialog.close();
        }
    }

    /**
     * Add to the list any widgets that should be added to the basic properties dialog tab.
     *
     * @param comps List containing labels and widgets for the basic properties tab.
     */
    protected void initPropertyComponents(List comps) {}


    /**
     * Called by the PropertiesDialog to apply any special properties
     */
    protected void applyProperties() {
        canvas.setHaveChanged(true);
    }

    /**
     * Add extra components to the property tab
     *
     * @param tabbedPane Tabbed pane to add to
     */
    public void addPropertyTabs(JTabbedPane tabbedPane) {}

    /**
     * Should the 'Color by' properties dialog tab be shown.  Subclasses
     * that don't want this done (i.e., ones that create specific color
     * schemes) should override to return false.
     *
     * @return Show the color by gui (true).
     */
    protected boolean shouldShowColorTableGui() {
        return true;
    }


    /**
     * Should the 'Rotate by' properties dialog tab be shown. Subclasses
     * should override to return false if need be.
     *
     * @return Show the rotate by gui (true).
     */
    protected boolean shouldShowRotateGui() {
        return true;
    }

    /**
     * Should the 'Scale by' properties dialog tab be shown. Subclasses
     * should override to return false if need be.
     *
     * @return Show the scale by gui (true).
     */
    protected boolean shouldShowScaleGui() {
        return true;
    }


    /**
     *  Is used by the StationModelDisplayable to determine if the
     *  shapes should be scaled. Subclasses
     * should override to return false if need be.
     *
     *  @return Should the shapes be scaled (true).
     */
    public boolean shouldScaleShape() {
        return true;
    }

    /**
     * Is used by the StationModelDisplayable to determine if the color
     * of the shapes should be set. Subclasses should override this
     * if they create their own color schemes.
     *
     * @return Should the shapes be colored
     */
    public boolean shouldBeColored() {
        return true;
    }


    /**
     * Is used by the StationModelDisplayable to determine if the
     * shapes should be moved in accordance to the position of this symbol.
     * Subclasses should override to return false if need be.
     *
     * @return Should the shapes be offset
     */
    public boolean shouldOffsetShape() {
        return true;
    }




    /**
     * Called by the StationModelDisplayable to determine if the shapes
     * should be created with a call to makeShapes(PointOb).
     *
     * @return Should this symbol take the whole PointOb  when making its own shapes.
     */
    public boolean doAllObs() {
        return false;
    }

    /**
     * If doAllObs returns true then this method is called to create the shapes.
     *
     * @param ob The observation
     *
     * @return The shapes
     *
     * @throws Exception On badness
     */
    public VisADGeometryArray[] makeShapes(PointOb ob) throws Exception {
        return null;
    }


    /**
     * If this symbol is not one of the 'known' symbols then this method is
     * called to create the shapes.
     *
     * @param dataArray The array of data objects. May have 0 or more data depending
     * on the paramids.
     * @param ob The observation
     *
     * @return The shapes
     *
     * @throws Exception On badness
     */
    public VisADGeometryArray[] makeShapes(Data[] dataArray, PointOb ob)
            throws Exception {
        return null;
    }


    /**
     * Determine whether this <code>MetSymbol</code> should show an
     * alignment menu in an editor.  Subclasses should override if not.
     * @return true
     */
    public boolean doAlignmentMenu() {
        return true;
    }

    /**
     * Set the parameter ids for this <code>MetSymbol</code>.
     * @param  value   array of parameter ids
     */
    public void setParamIds(String[] value) {
        paramIds = value;
    }

    /**
     * Get the parameter ids for this <code>MetSymbol</code>.
     * @return  array of parameter ids
     */
    public String[] getParamIds() {
        return paramIds;
    }

    /**
     * Set the parameter descriptions for this <code>MetSymbol</code>.
     * Descriptions are used for UI widgets (labels, combo boxes, etc)
     * @param  value   array of parameter descriptions
     */
    public void setParamDescs(String[] value) {
        paramDescs = value;
    }

    /**
     * Get the parameter descriptions for this <code>MetSymbol</code>.
     * Descriptions are used for UI widgets (labels, combo boxes, etc)
     * @return  copy of the array of parameter descriptions
     */
    public String[] getParamDescs() {
        return paramDescs;
    }

    /**
     * Set the offset from the center for this <code>MetSymbol</code>.
     * @param  x    x offset
     * @param  y    y offset
     */
    public void setOffset(int x, int y) {
        if (offset != null) {
            offset.x = x;
            offset.y = y;
        }
        bounds.x = x;
        bounds.y = y;
    }

    /**
     * Get the x value of the offset.
     * @return  x offset from center value.
     */
    public int getXOffset() {
        if (offset != null) {
            return offset.x;
        }
        return bounds.x + bounds.width / 2;
    }

    /**
     * Get the point at which this object is alligned.
     * @return allignment point (e.g. Glyph.PT_MM, PT_LL)
     */
    public String getRectPoint() {
        return rectPoint;
    }

    /**
     * Set the point at which this object is alligned.
     * @param  rp  allignment point (e.g. Glyph.PT_MM, Glyph.PT_LL)
     */
    public void setRectPoint(String rp) {
        rectPoint = rp;
    }

    /**
     * Get the y value of the offset.
     * @return  y offset from center value.
     */
    public int getYOffset() {
        if (offset != null) {
            return offset.y;
        }
        return -(bounds.y + bounds.height / 2);
    }

    /**
     * Increase the size of this <code>MetSymbol</code>.  Subclasses
     * should implement this if they support such a thing.
     */
    public void incrSize() {}

    /**
     * Increase the size of this <code>MetSymbol</code>.  Subclasses
     * should implement this if they support such a thing.
     */
    public void decrSize() {}


    /**
     * Get the <code>String</code> that represents this <code>MetSymbol</code>.
     * @return <code>String</code> representation.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        for(String s:paramIds) {
            sb.append(" param:" + s);
        }
        return  sb.toString();
    }

    /**
     * Get the label to show the user what I am in the properties
     *
     * @return label
     */
    public String getLabel() {
        String className = getClass().getName();
        int    idx       = className.lastIndexOf(".");
        if (idx >= 0) {
            className = className.substring(idx + 1);
        }
        if (className.endsWith("Symbol")) {
            className = StringUtil.replace(className, "Symbol", " Symbol");
        }
        return className;
    }


    /**
     * Clone this object
     * @return a clone
     *
     * @throws CloneNotSupportedException  if cloning cannot be done
     */
    protected Object clone() throws CloneNotSupportedException {
        MetSymbol theClone = (MetSymbol) super.clone();
        //Make sure we clear out the propertiesDialog
        theClone.propertiesDialog = null;
        if(theClone.paramIds!=null) 
            theClone.paramIds = (String[])theClone.paramIds.clone();
        if(theClone.paramDescs!=null) 
            theClone.paramDescs = (String[])theClone.paramDescs.clone();

        if(theClone.rotateXInfo!=null) 
            theClone.rotateXInfo = (RotateInfo)theClone.rotateXInfo.doClone();
        if(theClone.rotateYInfo!=null) 
           theClone.rotateYInfo = (RotateInfo)theClone.rotateYInfo.doClone();
        if(theClone.rotateZInfo!=null) 
            theClone.rotateZInfo = (RotateInfo)theClone.rotateZInfo.doClone();

        return theClone;
    }

    /**
     * Return a clone of this object.
     * @return clone
     */
    public Object cloneMe() {
        try {
            Object cl = super.clone();
            return cl;
        } catch (CloneNotSupportedException cnse) {}
        return null;
    }



    /**
     * Get the name of this <code>MetSymbol</code>.
     * @return name of first <code>paramId</code>
     */
    public String getName() {
        if ((paramIds != null) && (paramIds.length > 0)) {
            return paramIds[0];
        }
        return getClass().getName();
    }

    /**
     * Get the description of this <code>MetSymbol</code>.
     * @return description of first <code>paramId</code>
     */
    public String getDesc() {
        if ((paramDescs != null) && (paramDescs.length > 0)) {
            return paramDescs[0];
        }
        return getClass().getName();
    }

    /**
     * Get the parameter value at the index specified.  Subclasses
     * need to override this.
     * @param  index  index into param array
     * @return null since this is the super class
     */
    public Object getParamValue(int index) {
        return null;
    }

    /**
     * Set the parameter value at the index specified.  Subclasses
     * need to override this.
     * @param  index  index into param array
     * @param  v  value to set at that index.
     */
    public void setParamValue(int index, Object v) {}

    /**
     * Get the parameter at the index specified.
     * @param  index  index into param array
     * @return parameter at index.
     */
    public String getParam(int index) {
        if (index < paramIds.length) {
            return paramIds[index];
        }
        throw new IllegalArgumentException("Out of bounds parameter index: "
                                           + index);
    }

    /**
     * This is called to paint within the EditCanvas
     * @param  g  Graphics
     * @param  c  DisplayCanvas to paint on.
     */
    public void paint(Graphics g, DisplayCanvas c) {
        Rectangle nb = transformOutput(c, getBounds());
        if ( !getActive()) {
            Rectangle r = getBounds();
            g.setColor(Color.lightGray);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        draw((Graphics2D) g, nb.x, nb.y, nb.width, nb.height);

        if ((c instanceof StationModelCanvas)
                && ((StationModelCanvas) c).getShowParams()) {
            int         xoff = 0;
            FontMetrics fm   = g.getFontMetrics();
            g.setColor(Color.black);
            for (int i = 0; i < paramIds.length; i++) {
                String s = paramIds[i];
                if (i > 0) {
                    s = ", " + s;
                }
                g.drawString(s, nb.x + xoff,
                             nb.y + nb.height + fm.getMaxDescent()
                             + fm.getMaxAscent() + 4);
                xoff += fm.stringWidth(s);
            }
        }
        if ((c instanceof StationModelCanvas)
                && ((StationModelCanvas) c).shouldShowAlignmentPoints()) {
            paintRectPoint(g, c);
        }
    }

    /**
     * Paint the selection
     *
     * @param g   Graphics to use for painting
     * @param c   DisplayCanvas to paint on
     */
    public void paintSelection(Graphics g, DisplayCanvas c) {
        super.paintSelection(g, c);
        paintRectPoint(g, c);
    }

    /**
     * Paint a rectangle point
     *
     * @param g   Graphics to use for painting
     * @param c   DisplayCanvas to paint on
     */
    private void paintRectPoint(Graphics g, DisplayCanvas c) {
        Rectangle nb = transformOutput(c, getBounds());
        g.setColor(Color.red);
        Point2D rp = getPointOnRect(rectPoint, nb);
        g.fillRect((int) rp.getX() - 3, (int) rp.getY() - 3, 6, 6);

    }


    /**
     * Get the bounds for this <code>MetSymbol</code> relative to
     * the point specified.
     * @param loc  Java coordinates to draw at
     * @return  Rectangle based on that point, the offset and the
     *          width and height of the symbol.
     */
    public Rectangle getBounds(Point2D loc) {
        if (offset != null) {
            bounds.x = offset.x + (int) loc.getX() - bounds.width / 2;
            bounds.y = offset.y + (int) loc.getY() - bounds.height / 2;
        } else {
            bounds.x = (int) loc.getX();
            bounds.y = (int) loc.getY();
        }
        return bounds;
    }

    /**
     * This is called to paint when we are managed by other objects
     * (e.g., StationLocationRenderer)
     * @param  g  graphics to draw on
     * @param  loc  location to draw at.  Used to normalize the
     *         bounds.
     * @see #getBounds(Point2D)
     */
    public void draw(Graphics2D g, Point2D loc) {

        getBounds(loc);
        //      g.setColor (Color.green);
        //      g.drawRect (bounds.x, bounds.y, bounds.width, bounds.height);
        draw(g, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Draw the symbol, offset from the given location, using
     * "normalized device" coordinates. Subclasses implement this
     * based on the behavior of their own instance.
     * @param  g        Graphics to draw to
     * @param  x        x location
     * @param  y        y location
     * @param  width    width to draw
     * @param  height   height to draw
     */
    public abstract void draw(Graphics2D g, int x, int y, int width,
                              int height);



    /**
     * Set whether this <code>MetSymbol</code> is active or not.
     * @param active  true to be active (i.e., visible)
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get whether this <code>MetSymbol</code> is active or not.
     * @return  true if active (i.e., visible)
     */
    public boolean getActive() {
        return active;
    }


    /**
     * Set whether this <code>MetSymbol</code> is missing or not.
     * (i.e., no parameters or values have been set.
     * @param missing  true if missing.
     */
    public void setMissing(boolean missing) {
        this.missing = missing;
    }


    /**
     * Get whether this <code>MetSymbol</code> is missing or not.
     * (i.e., no parameters or values have been set.
     * @return true if missing.
     */
    public boolean getMissing() {
        return missing;
    }

    /**
     * Set the attribute with the value supplied.
     * @param name   name of attribute.
     * @param value  value of attribute.
     */
    public void setAttr(String name, String value) {
        if (name.equals(ATTR_PARAMID)) {
            if (paramIds == null) {
                paramIds = new String[1];
            }
            paramIds[0] = value;
        } else {
            super.setAttr(name, value);
        }
    }



    /**
     * Set the ColorTable property.
     *
     * @param value The new value for ColorTable
     */
    public void setColorTable(ColorTable value) {
        colorTable = value;
    }

    /**
     * Get the ColorTable property.
     *
     * @return The ColorTable
     */
    public ColorTable getColorTable() {
        return colorTable;
    }


    /**
     * Set the ColorTableParam property.
     *
     * @param value The new value for ColorTableParam
     */
    public void setColorTableParam(String value) {
        colorTableParam = value;
    }

    /**
     * Get the ColorTableParam property.
     *
     * @return The ColorTableParam
     */
    public String getColorTableParam() {
        return colorTableParam;
    }




    /**
     * Set the name of the ColorTable unit.  Used by subclasses which
     * have values that can be ColorTableed in different units.
     * @param name    name of unit
     */
    public void setColorTableUnitName(String name) {
        colorTableUnitName = name;
        colorTableUnit     = null;
    }

    /**
     * Get the name of the colorTable unit.
     * @return  String representation of the unit name.
     *          May be <code>null</code>.
     */
    public String getColorTableUnitName() {
        return colorTableUnitName;
    }



    /**
     * Get the the colorTable unit.
     * @return  Unit used for colorTableing values. May be <code>null</code>.
     */
    public Unit getColorTableUnit() {
        if ((colorTableUnit == null) && (colorTableUnitName != null)) {
            try {
                colorTableUnit =
                    ucar.visad.Util.parseUnit(colorTableUnitName);
            } catch (Exception exc) {}
        }
        return colorTableUnit;
    }



    /**
     *  Set the ColorTableRange property.
     *
     *  @param value The new value for ColorTableRange
     */
    public void setColorTableRange(Range value) {
        if(value!=null) {
            colorTableRange = new Range(value);
        } else {
            colorTableRange = value;
        }
    }

    /**
     *  Get the ColorTableRange property.
     *
     *  @return The ColorTableRange
     */
    public Range getColorTableRange() {
        return colorTableRange;
    }



    /**
     * Set the name of the Scale unit.  Used by subclasses which
     * have values that can be Scaleed in different units.
     * @param name    name of unit
     */
    public void setScaleUnitName(String name) {
        scaleUnitName = name;
        scaleUnit     = null;
    }

    /**
     * Get the name of the scale unit.
     * @return  String representation of the unit name.
     *          May be <code>null</code>.
     */
    public String getScaleUnitName() {
        return scaleUnitName;
    }



    /**
     * Get the the scale unit.
     * @return  Unit used for scaleing values. May be <code>null</code>.
     */
    public Unit getScaleUnit() {
        if ((scaleUnit == null) && (scaleUnitName != null)) {
            try {
                scaleUnit = ucar.visad.Util.parseUnit(scaleUnitName);
            } catch (Exception exc) {}
        }
        return scaleUnit;
    }



    /**
     *  Set the ScaleRange property.
     *
     *  @param value The new value for ScaleRange
     */
    public void setScaleRange(Range value) {
        scaleRange = value;
    }

    /**
     *  Get the ScaleRange property.
     *
     *  @return The ScaleRange
     */
    public Range getScaleRange() {
        return scaleRange;
    }


    /**
     *  Set the ScaleRange property.
     *
     *  @param value The new value for ScaleRange
     */
    public void setScaleDataRange(Range value) {
        scaleDataRange = value;
    }

    /**
     *  Get the ScaleRange property.
     *
     *  @return The ScaleRange
     */
    public Range getScaleDataRange() {
        return scaleDataRange;
    }



    /**
     *  Set the ScaleParam property.
     *
     *  @param value The new value for ScaleParam
     */
    public void setScaleParam(String value) {
        scaleParam = value;
    }

    /**
     *  Get the ScaleParam property.
     *
     *  @return The ScaleParam
     */
    public String getScaleParam() {
        return scaleParam;
    }

    /**
     *  Set the Scale property.
     *
     *  @param value The new value for Scale
     */
    public void setScale(double value) {
        scale = value;
    }

    /**
     *  Get the Scale property.
     *
     *  @return The Scale
     */
    public double getScale() {
        return scale;
    }






    /**
     * Set the RotateParam property.
     *
     * @param value The new value for RotateParam
     */
    public void setRotateParam(String value) {
        getRotateZInfo().setParam(value);
    }



    /**
     * Set the name of the Rotate unit.  Used by subclasses which
     * have values that can be Rotateed in different units.
     * @param name    name of unit
     */
    public void setRotateUnitName(String name) {
        getRotateZInfo().setUnitName(name);
    }





    /**
     *  Set the RotateRange property.
     *
     *  @param value The new value for RotateRange
     */
    public void setRotateRange(Range value) {
        getRotateZInfo().setRange(value);
    }


    /**
     *  Set the RotateRange property.
     *
     *  @param value The new value for RotateRange
     */
    public void setRotateDataRange(Range value) {
        getRotateZInfo().setDataRange(value);
    }








    /**
     *  Set the RotateXInfo property.
     *
     *  @param value The new value for RotateXInfo
     */
    public void setRotateXInfo(RotateInfo value) {
        rotateXInfo = value;
    }

    /**
     *  Get the RotateXInfo property.
     *
     *  @return The RotateXInfo
     */
    public RotateInfo getRotateXInfo() {
        if (rotateXInfo == null) {
            rotateXInfo = new RotateInfo(RotateInfo.TYPE_X);
        }
        return rotateXInfo;
    }



    /**
     *  Set the RotateYInfo property.
     *
     *  @param value The new value for RotateYInfo
     */
    public void setRotateYInfo(RotateInfo value) {
        rotateYInfo = value;
    }

    /**
     *  Get the RotateYInfo property.
     *
     *  @return The RotateYInfo
     */
    public RotateInfo getRotateYInfo() {
        if (rotateYInfo == null) {
            rotateYInfo = new RotateInfo(RotateInfo.TYPE_Y);
        }
        return rotateYInfo;
    }


    /**
     *  Set the RotateZInfo property.
     *
     *  @param value The new value for RotateZInfo
     */
    public void setRotateZInfo(RotateInfo value) {
        rotateZInfo = value;
    }

    /**
     *  Get the RotateZInfo property.
     *
     *  @return The RotateZInfo
     */
    public RotateInfo getRotateZInfo() {
        if (rotateZInfo == null) {
            rotateZInfo = new RotateInfo(RotateInfo.TYPE_Z);
        }
        return rotateZInfo;
    }

    /**
     * Get the rotate info object for the given type. Type is
     * TYPE_X, TYPE_Y or TYPE_Z
     *
     * @param type type
     *
     * @return The corresponding rotate info object
     */
    public RotateInfo getRotateInfo(int type) {
        if (type == RotateInfo.TYPE_X) {
            return getRotateXInfo();
        }
        if (type == RotateInfo.TYPE_Y) {
            return getRotateYInfo();
        }
        return getRotateZInfo();
    }



    /**
     *  Set the ColorParam property.
     *
     *  @param value The new value for ColorParam
     */
    public void setColorParam(String value) {
        colorParam = value;
    }

    /**
     *  Get the ColorParam property.
     *
     *  @return The ColorParam
     */
    public String getColorParam() {
        return colorParam;
    }


    /**
     * Set the ColorMappings property.
     *
     * @param value The new value for ColorMappings
     */
    public void setColorMappings(List value) {
        colorMappings = value;
    }

    /**
     * Get the ColorMappings property.
     *
     * @return The ColorMappings
     */
    public List getColorMappings() {
        return colorMappings;
    }



    /**
     * Set the name of the display unit.  Used by subclasses which
     * have values that can be displayed in different units.
     * @param name    name of unit
     */
    public void setDisplayUnitName(String name) {
        displayUnitName = name;
        displayUnit     = null;
    }

    /**
     * Should we show the display unit widget in the properties dialog
     *
     * @return true
     */
    protected boolean showDisplayUnitInProperties() {
        return false;
    }


    /**
     * Get the name of the display unit.
     * @return  String representation of the unit name.
     *          May be <code>null</code>.
     */
    public String getDisplayUnitName() {
        return displayUnitName;
    }


    /**
     * Set the the display unit.  Used by subclasses which
     * have values that can be displayed in different units.
     * @param u  unit to display values in
     */
    public void setTheDisplayUnit(Unit u) {
        displayUnit     = u;
        displayUnitName = (u == null)
                          ? null
                          : u.toString();
    }


    /**
     * Get the the display unit.
     * @return  Unit used for displaying values. May be <code>null</code>.
     */
    public Unit getDisplayUnit() {
        if ((displayUnit == null) && (displayUnitName != null)) {
            try {
                displayUnit = ucar.visad.Util.parseUnit(displayUnitName);
            } catch (Exception exc) {}
        }
        return displayUnit;
    }

    /**
     * Can we rotate this symbol when the display rotates
     */
    public boolean rotateOnEarth() {
        return true;
    }


}

