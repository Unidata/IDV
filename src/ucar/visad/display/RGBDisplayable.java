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


import ucar.unidata.beans.*;

import visad.*;

import java.awt.Color;



import java.rmi.RemoteException;

import java.util.Iterator;


/**
 * Provides support for a Displayable that needs a map to either Display.RGB
 * or to Display.RGBA.
 *
 * <p>
 * Instances of this class have the following bound properties:<br>
 * <table border align=center>
 *
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Access</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr align=center>
 * <td>colorPalette</td>
 * <td>float[][]</td>
 * <td>set/get</td>
 * <td>0</td>
 * <td align=left>The color palette for this instance.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>rgbRealType</td>
 * <td>visad.RealType</td>
 * <td>set/get</td>
 * <td><code>null</code></td>
 * <td align=left>The VisAD type of the colored quantity.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>lineWidth</td>
 * <td>float</td>
 * <td>set/get</td>
 * <td>1.0f</td>
 * <td align=left>The width of rendered lines.</td>
 *
 * </tr>
 *
 * </table>
 *
 * @author Don Murray
 * @version $Revision: 1.58 $
 */
public abstract class RGBDisplayable extends DisplayableData {

    /**
     * The name of the "color palette" property.
     */
    public static final String COLOR_PALETTE = "colorPalette";

    /**
     * The name of the "RGB real-type" property.
     */
    public static final String RGB_REAL_TYPE = "rgbRealType";

    /**
     * The polygon fill style
     */
    public static final int POLYGON_FILL = 0;

    /**
     * The polygon line style
     */
    public static final int POLYGON_LINE = 1;

    /**
     * The polygon point style
     */
    public static final int POLYGON_POINT = 2;

    /**
     * Color Palette
     */
    private float[][] colorPalette = null;

    /** color ScalarMap */
    private volatile ScalarMap colorMap;

    /** control for ScalarMap */
    private volatile BaseColorControl colorControl;

    /** RealType for the ScalarMap */
    private volatile RealType rgbRealType;

    /** RealType for the SelectRange ScalarMap */
    private ScalarMap selectMap = null;

    /** Control for select range */
    private RangeControl selectControl;

    /** RealType for the SelectRange ScalarMap */
    private RealType selectRealType = null;

    /** flag for whether alpha is used or not */
    private boolean alphaflag;

    /** low range for colors */
    private double lowRange = Double.NaN;  // low range for scalarmap

    /** high range for colors */
    private double highRange = Double.NaN;  // high range for scalarmap

    /** default polygonMode */
    private int polygonMode = POLYGON_FILL;

    /** default curvedSize */
    private int curvedSize = 10;

    /** low range for select */
    private double lowSelectedRange = Double.NaN;  // low range for scalarmap

    /** high range for select */
    private double highSelectedRange = Double.NaN;  // high range for scalarmap

    /** low range for select map */
    private double minSelect = Double.NaN;  // low range for scalarmap

    /** high range for select map */
    private double maxSelect = Double.NaN;  // high range for scalarmap

    /** type for select */
    private boolean useRGBTypeForSelect = false;

    /** type for select */
    private boolean autoScaleColorRange = false;

    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @param rgbRealType       The type of the RGB parameter.  May be
     *                          <code>null</code>.
     * @param alphaflag         boolean - will use Display.RBGA if true
     *                            otherwise only Display.RGB
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RGBDisplayable(String name, RealType rgbRealType,
                          boolean alphaflag)
            throws VisADException, RemoteException {
        this(name, rgbRealType, null, alphaflag);
    }

    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @param rgbRealType       The type of the RGB parameter.  May be
     *                          <code>null</code>.
     * @param colorPalette      The initial colorPalette to use. May be
     *                          <code>null</code> (Vis5D palette used
     *                          as default).
     * @param alphaflag         boolean - use Display.RBGA if true
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RGBDisplayable(String name, RealType rgbRealType,
                          float[][] colorPalette, boolean alphaflag)
            throws VisADException, RemoteException {

        super(name);

        this.rgbRealType  = rgbRealType;
        this.colorPalette = colorPalette;
        this.alphaflag    = alphaflag;

        if (rgbRealType != null) {
            setColorMaps();
            if (useDisplayUnitForColor()) {
                setDisplayUnit(rgbRealType.getDefaultUnit());
            } else {
                setColorUnit(rgbRealType.getDefaultUnit());
            }
        }
    }

