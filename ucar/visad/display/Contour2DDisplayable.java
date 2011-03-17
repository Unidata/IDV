/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.grid.GridUtil;

import visad.*;

import visad.util.DataUtility;

import java.awt.Color;

import java.rmi.RemoteException;


/**
 * A class to support showing 2D gridded data as colored contours on a plane
 * in a NavigatedDisplay.
 *
 * @author IDV Development Team
 * @version $Revision: 1.40 $
 */
public class Contour2DDisplayable extends ContourLines implements GridDisplayable {

    /**
     * The name of the "RGB real-type" property.
     */
    public static final String RGB_REAL_TYPE = "rgbRealType";

    /** local copy of the data */
    private Field field;

    /** RealType for the RGB ScalarMap */
    private RealType rgbRealType;

    /** color palette */
    private float[][] colorPalette = null;

    /** ScalarMap for color (RGB) */
    private volatile ScalarMap colorMap;

    /** Color control */
    private volatile BaseColorControl colorControl;

    /** low range for scalarmap */
    private double lowRange = Double.NaN;

    /** high range for scalarmap */
    private double highRange = Double.NaN;

    /** flag for whether Alpha is used or not */
    private boolean alphaFlag = false;

    /** flag for whether this is set up as one param colored by another */
    private boolean coloredByAnother = false;

    /**
     * Constructs an instance with the supplied name and null initial RealType.
     *
     * @param name a String identifier
     * @exception VisADException  from construction of super class
     * @exception RemoteException from construction of super class
     */
    public Contour2DDisplayable(String name)
            throws VisADException, RemoteException {
        this(name, false);
    }


    /**
     * Constructs an instance with the supplied name and null initial RealType
     * and given alphaFlag
     *
     * @param name a String identifier
     * @param alphaFlag  true if should use RGBA
     * @exception VisADException  from construction of super class
     * @exception RemoteException from construction of super class
     */
    public Contour2DDisplayable(String name, boolean alphaFlag)
            throws VisADException, RemoteException {
        this(name, null, null, alphaFlag);
    }

    /**
     * Constructs an instance with the supplied name and null initial RealType
     * and given alphaFlag and colorFill
     *
     * @param name a String identifier
     * @param alphaFlag  true if should use RGBA
     * @param colorFill  true if contours should be color filled
     * @exception VisADException  from construction of super class
     * @exception RemoteException from construction of super class
     */
    public Contour2DDisplayable(String name, boolean alphaFlag,
                                boolean colorFill)
            throws VisADException, RemoteException {
        this(name, null, null, alphaFlag);
        if (colorFill) {
            setColorFill(true);
        }
    }




    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter.
     *
     * @param name              The name for the displayable.
     * @param c2dRealType       The type of the parameter.  May be
     *                          <code>null</code>.
     * @param colorPalette      The initial colorPalette to use. May be
     *                          <code>null</code> (Vis5D palette used
     *                          as default).
     * @param rangeLimits       limits of the color range
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     * @deprecated rangeLimits not needed
     */
    public Contour2DDisplayable(String name, RealType c2dRealType,
                                float[][] colorPalette, float[] rangeLimits)
            throws VisADException, RemoteException {

        this(name, c2dRealType, colorPalette);

        this.colorPalette = colorPalette;

        if (c2dRealType != null) {
            setContourRealType(c2dRealType);
            setRGBRealType(c2dRealType);
        }
    }

    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter.
     *
     * @param name              The name for the displayable.
     * @param c2dRealType       The type of the parameter.  May be
     *                          <code>null</code>.
     * @param colorPalette      The initial colorPalette to use. May be
     *                          <code>null</code> (Vis5D palette used
     *                          as default).
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public Contour2DDisplayable(String name, RealType c2dRealType,
                                float[][] colorPalette)
            throws VisADException, RemoteException {

        this(name, c2dRealType, colorPalette, false);
    }


    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter.
     *
     * @param name              The name for the displayable.
     * @param c2dRealType       The type of the parameter.  May be
     *                          <code>null</code>.
     * @param colorPalette      The initial colorPalette to use. May be
     *                          <code>null</code> (Vis5D palette used
     *                          as default).
     * @param alphaFlag         true if should use RGBA
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public Contour2DDisplayable(String name, RealType c2dRealType,
                                float[][] colorPalette, boolean alphaFlag)
            throws VisADException, RemoteException {
        super(name, null);
        this.alphaFlag    = alphaFlag;
        this.colorPalette = colorPalette;

        if (c2dRealType != null) {
            setContourRealType(c2dRealType);
            setRGBRealType(c2dRealType);
        }
    }




