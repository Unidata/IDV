/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.CommonUnit;
import visad.Data;
import visad.FieldImpl;
import visad.FunctionType;
import visad.Integer1DSet;
import visad.MathType;
import visad.Real;
import visad.RealType;
import visad.ScalarType;
import visad.Text;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;


import java.rmi.RemoteException;

import java.util.Arrays;


/**
 * Class for creating lat/lon labels
 */
public class LatLonLabels extends TextDisplayable {

    /** is this for latitude labels? */
    private boolean isLatitude;

    /** the base for labeling */
    private float base;

    /** the maximum value */
    private float max;

    /** the minimum value */
    private float min;

    /** the increment between labels */
    private float interval;

    /** the lines where the labels go */
    private float[] labelLines;

    /** the label format */
    private String labelFormat = "DD";

    /** the label format */
    private TupleType labelType;

    /** the label format */
    private boolean use360 = false;


    /** proto real */
    private Real latReal = new Real(RealType.Latitude, 0, CommonUnit.degree);

    /** proto real */
    private Real lonReal = new Real(RealType.Longitude, 0, CommonUnit.degree);

    /**
     * Default ctor
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    public LatLonLabels() throws VisADException, RemoteException {
        this("LatLonLabels");
    }

    /**
     * Create a LatLonLabels with the name
     *
     * @param name  the name for the DataReference
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    public LatLonLabels(String name) throws VisADException, RemoteException {
        this(name, TextType.getTextType(name));
    }

    /**
     * Create a LatLonLabels with the type for the values
     *
     * @param textType  the type for the text
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    public LatLonLabels(ScalarType textType)
            throws VisADException, RemoteException {
        this("LatLonLabels", textType);
    }

    /**
     * Create a default object
     *
     * @param name  the name for the DataReference
     * @param textType   the type for the values
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    public LatLonLabels(String name, ScalarType textType)
            throws VisADException, RemoteException {
        this(name, textType, true, 15, -90, 90, 0, new float[] { 0, -180 });
    }

    /**
     * Create LatLonLabels
     *
     * @param name       the name for the DataReference
     * @param textType   the type for the values
     * @param isLatitude true if is latitude
     * @param increment  the increment
     * @param min        the minimum value
     * @param max        the maximum value
     * @param base       the base value
     * @param labelLines the lines to label
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    public LatLonLabels(String name, ScalarType textType, boolean isLatitude,
                        float increment, float min, float max, float base,
                        float[] labelLines)
            throws VisADException, RemoteException {
        super(name, textType, false);
        this.isLatitude = isLatitude;
        this.interval   = increment;
        this.min        = min;
        this.max        = max;
        this.base       = base;
        this.labelLines = labelLines;
        createLabels();
    }

    /**
     * Copy constructor
     *
     * @param that  the other LatLonLabels
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Problem creating VisAD object
     */
    public LatLonLabels(LatLonLabels that)
            throws RemoteException, VisADException {
        super(that);
    }


    /**
     * Set the values for this LatLonLabels
     *
     * @param increment  the increment
     * @param base  the base
     * @param max the maximum value
     * @param min  the minimum value
     * @param labelLines  the label locations
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    public void setValues(float increment, float min, float max, float base,
                          float[] labelLines)
            throws VisADException, RemoteException {
        this.interval   = increment;
        this.min        = min;
        this.max        = max;
        this.base       = base;
        this.labelLines = labelLines;
        createLabels();
    }

    /**
     * Return the currently used ScalarType for display.
     * @return  the ScalarType
     */
    public ScalarType getTextType() {
        if (super.getTextType() instanceof RealType) {
            return TextType.getTextType(super.getTextType().getName());
        }
        return super.getTextType();
    }