    /**
     * Does this object use the displayUnit (or the colorUnit) for its
     * display unit. The default is true.  This allows derived classes
     * to have this class use the colorUnit.
     * @return  true if the display unit is the same as the color unit
     */
    protected boolean useDisplayUnitForColor() {
        return true;
    }


    /**
     * Constructs from another instance.  The following attributes are set from
     * the other instance: color palette, the color RealType.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected RGBDisplayable(RGBDisplayable that)
            throws VisADException, RemoteException {

        super(that);
        colorPalette   = that.colorPalette;
        rgbRealType    = that.rgbRealType;     // immutable object
        selectRealType = that.selectRealType;  // immutable object
        alphaflag      = that.alphaflag;

        if (rgbRealType != null) {
            setColorMaps();
        }
    }

    /**
     * Sets the RealType of the RGB parameter.
     * @param realType          The RealType of the RGB parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRGBRealType(RealType realType)
            throws RemoteException, VisADException {

        if ( !realType.equals(rgbRealType)) {
            RealType oldValue = rgbRealType;
            rgbRealType = realType;
            setColorMaps();
            if (useDisplayUnitForColor()) {
                if ( !isUnitCompatible(rgbRealType, getDisplayUnit())) {
                    setDisplayUnit(null);
                }
            } else {
                if ( !isUnitCompatible(rgbRealType, getColorUnit())) {
                    setColorUnit(null);
                }
            }
            firePropertyChange(RGB_REAL_TYPE, oldValue, rgbRealType);
        }
        if (getUseRGBTypeForSelect()) {
            setSelectRealType(rgbRealType);
        }
    }

    /**
     * Returns the RealType of the RGB parameter.
     * @return                  The RealType of the color parameter.  May
     *                          be <code>null</code>.
     */
    public RealType getRGBRealType() {
        return rgbRealType;
    }

    /**
     * Returns the RealType of the SelectRange parameter.
     * @return                  The RealType of the select range parameter.  May
     *                          be <code>null</code>.
     */
    public RealType getSelectRealType() {
        return selectRealType;
    }

    /**
     * Sets the set of ScalarMap-s of this instance.  The ScalarMap-s of
     * this instance will be added to the set before the SCALAR_MAP_SET
     * property is set.  This method fires a PropertyChangeEvent for
     * SCALAR_MAP_SET with <code>null</code> for the old value and the new
     * set of ScalarMap-s for the new Value.  Intermediate subclasses that
     * have their own ScalarMap-s should override this method and invoke
     * <code>super.setScalarMaps(ScalarMapSet)</code>.
     * @param maps              The set of ScalarMap-s to be added.
     * @throws BadMappingException      The RealType of the color parameter
     *                          has not been set or its ScalarMap is alread in
     *                          the set.
     */
    protected void setScalarMaps(ScalarMapSet maps)
            throws BadMappingException {

        if (colorMap == null) {
            throw new BadMappingException(getClass().getName()
                                          + ".setScalarMaps(ScalarMapSet): "
                                          + "Color not yet set");
        }

        maps.add(colorMap);

        if (selectMap != null) {

            maps.add(selectMap);
        }

        super.setScalarMapSet(maps);
    }

    /**
     * This method sets the color palette
     * according to the color table in argument;
     * pair this method with setRange(lo,high) to get
     * a fixed association of color table and range of values.
     *
     * @param colorPalette     the color table or color-alpha table desired
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setColorPalette(float[][] colorPalette)
            throws RemoteException, VisADException {
        if (colorControl != null) {
            colorControl.setTable(colorPalette);
        }

        this.colorPalette = colorPalette;
    }

    /**
     * Return the current color palette in this Displayable
     *
     * @return a color table float[3][len] or color-alpha table float[4][len]
     */
    public float[][] getColorPalette() {
        return colorPalette;
    }