    /**
     * Constructs from a Contour2DDisplayable.
     *
     * @param that a Contour2DDisplayable.
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    protected Contour2DDisplayable(Contour2DDisplayable that)
            throws VisADException, RemoteException {

        super(that);

        colorPalette = that.colorPalette;
        this.field   = that.field;

        if (that.getContourRealType() != null) {
            setContourRealType(that.getContourRealType());
        }
        if (that.rgbRealType != null) {
            setRGBRealType(that.getRGBRealType());
        }
    }

    /**
     * Does this object use the displayUnit (or the colorUnit) for its
     * display unit.
     * @return false if contour lines are colored by another field otherwise
     *               true
     */
    protected boolean useDisplayUnitForColor() {
        return !coloredByAnother;
    }

    /**
     * Sets the RealType of the parameter.
     * @param realType          The RealType of the parameter.  May
     *                          not be <code>null</code>.
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public void setC2DRealType(RealType realType)
            throws RemoteException, VisADException {

        setContourRealType(realType);
        setRGBRealType(realType);
        fireScalarMapSetChange();
        /*
        if (!realType.equals(gridRealType)) {
            RealType oldValue = gridRealType;
            gridRealType = realType;
            setColorMaps();
            super.setContourRealType (gridRealType);
            fireScalarMapSetChange();
        }
        */
    }


    /**
     * This method with no argument sets the default Vis5D color spectrum.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setColorPalette() throws RemoteException, VisADException {

        if (colorControl != null) {
            colorControl.initVis5D();
            colorPalette = colorControl.getTable();
        }
    }

    /**
     * Override setColor to actually set a color palette since this
     * class has both
     *
     * @param color   Color to use
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setColor(Color color) throws RemoteException, VisADException {

        colorPalette = new float[][] {
            { color.getRed() / 255.f }, { color.getGreen() / 255.f },
            { color.getBlue() / 255.f }
        };

        if (colorControl != null) {
            colorControl.setTable(colorPalette);
        }
    }

    /**
     * This method sets the color palette
     * according to the color table in argument;
     * pair this method with setRangeForColor below to get
     * a fixed association of color table and range of values.
     * @param colorPalette      The initial colorPalette to use. May be
     *                          <code>null</code> (Vis5D palette used
     *                          as default).
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public void setColorPalette(float[][] colorPalette)
            throws RemoteException, VisADException {

        if (colorControl != null) {
            colorControl.setTable(colorPalette);
        }

        this.colorPalette = colorPalette;
    }

    /**
     * To make a connection between a particular color and a particular data
     * value or a range of data values ...
     * "This mapping is determined by the low and high values passed
     * to ScalarMap.setRange(), and by the 'float[][] table' array
     * passed to BaseColorControl.setTable().  The table values
     * (length = table[0].length) are distributed evenly over the
     * (low, high) range.  If an application does not call setRange(),
     * then the range is determined by the range of values in
     * displayed data objects." (BH)
     * @param low               The minimum value of the parameter matched to
     *                          the low end of the color table.
     * @param high              The maximum value of the parameter matched to
     *                          the high end of the color table.
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public void setRangeForColor(double low, double high)
            throws VisADException, RemoteException {

        lowRange  = low;
        highRange = high;

        if ((colorMap != null) && hasRange()) {
            colorMap.setRange(low, high);
        }

    }

    /**
     * Get the color range
     *
     * @return an array of the low and high values for the range
     * @deprecated use #getRangeForColor()
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
     * Return the current color palette (color table).
     *
     * @return float[][] a VisAD color table
     */
    public float[][] getColorPalette() {
        return colorPalette;
    }

    /**
     * Set the data into the Displayable
     *
     * @param field a VisAD FieldImpl with a 2D nature.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     * @deprecated Should use loadData now
     */
    public void setGrid2D(FieldImpl field)
            throws RemoteException, VisADException {
        loadData(field);
    }

    /**
     * Set the appropriate ScalarMaps based on the data and then
     * load the data into the DataReference.
     *
     * @param field a 2D VisAD Field representing the data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void loadData(FieldImpl field)
            throws VisADException, RemoteException {


        RealType[] realTypes =
            GridUtil.getParamType(field).getRealComponents();

        // uncomment to determine ad-hoc rather than programatically
        //coloredByAnother = coloredByAnother && (rtt.getDimension() == 2);
        // get the RealType of the range data; use both for
        // isosurface and for RGB
        RealType contourType = realTypes[0];
        setContourRealType(contourType);  // in IsoSurface

        // get the type of the field for rgb color
        //    RealType rgbType = coloredByAnother
        //                      ? realTypes[1]
        //                       : contourType;
        RealType rgbType = contourType;
        if (coloredByAnother) {
            if (realTypes.length > 1) {
                rgbType = realTypes[1];
            } else if (GridUtil.hasEnsemble(field)) {
                rgbType = GridUtil.ENSEMBLE_TYPE;
            }
        }
        //if(GridUtil.hasEnsemble(field)){
        //    rgbType = GridUtil.ENSEMBLE_TYPE;
        //}

        setRGBRealType(rgbType);
        super.setData(field);
    }

    /**
     * Sets the RealType of the RGB parameter.
     * @param realType          The RealType of the RGB parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setRGBRealType(RealType realType)
            throws RemoteException, VisADException {

        if ( !realType.equals(rgbRealType)) {
            RealType oldValue = rgbRealType;
            rgbRealType = realType;
            if (useDisplayUnitForColor()) {
                if ( !isUnitCompatible(rgbRealType, getDisplayUnit())) {
                    setDisplayUnit(null);
                }
            } else {
                if ( !isUnitCompatible(rgbRealType, getColorUnit())) {
                    setColorUnit(null);
                }
            }
            setColorMaps();
            firePropertyChange(RGB_REAL_TYPE, oldValue, rgbRealType);
        }
    }

    /**
     * Set whether the contours should be displayed as color-filled
     * contours.  Need to override because if color filled is turned
     * on, we need to make sure that the RGB type is the same as the
     * Contour type.
     * @param yesorno  true for color fill
     * @throws VisADException  unable to set this
     * @throws RemoteException  unable to set this on remote display
     */
    public void setColorFill(boolean yesorno)
            throws VisADException, RemoteException {
        if (getContourRealType() != null) {
            setRGBRealType(getContourRealType());
        }
        super.setColorFill(yesorno);
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
     * Set appropriate contour levels with super class's methods
     * (BOTH REQUIRED IN THIS ORDER)
     *    setRange(min, max);
     * &    setContourLevels   (interval, base, min, max);
     *
     * @param interval      delta value between contours
     * @param base          one contour must be of this value
     * @param min           no contour below this value
     * @param max           no contour above this value
     *
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public void setContourLevels(float interval, float base, float min,
                                 float max)
            throws VisADException, RemoteException {
        setContourLevels(interval, base, min, max, false);
    }

    /**
     * Set appropriate contour levels with super class's methods
     * (BOTH REQUIRED IN THIS ORDER)
     *    setRange(min, max);
     *    setContourLevels   (interval, base, min, max);
     *
     * @param interval      delta value between contours
     * @param base          one contour must be of this value
     * @param min           no contour below this value
     * @param max           no contour above this value
     * @param dash          dash contours below base
     *
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public void setContourLevels(float interval, float base, float min,
                                 float max, boolean dash)
            throws VisADException, RemoteException {

        setRange(min, max);
        super.setContourInterval(interval, base, min, max, dash);
    }

    /**
     * Returns whether this Displayable has a valid range (i.e., lowRange and
     * highRange are both not NaN's
     *
     * @return true if the range for color has been set (i.e. not NaN)
     */
    public boolean hasRange() {
        return ( !Double.isNaN(lowRange) && !Double.isNaN(highRange));
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
        super.setDisplayUnit(unit);
        if (useDisplayUnitForColor()) {
            //Make sure this unit is ok
            checkUnit(rgbRealType, unit);
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
     *  Apply the correct unit (either the displayUnit or the colorUnit)
     *  to the scalar map
     *
     * @param colorMap
     * @param rgbRealType
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
     * VisAD color control setup.
     *
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    private void setColorMaps() throws RemoteException, VisADException {

        ScalarMap oldColorMap = colorMap;
        if ( !alphaFlag) {
            colorMap = new ScalarMap(rgbRealType, Display.RGB);
        } else {
            colorMap = new ScalarMap(rgbRealType, Display.RGBA);
        }

        if (hasRange()) {
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
                if ((event.getId() == event.AUTO_SCALE) && hasRange()) {
                    colorMap.setRange(lowRange, highRange);
                }
            }
        });
        applyDisplayUnit(colorMap, rgbRealType);

        replaceScalarMap(oldColorMap, colorMap);
        fireScalarMapSetChange();
    }  //  end setColorMaps

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     *
     * @exception VisADException        VisAD failure.
     * @exception RemoteException       Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        return new Contour2DDisplayable(this);
    }

    /**
     * Set whether this GridDisplayable should have the data colored
     * by another parameter.
     *
     * @param yesno true if colored by another
     */
    public void setColoredByAnother(boolean yesno) {
        coloredByAnother = yesno;
    }

}
