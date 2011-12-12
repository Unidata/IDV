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


import visad.*;

import java.awt.Color;

import java.rmi.RemoteException;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.util.Arrays;


/**
 * Labels for RingSets
 *
 * @author IDV Development Team
 * @version $Revision: 1.6 $
 */
public class RingLabels extends TextDisplayable {

    /** bearing */
    Real bearing = null;

    /** bearing RealType */
    RealType bearingType;

    /** range RealType */
    RealType rangeType;

    /** polar coordinate RealTupleType */
    RealTupleType polarType;

    /** values for the ring labels */
    Gridded1DSet values = null;

    /** label units */
    Unit labelUnit = null;

    /**
     * Use a specific RingSet to define the labels.  Each line
     * in the RingSet will be labeled.
     *
     * @param name  name of this object
     * @param polarType  type for the polar transform
     * @param color  Color for the ring set
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public RingLabels(String name, RealTupleType polarType, Color color)
            throws RemoteException, VisADException {
        this(name, polarType,
             new Real(
                 (RealType) polarType.getComponent(
                     PolarLineDrawing.getBearingIndex(polarType)), 0), color);

    }

    /**
     * Construct a set of RingLabels
     *
     * @param name       name of this object
     * @param polarType  type for the polar transform
     * @param bearing    bearing to put the labels
     * @param color      Color for the ring set
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public RingLabels(String name, RealTupleType polarType, Real bearing,
                      Color color)
            throws RemoteException, VisADException {
        //super(TextType.getTextType(name));
        super(RealType.getRealType(name));
        this.polarType = polarType;
        bearingType = (RealType) polarType.getComponent(
            PolarLineDrawing.getBearingIndex(polarType));
        rangeType = (RealType) polarType.getComponent(1
                - PolarLineDrawing.getBearingIndex(polarType));
        this.bearing = bearing;
        labelUnit    = rangeType.getDefaultUnit();
        createLabels();
        setColor(color);
    }

    /**
     * Set the label units
     *
     * @param unit  unit for the labels
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelUnit(Unit unit)
            throws VisADException, RemoteException {
        labelUnit = unit;
        createLabels();
    }

    /**
     * Get the label unit.
     *
     * @return  label unit
     */
    public Unit getLabelUnit() {
        return labelUnit;
    }

    /**
     * Set the label values
     *
     * @param labelValues values in a Gridded1DSet
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelValues(Gridded1DSet labelValues)
            throws VisADException, RemoteException {
        if (labelValues != null) {
            if (rangeType.equals(
                    ((SetType) labelValues.getType()).getDomain())) {
                throw new VisADException(
                    "labelValue set not compatible with this object's range type");
            }
        }
        this.values = labelValues;
        createLabels();
    }

    /**
     * Set the label values from the array.  Use label type to construct
     * the set.
     *
     * @param values  array of label values
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelValues(float[] values)
            throws VisADException, RemoteException {
        setLabelValues(values, labelUnit);
    }

    /**
     * Set the label values from the array.  Use label type to construct
     * the set.
     *
     * @param values    label values
     * @param units     unit for the labels.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelValues(float[] values, Unit units)
            throws VisADException, RemoteException {
        setLabelValues(new Gridded1DSet(rangeType, new float[][] {
            values
        }, values.length, (CoordinateSystem) null, new Unit[] { units },
           (ErrorEstimate[]) null));
    }

    /**
     * Set the label values base on the increment and maximum value
     *
     * @param rangeInc    increment between labels
     * @param rangeMax    maximum label value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelValues(Real rangeInc, Real rangeMax)
            throws VisADException, RemoteException {
        float min = (float) rangeInc.getValue(labelUnit);
        float[] values = Contour2D.intervalToLevels(min, min,
                             (float) rangeMax.getValue(labelUnit), min,
                             new boolean[] { false });
        if (values != null) {
            setLabelValues(values, labelUnit);
        } else {
            setLabelValues((Gridded1DSet) null);
        }
    }

    /**
     * Set the position of the labels along the bearing (azimuth)
     *
     * @param bearing  azimuth value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setLabelPosition(Real bearing)
            throws VisADException, RemoteException {
        if ( !Unit.canConvert(bearing.getUnit(), CommonUnit.degree)) {
            throw new VisADException(
                "bearing units not compatible with this object's bearing type");
        }
        this.bearing = bearing;
        createLabels();
    }

    /**
     * Create the labels.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void createLabels() throws VisADException, RemoteException {
        if ((values == null) || (bearing == null)) {
            setData(new SingletonSet(new RealTuple(polarType)));
            return;
        }
        float bear = (float) bearing.getValue(bearingType.getDefaultUnit());
        int numLabels = values.getLength();
        float[][] ranges = Unit.convertTuple(values.getSamples(),
                                             values.getSetUnits(),
                                             new Unit[] { labelUnit });

        float[] bearings = new float[numLabels];
        Arrays.fill(bearings, bear);
        float[][] samples = (PolarLineDrawing.getBearingIndex(polarType) == 0)
                            ? new float[][] {
            bearings, ranges[0]
        }
                            : new float[][] {
            ranges[0], bearings
        };


        Gridded2DSet domain =
            new Gridded2DSet(polarType, samples, numLabels,
                             (CoordinateSystem) null,
                             (PolarLineDrawing.getBearingIndex(polarType)
                              == 0)
                             ? new Unit[] { bearingType.getDefaultUnit(),
                                            labelUnit }
                             : new Unit[] { labelUnit, bearingType
                                 .getDefaultUnit() }, (ErrorEstimate[]) null);
        ScalarType   type       = getTextType();
        FunctionType ft         = new FunctionType(polarType, type);
        FieldImpl    labelField = new FieldImpl(ft, domain);
        Scalar[]     labels     = null;
        if (type instanceof TextType) {
            labels = new Text[numLabels];
            NumberFormat labelFormat = getNumberFormat();
            for (int i = 0; i < numLabels; i++) {
                labels[i] = new Text((TextType) type,
                                     labelFormat.format((int) ranges[0][i]));
            }
        } else if (type instanceof RealType) {
            labels = new Real[numLabels];
            for (int i = 0; i < numLabels; i++) {
                labels[i] =
                    (Unit.canConvert(((RealType) type).getDefaultUnit(),
                                     labelUnit) == true)
                    ? new Real((RealType) type, ranges[0][i], labelUnit)
                    : new Real((RealType) type, ranges[0][i]);
            }
        }
        labelField.setSamples(labels, false);

        setData(labelField);
    }
}