    /**
     * Make a color palette representing this color and set it as the
     * color pallete.
     *
     * @param  color  color to use
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setColor(Color color) throws RemoteException, VisADException {
        int       len   = 5;
        float[][] table = new float[(alphaflag == true)
                                    ? 4
                                    : 3][len];
        for (int m = 0; m < len; m++) {
            table[0][m] = color.getRed() / 255.f;        // Red amount  
            table[1][m] = color.getGreen() / 255.f;      // Green
            table[2][m] = color.getBlue() / 255.f;       // Blue  
            if (alphaflag) {
                table[3][m] = color.getAlpha() / 255.f;  // alpha
            }
        }
        setColorPalette(table);
    }

    /**
     * This method sets the color palette to shades of grey.
     *
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public final void setGreyPalette()
            throws RemoteException, VisADException {

        if (colorControl != null) {
            colorControl.initGreyWedge();
            setColorPalette(colorControl.getTable());
        }
    }

    /**
     * This method with no argument sets the default Vis5D color spectrum.
     *
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public final void setVisADPalette()
            throws RemoteException, VisADException {

        if (colorControl != null) {
            colorControl.initVis5D();
            setColorPalette(colorControl.getTable());
        }
    }

    /**
     * Set the upper and lower limit of the range values associated
     * with a color table.
     *
     * @param low    the minimun value
     * @param hi     the maximum value
     * @deprecated   use setRangeForColor
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRange(double low, double hi)
            throws VisADException, RemoteException {

        setRangeForColor(low, hi);
    }

    /**
     * Set the upper and lower limit of the range values associated
     * with a color table.
     *
     * Matches method name in Contour2DDisplayable
     *
     * @param low               The minimum value of the parameter matched to
     *                          the low end of the color table.
     * @param hi                The maximum value of the parameter matched to
     *                          the high end of the color table.
     *
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public void setRangeForColor(double low, double hi)
            throws VisADException, RemoteException {
        lowRange  = low;
        highRange = hi;
        if ((colorMap != null) && hasRange() && !getAutoScaleColorRange()) {
            colorMap.setRange(low, hi);
        }
    }

    /**
     * Get the color range
     *
     * @return an array of the low and high values for the range
     * @deprecated  use #getRangeForColor()
     */
    public double[] getRangeforColor() {
        return getRangeForColor();
    }

    /**
     * Get the color range
     *
     * @return an array of the low and high values for the range
     */
    public double[] getRangeForColor() {
        return new double[] { lowRange, highRange };
    }

    /**
     * Apply the correct unit (either the displayUnit or the colorUnit)
     * to the scalar map
     *
     * @param colorMap   ScalarMap to apply to
     * @param rgbRealType  RealType for default Unit
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void applyUnit(ScalarMap colorMap, RealType rgbRealType)
            throws VisADException, RemoteException {
        if (useDisplayUnitForColor()) {
            applyDisplayUnit(colorMap, rgbRealType);
        } else {
            applyColorUnit(colorMap, rgbRealType);
        }
    }


    /**
     * Set the units for the displayed range
     *
     * @param unit Unit for display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setDisplayUnit(Unit unit)
            throws VisADException, RemoteException {
        if (useDisplayUnitForColor()) {
            //Make sure this unit is ok
            checkUnit(rgbRealType, unit);
        }
        super.setDisplayUnit(unit);
        if (useDisplayUnitForColor()) {
            applyUnit(colorMap, rgbRealType);
        }
        if (getUseRGBTypeForSelect()) {
            applyUnit(selectMap, selectRealType);
        }
    }


    /**
     * Set the units for the displayed range
     *
     * @param unit Unit for display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setColorUnit(Unit unit)
            throws VisADException, RemoteException {
        if ( !useDisplayUnitForColor()) {
            //Make sure this unit is ok
            checkUnit(rgbRealType, unit);
        }
        super.setColorUnit(unit);
        if ( !useDisplayUnitForColor()) {
            applyUnit(colorMap, rgbRealType);
        }
    }


    /**
     * Returns whether this Displayable has a valid range (i.e., lowRange and
     * highRange are both not NaN's
     *
     * @return true if range has been set
     */
    public boolean hasRange() {
        return ( !Double.isNaN(lowRange) && !Double.isNaN(highRange));
    }


