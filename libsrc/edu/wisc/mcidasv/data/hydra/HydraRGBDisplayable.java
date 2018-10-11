/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import java.awt.Color;
import java.rmi.RemoteException;

import ucar.unidata.util.Range;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.ScalarMapSet;

import visad.BadMappingException;
import visad.BaseColorControl;
import visad.ConstantMap;
import visad.DataRenderer;
import visad.Display;
import visad.RangeControl;
import visad.RealType;
import visad.ScalarMap;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.ScalarMapListener;
import visad.Unit;
import visad.VisADException;
import visad.bom.ImageRendererJ3D;
import visad.java3d.DefaultRendererJ3D;

import ucar.unidata.idv.control.HydraControl;

public class HydraRGBDisplayable extends DisplayableData {

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

    private String colorPaletteName = null;

    /** color ScalarMap */
    private volatile ScalarMap colorMap;

    /** field index to Animation ScalarMap */
    private volatile ScalarMap animMap;

    /** control for ScalarMap */
    private volatile BaseColorControl colorControl;

    /** RealType for the ScalarMap */
    private volatile RealType rgbRealType;

    /** RealType for the SelectRange ScalarMap */
    private ScalarMap selectMap = null;

    /** RealType for the Animation ScalarMap */
    private RealType indexRealType;

    /** Control for select range */
    private RangeControl selectControl;

    /** RealType for the SelectRange ScalarMap */
    private RealType selectRealType = null;

    /** flag for whether alpha is used or not */
    private boolean alphaflag;

    /** local point size */
    private float myPointSize;

    /** low range for colors */
    //private double lowRange = 315;           // low range for scalarmap
    private double lowRange = Double.NaN;           // low range for scalarmap

    /** high range for colors */
    //private double highRange = 230;          // high range for scalarmap
    private double highRange = Double.NaN;          // high range for scalarmap

    /** default polygonMode */
    private int polygonMode = POLYGON_FILL;

    /** default curvedSize */
    private int curvedSize = 10;

    /** low range for select */
    private double lowSelectedRange = Double.NaN;   // low range for scalarmap

    /** high range for select */
    private double highSelectedRange = Double.NaN;  // high range for scalarmap

    /** low range for select map */
    private double minSelect = Double.NaN;          // low range for scalarmap

    /** high range for select map */
    private double maxSelect = Double.NaN;          // high range for scalarmap

    private HydraControl multiSpecCntrl;

    private boolean useDefaultRenderer = false;

    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @param rgbRealType       The type of the RGB parameter.  May be
     *                          {@code null}.
     * @param alphaflag         boolean - will use Display.RBGA if true
     *                            otherwise only Display.RGB
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public HydraRGBDisplayable(String name, RealType rgbRealType, RealType indexRealType, boolean alphaflag, 
                 HydraControl multiSpecCntrl)
            throws VisADException, RemoteException {
        this(name, rgbRealType, indexRealType, null, alphaflag, null, multiSpecCntrl);
    }

