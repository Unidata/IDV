/*
 * $Id: ProfileCombinerCell.java,v 1.5 2005/05/13 18:33:35 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.rmi.RemoteException;

import java.util.Arrays;

import ucar.visad.functiontypes.AtmosphericProfile;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.VirtualTemperature;
import ucar.visad.Util;

import visad.CoordinateSystem;

import visad.Data;

import visad.DataReference;

import visad.ErrorEstimate;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.Gridded1DSet;

import visad.RealType;

import visad.SampledSet;

import visad.Set;

import visad.TypeException;

import visad.UnionSet;

import visad.Unit;

import visad.VisADException;


/**
 * Combines two profiles together.  The profiles must have the type specified
 * during construction.  The actual units in the domain and range of the
 * profiles must be identical.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:35 $
 */
public final class ProfileCombinerCell extends ComputeCell {

    /** output data type */
    private final FunctionType outType;

    /** range unit */
    private final Unit rangeUnit;

    /** no data */
    private final FlatField noData;

    /**
     * Constructs from data references.
     *
     * @param pro1Ref          The input first profile reference.
     * @param pro2Ref          The input second profile reference.
     * @param rangeType        The type of the range of the output profile.
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public ProfileCombinerCell(DataReference pro1Ref, DataReference pro2Ref, RealType rangeType)
            throws VisADException, RemoteException {

        super("ProfileCombinerCell", new DataReference[]{ pro1Ref, pro2Ref },
              noDataField(rangeType));

        outType   = new AtmosphericProfile(rangeType);
        rangeUnit = rangeType.getDefaultUnit();
        noData    = noDataField(rangeType);

        enableAllInputRefs();
    }

    /**
     * Computes the combined profile of two input profiles.
     *
     * @param datums                The input data in the same order as during
     *                              construction: <code>datums[0]</code> is the
     *                              first profile and <code>datums[1]</code> is
     *                              the second profile.
     * @return                      The corresponding combined profile.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        FlatField pro  = noData;
        Field     pro1 = (Field) datums[0];

        if (pro1 != null) {
            Util.vetType(outType, pro1);

            Field pro2 = (Field) datums[1];

            if (pro2 != null) {
                Util.vetType(outType, pro2);

                SampledSet dom1     = (SampledSet) pro1.getDomainSet();
                SampledSet dom2     = (SampledSet) pro2.getDomainSet();
                Unit       dom1Unit = dom1.getSetUnits()[0];
                Unit       dom2Unit = dom2.getSetUnits()[0];

                if ( !Unit.canConvert(dom1Unit, dom2Unit)) {
                    throw new TypeException("dom1Unit=" + dom1Unit
                                            + "; dom2Unit=" + dom2Unit);
                }

                Unit range1Unit = pro1.getDefaultRangeUnits()[0];
                Unit range2Unit = pro2.getDefaultRangeUnits()[0];

                if ( !Unit.canConvert(range1Unit, range2Unit)) {
                    throw new TypeException("range1Unit=" + range1Unit
                                            + "; range2Unit=" + range2Unit);
                }

                CoordinateSystem coordSys1 =
                    Util.getRangeCoordinateSystem(pro1);
                CoordinateSystem coordSys2 =
                    Util.getRangeCoordinateSystem(pro2);

                if ((coordSys1 == null)
                    ? coordSys2 != null
                    : !coordSys1.equals(coordSys2)) {
                    throw new TypeException(coordSys1.toString() + " != "
                                            + coordSys2);
                }

                CoordinateSystem[] coordSyses1 =
                    Util.getRangeCoordinateSystems(pro1);
                CoordinateSystem[] coordSyses2 =
                    Util.getRangeCoordinateSystems(pro2);

                if ((coordSyses1 == null)
                    ? coordSyses2 != null
                    : !Arrays.equals(coordSyses1, coordSyses2)) {
                    throw new TypeException();
                }

                if ((coordSyses1 == null)
                    ? coordSyses2 != null
                    : !Arrays.equals(coordSyses1, coordSyses2)) {
                    throw new TypeException();
                }

                int     n1         = dom1.getLength();
                int     n2         = dom2.getLength();
                float[] newDomVals = new float[n1 + n2];

                System.arraycopy(
                    AirPressure.getRealType().getDefaultUnit().toThis(
                        dom1.getSamples(true)[0], dom1Unit), 0, newDomVals,
                            0, n1);
                System.arraycopy(
                    AirPressure.getRealType().getDefaultUnit().toThis(
                        dom2.getSamples(true)[0], dom2Unit), 0, newDomVals,
                            n1, n2);

                if (Util.isSorted(newDomVals)) {
                    pro = new FlatField(outType,
                                        Gridded1DSet.create(outType.getDomain(), newDomVals, (CoordinateSystem) null, (Unit) null, (ErrorEstimate) null),
                                        coordSys1, coordSyses1, (Set[]) null,
                                        (Unit[]) null);

                    float[] values = new float[n1 + n2];

                    System.arraycopy(
                        rangeUnit.toThis(
                            pro1.getFloats(false)[0], range1Unit), 0, values,
                                0, n1);
                    System.arraycopy(
                        rangeUnit.toThis(
                            pro2.getFloats(false)[0], range2Unit), 0, values,
                                n1, n2);
                    pro.setSamples(new float[][] {
                        values
                    }, false);
                }
            }
        }

        return pro;
    }
}