    /**
     * Set the type of polygon display that should be used
     *
     * @param polygonMode  polygon mode
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setPolygonMode(int polygonMode)
            throws VisADException, RemoteException {
        this.polygonMode = polygonMode;
        addConstantMap(
            new ConstantMap(
                convertToVisADPolygonMode(polygonMode), Display.PolygonMode));
    }

    /**
     * Converts an RGBDisplayable Polygon mode to the appropriate
     * (or default) VisAD mode
     *
     * @param myMode  polygon mode
     * @return  Java3D mode
     */
    private int convertToVisADPolygonMode(int myMode) {
        if (visad.util.Util.canDoJava3D()) {
            switch (myMode) {

              case POLYGON_FILL :
                  return visad.java3d.DisplayImplJ3D.POLYGON_FILL;

              case POLYGON_LINE :
                  return visad.java3d.DisplayImplJ3D.POLYGON_LINE;

              case POLYGON_POINT :
                  return visad.java3d.DisplayImplJ3D.POLYGON_POINT;

              default :
                  return visad.java3d.DisplayImplJ3D.POLYGON_FILL;
            }
        } else {
            return 0;
        }
    }

    /**
     * Return the type of polygon mode being used
     *
     * @return polygon mode
     */
    public int getPolygonMode() {
        return polygonMode;
    }

    /**
     * Set the curved size for textured displays
     *
     * @param curvedSize size to use (> 0)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setCurvedSize(int curvedSize)
            throws VisADException, RemoteException {
        this.curvedSize = curvedSize;
        addConstantMap(makeCurvedSizeMap(curvedSize));
    }

    /**
     * Create the ConstantMap for the texture curve size
     *
     * @param curvedSize   size for texture curve
     * @return  ConstantMap
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    protected ConstantMap makeCurvedSizeMap(int curvedSize)
            throws VisADException, RemoteException {
        return new ConstantMap(curvedSize, Display.CurvedSize);
    }

    /**
     * Return the size of a curved texture
     * @return curved size
     */
    public int getCurvedSize() {
        return curvedSize;
    }

    /**
     * creates the ScalarMap for color and ColorControl for this Displayable.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setColorMaps() throws RemoteException, VisADException {

        // ScalarMap is either mapping to Display.RGB (color only)
        // or to Display.RGBA color plus transparency.
        ScalarMapSet maps = getScalarMapSet();  //new ScalarMapSet();
        if (colorMap != null) {
            maps.remove(colorMap);
        }
        if ( !alphaflag) {
            colorMap = new ScalarMap(rgbRealType, Display.RGB);
        } else {
            colorMap = new ScalarMap(rgbRealType, Display.RGBA);
        }

        applyUnit(colorMap, rgbRealType);

        if (hasRange() && !getAutoScaleColorRange()) {
            colorMap.setRange(lowRange, highRange);
        }

        colorMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    colorControl = (BaseColorControl) colorMap.getControl();

                    if (colorControl != null) {
                        if (colorPalette != null) {
                            colorControl.setTable(colorPalette);
                        } else {
                            colorPalette = colorControl.getTable();
                        }
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {
                if ((event.getId() == event.AUTO_SCALE) && hasRange()
                        && !getAutoScaleColorRange()) {
                    colorMap.setRange(lowRange, highRange);
                }
            }
        });
        maps.add(colorMap);
        setScalarMapSet(maps);
    }

    /**
     * Sets the RealType of the select parameter.
     * @param realType          The RealType of the RGB parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setSelectRealType(RealType realType)
            throws RemoteException, VisADException {

        if ( !realType.equals(selectRealType)) {
            RealType oldValue = selectRealType;
            selectRealType = realType;
            setSelectMaps();
            if (useDisplayUnitForColor()) {
                if ( !isUnitCompatible(selectRealType, getDisplayUnit())) {
                    setDisplayUnit(null);
                }
            } else {
                if ( !isUnitCompatible(selectRealType, getColorUnit())) {
                    setColorUnit(null);
                }
            }
        }
    }

    /**
     * Returns whether this Displayable has a valid range
     * (i.e., lowSelectedRange and highSelectedRange are both not NaN's
     *
     * @return true if range has been set
     */
    public boolean hasSelectedRange() {
        return ( !Double.isNaN(lowSelectedRange)
                 && !Double.isNaN(highSelectedRange));
    }

