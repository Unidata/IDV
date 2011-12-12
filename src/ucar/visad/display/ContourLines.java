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


import ucar.unidata.beans.*;

import ucar.unidata.util.ContourInfo;

import visad.*;

import java.rmi.RemoteException;

import java.util.Iterator;


/**
 * Provides support for a Displayable that comprises a set of contour lines.
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
 * <td>contourLevels</td>
 * <td>ContourLevels</td>
 * <td>set/get</td>
 * <td>no contour levels </td>
 * <td align=left>The displayed contour levels associated with this
 * instance.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>labeling</td>
 * <td>boolean</td>
 * <td>set/is</td>
 * <td><code>false</code></td>
 * <td align=left>Whether or not the contour lines should be labeled.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>contourRealType</td>
 * <td>visad.RealType</td>
 * <td>set/get</td>
 * <td><code>null</code></td>
 * <td align=left>The VisAD type of the contoured quantity.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>colorFill</td>
 * <td>boolean</td>
 * <td>set/get</td>
 * <td><code>null</code></td>
 * <td align=left>Whether or not the contour lines are color filled.</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.30 $
 */
public abstract class ContourLines extends LineDrawing {

    /**
     * The name of the "contour levels" property.
     */
    public static final String CONTOUR_LEVELS = "contourLevels";

    /**
     * The name of the "labeling" property.
     */
    public static final String LABELING = "labeling";

    /**
     * The name of the "contour real-type" property.
     */
    public static final String CONTOUR_REAL_TYPE = "contourRealType";

    /**
     * The name of the "color fill" property.
     */
    public static final String COLOR_FILL = "colorFill";

    /**
     * The contour levels.
     */
    private volatile ContourLevels contourLevels;

    /**
     * Whether or not the contour lines should be labeled.
     */
    private volatile boolean labeling = false;

    /** Contour ScalarMap */
    private volatile ScalarMap contourMap;

    /** Control for ScalarMap */
    private volatile ContourControl contourControl;

    /** RealType for data being contoured */
    private volatile RealType contourRealType;

    /** minimum contour value */
    private volatile float rangeMinimum = Float.NaN;

    /** maximum contour value */
    private volatile float rangeMaximum = Float.NaN;

    /** flag for color fill */
    private volatile boolean colorFill = false;

    /** dashed line style */
    private volatile int dashedStyle = GraphicsModeControl.DASH_STYLE;

    /** default label scale factor */
    private static final double DEFAULT_SIZE = 12.0;

    /** label size factor */
    private volatile double labelFactor = 1.;

    /** label font */
    private volatile Object labelFont = null;

    /** label alignment */
    private boolean alignLabels = true;


    /**
     * The {@link visad.Unit} for the display.
     */

    private static ContourLevels defaultContourLevels =
        new IrregularContourLevels(new float[0]);

    /**
     * Constructs from a name for the Displayable and the type of the contour
     * parameter.  The contour levels will be the VisAD default and the lines
     * will not be labeled.
     * @param name              The name for the displayable.
     * @param contourRealType   The type of the contour parameter.  May be
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ContourLines(String name, RealType contourRealType)
            throws VisADException, RemoteException {

        super(name);

        contourLevels        = defaultContourLevels;
        this.contourRealType = contourRealType;

        if (contourRealType != null) {
            setContourMap();
        }
    }

    /**
     * Constructs from another instance.  The following attributes are set from
     * the other instance: contour levels, labeling, the contour RealType, the
     * range minimum, and the range maximum.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ContourLines(ContourLines that)
            throws VisADException, RemoteException {

        super(that);

        contourLevels   = that.contourLevels;    // immutable object
        labeling        = that.labeling;
        contourRealType = that.contourRealType;  // immutable object
        rangeMinimum    = that.rangeMinimum;
        rangeMaximum    = that.rangeMaximum;

        if (contourRealType != null) {
            setContourMap();
        }
    }

    /**
     * Sets the RealType of the contoured parameter.
     * @param realType          The RealType of the contoured parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setContourRealType(RealType realType)
            throws RemoteException, VisADException {

        if ( !realType.equals(contourRealType)) {
            RealType oldValue = contourRealType;
            contourRealType = realType;
            setContourMap();
            firePropertyChange(CONTOUR_REAL_TYPE, oldValue, contourRealType);
        }
    }

    /**
     * Returns the RealType of the contoured parameter.
     * @return                  The RealType of the contoured parameter.  May
     *                          be <code>null</code>.
     */
    public RealType getContourRealType() {
        return contourRealType;
    }