    /**
     * Set the text type to use.
     * @param textType  RealType or TextType to map to Display.Text
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setTextType(ScalarType textType)
            throws RemoteException, VisADException {
        super.setTextType(textType);
        labelType = null;
    }


    /**
     * Make the label type
     *
     * @return  the type for the label data
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    private TupleType makeLabelType() throws VisADException, RemoteException {
        return new TupleType(new MathType[] { RealType.Latitude,
                RealType.Longitude, getTextType() });
    }

    /**
     * Generate the label field
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    protected void createLabels() throws VisADException, RemoteException {
        if (labelType == null) {
            labelType = makeLabelType();
        }
        // Handle overlapping -180/180 and 0/360 labels
        // if the max and min are the same modulo 360, shave a little off the min
        if ( !isLatitude) {
            if (0 == Float.compare(min + 360, max)) {
                min += 0.01;
            }
        }
        float[] labelVals = Misc.computeTicks(max, min, base, interval);
        /*
        Misc.printArray(isLatitude
                        ? "lats"
                        : "lons", labelVals);
        */
        int       numLabels  = labelVals.length * labelLines.length;
        FieldImpl labelField = new FieldImpl(
                                   new FunctionType(
                                       RealType.getRealType("index"),
                                       labelType), new Integer1DSet(
                                           numLabels));
        //float[][] vals = new float[3][numLabels];
        Tuple[] labelTuples = new Tuple[numLabels];
        int     m           = 0;
        for (int i = 0; i < labelLines.length; i++) {
            for (int j = 0; j < labelVals.length; j++) {
                Real lat   = isLatitude
                             ? latReal.cloneButValue(labelVals[j])
                             : latReal.cloneButValue(labelLines[i]);
                Real lon   = isLatitude
                             ? lonReal.cloneButValue(labelLines[i])
                             : lonReal.cloneButValue(labelVals[j]);
                Text label = new Text((TextType) getTextType(),
                                      formatLabel(labelVals[j]));
                labelTuples[m++] = new Tuple(labelType, new Data[] { lat, lon,
                        label }, false, false);
            }
        }
        labelField.setSamples(labelTuples, false);
        setData(labelField);
    }

    /**
     * Format the label
     * @param value  the value to format
     * @return formatted value
     */
    private String formatLabel(double value) {
        return Misc.formatLatOrLon(value, labelFormat, isLatitude, use360);
    }

    /**
     * @return the isLatitude
     */
    public boolean isLatitude() {
        return isLatitude;
    }

    /**
     * @param isLatitude the isLatitude to set
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating labels
     */
    public void setLatitude(boolean isLatitude)
            throws VisADException, RemoteException {
        if (this.isLatitude == isLatitude) {
            return;
        }
        this.isLatitude = isLatitude;
        createLabels();
    }

    /**
     * @return the base
     */
    public float getBase() {
        return base;
    }

    /**
     * @param base the base to set
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating labels
     */
    public void setBase(float base) throws VisADException, RemoteException {
        if (this.base == base) {
            return;
        }
        this.base = base;
        createLabels();
    }

    /**
     * @return the max
     */
    public float getMax() {
        return max;
    }

    /**
     * @param max the max to set
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating labels
     */
    public void setMax(float max) throws VisADException, RemoteException {
        if (this.max == max) {
            return;
        }
        this.max = max;
        createLabels();
    }

    /**
     * @return the min
     */
    public float getMin() {
        return min;
    }

    /**
     * @param min the min to set
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating labels
     */
    public void setMin(float min) throws VisADException, RemoteException {
        if (this.min == min) {
            return;
        }
        this.min = min;
        createLabels();
    }

    /**
     * Get the interval between labels (degrees)
     * @return the increment
     */
    public float getInterval() {
        return interval;
    }

    /**
     * Set the interval between labels (degrees)
     * @param interval the interval to set
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating labels
     */
    public void setInterval(float interval)
            throws VisADException, RemoteException {
        if (this.interval == interval) {
            return;
        }
        this.interval = interval;
        createLabels();
    }

    /**
     * Get whether to use 0-360 labels for longitude
     * @return true if using 0-360
     */
    public boolean getUse360() {
        return use360;
    }

    /**
     * Set whether to use 0-360 labels for longitude
     * @param yesorno  true to use 0-360 labels for longitude
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating labels
     */
    public void setUse360(boolean yesorno)
            throws VisADException, RemoteException {
        if (this.use360 == yesorno) {
            return;
        }
        this.use360 = yesorno;
        createLabels();
    }

    /**
     * Get the label format
     * @return the increment
     */
    public String getLabelFormat() {
        return labelFormat;
    }

    /**
     * Set the interval between labels (degrees)
     *
     * @param labelFormat the label format
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating labels
     */
    public void setLabelFormat(String labelFormat)
            throws VisADException, RemoteException {
        if ((labelFormat == null)
                || Misc.equals(this.labelFormat, labelFormat)) {
            return;
        }
        this.labelFormat = labelFormat;
        createLabels();
    }

    /**
     * @return the labelLines
     */
    public float[] getLabelLines() {
        return labelLines;
    }

    /**
     * @param labelLines the labelLines to set
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException problem creating labels
     */
    public void setLabelLines(float[] labelLines)
            throws VisADException, RemoteException {
        if (Arrays.equals(this.labelLines, labelLines)) {
            return;
        }
        this.labelLines = labelLines;
        createLabels();
    }


}