    /**
     * Set selected range with the range for select
     *
     * @param low  low select value
     * @param hi   hi select value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setSelectedRange(double low, double hi)
            throws VisADException, RemoteException {

        lowSelectedRange  = low;
        highSelectedRange = hi;
        if ((selectControl != null) && hasSelectedRange()) {
            selectControl.setRange(new double[] { low, hi });
        }

    }

    /**
     * Set the upper and lower limit of the range values associated
     * with a color table.
     *
     * @param low    the minimun value
     * @param hi     the maximum value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRangeForSelect(double low, double hi)
            throws VisADException, RemoteException {

        minSelect = low;
        maxSelect = hi;
        if ((selectMap != null) && hasSelectMinMax()) {
            selectMap.setRange(low, hi);
        }
    }

    /**
     * Check to see if the range has been set for the select
     *
     * @return true if it has
     */
    private boolean hasSelectMinMax() {
        return ( !Double.isNaN(minSelect) && !Double.isNaN(maxSelect));
    }

    /**
     * Set whether the RGB type is used for the select range.
     * @param yesno  true to use the RGB type for the
     */
    public void setUseRGBTypeForSelect(boolean yesno) {
        useRGBTypeForSelect = yesno;
    }

    /**
     * Get whether the RGB type is used for the select range.
     * @return true if the RGB type for the
     */
    public boolean getUseRGBTypeForSelect() {
        return useRGBTypeForSelect;
    }

    /**
     * Set whether the color scale should auto scale
     * @param yesno  true to autoscale
     */
    public void setAutoScaleColorRange(boolean yesno) {
        autoScaleColorRange = yesno;
        if (colorMap != null) {
            if (autoScaleColorRange) {
                colorMap.resetAutoScale();
            } else if (hasRange()) {
                try {
                    colorMap.setRange(lowRange, highRange);
                } catch (Exception ve) {}
            }
        }
    }

    /**
     * Set whether the RGB type is used for the select range.
     * @return true if autoscaling  is on
     */
    public boolean getAutoScaleColorRange() {
        return autoScaleColorRange;
    }

    /**
     * creates the ScalarMap for SelectRange and control for this Displayable.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setSelectMaps() throws RemoteException, VisADException {

        ScalarMapSet maps = getScalarMapSet();
        if (selectMap != null) {
            maps.remove(selectMap);  // remove the old one
        }
        selectMap = new ScalarMap(selectRealType, Display.SelectRange);

        if (selectRealType.equals(rgbRealType)) {
            applyUnit(selectMap, selectRealType);
        }

        if (hasSelectMinMax()) {
            selectMap.setRange(minSelect, maxSelect);
        }

        selectMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    selectControl = (RangeControl) selectMap.getControl();
                    if (hasSelectedRange() && (selectControl != null)) {
                        selectControl.setRange(new double[] {
                            lowSelectedRange,
                            highSelectedRange });
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {
                if ((event.getId() == event.AUTO_SCALE)
                        && hasSelectMinMax()) {
                    selectMap.setRange(minSelect, maxSelect);
                }
            }
        });
        maps.add(selectMap);
        setScalarMapSet(maps);
    }

}