    public HydraRGBDisplayable(String name, RealType rgbRealType, RealType indexRealType, float[][] colorPalette, boolean alphaflag, Range initRange,
                   HydraControl multiSpecCntrl)
            throws VisADException, RemoteException {
        this(name, rgbRealType, indexRealType, colorPalette, null, alphaflag, initRange, multiSpecCntrl);
    }

    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @param rgbRealType       The type of the RGB parameter.  May be
     *                          {@code null}.
     * @param colorPalette      The initial colorPalette to use. May be
     *                          {@code null} (Vis5D palette used
     *                          as default).
     * @param alphaflag         boolean - use Display.RBGA if true
     * @param initRange         Range to use as initial or first min,max
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public HydraRGBDisplayable(String name, RealType rgbRealType, RealType indexRealType, float[][] colorPalette, String colorPaletteName, boolean alphaflag, Range initRange,
                   HydraControl multiSpecCntrl)
            throws VisADException, RemoteException {

        super(name);
        
        this.rgbRealType  = rgbRealType;
        this.selectRealType = rgbRealType;
        this.indexRealType  = indexRealType;
        this.colorPalette = colorPalette;
        this.colorPaletteName = colorPaletteName;
        this.alphaflag    = alphaflag;
        this.multiSpecCntrl = multiSpecCntrl;

        if (initRange != null) {
          this.lowRange = initRange.getMin();
          this.highRange = initRange.getMax();
        }

        if (rgbRealType != null) {
            setColorMaps();
            if (useDisplayUnitForColor()) {
                setDisplayUnit(rgbRealType.getDefaultUnit());
            } else {
                setColorUnit(rgbRealType.getDefaultUnit());
            }
        }

        if (indexRealType != null) {
          //-setAnimationMap();
          setSelectMap();
        }

        if (selectRealType != null) {
          //setSelectMaps();
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
    protected HydraRGBDisplayable(HydraRGBDisplayable that)
            throws VisADException, RemoteException {

        super(that);
        colorPalette = that.colorPalette;
        rgbRealType  = that.rgbRealType;  // immutable object
        alphaflag    = that.alphaflag;

        if (rgbRealType != null) {
            setColorMaps();
        }
    }

    /**
     * Sets the RealType of the RGB parameter.
     * @param realType          The RealType of the RGB parameter.  May
     *                          not be {@code null}.
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
    }

    public ScalarMap getColorMap() {
      return colorMap;
    }

    public ScalarMap getAnimationMap() {
      return animMap;
    }


    /**
     * Returns the RealType of the RGB parameter.
     * @return                  The RealType of the color parameter.  May
     *                          be {@code null}.
     */
    public RealType getRGBRealType() {
        return rgbRealType;
    }

    /**
     * Returns the RealType of the SelectRange parameter.
     * @return                  The RealType of the select range parameter.  May
     *                          be {@code null}.
     */
    public RealType getSelectRealType() {
        return selectRealType;
    }

    protected DataRenderer getDataRenderer() throws VisADException {
      if (useDefaultRenderer) {
        return new DefaultRendererJ3D();
      }
      else {
        return new ImageRendererJ3D();
      }
    }

    public void setDefaultRenderer() {
      useDefaultRenderer = true;
    }

    public void setImageRenderer() {
      useDefaultRenderer = false;
    }

    /**
     * Sets the set of ScalarMap-s of this instance.  The ScalarMap-s of
     * this instance will be added to the set before the SCALAR_MAP_SET
     * property is set.  This method fires a PropertyChangeEvent for
     * SCALAR_MAP_SET with {@code null} for the old value and the new
     * set of ScalarMap-s for the new Value.  Intermediate subclasses that
     * have their own ScalarMap-s should override this method and invoke
     * {@code super.setScalarMaps(ScalarMapSet)}.
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
     * @param colorPalette Color table or color-alpha table desired.
     * @param name Name for the color table (can be {@code null}).
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setColorPalette(float[][] colorPalette, String name)
            throws RemoteException, VisADException {
        if (colorControl != null) {
            colorControl.setTable(colorPalette);
        }

        this.colorPalette = colorPalette;
        this.colorPaletteName = name;
    }

    /**
     * This method sets the color palette
     * according to the color table in argument;
     * pair this method with setRange(lo,high) to get
     * a fixed association of color table and range of values;
     * asigns null (doesn't have a name) for the name.
     *
     * @param colorPalette     the color table or color-alpha table desired
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setColorPalette(float[][] colorPalette)
            throws VisADException, RemoteException {
        setColorPalette(colorPalette, null);
    }

    /**
     * Return the current color palette in this Displayable
     *
     * @return a color table float[3][len] or color-alpha table float[4][len]
     */
    public float[][] getColorPalette() {
        return colorPalette;
    }

    public String getColorPaletteName() {
        return colorPaletteName;
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
            table[0][m] = color.getRed() / 255.f;    // Red amount  
            table[1][m] = color.getGreen() / 255.f;  // Green
            table[2][m] = color.getBlue() / 255.f;   // Blue  
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
        if ((colorMap != null) && hasRange()) {
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
        return new double[]{ lowRange, highRange };
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
     * Sets the size of points in this Displayable.
     *
     * @param   pointSize     Size of points (2 = normal)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setPointSize(float pointSize)
            throws VisADException, RemoteException {

        synchronized (this) {
            addConstantMap(new ConstantMap(pointSize, Display.PointSize));
            myPointSize = pointSize;
        }

    }

    /**
     * Gets the point size associated with this LineDrawing
     *
     * @return  point size
     */
    public float getPointSize() {
        return myPointSize;
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
        addConstantMap(new ConstantMap(convertToVisADPolygonMode(polygonMode),
                                       Display.PolygonMode));
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
     * @param curvedSize size to use (&gt; 0)
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
        if ( !alphaflag) {
            colorMap = new ScalarMap(rgbRealType, Display.RGB);
        } else {
            colorMap = new ScalarMap(rgbRealType, Display.RGBA);
        }

        applyUnit(colorMap, rgbRealType);

        if (hasRange()) {
           colorMap.setRange(lowRange, highRange);
        }

        colorMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == ScalarMapEvent.CONTROL_ADDED)
                        || (id == ScalarMapEvent.CONTROL_REPLACED)) {
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
                if ((event.getId() == ScalarMapEvent.AUTO_SCALE) && hasRange()) {
                  double[] rng = colorMap.getRange();
                  if (multiSpecCntrl != null) {
                    multiSpecCntrl.updateRange(new Range(rng));
                  }
                }
            }
        });
        ScalarMapSet maps = getScalarMapSet();  //new ScalarMapSet();
        maps.add(colorMap);
        setScalarMapSet(maps);
    }

    private void setSelectMap() throws RemoteException, VisADException {
      animMap = new ScalarMap(indexRealType, Display.SelectValue);
      ScalarMapSet maps = getScalarMapSet();
      maps.add(animMap);
      setScalarMapSet(maps);
    }

    /**
     * Sets the RealType of the select parameter.
     * @param realType          The RealType of the RGB parameter.  May
     *                          not be {@code null}.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setSelectRealType(RealType realType)
            throws RemoteException, VisADException {

        if ( !realType.equals(selectRealType)) {
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
            selectControl.setRange(new double[]{ low, hi });
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
     * creates the ScalarMap for SelectRange and control for this Displayable.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setSelectMaps() throws RemoteException, VisADException {

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

                if ((id == ScalarMapEvent.CONTROL_ADDED)
                        || (id == ScalarMapEvent.CONTROL_REPLACED)) {
                    selectControl = (RangeControl) selectMap.getControl();
                    if (hasSelectedRange()) {
                        selectControl.setRange(new double[]{ lowSelectedRange,
                                                             highSelectedRange });
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {
                if ((event.getId() == ScalarMapEvent.AUTO_SCALE)
                        && hasSelectMinMax()) {
                    selectMap.setRange(minSelect, maxSelect);
                }
            }
        });
        ScalarMapSet maps = getScalarMapSet();  //new ScalarMapSet();
        maps.add(selectMap);
        setScalarMapSet(maps);
    }

}
