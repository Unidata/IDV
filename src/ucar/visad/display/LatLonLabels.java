/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
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

import visad.FlatField;
import visad.FunctionType;
import visad.Integer1DSet;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarType;
import visad.VisADException;

import java.rmi.RemoteException;

import java.util.Arrays;


/**
 * Class description
 *
 *
 * @version        Enter version here..., Thu, Aug 2, '12
 * @author         Enter your name here...
 */
public class LatLonLabels extends TextDisplayable {

    /** is this for latitude labels? */
    private boolean isLatitude;

    /** the base for labelling */
    private float base;

    /** the maximum value */
    private float max;

    /** the minimum value */
    private float min;

    /** the increment between labels */
    private float increment;

    /** the lines where the labels go */
    private float[] labelLines;

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
        this(name, RealType.getRealType(name));
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
        this.increment  = increment;
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
        // TODO Auto-generated constructor stub
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
        this.increment  = increment;
        this.min        = min;
        this.max        = max;
        this.base       = base;
        this.labelLines = labelLines;
        createLabels();
    }

    /**
     * Generate the label field
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   Unable to create VisAD object
     */
    protected void createLabels()
            throws VisADException, RemoteException {
        RealTupleType labelType =
            new RealTupleType(RealType.Latitude, RealType.Longitude,
                              RealType.getRealType(getTextType().getName()));
        float[]   labelVals  = Misc.computeTicks(max, min, base, increment);
        int       numLabels  = labelVals.length * labelLines.length;
        FlatField labelField = new FlatField(
                                   new FunctionType(
                                       RealType.getRealType("index"),
                                       labelType), new Integer1DSet(
                                           numLabels));
        float[][] vals = new float[3][numLabels];
        int       m    = 0;
        for (int i = 0; i < labelLines.length; i++) {
            for (int j = 0; j < labelVals.length; j++) {
                vals[0][m] = isLatitude
                             ? labelVals[j]
                             : labelLines[i];
                vals[1][m] = isLatitude
                             ? labelLines[i]
                             : labelVals[j];
                vals[2][m] = labelVals[j];
                m++;
            }
        }
        labelField.setSamples(vals, false);
        setData(labelField);
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
     * @throws RemoteException _more_
     * @throws VisADException _more_
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
     * @throws RemoteException _more_
     * @throws VisADException _more_
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
     * @throws RemoteException _more_
     * @throws VisADException _more_
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
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setMin(float min) throws VisADException, RemoteException {
        if (this.min == min) {
            return;
        }
        this.min = min;
        createLabels();
    }

    /**
     * @return the increment
     */
    public float getIncrement() {
        return increment;
    }

    /**
     * @param increment the increment to set
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setIncrement(float increment)
            throws VisADException, RemoteException {
        if (this.increment == increment) {
            return;
        }
        this.increment = increment;
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
     * @throws RemoteException _more_
     * @throws VisADException _more_
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