    /**
     * Sets the range of contour values.
     *
     * @param min               The minimum contour value.
     * @param max               The maximum contour value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setRange(float min, float max)
            throws RemoteException, VisADException {

        rangeMinimum = min;
        rangeMaximum = max;

        if (contourControl != null) {

            setContourLevels();
        }
    }

    /**
     * Sets the contour values.  This method fires a PropertyChangeEvent for
     * CONTOUR_LEVELS.
     * @param contourLevels     The contour values.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #CONTOUR_LEVELS
     */
    public final void setContourLevels(ContourLevels contourLevels)
            throws RemoteException, VisADException {

        if ( !contourLevels.equals(this.contourLevels)) {
            ContourLevels oldValue = this.contourLevels;

            this.contourLevels = contourLevels;

            if (contourControl != null) {
                setContourLevels();
            }

            firePropertyChange(CONTOUR_LEVELS, oldValue, this.contourLevels);
        }
    }

    /**
     * Sets the contour values, with interval, min, max, base.
     * This method fires a PropertyChangeEvent for CONTOUR_LEVELS.
     *
     * @param inter             The contour interval.
     * @param min               The minimum contour value.
     * @param max               The maximum contour value.
     * @param base              The base contour value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #CONTOUR_LEVELS
     */
    public final void setContourInterval(float inter, float base, float min,
                                         float max)
            throws RemoteException, VisADException {
        setContourInterval(inter, base, min, max, false);
    }

