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

package ucar.visad;


import visad.*;


/**
 * A CoordinateSystem to transform between a Tuple and a reference in
 * reverse order (eg:, (Latitude, Longitude) <-> (Longitude, Latitude)
 */
public class ReverseCoordinateSystem extends CoordinateSystem {


    /**
     * Construct a coordinate system with reverse reference specified
     * @param  reference  MathType of values
     * @throws VisADException  problem instantiating the CS
     */
    public ReverseCoordinateSystem(RealTupleType reference)
            throws VisADException {
        super(reference, makeCSUnits(reference.getDefaultUnits()));
    }


    /**
     * Convert values to reverse values.
     * @param  values  array of values
     * @return array of reverse of values
     * @throws VisADException values dimension not the same as CS dimension
     */
    public double[][] toReference(double[][] values) throws VisADException {
        if ((values == null) || (values[0].length < 1)) {
            return values;
        }
        if (values.length != getDimension()) {
            throw new CoordinateSystemException("ReverseCoordinateSystem."
                    + "toReference: values wrong dimension");
        }

        double[][] revValues = new double[getDimension()][];

        int        l         = getDimension();
        for (int i = 0; i < getDimension(); i++) {
            revValues[--l] = values[i];
        }
        return revValues;
    }


    /**
     * Convert logrithmic values to values.
     * @param  revValues  array of reverse values
     * @return array of values
     * @throws VisADException revValues dimension not the same as CS dimension
     */
    public double[][] fromReference(double[][] revValues)
            throws VisADException {
        if ((revValues == null) || (revValues[0].length < 1)) {
            return revValues;
        }
        if (revValues.length != getDimension()) {
            throw new CoordinateSystemException("ReverseCoordinateSystem."
                    + "fromReference: revValues wrong dimension");
        }

        double[][] values = new double[getDimension()][];

        int        l      = getDimension();
        for (int i = 0; i < getDimension(); i++) {
            values[--l] = revValues[i];
        }
        return values;
    }

    /**
     * Convert values to reverse values.
     * @param  values  array of values
     * @return array of reverse of values
     * @throws VisADException values dimension not the same as CS dimension
     */
    public float[][] toReference(float[][] values) throws VisADException {
        if ((values == null) || (values[0].length < 1)) {
            return values;
        }
        if (values.length != getDimension()) {
            throw new CoordinateSystemException("ReverseCoordinateSystem."
                    + "toReference: values wrong dimension");
        }

        float[][] revValues = new float[getDimension()][];

        int       l         = getDimension();
        for (int i = 0; i < getDimension(); i++) {
            revValues[--l] = values[i];
        }
        return revValues;
    }


    /**
     * Convert logrithmic values to values.
     * @param  revValues  array of reverse values
     * @return array of values
     * @throws VisADException revValues dimension not the same as CS dimension
     */
    public float[][] fromReference(float[][] revValues)
            throws VisADException {
        if ((revValues == null) || (revValues[0].length < 1)) {
            return revValues;
        }
        if (revValues.length != getDimension()) {
            throw new CoordinateSystemException("ReverseCoordinateSystem."
                    + "fromReference: revValues wrong dimension");
        }

        float[][] values = new float[getDimension()][];

        int       l      = getDimension();
        for (int i = 0; i < getDimension(); i++) {
            values[--l] = revValues[i];
        }
        return values;
    }

    /**
     * See if the Object in question is equal to this ReverseCoordinateSystem
     * @param cs  Object in question
     * @return  true if cs's reference tuples and base is equal to this's
     */
    public boolean equals(Object cs) {
        if ( !(cs instanceof ReverseCoordinateSystem)) {
            return false;
        }
        ReverseCoordinateSystem that = (ReverseCoordinateSystem) cs;
        return (this == that)
               || that.getReference().equals(this.getReference());
    }

    /**
     * Make the units for this CS (reversal of the reference)
     *
     * @param refUnits CS of the reference
     *
     * @return reversed unit array
     */
    private static Unit[] makeCSUnits(Unit[] refUnits) {
        int    l       = refUnits.length;
        Unit[] csUnits = new Unit[l];
        for (int i = 0; i < refUnits.length; i++) {
            csUnits[--l] = refUnits[i];
        }
        return csUnits;
    }
}