    /**
     * Sets the contour values, with interval, min, max, base and dash.
     * This method fires a PropertyChangeEvent for CONTOUR_LEVELS.
     *
     * @param inter             The contour interval.
     * @param min               The minimum contour value.
     * @param max               The maximum contour value.
     * @param base              The base contour value.
     * @param dash              Whether or not to draw dashed lines for contours
     *                          less than the base contour value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #CONTOUR_LEVELS
     */
    public final void setContourInterval(float inter, float base, float min,
                                         float max, boolean dash)
            throws RemoteException, VisADException {

        /*
        ContourLevels oldValue = this.contourLevels;

        this.contourLevels = new RegularContourLevels(inter, base, min, max);

        //setContourLevels(new RegularContourLevels(inter, base, min, max));
        if (contourControl != null) {

            //contourControl.setContourInterval(inter, min, max, base);
            contourControl.setLevels(
                visad.Contour2D.intervalToLevels(
                inter, min, max, base, new boolean[1]), base, dash);
            setContourLevels();
        }
        */

        //System.out.println("           ContourLiness int="+inter+"  base="+base+
        //                 "  min = "+min+"  max = "+max); 

        setContourLevels(new RegularContourLevels(inter, base, min, max,
                dash));
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
     * @throws BadMappingException      The RealType of the contour parameter
     *                          has not been set or its ScalarMap is alread in
     *                          the set.
     */
    protected void setScalarMaps(ScalarMapSet maps)
            throws BadMappingException {

        if (contourMap == null) {
            throw new BadMappingException(getClass().getName()
                                          + ".setScalarMaps(ScalarMapSet): "
                                          + "Contour parameter not yet set");
        }

        maps.add(contourMap);
        super.setScalarMapSet(maps);
    }

    /**
     * Returns the contour levels.
     * @return          The Contour levels.  The default value is an empty
     *                  set of levels.
     */
    public final ContourLevels getContourLevels() {
        return contourLevels;
    }

    /**
     * Gets the contour values.
     * @return          The contour values.
     * @throws VisADException   VisAD failure.
     */
    public final float[] getContourValues() throws VisADException {
        return contourLevels.getLevels(rangeMinimum, rangeMaximum);
    }

    /**
     * Set appropriate contour levels info
     *
     * @param contourInfo   Contains contour and labeling information
     *
     * @exception VisADException   VisAD failure.
     * @exception RemoteException  Java RMI failure.
     */
    public void setContourInfo(ContourInfo contourInfo)
            throws VisADException, RemoteException {

        if (contourInfo == null) {
            return;
        }

        setActive(false);
        setContourLevels(
            new IrregularContourLevels(
                contourInfo.getContourLevels(), contourInfo.getBase(),
                contourInfo.getDashOn()));
        setLabeling(contourInfo.getIsLabeled());
        setLineWidth(contourInfo.getLineWidth());
        setDashedStyle(contourInfo.getDashedStyle());
        setFont(contourInfo.getFont(), contourInfo.getLabelSize(),
                contourInfo.getAlignLabels());
        setActive(true);
    }

    /**
     * Set whether the contours should be displayed as color-filled
     * contours.
     * @param yesorno  true for color fill
     * @throws VisADException  unable to set this
     * @throws RemoteException  unable to set this on remote display
     */
    public void setColorFill(boolean yesorno)
            throws VisADException, RemoteException {
        if (yesorno != colorFill) {
            Boolean oldValue = new Boolean(colorFill);

            this.colorFill = yesorno;

            if (contourControl != null) {
                contourControl.setContourFill(colorFill);
            }
            firePropertyChange(LABELING, oldValue, new Boolean(colorFill));
        }
    }

    /**
     * Ask if color filled contours are enabled.
     * @return true if using color-filled contours.
     */
    public boolean getColorFillEnabled() {
        return colorFill;
    }

    /**
     * Sets the labeling of contour lines.
     * @param on                Whether or not the contour lines should be
     *                          labeled.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public final void setLabeling(boolean on)
            throws VisADException, RemoteException {

        if (on != labeling) {
            Boolean oldValue = new Boolean(labeling);

            this.labeling = on;

            if (contourControl != null) {
                contourControl.enableLabels(labeling);
            }

            firePropertyChange(LABELING, oldValue, new Boolean(labeling));
        }
    }

    /**
     * Returns the labeling of contour lines.
     * @return                  <code>true</code> if and only if the contour
     *                          lines should be labeled.
     */
    public final boolean isLabeling() {
        return labeling;
    }

    /**
     * Set the units for the displayed range
     * @param unit Unit for display
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDisplayUnit(Unit unit)
            throws VisADException, RemoteException {
        //Make sure this unit is ok
        checkUnit(contourRealType, unit);
        super.setDisplayUnit(unit);
        applyDisplayUnit(contourMap, contourRealType);
    }



    /**
     * Set the contour levels.  Assumes that the control is not null.
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   Problem setting the contour levels
     */
    private void setContourLevels() throws VisADException, RemoteException {
        if (contourLevels != defaultContourLevels) {
            if ((rangeMinimum != rangeMinimum)
                    || (rangeMaximum != rangeMaximum)) {
                contourLevels.setControl(contourControl);
            } else {
                contourLevels.setControl(contourControl, rangeMinimum,
                                         rangeMaximum);
            }
        }
    }

    /**
     * Set the dashed style.
     * @param  style  dashed line style
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException Problem setting the dashed style
     */
    public void setDashedStyle(int style)
            throws RemoteException, VisADException {

        if (style != dashedStyle) {

            dashedStyle = style;

            if (contourControl != null) {
                contourControl.setDashedStyle(dashedStyle);
            }
        }
    }

    /**
     * Set the font
     * @param  font  the font name/weight
     * @param  size  the label (font) size
     * @param align _more_
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException Problem setting the dashed style
     */
    public void setFont(Object font, int size, boolean align)
            throws RemoteException, VisADException {

        double factor = size / DEFAULT_SIZE;

        //if (font != null) {

        if ((font != labelFont) || (factor != labelFactor)
                || (align != alignLabels)) {
            labelFont   = font;
            labelFactor = factor;
            alignLabels = align;

            if (contourControl != null) {
                contourControl.setLabelFont(font);
                contourControl.setLabelSize(factor);
                contourControl.setAlignLabels(align);
            }
        }
        //}
    }

    /**
     * Set the dashed style.
     * @return  dashed line style
     */
    public int getDashedStyle() {
        return dashedStyle;
    }

    /**
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setContourMap() throws RemoteException, VisADException {

        ScalarMap oldContourMap = contourMap;
        contourMap = new ScalarMap(contourRealType, Display.IsoContour);
        contourMap.addScalarMapListener(new ScalarMapListener() {
            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {
                int id = event.getId();
                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    contourControl = (ContourControl) contourMap.getControl();
                    if (contourControl != null) {
                        setContourLevels();
                        contourControl.enableLabels(labeling);
                        contourControl.setContourFill(colorFill);
                        contourControl.setDashedStyle(dashedStyle);
                        contourControl.setLabelFont(labelFont);
                        contourControl.setLabelSize(labelFactor);
                        contourControl.setAlignLabels(alignLabels);
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event) {
                //System.out.println("ContourLines: Autoscaling");
            }  // ignore
        });

        applyDisplayUnit(contourMap, contourRealType);

        replaceScalarMap(oldContourMap, contourMap);  // in Displayable.java
        fireScalarMapSetChange();

    }
}
